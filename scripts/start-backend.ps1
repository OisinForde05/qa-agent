param(
    [switch]$SkipAi
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $repoRoot "backend"

$jdk21 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $jdk21) {
    throw "Java 21 was not found in C:\Program Files\Eclipse Adoptium."
}

$env:JAVA_HOME = $jdk21.FullName
$javaBin = Join-Path $env:JAVA_HOME "bin"

if (($env:Path -split ";") -notcontains $javaBin) {
    $env:Path = "$javaBin;$env:Path"
}

$env:DATABASE_HOST = "localhost"
$env:DATABASE_PORT = "5432"
$env:DATABASE_NAME = "qa_agent"
$env:DATABASE_USERNAME = "qa_agent_app"

if ([string]::IsNullOrEmpty($env:DATABASE_PASSWORD)) {
    $dbPassword = Read-Host "Password for qa_agent_app" -AsSecureString
    $env:DATABASE_PASSWORD = [System.Net.NetworkCredential]::new("", $dbPassword).Password
}

if ([string]::IsNullOrEmpty($env:DATABASE_PASSWORD)) {
    throw "A database password is required."
}

if (-not $SkipAi) {
    if ([string]::IsNullOrWhiteSpace($env:GEMINI_MODEL)) {
        $env:GEMINI_MODEL = (Read-Host "Your working Gemini model ID").Trim()
    }

    if ([string]::IsNullOrWhiteSpace($env:GEMINI_MODEL)) {
        throw "A Gemini model ID is required, or use -SkipAi."
    }

    if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
        $geminiKey = Read-Host "Gemini API key" -AsSecureString
        $env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new("", $geminiKey).Password
    }

    if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
        throw "A Gemini API key is required, or use -SkipAi."
    }
}

Push-Location $backendPath

try {
    Write-Host "Starting QA-Agent backend with Java 21..."
    & .\mvnw.cmd spring-boot:run

    if ($LASTEXITCODE -ne 0) {
        throw "Backend startup failed. Check the error messages above."
    }
}
finally {
    Pop-Location
}