$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "src\main\java"
$out = Join-Path $root "out"

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

$javac = Resolve-JavaTool "javac"

if (Test-Path -LiteralPath $out) {
    Remove-Item -LiteralPath $out -Recurse -Force
}
New-Item -ItemType Directory -Path $out | Out-Null

$sources = Get-ChildItem -LiteralPath $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found under $src"
}

& $javac -encoding UTF-8 -d $out $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}
Write-Host "Build OK: $out"
