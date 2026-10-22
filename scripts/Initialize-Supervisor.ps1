# One-time local setup using the application's existing guarded bootstrap runner.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$settings = Join-Path $projectRoot '.vscode\local.env'
if (!(Test-Path -LiteralPath $settings)) { throw 'Missing .vscode/local.env with DB_PASSWORD.' }
$username = (Read-Host 'Choose the first Supervisor username').Trim().ToLowerInvariant()
if ($username -notmatch '^[a-z][a-z0-9._-]{2,63}$') {
    throw 'Username: 3-64 characters, starting with a letter; use letters, digits, dots, underscores or hyphens.'
}
$securePassword = Read-Host 'Choose a password (12 or more characters)' -AsSecureString
$confirmation = Read-Host 'Confirm the password' -AsSecureString
$passwordPointer = [IntPtr]::Zero
$confirmationPointer = [IntPtr]::Zero
$prior = @{}
$names = @('DB_PASSWORD','DB_USER','DB_URL','SERVER_ADDRESS','SERVER_PORT','KAFKA_BOOTSTRAP','WORKBENCH_DEMO_ENABLED','WORKBENCH_BOOTSTRAP_ENABLED','WORKBENCH_BOOTSTRAP_USERNAME','WORKBENCH_BOOTSTRAP_PASSWORD')
foreach ($name in $names) { $prior[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $confirmationPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirmation)
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $confirmed = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($confirmationPointer)
    if ($password -cne $confirmed) { throw 'Passwords do not match.' }
    if ($password.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($password) -gt 72 -or $password -match '\p{Cc}' -or [string]::IsNullOrWhiteSpace($password)) {
        throw 'Password must have at least 12 characters, at most 72 UTF-8 bytes, and no control characters.'
    }
    foreach ($line in Get-Content -LiteralPath $settings) {
        if ($line -match '^(DB_PASSWORD|DB_USER|DB_URL)=(.*)$') {
            [Environment]::SetEnvironmentVariable($matches[1],$matches[2].Trim().Trim('"'),'Process')
        }
    }
    if (!$env:DB_PASSWORD) { throw 'DB_PASSWORD is missing from .vscode/local.env.' }
    $java = 'C:\jdk25\bin\java.exe'
    $jar = Join-Path $projectRoot 'backend\access-data\target\access-data-0.1.0-SNAPSHOT.jar'
    if (!(Test-Path -LiteralPath $java) -or !(Test-Path -LiteralPath $jar)) { throw 'Build the backend before initial account setup.' }
    $existing = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($existing) {
        $running = Get-CimInstance Win32_Process -Filter "ProcessId = $($existing.OwningProcess)"
        if (!$running.CommandLine.Contains($jar)) { throw 'Port 8081 belongs to another launch. Stop Access & Data in VS Code before running setup.' }
        Stop-Process -Id $existing.OwningProcess
        Start-Sleep -Seconds 2
    }
    $env:DB_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
    $env:DB_USER = 'system'
    $env:SERVER_ADDRESS = '127.0.0.1'
    $env:SERVER_PORT = '8081'
    $env:KAFKA_BOOTSTRAP = 'localhost:9092'
    $env:WORKBENCH_DEMO_ENABLED = 'true'
    $env:WORKBENCH_BOOTSTRAP_ENABLED = 'true'
    $env:WORKBENCH_BOOTSTRAP_USERNAME = $username
    $env:WORKBENCH_BOOTSTRAP_PASSWORD = $password
    $logs = Join-Path $projectRoot '.local-run'
    New-Item -ItemType Directory -Path $logs -Force | Out-Null
    $out = Join-Path $logs 'access-data.out.log'
    $child = Start-Process -FilePath $java -ArgumentList @('-jar',('"'+$jar+'"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError (Join-Path $logs 'access-data.err.log') -PassThru
    $complete = $false
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        if (Test-Path -LiteralPath $out) {
            $content = Get-Content -LiteralPath $out -Raw
            if ($content -match 'Initial Supervisor account created|Initial Supervisor already exists') { $complete=$true; break }
        }
        $child.Refresh()
        if ($child.HasExited) { throw 'Account setup did not complete. Check .local-run/access-data.out.log. Existing accounts are never overwritten.' }
        Start-Sleep -Seconds 1
    }
    if (!$complete) { throw 'Account setup is still pending. Check .local-run/access-data.out.log.' }
    Write-Host "Supervisor setup complete for '$username'. Existing account passwords, if any, were preserved."
    Write-Host 'Open http://localhost:5173/, choose Supervisor Login, and use your credentials.'
    Write-Host 'After signing in, use User Management to create Analyst and Contributor accounts.'
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$prior[$name],'Process') }
    if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
    if ($confirmationPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($confirmationPointer) }
    $password = $null
    $confirmed = $null
    $securePassword.Dispose()
    $confirmation.Dispose()
}
