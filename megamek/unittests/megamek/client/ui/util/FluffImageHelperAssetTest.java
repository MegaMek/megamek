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
 */

package megamek.client.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FluffImageHelperAssetTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static BattlefieldSupportAsset asset(BFSAssetType type) {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setAssetType(type);
        return asset;
    }

    @Test
    void assetsResolveToAssetFolderSingularPath() {
        assertEquals(FluffImageHelper.DIR_NAME_ASSET, FluffImageHelper.getFluffPath(asset(BFSAssetType.VEHICLE)));
    }

    @Test
    void vehicleAssetSearchesAssetThenVehicleFolder() {
        List<String> paths = FluffImageHelper.getFluffPaths(asset(BFSAssetType.VEHICLE));
        assertEquals(List.of(FluffImageHelper.DIR_NAME_ASSET, FluffImageHelper.DIR_NAME_VEHICLE), paths);
    }

    @Test
    void infantryAssetSearchesAssetThenInfantryFolder() {
        List<String> paths = FluffImageHelper.getFluffPaths(asset(BFSAssetType.CONV_INFANTRY));
        assertEquals(List.of(FluffImageHelper.DIR_NAME_ASSET, FluffImageHelper.DIR_NAME_INFANTRY), paths);
    }

    @Test
    void battleArmorAssetSearchesAssetThenBattleArmorFolder() {
        List<String> paths = FluffImageHelper.getFluffPaths(asset(BFSAssetType.BATTLE_ARMOR));
        assertEquals(List.of(FluffImageHelper.DIR_NAME_ASSET, FluffImageHelper.DIR_NAME_BA), paths);
    }

    @Test
    void emplacementAssetSearchesOnlyAssetFolder() {
        List<String> paths = FluffImageHelper.getFluffPaths(asset(BFSAssetType.EMPLACEMENT));
        assertEquals(List.of(FluffImageHelper.DIR_NAME_ASSET), paths);
        assertTrue(paths.contains(FluffImageHelper.DIR_NAME_ASSET));
        assertFalse(paths.contains(FluffImageHelper.DIR_NAME_VEHICLE));
    }
}
