@echo off

echo 🔨 Building project...
call mvn clean package

echo 📦 Preparing app files...

if exist app rmdir /s /q app
mkdir app

echo 🔍 Checking target contents...
dir target

copy target\app.jar app\app.jar

if not exist app\app.jar (
    echo ❌ ERROR: app.jar NOT created!
    pause
    exit /b
)

xcopy tools app\tools /E /I /Y

copy assets\icon.ico app\

echo ✅ App folder contents:
dir app

echo 🚀 App build ready in /app folder
pause