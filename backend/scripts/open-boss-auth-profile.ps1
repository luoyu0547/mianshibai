param(
    [string]$ProfileRoot = "browser-profiles",
    [int]$KeepAliveMinutes = 30
)

$ErrorActionPreference = "Stop"

$backendRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
Push-Location $backendRoot
try {
    if ([System.IO.Path]::IsPathRooted($ProfileRoot)) {
        $profilePath = $ProfileRoot
    } else {
        $profilePath = Join-Path $backendRoot $ProfileRoot
    }
    $profilePath = [System.IO.Path]::GetFullPath($profilePath)
    $bossProfilePath = Join-Path $profilePath "boss"
    if (-not (Test-Path -LiteralPath $bossProfilePath)) {
        New-Item -ItemType Directory -Path $bossProfilePath | Out-Null
    }

    $chromeCandidates = @(
        "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
        "$env:ProgramFiles(x86)\Google\Chrome\Application\chrome.exe",
        "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
        "$env:ProgramFiles(x86)\Microsoft\Edge\Application\msedge.exe"
    )
    $browserPath = $chromeCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $browserPath) {
        throw "Chrome or Edge executable was not found."
    }

    Write-Host "Opening Boss auth browser profile at: $bossProfilePath"
    Write-Host "Browser executable: $browserPath"
    Write-Host "Login in the opened browser. Keep this profile path unchanged for backend crawling."
    Start-Process -FilePath $browserPath -ArgumentList @(
        "--user-data-dir=$bossProfilePath",
        "--new-window",
        "https://www.zhipin.com/"
    )
    Write-Host "Boss auth profile saved. You can now run Boss crawling with the same profile root."
} finally {
    Pop-Location
}
