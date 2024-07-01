#!/bin/bash

# MegaMek -
# Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
# 
#  This program is free software; you can redistribute it and/or modify it
#  under the terms of the GNU General Public License as published by the Free
#  Software Foundation; either version 2 of the License, or (at your option)
#  any later version.
# 
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
#  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
#  for more details.
# 

# Define script constants.
MEGAMEK_NIX_NAME="megamek"  # Assuming on *nix name is lowercase (megamek-*)
MEGAMEK_MAIN_CLASS="megamek.MegaMek"
MEGAMEK_DEFAULT_JARNAME="MegaMek.jar"
MEGAMEK_DEFAULT_CLASSPATH="/usr/share/java"
MEGAMEK_DEFAULT_CONFROOT="$HOME"
MEGAMEK_DEFAULT_DATAPATH="/usr/share/MegaMek"

MEGAMEK_ACTUAL_PATH=$(dirname "${BASH_SOURCE[0]}")
cd $MEGAMEK_ACTUAL_PATH


# Try to find the executable for Java.
JAVA=/usr/bin/java
test -x "$JAVA_HOME/bin/java" && JAVA="$JAVA_HOME/bin/java"

# Try to find the directory containing MegaMek JARs.
#  MEGAMEK_CLASSPATH may be set before hand, which is not desired.
if ! test -z "$MEGAMEK_CLASSPATH"; then
    if ! test -d "/.$MEGAMEK_CLASSPATH" -a \
        -f "/.$MEGAMEK_CLASSPATH/$MEGAMEK_DEFAULT_JARNAME"; then
        echo "Relative path: $MEGAMEK_CLASSPATH.  Clearing."
        MEGAMEK_CLASSPATH=""
    fi
fi
if test -z "$MEGAMEK_CLASSPATH"; then
    temp_path=$MEGAMEK_DEFAULT_CLASSPATH
    if test -d $temp_path -a \
        -f "$temp_path/$MEGAMEK_DEFAULT_JARNAME"; then
        MEGAMEK_CLASSPATH=$temp_path
    else
        if test -f "$PWD/$MEGAMEK_DEFAULT_JARNAME"; then
            MEGAMEK_CLASSPATH=$PWD
        fi
    fi
fi

# Exit if we could not find $MEGAMEK_DEFAULT_JARNAME.
if test -z "$MEGAMEK_CLASSPATH"; then
    echo "Could not find the MegaMek JAR file.  Exiting."
    exit 1
fi

# Try to find the configuration directory.
#  MEGAMEK_CONFPATH may be set before hand, which is not desired.
if ! test -z "$MEGAMEK_CONFPATH"; then
    if ! test -d "/.$MEGAMEK_CONFPATH" -a \
        -f "/.$MEGAMEK_CONFPATH/$MEGAMEK_DEFAULT_JARNAME"; then
        echo "Relative path: $MEGAMEK_CONFPATH.  Clearing."
        MEGAMEK_CONFPATH=""
    fi
fi
if test -z "$MEGAMEK_CONFPATH"; then
    if test -w $PWD; then
        MEGAMEK_CONFPATH=$PWD
    fi
fi

# Exit if we could not find configuration directory.
if test -z "$MEGAMEK_CONFPATH"; then
    echo "Could not find the MegaMek config files.  Exiting."
    exit 2
fi

# Try to find the data directory.
#  MEGAMEK_DATAPATH may be set before hand, which is not desired.
if ! test -z "$MEGAMEK_DATAPATH"; then
    if ! test -d "/.$MEGAMEK_DATAPATH" -a \
        -f "/.$MEGAMEK_DATAPATH/$MEGAMEK_DEFAULT_JARNAME"; then
        echo "Relative path: $MEGAMEK_DATAPATH.  Clearing."
        MEGAMEK_DATAPATH=""
    fi
fi
if test -z "$MEGAMEK_DATAPATH"; then
    temp_path=$MEGAMEK_CONFPATH
    if test -d $temp_path -a \
        -d "$temp_path/data"; then
        MEGAMEK_DATAPATH=$temp_path
    else
        temp_path=$MEGAMEK_DEFAULT_DATAPATH
        if test -d $temp_path -a \
            -d "$temp_path/data"; then
            MEGAMEK_DATAPATH=$temp_path
        else
            if test -d "$PWD/data"; then
                MEGAMEK_DATAPATH=$PWD
            fi
        fi
    fi
fi

# Exit if we could not find data directory.
if test -z "$MEGAMEK_DATAPATH"; then
    echo "Could not find the MegaMek data files.  Exiting."
    exit 3
fi

# Try to link the data directory into the
# configuration directory, if it does not exist.
if ! test -d $MEGAMEK_CONFPATH/data -o \
    -L $MEGAMEK_CONFPATH/data; then
    echo "Linking the MegaMek data directory to $MEGAMEK_CONFPATH/data."
    ln -s $MEGAMEK_DATAPATH/data $MEGAMEK_CONFPATH/data

    # Exit if we could not link in the data directory.
    if ! test -L $MEGAMEK_CONFPATH/data; then
        echo "Could not link to $MEGAMEK_CONFPATH/data.  Exiting."
        exit 4
    fi
fi

# Try to link the docs directory into the
# configuration directory, if it does not exist.
if ! test -d $MEGAMEK_CONFPATH/docs -o \
    -L $MEGAMEK_CONFPATH/docs; then
    echo "Linking the MegaMek docs directory to $MEGAMEK_CONFPATH/docs."
    ln -s $MEGAMEK_DATAPATH/docs $MEGAMEK_CONFPATH/docs

    # Exit if we could not link in the data directory.
    if ! test -L $MEGAMEK_CONFPATH/docs; then
        echo "Could not link to $MEGAMEK_CONFPATH/data.  Exiting."
        exit 4
    fi
fi

# Try to create the configuration subdirectories if necessary.
for subdir in logs mmconf savegames; do
    if ! test -d "$MEGAMEK_CONFPATH/$subdir"; then
        mkdir $MEGAMEK_CONFPATH/$subdir
    fi
done

# Build a classpath containing all JARs.
RUNPATH=`ls -1 $MEGAMEK_CLASSPATH/*.jar | awk 'BEGIN {ORS=":"} {print $1}' | sed 's/:$//'`

# Check for Java 9, and set additional modules
# Java 9 may report version as 9.x, instead of 1.8
# We'll just test both versions to see if they are greater than 8
JAVA_VERSION1=`java -version 2>&1 | grep "version" | awk '{print $3}' | tr -d \" | awk '{split($0, array, ".")} END{print array[1]}'`
JAVA_VERSION2=`java -version 2>&1 | grep "version" | awk '{print $3}' | tr -d \" | awk '{split($0, array, ".")} END{print array[2]}'`
if [ $JAVA_VERSION1 -gt 8 -o $JAVA_VERSION2 -gt 8 ]; then
    JAVA_MODULES="--add-modules java.se.ee"
else
    JAVA_MODULES=""
fi

# Run MegaMek.
if ! test $PWD -ef $MEGAMEK_CONFPATH; then
    echo "Switching directory to $MEGAMEK_CONFPATH."
    cd $MEGAMEK_CONFPATH
fi
#Change the number in -Xmx to the amount of memory available to MegaMek
$JAVA $JAVA_MODULES -Xmx1024m -classpath $RUNPATH $MEGAMEK_MAIN_CLASS $@
