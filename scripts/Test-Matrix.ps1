param([ValidateSet('A','B','C','D','E')][string[]]$Profiles = @('A','B','C','D','E'))
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
foreach ($profile in $Profiles) {
    $log = Join-Path $root ('work/matrix-' + $profile + '.log')
    Write-Output ('Starting singleplayer profile ' + $profile)
    & (Join-Path $PSScriptRoot 'Build.ps1') build runProductionGameTest ('-PcompatProfile=' + $profile) > $log 2>&1
    if ($LASTEXITCODE -ne 0) {
        Get-Content -LiteralPath $log -Tail 65
        exit $LASTEXITCODE
    }
    Select-String -LiteralPath $log -Pattern 'TEST .*heat scan|TEST .*PASSED|BUILD SUCCESSFUL' | ForEach-Object { $_.Line }
}
