/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.generator;

import java.util.HashSet;

import megamek.client.ratgenerator.ForceDescriptor;

/**
 * Bare data class to pass around and store configuration-influencing info
 */
public class ReconfigurationParameters {

    // Game settings
    public boolean enemiesVisible = true;
    public int allowedYear = 3151;

    // Map settings
    public boolean darkEnvironment = false;
    public boolean groundMap = false;
    public boolean spaceEnvironment = false;

    // Enemy stats
    public long enemyCount = 0;
    public long enemyFliers = 0;
    public long enemyBombers = 0;
    public long enemyInfantry = 0;
    public long enemyBattleArmor = 0;
    public long enemyVehicles = 0;
    public long enemyMeks = 0;
    public long enemyEnergyBoats = 0;
    public long enemyMissileBoats = 0;
    public long enemyAdvancedArmorCount = 0;
    public long enemyReflectiveArmorCount = 0;
    public long enemyFireproofArmorCount = 0;
    public long enemyFastMovers = 0;
    public long enemyOffBoard = 0;
    public long enemyECMCount = 0;
    public long enemyTSMCount = 0;
    public HashSet<String> enemyFactions = new HashSet<String>();

    // Friendly stats
    //
    public int friendlyQuality = ForceDescriptor.RATING_5;
    public String friendlyFaction = "";
    public boolean isPirate = false;
    public long friendlyCount = 0;
    public long friendlyTAGs = 0;
    public long friendlyNARCs = 0;
    public long friendlyEnergyBoats = 0;
    public long friendlyMissileBoats = 0;
    public long friendlyOffBoard = 0;
    public long friendlyECMCount = 0;
    public long friendlyInfantry = 0;
    public long friendlyBattleArmor = 0;
    public long friendlyHeatGens = 0;

    // User-selected directives
    // Nukes may be banned for a given team but allowed in general (boo, hiss)
    public boolean nukesBannedForMe = true;

    // Allow setting bin filled percentages (e.g. for destitute pirates)
    public float binFillPercent = 1.0f;

    // Datatype for passing around game parameters the Loadout Generator cares about
    ReconfigurationParameters() {
    }
}
