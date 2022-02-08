:start
"C:\Program Files\Java\jdk1.8.0_301\bin\java.exe" -server -Xmx180M -XX:NewSize=80M -XX:+UseSerialGC -jar build/libs/toobeetooteebot-0.2.8-1.12.2.jar
echo Restarting. Press Ctrl+C to stop
timeout 3
goto start
PAUSE