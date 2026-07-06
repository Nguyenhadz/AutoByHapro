@echo off
chcp 65001 >nul
title Build Auto By Hapro Main App

cd /d "%~dp0"

echo ========================================
echo        BUILD AUTO BY HAPRO MAIN APP
echo ========================================
echo.
echo Thu muc dang chay:
echo %cd%
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

set "BUILD_ROOT=main_tool_app"
set "APP_BUILD=%BUILD_ROOT%\build"
set "APP_CLASSES=%APP_BUILD%\classes"
set "DIST_ROOT=%BUILD_ROOT%\dist"
set "JAR_FILE=%APP_BUILD%\%APP_NAME%.jar"

if not exist "pom.xml" (
    echo LOI: Khong tim thay pom.xml.
    echo File .bat nay phai dat trong thu muc goc project.
    pause
    exit /b 1
)

if not exist "src\main\java\com\hapro\autobyhapro\gui\GuiLauncher.java" (
    echo LOI: Chua co GuiLauncher.java.
    pause
    exit /b 1
)

where jpackage >nul 2>nul
if errorlevel 1 (
    echo LOI: Khong tim thay jpackage.
    echo Can JDK day du, khong phai JRE.
    pause
    exit /b 1
)

where jar >nul 2>nul
if errorlevel 1 (
    echo LOI: Khong tim thay jar.
    echo Can JDK day du.
    pause
    exit /b 1
)

echo [1/9] Compile project va copy dependency...
call mvn clean compile dependency:copy-dependencies ^
  -DincludeScope=runtime ^
  -DoutputDirectory=target\dependency ^
  -Drevision=%APP_VERSION%

if errorlevel 1 (
    echo.
    echo LOI: Compile Maven that bai.
    pause
    exit /b 1
)

if not exist "target\classes\com\hapro\autobyhapro\gui\GuiLauncher.class" (
    echo.
    echo LOI: Khong thay GuiLauncher.class sau compile.
    pause
    exit /b 1
)

if not exist "target\classes\version.properties" (
    echo.
    echo LOI: version.properties chua duoc copy vao target\classes.
    pause
    exit /b 1
)

echo.
echo [2/9] Xoa build cu...
if exist "%BUILD_ROOT%" rmdir /s /q "%BUILD_ROOT%"

mkdir "%APP_BUILD%"
mkdir "%APP_CLASSES%"
mkdir "%DIST_ROOT%"

echo.
echo [3/9] Copy class va resource...
robocopy "target\classes" "%APP_CLASSES%" /E >nul

echo.
echo [4/9] Xoa tool admin tao licence khoi app chinh...
if exist "%APP_CLASSES%\com\hapro\autobyhapro\license\admin" (
    rmdir /s /q "%APP_CLASSES%\com\hapro\autobyhapro\license\admin"
)

echo.
echo [5/9] Tao jar app chinh voi main-class GuiLauncher...
jar --create --file "%JAR_FILE%" --main-class com.hapro.autobyhapro.gui.GuiLauncher -C "%APP_CLASSES%" .

if errorlevel 1 (
    echo.
    echo LOI: Tao jar that bai.
    pause
    exit /b 1
)

echo.
echo [6/9] Copy dependencies vao folder build...
if exist "target\dependency\*.jar" (
    copy "target\dependency\*.jar" "%APP_BUILD%\" >nul
)

echo.
echo Kiem tra JavaFX jars trong build:
dir "%APP_BUILD%\javafx*.jar" /b

echo.
echo Neu khong thay javafx-base / javafx-controls / javafx-graphics thi POM dependency chua copy dung.
echo.

echo.
echo [7/9] Tao app-image bang jpackage, kem JavaFX modules...
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --input "%APP_BUILD%" ^
  --main-jar "%APP_NAME%.jar" ^
  --main-class com.hapro.autobyhapro.gui.GuiLauncher ^
  --dest "%DIST_ROOT%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "%APP_PUBLISHER%" ^
  --runtime-image "%JAVA_HOME%" ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo.
    echo LOI: jpackage that bai.
    echo Chup lai toan bo man hinh loi gui minh.
    pause
    exit /b 1
)

echo.
echo [8/9] Tao folder du lieu cho app...
mkdir "%DIST_ROOT%\%APP_NAME%\data"
mkdir "%DIST_ROOT%\%APP_NAME%\tools"
mkdir "%DIST_ROOT%\%APP_NAME%\video"
mkdir "%DIST_ROOT%\%APP_NAME%\video\raw"
mkdir "%DIST_ROOT%\%APP_NAME%\video\capcut_export"
mkdir "%DIST_ROOT%\%APP_NAME%\video\edited"
mkdir "%DIST_ROOT%\%APP_NAME%\video\edited_unknown"
mkdir "%DIST_ROOT%\%APP_NAME%\exports"
mkdir "%DIST_ROOT%\%APP_NAME%\logs"
mkdir "%DIST_ROOT%\%APP_NAME%\backups"
mkdir "%DIST_ROOT%\%APP_NAME%\backups\database"

echo.
echo Copy tools neu co...
if exist "tools" (
    robocopy "tools" "%DIST_ROOT%\%APP_NAME%\tools" /E >nul
)

echo.
echo Tao ban release sach: KHONG copy database ca nhan.
echo Database moi se duoc tao khi mo app lan dau.

if exist "%DIST_ROOT%\%APP_NAME%\data\download.db" (
    del /f /q "%DIST_ROOT%\%APP_NAME%\data\download.db"
)

if exist "%DIST_ROOT%\%APP_NAME%\data\license.lic" (
    del /f /q "%DIST_ROOT%\%APP_NAME%\data\license.lic"
)

echo.
echo [9/9] Tao file mo nhanh...
echo @echo off > "%DIST_ROOT%\open_%APP_NAME%.bat"
echo cd /d "%%~dp0%APP_NAME%" >> "%DIST_ROOT%\open_%APP_NAME%.bat"
echo start "" "%APP_NAME%.exe" >> "%DIST_ROOT%\open_%APP_NAME%.bat"

echo.
echo ========================================
echo BUILD XONG TOOL CHINH
echo ========================================
echo.
echo App: %APP_NAME%
echo Version: %APP_VERSION%
echo.
echo App chinh:
echo %cd%\%DIST_ROOT%\%APP_NAME%\%APP_NAME%.exe
echo.
echo File mo nhanh:
echo %cd%\%DIST_ROOT%\open_%APP_NAME%.bat
echo.
echo Luu y:
echo - Hay chay dung file exe trong main_tool_app\dist\%APP_NAME%.
echo - Khong chay file exe cu.
echo - App chinh KHONG copy private_key.pem.
echo.
pause
