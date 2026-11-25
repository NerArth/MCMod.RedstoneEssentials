<#
Runs the Gradle wrapper with a JDK 25 for this session only (system JAVA_HOME / PATH untouched).
Usage: .\gw.ps1 [gradle args]   e.g. .\gw.ps1 build   |   .\gw.ps1 runClient
Override the JDK location with: $env:MOD_JDK25 = "C:\path\to\jdk-25"
#>
$ErrorActionPreference = 'Stop'

function Find-Jdk25 {
    if ($env:MOD_JDK25 -and (Test-Path "$env:MOD_JDK25\bin\java.exe")) { return $env:MOD_JDK25 }
    $roots = "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java", "C:\Program Files\Microsoft"
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $hit = Get-ChildItem $root -Directory -Filter "jdk-25*" |
            Where-Object { Test-Path "$($_.FullName)\bin\java.exe" } |
            Sort-Object Name -Descending | Select-Object -First 1
        if ($hit) { return $hit.FullName }
    }
    return $null
}

$jdk = Find-Jdk25
if (-not $jdk) {
    Write-Error "JDK 25 not found. Install it (winget install EclipseAdoptium.Temurin.25.JDK) or set `$env:MOD_JDK25."
}

$env:JAVA_HOME = $jdk
Write-Host "Using JDK: $jdk"
& "$PSScriptRoot\gradlew.bat" @args
exit $LASTEXITCODE
