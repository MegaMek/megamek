/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006,2007 Ben Mazur (bmazur@sev.org)
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

import megamek.common.weapons.*;
import megamek.common.weapons.battlearmor.*;
import megamek.common.weapons.infantry.*;



// TODO add XML support back in.

/**
 * A type of mech or vehicle weapon. There is only one instance of this weapon
 * for all weapons of this type.
 */
public class WeaponType extends EquipmentType {
    public static final int DAMAGE_BY_CLUSTERTABLE = -2;
    public static final int DAMAGE_VARIABLE = -3;
    public static final int DAMAGE_SPECIAL = -4;
    public static final int DAMAGE_ARTILLERY = -5;
    public static final int WEAPON_NA = Integer.MIN_VALUE;

    // weapon flags (note: many weapons can be identified by their ammo type)

    // marks any weapon affected by a targetting computer
    public static final BigInteger F_DIRECT_FIRE = BigInteger.valueOf(1).shiftLeft(0);
    public static final BigInteger F_FLAMER = BigInteger.valueOf(1).shiftLeft(1);
    // Glaze armor
    public static final BigInteger F_LASER = BigInteger.valueOf(1).shiftLeft(2);
    public static final BigInteger F_PPC = BigInteger.valueOf(1).shiftLeft(3);
    // for weapons that target Automatically (AMS)
    public static final BigInteger F_AUTO_TARGET = BigInteger.valueOf(1).shiftLeft(4);
    //can not start fires
    public static final BigInteger F_NO_FIRES = BigInteger.valueOf(1).shiftLeft(5);
    //must be only weapon attacking
    public static final BigInteger F_SOLO_ATTACK = BigInteger.valueOf(1).shiftLeft(7);
    public static final BigInteger F_VGL = BigInteger.valueOf(1).shiftLeft(8);
    // MGL for rapid fire setup
    public static final BigInteger F_MG = BigInteger.valueOf(1).shiftLeft(9);
    //Inferno weapon
    public static final BigInteger F_INFERNO = BigInteger.valueOf(1).shiftLeft(10);
    // Infantry caliber weapon, damage based on # of men shooting
    public static final BigInteger F_INFANTRY = BigInteger.valueOf(1).shiftLeft(11);
    // use missile rules for # of hits
    public static final BigInteger F_MISSILE_HITS = BigInteger.valueOf(1).shiftLeft(13);
    public static final BigInteger F_ONESHOT = BigInteger.valueOf(1).shiftLeft(14);
    public static final BigInteger F_ARTILLERY = BigInteger.valueOf(1).shiftLeft(15);

    //for Gunnery/Ballistic
    public static final BigInteger F_BALLISTIC = BigInteger.valueOf(1).shiftLeft(16);
    //for Gunnery/Energy
    public static final BigInteger F_ENERGY = BigInteger.valueOf(1).shiftLeft(17);
    //for Gunnery/Missile
    public static final BigInteger F_MISSILE = BigInteger.valueOf(1).shiftLeft(18);

    //fires
    public static final BigInteger F_PLASMA = BigInteger.valueOf(1).shiftLeft(19);
    public static final BigInteger F_INCENDIARY_NEEDLES = BigInteger.valueOf(1).shiftLeft(20);

    //War of 3039 prototypes
    public static final BigInteger F_PROTOTYPE = BigInteger.valueOf(1).shiftLeft(21);
    //Variable heat, heat is listed in dice, not points
    public static final BigInteger F_HEATASDICE = BigInteger.valueOf(1).shiftLeft(22);
    public static final BigInteger F_AMS = BigInteger.valueOf(1).shiftLeft(23);

    //may only target Infantry
    public static final BigInteger F_INFANTRY_ONLY = BigInteger.valueOf(1).shiftLeft(25);

    public static final BigInteger F_TAG = BigInteger.valueOf(1).shiftLeft(26);
    //C3 Master with Target Acquisition gear
    public static final BigInteger F_C3M = BigInteger.valueOf(1).shiftLeft(27);

    //Plasma Rifle
    public static final BigInteger F_PLASMA_MFUK = BigInteger.valueOf(1).shiftLeft(28);
    //fire Extinguisher
    public static final BigInteger F_EXTINGUISHER = BigInteger.valueOf(1).shiftLeft(29);
    public static final BigInteger F_PULSE = BigInteger.valueOf(1).shiftLeft(30);
    // Full Damage vs. Infantry
    public static final BigInteger F_BURST_FIRE = BigInteger.valueOf(1).shiftLeft(31);
    //Machine Gun Array
    public static final BigInteger F_MGA = BigInteger.valueOf(1).shiftLeft(32);
    public static final BigInteger F_NO_AIM = BigInteger.valueOf(1).shiftLeft(33);
    public static final BigInteger F_BOMBAST_LASER = BigInteger.valueOf(1).shiftLeft(34);
    public static final BigInteger F_CRUISE_MISSILE = BigInteger.valueOf(1).shiftLeft(35);
    public static final BigInteger F_B_POD = BigInteger.valueOf(1).shiftLeft(36);
    public static final BigInteger F_TASER = BigInteger.valueOf(1).shiftLeft(37);

    //Anti-ship missiles
    public static final BigInteger F_ANTI_SHIP = BigInteger.valueOf(1).shiftLeft(38);
    public static final BigInteger F_SPACE_BOMB = BigInteger.valueOf(1).shiftLeft(39);
    public static final BigInteger F_M_POD = BigInteger.valueOf(1).shiftLeft(40);
    public static final BigInteger F_DIVE_BOMB = BigInteger.valueOf(1).shiftLeft(41);
    public static final BigInteger F_ALT_BOMB = BigInteger.valueOf(1).shiftLeft(42);

    // Currently only used by MML
    public static final BigInteger F_BA_WEAPON = BigInteger.valueOf(1).shiftLeft(43);
    public static final BigInteger F_MECH_WEAPON = BigInteger.valueOf(1).shiftLeft(44);
    public static final BigInteger F_AERO_WEAPON = BigInteger.valueOf(1).shiftLeft(45);
    public static final BigInteger F_PROTO_WEAPON = BigInteger.valueOf(1).shiftLeft(46);
    public static final BigInteger F_TANK_WEAPON = BigInteger.valueOf(1).shiftLeft(47);

    public static final BigInteger F_INFANTRY_ATTACK = BigInteger.valueOf(1).shiftLeft(48);
    public static final BigInteger F_INF_BURST = BigInteger.valueOf(1).shiftLeft(49);
    public static final BigInteger F_INF_AA = BigInteger.valueOf(1).shiftLeft(50);
    public static final BigInteger F_INF_NONPENETRATING = BigInteger.valueOf(1).shiftLeft(51);
    public static final BigInteger F_INF_POINT_BLANK = BigInteger.valueOf(1).shiftLeft(52);
    public static final BigInteger F_INF_SUPPORT = BigInteger.valueOf(1).shiftLeft(53);
    public static final BigInteger F_INF_ENCUMBER = BigInteger.valueOf(1).shiftLeft(54);
    public static final BigInteger F_INF_ARCHAIC = BigInteger.valueOf(1).shiftLeft(55);

    // C3 Master Booster System
    public static final BigInteger F_C3MBS = BigInteger.valueOf(1).shiftLeft(56);
    
    public static final BigInteger F_TSEMP = BigInteger.valueOf(1).shiftLeft(57);
    
    //Naval Mass Drivers
    public static final BigInteger F_MASS_DRIVER = BigInteger.valueOf(1).shiftLeft(58);

    public static final BigInteger F_CWS = BigInteger.valueOf(1).shiftLeft(59);
    
    public static final BigInteger F_MEK_MORTAR = BigInteger.valueOf(1).shiftLeft(60);
    
    // add maximum range for AT2
    public static final int RANGE_SHORT = RangeType.RANGE_SHORT;
    public static final int RANGE_MED = RangeType.RANGE_MEDIUM;
    public static final int RANGE_LONG = RangeType.RANGE_LONG;
    public static final int RANGE_EXT = RangeType.RANGE_EXTREME;

    // add weapon classes for AT2
    public static final int CLASS_NONE = 0;
    public static final int CLASS_LASER = 1;
    public static final int CLASS_POINT_DEFENSE = 2;
    public static final int CLASS_PPC = 3;
    public static final int CLASS_PULSE_LASER = 4;
    public static final int CLASS_ARTILLERY = 5;
    public static final int CLASS_PLASMA = 6;
    public static final int CLASS_AC = 7;
    public static final int CLASS_LBX_AC = 8;
    public static final int CLASS_LRM = 9;
    public static final int CLASS_SRM = 10;
    public static final int CLASS_MRM = 11;
    public static final int CLASS_MML = 12;
    public static final int CLASS_ATM = 13;
    public static final int CLASS_ROCKET_LAUNCHER = 14;
    public static final int CLASS_CAPITAL_LASER = 15;
    public static final int CLASS_CAPITAL_PPC = 16;
    public static final int CLASS_CAPITAL_AC = 17;
    public static final int CLASS_CAPITAL_GAUSS = 18;
    public static final int CLASS_CAPITAL_MISSILE = 19;
    public static final int CLASS_AR10 = 20;
    public static final int CLASS_SCREEN = 21;
    public static final int CLASS_SUB_CAPITAL_CANNON = 22;
    public static final int CLASS_CAPITAL_MD = 23;
    public static final int NUM_CLASSES = 24;

    public static final int WEAPON_DIRECT_FIRE = 0;
    public static final int WEAPON_CLUSTER_BALLISTIC = 1;
    public static final int WEAPON_PULSE = 2;
    public static final int WEAPON_CLUSTER_MISSILE = 3;
    public static final int WEAPON_CLUSTER_MISSILE_1D6 = 4;
    public static final int WEAPON_CLUSTER_MISSILE_2D6 = 5;
    public static final int WEAPON_CLUSTER_MISSILE_3D6 = 6;
    public static final int WEAPON_BURST_HALFD6 = 7;
    public static final int WEAPON_BURST_1D6 = 8;
    public static final int WEAPON_BURST_2D6 = 9;
    public static final int WEAPON_BURST_3D6 = 10;
    public static final int WEAPON_BURST_4D6 = 11;
    public static final int WEAPON_BURST_5D6 = 12;
    public static final int WEAPON_BURST_6D6 = 13;
    public static final int WEAPON_BURST_7D6 = 14;
    // Used for BA vs BA damage for BA Plasma Rifle
    public static final int WEAPON_PLASMA = 15;


    public static String[] classNames =
        { "Unknown", "Laser", "Point Defense", "PPC", "Pulse Laser", "Artilery", "AMS", "AC", "LBX", "LRM", "SRM", "MRM", "ATM", "Rocket Launcher", "Capital Laser", "Capital PPC", "Capital AC", "Capital Gauss", "Capital Missile", "AR10", "Screen", "Sub Capital Cannon" };

    // protected RangeType rangeL;
    protected int heat;
    protected int damage;
    protected int damageShort;
    protected int damageMedium;
    protected int damageLong;
    protected int explosionDamage = 0;

    public int rackSize; // or AC size, or whatever
    public int ammoType;

    public int minimumRange;
    public int shortRange;
    public int mediumRange;
    public int longRange;
    public int extremeRange;
    public int waterShortRange;
    public int waterMediumRange;
    public int waterLongRange;
    public int waterExtremeRange;

    //the class of weapon for infantry damage
    public int infDamageClass = WEAPON_DIRECT_FIRE;
    /**
     *  Used for the BA vs BA damage rules on TO pg 109.  Determines how much
     *  damage a weapon will inflict on BA, where the default WEAPON_DIRECT_FIRE
     *  indicates normal weapon damage. 
     */
    protected int baDamageClass = WEAPON_DIRECT_FIRE;

    // get stuff for AT2
    // separate attack value by range. It will make weapon bays easier
    public double shortAV = 0.0;
    public double medAV = 0;
    public double longAV = 0;
    public double extAV = 0;
    public int maxRange = RANGE_SHORT;
    public boolean capital = false;
    public boolean subCapital = false;
    public int atClass = CLASS_NONE;

    public void setDamage(int inD) {
        damage = inD;
    }

    public void setName(String inN) {
        name = inN;
        setInternalName(inN);
    }

    public void setMinimumRange(int inMR) {
        minimumRange = inMR;
    }

    public void setRanges(int sho, int med, int lon, int ext) {
        shortRange = sho;
        mediumRange = med;
        longRange = lon;
        extremeRange = ext;
    }

    public void setWaterRanges(int sho, int med, int lon, int ext) {
        waterShortRange = sho;
        waterMediumRange = med;
        waterLongRange = lon;
        waterExtremeRange = ext;
    }

    public void setAmmoType(int inAT) {
        ammoType = inAT;
    }

    public void setRackSize(int inRS) {
        rackSize = inRS;
    }

    public WeaponType() {
    }

    public int getHeat() {
        return heat;
    }

    public int getFireTN() {
        if (hasFlag(F_NO_FIRES)) {
            return TargetRoll.IMPOSSIBLE;
        } else if (hasFlag(F_FLAMER)) {
            return 4;
        } else if (hasFlag(F_PLASMA)) {
            return 2;
        } else if (hasFlag(F_PLASMA_MFUK)) {
            return 2;
        } else if (hasFlag(F_INFERNO)) {
            return 2;
        } else if (hasFlag(F_INCENDIARY_NEEDLES)) {
            return 6;
        } else if (hasFlag(F_PPC) || hasFlag(F_LASER)) {
            return 7;
        } else {
            return 9;
        }
    }

    public int getDamage(int range) {
        return damage;
    }

    public int getDamage() {
        return damage;
    }

    public int getRackSize() {
        return rackSize;
    }

    public int getAmmoType() {
        return ammoType;
    }

    public int[] getRanges(Mounted weapon) {
        // modify the ranges for ATM missile systems based on the ammo selected
        // TODO: this is not the right place to hardcode these
        int minRange = getMinimumRange();
        int sRange = getShortRange();
        int mRange = getMediumRange();
        int lRange = getLongRange();
        int eRange = getExtremeRange();
        if (getAmmoType() == AmmoType.T_ATM) {
            AmmoType atype = (AmmoType) weapon.getLinked().getType();
            if ((atype.getAmmoType() == AmmoType.T_ATM) && (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE)) {
                minRange = 4;
                sRange = 9;
                mRange = 18;
                lRange = 27;
                eRange = 36;
            } else if ((atype.getAmmoType() == AmmoType.T_ATM) && (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)) {
                minRange = 0;
                sRange = 3;
                mRange = 6;
                lRange = 9;
                eRange = 12;
            }
        }
        if (getAmmoType() == AmmoType.T_IATM) {
            AmmoType atype = (AmmoType) weapon.getLinked().getType();
            if ((atype.getAmmoType() == AmmoType.T_IATM) && (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE)) {
                minRange = 4;
                sRange = 9;
                mRange = 18;
                lRange = 27;
                eRange = 36;
            } else if ((atype.getAmmoType() == AmmoType.T_IATM) && ((atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)||(atype.getMunitionType() == AmmoType.M_IATM_IMP))) {
                minRange = 0;
                sRange = 3;
                mRange = 6;
                lRange = 9;
                eRange = 12;
            }
        }
        if (getAmmoType() == AmmoType.T_MML) {
            AmmoType atype = (AmmoType) weapon.getLinked().getType();
            if (atype.hasFlag(AmmoType.F_MML_LRM) || (getAmmoType() == AmmoType.T_LRM_TORPEDO)) {
                minRange = 6;
                sRange = 7;
                mRange = 14;
                lRange = 21;
                eRange = 28;
            } else {
                minRange = 0;
                sRange = 3;
                mRange = 6;
                lRange = 9;
                eRange = 12;
            }
        }
        int[] weaponRanges =
            { minRange, sRange, mRange, lRange, eRange };
        return weaponRanges;
    }

    public int getMinimumRange() {
        return minimumRange;
    }

    public int getShortRange() {
        return shortRange;
    }

    public int getMediumRange() {
        return mediumRange;
    }

    public int getLongRange() {
        return longRange;
    }

    public int getExtremeRange() {
        return extremeRange;
    }

    public int[] getWRanges() {
        return new int[]
            { minimumRange, waterShortRange, waterMediumRange, waterLongRange, waterExtremeRange };
    }

    public int getWShortRange() {
        return waterShortRange;
    }

    public int getWMediumRange() {
        return waterMediumRange;
    }

    public int getWLongRange() {
        return waterLongRange;
    }

    public int getWExtremeRange() {
        return waterExtremeRange;
    }

    public int getMaxRange(Mounted weapon) {
        if(null != weapon) {
            if (getAmmoType() == AmmoType.T_ATM) {
                AmmoType atype = (AmmoType) weapon.getLinked().getType();
                if ((atype.getAmmoType() == AmmoType.T_ATM) && (atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE)) {
                    return RANGE_EXT;
                } else if ((atype.getAmmoType() == AmmoType.T_ATM) && (atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)) {
                    return RANGE_SHORT;
                }
            }
            if (getAmmoType() == AmmoType.T_MML) {
                AmmoType atype = (AmmoType) weapon.getLinked().getType();
                if (atype.hasFlag(AmmoType.F_MML_LRM) || (getAmmoType() == AmmoType.T_LRM_TORPEDO)) {
                    return RANGE_LONG;
                } else {
                    return RANGE_SHORT;
                }
            }
        }
        return maxRange;
    }

    public int getInfantryDamageClass() {
        return infDamageClass;
    }
    
    public int getBADamageClass() {
        return baDamageClass;
    }

    public int[] getATRanges() {
        if (capital) {
            return new int[]
                { Integer.MIN_VALUE, 12, 24, 40, 50 };
        }
        return new int[]
            { Integer.MIN_VALUE, 6, 12, 20, 25 };
    }

    public double getShortAV() {
        return shortAV;
    }

    public int getRoundShortAV() {
        return (int) Math.ceil(shortAV);
    }

    public double getMedAV() {
        return medAV;
    }

    public int getRoundMedAV() {
        return (int) Math.ceil(medAV);
    }

    public double getLongAV() {
        return longAV;
    }

    public int getRoundLongAV() {
        return (int) Math.ceil(longAV);
    }

    public double getExtAV() {
        return extAV;
    }

    public int getRoundExtAV() {
        return (int) Math.ceil(extAV);
    }

    public boolean isCapital() {
        return capital;
    }

    public boolean isSubCapital() {
        return subCapital;
    }

    public int getAtClass() {
        return atClass;
    }

    // Probably not the best place for this
    public EquipmentType getBayType() {
        // return the correct weapons bay for the given type of weapon
        switch (atClass) {
            case (CLASS_LASER):
                return EquipmentType.get("Laser Bay");
            case (CLASS_POINT_DEFENSE):
                return EquipmentType.get("Point Defense Bay");
            case (CLASS_PPC):
                return EquipmentType.get("PPC Bay");
            case (CLASS_PULSE_LASER):
                return EquipmentType.get("Pulse Laser Bay");
            case (CLASS_ARTILLERY):
                return EquipmentType.get("Artillery Bay");
            case (CLASS_PLASMA):
                return EquipmentType.get("Plasma Bay");
            case (CLASS_AC):
                return EquipmentType.get("AC Bay");
            case (CLASS_LBX_AC):
                return EquipmentType.get("LBX AC Bay");
            case (CLASS_LRM):
                return EquipmentType.get("LRM Bay");
            case (CLASS_SRM):
                return EquipmentType.get("SRM Bay");
            case (CLASS_MRM):
                return EquipmentType.get("MRM Bay");
            case (CLASS_MML):
                return EquipmentType.get("MML Bay");
            case (CLASS_ATM):
                return EquipmentType.get("ATM Bay");
            case (CLASS_ROCKET_LAUNCHER):
                return EquipmentType.get("Rocket Launcher Bay");
            case (CLASS_CAPITAL_LASER):
                if (subCapital) {
                    return EquipmentType.get("Sub-Capital Laser Bay");
                }
                return EquipmentType.get("Capital Laser Bay");
            case (CLASS_CAPITAL_PPC):
                return EquipmentType.get("Capital PPC Bay");
            case (CLASS_CAPITAL_AC):
                if (subCapital) {
                    return EquipmentType.get("Sub-Capital Cannon Bay");
                }
                return EquipmentType.get("Capital AC Bay");
            case (CLASS_CAPITAL_GAUSS):
                return EquipmentType.get("Capital Gauss Bay");
            case (CLASS_CAPITAL_MD):
                return EquipmentType.get("Capital Mass Driver Bay");
            case (CLASS_CAPITAL_MISSILE):
                return EquipmentType.get("Capital Missile Bay");
            case (CLASS_AR10):
                return EquipmentType.get("AR10 Bay");
            case (CLASS_SCREEN):
                return EquipmentType.get("Screen Launcher Bay");
            default:
                return EquipmentType.get("Misc Bay");
        }
    }

    /**
     * Add all the types of weapons we can create to the list
     */
    public static void initializeTypes() {
        // Laser types
        EquipmentType.addType(new ISMediumLaser());
        EquipmentType.addType(new ISMediumLaserPrimitive());
        EquipmentType.addType(new ISLargeLaser());
        EquipmentType.addType(new ISLargeLaserPrimitive());
        EquipmentType.addType(new ISSmallLaser());
        EquipmentType.addType(new ISSmallLaserPrimitive());
        EquipmentType.addType(new ISLargePulseLaser());
        EquipmentType.addType(new ISLargePulseLaserPrototype());
        EquipmentType.addType(new ISLargeXPulseLaser());
        EquipmentType.addType(new ISERLargeLaser());
        EquipmentType.addType(new ISERLargeLaserPrototype());
        EquipmentType.addType(new ISERMediumLaser());
        EquipmentType.addType(new ISMediumPulseLaser());
        EquipmentType.addType(new ISMediumPulseLaserPrototype());
        EquipmentType.addType(new ISMediumXPulseLaser());
        EquipmentType.addType(new ISSmallPulseLaser());
        EquipmentType.addType(new ISSmallXPulseLaser());
        EquipmentType.addType(new ISSmallPulseLaserPrototype());
        EquipmentType.addType(new ISERSmallLaser());
        EquipmentType.addType(new ISMediumVariableSpeedPulseLaser());
        EquipmentType.addType(new ISSmallVariableSpeedPulseLaser());
        EquipmentType.addType(new ISLargeVariableSpeedPulseLaser());
        EquipmentType.addType(new ISBinaryLaserCannon());
        EquipmentType.addType(new ISBombastLaser());
        EquipmentType.addType(new CLERLargeLaser());
        EquipmentType.addType(new CLHeavyLargeLaser());
        EquipmentType.addType(new CLLargePulseLaser());
        EquipmentType.addType(new CLERLargePulseLaser());
        EquipmentType.addType(new CLERMediumLaser());
        EquipmentType.addType(new CLERMediumLaserPrototype());
        EquipmentType.addType(new CLHeavyMediumLaser());
        EquipmentType.addType(new CLMediumPulseLaser());
        EquipmentType.addType(new CLERMediumPulseLaser());
        EquipmentType.addType(new CLERSmallLaser());
        EquipmentType.addType(new CLERSmallLaserPrototype());
        EquipmentType.addType(new CLSmallPulseLaser());
        EquipmentType.addType(new CLERSmallPulseLaser());
        EquipmentType.addType(new CLHeavySmallLaser());
        EquipmentType.addType(new CLERMicroLaser());
        EquipmentType.addType(new CLMicroPulseLaser());
        EquipmentType.addType(new CLImprovedHeavyLargeLaser());
        EquipmentType.addType(new CLImprovedHeavyMediumLaser());
        EquipmentType.addType(new CLImprovedHeavySmallLaser());
        EquipmentType.addType(new CLLargeChemicalLaser());
        EquipmentType.addType(new CLMediumChemicalLaser());
        EquipmentType.addType(new CLSmallChemicalLaser());
        // PPC types
        EquipmentType.addType(new ISPPC());
        EquipmentType.addType(new ISPPCPrimitive());
        EquipmentType.addType(new ISERPPC());
        EquipmentType.addType(new ISEHERPPC());
        EquipmentType.addType(new CLERPPC());
        EquipmentType.addType(new ISSnubNosePPC());
        EquipmentType.addType(new ISLightPPC());
        EquipmentType.addType(new ISHeavyPPC());
        EquipmentType.addType(new ISHERPPC());
        EquipmentType.addType(new ISBASupportPPC());
        EquipmentType.addType(new CLBASupportPPC());
        // Flamers
        EquipmentType.addType(new CLFlamer());
        EquipmentType.addType(new ISFlamer());
        EquipmentType.addType(new CLVehicleFlamer());
        EquipmentType.addType(new ISVehicleFlamer());
        EquipmentType.addType(new CLHeavyFlamer());
        EquipmentType.addType(new ISHeavyFlamer());
        EquipmentType.addType(new ISERFlamer());
        EquipmentType.addType(new CLERFlamer());
        // Autocannons
        EquipmentType.addType(new ISAC2());
        EquipmentType.addType(new ISAC5());
        EquipmentType.addType(new ISAC10());
        EquipmentType.addType(new ISAC20());
        EquipmentType.addType(new CLProtoMechAC2());
        EquipmentType.addType(new CLProtoMechAC4());
        EquipmentType.addType(new CLProtoMechAC8());
        EquipmentType.addType(new ISAC2Primitive());
        EquipmentType.addType(new ISAC5Primitive());
        EquipmentType.addType(new ISAC10Primitive());
        EquipmentType.addType(new ISAC20Primitive());
        // Ultras
        EquipmentType.addType(new ISUAC2());
        EquipmentType.addType(new ISUAC5());
        EquipmentType.addType(new ISUAC5Prototype());
        EquipmentType.addType(new ISUAC10());
        EquipmentType.addType(new ISUAC20());
        EquipmentType.addType(new ISTHBUAC2());
        EquipmentType.addType(new ISTHBUAC10());
        EquipmentType.addType(new ISTHBUAC20());
        EquipmentType.addType(new CLUAC2());
        EquipmentType.addType(new CLUAC2Prototype());
        EquipmentType.addType(new CLUAC5());
        EquipmentType.addType(new CLUAC10());
        EquipmentType.addType(new CLUAC10Prototype());
        EquipmentType.addType(new CLUAC20());
        EquipmentType.addType(new CLUAC20Prototype());
        // LBXs
        EquipmentType.addType(new ISLB2XAC());
        EquipmentType.addType(new ISLB5XAC());
        EquipmentType.addType(new ISLB10XAC());
        EquipmentType.addType(new ISLB10XACPrototype());
        EquipmentType.addType(new ISLB20XAC());
        EquipmentType.addType(new CLLB2XAC());
        EquipmentType.addType(new CLLB2XACPrototype());
        EquipmentType.addType(new CLLB5XAC());
        EquipmentType.addType(new CLLB5XACPrototype());
        EquipmentType.addType(new CLLB10XAC());
        EquipmentType.addType(new CLLB20XAC());
        EquipmentType.addType(new CLLB20XACPrototype());
        EquipmentType.addType(new ISTHBLB2XAC());
        EquipmentType.addType(new ISTHBLB5XAC());
        EquipmentType.addType(new ISTHBLB20XAC());
        // RACs
        EquipmentType.addType(new ISRAC2());
        EquipmentType.addType(new ISRAC5());
        // LACs
        EquipmentType.addType(new ISLAC2());
        EquipmentType.addType(new ISLAC5());
        EquipmentType.addType(new ISLAC10());
        EquipmentType.addType(new ISLAC20());
        // HVACs
        EquipmentType.addType(new ISHVAC2());
        EquipmentType.addType(new ISHVAC5());
        EquipmentType.addType(new ISHVAC10());
        // Gausses
        EquipmentType.addType(new ISGaussRifle());
        EquipmentType.addType(new ISGaussRiflePrototype());
        EquipmentType.addType(new ISSilverBulletGauss());
        EquipmentType.addType(new CLGaussRifle());
        EquipmentType.addType(new ISLGaussRifle());
        EquipmentType.addType(new ISHGaussRifle());
        EquipmentType.addType(new ISIHGaussRifle());
        EquipmentType.addType(new CLHAG20());
        EquipmentType.addType(new CLHAG30());
        EquipmentType.addType(new CLHAG40());
        EquipmentType.addType(new CLAPGaussRifle());
        // MGs
        EquipmentType.addType(new ISMG());
        EquipmentType.addType(new ISLightMG());
        EquipmentType.addType(new ISHeavyMG());
        EquipmentType.addType(new ISMGA());
        EquipmentType.addType(new ISLightMGA());
        EquipmentType.addType(new ISHeavyMGA());
        EquipmentType.addType(new CLMG());
        EquipmentType.addType(new CLLightMG());
        EquipmentType.addType(new CLHeavyMG());
        EquipmentType.addType(new CLMGA());
        EquipmentType.addType(new CLLightMGA());
        EquipmentType.addType(new CLHeavyMGA());
        // LRMs
        EquipmentType.addType(new ISLRM1());
        EquipmentType.addType(new ISLRM1OS());
        EquipmentType.addType(new ISLRM2());
        EquipmentType.addType(new ISLRM2OS());
        EquipmentType.addType(new ISLRM3());
        EquipmentType.addType(new ISLRM3OS());
        EquipmentType.addType(new ISLRM4());
        EquipmentType.addType(new ISLRM4OS());
        EquipmentType.addType(new ISLRM5());
        EquipmentType.addType(new ISLRM10());
        EquipmentType.addType(new ISLRM15());
        EquipmentType.addType(new ISLRM20());
        EquipmentType.addType(new ISLRM5OS());
        EquipmentType.addType(new ISLRM10OS());
        EquipmentType.addType(new ISLRM15OS());
        EquipmentType.addType(new ISLRM20OS());
        EquipmentType.addType(new CLLRM1());
        EquipmentType.addType(new CLLRM1OS());
        EquipmentType.addType(new CLLRM2());
        EquipmentType.addType(new CLLRM2OS());
        EquipmentType.addType(new CLLRM3());
        EquipmentType.addType(new CLLRM3OS());
        EquipmentType.addType(new CLLRM4());
        EquipmentType.addType(new CLLRM4OS());
        EquipmentType.addType(new CLLRM5());
        EquipmentType.addType(new CLLRM6());
        EquipmentType.addType(new CLLRM7());
        EquipmentType.addType(new CLLRM8());
        EquipmentType.addType(new CLLRM9());
        EquipmentType.addType(new CLLRM10());
        EquipmentType.addType(new CLLRM11());
        EquipmentType.addType(new CLLRM12());
        EquipmentType.addType(new CLLRM13());
        EquipmentType.addType(new CLLRM14());
        EquipmentType.addType(new CLLRM15());
        EquipmentType.addType(new CLLRM16());
        EquipmentType.addType(new CLLRM17());
        EquipmentType.addType(new CLLRM18());
        EquipmentType.addType(new CLLRM19());
        EquipmentType.addType(new CLLRM20());
        EquipmentType.addType(new CLLRM5OS());
        EquipmentType.addType(new CLLRM10OS());
        EquipmentType.addType(new CLLRM15OS());
        EquipmentType.addType(new CLLRM20OS());
        
        EquipmentType.addType(new CLStreakLRM1());
        EquipmentType.addType(new CLStreakLRM2());
        EquipmentType.addType(new CLStreakLRM3());
        EquipmentType.addType(new CLStreakLRM4());
        EquipmentType.addType(new CLStreakLRM5());
        EquipmentType.addType(new CLStreakLRM6());
        EquipmentType.addType(new CLStreakLRM7());
        EquipmentType.addType(new CLStreakLRM8());
        EquipmentType.addType(new CLStreakLRM9());
        EquipmentType.addType(new CLStreakLRM10());
        EquipmentType.addType(new CLStreakLRM11());
        EquipmentType.addType(new CLStreakLRM12());
        EquipmentType.addType(new CLStreakLRM13());
        EquipmentType.addType(new CLStreakLRM14());
        EquipmentType.addType(new CLStreakLRM15());
        EquipmentType.addType(new CLStreakLRM16());
        EquipmentType.addType(new CLStreakLRM17());
        EquipmentType.addType(new CLStreakLRM18());
        EquipmentType.addType(new CLStreakLRM19());
        EquipmentType.addType(new CLStreakLRM20());
        EquipmentType.addType(new CLStreakLRM1OS());
        EquipmentType.addType(new CLStreakLRM2OS());
        EquipmentType.addType(new CLStreakLRM3OS());
        EquipmentType.addType(new CLStreakLRM4OS());
        EquipmentType.addType(new CLStreakLRM5OS());
        EquipmentType.addType(new CLStreakLRM6OS());
        EquipmentType.addType(new CLStreakLRM7OS());
        EquipmentType.addType(new CLStreakLRM8OS());
        EquipmentType.addType(new CLStreakLRM9OS());
        EquipmentType.addType(new CLStreakLRM10OS());
        EquipmentType.addType(new CLStreakLRM11OS());
        EquipmentType.addType(new CLStreakLRM12OS());
        EquipmentType.addType(new CLStreakLRM13OS());
        EquipmentType.addType(new CLStreakLRM14OS());
        EquipmentType.addType(new CLStreakLRM15OS());
        EquipmentType.addType(new CLStreakLRM16OS());
        EquipmentType.addType(new CLStreakLRM17OS());
        EquipmentType.addType(new CLStreakLRM18OS());
        EquipmentType.addType(new CLStreakLRM19OS());
        EquipmentType.addType(new CLStreakLRM20OS());
        EquipmentType.addType(new ISExtendedLRM5());
        EquipmentType.addType(new ISExtendedLRM10());
        EquipmentType.addType(new ISExtendedLRM15());
        EquipmentType.addType(new ISExtendedLRM20());
        EquipmentType.addType(new ISEnhancedLRM5());
        EquipmentType.addType(new ISEnhancedLRM10());
        EquipmentType.addType(new ISEnhancedLRM15());
        EquipmentType.addType(new ISEnhancedLRM20());
        EquipmentType.addType(new ISLRM5Primitive());
        EquipmentType.addType(new ISLRM10Primitive());
        EquipmentType.addType(new ISLRM15Primitive());
        EquipmentType.addType(new ISLRM20Primitive());
        // LRTs
        EquipmentType.addType(new ISLRT5());
        EquipmentType.addType(new ISLRT10());
        EquipmentType.addType(new ISLRT15());
        EquipmentType.addType(new ISLRT20());
        EquipmentType.addType(new ISLRT5OS());
        EquipmentType.addType(new ISLRT10OS());
        EquipmentType.addType(new ISLRT15OS());
        EquipmentType.addType(new ISLRT20OS());

        EquipmentType.addType(new CLLRT1());
        EquipmentType.addType(new CLLRT2());
        EquipmentType.addType(new CLLRT3());
        EquipmentType.addType(new CLLRT4());
        EquipmentType.addType(new CLLRT5());
        EquipmentType.addType(new CLLRT6());
        EquipmentType.addType(new CLLRT7());
        EquipmentType.addType(new CLLRT8());
        EquipmentType.addType(new CLLRT9());
        EquipmentType.addType(new CLLRT10());
        EquipmentType.addType(new CLLRT11());
        EquipmentType.addType(new CLLRT12());
        EquipmentType.addType(new CLLRT13());
        EquipmentType.addType(new CLLRT14());
        EquipmentType.addType(new CLLRT15());
        EquipmentType.addType(new CLLRT16());
        EquipmentType.addType(new CLLRT17());
        EquipmentType.addType(new CLLRT18());
        EquipmentType.addType(new CLLRT19());
        EquipmentType.addType(new CLLRT20());
        EquipmentType.addType(new CLLRT5OS());
        EquipmentType.addType(new CLLRT10OS());
        EquipmentType.addType(new CLLRT15OS());
        EquipmentType.addType(new CLLRT20OS());
        // SRMs
        EquipmentType.addType(new ISSRM1());
        EquipmentType.addType(new ISSRM2());
        EquipmentType.addType(new ISSRM3());
        EquipmentType.addType(new ISSRM4());
        EquipmentType.addType(new ISSRM5());
        EquipmentType.addType(new ISSRM6());
        EquipmentType.addType(new ISSRM1OS());
        EquipmentType.addType(new ISSRM2OS());
        EquipmentType.addType(new ISSRM3OS());
        EquipmentType.addType(new ISSRM4OS());
        EquipmentType.addType(new ISSRM5OS());
        EquipmentType.addType(new ISSRM6OS());
        EquipmentType.addType(new CLSRM1());
        EquipmentType.addType(new CLSRM1OS());
        EquipmentType.addType(new CLSRM2());
        EquipmentType.addType(new CLSRM3());
        EquipmentType.addType(new CLSRM3OS());
        EquipmentType.addType(new CLSRM4());
        EquipmentType.addType(new CLSRM5());
        EquipmentType.addType(new CLSRM5OS());
        EquipmentType.addType(new CLSRM6());
        EquipmentType.addType(new CLSRM2OS());
        EquipmentType.addType(new CLSRM4OS());
        EquipmentType.addType(new CLSRM6OS());
        EquipmentType.addType(new ISStreakSRM2());
        EquipmentType.addType(new ISStreakSRM4());
        EquipmentType.addType(new ISStreakSRM6());
        EquipmentType.addType(new ISStreakSRM2OS());
        EquipmentType.addType(new ISStreakSRM4OS());
        EquipmentType.addType(new ISStreakSRM6OS());
        EquipmentType.addType(new CLStreakSRM1());
        EquipmentType.addType(new CLStreakSRM2());
        EquipmentType.addType(new CLStreakSRM3());
        EquipmentType.addType(new CLStreakSRM4());
        EquipmentType.addType(new CLStreakSRM4Prototype());
        EquipmentType.addType(new CLStreakSRM5());
        EquipmentType.addType(new CLStreakSRM6());
        EquipmentType.addType(new CLStreakSRM6Prototype());
        EquipmentType.addType(new CLStreakSRM2OS());
        EquipmentType.addType(new CLStreakSRM4OS());
        EquipmentType.addType(new CLStreakSRM6OS());
        EquipmentType.addType(new ISSRM2Primitive());
        EquipmentType.addType(new ISSRM4Primitive());
        EquipmentType.addType(new ISSRM6Primitive());
        // SRTs
        EquipmentType.addType(new ISSRT2());
        EquipmentType.addType(new ISSRT4());
        EquipmentType.addType(new ISSRT6());
        EquipmentType.addType(new ISSRT2OS());
        EquipmentType.addType(new ISSRT4OS());
        EquipmentType.addType(new ISSRT6OS());
        EquipmentType.addType(new CLSRT2());
        EquipmentType.addType(new CLSRT4());
        EquipmentType.addType(new CLSRT6());
        EquipmentType.addType(new CLSRT2OS());
        EquipmentType.addType(new CLSRT4OS());
        EquipmentType.addType(new CLSRT6OS());
        // RLs
        EquipmentType.addType(new ISRL1());
        EquipmentType.addType(new ISRL2());
        EquipmentType.addType(new ISRL3());
        EquipmentType.addType(new ISRL4());
        EquipmentType.addType(new ISRL5());
        EquipmentType.addType(new ISRL10());
        EquipmentType.addType(new ISRL15());
        EquipmentType.addType(new ISPrimRL15());
        EquipmentType.addType(new ISRL20());
        EquipmentType.addType(new CLPrototypeRL10());
        EquipmentType.addType(new CLPrototypeRL15());
        EquipmentType.addType(new CLPrototypeRL20());
        // ATMs
        EquipmentType.addType(new CLATM3());
        EquipmentType.addType(new CLATM6());
        EquipmentType.addType(new CLATM9());
        EquipmentType.addType(new CLATM12());
        // iATMs
        EquipmentType.addType(new CLIATM3());
        EquipmentType.addType(new CLIATM6());
        EquipmentType.addType(new CLIATM9());
        EquipmentType.addType(new CLIATM12());
        // MRMs
        EquipmentType.addType(new ISMRM1());
        EquipmentType.addType(new ISMRM2());
        EquipmentType.addType(new ISMRM3());
        EquipmentType.addType(new ISMRM4());
        EquipmentType.addType(new ISMRM5());
        EquipmentType.addType(new ISMRM1OS());
        EquipmentType.addType(new ISMRM2OS());
        EquipmentType.addType(new ISMRM3OS());
        EquipmentType.addType(new ISMRM4OS());
        EquipmentType.addType(new ISMRM5OS());
        EquipmentType.addType(new ISMRM10());
        EquipmentType.addType(new ISMRM20());
        EquipmentType.addType(new ISMRM30());
        EquipmentType.addType(new ISMRM40());
        EquipmentType.addType(new ISMRM10OS());
        EquipmentType.addType(new ISMRM20OS());
        EquipmentType.addType(new ISMRM30OS());
        EquipmentType.addType(new ISMRM40OS());
        // NARCs
        EquipmentType.addType(new ISNarc());
        EquipmentType.addType(new ISNarcOS());
        EquipmentType.addType(new ISNarcIOS());
        EquipmentType.addType(new CLNarc());
        EquipmentType.addType(new CLNarcOS());
        EquipmentType.addType(new CLNarcIOS());
        EquipmentType.addType(new ISImprovedNarc());
        EquipmentType.addType(new ISImprovedNarcOS());
        // AMSs
        EquipmentType.addType(new ISAMS());
        EquipmentType.addType(new ISLaserAMS());
        EquipmentType.addType(new ISLaserAMSTHB());
        EquipmentType.addType(new CLAMS());
        EquipmentType.addType(new CLLaserAMS());
        // TAGs
        EquipmentType.addType(new ISLightTAG());
        EquipmentType.addType(new ISTAG());
        EquipmentType.addType(new ISC3M());
        EquipmentType.addType(new ISC3MBS());
        EquipmentType.addType(new CLLightTAG());
        EquipmentType.addType(new CLTAG());
        EquipmentType.addType(new ISBALightTAG());
        EquipmentType.addType(new CLBALightTAG());
        // MMLs
        EquipmentType.addType(new ISMML3());
        EquipmentType.addType(new ISMML5());
        EquipmentType.addType(new ISMML7());
        EquipmentType.addType(new ISMML9());
        // Arty
        EquipmentType.addType(new ISLongTom());
        EquipmentType.addType(new ISThumper());
        EquipmentType.addType(new ISSniper());
        EquipmentType.addType(new ISArrowIV());
        EquipmentType.addType(new CLLongTom());
        EquipmentType.addType(new CLSniper());
        EquipmentType.addType(new CLThumper());
        EquipmentType.addType(new CLArrowIV());
        EquipmentType.addType(new ISBATubeArtillery());
        // Arty Cannons
        EquipmentType.addType(new ISLongTomCannon());
        EquipmentType.addType(new ISThumperCannon());
        EquipmentType.addType(new ISSniperCannon());
        EquipmentType.addType(new CLLongTomCannon());
        EquipmentType.addType(new CLSniperCannon());
        EquipmentType.addType(new CLThumperCannon());
        // MFUK weapons
        EquipmentType.addType(new CLPlasmaRifle());
        EquipmentType.addType(new CLRAC2());
        EquipmentType.addType(new CLRAC5());
        EquipmentType.addType(new CLRAC10());
        EquipmentType.addType(new CLRAC20());
        // misc lvl3 stuff
        EquipmentType.addType(new ISRailGun());
        EquipmentType.addType(new ISFluidGun());
        EquipmentType.addType(new CLFluidGun());
        EquipmentType.addType(new ISCenturionWeaponSystem());
        // MapPack Solaris VII
        EquipmentType.addType(new ISMagshotGaussRifle());
        EquipmentType.addType(new ISMPod());
        EquipmentType.addType(new CLMPod());
        EquipmentType.addType(new ISBPod());
        EquipmentType.addType(new CLBPod());
        // Thunderbolts
        EquipmentType.addType(new ISThunderBolt5());
        EquipmentType.addType(new ISThunderBolt10());
        EquipmentType.addType(new ISThunderBolt15());
        EquipmentType.addType(new ISThunderBolt20());
        // Taser
        EquipmentType.addType(new ISMekTaser());

        EquipmentType.addType(new ISNailGun());
        EquipmentType.addType(new ISRivetGun());
        EquipmentType.addType(new CLNailGun());
        EquipmentType.addType(new CLRivetGun());

        // rifles
        EquipmentType.addType(new ISLightRifle());
        EquipmentType.addType(new ISMediumRifle());
        EquipmentType.addType(new ISHeavyRifle());

        // VGLs
        EquipmentType.addType(new ISVehicularGrenadeLauncher());
        EquipmentType.addType(new CLVehicularGrenadeLauncher());

        EquipmentType.addType(new ISC3RemoteSensorLauncher());

        // Infantry Attacks
        EquipmentType.addType(new LegAttack());
        EquipmentType.addType(new SwarmAttack());
        EquipmentType.addType(new SwarmWeaponAttack());
        EquipmentType.addType(new StopSwarmAttack());

        // Infantry Level 1 Weapons
        EquipmentType.addType(new InfantryRifleLaserWeapon());
        EquipmentType.addType(new InfantrySupportLRMWeapon());
        EquipmentType.addType(new InfantrySupportLightSRMInfernoWeapon());
        EquipmentType.addType(new InfantrySupportPortableFlamerWeapon());
        EquipmentType.addType(new InfantryTWFlamerWeapon());
        
        // Infantry Archaic Weapons
        EquipmentType.addType(new InfantryArchaicAxeWeapon());
        EquipmentType.addType(new InfantryArchaicBasicCrossbowWeapon());
        EquipmentType.addType(new InfantryArchaicBlackjackWeapon());
        EquipmentType.addType(new InfantryArchaicBokkenWeapon());
        EquipmentType.addType(new InfantryArchaicCarbonReinforcedNailsWeapon());
        EquipmentType.addType(new InfantryArchaicClanVibroSwordWeapon());
        EquipmentType.addType(new InfantryArchaicCompoundBowWeapon());
        EquipmentType.addType(new InfantryArchaicDaggerWeapon());
        EquipmentType.addType(new InfantryArchaicDaikyuBowWeapon());
        EquipmentType.addType(new InfantryArchaicDaoWeapon());
        EquipmentType.addType(new InfantryArchaicDoubleStunStaffWeapon());
        EquipmentType.addType(new InfantryArchaicHankyuBowWeapon());
        EquipmentType.addType(new InfantryArchaicHatchetWeapon());
        EquipmentType.addType(new InfantryArchaicHeavyCrossbowWeapon());
        EquipmentType.addType(new InfantryArchaicImprovisedClubWeapon());
        EquipmentType.addType(new InfantryArchaicKatanaWeapon());
        EquipmentType.addType(new InfantryArchaicLongBowWeapon());
        EquipmentType.addType(new InfantryArchaicMedusaWhipWeapon());
        EquipmentType.addType(new InfantryArchaicMiniStunstickWeapon());
        EquipmentType.addType(new InfantryArchaicMonowireWeapon());
        EquipmentType.addType(new InfantryArchaicNeuralLashWeapon());
        EquipmentType.addType(new InfantryArchaicNeuralWhipWeapon());
        EquipmentType.addType(new InfantryArchaicNoDachiWeapon());
        EquipmentType.addType(new InfantryArchaicNunchakuWeapon());
        EquipmentType.addType(new InfantryArchaicPolearmWeapon());
        EquipmentType.addType(new InfantryArchaicShortBowWeapon());
        EquipmentType.addType(new InfantryArchaicShurikenWeapon());
        EquipmentType.addType(new InfantryArchaicSingleStunStaffWeapon());
        EquipmentType.addType(new InfantryArchaicStaffWeapon());
        EquipmentType.addType(new InfantryArchaicStunstickWeapon());
        EquipmentType.addType(new InfantryArchaicSwordWeapon());
        EquipmentType.addType(new InfantryArchaicVibroAxeWeapon());
        EquipmentType.addType(new InfantryArchaicVibroBladeWeapon());
        EquipmentType.addType(new InfantryArchaicVibroKatanaWeapon());
        EquipmentType.addType(new InfantryArchaicVibroSwordWeapon());
        EquipmentType.addType(new InfantryArchaicWakizashiWeapon());
        EquipmentType.addType(new InfantryArchaicWhipWeapon());
        EquipmentType.addType(new InfantryArchaicYumiBowWeapon());
        EquipmentType.addType(new InfantryArchaicPrimitiveBowWeapon());
        EquipmentType.addType(new InfantryArchaicBladeArchaicSwordWeapon());
        EquipmentType.addType(new InfantryArchaicBladeZweihanderSwordWeapon());
        EquipmentType.addType(new InfantryArchaicBladeJoustingLanceWeapon());
        EquipmentType.addType(new InfantryArchaicWhipWeapon());
        EquipmentType.addType(new InfantryArchaicShockStaffWeapon());
        
        // Infantry Pistols
        EquipmentType.addType(new InfantryPistolAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolAutoPistolNissanWeapon());
        EquipmentType.addType(new InfantryPistolBlazerPistolWeapon());
        EquipmentType.addType(new InfantryPistolClanERLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolClanGaussPistolWeapon());
        EquipmentType.addType(new InfantryPistolClanPulseLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolCoventryHandrocketGyrojetPistolWeapon());
        EquipmentType.addType(new InfantryPistolDartGunWeapon());
        EquipmentType.addType(new InfantryPistolFlamerPistolWeapon());
        EquipmentType.addType(new InfantryPistolFlarePistolWeapon());
        EquipmentType.addType(new InfantryPistolGyrojetPistolWeapon());
        EquipmentType.addType(new InfantryPistolHawkEagleAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolHoldoutGyrojetPistolWeapon());
        EquipmentType.addType(new InfantryPistolHoldOutLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolHoldoutNeedlerPistolWeapon());
        EquipmentType.addType(new InfantryPistolHoldoutPistolWeapon());
        EquipmentType.addType(new InfantryPistolLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolMagnumRevolverWeapon());
        EquipmentType.addType(new InfantryPistolMakeshiftPistolWeapon());
        EquipmentType.addType(new InfantryPistolMandrakeGaussPistolWeapon());
        EquipmentType.addType(new InfantryPistolMartialEagleMachinePistolWeapon());
        EquipmentType.addType(new InfantryPistolMauserAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolMauserNeedlerPistolWeapon());
        EquipmentType.addType(new InfantryPistolMagnumAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolMydronAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolNakjimaLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolNambuAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolNeedlerPistolWeapon());
        EquipmentType.addType(new InfantryPistolPaintGunPistolWeapon());
        EquipmentType.addType(new InfantryPistolPulseLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolRevolverWeapon());
        EquipmentType.addType(new InfantryPistolSeaEagleNeedlerPistolWeapon());
        EquipmentType.addType(new InfantryPistolSerrekAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolSonicStunnerWeapon());
        EquipmentType.addType(new InfantryPistolSpitballGasPistolWeapon());
        EquipmentType.addType(new InfantryPistolSternsnachtPistolWeapon());
        EquipmentType.addType(new InfantryPistolSternsnachtPythonAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolStettaAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolSunbeamLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolSunbeamNovaLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistolTKEnforcerAutoPistolWeapon());
        EquipmentType.addType(new InfantryPistolTranqGunWeapon());
        EquipmentType.addType(new InfantryPistolWhiteDwarfLaserPistolWeapon());
        EquipmentType.addType(new InfantryPistoMachinePistolSPEC7AWeapon());
        EquipmentType.addType(new InfantryPistolVintageWeapon());

        // Infantry Rifles
        EquipmentType.addType(new InfantryRifleAutoRifleWeapon());
        EquipmentType.addType(new InfantryRifleBlazerRifleWeapon());
        EquipmentType.addType(new InfantryRifleBoltActionSniperWeapon());
        EquipmentType.addType(new InfantryRifleBoltActionWeapon());
        EquipmentType.addType(new InfantryRifleClanERLaserWeapon());
        EquipmentType.addType(new InfantryRifleClanMauserIICIASInfernoWeapon());
        EquipmentType.addType(new InfantryRifleClanMauserIICIASWeapon());
        EquipmentType.addType(new InfantryRifleClanPulseLaserWeapon());
        EquipmentType.addType(new InfantryRifleEbonyAssaultLaserWeapon());
        EquipmentType.addType(new InfantryRifleElephantGunWeapon());
        EquipmentType.addType(new InfantryRifleFederatedBarrettM42BInfernoWeapon());
        EquipmentType.addType(new InfantryRifleFederatedBarrettM42BWeapon());
        EquipmentType.addType(new InfantryRifleFederatedBarrettM61ALaserInfernoWeapon());
        EquipmentType.addType(new InfantryRifleFederatedBarrettM61ALaserWeapon());
        EquipmentType.addType(new InfantryRifleFederatedLongWeapon());
        EquipmentType.addType(new InfantryRifleGyrojetWeapon());
        EquipmentType.addType(new InfantryRifleGyroslugCarbineWeapon());
        EquipmentType.addType(new InfantryRifleGyroslugRifleWeapon());
        EquipmentType.addType(new InfantryRifleHeavyGyrojetGunWeapon());
        EquipmentType.addType(new InfantryRifleImperatorAX22AssaultWeapon());
        EquipmentType.addType(new InfantryRifleIntekLaserWeapon());
        EquipmentType.addType(new InfantryRifleMagnaLaserWeapon());
        EquipmentType.addType(new InfantryRifleMakeshiftWeapon());
        EquipmentType.addType(new InfantryRifleMarxXXLaserWeapon());
        EquipmentType.addType(new InfantryRifleMauser1200LSSWeapon());
        EquipmentType.addType(new InfantryRifleMauser960LaserWeapon());
        EquipmentType.addType(new InfantryRifleMauserG150Weapon());
        EquipmentType.addType(new InfantryRifleMaxellPL10LaserWeapon());
        EquipmentType.addType(new InfantryRifleMGFlechetteNeedlerWeapon());
        EquipmentType.addType(new InfantryRifleMinolta9000Weapon());
        EquipmentType.addType(new InfantryRifleNeedlerWeapon());
        EquipmentType.addType(new InfantryRiflePulseLaserWeapon());
        EquipmentType.addType(new InfantryRifleRadiumLaserSniperWeapon());
        EquipmentType.addType(new InfantryRifleShredderHeavyNeedlerWeapon());
        EquipmentType.addType(new InfantryRifleStarKingGyroslugCarbineWeapon());
        EquipmentType.addType(new InfantryRifleSunbeamStarfireERLaserWeapon());
        EquipmentType.addType(new InfantryRifleThunderstrokeIIWeapon());
        EquipmentType.addType(new InfantryRifleThunderstrokeWeapon());
        EquipmentType.addType(new InfantryRifleTKAssaultWeapon());
        EquipmentType.addType(new InfantryRifleZeusHeavyWeapon());
        EquipmentType.addType(new InfantryRifleVintageWeapon());        
        EquipmentType.addType(new InfantryRifleVSPLaserWeapon());
        
        // Infantry Shotguns
        EquipmentType.addType(new InfantryShotgunAutomaticWeapon());
        EquipmentType.addType(new InfantryShotgunAvengerCCWWeapon());
        EquipmentType.addType(new InfantryShotgunBuccaneerGelGunWeapon());
        EquipmentType.addType(new InfantryShotgunCeresCrowdbusterWeapon());
        EquipmentType.addType(new InfantryShotgunCombatWeapon());
        EquipmentType.addType(new InfantryShotgunDoubleBarrelWeapon());
        EquipmentType.addType(new InfantryShotgunPumpActionWeapon());
        EquipmentType.addType(new InfantryShotgunSawnoffDoubleBarrelWeapon());
        EquipmentType.addType(new InfantryShotgunSawnoffPumpActionWeapon());
        EquipmentType.addType(new InfantryShotgunWakazashiWeapon());      
      
         // Infantry Support Weapons
        EquipmentType.addType(new InfantrySupportPortableMGWeapon());
        EquipmentType.addType(new InfantrySupportMk1LightAAWeapon());
        EquipmentType.addType(new InfantrySupportMk2PortableAAWeapon());
        EquipmentType.addType(new InfantrySupportClanBearhunterAutocannonWeapon());
        EquipmentType.addType(new InfantrySupportPortableAutocannonWeapon());
        EquipmentType.addType(new InfantrySupportHeavyFlamerWeapon());
        EquipmentType.addType(new InfantrySupportGrandMaulerGaussCannonWeapon());
        EquipmentType.addType(new InfantrySupportMagshotGaussRifleWeapon());
        EquipmentType.addType(new InfantrySupportTsunamiHeavyGaussRifleWeapon());
        EquipmentType.addType(new InfantrySupportDavidLightGaussRifleWeapon());
        EquipmentType.addType(new InfantrySupportKingDavidLightGaussRifleWeapon());
        EquipmentType.addType(new InfantrySupportGrenadeLauncherWeapon());
        EquipmentType.addType(new InfantrySupportGrenadeLauncherInfernoWeapon());
        EquipmentType.addType(new InfantrySupportAutoGrenadeLauncherWeapon());
        EquipmentType.addType(new InfantrySupportAutoGrenadeLauncherInfernoWeapon());
        EquipmentType.addType(new InfantrySupportCompactGrenadeLauncherWeapon());
        EquipmentType.addType(new InfantrySupportHeavyGrenadeLauncherWeapon());
        EquipmentType.addType(new InfantrySupportHeavyGrenadeLauncherInfernoWeapon());
        EquipmentType.addType(new InfantrySupportHeavyAutoGrenadeLauncherWeapon());
        EquipmentType.addType(new InfantrySupportHeavyAutoGrenadeLauncherInfernoWeapon());
        EquipmentType.addType(new InfantrySupportHellboreAssaultLaserWeapon());
        EquipmentType.addType(new InfantrySupportLightMachineGunWeapon());
        EquipmentType.addType(new InfantrySupportHeavyMachineGunWeapon());
        EquipmentType.addType(new InfantrySupportHeavyMortarWeapon());
        EquipmentType.addType(new InfantrySupportHeavyMortarInfernoWeapon());
        EquipmentType.addType(new InfantrySupportLightMortarWeapon());
        EquipmentType.addType(new InfantrySupportLightMortarInfernoWeapon());
        EquipmentType.addType(new InfantrySupportOneShotMRMWeapon());
        EquipmentType.addType(new InfantrySupportFiredrakeNeedlerWeapon());
        EquipmentType.addType(new InfantrySupportPortablePPCWeapon());
        EquipmentType.addType(new InfantrySupportHeavyPPCWeapon());
        EquipmentType.addType(new InfantrySupportPortablePlasmaWeapon());
        EquipmentType.addType(new InfantrySupportDragonsbaneDisposablePulseLaserWeapon());
        EquipmentType.addType(new InfantrySupportHeavyRecoillessRifleWeapon());
        EquipmentType.addType(new InfantrySupportHeavyRecoillessRifleInfernoWeapon());
        EquipmentType.addType(new InfantrySupportLightRecoillessRifleWeapon());
        EquipmentType.addType(new InfantrySupportLightRecoillessRifleInfernoWeapon());
        EquipmentType.addType(new InfantrySupportMediumRecoillessRifleWeapon());
        EquipmentType.addType(new InfantrySupportMediumRecoillessRifleInfernoWeapon());
        EquipmentType.addType(new InfantrySupportLAWWeapon());
        EquipmentType.addType(new InfantrySupportVLAWWeapon());
        EquipmentType.addType(new InfantrySupportStandardSRMWeapon());
        EquipmentType.addType(new InfantrySupportStandardSRMInfernoWeapon());
        EquipmentType.addType(new InfantrySupportHeavySRMWeapon());
        EquipmentType.addType(new InfantrySupportHeavySRMInfernoWeapon());
        EquipmentType.addType(new InfantrySupportLightSRMWeapon());
        EquipmentType.addType(new InfantrySupportLaserWeapon());
        EquipmentType.addType(new InfantrySupportERLaserWeapon());
        EquipmentType.addType(new InfantrySupportClanERLaserWeapon());
        EquipmentType.addType(new InfantrySupportHeavyLaserWeapon());
        EquipmentType.addType(new InfantrySupportERHeavyLaserWeapon());
        EquipmentType.addType(new InfantrySupportClanERHeavyLaserWeapon());
        EquipmentType.addType(new InfantrySupportClanSemiPortableHeavyLaserWeapon());
        EquipmentType.addType(new InfantrySupportClanSemiPortableERLaserWeapon());
        EquipmentType.addType(new InfantrySupportSemiPortableLaserWeapon());
        EquipmentType.addType(new InfantrySupportPulseLaserWeapon());
        EquipmentType.addType(new InfantrySupportHeavyPulseLaserWeapon());
        EquipmentType.addType(new InfantrySupportClanSemiPortablePulseLaserWeapon());
        EquipmentType.addType(new InfantrySupportUltraHeavyLaserWeapon());
        EquipmentType.addType(new InfantrySupportVintageMGWeapon());
        EquipmentType.addType(new InfantrySupportVintageMiniGunWeapon());
        EquipmentType.addType(new InfantrySupportVintageGatlingGunWeapon());
        EquipmentType.addType(new InfantrySupportWireGuidedMissileWeapon());
        EquipmentType.addType(new InfantrySupportGungnirHeavyGaussWeapon());
        EquipmentType.addType(new InfantrySupportMagPulseHarpoonWeapon());
        EquipmentType.addType(new InfantrySupportSnubNoseSupportPPCWeapon());
        
        // Infantry Grenade Weapons
        EquipmentType.addType(new InfantryGrenadeInfernoWeapon());
        EquipmentType.addType(new InfantryGrenadeMicroWeapon());
        EquipmentType.addType(new InfantryGrenadeMiniInfernoWeapon());
        EquipmentType.addType(new InfantryGrenadeRAGWeapon());
        EquipmentType.addType(new InfantryGrenadeStandardWeapon());
        
        // Infantry SMG Weapons
        EquipmentType.addType(new InfantrySMGClanGaussWeapon());
        EquipmentType.addType(new InfantrySMGGuntherMP20Weapon());
        EquipmentType.addType(new InfantrySMGImperator2894A1Weapon());
        EquipmentType.addType(new InfantrySMGKA23SubgunWeapon());
        EquipmentType.addType(new InfantrySMGRorynexRM3XXIWeapon());
        EquipmentType.addType(new InfantrySMGRuganWeapon());
        EquipmentType.addType(new InfantrySMGWeapon());
        
        // Prosthetic Weapon from ATOW Companion
        EquipmentType.addType(new InfantryProstheticLaserWeapon());
        EquipmentType.addType(new InfantryProstheticBallisticWeapon());
        EquipmentType.addType(new InfantryProstheticDartgunWeapon());
        EquipmentType.addType(new InfantryProstheticNeedlerWeapon());
        EquipmentType.addType(new InfantryProstheticShotgunWeapon());
        EquipmentType.addType(new InfantryProstheticSonicStunnerWeapon());
        EquipmentType.addType(new InfantryProstheticSMGWeapon());
        EquipmentType.addType(new InfantryProstheticBladeWeapon());
        EquipmentType.addType(new InfantryProstheticNeedleWeapon());
        EquipmentType.addType(new InfantryProstheticShockerWeapon());
        EquipmentType.addType(new InfantryProstheticVibroBladeWeapon());
        EquipmentType.addType(new InfantryProstheticClimbingClawsWeapon());    

        EquipmentType.addType(new ISFireExtinguisher());
        EquipmentType.addType(new CLFireExtinguisher());

        // plasma weapons
        EquipmentType.addType(new ISPlasmaRifle());
        EquipmentType.addType(new CLPlasmaCannon());

        // MekMortarWeapons
        EquipmentType.addType(new ISMekMortar1());
        EquipmentType.addType(new ISMekMortar2());
        EquipmentType.addType(new ISMekMortar4());
        EquipmentType.addType(new ISMekMortar8());
        EquipmentType.addType(new CLMekMortar1());
        EquipmentType.addType(new CLMekMortar2());
        EquipmentType.addType(new CLMekMortar4());
        EquipmentType.addType(new CLMekMortar8());

        // BA weapons
        EquipmentType.addType(new CLAdvancedSRM1());
        EquipmentType.addType(new CLAdvancedSRM1OS());
        EquipmentType.addType(new CLAdvancedSRM2());
        EquipmentType.addType(new CLAdvancedSRM2OS());
        EquipmentType.addType(new CLAdvancedSRM3());
        EquipmentType.addType(new CLAdvancedSRM3OS());
        EquipmentType.addType(new CLAdvancedSRM4());
        EquipmentType.addType(new CLAdvancedSRM4OS());
        EquipmentType.addType(new CLAdvancedSRM5());
        EquipmentType.addType(new CLAdvancedSRM5OS());
        EquipmentType.addType(new CLAdvancedSRM6());
        EquipmentType.addType(new CLAdvancedSRM6OS());
        EquipmentType.addType(new CLBAAPGaussRifle());
        EquipmentType.addType(new CLBABearhunterSuperheavyAC());
        EquipmentType.addType(new CLBAERMediumLaser());
        EquipmentType.addType(new CLBAERMediumPulseLaser());
        EquipmentType.addType(new CLBAERMicroLaser());
        EquipmentType.addType(new CLBAERSmallLaser());
        EquipmentType.addType(new CLBAERSmallPulseLaser());
        EquipmentType.addType(new CLBAFlamer());
        EquipmentType.addType(new CLBAHeavyFlamer());
        EquipmentType.addType(new CLBAHeavyGrenadeLauncher());
        EquipmentType.addType(new CLBAHeavyMediumLaser());
        EquipmentType.addType(new CLBAHeavyMG());
        EquipmentType.addType(new CLBAHeavyRecoillessRifle());
        EquipmentType.addType(new CLBAHeavySmallLaser());
        EquipmentType.addType(new CLBALBX());
        EquipmentType.addType(new CLBALightMG());
        EquipmentType.addType(new CLBALightRecoillessRifle());
        EquipmentType.addType(new CLBALRM1());
        EquipmentType.addType(new CLBALRM1OS());
        EquipmentType.addType(new CLBALRM2());
        EquipmentType.addType(new CLBALRM2OS());
        EquipmentType.addType(new CLBALRM3());
        EquipmentType.addType(new CLBALRM3OS());
        EquipmentType.addType(new CLBALRM4());
        EquipmentType.addType(new CLBALRM4OS());
        EquipmentType.addType(new CLBALRM5());
        EquipmentType.addType(new CLBALRM5OS());
        EquipmentType.addType(new CLBAMediumPulseLaser());
        EquipmentType.addType(new CLBAMediumRecoillessRifle());
        EquipmentType.addType(new CLBAMG());
        EquipmentType.addType(new CLBAMicroPulseLaser());
        EquipmentType.addType(new CLBASmallLaser());
        EquipmentType.addType(new CLBASmallPulseLaser());
        EquipmentType.addType(new CLBASRM1());
        EquipmentType.addType(new CLBASRM1OS());
        EquipmentType.addType(new CLBASRM2());
        EquipmentType.addType(new CLBASRM2OS());
        EquipmentType.addType(new CLBASRM3());
        EquipmentType.addType(new CLBASRM3OS());
        EquipmentType.addType(new CLBASRM4());
        EquipmentType.addType(new CLBASRM4OS());
        EquipmentType.addType(new CLBASRM5());
        EquipmentType.addType(new CLBASRM5OS());
        EquipmentType.addType(new CLBASRM6());
        EquipmentType.addType(new CLBASRM6OS());
        EquipmentType.addType(new CLBAMicroBomb());
        EquipmentType.addType(new CLBACompactNarc());
        EquipmentType.addType(new ISBAERMediumLaser());
        EquipmentType.addType(new ISBAERSmallLaser());
        EquipmentType.addType(new ISBAFlamer());
        EquipmentType.addType(new ISBAHeavyFlamer());
        EquipmentType.addType(new ISBAHeavyMG());
        EquipmentType.addType(new ISBALightMG());
        EquipmentType.addType(new ISBAMagshotGaussRifle());
        EquipmentType.addType(new ISBAMediumLaser());
        EquipmentType.addType(new ISBAMediumPulseLaser());
        EquipmentType.addType(new ISBAMG());
        EquipmentType.addType(new ISBAPlasmaRifle());
        EquipmentType.addType(new ISBASmallLaser());
        EquipmentType.addType(new ISBASmallPulseLaser());
        EquipmentType.addType(new ISBASmallVariableSpeedPulseLaser());
        EquipmentType.addType(new ISBAMediumVariableSpeedPulseLaser());
        EquipmentType.addType(new ISBATaser());
        EquipmentType.addType(new ISBACompactNarc());
        EquipmentType.addType(new ISBADavidLightGaussRifle());
        EquipmentType.addType(new ISBAFiredrakeNeedler());
        EquipmentType.addType(new ISBAGrandMaulerGaussCannon());
        EquipmentType.addType(new ISBAHeavyGrenadeLauncher());
        EquipmentType.addType(new ISBAHeavyMortar());
        EquipmentType.addType(new ISBAHeavyRecoillessRifle());
        EquipmentType.addType(new ISBAKingDavidLightGaussRifle());
        EquipmentType.addType(new ISBALightMortar());
        EquipmentType.addType(new ISBALightRecoillessRifle());
        EquipmentType.addType(new ISBAMediumRecoillessRifle());
        EquipmentType.addType(new ISBAMicroGrenadeLauncher());
        EquipmentType.addType(new ISBAGrenadeLauncher());
        EquipmentType.addType(new ISBAPopUpMineLauncher());
        EquipmentType.addType(new ISBATsunamiGaussRifle());
        EquipmentType.addType(new ISBASRM1());
        EquipmentType.addType(new ISBASRM2());
        EquipmentType.addType(new ISBASRM3());
        EquipmentType.addType(new ISBASRM4());
        EquipmentType.addType(new ISBASRM5());
        EquipmentType.addType(new ISBASRM6());
        EquipmentType.addType(new ISBALRM1());
        EquipmentType.addType(new ISBALRM2());
        EquipmentType.addType(new ISBALRM3());
        EquipmentType.addType(new ISBALRM4());
        EquipmentType.addType(new ISBALRM5());
        EquipmentType.addType(new ISBAMRM1());
        EquipmentType.addType(new ISBAMRM2());
        EquipmentType.addType(new ISBAMRM3());
        EquipmentType.addType(new ISBAMRM4());
        EquipmentType.addType(new ISBAMRM5());
        EquipmentType.addType(new ISBASRM1OS());
        EquipmentType.addType(new ISBASRM2OS());
        EquipmentType.addType(new ISBASRM3OS());
        EquipmentType.addType(new ISBASRM4OS());
        EquipmentType.addType(new ISBASRM5OS());
        EquipmentType.addType(new ISBASRM6OS());
        EquipmentType.addType(new ISBALRM1OS());
        EquipmentType.addType(new ISBALRM2OS());
        EquipmentType.addType(new ISBALRM3OS());
        EquipmentType.addType(new ISBALRM4OS());
        EquipmentType.addType(new ISBALRM5OS());
        EquipmentType.addType(new ISBAMRM1OS());
        EquipmentType.addType(new ISBAMRM2OS());
        EquipmentType.addType(new ISBAMRM3OS());
        EquipmentType.addType(new ISBAMRM4OS());
        EquipmentType.addType(new ISBAMRM5OS());
        EquipmentType.addType(new ISBARL1());
        EquipmentType.addType(new ISBARL2());
        EquipmentType.addType(new ISBARL3());
        EquipmentType.addType(new ISBARL4());
        EquipmentType.addType(new ISBARL5());

        // Unofficial BA Weapons
        EquipmentType.addType(new CLBABearhunterSuperheavyACi());

        // Cruise Missiles
        EquipmentType.addType(new ISCruiseMissile50());
        EquipmentType.addType(new ISCruiseMissile70());
        EquipmentType.addType(new ISCruiseMissile90());
        EquipmentType.addType(new ISCruiseMissile120());

        // Unofficial Weapons
        EquipmentType.addType(new ISAC10i());
        EquipmentType.addType(new ISAC15());
        EquipmentType.addType(new ISGAC2());
        EquipmentType.addType(new ISGAC4());
        EquipmentType.addType(new ISGAC6());
        EquipmentType.addType(new ISGAC8());
        EquipmentType.addType(new CLWERPPC());

        // Naval weapons
        EquipmentType.addType(new ISNL35Weapon());
        EquipmentType.addType(new ISNL45Weapon());
        EquipmentType.addType(new ISNL55Weapon());
        EquipmentType.addType(new CLNL35Weapon());
        EquipmentType.addType(new CLNL45Weapon());
        EquipmentType.addType(new CLNL55Weapon());
        EquipmentType.addType(new ISNPPCWeaponLight());
        EquipmentType.addType(new ISNPPCWeaponMedium());
        EquipmentType.addType(new ISNPPCWeaponHeavy());
        EquipmentType.addType(new CLNPPCWeaponLight());
        EquipmentType.addType(new CLNPPCWeaponMedium());
        EquipmentType.addType(new CLNPPCWeaponHeavy());
        EquipmentType.addType(new NAC10Weapon());
        EquipmentType.addType(new NAC20Weapon());
        EquipmentType.addType(new NAC25Weapon());
        EquipmentType.addType(new NAC30Weapon());
        EquipmentType.addType(new NAC35Weapon());
        EquipmentType.addType(new NAC40Weapon());
        EquipmentType.addType(new ISNGaussWeaponLight());
        EquipmentType.addType(new ISNGaussWeaponMedium());
        EquipmentType.addType(new ISNGaussWeaponHeavy());
        EquipmentType.addType(new CLNGaussWeaponLight());
        EquipmentType.addType(new CLNGaussWeaponMedium());
        EquipmentType.addType(new CLNGaussWeaponHeavy());
        EquipmentType.addType(new ISBarracudaWeapon());
        EquipmentType.addType(new ISWhiteSharkWeapon());
        EquipmentType.addType(new ISKillerWhaleWeapon());
        EquipmentType.addType(new CLBarracudaWeapon());
        EquipmentType.addType(new CLWhiteSharkWeapon());
        EquipmentType.addType(new CLKillerWhaleWeapon());
        EquipmentType.addType(new ISBarracudaTWeapon());
        EquipmentType.addType(new WhiteSharkTWeapon());
        EquipmentType.addType(new KillerWhaleTWeapon());
        EquipmentType.addType(new KrakenTWeapon());
        EquipmentType.addType(new ISKrakenWeapon());
        EquipmentType.addType(new ISAR10Weapon());
        EquipmentType.addType(new CLAR10Weapon());
        EquipmentType.addType(new ScreenLauncherWeapon());
        EquipmentType.addType(new ISSCCWeaponLight());
        EquipmentType.addType(new ISSCCWeaponMedium());
        EquipmentType.addType(new ISSCCWeaponHeavy());
        EquipmentType.addType(new CLSCCWeaponLight());
        EquipmentType.addType(new CLSCCWeaponMedium());
        EquipmentType.addType(new CLSCCWeaponHeavy());
        EquipmentType.addType(new ISSCL1Weapon());
        EquipmentType.addType(new ISSCL2Weapon());
        EquipmentType.addType(new ISSCL3Weapon());
        EquipmentType.addType(new CLSCL1Weapon());
        EquipmentType.addType(new CLSCL2Weapon());
        EquipmentType.addType(new CLSCL3Weapon());
        EquipmentType.addType(new ISPiranhaWeapon());
        EquipmentType.addType(new ISStingrayWeapon());
        EquipmentType.addType(new ISSwordfishWeapon());
        EquipmentType.addType(new ISMantaRayWeapon());
        EquipmentType.addType(new CLPiranhaWeapon());
        EquipmentType.addType(new CLStingrayWeapon());
        EquipmentType.addType(new CLSwordfishWeapon());
        EquipmentType.addType(new CLMantaRayWeapon());
        EquipmentType.addType(new ISMassDriverHeavy());
        EquipmentType.addType(new ISMassDriverMedium());
        EquipmentType.addType(new ISMassDriverLight());
        
        // bomb-related weapons
        EquipmentType.addType(new ISAAAMissileWeapon());
        EquipmentType.addType(new CLAAAMissileWeapon());
        EquipmentType.addType(new ASMissileWeapon());
        EquipmentType.addType(new ASEWMissileWeapon());
        EquipmentType.addType(new ISLAAMissileWeapon());
        EquipmentType.addType(new CLLAAMissileWeapon());
        EquipmentType.addType(new BombArrowIV());
        //EquipmentType.addType(new CLBombArrowIV());
        EquipmentType.addType(new BombTAG());
        EquipmentType.addType(new BombISRL10());
        EquipmentType.addType(new AlamoMissileWeapon());
        EquipmentType.addType(new SpaceBombAttack());
        EquipmentType.addType(new DiveBombAttack());
        EquipmentType.addType(new AltitudeBombAttack());

        // Weapon Bays
        EquipmentType.addType(new LaserBayWeapon());
        EquipmentType.addType(new PointDefenseBayWeapon());
        EquipmentType.addType(new PPCBayWeapon());
        EquipmentType.addType(new PulseLaserBayWeapon());
        EquipmentType.addType(new ArtilleryBayWeapon());
        EquipmentType.addType(new PlasmaBayWeapon());
        EquipmentType.addType(new ACBayWeapon());
        EquipmentType.addType(new LBXBayWeapon());
        EquipmentType.addType(new LRMBayWeapon());
        EquipmentType.addType(new SRMBayWeapon());
        EquipmentType.addType(new MRMBayWeapon());
        EquipmentType.addType(new MMLBayWeapon());
        EquipmentType.addType(new ATMBayWeapon());
        EquipmentType.addType(new RLBayWeapon());
        EquipmentType.addType(new CapitalLaserBayWeapon());
        EquipmentType.addType(new CapitalACBayWeapon());
        EquipmentType.addType(new CapitalGaussBayWeapon());
        EquipmentType.addType(new CapitalPPCBayWeapon());
        EquipmentType.addType(new CapitalMissileBayWeapon());
        EquipmentType.addType(new CapitalMDBayWeapon());
        EquipmentType.addType(new ISAR10BayWeapon());
        EquipmentType.addType(new CLAR10BayWeapon());
        EquipmentType.addType(new ScreenLauncherBayWeapon());
        EquipmentType.addType(new SCCBayWeapon());
        EquipmentType.addType(new SCLBayWeapon());
        EquipmentType.addType(new SubCapitalMissileBayWeapon());
        EquipmentType.addType(new MiscBayWeapon());

        // Improved OS Weapons
        EquipmentType.addType(new ISLRM5IOS());
        EquipmentType.addType(new ISLRM10IOS());
        EquipmentType.addType(new ISLRM15IOS());
        EquipmentType.addType(new ISLRM20IOS());
        EquipmentType.addType(new CLLRM5IOS());
        EquipmentType.addType(new CLLRM10IOS());
        EquipmentType.addType(new CLLRM15IOS());
        EquipmentType.addType(new CLLRM20IOS());
        EquipmentType.addType(new CLStreakLRM10IOS());
        EquipmentType.addType(new CLStreakLRM15IOS());
        EquipmentType.addType(new CLStreakLRM20IOS());
        EquipmentType.addType(new ISLRT5IOS());
        EquipmentType.addType(new ISLRT10IOS());
        EquipmentType.addType(new ISLRT15IOS());
        EquipmentType.addType(new ISLRT20IOS());
        EquipmentType.addType(new CLLRT5IOS());
        EquipmentType.addType(new CLLRT10IOS());
        EquipmentType.addType(new CLLRT15IOS());
        EquipmentType.addType(new CLLRT20IOS());
        EquipmentType.addType(new ISSRM2IOS());
        EquipmentType.addType(new ISSRM4IOS());
        EquipmentType.addType(new ISSRM6IOS());
        EquipmentType.addType(new CLSRM2IOS());
        EquipmentType.addType(new CLSRM4IOS());
        EquipmentType.addType(new CLSRM6IOS());
        EquipmentType.addType(new ISStreakSRM2IOS());
        EquipmentType.addType(new ISStreakSRM4IOS());
        EquipmentType.addType(new ISStreakSRM6IOS());
        EquipmentType.addType(new CLStreakSRM2IOS());
        EquipmentType.addType(new CLStreakSRM4IOS());
        EquipmentType.addType(new CLStreakSRM6IOS());
        EquipmentType.addType(new ISSRT2IOS());
        EquipmentType.addType(new ISSRT4IOS());
        EquipmentType.addType(new ISSRT6IOS());
        EquipmentType.addType(new CLSRT2IOS());
        EquipmentType.addType(new CLSRT4IOS());
        EquipmentType.addType(new CLSRT6IOS());
        EquipmentType.addType(new ISMRM10IOS());
        EquipmentType.addType(new ISMRM20IOS());
        EquipmentType.addType(new ISMRM30IOS());
        EquipmentType.addType(new ISMRM40IOS());

        EquipmentType.addType(new ISSmallReengineeredLaser());
        EquipmentType.addType(new ISMediumReengineeredLaser());
        EquipmentType.addType(new ISLargeReengineeredLaser());

        EquipmentType.addType(new ISTSEMPCannon());
        EquipmentType.addType(new ISTSEMPOneShot());

        EquipmentType.addType(new ISAPDS());
        EquipmentType.addType(new ISBAAPDS());
        EquipmentType.addType(new ISRISCHyperLaser());
    }

    public int getExplosionDamage() {
        return explosionDamage;
    }

    @Override
    public double getCost(Entity entity, boolean isArmored, int loc) {
        if (isArmored) {
            double armoredCost = cost;
            armoredCost += 150000 * getCriticals(entity);

            return armoredCost;
        }

        return super.getCost(entity, isArmored, loc);
    }

    public boolean isSplitable() {
        return criticals >= 8;
    }

}
