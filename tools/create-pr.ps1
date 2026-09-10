# create-pr.ps1 - Create a GitHub PR without native-arg quoting pitfalls (built by Gate 5)
# Usage:
#   $env:GH_TOKEN = '<pat>'
#   .\tools\create-pr.ps1 -Title "feat(finance): ..." -Body "..."        # current branch -> main
#   .\tools\create-pr.ps1 -Title "..." -Branch feat/xxx -Base main -Body "..."
# Outputs PR number and URL. Body is sent via a temp file (lessons L08/L09).

param(
    [Parameter(Mandatory = $true)][string]$Title,
    [string]$Branch,
    [string]$Base = 'main',
    [string]$Body = '',
    [string]$Repo = 'Webzhangjinlong/finance-system'
)

$ErrorActionPreference = 'Stop'

if (-not $env:GH_TOKEN) {
    Write-Error "GH_TOKEN environment variable is required (set it in the current shell, do not hardcode)."
    exit 2
}

if (-not $Branch) {
    $Branch = git branch --show-current
    if (-not $Branch) { Write-Error "Cannot detect current branch"; exit 2 }
}

$bodyFile = Join-Path $env:TEMP "pr_body_$([guid]::NewGuid().ToString('N')).json"
$json = @{
    title = $Title
    head  = $Branch
    base  = $Base
    body  = $Body
} | ConvertTo-Json -Compress

# Write without BOM so GitHub JSON parser accepts it
[System.IO.File]::WriteAllText($bodyFile, $json, (New-Object System.Text.UTF8Encoding($false)))

try {
    $resp = curl.exe -s -X POST "https://api.github.com/repos/$Repo/pulls" `
        -H "Authorization: token $env:GH_TOKEN" `
        -H "Accept: application/vnd.github+json" `
        -H "Content-Type: application/json" `
        -d "@$bodyFile"
    $pr = $resp | ConvertFrom-Json
    if (-not $pr.number) {
        Write-Error "PR creation failed: $resp"
        exit 1
    }
    Write-Host "PR #$($pr.number) created: $($pr.html_url)"
    Write-Host "State: $($pr.state)"
} finally {
    Remove-Item $bodyFile -Force -ErrorAction SilentlyContinue
}
