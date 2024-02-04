#!/bin/sh

#########################
# Run this from the repo's root directory
# ./scripts/launcher-git.sh
#########################

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
$PYTHON_CMD -m pip > /dev/null 2>&1
has_pip="$?"
if [ "$has_pip" = 1 ] ; then
  echo "Error: pip is required but not installed!"
  echo "Help installing: https://pip.pypa.io/en/stable/installation/"
  echo "Or on Ubuntu: 'apt install python3-pip'"
  exit 1
fi
echo Verifying requirements...
$PYTHON_CMD -m pip install -r src/launcher/requirements.txt -qq --disable-pip-version-check --no-input
echo Starting Launcher...
$PYTHON_CMD src/launcher/__main__.py "$@"
