@echo off
setlocal enabledelayedexpansion
set "JAVA_BIN=C:\Program Files\Android\Android Studio\jbr\bin"
if not exist keystore mkdir keystore
if not exist "%JAVA_BIN%\keytool.exe" (
  echo KEYTOOL_MISSING
  exit /b 1
)
"%JAVA_BIN%\keytool.exe" -genkeypair -alias release -keystore keystore\release.keystore -storepass releasepass -keypass releasepass -dname "CN=Echo Music, OU=Dev, O=Echo Music, L=City, S=State, C=US" -validity 10000 -keyalg RSA -keysize 2048 -noprompt
if not exist keystore\release.keystore (
  echo KEYSTORE_CREATION_FAILED
  exit /b 1
)
echo KEYSTORE_CREATED
set "KEY_ALIAS=release"
set "KEY_PASSWORD=releasepass"
set "STORE_PASSWORD=releasepass"
set "PATH=%JAVA_BIN%;%PATH%"
echo Building signed universal FOSS release APK...
call gradlew.bat :app:assembleUniversalFossRelease --console=plain --stacktrace
if errorlevel 1 exit /b %errorlevel%
endlocal