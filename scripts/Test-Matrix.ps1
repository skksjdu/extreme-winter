param([ValidateSet('A','B','C','D','E')][string[]]$Profiles = @('A','B','C','D','E'))
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$properties = Get-Content (Join-Path $root 'gradle.properties')
$minecraftVersion = ($properties | Where-Object { $_ -like 'minecraft_version=*' }).Split('=')[1]
$modVersion = ($properties | Where-Object { $_ -like 'mod_version=*' }).Split('=')[1]
foreach ($profile in $Profiles) {
    $run = Join-Path $root "work/run-production-$minecraftVersion-$profile"
    $config = Join-Path $run 'config'
    New-Item -ItemType Directory -Path $config -Force | Out-Null
    $shaderName = ''
    if ($profile -eq 'D') {
        $shader = Get-ChildItem (Join-Path $root "work/compat/$minecraftVersion/complementary-reimagined") -Filter '*.zip' | Select-Object -First 1
        if (!$shader) { throw 'Run scripts/Prepare-Compat.ps1 before the shader profile' }
        $shaderpacks = Join-Path $run 'shaderpacks'
        New-Item -ItemType Directory -Path $shaderpacks -Force | Out-Null
        Copy-Item -LiteralPath $shader.FullName -Destination $shaderpacks
        $shaderName = $shader.Name
    }
    $enabled = if ($profile -eq 'D') { 'true' } else { 'false' }
    @("enableShaders=$enabled", "shaderPack=$shaderName", 'maxShadowRenderDistance=32') | Set-Content (Join-Path $config 'iris.properties') -Encoding ascii
    $log = Join-Path $root ("work/test-$modVersion-$profile.log")
    if (Test-Path -LiteralPath $log) {
        Copy-Item -LiteralPath $log -Destination ($log + '.' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff') + '.previous')
    }
    Write-Output ('Starting singleplayer profile ' + $profile)
    & (Join-Path $PSScriptRoot 'Build.ps1') build runProductionGameTest ('-PcompatProfile=' + $profile) > $log 2>&1
    if ($LASTEXITCODE -ne 0) {
        Get-Content -LiteralPath $log -Tail 65
        exit $LASTEXITCODE
    }
    Select-String -LiteralPath $log -Pattern 'TEST .*heat scan|TEST .*PASSED|BUILD SUCCESSFUL' | ForEach-Object { $_.Line }
}
