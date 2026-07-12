@echo off
chcp 65001 >nul
title Export Skip Video IDs - AutoByHapro

cd /d "%~dp0"

echo ============================================
echo   EXPORT VIDEO ID CAN BO QUA - AutoByHapro
echo ============================================
echo.

set "YTDLP=tools\yt-dlp.exe"

if not exist "%YTDLP%" (
    echo LOI: Khong tim thay %YTDLP%
    echo Hay dat file nay trong thu muc goc project: D:\auto-by-Hapro
    pause
    exit /b 1
)

set "COOKIE_ARG="
set "COOKIE_FILE="

if exist "data\cookies\youtube_cookies.txt" set "COOKIE_FILE=data\cookies\youtube_cookies.txt"
if not defined COOKIE_FILE if exist "youtube_cookies.txt" set "COOKIE_FILE=youtube_cookies.txt"
if not defined COOKIE_FILE if exist "tools\youtube_cookies.txt" set "COOKIE_FILE=tools\youtube_cookies.txt"
if not defined COOKIE_FILE if exist "data\youtube_cookies.txt" set "COOKIE_FILE=data\youtube_cookies.txt"

echo Dung yt-dlp:
echo %YTDLP%
echo.

if defined COOKIE_FILE (
    set "COOKIE_ARG=--cookies "%COOKIE_FILE%""
    echo Dung cookies:
    echo %COOKIE_FILE%
) else (
    echo Khong thay youtube_cookies.txt, se quet khong dung cookies.
    echo Neu can cookies, dat file tai: data\cookies\youtube_cookies.txt
)

echo.

if exist "skip_ids" rmdir /s /q "skip_ids"
mkdir "skip_ids"

echo [1/6] Tiem nha Haya - bo qua 162 video dau...
"%YTDLP%" --flat-playlist --playlist-end 162 --ignore-errors --no-warnings %COOKIE_ARG% --print "%%(id)s|%%(title)s|%%(webpage_url)s" "https://www.youtube.com/@Pithongthai/shorts" > "skip_ids\01_Tiem_nha_Haya_Pithongthai_162.txt"

echo [2/6] Tiem nha LiNa - bo qua 156 video dau...
"%YTDLP%" --flat-playlist --playlist-end 156 --ignore-errors --no-warnings %COOKIE_ARG% --print "%%(id)s|%%(title)s|%%(webpage_url)s" "https://www.youtube.com/@moingaymotxi8823/shorts" > "skip_ids\02_Tiem_nha_LiNa_moingaymotxi8823_156.txt"

echo [3/6] Nho nhit be bong - bo qua 150 video dau...
"%YTDLP%" --flat-playlist --playlist-end 150 --ignore-errors --no-warnings %COOKIE_ARG% --print "%%(id)s|%%(title)s|%%(webpage_url)s" "https://www.youtube.com/@NextFarm009/shorts" > "skip_ids\03_Nho_nhit_be_bong_NextFarm009_150.txt"

echo [4/6] Review phim dao - bo qua 150 video dau...
"%YTDLP%" --flat-playlist --playlist-end 150 --ignore-errors --no-warnings %COOKIE_ARG% --print "%%(id)s|%%(title)s|%%(webpage_url)s" "https://www.youtube.com/@DopamineTriTh%%E1%%BB%%A9c/shorts" > "skip_ids\04_Review_phim_dao_DopamineTriThuc_150.txt"

echo [5/6] Phim moi - bo qua 150 video dau...
"%YTDLP%" --flat-playlist --playlist-end 150 --ignore-errors --no-warnings %COOKIE_ARG% --print "%%(id)s|%%(title)s|%%(webpage_url)s" "https://www.youtube.com/@gio_dong_lao/shorts" > "skip_ids\05_Phim_moi_gio_dong_lao_150.txt"

echo [6/6] Hoai niem xua - bo qua 130 video dau...
"%YTDLP%" --flat-playlist --playlist-end 130 --ignore-errors --no-warnings %COOKIE_ARG% --print "%%(id)s|%%(title)s|%%(webpage_url)s" "https://www.youtube.com/@matkinhcan/shorts" > "skip_ids\06_Hoai_niem_xua_matkinhcan_130.txt"

echo.
echo ============================================
echo KIEM TRA SO DONG DA XUAT
echo ============================================
for %%F in ("skip_ids\*.txt") do (
    for /f %%C in ('find /c /v "" ^< "%%F"') do echo %%~nxF : %%C dong
)

echo.
echo XONG
echo Hay nen ca thu muc skip_ids thanh zip roi gui lai cho ChatGPT.
echo.
pause
