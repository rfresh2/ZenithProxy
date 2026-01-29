#!/bin/sh

PYTHON_CMD="python3"

echo Finding python interpreter...

if command -v 'uv' >/dev/null 2>&1; then
    echo Found uv
    echo Starting Launcher...
    uv run --python ">=3.14" --upgrade --with-requirements requirements.txt -- launcher-py.zip "$@"
    exit 0
fi

# Check if 'python3' is not found, then try 'python'
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  PYTHON_CMD="python"
fi

# Check if neither 'python' nor 'python3' is found
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  echo "Error: Python interpreter not found"
  echo "Install uv: https://docs.astral.sh/uv/getting-started/installation/"
  exit 1
fi

echo Using Python interpreter: $PYTHON_CMD
$PYTHON_CMD -m pip > /dev/null 2>&1
has_pip="$?"
if [ "$has_pip" = 1 ] ; then
  echo "Error: pip is required but not installed!"
  echo "Install uv: https://docs.astral.sh/uv/getting-started/installation/"
  exit 1
fi
echo Updating Python dependencies...
$PYTHON_CMD -m pip install --upgrade --requirement requirements.txt -qq --disable-pip-version-check --no-input
echo Starting Launcher...
$PYTHON_CMD launcher-py.zip "$@"
