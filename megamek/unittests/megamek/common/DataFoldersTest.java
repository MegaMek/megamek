/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import megamek.common.DataFolders.DataFolder;
import megamek.common.DataFolders.Layer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataFoldersTest {

    @TempDir
    File root;

    private File officialData;
    private File userDir;
    private File userDirData;
    private File userData;
    private File userDataData;

    @BeforeEach
    void setUp() {
        officialData = new File(root, "install/data");
        userDir = new File(root, "My Games/MegaMek");
        userDirData = new File(userDir, DataFolders.DATA_DIRECTORY_NAME);
        userData = new File(root, "install/userdata");
        userDataData = new File(userData, DataFolders.DATA_DIRECTORY_NAME);
    }

    private static void createDirectories(File... directories) {
        for (File directory : directories) {
            assertTrue(directory.mkdirs(), "Could not create " + directory);
        }
    }

    @Test
    void loadOrderGoesFromOfficialToUserDirToUserData() {
        createDirectories(officialData, userDirData, userDataData);

        List<DataFolder> folders = DataFolders.loadOrder(officialData, userDir.getPath(), userData);

        assertEquals(List.of(new DataFolder(Layer.OFFICIAL, officialData),
              new DataFolder(Layer.USER_DIR, userDirData),
              new DataFolder(Layer.USER_DATA, userDataData)), folders);
    }

    @Test
    void lookupOrderIsTheReverseOfTheLoadOrder() {
        createDirectories(officialData, userDirData, userDataData);

        List<DataFolder> folders = DataFolders.reversed(DataFolders.loadOrder(officialData,
              userDir.getPath(),
              userData));

        assertEquals(List.of(Layer.USER_DATA, Layer.USER_DIR, Layer.OFFICIAL),
              folders.stream().map(DataFolder::layer).toList());
    }

    @Test
    void unsetUserDirIsLeftOut() {
        createDirectories(officialData, userDirData, userDataData);

        assertEquals(List.of(Layer.OFFICIAL, Layer.USER_DATA),
              DataFolders.loadOrder(officialData, null, userData).stream().map(DataFolder::layer).toList());
        assertEquals(List.of(Layer.OFFICIAL, Layer.USER_DATA),
              DataFolders.loadOrder(officialData, "  ", userData).stream().map(DataFolder::layer).toList());
    }

    @Test
    void missingFoldersAreLeftOut() {
        createDirectories(officialData, userDir);

        assertEquals(List.of(new DataFolder(Layer.OFFICIAL, officialData)),
              DataFolders.loadOrder(officialData, userDir.getPath(), userData));
    }

    @Test
    void userDirInsideTheInstallationIsOnlyListedOnce() {
        createDirectories(officialData, userDataData);
        File installation = officialData.getParentFile();

        assertEquals(List.of(Layer.OFFICIAL, Layer.USER_DATA),
              DataFolders.loadOrder(officialData, installation.getPath(), userData)
                    .stream()
                    .map(DataFolder::layer)
                    .toList());
    }

    @Test
    void resolveIsRelativeToTheDataFolder() {
        DataFolder folder = new DataFolder(Layer.USER_DIR, userDirData);

        assertEquals(new File(userDirData, "unitfluff"), folder.resolve("unitfluff"));
    }
}
