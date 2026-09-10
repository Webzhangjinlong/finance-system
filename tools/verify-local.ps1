# verify-local.ps1 - Local full verification (built by Gate 5)
# Runs: backend `mvn verify` (tests + ArchUnit + Flyway + JaCoCo) and frontend `npm ci && npm run build`.
# Usage:
#   .\tools\verify-local.ps1            # backend + frontend
#   .\tools\verify-local.ps1 -BackendOnly
#   .\tools\verify-local.ps1 -FrontendOnly
# Exit code 0 = all green, 1 = any failure.
# NOTE: does NOT use $ErrorActionPreference='Stop' - native stderr warnings (JVM/npm)
#       must not be treated as failures (lessons L20).

param(
    [switch]$BackendOnly,
    [switch]$FrontendOnly
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverDir = Join-Path $repoRoot 'finance-server'
$webDir = Join-Path $repoRoot 'finance-web'
$script:failed = $false

function Invoke-Step([string]$name, [scriptblock]$block) {
    Write-Host "`n===== $name ====="
    & $block
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        Write-Host "[FAIL] $name (exit $code)" -ForegroundColor Red
        $script:failed = $true
    } else {
        Write-Host "[OK] $name"
    }
}

if (-not $FrontendOnly) {
    Invoke-Step 'Backend mvn verify' {
        Push-Location $serverDir
        try { mvn -pl finance-admin -am verify } finally { Pop-Location }
    }
}

if (-not $BackendOnly) {
    Invoke-Step 'Frontend npm ci' {
        Push-Location $webDir
        try { npm ci } finally { Pop-Location }
    }
    Invoke-Step 'Frontend npm run build' {
        Push-Location $webDir
        try { npm run build } finally { Pop-Location }
    }
}

if ($script:failed) {
    Write-Host "`nVERIFY-LOCAL: FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "`nVERIFY-LOCAL: ALL GREEN" -ForegroundColor Green
exit 0
