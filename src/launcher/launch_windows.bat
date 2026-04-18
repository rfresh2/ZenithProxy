@echo off
setlocal

where uv >nul 2>nul
if %errorlevel% == 0 (
  echo Found uv
  echo Starting Launcher...
  uv run --python ">=3.14" --upgrade --with-requirements requirements.txt -- launcher-py.zip %*
  exit /b
)

if "%1" == "--reinstall-python" (
	echo Reinstalling Python...
	del python /Q
)

if not exist python\python.exe (
	if exist python.tar.gz (
		del python.tar.gz
	)

    echo Downloading Python...
    REM --ssl-no-revoke necessary as some users have broken windows installs? https://discord.com/channels/1127460556710883391/1127461501960208465/1320859617487618078
    curl -L --ssl-no-revoke -o python.tar.gz "https://github.com/astral-sh/python-build-standalone/releases/download/20260414/cpython-3.14.4+20260414-x86_64-pc-windows-msvc-install_only_stripped.tar.gz"

  	if errorlevel 1 (
		echo Error: Failed to download Python.
		exit /b 1
	)
	if not exist python.tar.gz (
		echo Error: Failed to download Python.
		exit /b 1
	)
	REM sanity checking if python.tar.gz is less than 5MB
	REM meaning we probably downloaded an error page
	for %%A in (python.tar.gz) do if %%~zA LSS 5000000 (
		echo Error: Failed to download Python.
		exit /b 1
	)

    echo Extracting Python...
    tar -xf python.tar.gz

	if errorlevel 1 (
		echo Error: Failed to extract Python.
		exit /b 1
	)
	if not exist python\python.exe (
		echo Error: Failed to extract Python.
		exit /b 1
	)

    del python.tar.gz
) else (
    echo Found existing Python installation.
)

echo Updating Python dependencies...
python\python.exe -m pip install --upgrade --requirement requirements.txt -qq --disable-pip-version-check --no-input

if errorlevel 1 (
	echo Error: Failed installing Python requirements.
	echo Install uv: https://docs.astral.sh/uv/getting-started/installation/
	exit /b 1
)

echo Starting Launcher...
python\python.exe launcher-py.zip %*

endlocal
