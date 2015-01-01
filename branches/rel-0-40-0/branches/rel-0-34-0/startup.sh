#!/bin/sh

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
MEGAMEK_MAIN_CLASS="megamek.MegaMek"
MEGAMEK_DEFAULT_JARNAME="MegaMek.jar"
MEGAMEK_DEFAULT_CLASSPATH="/usr/share/java"
MEGAMEK_DEFAULT_CONFROOT="$HOME"
MEGAMEK_DEFAULT_CONFPATH="$MEGAMEK_DEFAULT_CONFROOT/.megamek"
MEGAMEK_DEFAULT_DATAPATH="/usr/share/MegaMek"


# Try to find the executable for Java.
JAVA=/usr/bin/java
test -x "$JAVA_HOME/bin/java" && JAVA="$JAVA_HOME/bin/java"

# Try to find the directory containing MegaMek JARs.
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
if ! test -z "$MEGAMEK_CONFPATH"; then
    if ! test -d "/.$MEGAMEK_CONFPATH" -a \
        -f "/.$MEGAMEK_CONFPATH/$MEGAMEK_DEFAULT_JARNAME"; then
        echo "Relative path: $MEGAMEK_CONFPATH.  Clearing."
        MEGAMEK_CONFPATH=""
    fi
fi
if test -z "$MEGAMEK_CONFPATH"; then
    temp_path=$MEGAMEK_DEFAULT_CONFPATH
    if test -d $temp_path; then
        MEGAMEK_CONFPATH=$temp_path
    else
        # See if we should create the default configuration directory.
        if test -d $MEGAMEK_DEFAULT_CONFROOT -a \
            -w $MEGAMEK_DEFAULT_CONFROOT; then
            mkdir $temp_path
            if test -d $temp_path; then
                MEGAMEK_CONFPATH=$temp_path
            fi
        fi

        # If MEGAMEK_CONFPATH is still not set, try the PWD.
        if test -z "$MEGAMEK_CONFPATH"; then
            if test -w $PWD; then
                MEGAMEK_CONFPATH=$PWD
            fi
        fi
    fi
fi

# Exit if we could not find configuration directory.
if test -z "$MEGAMEK_CONFPATH"; then
    echo "Could not find the MegaMek config files.  Exiting."
    exit 2
fi

# Try to find the data directory.
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

    # Copy all the readme files to the configuation directory.
    cp $MEGAMEK_DATAPATH/readme*.txt $MEGAMEK_CONFPATH/
fi

# Try to create the configuration subdirectories if necessary.
for subdir in logs mmconf savegames; do
    if ! test -d "$MEGAMEK_CONFPATH/$subdir"; then
        mkdir $MEGAMEK_CONFPATH/$subdir
    fi
done

# Build a classpath containing all JARs.
RUNPATH=`ls -1 $MEGAMEK_CLASSPATH/*.jar | awk 'BEGIN {ORS=":"} {print $1}' | sed 's/:$//'`

# Run MegaMek.
if ! test $PWD -ef $MEGAMEK_CONFPATH; then
    echo "Switching directory to $MEGAMEK_CONFPATH."
    cd $MEGAMEK_CONFPATH
fi
$JAVA -XmX256m -classpath $RUNPATH $MEGAMEK_MAIN_CLASS $@
