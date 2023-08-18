:loop
call scripts\build.bat
call scripts\run.bat
echo Press [CTRL+C] to stop...
timeout /t 3
goto loop

