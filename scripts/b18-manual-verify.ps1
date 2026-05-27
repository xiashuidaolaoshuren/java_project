$ErrorActionPreference = "Stop"
$base = if ($env:FOCUSFLOW_BASE_URL) { $env:FOCUSFLOW_BASE_URL } else { "http://localhost:8080" }
$cookie = Join-Path $PSScriptRoot "b18-cookies.txt"
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
$registerFile = Join-Path $env:TEMP "b18-register-$suffix.json"
$loginFile = Join-Path $env:TEMP "b18-login-$suffix.json"
$taskFile = Join-Path $env:TEMP "b18-task-$suffix.json"
$updateFile = Join-Path $env:TEMP "b18-task-update-$suffix.json"
$planFile = Join-Path $env:TEMP "b18-plan-$suffix.json"

@{ email = "b18-$suffix@example.com"; username = "b18user$suffix"; password = "password123" } | ConvertTo-Json -Compress | Set-Content $registerFile -Encoding ascii -NoNewline
@{ username = "b18user$suffix"; password = "password123" } | ConvertTo-Json -Compress | Set-Content $loginFile -Encoding ascii -NoNewline
@{ title = "Write README"; description = "B18 verification"; priority = "HIGH"; dueDate = "2026-06-01"; estimatedMinutes = 60 } | ConvertTo-Json -Compress | Set-Content $taskFile -Encoding ascii -NoNewline
@{ title = "Write README updated"; description = "B18"; priority = "MEDIUM"; dueDate = "2026-06-02"; estimatedMinutes = 45 } | ConvertTo-Json -Compress | Set-Content $updateFile -Encoding ascii -NoNewline
@{ availableMinutes = 120; planDate = "2026-06-01" } | ConvertTo-Json -Compress | Set-Content $planFile -Encoding ascii -NoNewline

Write-Host "=== CSRF bootstrap ==="
Invoke-Api -Method POST -Path "/api/auth/register" -BodyFile $registerFile | Out-Null
$csrf = Get-CsrfToken $cookie

Write-Host "=== Auth ==="
Write-Host (Invoke-Api -Method POST -Path "/api/auth/register" -BodyFile $registerFile -Csrf $csrf)
Write-Host (Invoke-Api -Method POST -Path "/api/auth/login" -BodyFile $loginFile -Csrf $csrf)
Write-Host (Invoke-Api -Path "/api/auth/me")

Write-Host "=== Tasks ==="
$taskCreate = Invoke-Api -Method POST -Path "/api/tasks" -BodyFile $taskFile -Csrf $csrf
Write-Host $taskCreate
$taskId = [regex]::Match($taskCreate, '"id":(\d+)').Groups[1].Value
Write-Host (Invoke-Api -Path "/api/tasks")
Write-Host (Invoke-Api -Path "/api/tasks/$taskId")
Write-Host (Invoke-Api -Method PUT -Path "/api/tasks/$taskId" -BodyFile $updateFile -Csrf $csrf)

Write-Host "=== Daily plans (before generate) ==="
Write-Host (Invoke-Api -Path "/api/daily-plans")

Write-Host "=== Daily plan generate ==="
$planCreate = Invoke-Api -Method POST -Path "/api/daily-plans/generate" -BodyFile $planFile -Csrf $csrf
Write-Host $planCreate
$planId = [regex]::Match($planCreate, '"id":(\d+)').Groups[1].Value
Write-Host (Invoke-Api -Path "/api/daily-plans")
if ($planId) { Write-Host (Invoke-Api -Path "/api/daily-plans/$planId") }

Write-Host "=== Task delete ==="
Write-Host (Invoke-Api -Method DELETE -Path "/api/tasks/$taskId" -Csrf $csrf)
