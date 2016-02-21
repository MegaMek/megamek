/*
  * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@SuppressWarnings("unchecked")
public class AmmoType extends EquipmentType {
    // ammo types
    public static final int T_NA = -1;
    public static final int T_AC = 1;
    public static final int T_VEHICLE_FLAMER = 2;
    public static final int T_MG = 3;
    public static final int T_MG_HEAVY = 4;
    public static final int T_MG_LIGHT = 5;
    public static final int T_GAUSS = 6;
    public static final int T_LRM = 7;
    public static final int T_LRM_TORPEDO = 8;
    public static final int T_SRM = 9;
    public static final int T_SRM_TORPEDO = 10;
    public static final int T_SRM_STREAK = 11;
    public static final int T_MRM = 12;
    public static final int T_NARC = 13;
    public static final int T_AMS = 14;
    public static final int T_ARROW_IV = 15;
    public static final int T_LONG_TOM = 16;
    public static final int T_SNIPER = 17;
    public static final int T_THUMPER = 18;
    public static final int T_AC_LBX = 19;
    public static final int T_AC_ULTRA = 20;
    public static final int T_GAUSS_LIGHT = 21;
    public static final int T_GAUSS_HEAVY = 22;
    public static final int T_AC_ROTARY = 23;
    public static final int T_SRM_ADVANCED = 24;
    public static final int T_BA_MICRO_BOMB = 25;
    public static final int T_LRM_TORPEDO_COMBO = 26;
    public static final int T_MINE = 27;
    public static final int T_ATM = 28; // Clan ATM missile systems
    public static final int T_ROCKET_LAUNCHER = 29;
    public static final int T_INARC = 30;
    public static final int T_LRM_STREAK = 31;
    public static final int T_AC_LBX_THB = 32;
    public static final int T_AC_ULTRA_THB = 33;
    public static final int T_LAC = 34;
    public static final int T_HEAVY_FLAMER = 35;
    public static final int T_COOLANT_POD = 36; // not really ammo, but explodes
    // and is depleted
    public static final int T_EXLRM = 37;
    public static final int T_APGAUSS = 38;
    public static final int T_MAGSHOT = 39;
    public static final int T_PXLRM = 40;
    public static final int T_HSRM = 41;
    public static final int T_MRM_STREAK = 42;
    public static final int T_MPOD = 43;
    public static final int T_HAG = 44;
    public static final int T_MML = 45;
    public static final int T_PLASMA = 46;
    public static final int T_SBGAUSS = 47;
    public static final int T_RAIL_GUN = 48;
    public static final int T_TBOLT_5 = 49;
    public static final int T_TBOLT_10 = 50;
    public static final int T_TBOLT_15 = 51;
    public static final int T_TBOLT_20 = 52;
    public static final int T_NAC = 53;
    public static final int T_LIGHT_NGAUSS = 54;
    public static final int T_MED_NGAUSS = 55;
    public static final int T_HEAVY_NGAUSS = 56;
    public static final int T_KILLER_WHALE = 57;
    public static final int T_WHITE_SHARK = 58;
    public static final int T_BARRACUDA = 59;
    public static final int T_KRAKEN_T = 60;
    public static final int T_AR10 = 61;
    public static final int T_SCREEN_LAUNCHER = 62;
    public static final int T_ALAMO = 63;
    public static final int T_IGAUSS_HEAVY = 64;
    public static final int T_CHEMICAL_LASER = 65;
    public static final int T_HYPER_VELOCITY = 66;
    public static final int T_MEK_MORTAR = 67;
    public static final int T_CRUISE_MISSILE = 68;
    public static final int T_BPOD = 69;
    public static final int T_SCC = 70;
    public static final int T_MANTA_RAY = 71;
    public static final int T_SWORDFISH = 72;
    public static final int T_STINGRAY = 73;
    public static final int T_PIRANHA = 74;
    public static final int T_TASER = 75;
    public static final int T_BOMB = 76;
    public static final int T_AAA_MISSILE = 77;
    public static final int T_AS_MISSILE = 78;
    public static final int T_ASEW_MISSILE = 79;
    public static final int T_LAA_MISSILE = 80;
    public static final int T_RL_BOMB = 81;
    public static final int T_ARROW_IV_BOMB = 82;
    public static final int T_FLUID_GUN = 83;
    public static final int T_SNIPER_CANNON = 84;
    public static final int T_THUMPER_CANNON = 85;
    public static final int T_LONG_TOM_CANNON = 86;
    public static final int T_NAIL_RIVET_GUN = 87;
    public static final int T_ACi = 88;
    public static final int T_KRAKENM = 89;
    public static final int T_PAC = 90;
    public static final int T_NLRM = 91;
    public static final int T_RIFLE = 92;
    public static final int T_VGL = 93;
    public static final int T_C3_REMOTE_SENSOR = 94;
    public static final int T_AC_PRIMITIVE = 95;
    public static final int T_LRM_PRIMITIVE = 96;
    public static final int T_SRM_PRIMITIVE = 97;
    public static final int T_BA_TUBE = 98;
    public static final int T_IATM = 99;
    public static final int T_LMASS = 100;
    public static final int T_MMASS = 101;
    public static final int T_HMASS = 102;
    public static final int T_APDS = 103;
    public static final int NUM_TYPES = 104;

    // ammo flags
    public static final BigInteger F_MG = BigInteger.valueOf(1).shiftLeft(0);
    public static final BigInteger F_BATTLEARMOR = BigInteger.valueOf(1)
            .shiftLeft(1); // only
    // used
    // by
    // BA
    // squads
    public static final BigInteger F_PROTOMECH = BigInteger.valueOf(1)
            .shiftLeft(2); // only
    // used
    // by
    // Protomechs
    public static final BigInteger F_HOTLOAD = BigInteger.valueOf(1).shiftLeft(
            3); // Ammo
    // Can
    // be
    // hotloaded
    public static final BigInteger F_ENCUMBERING = BigInteger.valueOf(1)
            .shiftLeft(4); // BA
    // can't
    // jump
    // or
    // make
    // antimech until dumped
    public static final BigInteger F_MML_LRM = BigInteger.valueOf(1).shiftLeft(
            5); // LRM
    // type
    public static final BigInteger F_AR10_WHITE_SHARK = BigInteger.valueOf(1)
            .shiftLeft(6); // White
    // shark
    // type
    public static final BigInteger F_AR10_KILLER_WHALE = BigInteger.valueOf(1)
            .shiftLeft(7); // Killer
    // Whale
    // type
    public static final BigInteger F_AR10_BARRACUDA = BigInteger.valueOf(1)
            .shiftLeft(8); // barracuda
    // type
    public static final BigInteger F_NUCLEAR = BigInteger.valueOf(1).shiftLeft(
            9); // Nuclear
    // missile
    public static final BigInteger F_TELE_MISSILE = BigInteger.valueOf(1)
            .shiftLeft(10); // Tele-Missile
    public static final BigInteger F_CAP_MISSILE = BigInteger.valueOf(1)
            .shiftLeft(11); // Tele-Missile
    public static final BigInteger F_SPACE_BOMB = BigInteger.valueOf(1)
            .shiftLeft(12); // can
    // be
    // used
    // to
    // space
    // bomb
    public static final BigInteger F_GROUND_BOMB = BigInteger.valueOf(1)
            .shiftLeft(13); // can
    // be
    // used
    // to
    // ground
    // bomb

    // ammo munitions, used for custom loadouts
    // N.B. we play bit-shifting games to allow "incendiary"
    // to be combined to other munition types.

    // M_STANDARD can be used for anything.
    public static final long M_STANDARD = 0;

    // AC Munition Types
    public static final long M_CLUSTER = 1l << 0;
    public static final long M_ARMOR_PIERCING = 1l << 1;
    public static final long M_FLECHETTE = 1l << 2;
    public static final long M_INCENDIARY_AC = 1l << 3;
    public static final long M_PRECISION = 1l << 4;
    public static final long M_TRACER = 1l << 5;
    public static final long M_FLAK = 1l << 6;

    // ATM Munition Types
    public static final long M_EXTENDED_RANGE = 1l << 7;
    public static final long M_HIGH_EXPLOSIVE = 1l << 8;
    public static final long M_IATM_IMP = 1l << 57;
    public static final long M_IATM_IIW = 1l << 58;

    // LRM & SRM Munition Types
    public static final long M_FRAGMENTATION = 1l << 9;
    public static final long M_LISTEN_KILL = 1l << 10;
    public static final long M_ANTI_TSM = 1l << 11;
    public static final long M_NARC_CAPABLE = 1l << 12;
    public static final long M_ARTEMIS_CAPABLE = 1l << 13;
    public static final long M_DEAD_FIRE = 1l << 14;
    public static final long M_HEAT_SEEKING = 1l << 15;
    public static final long M_TANDEM_CHARGE = 1l << 16;
    public static final long M_ARTEMIS_V_CAPABLE = 1l << 17;
    public static final long M_SMOKE_WARHEAD = 1l << 18;
    // Mine Clearance munition type defined later, to maintain order
    
    // LRM Munition Types
    // Incendiary is special, though...
    // FIXME - I'm not implemented!!!
    public static final long M_INCENDIARY_LRM = 1l << 19;
    public static final long M_FLARE = 1l << 20;
    public static final long M_SEMIGUIDED = 1l << 21;
    public static final long M_SWARM = 1l << 22;
    public static final long M_SWARM_I = 1l << 23;
    public static final long M_THUNDER = 1l << 24;
    public static final long M_THUNDER_AUGMENTED = 1l << 25;
    public static final long M_THUNDER_INFERNO = 1l << 26;
    public static final long M_THUNDER_VIBRABOMB = 1l << 27;
    public static final long M_THUNDER_ACTIVE = 1l << 28;
    public static final long M_FOLLOW_THE_LEADER = 1l << 29;
    public static final long M_MULTI_PURPOSE = 1l << 30;
    // SRM Munition Types
    // TODO: Inferno should be available to fluid guns and vehicle flamers
    // TO page 362
    public static final long M_INFERNO = 1l << 31;
    public static final long M_AX_HEAD = 1l << 32;

    // SRM, MRM and LRM
    public static final long M_TORPEDO = 1l << 33;

    // iNarc Munition Types
    public static final long M_NARC_EX = 1l << 34;
    public static final long M_ECM = 1l << 35;
    public static final long M_HAYWIRE = 1l << 36;
    public static final long M_NEMESIS = 1l << 37;

    public static final long M_EXPLOSIVE = 1l << 38;

    // Arrow IV Munition Types
    public static final long M_HOMING = 1l << 39;
    public static final long M_FASCAM = 1l << 40;
    public static final long M_INFERNO_IV = 1l << 41;
    public static final long M_VIBRABOMB_IV = 1l << 42;
    public static final long M_SMOKE = 1l << 43;
    public static final long M_LASER_INHIB = 1l << 44;

    // Nuclear Munitions
    public static final long M_DAVY_CROCKETT_M = 1l << 45;
    public static final long M_SANTA_ANNA = 1l << 46;

    // tele-missile
    public static final long M_TELE = 1l << 47;

    // fluid gun
    // TODO: implement all of these except coolant
    // water should also be used for vehicle flamers
    // TO page 361-363
    public static final long M_WATER = 1l << 48;
    public static final long M_PAINT_OBSCURANT = 1l << 49;
    public static final long M_OIL_SLICK = 1l << 50;
    public static final long M_ANTI_FLAME_FOAM = 1l << 51;
    public static final long M_CORROSIVE = 1l << 52;
    public static final long M_COOLANT = 1l << 53;

    // vehicular grenade launcher
    public static final long M_CHAFF = 1l << 54;
    public static final long M_INCENDIARY = 1l << 55;
    public static final long M_SMOKEGRENADE = 1l << 56;

    // Number 57 is used for iATMs IMP ammo in the ATM section above.
    // and 58 for IIW
    
    // Mek mortar munitions
    public static final long M_AIRBURST = 1l << 59;
    public static final long M_ANTI_PERSONNEL = 1l << 60;
    // The rest were already defined
    // Flare
    // Semi-guided
    // Smoke
    
    // More SRM+LRM Munitions types
    public static final long M_MINE_CLEARANCE = 1l << 61;
    
    
    /*
     * public static final String[] MUNITION_NAMES = { "Standard", "Cluster",
     * "Armor Piercing", "Flechette", "Incendiary", "Incendiary", "Precision",
     * "Extended Range", "High Explosive", "Flare", "Fragmentation", "Inferno",
     * "Semiguided", "Swarm", "Swarm-I", "Thunder", "Thunder/Augmented",
     * "Thunder/Inferno", "Thunder/Vibrabomb", "Thunder/Active", "Explosive",
     * "ECM", "Haywire", "Nemesis", "Homing", "FASCAM", "Inferno-IV",
     * "Vibrabomb-IV", "Smoke", "Narc-Capable", "Artemis-Capable",
     * "Listen-Kill", "Anti-TSM", "Acid-Head" };
     */
    private static Vector<AmmoType>[] m_vaMunitions = new Vector[NUM_TYPES];

    public static Vector<AmmoType> getMunitionsFor(int nAmmoType) {
        return m_vaMunitions[nAmmoType];
    }

    protected int damagePerShot;
    protected int rackSize;
    protected int ammoType;
    protected long munitionType;
    protected int shots;
    private double kgPerShot = -1;

    // ratio for capital ammo
    private double ammoRatio;
    /**
     * Used for returning the submunition name for a submunition, such as
     * precision AC-10. The submunition name is pre-pended onto the
     * <code>shortName</code> and this variable keeps track of the index of the
     * end of the submunition name.
     */
    public int subMunitionBegin = 0;
    public int subMunitionLength = 0;

    // Short name of Ammo or RS Printing
    protected String shortName = "";

    public AmmoType() {
        criticals = 1;
        tankslots = 0;
        tonnage = 1.0f;
        explosive = true;
        instantModeSwitch = false;
        ammoRatio = 0;
    }

    /**
     * When comparing <code>AmmoType</code>s, look at the ammoType and rackSize.
     *
     * @param other the <code>Object</code> to compare to this one.
     * @return <code>true</code> if the other is an <code>AmmoType</code> object
     *         of the same <code>ammoType</code> as this object. N.B. different
     *         munition types are still equal.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AmmoType)) {
            return false;
        }
        // there a couple of flags that need to be checked
        if (getAmmoType() == T_MML) {
            if (hasFlag(F_MML_LRM) != ((AmmoType) other).hasFlag(F_MML_LRM)) {
                return false;
            }
        }
        if (getAmmoType() == T_AR10) {
            if (hasFlag(F_AR10_BARRACUDA) != ((AmmoType) other)
                    .hasFlag(F_AR10_BARRACUDA)) {
                return false;
            }
            if (hasFlag(F_AR10_WHITE_SHARK) != ((AmmoType) other)
                    .hasFlag(F_AR10_WHITE_SHARK)) {
                return false;
            }
            if (hasFlag(F_AR10_KILLER_WHALE) != ((AmmoType) other)
                    .hasFlag(F_AR10_KILLER_WHALE)) {
                return false;
            }
        }
        return ((getAmmoType() == ((AmmoType) other).getAmmoType()) && (getRackSize() == ((AmmoType) other)
                .getRackSize()));
    }

    public int getAmmoType() {
        return ammoType;
    }

    public long getMunitionType() {
        return munitionType;
    }

    protected int heat;
    protected RangeType range;
    protected int tech;
    protected boolean capital = false;

    public int getDamagePerShot() {
        return damagePerShot;
    }

    public int getRackSize() {
        return rackSize;
    }

    public int getShots() {
        return shots;
    }

    public double getAmmoRatio() {
        return ammoRatio;
    }

    public boolean isCapital() {
        return capital;
    }

    public double getKgPerShot() {
        return kgPerShot;
    }

    /**
     * Returns the first usable ammo type for the given oneshot launcher
     *
     * @param mounted
     * @return
     */
    public static AmmoType getOneshotAmmo(Mounted mounted) {
        WeaponType wt = (WeaponType) mounted.getType();
        if (wt.getAmmoType() == -1) {
            return null;
        }
        Vector<AmmoType> vAmmo = AmmoType.getMunitionsFor(wt.getAmmoType());
        AmmoType at = null;
        for (int i = 0; i < vAmmo.size(); i++) {
            at = vAmmo.elementAt(i);
            if ((at.getRackSize() == wt.getRackSize())
                    && (TechConstants.isLegal(
                    mounted.getType().getTechLevel(
                            mounted.getEntity().getTechLevelYear()), at
                    .getTechLevel(mounted.getEntity()
                            .getTechLevelYear()), false,
                    mounted.getEntity().isMixedTech()))) {
                return at;
            }
        }
        // found none, let's try again with techlevelyear 3071
        for (int i = 0; i < vAmmo.size(); i++) {
            at = vAmmo.elementAt(i);
            if ((at.getRackSize() == wt.getRackSize())
                    && (TechConstants.isLegal(
                    mounted.getType().getTechLevel(3071), at
                    .getTechLevel(3071), false, mounted
                    .getEntity().isMixedTech()))) {
                return at;
            }
        }
        return null; // couldn't find any ammo for this weapon type
    }

    public static void initializeTypes() {
        // Save copies of the SRM and LRM ammos to use to create munitions.
        ArrayList<AmmoType> srmAmmos = new ArrayList<AmmoType>(11);
        ArrayList<AmmoType> clanSrmAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> baSrmAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> clanBaLrmAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> isBaLrmAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> lrmAmmos = new ArrayList<AmmoType>(26);
        ArrayList<AmmoType> clanLrmAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> enhancedlrmAmmos = new ArrayList<AmmoType>(26);
        ArrayList<AmmoType> acAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> arrowAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> clanArrowAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> thumperAmmos = new ArrayList<AmmoType>(3);
        ArrayList<AmmoType> thumperCannonAmmos = new ArrayList<AmmoType>(3);
        ArrayList<AmmoType> sniperAmmos = new ArrayList<AmmoType>(3);
        ArrayList<AmmoType> sniperCannonAmmos = new ArrayList<AmmoType>(3);
        ArrayList<AmmoType> longTomAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> longTomCannonAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> clanArtyAmmos = new ArrayList<AmmoType>(6);
        ArrayList<AmmoType> mortarAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> clanMortarAmmos = new ArrayList<AmmoType>(4);
        ArrayList<AmmoType> lrtAmmos = new ArrayList<AmmoType>(26);
        ArrayList<AmmoType> clanLrtAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> srtAmmos = new ArrayList<AmmoType>(26);
        ArrayList<AmmoType> clanSrtAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> vglAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> clanVGLAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> vehicleFlamerAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> clanVehicleFlamerAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> heavyFlamerAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> clanHeavyFlamerAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> fluidGunAmmos = new ArrayList<AmmoType>();
        ArrayList<AmmoType> clanFluidGunAmmos = new ArrayList<AmmoType>();

        ArrayList<MunitionMutator> munitions = new ArrayList<MunitionMutator>();

        AmmoType base = null;

        // all level 1 ammo
        base = AmmoType.createISVehicleFlamerAmmo();
        vehicleFlamerAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISMGAmmo());
        EquipmentType.addType(AmmoType.createISMGAmmoHalf());
        base = AmmoType.createISAC2Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISAC5Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISAC10Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISAC20Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM5Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM10Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM15Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM20Ammo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM5pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM10pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM15pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLRM20pAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSRM2Ammo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSRM4Ammo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSRM6Ammo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSRM2pAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSRM4pAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSRM6pAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISAC2pAmmo());
        EquipmentType.addType(AmmoType.createISAC5pAmmo());
        EquipmentType.addType(AmmoType.createISAC10pAmmo());
        EquipmentType.addType(AmmoType.createISAC20pAmmo());

        // Level 3 Ammo
        // Note, some level 3 stuff is mixed into level 2.
        base = AmmoType.createISEnhancedLRM5Ammo();
        enhancedlrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISEnhancedLRM10Ammo();
        enhancedlrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISEnhancedLRM15Ammo();
        enhancedlrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISEnhancedLRM20Ammo();
        enhancedlrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC2Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC5Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC10Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLAC20Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISHeavyFlamerAmmo();
        heavyFlamerAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLHeavyFlamerAmmo();
        clanHeavyFlamerAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISCoolantPod());
        EquipmentType.addType(AmmoType.createCLCoolantPod());
        EquipmentType.addType(AmmoType.createISRailGunAmmo());
        EquipmentType.addType(AmmoType.createISMPodAmmo());
        EquipmentType.addType(AmmoType.createISBPodAmmo());

        // Start of Level2 Ammo
        EquipmentType.addType(AmmoType.createISLB2XAmmo());
        EquipmentType.addType(AmmoType.createISLB5XAmmo());
        EquipmentType.addType(AmmoType.createISLB10XAmmo());
        EquipmentType.addType(AmmoType.createISLB20XAmmo());
        EquipmentType.addType(AmmoType.createISLB2XClusterAmmo());
        EquipmentType.addType(AmmoType.createISLB5XClusterAmmo());
        EquipmentType.addType(AmmoType.createISLB10XClusterAmmo());
        EquipmentType.addType(AmmoType.createISLB20XClusterAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB2XAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB5XAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB20XAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB2XClusterAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB5XClusterAmmo());
        EquipmentType.addType(AmmoType.createISTHBLB20XClusterAmmo());
        EquipmentType.addType(AmmoType.createISUltra2Ammo());
        EquipmentType.addType(AmmoType.createISUltra5Ammo());
        EquipmentType.addType(AmmoType.createISUltra10Ammo());
        EquipmentType.addType(AmmoType.createISUltra20Ammo());
        EquipmentType.addType(AmmoType.createISTHBUltra2Ammo());
        EquipmentType.addType(AmmoType.createISTHBUltra10Ammo());
        EquipmentType.addType(AmmoType.createISTHBUltra20Ammo());
        EquipmentType.addType(AmmoType.createISRotary2Ammo());
        EquipmentType.addType(AmmoType.createISRotary5Ammo());
        EquipmentType.addType(AmmoType.createISRotary10Ammo());
        EquipmentType.addType(AmmoType.createISRotary20Ammo());
        EquipmentType.addType(AmmoType.createISGaussAmmo());
        EquipmentType.addType(AmmoType.createISLTGaussAmmo());
        EquipmentType.addType(AmmoType.createISHVGaussAmmo());
        EquipmentType.addType(AmmoType.createISIHVGaussAmmo());
        EquipmentType.addType(AmmoType.createISStreakSRM2Ammo());
        EquipmentType.addType(AmmoType.createISStreakSRM4Ammo());
        EquipmentType.addType(AmmoType.createISStreakSRM6Ammo());
        EquipmentType.addType(AmmoType.createISMRM10Ammo());
        EquipmentType.addType(AmmoType.createISMRM20Ammo());
        EquipmentType.addType(AmmoType.createISMRM30Ammo());
        EquipmentType.addType(AmmoType.createISMRM40Ammo());
        EquipmentType.addType(AmmoType.createISRL10Ammo());
        EquipmentType.addType(AmmoType.createISRL15Ammo());
        EquipmentType.addType(AmmoType.createISRL20Ammo());
        EquipmentType.addType(AmmoType.createISAMSAmmo());
        EquipmentType.addType(AmmoType.createISNarcAmmo());
        EquipmentType.addType(AmmoType.createISNarcExplosiveAmmo());
        EquipmentType.addType(AmmoType.createISiNarcAmmo());
        EquipmentType.addType(AmmoType.createISiNarcECMAmmo());
        EquipmentType.addType(AmmoType.createISiNarcExplosiveAmmo());
        EquipmentType.addType(AmmoType.createISiNarcHaywireAmmo());
        EquipmentType.addType(AmmoType.createISiNarcNemesisAmmo());
        EquipmentType.addType(AmmoType.createISExtendedLRM5Ammo());
        EquipmentType.addType(AmmoType.createISExtendedLRM10Ammo());
        EquipmentType.addType(AmmoType.createISExtendedLRM15Ammo());
        EquipmentType.addType(AmmoType.createISExtendedLRM20Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt5Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt10Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt15Ammo());
        EquipmentType.addType(AmmoType.createISThunderbolt20Ammo());
        EquipmentType.addType(AmmoType.createISMagshotGRAmmo());
        EquipmentType.addType(AmmoType.createISPXLRM5Ammo());
        EquipmentType.addType(AmmoType.createISPXLRM10Ammo());
        EquipmentType.addType(AmmoType.createISPXLRM15Ammo());
        EquipmentType.addType(AmmoType.createISPXLRM20Ammo());
        EquipmentType.addType(AmmoType.createISHawkSRM2Ammo());
        EquipmentType.addType(AmmoType.createISHawkSRM4Ammo());
        EquipmentType.addType(AmmoType.createISHawkSRM6Ammo());
        EquipmentType.addType(AmmoType.createISStreakMRM10Ammo());
        EquipmentType.addType(AmmoType.createISStreakMRM20Ammo());
        EquipmentType.addType(AmmoType.createISStreakMRM30Ammo());
        EquipmentType.addType(AmmoType.createISStreakMRM40Ammo());
        EquipmentType.addType(AmmoType.createISHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createISHeavyMGAmmoHalf());
        EquipmentType.addType(AmmoType.createISLightMGAmmo());
        EquipmentType.addType(AmmoType.createISLightMGAmmoHalf());
        EquipmentType.addType(AmmoType.createISSBGaussRifleAmmo());
        EquipmentType.addType(AmmoType.createISHVAC10Ammo());
        EquipmentType.addType(AmmoType.createISHVAC5Ammo());
        EquipmentType.addType(AmmoType.createISHVAC2Ammo());
        EquipmentType.addType(AmmoType.createISMekTaserAmmo());

        base = AmmoType.createISMML3LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML3SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML5LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML5SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML7LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML7SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML9LRMAmmo();
        lrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISMML9SRMAmmo();
        srmAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISLongTomAmmo();
        longTomAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISLongTomCannonAmmo();
        longTomCannonAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSniperAmmo();
        sniperAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISSniperCannonAmmo();
        sniperCannonAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISThumperAmmo();
        thumperAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISThumperCannonAmmo();
        thumperCannonAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISArrowIVAmmo();
        arrowAmmos.add(base);
        EquipmentType.addType(base);

        EquipmentType.addType(AmmoType.createCLLB2XAmmo());
        EquipmentType.addType(AmmoType.createCLLB5XAmmo());
        EquipmentType.addType(AmmoType.createCLLB10XAmmo());
        EquipmentType.addType(AmmoType.createCLLB20XAmmo());
        EquipmentType.addType(AmmoType.createCLLB2XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLLB5XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLLB10XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLLB20XClusterAmmo());
        EquipmentType.addType(AmmoType.createCLUltra2Ammo());
        EquipmentType.addType(AmmoType.createCLUltra5Ammo());
        EquipmentType.addType(AmmoType.createCLUltra10Ammo());
        EquipmentType.addType(AmmoType.createCLUltra20Ammo());
        EquipmentType.addType(AmmoType.createCLRotary2Ammo());
        EquipmentType.addType(AmmoType.createCLRotary5Ammo());
        EquipmentType.addType(AmmoType.createCLRotary10Ammo());
        EquipmentType.addType(AmmoType.createCLRotary20Ammo());
        EquipmentType.addType(AmmoType.createCLGaussAmmo());
        EquipmentType.addType(AmmoType.createCLStreakSRM1Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM2Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM3Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM4Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM5Ammo());
        EquipmentType.addType(AmmoType.createCLStreakSRM6Ammo());
        base = AmmoType.createCLVehicleFlamerAmmo();
        clanVehicleFlamerAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createCLMGAmmo());
        EquipmentType.addType(AmmoType.createCLMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createCLHeavyMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLLightMGAmmo());
        EquipmentType.addType(AmmoType.createCLLightMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLAMSAmmo());
        EquipmentType.addType(AmmoType.createCLNarcAmmo());
        EquipmentType.addType(AmmoType.createCLNarcExplosiveAmmo());
        EquipmentType.addType(AmmoType.createCLATM3Ammo());
        EquipmentType.addType(AmmoType.createCLATM3ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM3HEAmmo());
        EquipmentType.addType(AmmoType.createCLATM6Ammo());
        EquipmentType.addType(AmmoType.createCLATM6ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM6HEAmmo());
        EquipmentType.addType(AmmoType.createCLATM9Ammo());
        EquipmentType.addType(AmmoType.createCLATM9ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM9HEAmmo());
        EquipmentType.addType(AmmoType.createCLATM12Ammo());
        EquipmentType.addType(AmmoType.createCLATM12ERAmmo());
        EquipmentType.addType(AmmoType.createCLATM12HEAmmo());
        EquipmentType.addType(AmmoType.createCLStreakLRM1Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM2Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM3Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM4Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM5Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM6Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM7Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM8Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM9Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM10Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM11Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM12Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM13Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM14Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM15Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM16Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM17Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM18Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM19Ammo());
        EquipmentType.addType(AmmoType.createCLStreakLRM20Ammo());
        EquipmentType.addType(AmmoType.createCLSRT1Ammo());
        EquipmentType.addType(AmmoType.createCLSRT2Ammo());
        EquipmentType.addType(AmmoType.createCLSRT3Ammo());
        EquipmentType.addType(AmmoType.createCLSRT4Ammo());
        EquipmentType.addType(AmmoType.createCLSRT5Ammo());
        EquipmentType.addType(AmmoType.createCLSRT6Ammo());
        EquipmentType.addType(AmmoType.createCLLRT1Ammo());
        EquipmentType.addType(AmmoType.createCLLRT2Ammo());
        EquipmentType.addType(AmmoType.createCLLRT3Ammo());
        EquipmentType.addType(AmmoType.createCLLRT4Ammo());
        EquipmentType.addType(AmmoType.createCLLRT5Ammo());
        EquipmentType.addType(AmmoType.createCLLRT6Ammo());
        EquipmentType.addType(AmmoType.createCLLRT7Ammo());
        EquipmentType.addType(AmmoType.createCLLRT8Ammo());
        EquipmentType.addType(AmmoType.createCLLRT9Ammo());
        EquipmentType.addType(AmmoType.createCLLRT10Ammo());
        EquipmentType.addType(AmmoType.createCLLRT11Ammo());
        EquipmentType.addType(AmmoType.createCLLRT12Ammo());
        EquipmentType.addType(AmmoType.createCLLRT13Ammo());
        EquipmentType.addType(AmmoType.createCLLRT14Ammo());
        EquipmentType.addType(AmmoType.createCLLRT15Ammo());
        EquipmentType.addType(AmmoType.createCLLRT16Ammo());
        EquipmentType.addType(AmmoType.createCLLRT17Ammo());
        EquipmentType.addType(AmmoType.createCLLRT18Ammo());
        EquipmentType.addType(AmmoType.createCLLRT19Ammo());
        EquipmentType.addType(AmmoType.createCLLRT20Ammo());
        EquipmentType.addType(AmmoType.createCLMPodAmmo());
        EquipmentType.addType(AmmoType.createCLBPodAmmo());
        EquipmentType.addType(AmmoType.createCLHAG20Ammo());
        EquipmentType.addType(AmmoType.createCLHAG30Ammo());
        EquipmentType.addType(AmmoType.createCLHAG40Ammo());
        EquipmentType.addType(AmmoType.createCLPlasmaCannonAmmo());
        EquipmentType.addType(AmmoType.createISPlasmaRifleAmmo());
        EquipmentType.addType(AmmoType.createCLAPGaussRifleAmmo());
        EquipmentType.addType(AmmoType.createCLMediumChemicalLaserAmmo());
        EquipmentType.addType(AmmoType.createCLSmallChemicalLaserAmmo());
        EquipmentType.addType(AmmoType.createCLLargeChemicalLaserAmmo());
        EquipmentType.addType(AmmoType.createISNailRivetGunAmmo());
        EquipmentType.addType(AmmoType.createISNailRivetGunAmmoHalf());
        EquipmentType.addType(AmmoType.createCLNailRivetGunAmmo());
        EquipmentType.addType(AmmoType.createCLNailRivetGunAmmoHalf());
        EquipmentType.addType(AmmoType.createCLRL10PrototypeAmmo());
        EquipmentType.addType(AmmoType.createCLRL15PrototypeAmmo());
        EquipmentType.addType(AmmoType.createCLRL20PrototypeAmmo());
        EquipmentType.addType(AmmoType.createISC3RemoteSensorAmmo());

        EquipmentType.addType(AmmoType.createCLIATM3Ammo());
        EquipmentType.addType(AmmoType.createCLIATM3ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM3HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM3IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM3IMPAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6Ammo());
        EquipmentType.addType(AmmoType.createCLIATM6ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM6IMPAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9Ammo());
        EquipmentType.addType(AmmoType.createCLIATM9ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM9IMPAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12Ammo());
        EquipmentType.addType(AmmoType.createCLIATM12ERAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12HEAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12IIWAmmo());
        EquipmentType.addType(AmmoType.createCLIATM12IMPAmmo());

        // Unofficial Ammo
        base = AmmoType.createISAC15Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        EquipmentType.addType(AmmoType.createISAC10iAmmo());
        base = AmmoType.createISGAC2Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISGAC4Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISGAC6Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createISGAC8Ammo();
        acAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLLongTomAmmo();
        clanArtyAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLongTomCannonAmmo();
        clanArtyAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSniperAmmo();
        clanArtyAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSniperCannonAmmo();
        clanArtyAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLThumperAmmo();
        EquipmentType.addType(base);
        clanArtyAmmos.add(base);
        base = AmmoType.createCLThumperCannonAmmo();
        clanArtyAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLArrowIVAmmo();
        clanArrowAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM1Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM2Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM3Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM4Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM5Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLSRM6Ammo();
        clanSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM1Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM2Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM3Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM4Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM5Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM6Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM7Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM8Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM9Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM10Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM11Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM12Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM13Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM14Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM15Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM16Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM17Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM18Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM19Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLLRM20Ammo();
        clanLrmAmmos.add(base);
        EquipmentType.addType(base);

        // Start of BattleArmor ammo
        EquipmentType.addType(AmmoType.createBAMicroBombAmmo());
        EquipmentType.addType(AmmoType.createCLTorpedoLRM5Ammo());
        EquipmentType.addType(AmmoType.createBACompactNarcAmmo());
        EquipmentType.addType(AmmoType.createBAMineLauncherAmmo());
        EquipmentType.addType(AmmoType.createAdvancedSRM1Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM2Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM3Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM4Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM5Ammo());
        EquipmentType.addType(AmmoType.createAdvancedSRM6Ammo());
        EquipmentType.addType(AmmoType.createBARL1Ammo());
        EquipmentType.addType(AmmoType.createBARL2Ammo());
        EquipmentType.addType(AmmoType.createBARL3Ammo());
        EquipmentType.addType(AmmoType.createBARL4Ammo());
        EquipmentType.addType(AmmoType.createBARL5Ammo());
        EquipmentType.addType(AmmoType.createISMRM1Ammo());
        EquipmentType.addType(AmmoType.createISMRM2Ammo());
        EquipmentType.addType(AmmoType.createISMRM3Ammo());
        EquipmentType.addType(AmmoType.createISMRM4Ammo());
        EquipmentType.addType(AmmoType.createISMRM5Ammo());
        EquipmentType.addType(AmmoType.createISBATaserAmmo());
        EquipmentType.addType(AmmoType.createBATubeArtyAmmo());
        base = AmmoType.createBAISLRM1Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM2Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM3Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM4Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBAISLRM5Ammo();
        isBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM1Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM2Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM3Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM4Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBACLLRM5Ammo();
        clanBaLrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM1Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM2Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM3Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM4Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM5Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createBASRM6Ammo();
        baSrmAmmos.add(base);
        EquipmentType.addType(base);

        // Protomech-specific ammo
        EquipmentType.addType(AmmoType.createCLPROHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROLightMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROAC2Ammo());
        EquipmentType.addType(AmmoType.createCLPROAC4Ammo());
        EquipmentType.addType(AmmoType.createCLPROAC8Ammo());

        // naval ammo
        EquipmentType.addType(AmmoType.createNAC10Ammo());
        EquipmentType.addType(AmmoType.createNAC20Ammo());
        EquipmentType.addType(AmmoType.createNAC25Ammo());
        EquipmentType.addType(AmmoType.createNAC30Ammo());
        EquipmentType.addType(AmmoType.createNAC35Ammo());
        EquipmentType.addType(AmmoType.createNAC40Ammo());
        EquipmentType.addType(AmmoType.createLightNGaussAmmo());
        EquipmentType.addType(AmmoType.createMediumNGaussAmmo());
        EquipmentType.addType(AmmoType.createHeavyNGaussAmmo());
        EquipmentType.addType(AmmoType.createCLLightNGaussAmmo());
        EquipmentType.addType(AmmoType.createCLMediumNGaussAmmo());
        EquipmentType.addType(AmmoType.createCLHeavyNGaussAmmo());
        EquipmentType.addType(AmmoType.createKrakenAmmo());
        EquipmentType.addType(AmmoType.createCLKrakenAmmo());
        EquipmentType.addType(AmmoType.createKillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createSantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createWhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createBarracudaAmmo());
        EquipmentType.addType(AmmoType.createCLKillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createCLSantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createCLWhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createCLBarracudaAmmo());
        EquipmentType.addType(AmmoType.createKillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createCLKillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createWhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createCLWhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createBarracudaTAmmo());
        EquipmentType.addType(AmmoType.createCLBarracudaTAmmo());
        EquipmentType.addType(AmmoType.createAR10KillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createAR10WhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createAR10SantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createAR10BarracudaAmmo());
        EquipmentType.addType(AmmoType.createCLAR10KillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createCLAR10WhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createCLAR10SantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createCLAR10BarracudaAmmo());
        EquipmentType.addType(AmmoType.createAR10KillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createCLAR10KillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createAR10WhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createCLAR10WhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createAR10BarracudaTAmmo());
        EquipmentType.addType(AmmoType.createScreenLauncherAmmo());
        EquipmentType.addType(AmmoType.createCLAR10BarracudaTAmmo());
        EquipmentType.addType(AmmoType.createAlamoAmmo());
        EquipmentType.addType(AmmoType.createCLLightSCCAmmo());
        EquipmentType.addType(AmmoType.createCLMediumSCCAmmo());
        EquipmentType.addType(AmmoType.createCLHeavySCCAmmo());
        EquipmentType.addType(AmmoType.createCLMantaRayAmmo());
        EquipmentType.addType(AmmoType.createCLSwordfishAmmo());
        EquipmentType.addType(AmmoType.createCLStingrayAmmo());
        EquipmentType.addType(AmmoType.createCLPiranhaAmmo());
        EquipmentType.addType(AmmoType.createLightSCCAmmo());
        EquipmentType.addType(AmmoType.createMediumSCCAmmo());
        EquipmentType.addType(AmmoType.createHeavySCCAmmo());
        EquipmentType.addType(AmmoType.createMantaRayAmmo());
        EquipmentType.addType(AmmoType.createSwordfishAmmo());
        EquipmentType.addType(AmmoType.createStingrayAmmo());
        EquipmentType.addType(AmmoType.createPiranhaAmmo());
        EquipmentType.addType(AmmoType.createKrakenMAmmo());
        EquipmentType.addType(AmmoType.createHeavyMassDriverAmmo());
        EquipmentType.addType(AmmoType.createMediumMassDriverAmmo());
        EquipmentType.addType(AmmoType.createLightMassDriverAmmo());

        base = AmmoType.createISAPMortar1Ammo();
        mortarAmmos.add(base);
        base = AmmoType.createISAPMortar2Ammo();
        mortarAmmos.add(base);
        base = AmmoType.createISAPMortar4Ammo();
        mortarAmmos.add(base);
        base = AmmoType.createISAPMortar8Ammo();
        mortarAmmos.add(base);

        base = AmmoType.createCLAPMortar1Ammo();
        clanMortarAmmos.add(base);
        base = AmmoType.createCLAPMortar2Ammo();
        clanMortarAmmos.add(base);
        base = AmmoType.createCLAPMortar4Ammo();
        clanMortarAmmos.add(base);
        base = AmmoType.createCLAPMortar8Ammo();
        clanMortarAmmos.add(base);
        
        // Create the munition types for IS Mek mortars
        munitions.clear();
        munitions.add(new MunitionMutator("Airburst", 1, M_AIRBURST, 2544, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Anti-personnel", 1, M_ANTI_PERSONNEL, 2531, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Flare", 1, M_FLARE, 2536, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Semi-Guided", 1, M_SEMIGUIDED, 3064, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD, 2531, TechConstants.T_IS_TW_NON_BOX));
        
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(mortarAmmos, munitions);
        
     // Create the munition types for Clan Mek mortars
        munitions.clear();
        munitions.add(new MunitionMutator("Airburst", 1, M_AIRBURST, 2544, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("Anti-personnel", 1, M_ANTI_PERSONNEL, 2531, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("Flare", 1, M_FLARE, 2536, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD, 2531, TechConstants.T_CLAN_ADVANCED));

        AmmoType.createMunitions(clanMortarAmmos, munitions);
        
        // Long range Torpedo
        base = AmmoType.createISLRT5Ammo();
        lrtAmmos.add(base);
        base = AmmoType.createISLRT10Ammo();
        lrtAmmos.add(base);
        base = AmmoType.createISLRT15Ammo();
        lrtAmmos.add(base);
        base = AmmoType.createISLRT20Ammo();
        lrtAmmos.add(base);

        EquipmentType.addType(AmmoType.createISLRT5Ammo());
        EquipmentType.addType(AmmoType.createISLRT10Ammo());
        EquipmentType.addType(AmmoType.createISLRT15Ammo());
        EquipmentType.addType(AmmoType.createISLRT20Ammo());

        base = AmmoType.createISSRT2Ammo();
        srtAmmos.add(base);
        base = AmmoType.createISSRT4Ammo();
        srtAmmos.add(base);
        base = AmmoType.createISSRT6Ammo();
        srtAmmos.add(base);

        EquipmentType.addType(AmmoType.createISSRT2Ammo());
        EquipmentType.addType(AmmoType.createISSRT4Ammo());
        EquipmentType.addType(AmmoType.createISSRT6Ammo());

        EquipmentType.addType(AmmoType.createISAPMortar1Ammo());
        EquipmentType.addType(AmmoType.createISAPMortar2Ammo());
        EquipmentType.addType(AmmoType.createISAPMortar4Ammo());
        EquipmentType.addType(AmmoType.createISAPMortar8Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar1Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar2Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar4Ammo());
        EquipmentType.addType(AmmoType.createCLAPMortar8Ammo());

        EquipmentType.addType(AmmoType.createISCruiseMissile50Ammo());
        EquipmentType.addType(AmmoType.createISCruiseMissile70Ammo());
        EquipmentType.addType(AmmoType.createISCruiseMissile90Ammo());
        EquipmentType.addType(AmmoType.createISCruiseMissile120Ammo());

        base = AmmoType.createISFluidGunAmmo();
        fluidGunAmmos.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLFluidGunAmmo();
        clanFluidGunAmmos.add(base);
        EquipmentType.addType(base);

        // Rifles
        EquipmentType.addType(AmmoType.createISLightRifleAmmo());
        EquipmentType.addType(AmmoType.createISMediumRifleAmmo());
        EquipmentType.addType(AmmoType.createISHeavyRifleAmmo());

        EquipmentType.addType(AmmoType.createISAPDSAmmo());

        base = AmmoType.createCLLRT5Ammo();
        clanLrtAmmos.add(base);
        base = AmmoType.createCLLRT10Ammo();
        clanLrtAmmos.add(base);
        base = AmmoType.createCLLRT15Ammo();
        clanLrtAmmos.add(base);
        base = AmmoType.createCLLRT20Ammo();
        clanLrtAmmos.add(base);

        base = AmmoType.createCLSRT2Ammo();
        clanSrtAmmos.add(base);
        base = AmmoType.createCLSRT4Ammo();
        clanSrtAmmos.add(base);
        base = AmmoType.createCLSRT6Ammo();
        clanSrtAmmos.add(base);

        base = AmmoType.createISVGLAmmo();
        EquipmentType.addType(base);
        vglAmmos.add(base);
        base = AmmoType.createCLVGLAmmo();
        clanVGLAmmos.add(base);
        EquipmentType.addType(base);

        // Create the munition types for IS SRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Inferno", 1, M_INFERNO, 2380, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Fragmentation", 1, M_FRAGMENTATION, 2385, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Narc-capable", 1, M_NARC_CAPABLE, 2597, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2598, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Listen-Kill", 1, M_LISTEN_KILL, 3038, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Anti-TSM", 1, M_ANTI_TSM, 3030, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Acid", 2, M_AX_HEAD, 3053, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Dead-Fire", 2, M_DEAD_FIRE, 3055, TechConstants.T_IS_UNOFFICIAL));
        munitions.add(new MunitionMutator("Heat-Seeking", 2, M_HEAT_SEEKING, 2340, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Tandem-Charge", 2, M_TANDEM_CHARGE, 2757, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD, 2333, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Mine Clearance", 1, M_MINE_CLEARANCE, 3069, TechConstants.T_IS_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(srmAmmos, munitions);
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for Clan SRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Inferno", 1, M_INFERNO, 2820,TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Fragmentation", 1, M_FRAGMENTATION, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Narc-capable", 1, M_NARC_CAPABLE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE, 3061, TechConstants.T_CLAN_EXPERIMENTAL).addTechLevel(3085, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Heat-Seeking", 2, M_HEAT_SEEKING, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Tandem-Charge", 2, M_TANDEM_CHARGE, 2757, TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Smoke", 1, M_SMOKE_WARHEAD, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Listen-Kill", 1,M_LISTEN_KILL,3050, TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Anti-TSM", 1, M_ANTI_TSM,3050,TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Acid", 2, M_AX_HEAD,3053,TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Dead-Fire", 2, M_DEAD_FIRE,3055,TechConstants.T_CLAN_UNOFFICIAL));
        munitions.add(new MunitionMutator("(Clan) Mine Clearance", 1, M_MINE_CLEARANCE, 3069, TechConstants.T_CLAN_UNOFFICIAL));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanSrmAmmos, munitions);
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for BA SRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Torpedo", 1, M_TORPEDO, 2380, TechConstants.T_ALLOWED_ALL));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for IS BA LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Torpedo", 1, M_TORPEDO, 2380, TechConstants.T_IS_TW_NON_BOX));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(isBaLrmAmmos, munitions);

        // Create the munition types for clan BA LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Multi-Purpose", 1, M_MULTI_PURPOSE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("Torpedo", 1, M_TORPEDO, 2820, TechConstants.T_CLAN_TW));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanBaLrmAmmos, munitions);

        // Create the munition types for IS LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Fragmentation", 1, M_FRAGMENTATION, 2385, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Thunder", 1, M_THUNDER, 2620, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Thunder-Augmented", 2, M_THUNDER_AUGMENTED, 3057, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Thunder-Inferno", 2, M_THUNDER_INFERNO, 3056, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Thunder-Active", 2, M_THUNDER_ACTIVE, 3058, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Thunder-Vibrabomb", 2, M_THUNDER_VIBRABOMB, 3057, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Narc-capable", 1, M_NARC_CAPABLE, 2597, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2598, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Semi-guided", 1, M_SEMIGUIDED, 3057, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Swarm", 1, M_SWARM, 2621, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Swarm-I", 1, M_SWARM_I, 3057, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Listen-Kill", 1, M_LISTEN_KILL, 3038, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Anti-TSM", 1, M_ANTI_TSM, 3030, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Dead-Fire", 2, M_DEAD_FIRE, 3055, TechConstants.T_IS_UNOFFICIAL));
        munitions.add(new MunitionMutator("Heat-Seeking", 2, M_HEAT_SEEKING, 2340, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Follow The Leader", 2, M_FOLLOW_THE_LEADER, 3053, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD, 2333, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Mine Clearance", 1, M_MINE_CLEARANCE, 3069, TechConstants.T_IS_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(lrmAmmos, munitions);
        AmmoType.createMunitions(enhancedlrmAmmos, munitions);
        AmmoType.createMunitions(isBaLrmAmmos, munitions);

        // Create the munition types for Clan LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Fragmentation", 1, M_FRAGMENTATION, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Thunder", 1, M_THUNDER, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Thunder-Augmented", 2, M_THUNDER_AUGMENTED,3057, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Thunder-Inferno", 2, M_THUNDER_INFERNO,3056, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Thunder-Active", 2, M_THUNDER_ACTIVE,3058, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Thunder-Vibrabomb", 2,M_THUNDER_VIBRABOMB,3058, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Swarm-I", 1, M_SWARM_I,3058,TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Semi-guided", 1,M_SEMIGUIDED,3058, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Narc-capable", 1, M_NARC_CAPABLE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE, 3061, TechConstants.T_CLAN_EXPERIMENTAL).addTechLevel(3085, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Swarm", 1, M_SWARM, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Heat-Seeking", 2, M_HEAT_SEEKING, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Follow The Leader", 2, M_FOLLOW_THE_LEADER, 2755, TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Smoke", 1, M_SMOKE_WARHEAD, 2825, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Listen-Kill", 1, M_LISTEN_KILL,3055,TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Anti-TSM", 1, M_ANTI_TSM,3055,TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("(Clan) Dead-Fire", 2, M_DEAD_FIRE,3055,TechConstants.T_CLAN_UNOFFICIAL));
        munitions.add(new MunitionMutator("(Clan) Mine Clearance", 1, M_MINE_CLEARANCE, 3069, TechConstants.T_CLAN_UNOFFICIAL));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanLrmAmmos, munitions);
        AmmoType.createMunitions(clanBaLrmAmmos, munitions);

        // Create the munition types for AC rounds.
        munitions.clear();
        munitions.add(new MunitionMutator("Precision", 2, M_PRECISION, 3062, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Armor-Piercing", 2, M_ARMOR_PIERCING, 3059, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE, 3055, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Incendiary", 1, M_INCENDIARY_AC, 3059, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Tracer", 1, M_TRACER, 2310, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Flak", 1, M_FLAK, 2310, TechConstants.T_IS_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(acAmmos, munitions);

        // Create the munition types for IS Arrow IV launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER, 2620, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("FASCAM", 1, M_FASCAM, 2621, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Homing", 1, M_HOMING, 2600, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Inferno-IV", 1, M_INFERNO_IV, 3055, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Laser Inhibiting", 1, M_LASER_INHIB, 3052, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, 2620, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Vibrabomb-IV", 1, M_VIBRABOMB_IV, 3065, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Davy Crockett-M", 5, M_DAVY_CROCKETT_M, 2620, TechConstants.T_IS_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Flare", 1, M_FLARE, 2620, TechConstants.T_IS_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(arrowAmmos, munitions);

        // Create the munition types for clan Arrow IV launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER, 2807, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("FASCAM", 1, M_FASCAM, 2807, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("Homing", 1, M_HOMING, 2807, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("Inferno-IV", 1, M_INFERNO_IV, 2807, TechConstants.T_CLAN_UNOFFICIAL));
        munitions.add(new MunitionMutator("Laser Inhibiting", 1, M_LASER_INHIB, 3052, TechConstants.T_CLAN_UNOFFICIAL));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, 2807, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("Vibrabomb-IV", 1, M_VIBRABOMB_IV, 3065, TechConstants.T_CLAN_UNOFFICIAL));
        munitions.add(new MunitionMutator("Davy Crockett-M", 5, M_DAVY_CROCKETT_M, 2807, TechConstants.T_CLAN_EXPERIMENTAL));
        munitions.add(new MunitionMutator("Flare", 1, M_FLARE, 2807, TechConstants.T_CLAN_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanArrowAmmos, munitions);

        // create the munition types for clan vehicular grenade launchers
        munitions.clear();
        munitions.add(new MunitionMutator("Chaff", 1, M_CHAFF, 3065, TechConstants.T_CLAN_ADVANCED).addTechLevel(3080, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("Incendiary", 1, M_INCENDIARY, 2820, TechConstants.T_CLAN_ADVANCED).addTechLevel(3080, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKEGRENADE, 2820, TechConstants.T_CLAN_ADVANCED).addTechLevel(3080, TechConstants.T_CLAN_TW));

        AmmoType.createMunitions(clanVGLAmmos, munitions);

        // create the munition types for IS vehicular grenade launchers
        munitions.clear();
        munitions.add(new MunitionMutator("Chaff", 1, M_CHAFF, 3052, TechConstants.T_IS_ADVANCED).addTechLevel(3080, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Incendiary", 1, M_INCENDIARY, 1950, TechConstants.T_IS_ADVANCED).addTechLevel(3080, TechConstants.T_IS_TW_NON_BOX));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKEGRENADE, 1950, TechConstants.T_IS_ADVANCED).addTechLevel(3080, TechConstants.T_IS_TW_NON_BOX));

        AmmoType.createMunitions(vglAmmos, munitions);

        // Create the munition types for Artillery launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER, 2645, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Copperhead", 1, M_HOMING, 2645, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("FASCAM", 1, M_FASCAM, 2621, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE, 1950, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, 1950, TechConstants.T_IS_ADVANCED));
        munitions.add(new MunitionMutator("Flare", 1, M_FLARE, 1950, TechConstants.T_IS_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(sniperAmmos, munitions);
        AmmoType.createMunitions(thumperAmmos, munitions);

        // Make Davy Crockett-Ms for Long Toms, but not Thumper or Sniper.
        munitions.add(new MunitionMutator("Davy Crockett-M", 5, M_DAVY_CROCKETT_M, 1950, TechConstants.T_IS_EXPERIMENTAL));
        AmmoType.createMunitions(longTomAmmos, munitions);

        // Create the munition types for Artillery Cannons.
        // These were taken out in TacOps errata, so are unofficial.
        munitions.clear();
        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER, 2645, TechConstants.T_IS_UNOFFICIAL));
        munitions.add(new MunitionMutator("Copperhead", 1, M_HOMING, 2645, TechConstants.T_IS_UNOFFICIAL));
        munitions.add(new MunitionMutator("FASCAM", 1, M_FASCAM, 2621, TechConstants.T_IS_UNOFFICIAL));
        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE, 1950, TechConstants.T_IS_UNOFFICIAL));
        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, 1950, TechConstants.T_IS_UNOFFICIAL));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(sniperCannonAmmos, munitions);
        AmmoType.createMunitions(thumperCannonAmmos, munitions);

        // Make Davy Crockett-Ms for Long Toms, but not Thumper or Sniper.
        munitions.add(new MunitionMutator("Davy Crockett-M", 5, M_DAVY_CROCKETT_M, 1950, TechConstants.T_IS_UNOFFICIAL));
        AmmoType.createMunitions(longTomCannonAmmos, munitions);

        // Create the munition types for Clan Artillery launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Cluster", 1, M_CLUSTER, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Copperhead", 1, M_HOMING, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) FASCAM", 1, M_FASCAM, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Flechette", 1, M_FLECHETTE, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Smoke", 1, M_SMOKE, 2820, TechConstants.T_CLAN_ADVANCED));
        munitions.add(new MunitionMutator("(Clan) Flare", 1, M_FLARE, 2820, TechConstants.T_CLAN_ADVANCED));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanArtyAmmos, munitions);

        // Create the munition types for SRT launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2598, TechConstants.T_IS_TW_NON_BOX));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(srtAmmos, munitions);

        // Create the munition types for SRT launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2598, TechConstants.T_IS_TW_NON_BOX));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(lrtAmmos, munitions);

        // Create the munition types for SRT launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE, 3061, TechConstants.T_CLAN_EXPERIMENTAL).addTechLevel(3085, TechConstants.T_CLAN_TW));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanSrtAmmos, munitions);

        // Create the munition types for SRT launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Artemis-capable", 1, M_ARTEMIS_CAPABLE, 2820, TechConstants.T_CLAN_TW));
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE, 3061, TechConstants.T_CLAN_EXPERIMENTAL).addTechLevel(3085, TechConstants.T_CLAN_TW));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanLrtAmmos, munitions);

        // Create the munition types for vehicle flamers
        munitions.clear();
        munitions.add(new MunitionMutator("Coolant", 1, M_COOLANT, 1950, TechConstants.T_IS_ADVANCED));
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(vehicleFlamerAmmos, munitions);

        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Coolant", 1, M_COOLANT, 1950, TechConstants.T_CLAN_ADVANCED));
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanVehicleFlamerAmmos, munitions);

        // Create the munition types for heavy flamers
        munitions.clear();
        munitions.add(new MunitionMutator("Coolant", 1, M_COOLANT, 1950, TechConstants.T_IS_ADVANCED));
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(heavyFlamerAmmos, munitions);

        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Coolant", 1, M_COOLANT, 3067, TechConstants.T_CLAN_ADVANCED));
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanHeavyFlamerAmmos, munitions);

        // cache types that share a launcher for loadout purposes
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e
                .hasMoreElements(); ) {
            EquipmentType et = e.nextElement();
            if (!(et instanceof AmmoType)) {
                continue;
            }
            AmmoType at = (AmmoType) et;
            int nType = at.getAmmoType();
            if (m_vaMunitions[nType] == null) {
                m_vaMunitions[nType] = new Vector<AmmoType>();
            }

            m_vaMunitions[nType].addElement(at);
        }
    }

    private static void createMunitions(List<AmmoType> bases,
                                        List<MunitionMutator> munitions) {
        for (AmmoType base : bases) {
            for (MunitionMutator mutator : munitions) {
                EquipmentType.addType(mutator.createMunitionType(base));
            }
        }
    }

    private static AmmoType createISAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "AC/2 Ammo";
        ammo.shortName = "AC/2";
        ammo.setInternalName("IS Ammo AC/2");
        ammo.addLookupName("ISAC2 Ammo");
        ammo.addLookupName("IS Autocannon/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_D, EquipmentType.RATING_D};
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAC2pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "AC/2p Ammo";
        ammo.shortName = "AC/2p";
        ammo.setInternalName("IS Ammo AC/2 Primitive");
        ammo.addLookupName("ISAC2p Ammo");
        ammo.addLookupName("IS Autocannon/2 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_PRIMITIVE;
        ammo.shots = 34;
        ammo.bv = 5;
        ammo.cost = 1000;

        return ammo;
    }

    private static AmmoType createISAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "AC/5 Ammo";
        ammo.shortName = "AC/5";
        ammo.setInternalName("IS Ammo AC/5");
        ammo.addLookupName("ISAC5 Ammo");
        ammo.addLookupName("IS Autocannon/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 20;
        ammo.bv = 9;
        ammo.cost = 4500;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_D};
        ammo.introDate = 2250;
        ammo.techLevel.put(2250, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAC5pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "AC/5p Ammo";
        ammo.shortName = "AC/5p";
        ammo.setInternalName("IS Ammo AC/5 Primitive");
        ammo.addLookupName("ISAC5p Ammo");
        ammo.addLookupName("IS Autocannon/5 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_PRIMITIVE;
        ammo.shots = 15;
        ammo.bv = 9;
        ammo.cost = 4500;

        return ammo;
    }

    private static AmmoType createISAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "AC/10 Ammo";
        ammo.shortName = "AC/10";
        ammo.setInternalName("IS Ammo AC/10");
        ammo.addLookupName("ISAC10 Ammo");
        ammo.addLookupName("IS Autocannon/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 10;
        ammo.bv = 15;
        ammo.cost = 6000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_D, EquipmentType.RATING_D};
        ammo.introDate = 2460;
        ammo.techLevel.put(2460, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAC15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "AC/15 Ammo";
        ammo.shortName = "AC/15";
        ammo.setInternalName("IS Ammo AC/15");
        ammo.addLookupName("ISAC15 Ammo");
        ammo.addLookupName("IS Autocannon/15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 7;
        ammo.bv = 22;
        ammo.cost = 8500;
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "AC/20 Ammo";
        ammo.shortName = "AC/20";
        ammo.setInternalName("IS Ammo AC/20");
        ammo.addLookupName("ISAC20 Ammo");
        ammo.addLookupName("IS Autocannon/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 5;
        ammo.bv = 22;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_D,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2500;
        ammo.techLevel.put(2500, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAC20pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "AC/20p Ammo";
        ammo.shortName = "AC/20p";
        ammo.setInternalName("IS Ammo AC/20 Primitive");
        ammo.addLookupName("ISAC20p Ammo");
        ammo.addLookupName("IS Autocannon/20 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_PRIMITIVE;
        ammo.shots = 4;
        ammo.bv = 22;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_D,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2488;
        ammo.techLevel.put(2488, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISVehicleFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "Vehicle Flamer Ammo";
        ammo.shortName = "Flamer";
        ammo.setInternalName("IS Vehicle Flamer Ammo");
        ammo.addLookupName("IS Ammo Vehicle Flamer");
        ammo.addLookupName("ISVehicleFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_VEHICLE_FLAMER;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_A,
                EquipmentType.RATING_A, EquipmentType.RATING_B};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("IS Ammo MG - Full");
        ammo.addLookupName("ISMG Ammo (200)");
        ammo.addLookupName("ISMG Ammo Full");
        ammo.addLookupName("IS Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_A,
                EquipmentType.RATING_A, EquipmentType.RATING_B};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "Half Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("IS Machine Gun Ammo - Half");
        ammo.addLookupName("IS Ammo MG - Half");
        ammo.addLookupName("ISMG Ammo (100)");
        ammo.addLookupName("ISMG Ammo Half");
        ammo.addLookupName("IS Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        ammo.cost = 500;
        ammo.availRating = new int[]{EquipmentType.RATING_A,
                EquipmentType.RATING_A, EquipmentType.RATING_B};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Heavy Machine Gun Ammo";
        ammo.shortName = "Heavy Machine Gun";
        ammo.setInternalName("IS Heavy Machine Gun Ammo - Full");
        ammo.addLookupName("ISHeavyMG Ammo (100)");
        ammo.addLookupName("IS Heavy Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_C};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Half Heavy Machine Gun Ammo";
        ammo.shortName = "Heavy Machine Gun";
        ammo.setInternalName("IS Heavy Machine Gun Ammo - Half");
        ammo.addLookupName("ISHeavyMG Ammo (50)");
        ammo.addLookupName("IS Heavy Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 50;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 500;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_C};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISLightMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Light Machine Gun Ammo";
        ammo.shortName = "Light Machine Gun";
        ammo.setInternalName("IS Light Machine Gun Ammo - Full");
        ammo.addLookupName("ISLightMG Ammo (200)");
        ammo.addLookupName("IS Light Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_C};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Half Light Machine Gun Ammo";
        ammo.shortName = "Light Machine Gun";
        ammo.setInternalName("IS Light Machine Gun Ammo - Half");
        ammo.addLookupName("ISLightMG Ammo (100)");
        ammo.addLookupName("IS Light Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 250;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_C};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("IS Ammo LRM-5");
        ammo.addLookupName("ISLRM5 Ammo");
        ammo.addLookupName("IS LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLRM5pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LRM 5p Ammo";
        ammo.shortName = "LRM 5p";
        ammo.setInternalName("IS Ammo LRM-5 Primitive");
        ammo.addLookupName("ISLRM5p Ammo");
        ammo.addLookupName("IS LRM 5 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_PRIMITIVE;
        ammo.shots = 18;
        ammo.bv = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "LRM 10 Ammo";
        ammo.shortName = "LRM 10";
        ammo.setInternalName("IS Ammo LRM-10");
        ammo.addLookupName("ISLRM10 Ammo");
        ammo.addLookupName("IS LRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISLRM10pAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LRM 10p Ammo";
        ammo.shortName = "LRM 10p";
        ammo.setInternalName("IS Ammo LRM-10 Primitive");
        ammo.addLookupName("ISLRM10p Ammo");
        ammo.addLookupName("IS LRM 10 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM_PRIMITIVE;
        ammo.shots = 9;
        ammo.bv = 11;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "LRM 15 Ammo";
        ammo.shortName = "LRM 15";
        ammo.setInternalName("IS Ammo LRM-15");
        ammo.addLookupName("ISLRM15 Ammo");
        ammo.addLookupName("IS LRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLRM15pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LRM 15p Ammo";
        ammo.shortName = "LRM 15p";
        ammo.setInternalName("IS Ammo LRM-15 Primitive");
        ammo.addLookupName("ISLRM15p Ammo");
        ammo.addLookupName("IS LRM 15 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM_PRIMITIVE;
        ammo.shots = 6;
        ammo.bv = 17;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "LRM 20 Ammo";
        ammo.shortName = "LRM 20";
        ammo.setInternalName("IS Ammo LRM-20");
        ammo.addLookupName("ISLRM20 Ammo");
        ammo.addLookupName("IS LRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLRM20pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LRM 20p Ammo";
        ammo.shortName = "LRM 20p";
        ammo.setInternalName("IS Ammo LRM-20 Primitive");
        ammo.addLookupName("ISLRM20p Ammo");
        ammo.addLookupName("IS LRM 20 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM_PRIMITIVE;
        ammo.shots = 5;
        ammo.bv = 23;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "SRM 2 Ammo";
        ammo.shortName = "SRM 2";
        ammo.setInternalName("IS Ammo SRM-2");
        ammo.addLookupName("ISSRM2 Ammo");
        ammo.addLookupName("IS SRM 2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISSRM2pAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "SRM 2p Ammo";
        ammo.shortName = "SRM 2p";
        ammo.setInternalName("IS Ammo SRM-2 Primitive");
        ammo.addLookupName("ISSRM2p Ammo");
        ammo.addLookupName("IS SRM 2 Primitive Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_PRIMITIVE;
        ammo.shots = 38;
        ammo.bv = 3;
        ammo.cost = 27000;

        return ammo;
    }

    private static AmmoType createISSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "SRM 4 Ammo";
        ammo.shortName = "SRM 4";
        ammo.setInternalName("IS Ammo SRM-4");
        ammo.addLookupName("ISSRM4 Ammo");
        ammo.addLookupName("IS SRM 4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;
        return ammo;
    }

    private static AmmoType createISSRM4pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "SRM 4p Ammo";
        ammo.shortName = "SRM 4p";
        ammo.setInternalName("IS Ammo SRM-4 Primitive");
        ammo.addLookupName("ISSRM4p Ammo");
        ammo.addLookupName("IS SRM 4 Primitive Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_PRIMITIVE;
        ammo.shots = 19;
        ammo.bv = 5;
        ammo.cost = 27000;

        return ammo;
    }

    private static AmmoType createISSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_INTRO_BOXSET);
        ammo.name = "SRM 6 Ammo";
        ammo.shortName = "SRM 6";
        ammo.setInternalName("IS Ammo SRM-6");
        ammo.addLookupName("ISSRM6 Ammo");
        ammo.addLookupName("IS SRM 6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;
        return ammo;
    }

    private static AmmoType createISSRM6pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "SRM 6p Ammo";
        ammo.shortName = "SRM 6p";
        ammo.setInternalName("IS Ammo SRM-6 Primitive");
        ammo.addLookupName("ISSRM6p Ammo");
        ammo.addLookupName("IS SRM 6 Primitive Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_PRIMITIVE;
        ammo.shots = 11;
        ammo.bv = 7;
        ammo.cost = 27000;

        return ammo;
    }

    private static AmmoType createISLRT5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LRT 5 Ammo";
        ammo.shortName = "LRT 5";
        ammo.setInternalName("IS Ammo LRTorpedo-5");
        ammo.addLookupName("ISLRTorpedo5 Ammo");
        ammo.addLookupName("IS LRTorpedo 5 Ammo");
        ammo.addLookupName("ISLRT5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLRT10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LRT 10 Ammo";
        ammo.shortName = "LRT 10";
        ammo.setInternalName("IS Ammo LRTorpedo-10");
        ammo.addLookupName("ISLRTorpedo10 Ammo");
        ammo.addLookupName("IS LRTorpedo 10 Ammo");
        ammo.addLookupName("ISLRT10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLRT15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LRT 15 Ammo";
        ammo.shortName = "LRT 15";
        ammo.setInternalName("IS Ammo LRTorpedo-15");
        ammo.addLookupName("ISLRTorpedo15 Ammo");
        ammo.addLookupName("IS LRv 15 Ammo");
        ammo.addLookupName("ISLRT15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLRT20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LRT 20 Ammo";
        ammo.shortName = "LRT 20";
        ammo.setInternalName("IS Ammo LRTorpedo-20");
        ammo.addLookupName("ISLRTorpedo20 Ammo");
        ammo.addLookupName("IS LRTorpedo 20 Ammo");
        ammo.addLookupName("ISLRT20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISSRT2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "SRT 2 Ammo";
        ammo.shortName = "SRT 2";
        ammo.setInternalName("IS Ammo SRTorpedo-2");
        ammo.addLookupName("ISSRTorpedo2 Ammo");
        ammo.addLookupName("IS SRTorpedo 2 Ammo");
        ammo.addLookupName("ISSRT2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_C, RATING_C, RATING_C};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISSRT4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "SRT 4 Ammo";
        ammo.shortName = "SRT 4";
        ammo.setInternalName("IS Ammo SRTorpedo-4");
        ammo.addLookupName("ISSRTorpedo4 Ammo");
        ammo.addLookupName("IS SRTorpedo 4 Ammo");
        ammo.addLookupName("ISSRT4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_C, RATING_C, RATING_C};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISSRT6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "SRT 6 Ammo";
        ammo.shortName = "SRT 6";
        ammo.setInternalName("IS Ammo SRTorpedo-6");
        ammo.addLookupName("ISSRTorpedo6 Ammo");
        ammo.addLookupName("IS SRTorpedo 6 Ammo");
        ammo.addLookupName("ISSRT6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.introDate = 2380;
        ammo.techLevel.put(2380, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_C, RATING_C, RATING_C};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISLongTomAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Long Tom Ammo";
        ammo.shortName = "Long Tom";
        ammo.setInternalName("ISLongTomAmmo");
        ammo.addLookupName("ISLongTom Ammo");
        ammo.addLookupName("ISLongTomArtillery Ammo");
        ammo.addLookupName("IS Ammo Long Tom");
        ammo.addLookupName("IS Long Tom Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoType.T_LONG_TOM;
        ammo.shots = 5;
        ammo.bv = 46;
        ammo.cost = 10000;
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_C, RATING_C, RATING_C};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISLongTomCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Long Tom Cannon Ammo";
        ammo.shortName = "Long Tom Cannon";
        ammo.setInternalName("ISLongTomCannonAmmo");
        ammo.addLookupName("ISLongTomCannon Ammo");
        ammo.addLookupName("ISLongTomArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Long Tom Cannon");
        ammo.addLookupName("IS Long Tom Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LONG_TOM_CANNON;
        ammo.shots = 5;
        ammo.bv = 41;
        ammo.cost = 20000;
        ammo.introDate = 3012;
        ammo.techLevel.put(3012, ammo.techLevel.get(3067));
        ammo.techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        ammo.availRating = new int[]{RATING_E, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLongTomCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Long Tom Cannon Ammo";
        ammo.shortName = "Long Tom Cannon";
        ammo.setInternalName("CLLongTomCannonAmmo");
        ammo.addLookupName("CLLongTomCannon Ammo");
        ammo.addLookupName("CLLongTomArtilleryCannon Ammo");
        ammo.addLookupName("CL Ammo Long Tom Cannon");
        ammo.addLookupName("CL Long Tom Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LONG_TOM_CANNON;
        ammo.shots = 5;
        ammo.bv = 41;
        ammo.cost = 20000;
        ammo.introDate = 3032;
        ammo.techLevel.put(3032, ammo.techLevel.get(3067));
        ammo.techLevel.put(3079, TechConstants.T_CLAN_ADVANCED);
        ammo.availRating = new int[]{RATING_X, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISSniperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Sniper Ammo";
        ammo.shortName = "Sniper";
        ammo.setInternalName("ISSniperAmmo");
        ammo.addLookupName("ISSniper Ammo");
        ammo.addLookupName("ISSniperArtillery Ammo");
        ammo.addLookupName("IS Ammo Sniper");
        ammo.addLookupName("IS Sniper Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_SNIPER;
        ammo.shots = 10;
        ammo.bv = 11;
        ammo.cost = 6000;
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_C, RATING_C, RATING_C};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISSniperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Sniper Cannon Ammo";
        ammo.shortName = "Sniper Cannon";
        ammo.setInternalName("ISSniperCannonAmmo");
        ammo.addLookupName("ISSniperCannon Ammo");
        ammo.addLookupName("ISSniperArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Sniper Cannon");
        ammo.addLookupName("IS Sniper Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_SNIPER_CANNON;
        ammo.shots = 10;
        ammo.bv = 10;
        ammo.cost = 15000;
        ammo.introDate = 3012;
        ammo.techLevel.put(3012, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        ammo.availRating = new int[]{RATING_E, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLSniperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Sniper Cannon Ammo";
        ammo.shortName = "Sniper Cannon";
        ammo.setInternalName("CLSniperCannonAmmo");
        ammo.addLookupName("CLSniperCannon Ammo");
        ammo.addLookupName("CLSniperArtilleryCannon Ammo");
        ammo.addLookupName("CL Ammo Sniper Cannon");
        ammo.addLookupName("CL Sniper Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_SNIPER_CANNON;
        ammo.shots = 10;
        ammo.bv = 10;
        ammo.cost = 15000;
        ammo.introDate = 3032;
        ammo.techLevel.put(3032, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_CLAN_ADVANCED);
        ammo.availRating = new int[]{RATING_X, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISThumperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Thumper Ammo";
        ammo.shortName = "Thumper";
        ammo.setInternalName("ISThumperAmmo");
        ammo.addLookupName("ISThumper Ammo");
        ammo.addLookupName("ISThumperArtillery Ammo");
        ammo.addLookupName("IS Ammo Thumper");
        ammo.addLookupName("IS Thumper Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_THUMPER;
        ammo.shots = 20;
        ammo.bv = 5;
        ammo.cost = 4500;
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_C, RATING_C, RATING_C};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISThumperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Thumper Cannon Ammo";
        ammo.shortName = "Thumper Cannon";
        ammo.setInternalName("ISThumperCannonAmmo");
        ammo.addLookupName("ISThumperCannon Ammo");
        ammo.addLookupName("ISThumperArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Thumper Cannon");
        ammo.addLookupName("IS Thumper Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_THUMPER_CANNON;
        ammo.shots = 20;
        ammo.bv = 5;
        ammo.cost = 10000;
        ammo.introDate = 3012;
        ammo.techLevel.put(3012, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        ammo.availRating = new int[]{RATING_E, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLThumperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Thumper Cannon Ammo";
        ammo.shortName = "Thumper Cannon";
        ammo.setInternalName("CLThumperCannonAmmo");
        ammo.addLookupName("CLThumperCannon Ammo");
        ammo.addLookupName("CLThumperArtilleryCannon Ammo");
        ammo.addLookupName("CL Ammo Thumper Cannon");
        ammo.addLookupName("CL Thumper Cannon Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_THUMPER_CANNON;
        ammo.shots = 20;
        ammo.bv = 5;
        ammo.cost = 10000;
        ammo.introDate = 3032;
        ammo.techLevel.put(3032, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_CLAN_ADVANCED);
        ammo.availRating = new int[]{RATING_X, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    // Start of Level2 Ammo

    private static AmmoType createISLB2XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 2-X AC Ammo";
        ammo.shortName = "LB 2-X";
        ammo.setInternalName("IS LB 2-X AC Ammo");
        ammo.addLookupName("IS Ammo 2-X");
        ammo.addLookupName("ISLBXAC2 Ammo");
        ammo.addLookupName("IS LB 2-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 2000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB5XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 5-X AC Ammo";
        ammo.shortName = "LB 5-X";
        ammo.setInternalName("IS LB 5-X AC Ammo");
        ammo.addLookupName("IS Ammo 5-X");
        ammo.addLookupName("ISLBXAC5 Ammo");
        ammo.addLookupName("IS LB 5-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 20;
        ammo.bv = 10;
        ammo.cost = 9000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB10XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 10-X AC Ammo";
        ammo.shortName = "LB 10-X";
        ammo.setInternalName("IS LB 10-X AC Ammo");
        ammo.addLookupName("IS Ammo 10-X");
        ammo.addLookupName("ISLBXAC10 Ammo");
        ammo.addLookupName("IS LB 10-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 12000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2595;
        ammo.techLevel.put(2595, ammo.techLevel.get(3067));
        ammo.extinctDate = 2840;
        ammo.reintroDate = 3035;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB20XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 20-X AC Ammo";
        ammo.shortName = "LB 20-X";
        ammo.setInternalName("IS LB 20-X AC Ammo");
        ammo.addLookupName("IS Ammo 20-X");
        ammo.addLookupName("ISLBXAC20 Ammo");
        ammo.addLookupName("IS LB 20-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 2-X Cluster Ammo";
        ammo.shortName = "LB 2-X Cluster";
        ammo.subMunitionBegin = 7;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 2-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 2-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC2 CL Ammo");
        ammo.addLookupName("IS LB 2-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 3300;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 5-X Cluster Ammo";
        ammo.shortName = "LB 5-X Cluster";
        ammo.subMunitionBegin = 7;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 5-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 5-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC5 CL Ammo");
        ammo.addLookupName("IS LB 5-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 20;
        ammo.bv = 10;
        ammo.cost = 15000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 10-X Cluster Ammo";
        ammo.shortName = "LB 10-X Cluster";
        ammo.subMunitionBegin = 8;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 10-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 10-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC10 CL Ammo");
        ammo.addLookupName("IS LB 10-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2595;
        ammo.techLevel.put(2595, ammo.techLevel.get(3067));
        ammo.extinctDate = 2840;
        ammo.reintroDate = 3035;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LB 20-X Cluster Ammo";
        ammo.shortName = "LB 20-X Cluster";
        ammo.subMunitionBegin = 8;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 20-X Cluster Ammo");
        ammo.addLookupName("IS Ammo 20-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC20 CL Ammo");
        ammo.addLookupName("IS LB 20-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 34000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISTHBLB2XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "LB 2-X AC Ammo (THB)";
        ammo.shortName = "LB 2-X";
        ammo.setInternalName("IS LB 2-X AC Ammo (THB)");
        ammo.addLookupName("IS Ammo 2-X (THB)");
        ammo.addLookupName("ISLBXAC2 Ammo (THB)");
        ammo.addLookupName("IS LB 2-X AC Ammo - Slug (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.shots = 40;
        ammo.bv = 5;
        ammo.cost = 3000;

        return ammo;
    }

    private static AmmoType createISTHBLB5XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "LB 5-X AC Ammo (THB)";
        ammo.shortName = "LB 5-X";
        ammo.setInternalName("IS LB 5-X AC Ammo (THB)");
        ammo.addLookupName("IS Ammo 5-X (THB)");
        ammo.addLookupName("ISLBXAC5 Ammo (THB)");
        ammo.addLookupName("IS LB 5-X AC Ammo - Slug (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.shots = 16;
        ammo.bv = 11;
        ammo.cost = 15000;

        return ammo;
    }

    private static AmmoType createISTHBLB20XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LB 20-X AC Ammo (THB)";
        ammo.shortName = "LB 20-X";
        ammo.setInternalName("IS LB 20-X AC Ammo (THB)");
        ammo.addLookupName("IS Ammo 20-X (THB)");
        ammo.addLookupName("ISLBXAC20 Ammo (THB)");
        ammo.addLookupName("IS LB 20-X AC Ammo - Slug (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.shots = 4;
        ammo.bv = 26;
        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISTHBLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LB 2-X Cluster Ammo (THB)";
        ammo.shortName = "LB 2-X Cluster";
        ammo.subMunitionBegin = 7;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 2-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 2-X (CL) (THB)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC2 CL Ammo (THB)");
        ammo.addLookupName("IS LB 2-X AC Ammo - Cluster (THB)");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 40;
        ammo.bv = 5;
        ammo.cost = 4950;

        return ammo;
    }

    private static AmmoType createISTHBLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LB 5-X Cluster Ammo (THB)";
        ammo.shortName = "LB 5-X Cluster";
        ammo.subMunitionBegin = 7;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 5-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 5-X (CL) (THB)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC5 CL Ammo (THB)");
        ammo.addLookupName("IS LB 5-X AC Ammo - Cluster (THB)");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 16;
        ammo.bv = 11;
        ammo.cost = 25000;

        return ammo;
    }

    private static AmmoType createISTHBLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LB 20-X Cluster Ammo (THB)";
        ammo.shortName = "LB 20-X Cluster";
        ammo.subMunitionBegin = 8;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("IS LB 20-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 20-X (CL) (THB)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC20 CL Ammo (THB)");
        ammo.addLookupName("IS LB 20-X AC Ammo - Cluster (THB)");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 4;
        ammo.bv = 26;
        ammo.cost = 51000;

        return ammo;
    }

    private static AmmoType createISUltra2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Ultra AC/2 Ammo";
        ammo.shortName = "Ultra AC/2";
        ammo.setInternalName("IS Ultra AC/2 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/2");
        ammo.addLookupName("ISUltraAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 7;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISUltra5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Ultra AC/5 Ammo";
        ammo.shortName = "Ultra AC/5";
        ammo.setInternalName("IS Ultra AC/5 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/5");
        ammo.addLookupName("ISUltraAC5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 9000;
        ammo.availRating = new int[]{EquipmentType.RATING_D,
                EquipmentType.RATING_F, EquipmentType.RATING_D};
        ammo.introDate = 2640;
        ammo.techLevel.put(2640, ammo.techLevel.get(3067));
        ammo.extinctDate = 2915;
        ammo.reintroDate = 3035;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISUltra10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Ultra AC/10 Ammo";
        ammo.shortName = "Ultra AC/10";
        ammo.setInternalName("IS Ultra AC/10 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/10");
        ammo.addLookupName("ISUltraAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 12000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISUltra20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Ultra AC/20 Ammo";
        ammo.shortName = "Ultra AC/20";
        ammo.setInternalName("IS Ultra AC/20 Ammo");
        ammo.addLookupName("IS Ammo Ultra AC/20");
        ammo.addLookupName("ISUltraAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 35;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISTHBUltra2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Ultra AC/2 Ammo (THB)";
        ammo.shortName = "Ultra AC/2";
        ammo.setInternalName("IS Ultra AC/2 Ammo (THB)");
        ammo.addLookupName("IS Ammo Ultra AC/2 (THB)");
        ammo.addLookupName("ISUltraAC2 Ammo (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA_THB;
        ammo.shots = 45;
        ammo.bv = 8;
        ammo.cost = 2000;

        return ammo;
    }

    private static AmmoType createISTHBUltra10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Ultra AC/10 Ammo (THB)";
        ammo.shortName = "Ultra AC/10";
        ammo.setInternalName("IS Ultra AC/10 Ammo (THB)");
        ammo.addLookupName("IS Ammo Ultra AC/10 (THB)");
        ammo.addLookupName("ISUltraAC10 Ammo (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA_THB;
        ammo.shots = 10;
        ammo.bv = 31;
        ammo.cost = 15000;

        return ammo;
    }

    private static AmmoType createISTHBUltra20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Ultra AC/20 Ammo (THB)";
        ammo.shortName = "Ultra AC/20";
        ammo.setInternalName("IS Ultra AC/20 Ammo (THB)");
        ammo.addLookupName("IS Ammo Ultra AC/20 (THB)");
        ammo.addLookupName("ISUltraAC20 Ammo (THB)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA_THB;
        ammo.shots = 5;
        ammo.bv = 42;
        ammo.cost = 30000;

        return ammo;
    }

    private static AmmoType createISRotary2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Rotary AC/2 Ammo";
        ammo.shortName = "RAC/2";
        ammo.setInternalName("ISRotaryAC2 Ammo");
        ammo.addLookupName("IS Rotary AC/2 Ammo");
        ammo.addLookupName("ISRAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 45;
        ammo.bv = 15;
        ammo.cost = 3000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISRotary5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Rotary AC/5 Ammo";
        ammo.shortName = "RAC/5";
        ammo.setInternalName("ISRotaryAC5 Ammo");
        ammo.addLookupName("IS Rotary AC/5 Ammo");
        ammo.addLookupName("ISRAC5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 20;
        ammo.bv = 31;
        ammo.cost = 12000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISRotary10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Rotary AC/10 Ammo";
        ammo.shortName = "RAC/10";
        ammo.setInternalName("ISRotaryAC10 Ammo");
        ammo.addLookupName("IS Rotary AC/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 10;
        ammo.bv = 37;
        ammo.cost = 30000;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISRotary20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Rotary AC/20 Ammo";
        ammo.shortName = "RAC/20";
        ammo.setInternalName("ISRotaryAC20 Ammo");
        ammo.addLookupName("IS Rotary AC/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 5;
        ammo.bv = 59;
        ammo.cost = 80000;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Gauss Ammo";
        ammo.shortName = "Gauss";
        ammo.setInternalName("IS Gauss Ammo");
        ammo.addLookupName("IS Ammo Gauss");
        ammo.addLookupName("ISGauss Ammo");
        ammo.addLookupName("IS Gauss Rifle Ammo");
        ammo.addLookupName("ISGaussRifle Ammo");
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS;
        ammo.shots = 8;
        ammo.bv = 40;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_D,
                EquipmentType.RATING_F, EquipmentType.RATING_D};
        ammo.introDate = 2590;
        ammo.techLevel.put(2590, ammo.techLevel.get(3067));
        ammo.extinctDate = 2865;
        ammo.reintroDate = 3040;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLTGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Light Gauss Ammo";
        ammo.shortName = "Light Gauss";
        ammo.setInternalName("IS Light Gauss Ammo");
        ammo.addLookupName("ISLightGauss Ammo");
        ammo.addLookupName("IS Light Gauss Rifle Ammo");
        ammo.addLookupName("ISLightGaussRifle Ammo");
        ammo.damagePerShot = 8;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS_LIGHT;
        ammo.shots = 16;
        ammo.bv = 20;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISHVGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Heavy Gauss Ammo";
        ammo.shortName = "Heavy Gauss";
        ammo.setInternalName("ISHeavyGauss Ammo");
        ammo.addLookupName("IS Heavy Gauss Rifle Ammo");
        ammo.addLookupName("ISHeavyGaussRifle Ammo");
        ammo.damagePerShot = 25; // actually variable
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS_HEAVY;
        ammo.shots = 4;
        ammo.bv = 43;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3061;
        ammo.techLevel.put(3061, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISIHVGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3065, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Improved Heavy Gauss Ammo";
        ammo.shortName = "iHeavy Gauss";
        ammo.setInternalName("ISImprovedHeavyGauss Ammo");
        ammo.addLookupName("IS Improved Heavy Gauss Rifle Ammo");
        ammo.addLookupName("ISImprovedHeavyGaussRifle Ammo");
        ammo.damagePerShot = 22;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_IGAUSS_HEAVY;
        ammo.shots = 4;
        ammo.bv = 48;
        ammo.cost = 20000;
        ammo.introDate = 3065;
        ammo.techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);

        return ammo;
    }

    private static AmmoType createISStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Streak SRM 2 Ammo";
        ammo.shortName = "Streak SRM 2";
        ammo.setInternalName("IS Streak SRM 2 Ammo");
        ammo.addLookupName("IS Ammo Streak-2");
        ammo.addLookupName("ISStreakSRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 4;
        ammo.cost = 54000;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_F, EquipmentType.RATING_D};
        ammo.introDate = 2647;
        ammo.techLevel.put(2647, ammo.techLevel.get(3067));
        ammo.extinctDate = 2845;
        ammo.reintroDate = 3035;

        return ammo;
    }

    private static AmmoType createISStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Streak SRM 4 Ammo";
        ammo.shortName = "Streak SRM 4";
        ammo.setInternalName("IS Streak SRM 4 Ammo");
        ammo.addLookupName("IS Ammo Streak-4");
        ammo.addLookupName("ISStreakSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 7;
        ammo.cost = 54000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Streak SRM 6 Ammo";
        ammo.shortName = "Streak SRM 6";
        ammo.setInternalName("IS Streak SRM 6 Ammo");
        ammo.addLookupName("IS Ammo Streak-6");
        ammo.addLookupName("ISStreakSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 11;
        ammo.cost = 54000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISMRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 10 Ammo";
        ammo.shortName = "MRM 10";
        ammo.setInternalName("IS MRM 10 Ammo");
        ammo.addLookupName("ISMRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.cost = 5000;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISMRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 20 Ammo";
        ammo.shortName = "MRM 20";
        ammo.setInternalName("IS MRM 20 Ammo");
        ammo.addLookupName("ISMRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.cost = 5000;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISMRM30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 30 Ammo";
        ammo.shortName = "MRM 30";
        ammo.setInternalName("IS MRM 30 Ammo");
        ammo.addLookupName("ISMRM30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 5000;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISMRM40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 40 Ammo";
        ammo.shortName = "MRM 40";
        ammo.setInternalName("IS MRM 40 Ammo");
        ammo.addLookupName("ISMRM40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 6;
        ammo.bv = 28;
        ammo.cost = 5000;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISRL10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 10 Ammo";
        ammo.setInternalName("IS Ammo RL-10");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISRL15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 15 Ammo";
        ammo.setInternalName("IS Ammo RL-15");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISRL20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 20 Ammo";
        ammo.setInternalName("IS Ammo RL-20");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 2000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.techRating = RATING_B;
        return ammo;
    }

    private static AmmoType createCLRL10PrototypeAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "RL Prototype 10 Ammo";
        ammo.setInternalName("IS Ammo RL-Prototype-10");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1000;
        ammo.introDate = 2801;
        ammo.techLevel.put(2801, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createCLRL15PrototypeAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "RL Prototype 15 Ammo";
        ammo.setInternalName("CL Ammo RL-Prototype-15");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.introDate = 2801;
        ammo.techLevel.put(2801, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createCLRL20PrototypeAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "RL Prototype 20 Ammo";
        ammo.setInternalName("CL Ammo RL-Prototype-20");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 2000;
        ammo.introDate = 2801;
        ammo.techLevel.put(2801, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISAMSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "AMS Ammo";
        ammo.shortName = "AMS";
        ammo.setInternalName("ISAMS Ammo");
        ammo.addLookupName("IS Ammo AMS");
        ammo.addLookupName("IS AMS Ammo");
        ammo.damagePerShot = 1; // only used for ammo crits
        ammo.rackSize = 2; // only used for ammo crits
        ammo.ammoType = AmmoType.T_AMS;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 2000;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_F, EquipmentType.RATING_D};
        ammo.introDate = 2617;
        ammo.techLevel.put(2617, ammo.techLevel.get(3067));
        ammo.extinctDate = 2796;
        ammo.reintroDate = 3040;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISAPDSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3137, TechConstants.T_IS_ADVANCED);
        ammo.name = "APDS Ammo";
        ammo.shortName = "APDS";
        ammo.setInternalName("ISAPDS Ammo");
        ammo.damagePerShot = 1; // only used for ammo crits
        ammo.rackSize = 2; // only used for ammo crits
        ammo.ammoType = AmmoType.T_APDS;
        ammo.shots = 12;
        ammo.bv = 22;
        ammo.cost = 2000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_X, RATING_E};
        ammo.introDate = 3137;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISNarcAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Narc Pods";
        ammo.shortName = "Narc";
        ammo.setInternalName("ISNarc Pods");
        ammo.addLookupName("IS Ammo Narc");
        ammo.addLookupName("IS Narc Missile Beacon Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 6000;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_F, EquipmentType.RATING_D};
        ammo.introDate = 2587;
        ammo.techLevel.put(2587, ammo.techLevel.get(3067));
        ammo.extinctDate = 2795;
        ammo.reintroDate = 3035;
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Narc Explosive Pods";
        ammo.shortName = "Narc Explosive";
        ammo.setInternalName("ISNarc ExplosivePods");
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.munitionType = M_NARC_EX;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISiNarcAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "iNarc Pods";
        ammo.shortName = "iNarc";
        ammo.setInternalName("ISiNarc Pods");
        ammo.addLookupName("IS Ammo iNarc");
        ammo.addLookupName("IS iNarc Missile Beacon Ammo");
        ammo.addLookupName("iNarc Ammo");
        ammo.damagePerShot = 3; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_INARC;
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 7500;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISiNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "iNarc Explosive Pods";
        ammo.shortName = "iNarc Explosive";
        ammo.setInternalName("ISiNarc Explosive Pods");
        ammo.addLookupName("iNarc Explosive Ammo");
        ammo.damagePerShot = 6; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_INARC;
        ammo.munitionType = AmmoType.M_EXPLOSIVE;
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISiNarcECMAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "iNarc ECM Pods";
        ammo.shortName = "iNarc ECM";
        ammo.setInternalName("ISiNarc ECM Pods");
        ammo.addLookupName("iNarc ECM Ammo");
        ammo.damagePerShot = 3; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_INARC;
        ammo.munitionType = AmmoType.M_ECM;
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 15000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISiNarcHaywireAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "iNarc Haywire Pods";
        ammo.shortName = "iNarc Haywire";
        ammo.setInternalName("ISiNarc Haywire Pods");
        ammo.addLookupName("iNarc Haywire Ammo");
        ammo.damagePerShot = 3; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_INARC;
        ammo.munitionType = AmmoType.M_HAYWIRE;
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 20000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISiNarcNemesisAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "iNarc Nemesis Pods";
        ammo.shortName = "iNarc Nemesis";
        ammo.setInternalName("ISiNarc Nemesis Pods");
        ammo.addLookupName("iNarc Nemesis Ammo");
        ammo.damagePerShot = 3; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_INARC;
        ammo.munitionType = AmmoType.M_NEMESIS;
        ammo.shots = 4;
        ammo.bv = 0;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISFluidGunAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Fluid Gun Ammo";
        ammo.shortName = "Fluid Gun";
        ammo.setInternalName("ISFluidGun Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_FLUID_GUN;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.explosive = false;

        return ammo;
    }

    private static AmmoType createCLFluidGunAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Fluid Gun Ammo";
        ammo.shortName = "Fluid Gun";
        ammo.setInternalName("CLFluidGun Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_FLUID_GUN;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_B, RATING_B, RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createCLGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Gauss Ammo";
        ammo.shortName = "Gauss";
        ammo.setInternalName("Clan Gauss Ammo");
        ammo.addLookupName("Clan Ammo Gauss");
        ammo.addLookupName("CLGauss Ammo");
        ammo.addLookupName("Clan Gauss Rifle Ammo");
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS;
        ammo.shots = 8;
        ammo.bv = 40;
        ammo.cost = 20000;
        ammo.kgPerShot = 125;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_D, EquipmentType.RATING_C};
        ammo.introDate = 2828;
        ammo.techLevel.put(2828, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createCLLB2XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 2-X AC Ammo";
        ammo.shortName = "LB-2X";
        ammo.setInternalName("Clan LB 2-X AC Ammo");
        ammo.addLookupName("Clan Ammo 2-X");
        ammo.addLookupName("CLLBXAC2 Ammo");
        ammo.addLookupName("Clan LB 2-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.cost = 2000;
        ammo.kgPerShot = 20;
        ammo.introDate = 2826;
        ammo.techLevel.put(2826, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createCLLB5XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 5-X AC Ammo";
        ammo.shortName = "LB-5X";
        ammo.setInternalName("Clan LB 5-X AC Ammo");
        ammo.addLookupName("Clan Ammo 5-X");
        ammo.addLookupName("CLLBXAC5 Ammo");
        ammo.addLookupName("Clan LB 5-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 20;
        ammo.bv = 12;
        ammo.cost = 9000;
        ammo.kgPerShot = 50;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLB10XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 10-X AC Ammo";
        ammo.shortName = "LB-10X";
        ammo.setInternalName("Clan LB 10-X AC Ammo");
        ammo.addLookupName("Clan Ammo 10-X");
        ammo.addLookupName("CLLBXAC10 Ammo");
        ammo.addLookupName("Clan LB 10-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 12000;
        ammo.kgPerShot = 100;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_E, RATING_D};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLB20XAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 20-X AC Ammo";
        ammo.shortName = "LB-20X";
        ammo.setInternalName("Clan LB 20-X AC Ammo");
        ammo.addLookupName("Clan Ammo 20-X");
        ammo.addLookupName("CLLBXAC20 Ammo");
        ammo.addLookupName("Clan LB 20-X AC Ammo - Slug");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 20000;
        ammo.introDate = 2826;
        ammo.techLevel.put(2826, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 2-X Cluster Ammo";
        ammo.shortName = "LB-2X Cluster";
        ammo.subMunitionBegin = 6;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("Clan LB 2-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 2-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC2 CL Ammo");
        ammo.addLookupName("Clan LB 2-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.cost = 3300;
        ammo.introDate = 2826;
        ammo.techLevel.put(2826, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 5-X Cluster Ammo";
        ammo.shortName = "LB-5X Cluster";
        ammo.subMunitionBegin = 6;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("Clan LB 5-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 5-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC5 CL Ammo");
        ammo.addLookupName("Clan LB 5-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 20;
        ammo.bv = 12;
        ammo.cost = 15000;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 10-X Cluster Ammo";
        ammo.shortName = "LB-10X Cluster";
        ammo.subMunitionBegin = 7;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("Clan LB 10-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 10-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC10 CL Ammo");
        ammo.addLookupName("Clan LB 10-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.cost = 20000;
        ammo.kgPerShot = 100;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_D, EquipmentType.RATING_C};
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LB 20-X Cluster Ammo";
        ammo.shortName = "LB-20X Cluster";
        ammo.subMunitionBegin = 7;
        ammo.subMunitionLength = 7;
        ammo.setInternalName("Clan LB 20-X Cluster Ammo");
        ammo.addLookupName("Clan Ammo 20-X (CL)");
        // this isn't a true mtf code
        ammo.addLookupName("CLLBXAC20 CL Ammo");
        ammo.addLookupName("Clan LB 20-X AC Ammo - Cluster");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 34000;
        ammo.introDate = 2826;
        ammo.techLevel.put(2826, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLVehicleFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Vehicle Flamer Ammo";
        ammo.shortName = "Vehicle Flamer";
        ammo.setInternalName("Clan Vehicle Flamer Ammo");
        ammo.addLookupName("Clan Ammo Vehicle Flamer");
        ammo.addLookupName("CLVehicleFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_VEHICLE_FLAMER;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.kgPerShot = 50;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_A, EquipmentType.RATING_A};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createCLHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Heavy Machine Gun Ammo";
        ammo.shortName = "Heavy Machine Gun";
        ammo.setInternalName("Clan Heavy Machine Gun Ammo - Full");
        ammo.addLookupName("CLHeavyMG Ammo (100)");
        ammo.addLookupName("Clan Heavy Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.kgPerShot = 10;
        ammo.techRating = RATING_A;
        ammo.introDate = 3059;
        ammo.techLevel.put(3059, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_C};

        return ammo;
    }

    private static AmmoType createCLHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Half Heavy Machine Gun Ammo";
        ammo.shortName = "Heavy Machine Gun";
        ammo.setInternalName("Clan Heavy Machine Gun Ammo - Half");
        ammo.addLookupName("CLHeavyMG Ammo (50)");
        ammo.addLookupName("Clan Heavy Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 50;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 500;
        ammo.kgPerShot = 10;
        ammo.techRating = RATING_A;
        ammo.introDate = 3059;
        ammo.techLevel.put(3059, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_C};

        return ammo;
    }

    private static AmmoType createCLMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("Clan Machine Gun Ammo - Full");
        ammo.addLookupName("Clan Ammo MG - Full");
        ammo.addLookupName("CLMG Ammo (200)");
        ammo.addLookupName("Clan Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.kgPerShot = 5;
        ammo.techRating = RATING_A;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_A, RATING_A};

        return ammo;
    }

    private static AmmoType createCLMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Half Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("Clan Machine Gun Ammo - Half");
        ammo.addLookupName("Clan Ammo MG - Half");
        ammo.addLookupName("CLMG Ammo (100)");
        ammo.addLookupName("Clan Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 500;
        ammo.kgPerShot = 5;
        ammo.techRating = RATING_A;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_A, RATING_A};

        return ammo;
    }

    private static AmmoType createCLLightMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Light Machine Gun Ammo";
        ammo.shortName = "Light Machine Gun";
        ammo.setInternalName("Clan Light Machine Gun Ammo - Full");
        ammo.addLookupName("CLLightMG Ammo (200)");
        ammo.addLookupName("Clan Light Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.kgPerShot = 5;
        ammo.techRating = RATING_A;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_C};

        return ammo;
    }

    private static AmmoType createCLLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Half Light Machine Gun Ammo";
        ammo.shortName = "Light Machine Gun";
        ammo.setInternalName("Clan Light Machine Gun Ammo - Half");
        ammo.addLookupName("CLLightMG Ammo (100)");
        ammo.addLookupName("Clan Light Machine Gun Ammo (1/2 ton)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG);
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 0.5f;
        ammo.cost = 250;
        ammo.kgPerShot = 5;
        ammo.techRating = RATING_A;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_C};

        return ammo;
    }

    private static AmmoType createCLUltra2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Ultra AC/2 Ammo";
        ammo.shortName = "Ultra AC/2";
        ammo.setInternalName("Clan Ultra AC/2 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/2");
        ammo.addLookupName("CLUltraAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 8;
        ammo.cost = 1000;
        ammo.kgPerShot = 20;
        ammo.introDate = 2827;
        ammo.techLevel.put(2827, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLUltra5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Ultra AC/5 Ammo";
        ammo.shortName = "Ultra AC/5";
        ammo.setInternalName("Clan Ultra AC/5 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/5");
        ammo.addLookupName("CLUltraAC5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 15;
        ammo.cost = 9000;
        ammo.kgPerShot = 50;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLUltra10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Ultra AC/10 Ammo";
        ammo.shortName = "Ultra AC/10";
        ammo.setInternalName("Clan Ultra AC/10 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/10");
        ammo.addLookupName("CLUltraAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 12000;
        ammo.introDate = 2825;
        ammo.kgPerShot = 100;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLUltra20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Ultra AC/20 Ammo";
        ammo.shortName = "Ultra AC/20";
        ammo.setInternalName("Clan Ultra AC/20 Ammo");
        ammo.addLookupName("Clan Ammo Ultra AC/20");
        ammo.addLookupName("CLUltraAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 42;
        ammo.cost = 20000;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLRotary2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3073, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Rotary AC/2 Ammo";
        ammo.shortName = "RAC/2";
        ammo.setInternalName("CLRotaryAC2 Ammo");
        ammo.addLookupName("CL Rotary AC/2 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 45;
        ammo.bv = 20;
        ammo.cost = 5000;
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3073));
        ammo.techLevel.put(3104, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F; 

        return ammo;
    }

    private static AmmoType createCLRotary5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3073, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Rotary AC/5 Ammo";
        ammo.shortName = "RAC/5";
        ammo.setInternalName("CLRotaryAC5 Ammo");
        ammo.addLookupName("CL Rotary AC/5 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 20;
        ammo.bv = 43;
        ammo.cost = 13000;
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3073));
        ammo.techLevel.put(3104, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLRotary10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Rotary AC/10 Ammo";
        ammo.shortName = "RAC/10";
        ammo.setInternalName("CLRotaryAC10 Ammo");
        ammo.addLookupName("CL Rotary AC/10 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 10;
        ammo.bv = 74;
        ammo.cost = 16000;
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLRotary20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Rotary AC/20 Ammo";
        ammo.shortName = "RAC/20";
        ammo.setInternalName("CLRotaryAC20 Ammo");
        ammo.addLookupName("CL Rotary AC/20 Ammo");
        ammo.addLookupName("Rotary Assault Cannon/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 5;
        ammo.bv = 118;
        ammo.cost = 24000;
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLLRT1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 1 Ammo";
        ammo.shortName = "LRT 1";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-1");
        ammo.addLookupName("Clan Ammo LRTorpedo-1");
        ammo.addLookupName("CLLRTorpedo1 Ammo");
        ammo.addLookupName("Clan LRTorpedo 1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT2Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 2 Ammo";
        ammo.shortName = "LRT 2";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-2");
        ammo.addLookupName("Clan Ammo LRTorpedo-2");
        ammo.addLookupName("CLLRTorpedo2 Ammo");
        ammo.addLookupName("Clan LRTorpedo 2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 3 Ammo";
        ammo.shortName = "LRT 3";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-3");
        ammo.addLookupName("Clan Ammo LRTorpedo-3");
        ammo.addLookupName("CLLRTorpedo3 Ammo");
        ammo.addLookupName("Clan LRTorpedo 3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 5;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT4Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 4 Ammo";
        ammo.shortName = "LRT 4";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-4");
        ammo.addLookupName("Clan Ammo LRTorpedo-4");
        ammo.addLookupName("CLLRTorpedo4 Ammo");
        ammo.addLookupName("Clan LRTorpedo 4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 5 Ammo";
        ammo.shortName = "LRT 5";
        ammo.setInternalName("Clan Ammo LRTorpedo-5");
        ammo.addLookupName("CLLRTorpedo5 Ammo");
        ammo.addLookupName("Clan LRTorpedo 5 Ammo");
        ammo.addLookupName("CLLRT5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLLRT6Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 6 Ammo";
        ammo.shortName = "LRT 6";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-6");
        ammo.addLookupName("Clan Ammo LRTorpedo-6");
        ammo.addLookupName("CLLRTorpedo6 Ammo");
        ammo.addLookupName("Clan LRTorpedo 6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 9;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT7Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 7 Ammo";
        ammo.shortName = "LRT 7";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-7");
        ammo.addLookupName("Clan Ammo LRTorpedo-7");
        ammo.addLookupName("CLLRTorpedo7 Ammo");
        ammo.addLookupName("Clan LRTorpedo 7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 10;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT8Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 8 Ammo";
        ammo.shortName = "LRT 8";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-8");
        ammo.addLookupName("Clan Ammo LRTorpedo-8");
        ammo.addLookupName("CLLRTorpedo8 Ammo");
        ammo.addLookupName("Clan LRTorpedo 8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 11;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT9Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 9 Ammo";
        ammo.shortName = "LRT 9";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-9");
        ammo.addLookupName("Clan Ammo LRTorpedo-9");
        ammo.addLookupName("CLLRTorpedo9 Ammo");
        ammo.addLookupName("Clan LRTorpedo 9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 12;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 10 Ammo";
        ammo.shortName = "LRT 10";
        ammo.setInternalName("Clan Ammo LRTorpedo-10");
        ammo.addLookupName("CLLRTorpedo10 Ammo");
        ammo.addLookupName("Clan LRTorpedo 10 Ammo");
        ammo.addLookupName("CLLRT10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLLRT11Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 11 Ammo";
        ammo.shortName = "LRT 11";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-11");
        ammo.addLookupName("Clan Ammo LRTorpedo-11");
        ammo.addLookupName("CLLRTorpedo11 Ammo");
        ammo.addLookupName("Clan LRTorpedo 11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT12Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 12 Ammo";
        ammo.shortName = "LRT 12";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-12");
        ammo.addLookupName("Clan Ammo LRTorpedo-12");
        ammo.addLookupName("CLLRTorpedo12 Ammo");
        ammo.addLookupName("Clan LRTorpedo 12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT13Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 13 Ammo";
        ammo.shortName = "LRT 13";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-13");
        ammo.addLookupName("Clan Ammo LRTorpedo-13");
        ammo.addLookupName("CLLRTorpedo13 Ammo");
        ammo.addLookupName("Clan LRTorpedo 13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 20;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT14Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 14 Ammo";
        ammo.shortName = "LRT 14";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-14");
        ammo.addLookupName("Clan Ammo LRTorpedo-14");
        ammo.addLookupName("CLLRTorpedo14 Ammo");
        ammo.addLookupName("Clan LRTorpedo 14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 21;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 15 Ammo";
        ammo.shortName = "LRT 15";
        ammo.setInternalName("Clan Ammo LRTorpedo-15");
        ammo.addLookupName("CLLRTorpedo15 Ammo");
        ammo.addLookupName("Clan LRTorpedo 15 Ammo");
        ammo.addLookupName("CLLRT15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLLRT16Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 16 Ammo";
        ammo.shortName = "LRT 16";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-16");
        ammo.addLookupName("Clan Ammo LRTorpedo-16");
        ammo.addLookupName("CLLRTorpedo16 Ammo");
        ammo.addLookupName("Clan LRTorpedo 16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT17Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 17 Ammo";
        ammo.shortName = "LRT 17";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-17");
        ammo.addLookupName("Clan Ammo LRTorpedo-17");
        ammo.addLookupName("CLLRTorpedo17 Ammo");
        ammo.addLookupName("Clan LRTorpedo 17 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT18Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 18 Ammo";
        ammo.shortName = "LRT 18";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-18");
        ammo.addLookupName("Clan Ammo LRTorpedo-18");
        ammo.addLookupName("CLLRTorpedo18 Ammo");
        ammo.addLookupName("Clan LRTorpedo 18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT19Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 19 Ammo";
        ammo.shortName = "LRT 19";
        ammo.setInternalName("Clan Ammo Protomech LRTorpedo-19");
        ammo.addLookupName("Clan Ammo LRTorpedo-19");
        ammo.addLookupName("CLLRTorpedo19 Ammo");
        ammo.addLookupName("Clan LRTorpedo 19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRT20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRT 20 Ammo";
        ammo.shortName = "LRT 20";
        ammo.setInternalName("Clan Ammo LRTorpedo-20");
        ammo.addLookupName("CLLRTorpedo20 Ammo");
        ammo.addLookupName("Clan LRTorpedo 20 Ammo");
        ammo.addLookupName("CLLRT20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRM 1 Ammo";
        ammo.shortName = "SRM 1";
        ammo.setInternalName("Clan Ammo SRM-1");
        ammo.addLookupName("CLSRM1 Ammo");
        ammo.addLookupName("Clan SRM 1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRM 2 Ammo";
        ammo.shortName = "SRM 2";
        ammo.setInternalName("Clan Ammo SRM-2");
        ammo.addLookupName("CLSRM2 Ammo");
        ammo.addLookupName("Clan SRM 2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_B, RATING_A};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRM 3 Ammo";
        ammo.shortName = "SRM 3";
        ammo.setInternalName("Clan Ammo SRM-3");
        ammo.addLookupName("CLSRM3 Ammo");
        ammo.addLookupName("Clan SRM 3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.kgPerShot = 10;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRM 4 Ammo";
        ammo.shortName = "SRM 4";
        ammo.setInternalName("Clan Ammo SRM-4");
        ammo.addLookupName("CLSRM4 Ammo");
        ammo.addLookupName("Clan SRM 4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_B, RATING_A};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRM 5 Ammo";
        ammo.shortName = "SRM 5";
        ammo.setInternalName("Clan Ammo SRM-5");
        ammo.addLookupName("CLSRM5 Ammo");
        ammo.addLookupName("Clan SRM 5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 10;
        ammo.techRating = RATING_C;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};

        return ammo;
    }

    private static AmmoType createCLSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRM 6 Ammo";
        ammo.shortName = "SRM 6";
        ammo.setInternalName("Clan Ammo SRM-6");
        ammo.addLookupName("CLSRM6 Ammo");
        ammo.addLookupName("Clan SRM 6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.techRating = RATING_C;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_B, RATING_A};

        return ammo;
    }

    private static AmmoType createCLLRM1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 1 Ammo";
        ammo.shortName = "LRM 1";
        ammo.setInternalName("Clan Ammo Protomech LRM-1");
        ammo.addLookupName("Clan Ammo LRM-1");
        ammo.addLookupName("CLLRM1 Ammo");
        ammo.addLookupName("Clan LRM 1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM2Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 2 Ammo";
        ammo.shortName = "LRM 2";
        ammo.setInternalName("Clan Ammo Protomech LRM-2");
        ammo.addLookupName("Clan Ammo LRM-2");
        ammo.addLookupName("CLLRM2 Ammo");
        ammo.addLookupName("Clan LRM 2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 3 Ammo";
        ammo.shortName = "LRM 3";
        ammo.setInternalName("Clan Ammo Protomech LRM-3");
        ammo.addLookupName("Clan Ammo LRM-3");
        ammo.addLookupName("CLLRM3 Ammo");
        ammo.addLookupName("Clan LRM 3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 5;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM4Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 4 Ammo";
        ammo.shortName = "LRM 4";
        ammo.setInternalName("Clan Ammo Protomech LRM-4");
        ammo.addLookupName("Clan Ammo LRM-4");
        ammo.addLookupName("CLLRM4 Ammo");
        ammo.addLookupName("Clan LRM 4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("Clan Ammo LRM-5");
        ammo.addLookupName("CLLRM5 Ammo");
        ammo.addLookupName("Clan LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.techRating = RATING_C;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[] { RATING_X, RATING_C, RATING_B };

        return ammo;
    }

    private static AmmoType createCLLRM6Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 6 Ammo";
        ammo.shortName = "LRM 6";
        ammo.setInternalName("Clan Ammo Protomech LRM-6");
        ammo.addLookupName("Clan Ammo LRM-6");
        ammo.addLookupName("CLLRM6 Ammo");
        ammo.addLookupName("Clan LRM 6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 9;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM7Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 7 Ammo";
        ammo.shortName = "LRM 7";
        ammo.setInternalName("Clan Ammo Protomech LRM-7");
        ammo.addLookupName("Clan Ammo LRM-7");
        ammo.addLookupName("CLLRM7 Ammo");
        ammo.addLookupName("Clan LRM 7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 10;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM8Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 8 Ammo";
        ammo.shortName = "LRM 8";
        ammo.setInternalName("Clan Ammo Protomech LRM-8");
        ammo.addLookupName("Clan Ammo LRM-8");
        ammo.addLookupName("CLLRM8 Ammo");
        ammo.addLookupName("Clan LRM 8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 11;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM9Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 9 Ammo";
        ammo.shortName = "LRM 9";
        ammo.setInternalName("Clan Ammo Protomech LRM-9");
        ammo.addLookupName("Clan Ammo LRM-9");
        ammo.addLookupName("CLLRM9 Ammo");
        ammo.addLookupName("Clan LRM 9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 12;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 10 Ammo";
        ammo.shortName = "LRM 10";
        ammo.setInternalName("Clan Ammo LRM-10");
        ammo.addLookupName("CLLRM10 Ammo");
        ammo.addLookupName("Clan LRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.techRating = RATING_C;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[] { RATING_X, RATING_C, RATING_B };

        return ammo;
    }

    private static AmmoType createCLLRM11Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 11 Ammo";
        ammo.shortName = "LRM 11";
        ammo.setInternalName("Clan Ammo Protomech LRM-11");
        ammo.addLookupName("Clan Ammo LRM-11");
        ammo.addLookupName("CLLRM11 Ammo");
        ammo.addLookupName("Clan LRM 11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM12Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 12 Ammo";
        ammo.setInternalName("Clan Ammo Protomech LRM-12");
        ammo.shortName = "LRM 12";
        ammo.addLookupName("Clan Ammo LRM-12");
        ammo.addLookupName("CLLRM12 Ammo");
        ammo.addLookupName("Clan LRM 12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 18;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM13Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 13 Ammo";
        ammo.shortName = "LRM 13";
        ammo.setInternalName("Clan Ammo Protomech LRM-13");
        ammo.addLookupName("Clan Ammo LRM-13");
        ammo.addLookupName("CLLRM13 Ammo");
        ammo.addLookupName("Clan LRM 13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 20;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM14Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 14 Ammo";
        ammo.shortName = "LRM 14";
        ammo.setInternalName("Clan Ammo Protomech LRM-14");
        ammo.addLookupName("Clan Ammo LRM-14");
        ammo.addLookupName("CLLRM14 Ammo");
        ammo.addLookupName("Clan LRM 14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 21;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.shortName = "LRM 15";
        ammo.name = "LRM 15 Ammo";
        ammo.setInternalName("Clan Ammo LRM-15");
        ammo.addLookupName("CLLRM15 Ammo");
        ammo.addLookupName("Clan LRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.techRating = RATING_C;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[] { RATING_X, RATING_C, RATING_B };

        return ammo;
    }

    private static AmmoType createCLLRM16Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 16 Ammo";
        ammo.shortName = "LRM 16";
        ammo.setInternalName("Clan Ammo Protomech LRM-16");
        ammo.addLookupName("Clan Ammo LRM-16");
        ammo.addLookupName("CLLRM16 Ammo");
        ammo.addLookupName("Clan LRM 16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM17Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 17 Ammo";
        ammo.shortName = "LRM 17";
        ammo.setInternalName("Clan Ammo Protomech LRM-17");
        ammo.addLookupName("Clan Ammo LRM-17");
        ammo.addLookupName("CLLRM17 Ammo");
        ammo.addLookupName("Clan LRM 17 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM18Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 18 Ammo";
        ammo.shortName = "LRM 18";
        ammo.setInternalName("Clan Ammo Protomech LRM-18");
        ammo.addLookupName("Clan Ammo LRM-18");
        ammo.addLookupName("CLLRM18 Ammo");
        ammo.addLookupName("Clan LRM 18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM19Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.shortName = "LRM 19";
        ammo.name = "LRM 19 Ammo";
        ammo.setInternalName("Clan Ammo Protomech LRM-19");
        ammo.addLookupName("Clan Ammo LRM-19");
        ammo.addLookupName("CLLRM19 Ammo");
        ammo.addLookupName("Clan LRM 19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 27;
        ammo.kgPerShot = 8.33;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_C;
        return ammo;

    }

    private static AmmoType createCLLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "LRM 20 Ammo";
        ammo.shortName = "LRM 20";
        ammo.setInternalName("Clan Ammo LRM-20");
        ammo.addLookupName("CLLRM20 Ammo");
        ammo.addLookupName("Clan LRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.cost = 30000;
        ammo.kgPerShot = 8.33;
        ammo.techRating = RATING_C;
        ammo.introDate = 2824;
        ammo.techLevel.put(2824, ammo.techLevel.get(3067));
        ammo.availRating = new int[] { RATING_X, RATING_C, RATING_B };

        return ammo;
    }

    private static AmmoType createCLSRT1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRT 1 Ammo";
        ammo.shortName = "SRT 1";
        ammo.setInternalName("Clan Ammo SRTorpedo-1");
        ammo.addLookupName("CLSRTorpedo1 Ammo");
        ammo.addLookupName("Clan SRTorpedo 1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRT2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRT 2 Ammo";
        ammo.shortName = "SRT 2";
        ammo.setInternalName("Clan Ammo SRTorpedo-2");
        ammo.addLookupName("CLSRTorpedo2 Ammo");
        ammo.addLookupName("Clan SRTorpedo 2 Ammo");
        ammo.addLookupName("CLSRT2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.cost = 27000;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRT3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRT 3 Ammo";
        ammo.shortName = "SRT 3";
        ammo.setInternalName("Clan Ammo SRTorpedo-3");
        ammo.addLookupName("CLSRTorpedo3 Ammo");
        ammo.addLookupName("Clan SRTorpedo 3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRT4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRT 4 Ammo";
        ammo.shortName = "SRT 4";
        ammo.setInternalName("Clan Ammo SRTorpedo-4");
        ammo.addLookupName("CLSRTorpedo4 Ammo");
        ammo.addLookupName("Clan SRTorpedo 4 Ammo");
        ammo.addLookupName("CLSRT4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.cost = 27000;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRT5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "SRT 5 Ammo";
        ammo.shortName = "SRT 5";
        ammo.setInternalName("Clan Ammo SRTorpedo-5");
        ammo.addLookupName("CLSRTorpedo5 Ammo");
        ammo.addLookupName("Clan SRTorpedo 5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLSRT6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.shortName = "SRT 6";
        ammo.name = "SRT 6 Ammo";
        ammo.setInternalName("Clan Ammo SRTorpedo-6");
        ammo.addLookupName("CLSRTorpedo6 Ammo");
        ammo.addLookupName("Clan SRTorpedo 6 Ammo");
        ammo.addLookupName("CLSRT6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_TORPEDO;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLStreakSRM1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Streak SRM 1 Ammo";
        ammo.shortName = "Streak SRM 1";
        ammo.setInternalName("Clan Streak SRM 1 Ammo");
        ammo.addLookupName("Clan Ammo Streak-1");
        ammo.addLookupName("CLStreakSRM1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.kgPerShot = 10;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createCLStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Streak SRM 2 Ammo";
        ammo.shortName = "Streak SRM 2";
        ammo.setInternalName("Clan Streak SRM 2 Ammo");
        ammo.addLookupName("Clan Ammo Streak-2");
        ammo.addLookupName("CLStreakSRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 5;
        ammo.cost = 54000;
        ammo.kgPerShot = 10;
        ammo.introDate = 2822;
        ammo.techLevel.put(2822, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLStreakSRM3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Streak SRM 3 Ammo";
        ammo.shortName = "Streak SRM 3";
        ammo.setInternalName("Clan Streak SRM 3 Ammo");
        ammo.addLookupName("Clan Ammo Streak-3");
        ammo.addLookupName("CLStreakSRM3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 7;
        ammo.kgPerShot = 10;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Streak SRM 4 Ammo";
        ammo.shortName = "Streak SRM 4";
        ammo.setInternalName("Clan Streak SRM 4 Ammo");
        ammo.addLookupName("Clan Ammo Streak-4");
        ammo.addLookupName("CLStreakSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 10;
        ammo.cost = 54000;
        ammo.kgPerShot = 10;
        ammo.introDate = 2822;
        ammo.techLevel.put(2822, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLStreakSRM5Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Streak SRM 5 Ammo";
        ammo.shortName = "Streak SRM 5";
        ammo.setInternalName("Clan Streak SRM 5 Ammo");
        ammo.addLookupName("Clan Ammo Streak-5");
        ammo.addLookupName("CLStreakSRM5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 13;
        ammo.kgPerShot = 10;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Streak SRM 6 Ammo";
        ammo.shortName = "Streak SRM 6";
        ammo.setInternalName("Clan Streak SRM 6 Ammo");
        ammo.addLookupName("Clan Ammo Streak-6");
        ammo.addLookupName("CLStreakSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 15;
        ammo.cost = 54000;
        ammo.kgPerShot = 10;
        ammo.introDate = 2822;
        ammo.techLevel.put(2822, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLAMSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "AMS Ammo";
        ammo.shortName = "AMS";
        ammo.setInternalName("CLAMS Ammo");
        ammo.addLookupName("Clan Ammo AMS");
        ammo.addLookupName("Clan AMS Ammo");
        ammo.damagePerShot = 1; // only used for ammo crits
        ammo.rackSize = 2; // only used for ammo crits
        ammo.ammoType = AmmoType.T_AMS;
        ammo.shots = 24;
        ammo.bv = 22;
        ammo.cost = 2000;
        ammo.kgPerShot = 40;
        ammo.introDate = 2831;
        ammo.techLevel.put(2831, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_E, RATING_D};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLNarcAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Narc Pods";
        ammo.shortName = "Narc";
        ammo.setInternalName("CLNarc Pods");
        ammo.addLookupName("Clan Ammo Narc");
        ammo.addLookupName("Clan Narc Missile Beacon Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 6000;
        ammo.kgPerShot = 150;
        ammo.introDate = 2828;
        ammo.techLevel.put(2828, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Narc Explosive Pods";
        ammo.shortName = "Narc Explosive";
        ammo.setInternalName("CLNarc Explosive Pods");
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.munitionType = AmmoType.M_NARC_EX;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.kgPerShot = 150;
        ammo.introDate = 2828;
        ammo.techLevel.put(2828, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLSmallChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3071, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Small Chemical Laser Ammo";
        ammo.shortName = "Small Chemical Laser";
        ammo.setInternalName("CLSmallChemLaserAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_CHEMICAL_LASER;
        ammo.shots = 60;
        ammo.bv = 1;
        ammo.cost = 30000;
        ammo.introDate = 3059;
        ammo.techLevel.put(3083, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLMediumChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Medium Chemical Laser Ammo";
        ammo.shortName = "Medium Chemical Laser";
        ammo.setInternalName("CLMediumChemLaserAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_CHEMICAL_LASER;
        ammo.shots = 30;
        ammo.bv = 5;
        ammo.cost = 30000;
        ammo.introDate = 3059;
        ammo.techLevel.put(3083, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLLargeChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Large Chemical Laser Ammo";
        ammo.shortName = "Large Chemical Laser";
        ammo.setInternalName("CLLargeChemLaserAmmo");
        ammo.damagePerShot = 8;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_CHEMICAL_LASER;
        ammo.shots = 10;
        ammo.bv = 12;
        ammo.cost = 30000;
        ammo.introDate = 3059;
        ammo.techLevel.put(3083, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;

    }

    private static AmmoType createISMML3LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 3 LRM Ammo";
        ammo.shortName = "MML 3/LRM";
        ammo.setInternalName("IS Ammo MML-3 LRM");
        ammo.addLookupName("ISMML3 LRM Ammo");
        ammo.addLookupName("IS MML-3 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 40;
        ammo.bv = 4;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createISMML3SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 3 SRM Ammo";
        ammo.shortName = "MML 3/SRM";
        ammo.setInternalName("IS Ammo MML-3 SRM");
        ammo.addLookupName("ISMML3 SRM Ammo");
        ammo.addLookupName("IS MML-3 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 33;
        ammo.bv = 4;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMML5LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 5 LRM Ammo";
        ammo.shortName = "MML 5/LRM";
        ammo.setInternalName("IS Ammo MML-5 LRM");
        ammo.addLookupName("ISMML5 LRM Ammo");
        ammo.addLookupName("IS MML-5 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMML5SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 5 SRM Ammo";
        ammo.shortName = "MML 5/SRM";
        ammo.setInternalName("IS Ammo MML-5 SRM");
        ammo.addLookupName("ISMML5 SRM Ammo");
        ammo.addLookupName("IS MML-5 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 20;
        ammo.bv = 6;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMML7LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 7 LRM Ammo";
        ammo.shortName = "MML 7/LRM";
        ammo.setInternalName("IS Ammo MML-7 LRM");
        ammo.addLookupName("ISMML7 LRM Ammo");
        ammo.addLookupName("IS MML-7 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 17;
        ammo.bv = 8;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMML7SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 7 SRM Ammo";
        ammo.shortName = "MML 7/SRM";
        ammo.setInternalName("IS Ammo MML-7 SRM");
        ammo.addLookupName("ISMML7 SRM Ammo");
        ammo.addLookupName("IS MML-7 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 14;
        ammo.bv = 8;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMML9LRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 9 LRM Ammo";
        ammo.shortName = "MML 9/LRM";
        ammo.setInternalName("IS Ammo MML-9 LRM");
        ammo.addLookupName("ISMML9 LRM Ammo");
        ammo.addLookupName("IS MML-9 LRM Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 13;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_LRM);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.introDate = 2300;
        ammo.techLevel.put(2300, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMML9SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MML 9 SRM Ammo";
        ammo.shortName = "MML 9/SRM";
        ammo.setInternalName("IS Ammo MML-9 SRM");
        ammo.addLookupName("ISMML9 SRM Ammo");
        ammo.addLookupName("IS MML-9 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_MML;
        ammo.shots = 11;
        ammo.bv = 11;
        ammo.cost = 27000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2370;
        ammo.techLevel.put(2370, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLATM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 3 Ammo";
        ammo.shortName = "ATM 3";
        ammo.setInternalName("Clan Ammo ATM-3");
        ammo.addLookupName("CLATM3 Ammo");
        ammo.addLookupName("Clan ATM-3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM3ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 3 ER Ammo";
        ammo.shortName = "ATM 3 ER";
        ammo.setInternalName("Clan Ammo ATM-3 ER");
        ammo.addLookupName("CLATM3 ER Ammo");
        ammo.addLookupName("Clan ATM-3 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM3HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 3 HE Ammo";
        ammo.shortName = "ATM 3 HE";
        ammo.setInternalName("Clan Ammo ATM-3 HE");
        ammo.addLookupName("CLATM3 HE Ammo");
        ammo.addLookupName("Clan ATM-3 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.cost = 75000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 6 Ammo";
        ammo.shortName = "ATM 6";
        ammo.setInternalName("Clan Ammo ATM-6");
        ammo.addLookupName("CLATM6 Ammo");
        ammo.addLookupName("Clan ATM-6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM6ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 6 ER Ammo";
        ammo.shortName = "ATM 6 ER";
        ammo.setInternalName("Clan Ammo ATM-6 ER");
        ammo.addLookupName("CLATM6 ER Ammo");
        ammo.addLookupName("Clan ATM-6 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM6HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 6 HE Ammo";
        ammo.shortName = "ATM 6 HE";
        ammo.setInternalName("Clan Ammo ATM-6 HE");
        ammo.addLookupName("CLATM6 HE Ammo");
        ammo.addLookupName("Clan ATM-6 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 75000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 9 Ammo";
        ammo.shortName = "ATM 9";
        ammo.setInternalName("Clan Ammo ATM-9");
        ammo.addLookupName("CLATM9 Ammo");
        ammo.addLookupName("Clan ATM-9 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM9ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 9 ER Ammo";
        ammo.shortName = "ATM 9 ER";
        ammo.setInternalName("Clan Ammo ATM-9 ER");
        ammo.addLookupName("CLATM9 ER Ammo");
        ammo.addLookupName("Clan ATM-9 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM9HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 9 HE Ammo";
        ammo.shortName = "ATM 9 HE";
        ammo.setInternalName("Clan Ammo ATM-9 HE");
        ammo.addLookupName("CLATM9 HE Ammo");
        ammo.addLookupName("Clan ATM-9 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.cost = 75000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 12 Ammo";
        ammo.shortName = "ATM 12";
        ammo.setInternalName("Clan Ammo ATM-12");
        ammo.addLookupName("CLATM12 Ammo");
        ammo.addLookupName("Clan ATM-12 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM12ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 12 ER Ammo";
        ammo.shortName = "ATM 12 ER";
        ammo.setInternalName("Clan Ammo ATM-12 ER");
        ammo.addLookupName("CLATM12 ER Ammo");
        ammo.addLookupName("Clan ATM-12 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLATM12HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "ATM 12 HE Ammo";
        ammo.shortName = "ATM 12 HE";
        ammo.setInternalName("Clan Ammo ATM-12 HE");
        ammo.addLookupName("CLATM12 HE Ammo");
        ammo.addLookupName("Clan ATM-12 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.cost = 75000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 3 Ammo";
        ammo.shortName = "iATM 3";
        ammo.setInternalName("Clan Ammo iATM-3");
        ammo.addLookupName("CLIATM3 Ammo");
        ammo.addLookupName("Clan iATM-3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.shots = 20;
        ammo.bv = 21;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM3ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 3 ER Ammo";
        ammo.shortName = "iATM 3 ER";
        ammo.setInternalName("Clan Ammo iATM-3 ER");
        ammo.addLookupName("CLIATM3 ER Ammo");
        ammo.addLookupName("Clan iATM-3 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 20;
        ammo.bv = 21;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM3HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 3 HE Ammo";
        ammo.shortName = "iATM 3 HE";
        ammo.setInternalName("Clan Ammo iATM-3 HE");
        ammo.addLookupName("CLIATM3 HE Ammo");
        ammo.addLookupName("Clan iATM-3 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 20;
        ammo.bv = 21;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM3IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 3 IIW Ammo";
        ammo.shortName = "iATM 3 IIW";
        ammo.setInternalName("Clan Ammo iATM-3 IIW");
        ammo.addLookupName("CLIATM3 IIW Ammo");
        ammo.addLookupName("Clan iATM-3 IIW Ammo");
        ammo.addLookupName("CLIIW3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IIW;
        ammo.shots = 20;
        ammo.bv = 27; // 21 * 1.3 = 27.3, round down (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM3IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 3 IMP Ammo";
        ammo.shortName = "iATM 3 IMP";
        ammo.setInternalName("Clan Ammo iATM-3 IMP");
        ammo.addLookupName("CLIATM3 IMP Ammo");
        ammo.addLookupName("Clan iATM-3 IMP Ammo");
        ammo.addLookupName("CLIMP3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IMP;
        ammo.shots = 20;
        ammo.bv = 42; // 21 * 2 = 42
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 6 Ammo";
        ammo.shortName = "iATM 6";
        ammo.setInternalName("Clan Ammo iATM-6");
        ammo.addLookupName("CLIATM6 Ammo");
        ammo.addLookupName("Clan iATM-6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.shots = 10;
        ammo.bv = 39;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM6ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 6 ER Ammo";
        ammo.shortName = "iATM 6 ER";
        ammo.setInternalName("Clan Ammo iATM-6 ER");
        ammo.addLookupName("CLIATM6 ER Ammo");
        ammo.addLookupName("Clan iATM-6 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 10;
        ammo.bv = 39;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM6HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 6 HE Ammo";
        ammo.shortName = "iATM 6 HE";
        ammo.setInternalName("Clan Ammo iATM-6 HE");
        ammo.addLookupName("CLIATM6 HE Ammo");
        ammo.addLookupName("Clan iATM-6 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 10;
        ammo.bv = 39;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM6IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 6 IIW Ammo";
        ammo.shortName = "iATM 6 IIW";
        ammo.setInternalName("Clan Ammo iATM-6 IIW");
        ammo.addLookupName("CLIATM6 IIW Ammo");
        ammo.addLookupName("Clan iATM-6 IIW Ammo");
        ammo.addLookupName("CLIIW6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IIW;
        ammo.shots = 10;
        ammo.bv = 51; // 50.7 round up (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM6IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 6 IMP Ammo";
        ammo.shortName = "iATM 6 IMP";
        ammo.setInternalName("Clan Ammo iATM-6 IMP");
        ammo.addLookupName("CLIATM6 IMP Ammo");
        ammo.addLookupName("Clan iATM-6 IMP Ammo");
        ammo.addLookupName("CLIMP6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IMP;
        ammo.shots = 10;
        ammo.bv = 78; // 39 * 2 = 78
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 9 Ammo";
        ammo.shortName = "iATM 9";
        ammo.setInternalName("Clan Ammo iATM-9");
        ammo.addLookupName("CLIATM9 Ammo");
        ammo.addLookupName("Clan iATM-9 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.shots = 7;
        ammo.bv = 54;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM9ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 9 ER Ammo";
        ammo.shortName = "iATM 9 ER";
        ammo.setInternalName("Clan Ammo iATM-9 ER");
        ammo.addLookupName("CLIATM9 ER Ammo");
        ammo.addLookupName("Clan iATM-9 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 7;
        ammo.bv = 54;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM9HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 9 HE Ammo";
        ammo.shortName = "iATM 9 HE";
        ammo.setInternalName("Clan Ammo iATM-9 HE");
        ammo.addLookupName("CLIATM9 HE Ammo");
        ammo.addLookupName("Clan iATM-9 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 7;
        ammo.bv = 54;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM9IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 9 IIW Ammo";
        ammo.shortName = "iATM 9 IIW";
        ammo.setInternalName("Clan Ammo iATM-9 IIW");
        ammo.addLookupName("CLIATM9 IIW Ammo");
        ammo.addLookupName("Clan iATM-9 IIW Ammo");
        ammo.addLookupName("CLIIW9 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IIW;
        ammo.shots = 7;
        ammo.bv = 70; // 54 * 1.3 = 70.2, round down (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM9IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 9 IMP Ammo";
        ammo.shortName = "iATM 9 IMP";
        ammo.setInternalName("Clan Ammo iATM-9 IMP");
        ammo.addLookupName("CLIATM9 IMP Ammo");
        ammo.addLookupName("Clan iATM-9 IMP Ammo");
        ammo.addLookupName("CLIMP9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IMP;
        ammo.shots = 7;
        ammo.bv = 108; // 54 * 2 = 108
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 12 Ammo";
        ammo.shortName = "iATM 12";
        ammo.setInternalName("Clan Ammo iATM-12");
        ammo.addLookupName("CLIATM12 Ammo");
        ammo.addLookupName("Clan iATM-12 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.shots = 5;
        ammo.bv = 78;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM12ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 12 ER Ammo";
        ammo.shortName = "iATM 12 ER";
        ammo.setInternalName("Clan Ammo iATM-12 ER");
        ammo.addLookupName("CLIATM12 ER Ammo");
        ammo.addLookupName("Clan iATM-12 ER Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 5;
        ammo.bv = 78;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM12HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 12 HE Ammo";
        ammo.shortName = "iATM 12 HE";
        ammo.setInternalName("Clan Ammo iATM-12 HE");
        ammo.addLookupName("CLIATM12 HE Ammo");
        ammo.addLookupName("Clan iATM-12 HE Ammo");
        ammo.damagePerShot = 3;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 5;
        ammo.bv = 78;
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM12IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 12 IIW Ammo";
        ammo.shortName = "iATM 12 IIW";
        ammo.setInternalName("Clan Ammo iATM-12 IIW");
        ammo.addLookupName("CLIATM12 IIW Ammo");
        ammo.addLookupName("Clan iATM-12 IIW Ammo");
        ammo.addLookupName("CLIIW12 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IIW;
        ammo.shots = 5;
        ammo.bv = 101; // 78 * 1.3 = 101.4, round down (?)
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLIATM12IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "iATM 12 IMP Ammo";
        ammo.shortName = "iATM 12 IMP";
        ammo.setInternalName("Clan Ammo iATM-12 IMP");
        ammo.addLookupName("CLIATM12 IMP Ammo");
        ammo.addLookupName("Clan iATM-12 IMP Ammo");
        ammo.addLookupName("CLIMP12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_IATM;
        ammo.munitionType = M_IATM_IMP;
        ammo.shots = 5;
        ammo.bv = 156; // 78 * 2 = 156
        ammo.cost = 75000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techRating = RATING_F;

        return ammo;
    }
    
    private static AmmoType createCLStreakLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 1 Ammo";
        ammo.shortName = "Streak LRM 1";
        ammo.setInternalName("Clan Streak LRM 1 Ammo");
        ammo.addLookupName("CLStreakLRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 2 Ammo";
        ammo.shortName = "Streak LRM 2";
        ammo.setInternalName("Clan Streak LRM 2 Ammo");
        ammo.addLookupName("CLStreakLRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 3 Ammo";
        ammo.shortName = "Streak LRM 3";
        ammo.setInternalName("Clan Streak LRM 3 Ammo");
        ammo.addLookupName("CLStreakLRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 4 Ammo";
        ammo.shortName = "Streak LRM 4";
        ammo.setInternalName("Clan Streak LRM 4 Ammo");
        ammo.addLookupName("CLStreakLRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }


    private static AmmoType createCLStreakLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 5 Ammo";
        ammo.shortName = "Streak LRM 5";
        ammo.setInternalName("Clan Streak LRM 5 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-5");
        ammo.addLookupName("CLStreakLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.shots = 24;
        ammo.bv = 11;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
    private static AmmoType createCLStreakLRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 6 Ammo";
        ammo.shortName = "Streak LRM 6";
        ammo.setInternalName("Clan Streak LRM 6 Ammo");
        ammo.addLookupName("CLStreakLRM6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM7Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 7 Ammo";
        ammo.shortName = "Streak LRM 7";
        ammo.setInternalName("Clan Streak LRM 7 Ammo");
        ammo.addLookupName("CLStreakLRM7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 8 Ammo";
        ammo.shortName = "Streak LRM 8";
        ammo.setInternalName("Clan Streak LRM 8 Ammo");
        ammo.addLookupName("CLStreakLRM8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 9 Ammo";
        ammo.shortName = "Streak LRM 9";
        ammo.setInternalName("Clan Streak LRM 9 Ammo");
        ammo.addLookupName("CLStreakLRM9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLStreakLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 10 Ammo";
        ammo.shortName = "Streak LRM 10";
        ammo.setInternalName("Clan Streak LRM 10 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-10");
        ammo.addLookupName("CLStreakLRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.shots = 12;
        ammo.bv = 22;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
    private static AmmoType createCLStreakLRM11Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 11 Ammo";
        ammo.shortName = "Streak LRM 11";
        ammo.setInternalName("Clan Streak LRM 11 Ammo");
        ammo.addLookupName("CLStreakLRM11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 12 Ammo";
        ammo.shortName = "Streak LRM 12";
        ammo.setInternalName("Clan Streak LRM 12 Ammo");
        ammo.addLookupName("CLStreakLRM12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM13Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 13 Ammo";
        ammo.shortName = "Streak LRM 13";
        ammo.setInternalName("Clan Streak LRM 13 Ammo");
        ammo.addLookupName("CLStreakLRM13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM14Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 14 Ammo";
        ammo.shortName = "Streak LRM 14";
        ammo.setInternalName("Clan Streak LRM 14 Ammo");
        ammo.addLookupName("CLStreakLRM14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLStreakLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 15 Ammo";
        ammo.shortName = "Streak LRM 15";
        ammo.setInternalName("Clan Streak LRM 15 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-15");
        ammo.addLookupName("CLStreakLRM15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.shots = 8;
        ammo.bv = 32;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
    private static AmmoType createCLStreakLRM16Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 16 Ammo";
        ammo.shortName = "Streak LRM 16";
        ammo.setInternalName("Clan Streak LRM 16 Ammo");
        ammo.addLookupName("CLStreakLRM16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM17Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 17 Ammo";
        ammo.shortName = "Streak LRM 17";
        ammo.setInternalName("Clan Streak LRM 17 Ammo");
        ammo.addLookupName("CLStreakLRM17 mmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM18Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 18 Ammo";
        ammo.shortName = "Streak LRM 18";
        ammo.setInternalName("Clan Streak LRM 18 Ammo");
        ammo.addLookupName("CLStreakLRM18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }
    
        private static AmmoType createCLStreakLRM19Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 19 Ammo";
        ammo.shortName = "Streak LRM 19";
        ammo.setInternalName("Clan Streak LRM 19 Ammo");
        ammo.addLookupName("CLStreakLRM19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);        
        ammo.shots = 1;
        ammo.kgPerShot = 20;
        ammo.bv = 0;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLStreakLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3057, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.name = "Streak LRM 20 Ammo";
        ammo.shortName = "Streak LRM 20";
        ammo.setInternalName("Clan Streak LRM 20 Ammo");
        // ammo.addLookupName("Clan Ammo Streak-20");
        ammo.addLookupName("CLStreakLRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.shots = 6;
        ammo.bv = 43;
        ammo.cost = 60000;
        ammo.introDate = 3057;
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    // Start BattleArmor ammo
    private static AmmoType createBASRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "BA SRM 2 Ammo";
        ammo.shortName = "SRM 2";
        ammo.setInternalName("BA-SRM2 Ammo");
        ammo.addLookupName("BASRM-2 Ammo");
        ammo.addLookupName("BASRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.kgPerShot = 20;
        ammo.introDate = 2868;
        ammo.techLevel.put(2868, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_D};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBAMicroBombAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Micro Bomb Ammo";
        ammo.shortName = "Micro Bomb";
        ammo.setInternalName("BA-Micro Bomb Ammo");
        ammo.addLookupName("BAMicroBomb Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_BA_MICRO_BOMB;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 500;
        ammo.techRating = RATING_F;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};

        return ammo;
    }

    private static AmmoType createCLTorpedoLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Torpedo/LRM 5 Ammo";
        ammo.shortName = "Torpedo/LRM 5";
        ammo.setInternalName("Clan Torpedo/LRM5 Ammo");
        ammo.addLookupName("CLTorpedoLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO_COMBO;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 7;
        ammo.cost = 30000;
        ammo.techRating = RATING_C;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_C};

        return ammo;
    }

    private static AmmoType createBACompactNarcAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "Compact Narc Ammo";
        ammo.shortName = "Compact Narc";
        ammo.setInternalName(BattleArmor.DISPOSABLE_NARC_AMMO);
        ammo.addLookupName("BACompactNarc Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR).or(F_ENCUMBERING);
        ammo.shots = 1;
        ammo.explosive = false;
        ammo.bv = 0;
        ammo.kgPerShot = 10;
        ammo.techRating = RATING_E;
        ammo.introDate = 2825;
        ammo.techLevel.put(2825, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_D, RATING_C};

        return ammo;
    }

    private static AmmoType createBAMineLauncherAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Pop-up Mine Ammo";
        ammo.shortName = "Mine";
        ammo.setInternalName("BA-Mine Launcher Ammo");
        ammo.addLookupName("BAMineLauncher Ammo");
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MINE;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 15000;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    // Proto Ammos
    private static AmmoType createCLPROHeavyMGAmmo() {
        // Need special processing to allow non-standard ammo loads.
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Heavy Machine Gun Ammo";
        ammo.shortName = "Heavy Machine Gun";
        ammo.setInternalName("Clan Heavy Machine Gun Ammo - Proto");
        ammo.addLookupName("CLHeavyMG Ammo");
        ammo.addLookupName("Clan Heavy Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMECH);
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.introDate = 3059;
        ammo.techLevel.put(3059, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_D};
        return ammo;
    }

    private static AmmoType createCLPROMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("Clan Machine Gun Ammo - Proto");
        ammo.addLookupName("CLMG Ammo");
        ammo.addLookupName("Clan Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMECH);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_D};
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createCLPROLightMGAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Light Machine Gun Ammo";
        ammo.shortName = "Light Machine Gun";
        ammo.setInternalName("Clan Light Machine Gun Ammo - Proto");
        ammo.addLookupName("CLLightMG Ammo");
        ammo.addLookupName("Clan Light Machine Gun Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMECH);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_D};
        ammo.techRating = RATING_A;
        return ammo;
    }

    private static AmmoType createCLPROAC2Ammo() {
        // Need special processing to allow non-standard ammo loads.
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "ProtoMech AC/2 Ammo";
        ammo.shortName = "Proto AC/2";
        ammo.setInternalName("Clan ProtoMech AC/2 Ammo");
        ammo.addLookupName("CLProtoAC2Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_PAC;
        ammo.shots = 40;
        ammo.bv = 4;
        ammo.cost = 1200;
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3070));
        ammo.techLevel.put(3073, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLPROAC4Ammo() {
        // Need special processing to allow non-standard ammo loads.
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "ProtoMech AC/4 Ammo";
        ammo.shortName = "Proto AC/4";
        ammo.setInternalName("Clan ProtoMech AC/4 Ammo");
        ammo.addLookupName("CLProtoAC4Ammo");
        ammo.damagePerShot = 4;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_PAC;
        ammo.shots = 20;
        ammo.bv = 6;
        ammo.cost = 4800;
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3070));
        ammo.techLevel.put(3073, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLPROAC8Ammo() {
        // Need special processing to allow non-standard ammo loads.
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3070, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "ProtoMech AC/8 Ammo";
        ammo.shortName = "Proto AC/8";
        ammo.setInternalName("Clan ProtoMech AC/8 Ammo");
        ammo.addLookupName("CLProtoAC8Ammo");
        ammo.damagePerShot = 8;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_PAC;
        ammo.shots = 10;
        ammo.bv = 8;
        ammo.cost = 6300;
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3070));
        ammo.techLevel.put(3073, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Arrow IV Ammo";
        ammo.shortName = "Arrow IV";
        ammo.setInternalName("ISArrowIVAmmo");
        ammo.addLookupName("ISArrowIV Ammo");
        ammo.addLookupName("IS Ammo Arrow");
        ammo.addLookupName("IS Arrow IV Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_ARROW_IV;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 10000;
        ammo.introDate = 2600;
        ammo.techLevel.put(2600, ammo.techLevel.get(3067));
        ammo.extinctDate = 2830;
        ammo.reintroDate = 3045;
        ammo.availRating = new int[]{RATING_E, RATING_F, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Arrow IV Ammo";
        ammo.shortName = "Arrow IV";
        ammo.setInternalName("CLArrowIVAmmo");
        ammo.addLookupName("CLArrowIV Ammo");
        ammo.addLookupName("Clan Ammo Arrow");
        ammo.addLookupName("Clan Arrow IV Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_ARROW_IV;
        ammo.shots = 5;
        ammo.bv = 30;
        ammo.cost = 10000;
        ammo.introDate = 2807;
        ammo.techLevel.put(2807, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_E, RATING_D};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLLongTomAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Long Tom Ammo";
        ammo.shortName = "Long Tom";
        ammo.setInternalName("CLLongTomAmmo");
        ammo.addLookupName("CLLongTom Ammo");
        ammo.addLookupName("CLLongTomArtillery Ammo");
        ammo.addLookupName("Clan Ammo Long Tom");
        ammo.addLookupName("Clan Long Tom Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoType.T_LONG_TOM;
        ammo.shots = 5;
        ammo.bv = 46;
        ammo.cost = 10000;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createCLSniperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Sniper Ammo";
        ammo.shortName = "Sniper";
        ammo.setInternalName("CLSniperAmmo");
        ammo.addLookupName("CLSniper Ammo");
        ammo.addLookupName("CLSniperArtillery Ammo");
        ammo.addLookupName("Clan Ammo Sniper");
        ammo.addLookupName("Clan Sniper Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_SNIPER;
        ammo.shots = 10;
        ammo.bv = 11;
        ammo.cost = 6000;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createCLThumperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Thumper Ammo";
        ammo.shortName = "Thumper";
        ammo.setInternalName("CLThumperAmmo");
        ammo.addLookupName("CLThumper Ammo");
        ammo.addLookupName("CLThumperArtillery Ammo");
        ammo.addLookupName("Clan Ammo Thumper");
        ammo.addLookupName("Clan Thumper Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_THUMPER;
        ammo.shots = 20;
        ammo.bv = 5;
        ammo.cost = 4500;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_C, RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createBAISLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "BA LRM 1 Ammo";
        ammo.shortName = "LRM 1";
        ammo.setInternalName("IS BA Ammo LRM-1");
        ammo.addLookupName("BAISLRM1 Ammo");
        ammo.addLookupName("BAISLRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.kgPerShot = 8.3;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBAISLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "BA LRM 2 Ammo";
        ammo.shortName = "LRM 2";
        ammo.setInternalName("IS BA Ammo LRM-2");
        ammo.addLookupName("BAISLRM2 Ammo");
        ammo.addLookupName("BAISLRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.kgPerShot = 16.6;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBAISLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "BA LRM 3 Ammo";
        ammo.shortName = "LRM 3";
        ammo.setInternalName("IS BA Ammo LRM-3");
        ammo.addLookupName("BAISLRM3 Ammo");
        ammo.addLookupName("BAISLRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.kgPerShot = 25;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBAISLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "BA LRM 4 Ammo";
        ammo.shortName = "LRM 4";
        ammo.setInternalName("IS BA Ammo LRM-4");
        ammo.addLookupName("BAISLRM4 Ammo");
        ammo.addLookupName("BAISLRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 5;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.kgPerShot = 33.3;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBAISLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "BA LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("IS BA Ammo LRM-5");
        ammo.addLookupName("BAISLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.kgPerShot = 41.5;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBACLLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "BA LRM 1 Ammo";
        ammo.shortName = "LRM 1";
        ammo.setInternalName("BACL Ammo LRM-1");
        ammo.addLookupName("BACLLRM1 Ammo");
        ammo.addLookupName("BACL LRM 1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 8.3;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createBACLLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "BA LRM 2 Ammo";
        ammo.shortName = "LRM 2";
        ammo.setInternalName("BACL Ammo LRM-2");
        ammo.addLookupName("BACLLRM2 Ammo");
        ammo.addLookupName("BACL LRM 2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.kgPerShot = 16.6;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createBACLLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "BA LRM 3 Ammo";
        ammo.shortName = "LRM 3";
        ammo.setInternalName("BACL Ammo LRM-3");
        ammo.addLookupName("BACLLRM3 Ammo");
        ammo.addLookupName("BACL LRM 3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 5;
        ammo.kgPerShot = 25;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createBACLLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "BA LRM 4 Ammo";
        ammo.shortName = "LRM 4";
        ammo.setInternalName("BACL Ammo LRM-4");
        ammo.addLookupName("BACLLRM4 Ammo");
        ammo.addLookupName("BACL LRM 4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.kgPerShot = 33.3;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createBACLLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "BA LRM 5 Ammo";
        ammo.shortName = "LRM 5";
        ammo.setInternalName("BACL Ammo LRM-5");
        ammo.addLookupName("BACLLRM5 Ammo");
        ammo.addLookupName("BACL LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 7;
        ammo.kgPerShot = 41.5;
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createBASRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "BA SRM 6 Ammo";
        ammo.shortName = "SRM 6";
        ammo.setInternalName("BA-SRM6 Ammo");
        ammo.addLookupName("BASRM-6 Ammo");
        ammo.addLookupName("BASRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 7;
        ammo.kgPerShot = 60;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createBASRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "BA SRM 5 Ammo";
        ammo.shortName = "SRM 5";
        ammo.setInternalName("BA-SRM5 Ammo");
        ammo.addLookupName("BASRM-5 Ammo");
        ammo.addLookupName("BASRM5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.kgPerShot = 50;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createBASRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "BA SRM 4 Ammo";
        ammo.shortName = "SRM 4";
        ammo.setInternalName("BA-SRM4 Ammo");
        ammo.addLookupName("BASRM-4 Ammo");
        ammo.addLookupName("BASRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 5;
        ammo.kgPerShot = 40;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createBASRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "BA SRM 3 Ammo";
        ammo.shortName = "SRM 3";
        ammo.setInternalName("BA-SRM3 Ammo");
        ammo.addLookupName("BASRM-3 Ammo");
        ammo.addLookupName("BASRM3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 30;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createBASRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_ALLOWED_ALL);
        ammo.name = "BA SRM 1 Ammo";
        ammo.shortName = "SRM 1";
        ammo.setInternalName("BA-SRM1 Ammo");
        ammo.addLookupName("BASRM-1 Ammo");
        ammo.addLookupName("BASRM1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createBATubeArtyAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "BA Tube Artillery Ammo";
        ammo.shortName = "Tube Artillery";
        ammo.setInternalName("ISBATubeArtilleryAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_BA_TUBE;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 2;
        ammo.bv = 4;
        ammo.kgPerShot = 15;
        ammo.introDate = 3075;
        ammo.cost = 900;
        ammo.techLevel.put(3075, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createISMRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 1 Ammo";
        ammo.shortName = "MRM 1";
        ammo.setInternalName("IS MRM 1 Ammo");
        ammo.addLookupName("ISMRM1 Ammo");
        ammo.addLookupName("ISBAMRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 1;
        ammo.kgPerShot = 5;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createISMRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 2 Ammo";
        ammo.shortName = "MRM 2";
        ammo.setInternalName("IS MRM 2 Ammo");
        ammo.addLookupName("ISMRM2 Ammo");
        ammo.addLookupName("ISBAMRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createISMRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 3 Ammo";
        ammo.shortName = "MRM 3";
        ammo.setInternalName("IS MRM 3 Ammo");
        ammo.addLookupName("ISMRM3 Ammo");
        ammo.addLookupName("ISBAMRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 15;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createISMRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 4 Ammo";
        ammo.shortName = "MRM 4";
        ammo.setInternalName("IS MRM 4 Ammo");
        ammo.addLookupName("ISMRM4 Ammo");
        ammo.addLookupName("ISBAMRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 3;
        ammo.kgPerShot = 20;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createISMRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "MRM 5 Ammo";
        ammo.shortName = "MRM 5";
        ammo.setInternalName("IS MRM 5 Ammo");
        ammo.addLookupName("ISMRM5 Ammo");
        ammo.addLookupName("ISBAMRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 25;
        ammo.introDate = 3060;
        ammo.techLevel.put(3060, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createBARL1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 1 Ammo";
        ammo.setInternalName("BARL1 Ammo");
        ammo.addLookupName("LAW Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISBATaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        ammo.name = "BA Taser Ammo";
        ammo.shortName = "Taser";
        ammo.setInternalName(ammo.name);
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TASER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.introDate = 3067;
        ammo.techLevel.put(3067, ammo.techLevel.get(3071));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISMekTaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Taser Ammo";
        ammo.shortName = "Taser";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("MekTaserAmmo");
        ammo.damagePerShot = 6;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TASER;
        ammo.shots = 5;
        ammo.bv = 5;
        ammo.cost = 2000;
        ammo.introDate = 3067;
        ammo.techLevel.put(3067, ammo.techLevel.get(3071));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_E;
        ammo.techLevel.put(3084, TechConstants.T_IS_ADVANCED);

        return ammo;
    }

    private static AmmoType createBARL2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 2 Ammo";
        ammo.setInternalName("BARL2 Ammo");
        ammo.addLookupName("LAW 2 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-2 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createBARL3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 3 Ammo";
        ammo.setInternalName("BARL3 Ammo");
        ammo.addLookupName("LAW 3 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-3 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createBARL4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 4 Ammo";
        ammo.setInternalName("BARL4 Ammo");
        ammo.addLookupName("LAW 4 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-4 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createBARL5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "RL 5 Ammo";
        ammo.setInternalName("BARL5 Ammo");
        ammo.addLookupName("LAW 5 Launcher Ammo");
        ammo.addLookupName("IS Ammo LAW-5 Launcher");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.introDate = 3050;
        ammo.techLevel.put(3050, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_B};
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createAdvancedSRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Advanced SRM 1 Ammo";
        ammo.shortName = "Advanced SRM 1";
        ammo.setInternalName("BA-Advanced SRM-1 Ammo");
        ammo.addLookupName("BAAdvanced SRM1 Ammo");
        ammo.addLookupName("BAAdvancedSRM1 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 2;
        ammo.kgPerShot = 10;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createAdvancedSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Advanced SRM 2 Ammo";
        ammo.shortName = "Advanced SRM 2";
        ammo.setInternalName("BA-Advanced SRM-2 Ammo");
        ammo.addLookupName("BA-Advanced SRM-2 Ammo OS");
        ammo.addLookupName("BAAdvancedSRM2 Ammo");
        ammo.addLookupName("BAAdvanced SRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 4;
        ammo.kgPerShot = 20;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createAdvancedSRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Advanced SRM 3 Ammo";
        ammo.shortName = "Advanced SRM 3";
        ammo.setInternalName("BA-Advanced SRM-3 Ammo");
        ammo.addLookupName("BAAdvanced SRM3 Ammo");
        ammo.addLookupName("BAAdvancedSRM3 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 6;
        ammo.kgPerShot = 30;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createAdvancedSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Advanced SRM 4 Ammo";
        ammo.shortName = "Advanced SRM 4";
        ammo.setInternalName("BA-Advanced SRM-4 Ammo");
        ammo.addLookupName("BAAdvanced SRM4 Ammo");
        ammo.addLookupName("BAAdvancedSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 8;
        ammo.kgPerShot = 40;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createAdvancedSRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Advanced SRM 5 Ammo";
        ammo.shortName = "Advanced SRM 5";
        ammo.setInternalName("BA-Advanced SRM-5 Ammo");
        ammo.addLookupName("BAAdvancedSRM5 Ammo");
        ammo.addLookupName("BAAdvanced SRM5 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 10;
        ammo.kgPerShot = 50;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createAdvancedSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Advanced SRM 6 Ammo";
        ammo.shortName = "Advanced SRM 6";
        ammo.setInternalName("BA-Advanced SRM-6 Ammo");
        ammo.addLookupName("BAAdvanced SRM6 Ammo");
        ammo.addLookupName("BAAdvancedSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 12;
        ammo.kgPerShot = 60;
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISLAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LAC/2 Ammo";
        ammo.shortName = "LAC/2";
        ammo.setInternalName("IS Ammo LAC/2");
        ammo.addLookupName("ISLAC2 Ammo");
        ammo.addLookupName("IS Light Autocannon/2 Ammo");
        ammo.addLookupName("Light AC/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LAC;
        ammo.shots = 45;
        ammo.bv = 4;
        ammo.cost = 2000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createISLAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "LAC/5 Ammo";
        ammo.shortName = "LAC/5";
        ammo.setInternalName("IS Ammo LAC/5");
        ammo.addLookupName("ISLAC5 Ammo");
        ammo.addLookupName("IS Light Autocannon/5 Ammo");
        ammo.addLookupName("Light AC/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LAC;
        ammo.shots = 20;
        ammo.bv = 8;
        ammo.cost = 5000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createISLAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LAC/10 Ammo";
        ammo.shortName = "LAC/10";
        ammo.setInternalName("IS Ammo LAC/10");
        ammo.addLookupName("ISLAC10 Ammo");
        ammo.addLookupName("IS Light Autocannon/10 Ammo");
        ammo.addLookupName("Light AC/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LAC;
        ammo.shots = 10;
        ammo.bv = 9;
        ammo.cost = 10000;
        ammo.techRating = RATING_D;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISLAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "LAC/20 Ammo";
        ammo.shortName = "LAC/20";
        ammo.setInternalName("IS Ammo LAC/20");
        ammo.addLookupName("ISLAC20 Ammo");
        ammo.addLookupName("IS Light Autocannon/20 Ammo");
        ammo.addLookupName("Light AC/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LAC;
        ammo.shots = 5;
        ammo.bv = 15;
        ammo.cost = 20000;
        ammo.techRating = RATING_D;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISHVAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "HVAC/2 Ammo";
        ammo.shortName = "HVAC/2";
        ammo.setInternalName("IS Ammo HVAC/2");
        ammo.addLookupName("ISHVAC2 Ammo");
        ammo.addLookupName("IS Hyper Velocity Autocannon/2 Ammo");
        ammo.addLookupName("Hyper Velocity AC/2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_HYPER_VELOCITY;
        ammo.shots = 30;
        ammo.bv = 7;
        ammo.cost = 3000;
        ammo.introDate = 3059;
        ammo.techLevel.put(3059, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISHVAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "HVAC/5 Ammo";
        ammo.shortName = "HVAC/5";
        ammo.setInternalName("IS Ammo HVAC/5");
        ammo.addLookupName("ISHVAC5 Ammo");
        ammo.addLookupName("IS Hyper Velocity Autocannon/5 Ammo");
        ammo.addLookupName(" Hyper Velocity AC/5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_HYPER_VELOCITY;
        ammo.shots = 15;
        ammo.bv = 14;
        ammo.cost = 10000;
        ammo.introDate = 3059;
        ammo.techLevel.put(3059, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISHVAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "HVAC/10 Ammo";
        ammo.shortName = "HVAC/10";
        ammo.setInternalName("IS Ammo HVAC/10");
        ammo.addLookupName("ISHVAC10 Ammo");
        ammo.addLookupName("IS Hyper Velocity Autocannon/10 Ammo");
        ammo.addLookupName("Hyper Velocity AC/10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_HYPER_VELOCITY;
        ammo.shots = 8;
        ammo.bv = 20;
        ammo.cost = 20000;
        ammo.introDate = 3059;
        ammo.techLevel.put(3059, ammo.techLevel.get(3071));
        ammo.techLevel.put(3079, TechConstants.T_IS_ADVANCED);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISHeavyFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Heavy Flamer Ammo";
        ammo.shortName = "Heavy Flamer";
        ammo.setInternalName("IS Heavy Flamer Ammo");
        ammo.addLookupName("IS Ammo Heavy Flamer");
        ammo.addLookupName("ISHeavyFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_HEAVY_FLAMER;
        ammo.shots = 10;
        ammo.bv = 2;
        ammo.cost = 2000;
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{EquipmentType.RATING_A,
                EquipmentType.RATING_A, EquipmentType.RATING_B};
        ammo.techRating = RATING_A;
        ammo.techLevel.put(3079, TechConstants.T_IS_TW_NON_BOX);

        return ammo;
    }

    private static AmmoType createCLHeavyFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Heavy Flamer Ammo";
        ammo.shortName = "Heavy Flamer";
        ammo.setInternalName("CL Heavy Flamer Ammo");
        ammo.addLookupName("CL Ammo Heavy Flamer");
        ammo.addLookupName("Clan Ammo Heavy Flamer");
        ammo.addLookupName("Clan Heavy Flamer Ammo");
        ammo.addLookupName("CLHeavyFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_HEAVY_FLAMER;
        ammo.shots = 10;
        ammo.bv = 2;
        ammo.cost = 2000;
        ammo.introDate = 3067;
        ammo.techLevel.put(3067, ammo.techLevel.get(3067));
        ammo.techLevel.put(3079, TechConstants.T_CLAN_TW);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_A, EquipmentType.RATING_A};
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISCoolantPod() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Coolant Pod";
        ammo.shortName = "Coolant Pod";
        ammo.setInternalName("IS Coolant Pod");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_COOLANT_POD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 50000;
        ammo.introDate = 3049;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.techRating = RATING_D;
        ammo.techLevel.put(3049, TechConstants.T_IS_EXPERIMENTAL);
        ammo.techLevel.put(3079, TechConstants.T_IS_TW_NON_BOX);

        // TODO: modes is a bodge because there is no proper end phase
        String[] theModes = {"safe", "efficient", "off", "dump"};
        ammo.setModes(theModes);
        ammo.setInstantModeSwitch(true);

        return ammo;
    }

    private static AmmoType createCLCoolantPod() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Coolant Pod";
        ammo.shortName = "Coolant Pod";
        ammo.setInternalName("Clan Coolant Pod");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_COOLANT_POD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 50000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.techRating = RATING_D;
        ammo.techLevel.put(3056, TechConstants.T_CLAN_EXPERIMENTAL);
        ammo.techLevel.put(3083, TechConstants.T_CLAN_TW);

        // TODO: modes is a bodge because there is no proper end phase
        String[] theModes = {"safe", "efficient", "off", "dump"};
        ammo.setModes(theModes);
        ammo.setInstantModeSwitch(true);

        return ammo;
    }

    private static AmmoType createISExtendedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Extended LRM 5 Ammo";
        ammo.shortName = "ELRM 5";
        ammo.setInternalName("IS Ammo Extended LRM-5");
        ammo.addLookupName("ISExtended LRM5 Ammo");
        ammo.addLookupName("ISExtendedLRM5 Ammo");
        ammo.addLookupName("IS Extended LRM 5 Ammo");
        ammo.addLookupName("ELRM-5 Ammo (THB)");
        ammo.addLookupName("ELRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_EXLRM;
        ammo.shots = 18;
        ammo.bv = 8;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3071));
        ammo.techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createISExtendedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Extended LRM 10 Ammo";
        ammo.shortName = "ELRM 10";
        ammo.setInternalName("IS Ammo Extended LRM-10");
        ammo.addLookupName("ISExtended LRM10 Ammo");
        ammo.addLookupName("ISExtendedLRM10 Ammo");
        ammo.addLookupName("IS Extended LRM 10 Ammo");
        ammo.addLookupName("ELRM-10 Ammo (THB)");
        ammo.addLookupName("ELRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_EXLRM;
        ammo.shots = 9;
        ammo.bv = 17;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3071));
        ammo.techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createISExtendedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Extended LRM 15 Ammo";
        ammo.shortName = "ELRM 15";
        ammo.setInternalName("IS Ammo Extended LRM-15");
        ammo.addLookupName("ISExtended LRM15 Ammo");
        ammo.addLookupName("ISExtendedLRM15 Ammo");
        ammo.addLookupName("IS Extended LRM 15 Ammo");
        ammo.addLookupName("ELRM-15 Ammo (THB)");
        ammo.addLookupName("ELRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_EXLRM;
        ammo.shots = 6;
        ammo.bv = 25;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3071));
        ammo.techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISExtendedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Extended LRM 20 Ammo";
        ammo.shortName = "ELRM 20";
        ammo.setInternalName("IS Ammo Extended LRM-20");
        ammo.addLookupName("ISExtended LRM20 Ammo");
        ammo.addLookupName("ISExtendedLRM20 Ammo");
        ammo.addLookupName("IS Extended LRM 20 Ammo");
        ammo.addLookupName("ELRM-20 Ammo (THB)");
        ammo.addLookupName("ELRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_EXLRM;
        ammo.shots = 4;
        ammo.bv = 34;
        ammo.cost = 90000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3071));
        ammo.techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISThunderbolt5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        ammo.name = "Thunderbolt 5 Ammo";
        ammo.shortName = "Thunderbolt 5";
        ammo.setInternalName("IS Ammo Thunderbolt-5");
        ammo.addLookupName("ISThunderbolt5 Ammo");
        ammo.addLookupName("IS Thunderbolt 5 Ammo");
        ammo.addLookupName("ISTBolt5 Ammo");
        ammo.damagePerShot = 5;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TBOLT_5;
        ammo.shots = 12;
        ammo.bv = 8;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3071));
        ammo.techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISThunderbolt10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        ammo.name = "Thunderbolt 10 Ammo";
        ammo.shortName = "Thunderbolt 10";
        ammo.setInternalName("IS Ammo Thunderbolt-10");
        ammo.addLookupName("ISThunderbolt10 Ammo");
        ammo.addLookupName("IS Thunderbolt 10 Ammo");
        ammo.addLookupName("ISTBolt10 Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TBOLT_10;
        ammo.shots = 6;
        ammo.bv = 16;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3071));
        ammo.techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISThunderbolt15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        ammo.name = "Thunderbolt 15 Ammo";
        ammo.shortName = "Thunderbolt 15";
        ammo.setInternalName("IS Ammo Thunderbolt-15");
        ammo.addLookupName("ISThunderbolt15 Ammo");
        ammo.addLookupName("IS Thunderbolt 15 Ammo");
        ammo.addLookupName("ISTBolt15 Ammo");
        ammo.damagePerShot = 15;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TBOLT_15;
        ammo.shots = 4;
        ammo.bv = 29;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3071));
        ammo.techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISThunderbolt20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_ADVANCED);
        ammo.name = "Thunderbolt 20 Ammo";
        ammo.shortName = "Thunderbolt 20";
        ammo.setInternalName("IS Ammo Thunderbolt-20");
        ammo.addLookupName("ISThunderbolt20 Ammo");
        ammo.addLookupName("IS Thunderbolt 20 Ammo");
        ammo.addLookupName("ISTBolt20 Ammo");
        ammo.damagePerShot = 20;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TBOLT_20;
        ammo.shots = 3;
        ammo.bv = 38;
        ammo.cost = 50000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3071));
        ammo.techLevel.put(3081, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISRailGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Rail Gun Ammo";
        ammo.shortName = "Rail Gun";
        ammo.setInternalName("ISRailGun Ammo");
        ammo.addLookupName("IS Rail Gun Ammo");
        ammo.damagePerShot = 22;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_RAIL_GUN;
        ammo.shots = 5;
        ammo.bv = 51;
        ammo.cost = 20000;
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_A, RATING_A, RATING_A};
        ammo.techRating = RATING_A;

        return ammo;
    }

    private static AmmoType createISMagshotGRAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3072, TechConstants.T_IS_ADVANCED);
        ammo.name = "Magshot GR Ammo";
        ammo.shortName = "Magshot";
        ammo.setInternalName("ISMagshotGR Ammo");
        ammo.addLookupName("IS Magshot GR Ammo");
        ammo.damagePerShot = 2;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_MAGSHOT;
        ammo.shots = 50;
        ammo.bv = 2;
        ammo.cost = 1000;
        ammo.introDate = 3072;
        ammo.techLevel.put(3078, TechConstants.T_IS_TW_NON_BOX);
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createCLAPGaussRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "AP Gauss Rifle Ammo";
        ammo.shortName = "AP Gauss";
        ammo.setInternalName("CLAPGaussRifle Ammo");
        ammo.addLookupName("Clan AP Gauss Rifle Ammo");
        ammo.addLookupName("Clan Anti-Personnel Gauss Rifle Ammo");
        ammo.damagePerShot = 3;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_APGAUSS;
        ammo.shots = 40;
        ammo.bv = 3;
        ammo.cost = 1000;
        ammo.kgPerShot = 25;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3069;
        ammo.techLevel.put(3069, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;
        return ammo;
    }

    private static AmmoType createISPXLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Phoenix LRM 5 Ammo";
        ammo.shortName = "Phoenix LRM 5";
        ammo.setInternalName("ISPhoenixLRM5 Ammo");
        ammo.addLookupName("ISPhoenix LRM 5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_PXLRM;
        ammo.shots = 12;
        ammo.bv = 7;
        ammo.cost = 60000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISPXLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Phoenix LRM 10 Ammo";
        ammo.shortName = "Phoenix LRM 10";
        ammo.setInternalName("ISPhoenixLRM10 Ammo");
        ammo.addLookupName("ISPhoenix LRM 10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_PXLRM;
        ammo.shots = 6;
        ammo.bv = 14;
        ammo.cost = 60000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_D};
        ammo.introDate = 3054;
        ammo.techLevel.put(3054, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISPXLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Phoenix LRM 15 Ammo";
        ammo.shortName = "Phoenix LRM 15";
        ammo.setInternalName("ISPhoenixLRM15 Ammo");
        ammo.addLookupName("ISPhoenix LRM 15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_PXLRM;
        ammo.shots = 4;
        ammo.bv = 21;
        ammo.cost = 60000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2400;
        ammo.techLevel.put(2400, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISPXLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Phoenix LRM 20 Ammo";
        ammo.shortName = "Phoenix LRM 20";
        ammo.setInternalName("ISPhoenixLRM20 Ammo");
        ammo.addLookupName("ISPhoenix LRM 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_PXLRM;
        ammo.shots = 3;
        ammo.bv = 28;
        ammo.cost = 60000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_C, EquipmentType.RATING_C};
        ammo.introDate = 2400;
        ammo.techLevel.put(2400, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISHawkSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Hawk SRM 2 Ammo";
        ammo.shortName = "Hawk SRM 2";
        ammo.setInternalName("ISHawkSRM2 Ammo");
        ammo.addLookupName("IS Hawk SRM 2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_HSRM;
        ammo.shots = 25;
        ammo.bv = 4;
        ammo.cost = 52000;

        return ammo;
    }

    private static AmmoType createISHawkSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Hawk SRM 4 Ammo";
        ammo.shortName = "Hawk SRM 4";
        ammo.setInternalName("ISHawkSRM4 Ammo");
        ammo.addLookupName("IS Hawk SRM 4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_HSRM;
        ammo.shots = 13;
        ammo.bv = 6;
        ammo.cost = 52000;

        return ammo;
    }

    private static AmmoType createISHawkSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Hawk SRM 6 Ammo";
        ammo.shortName = "Hawk SRM 6";
        ammo.setInternalName("ISHawkSRM6 Ammo");
        ammo.addLookupName("IS Hawk SRM 6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_HSRM;
        ammo.shots = 8;
        ammo.bv = 10;
        ammo.cost = 52000;

        return ammo;
    }

    private static AmmoType createISStreakMRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Streak MRM 10 Ammo";
        ammo.shortName = "Streak MRM 10";
        ammo.setInternalName("IS Streak MRM 10 Ammo");
        ammo.addLookupName("ISStreakMRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_MRM_STREAK;
        ammo.shots = 24;
        ammo.bv = 11;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISStreakMRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Streak MRM 20 Ammo";
        ammo.shortName = "Streak MRM 20";
        ammo.setInternalName("IS Streak MRM 20 Ammo");
        ammo.addLookupName("ISStreakMRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_MRM_STREAK;
        ammo.shots = 12;
        ammo.bv = 22;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISStreakMRM30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Streak MRM 30 Ammo";
        ammo.shortName = "Streak MRM 30";
        ammo.setInternalName("IS Streak MRM 30 Ammo");
        ammo.addLookupName("ISStreakMRM30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_MRM_STREAK;
        ammo.shots = 8;
        ammo.bv = 33;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISStreakMRM40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Streak MRM 40 Ammo";
        ammo.shortName = "Streak MRM 40";
        ammo.setInternalName("IS Streak MRM 40 Ammo");
        ammo.addLookupName("ISStreakMRM40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_MRM_STREAK;
        ammo.shots = 6;
        ammo.bv = 44;
        ammo.cost = 10000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3067));

        return ammo;
    }

    private static AmmoType createISMPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "MPod Ammo";
        ammo.setInternalName("IS M-Pod Ammo");
        ammo.addLookupName("IS MPod Ammo");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_MPOD;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.introDate = 3064;
        ammo.techLevel.put(3064, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLMPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "MPod Ammo";
        ammo.setInternalName("Clan M-Pod Ammo");
        ammo.addLookupName("Clan MPod Ammo");
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_MPOD;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3064;
        ammo.techLevel.put(3064, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createISSBGaussRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Silver Bullet Gauss Ammo";
        ammo.shortName = "Silver Bullet";
        ammo.setInternalName("Silver Bullet Gauss Ammo");
        ammo.addLookupName("IS SBGauss Rifle Ammo");
        ammo.addLookupName("ISSBGauss Ammo");
        ammo.addLookupName("ISSBGaussRifleAmmo");
        ammo.explosive = false;
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_SBGAUSS;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 8;
        ammo.bv = 25;
        ammo.cost = 25000;
        ammo.toHitModifier = -1;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3051;
        ammo.techLevel.put(3051, ammo.techLevel.get(3071));
        ammo.techLevel.put(3080, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLHAG20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "HAG/20 Ammo";
        ammo.shortName = "HAG 20";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG20 Ammo");
        ammo.addLookupName("Clan HAG 20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_HAG;
        ammo.shots = 6;
        ammo.bv = 33;
        ammo.cost = 30000;
        ammo.kgPerShot = 166.66;
        ammo.explosive = false;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLHAG30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "HAG/30 Ammo";
        ammo.shortName = "HAG 30";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG30 Ammo");
        ammo.addLookupName("Clan HAG 30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_HAG;
        ammo.shots = 4;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLHAG40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "HAG/40 Ammo";
        ammo.shortName = "HAG 40";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG40 Ammo");
        ammo.addLookupName("Clan HAG 40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_HAG;
        ammo.shots = 3;
        ammo.bv = 67;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3069;
        ammo.techLevel.put(3069, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createCLPlasmaCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Plasma Cannon Ammo";
        ammo.shortName = "Plasma Cannon";
        ammo.setInternalName("CLPlasmaCannonAmmo");
        ammo.addLookupName("CLPlasmaCannon Ammo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_PLASMA;
        ammo.shots = 10;
        ammo.bv = 21;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3069;
        ammo.techLevel.put(3069, ammo.techLevel.get(3067));
        ammo.techRating = RATING_F;

        return ammo;
    }

    private static AmmoType createISPlasmaRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Plasma Rifle Ammo";
        ammo.shortName = "Plasma Rifle";
        ammo.setInternalName("ISPlasmaRifleAmmo");
        ammo.addLookupName("ISPlasmaRifle Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_PLASMA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    // naval ammo
    /*
     * Because ammo by ton is not in whole numberI am doing this as single shot
     * with a functionto change the number of shots which will be calledfrom the
     * BLK file. This means I also have to convertBV and cost per ton to BV and
     * cost per shot
     */

    private static AmmoType createNAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "NAC/10 Ammo";
        ammo.setInternalName("Ammo NAC/10");
        ammo.addLookupName("NAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.bv = 237;
        ammo.cost = 30000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2195;
        ammo.techLevel.put(2195, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createNAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "NAC/20 Ammo";
        ammo.setInternalName("Ammo NAC/20");
        ammo.addLookupName("NAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.bv = 474;
        ammo.cost = 60000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2197;
        ammo.techLevel.put(2197, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createNAC25Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "NAC/25 Ammo";
        ammo.setInternalName("Ammo NAC/25");
        ammo.addLookupName("NAC25 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.bv = 593;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.6;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2200;
        ammo.techLevel.put(2200, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createNAC30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "NAC/30 Ammo";
        ammo.setInternalName("Ammo NAC/30");
        ammo.addLookupName("NAC30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.bv = 711;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.8;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2200;
        ammo.techLevel.put(2200, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createNAC35Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "NAC/35 Ammo";
        ammo.setInternalName("Ammo NAC/35");
        ammo.addLookupName("NAC35 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 35;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.bv = 620;
        ammo.cost = 105000;
        ammo.ammoRatio = 1.0;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2201;
        ammo.techLevel.put(2201, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createNAC40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "NAC/40 Ammo";
        ammo.setInternalName("Ammo NAC/40");
        ammo.addLookupName("NAC40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.bv = 708;
        ammo.cost = 120000;
        ammo.ammoRatio = 1.2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2202;
        ammo.techLevel.put(2202, ammo.techLevel.get(3067));
        ammo.techRating = RATING_D;

        return ammo;
    }

    private static AmmoType createLightNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Light N-Gauss Ammo";
        ammo.setInternalName("Ammo Light N-Gauss");
        ammo.addLookupName("LightNGauss Ammo");
        ammo.damagePerShot = 15;
        ammo.ammoType = AmmoType.T_LIGHT_NGAUSS;
        ammo.shots = 1;
        ammo.bv = 378;
        ammo.cost = 45000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2448;
        ammo.techLevel.put(2448, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLLightNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Light N-Gauss Ammo";
        ammo.setInternalName("Ammo Clan Light N-Gauss");
        ammo.addLookupName("CLLightNGauss Ammo");
        ammo.damagePerShot = 15;
        ammo.ammoType = AmmoType.T_LIGHT_NGAUSS;
        ammo.shots = 1;
        ammo.bv = 378;
        ammo.cost = 45000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createMediumNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Medium N-Gauss Ammo";
        ammo.setInternalName("Ammo Medium N-Gauss");
        ammo.addLookupName("MediumNGauss Ammo");
        ammo.damagePerShot = 25;
        ammo.ammoType = AmmoType.T_MED_NGAUSS;
        ammo.shots = 1;
        ammo.bv = 630;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2448;
        ammo.techLevel.put(2448, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLMediumNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "CLan Medium N-Gauss Ammo";
        ammo.setInternalName("Ammo Clan Medium N-Gauss");
        ammo.addLookupName("CLMediumNGauss Ammo");
        ammo.damagePerShot = 25;
        ammo.ammoType = AmmoType.T_MED_NGAUSS;
        ammo.shots = 1;
        ammo.bv = 630;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createHeavyNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Heavy N-Gauss Ammo";
        ammo.setInternalName("Ammo Heavy N-Gauss");
        ammo.addLookupName("HeavyNGauss Ammo");
        ammo.damagePerShot = 40;
        ammo.ammoType = AmmoType.T_HEAVY_NGAUSS;
        ammo.shots = 1;
        ammo.bv = 756;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_E,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2448;
        ammo.techLevel.put(2448, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLHeavyNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Heavy N-Gauss Ammo";
        ammo.setInternalName("Ammo Clan Heavy N-Gauss");
        ammo.addLookupName("CLHeavyNGauss Ammo");
        ammo.damagePerShot = 40;
        ammo.ammoType = AmmoType.T_HEAVY_NGAUSS;
        ammo.shots = 1;
        ammo.bv = 756;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_E, EquipmentType.RATING_D};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }
    
     private static AmmoType createLightMassDriverAmmo() {
            AmmoType ammo = new AmmoType();

            ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
            ammo.name = "Light Mass Driver Ammo";
            ammo.setInternalName("Ammo Light Mass Driver");
            ammo.addLookupName("LightMassDriver Ammo");
            ammo.damagePerShot = 60;
            ammo.ammoType = AmmoType.T_LMASS;
            ammo.shots = 1;
            ammo.bv = 0;
            ammo.cost = 150000;
            ammo.ammoRatio = 30;
            ammo.capital = true;
            ammo.availRating = new int[]{EquipmentType.RATING_E,
                    EquipmentType.RATING_X, EquipmentType.RATING_E};
            ammo.introDate = 2715;
            ammo.techLevel.put(2715, ammo.techLevel.get(3067));
            ammo.techRating = RATING_D;

            return ammo;
        }

        private static AmmoType createMediumMassDriverAmmo() {
            AmmoType ammo = new AmmoType();

            ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
            ammo.name = "Medium Mass Driver Ammo";
            ammo.setInternalName("Ammo Medium Mass Driver");
            ammo.addLookupName("MediumMassDriver Ammo");
            ammo.damagePerShot = 100;
            ammo.ammoType = AmmoType.T_MMASS;
            ammo.shots = 1;
            ammo.bv = 0;
            ammo.cost = 300000;
            ammo.ammoRatio = 30;
            ammo.capital = true;
            ammo.availRating = new int[]{EquipmentType.RATING_E,
                    EquipmentType.RATING_X, EquipmentType.RATING_E};
            ammo.introDate = 2715;
            ammo.techLevel.put(2715, ammo.techLevel.get(3067));
            ammo.techRating = RATING_D;

            return ammo;
        }

        private static AmmoType createHeavyMassDriverAmmo() {
            AmmoType ammo = new AmmoType();

            ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
            ammo.name = "Heavy Mass Driver Ammo";
            ammo.setInternalName("Ammo Heavy Mass Driver");
            ammo.addLookupName("HeavyMassDriver Ammo");
            ammo.damagePerShot = 140;
            ammo.ammoType = AmmoType.T_HMASS;
            ammo.shots = 1;
            ammo.bv = 0;
            ammo.cost = 600000;
            ammo.ammoRatio = 30;
            ammo.capital = true;
            ammo.availRating = new int[]{EquipmentType.RATING_E,
                    EquipmentType.RATING_X, EquipmentType.RATING_E};
            ammo.introDate = 2715;
            ammo.techLevel.put(2715, ammo.techLevel.get(3067));
            ammo.techRating = RATING_D;

            return ammo;
        }

    private static AmmoType createKrakenMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "Kraken Ammo";
        ammo.setInternalName("Ammo Kraken");
        ammo.addLookupName("Kraken Ammo");
        ammo.damagePerShot = 10;
        ammo.ammoType = AmmoType.T_KRAKENM;
        ammo.shots = 1;
        ammo.bv = 288;
        ammo.cost = 55000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createKrakenAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Kraken-T Ammo";
        ammo.setInternalName("Ammo KrakenT");
        ammo.addLookupName("KrakenT Ammo");
        ammo.damagePerShot = 10;
        ammo.ammoType = AmmoType.T_KRAKEN_T;
        ammo.shots = 1;
        ammo.bv = 288;
        ammo.cost = 55000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLKrakenAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan Kraken-T Ammo";
        ammo.setInternalName("Ammo Clan KrakenT");
        ammo.addLookupName("CLKrakenT Ammo");
        ammo.damagePerShot = 10;
        ammo.ammoType = AmmoType.T_KRAKEN_T;
        ammo.shots = 1;
        ammo.bv = 288;
        ammo.cost = 55000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3057;
        ammo.techLevel.put(3057, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createKillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Killer Whale Ammo";
        ammo.setInternalName("Ammo Killer Whale");
        ammo.addLookupName("KillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_E};
        ammo.introDate = 2305;
        ammo.techLevel.put(2305, ammo.techLevel.get(3067));
        ammo.extinctDate = 2855;
        ammo.reintroDate = 3051;
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLKillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Killer Whale Ammo";
        ammo.setInternalName("Ammo Clan Killer Whale");
        ammo.addLookupName("CLKillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_C, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createKillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Killer Whale-T Ammo";
        ammo.setInternalName("Ammo Killer Whale-T");
        ammo.addLookupName("KillerWhaleT Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLKillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan Killer Whale-T Ammo";
        ammo.setInternalName("Ammo Clan Killer Whale-T");
        ammo.addLookupName("CLKillerWhaleT Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }


    private static AmmoType createSantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Santa Anna Ammo";
        ammo.setInternalName("Ammo Santa Anna");
        ammo.addLookupName("SantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.subMunitionBegin = 0;
        ammo.subMunitionLength = ammo.shortName.length();
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.munitionType = AmmoType.M_SANTA_ANNA;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_NUCLEAR).or(F_CAP_MISSILE);
        ammo.capital = true;

        return ammo;
    }
    
    private static AmmoType createCLSantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Santa Anna Ammo";
        ammo.setInternalName("Ammo Clan Santa Anna");
        ammo.addLookupName("CLSantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.subMunitionBegin = 0;
        ammo.subMunitionLength = ammo.shortName.length();
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.munitionType = AmmoType.M_SANTA_ANNA;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_NUCLEAR).or(F_CAP_MISSILE);
        ammo.capital = true;

        return ammo;
    }

    private static AmmoType createWhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "White Shark Ammo";
        ammo.setInternalName("Ammo White Shark");
        ammo.addLookupName("WhiteShark Ammo");
        ammo.addLookupName("White Shark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_WHITE_SHARK;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.ammoRatio = 40;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_E};
        ammo.introDate = 2305;
        ammo.techLevel.put(2305, ammo.techLevel.get(3067));
        ammo.extinctDate = 2854;
        ammo.reintroDate = 3051;
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLWhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan White Shark Ammo";
        ammo.setInternalName("Ammo Clan White Shark");
        ammo.addLookupName("CLWhiteShark Ammo");
        ammo.addLookupName("CLWhite Shark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_WHITE_SHARK;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.ammoRatio = 40;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_C, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createWhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "White Shark-T Ammo";
        ammo.setInternalName("Ammo White Shark-T");
        ammo.addLookupName("WhiteSharkT Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_WHITE_SHARK;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLWhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan White Shark-T Ammo";
        ammo.setInternalName("Ammo Clan White Shark-T");
        ammo.addLookupName("CLWhiteSharkT Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_WHITE_SHARK;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Barracuda Ammo";
        ammo.setInternalName("Ammo Barracuda");
        ammo.addLookupName("Barracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_BARRACUDA;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_E};
        ammo.introDate = 2305;
        ammo.techLevel.put(2305, ammo.techLevel.get(3067));
        ammo.extinctDate = 2855;
        ammo.reintroDate = 3051;
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLBarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Barracuda Ammo";
        ammo.setInternalName("Ammo Clan Barracuda");
        ammo.addLookupName("CLBarracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_BARRACUDA;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_C, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createBarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Barracuda-T Ammo";
        ammo.setInternalName("Ammo Barracuda-T");
        ammo.addLookupName("BarracudaT Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_BARRACUDA;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLBarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan Barracuda-T Ammo";
        ammo.setInternalName("Ammo Clan Barracuda-T");
        ammo.addLookupName("CLBarracudaT Ammo"); 
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_BARRACUDA;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createAR10BarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "AR10 Barracuda Ammo";
        ammo.setInternalName("Ammo AR10 Barracuda");
        ammo.addLookupName("AR10 Barracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_D};
        ammo.introDate = 2550;
        ammo.techLevel.put(2550, ammo.techLevel.get(3067));
        ammo.extinctDate = 2850;
        ammo.reintroDate = 3051;
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLAR10BarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan AR10 Barracuda Ammo";
        ammo.setInternalName("Ammo Clan AR10 Barracuda");
        ammo.addLookupName("CLAR10 Barracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_C, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createAR10KillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "AR10 Killer Whale Ammo";
        ammo.setInternalName("Ammo AR10 Killer Whale");
        ammo.addLookupName("AR10 KillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2550;
        ammo.techLevel.put(2550, ammo.techLevel.get(3067));
        ammo.extinctDate = 2850;
        ammo.reintroDate = 3051;
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLAR10KillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan AR10 Killer Whale Ammo";
        ammo.setInternalName("Ammo Clan AR10 Killer Whale");
        ammo.addLookupName("CLAR10 KillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_C, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createAR10SantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "AR10 Santa Anna Ammo";
        ammo.setInternalName("Ammo AR10 Santa Anna");
        ammo.addLookupName("AR10 SantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.subMunitionBegin = 0;
        ammo.subMunitionLength = ammo.shortName.length();
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.munitionType = AmmoType.M_SANTA_ANNA;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_NUCLEAR)
                .or(F_CAP_MISSILE);
        ammo.capital = true;

        return ammo;
    }
    
    private static AmmoType createCLAR10SantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan AR10 Santa Anna Ammo";
        ammo.setInternalName("Ammo Clan AR10 Santa Anna");
        ammo.addLookupName("CLAR10 SantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.subMunitionBegin = 0;
        ammo.subMunitionLength = ammo.shortName.length();
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.munitionType = AmmoType.M_SANTA_ANNA;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_NUCLEAR)
                .or(F_CAP_MISSILE);
        ammo.capital = true;

        return ammo;
    }

    private static AmmoType createAR10WhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "AR10 White Shark Ammo";
        ammo.setInternalName("Ammo AR10 White Shark");
        ammo.addLookupName("AR10 WhiteShark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_X, EquipmentType.RATING_E};
        ammo.introDate = 2305;
        ammo.techLevel.put(2305, ammo.techLevel.get(3067));
        ammo.extinctDate = 2854;
        ammo.reintroDate = 3051;
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLAR10WhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan AR10 White Shark Ammo";
        ammo.setInternalName("Ammo Clan AR10 White Shark");
        ammo.addLookupName("CLAR10 WhiteShark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_C, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createAR10BarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "AR10 Barracuda-T Ammo";
        ammo.setInternalName("Ammo AR10 Barracuda-T");
        ammo.addLookupName("AR10 BarracudaT Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_TELE_MISSILE)
                .or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLAR10BarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan AR10 Barracuda-T Ammo";
        ammo.setInternalName("Ammo Clan AR10 Barracuda-T");
        ammo.addLookupName("CLAR10 BarracudaT Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_TELE_MISSILE)
                .or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createAR10KillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "AR10 Killer Whale-T Ammo";
        ammo.setInternalName("Ammo AR10 Killer Whale-T");
        ammo.addLookupName("AR10 KillerWhaleT Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_TELE_MISSILE)
                .or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLAR10KillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan AR10 Killer Whale-T Ammo";
        ammo.setInternalName("Ammo Clan AR10 Killer Whale-T");
        ammo.addLookupName("CLAR10 KillerWhaleT Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_TELE_MISSILE)
                .or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createAR10WhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "AR10 White Shark-T Ammo";
        ammo.setInternalName("Ammo AR10 White Shark-T");
        ammo.addLookupName("AR10 WhiteSharkT Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_TELE_MISSILE)
                .or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }
    
    private static AmmoType createCLAR10WhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_UNOFFICIAL);
        ammo.name = "Clan AR10 White Shark-T Ammo";
        ammo.setInternalName("Ammo Clan AR10 White Shark-T");
        ammo.addLookupName("CLAR10 WhiteSharkT Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_TELE_MISSILE)
                .or(F_CAP_MISSILE);
        ammo.capital = true;
        ammo.munitionType = AmmoType.M_TELE;
        ammo.introDate = 3056;
        ammo.techLevel.put(3056, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createScreenLauncherAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Screen Launcher Ammo";
        ammo.setInternalName("Ammo Screen");
        ammo.addLookupName("ScreenLauncher Ammo");
        ammo.damagePerShot = 0;
        ammo.ammoType = AmmoType.T_SCREEN_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 20;
        ammo.cost = 10000;
        ammo.introDate = 3055;
        ammo.techLevel.put(3055, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_F};
        ammo.techRating = RATING_E;
        return ammo;
    }

    private static AmmoType createAlamoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Alamo Ammo";
        ammo.setInternalName("Ammo Alamo");
        ammo.addLookupName("Alamo Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_ALAMO;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.flags = ammo.flags.or(F_NUCLEAR);
        ammo.capital = true;

        return ammo;
    }

    private static AmmoType createLightSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Light SCC Ammo";
        ammo.setInternalName("Ammo Light SCC");
        ammo.addLookupName("Light SCC Ammo");
        ammo.addLookupName("LightSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.bv = 47;
        ammo.cost = 10000;
        ammo.ammoRatio = 2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createMediumSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Medium SCC Ammo";
        ammo.setInternalName("Ammo Medium SCC");
        ammo.addLookupName("Medium SCC Ammo");
        ammo.addLookupName("MediumSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.bv = 89;
        ammo.cost = 18000;
        ammo.ammoRatio = 1;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createHeavySCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Heavy SCC Ammo";
        ammo.setInternalName("Ammo Heavy SCC");
        ammo.addLookupName("Heavy SCC Ammo");
        ammo.addLookupName("HeavySCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.bv = 124;
        ammo.cost = 25000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3073;
        ammo.techLevel.put(3073, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLLightSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Light SCC Ammo";
        ammo.setInternalName("Ammo Cllan Light SCC");
        ammo.addLookupName("CLLight SCC Ammo");
        ammo.addLookupName("CLLightSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.bv = 47;
        ammo.cost = 10000;
        ammo.ammoRatio = 2;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3090;
        ammo.techLevel.put(3090, ammo.techLevel.get(3067));
        ammo.techLevel.put(3091, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLMediumSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Medium SCC Ammo";
        ammo.setInternalName("Ammo Clan Medium SCC");
        ammo.addLookupName("CLMedium SCC Ammo");
        ammo.addLookupName("CLMediumSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.bv = 89;
        ammo.cost = 18000;
        ammo.ammoRatio = 1;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3090;
        ammo.techLevel.put(3090, ammo.techLevel.get(3067));
        ammo.techLevel.put(3091, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLHeavySCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Heavy SCC Ammo";
        ammo.setInternalName("Ammo Clan Heavy SCC");
        ammo.addLookupName("CLHeavy SCC Ammo");
        ammo.addLookupName("CLHeavySCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.bv = 124;
        ammo.cost = 25000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3090;
        ammo.techLevel.put(3090, ammo.techLevel.get(3067));
        ammo.techLevel.put(3091, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createMantaRayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Manta Ray Ammo";
        ammo.setInternalName("Ammo Manta Ray");
        ammo.addLookupName("MantaRay Ammo");
        ammo.addLookupName("Manta Ray Ammo");
        ammo.damagePerShot = 5;
        ammo.ammoType = AmmoType.T_MANTA_RAY;
        ammo.shots = 1;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.ammoRatio = 18;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createSwordfishAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Swordfish Ammo";
        ammo.setInternalName("Ammo Swordfish");
        ammo.addLookupName("Swordfish Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_SWORDFISH;
        ammo.shots = 1;
        ammo.bv = 40;
        ammo.cost = 25000;
        ammo.capital = true;
        ammo.ammoRatio = 15;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createStingrayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Stringray Ammo";
        ammo.setInternalName("Ammo Stringray");
        ammo.addLookupName("Stingray Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_STINGRAY;
        ammo.shots = 1;
        ammo.bv = 62;
        ammo.cost = 19000;
        ammo.ammoRatio = 12;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createPiranhaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Piranha Ammo";
        ammo.setInternalName("Ammo Piranha");
        ammo.addLookupName("Piranha Ammo");
        ammo.addLookupName("PiranhaAmmo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_PIRANHA;
        ammo.shots = 1;
        ammo.bv = 84;
        ammo.cost = 15000;
        ammo.ammoRatio = 10;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }
    
    private static AmmoType createCLMantaRayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Manta Ray Ammo";
        ammo.setInternalName("Ammo Clan Manta Ray");
        ammo.addLookupName("CLMantaRay Ammo");
        ammo.addLookupName("CLManta Ray Ammo");
        ammo.damagePerShot = 5;
        ammo.ammoType = AmmoType.T_MANTA_RAY;
        ammo.shots = 1;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.ammoRatio = 18;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3067));
        ammo.techLevel.put(3072, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLSwordfishAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Swordfish Ammo";
        ammo.setInternalName("Ammo Clan Swordfish");
        ammo.addLookupName("CLSwordfish Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_SWORDFISH;
        ammo.shots = 1;
        ammo.bv = 40;
        ammo.cost = 25000;
        ammo.capital = true;
        ammo.ammoRatio = 15;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3067));
        ammo.techLevel.put(3072, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLStingrayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Stringray Ammo";
        ammo.setInternalName("Ammo Clan Stringray");
        ammo.addLookupName("CLStingray Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_STINGRAY;
        ammo.shots = 1;
        ammo.bv = 62;
        ammo.cost = 19000;
        ammo.ammoRatio = 12;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3067));
        ammo.techLevel.put(3072, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLPiranhaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Clan Piranha Ammo";
        ammo.setInternalName("Ammo Clan Piranha");
        ammo.addLookupName("CLPiranha Ammo");
        ammo.addLookupName("CLPiranhaAmmo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_PIRANHA;
        ammo.shots = 1;
        ammo.bv = 84;
        ammo.cost = 15000;
        ammo.ammoRatio = 10;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3070;
        ammo.techLevel.put(3070, ammo.techLevel.get(3067));
        ammo.techLevel.put(3072, TechConstants.T_CLAN_TW);
        ammo.techRating = RATING_E;

        return ammo;
    }


    private static AmmoType createISAPMortar1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Shaped Charge Mortar 1 Ammo";
        ammo.shortName = "Mortar SC 1";
        ammo.setInternalName("IS Ammo SC Mortar-1");
        ammo.addLookupName("ISArmorPiercingMortarAmmo1");
        ammo.addLookupName("ISSCMortarAmmo1");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 24;
        ammo.bv = 1.2;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2531;
        ammo.techLevel.put(2531, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAPMortar2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Shaped Charge Mortar 2 Ammo";
        ammo.shortName = "Mortar SC 2";
        ammo.setInternalName("IS Ammo SC Mortar-2");
        ammo.addLookupName("ISArmorPiercingMortarAmmo2");
        ammo.addLookupName("ISSCMortarAmmo2");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 12;
        ammo.bv = 2.4;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2531;
        ammo.techLevel.put(2531, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAPMortar4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Shaped Charge Mortar 4 Ammo";
        ammo.shortName = "Mortar SC 4";
        ammo.setInternalName("IS Ammo SC Mortar-4");
        ammo.addLookupName("ISArmorPiercingMortarAmmo4");
        ammo.addLookupName("ISSCMortarAmmo4");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 6;
        ammo.bv = 3.6;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2531;
        ammo.techLevel.put(2531, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISAPMortar8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "Shaped Charge Mortar 8 Ammo";
        ammo.shortName = "Mortar SC 8";
        ammo.setInternalName("IS Ammo SC Mortar-8");
        ammo.addLookupName("ISArmorPiercingMortarAmmo8");
        ammo.addLookupName("ISSCMortarAmmo8");
        ammo.damagePerShot = 2;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 4;
        ammo.bv = 7.2;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2531;
        ammo.techLevel.put(2531, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLAPMortar1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Shaped Charge Mortar 1 Ammo";
        ammo.shortName = "Mortar SC 1";
        ammo.setInternalName("Clan Ammo SC Mortar-1");
        ammo.addLookupName("CLArmorPiercingMortarAmmo1");
        ammo.addLookupName("CLSCMortarAmmo1");
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 24;
        ammo.bv = 1.2;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2840;
        ammo.techLevel.put(2840, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLAPMortar2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Shaped Charge Mortar 2 Ammo";
        ammo.shortName = "Mortar SC 2";
        ammo.setInternalName("Clan Ammo SC Mortar-2");
        ammo.addLookupName("CLArmorPiercingMortarAmmo2");
        ammo.addLookupName("CLSCMortarAmmo2");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 12;
        ammo.bv = 2.4;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2840;
        ammo.techLevel.put(2840, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLAPMortar4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Shaped Charge Mortar 4 Ammo";
        ammo.shortName = "Mortar SC 4";
        ammo.setInternalName("Clan Ammo SC Mortar-4");
        ammo.addLookupName("CLArmorPiercingMortarAmmo4");
        ammo.addLookupName("CLSCMortarAmmo4");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 6;
        ammo.bv = 3.6;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2840;
        ammo.techLevel.put(2840, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createCLAPMortar8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "Shaped Charge Mortar 8 Ammo";
        ammo.shortName = "Mortar SC 8";
        ammo.setInternalName("Clan Ammo SC Mortar-8");
        ammo.addLookupName("CLArmorPiercingMortarAmmo8");
        ammo.addLookupName("CLSCMortarAmmo8");
        ammo.damagePerShot = 2;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_MEK_MORTAR;
        ammo.shots = 4;
        ammo.bv = 7.2;
        ammo.cost = 28000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2840;
        ammo.techLevel.put(2840, ammo.techLevel.get(3067));
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISCruiseMissile50Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Cruise Missile/50 Ammo";
        ammo.setInternalName("ISCruiseMissile50Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 50;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 75;
        ammo.cost = 20000;
        ammo.tonnage = 25;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3065;
        ammo.techLevel.put(3065, ammo.techLevel.get(3067));
        ammo.techLevel.put(3095, TechConstants.T_IS_ADVANCED);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISCruiseMissile70Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Cruise Missile/70 Ammo";
        ammo.setInternalName("ISCruiseMissile70Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 70;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 129;
        ammo.cost = 50000;
        ammo.tonnage = 35;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3065;
        ammo.techLevel.put(3065, ammo.techLevel.get(3067));
        ammo.techLevel.put(3095, TechConstants.T_IS_ADVANCED);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISCruiseMissile90Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Cruise Missile/90 Ammo";
        ammo.setInternalName("ISCruiseMissile90Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 90;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 191;
        ammo.cost = 90000;
        ammo.tonnage = 45;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3065;
        ammo.techLevel.put(3065, ammo.techLevel.get(3067));
        ammo.techLevel.put(3095, TechConstants.T_IS_ADVANCED);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISCruiseMissile120Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Cruise Missile/120 Ammo";
        ammo.setInternalName("ISCruiseMissile120Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 120;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 285;
        ammo.cost = 140000;
        ammo.tonnage = 60;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3065;
        ammo.techLevel.put(3065, ammo.techLevel.get(3067));
        ammo.techLevel.put(3095, TechConstants.T_IS_ADVANCED);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISBPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "B Pod Ammo";
        ammo.setInternalName("ISBPodAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_BPOD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.introDate = 3069;
        ammo.techLevel.put(3069, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createCLBPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "B Pod Ammo";
        ammo.setInternalName("ClanBPodAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_BPOD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.introDate = 3068;
        ammo.techLevel.put(3068, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_X, RATING_E};
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISNailRivetGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Nail/Rivet Gun Ammo";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("IS Ammo Nail/Rivet - Full");
        ammo.addLookupName("ISNailRivetGun Ammo (300)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NAIL_RIVET_GUN;
        ammo.shots = 300;
        ammo.bv = 1;
        ammo.cost = 300;
        ammo.tonnage = 1f;
        ammo.explosive = false;
        ammo.techRating = RATING_A;
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_A, RATING_A, RATING_A};

        return ammo;
    }

    private static AmmoType createISNailRivetGunAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        ammo.name = "Half Nail/Rivet Gun Ammo";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("IS Ammo Nail/Rivet - Half");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NAIL_RIVET_GUN;
        ammo.shots = 150;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        ammo.cost = 150;
        ammo.explosive = false;
        ammo.techRating = RATING_A;
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_A, RATING_A, RATING_A};

        return ammo;
    }

    private static AmmoType createCLNailRivetGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Nail/Rivet Gun Ammo";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("CL Ammo Nail/Rivet - Full");
        ammo.addLookupName("CLNailRivetGun Ammo (300)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NAIL_RIVET_GUN;
        ammo.shots = 300;
        ammo.bv = 1;
        ammo.cost = 300;
        ammo.tonnage = 1f;
        ammo.explosive = false;
        ammo.techRating = RATING_A;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_A, RATING_A};

        return ammo;
    }

    private static AmmoType createCLNailRivetGunAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_TW);
        ammo.name = "Half Nail/Rivet Gun Ammo";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("CL Ammo Nail/Rivet - Half");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NAIL_RIVET_GUN;
        ammo.shots = 150;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        ammo.cost = 150;
        ammo.explosive = false;
        ammo.techRating = RATING_A;
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.availRating = new int[]{RATING_X, RATING_A, RATING_A};

        return ammo;
    }

    private static AmmoType createISAC10iAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_UNOFFICIAL);
        ammo.name = "AC/10i Ammo";
        ammo.shortName = "AC/10i";
        ammo.setInternalName("IS Ammo AC/10i");
        ammo.addLookupName("ISAC10i Ammo");
        ammo.addLookupName("IS Autocannon/10i Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_ACi;
        ammo.shots = 10;
        ammo.bv = 21;
        ammo.cost = 12000;

        return ammo;
    }

    private static AmmoType createISAC10pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "AC/10p Ammo";
        ammo.shortName = "AC/10p";
        ammo.setInternalName("IS Ammo AC/10 Primitive");
        ammo.addLookupName("ISAC10p Ammo");
        ammo.addLookupName("IS Autocannon/10 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_PRIMITIVE;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.cost = 12000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_D, EquipmentType.RATING_D};
        ammo.introDate = 2450;
        ammo.techLevel.put(2450, ammo.techLevel.get(3067));
        ammo.extinctDate = 2460;
        ammo.techRating = RATING_C;

        return ammo;
    }

    private static AmmoType createISGAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/2 Ammo";
        ammo.shortName = "GAC/2";
        ammo.setInternalName("IS Ammo GAC/2");
        ammo.addLookupName("ISGAC2 Ammo");
        ammo.addLookupName("IS Gatling AC/2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 22;
        ammo.bv = 12;
        ammo.cost = 1000;
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, TechConstants.T_IS_UNOFFICIAL);
        ammo.availRating = new int[] { RATING_X, RATING_X, RATING_E };
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISGAC4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/4 Ammo";
        ammo.shortName = "GAC/4";
        ammo.setInternalName("IS Ammo GAC/4");
        ammo.addLookupName("ISGAC4 Ammo");
        ammo.addLookupName("IS Gatling AC/4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 11;
        ammo.bv = 22;
        ammo.cost = 1000;
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, TechConstants.T_IS_UNOFFICIAL);
        ammo.availRating = new int[] { RATING_X, RATING_X, RATING_E };
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISGAC6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/6 Ammo";
        ammo.shortName = "GAC/6";
        ammo.setInternalName("IS Ammo GAC/6");
        ammo.addLookupName("ISGAC6 Ammo");
        ammo.addLookupName("IS Gatling AC/6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 7;
        ammo.bv = 40;
        ammo.cost = 1000;
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, TechConstants.T_IS_UNOFFICIAL);
        ammo.availRating = new int[] { RATING_X, RATING_X, RATING_E };
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISGAC8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "GAC/8 Ammo";
        ammo.shortName = "GAC/8";
        ammo.setInternalName("IS Ammo GAC/8");
        ammo.addLookupName("ISGAC8 Ammo");
        ammo.addLookupName("IS Gatling AC/8 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 5;
        ammo.bv = 53;
        ammo.cost = 1000;
        ammo.introDate = 3062;
        ammo.techLevel.put(3062, TechConstants.T_IS_UNOFFICIAL);
        ammo.availRating = new int[] { RATING_X, RATING_X, RATING_E };
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISEnhancedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Enhanced LRM 5 Ammo";
        ammo.shortName = "NLRM 5";
        ammo.setInternalName("ISEnhancedLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 24;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.bv = 7;
        ammo.cost = 31000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3071));
        ammo.techLevel.put(3082, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISEnhancedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Enhanced LRM 10 Ammo";
        ammo.shortName = "NLRM 10";
        ammo.setInternalName("ISEnhancedLRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 12;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.bv = 13;
        ammo.cost = 31000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3071));
        ammo.techLevel.put(3082, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISEnhancedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Enhanced LRM 15 Ammo";
        ammo.shortName = "NLRM 15";
        ammo.setInternalName("ISEnhancedLRM15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 8;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.bv = 20;
        ammo.cost = 31000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3071));
        ammo.techLevel.put(3082, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISEnhancedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Enhanced LRM 20 Ammo";
        ammo.shortName = "NLRM 20";
        ammo.setInternalName("ISEnhancedLRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes(new String[]{"", "HotLoad"});
        ammo.bv = 26;
        ammo.cost = 31000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3058;
        ammo.techLevel.put(3058, ammo.techLevel.get(3071));
        ammo.techLevel.put(3082, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_E;

        return ammo;
    }

    private static AmmoType createISLightRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Light Rifle Ammo";
        ammo.shortName = "Light Rifle";
        ammo.setInternalName("IS Ammo Light Rifle");
        ammo.addLookupName("ISLight Rifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_RIFLE;
        ammo.shots = 18;
        ammo.bv = 3;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_X};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3071));
        ammo.techLevel.put(3084, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISMediumRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Medium Rifle Ammo";
        ammo.shortName = "Medium Rifle";
        ammo.setInternalName("IS Ammo Medium Rifle");
        ammo.addLookupName("ISMedium Rifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_RIFLE;
        ammo.shots = 9;
        ammo.bv = 6;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_X};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3071));
        ammo.techLevel.put(3084, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createISHeavyRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "Heavy Rifle Ammo";
        ammo.shortName = "Heavy Rifle";
        ammo.setInternalName("IS Ammo Heavy Rifle");
        ammo.addLookupName("ISHeavy Rifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_RIFLE;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.cost = 1000;
        ammo.availRating = new int[]{EquipmentType.RATING_C,
                EquipmentType.RATING_F, EquipmentType.RATING_X};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3071));
        ammo.techLevel.put(3084, TechConstants.T_IS_TW_NON_BOX);
        ammo.techRating = RATING_B;

        return ammo;
    }

    private static AmmoType createCLVGLAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_CLAN_ADVANCED);
        ammo.name = "VGL Fragmentation Grenade";
        ammo.subMunitionBegin = 4;
        ammo.subMunitionLength = 13;
        ammo.shortName = "VGL Fragmentation";
        ammo.setInternalName("CL Ammo VGL");
        ammo.addLookupName("CLVehicularGrenadeLauncherAmmo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_VGL;
        ammo.munitionType = AmmoType.M_STANDARD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 2820;
        ammo.techLevel.put(2820, ammo.techLevel.get(3067));
        ammo.techRating = RATING_B;
        return ammo;
    }

    private static AmmoType createISVGLAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.techLevel.put(3067, TechConstants.T_IS_ADVANCED);
        ammo.name = "VGL Fragmentation Grenade";
        ammo.subMunitionBegin = 4;
        ammo.subMunitionLength = 13;
        ammo.shortName = "VGL Fragmentation";
        ammo.setInternalName("IS Ammo VGL");
        ammo.addLookupName("ISVehicularGrenadeLauncherAmmo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_VGL;
        ammo.munitionType = AmmoType.M_STANDARD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.availRating = new int[]{EquipmentType.RATING_B,
                EquipmentType.RATING_B, EquipmentType.RATING_B};
        ammo.introDate = 1950;
        ammo.techLevel.put(1950, ammo.techLevel.get(3067));
        ammo.techRating = RATING_B;
        return ammo;
    }

    private static AmmoType createISC3RemoteSensorAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        ammo.name = "C3 Remote Sensors";
        ammo.shortName = "C3 Remote Sensor";
        ammo.setInternalName("ISC3Sensors");
        ammo.explosive = false;
        ammo.damagePerShot = 0; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_C3_REMOTE_SENSOR;
        ammo.shots = 4;
        ammo.bv = 6;
        ammo.cost = 100000;
        ammo.availRating = new int[]{EquipmentType.RATING_X,
                EquipmentType.RATING_X, EquipmentType.RATING_F};
        ammo.introDate = 3072;
        ammo.techLevel.put(3072, ammo.techLevel.get(3067));
        ammo.techRating = RATING_E;

        return ammo;
    }

    @Override
    public String toString() {
        return "Ammo: " + name;
    }

    public static boolean canClearMinefield(AmmoType at) {
        // first the normal munition types
        if (at != null) {
            // LRM-20's, RL-20's, and MRM 20, 30, and 40 can clear minefields
            if (((at.getAmmoType() == T_LRM)
                    || (at.getAmmoType() == T_LRM_STREAK)
                    || (at.getAmmoType() == T_EXLRM)
                    || (at.getAmmoType() == T_PXLRM)
                    || (at.getAmmoType() == T_MRM)
                    || (at.getAmmoType() == T_MRM_STREAK) 
                    || (at.getAmmoType() == T_ROCKET_LAUNCHER))
                    && (at.getRackSize() >= 20)
                    && ((at.getMunitionType() == M_STANDARD)
                            || (at.getMunitionType() == M_ARTEMIS_CAPABLE)
                            || (at.getMunitionType() == M_ARTEMIS_V_CAPABLE)
                            || (at.getMunitionType() == M_NARC_CAPABLE))) {
                return true;
            }
            // ATMs
            if ((at.getAmmoType() == T_ATM)
                    && ((at.getRackSize() >= 12 && at.getMunitionType() != M_EXTENDED_RANGE)
                            || (at.getRackSize() >= 9 && at.getMunitionType() == M_HIGH_EXPLOSIVE))) {
                return true;
            }
            // Artillery
            if (((at.getAmmoType() == T_ARROW_IV)
                    || (at.getAmmoType() == T_LONG_TOM)
                    || (at.getAmmoType() == T_SNIPER) || (at.getAmmoType() == T_THUMPER))
                    && (at.getMunitionType() == M_STANDARD)) {
                return true;
            }
        }
        // TODO: mine clearance munitions

        return false;
    }

    public static boolean canDeliverMinefield(AmmoType at) {

        if ((at != null)
                && ((at.getAmmoType() == T_LRM) || (at.getAmmoType() == AmmoType.T_MML))
                && ((at.getMunitionType() == M_THUNDER)
                || (at.getMunitionType() == M_THUNDER_INFERNO)
                || (at.getMunitionType() == M_THUNDER_AUGMENTED)
                || (at.getMunitionType() == M_THUNDER_VIBRABOMB) || (at
                .getMunitionType() == M_THUNDER_ACTIVE))) {
            return true;
        }

        return false;
    }

    private void addToEnd(AmmoType base, String modifier) {
        Enumeration<String> n = base.getNames();
        while (n.hasMoreElements()) {
            String s = n.nextElement();
            addLookupName(s + modifier);
        }
    }

    private void addBeforeString(AmmoType base, String keyWord, String modifier) {
        Enumeration<String> n = base.getNames();
        while (n.hasMoreElements()) {
            String s = n.nextElement();
            StringBuffer sb = new StringBuffer(s);
            sb.insert(s.lastIndexOf(keyWord), modifier);
            addLookupName(sb.toString());
        }
    }

    /**
     * Helper class for creating munition types.
     */
    static private class MunitionMutator {
        /**
         * The name of this munition type.
         */
        private String name;

        /**
         * The weight ratio of a round of this munition to a standard round.
         */
        private int weight;

        /**
         * The munition flag(s) for this type.
         */
        private long type;

        private Map<Integer, Integer> techLevel;

        /**
         * Create a mutator that will transform the <code>AmmoType</code> of a
         * base round into one of its muntions.
         *
         * @param munitionName   - the <code>String</code> name of this munition type.
         * @param weightRatio    - the <code>int</code> ratio of a round of this munition
         *                       to a round of the standard type.
         * @param munitionType   - the <code>int</code> munition flag(s) of this type.
         * @param introYear      - the year of introduction for the tech level
         * @param introTechLevel - The new tech level, if different from the base ammo
         *                       type.
         */
        public MunitionMutator(String munitionName, int weightRatio,
                               long munitionType, int introYear, int introTechLevel) {
            name = munitionName;
            weight = weightRatio;
            type = munitionType;
            techLevel = new HashMap<Integer, Integer>();
            techLevel.put(introYear, introTechLevel);
        }

        /**
         * Adds another tech level and year that level is assumed.
         *
         * @param year
         * @param tl
         */
        public MunitionMutator addTechLevel(int year, int tl) {
            techLevel.put(year, tl);
            return this;
        }

        /**
         * Create the <code>AmmoType</code> for this munition type for the given
         * rack size.
         *
         * @param base - the <code>AmmoType</code> of the base round.
         * @return this munition's <code>AmmotType</code>.
         */
        public AmmoType createMunitionType(AmmoType base) {
            StringBuffer nameBuf;
            StringBuffer internalName;
            int index;

            // Create an uninitialized munition object.
            AmmoType munition = new AmmoType();
            munition.setTonnage(base.getTonnage(null));

            // Manipulate the base round's names, depending on ammoType.
            switch (base.ammoType) {
                case AmmoType.T_AC:
                case AmmoType.T_AC_PRIMITIVE:
                case AmmoType.T_LAC:
                    // Add the munition name to the beginning of the display
                    // name.
                    nameBuf = new StringBuffer(name);
                    nameBuf.append(" ");
                    munition.subMunitionBegin = 0;
                    munition.subMunitionLength = nameBuf.length();
                    nameBuf.append(base.name);
                    munition.name = nameBuf.toString();

                    // Add the munition name to the end of the TDB ammo name.
                    nameBuf = new StringBuffer(" - ");
                    nameBuf.append(name);
                    munition.addToEnd(base, " - " + name);

                    // The munition name appears in the middle of the other
                    // names.
                    nameBuf = new StringBuffer(base.internalName);
                    index = base.internalName.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    nameBuf.insert(index, name);
                    munition.setInternalName(nameBuf.toString());
                    munition.shortName = munition.name;
                    munition.addBeforeString(base, "Ammo", name + " ");
                    nameBuf = null;
                    break;
                case AmmoType.T_ARROW_IV:
                    // The munition name appears in the middle of all names.
                    nameBuf = new StringBuffer(base.name);
                    index = base.name.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    // Do special processing for munition names ending in "IV".
                    // Note: this does not work for The Drawing Board
                    if (name.endsWith("-IV")) {
                        StringBuffer tempName = new StringBuffer(name);
                        tempName.setLength(tempName.length() - 3);
                        munition.subMunitionBegin = nameBuf.length();
                        munition.subMunitionLength = tempName.length();
                        nameBuf.insert(index, tempName.toString());
                    } else {
                        munition.subMunitionBegin = nameBuf.length();
                        munition.subMunitionLength = name.length();
                        nameBuf.insert(index, name);
                    }
                    munition.name = nameBuf.toString();

                    nameBuf = new StringBuffer(base.internalName);
                    index = base.internalName.lastIndexOf("Ammo");
                    nameBuf.insert(index, name);
                    munition.setInternalName(nameBuf.toString());
                    munition.shortName = munition.name;

                    munition.addBeforeString(base, "Ammo", name + " ");
                    munition.addToEnd(base, " - " + name);
                    if (name.equals("Homing")) {
                        munition.addToEnd(base, " (HO)"); // mep
                    }
                    nameBuf = null;

                    break;
                case AmmoType.T_SRM:
                case AmmoType.T_SRM_PRIMITIVE:
                case AmmoType.T_MRM:
                case AmmoType.T_LRM:
                case AmmoType.T_LRM_PRIMITIVE:
                case AmmoType.T_MML:
                case AmmoType.T_NLRM:
                case AmmoType.T_SRM_TORPEDO:
                case AmmoType.T_LRM_TORPEDO:
                    // Add the munition name to the end of some of the ammo
                    // names.
                    nameBuf = new StringBuffer(" ");
                    nameBuf.append(name);
                    munition.setInternalName(base.internalName
                            + nameBuf.toString());
                    munition.addToEnd(base, nameBuf.toString());
                    nameBuf.insert(0, " -");
                    munition.addToEnd(base, nameBuf.toString());

                    // The munition name appears in the middle of the other
                    // names.
                    nameBuf = new StringBuffer(base.name);
                    index = base.name.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    munition.subMunitionBegin = index;
                    munition.subMunitionLength = name.length();
                    nameBuf.insert(index, name);
                    munition.name = nameBuf.toString();
                    nameBuf = null;
                    munition.shortName = munition.name;
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case AmmoType.T_VGL:
                    // Replace "Fragmentation" with the submunition name
                    munition.name = base.name.replace("Fragmentation", name);
                    munition.subMunitionBegin = base.subMunitionBegin;
                    munition.subMunitionLength = name.length();                    

                    munition.shortName = 
                            base.shortName.replace("Fragmentation", name);
                    internalName = new StringBuffer(base.getInternalName());
                    munition.setInternalName(internalName.insert(
                            internalName.lastIndexOf("Ammo"), name + " ")
                            .toString());
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case AmmoType.T_MEK_MORTAR:
                    // Replace "Shaped Charge" with the submunition name
                    munition.name = base.name.replace("Shaped Charge", name);
                    munition.subMunitionBegin = base.subMunitionBegin;
                    munition.subMunitionLength = name.length();                    
                    String abr = "SC";
                    if (type == AmmoType.M_AIRBURST) {
                        abr = "AB";
                    } else if (type == AmmoType.M_ANTI_PERSONNEL) { 
                        abr = "AP";
                    } else if (type == AmmoType.M_FLARE) {
                        abr = "FL";
                    } else if (type == AmmoType.M_SMOKE_WARHEAD) {
                        abr = "SM";
                    } else if (type == AmmoType.M_SEMIGUIDED) {
                        abr = "SG";
                    }
                    munition.shortName = base.shortName.replace("SC", abr);
                    internalName = new StringBuffer(base.getInternalName()
                            .replace("SC", abr));                    
                    munition.setInternalName(internalName.toString());
                    break;                    
                case AmmoType.T_LONG_TOM:
                case AmmoType.T_SNIPER:
                case AmmoType.T_THUMPER:
                case AmmoType.T_LONG_TOM_CANNON:
                case AmmoType.T_SNIPER_CANNON:
                case AmmoType.T_THUMPER_CANNON:
                case AmmoType.T_VEHICLE_FLAMER:
                case AmmoType.T_HEAVY_FLAMER:
                case AmmoType.T_FLUID_GUN:
                    // Add the munition name to the beginning of the display
                    // name.
                    munition.subMunitionBegin = 0;
                    munition.subMunitionLength = name.length();
                    nameBuf = new StringBuffer(name);
                    nameBuf.append(" ");
                    nameBuf.append(base.name);
                    munition.name = nameBuf.toString();
                    munition.setInternalName(munition.name);
                    munition.addToEnd(base, munition.name);

                    munition.shortName = munition.name;
                    // The munition name appears in the middle of the other
                    // names.
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Don't know how to create munitions for "
                                    + base.ammoType);
            }

            // Assign our munition type.
            munition.munitionType = type;

            // Make sure the tech level is now correct.
            if (!techLevel.isEmpty()) {
                munition.techLevel = techLevel;
            } else {
                munition.techLevel = base.techLevel;
            }

            // Reduce base number of shots to reflect the munition's weight.
            munition.shots = Math.max(1, base.shots / weight);
            munition.kgPerShot = base.kgPerShot * weight;

            // copy base ammoType
            munition.ammoType = base.ammoType;
            // check for cost
            double cost = base.cost;
            double bv = base.bv;

            if ((munition.getAmmoType() == T_AC)
                    || (munition.getAmmoType() == T_LAC)) {
                if (munition.getMunitionType() == AmmoType.M_ARMOR_PIERCING) {
                    cost *= 4;
                }
                if ((munition.getMunitionType() == AmmoType.M_FLECHETTE)
                        || (munition.getMunitionType() == AmmoType.M_FLAK)) {
                    cost *= 1.5;
                }

                if (munition.getMunitionType() == AmmoType.M_TRACER) {
                    cost *= 1.5;
                    bv *= .25;
                }

                if (munition.getMunitionType() == AmmoType.M_INCENDIARY_AC) {
                    cost *= 2;
                }
                if (munition.getMunitionType() == AmmoType.M_PRECISION) {
                    cost *= 6;
                }
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_AX_HEAD)) {
                cost *= .5;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SMOKE_WARHEAD)) {
                cost *= .5;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_INCENDIARY_LRM)) {
                cost *= 1.5;
            }

            if (((munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_MML))
                    && (munition.getMunitionType() == AmmoType.M_INFERNO)) {
                cost = 13500;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SEMIGUIDED)) {
                cost *= 3;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SWARM)) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SWARM_I)) {
                cost *= 3;
                bv *= .2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER)) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED)) {
                cost *= 4;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_INFERNO)) {
                cost *= 1;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)) {
                cost *= 2.5;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)) {
                cost *= 3;
            }
            if (munition.getMunitionType() == AmmoType.M_HOMING) {
                cost = 15000;
                // Allow Homing munitions to instantly switch between modes
                munition.instantModeSwitch = true;
                munition.setModes(new String[] { "Homing", "Non-Homing" });
            }
            if (munition.getMunitionType() == AmmoType.M_FASCAM) {
                cost *= 1.5;
            }
            if (munition.getMunitionType() == AmmoType.M_INFERNO_IV) {
                cost *= 1;
            }
            if (munition.getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
                cost *= 2;
            }

            // This is just a hack to make it expensive.
            // We don't actually have a price for this.
            if (munition.getMunitionType() == AmmoType.M_DAVY_CROCKETT_M) {
                cost *= 50;
            }
            if (munition.getMunitionType() == AmmoType.M_LASER_INHIB) {
                cost *= 4;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_NARC_CAPABLE)) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE)) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_LISTEN_KILL)) {
                cost *= 1.1;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && ((munition.getMunitionType() == AmmoType.M_ANTI_TSM)
                    || (munition.getMunitionType() == AmmoType.M_DEAD_FIRE) || (munition
                    .getMunitionType() == AmmoType.M_FRAGMENTATION))) {
                cost *= 2;
            }

            if (((munition.getAmmoType() == AmmoType.T_MML) || (munition
                    .getAmmoType() == AmmoType.T_SRM))
                    && ((munition.getMunitionType() == AmmoType.M_TANDEM_CHARGE) || (munition
                    .getMunitionType() == AmmoType.M_ARTEMIS_V_CAPABLE))) {
                cost *= 5;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM)
                    || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition
                    .getAmmoType() == AmmoType.T_NLRM))
                    && ((munition.getMunitionType() == AmmoType.M_HEAT_SEEKING) || (munition
                    .getMunitionType() == AmmoType.M_FOLLOW_THE_LEADER))) {
                cost *= 2;
                bv *= .5;
            }
            if (((munition.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (munition.getAmmoType() == AmmoType.T_HEAVY_FLAMER) || (munition
                    .getAmmoType() == AmmoType.T_FLUID_GUN))
                    && (munition.getMunitionType() == AmmoType.M_COOLANT)) {
                cost = 3000;
            }
            munition.bv = bv;
            munition.cost = cost;

            // Copy over all other values.
            munition.damagePerShot = base.damagePerShot;
            munition.rackSize = base.rackSize;
            munition.ammoType = base.ammoType;
            munition.flags = base.flags;
            munition.hittable = base.hittable;
            munition.explosive = base.explosive;
            munition.toHitModifier = base.toHitModifier;

            // Return the new munition.
            return munition;
        }
    } // End private class MunitionMutator

    /**
     * get bv for protomech loads
     */
    public double getProtoBV(int shots) {
        if ((getAmmoType() == AmmoType.T_SRM)
                || (getAmmoType() == AmmoType.T_SRM_STREAK)
                || (getAmmoType() == AmmoType.T_LRM)
                || (getAmmoType() == AmmoType.T_SRM_TORPEDO)
                || (getAmmoType() == AmmoType.T_LRM_TORPEDO)) {
            return ((kgPerShot * rackSize * shots) / 1000) * bv;
        }
        return ((kgPerShot * shots) / 1000) * bv;
    }

    /**
     * get BV for BA loads
     *
     * @return
     */
    public double getBABV() {
        return ((kgPerShot * shots) / 1000) * bv;
    }

    @Override
    public String getShortName() {
        if (shortName.trim().length() < 1) {
            return getName();
        }

        return shortName;
    }

    public String getSubMunitionName() {
        if (ammoType == T_MEK_MORTAR) {
            return name.substring(subMunitionBegin, subMunitionBegin
                    + subMunitionLength);
        } else {
            return shortName.substring(subMunitionBegin, subMunitionBegin
                    + subMunitionLength);
        }
    }

    /**
     * Checks to ensure that the given ammo can be used with the giveen weapon type.  Performs the following tests:<br/>
     * {@code ammo} != null<br/>
     * {@link Mounted#getType()} instanceof {@link AmmoType}<br/>
     * {@link Mounted#isAmmoUsable()}<br/>
     * {@link #isAmmoValid(AmmoType, WeaponType)}.
     *
     * @param ammo       The ammunition to be tested.
     * @param weaponType The weapon the ammo is to be used with.
     * @return TRUE if the ammmo and weapon are compatible.
     */
    public static boolean isAmmoValid(Mounted ammo, WeaponType weaponType) {
        if (ammo == null) {
            return false;
        }
        if (!(ammo.getType() instanceof AmmoType)) {
            return false;
        }
        return ammo.isAmmoUsable() && isAmmoValid((AmmoType) ammo.getType(), weaponType);
    }

    /**
     * Checks to ensure that the given ammunition type is compatable with the given weapon type.  Performs the following
     * tests:<br/>
     * {@code ammoType} != null<br/>
     * {@link AmmoType#getAmmoType()} == {@link WeaponType#getAmmoType()}<br/>
     * {@link AmmoType#getRackSize()} == {@link WeaponType#getRackSize()}
     *
     * @param ammoType   The type of ammo to be tested.
     * @param weaponType The type of weapon the ammo is to be used with.
     * @return TRUE if the ammo type and wepaon type are compatable.
     */
    public static boolean isAmmoValid(AmmoType ammoType, WeaponType weaponType) {
        if (ammoType == null) {
            return false;
        }
        if (ammoType.getAmmoType() != weaponType.getAmmoType()) {
            return false;
        }
        if (ammoType.getRackSize() != weaponType.getRackSize()) {
            return false;
        }
        return true;
    }
}