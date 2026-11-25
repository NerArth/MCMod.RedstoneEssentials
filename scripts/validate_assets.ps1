<#
.SYNOPSIS
    Validates all JSON resources and internal model references under src/main/resources.
.DESCRIPTION
    Run from anywhere: .\scripts\validate_assets.ps1
#>
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$resRoot = Join-Path $projectRoot "src\main\resources"
$assetsRoot = Join-Path $resRoot "assets\redstone_essentials"

# 1. Parse all JSON files in src/main/resources (compatible with Windows PowerShell 5.1 and PowerShell 7+)
$jsonFiles = @(Get-ChildItem -Path $resRoot -Recurse -Filter "*.json" -File)
foreach ($f in $jsonFiles) {
    try {
        $raw = [System.IO.File]::ReadAllText($f.FullName)
        $null = $raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        Write-Error "Invalid JSON in $($f.FullName): $_"
    }
}
Write-Host "Validated $($jsonFiles.Count) JSON files with zero syntax errors."

# 2. Verify any redstone_essentials:(block|item)/... model references in blockstates/ and items/ exist
$refSources = @()
foreach ($subDir in @("blockstates", "items")) {
    $dirPath = Join-Path $assetsRoot $subDir
    if (Test-Path $dirPath) {
        $refSources += Get-ChildItem -Path $dirPath -Filter "*.json" -File
    }
}

$checkedRefs = 0
foreach ($f in $refSources) {
    $raw = [System.IO.File]::ReadAllText($f.FullName)
    $matches = [regex]::Matches($raw, '"redstone_essentials:(block|item)/([^"]+)"')
    foreach ($m in $matches) {
        $kind = $m.Groups[1].Value
        $name = $m.Groups[2].Value
        $modelFile = Join-Path $assetsRoot "models\$kind\$name.json"
        if (-not (Test-Path $modelFile)) {
            Write-Error "Missing model $modelFile referenced by $($f.FullName)"
        }
        $checkedRefs++
    }
}
Write-Host "Verified $checkedRefs model references across $($refSources.Count) blockstate/item definition files."
