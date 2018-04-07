#!/bin/bash

# This is the shell startup file for Pork2b2tBot.
# Input ./start.sh while in the server directory
# to start the server.

#Change this to "true" to 
#loop Pork2b2tBot after restart!

DO_LOOP="true"

###############################
# DO NOT EDIT ANYTHING BELOW! #
###############################

clear

cd MCProtocolLib
if git pull origin master | grep -q 'Already up-to-date.'; then
        clear
        cd ..
        echo "Nothing  changed, continuing..."
else
        cd ..
        mvn
        sleep 0.5
        clear
        echo "Compiled, continuing..." 
fi

if git pull | grep -q 'Already up-to-date.'; then
    clear
    echo "Nothing  changed, starting..."
else
    mvn
    clear
    echo "Compiled, starting..." 
fi

sleep 2

clear 

while [ "$DO_LOOP" == "true" ]; do
	mvn exec:java -Dexec.mainClass="net.daporkchop.toobeetooteebot.TooBeeTooTeeBot" -Dexec.classpathScope=runtime
	echo "Press Ctrl+c to stop" 
	sleep 0.5
	cd MCProtocolLib
        if git pull origin master | grep -q 'Already up-to-date.'; then
                clear
		        cd ..
                echo "Nothing  changed, continuing..."
        else
		        cd ..
                mvn
                sleep 0.5
                clear
                echo "Compiled, continuing..." 
        fi

        if git pull | grep -q 'Already up-to-date.'; then
		clear
                echo "Nothing  changed, starting..."
	else
        mvn
		sleep 0.5
		clear
		echo "Compiled, starting..." 
	fi
done
