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
package megamek.client.ui.entityreadout;

import megamek.client.ui.util.ViewFormatting;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests a selected few units of every general type of unit if the Entity Readout works without throwing an exception
 * and returns a non-empty string.
 */
class EntityReadoutExportTest {

    @BeforeAll
    static void setup() {
        Configuration.setDataDir(new File("testresources/data"));
        MekSummaryCache.getInstance();
        MekSummaryCache.refreshUnitData(true);
        EquipmentType.initializeTypes();
    }

    @Test
    void testExportToTextDoesNotThrow() {
        for (MekSummary ms : MekSummaryCache.getInstance().getAllMeks()) {
            assertDoesNotThrow(() -> {
                var entity = ms.loadEntity();
                EntityReadout readout = EntityReadout.createReadout(entity, true);
                readout.getFullReadout(ViewFormatting.NONE);
            }, "Export to Text fails for " + ms.getName());
        }
    }

    @Test
    void testExportToHTMLDoesNotThrow() {
        for (MekSummary ms : MekSummaryCache.getInstance().getAllMeks()) {
            assertDoesNotThrow(() -> {
                var entity = ms.loadEntity();
                EntityReadout readout = EntityReadout.createReadout(entity, true);
                readout.getFullReadout(ViewFormatting.HTML);
            }, "Export to HTML fails for " + ms.getName());
        }
    }

    @Test
    void testExportToDiscordDoesNotThrow() {
        for (MekSummary ms : MekSummaryCache.getInstance().getAllMeks()) {
            assertDoesNotThrow(() -> {
                var entity = ms.loadEntity();
                EntityReadout readout = EntityReadout.createReadout(entity, true);
                readout.getFullReadout(ViewFormatting.DISCORD);
            }, "Export to Discord fails for " + ms.getName());
        }
    }

    @Test
    void testExportNotEmpty() {
        for (MekSummary ms : MekSummaryCache.getInstance().getAllMeks()) {
            var entity = ms.loadEntity();
            EntityReadout readout = EntityReadout.createReadout(entity, true);
            assertFalse(readout.getFullReadout(ViewFormatting.NONE).isBlank());
            assertFalse(readout.getFullReadout(ViewFormatting.DISCORD).isBlank());
            assertFalse(readout.getFullReadout(ViewFormatting.HTML).isBlank());
        }
    }
}
