param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputDirectory = "dist",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$pomPath = Join-Path $projectRoot "pom.xml"
[xml]$pom = Get-Content -LiteralPath $pomPath
$appVersion = $pom.project.version
$jarName = "myproxy-$appVersion.jar"
$jarPath = Join-Path $projectRoot "target\$jarName"
$outputPath = Join-Path $projectRoot $OutputDirectory
$runtimePath = Join-Path $projectRoot "target\jpackage-runtime"
$inputPath = Join-Path $projectRoot "target\jpackage-input"
$iconPath = Join-Path $projectRoot "src\main\resources\icons\MyProxy.ico"

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw "JAVA_HOME is not set. Install a JDK 21 or newer, or pass -JavaHome."
}

$java = Join-Path $JavaHome "bin\java.exe"
$jdeps = Join-Path $JavaHome "bin\jdeps.exe"
$jlink = Join-Path $JavaHome "bin\jlink.exe"
$jpackage = Join-Path $JavaHome "bin\jpackage.exe"
foreach ($tool in @($java, $jdeps, $jlink, $jpackage)) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) {
        throw "Required JDK tool was not found: $tool"
    }
}

$javaVersionOutput = & $java --version
if ($LASTEXITCODE -ne 0 -or ($javaVersionOutput -join "`n") -notmatch '(?m)^[^\r\n]*?\b(?<major>\d+)(?:\.\d+)') {
    throw "Unable to determine the JDK version under $JavaHome."
}
if ([int]$Matches.major -lt 21) {
    throw "JDK 21 or newer is required. Found JDK $($Matches.major) under $JavaHome."
}

$maven = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
if ($null -eq $maven) {
    $maven = Get-Command "mvn" -ErrorAction SilentlyContinue
}
if ($null -eq $maven) {
    throw "Maven was not found on PATH."
}

# Maven must use the same JDK that supplies jdeps, jlink, and jpackage.
$env:JAVA_HOME = $JavaHome
$env:Path = "$(Join-Path $JavaHome 'bin');$env:Path"

$mavenArguments = @("clean", "package")
if ($SkipTests) {
    $mavenArguments += "-DskipTests"
}
& $maven.Source @mavenArguments
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE."
}
if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Executable jar was not created: $jarPath"
}

New-Item -ItemType Directory -Path $inputPath | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputPath $jarName)
if (-not (Test-Path -LiteralPath $iconPath -PathType Leaf)) {
    throw "Application icon was not found: $iconPath. Run: java tools/IconGenerator.java"
}

$moduleOutput = & $jdeps --ignore-missing-deps --multi-release 21 --print-module-deps $jarPath 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "jdeps failed:`n$($moduleOutput -join "`n")"
}
$detectedModules = ($moduleOutput | Select-Object -Last 1).Trim()
if ([string]::IsNullOrWhiteSpace($detectedModules)) {
    throw "jdeps did not report any Java modules."
}
$modules = (@($detectedModules.Split(',')) + @("jdk.crypto.ec", "jdk.localedata") |
        Sort-Object -Unique) -join ','

if (Test-Path -LiteralPath $runtimePath) {
    Remove-Item -LiteralPath $runtimePath -Recurse -Force
}
& $jlink --add-modules $modules --strip-debug --no-header-files `
    --no-man-pages --compress=2 --output $runtimePath
if ($LASTEXITCODE -ne 0) {
    throw "jlink failed with exit code $LASTEXITCODE."
}

if (Test-Path -LiteralPath $outputPath) {
    Remove-Item -LiteralPath $outputPath -Recurse -Force
}
New-Item -ItemType Directory -Path $outputPath | Out-Null

# Build the Windows installer with jpackage.
# --win-per-user-install: install to %LocalAppData% (no admin rights needed),
#   so the app can self-update by writing new jars into the app directory.
# --win-upgrade-uuid: stable product identifier so new versions can overwrite
#   existing installs (without this, same-version re-install silently exits).
# --win-dir-chooser is intentionally omitted: if the user picks a directory
#   they lack write permission for (e.g. C:\Program Files), the auto-updater
#   cannot download/replace jars, so installation must stay in the fixed
#   per-user location.
& $jpackage --type exe --name "MyProxy" --app-version $appVersion `
    --vendor "MyProxy" --description "HTTP forward and reverse proxy" `
    --input $inputPath --main-jar $jarName --icon $iconPath `
    --main-class "com.myproxy.MyProxyApplication" --runtime-image $runtimePath `
    --dest $outputPath --win-menu --win-menu-group "MyProxy" `
    --win-shortcut --win-per-user-install `
    --win-upgrade-uuid "850015E9-1ED2-402F-AF5C-336B056012AE"
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed. On JDK 21, verify that WiX Toolset 3.x is installed and on PATH."
}

$installer = Get-ChildItem -LiteralPath $outputPath -Filter "*.exe" | Select-Object -First 1
if ($null -eq $installer) {
    throw "jpackage completed but no EXE installer was found under $outputPath."
}
Write-Host "Created installer: $($installer.FullName)"
