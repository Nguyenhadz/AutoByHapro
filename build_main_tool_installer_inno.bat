@echo off
chcp 65001 >nul
title Build AutoByHapro Installer With Inno Setup

cd /d "%~dp0"

echo ========================================
echo    BUILD AUTO BY HAPRO INSTALLER - INNO
echo ========================================
echo.

set "VERSION_FILE=src\main\resources\version.properties"
set "APP_NAME="
set "APP_VERSION="
set "APP_PUBLISHER="

if not exist "%VERSION_FILE%" (
    echo LOI: Khong tim thay %VERSION_FILE%
    pause
    exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%VERSION_FILE%") do (
    if /I "%%A"=="app.name" set "APP_NAME=%%B"
    if /I "%%A"=="app.version" set "APP_VERSION=%%B"
    if /I "%%A"=="app.publisher" set "APP_PUBLISHER=%%B"
)

if not defined APP_NAME (
    echo LOI: version.properties thieu app.name
    pause
    exit /b 1
)

if not defined APP_VERSION (
    echo LOI: version.properties thieu app.version
    pause
    exit /b 1
)

if not defined APP_PUBLISHER (
    set "APP_PUBLISHER=Hapro"
)

echo App: %APP_NAME%
echo Version: %APP_VERSION%
echo Publisher: %APP_PUBLISHER%
echo.

set "APP_IMAGE=main_tool_app\dist\%APP_NAME%"
set "ISS_FILE=installer_auto_by_hapro.iss"
set "VERSION_INCLUDE=installer_version.issinc"
set "OUTPUT_DIR=main_tool_installer\dist"

if not exist "%APP_IMAGE%\%APP_NAME%.exe" (
    echo LOI: Chua co app-image version moi.
    echo Hay chay build_main_tool_app.bat truoc.
    pause
    exit /b 1
)

if not exist "%ISS_FILE%" (
    echo LOI: Khong tim thay file %ISS_FILE%
    pause
    exit /b 1
)

set "ISCC_EXE="

if exist "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" (
    set "ISCC_EXE=C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
)

if not defined ISCC_EXE if exist "C:\Users\Admin\AppData\Local\Programs\Inno Setup 6\ISCC.exe" (
    set "ISCC_EXE=C:\Users\Admin\AppData\Local\Programs\Inno Setup 6\ISCC.exe"
)

if not defined ISCC_EXE (
    where iscc >nul 2>nul
    if not errorlevel 1 (
        set "ISCC_EXE=iscc"
    )
)

if not defined ISCC_EXE (
    echo LOI: Khong tim thay ISCC.exe cua Inno Setup.
    echo Hay kiem tra Inno Setup da cai thanh cong chua.
    pause
    exit /b 1
)

echo Da tim thay Inno Setup Compiler:
echo %ISCC_EXE%
echo.

echo [1/5] Tao file version tam cho Inno Setup...
> "%VERSION_INCLUDE%" echo #define MyAppName "%APP_NAME%"
>> "%VERSION_INCLUDE%" echo #define MyAppVersion "%APP_VERSION%"
>> "%VERSION_INCLUDE%" echo #define MyAppPublisher "%APP_PUBLISHER%"

if not exist "%VERSION_INCLUDE%" (
    echo LOI: Khong tao duoc %VERSION_INCLUDE%
    pause
    exit /b 1
)

echo [2/5] Xoa installer cu...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

echo.
echo [3/5] Don file ca nhan / nhay cam khoi app-image...

if exist "%APP_IMAGE%\data\download.db" (
    del /f /q "%APP_IMAGE%\data\download.db"
    echo Da xoa download.db ca nhan.
)

if exist "%APP_IMAGE%\data\license.lic" (
    del /f /q "%APP_IMAGE%\data\license.lic"
    echo Da xoa license.lic.
)

if exist "%APP_IMAGE%\backups\database\*.db" (
    del /f /q "%APP_IMAGE%\backups\database\*.db"
    echo Da xoa backup DB ca nhan.
)

if exist "%APP_IMAGE%\license_admin" (
    rmdir /s /q "%APP_IMAGE%\license_admin"
)

if exist "%APP_IMAGE%\license_admin_app" (
    rmdir /s /q "%APP_IMAGE%\license_admin_app"
)

if exist "%APP_IMAGE%\generated_licenses" (
    rmdir /s /q "%APP_IMAGE%\generated_licenses"
)

if exist "%APP_IMAGE%\private_key.pem" (
    del /f /q "%APP_IMAGE%\private_key.pem"
)

echo.
echo [4/5] Build installer bang Inno Setup...
"%ISCC_EXE%" "%ISS_FILE%"

if errorlevel 1 (
    echo.
    echo LOI: Build installer that bai.
    pause
    exit /b 1
)

echo.
echo [5/5] Hoan tat.
echo.
echo Installer moi nam tai:
echo %cd%\%OUTPUT_DIR%\%APP_NAME%_%APP_VERSION%.exe
echo.
echo Tu gio dung file installer nay, khong dung installer jpackage nua.
echo.
pause
