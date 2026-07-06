@echo off
chcp 65001 >nul
title Build Hapro License Admin

cd /d "%~dp0"

set APP_NAME=HaproLicenseAdmin
set BUILD_ROOT=license_admin_app
set APP_BUILD=%BUILD_ROOT%\build
set DIST_ROOT=%BUILD_ROOT%\dist
set JAR_FILE=%APP_BUILD%\%APP_NAME%.jar

echo ========================================
echo       BUILD HAPRO LICENSE ADMIN
echo ========================================
echo.

if not exist "pom.xml" (
    echo Khong tim thay pom.xml.
    echo Hay dat file nay o dung thu muc goc project.
    echo.
    pause
    exit /b
)

if not exist "license_admin\private_key.pem" (
    echo Khong tim thay license_admin\private_key.pem
    echo.
    echo M can giu folder license_admin trong project nay.
    echo Neu chua co key, hay chay LicenseAdminKeyGenerator truoc.
    echo.
    pause
    exit /b
)

where jpackage >nul 2>nul

if errorlevel 1 (
    echo Khong tim thay jpackage.
    echo.
    echo May m dang thieu JDK day du hoac bien moi truong PATH chua tro den JDK.
    echo Can co file jpackage.exe trong JDK, vi du:
    echo C:\Program Files\Java\jdk-21\bin\jpackage.exe
    echo.
    pause
    exit /b
)

where jar >nul 2>nul

if errorlevel 1 (
    echo Khong tim thay jar.
    echo.
    echo Can cai JDK day du, khong phai chi JRE.
    echo.
    pause
    exit /b
)

echo [1/6] Compile project...
call mvn clean compile

if errorlevel 1 (
    echo.
    echo Compile loi, dung build app admin.
    pause
    exit /b
)

echo.
echo [2/6] Xoa build cu...
if exist "%BUILD_ROOT%" rmdir /s /q "%BUILD_ROOT%"

mkdir "%APP_BUILD%"
mkdir "%DIST_ROOT%"

echo.
echo [3/6] Tao jar admin...
jar --create --file "%JAR_FILE%" --main-class com.hapro.autobyhapro.license.admin.LicenseAdminGui -C target/classes com/hapro/autobyhapro/license

if errorlevel 1 (
    echo.
    echo Tao jar loi.
    pause
    exit /b
)

echo.
echo [4/6] Tao app-image bang jpackage...
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --input "%APP_BUILD%" ^
  --main-jar "%APP_NAME%.jar" ^
  --dest "%DIST_ROOT%" ^
  --app-version 1.0 ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo.
    echo jpackage loi, dung build app admin.
    echo Hay chup lai man hinh loi gui cho minh.
    echo.
    pause
    exit /b
)

echo.
echo [5/6] Copy private/public key vao app admin...
mkdir "%DIST_ROOT%\%APP_NAME%\license_admin"
copy "license_admin\private_key.pem" "%DIST_ROOT%\%APP_NAME%\license_admin\" >nul

if exist "license_admin\public_key_base64.txt" (
    copy "license_admin\public_key_base64.txt" "%DIST_ROOT%\%APP_NAME%\license_admin\" >nul
)

if exist "license_admin\public_key.pem" (
    copy "license_admin\public_key.pem" "%DIST_ROOT%\%APP_NAME%\license_admin\" >nul
)

mkdir "%DIST_ROOT%\%APP_NAME%\generated_licenses"

echo.
echo [6/6] Tao file mo nhanh...
echo @echo off > "%DIST_ROOT%\open_license_admin.bat"
echo cd /d "%%~dp0%APP_NAME%" >> "%DIST_ROOT%\open_license_admin.bat"
echo start "" "%APP_NAME%.exe" >> "%DIST_ROOT%\open_license_admin.bat"

echo.
echo ========================================
echo BUILD XONG APP TAO LICENCE
echo ========================================
echo.
echo App chinh:
echo %cd%\%DIST_ROOT%\%APP_NAME%\%APP_NAME%.exe
echo.
echo File mo nhanh:
echo %cd%\%DIST_ROOT%\open_license_admin.bat
echo.
echo Luu y:
echo - Folder nay co chua private_key.pem.
echo - Chi de tren may cua m.
echo - Khong gui folder nay cho khach.
echo.
pause