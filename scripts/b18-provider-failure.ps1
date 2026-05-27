$ErrorActionPreference = "Stop"
$base = if ($env:FOCUSFLOW_BASE_URL) { $env:FOCUSFLOW_BASE_URL } else { "http://localhost:8080" }
$cookie = Join-Path $PSScriptRoot "b18-failure-cookies.txt"
if (Test-Path $cookie) { Remove-Item $cookie }

function Get-CsrfToken {
    param([string]$CookieFile)
    $line = Get-Content $CookieFile | Where-Object { $_ -match 'XSRF-TOKEN' } | Select-Object -First 1
    if (-not $line) { throw "XSRF-TOKEN cookie not found" }
    return ($line -split "`t")[-1]
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [string]$BodyFile = $null,
        [string]$Csrf = $null
    )
    $args = @("-s", "-c", $cookie, "-b", $cookie, "-w", "`nHTTP_STATUS:%{http_code}`n")
    if ($Method -ne "GET") { $args += @("-X", $Method) }
    if ($Csrf) { $args += @("-H", "X-XSRF-TOKEN: $Csrf") }
    if ($BodyFile) { $args += @("-H", "Content-Type: application/json", "-d", "@$BodyFile") }
    $args += "$base$Path"
    return (& curl.exe @args)
}

$suffix = Get-Random
$registerFile = Join-Path $env:TEMP "b18-fail-register-$suffix.json"
$loginFile = Join-Path $env:TEMP "b18-fail-login-$suffix.json"
$taskFile = Join-Path $env:TEMP "b18-fail-task-$suffix.json"
$planFile = Join-Path $env:TEMP "b18-fail-plan-$suffix.json"

@{ email = "b18-fail-$suffix@example.com"; username = "b18fail$suffix"; password = "password123" } | ConvertTo-Json -Compress | Set-Content $registerFile -Encoding ascii -NoNewline
@{ username = "b18fail$suffix"; password = "password123" } | ConvertTo-Json -Compress | Set-Content $loginFile -Encoding ascii -NoNewline
@{ title = "Failure test task"; priority = "HIGH" } | ConvertTo-Json -Compress | Set-Content $taskFile -Encoding ascii -NoNewline
@{ availableMinutes = 60 } | ConvertTo-Json -Compress | Set-Content $planFile -Encoding ascii -NoNewline

Invoke-Api -Method POST -Path "/api/auth/register" -BodyFile $registerFile | Out-Null
$csrf = Get-CsrfToken $cookie
Invoke-Api -Method POST -Path "/api/auth/register" -BodyFile $registerFile -Csrf $csrf | Out-Null
Invoke-Api -Method POST -Path "/api/auth/login" -BodyFile $loginFile -Csrf $csrf | Out-Null
Invoke-Api -Method POST -Path "/api/tasks" -BodyFile $taskFile -Csrf $csrf | Out-Null

Write-Host "=== Plans before failed generation ==="
$before = Invoke-Api -Path "/api/daily-plans"
Write-Host $before

Write-Host "=== Failed plan generation (expect 502) ==="
$failed = Invoke-Api -Method POST -Path "/api/daily-plans/generate" -BodyFile $planFile -Csrf $csrf
Write-Host $failed

Write-Host "=== Plans after failed generation (expect empty) ==="
$after = Invoke-Api -Path "/api/daily-plans"
Write-Host $after
