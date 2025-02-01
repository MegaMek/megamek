/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;


/**
 * Set of flags that can be used to determine how the weapon is used and its
 * special properties
 * note: many weapons can be identified by their ammo type
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
    F_DOUBLE_ONESHOT, // Fusillade works like a one-shot weapon but has a second round.
    F_EXTINGUISHER, // fire Extinguisher
    F_HEATASDICE, // Variable heat, heat is listed in dice, not points
    F_INFANTRY_ONLY, // may only target Infantry
    F_MISSILE_HITS, // use missile rules for # of hits
    F_MORTARTYPE_INDIRECT,  // This flag is used by mortar-type weapons that allow indirect fire without a spotter and/or with LOS.
    F_NO_FIRES, // can not start fires
    F_NO_AIM,
    F_ONESHOT,
    F_SOLO_ATTACK, // must be only weapon attacking
    F_PROTOTYPE, // War of 3039 prototypes
    F_REPEATING,

    // firestarters
    F_INCENDIARY_NEEDLES, // fires
    F_INFERNO, // Inferno weapon
    F_PLASMA, // fires

    // Special behaviors
    F_TAG,
    F_C3M, // C3 Master with Target Acquisition gear
    F_C3MBS, // C3 Master Booster System

    // Weapon classes
    F_ANTI_SHIP, // Anti-ship missiles
    F_B_POD,
    F_BOMBAST_LASER,
    F_HYPER, // Hyper-Laser
    F_LASER,
    F_LARGEMISSILE, // Thunderbolt and similar large missiles, for use with AMS resolution
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

    // self defense weapons
    F_AMS, // AMS
    F_CWS,

    // Capital sized self defense
    F_AMSBAY, // AMS and Point Defense Bays - Have to work differently from code using the F_AMS flag
    F_PDBAY, // AMS and Point Defense Bays - Have to work differently from code using the F_AMS flag

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
    F_INF_CLIMBINGCLAWS, // TODO Add game rules IO pg 84
    F_INF_ENCUMBER,
    F_INF_NONPENETRATING,
    F_INF_POINT_BLANK,
    F_INF_SUPPORT,
}
