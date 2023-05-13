/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import megamek.common.options.OptionsConstants;

import java.math.BigInteger;
import java.util.*;

@SuppressWarnings(value = "unchecked")
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
    //Removed
    //public static final int T_PXLRM = 40;
    // public static final int T_HSRM = 41;
    //public static final int T_MRM_STREAK = 42;
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
    public static final int T_AC_IMP = 104;
    public static final int T_GAUSS_IMP = 105;
    public static final int T_SRM_IMP = 106;
    public static final int T_LRM_IMP = 107;
    public static final int T_LONG_TOM_PRIM = 108;
    public static final int T_ARROWIV_PROTO = 109;
    public static final int T_KILLER_WHALE_T = 110;
    public static final int T_WHITE_SHARK_T = 111;
    public static final int T_BARRACUDA_T = 112;
    public static final int T_INFANTRY = 113;
    public static final int NUM_TYPES = 114; // Should always be at the end with the highest number

    /**
     * Contains the {@code AmmoType}s that could share ammo (e.g. SRM 2 and SRM 6,
     * both fire SRM rounds).
     */
    private static final Integer[] ALLOWED_BY_TYPE_ARRAY = { AmmoType.T_LRM, AmmoType.T_LRM_PRIMITIVE,
            AmmoType.T_LRM_STREAK, AmmoType.T_LRM_TORPEDO, AmmoType.T_LRM_TORPEDO_COMBO, AmmoType.T_SRM,
            AmmoType.T_SRM_ADVANCED, AmmoType.T_SRM_PRIMITIVE, AmmoType.T_SRM_STREAK, AmmoType.T_SRM_TORPEDO,
            AmmoType.T_MRM, AmmoType.T_ROCKET_LAUNCHER, AmmoType.T_EXLRM, AmmoType.T_MML, AmmoType.T_NLRM, AmmoType.T_MG, AmmoType.T_MG_LIGHT, AmmoType.T_MG_HEAVY,
            AmmoType.T_NAIL_RIVET_GUN, };

    /**
     * Contains the set of {@code AmmoType}s which could share ammo (e.g. SRM 2 and
     * SRM 6, both fire SRM rounds), and conceptually can share ammo.
     *
     * NB: This is used in MekHQ.
     */
    public static final Set<Integer> ALLOWED_BY_TYPE = Set.of(ALLOWED_BY_TYPE_ARRAY);

    // ammo flags
    public static final BigInteger F_MG = BigInteger.valueOf(1).shiftLeft(0);
    public static final BigInteger F_BATTLEARMOR = BigInteger.valueOf(1).shiftLeft(1); // only used by BA squads
    public static final BigInteger F_PROTOMECH = BigInteger.valueOf(1).shiftLeft(2); // only used by ProtoMeks
    public static final BigInteger F_HOTLOAD = BigInteger.valueOf(1).shiftLeft(3); // Ammo can be hotloaded
    public static final BigInteger F_ENCUMBERING = BigInteger.valueOf(1).shiftLeft(4); // BA can't jump or make antimech until dumped
    public static final BigInteger F_MML_LRM = BigInteger.valueOf(1).shiftLeft(5); // LRM type
    public static final BigInteger F_AR10_WHITE_SHARK = BigInteger.valueOf(1).shiftLeft(6); // White shark type
    public static final BigInteger F_AR10_KILLER_WHALE = BigInteger.valueOf(1).shiftLeft(7); // Killer Whale type
    public static final BigInteger F_AR10_BARRACUDA = BigInteger.valueOf(1).shiftLeft(8); // barracuda type
    public static final BigInteger F_NUCLEAR = BigInteger.valueOf(1).shiftLeft(9); // Nuclear missile
    public static final BigInteger F_SANTA_ANNA = BigInteger.valueOf(1).shiftLeft(14); // Santa Anna Missile
    public static final BigInteger F_PEACEMAKER = BigInteger.valueOf(1).shiftLeft(15); // Peacemaker Missile
    public static final BigInteger F_TELE_MISSILE = BigInteger.valueOf(1).shiftLeft(10); // Tele-Missile
    public static final BigInteger F_CAP_MISSILE = BigInteger.valueOf(1).shiftLeft(11); // Other Capital-Missile
    public static final BigInteger F_SPACE_BOMB = BigInteger.valueOf(1).shiftLeft(12); // can be used to space bomb
    public static final BigInteger F_GROUND_BOMB = BigInteger.valueOf(1).shiftLeft(13); // can be used to ground bomb
    public static final BigInteger F_MML_SRM = BigInteger.valueOf(1).shiftLeft(14); // SRM type

    // Numbers 14-15 out of order. See nuclear missiles, above

    // For tag, rl pods, missiles and the like
    public static final BigInteger F_OTHER_BOMB = BigInteger.valueOf(1).shiftLeft(16);

    // Used by MHQ for loading ammo bins
    public static final BigInteger F_CRUISE_MISSILE = BigInteger.valueOf(1).shiftLeft(17);

    // Used by MHQ for loading ammo bins
    public static final BigInteger F_SCREEN = BigInteger.valueOf(1).shiftLeft(18);

    // ammo munitions, used for custom loadouts
    // N.B. we play bit-shifting games to allow "incendiary"
    // to be combined to other munition types.

    // M_STANDARD can be used for anything.
    public static final long M_STANDARD = 0;

    // AC Munition Types
    public static final long M_CLUSTER = 1L << 0;
    public static final long M_ARMOR_PIERCING = 1L << 1;
    public static final long M_FLECHETTE = 1L << 2;
    public static final long M_INCENDIARY_AC = 1L << 3;
    public static final long M_PRECISION = 1L << 4;
    public static final long M_TRACER = 1L << 5;
    public static final long M_FLAK = 1L << 6;
    public static final long M_CASELESS = 1L << 62;

    // ATM Munition Types
    public static final long M_EXTENDED_RANGE = 1L << 7;
    public static final long M_HIGH_EXPLOSIVE = 1L << 8;
    public static final long M_IATM_IMP = 1L << 57;
    public static final long M_IATM_IIW = 1L << 58;

    // LRM & SRM Munition Types
    public static final long M_FRAGMENTATION = 1L << 9;
    public static final long M_LISTEN_KILL = 1L << 10;
    public static final long M_ANTI_TSM = 1L << 11;
    public static final long M_NARC_CAPABLE = 1L << 12;
    public static final long M_ARTEMIS_CAPABLE = 1L << 13;
    public static final long M_DEAD_FIRE = 1L << 14;
    public static final long M_HEAT_SEEKING = 1L << 15;
    public static final long M_TANDEM_CHARGE = 1L << 16;
    public static final long M_ARTEMIS_V_CAPABLE = 1L << 17;
    public static final long M_SMOKE_WARHEAD = 1L << 18;
    // Mine Clearance munition type defined later, to maintain order

    // LRM Munition Types
    // Incendiary is special, though...
    // FIXME - I'm not implemented!!!
    public static final long M_INCENDIARY_LRM = 1L << 19;
    public static final long M_FLARE = 1L << 20;
    public static final long M_SEMIGUIDED = 1L << 21;
    public static final long M_SWARM = 1L << 22;
    public static final long M_SWARM_I = 1L << 23;
    public static final long M_THUNDER = 1L << 24;
    public static final long M_THUNDER_AUGMENTED = 1L << 25;
    public static final long M_THUNDER_INFERNO = 1L << 26;
    public static final long M_THUNDER_VIBRABOMB = 1L << 27;
    public static final long M_THUNDER_ACTIVE = 1L << 28;
    public static final long M_FOLLOW_THE_LEADER = 1L << 29;
    public static final long M_MULTI_PURPOSE = 1L << 30;
    // SRM Munition Types
    // TODO: Inferno should be available to fluid guns and vehicle flamers
    // TO page 362
    public static final long M_INFERNO = 1L << 31;
    public static final long M_AX_HEAD = 1L << 32;
    // HARPOON

    // SRM, MRM and LRM
    public static final long M_TORPEDO = 1L << 33;

    // iNarc Munition Types
    public static final long M_NARC_EX = 1L << 34;
    public static final long M_ECM = 1L << 35;
    public static final long M_HAYWIRE = 1L << 36;
    public static final long M_NEMESIS = 1L << 37;

    public static final long M_EXPLOSIVE = 1L << 38;

    // Arrow IV Munition Types
    public static final long M_HOMING = 1L << 39;
    public static final long M_FASCAM = 1L << 40;
    public static final long M_INFERNO_IV = 1L << 41;
    public static final long M_VIBRABOMB_IV = 1L << 42;
//  public static final long M_ACTIVE_IV   
    public static final long M_SMOKE = 1L << 43;
    public static final long M_LASER_INHIB = 1L << 44;

    // Nuclear Munitions
    public static final long M_DAVY_CROCKETT_M = 1L << 45;
//    public static final long M_SANTA_ANNA = 1L << 46;

    // fluid gun
    // TODO: implement all of these except coolant
    // water should also be used for vehicle flamers
    // TO page 361-363
    public static final long M_WATER = 1L << 48;
    public static final long M_PAINT_OBSCURANT = 1L << 49;
    public static final long M_OIL_SLICK = 1L << 50;
    public static final long M_ANTI_FLAME_FOAM = 1L << 51;
    public static final long M_CORROSIVE = 1L << 52;
    public static final long M_COOLANT = 1L << 53;

    // vehicular grenade launcher
    public static final long M_CHAFF = 1L << 54;
    public static final long M_INCENDIARY = 1L << 55;
    // Number 56 was M_SMOKEGRENADE, but that has now been merged with M_SMOKE

    // Number 57 is used for iATMs IMP ammo in the ATM section above.
    // and 58 for IIW

    // Mek mortar munitions
    public static final long M_AIRBURST = 1L << 59;
    public static final long M_ANTI_PERSONNEL = 1L << 60;
    // The rest were already defined
    // Flare
    // Semi-guided
    // Smoke

    // More SRM+LRM Munitions types
    public static final long M_MINE_CLEARANCE = 1L << 61;

    // note that 62 is in use above
    // this area is a primary target for the introduction of an enum or some other
    // kind of refactoring
    public static final long M_FAE = 1L << 63;

    // If you want to add another munition type, tough luck: longs can only be
    // bit-shifted 63 times.

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
    private String subMunitionName = "";

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
     * When comparing <code>AmmoType</code>s, look at the ammoType only.
     *
     * @param other the <code>Object</code> to compare to this one.
     * @return <code>true</code> if the other is an <code>AmmoType</code> object of
     *         the same <code>ammoType</code> as this object. N.B. different
     *         munition types are still equal.
     */
    public boolean equalsAmmoTypeOnly(Object other) {
        if (!(other instanceof AmmoType)) {
            return false;
        }

        AmmoType otherAmmoType = (AmmoType) other;

        // There a couple of flags that need to be checked before we check
        // on getAmmoType() strictly.
        if (is(T_MML)) {
            if (hasFlag(F_MML_LRM) != otherAmmoType.hasFlag(F_MML_LRM)) {
                return false;
            }
        }

        if (is(T_AR10)) {
            if (hasFlag(F_AR10_BARRACUDA) != otherAmmoType.hasFlag(F_AR10_BARRACUDA)) {
                return false;
            }
            if (hasFlag(F_AR10_WHITE_SHARK) != otherAmmoType.hasFlag(F_AR10_WHITE_SHARK)) {
                return false;
            }
            if (hasFlag(F_AR10_KILLER_WHALE) != otherAmmoType.hasFlag(F_AR10_KILLER_WHALE)) {
                return false;
            }
            if (hasFlag(F_NUCLEAR) != otherAmmoType.hasFlag(F_NUCLEAR)) {
                return false;
            }
        }

        return is(otherAmmoType.getAmmoType());
    }

    /**
     * Gets a value indicating whether this {@code AmmoType} is compatible
     * with another {@code AmmoType}.
     *
     * NB: this roughly means the same ammo type and munition type, but not rack
     * size.
     *
     * @param other The other {@code AmmoType} to determine compatibility with.
     */
    public boolean isCompatibleWith(AmmoType other) {
        if (other == null) {
            return false;
        }

        // If it isn't an allowed type, then nope!
        if (!ALLOWED_BY_TYPE.contains(getAmmoType()) || !ALLOWED_BY_TYPE.contains(other.getAmmoType())) {
            return false;
        }

        // MML Launchers, ugh.
        if ((is(T_MML) || other.is(T_MML)) && (getMunitionType() == other.getMunitionType())) {
            // LRMs...
            if (is(T_MML) && hasFlag(F_MML_LRM) && other.is(T_LRM)) {
                return true;
            } else if (other.is(T_MML) && other.hasFlag(AmmoType.F_MML_LRM) && is(T_LRM)) {
                return true;
            }

            // SRMs
            if (is(T_MML) && !hasFlag(AmmoType.F_MML_LRM) && is(T_SRM)) {
                return true;
            } else if (other.is(T_MML) && !other.hasFlag(AmmoType.F_MML_LRM) && is(T_SRM)) {
                return true;
            }
        }

        // General Launchers
        if (is(other.getAmmoType()) && (getMunitionType() == other.getMunitionType())) {
            return true;
        }

        return false;
    }

    public int getAmmoType() {
        return ammoType;
    }

    /**
     * Gets a value indicating whether this is a certain ammo type.
     *
     * @param ammoType The ammo type to compare against.
     */
    public boolean is(int ammoType) {
        return getAmmoType() == ammoType;
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

    /**
     * Used by units that are constructed using per-shot weights (BA and ProtoMeks). Some ammo is
     * defined in the rules rounded to a set number of decimal places.
     *
     * @return
     */
    public double getKgPerShot() {
        // kgPerShot is initialized to -1. Some ammo types are set by the rules to round
        // to a certain number of decimal places and can do that by setting the
        // kgPerShot field.
        // For those that are not set we calculate it.
        if (kgPerShot < 0) {
            return 1000.0 / shots;
        }
        return kgPerShot;
    }

    /**
     * Aerospace units cannot use specialty munitions except Artemis and LBX cluster
     * (but not standard). ATM ER and HE rounds are considered standard munitions.
     * AR10 missiles are designed for aerospace units and all munition types are
     * available.
     *
     * @return true if the munition can be used by aerospace units
     */
    public boolean canAeroUse() {
        switch (ammoType) {
            case T_AC_LBX:
            case T_SBGAUSS:
                return munitionType == M_CLUSTER;
            case T_ATM:
            case T_IATM:
                return (munitionType == M_STANDARD) || (munitionType == M_HIGH_EXPLOSIVE)
                        || (munitionType == M_EXTENDED_RANGE);
            case T_AR10:
                return true;
            default:
                return (munitionType == M_STANDARD) || (munitionType == M_ARTEMIS_CAPABLE)
                        || (munitionType == M_ARTEMIS_V_CAPABLE);
        }
    }

    /**
     * Aerospace units cannot use specialty munitions except Artemis and LBX cluster
     * (but not standard). ATM ER and HE rounds are considered standard munitions.
     * AR10 missiles are designed for aerospace units and all munition types are
     * available.
     *
     * @param option True if unofficial game option allowing alternate munitions for
     *               artillery bays is enabled
     *
     * @return true if the munition can be used by aerospace units
     */
    public boolean canAeroUse(boolean option) {
        if (option) {
            switch (ammoType) {
                case T_AC_LBX:
                case T_SBGAUSS:
                    return munitionType == M_CLUSTER;
                case T_ATM:
                case T_IATM:
                    return (munitionType == M_STANDARD) || (munitionType == M_HIGH_EXPLOSIVE)
                            || (munitionType == M_EXTENDED_RANGE);
                case T_AR10:
                    return true;
                case T_ARROW_IV:
                    return (munitionType == M_FLARE) || (munitionType == M_CLUSTER) || (munitionType == M_HOMING)
                            || (munitionType == M_INFERNO_IV) || (munitionType == M_LASER_INHIB)
                            || (munitionType == M_SMOKE) || (munitionType == M_FASCAM)
                            || (munitionType == M_DAVY_CROCKETT_M) || (munitionType == M_VIBRABOMB_IV)
                            || (munitionType == M_STANDARD);
                case T_LONG_TOM:
                    return (munitionType == M_FLARE) || (munitionType == M_CLUSTER) || (munitionType == M_HOMING)
                            || (munitionType == M_FLECHETTE) || (munitionType == M_SMOKE) || (munitionType == M_FASCAM)
                            || (munitionType == M_DAVY_CROCKETT_M) || (munitionType == M_STANDARD);
                case T_SNIPER:
                case T_THUMPER:
                    return (munitionType == M_FLARE) || (munitionType == M_CLUSTER) || (munitionType == M_HOMING)
                            || (munitionType == M_FLECHETTE) || (munitionType == M_SMOKE) || (munitionType == M_FASCAM)
                            || (munitionType == M_STANDARD);
                default:
                    return (munitionType == M_STANDARD) || (munitionType == M_ARTEMIS_CAPABLE)
                            || (munitionType == M_ARTEMIS_V_CAPABLE);
            }
        } else {
            return canAeroUse();
        }
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
        AmmoType at;
        for (int i = 0; i < vAmmo.size(); i++) {
            at = vAmmo.elementAt(i);
            if ((at.getRackSize() == wt.getRackSize()) && at.isLegal(mounted.getEntity().getTechLevelYear(),
                    mounted.getType().getTechLevel(mounted.getEntity().getTechLevelYear()),
                    mounted.getEntity().isMixedTech())) {
                return at;
            }
        }
        // found none, let's try again with techlevelyear 3071
        for (int i = 0; i < vAmmo.size(); i++) {
            at = vAmmo.elementAt(i);
            if ((at.getRackSize() == wt.getRackSize()) && (TechConstants.isLegal(mounted.getType().getTechLevel(3071),
                    at.getTechLevel(3071), false, mounted.getEntity().isMixedTech()))) {
                return at;
            }
        }
        return null; // couldn't find any ammo for this weapon type
    }

    public static void initializeTypes() {
        // Save copies of the SRM and LRM ammos to use to create munitions.
        ArrayList<AmmoType> srmAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanSrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> baSrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanBaLrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> isBaLrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> lrmAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> clanLrmAmmos = new ArrayList<>();
        ArrayList<AmmoType> enhancedlrmAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> acAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> arrowAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> protoarrowAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> clanArrowAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> thumperAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> thumperCannonAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> sniperAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> sniperCannonAmmos = new ArrayList<>(3);
        ArrayList<AmmoType> longTomAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> longTomCannonAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> mortarAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> clanMortarAmmos = new ArrayList<>(4);
        ArrayList<AmmoType> lrtAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> clanLrtAmmos = new ArrayList<>();
        ArrayList<AmmoType> srtAmmos = new ArrayList<>(26);
        ArrayList<AmmoType> clanSrtAmmos = new ArrayList<>();
        ArrayList<AmmoType> vglAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanVGLAmmos = new ArrayList<>();
        ArrayList<AmmoType> vehicleFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanVehicleFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> heavyFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanHeavyFlamerAmmos = new ArrayList<>();
        ArrayList<AmmoType> fluidGunAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanFluidGunAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanImprovedLrmsAmmo = new ArrayList<>();
        ArrayList<AmmoType> clanImprovedSrmsAmmo = new ArrayList<>();
        ArrayList<AmmoType> clanImprovedAcAmmo = new ArrayList<>();
        ArrayList<AmmoType> primLongTomAmmos = new ArrayList<>();
        ArrayList<AmmoType> clanProtoAcAmmo = new ArrayList<>();

        ArrayList<MunitionMutator> munitions = new ArrayList<>();

        AmmoType base;

        // all level 1 ammo
        base = AmmoType.createISVehicleFlamerAmmo();
        vehicleFlamerAmmos.add(base);
        clanVehicleFlamerAmmos.add(base);
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
        //Removed all references to Phoenix/Hawk/Streak MRM. Was ammo only with 
        //no weapon or code to support them.
        EquipmentType.addType(AmmoType.createISHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createISHeavyMGAmmoHalf());
        EquipmentType.addType(AmmoType.createISLightMGAmmo());
        EquipmentType.addType(AmmoType.createISLightMGAmmoHalf());
        EquipmentType.addType(AmmoType.createISSBGaussRifleAmmo());
        EquipmentType.addType(AmmoType.createISHVAC10Ammo());
        EquipmentType.addType(AmmoType.createISHVAC5Ammo());
        EquipmentType.addType(AmmoType.createISHVAC2Ammo());
        EquipmentType.addType(AmmoType.createISMekTaserAmmo());
        EquipmentType.addType(AmmoType.createISAC2pAmmo());
        EquipmentType.addType(AmmoType.createISAC5pAmmo());
        EquipmentType.addType(AmmoType.createISAC10pAmmo());
        EquipmentType.addType(AmmoType.createISAC20pAmmo());

        // IO Equipment
        EquipmentType.addType(AmmoType.createCLImprovedGaussAmmo());

        // Clan Improved
        base = AmmoType.createCLImprovedAC2Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedAC5Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedAC10Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedAC20Ammo();
        clanImprovedAcAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLPROAC2Ammo();
        clanProtoAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLPROAC4Ammo();
        clanProtoAcAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLPROAC8Ammo();
        clanProtoAcAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLImprovedLRM5Ammo();
        clanImprovedLrmsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedLRM10Ammo();
        clanImprovedLrmsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedLRM15Ammo();
        clanImprovedLrmsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedLRM20Ammo();
        clanImprovedLrmsAmmo.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createCLImprovedSRM2Ammo();
        clanImprovedSrmsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedSRM4Ammo();
        clanImprovedSrmsAmmo.add(base);
        EquipmentType.addType(base);
        base = AmmoType.createCLImprovedSRM6Ammo();
        clanImprovedSrmsAmmo.add(base);
        EquipmentType.addType(base);

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

        base = AmmoType.createLongTomAmmo();
        longTomAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISPrimitiveLongTomAmmo();
        primLongTomAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISLongTomCannonAmmo();
        longTomCannonAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createSniperAmmo();
        sniperAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISSniperCannonAmmo();
        sniperCannonAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createThumperAmmo();
        thumperAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISThumperCannonAmmo();
        thumperCannonAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createISArrowIVAmmo();
        arrowAmmos.add(base);
        EquipmentType.addType(base);

        base = AmmoType.createPrototypeArrowIVAmmo();
        protoarrowAmmos.add(base);
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
        EquipmentType.addType(AmmoType.createCLMGAmmo());
        EquipmentType.addType(AmmoType.createCLMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createCLHeavyMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLLightMGAmmo());
        EquipmentType.addType(AmmoType.createCLLightMGAmmoHalf());
        EquipmentType.addType(AmmoType.createCLAMSAmmo());
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

        // ProtoMek-specific ammo
        EquipmentType.addType(AmmoType.createCLPROHeavyMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROMGAmmo());
        EquipmentType.addType(AmmoType.createCLPROLightMGAmmo());

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
        EquipmentType.addType(AmmoType.createKrakenAmmo());
        EquipmentType.addType(AmmoType.createKillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createPeacemakerAmmo());
        EquipmentType.addType(AmmoType.createWhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createSantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createBarracudaAmmo());
        EquipmentType.addType(AmmoType.createKillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createWhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createBarracudaTAmmo());
        EquipmentType.addType(AmmoType.createAR10KillerWhaleAmmo());
        EquipmentType.addType(AmmoType.createAR10PeacemakerAmmo());
        EquipmentType.addType(AmmoType.createAR10WhiteSharkAmmo());
        EquipmentType.addType(AmmoType.createAR10SantaAnnaAmmo());
        EquipmentType.addType(AmmoType.createAR10BarracudaAmmo());
        EquipmentType.addType(AmmoType.createAR10KillerWhaleTAmmo());
        EquipmentType.addType(AmmoType.createAR10WhiteSharkTAmmo());
        EquipmentType.addType(AmmoType.createAR10BarracudaTAmmo());
        EquipmentType.addType(AmmoType.createScreenLauncherAmmo());
        EquipmentType.addType(AmmoType.createAlamoAmmo());
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
        EquipmentType.addType(AmmoType.createInfantryAmmo());
        EquipmentType.addType(AmmoType.createInfantryInfernoAmmo());

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
        munitions.add(new MunitionMutator("Airburst", 1, M_AIRBURST,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_B, RATING_D, RATING_C, RATING_D)
                        .setISAdvancement(2540, 2544, DATE_NONE, 2819, 3043)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Anti-personnel", 1, M_ANTI_PERSONNEL,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setISAdvancement(2526, 2531, 3052, 2819, 3043)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        // Armor Piercing is the base ammo type see further down.

        munitions.add(new MunitionMutator("Flare", 1, M_FLARE, new TechAdvancement(TECH_BASE_IS).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_B).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(2533, 2536, DATE_NONE, 2819, 3043).setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), "374, TO"));

        munitions.add(new MunitionMutator("Semi-Guided", 1, M_SEMIGUIDED,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setISAdvancement(3055, 3064, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                        .setProductionFactions(F_FW).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "374, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                        .setISAdvancement(2526, 2531, DATE_NONE, 2819, 3043)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "375, TO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(mortarAmmos, munitions);

        // Create the munition types for Clan Mek mortars
        munitions.clear();
        munitions.add(new MunitionMutator("Airburst", 1, M_AIRBURST,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_B, RATING_D, RATING_C, RATING_D)
                        .setClanAdvancement(2540, 2544, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Anti-personnel", 1, M_ANTI_PERSONNEL,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setClanAdvancement(2540, 2544, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        // Armor Piercing is the base ammo type see further down.

        munitions.add(new MunitionMutator("Flare", 1, M_FLARE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                        .setClanAdvancement(2533, 2536, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "374, TO"));

        munitions.add(new MunitionMutator("Semi-Guided", 1, M_SEMIGUIDED,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setClanAdvancement(3055, 3064, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "374, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                        .setClanAdvancement(2526, 2531, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "375, TO"));

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

        munitions.add(new MunitionMutator("Acid", 2, M_AX_HEAD,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F).setISAdvancement(3053)
                        .setPrototypeFactions(F_FS, F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "367, TO"));

        /*
         * munitions.add(new MunitionMutator("Harpoon", 2, M_HARPOON, new
         * TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).
         * setTechRating(RATING_C) .setAvailability(RATING_C, RATING_C, RATING_C,
         * RATING_C) .setISAdvancement(2395, 2400, 2415, DATE_NONE, DATE_NONE)
         * .setISApproximate(true, false, false, false,
         * false).setPrototypeFactions(F_LC) .setProductionFactions(F_LC), "369, TO"));
         */

        munitions.add(new MunitionMutator("Heat-Seeking", 2, M_HEAT_SEEKING,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_E, RATING_E, RATING_F)
                        .setISAdvancement(2365, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "369, TO"));

        munitions.add(new MunitionMutator("Inferno", 1, M_INFERNO,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setISAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "231, TM"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setISAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        munitions.add(new MunitionMutator("Tandem-Charge", 2, M_TANDEM_CHARGE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(2757, 3062, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_FS).setReintroductionFactions(F_FS)
                        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "372, TO"));

        // TODO Tear Gas See IO pg 372

        // TODO Retro-Streak IO pg 132

        munitions.add(new MunitionMutator("Anti-TSM", 1, M_ANTI_TSM,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                        .setISAdvancement(3026, 3027, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "104, IO"));

        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setISAdvancement(2592, 2598, 3045, 2855, 3035)
                        .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("Dead-Fire", 1, M_DEAD_FIRE,
                new TechAdvancement(TECH_BASE_IS).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E).setISAdvancement(3052)
                        .setPrototypeFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "131, IO"));

        munitions.add(new MunitionMutator("Fragmentation", 1, M_FRAGMENTATION,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(2375, 2377, 3058, 2790, 3054)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("Listen-Kill", 1, M_LISTEN_KILL,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
                        .setISAdvancement(3037, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "105, IO"));

        // TODO Mag Pulse see IO pg 62

        munitions.add(new MunitionMutator("Mine Clearance", 1, M_MINE_CLEARANCE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setISAdvancement(3065, 3069, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "370, TO"));

        munitions.add(new MunitionMutator("Narc-capable", 1, M_NARC_CAPABLE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setISAdvancement(2520, 2587, 3049, 2795, 3035)
                        .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "142, TW"));

        // TODO Anti-Radiation Missiles (see IO pg 62)

        // TODO: Harpoon SRMs (TO 369), Tear Gas SRMs (TO 371), RETRO-STREAK (IO 193)

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(srmAmmos, munitions);
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for Clan SRM launchers.
        munitions.clear();

        munitions.add(new MunitionMutator("(Clan) Acid", 2, M_AX_HEAD,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F).setClanAdvancement(3053)
                        .setPrototypeFactions(F_FS, F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "367, TO"));

        /*
         * munitions.add(new MunitionMutator("Harpoon", 2, M_HARPOON, new
         * TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).
         * setTechRating(RATING_C) .setAvailability(RATING_C, RATING_C, RATING_C,
         * RATING_C) .setClanAdvancement(2395, 2400, 2415, DATE_NONE, DATE_NONE)
         * .setClanApproximate(true, false, false, false,
         * false).setPrototypeFactions(F_LC) .setProductionFactions(F_LC), "369, TO"));
         */

        munitions.add(new MunitionMutator("(Clan) Heat-Seeking", 2, M_HEAT_SEEKING,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_E, RATING_E, RATING_F)
                        .setClanAdvancement(2365, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "369, TO"));

        munitions.add(new MunitionMutator("(Clan) Inferno", 1, M_INFERNO,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setClanAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "231, TM"));

        munitions.add(new MunitionMutator("(Clan) Smoke", 1, M_SMOKE_WARHEAD,

                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setClanAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        munitions.add(new MunitionMutator("(Clan) Tandem-Charge", 2, M_TANDEM_CHARGE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(2757, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "372, TO"));

        // TODO Tear Gas See IO pg 372

        munitions.add(new MunitionMutator("(Clan) Anti-TSM", 1, M_ANTI_TSM,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                        .setClanAdvancement(3026, 3027, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "104, IO"));

        munitions.add(new MunitionMutator("(Clan) Artemis-capable", 1, M_ARTEMIS_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 2818, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSA)
                        .setProductionFactions(F_CSA).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "207, TM"));

        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                        .setClanAdvancement(DATE_NONE, 3061, 3085, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CGS)
                        .setProductionFactions(F_CSF, F_RD).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "283, TO"));

        munitions.add(new MunitionMutator("(Clan) Dead-Fire", 1, M_DEAD_FIRE,
                new TechAdvancement(TECH_BASE_CLAN).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E).setClanAdvancement(3052)
                        .setPrototypeFactions(F_DC).setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
                "131, IO"));

        munitions.add(new MunitionMutator("(Clan) Fragmentation", 1, M_FRAGMENTATION,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setClanAdvancement(2375, 2377, 3058, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("(Clan) Listen-Kill", 1, M_LISTEN_KILL,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                        .setClanAdvancement(3037, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
                "230, TM"));

        // TODO Mag Pulse See IO pg 62

        munitions.add(new MunitionMutator("(Clan) Mine Clearance", 1, M_MINE_CLEARANCE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setClanAdvancement(3065, 3069, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "370, TO"));

        munitions.add(new MunitionMutator("(Clan) Narc-capable", 1, M_NARC_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 2828, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "370, TO"));

        // TODO Anti-Radiation Missiles See IO pg 62 (TO 368)

        // TODO: Harpoon SRMs (TO 369), Tear Gas SRMs (TO 371), RETRO-STREAK (IO 193)

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanSrmAmmos, munitions);
        AmmoType.createMunitions(clanImprovedSrmsAmmo, munitions);
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for CLAN BA SRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Torpedo", 1, M_TORPEDO,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 2828, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("(Clan) Multi-Purpose", 1, M_MULTI_PURPOSE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                        .setProductionFactions(F_CGS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "229, TW"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for IS BA LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Torpedo", 1, M_TORPEDO,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setISAdvancement(DATE_NONE, DATE_NONE, 3052, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(isBaLrmAmmos, munitions);
        AmmoType.createMunitions(baSrmAmmos, munitions);

        // Create the munition types for clan BA LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Multi-Purpose", 1, M_MULTI_PURPOSE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                        .setProductionFactions(F_CGS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "229, TW"));

        munitions.add(new MunitionMutator("Torpedo", 1, M_TORPEDO,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 2828, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanBaLrmAmmos, munitions);

        // Create the munition types for IS LRM launchers.
        munitions.clear();

        // TODO Flare LRMs IO pg 230

        munitions.add(new MunitionMutator("Follow The Leader", 2, M_FOLLOW_THE_LEADER,
                new TechAdvancement(TECH_BASE_IS).setTechRating(RATING_E)
                        .setAvailability(RATING_F, RATING_X, RATING_E, RATING_X)
                        .setISAdvancement(2750, DATE_NONE, DATE_NONE, 2770, 3046)
                        .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                        .setReintroductionFactions(F_FS, F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "368, TO"));

        munitions.add(new MunitionMutator("Heat-Seeking", 2, M_HEAT_SEEKING,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_E, RATING_E, RATING_F)
                        .setISAdvancement(2365, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "369, TO"));

        // TODO Incendiary LRMs - IO pg 61, TO pg 369

        /*
         * munitions.add(new MunitionMutator("Incendiary", 2, M_INCENDIARY_LRM, new
         * TechAdvancement(TECH_BASE_IS) .setIntroLevel(false) .setUnofficial(false)
         * .setTechRating(RATING_C) .setAvailability(RATING_E, RATING_E, RATING_E,
         * RATING_E) .setClanAdvancement(2341, 2342, 2352, DATE_NONE, DATE_NONE)
         * .setClanApproximate(false, false, false, false, false)
         * .setPrototypeFactions(F_TH) .setProductionFactions(F_TH),"369, TO"));
         */

        munitions.add(new MunitionMutator("Semi-guided", 1, M_SEMIGUIDED,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                        .setISAdvancement(3053, 3057, 3065, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                        .setProductionFactions(F_FW).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "231, TM"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE_WARHEAD,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setISAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        //Note of Swarms the intro dates in IntOps are off and it allows Swarm-I to appear before Swarm during the 
        //Clan Invasion. Proposed errata makes 3052 for Swarm-I a hard date, and 3053 for Swarm re-iintroduction a flexible date.
        munitions.add(new MunitionMutator("Swarm", 1, M_SWARM, new TechAdvancement(TECH_BASE_IS).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_E).setAvailability(RATING_E, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(2615, 2621, 3058, 2833, 3053).setISApproximate(true, false, false, false, true)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        munitions.add(new MunitionMutator("Swarm-I", 1, M_SWARM_I,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_X, RATING_D, RATING_D)
                        .setISAdvancement(3052, 3057, 3066, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FW)
                        .setProductionFactions(F_FW).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        munitions.add(new MunitionMutator("Thunder", 1, M_THUNDER,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_D, RATING_X, RATING_D, RATING_D)
                        .setISAdvancement(2618, 2620, 2650, 2840, 3052)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_LC, F_FS)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Thunder-Active", 2, M_THUNDER_ACTIVE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3054, 3058, 3064, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Thunder-Augmented", 2, M_THUNDER_AUGMENTED,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3054, 3057, 3064, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Thunder-Vibrabomb", 2, M_THUNDER_VIBRABOMB,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3054, 3056, 3064, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Thunder-Inferno", 2, M_THUNDER_INFERNO,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3054, 3056, 3062, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("Anti-TSM", 1, M_ANTI_TSM,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                        .setISAdvancement(3026, 3027, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "104, IO"));

        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setISAdvancement(2592, 2598, 3045, 2855, 3035)
                        .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("Dead-Fire", 1, M_DEAD_FIRE,
                new TechAdvancement(TECH_BASE_IS).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E).setISAdvancement(3052)
                        .setPrototypeFactions(F_DC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "131, IO"));

        munitions.add(new MunitionMutator("Fragmentation", 1, M_FRAGMENTATION,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(2375, 2377, 3058, 2790, 3054)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setReintroductionFactions(F_FW)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("Listen-Kill", 1, M_LISTEN_KILL,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
                        .setISAdvancement(3037, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "105, IO"));

        // TODO Mag Pulse see IO pg 62

        munitions.add(new MunitionMutator("Mine Clearance", 1, M_MINE_CLEARANCE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setISAdvancement(3065, 3069, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "370, TO"));

        munitions.add(new MunitionMutator("Narc-capable", 1, M_NARC_CAPABLE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setISAdvancement(2520, 2587, 3049, 2795, 3035)
                        .setISApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "142, TW"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(lrmAmmos, munitions);
        AmmoType.createMunitions(enhancedlrmAmmos, munitions);
        AmmoType.createMunitions(isBaLrmAmmos, munitions);

        // Create the munition types for Clan LRM launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Follow The Leader", 2, M_FOLLOW_THE_LEADER,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_F, RATING_X, RATING_E, RATING_X)
                        .setClanAdvancement(2750, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, true, false).setPrototypeFactions(F_TH)
                        .setReintroductionFactions(F_FS, F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "368, TO"));

        munitions.add(new MunitionMutator("(Clan) Heat-Seeking", 2, M_HEAT_SEEKING,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_E, RATING_E, RATING_F)
                        .setClanAdvancement(2365, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "369, TO"));

        // TODO Incendiary LRMs - IO pg 61, TO pg 369

        /*
         * munitions.add(new MunitionMutator("(Clan) Incendiary", 2, M_INCENDIARY_LRM,
         * new TechAdvancement(TECH_BASE_CLAN) .setIntroLevel(false)
         * .setUnofficial(false) .setTechRating(RATING_C) .setAvailability(RATING_E,
         * RATING_E, RATING_E, RATING_E) .setClanAdvancement(2341, 2342, 2352,
         * DATE_NONE, DATE_NONE) .setClanApproximate(false, false, false, false, false)
         * .setPrototypeFactions(F_TH) .setProductionFactions(F_TH),"369, TO"));
         */

        munitions.add(new MunitionMutator("(Clan) Semi-guided", 1, M_SEMIGUIDED,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                        .setClanAdvancement(3053, 3057, 3065, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                        .setProductionFactions(F_FW).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "231, TM"));

        munitions.add(new MunitionMutator("(Clan) Smoke", 1, M_SMOKE_WARHEAD,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setClanAdvancement(2333, 2370, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "371, TO"));

        munitions.add(new MunitionMutator("(Clan) Swarm", 1, M_SWARM,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_X, RATING_D, RATING_D)
                        .setClanAdvancement(2615, 2621, 3058, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        munitions.add(new MunitionMutator("(Clan) Swarm-I", 1, M_SWARM_I,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_X, RATING_D, RATING_D)
                        .setClanAdvancement(3052, 3057, 3066, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                        .setProductionFactions(F_FW).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "371, TO"));

        munitions.add(new MunitionMutator("(Clan) Thunder", 1, M_THUNDER,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                        .setClanAdvancement(2618, 2620, 2650, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_LC, F_FS)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("(Clan) Thunder-Active", 2, M_THUNDER_ACTIVE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3054, 3058, 3064, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("(Clan) Thunder-Augmented", 2, M_THUNDER_AUGMENTED,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3054, 3057, 3064, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("(Clan) Thunder-Vibrabomb", 2, M_THUNDER_VIBRABOMB,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3054, 3056, 3064, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("(Clan) Thunder-Inferno", 2, M_THUNDER_INFERNO,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3054, 3056, 3062, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "373, TO"));

        munitions.add(new MunitionMutator("(Clan) Anti-TSM", 1, M_ANTI_TSM,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                        .setClanAdvancement(3026, 3027, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "104, IO"));

        munitions.add(new MunitionMutator("(Clan) Artemis-capable", 1, M_ARTEMIS_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(2592, 2598, 3045, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "207, TM"));

        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                        .setClanAdvancement(DATE_NONE, 3061, 3085, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CGS)
                        .setProductionFactions(F_CSF, F_RD).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "283, TO"));

        munitions.add(new MunitionMutator("(Clan) Dead-Fire", 1, M_DEAD_FIRE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3052, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "131, IO"));

        munitions.add(new MunitionMutator("(Clan) Fragmentation", 1, M_FRAGMENTATION,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setClanAdvancement(2375, 2377, 3058, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        munitions.add(new MunitionMutator("(Clan) Listen-Kill", 1, M_LISTEN_KILL,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                        .setClanAdvancement(3037, DATE_NONE, DATE_NONE, 3040, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
                "230, TM"));

        // TODO Mag Pulse see IO pg 62

        munitions.add(new MunitionMutator("(Clan) Mine Clearance", 1, M_MINE_CLEARANCE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setClanAdvancement(3065, 3069, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "370, TO"));

        munitions.add(new MunitionMutator("(Clan) Multi-Purpose", 1, M_MULTI_PURPOSE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "229, TW"));

        munitions.add(new MunitionMutator("(Clan) Narc-capable", 1, M_NARC_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(2520, 2587, 3049, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "142, TW"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanLrmAmmos, munitions);
        AmmoType.createMunitions(clanBaLrmAmmos, munitions);
        AmmoType.createMunitions(clanImprovedLrmsAmmo, munitions);

        // Create the munition types for AC rounds.
        munitions.clear();
        munitions.add(new MunitionMutator("Armor-Piercing", 2, M_ARMOR_PIERCING,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3055, 3059, 3063, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Caseless", 1, M_CASELESS,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setISAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "352, TO"));

        munitions.add(new MunitionMutator("Flak", 1, M_FLAK,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                        .setAdvancement(DATE_ES, 2310, 3070, DATE_NONE, DATE_NONE)
                        .setApproximate(false, false, true, false, false).setProductionFactions(F_TA)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "352, TO"));

        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3053, 3055, 3058, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Precision", 2, M_PRECISION,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3058, 3062, 3066, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Tracer", 1, M_TRACER,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_D, RATING_E, RATING_F, RATING_E)
                        .setISAdvancement(DATE_ES, 2300, 3060, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, true, false, false, false).setProductionFactions(F_TA)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "353, TO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(acAmmos, munitions);

        // Create the munition types for Clan Improved AC rounds. Since Improved AC go
        // extinct the ammo will as well.
        munitions.clear();
        munitions.add(new MunitionMutator("Armor-Piercing", 2, M_ARMOR_PIERCING,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CLAN)
                        .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Caseless", 1, M_CASELESS,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setISAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "352, TO"));

        munitions.add(new MunitionMutator("Flak", 1, M_FLAK,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                        .setAdvancement(DATE_ES, 2310, 3070, DATE_NONE, DATE_NONE)
                        .setApproximate(false, false, true, false, false).setProductionFactions(F_TA)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "352, TO"));


        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 3105, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CLAN)
                        .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Precision", 2, M_PRECISION,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3053, 3055, 3058, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
                "208, TM"));

        munitions.add(new MunitionMutator("Tracer", 1, M_TRACER,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_D, RATING_E, RATING_F, RATING_E)
                        .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                        .setClanApproximate(false, true, false, true, false).setPrototypeFactions(F_CLAN)
                        .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "353, TO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanImprovedAcAmmo, munitions);

        // Create the munition types for Clan Protomek AC rounds. Ammo Tech Ratings
        // based off the weapon itself
        munitions.clear();
        munitions.add(new MunitionMutator("Armor-Piercing", 2, M_ARMOR_PIERCING,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                        .setClanAdvancement(DATE_NONE, 3095, 3105, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, true, false, false, false).setProductionFactions(F_CJF)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        munitions.add(new MunitionMutator("Caseless", 1, M_CASELESS,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                        .setISAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(DATE_NONE, DATE_NONE, 3109, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "352, TO"));

        munitions.add(new MunitionMutator("Flak", 1, M_FLAK,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_E, RATING_F, RATING_F, RATING_E)
                        .setAdvancement(DATE_ES, 2310, 3070, DATE_NONE, DATE_NONE)
                        .setApproximate(false, false, true, false, false).setProductionFactions(F_TA)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "352, TO"));

        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                        .setClanAdvancement(DATE_NONE, 3095, 3105, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, true, false, false, false).setProductionFactions(F_CHH)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Precision", 2, M_PRECISION,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_F)
                        .setClanAdvancement(3070, 3073, 3145, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CBS)
                        .setProductionFactions(F_CBS).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "208, TM"));

        munitions.add(new MunitionMutator("Tracer", 1, M_TRACER,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                        .setClanAdvancement(3070, 3073, 3145, DATE_NONE, DATE_NONE)
                        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBS)
                        .setProductionFactions(F_CBS).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "353, TO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanProtoAcAmmo, munitions);

        // Create the munition types for IS Arrow IV launchers.
        munitions.clear();
        // TODO
        /*
         * munitions.add(new MunitionMutator("Air-Defense Arrow (ADA) Missiles", 1,
         * M_XXXXXXX, new
         * TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false)
         * .setISAdvancement(3068, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
         * .setApproximate(false, false, false, false, false).setTechRating(RATING_E)
         * .setAvailability(RATING_X, RATING_X, RATING_F,
         * RATING_E).setPrototypeFactions(F_CC)
         * .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED)
         * , "353, TO"));
         */

        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(2594, 2600, DATE_NONE, 2830, 3047)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "354, TO"));

        munitions.add(new MunitionMutator("Homing", 1, M_HOMING, new TechAdvancement(TECH_BASE_IS).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3045).setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED), "354, TO"));

        munitions.add(new MunitionMutator("Illumination", 1, M_FLARE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setISAdvancement(2615, 2621, DATE_NONE, 2800, 3047)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Inferno-IV", 1, M_INFERNO_IV,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                        .setISAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Laser Inhibiting", 1, M_LASER_INHIB,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                        .setISAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS, F_LC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, new TechAdvancement(TECH_BASE_IS).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2595, 2600, DATE_NONE, 2840, 3044).setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "356, TO"));

        munitions.add(new MunitionMutator("Thunder (FASCAM)", 1, M_FASCAM,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                        .setISAdvancement(2621, 2844, DATE_NONE, 2770, 3051)
                        .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_CHH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "356, TO"));

        // TODO - Implement them.
        /*
         * munitions.add(new MunitionMutator("Thunder-Active-IV", 1, M_ACTIVE_IV, new
         * TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).
         * setTechRating(RATING_D) .setAvailability(RATING_X, RATING_X, RATING_E,
         * RATING_E) .setISAdvancement(3056, 3065, DATE_NONE, DATE_NONE, DATE_NONE)
         * .setApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
         * .setProductionFactions(F_CCC), "356, TO"));
         */

        munitions.add(new MunitionMutator("Thunder Vibrabomb-IV", 1, M_VIBRABOMB_IV,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setISAdvancement(3056, 3065, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "357, TO"));

        munitions.add(new MunitionMutator("Davy Crocket-M", 5, M_DAVY_CROCKETT_M,
                new TechAdvancement(TECH_BASE_IS).setTechRating(RATING_D)
                        .setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                        .setISAdvancement(2412, DATE_NONE, DATE_NONE, 2830, 3044)
                        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "174, IO"));

        munitions.add(new MunitionMutator("Fuel-Air", 1, M_FAE, new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "165, IO"));

        // TODO:
        /*
         * Arrow IV [Air-Defense Arrow (ADA) Missiles] - (TO 353), Arrow IV [Thunder
         * Active-IV] - TO (357)
         */

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(arrowAmmos, munitions);
        AmmoType.createMunitions(protoarrowAmmos, munitions);

        // Create the munition types for Clan Arrow IV launchers.
        munitions.clear();
        // TODO
        /*
         * munitions.add(new MunitionMutator("Air-Defense Arrow (ADA) Missiles", 1,
         * M_XXXXXXX, new
         * TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false)
         * .setISAdvancement(3068, 3080, DATE_NONE, DATE_NONE, DATE_NONE)
         * .setApproximate(false, false, false, false, false).setTechRating(RATING_E)
         * .setAvailability(RATING_X, RATING_X, RATING_F,
         * RATING_E).setPrototypeFactions(F_CC) .setProductionFactions(F_CC),
         * "353, TO"));
         */

        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setClanAdvancement(2594, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "354, TO"));

        munitions.add(new MunitionMutator("Homing", 1, M_HOMING,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setClanAdvancement(2593, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "354, TO"));

        munitions.add(new MunitionMutator("Illumination", 1, M_FLARE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setClanAdvancement(2615, 2621, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Inferno-IV", 1, M_INFERNO_IV,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                        .setClanAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Laser Inhibiting", 1, M_LASER_INHIB,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                        .setClanAdvancement(3053, 3083, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                        .setProductionFactions(F_FS, F_LC).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "355, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setClanAdvancement(2595, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "356, TO"));

        munitions.add(new MunitionMutator("Thunder (FASCAM)", 1, M_FASCAM,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                        .setClanAdvancement(2621, 2844, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_CHH).setReintroductionFactions(F_CC)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "356, TO"));

        // TODO - Implement them.
        /*
         * munitions.add(new MunitionMutator("Thunder-Active-IV", 1, M_ACTIVE_IV, new
         * TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).
         * setTechRating(RATING_D) .setAvailability(RATING_X, RATING_X, RATING_E,
         * RATING_E) .setISAdvancement(3056, 3065, DATE_NONE, DATE_NONE, DATE_NONE)
         * .setApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
         * .setProductionFactions(F_CCC), "356, TO"));
         */

        munitions.add(new MunitionMutator("Thunder Vibrabomb-IV", 1, M_VIBRABOMB_IV,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_D)
                        .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                        .setClanAdvancement(3056, 3065, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CC)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "357, TO"));

        // TODO:
        // Fuel-Air Mutators (See IO 165)

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanArrowAmmos, munitions);

        // create the munition types for clan vehicular grenade launchers
        munitions.clear();
        munitions.add(new MunitionMutator("Chaff", 1, M_CHAFF,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_D, RATING_E, RATING_E, RATING_E)
                        .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, true, false, false)
                        .setPrototypeFactions(F_CLAN)
                        .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.STANDARD), "363, TO"));

        munitions.add(new MunitionMutator("Incendiary", 1, M_INCENDIARY,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, true, false, false)
                        .setPrototypeFactions(F_CLAN)
                        .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.STANDARD), "364, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setClanAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN).setStaticTechLevel(SimpleTechLevel.STANDARD), "364, TO"));

        AmmoType.createMunitions(clanVGLAmmos, munitions);

        // create the munition types for IS vehicular grenade launchers
        munitions.clear();
        munitions.add(new MunitionMutator("Chaff", 1, M_CHAFF, new TechAdvancement(TECH_BASE_IS).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_B).setAvailability(RATING_X, RATING_E, RATING_E, RATING_E)
                .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), "363, TO"));

        munitions.add(new MunitionMutator("Incendiary", 1, M_INCENDIARY,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, true, false, false)
                        .setStaticTechLevel(SimpleTechLevel.STANDARD), "363, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, new TechAdvancement(TECH_BASE_IS).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_NONE, DATE_PS, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setStaticTechLevel(SimpleTechLevel.STANDARD), "363, TO"));

        AmmoType.createMunitions(vglAmmos, munitions);

        // Create the munition types for Artillery launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER,
                new TechAdvancement(TECH_BASE_ALL).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "354, TO"));

        munitions.add(new MunitionMutator("Copperhead", 1, M_HOMING,
                new TechAdvancement(TECH_BASE_ALL).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(2640, 2645, DATE_NONE, 2800, 3051).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "354, TO"));

        munitions.add(new MunitionMutator("FASCAM", 1, M_FASCAM,
                new TechAdvancement(TECH_BASE_ALL).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                        .setISAdvancement(2621, 2844, DATE_NONE, 2770, 3051).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_CC).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE,
                new TechAdvancement(TECH_BASE_ALL).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                        .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Illumination", 1, M_FLARE,
                new TechAdvancement(TECH_BASE_ALL).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE,
                new TechAdvancement(TECH_BASE_ALL).setTechRating(RATING_B)
                        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                        .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "355, TO"));

        munitions.add(new MunitionMutator("Fuel-Air", 1, M_FAE, new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "165, IO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(sniperAmmos, munitions);
        AmmoType.createMunitions(thumperAmmos, munitions);

        // Make Davy Crockett-Ms for Long Toms, but not Thumper or Sniper.
        munitions.add(new MunitionMutator("Davy Crocket-M", 5, M_DAVY_CROCKETT_M,
                new TechAdvancement(TECH_BASE_IS).setTechRating(RATING_D)
                        .setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                        .setISAdvancement(2412, DATE_NONE, DATE_NONE, 2830, 3044)
                        .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL),
                "174, IO"));
        AmmoType.createMunitions(longTomAmmos, munitions);

        // Create the munition types for Artillery Cannons.
        // These were taken out in TacOps errata, so are unofficial.
        munitions.clear();
        munitions.add(new MunitionMutator("Cluster", 1, M_CLUSTER,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false),
                "354, TO"));

        munitions.add(new MunitionMutator("Copperhead", 1, M_HOMING,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                        .setISAdvancement(2640, 2645, DATE_NONE, 2800, 3051)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(2640, 2645, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH),
                "354, TO"));

        munitions.add(new MunitionMutator("FASCAM", 1, M_FASCAM, new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false)
                .setUnofficial(true).setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(2621, 2844, DATE_NONE, 2770, 3051).setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2621, 2844, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_CHH), "355, TO"));

        munitions.add(new MunitionMutator("Flechette", 1, M_FLECHETTE,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                        .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false),
                "355, TO"));

        munitions.add(new MunitionMutator("Illumination", 1, M_FLARE,
                new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_C)
                        .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                        .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false),
                "355, TO"));

        munitions.add(new MunitionMutator("Smoke", 1, M_SMOKE, new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false)
                .setUnofficial(true).setTechRating(RATING_B).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "356, TO"));

        munitions.add(new MunitionMutator("Fuel-Air", 1, M_FAE, new TechAdvancement(TECH_BASE_ALL).setIntroLevel(false)
                .setUnofficial(false).setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "165, IO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(sniperCannonAmmos, munitions);
        AmmoType.createMunitions(thumperCannonAmmos, munitions);

        // Make Davy Crockett-Ms for Long Toms, but not Thumper or Sniper.
        munitions.add(new MunitionMutator("Davy Crocket-M", 5, M_DAVY_CROCKETT_M,
                new TechAdvancement(TECH_BASE_IS).setTechRating(RATING_D)
                        .setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                        .setISAdvancement(2412, DATE_NONE, DATE_NONE, 2830, 3044)
                        .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL),
                "174, IO"));
        AmmoType.createMunitions(longTomCannonAmmos, munitions);

        // Create the munition types for SRT launchers.
        munitions.clear();
        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setISAdvancement(2592, 2598, 3045, 2855, 3035)
                        .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(srtAmmos, munitions);
        AmmoType.createMunitions(lrtAmmos, munitions);

        // Create the munition types for Clan SRT launchers.
        munitions.clear();
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        munitions.add(new MunitionMutator("(Clan) Artemis V-capable", 1, M_ARTEMIS_V_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                        .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                        .setClanAdvancement(DATE_NONE, 3061, 3085, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_CGS)
                        .setProductionFactions(F_CSF, F_RD).setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "283, TO"));
        munitions.add(new MunitionMutator("Artemis-capable", 1, M_ARTEMIS_CAPABLE,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                        .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                        .setClanAdvancement(2592, 2598, 3045, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                        .setProductionFactions(F_TH).setStaticTechLevel(SimpleTechLevel.STANDARD),
                "230, TM"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanSrtAmmos, munitions);
        AmmoType.createMunitions(clanLrtAmmos, munitions);

        // TODO : Need Corrosive, Flame-Retardant, Oil Slick, Paint and Water Ammo's for
        // all Fluid Guns/Sprayers
        // Create the munition types for vehicle flamers
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        // December 2021 - CGL requested we move this to Advanced for all fluid gun ammos.
        munitions.clear();
        munitions.add(new MunitionMutator("Coolant", 1, M_COOLANT,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setISAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "360, TO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(vehicleFlamerAmmos, munitions);

        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Coolant", 1, M_COOLANT,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setClanAdvancement(DATE_ES, DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "360, TO"));

        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanVehicleFlamerAmmos, munitions);

        // Create the munition types for heavy flamers
        munitions.clear();
        munitions.add(new MunitionMutator("Coolant", 1, M_COOLANT,
                new TechAdvancement(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setISAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                        .setISApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "360, TO"));
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(heavyFlamerAmmos, munitions);

        munitions.clear();
        munitions.add(new MunitionMutator("(Clan) Coolant", 1, M_COOLANT,
                new TechAdvancement(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                        .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                        .setClanAdvancement(DATE_ES, DATE_ES, DATE_ES, DATE_NONE, DATE_NONE)
                        .setClanApproximate(false, false, false, false, false)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED),
                "360, TO"));
        // Walk through both the base types and the
        // mutators, and create munition types.
        AmmoType.createMunitions(clanHeavyFlamerAmmos, munitions);

        // cache types that share a launcher for loadout purposes
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if (!(et instanceof AmmoType)) {
                continue;
            }
            AmmoType at = (AmmoType) et;
            int nType = at.getAmmoType();
            if (m_vaMunitions[nType] == null) {
                m_vaMunitions[nType] = new Vector<>();
            }

            m_vaMunitions[nType].addElement(at);
        }
    }

    private static void createMunitions(List<AmmoType> bases, List<MunitionMutator> munitions) {
        for (AmmoType base : bases) {
            for (MunitionMutator mutator : munitions) {
                EquipmentType.addType(mutator.createMunitionType(base));
            }
        }
    }

    // Anti-Missile Ammo

    private static AmmoType createISAMSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-Missile System Ammo [IS]";
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
        ammo.rulesRefs = "204, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2613, 2617, 3048, 2835, 3045)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_CC);
        return ammo;
    }

    private static AmmoType createCLAMSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-Missile System Ammo [Clan]";
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
        ammo.rulesRefs = "204, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_F, RATING_D, RATING_C)
                .setClanAdvancement(2824, 2831, 2835, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSA)
                .setProductionFactions(F_CSA);
        return ammo;
    }

    // Arrow Missile Launchers and Artillery Ammo - see Mutators above as well.

    private static AmmoType createISArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3044).setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createCLArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(2593, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createLongTomAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Long Tom Ammo";
        ammo.shortName = "Long Tom";
        ammo.setInternalName("ISLongTomAmmo");
        ammo.addLookupName("ISLongTom Ammo");
        ammo.addLookupName("ISLongTomArtillery Ammo");
        ammo.addLookupName("IS Ammo Long Tom");
        ammo.addLookupName("IS Long Tom Ammo");
        ammo.addLookupName("CLLongTomAmmo");
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
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setAdvancement(2445, 2500, 2520, DATE_NONE, DATE_NONE).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createSniperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Sniper Ammo";
        ammo.shortName = "Sniper";
        ammo.setInternalName("ISSniperAmmo");
        ammo.addLookupName("ISSniper Ammo");
        ammo.addLookupName("ISSniperArtillery Ammo");
        ammo.addLookupName("IS Ammo Sniper");
        ammo.addLookupName("IS Sniper Ammo");
        ammo.addLookupName("CLSniperAmmo");
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
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createThumperAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thumper Ammo";
        ammo.shortName = "Thumper";
        ammo.setInternalName("ISThumperAmmo");
        ammo.addLookupName("ISThumper Ammo");
        ammo.addLookupName("ISThumperArtillery Ammo");
        ammo.addLookupName("IS Ammo Thumper");
        ammo.addLookupName("IS Thumper Ammo");
        ammo.addLookupName("CLThumperAmmo");
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
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    // Cruise Missiles

    private static AmmoType createISCruiseMissile50Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/50 Ammo";
        ammo.setInternalName("ISCruiseMissile50Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 50;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 75;
        ammo.cost = 20000;
        ammo.tonnage = 25;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISCruiseMissile70Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/70 Ammo";
        ammo.setInternalName("ISCruiseMissile70Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 70;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 129;
        ammo.cost = 50000;
        ammo.tonnage = 35;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISCruiseMissile90Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/90 Ammo";
        ammo.setInternalName("ISCruiseMissile90Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 90;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 191;
        ammo.cost = 90000;
        ammo.tonnage = 45;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISCruiseMissile120Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Cruise Missile/120 Ammo";
        ammo.setInternalName("ISCruiseMissile120Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 120;
        ammo.ammoType = AmmoType.T_CRUISE_MISSILE;
        ammo.shots = 1;
        ammo.bv = 285;
        ammo.cost = 140000;
        ammo.tonnage = 60;
        ammo.flags = ammo.flags.or(F_CRUISE_MISSILE);
        ammo.rulesRefs = "284, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3065, 3095, DATE_NONE, DATE_NONE, DATE_NONE)
            .setISApproximate(false, true, false, false, false)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    // Artillery Cannon Shells

    private static AmmoType createISLongTomCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Long Tom Cannon Ammo";
        ammo.shortName = "Long Tom Cannon";
        ammo.setInternalName("ISLongTomCannonAmmo");
        ammo.addLookupName("ISLongTomCannon Ammo");
        ammo.addLookupName("ISLongTomArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Long Tom Cannon");
        ammo.addLookupName("IS Long Tom Cannon Ammo");
        ammo.addLookupName("CLLongTomCannonAmmo");
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
        ammo.rulesRefs = "285, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_LC, F_CWF)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }
    /*
     * private static AmmoType createCLLongTomCannonAmmo() { AmmoType ammo = new
     * AmmoType();
     *
     * ammo.name = "Long Tom Cannon Ammo"; ammo.shortName = "Long Tom Cannon";
     * ammo.setInternalName("CLLongTomCannonAmmo");
     * ammo.addLookupName("CLLongTomCannon Ammo");
     * ammo.addLookupName("CLLongTomArtilleryCannon Ammo");
     * ammo.addLookupName("CL Ammo Long Tom Cannon");
     * ammo.addLookupName("CL Long Tom Cannon Ammo"); ammo.damagePerShot = 1;
     * ammo.rackSize = 20; ammo.ammoType = AmmoType.T_LONG_TOM_CANNON; ammo.shots =
     * 5; ammo.bv = 41; ammo.cost = 20000; ammo.rulesRefs = "285, TO";
     *
     * ammo.techAdvancement.setTechBase(TECH_BASE_CLAN);
     * ammo.techAdvancement.setClanAdvancement(3032, 3072, DATE_NONE);
     * ammo.techAdvancement.setTechRating(RATING_B);
     * ammo.techAdvancement.setAvailability( new int[] { RATING_X, RATING_F,
     * RATING_E, RATING_D }); return ammo; }
     */

    private static AmmoType createISSniperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Sniper Cannon Ammo";
        ammo.shortName = "Sniper Cannon";
        ammo.setInternalName("ISSniperCannonAmmo");
        ammo.addLookupName("ISSniperCannon Ammo");
        ammo.addLookupName("ISSniperArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Sniper Cannon");
        ammo.addLookupName("IS Sniper Cannon Ammo");
        ammo.addLookupName("CLSniperCannonAmmo");
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
        ammo.rulesRefs = "285, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_LC, F_CWF)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    /*
     * private static AmmoType createCLSniperCannonAmmo() { AmmoType ammo = new
     * AmmoType();
     *
     * ammo.name = "Sniper Cannon Ammo"; ammo.shortName = "Sniper Cannon";
     * ammo.setInternalName("CLSniperCannonAmmo");
     * ammo.addLookupName("CLSniperCannon Ammo");
     * ammo.addLookupName("CLSniperArtilleryCannon Ammo");
     * ammo.addLookupName("CL Ammo Sniper Cannon");
     * ammo.addLookupName("CL Sniper Cannon Ammo"); ammo.damagePerShot = 1;
     * ammo.rackSize = 10; ammo.ammoType = AmmoType.T_SNIPER_CANNON; ammo.shots =
     * 10; ammo.bv = 10; ammo.cost = 15000; ammo.rulesRefs = "285, TO";
     *
     * ammo.techAdvancement.setTechBase(TECH_BASE_CLAN);
     * ammo.techAdvancement.setClanAdvancement(3032, 3072, DATE_NONE);
     * ammo.techAdvancement.setTechRating(RATING_B);
     * ammo.techAdvancement.setAvailability( new int[] { RATING_X, RATING_F,
     * RATING_E, RATING_D }); return ammo; }
     */

    private static AmmoType createISThumperCannonAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Thumper Cannon Ammo";
        ammo.shortName = "Thumper Cannon";
        ammo.setInternalName("ISThumperCannonAmmo");
        ammo.addLookupName("ISThumperCannon Ammo");
        ammo.addLookupName("ISThumperArtilleryCannon Ammo");
        ammo.addLookupName("IS Ammo Thumper Cannon");
        ammo.addLookupName("IS Thumper Cannon Ammo");
        ammo.addLookupName("CLThumperCannonAmmo");
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
        ammo.rulesRefs = "285, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(3012, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(3032, 3079, DATE_NONE, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_LC, F_CWF)
                .setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    /*
     * private static AmmoType createCLThumperCannonAmmo() { AmmoType ammo = new
     * AmmoType();
     *
     * ammo.name = "Thumper Cannon Ammo"; ammo.shortName = "Thumper Cannon";
     * ammo.setInternalName("CLThumperCannonAmmo");
     * ammo.addLookupName("CLThumperCannon Ammo");
     * ammo.addLookupName("CLThumperArtilleryCannon Ammo");
     * ammo.addLookupName("CL Ammo Thumper Cannon");
     * ammo.addLookupName("CL Thumper Cannon Ammo"); ammo.damagePerShot = 1;
     * ammo.rackSize = 5; ammo.ammoType = AmmoType.T_THUMPER_CANNON; ammo.shots =
     * 20; ammo.bv = 5; ammo.cost = 10000; ammo.rulesRefs = "285, TO";
     *
     * ammo.techAdvancement.setTechBase(TECH_BASE_CLAN);
     * ammo.techAdvancement.setClanAdvancement(3032, 3072, DATE_NONE);
     * ammo.techAdvancement.setTechRating(RATING_B);
     * ammo.techAdvancement.setAvailability( new int[] { RATING_X, RATING_F,
     * RATING_E, RATING_D }); return ammo; }
     */

    private static AmmoType createBATubeArtyAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA Tube Artillery Ammo";
        // TODO need mutator for Smoke Artillery
        ammo.shortName = "Tube Artillery";
        ammo.setInternalName("ISBATubeArtilleryAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_BA_TUBE;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 2;
        ammo.bv = 4;
        ammo.kgPerShot = 15;
        ammo.cost = 900;
        ammo.rulesRefs = "284, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3070, 3075, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS);
        return ammo;
    }

    // AUTOCANNON AND RIFLE AMMO

    private static AmmoType createISAC2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_D, RATING_D)
                .setISAdvancement(2290, 2300, 2305, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2290, 2300, 2305, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISAC5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_D, RATING_D)
                .setISAdvancement(2240, 2250, 2255, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2240, 2250, 2255, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISAC10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_D, RATING_D)
                .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createISAC20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setTechRating(RATING_B)
                .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return ammo;
    }

    private static AmmoType createISLAC2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLAC5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createCLPROAC2Ammo() {

        AmmoType ammo = new AmmoType();

        ammo.name = "ProtoMech AC/2 Ammo";
        ammo.shortName = "Proto AC/2";
        ammo.setInternalName("Clan ProtoMech AC/2 Ammo");
        ammo.addLookupName("CLProtoAC2Ammo");
        ammo.addLookupName("CLProtoAC2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_PAC;
        ammo.shots = 40;
        ammo.bv = 4;
        ammo.cost = 1200;
        ammo.rulesRefs = "286, TO";
        ammo.kgPerShot = 25;
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CBS).setProductionFactions(F_CBS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLPROAC4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "ProtoMech AC/4 Ammo";
        ammo.shortName = "Proto AC/4";
        ammo.setInternalName("Clan ProtoMech AC/4 Ammo");
        ammo.addLookupName("CLProtoAC4Ammo");
        ammo.addLookupName("CLProtoAC4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_PAC;
        ammo.shots = 20;
        ammo.bv = 6;
        ammo.cost = 4800;
        ammo.rulesRefs = "286, TO";
        ammo.kgPerShot = 50;
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CBS).setProductionFactions(F_CBS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLPROAC8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "ProtoMech AC/8 Ammo";
        ammo.shortName = "Proto AC/8";
        ammo.setInternalName("Clan ProtoMech AC/8 Ammo");
        ammo.addLookupName("CLProtoAC8Ammo");
        ammo.addLookupName("CLProtoAC8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_PAC;
        ammo.shots = 10;
        ammo.bv = 8;
        ammo.cost = 6300;
        ammo.kgPerShot = 100;
        ammo.rulesRefs = "286, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3070, 3073, DATE_NONE,DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_CBS).setProductionFactions(F_CBS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISHVAC2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "285, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E).setISAdvancement(3059, 3079)
                .setISApproximate(false, false).setPrototypeFactions(F_CC).setProductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISHVAC5Ammo() {
        AmmoType ammo = new AmmoType();

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
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E).setISAdvancement(3059, 3079)
                .setISApproximate(false, false).setPrototypeFactions(F_CC).setProductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    private static AmmoType createISHVAC10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "285, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E).setISAdvancement(3059, 3079)
                .setISApproximate(false, false).setPrototypeFactions(F_CC).setProductionFactions(F_CC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
        return ammo;
    }

    // LB-X Cluster Ammos

    private static AmmoType createCLLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X Cluster Ammo";
        ammo.shortName = "LB-2X Cluster";
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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_CCY)
                .setReintroductionFactions(F_CGS);
        return ammo;
    }

    private static AmmoType createCLLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X Cluster Ammo";
        ammo.shortName = "LB-5X Cluster";
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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 10-X Cluster Ammo";
        ammo.shortName = "LB-10X Cluster";
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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_CLAN);
        return ammo;
    }

    private static AmmoType createCLLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X Cluster Ammo";
        ammo.shortName = "LB-20X Cluster";
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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_CCY)
                .setReintroductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createISLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X Cluster Ammo";
        ammo.shortName = "LB 2-X Cluster";
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
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 5-X Cluster Ammo";
        ammo.shortName = "LB 5-X Cluster";
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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 10-X Cluster Ammo";
        ammo.shortName = "LB 10-X Cluster";
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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2590, 2595, 3040, 2840, 3035)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X Cluster Ammo";
        ammo.shortName = "LB 20-X Cluster";
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
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createCLLB2XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_CCY)
                .setReintroductionFactions(F_CGS);
        return ammo;
    }

    private static AmmoType createCLLB5XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLLB10XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_CLAN);
        return ammo;
    }

    private static AmmoType createCLLB20XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setClanAdvancement(2824, 2826, 2828, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_CCY)
                .setReintroductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createISLB2XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLB5XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLB10XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_C).setISAdvancement(2590, 2595, 3040, 2840, 3035)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLB20XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "TM 207";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createCLUltra2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN);
        return ammo;
    }

    private static AmmoType createCLUltra5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN);
        return ammo;
    }

    private static AmmoType createCLUltra10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 100;
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN);
        return ammo;
    }

    private static AmmoType createCLUltra20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_D, RATING_C)
                .setClanAdvancement(2825, 2827, 2829, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CLAN)
                .setProductionFactions(F_CLAN);
        return ammo;
    }

    private static AmmoType createISUltra2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISUltra5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_D).setISAdvancement(2635, 2640, 3040, 2915, 3035)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISUltra10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISUltra20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "208, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3057, 3060, 3061, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_LC, F_FW);
        return ammo;
    }

    private static AmmoType createISRotary2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISRotary5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createCLRotary2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 22.2;
        ammo.rulesRefs = "286, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN)
            .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(3073, DATE_NONE, 3104, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, false, false, false)
            .setPrototypeFactions(F_CSF).setProductionFactions(F_CSF)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLRotary5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "286, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN)
            .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setClanAdvancement(3073, DATE_NONE, 3104, DATE_NONE, DATE_NONE)
            .setClanApproximate(false, false, false, false, false)
            .setPrototypeFactions(F_CSF).setProductionFactions(F_CSF)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISLightRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Rifle Ammo";
        ammo.shortName = "Light Rifle";
        ammo.setInternalName("IS Ammo Light Rifle");
        ammo.addLookupName("ISLight Rifle Ammo");
        ammo.addLookupName("ISLightRifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_RIFLE;
        ammo.shots = 18;
        ammo.bv = 3;
        ammo.cost = 1000;
        ammo.rulesRefs = "338, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_F, RATING_X, RATING_D)
            .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, true, false, false)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMediumRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium Rifle Ammo";
        ammo.shortName = "Medium Rifle";
        ammo.setInternalName("IS Ammo Medium Rifle");
        ammo.addLookupName("ISMedium Rifle Ammo");
        ammo.addLookupName("ISMediumRifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_RIFLE;
        ammo.shots = 9;
        ammo.bv = 6;
        ammo.cost = 1000;
        ammo.rulesRefs = "338, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_F, RATING_X, RATING_D)
            .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, true, false, false)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISHeavyRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Rifle Ammo";
        ammo.shortName = "Heavy Rifle";
        ammo.setInternalName("IS Ammo Heavy Rifle");
        ammo.addLookupName("ISHeavy Rifle Ammo");
        ammo.addLookupName("ISHeavyRifle Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_RIFLE;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.cost = 1000;
        ammo.rulesRefs = "338, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_F, RATING_X, RATING_D)
            .setISAdvancement(DATE_PS, DATE_NONE, 3084, DATE_NONE, DATE_NONE)
            .setISApproximate(false, false, true, false, false)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

//Chemical Laser Ammos

    private static AmmoType createCLSmallChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Small Chemical Laser Ammo";
        ammo.shortName = "Small Chemical Laser";
        ammo.setInternalName("CLSmallChemLaserAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_CHEMICAL_LASER;
        ammo.shots = 60;
        ammo.bv = 1;
        ammo.cost = 30000;
        ammo.rulesRefs = "320, TO";
        ammo.kgPerShot = 16.6;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLMediumChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium Chemical Laser Ammo";
        ammo.shortName = "Medium Chemical Laser";
        ammo.setInternalName("CLMediumChemLaserAmmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_CHEMICAL_LASER;
        ammo.shots = 30;
        ammo.bv = 5;
        ammo.cost = 30000;
        ammo.rulesRefs = "320, TO";
        ammo.kgPerShot = 33.33;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLLargeChemicalLaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Large Chemical Laser Ammo";
        ammo.shortName = "Large Chemical Laser";
        ammo.setInternalName("CLLargeChemLaserAmmo");
        ammo.damagePerShot = 8;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_CHEMICAL_LASER;
        ammo.shots = 10;
        ammo.bv = 12;
        ammo.cost = 30000;
        ammo.rulesRefs = "320, TO";
        ammo.kgPerShot = 100;
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setClanAdvancement(DATE_NONE, 3059, 3083, DATE_NONE, DATE_NONE).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;

    }

//Flamer and Fluid Gun/Sprayer Ammo (Mutators Above)

    private static AmmoType createISHeavyFlamerAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "312, TO";

        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3068, 3079, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return ammo;
    }

    private static AmmoType createCLHeavyFlamerAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "312, TO";
        ammo.kgPerShot = 10;
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3065, 3067, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF);
        return ammo;
    }

    private static AmmoType createISVehicleFlamerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Vehicle Flamer Ammo";
        ammo.shortName = "Flamer";
        ammo.setInternalName("IS Vehicle Flamer Ammo");
        ammo.addLookupName("IS Ammo Vehicle Flamer");
        ammo.addLookupName("ISVehicleFlamer Ammo");
        ammo.addLookupName("Clan Vehicle Flamer Ammo");
        ammo.addLookupName("Clan Ammo Vehicle Flamer");
        ammo.addLookupName("CLVehicleFlamer Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_VEHICLE_FLAMER;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 1000;
        ammo.rulesRefs = "218, TM";
        ammo.kgPerShot = 50;
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_A, RATING_A, RATING_B, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createISFluidGunAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "313, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createCLFluidGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Fluid Gun Ammo";
        ammo.shortName = "Fluid Gun";
        ammo.setInternalName("CLFluidGun Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_FLUID_GUN;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.cost = 500;
        ammo.rulesRefs = "313, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

//Gauss Rifle Ammos

    private static AmmoType createISGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Gauss Rifle Ammo [IS]";
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
        ammo.rulesRefs = "219, TM";
        // This is going to be a rare difference between TT and MM. Removing the
        // Extinction date on Gauss ammo.
        // The prototype share the base ammo and rather than make a whole new ammo, just
        // going say the IS can figure out
        // how to make large round steel balls.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_D, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2587, 2590, 3045, DATE_NONE, 3038).setISApproximate(false, false, false, false, true)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_FC, F_FW, F_DC);

        return ammo;
    }

    private static AmmoType createCLGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Gauss Rifle Ammo [Clan]";
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
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_F, RATING_D, RATING_D)
                .setClanAdvancement(2822, 2828, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
        return ammo;
    }

    private static AmmoType createISLTGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Gauss Rifle Ammo";
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
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3049, 3056, 3065, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISHVGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Gauss Rifle Ammo";
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
        ammo.rulesRefs = "218, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3051, 3061, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FC);
        return ammo;
    }

    private static AmmoType createCLAPGaussRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-Personnel Gauss Rifle Ammo";
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
        ammo.rulesRefs = "218, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3065, 3069, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CJF)
                .setProductionFactions(F_CJF);
        return ammo;
    }

    private static AmmoType createCLHAG20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Hyper-Assault Gauss Rifle/20 Ammo";
        ammo.shortName = "HAG/20";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG20 Ammo");
        ammo.addLookupName("Clan HAG 20 Ammo");
        ammo.addLookupName("HAG/20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_HAG;
        ammo.shots = 6;
        ammo.bv = 33;
        ammo.cost = 30000;
        ammo.kgPerShot = 166.66;
        ammo.explosive = false;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(3062, 3068, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createCLHAG30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Hyper-Assault Gauss Rifle/30 Ammo";
        ammo.shortName = "HAG/30";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG30 Ammo");
        ammo.addLookupName("Clan HAG 30 Ammo");
        ammo.addLookupName("HAG/30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_HAG;
        ammo.shots = 4;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(3062, 3068, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createCLHAG40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Hyper-Assault Gauss Rifle/40 Ammo";
        ammo.shortName = "HAG/40";
        ammo.setInternalName(ammo.name);
        ammo.addLookupName("CLHAG40 Ammo");
        ammo.addLookupName("Clan HAG 40 Ammo");
        ammo.addLookupName("HAG/40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_HAG;
        ammo.shots = 3;
        ammo.bv = 67;
        ammo.cost = 30000;
        ammo.explosive = false;
        ammo.rulesRefs = "219, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setClanAdvancement(3062, 3068, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createISIHVGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Heavy Gauss Rifle Ammo";
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
        ammo.rulesRefs = "313, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3065, DATE_NONE, 3081, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMagshotGRAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Magshot Gauss Rifle Ammo";
        ammo.shortName = "Magshot";
        ammo.setInternalName("ISMagshotGR Ammo");
        ammo.addLookupName("IS Magshot GR Ammo");
        ammo.damagePerShot = 2;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_MAGSHOT;
        ammo.shots = 50;
        ammo.bv = 2;
        ammo.cost = 1000;
        ammo.rulesRefs = "314, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_C)
                .setISAdvancement(3059, 3072, 3078, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISSBGaussRifleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Silver Bullet Gauss Rifle Ammo";
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
        ammo.rulesRefs = "314, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3051, DATE_NONE, 3080, DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FC).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

//Grenade Launcher Ammo (See Mutators above)

    private static AmmoType createISVGLAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Fragmentation Grenades [VGL]";
        ammo.shortName = "VGL Fragmentation";
        ammo.setInternalName("IS Ammo VGL");
        ammo.addLookupName("ISVehicularGrenadeLauncherAmmo");
        ammo.addLookupName("CL Ammo VGL");
        ammo.addLookupName("CLVehicularGrenadeLauncherAmmo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_VGL;
        ammo.munitionType = AmmoType.M_STANDARD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "315, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_PS, DATE_ES, 3080, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_PS, DATE_ES, 3080, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }


    private static AmmoType createCLVGLAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Fragmentation Grenades [VGL]";
        ammo.shortName = "VGL Fragmentation";
        ammo.setInternalName("CL Ammo VGL");
        ammo.addLookupName("CLVehicularGrenadeLauncherAmmo");
        ammo.damagePerShot = 0;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_VGL;
        ammo.munitionType =
        AmmoType.M_STANDARD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "315, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_B)
                .setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setClanAdvancement(DATE_PS, DATE_ES, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, true, false);
        return ammo;
    }
    
    // Machine Gun Ammos
    // Standard MGs
    private static AmmoType createISMGAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_A, RATING_A, RATING_B, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, 2826, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createCLMGAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_B, RATING_B, RATING_A)
                .setClanAdvancement(2821, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createISMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_A, RATING_A, RATING_B, RATING_A)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, 2826, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
        return ammo;
    }

    private static AmmoType createCLMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_B, RATING_B, RATING_A)
                .setClanAdvancement(2821, 2825, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    // Light MGs

    private static AmmoType createISLightMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Full]";
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
        ammo.kgPerShot = 5;
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setISAdvancement(3064, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return ammo;
    }

    private static AmmoType createCLLightMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Full]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setClanAdvancement(3055, 3060, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createISLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Half]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setISAdvancement(3064, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return ammo;
    }

    private static AmmoType createCLLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Machine Gun Ammo [Half]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setClanAdvancement(3055, 3060, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    // Heavy MGs

    private static AmmoType createISHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Full]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setISAdvancement(3063, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TC)
                .setProductionFactions(F_TC);
        return ammo;
    }

    private static AmmoType createISHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Half]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_B)
                .setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setISAdvancement(3063, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TC)
                .setProductionFactions(F_TC);
        return ammo;
    }

    private static AmmoType createCLHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Full]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setClanAdvancement(3054, 3059, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Machine Gun Ammo [Half]";
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
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_B)
                .setClanAdvancement(3054, 3059, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    // Mines - See Minefield.java TODO - Need EMP mines (See IO pg 61 and TO 365)

// Missile Launcher Munitions
    // Standard ATMs

    private static AmmoType createCLATM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/6 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard ATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    // ATM Extended Range
    private static AmmoType createCLATM3ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM6ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/6 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM9ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM12ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range ATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    // ATM HE AMMOs
    private static AmmoType createCLATM3HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/3 Ammo";
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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM6HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/6 Ammo";
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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM9HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/9 Ammo";
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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLATM12HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive ATM/12 Ammo";
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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3052, 3053, 3054, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, true, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

//iATMs
    // iATM Standard

    private static AmmoType createCLIATM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/6 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Standard iATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    // iATM ER
    private static AmmoType createCLIATM3ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM6ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/6 Ammo";
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
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM9ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM12ERAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Extended-Range iATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    // iATM HE Ammo
    private static AmmoType createCLIATM3HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM6HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/6 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM9HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLIATM12HEAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "High-Explosive iATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
        .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
        .setClanAdvancement(3054, 3070, DATE_NONE, DATE_NONE, DATE_NONE)
        .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
        .setProductionFactions(F_CCY);
        return ammo;
    }

    // iATM Improved Inferno
    private static AmmoType createCLIATM3IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        //IntOPs doesn't have an extinct date for IIW but code indicates its extinct. Giving benefit of the
        //doubt and assigning F code for Dark Age for Homeworld Clans.
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM6IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/6 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        //IntOPs doesn't have an extinct date for IIW but code indicates its extinct. Giving benefit of the
        //doubt and assigning F code for Dark Age for Homeworld Clans.
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM9IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        //IntOPs doesn't have an extinct date for IIW but code indicates its extinct. Giving benefit of the
        //doubt and assigning F code for Dark Age for Homeworld Clans.
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM12IIWAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Inferno iATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "65, IO";
        //IntOPs doesn't have an extinct date for IIW but code indicates its extinct. Giving benefit of the
        //doubt and assigning F code for Dark Age for Homeworld Clans.
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // iATM Improved Mag Pulse
    private static AmmoType createCLIATM3IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/3 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM6IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/6 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM9IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/9 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLIATM12IMPAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Magnetic Pulse iATM/12 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "67, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
                .setClanAdvancement(3070, DATE_NONE, DATE_NONE, 3080, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY).setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

// Standard LRMs (see Mutators Above)

    private static AmmoType createISLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2295, 2300, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2295, 2300, 2400, 2830, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    // Enhanced LRMs

    private static AmmoType createISEnhancedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 5 Ammo";
        ammo.shortName = "NLRM 5";
        ammo.setInternalName("ISEnhancedLRM5 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 24;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 7;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
       ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3058,DATE_NONE, 3082,DATE_NONE,DATE_NONE)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        
        return ammo;
    }

    private static AmmoType createISEnhancedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 10 Ammo";
        ammo.shortName = "NLRM 10";
        ammo.setInternalName("ISEnhancedLRM10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 12;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 13;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
       ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3058,DATE_NONE, 3082,DATE_NONE,DATE_NONE)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISEnhancedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 15 Ammo";
        ammo.shortName = "NLRM 15";
        ammo.setInternalName("ISEnhancedLRM15 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 8;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 20;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
       ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3058,DATE_NONE, 3082,DATE_NONE,DATE_NONE)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISEnhancedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Enhanced LRM 20 Ammo";
        ammo.shortName = "NLRM 20";
        ammo.setInternalName("ISEnhancedLRM20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_NLRM;
        ammo.shots = 6;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.bv = 26;
        ammo.cost = 31000;
        ammo.rulesRefs = "326, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
       ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
            .setISAdvancement(3058,DATE_NONE, 3082,DATE_NONE,DATE_NONE)
            .setPrototypeFactions(F_FS)
            .setProductionFactions(F_FS)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // EXTENDED LRMs
    private static AmmoType createISExtendedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3054, 3080,DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_LC).setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISExtendedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3054, 3080,DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_LC).setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISExtendedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3054, 3080,DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_LC).setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISExtendedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "327, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(DATE_NONE, 3054, 3080,DATE_NONE, DATE_NONE)
                .setPrototypeFactions(F_FS, F_LC).setProductionFactions(F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // STANDARD CLAN LRMS
    private static AmmoType createCLLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 41.65;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLLRM10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 83.3;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLLRM15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 124.95;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    private static AmmoType createCLLRM20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 166.6;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCY)
                .setProductionFactions(F_CCY);
        return ammo;
    }

    // CLAN STREAK LRMs

    private static AmmoType createCLStreakLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 41.65;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 83.3;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 124.95;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 166.6;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Clan Streak LRMs - The protomek editions

    private static AmmoType createCLStreakLRM1Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 1 Ammo";
        ammo.shortName = "Streak LRM 1";
        ammo.setInternalName("Clan Streak LRM 1 Ammo");
        ammo.addLookupName("CLStreakLRM1 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 8.33;
        ammo.bv = 0.016;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 2 Ammo";
        ammo.shortName = "Streak LRM 2";
        ammo.setInternalName("Clan Streak LRM 2 Ammo");
        ammo.addLookupName("CLStreakLRM2 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 16.67;
        ammo.bv = 0.033;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM3Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 3 Ammo";
        ammo.shortName = "Streak LRM 3";
        ammo.setInternalName("Clan Streak LRM 3 Ammo");
        ammo.addLookupName("CLStreakLRM3 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 24.99;
        ammo.bv = 0.05;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 4 Ammo";
        ammo.shortName = "Streak LRM 4";
        ammo.setInternalName("Clan Streak LRM 4 Ammo");
        ammo.addLookupName("CLStreakLRM4 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 33.32;
        ammo.bv = 0.067;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 6 Ammo";
        ammo.shortName = "Streak LRM 6";
        ammo.setInternalName("Clan Streak LRM 6 Ammo");
        ammo.addLookupName("CLStreakLRM6 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 49.98;
        ammo.bv = 0.1;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM7Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 7 Ammo";
        ammo.shortName = "Streak LRM 7";
        ammo.setInternalName("Clan Streak LRM 7 Ammo");
        ammo.addLookupName("CLStreakLRM7 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 58.31;
        ammo.bv = 0.117;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM8Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 8 Ammo";
        ammo.shortName = "Streak LRM 8";
        ammo.setInternalName("Clan Streak LRM 8 Ammo");
        ammo.addLookupName("CLStreakLRM8 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 66.64;
        ammo.bv = 0.133;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM9Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 9 Ammo";
        ammo.shortName = "Streak LRM 9";
        ammo.setInternalName("Clan Streak LRM 9 Ammo");
        ammo.addLookupName("CLStreakLRM9 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 74.97;
        ammo.bv = 0.15;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM11Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 11 Ammo";
        ammo.shortName = "Streak LRM 11";
        ammo.setInternalName("Clan Streak LRM 11 Ammo");
        ammo.addLookupName("CLStreakLRM11 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 91.63;
        ammo.bv = 0.183;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM12Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 12 Ammo";
        ammo.shortName = "Streak LRM 12";
        ammo.setInternalName("Clan Streak LRM 12 Ammo");
        ammo.addLookupName("CLStreakLRM12 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 99.96;
        ammo.bv = 0.2;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM13Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 13 Ammo";
        ammo.shortName = "Streak LRM 13";
        ammo.setInternalName("Clan Streak LRM 13 Ammo");
        ammo.addLookupName("CLStreakLRM13 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 108.29;
        ammo.bv = 0.216;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM14Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 14 Ammo";
        ammo.shortName = "Streak LRM 14";
        ammo.setInternalName("Clan Streak LRM 14 Ammo");
        ammo.addLookupName("CLStreakLRM14 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 116.62;
        ammo.bv = 0.233;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM16Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 16 Ammo";
        ammo.shortName = "Streak LRM 16";
        ammo.setInternalName("Clan Streak LRM 16 Ammo");
        ammo.addLookupName("CLStreakLRM16 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 133.28;
        ammo.bv = 0.266;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM17Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 17 Ammo";
        ammo.shortName = "Streak LRM 17";
        ammo.setInternalName("Clan Streak LRM 17 Ammo");
        ammo.addLookupName("CLStreakLRM17 mmo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 141.61;
        ammo.bv = 0.283;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM18Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 18 Ammo";
        ammo.shortName = "Streak LRM 18";
        ammo.setInternalName("Clan Streak LRM 18 Ammo");
        ammo.addLookupName("CLStreakLRM18 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 149.94;
        ammo.bv = 0.3;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createCLStreakLRM19Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Streak LRM 19 Ammo";
        ammo.shortName = "Streak LRM 19";
        ammo.setInternalName("Clan Streak LRM 19 Ammo");
        ammo.addLookupName("CLStreakLRM19 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoType.T_LRM_STREAK;
        ammo.flags = ammo.flags.or(F_PROTOMECH);
        ammo.shots = 1;
        ammo.kgPerShot = 158.27;
        ammo.bv = 0.316;
        ammo.cost = 60000;
        ammo.rulesRefs = "327, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(DATE_NONE, 3057, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CJF)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // CLAN PROTO LRMS
    private static AmmoType createCLLRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.bv = 0.02;
        ammo.kgPerShot = 8.33;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 16.66;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 24.99;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 33.32;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 49.98;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM7Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 58.31;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM8Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 66.64;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM9Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 74.97;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM11Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 91.63;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM12Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 99.96;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM13Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 108.29;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM14Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 116.62;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM16Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 133.28;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM17Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 141.61;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM18Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 149.94;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRM19Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 158.27;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

//Standard MRMs
    private static AmmoType createISMRM10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM30Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM40Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3052, 3058, 3063, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    // Standard SRMs
    private static AmmoType createISSRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createISSRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createISSRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(true).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2365, 2370, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2365, 2370, 2400, 2836, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    // Clan SRMs (Includes Proto ones)

    private static AmmoType createCLSRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLSRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CCC);
        return ammo;
    }

    private static AmmoType createCLSRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 30;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLSRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CCC);
        return ammo;
    }

    private static AmmoType createCLSRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 50;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLSRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CCC);
        return ammo;
    }

    // Multi-Missile Launcher(MMLs)
    private static AmmoType createISMML3LRMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        //March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML3SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 3 SRM Ammo";
        ammo.shortName = "MML 3/SRM";
        ammo.setInternalName("IS Ammo MML-3 SRM");
        ammo.addLookupName("ISMML3 SRM Ammo");
        ammo.addLookupName("IS MML-3 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MML;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.shots = 33;
        ammo.bv = 4;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        //March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML5LRMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML5SRMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML7LRMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML7SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 7 SRM Ammo";
        ammo.shortName = "MML 7/SRM";
        ammo.setInternalName("IS Ammo MML-7 SRM");
        ammo.addLookupName("ISMML7 SRM Ammo");
        ammo.addLookupName("IS MML-7 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_MML;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.shots = 14;
        ammo.bv = 8;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML9LRMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";
        // March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    private static AmmoType createISMML9SRMAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "MML 9 SRM Ammo";
        ammo.shortName = "MML 9/SRM";
        ammo.setInternalName("IS Ammo MML-9 SRM");
        ammo.addLookupName("ISMML9 SRM Ammo");
        ammo.addLookupName("IS MML-9 SRM Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_MML;
        ammo.flags = ammo.flags.or(F_HOTLOAD).or(F_MML_SRM);
        ammo.shots = 11;
        ammo.bv = 11;
        ammo.cost = 27000;
        ammo.rulesRefs = "229, TM";
        //March 2022 - CGL (Greekfire) requested MML adjustments to Tech Progression.
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, 3067, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setProductionFactions(F_MERC,F_WB);
        return ammo;
    }

    // Rocket Launcher Ammo
    private static AmmoType createISRL10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 10 Ammo";
        ammo.setInternalName("IS Ammo RL-10");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_MH);
        return ammo;
    }

    private static AmmoType createISRL15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 15 Ammo";
        ammo.setInternalName("IS Ammo RL-15");
        ammo.addLookupName("CL Ammo RL-Prototype-15");
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 1500;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_MH);
        return ammo;
    }

    private static AmmoType createISRL20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RL 20 Ammo";
        ammo.setInternalName("IS Ammo RL-20");
        ammo.addLookupName("CL Ammo RL-Prototype-20");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 2000;
        ammo.rulesRefs = "229, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                .setISAdvancement(DATE_ES, 3064, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setProductionFactions(F_MH);
        return ammo;
    }

    // Clan Standard Streak Launchers
    private static AmmoType createCLStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_D, RATING_D)
                .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSA)
                .setProductionFactions(F_CSA);
        return ammo;
    }

    private static AmmoType createCLStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_D, RATING_D)
                .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSA)
                .setProductionFactions(F_CSA);
        return ammo;
    }

    private static AmmoType createCLStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_D, RATING_D)
                .setClanAdvancement(2819, 2822, 2830, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_CSA)
                .setProductionFactions(F_CSA);
        return ammo;
    }

    // Clan ProtoMech Streak Launchers
    private static AmmoType createCLStreakSRM1Ammo() {
        AmmoType ammo = new AmmoType();
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
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLStreakSRM3Ammo() {
        AmmoType ammo = new AmmoType();
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
        ammo.kgPerShot = 30;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLStreakSRM5Ammo() {
        AmmoType ammo = new AmmoType();
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
        ammo.kgPerShot = 50;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    // IS Streak Launchers
    private static AmmoType createISStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(2645, 2647, 2650, 2845, 3035).setISApproximate(false, false, true, false, false)
                .setClanAdvancement(2645, 2647, 2650, 2845, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createISStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    // NARC PODS

    private static AmmoType createISNarcAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Narc Pods";
        ammo.shortName = "Narc";
        ammo.setInternalName("ISNarc Pods");
        ammo.addLookupName("IS Ammo Narc");
        ammo.addLookupName("IS Narc Missile Beacon Ammo");
        ammo.addLookupName("CLNarc Pods");
        ammo.addLookupName("Clan Ammo Narc");
        ammo.addLookupName("Clan Narc Missile Beacon Ammo");
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.cost = 6000;
        ammo.kgPerShot = 150;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_F, RATING_D, RATING_C)
                .setISAdvancement(2580, 2587, 3049, 2795, 3035).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 2818, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createISNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 150;
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3054, 3060, 3064, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createCLNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
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
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setClanAdvancement(3054, 3060, 3064, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    // TODO Shoot and Sit Narc Missiles - See IO pg 132

    private static AmmoType createISiNarcAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS, F_WB);
        return ammo;
    }

    private static AmmoType createISiNarcECMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS, F_WB);
        return ammo;
    }

    private static AmmoType createISiNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS, F_WB);
        return ammo;
    }

    private static AmmoType createISiNarcHaywireAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS, F_WB);
        return ammo;
    }

    private static AmmoType createISiNarcNemesisAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "141, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
                .setISAdvancement(3054, 3062, 3066, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CS)
                .setProductionFactions(F_CS, F_WB);
        return ammo;
    }

    // Torpedo Ammo (Damn the Torpedoes)
    private static AmmoType createISLRT5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2370, 2380, 2400, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
        return ammo;
    }

    private static AmmoType createISLRT10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";

        ammo.techAdvancement.setTechBase(TECH_BASE_IS);
        ammo.techAdvancement.setISAdvancement(2365, 2380, 2400);
        ammo.techAdvancement.setTechRating(RATING_C);
        ammo.techAdvancement.setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);
        return ammo;
    }

    private static AmmoType createISLRT15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";

        ammo.techAdvancement.setTechBase(TECH_BASE_IS);
        ammo.techAdvancement.setISAdvancement(2365, 2380, 2400);
        ammo.techAdvancement.setTechRating(RATING_C);
        ammo.techAdvancement.setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);
        return ammo;
    }

    private static AmmoType createISLRT20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "229, TM";

        ammo.techAdvancement.setTechBase(TECH_BASE_IS);
        ammo.techAdvancement.setISAdvancement(2365, 2380, 2400);
        ammo.techAdvancement.setTechRating(RATING_C);
        ammo.techAdvancement.setAvailability(RATING_C, RATING_C, RATING_C, RATING_C);
        return ammo;
    }

    private static AmmoType createISSRT2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C).setISAdvancement(2370, 2380, 2400)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISSRT4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C).setISAdvancement(2370, 2380, 2400)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISSRT6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C).setISAdvancement(2370, 2380, 2400)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FW);
        return ammo;
    }

    // Clan LRT

    private static AmmoType createCLLRT1Ammo() {
        AmmoType ammo = new AmmoType();

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
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 16.66;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 24.99;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 33.32;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 41.65;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createCLLRT6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 49.98;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT7Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 58.31;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT8Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 66.64;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT9Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 74.97;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 83.3;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createCLLRT11Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 91.63;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT12Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 99.96;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT13Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 108.29;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT14Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 116.62;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 124.95;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createCLLRT16Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 133.28;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT17Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 141.61;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT18Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 149.94;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT19Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 158.27;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But LRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;

    }

    private static AmmoType createCLLRT20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 166.6;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_D, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    // Clan SRTs

    private static AmmoType createCLSRT1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 10;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLSRT2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 20;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createCLSRT3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 30;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLSRT4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 40;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createCLSRT5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 50;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But SRM Tech Base and Avail Ratings.
        ammo.rulesRefs = "231, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_C, RATING_C)
                .setClanAdvancement(3055, 3060, 3061, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSJ)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLSRT6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 60;
        ammo.rulesRefs = "230, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_C, RATING_C, RATING_C)
                .setClanAdvancement(2820, 2824, 2825, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    // TODO Fusillade Ammo

    // MORTAR AMMOS - Most ammo's are mutators that are listed above.

    private static AmmoType createISAPMortar1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2526, 2531, 3052, 2819, 3043)
                .setISApproximate(true, false, false, false, false)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createISAPMortar2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2526, 2531, 3052, 2819, 3043).setISApproximate(true, false, false, false, false)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createISAPMortar4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2526, 2531, 3052, 2819, 3043).setISApproximate(true, false, false, false, false)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createISAPMortar8Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_F, RATING_E)
                .setISAdvancement(2526, 2531, 3052, 2819, 3043).setISApproximate(true, false, false, false, false)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createCLAPMortar1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
                .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
        return ammo;
    }

    private static AmmoType createCLAPMortar2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
                .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
        return ammo;
    }

    private static AmmoType createCLAPMortar4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
                .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
        return ammo;
    }

    private static AmmoType createCLAPMortar8Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "324, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_F, RATING_E, RATING_E)
                .setClanAdvancement(2835, 2840, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CBR)
                .setProductionFactions(F_CBR);
        return ammo;
    }

    // PLASMA WEAPONS

    private static AmmoType createISPlasmaRifleAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "234, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3061, 3068, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_CC)
                .setProductionFactions(F_CC);
        return ammo;
    }

    private static AmmoType createCLPlasmaCannonAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "234, TM";
        ammo.kgPerShot = 100;
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3068, 3069, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    // RISC APDS
    private static AmmoType createISAPDSAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "RISC Advanced Point Defense System Ammo";
        ammo.shortName = "RISC APDS";
        ammo.setInternalName("ISAPDS Ammo");
        ammo.damagePerShot = 1; // only used for ammo crits
        ammo.rackSize = 2; // only used for ammo crits
        ammo.ammoType = AmmoType.T_APDS;
        ammo.shots = 12;
        ammo.bv = 22;
        ammo.cost = 2000;
        ammo.rulesRefs = "91, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_X, RATING_E)
                .setISAdvancement(3134, 3137, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_RS)
                .setProductionFactions(F_RS);
        return ammo;
    }

    // Mek Taser

    private static AmmoType createISMekTaserAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "346, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3065, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    // CAPITAL AND SUB-CAP WEAPONS

    // naval ammo
    /*
     * Because ammo by ton is not in whole number I am doing this as single shot
     * with a function to change the number of shots which will be called from the
     * BLK file. This means I also have to convertBV and cost per ton to BV and cost
     * per shot
     */

    private static AmmoType createLightMassDriverAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light Mass Driver Ammo";
        ammo.setInternalName("Ammo Light Mass Driver");
        ammo.addLookupName("LightMassDriver Ammo");
        ammo.damagePerShot = 60;
        ammo.ammoType = AmmoType.T_LMASS;
        ammo.shots = 1;
        ammo.bv = 882;
        ammo.cost = 150000;
        ammo.ammoRatio = 30;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH);
        return ammo;
    }

    private static AmmoType createMediumMassDriverAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium Mass Driver Ammo";
        ammo.setInternalName("Ammo Medium Mass Driver");
        ammo.addLookupName("MediumMassDriver Ammo");
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_MMASS;
        ammo.shots = 1;
        ammo.bv = 1470;
        ammo.cost = 300000;
        ammo.ammoRatio = 30;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH);
        return ammo;
    }

    private static AmmoType createHeavyMassDriverAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy Mass Driver Ammo";
        ammo.setInternalName("Ammo Heavy Mass Driver");
        ammo.addLookupName("HeavyMassDriver Ammo");
        ammo.damagePerShot = 140;
        ammo.ammoType = AmmoType.T_HMASS;
        ammo.shots = 1;
        ammo.bv = 2058;
        ammo.cost = 600000;
        ammo.ammoRatio = 30;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_D)
                .setAvailability(RATING_F, RATING_X, RATING_F, RATING_F)
                .setISAdvancement(2715, DATE_NONE, DATE_NONE, 2855, 3066)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2715, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH);
        return ammo;
    }

    private static AmmoType createLightNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light N-Gauss Ammo";
        ammo.setInternalName("Ammo Light N-Gauss");
        ammo.addLookupName("LightNGauss Ammo");
        ammo.damagePerShot = 15;
        ammo.ammoType = AmmoType.T_LIGHT_NGAUSS;
        ammo.shots = 1;
        ammo.tonnage = 0.2;
        ammo.bv = 378;
        ammo.cost = 45000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052).setISApproximate(true, true, false, true, false)
                .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createMediumNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Medium N-Gauss Ammo";
        ammo.setInternalName("Ammo Medium N-Gauss");
        ammo.addLookupName("MediumNGauss Ammo");
        ammo.damagePerShot = 25;
        ammo.ammoType = AmmoType.T_MED_NGAUSS;
        ammo.shots = 1;
        ammo.tonnage = 0.4;
        ammo.bv = 630;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052).setISApproximate(true, true, false, true, false)
                .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createHeavyNGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy N-Gauss Ammo";
        ammo.setInternalName("Ammo Heavy N-Gauss");
        ammo.addLookupName("HeavyNGauss Ammo");
        ammo.damagePerShot = 40;
        ammo.ammoType = AmmoType.T_HEAVY_NGAUSS;
        ammo.shots = 1;
        ammo.tonnage = 0.5;
        ammo.bv = 756;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.rulesRefs = "323, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(2440, 2448, DATE_NONE, 2950, 3052).setISApproximate(true, true, false, true, false)
                .setClanAdvancement(2440, 2448, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createNAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/10 Ammo";
        ammo.setInternalName("Ammo NAC/10");
        ammo.addLookupName("NAC10 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.2;
        ammo.bv = 237;
        ammo.cost = 30000;
        ammo.ammoRatio = 0.2;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createNAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/20 Ammo";
        ammo.setInternalName("Ammo NAC/20");
        ammo.addLookupName("NAC20 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.4;
        ammo.bv = 474;
        ammo.cost = 60000;
        ammo.ammoRatio = 0.4;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createNAC25Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/25 Ammo";
        ammo.setInternalName("Ammo NAC/25");
        ammo.addLookupName("NAC25 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.6;
        ammo.bv = 593;
        ammo.cost = 75000;
        ammo.ammoRatio = 0.6;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createNAC30Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/30 Ammo";
        ammo.setInternalName("Ammo NAC/30");
        ammo.addLookupName("NAC30 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.tonnage = 0.8;
        ammo.bv = 711;
        ammo.cost = 90000;
        ammo.ammoRatio = 0.8;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createNAC35Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/35 Ammo";
        ammo.setInternalName("Ammo NAC/35");
        ammo.addLookupName("NAC35 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 35;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.tonnage = 1;
        ammo.bv = 620;
        ammo.cost = 105000;
        ammo.ammoRatio = 1.0;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createNAC40Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "NAC/40 Ammo";
        ammo.setInternalName("Ammo NAC/40");
        ammo.addLookupName("NAC40 Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_NAC;
        ammo.shots = 1;
        ammo.tonnage = 1.2;
        ammo.bv = 708;
        ammo.cost = 120000;
        ammo.ammoRatio = 1.2;
        ammo.capital = true;
        ammo.rulesRefs = "333, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_D).setAvailability(RATING_E, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2950, 3051)
                .setISApproximate(false, true, false, true, false)
                .setClanAdvancement(DATE_ES, 2200, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setProductionFactions(F_TA)
                .setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    // Standard Cap Missiles
    private static AmmoType createBarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Barracuda Ammo";
        ammo.setInternalName("Ammo Barracuda");
        ammo.addLookupName("Barracuda Ammo");
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_BARRACUDA;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2200, 2305, 3055, 2950, 3051).setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createWhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "White Shark Ammo";
        ammo.setInternalName("Ammo White Shark");
        ammo.addLookupName("WhiteShark Ammo");
        ammo.addLookupName("White Shark Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_WHITE_SHARK;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.ammoRatio = 40;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2200, 2305, 3055, 2950, 3051).setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createKillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Killer Whale Ammo";
        ammo.setInternalName("Ammo Killer Whale");
        ammo.addLookupName("KillerWhale Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2200, 2305, 3055, 2950, 3051).setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2200, 2305, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    // Tele-Operated Missiles

    private static AmmoType createBarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Barracuda (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo Barracuda-T");
        ammo.addLookupName("BarracudaT Ammo");
        ammo.shortName = "Barracuda-T";
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_BARRACUDA_T;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.toHitModifier = -2;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3056, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createWhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "White Shark (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo White Shark-T");
        ammo.addLookupName("WhiteSharkT Ammo");
        ammo.shortName = "White Shark-T";
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_WHITE_SHARK_T;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3056, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createKillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Killer Whale (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo Killer Whale-T");
        ammo.addLookupName("KillerWhaleT Ammo");
        ammo.shortName = "Killer Whale-T";
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_KILLER_WHALE_T;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3056, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createKrakenAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Kraken (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo KrakenT");
        ammo.addLookupName("KrakenT Ammo");
        ammo.shortName = "Kraken-T";
        ammo.damagePerShot = 10;
        ammo.ammoType = AmmoType.T_KRAKEN_T;
        ammo.shots = 1;
        ammo.tonnage = 100.0;
        ammo.bv = 288;
        ammo.cost = 55000;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.rulesRefs = "251, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, true, false).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createKrakenMAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createScreenLauncherAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Screen Launcher Ammo";
        ammo.setInternalName("Ammo Screen");
        ammo.addLookupName("ScreenLauncher Ammo");
        ammo.damagePerShot = 0;
        ammo.ammoType = AmmoType.T_SCREEN_LAUNCHER;
        ammo.shots = 1;
        ammo.tonnage = 10.0;
        ammo.bv = 20;
        ammo.cost = 10000;
        ammo.flags = ammo.flags.or(F_SCREEN);
        ammo.rulesRefs = "237, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3053, 3055, 3057, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    // Sub-Capital Cannons
    private static AmmoType createLightSCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Light SCC Ammo";
        ammo.setInternalName("Ammo Light SCC");
        ammo.addLookupName("Light SCC Ammo");
        ammo.addLookupName("LightSCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.tonnage = 0.5;
        ammo.bv = 47;
        ammo.cost = 10000;
        ammo.ammoRatio = 2;
        ammo.capital = true;
        ammo.rulesRefs = "343, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createMediumSCCAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "343, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createHeavySCCAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Heavy SCC Ammo";
        ammo.setInternalName("Ammo Heavy SCC");
        ammo.addLookupName("Heavy SCC Ammo");
        ammo.addLookupName("HeavySCC Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_SCC;
        ammo.shots = 1;
        ammo.tonnage = 2;
        ammo.bv = 124;
        ammo.cost = 25000;
        ammo.ammoRatio = 0.5;
        ammo.capital = true;
        ammo.rulesRefs = "343, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3068, 3073, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3090, 3091, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // Sub-Capital Missiles

    private static AmmoType createMantaRayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Manta Ray Ammo";
        ammo.setInternalName("Ammo Manta Ray");
        ammo.addLookupName("MantaRay Ammo");
        ammo.addLookupName("Manta Ray Ammo");
        ammo.damagePerShot = 5;
        ammo.ammoType = AmmoType.T_MANTA_RAY;
        ammo.shots = 1;
        ammo.tonnage = 18.0;
        ammo.bv = 50;
        ammo.cost = 30000;
        ammo.ammoRatio = 18;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB).setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createSwordfishAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Swordfish Ammo";
        ammo.setInternalName("Ammo Swordfish");
        ammo.addLookupName("Swordfish Ammo");
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_SWORDFISH;
        ammo.shots = 1;
        ammo.tonnage = 15.0;
        ammo.bv = 40;
        ammo.cost = 25000;
        ammo.capital = true;
        ammo.ammoRatio = 15;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB).setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createStingrayAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Stringray Ammo";
        ammo.setInternalName("Ammo Stringray");
        ammo.addLookupName("Stingray Ammo");
        ammo.addLookupName("ClStingray Ammo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_STINGRAY;
        ammo.shots = 1;
        ammo.tonnage = 12.0;
        ammo.bv = 62;
        ammo.cost = 19000;
        ammo.ammoRatio = 12;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB).setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createPiranhaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Piranha Ammo";
        ammo.setInternalName("Ammo Piranha");
        ammo.addLookupName("Piranha Ammo");
        ammo.addLookupName("PiranhaAmmo");
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_PIRANHA;
        ammo.shots = 1;
        ammo.tonnage = 10.0;
        ammo.bv = 84;
        ammo.cost = 15000;
        ammo.ammoRatio = 10;
        ammo.capital = true;
        ammo.flags = ammo.flags.or(F_CAP_MISSILE);
        ammo.rulesRefs = "345, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(DATE_NONE, 3060, 3072, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, 3070, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, true, false, false, false)
                .setPrototypeFactions(F_WB).setProductionFactions(F_WB)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    // AR10 Ammo - TODO - Check to see if these can be eliminated as the AR10 Fires
    // standard missiles.

    private static AmmoType createAR10BarracudaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Barracuda Ammo";
        ammo.setInternalName("Ammo AR10 Barracuda");
        ammo.addLookupName("AR10 Barracuda Ammo");
        ammo.shortName = "Barracuda";
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        // Set the date TP of these weapons to match the AR10 and the ratings to match
        // the missiles
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2540, 2550, 3055, 2950, 3051).setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createAR10KillerWhaleAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Killer Whale Ammo";
        ammo.setInternalName("Ammo AR10 Killer Whale");
        ammo.addLookupName("AR10 KillerWhale Ammo");
        ammo.shortName = "Killer Whale";
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date TP of these weapons to match the AR10 and the ratings to match
        // the missiles
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2540, 2550, 3055, 2950, 3051).setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createAR10WhiteSharkAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 White Shark Ammo";
        ammo.setInternalName("Ammo AR10 White Shark");
        ammo.addLookupName("AR10 WhiteShark Ammo");
        ammo.shortName = "White Shark";
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date TP of these weapons to match the AR10 and the ratings to match
        // the missiles
        ammo.rulesRefs = "210, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_D, RATING_E, RATING_E, RATING_D)
                .setISAdvancement(2540, 2550, 3055, 2950, 3051).setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2540, 2550, 3055, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH).setReintroductionFactions(F_FS, F_LC);
        return ammo;
    }

    // AR10s cannot launch tele missiles, so the AR10 tele-missiles are unofficial.

    private static AmmoType createAR10BarracudaTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Barracuda (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo AR10 Barracuda-T");
        ammo.addLookupName("AR10 BarracudaT Ammo");
        ammo.shortName = "Barracuda-T";
        ammo.damagePerShot = 2;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.tonnage = 30.0;
        ammo.bv = 65;
        ammo.cost = 8000;
        ammo.flags = ammo.flags.or(F_AR10_BARRACUDA).or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.toHitModifier = -2;
        ammo.capital = true;
        // Set the date of these weapons to match the Tele Missile itself
        ammo.rulesRefs = "251, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3053, 3056, 3060)
                .setISApproximate(true, false, false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return ammo;
    }

    private static AmmoType createAR10KillerWhaleTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Killer Whale (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo AR10 Killer Whale-T");
        ammo.addLookupName("AR10 KillerWhaleT Ammo");
        ammo.shortName = "Killer Whale-T";
        ammo.damagePerShot = 4;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.tonnage = 50.0;
        ammo.bv = 96;
        ammo.cost = 20000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date of these weapons to match the Tele Missile itself
        ammo.rulesRefs = "251, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3053, 3056, 3060)
                .setISApproximate(true, false, false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return ammo;
    }

    private static AmmoType createAR10WhiteSharkTAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 White Shark (Tele-Operated) Ammo";
        ammo.setInternalName("Ammo AR10 White Shark-T");
        ammo.addLookupName("AR10 WhiteSharkT Ammo");
        ammo.shortName = "White Shark-T";
        ammo.damagePerShot = 3;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.shots = 1;
        ammo.tonnage = 40.0;
        ammo.bv = 72;
        ammo.cost = 14000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_TELE_MISSILE).or(F_CAP_MISSILE);
        ammo.capital = true;
        // Set the date of these weapons to match the Tele Missile itself
        ammo.rulesRefs = "251, TW";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setISAdvancement(3053, 3056, 3060)
                .setISApproximate(true, false, false).setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D).setPrototypeFactions(F_CS, F_DC)
                .setProductionFactions(F_DC).setReintroductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.UNOFFICIAL);
        return ammo;
    }

    // Industrial Munitions

    private static AmmoType createISNailRivetGunAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Nail/Rivet Gun Ammo";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("IS Ammo Nail/Rivet - Full");
        ammo.addLookupName("ISNailRivetGun Ammo (300)");
        ammo.addLookupName("CL Ammo Nail/Rivet - Full");
        ammo.addLookupName("CLNailRivetGun Ammo (300)");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NAIL_RIVET_GUN;
        ammo.shots = 300;
        ammo.bv = 1;
        ammo.cost = 300;
        ammo.tonnage = 1f;
        ammo.explosive = false;
        ammo.rulesRefs = "246, TM";
        ammo.kgPerShot = 3.33;
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISNailRivetGunAmmoHalf() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Nail/Rivet Gun Ammo (Half-ton)";
        ammo.shortName = "Nail/Rivet Gun";
        ammo.setInternalName("IS Ammo Nail/Rivet - Half");
        ammo.addLookupName("CL Ammo Nail/Rivet - Half");
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NAIL_RIVET_GUN;
        ammo.shots = 150;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        ammo.cost = 150;
        ammo.explosive = false;
        ammo.rulesRefs = "246, TM";
        ammo.kgPerShot = 3.33;
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
                .setISApproximate(true, true, false, false, false)
                .setClanAdvancement(2309, 2310, 2312, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, true, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);
        return ammo;
    }

    private static AmmoType createISC3RemoteSensorAmmo() {
        AmmoType ammo = new AmmoType();
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

        ammo.techAdvancement.setTechBase(TECH_BASE_IS);
        ammo.techAdvancement.setISAdvancement(3072, DATE_NONE, DATE_NONE);
        ammo.techAdvancement.setTechRating(RATING_E);
        ammo.techAdvancement.setAvailability(RATING_X, RATING_X, RATING_F, RATING_X);
        return ammo;
    }

    // THUNDERBOLT LRMs
    private static AmmoType createISThunderbolt5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISThunderbolt10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISThunderbolt15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISThunderbolt20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.rulesRefs = "347, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

// TODO : PRIMITIVE and PROTOTYPE

    // PRIMITIVE AMMOs
    private static AmmoType createISAC2pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/2 Ammo";
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
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2290, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISAC5pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/5 Ammo";
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
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2240, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISAC10pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/10 Ammo";
        ammo.shortName = "AC/10p";
        ammo.setInternalName("IS Ammo AC/10 Primitive");
        ammo.addLookupName("ISAC10p Ammo");
        ammo.addLookupName("IS Autocannon/10 Primitive Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_PRIMITIVE;
        ammo.shots = 8;
        ammo.bv = 12;
        ammo.cost = 12000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2450, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISAC20pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Autocannon/20 Ammo";
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
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2488, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM5pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 5 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM10pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 10 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM15pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 15 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISLRM20pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype LRM 20 Ammo";
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
        ammo.setModes("", "HotLoad");
        ammo.cost = 30000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2295, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISSRM2pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype SRM 2 Ammo";
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
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISSRM4pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype SRM 4 Ammo";
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
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISSRM6pAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype SRM 6 Ammo";
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
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createISPrimitiveLongTomAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Primitive Prototype Long Tom Artillery Ammo";
        ammo.shortName = "Primitive Long Tom";
        ammo.setInternalName("ISPrimitiveLongTomAmmo");
        ammo.addLookupName("ISPrimitiveLongTomArtillery Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 25;
        ammo.ammoType = AmmoType.T_LONG_TOM_PRIM;
        ammo.shots = 4;
        ammo.bv = 35;
        ammo.cost = 10000;
        // IO Doesn't strictly define when these weapons stop production. Checked with
        // Herb and they would always be around
        // This to cover some of the back worlds in the Periphery.
        ammo.rulesRefs = "118, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_F, RATING_X, RATING_X, RATING_X)
                .setISAdvancement(2365, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_TA)
                .setProductionFactions(F_TA);
        return ammo;
    }

    private static AmmoType createPrototypeArrowIVAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Prototype Arrow IV Ammo";
        ammo.shortName = "pArrow IV";
        ammo.setInternalName("ProtoTypeArrowIVAmmo");
        ammo.addLookupName("ProtoArrowIV Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_ARROWIV_PROTO;
        ammo.shots = 4;
        ammo.bv = 30;
        ammo.cost = 40000;
        ammo.rulesRefs = "217, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(2593, 2600, DATE_NONE, 2830, 3044).setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH);
        return ammo;
    }

    // Clan Improved Stuff.
    private static AmmoType createCLImprovedAC2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/2 Ammo";
        ammo.shortName = "iAC/2 Ammo";
        ammo.setInternalName("CLIMPAmmoAC2");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_IMP;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.cost = 1000;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_C, RATING_X, RATING_X)
                .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                .setClanApproximate(false, true, false, false, false).setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedAC5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/5 Ammo";
        ammo.shortName = "iAC/5 Ammo";
        ammo.setInternalName("CLIMPAmmoAC5");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_IMP;
        ammo.shots = 20;
        ammo.bv = 9;
        ammo.cost = 4500;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_C, RATING_X, RATING_X)
                .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                .setClanApproximate(false, true, false, false, false).setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedAC10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/10 Ammo";
        ammo.shortName = "iAC/10 Ammo";
        ammo.setInternalName("CLIMPAmmoAC10");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_IMP;
        ammo.shots = 10;
        ammo.bv = 15;
        ammo.cost = 6000;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_C, RATING_X, RATING_X)
                .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                .setClanApproximate(false, true, false, false, false).setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedAC20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Autocannon/20 Ammo";
        ammo.shortName = "iAC/20 Ammo";
        ammo.setInternalName("CLIMPAmmoAC20");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_IMP;
        ammo.shots = 5;
        ammo.bv = 22;
        ammo.cost = 10000;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_C, RATING_X, RATING_X)
                .setClanAdvancement(DATE_NONE, 2815, 2818, 2833, 3080)
                .setClanApproximate(false, true, false, false, false).setProductionFactions(F_CLAN)
                .setReintroductionFactions(F_EI);
        return ammo;
    }

    // CLAN IMPROVED LRMS
    private static AmmoType createCLImprovedLRM5Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 5 Ammo";
        ammo.shortName = "iLRM 5";
        ammo.setInternalName("ClanImprovedLRM5Ammo");
        ammo.addLookupName("CLImpLRM5Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_IMP;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2818, 2820, 2831, 3080).setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CCY).setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedLRM10Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 10 Ammo";
        ammo.shortName = "iLRM 10";
        ammo.setInternalName("ClanImprovedLRM10Ammo");
        ammo.addLookupName("CLImpLRM10Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM_IMP;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2818, 2820, 2831, 3080).setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CCY).setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedLRM15Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 15 Ammo";
        ammo.setInternalName("ClanImprovedLRM15Ammo");
        ammo.addLookupName("CLImpLRM15Ammo");
        ammo.shortName = "iLRM 15";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM_IMP;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2818, 2820, 2831, 3080).setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CCY).setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedLRM20Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved LRM 20 Ammo";
        ammo.shortName = "iLRM 20";
        ammo.setInternalName("ClanImprovedLRM20Ammo");
        ammo.addLookupName("CLImpLRM20Ammo");
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM_IMP;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.cost = 30000;
        ammo.flags = ammo.flags.or(F_HOTLOAD);
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.33;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2818, 2820, 2831, 3080).setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_CCY).setProductionFactions(F_CCY).setReintroductionFactions(F_EI);
        return ammo;
    }

    private static AmmoType createCLImprovedGaussAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved Gauss Rifle Ammo";
        ammo.shortName = "iGauss";
        ammo.setInternalName("CLImpGaussAmmo");
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS_IMP;
        ammo.shots = 8;
        ammo.bv = 40;
        ammo.cost = 20000;
        ammo.kgPerShot = 125;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_E, RATING_X, RATING_E)
                .setClanAdvancement(2818, 2821, 2822, 2837, 3080).setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_CGS).setProductionFactions(F_CGS).setReintroductionFactions(F_EI);
        return ammo;
    }

    // Clan Improved SRMs
    private static AmmoType createCLImprovedSRM2Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved SRM 2 Ammo";
        ammo.shortName = "iSRM 2";
        ammo.setInternalName("ClanImpAmmoSRM2");
        ammo.addLookupName("CLImpSRM2 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_IMP;
        ammo.shots = 50;
        ammo.bv = 4;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2817, 2819, 2828, 3080).setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CCC).setProductionFactions(F_CCC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLImprovedSRM4Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved SRM 4 Ammo";
        ammo.shortName = "iSRM 4";
        ammo.setInternalName("ClImpAmmoSRM4");
        ammo.addLookupName("CLImpSRM4 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_IMP;
        ammo.shots = 25;
        ammo.bv = 7;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2817, 2819, 2828, 3080).setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CCC).setProductionFactions(F_CCC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createCLImprovedSRM6Ammo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Improved SRM 6 Ammo";
        ammo.shortName = "iSRM 6";
        ammo.setInternalName("CLImpAmmoSRM6");
        ammo.addLookupName("CLImpSRM6 Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_IMP;
        ammo.shots = 15;
        ammo.bv = 10;
        ammo.cost = 27000;
        ammo.kgPerShot = 10;
        ammo.rulesRefs = "96, IO";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_D, RATING_X, RATING_X)
                .setClanAdvancement(2815, 2817, 2819, 2828, 3080).setClanApproximate(true, false, false, true, false)
                .setPrototypeFactions(F_CCC).setProductionFactions(F_CCC)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // TODO - To be Sorted

    // Start BattleArmor and ProtoMek ammo

    private static AmmoType createBAMicroBombAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Micro Bomb Ammo";
        ammo.shortName = "Micro Bomb";
        ammo.setInternalName("BA-Micro Bomb Ammo");
        ammo.addLookupName("BAMicroBomb Ammo");
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_BA_MICRO_BOMB;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.kgPerShot = 0;
        ammo.bv = 0;
        ammo.cost = 500;
        ammo.rulesRefs = "253, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3055, 3060, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CCC)
                .setProductionFactions(F_CCC);
        return ammo;
    }

    private static AmmoType createCLTorpedoLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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

        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN);
        ammo.techAdvancement.setClanAdvancement(DATE_NONE, DATE_NONE, 2820);
        ammo.techAdvancement.setTechRating(RATING_C);
        ammo.techAdvancement.setAvailability(RATING_X, RATING_C, RATING_C, RATING_X);
        return ammo;
    }

    private static AmmoType createBACompactNarcAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Compact Narc Ammo";
        ammo.shortName = "Narc";
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
        ammo.rulesRefs = "263, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(DATE_NONE, DATE_NONE, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2870, 2875, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CSV)
                .setProductionFactions(F_CSV);
        return ammo;
    }

    private static AmmoType createBAMineLauncherAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "267, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_F)
                .setISAdvancement(DATE_NONE, 3050, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createCLPROHeavyMGAmmo() {
        // Need special processing to allow non-standard ammo loads.
        AmmoType ammo = new AmmoType();

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
        ammo.kgPerShot = 10;
        ammo.bv = 1;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But MG Tech Base and Avail Ratings.
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setClanAdvancement(3055, 3060, 3060, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLPROMGAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Machine Gun Ammo";
        ammo.shortName = "Machine Gun";
        ammo.setInternalName("Clan Machine Gun Ammo - Proto");
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags = ammo.flags.or(F_MG).or(F_PROTOMECH);
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.kgPerShot = 5;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But MG Tech Base and Avail Ratings.
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_X, RATING_B, RATING_A)
                .setClanAdvancement(3055, 3060, 3060, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    private static AmmoType createCLPROLightMGAmmo() {
        AmmoType ammo = new AmmoType();
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
        ammo.kgPerShot = 5;
        // Per Herb all ProtoMech launcher use the ProtoMech Chassis progression.
        // But MG Tech Base and Avail Ratings.
        ammo.rulesRefs = "228, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_X, RATING_C, RATING_B)
                .setClanAdvancement(3055, 3060, 3060, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CSJ);
        return ammo;
    }

    // IS BA LRM Missile Launchers
    private static AmmoType createBAISLRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 8.3;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createBAISLRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 16.6;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createBAISLRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 25;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createBAISLRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 33.4;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createBAISLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.setModes("", "HotLoad");
        ammo.kgPerShot = 41.5;
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3057, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    // Clan BA LRM Missile Launcher
    private static AmmoType createBACLLRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS);
        return ammo;
    }

    private static AmmoType createBACLLRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3058, 3060, 3062, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CGS)
                .setProductionFactions(F_CGS);
        return ammo;
    }

    // BA SRM
    private static AmmoType createBASRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF, F_LC, F_FS)
                .setProductionFactions(F_CWF, F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBASRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF, F_LC, F_FS)
                .setProductionFactions(F_CWF, F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBASRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF, F_LC, F_FS)
                .setProductionFactions(F_CWF, F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBASRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF, F_LC, F_FS)
                .setProductionFactions(F_CWF, F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBASRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF, F_LC, F_FS)
                .setProductionFactions(F_CWF, F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBASRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        // Hackish, blended the Clan and IS versions for Availability.
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_D, RATING_C, RATING_B)
                .setISAdvancement(3050, 3050, 3051, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2865, 2868, 2870, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWF, F_LC, F_FS)
                .setProductionFactions(F_CWF, F_FS, F_LC);
        return ammo;
    }

    // Advanced SRMs
    private static AmmoType createAdvancedSRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    private static AmmoType createAdvancedSRM6Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setClanAdvancement(3052, 3056, 3066, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CHH)
                .setProductionFactions(F_CHH);
        return ammo;
    }

    // BA MRMs
    private static AmmoType createISMRM1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_B)
                .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_B)
                .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_B)
                .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_B)
                .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISMRM5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_D, RATING_B)
                .setISAdvancement(3058, 3060, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_DC)
                .setProductionFactions(F_DC);
        return ammo;
    }

    private static AmmoType createISBATaserAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "BA Taser Ammo";
        ammo.shortName = "Taser";
        ammo.setInternalName(ammo.name);
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_TASER;
        ammo.flags = ammo.flags.or(F_BATTLEARMOR);
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.rulesRefs = "345, TO";
        //Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3067, 3084, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_WB)
                .setProductionFactions(F_WB);
        return ammo;
    }

    // BA Rocket Launchers
    private static AmmoType createBARL1Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBARL2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBARL3Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBARL4Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return ammo;
    }

    private static AmmoType createBARL5Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "261, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_B, RATING_B)
                .setISAdvancement(3050, 3050, 3052, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC);
        return ammo;
    }

    // Misc Stuff. (Pods)

    private static AmmoType createISCoolantPod() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Coolant Pod";
        ammo.shortName = "Coolant Pod";
        ammo.setInternalName(EquipmentTypeLookup.COOLANT_POD);
        ammo.addLookupName("IS Coolant Pod");
        ammo.addLookupName("Clan Coolant Pod");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_COOLANT_POD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 50000;

        // TODO : modes is a bodge because there is no proper end phase
        String[] theModes = { "safe", "efficient", "off", "dump" };
        ammo.setModes(theModes);
        ammo.setInstantModeSwitch(true);
        ammo.rulesRefs = "303, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setISAdvancement(DATE_NONE, 3049, 3079, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, true, false, false)
                .setClanAdvancement(DATE_NONE, 3056, 3079, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false).setPrototypeFactions(F_FS, F_LC)
                .setProductionFactions(F_FS, F_LC, F_CJF).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createISMPodAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "330, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_C)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3064, 3099, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return ammo;
    }

    // Per IO pg 40 - There is no Clan MPod.
    private static AmmoType createCLMPodAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true)
                .setTechRating(RATING_C).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3064, 3099, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return ammo;
    }

    private static AmmoType createISBPodAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Anti-BattleArmor Pods (B-Pods) Ammo";
        ammo.setInternalName("ISBPodAmmo");
        ammo.shortName = "B-Pod";
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_BPOD;
        ammo.shots = 1;
        ammo.bv = 0;
        ammo.cost = 0;
        ammo.tonnage = 0;
        ammo.rulesRefs = "204, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_E).setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3068, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(3065, 3068, 3070, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_CWX, F_LC, F_WB, F_FW)
                .setProductionFactions(F_CWX);
        return ammo;
    }

    // UNOFFICIAL AMMOs
    private static AmmoType createISAC15Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setUnofficial(true).setTechRating(RATING_C)
                .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(2488, 2500, 2502, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2488, 2500, 2502, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);
        return ammo;
    }

    private static AmmoType createISTHBLB2XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBLB5XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBLB20XAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 2-X Cluster Ammo (THB)";
        ammo.setInternalName("IS LB 2-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 2-X (CL) (THB)");
        ammo.shortName = "LB 2-X Cluster";
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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.setInternalName("IS LB 5-X Cluster Ammo (THB)");
        ammo.addLookupName("IS Ammo 5-X (CL) (THB)");
        // this isn't a true mtf code
        ammo.addLookupName("ISLBXAC5 CL Ammo (THB)");
        ammo.addLookupName("IS LB 5-X AC Ammo - Cluster (THB)");
        ammo.name = "LB 5-X Cluster Ammo (THB)";
        ammo.shortName = "LB 5-X Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX_THB;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 16;
        ammo.bv = 11;
        ammo.cost = 25000;
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "LB 20-X Cluster Ammo (THB)";
        ammo.shortName = "LB 20-X Cluster";
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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBUltra2Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBUltra10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISTHBUltra20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "THB (Unofficial)";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3055, 3058, 3060, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISRotary10Ammo() {
        AmmoType ammo = new AmmoType();

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

        ammo.techAdvancement.setTechBase(TECH_BASE_IS);
        ammo.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3067);
        ammo.techAdvancement.setTechRating(RATING_E);
        ammo.techAdvancement.setAvailability(RATING_E, RATING_E, RATING_E, RATING_E);
        return ammo;
    }

    private static AmmoType createISRotary20Ammo() {
        AmmoType ammo = new AmmoType();

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

        ammo.techAdvancement.setTechBase(TECH_BASE_IS);
        ammo.techAdvancement.setISAdvancement(DATE_NONE, DATE_NONE, 3067);
        ammo.techAdvancement.setTechRating(RATING_E);
        ammo.techAdvancement.setAvailability(RATING_E, RATING_E, RATING_E, RATING_E);
        return ammo;
    }

    private static AmmoType createCLRotary10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3073, 3104, 3145, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createCLRotary20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_CLAN).setIntroLevel(false).setUnofficial(true)
                .setTechRating(RATING_F).setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setClanAdvancement(3073, 3104, 3145, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false).setPrototypeFactions(F_CSF)
                .setProductionFactions(F_CSF);
        return ammo;
    }

    private static AmmoType createISLAC10Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_C)
                .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISLAC20Ammo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "207, TM";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_C)
                .setISAdvancement(3062, 3068, 3070, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    private static AmmoType createISRailGunAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3051, 3061, 3067, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FC);
        return ammo;
    }

  

    private static AmmoType createISAC10iAmmo() {
        AmmoType ammo = new AmmoType();

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
        ammo.rulesRefs = "Unofficial";
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setUnofficial(true).setTechRating(RATING_C)
                .setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(2443, 2460, 2465, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(2443, 2460, 2465, 2850, DATE_NONE)
                .setClanApproximate(false, false, false, true, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);
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
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
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
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
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
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
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
        ammo.rulesRefs = "207, TO";
        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(true).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setISAdvancement(3060, 3062, 3071, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS);
        return ammo;
    }

    // TODO - THINGS NUCLEAR
    private static AmmoType createAR10PeacemakerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Peacemaker Ammo";
        ammo.setInternalName("Ammo AR10 Peacemaker");
        ammo.addLookupName("AR10 Peacemaker Ammo");
        ammo.shortName = "Peacemaker";
        ammo.damagePerShot = 1000;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.tonnage = 50.0;
        ammo.shots = 1;
        ammo.bv = 10000;
        ammo.cost = 40000000;
        ammo.flags = ammo.flags.or(F_AR10_KILLER_WHALE).or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_PEACEMAKER);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E).setISAdvancement(2300)
                .setPrototypeFactions(F_TA).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createPeacemakerAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Peacemaker Ammo";
        ammo.setInternalName("Ammo Peacemaker");
        ammo.addLookupName("Peacemaker Ammo");
        ammo.addLookupName("CLPeacemaker Ammo");
        ammo.addLookupName("Ammo Clan Peacemaker");
        ammo.shortName = "Peacemaker";
        ammo.damagePerShot = 1000;
        ammo.ammoType = AmmoType.T_KILLER_WHALE;
        ammo.tonnage = 50.0;
        ammo.shots = 1;
        ammo.bv = 10000;
        ammo.cost = 40000000;
        ammo.flags = ammo.flags.or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_PEACEMAKER);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E).setISAdvancement(2300)
                .setPrototypeFactions(F_TA).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createAR10SantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "AR10 Santa Anna Ammo";
        ammo.setInternalName("Ammo AR10 Santa Anna");
        ammo.addLookupName("AR10 SantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_AR10;
        ammo.tonnage = 40.0;
        ammo.shots = 1;
        ammo.bv = 1000;
        ammo.cost = 15000000;
        ammo.flags = ammo.flags.or(F_AR10_WHITE_SHARK).or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_SANTA_ANNA);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E).setISAdvancement(2300)
                .setPrototypeFactions(F_TA).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createSantaAnnaAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Santa Anna Ammo";
        ammo.setInternalName("Ammo Santa Anna");
        ammo.addLookupName("SantaAnna Ammo");
        ammo.addLookupName("CLSantaAnna Ammo");
        ammo.shortName = "Santa Anna";
        ammo.damagePerShot = 100;
        ammo.ammoType = AmmoType.T_WHITE_SHARK;
        ammo.tonnage = 40.0;
        ammo.shots = 1;
        ammo.bv = 1000;
        ammo.cost = 15000000;
        ammo.flags = ammo.flags.or(F_NUCLEAR).or(F_CAP_MISSILE).or(F_SANTA_ANNA);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E).setISAdvancement(2300)
                .setPrototypeFactions(F_TA).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    private static AmmoType createAlamoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "Alamo Ammo";
        ammo.setInternalName("Ammo Alamo");
        ammo.addLookupName("Alamo Ammo");
        ammo.damagePerShot = 10;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_ALAMO;
        ammo.shots = 1;
        ammo.bv = 100;
        ammo.cost = 1000000;
        ammo.flags = ammo.flags.or(F_NUCLEAR);
        ammo.capital = true;

        ammo.techAdvancement.setTechBase(TECH_BASE_IS).setTechRating(RATING_E).setISAdvancement(2200)
                .setPrototypeFactions(F_TA).setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return ammo;
    }

    // Generic infantry ammo, stats are determined by the weapon it's linked to
    private static AmmoType createInfantryAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Standard Ammo";
        ammo.setInternalName(EquipmentTypeLookup.INFANTRY_AMMO);
        ammo.ammoType = AmmoType.T_INFANTRY;
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_A)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        return ammo;
    }

    private static AmmoType createInfantryInfernoAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Inferno Ammo";
        ammo.setInternalName(EquipmentTypeLookup.INFANTRY_INFERNO_AMMO);
        ammo.ammoType = AmmoType.T_INFANTRY;
        ammo.munitionType = M_INFERNO;
        ammo.techAdvancement.setTechBase(TECH_BASE_ALL).setTechRating(RATING_A)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
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
            if (((at.getAmmoType() == T_LRM) || (at.getAmmoType() == T_LRM_IMP) || (at.getAmmoType() == T_LRM_STREAK)
                    || (at.getAmmoType() == T_EXLRM) || (at.getAmmoType() == T_MRM)
                    || (at.getAmmoType() == T_ROCKET_LAUNCHER))
                    && (at.getRackSize() >= 20)
                    && ((at.getMunitionType() == M_STANDARD) || (at.getMunitionType() == M_ARTEMIS_CAPABLE)
                            || (at.getMunitionType() == M_ARTEMIS_V_CAPABLE)
                            || (at.getMunitionType() == M_NARC_CAPABLE))) {
                return true;
            }
            // ATMs
            if ((at.getAmmoType() == T_ATM) && ((at.getRackSize() >= 12 && at.getMunitionType() != M_EXTENDED_RANGE)
                    || (at.getRackSize() >= 9 && at.getMunitionType() == M_HIGH_EXPLOSIVE))) {
                return true;
            }
            // Artillery
            if (((at.getAmmoType() == T_ARROW_IV) || (at.getAmmoType() == T_LONG_TOM) || (at.getAmmoType() == T_SNIPER)
                    || (at.getAmmoType() == T_THUMPER)) && (at.getMunitionType() == M_STANDARD)) {
                return true;
            }
        }
        // TODO: mine clearance munitions

        return false;
    }

    public static boolean canDeliverMinefield(AmmoType at) {
        return (at != null)
                && ((at.getAmmoType() == T_LRM) || (at.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (at.getAmmoType() == AmmoType.T_MML))
                && ((at.getMunitionType() == M_THUNDER) || (at.getMunitionType() == M_THUNDER_INFERNO)
                        || (at.getMunitionType() == M_THUNDER_AUGMENTED)
                        || (at.getMunitionType() == M_THUNDER_VIBRABOMB)
                        || (at.getMunitionType() == M_THUNDER_ACTIVE));
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
            StringBuilder sb = new StringBuilder(s);
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

        protected String rulesRefs;

        private TechAdvancement techAdvancement;

        public MunitionMutator(String munitionName, int weightRatio, long munitionType,
                               TechAdvancement techAdvancement, String rulesRefs) {
            name = munitionName;
            weight = weightRatio;
            type = munitionType;
            this.techAdvancement = new TechAdvancement(techAdvancement);
            this.rulesRefs = rulesRefs;
        }

        /**
         * Create the <code>AmmoType</code> for this munition type for the given rack
         * size.
         *
         * @param base - the <code>AmmoType</code> of the base round.
         * @return this munition's <code>AmmoType</code>.
         */
        public AmmoType createMunitionType(AmmoType base) {
            StringBuilder nameBuf;
            StringBuilder internalName;
            int index;

            // Create an uninitialized munition object.
            AmmoType munition = new AmmoType();
            munition.setTonnage(base.getTonnage(null));
            munition.subMunitionName = name;

            // Manipulate the base round's names, depending on ammoType.
            switch (base.ammoType) {
                case AmmoType.T_AC:
                case AmmoType.T_AC_PRIMITIVE:
                case AmmoType.T_LAC:
                case AmmoType.T_AC_IMP:
                case AmmoType.T_PAC:
                    // Add the munition name to the beginning of the display name.
                    nameBuf = new StringBuilder(name);
                    nameBuf.append(" ");
                    nameBuf.append(base.name);
                    munition.name = nameBuf.toString();

                    // Add the munition name to the end of the TDB ammo name.
                    nameBuf = new StringBuilder(" - ");
                    nameBuf.append(name);
                    munition.addToEnd(base, " - " + name);

                    // The munition name appears in the middle of the other names.
                    nameBuf = new StringBuilder(base.internalName);
                    index = base.internalName.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    nameBuf.insert(index, name);
                    munition.setInternalName(nameBuf.toString());
                    munition.shortName = munition.name.replace(base.name, base.shortName);
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case AmmoType.T_ARROWIV_PROTO:
                case AmmoType.T_ARROW_IV:
                    // The munition name appears in the middle of all names.
                    nameBuf = new StringBuilder(base.name);
                    index = base.name.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    // Do special processing for munition names ending in "IV".
                    // Note: this does not work for The Drawing Board
                    if (name.endsWith("-IV")) {
                        StringBuilder tempName = new StringBuilder(name);
                        tempName.setLength(tempName.length() - 3);
                        nameBuf.insert(index, tempName);
                    } else {
                        nameBuf.insert(index, name);
                    }
                    munition.name = nameBuf.toString();

                    nameBuf = new StringBuilder(base.internalName);
                    index = base.internalName.lastIndexOf("Ammo");
                    nameBuf.insert(index, name);
                    munition.setInternalName(nameBuf.toString());
                    munition.shortName = munition.name.replace("Prototype ", "p");

                    munition.addBeforeString(base, "Ammo", name + " ");
                    munition.addToEnd(base, " - " + name);
                    if (name.equals("Homing")) {
                        munition.addToEnd(base, " (HO)"); // mep
                    }
                    break;
                case AmmoType.T_SRM:
                case AmmoType.T_SRM_PRIMITIVE:
                case AmmoType.T_SRM_IMP:
                case AmmoType.T_MRM:
                case AmmoType.T_LRM:
                case AmmoType.T_LRM_PRIMITIVE:
                case AmmoType.T_LRM_IMP:
                case AmmoType.T_MML:
                case AmmoType.T_NLRM:
                case AmmoType.T_SRM_TORPEDO:
                case AmmoType.T_LRM_TORPEDO:
                    // Add the munition name to the end of some ammo names.
                    nameBuf = new StringBuilder(" ");
                    nameBuf.append(name);
                    munition.setInternalName(base.internalName + nameBuf);
                    munition.addToEnd(base, nameBuf.toString());
                    nameBuf.insert(0, " -");
                    munition.addToEnd(base, nameBuf.toString());

                    // The munition name appears in the middle of the other names.
                    nameBuf = new StringBuilder(base.name);
                    index = base.name.lastIndexOf("Ammo");
                    nameBuf.insert(index, ' ');
                    nameBuf.insert(index, name);
                    munition.name = nameBuf.toString();
                    nameBuf = new StringBuilder(base.shortName);
                    nameBuf.append(' ');
                    nameBuf.append(name.replace("-capable", ""));
                    munition.shortName = nameBuf.toString();
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case AmmoType.T_VGL:
                    // Replace "Fragmentation" with the submunition name
                    munition.name = base.name.replace("Fragmentation", name);

                    munition.shortName = base.shortName.replace("Fragmentation", name);
                    internalName = new StringBuilder(base.getInternalName());
                    munition.setInternalName(internalName.insert(internalName.lastIndexOf("Ammo"), name + " ").toString());
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                case AmmoType.T_MEK_MORTAR:
                    // Replace "Shaped Charge" with the submunition name
                    munition.name = base.name.replace("Shaped Charge", name);
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
                    internalName = new StringBuilder(base.getInternalName().replace("SC", abr));
                    munition.setInternalName(internalName.toString());
                    break;
                case AmmoType.T_LONG_TOM:
                case AmmoType.T_LONG_TOM_PRIM:
                case AmmoType.T_SNIPER:
                case AmmoType.T_THUMPER:
                case AmmoType.T_LONG_TOM_CANNON:
                case AmmoType.T_SNIPER_CANNON:
                case AmmoType.T_THUMPER_CANNON:
                case AmmoType.T_VEHICLE_FLAMER:
                case AmmoType.T_HEAVY_FLAMER:
                case AmmoType.T_FLUID_GUN:
                    // Add the munition name to the beginning of the display name.
                    nameBuf = new StringBuilder(name);
                    nameBuf.append(" ");
                    nameBuf.append(base.name);
                    munition.name = nameBuf.toString();
                    munition.setInternalName(munition.name);
                    munition.addToEnd(base, munition.name);

                    munition.shortName = munition.name;
                    // The munition name appears in the middle of the other names.
                    munition.addBeforeString(base, "Ammo", name + " ");
                    break;
                default:
                    throw new IllegalArgumentException("Don't know how to create munitions for " + base.ammoType);
            }

            munition.shortName = munition.shortName.replace("(Clan) ", "");

            // Assign our munition type.
            munition.munitionType = type;

            // Make sure the tech level is now correct.
            if (techAdvancement.getIntroductionDate() > 0) {
                munition.techAdvancement = new TechAdvancement(techAdvancement);
            } else {
                munition.techAdvancement = new TechAdvancement(base.techAdvancement);
            }
            munition.techAdvancement.setStaticTechLevel(SimpleTechLevel.max(techAdvancement.getStaticTechLevel(),
                    base.techAdvancement.getStaticTechLevel()));

            munition.rulesRefs = rulesRefs;

            // Reduce base number of shots to reflect the munition's weight.
            if (munition.getMunitionType() == AmmoType.M_CASELESS) {
                munition.shots = Math.max(1, base.shots * 2);
                munition.kgPerShot = base.kgPerShot * (weight / 2);
            } else {
                munition.shots = Math.max(1, base.shots / weight);
                munition.kgPerShot = base.kgPerShot * weight;
            }

            // copy base ammoType
            munition.ammoType = base.ammoType;
            // check for cost
            double cost = base.cost;
            double bv = base.bv;

            if (((munition.getAmmoType() == T_LONG_TOM) || (munition.getAmmoType() == T_LONG_TOM_CANNON)
                    || (munition.getAmmoType() == T_SNIPER) || (munition.getAmmoType() == T_SNIPER_CANNON)
                    || (munition.getAmmoType() == T_THUMPER) || (munition.getAmmoType() == T_THUMPER_CANNON))
                    && munition.getMunitionType() == AmmoType.M_FAE) {
                bv *= 1.4;
                cost *= 3;
            }

            if ((munition.getAmmoType() == T_AC) || (munition.getAmmoType() == T_LAC)
                    || (munition.getAmmoType() == T_PAC)) {
                if (munition.getMunitionType() == AmmoType.M_ARMOR_PIERCING) {
                    cost *= 4;
                } else if ((munition.getMunitionType() == AmmoType.M_FLECHETTE)
                        || (munition.getMunitionType() == AmmoType.M_FLAK)) {
                    cost *= 1.5;
                } else if (munition.getMunitionType() == AmmoType.M_TRACER) {
                    cost *= 1.5;
                    bv *= 0.25;
                } else if (munition.getMunitionType() == AmmoType.M_INCENDIARY_AC) {
                    cost *= 2;
                } else if (munition.getMunitionType() == AmmoType.M_PRECISION) {
                    cost *= 6;
                } else if (munition.getMunitionType() == AmmoType.M_CASELESS) {
                    cost *= 1.5;
                    bv *= 1.0;
                }
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_MML)
                    || (munition.getAmmoType() == AmmoType.T_SRM) || (munition.getAmmoType() == AmmoType.T_SRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_AX_HEAD)) {
                cost *= 0.5;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SMOKE_WARHEAD)) {
                cost *= 0.5;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_INCENDIARY_LRM)) {
                cost *= 1.5;
            }

            if (((munition.getAmmoType() == AmmoType.T_SRM) || (munition.getAmmoType() == AmmoType.T_SRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML))
                    && (munition.getMunitionType() == AmmoType.M_INFERNO)) {
                cost = 13500;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SEMIGUIDED)) {
                cost *= 3;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SWARM)) {
                cost *= 2;
                bv *= 1;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_SWARM_I)) {
                cost *= 3;
                bv *= 0.2;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER)) {
                cost *= 2;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_AUGMENTED)) {
                cost *= 4;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_INFERNO)) {
                cost *= 1;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)) {
                cost *= 2.5;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_THUNDER_ACTIVE)) {
                cost *= 3;
            }

            if (munition.getMunitionType() == AmmoType.M_HOMING) {
                cost = 15000;
                // Allow Homing munitions to instantly switch between modes
                munition.instantModeSwitch = true;
                munition.setModes("Homing", "Non-Homing");
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
            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_NARC_CAPABLE)) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_ARTEMIS_CAPABLE)) {
                cost *= 2;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && (munition.getMunitionType() == AmmoType.M_LISTEN_KILL)) {
                cost *= 1.1;
            }
            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && ((munition.getMunitionType() == AmmoType.M_ANTI_TSM)
                            || (munition.getMunitionType() == AmmoType.M_FRAGMENTATION))) {
                cost *= 2;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && ((munition.getMunitionType() == AmmoType.M_DEAD_FIRE))) {
                cost *= 0.6;
                // TODO - DEAD-FIRE AMMO needs BV which is not a constant but launcher Ammo. 
            }

            if (((munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP))
                    && ((munition.getMunitionType() == AmmoType.M_TANDEM_CHARGE)
                            || (munition.getMunitionType() == AmmoType.M_ARTEMIS_V_CAPABLE))) {
                cost *= 5;
            }

            if (((munition.getAmmoType() == AmmoType.T_LRM) || (munition.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (munition.getAmmoType() == AmmoType.T_MML) || (munition.getAmmoType() == AmmoType.T_SRM)
                    || (munition.getAmmoType() == AmmoType.T_SRM_IMP) || (munition.getAmmoType() == AmmoType.T_NLRM))
                    && ((munition.getMunitionType() == AmmoType.M_HEAT_SEEKING)
                            || (munition.getMunitionType() == AmmoType.M_FOLLOW_THE_LEADER))) {
                cost *= 2;
                bv *= 0.5;
            }
            if (((munition.getAmmoType() == AmmoType.T_VEHICLE_FLAMER)
                    || (munition.getAmmoType() == AmmoType.T_HEAVY_FLAMER)
                    || (munition.getAmmoType() == AmmoType.T_FLUID_GUN))
                    && (munition.getMunitionType() == AmmoType.M_COOLANT)) {
                cost = 3000;
            }
            // Account for floating point imprecision
            munition.bv = Math.round(bv * 1000.0) / 1000.0;
            munition.cost = Math.round(cost * 1000.0) / 1000.0;

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

    /** @return The battle value for ProtoMek or BA ammo loads. */
    public double getKgPerShotBV(int shots) {
        return ((kgPerShot * shots) / 1000) * bv;
    }

    @Override
    public String getShortName() {
        return shortName.isBlank() ? getName() : shortName;
    }

    public String getSubMunitionName() {
        return subMunitionName;
    }

    /**
     * Checks to ensure that the given ammo can be used with the given weapon type.
     * Performs the following tests:<br>
     * {@code ammo} != null<br>
     * {@link Mounted#getType()} instanceof {@link AmmoType}<br>
     * {@link Mounted#isAmmoUsable()}<br>
     * {@link #isAmmoValid(AmmoType, WeaponType)}.
     *
     * @param ammo       The ammunition to be tested.
     * @param weaponType The weapon the ammo is to be used with.
     * @return TRUE if the ammo and weapon are compatible.
     */
    public static boolean isAmmoValid(Mounted ammo, WeaponType weaponType) {
        if (ammo == null) {
            return false;
        } else if (!(ammo.getType() instanceof AmmoType)) {
            return false;
        } else {
            return ammo.isAmmoUsable() && isAmmoValid((AmmoType) ammo.getType(), weaponType);
        }
    }

    /**
     * Checks to ensure that the given ammunition type is compatible with the given
     * weapon type. Performs the following tests:<br>
     * {@code ammoType} != null<br>
     * {@link AmmoType#getAmmoType()} == {@link WeaponType#getAmmoType()}<br>
     * {@link AmmoType#getRackSize()} == {@link WeaponType#getRackSize()}
     *
     * @param ammoType   The type of ammo to be tested.
     * @param weaponType The type of weapon the ammo is to be used with.
     * @return TRUE if the ammo type and weapon type are compatible.
     */
    public static boolean isAmmoValid(AmmoType ammoType, WeaponType weaponType) {
        if (ammoType == null) {
            return false;
        } else if (ammoType.getAmmoType() != weaponType.getAmmoType()) {
            return false;
        } else if (ammoType.getRackSize() != weaponType.getRackSize()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Whether the given weapon can switch to the given ammo type
     *
     * @param weapon    The weapon being considered
     * @param otherAmmo The other ammo type being considered
     * @return true/false - null arguments or linked ammo bin for the weapon result
     *         in false
     */
    public static boolean canSwitchToAmmo(Mounted weapon, AmmoType otherAmmo) {
        // no ammo switching if the weapon doesn't exist
        // or if it doesn't have an ammo bin
        // or the other ammo type doesn't exist
        if ((weapon == null) || (weapon.getLinked() == null) || (!(weapon.getLinked().getType() instanceof AmmoType))
                || (otherAmmo == null)) {
            return false;
        }

        AmmoType currentAmmoType = (AmmoType) weapon.getLinked().getType();

        // Ammo of the same type and rack size should be allowed
        boolean ammoOfSameType = currentAmmoType.equalsAmmoTypeOnly(otherAmmo)
                && (currentAmmoType.getRackSize() == otherAmmo.getRackSize());

        // MMLs can swap between different specific ammo types, so we have a special
        // case check here
        boolean mmlAmmoMatch = (currentAmmoType.getAmmoType() == AmmoType.T_MML)
                && (otherAmmo.getAmmoType() == AmmoType.T_MML)
                && (currentAmmoType.getRackSize() == otherAmmo.getRackSize());

        // AR10 ammo is explicitly excluded in equalsAmmoTypeOnly(), therefore check here
        boolean ar10Match = (currentAmmoType.getAmmoType() == AmmoType.T_AR10)
                && (otherAmmo.getAmmoType() == AmmoType.T_AR10);

        // LBXs can swap between cluster and slug ammo types
        boolean lbxAmmoMatch = (currentAmmoType.getAmmoType() == AmmoType.T_AC_LBX)
                && (otherAmmo.getAmmoType() == AmmoType.T_AC_LBX)
                && (currentAmmoType.getRackSize() == otherAmmo.getRackSize());

        boolean caselessLoaded = currentAmmoType.getMunitionType() == AmmoType.M_CASELESS;
        boolean otherBinCaseless = otherAmmo.getMunitionType() == AmmoType.M_CASELESS;
        boolean caselessMismatch = caselessLoaded != otherBinCaseless;

        boolean hasStaticFeed = weapon.hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_STATIC_FEED);
        boolean staticFeedMismatch = hasStaticFeed
                && (currentAmmoType.getMunitionType() != otherAmmo.getMunitionType());

        return (ammoOfSameType || mmlAmmoMatch || lbxAmmoMatch || ar10Match) && !caselessMismatch && !staticFeedMismatch;
    }
}
