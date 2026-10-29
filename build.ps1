param(
    [string]$SdkRoot=$env:ANDROID_HOME,
    [string]$JavaHome=$env:JAVA_HOME,
    [switch]$DevelopmentSigning
)
$ErrorActionPreference='Stop'
if(!$SdkRoot){$SdkRoot=Join-Path $env:LOCALAPPDATA 'Android\Sdk'}
if(!$JavaHome){throw 'Set JAVA_HOME or pass -JavaHome for JDK 21.'}
$sdk=$SdkRoot
$buildTools=Join-Path $sdk 'build-tools\36.0.0'
$javaBin=Join-Path $JavaHome 'bin'
$output=Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $output,(Join-Path $PSScriptRoot 'private') | Out-Null
foreach($required in @((Join-Path $javaBin 'javac.exe'),(Join-Path $buildTools 'aapt2.exe'),(Join-Path $sdk 'platforms\android-35\android.jar'))){if(!(Test-Path -LiteralPath $required)){throw "Required build component missing: $required"}}
$key=Join-Path $PSScriptRoot 'private\voice-sip-assistant.jks'
$passwordFile=Join-Path $PSScriptRoot 'private\signing.password'
if(!(Test-Path -LiteralPath $passwordFile)){
    $bytes=New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $value=[Convert]::ToBase64String($bytes)
    [IO.File]::WriteAllText($passwordFile,$value,[Text.UTF8Encoding]::new($false))
}
$env:VSA_HELPER_SIGNING_PASSWORD=[IO.File]::ReadAllText($passwordFile)
$env:JAVA_HOME=$JavaHome
try {
    if(!(Test-Path -LiteralPath $key)){
        & (Join-Path $javaBin 'keytool.exe') -genkeypair -keystore $key -storepass:env VSA_HELPER_SIGNING_PASSWORD -keypass:env VSA_HELPER_SIGNING_PASSWORD -alias voice-sip-assistant -keyalg RSA -keysize 3072 -validity 3650 -dname 'CN=Voice SIP Assistant' -noprompt 2>&1 | Out-Null
        if($LASTEXITCODE){throw 'Signing key generation failed'}
    }
    if(!$DevelopmentSigning){
        $rootSource=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'src\com\fann\voicesiphelper\Root.java'))
        $expected=[regex]::Match($rootSource,'DUALSIM_CERT="([a-f0-9]{64})"').Groups[1].Value
        $listing=& (Join-Path $javaBin 'keytool.exe') '-J-Duser.language=en' -list -v -alias voice-sip-assistant -keystore $key -storepass:env VSA_HELPER_SIGNING_PASSWORD
        if($LASTEXITCODE){throw 'Cannot verify the signing certificate.'}
        $actual=[regex]::Match(($listing -join "`n"),'SHA256:\s*([A-Fa-f0-9:]+)').Groups[1].Value.Replace(':','').ToLowerInvariant()
        if(!$expected -or $actual -ne $expected){throw 'Signing certificate does not match the app identity. Use the existing production key. DevelopmentSigning is only for disposable CI builds.'}
        Write-Output 'Production signing certificate verified.'
    }
    & (Join-Path $buildTools 'aapt2.exe') compile --dir (Join-Path $PSScriptRoot 'res') -o (Join-Path $output 'resources.zip')
    if($LASTEXITCODE){throw 'Resource compilation failed'}
    & (Join-Path $buildTools 'aapt2.exe') link --manifest (Join-Path $PSScriptRoot 'AndroidManifest.xml') -I (Join-Path $sdk 'platforms\android-35\android.jar') (Join-Path $output 'resources.zip') -o (Join-Path $output 'unsigned.apk')
    if($LASTEXITCODE){throw 'Manifest packaging failed'}
    $classes=Join-Path $output 'classes'
    $dex=Join-Path $output 'dex'
    New-Item -ItemType Directory -Force -Path $classes,$dex | Out-Null
    $sources=@(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
    & (Join-Path $javaBin 'javac.exe') --release 8 -encoding UTF-8 -classpath (Join-Path $sdk 'platforms\android-35\android.jar') -d $classes @sources
    if($LASTEXITCODE){throw 'Java compilation failed'}
    $compiled=@(Get-ChildItem -LiteralPath $classes -Recurse -Filter '*.class' | ForEach-Object {$_.FullName})
    & (Join-Path $buildTools 'd8.bat') --min-api 31 --lib (Join-Path $sdk 'platforms\android-35\android.jar') --output $dex @compiled
    if($LASTEXITCODE){throw 'DEX compilation failed'}
    & (Join-Path $javaBin 'jar.exe') -uf (Join-Path $output 'unsigned.apk') -C $dex classes.dex
    if($LASTEXITCODE){throw 'DEX packaging failed'}
    & (Join-Path $javaBin 'jar.exe') -uf (Join-Path $output 'unsigned.apk') -C $PSScriptRoot assets
    if($LASTEXITCODE){throw 'Runtime asset packaging failed'}
    & (Join-Path $buildTools 'zipalign.exe') -f 4 (Join-Path $output 'unsigned.apk') (Join-Path $output 'aligned.apk')
    if($LASTEXITCODE){throw 'APK alignment failed'}
    & (Join-Path $buildTools 'apksigner.bat') sign --ks $key --ks-key-alias voice-sip-assistant --ks-pass env:VSA_HELPER_SIGNING_PASSWORD --key-pass env:VSA_HELPER_SIGNING_PASSWORD --out (Join-Path $output 'Voice-SIP-Assistant-1.4.0.apk') (Join-Path $output 'aligned.apk')
    if($LASTEXITCODE){throw 'APK signing failed'}
    & (Join-Path $buildTools 'apksigner.bat') verify (Join-Path $output 'Voice-SIP-Assistant-1.4.0.apk')
    if($LASTEXITCODE){throw 'APK verification failed'}
    & (Join-Path $buildTools 'aapt2.exe') dump permissions (Join-Path $output 'Voice-SIP-Assistant-1.4.0.apk')
}finally{Remove-Item Env:VSA_HELPER_SIGNING_PASSWORD -ErrorAction SilentlyContinue}
