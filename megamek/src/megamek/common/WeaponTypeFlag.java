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
public enum WeaponTypeFlag implements IndexedFlag {
    F_DIRECT_FIRE(0), // marks any weapon affected by a targeting computer
    F_FLAMER(1),
    F_LASER(2),
    F_PPC(3),
    F_AUTO_TARGET(4), // for weapons that target Automatically (AMS)
    F_NO_FIRES(5), // can not start fires
    F_SOLO_ATTACK(7), // must be only weapon attacking
    F_VGL(8),
    F_MG(9), // MGL for rapid fire setup
    F_INFERNO(10), // Inferno weapon
    F_INFANTRY(11), // Infantry caliber weapon, damage based on # of men shooting
    F_MISSILE_HITS(13), // use missile rules for # of hits
    F_ONESHOT(14),
    F_ARTILLERY(15),
    F_BALLISTIC(16), // for Gunnery/Ballistic
    F_ENERGY(17), // for Gunnery/Energy
    F_MISSILE(18), // for Gunnery/Missile
    F_PLASMA(19), // fires
    F_INCENDIARY_NEEDLES(20), // fires
    F_PROTOTYPE(21), // War of 3039 prototypes
    F_HEATASDICE(22), // Variable heat, heat is listed in dice, not points
    F_AMS(23), // AMS
    F_INFANTRY_ONLY(25), // may only target Infantry
    F_TAG(26),
    F_C3M(27), // C3 Master with Target Acquisition gear
    F_PLASMA_MFUK(28), // Plasma Rifle
    F_EXTINGUISHER(29), // fire Extinguisher
    F_PULSE(30),
    F_BURST_FIRE(31), // Full Damage vs. Infantry
    F_MGA(32), // Machine Gun Array
    F_NO_AIM(33),
    F_BOMBAST_LASER(34),
    F_CRUISE_MISSILE(35),
    F_B_POD(36),
    F_TASER(37),
    F_ANTI_SHIP(38), // Anti-ship missiles
    F_SPACE_BOMB(39),
    F_M_POD(40),
    F_DIVE_BOMB(41),
    F_ALT_BOMB(42),
    F_BA_WEAPON(43), // Currently only used by MML
    F_MEK_WEAPON(44), // Currently only used by MML
    F_AERO_WEAPON(45), // Currently only used by MML
    F_PROTO_WEAPON(46), // Currently only used by MML
    F_TANK_WEAPON(47),
    F_INFANTRY_ATTACK(48),
    F_INF_BURST(49),
    F_INF_AA(50),
    F_INF_NONPENETRATING(51),
    F_INF_POINT_BLANK(52),
    F_INF_SUPPORT(53),
    F_INF_ENCUMBER(54),
    F_INF_ARCHAIC(55),
    F_INF_CLIMBINGCLAWS(63), // TODO Add game rules IO pg 84
    F_C3MBS(56), // C3 Master Booster System
    F_MASS_DRIVER(58), // Naval Mass Drivers
    F_CWS(59),
    F_MEK_MORTAR(60),
    F_BOMB_WEAPON(61), // Weapon required to make a bomb type function
    F_BA_INDIVIDUAL(62),
    F_PDBAY(64), // AMS and Point Defense Bays - Have to work differently from code using the F_AMS flag
    F_AMSBAY(65), // AMS and Point Defense Bays - Have to work differently from code using the F_AMS flag
    F_LARGEMISSILE(66), // Thunderbolt and similar large missiles, for use with AMS resolution
    F_HYPER(67), // Hyper-Laser
    F_DOUBLE_ONESHOT(68), // Fusillade works like a one-shot weapon but has a second round.
    F_ER_FLAMER(69), // ER flamers do half damage in heat mode
    F_ARTEMIS_COMPATIBLE(70), // Missile weapon that can be linked to an Artemis fire control system
    F_MORTARTYPE_INDIRECT(71),  // This flag is used by mortar-type weapons that allow indirect fire without a spotter and/or with LOS.
    F_TSEMP(57), // Used for TSEMP Weapons.
    F_REPEATING(72);

    private final int flagIndex;

    WeaponTypeFlag(int flagIndex) {
        assert flagIndex >= 0;
        this.flagIndex = flagIndex;
    }

    @Override
    public int getFlagIndex() {
        return flagIndex;
    }

}
