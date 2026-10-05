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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.weapons.infantry.support.srm.WithdrawnInfernoSrmLaunchers;
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
 * rather than renaming them: an SRM platoon declares Inferno munitions before the battle instead. The Inferno
 * launchers are withdrawn, and their names load as the matching plain SRM launcher so old unit files, MULs and saved
 * games still load.</p>
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

    private static Stream<Arguments> withdrawnInfernoSrmLaunchers() {
        return Stream.of(
              Arguments.of("InfantrySRMLightInferno", "InfantrySRMLight"),
              Arguments.of("SRM Launcher (Light) - Inferno", "InfantrySRMLight"),
              Arguments.of("Light SRM (Inferno)", "InfantrySRMLight"),
              Arguments.of("InfantryStandardSRMInferno", "InfantryStandardSRM"),
              Arguments.of("SRM Launcher (Std, Two-Shot) - Inferno", "InfantryStandardSRM"),
              Arguments.of("Infantry2ShotSRMInferno", "InfantryStandardSRM"),
              Arguments.of("Infantry Two-Shot SRM Launcher (Inferno)", "InfantryStandardSRM"),
              Arguments.of("InfantryHeavySRMInferno", "InfantryHeavySRM"),
              Arguments.of("SRM Launcher (Heavy) w/ Inferno", "InfantryHeavySRM"),
              Arguments.of("Infantry Heavy SRM Launcher (Inferno)", "InfantryHeavySRM"),
              Arguments.of("SRM Launcher (Hvy, One-Shot) w/ Inferno", "InfantryHeavySRM"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("withdrawnInfernoSrmLaunchers")
    @DisplayName("A withdrawn Inferno SRM launcher name loads as the plain launcher and is recognised as Inferno")
    void withdrawnInfernoSrmNameLoadsAsPlainLauncher(String withdrawnName, String plainInternalName) {
        EquipmentType byWithdrawnName = EquipmentType.get(withdrawnName);
        EquipmentType plainLauncher = EquipmentType.get(plainInternalName);

        assertNotNull(plainLauncher, "The plain launcher should be registered: " + plainInternalName);
        assertSame(plainLauncher, byWithdrawnName,
              "Old unit files and saves name \"" + withdrawnName + "\", which must load as the plain launcher");
        assertTrue(WithdrawnInfernoSrmLaunchers.isWithdrawnName(withdrawnName),
              "A unit file naming \"" + withdrawnName + "\" was built to carry Inferno munitions");
        assertFalse(WithdrawnInfernoSrmLaunchers.isWithdrawnName(plainLauncher.getName()),
              "The plain launcher's display name must not declare Inferno");
        assertFalse(WithdrawnInfernoSrmLaunchers.isWithdrawnName(plainInternalName),
              "The plain launcher's internal name must not declare Inferno");
    }

    @Test
    @DisplayName("Plain SRM launchers no longer offer small support vehicles an Inferno ammo split")
    void plainSrmLaunchersHaveNoInfernoAmmo() {
        // The withdrawn Inferno variants resolve to the plain launcher itself, so the Inferno variant lookup must
        // not mistake the launcher for its own Inferno version. A weapon that still has a real Inferno variant keeps
        // the split.
        for (String internalName : new String[] { "InfantrySRMLight", "InfantryStandardSRM", "InfantryHeavySRM" }) {
            InfantryWeapon launcher = (InfantryWeapon) EquipmentType.get(internalName);
            assertFalse(launcher.hasInfernoAmmo(), "The errata deletes Inferno SRM ammo: " + internalName);
        }
        InfantryWeapon recoillessRifle = (InfantryWeapon) EquipmentType.get("InfantryLRR");
        assertNotNull(recoillessRifle, "The light recoilless rifle should be registered");
        assertTrue(recoillessRifle.hasInfernoAmmo(), "Weapons with a real incendiary variant keep the ammo split");
    }
}
