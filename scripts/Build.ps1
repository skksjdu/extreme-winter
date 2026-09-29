param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$env:GRADLE_USER_HOME = Join-Path $root 'work/gradle-home'
$jdk = Get-ChildItem (Join-Path $root 'work/tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
$arguments = @('--console=plain')
if ($jdk) { $arguments += "-Porg.gradle.java.installations.paths=$($jdk.FullName)" }
Push-Location $root
try {
    & (Join-Path $root 'gradlew.bat') @arguments @Tasks
    $result = $LASTEXITCODE
} finally { Pop-Location }
exit $result
