#!/bin/bash
# Simple utility for validating Mechset Files

echo "Testing Official Mechset"
grep -v '^#' mechset.txt | grep -o '"\w*\/.*"' | sed -e 's/^"//' -e 's/"$//' | while IFS= read -r line;
do
	if [ ! -s "$line" ]; then
		echo "File '$line' does not exist."
	fi
done

printf "\n\n"
echo "Testing Local Client Mechset"
grep -v '^#' localclient_mechset.txt | grep -o '"\w*\/.*"' | sed -e 's/^"//' -e 's/"$//' | while IFS= read -r line;
do
        if [ ! -s "$line" ]; then
                echo "File '$line' does not exist."
        fi
done

printf "\n\n"
echo "Testing Local Server Mechset"
grep -v '^#' localserver_mechset.txt | grep -o '"\w*\/.*"' | sed -e 's/^"//' -e 's/"$//' | while IFS= read -r line;
do
        if [ ! -s "$line" ]; then
                echo "File '$line' does not exist."
        fi
done

