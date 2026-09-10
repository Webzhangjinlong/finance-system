# Merge a PR into a protected branch (solo development mode).
#
# Background: main branch protection requires 1 approving review + required
# status checks (backend-ci, frontend-ci), but GitHub forbids authors from
# approving their own PR. Solo strategy:
#   temporarily disable enforce_admins -> owner merges -> restore & verify.
#
# Usage:
#   $env:GH_TOKEN = 'ghp_xxx'    # PAT with repo scope
#   .\tools\merge-pr.ps1 -PrNumber 2
#   .\tools\merge-pr.ps1 -PrNumber 2 -CommitTitle "feat: xxx (#2)"
#
# Safety guarantees:
#   1) Protection state is re-checked after merge and enforce_admins=true is
#      forced back and verified;
#   2) required_status_checks (backend-ci, frontend-ci) is ALWAYS part of the
#      PUT body, because PUT /branches/main/protection is a FULL replace -
#      omitting it silently removes the CI gate (L35 regression, fixed Gate 10);
#   3) Any failure enters the fallback restore path; an open protection window
#      must never be left behind.

param(
    [Parameter(Mandatory = $true)]
    [int]$PrNumber,

    [string]$CommitTitle = "",

    [string]$Repo = "Webzhangjinlong/finance-system"
)

if (-not $env:GH_TOKEN) {
    Write-Error "Please set env var GH_TOKEN (repo scope required)"
    exit 1
}

$headers = @{
    Authorization = "Bearer $env:GH_TOKEN"
    Accept        = "application/vnd.github+json"
}

# Base protection rule body, kept in sync with the repository's real rules.
# NOTE: PUT /branches/main/protection is a FULL replace; required_status_checks
# must always be present here or CI gates are silently lost on every merge.
$base = @{
    required_status_checks        = @{
        contexts = @('backend-ci', 'frontend-ci')
        strict   = $false
    }
    required_pull_request_reviews = @{
        required_approving_review_count = 1
        dismiss_stale_reviews           = $true
    }
    restrictions                  = $null
    allow_force_pushes            = $false
    allow_deletions               = $false
}

$protectionUri = "https://api.github.com/repos/$Repo/branches/main/protection"
$mergeUri = "https://api.github.com/repos/$Repo/pulls/$PrNumber/merge"

function Set-EnforceAdmins([bool]$enabled) {
    $body = $base.Clone()
    $body['enforce_admins'] = $enabled
    Invoke-RestMethod -Uri $protectionUri -Method Put -Headers $headers `
        -Body ($body | ConvertTo-Json -Depth 5) -ContentType "application/json" | Out-Null
}

function Get-EnforceAdmins {
    $p = Invoke-RestMethod -Uri $protectionUri -Method Get -Headers $headers
    return $p.enforce_admins.enabled
}

try {
    # [1/3] Backup current state and temporarily disable admin enforcement.
    $enforceBefore = Get-EnforceAdmins
    Write-Host "Current enforce_admins = $enforceBefore"
    Set-EnforceAdmins $false
    Write-Host "[1/3] enforce_admins temporarily disabled"

    # [2/3] Merge the PR (squash).
    $mgBody = @{ merge_method = "squash" }
    if ($CommitTitle) {
        $mgBody['commit_title'] = $CommitTitle
    }
    $mg = Invoke-RestMethod -Uri $mergeUri -Method Put -Headers $headers `
        -Body ($mgBody | ConvertTo-Json) -ContentType "application/json"
    Write-Host "[2/3] merged=$($mg.merged) message=$($mg.message)"

    # [3/3] Restore and verify.
    Set-EnforceAdmins $true
    $enforceAfter = Get-EnforceAdmins
    if ($enforceAfter -ne $true) {
        throw "Restore failed: enforce_admins=$enforceAfter"
    }
    Write-Host "[3/3] protection restored, enforce_admins=true (verified)"
}
catch {
    Write-Error "Merge flow error: $($_.Exception.Message)"
    # Fallback: try hard to restore protection so no open window remains.
    try {
        Set-EnforceAdmins $true
        $verify = Get-EnforceAdmins
        Write-Host "(fallback) restore attempted, enforce_admins=$verify"
    }
    catch {
        Write-Error "Fallback restore failed! Check protection manually: $protectionUri"
    }
    exit 1
}
