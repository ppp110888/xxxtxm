$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot 'backend'
$frontendDir = Join-Path $projectRoot 'frontend'

if (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue) {
    throw 'Port 8080 is already in use.'
}
if (Get-NetTCPConnection -State Listen -LocalPort 5173 -ErrorAction SilentlyContinue) {
    throw 'Port 5173 is already in use.'
}

Start-Process -FilePath 'mvn.cmd' `
    -ArgumentList @('spring-boot:run', '-Dspring-boot.run.profiles=dev') `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput (Join-Path $projectRoot 'backend-dev.out.log') `
    -RedirectStandardError (Join-Path $projectRoot 'backend-dev.err.log')

Start-Process -FilePath 'pnpm.cmd' `
    -ArgumentList @('dev') `
    -WorkingDirectory $frontendDir `
    -RedirectStandardOutput (Join-Path $projectRoot 'frontend-dev.out.log') `
    -RedirectStandardError (Join-Path $projectRoot 'frontend-dev.err.log')

Write-Host 'CodeMate is starting:'
Write-Host '  Frontend: http://localhost:5173'
Write-Host '  API docs: http://localhost:8080/doc.html'
