#!/bin/sh

PYTHON_CMD="python"

echo Launching ZenithProxy...

# Check if 'python' is not found, then try 'python3'
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  PYTHON_CMD="python3"
fi

# Check if neither 'python' nor 'python3' is found
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  echo "Error: Python interpreter not found. Please install Python from https://www.python.org/downloads/."
  exit 1
fi

while true
do
  $PYTHON_CMD update_launcher.py
  $PYTHON_CMD launcher.py
  if [ $? -eq 69 ]; then
    exit 1
  fi
  echo "Restarting. Press Ctrl+C to stop"
  sleep 3
done
