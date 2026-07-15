$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot "build.ps1")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$testSrc = Join-Path $root "tests\java"
$testOut = Join-Path $root "out-test"
if (Test-Path -LiteralPath $testOut) { Remove-Item -LiteralPath $testOut -Recurse -Force }
New-Item -ItemType Directory -Path $testOut | Out-Null

$tests = Get-ChildItem -LiteralPath $testSrc -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp (Join-Path $root "out") -d $testOut $tests
if ($LASTEXITCODE -ne 0) { throw "test javac failed with exit code $LASTEXITCODE" }

java -cp ((Join-Path $root "out") + [IO.Path]::PathSeparator + $testOut) com.campuspilot.AllTests
if ($LASTEXITCODE -ne 0) { throw "tests failed with exit code $LASTEXITCODE" }