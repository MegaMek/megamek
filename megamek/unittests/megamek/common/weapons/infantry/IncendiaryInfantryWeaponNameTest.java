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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guards the TechManual pp. 350-352 errata rename of the conventional infantry incendiary weapons.
 *
 * <p>The errata replaces every use of "Inferno" with "Incendiary" on the conventional infantry weapon tables, so
 * that flame-based support weapons are not confused with true Inferno munitions. These weapons are renamed for
 * display, but their pre-errata names must keep resolving: unit files and saved games written before the rename
 * still refer to them.</p>
 *
 * <p>The SRM launchers that carry real Inferno ammo are handled differently, because the errata deletes those rows
 * rather than renaming them. The light and heavy versions had no users at all and are withdrawn. The two-shot
 * version stays registered and keeps its name: a stock unit mounts it, and player-built units and saved games may
 * too. Unlike the incendiary weapons, which only convert damage to heat, it carries true Inferno munitions.</p>
 */
class IncendiaryInfantryWeaponNameTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static Stream<Arguments> renamedWeapons() {
        return Stream.of(
              Arguments.of("Grenade (Inferno)", "Grenade (Incendiary)"),
              Arguments.of("Grenade (Mini) (Inferno)", "Grenade (Mini) (Incendiary)"),
              Arguments.of("Grenade (Non-Inferno)", "Grenade (Non-Incendiary)"),
              Arguments.of("Laser Rifle (Mauser IIC IAS) (Inferno Grenades)",
                    "Laser Rifle (Mauser IIC IAS) (Incendiary Grenades)"),
              Arguments.of("Rifle (Federated-Barrett M42B) (Inferno Grenades)",
                    "Rifle (Federated-Barrett M42B) (Incendiary Grenades)"),
              Arguments.of("Laser Rifle (Federated-Barrett M61A) (Inferno Grenades)",
                    "Laser Rifle (Federated-Barrett M61A) (Incendiary Grenades)"),
              Arguments.of("LRM Launcher (Corean Farshot) w/Inferno", "LRM Launcher (Corean Farshot) w/Incendiary"),
              Arguments.of("MRM Launcher w/Inferno", "MRM Launcher w/Incendiary"),
              Arguments.of("Grenade Launcher (Auto) - Inferno", "Grenade Launcher (Auto) - Incendiary"),
              Arguments.of("Grenade Launcher (Heavy Auto) w/Inferno", "Grenade Launcher (Heavy Auto) w/Incendiary"),
              Arguments.of("Grenade Launcher - Inferno", "Grenade Launcher - Incendiary"),
              Arguments.of("Grenade Launcher (Heavy) w/Inferno", "Grenade Launcher (Heavy) w/Incendiary"),
              Arguments.of("Mortar (Heavy) - Inferno", "Mortar (Heavy) - Incendiary"),
              Arguments.of("Mortar (Light) - Inferno", "Mortar (Light) - Incendiary"),
              Arguments.of("Recoilless Rifle (Heavy) - Inferno", "Recoilless Rifle (Heavy) - Incendiary"),
              Arguments.of("Recoilless Rifle (Light) - Inferno", "Recoilless Rifle (Light) - Incendiary"),
              Arguments.of("Recoilless Rifle (Medium) - Inferno", "Recoilless Rifle (Medium) - Incendiary"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("renamedWeapons")
    @DisplayName("The pre-errata name still resolves to the renamed weapon")
    void preErrataNameStillResolves(String preErrataName, String errataName) {
        EquipmentType byOldName = EquipmentType.get(preErrataName);
        EquipmentType byNewName = EquipmentType.get(errataName);

        assertNotNull(byOldName, "Unit files written before the rename still use \"" + preErrataName
              + "\", so it must stay resolvable");
        assertNotNull(byNewName, "The renamed weapon must resolve under its errata name \"" + errataName + "\"");
        assertSame(byOldName, byNewName, "Both names must resolve to the same weapon");
        assertEquals(errataName, byOldName.getName(), "The weapon should display its errata name");
    }

    @Test
    @DisplayName("The two-shot Inferno SRM launcher stays registered and keeps its name")
    void infernoSrmLauncherIsNotRenamed() {
        EquipmentType launcher = EquipmentType.get("InfantryStandardSRMInferno");

        assertNotNull(launcher, "A stock unit mounts this launcher, so withdrawing it would break unit files");
        // Pinned to the exact display name rather than to a substring: the test's whole claim is that this
        // launcher is NOT renamed, and a substring check would sail through a partial rename that happened to
        // keep the word Inferno in it.
        assertEquals("SRM Launcher (Std, Two-Shot) - Inferno", launcher.getName(),
              "This launcher carries true Inferno munitions and is not part of the incendiary rename");
    }

    @Test
    @DisplayName("The unused Inferno SRM launchers are withdrawn per the errata")
    void unusedInfernoSrmLaunchersAreNotRegistered() {
        String[] withdrawnLaunchers = { "InfantryHeavySRMInferno", "InfantrySRMLightInferno" };

        for (String internalName : withdrawnLaunchers) {
            assertNull(EquipmentType.get(internalName),
                  "The errata deletes this row and no unit file mounts it, so it should no longer be "
                        + "registered: " + internalName);
        }
    }
}
