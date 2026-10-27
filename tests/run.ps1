param([Parameter(Mandatory=$true)][string]$SdkRoot,[Parameter(Mandatory=$true)][string]$JavaHome)
$ErrorActionPreference='Stop'
$project=Split-Path -Parent $PSScriptRoot
$testOutput=Join-Path $project 'out\tests'
New-Item -ItemType Directory -Force -Path $testOutput,(Join-Path $testOutput 'classes'),(Join-Path $testOutput 'dex') | Out-Null
$tools=Join-Path $SdkRoot 'build-tools\36.0.0'
$androidJar=Join-Path $SdkRoot 'platforms\android-35\android.jar'
$javaBin=Join-Path $JavaHome 'bin'
$env:JAVA_HOME=$JavaHome
$env:VSA_HELPER_SIGNING_PASSWORD=[IO.File]::ReadAllText((Join-Path $project 'private\signing.password'))
try {
 & (Join-Path $javaBin 'javac.exe') --release 8 -encoding UTF-8 -classpath $androidJar -d (Join-Path $testOutput 'classes') (Join-Path $PSScriptRoot 'Checks.java')
 if($LASTEXITCODE){throw 'Test compilation failed'}
 $compiled=@(Get-ChildItem (Join-Path $testOutput 'classes') -Recurse -Filter '*.class' | ForEach-Object {$_.FullName})
 & (Join-Path $tools 'd8.bat') --min-api 31 --lib $androidJar --output (Join-Path $testOutput 'dex') @compiled
 if($LASTEXITCODE){throw 'Test DEX failed'}
 & (Join-Path $tools 'aapt2.exe') link --manifest (Join-Path $PSScriptRoot 'AndroidManifest.xml') -I $androidJar -o (Join-Path $testOutput 'unsigned.apk')
 if($LASTEXITCODE){throw 'Test packaging failed'}
 & (Join-Path $javaBin 'jar.exe') -uf (Join-Path $testOutput 'unsigned.apk') -C (Join-Path $testOutput 'dex') classes.dex
 if($LASTEXITCODE){throw 'Test DEX packaging failed'}
 & (Join-Path $tools 'zipalign.exe') -f 4 (Join-Path $testOutput 'unsigned.apk') (Join-Path $testOutput 'aligned.apk')
 if($LASTEXITCODE){throw 'Test alignment failed'}
 & (Join-Path $tools 'apksigner.bat') sign --ks (Join-Path $project 'private\voice-sip-assistant.jks') --ks-key-alias voice-sip-assistant --ks-pass env:VSA_HELPER_SIGNING_PASSWORD --key-pass env:VSA_HELPER_SIGNING_PASSWORD --out (Join-Path $testOutput 'checks.apk') (Join-Path $testOutput 'aligned.apk')
 if($LASTEXITCODE){throw 'Test signing failed'}
 & (Join-Path $SdkRoot 'platform-tools\adb.exe') install -r (Join-Path $testOutput 'checks.apk')
 if($LASTEXITCODE){throw 'Test install failed'}
 & (Join-Path $SdkRoot 'platform-tools\adb.exe') shell am instrument -w com.fann.voicesiphelper.tests/.Checks
 if($LASTEXITCODE){throw 'Instrumentation failed'}
} finally {Remove-Item Env:VSA_HELPER_SIGNING_PASSWORD -ErrorAction SilentlyContinue}
