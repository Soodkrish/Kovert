@echo off
setlocal enabledelayedexpansion

echo ================================
echo 🔨 STEP 1: Build JAR
echo ================================
call mvn clean package -B

IF ERRORLEVEL 1 (
    echo ❌ Maven build failed!
    pause
    exit /b 1
)

echo ================================
echo 📦 STEP 2: Prepare app folder
echo ================================
if exist app rmdir /s /q app
mkdir app

:: Copy the fresh JAR
copy target\app.jar app\app.jar >nul
xcopy tools app\tools /E /I /Y >nul
copy assets\icon.ico app\ >nul

:: ✅ IMPORTANT: Ensure cli.properties is where jpackage can see it
:: It should be in the root of your project or inside the app folder
if not exist cli.properties (
    echo ❌ ERROR: cli.properties missing!
    pause
    exit /b 1
)

echo ================================
echo 🚀 STEP 3: Run jpackage
echo ================================
if exist dist rmdir /s /q dist

"C:\Program Files\Java\jdk-23\bin\jpackage.exe" ^
  --name Kovert ^
  --app-version 1.0.0 ^
  --input app ^
  --main-jar app.jar ^
  --main-class com.yourfamily.pdf.secure_pdf_converter.ui.MainWindow ^
  --type app-image ^
  --dest dist ^
  --icon app\icon.ico ^
  --module-path "C:\Program Files\Java\javafx-jmods-24.0.1" ^
  --add-modules javafx.controls,javafx.graphics,javafx.swing ^
  --add-launcher kovertcli=cli.properties ^
  --java-options "-Dprism.order=sw"

IF ERRORLEVEL 1 (
    echo ❌ jpackage failed!
    pause
    exit /b 1
)

:: ✅ STEP 4 (Old BAT step) was REMOVED. 
:: jpackage already created kovertcli.exe inside dist\Kovert!

echo ================================
echo 📂 STEP 5: Harvest files (heat)
echo ================================
:: Added -arch x64 to the command below
heat dir dist\Kovert -cg AppFiles -dr INSTALLDIR -srd -sreg -gg -arch x64 -out files.wxs

IF ERRORLEVEL 1 (
    echo ❌ Heat failed!
    pause
    exit /b 1
)

echo ================================
echo 🔨 STEP 6: Build MSI
echo ================================
:: Note: I used 'installer.wxs' here, ensure your file is named exactly that
candle installer.wxs files.wxs

IF ERRORLEVEL 1 (
    echo ❌ Candle failed!
    pause
    exit /b 1
)

:: Adding -sval skips validation if you're hitting minor UI warnings
light installer.wixobj files.wixobj -ext WixUIExtension -sw1076 -sice:ICE57 -sice:ICE43 -sice:ICE38 -sice:ICE80 -b dist\Kovert -o Kovert.msi

IF ERRORLEVEL 1 (
    echo ❌ Light failed!
    pause
    exit /b 1
)

echo ================================
echo ✅ DONE: Kovert.msi created
echo ================================
pause