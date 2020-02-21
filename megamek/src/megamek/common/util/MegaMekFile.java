/*
 * Copyright (c) 2016, 2020 MekHQ Team. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util;

import java.io.File;

import megamek.common.Configuration;

/**
 * This is a local MegaMek version of java.io.File and is designed to support
 * having files that could exist in two locations: the MM install location and
 * a userData directory.  When a file is opened, the path is first checked to
 * see if it exists within the userData directory, and if it does, that file is
 * opened.  However, if it doesn't exist, then the file is opened from MM's
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
    
    public MegaMekFile(String pathName) {
        File userDataVersion = new File(Configuration.userdataDir(), pathName);
        if (userDataVersion.exists()) {
            file = userDataVersion;
        } else {
            file = new File(pathName);
        }
    }
    
    public File getFile() {
        return file;
    }
    
    public String toString() {
        return file.toString();
    }
}
