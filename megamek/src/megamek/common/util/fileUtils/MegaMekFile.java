/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util.fileUtils;

import java.io.File;

import megamek.common.Configuration;

/**
 * This is a local MegaMek version of java.io.File and is designed to support
 * having files that could exist in two locations: the MM install location and
 * a userdata directory. When a file is opened, the path is first checked to
 * see if it exists within the userdata directory, and if it does, that file is
 * opened. However, if it doesn't exist, then the file is opened from MM's
 * install directory instead.
 *
 * @author arlith
 *
 */
public class MegaMekFile {

    File file;

    public MegaMekFile(File parent, String child) {
        this(new File(parent, child).toString());
    }

    public MegaMekFile(String pathname) {
        File userdataVersion = new File(Configuration.userdataDir(), pathname);
        if (userdataVersion.exists()) {
            file = userdataVersion;
        } else {
            file = new File(pathname);
        }
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.toString();
    }

}
