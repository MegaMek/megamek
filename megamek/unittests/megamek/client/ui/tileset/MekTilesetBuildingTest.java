/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.client.ui.tileset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import megamek.common.enums.BuildingType;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.units.BuildingEntity;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MekTilesetBuildingTest {
    @TempDir
    Path directory;

    @Test
    void buildingsAndHandheldWeaponsUseTheirOwnDefaultSprites() throws IOException {
        Files.writeString(directory.resolve("mekset.txt"), """
              exact "default_gun_emplacement" "emplacement.png"
              exact "default_hhw" "handheld.png"
              """);
        MekTileset tileset = new MekTileset(directory.toFile());
        tileset.loadFromFile("mekset.txt");

        assertEquals("emplacement.png", tileset.genericFor(
              new BuildingEntity(BuildingType.HEAVY, IBuilding.GUN_EMPLACEMENT), -1).getImageFile());
        assertEquals("handheld.png", tileset.genericFor(new HandheldWeapon(), -1).getImageFile());
    }
}
