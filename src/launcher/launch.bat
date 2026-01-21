@echo off

setlocal

echo Finding python interpreter...

where uv >nul 2>nul
if %errorlevel% == 0 (
  echo Found uv
  echo Starting Launcher...
  uv run --python ">=3.14" --upgrade --with-requirements requirements.txt -- launcher-py.zip %*
  exit /b
)

set "PYTHON_CMD=python3"

REM Check if 'python3' is not found, then try 'python'
where %PYTHON_CMD% >nul 2>nul || set "PYTHON_CMD=python"

REM Check if neither 'python' nor 'python3' is found
where %PYTHON_CMD% >nul 2>nul || (
  echo Error: Python interpreter not found.
  echo Install uv: https://docs.astral.sh/uv/getting-started/installation/
  exit /b 1
)

echo Using Python interpreter: %PYTHON_CMD%
%PYTHON_CMD% -m pip >nul 2>nul
if errorlevel 1 (
  echo Error: pip is required but not installed!
  echo Install uv: https://docs.astral.sh/uv/getting-started/installation/
  exit /b 1
)
echo Updating Python dependencies...
%PYTHON_CMD% -m pip install --upgrade --requirement requirements.txt -qq --disable-pip-version-check --no-input
echo Starting Launcher...
%PYTHON_CMD% launcher-py.zip %*


