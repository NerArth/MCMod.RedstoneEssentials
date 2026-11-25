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
$dataRoot = Join-Path $resRoot "data\redstone_essentials"

$allBlocks = @(
    "tuned_redstone_crystal",
    "redstone_variode",
    "redstone_capacitor"
)

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

# 2. Check every block has blockstate, item definition, loot table, recipe, and lang entry
$langPath = Join-Path $assetsRoot "lang\en_us.json"
$lang = [System.IO.File]::ReadAllText($langPath) | ConvertFrom-Json -ErrorAction Stop
foreach ($b in $allBlocks) {
    if (-not (Test-Path (Join-Path $assetsRoot "blockstates\$b.json"))) { Write-Error "Missing blockstate for $b" }
    if (-not (Test-Path (Join-Path $assetsRoot "items\$b.json"))) { Write-Error "Missing item definition for $b" }
    if (-not (Test-Path (Join-Path $dataRoot "loot_table\blocks\$b.json"))) { Write-Error "Missing loot table for $b" }
    if (-not (Test-Path (Join-Path $dataRoot "recipe\$b.json"))) { Write-Error "Missing recipe for $b" }
    $langKey = "block.redstone_essentials.$b"
    if (-not ($lang.PSObject.Properties.Name -contains $langKey)) { Write-Error "Missing lang key $langKey" }
}
if (-not ($lang.PSObject.Properties.Name -contains "itemGroup.redstone_essentials.redstone_essentials")) {
    Write-Error "Missing lang key itemGroup.redstone_essentials.redstone_essentials"
}
Write-Host "All $($allBlocks.Count) blocks verified for blockstates, item definitions, loot tables, recipes, and translations."

# 3. Verify all redstone_essentials:(block|item)/... model & parent references exist
$refSources = @()
foreach ($subDir in @("blockstates", "items", "models\block", "models\item")) {
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
Write-Host "Verified $checkedRefs model/parent references across $($refSources.Count) JSON files."
