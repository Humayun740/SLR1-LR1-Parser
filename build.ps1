param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

$Grammar = "input/grammar2.txt"
$Input   = "input/input_valid.txt"
$OutDir  = "output"

switch ($CliArgs.Count) {
    0 { }
    1 { $Grammar = $CliArgs[0] }
    2 {
        $Grammar = $CliArgs[0]
        if ($CliArgs[1].ToLower().EndsWith(".txt")) { $Input = $CliArgs[1] }
        else                                        { $OutDir = $CliArgs[1] }
    }
    default {
        $Grammar = $CliArgs[0]
        $Input   = $CliArgs[1]
        $OutDir  = $CliArgs[2]
    }
}

if (-not (Test-Path -Path $Grammar -PathType Leaf)) {
    throw "Grammar file not found: $Grammar"
}

if (-not (Test-Path -Path $Input -PathType Leaf)) {
    throw "Input file not found: $Input"
}

Write-Host "Compiling Java sources in src/ ..."
Push-Location src
javac *.java
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Compilation failed (exit $LASTEXITCODE)" }
Pop-Location

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "Running SLR(1) + LR(1) parsers ..."
java -cp src Main $Grammar $Input $OutDir
if ($LASTEXITCODE -ne 0) { throw "Parser failed (exit $LASTEXITCODE)" }

Write-Host "Done. Output written to $OutDir"
