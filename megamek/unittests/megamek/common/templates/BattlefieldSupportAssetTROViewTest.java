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
package megamek.common.templates;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.util.ViewFormatting;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BFSDamage;
import megamek.common.battlefieldSupport.BFSRange;
import megamek.common.battlefieldSupport.BFSSpecial;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.BattlefieldSupportAssetData;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BattlefieldSupportAssetTROViewTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void dispatchesAndRendersHtmlWithNarrativeFluff() {
        TROView view = TROView.createView(testAsset(), ViewFormatting.HTML);

        assertInstanceOf(BattlefieldSupportAssetTROView.class, view);
        String output = view.processTemplate();
        assertNotNull(output);
        assertTrue(output.contains("Test overview"));
        assertAssetStats(output);
    }

    @Test
    void dispatchesAndRendersPlainTextWithNarrativeFluff() {
        TROView view = TROView.createView(testAsset(), ViewFormatting.NONE);

        assertInstanceOf(BattlefieldSupportAssetTROView.class, view);
        String output = view.processTemplate();
        assertNotNull(output);
        assertTrue(output.contains("Test overview"));
        assertAssetStats(output);
    }

    private static void assertAssetStats(String output) {
        assertTrue(output.contains("Asset Type:"));
        assertTrue(output.contains("Vehicle"));
        assertTrue(output.contains("Movement:"));
        assertTrue(output.contains("8H"));
        assertTrue(output.contains("Destroy Check:"));
        assertTrue(output.contains("7 -> 5") || output.contains("7 -&gt; 5"));
        assertTrue(output.contains("BSP Cost:"));
        assertTrue(output.contains("23(27)"));
        assertTrue(output.contains("BFS BV:"));
        assertTrue(output.contains("460(540)"));
        assertTrue(output.contains("APC1, IF2"));
    }

    private static BattlefieldSupportAsset testAsset() {
        BattlefieldSupportAssetData data = new BattlefieldSupportAssetData();
        data.setChassis("Test Hover Transport");
        data.setModel("BFS");
        data.setAssetType(BFSAssetType.VEHICLE);
        data.setSource("BattleTech: Mercenaries");
        data.setMp(8);
        data.setMovementMode(EntityMovementMode.HOVER);
        data.setTmm(3);
        data.setRange(new BFSRange(3, 6, 9));
        data.setSkill(6);
        data.setVeteranSkill(5);
        data.setDamage(new BFSDamage(5, 4));
        data.setDestroyCheck(7);
        data.setThreshold(5);
        data.setCost(23);
        data.setVeteranCost(27);
        data.setSpecials(List.of(BFSSpecial.of("APC", 1), BFSSpecial.of("IF", 2)));
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset(data);
        asset.getFluff().setOverview("Test overview");
        asset.setVeteranCrew(true);
        asset.setDestroyCheck(5);
        return asset;
    }
}
