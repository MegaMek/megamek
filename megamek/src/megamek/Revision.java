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
package megamek;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import megamek.common.Configuration;

/**
 * This class contains static methods to obtain the current code revision
 * hashCode of all three programs.
 *
 * The code revision can be used to check out the precise point in the code when
 * available from a log file.
 *
 * In the IDE, revisions may not be fully available. E.g. when working in MML,
 * the MM directories are not
 * used.
 */
public final class Revision {

    /**
     * @return The current MM code revision (e.g.
     *         "eba3a49e1006e51b16db12446caf5e43f57a18a7"), or "Unknown"
     *         if not available.
     */
    public static String mmRevision() {
        return instance.mmRevision;
    }

    /**
     * @return The current MML code revision (e.g.
     *         "eba3a49e1006e51b16db12446caf5e43f57a18a7"), or "Unknown"
     *         if not available.
     */
    public static String mmlRevision() {
        return instance.mmlRevision;
    }

    /**
     * @return The current MHQ code revision (e.g.
     *         "eba3a49e1006e51b16db12446caf5e43f57a18a7"), or "Unknown"
     *         if not available.
     */
    public static String mhqRevision() {
        return instance.mhqRevision;
    }

    // region PRIVATE
    private static final String UNKNOWN = "Unknown";

    private static final File MM_REVISION_FILE = new File(Configuration.docsDir(), "mm-revision.txt");
    private static final File MML_REVISION_FILE = new File(Configuration.docsDir(), "mml-revision.txt");
    private static final File MHQ_REVISION_FILE = new File(Configuration.docsDir(), "mhq-revision.txt");

    private static final Revision instance = new Revision();

    private final String mmRevision;
    private final String mmlRevision;
    private final String mhqRevision;

    private Revision() {
        mmRevision = loadRevision(MM_REVISION_FILE);
        mmlRevision = loadRevision(MML_REVISION_FILE);
        mhqRevision = loadRevision(MHQ_REVISION_FILE);
    }

    private String loadRevision(File revisionFile) {
        try {
            return Files.readAllLines(revisionFile.toPath()).get(0);
        } catch (IOException e) {
            return UNKNOWN;
        }
    }
    // endregion
}
