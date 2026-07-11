$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root "out"
$defaultStaticRoot = Resolve-Path -LiteralPath (Join-Path $root "..\campuspilot-home")

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

java -cp $out com.campuspilot.CampusPilotApplication
