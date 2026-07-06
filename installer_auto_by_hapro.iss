#if FileExists("installer_version.issinc")
    #include "installer_version.issinc"
#else
    #define MyAppName "AutoByHapro"
    #define MyAppVersion "1.0.0"
    #define MyAppPublisher "Hapro"
#endif

#define MyAppExeName "AutoByHapro.exe"

[Setup]
AppId={{B7D66B21-F4C6-4C55-BB75-9AB8487FB044}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName=Auto By Hapro
DisableDirPage=no
DisableProgramGroupPage=no
OutputDir=main_tool_installer\dist
OutputBaseFilename={#MyAppName}_{#MyAppVersion}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
SetupIconFile=src\main\resources\icons\app.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
PrivilegesRequired=admin
CloseApplications=yes
CloseApplicationsFilter=AutoByHapro.exe

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Tạo shortcut ngoài Desktop"; GroupDescription: "Shortcut:"; Flags: unchecked

[Files]
Source: "main_tool_app\dist\AutoByHapro\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs; Excludes: "data\license.lic,data\download.db,backups\database\*.db"
Source: "src\main\resources\icons\app.ico"; DestDir: "{app}"; DestName: "app.ico"; Flags: ignoreversion

[Icons]
Name: "{group}\AutoByHapro"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\app.ico"; IconIndex: 0
Name: "{group}\Gỡ AutoByHapro"; Filename: "{uninstallexe}"; IconFilename: "{app}\app.ico"; IconIndex: 0
Name: "{autodesktop}\AutoByHapro"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\app.ico"; IconIndex: 0; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Mở AutoByHapro"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
; Không xóa data mặc định để tránh mất DB/licence khi gỡ hoặc cài lại.
; Nếu sau này muốn gỡ sạch hoàn toàn thì thêm lựa chọn riêng.

[InstallDelete]
Type: files; Name: "{app}\data\license.lic"
