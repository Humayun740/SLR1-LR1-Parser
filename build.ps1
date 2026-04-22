param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

$ErrorActionPreference = "Stop"

$Grammar = "input/grammar1.txt"
$Input = "input/input_valid.txt"
$OutDir = "output"

switch ($CliArgs.Count) {
    0 { }
    1 {
        $Grammar = $CliArgs[0]
    }
    2 {
        $Grammar = $CliArgs[0]
        if ($CliArgs[1].ToLower().EndsWith(".txt")) {
            $Input = $CliArgs[1]
        } else {
            $OutDir = $CliArgs[1]
        }
    }
    default {
        $Grammar = $CliArgs[0]
        $Input = $CliArgs[1]
        $OutDir = $CliArgs[2]
    }
}

if (-not (Test-Path -Path $Grammar -PathType Leaf)) {
    throw "Grammar file not found: $Grammar"
}

if (-not (Test-Path -Path $Input -PathType Leaf)) {
    throw "Input file not found: $Input"
}

Write-Host "Compiling Java sources..."
javac src/*.java
if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed with exit code $LASTEXITCODE"
}

Write-Host "Running SLR parser..."
java -cp src Main $Grammar $Input $OutDir
if ($LASTEXITCODE -ne 0) {
    throw "SLR parser failed with exit code $LASTEXITCODE"
}

Write-Host "Done. Output written to $OutDir"
