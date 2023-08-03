#!/bin/sh

PYTHON_CMD="python"

# Check if 'python' is not found, then try 'python3'
if ! command -v $PYTHON_CMD >/dev/null 2>&1; then
  PYTHON_CMD="python3"
fi

while true
do
  # todo: auto-download and update start.py
  $PYTHON_CMD start.py
  echo "Restarting. Press Ctrl+C to stop"
  sleep 3
done

