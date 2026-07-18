$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root "out"
$defaultStaticRoot = Resolve-Path -LiteralPath (Join-Path $root "..\campuspilot-home")
$localConfig = Join-Path $root "config\application.local.ps1"

if (Test-Path -LiteralPath $localConfig) {
    . $localConfig
    Write-Host "Loaded local configuration: $localConfig"
}

if (-not (Test-Path -LiteralPath $out)) {
    & (Join-Path $PSScriptRoot "build.ps1")
}

if (-not $env:CAMPUSPILOT_STATIC_ROOT) {
    $env:CAMPUSPILOT_STATIC_ROOT = $defaultStaticRoot.Path
}
if (-not $env:CAMPUSPILOT_HOST) {
    $env:CAMPUSPILOT_HOST = "0.0.0.0"
}
if (-not $env:CAMPUSPILOT_PORT) {
    $env:CAMPUSPILOT_PORT = "8787"
}

function Resolve-JavaTool($name) {
    $fromPath = Get-Command $name -ErrorAction SilentlyContinue
    if ($fromPath) { return $fromPath.Source }

    if ($env:JAVA_HOME) {
        $fromJavaHome = Join-Path $env:JAVA_HOME "bin\$name.exe"
        if (Test-Path -LiteralPath $fromJavaHome) { return $fromJavaHome }
    }

    $ideaJbr = "D:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr\bin\$name.exe"
    if (Test-Path -LiteralPath $ideaJbr) { return $ideaJbr }

    throw "$name not found. Install JDK 21 or set JAVA_HOME."
}

$java = Resolve-JavaTool "java"
& $java -cp $out com.campuspilot.CampusPilotApplication
