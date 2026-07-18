$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "build.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$src = Join-Path $root "src\main\java"
$testSrc = Join-Path $root "tests\java"
$testOut = Join-Path $root "out-test"
if (Test-Path -LiteralPath $testOut) { Remove-Item -LiteralPath $testOut -Recurse -Force }
New-Item -ItemType Directory -Path $testOut | Out-Null

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
$java = Resolve-JavaTool "java"

$mainSources = Get-ChildItem -LiteralPath $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$tests = Get-ChildItem -LiteralPath $testSrc -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d $testOut ($mainSources + $tests)
if ($LASTEXITCODE -ne 0) { throw "test javac failed with exit code $LASTEXITCODE" }

& $java -cp $testOut com.campuspilot.AllTests
if ($LASTEXITCODE -ne 0) { throw "tests failed with exit code $LASTEXITCODE" }
