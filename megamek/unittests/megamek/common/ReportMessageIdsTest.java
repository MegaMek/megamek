/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * Catalyst Game Labs and the Catalyst Game Labs logo are tradema oductions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The report message file is loaded as a properties file, where a repeated key silently wins over the earlier one. A
 * new report that reuses an id another report already has therefore prints the wrong sentence with the wrong fields,
 * and nothing warns about it. This test fails instead.
 */
class ReportMessageIdsTest {

    private static final String REPORT_MESSAGES = "/megamek/common/report-messages.properties";

    @Test
    void testNoReportIdIsDefinedTwice() throws IOException {
        Set<String> seenIds = new HashSet<>();
        List<String> duplicateIds = new ArrayList<>();
        InputStream stream = ReportMessageIdsTest.class.getResourceAsStream(REPORT_MESSAGES);
        assertTrue(stream != null, "the report message file is on the classpath");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                int separator = line.indexOf('=');
                boolean isEntry = (separator > 0) && !line.startsWith("#") && !line.startsWith("!");
                if (isEntry) {
                    String key = line.substring(0, separator).trim();
                    if (!seenIds.add(key)) {
                        duplicateIds.add(key);
                    }
                }
                line = reader.readLine();
            }
        }
        assertTrue(duplicateIds.isEmpty(),
              "these report ids are defined more than once, so the later text overwrites the earlier report: "
                    + duplicateIds);
    }
}
