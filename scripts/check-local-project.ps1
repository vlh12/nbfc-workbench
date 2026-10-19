[CmdletBinding()]
param([switch]$Build)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = Split-Path -Parent $PSScriptRoot
$originalLocation = Get-Location
$failureCount = 0

function Assert-Tool {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command is unavailable: $Name. Install it and reopen VS Code."
    }
}

try {
    Set-Location -LiteralPath $projectRoot
    foreach ($toolName in @('java','mvn.cmd','node','npm.cmd')) { Assert-Tool $toolName }
    Write-Host 'Installed tool versions:'
    & java -version
    if ($LASTEXITCODE -ne 0) { throw 'Java version check failed.' }
    & mvn.cmd -version
    if ($LASTEXITCODE -ne 0) { throw 'Maven version check failed.' }
    & node --version
    if ($LASTEXITCODE -ne 0) { throw 'Node version check failed.' }
    & npm.cmd --version
    if ($LASTEXITCODE -ne 0) { throw 'npm version check failed.' }

    if ($Build) {
        Write-Host 'Running backend build and unit tests...'
        & mvn.cmd clean install
        if ($LASTEXITCODE -ne 0) { throw 'Backend build/tests failed. Read the first error above.' }
        Set-Location -LiteralPath (Join-Path $projectRoot 'frontend')
        if (Test-Path -LiteralPath 'package-lock.json') { & npm.cmd ci }
        else { & npm.cmd install }
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
        & npm.cmd run typecheck
        if ($LASTEXITCODE -ne 0) { throw 'Frontend type checking failed.' }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
        Set-Location -LiteralPath $projectRoot
    }

    foreach ($portNumber in @(8081,8082,8090)) {
        $healthUrl = "http://localhost:$portNumber/actuator/health"
        try {
            $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 10
            if ($health.status -ne 'UP') { throw "Health status: $($health.status)" }
            Write-Host "PASS: $healthUrl reports UP"
        } catch {
            $failureCount++
            Write-Warning "FAIL: $healthUrl - $($_.Exception.Message)"
        }
    }
    try {
        $frontend = Invoke-WebRequest -Uri 'http://localhost:5173/' -UseBasicParsing -TimeoutSec 10
        if ($frontend.StatusCode -ne 200) { throw "HTTP status: $($frontend.StatusCode)" }
        Write-Host 'PASS: frontend responds on port 5173'
    } catch {
        $failureCount++
        Write-Warning "FAIL: frontend - $($_.Exception.Message)"
    }
    Write-Host 'These checks cover tool availability, optional builds and service health. Client acceptance and database/event workflow verification remain separate.'
    Write-Host 'Current release status: not ready for client delivery. See client-readiness.json.'
    if ($failureCount -gt 0) { throw "$failureCount service checks failed." }
} finally {
    Set-Location -LiteralPath $originalLocation.Path
}

