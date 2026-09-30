param([string]$MinecraftVersion = '26.1')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $root "work/compat/$MinecraftVersion"
$headers = @{'User-Agent' = 'ExtremeWinter-compat-tests/26.0.0'}
$records = [System.Collections.Generic.List[object]]::new()

function Find-Version([string]$Project) {
    $games = [uri]::EscapeDataString('["' + $MinecraftVersion + '"]')
    $versions = Invoke-RestMethod "https://api.modrinth.com/v2/project/$Project/version?game_versions=$games&loaders=%5B%22fabric%22%5D" -Headers $headers
    $version = $versions | Where-Object version_type -eq 'release' | Select-Object -First 1
    if (!$version) { throw "No stable Fabric release of $Project supports $MinecraftVersion" }
    return $version
}

function Save-Version([string]$Project, $Version) {
    if ($MinecraftVersion -notin $Version.game_versions) { throw "$Project does not support $MinecraftVersion" }
    $file = $Version.files | Where-Object primary | Select-Object -First 1
    $directory = Join-Path $destination $Project
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $path = Join-Path $directory $file.filename
    if (!(Test-Path -LiteralPath $path)) { Invoke-WebRequest $file.url -Headers $headers -OutFile $path }
    $hash = (Get-FileHash -LiteralPath $path -Algorithm SHA512).Hash.ToLowerInvariant()
    if ($hash -ne $file.hashes.sha512) { throw "SHA512 mismatch: $path" }
    $records.Add([pscustomobject]@{project=$Project; version=$Version.version_number; id=$Version.id;
        file=$path; url=$file.url; sha512=$hash; game_versions=$Version.game_versions; dependencies=$Version.dependencies})
    Write-Output "$Project $($Version.version_number): verified"
}

# Newer Iris releases list 26.1 but require Sodium built only for 26.1.2.
# Pin the pair whose actual dependency also explicitly supports the exact target.
$iris = Invoke-RestMethod 'https://api.modrinth.com/v2/version/MwcLS51S' -Headers $headers
$sodiumId = ($iris.dependencies | Where-Object { $_.project_id -eq 'AANobbMI' -and $_.dependency_type -eq 'required' }).version_id
$sodium = if ($sodiumId) { Invoke-RestMethod "https://api.modrinth.com/v2/version/$sodiumId" -Headers $headers } else { Find-Version 'sodium' }
Save-Version 'sodium' $sodium
Save-Version 'iris' $iris
foreach ($project in @('lithium', 'ferrite-core', 'modmenu', 'cloth-config')) { Save-Version $project (Find-Version $project) }
$records | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $destination 'versions.json') -Encoding utf8
$games = [uri]::EscapeDataString('["' + $MinecraftVersion + '"]')
$shaders = Invoke-RestMethod "https://api.modrinth.com/v2/project/complementary-reimagined/version?game_versions=$games" -Headers $headers
$shader = $shaders | Where-Object version_type -eq 'release' | Select-Object -First 1
if (!$shader) { throw 'No compatible Complementary release' }
Save-Version 'complementary-reimagined' $shader
$shader | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $destination 'complementary-reimagined/version.json') -Encoding utf8
$records | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $destination 'versions.json') -Encoding utf8
