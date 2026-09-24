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
package megamek.common.compute;

/**
 * What a unit brought to an infantry vs. infantry action, as the Marine Points Tables (TO:AR p. 170) see it: the
 * kind of people and the rows of the Battle Armor Modifiers table they earned. A report uses a side's traits to
 * colour its narrative, so the order here is the order of precedence when a side has several: the trait that most
 * shaped the fighting comes first.
 */
public enum MarinePointsTrait {
    /** Paired battle claws of any kind. */
    CLAWS,
    /** One or more burst-fire weapons. */
    BURST_FIRE,
    /** One or more flame-based weapons. */
    FLAME,
    /** Heavy or assault battle armor. */
    HEAVY_SUITS,
    /** Magnetic clamps, counted only in microgravity. */
    MAGNETIC_CLAMPS,
    /** Clan Elemental troopers. */
    ELEMENTALS,
    /** Inner Sphere battle armor troopers. */
    INNER_SPHERE_BATTLE_ARMOR,
    /** PA(L) or exoskeleton battle armor. */
    LIGHT_SUITS,
    /** Cutting torches or industrial drills. */
    TORCHES_OR_DRILLS,
    /** Anti-personnel weapon mounts. */
    ANTI_PERSONNEL_MOUNTS,
    /** Conventional infantry with the marine specialisation. */
    MARINES,
    /** Conventional infantry in armor with a damage divisor of two or more. */
    ARMORED_INFANTRY,
    /** Conventional infantry without the marine specialisation. */
    LINE_INFANTRY,
    /** Passengers, taken to be civilians. */
    CIVILIANS,
    /** The crew of a building or vessel fighting for it. */
    BUILDING_CREW
}
