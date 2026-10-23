param([string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$settings = Join-Path $ProjectRoot '.vscode\local.env'
if (!(Test-Path -LiteralPath $settings)) { throw 'Create .vscode/local.env with DB_PASSWORD before starting.' }
foreach ($line in Get-Content -LiteralPath $settings) {
    if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim().Trim('"'), 'Process')
    }
}
if (!$env:DB_PASSWORD) { throw 'DB_PASSWORD is missing from .vscode/local.env.' }
$env:WORKBENCH_DEMO_ENABLED = 'true'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:DB_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
$env:DB_USER = 'system'
$env:KAFKA_BOOTSTRAP = 'localhost:9092'
$java = 'C:\jdk25\bin\java.exe'
$node = 'C:\nodejs\node.exe'
if (!(Test-Path -LiteralPath $node)) {
    $node = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
}
foreach ($executable in @($java, $node)) {
    if (!(Test-Path -LiteralPath $executable)) { throw "Runtime not found: $executable" }
}
$services = @(
    @{ Name = 'access-data'; Port = 8081 },
    @{ Name = 'exposure-analytics'; Port = 8082 },
    @{ Name = 'stress-simulation'; Port = 8083 },
    @{ Name = 'risk-governance'; Port = 8084 },
    @{ Name = 'gateway'; Port = 8090 }
)
$listeners = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue)
# Check every occupied port before starting anything; never kill an unrelated service.
foreach ($service in $services) {
    $existing = $listeners | Where-Object LocalPort -eq $service.Port | Select-Object -First 1
    if ($existing) {
        $running = Get-CimInstance Win32_Process -Filter "ProcessId = $($existing.OwningProcess)"
        if (!$running.CommandLine.Contains($ProjectRoot + '\backend\' + $service.Name + '\')) {
            throw "Port $($service.Port) belongs to another project. Stop that instance before starting version 3."
        }
        $service.Running = $true
    }
    $service.Jar = Join-Path $ProjectRoot "backend\$($service.Name)\target\$($service.Name)-0.1.0-SNAPSHOT.jar"
    if (!(Test-Path -LiteralPath $service.Jar)) { throw 'Build the backend first: mvn.cmd -DskipTests package' }
}
$frontendRunning = $listeners | Where-Object LocalPort -eq 5173 | Select-Object -First 1
if ($frontendRunning) {
    $main = (Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:5173/src/main.tsx' -TimeoutSec 5).Content
    if ($main -notmatch [regex]::Escape($ProjectRoot.Replace('\','/'))) {
        throw 'Port 5173 belongs to another frontend. Stop it before starting version 3.'
    }
}
$logs = Join-Path $ProjectRoot '.local-run'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
foreach ($service in $services) {
    if ($service.Running) { Write-Host "$($service.Name) is already running."; continue }
    $env:SERVER_PORT = [string]$service.Port
    $child = Start-Process -FilePath $java -ArgumentList @('-jar', ('"' + $service.Jar + '"')) -WorkingDirectory $ProjectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs "$($service.Name).out.log") -RedirectStandardError (Join-Path $logs "$($service.Name).err.log") -PassThru
    Write-Host "Started $($service.Name), PID $($child.Id), port $($service.Port)."
    if ($service.Name -eq 'access-data') {
        $ready = $false
        for ($attempt=0; $attempt -lt 45; $attempt++) {
            if ($child.HasExited) { throw 'Access & Data stopped. Check .local-run/access-data.out.log.' }
            try {
                $token = Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/auth/csrf' -TimeoutSec 2
                if ($token.token) { $ready=$true; break }
            } catch { }
            Start-Sleep -Seconds 1
            $child.Refresh()
        }
        if (!$ready) { throw 'Access & Data is not ready. Check .local-run/access-data.out.log.' }
    }
}
if (!$frontendRunning) {
    $frontend = Join-Path $ProjectRoot 'frontend'
    $vite = Join-Path $frontend 'node_modules\vite\bin\vite.js'
    if (!(Test-Path -LiteralPath $vite)) { throw 'Frontend dependencies are missing.' }
    $child = Start-Process -FilePath $node -ArgumentList @('"' + $vite + '"', '--host', '127.0.0.1', '--port', '5173', '--strictPort') -WorkingDirectory $frontend -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'frontend.out.log') -RedirectStandardError (Join-Path $logs 'frontend.err.log') -PassThru
    Write-Host "Started version-3 frontend, PID $($child.Id)."
}
Write-Host 'Open http://localhost:5173/ . Startup logs are in .local-run/.'
