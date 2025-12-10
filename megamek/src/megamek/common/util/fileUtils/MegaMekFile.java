/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.util.fileUtils;

import java.io.File;

import megamek.common.Configuration;

/**
 * This is a local MegaMek version of java.io.File and is designed to support having files that could exist in two
 * locations: the MM install location and an userdata directory. When a file is opened, the path is first checked to see
 * if it exists within the userdata directory, and if it does, that file is opened. However, if it doesn't exist, then
 * the file is opened from MM's install directory instead.
 *
 * @author arlith
 */
public class MegaMekFile {

    File file;

    public MegaMekFile(File parent, String child) {
        this(new File(parent, child).toString());
    }

    public MegaMekFile(String pathname) {
        File userdataVersion = new File(Configuration.userDataDir(), pathname);
        if (userdataVersion.exists()) {
            file = userdataVersion;
        } else {
            file = new File(pathname);
        }
    }

    public boolean isDirectory() {
        return this.getFile().isDirectory();
    }

    public boolean isFile() {
        return this.getFile().isFile();
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.toString();
    }

}
