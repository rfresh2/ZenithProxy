@echo off

setlocal

echo Finding python interpreter...

set "PYTHON_CMD=python"

REM Check if 'python' is not found, then try 'python3'
where %PYTHON_CMD% >nul 2>nul || set "PYTHON_CMD=python3"

REM Check if neither 'python' nor 'python3' is found
where %PYTHON_CMD% >nul 2>nul || (
  echo Error: Python interpreter not found. Please install Python from https://www.python.org/downloads/
  exit /b 1
)

echo Using Python interpreter: %PYTHON_CMD%
echo Installing dependencies...
%PYTHON_CMD% -m pip install -r requirements.txt
echo Launching ZenithProxy...
%PYTHON_CMD% launcher-py.zip %*


