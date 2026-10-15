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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * A MUL entry's unit-file UUID normally decides which unit file it loads. When the entry's name matches a different
 * unit file, the UUID is a shared or stale one (such as an older custom refit that kept its source design's UUID), and
 * the named unit should load instead.
 */
class MULParserUnitLookupTest {

    private static MekSummary summaryFor(String fileName) {
        MekSummary summary = new MekSummary();
        summary.setSourceFile(new File(fileName));
        return summary;
    }

    @Test
    void nameMatchingNothingKeepsUUIDLookup() {
        assertFalse(MULParser.nameIdentifiesOtherUnit(summaryFor("Charger CGR-1A5.mtf"), null),
              "A unit renamed since the MUL was written still loads by UUID");
    }

    @Test
    void nameMatchingSameFileKeepsUUIDLookup() {
        assertFalse(MULParser.nameIdentifiesOtherUnit(summaryFor("Charger CGR-1A5.mtf"),
              summaryFor("Charger CGR-1A5.mtf")));
    }

    @Test
    void nameMatchingOtherFileOverridesSharedUUID() {
        assertTrue(MULParser.nameIdentifiesOtherUnit(summaryFor("Charger CGR-1A5.mtf"),
              summaryFor("customs/Campaign/Charger CGR-1A5 Mk II.mtf")),
              "A custom that shares its source design's UUID loads by its own name");
    }

    @Test
    void nameMatchingOtherArchiveEntryOverridesSharedUUID() {
        MekSummary uuidSummary = summaryFor("units.zip");
        uuidSummary.setEntryName("Condor Heavy Hover Tank.blk");
        MekSummary namedSummary = summaryFor("units.zip");
        namedSummary.setEntryName("Condor Heavy Hover Tank Cargo.blk");

        assertTrue(MULParser.nameIdentifiesOtherUnit(uuidSummary, namedSummary));
    }

    @Test
    void assetFoundByUUIDIsNeverReplacedByItsBaseUnit() {
        MekSummary assetSummary = summaryFor("Charger CGR-1A5.bfs");
        assetSummary.setEntityType(Entity.ETYPE_BATTLEFIELD_SUPPORT_ASSET);

        assertFalse(MULParser.nameIdentifiesOtherUnit(assetSummary, summaryFor("Charger CGR-1A5.mtf")),
              "An asset shares its name with its base unit, so the UUID decides");
    }
}
