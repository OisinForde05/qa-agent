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
    if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
        $geminiKey = Read-Host "Gemini API key" -AsSecureString
        $env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new("", $geminiKey).Password
    }

    if ([string]::IsNullOrWhiteSpace($env:GEMINI_API_KEY)) {
        throw "A Gemini API key is required, or use -SkipAi."
    }

    $models = @()

    try {
        Write-Host "Loading Gemini models..."

        $headers = @{ "x-goog-api-key" = $env:GEMINI_API_KEY }
        $allModels = @()
        $pageToken = ""

        do {
            $uri = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000"

            if ($pageToken) {
                $uri += "&pageToken=" + [Uri]::EscapeDataString($pageToken)
            }

            $response = Invoke-RestMethod -Uri $uri -Headers $headers -TimeoutSec 20
            $allModels += $response.models
            $pageToken = $response.nextPageToken
        } while ($pageToken)

        $preferredModelIds = @(
    "gemini-3.5-flash-lite"
    "gemini-3.6-flash"
    "gemini-3.8-flash"
)

    $models = @(
        $allModels |
            Where-Object {
                $modelId = $_.name -replace "^models/", ""

                $preferredModelIds -contains $modelId -and
                $_.supportedGenerationMethods -contains "generateContent"
            } |
            Sort-Object name -Unique
    )
    }
    catch {
        Write-Warning "Could not load the model list. You can enter a model ID manually."
    }

    Write-Host ""
    Write-Host "Select a Gemini model"

    for ($i = 0; $i -lt $models.Count; $i++) {
        $modelId = $models[$i].name -replace "^models/", ""
        Write-Host "$($i + 1). $modelId"
    }

    Write-Host "0. Enter a model ID manually"

    $selectedModel = ""

    while ([string]::IsNullOrWhiteSpace($selectedModel)) {
        $choice = Read-Host "Enter a number"
        $selection = 0

        if (-not [int]::TryParse($choice, [ref]$selection)) {
            Write-Host "Enter a valid number."
            continue
        }

        if ($selection -eq 0) {
            $selectedModel = (Read-Host "Gemini model ID").Trim()

            if ([string]::IsNullOrWhiteSpace($selectedModel)) {
                Write-Host "The model ID cannot be empty."
            }
        }
        elseif ($selection -ge 1 -and $selection -le $models.Count) {
            $selectedModel = $models[$selection - 1].name -replace "^models/", ""
        }
        else {
            Write-Host "Choose a number from the menu."
        }
    }

    $env:GEMINI_MODEL = $selectedModel
    Write-Host "Selected model: $env:GEMINI_MODEL"
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