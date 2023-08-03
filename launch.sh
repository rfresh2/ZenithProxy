#!/bin/sh

while true
do
  # todo: auto-download and update start.py
  python start.py
  echo "Restarting. Press Ctrl+C to stop"
  sleep 3
done
