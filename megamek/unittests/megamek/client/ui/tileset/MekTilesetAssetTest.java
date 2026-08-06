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

package megamek.client.ui.tileset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the mek tileset resolves a sensible generic sprite for each Battlefield Support Asset type, so an
 * asset with no chassis/exact sprite still gets a top-down icon of the right kind (tank, hover, VTOL, infantry, battle
 * armor, gun emplacement).
 */
class MekTilesetAssetTest {

    private static MekTileset tileset;

    @TempDir
    private static Path tilesetDirectory;

    @BeforeAll
    static void beforeAll() throws IOException {
        EquipmentType.initializeTypes();
        Files.writeString(tilesetDirectory.resolve("mekset.txt"), """
              exact "default_tracked" "tracked.png"
              exact "default_wheeled" "wheeled.png"
              exact "default_hover" "hover.png"
              exact "default_vtol" "vtol.png"
              exact "default_infantry" "infantry.png"
              exact "default_ba" "ba.png"
              exact "default_gun_emplacement" "emplacement.png"
              """);
        tileset = new MekTileset(tilesetDirectory.toFile());
        tileset.loadFromFile("mekset.txt");
    }

    private static BattlefieldSupportAsset asset(BFSAssetType type, EntityMovementMode mode) {
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setAssetType(type);
        asset.setMovementMode(mode);
        return asset;
    }

    private String genericImageFor(BFSAssetType type, EntityMovementMode mode) {
        MekTileset.MekEntry entry = tileset.genericFor(asset(type, mode), -1);
        assertNotNull(entry);
        return entry.getImageFile().toLowerCase();
    }

    @Test
    void hoverVehicleResolvesToHoverSprite() {
        assertTrue(genericImageFor(BFSAssetType.VEHICLE, EntityMovementMode.HOVER).contains("hover"));
    }

    @Test
    void vtolVehicleResolvesToVtolSprite() {
        assertTrue(genericImageFor(BFSAssetType.VEHICLE, EntityMovementMode.VTOL).contains("vtol"));
    }

    @Test
    void wheeledVehicleResolvesToWheeledSprite() {
        assertTrue(genericImageFor(BFSAssetType.VEHICLE, EntityMovementMode.WHEELED).contains("wheeled"));
    }

    @Test
    void trackedVehicleResolvesToTrackedSprite() {
        assertTrue(genericImageFor(BFSAssetType.VEHICLE, EntityMovementMode.TRACKED).contains("tracked"));
    }

    @Test
    void infantryResolvesToInfantrySprite() {
        assertTrue(genericImageFor(BFSAssetType.CONV_INFANTRY, EntityMovementMode.INF_LEG).contains("inf"));
    }

    @Test
    void battleArmorResolvesToBattleArmorSprite() {
        assertTrue(genericImageFor(BFSAssetType.BATTLE_ARMOR, EntityMovementMode.INF_JUMP).contains("ba"));
    }

    @Test
    void emplacementResolvesToGunEmplacementSprite() {
        assertTrue(genericImageFor(BFSAssetType.EMPLACEMENT, EntityMovementMode.NONE).contains("emplacement"));
    }

    @Test
    void missingTilesetDefaultsReturnNoImage() {
        MekTileset emptyTileset = new MekTileset(tilesetDirectory.toFile());

        assertNull(emptyTileset.imageFor(asset(BFSAssetType.VEHICLE, EntityMovementMode.TRACKED)));
    }
}
