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

package megamek.common.equipment;


/**
 * Set of flags that can be used to determine how the weapon is used and its special properties note: many weapons can
 * be identified by their ammo type
 *
 * @author Luana Coppio
 */
public enum WeaponTypeFlag implements EquipmentFlag {
    // Skill type flags
    F_BALLISTIC, // for Gunnery/Ballistic
    F_ENERGY, // for Gunnery/Energy
    F_MISSILE, // for Gunnery/Missile

    // Weapon property flags
    F_ARTILLERY,
    F_ARTEMIS_COMPATIBLE, // Missile weapon that can be linked to an Artemis fire control system
    F_AUTO_TARGET, // for weapons that target Automatically
    F_BA_INDIVIDUAL,
    F_BURST_FIRE, // Full Damage vs. Infantry
    F_DIRECT_FIRE, // marks any weapon affected by a targeting computer
    F_DOUBLE_ONE_SHOT, // Fusillade works like a one-shot weapon but has a second round.
    F_EXTINGUISHER, // fire Extinguisher
    F_HEAT_AS_DICE, // Variable heat, heat is listed in dice, not points
    F_INFANTRY_ONLY, // may only target Infantry
    F_MISSILE_HITS, // use missile rules for # of hits
    F_MORTAR_TYPE_INDIRECT,  // This flag is used by mortar-type weapons that allow indirect fire without a spotter and/or with LOS.
    F_NO_FIRES, // can not start fires
    F_NO_AIM,
    F_ONE_SHOT,
    /** A weapon with this flag can only be fired alone, i.e. it must be the only weapon attacking */
    F_SOLO_ATTACK,
    F_PROTOTYPE, // War of 3039 prototypes
    F_REPEATING,

    // firestarters
    F_INCENDIARY_NEEDLES, // fires
    F_INFERNO, // Inferno weapon
    F_PLASMA, // fires

    // Special behaviors
    F_TAG,
    F_C3M, // C3 Master with Target Acquisition gear
    F_C3MBS, // C3 Master Booster SystemFluff

    // Weapon classes
    F_ANTI_SHIP, // Anti-ship missiles
    F_B_POD,
    F_BOMBAST_LASER,
    F_HYPER, // Hyper-Laser
    F_LASER,
    F_LARGE_MISSILE, // Thunderbolt and similar large missiles, for use with AMS resolution
    F_ER_FLAMER, // ER flamers do half damage in heat mode
    F_FLAMER,
    F_M_POD,
    F_MEK_MORTAR,
    F_MG, // MGL for rapid fire setup
    F_MGA, // Machine Gun Array
    F_PLASMA_MFUK, // Plasma Rifle
    F_PPC,
    F_PULSE,
    F_TASER,
    F_TSEMP, // Used for TSEMP Weapons.
    F_VGL,

    // Bomb types
    F_ALT_BOMB,
    F_BOMB_WEAPON, // Weapon required to make a bomb type function
    F_DIVE_BOMB,
    F_SPACE_BOMB,

    // self-defense weapons
    F_AMS, // AMS
    F_CWS,

    // Capital sized self defense
    F_AMS_BAY, // AMS and Point Defense Bays - Have to work differently from code using the F_AMS flag
    F_PD_BAY, // AMS and Point Defense Bays - Have to work differently from code using the F_AMS flag

    // Capital weapons
    F_CRUISE_MISSILE,
    F_MASS_DRIVER, // Naval Mass Drivers

    // Flags to restrict chassis that can receive the weapons
    F_AERO_WEAPON, // Currently only used by MML
    F_INFANTRY, // Infantry caliber weapon, damage based on # of men shooting
    F_BA_WEAPON, // Currently only used by MML
    F_MEK_WEAPON, // Currently only used by MML
    F_PROTO_WEAPON, // Currently only used by MML
    F_TANK_WEAPON,

    // Infantry weapon flags
    F_INFANTRY_ATTACK,
    F_INF_AA,
    F_INF_ARCHAIC,
    F_INF_BURST,
    F_INF_CLIMBING_CLAWS, // TODO Add game rules IO pg 84
    F_INF_ENCUMBER,
    F_INF_NONPENETRATING,
    F_INF_POINT_BLANK,
    F_INF_SUPPORT,

    /**
     * A weapon with this flag is not an actual weapon but only used as an internal representation of an attack. It
     * should be hidden in the readout, tooltip and RS but shown in the unit display's weapon list where it is used.
     */
    INTERNAL_REPRESENTATION,

    /**
     * Denotes Clan Heavy Lasers (S, M, L)
     */
    HEAVY_LASER
}
