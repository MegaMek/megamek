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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

class BattlefieldSupportAssetReadoutTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void factoryCreatesAssetReadout() {
        assertInstanceOf(BattlefieldSupportAssetReadout.class,
              EntityReadout.createReadout(testAsset(), false, false));
    }

    @Test
    void rendersAssetStatsWithoutConstructionRowsInEveryFormat() {
        EntityReadout readout = EntityReadout.createReadout(testAsset(), false, false);

        for (ViewFormatting formatting : ViewFormatting.values()) {
            String output = readout.getFullReadout(null, formatting);
            assertTrue(output.contains("Battlefield Support Asset"));
            assertTrue(output.contains("Asset Type"));
            assertTrue(output.contains("Movement"));
            assertTrue(output.contains("TMM"));
            assertTrue(output.contains("Range"));
            assertTrue(output.contains("Damage"));
            assertTrue(output.contains("Destroy Check"));
            assertFalse(output.contains("Weight:"));
            assertFalse(output.contains("Engine:"));
            assertFalse(output.contains("Armor:"));
        }

        String plainText = readout.getFullReadout(null, ViewFormatting.NONE);
        assertTrue(plainText.contains("8H"));
        assertTrue(plainText.contains("+3"));
        assertTrue(plainText.contains("3/6/9"));
        assertTrue(plainText.contains("5x4"));
        assertTrue(plainText.contains("7 -> 5"));
        assertTrue(plainText.contains("APC1, IF2"));
        assertTrue(plainText.contains("23(27)"));
        assertTrue(plainText.contains("460(540)"));
    }

    private static BattlefieldSupportAsset testAsset() {
        BattlefieldSupportAssetData data = new BattlefieldSupportAssetData();
        data.setChassis("Test Hover Transport");
        data.setModel("BFS");
        data.setAssetType(BFSAssetType.VEHICLE);
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
        asset.setVeteranCrew(true);
        asset.setDestroyCheck(5);
        return asset;
    }
}
