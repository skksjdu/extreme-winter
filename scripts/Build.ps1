param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$env:GRADLE_USER_HOME = Join-Path $root 'work/gradle-home'
$arguments = @('--console=plain', '--no-daemon')
Push-Location $root
try {
    & (Join-Path $root 'gradlew.bat') @arguments @Tasks
    $result = $LASTEXITCODE
} finally { Pop-Location }
exit $result
