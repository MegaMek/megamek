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
package megamek.common.weapons.infantry.support.srm;

import java.util.List;
import java.util.Locale;

import megamek.common.annotations.Nullable;

/**
 * Names of the conventional infantry Inferno SRM launchers withdrawn by the TechManual pp. 350-352 errata.
 *
 * <p>The errata deletes the SRM Launcher (Inferno Ammo) rows: an SRM platoon declares Inferno munitions before the
 * battle instead (see {@link megamek.common.units.ConvInfantry#setInfernoSrmsDeclared(boolean)}). Unit files, MULs,
 * campaigns and saved games written before that still name the old launchers, so each name loads as the matching
 * plain launcher, and a unit that names one starts the battle declared as Inferno.</p>
 */
public final class WithdrawnInfernoSrmLaunchers {

    /** Old class of the two-shot Inferno launcher, still named inside saved games written before its removal. */
    public static final String STANDARD_LAUNCHER_CLASS =
          "megamek.common.weapons.infantry.support.srm.InfantrySupportSRMStandardInfernoWeapon";
    /** Old class of the light Inferno launcher, still named inside saved games written before its removal. */
    public static final String LIGHT_LAUNCHER_CLASS =
          "megamek.common.weapons.infantry.support.srm.InfantrySupportSRMLightInfernoWeapon";
    /** Old class of the heavy Inferno launcher, still named inside saved games written before its removal. */
    public static final String HEAVY_LAUNCHER_CLASS =
          "megamek.common.weapons.infantry.support.srm.InfantrySupportSRMHeavyInfernoWeapon";

    /** Names of the withdrawn light Inferno launcher; they load as the light SRM launcher. */
    static final List<String> LIGHT_LAUNCHER_NAMES = List.of("InfantrySRMLightInferno",
          "SRM Launcher (Light) - Inferno", "Light SRM (Inferno)");

    /** Names of the withdrawn two-shot Inferno launcher; they load as the two-shot SRM launcher. */
    static final List<String> STANDARD_LAUNCHER_NAMES = List.of("InfantryStandardSRMInferno",
          "SRM Launcher (Std, Two-Shot) - Inferno", "Infantry2ShotSRMInferno",
          "Infantry Two-Shot SRM Launcher (Inferno)");

    /** Names of the withdrawn heavy Inferno launcher; they load as the heavy SRM launcher. */
    static final List<String> HEAVY_LAUNCHER_NAMES = List.of("InfantryHeavySRMInferno",
          "SRM Launcher (Heavy) w/ Inferno", "Infantry Heavy SRM Launcher (Inferno)",
          "SRM Launcher (Hvy, One-Shot) w/ Inferno");

    private WithdrawnInfernoSrmLaunchers() {
    }

    /**
     * @param weaponName a weapon name as written in a unit file, MUL or saved game
     *
     * @return {@code true} if the name belongs to a withdrawn Inferno SRM launcher, so the unit was built to carry
     *       Inferno munitions
     */
    public static boolean isWithdrawnName(@Nullable String weaponName) {
        if (weaponName == null) {
            return false;
        }
        String lowerCaseName = weaponName.toLowerCase(Locale.ROOT);
        return containsIgnoringCase(LIGHT_LAUNCHER_NAMES, lowerCaseName)
              || containsIgnoringCase(STANDARD_LAUNCHER_NAMES, lowerCaseName)
              || containsIgnoringCase(HEAVY_LAUNCHER_NAMES, lowerCaseName);
    }

    private static boolean containsIgnoringCase(List<String> names, String lowerCaseName) {
        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).equals(lowerCaseName)) {
                return true;
            }
        }
        return false;
    }
}
