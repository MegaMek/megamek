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

package megamek.common.weapons.infantry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guards backwards compatibility for the infantry weapon renames.
 *
 * <p>These weapons were renamed to the spelling used by the Infantry Weapons Calculator sheet, mostly to undo the
 * shouted capitalisation their Shrapnel entries were transcribed with. A saved game or a player's custom unit
 * written before the rename still names the weapon the old way, so every pre-rename name has to keep resolving,
 * and has to resolve to the same weapon as the new name.</p>
 *
 * <p>The Xing Shan ER is the awkward one. Its old name spelt Xing with a non-ASCII i, and MegaMek source is ASCII
 * only, so its lookup name is written as an escape rather than as the character itself.</p>
 */
class InfantryWeaponRenameCompatibilityTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static Stream<Arguments> renamedWeapons() {
        return Stream.of(
              Arguments.of("Sniper Rifle (Thors Hammer)", "Sniper Rifle (Thorshammer)"),
              Arguments.of("Laser Pistol (X\u012bng Shan ER)", "Laser Pistol (Xing Shan ER)"),
              Arguments.of("Laser Pistol (AWA Wiliby MK4 LASER PISTOL)",
                    "Laser Pistol (AWA Wiliby Mk4 Laser Pistol)"),
              Arguments.of("Laser Pistol (Kelvin 000 Lancer 3-MM)", "Laser Pistol (Kelvin 000 Lancer 3-mm)"),
              Arguments.of("Pulse Laser Pistol (RDI SunSwarm Pulsar)", "Pulse Laser Pistol (RDI Sunswarm Pulsar)"),
              Arguments.of("Pulse Laser Rifle (Gaul)", "Pulse Laser Rifle (GAUL)"),
              Arguments.of("Laser Pistol (BrightStar L-7)", "Laser Pistol (Brightstar L-7)"),
              Arguments.of("Laser Pistol (BrightStar L-12)", "Laser Pistol (Brightstar L-12)"),
              Arguments.of("Laser Pistol (BrightStar L-15)", "Laser Pistol (Brightstar L-15)"),
              Arguments.of("Laser Carbine (BrightStar L-15)", "Laser Carbine (Brightstar L-15)"),
              Arguments.of("Laser Pistol (Darklight IV)", "Laser Pistol (Darklight IV Laser Pistol)"),
              Arguments.of("Laser Rifle (Darklight-CL Light)", "Laser Rifle (Darklight-CL Light Laser Rifle)"),
              Arguments.of("Blazer Rifle (Scorcher VI)", "Laser Rifle (Scorcher VI Blazer Rifle)"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("renamedWeapons")
    @DisplayName("The pre-rename name still resolves to the renamed weapon")
    void preRenameNameStillResolves(String preRenameName, String currentName) {
        EquipmentType byOldName = EquipmentType.get(preRenameName);
        EquipmentType byNewName = EquipmentType.get(currentName);

        assertNotNull(byOldName, "A saved game or custom unit written before the rename still names this weapon \""
              + preRenameName + "\", so it must stay resolvable");
        assertNotNull(byNewName, "The weapon must resolve under its current name \"" + currentName + "\"");
        assertSame(byOldName, byNewName, "Both names must resolve to the same weapon");
        assertEquals(currentName, byOldName.getName(), "The weapon should display its current name");
    }
}
