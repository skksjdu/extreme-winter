param([ValidateSet('benchmark','survival','legacy-create','legacy-upgrade')][string]$Mode='benchmark',[int]$Attempt=1,[switch]$SkipClock)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & python tools/prepare_extended_tests.py --mode $Mode --attempt $Attempt
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $taskArguments=@('build','compileGametestJava','runWinterExtended','-I','work/e-validation/review.gradle',"-PwinterExtended=$Mode","-PwinterAttempt=$Attempt",'-PcompatProfile=A')
    if ($SkipClock) { $taskArguments += '-PwinterSkipClock=true' }
    & (Join-Path $PSScriptRoot 'Build.ps1') -Tasks $taskArguments
    $code = $LASTEXITCODE
} finally { Pop-Location }
exit $code
