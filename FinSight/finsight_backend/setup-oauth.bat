@echo off
REM Google OAuth2 Setup Script for FinSight Backend (Windows)
REM This script helps you set up Google OAuth2 credentials

echo 🔧 FinSight Google OAuth2 Setup
echo ================================
echo.

REM Check if .env file exists
if exist ".env" (
    echo ⚠️  .env file already exists. Backing up to .env.backup
    copy .env .env.backup
)

REM Create .env file from template
echo 📝 Creating .env file from template...
copy oauth-config.env .env

echo.
echo ✅ .env file created successfully!
echo.
echo 🔑 Next steps:
echo 1. Go to Google Cloud Console: https://console.cloud.google.com/
echo 2. Create a new project or select existing one
echo 3. Enable Google+ API in 'APIs & Services' → 'Library'
echo 4. Create OAuth 2.0 credentials in 'APIs & Services' → 'Credentials'
echo 5. Set authorized redirect URI to: http://localhost:8080/login/oauth2/code/google
echo 6. Copy your Client ID and Client Secret
echo 7. Edit the .env file and replace the placeholder values
echo.
echo 📁 Edit the .env file with your actual credentials:
echo    notepad .env
echo.
echo 🚀 After setting up credentials, start the backend:
echo    mvn spring-boot:run
echo.
echo 🌐 Test OAuth at: http://localhost:5173/login
pause

