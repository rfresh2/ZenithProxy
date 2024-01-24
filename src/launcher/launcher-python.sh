#!/bin/sh

PYTHON_CMD="python"

echo Finding python interpreter...

# Check if 'python' is not found, then try 'python3'
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  PYTHON_CMD="python3"
fi

# Check if neither 'python' nor 'python3' is found
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  echo "Error: Python interpreter not found. Please install Python from https://www.python.org/downloads/."
  exit 1
fi

echo Using Python interpreter: $PYTHON_CMD
echo Installing dependencies...
$PYTHON_CMD -m pip install -r requirements.txt
echo Launching ZenithProxy...
$PYTHON_CMD launcher-py.zip "$@"
