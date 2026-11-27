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
package megamek.utilities.ratdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import megamek.client.ratgenerator.FactionRecord;
import org.junit.jupiter.api.Test;

/**
 * Checks the exporter's command-line options and that it picks ratings the way MekHQ does.
 */
class RatDataExporterTest {

    @Test
    void defaultsCoverEveryEraWithRolesAndManifest() {
        RatDataExporter.ExportOptions options = RatDataExporter.ExportOptions.parse(new String[] { "--out", "data" });

        assertEquals(Path.of("data"), options.outputFolder());
        assertNull(options.fromYear());
        assertNull(options.toYear());
        assertFalse(options.includeTables());
        assertTrue(options.includeRoles());
        assertTrue(options.includeManifest());
    }

    @Test
    void yearRangeAndSwitchesAreRead() {
        RatDataExporter.ExportOptions options = RatDataExporter.ExportOptions.parse(new String[] {
              "--from", "3025", "--to", "3050", "--out", "out", "--tables", "--no-roles", "--no-manifest", "--build-id", "abc123" });

        assertEquals(3025, options.fromYear());
        assertEquals(3050, options.toYear());
        assertTrue(options.includeTables());
        assertFalse(options.includeRoles());
        assertFalse(options.includeManifest());
        assertEquals("abc123", options.buildId());
    }

    @Test
    void badArgumentsAreRefused() {
        assertThrows(IllegalArgumentException.class,
              () -> RatDataExporter.ExportOptions.parse(new String[] { "--from", "3025" }));
        assertThrows(IllegalArgumentException.class,
              () -> RatDataExporter.ExportOptions.parse(new String[] { "--out", "out", "--from", "soon" }));
        assertThrows(IllegalArgumentException.class,
              () -> RatDataExporter.ExportOptions.parse(new String[] { "--out", "out", "--from", "3050", "--to",
                                                                     "3025" }));
        assertThrows(IllegalArgumentException.class,
              () -> RatDataExporter.ExportOptions.parse(new String[] { "--out" }));
        assertThrows(IllegalArgumentException.class,
              () -> RatDataExporter.ExportOptions.parse(new String[] { "--out", "out", "--colour" }));
    }

    @Test
    void factionWithFullRatingSystemGetsEveryLevel() {
        FactionRecord faction = new FactionRecord("FS", "Federated Suns");
        faction.setRatings("F,D,C,B,A");

        assertEquals(List.of("F", "D", "C", "B", "A"), RatDataExporter.ratingsFor(faction));
    }

    @Test
    void factionLockedToOneRatingGetsOnlyThatLevel() {
        FactionRecord faction = new FactionRecord("AB", "Azami Brotherhood");
        faction.setRatings("D");

        assertEquals(List.of("D"), RatDataExporter.ratingsFor(faction));
    }
}
