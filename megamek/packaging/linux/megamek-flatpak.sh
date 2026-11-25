#!/bin/sh

# Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
#
# This file is part of MegaMek.
#
# MegaMek is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License (GPL),
# version 3 or (at your option) any later version,
# as published by the Free Software Foundation.
#
# MegaMek is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty
# of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License for more details.
#
# A copy of the GPL should have been included with this project;
# if not, see <https://www.gnu.org/licenses/>.
#
# NOTICE: The MegaMek organization is a non-profit group of volunteers
# creating free software for the BattleTech community.
#
# MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
# of The Topps Company, Inc. All Rights Reserved.
#
# Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
# InMediaRes Productions, LLC.
#
# MechWarrior Copyright Microsoft Corporation. MegaMek was created under
# Microsoft's "Game Content Usage Rules"
# <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
# affiliated with Microsoft.

# Starts MegaMek from a read-only install, such as the Flathub package.
#
# MegaMek finds mmconf, logs, savegames, userdata and data relative to the folder it starts in, and
# writes to several of them: settings to mmconf, logs to logs, and the unit cache to
# data/mekfiles/units.cache. A Flatpak installs the game read-only under /app, so this script builds
# a writable game folder in the user's data directory and starts MegaMek there:
#
#   mmconf               copied, once per file, so the player's own changes are kept
#   logs, savegames,     created empty
#   userdata
#   data/mekfiles        a real folder whose entries link to the install, so units.cache can be written
#   everything else      links to the install, so the game data is not copied
#
# It runs every start, so a game update adds new files and drops links to removed ones. The game is
# then started through the install's own bin/MegaMek, which carries the JVM options.
#
# MEGAMEK_INSTALL_DIR and MEGAMEK_USER_DIR override the two folders, for testing.

set -eu

install_dir="${MEGAMEK_INSTALL_DIR:-/app/share/megamek}"
user_dir="${MEGAMEK_USER_DIR:-${XDG_DATA_HOME:-$HOME/.local/share}/megamek}"

# Folders the game writes to, so they are never links into the read-only install.
writable_folders="data mmconf logs savegames userdata"

is_writable_folder() {
    for writable_folder in $writable_folders; do
        if [ "$1" = "$writable_folder" ]; then
            return 0
        fi
    done
    return 1
}

# Removes links whose target no longer exists, after a game update dropped a file.
remove_broken_links() {
    for entry in "$1"/* "$1"/.[!.]*; do
        if [ -L "$entry" ] && [ ! -e "$entry" ]; then
            rm -f "$entry"
        fi
    done
}

# Links every entry of a source folder into a target folder, except names in the skip list. A real
# file or folder already in the target is left alone, so the player's own files win.
link_entries() {
    source_folder="$1"
    target_folder="$2"
    skip_check="$3"
    for source_entry in "$source_folder"/* "$source_folder"/.[!.]*; do
        if [ ! -e "$source_entry" ]; then
            continue
        fi
        entry_name="$(basename "$source_entry")"
        if [ -n "$skip_check" ] && "$skip_check" "$entry_name"; then
            continue
        fi
        target_entry="$target_folder/$entry_name"
        if [ -e "$target_entry" ] && [ ! -L "$target_entry" ]; then
            continue
        fi
        ln -sfn "$source_entry" "$target_entry"
    done
    remove_broken_links "$target_folder"
}

is_mekfiles() {
    [ "$1" = "mekfiles" ]
}

# Copies the default settings files that the player does not have yet. Existing files are never
# overwritten. Copies are made writable, as the install's files are read-only.
copy_missing_settings() {
    (
        cd "$install_dir/mmconf"
        find . -type d | while IFS= read -r settings_folder; do
            mkdir -p "$user_dir/mmconf/$settings_folder"
        done
        find . -type f | while IFS= read -r settings_file; do
            if [ ! -e "$user_dir/mmconf/$settings_file" ]; then
                cp "$settings_file" "$user_dir/mmconf/$settings_file"
                chmod u+w "$user_dir/mmconf/$settings_file"
            fi
        done
    )
}

if [ ! -d "$install_dir" ]; then
    echo "MegaMek install folder not found: $install_dir" >&2
    exit 1
fi

mkdir -p "$user_dir/data/mekfiles" "$user_dir/mmconf" "$user_dir/logs" "$user_dir/savegames" \
      "$user_dir/userdata"

link_entries "$install_dir" "$user_dir" is_writable_folder
link_entries "$install_dir/data" "$user_dir/data" is_mekfiles
link_entries "$install_dir/data/mekfiles" "$user_dir/data/mekfiles" ""
copy_missing_settings

# The Flathub package installs Java at /app/jre; bin/MegaMek uses JAVA_HOME to find it.
if [ -z "${JAVA_HOME:-}" ] && [ -x /app/jre/bin/java ]; then
    JAVA_HOME=/app/jre
    export JAVA_HOME
fi

cd "$user_dir"
exec "$install_dir/bin/MegaMek" "$@"
