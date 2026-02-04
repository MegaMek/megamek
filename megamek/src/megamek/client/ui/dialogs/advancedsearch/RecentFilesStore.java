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
package megamek.client.ui.dialogs.advancedsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements a simple, not thread-safe recent-files-used storage. It is stored in a json file which must
 * be given as a constructor parameter.
 */
public final class RecentFilesStore {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private static final int MAX_ENTRIES = 10;

    private final Path jsonFile;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final LinkedList<String> recentFiles = new LinkedList<>();

    public RecentFilesStore(Path jsonFile) throws IOException {
        if (jsonFile == null) {
            throw new IllegalArgumentException("jsonFile cannot be null");
        }
        this.jsonFile = jsonFile;
        load();
    }

    /**
     * Adds or refreshes the given file as most-recently-used.
     *
     * @param file The "touched" file
     */
    public void touch(Path file) throws IOException {
        String path = file.normalize().toString();

        recentFiles.remove(path);     // de-duplicate
        recentFiles.addFirst(path);   // most recent first

        if (recentFiles.size() > MAX_ENTRIES) {
            recentFiles.removeLast(); // drop oldest
        }

        save();
    }

    /**
     * @return The list of most recent files with the most recent first in the list.
     */
    public List<Path> getRecentFiles() {
        List<Path> result = new ArrayList<>(recentFiles.size());
        for (String p : recentFiles) {
            result.add(Path.of(p));
        }
        return result;
    }

    // === Persistence ===

    private void load() throws IOException {
        if (!Files.exists(jsonFile)) {
            return;
        }

        List<String> loaded = mapper.readValue(jsonFile.toFile(), STRING_LIST_TYPE);

        recentFiles.clear();
        for (String p : loaded) {
            recentFiles.add(p);
            if (recentFiles.size() == MAX_ENTRIES) {
                return;
            }
        }
    }

    private void save() throws IOException {
        Files.createDirectories(jsonFile.getParent());
        mapper.writeValue(jsonFile.toFile(), recentFiles);
    }
}
