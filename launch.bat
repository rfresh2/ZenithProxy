@echo off

setlocal

echo Launching ZenithProxy...

set "PYTHON_CMD=python"

REM Check if 'python' is not found, then try 'python3'
where %PYTHON_CMD% >nul 2>nul || set "PYTHON_CMD=python3"

REM Check if neither 'python' nor 'python3' is found
where %PYTHON_CMD% >nul 2>nul || (
  echo Error: Python interpreter not found. Please install Python from https://www.python.org/downloads/
  exit /b 1
)

:loop
%PYTHON_CMD% update_launcher.py
%PYTHON_CMD% start.py
echo Restarting. Press Ctrl+C to stop
timeout /t 3 >nul
goto loop
