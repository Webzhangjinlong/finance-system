# db-migrate.ps1 - Flyway migration wrapper (built by Gate 5)
# Usage:
#   .\tools\db-migrate.ps1 -Action migrate [-DbName finance-system] [-User postgres] [-Password postgres]
#   .\tools\db-migrate.ps1 -Action repair ...
#   .\tools\db-migrate.ps1 -Action validate ...
#   .\tools\db-migrate.ps1 -Action info ...
#
# Notes:
#   - Internals: installs sibling modules first (needed for flyway plugin resolution),
#     then runs flyway goal via cmd /c to avoid PowerShell native-arg quoting issues (lessons L08/L15).
#   - Credentials are passed as command-line props (never written to files/repo).

param(
    [ValidateSet('migrate', 'repair', 'validate', 'info')]
    [string]$Action = 'migrate',
    [string]$DbName = 'finance-system',
    [string]$User = 'postgres',
    [string]$Password = 'postgres',
    [string]$DbHost = 'localhost',
    [string]$DbPort = '5432',
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot          # D:\codex\finance
$serverDir = Join-Path $repoRoot 'finance-server'
$locations = 'filesystem:finance-admin/src/main/resources/db/migration'
$url = "jdbc:postgresql://$DbHost`:$DbPort/$DbName"

Write-Host "[db-migrate] Action=$Action Db=$DbName ($DbHost`:$DbPort)"

if (-not $SkipInstall) {
    Write-Host "[db-migrate] Installing sibling modules..."
    Push-Location $serverDir
    try {
        & mvn -q -pl finance-admin -am install -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "mvn install failed (exit $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }
}

Push-Location $serverDir
try {
    $mvnCmd = "mvn -pl finance-admin flyway:$Action -Dflyway.url=$url -Dflyway.user=$User -Dflyway.password=$Password -Dflyway.locations=$locations"
    Write-Host "[db-migrate] $mvnCmd"
    cmd /c $mvnCmd
    if ($LASTEXITCODE -ne 0) { throw "flyway:$Action failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}

Write-Host "[db-migrate] OK - flyway:$Action completed"
