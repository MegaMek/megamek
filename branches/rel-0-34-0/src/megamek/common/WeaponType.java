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

import megamek.common.weapons.*;

// TODO add XML support back in.

/*
 * import java.io.File; import java.text.NumberFormat; import
 * java.util.Enumeration; import javax.xml.parsers.DocumentBuilder; import
 * javax.xml.parsers.DocumentBuilderFactory; import javax.xml.transform.*;
 * import javax.xml.transform.dom.*; import javax.xml.transform.stream.*; import
 * org.w3c.dom.*;
 */
/**
 * A type of mech or vehicle weapon. There is only one instance of this weapon
 * for all weapons of this type.
 */
public class WeaponType extends EquipmentType {
    public static final int DAMAGE_MISSILE = -2;
    public static final int DAMAGE_VARIABLE = -3;
    public static final int DAMAGE_SPECIAL = -4;
    public static final int DAMAGE_ARTILLERY = -5;
    public static final int WEAPON_NA = Integer.MIN_VALUE;

    // weapon flags (note: many weapons can be identified by their ammo type)
    public static final long F_DIRECT_FIRE = 1l << 0; // marks any weapon
    // affected by a
    // targetting computer
    public static final long F_FLAMER = 1l << 1;
    public static final long F_LASER = 1l << 2; // for eventual glazed armor
    // purposes
    public static final long F_PPC = 1l << 3; // "
    public static final long F_AUTO_TARGET = 1l << 4; // for weapons that
    // target automatically
    // (AMS)
    public static final long F_NO_FIRES = 1l << 5; // cannot start fires
    public static final long F_SOLO_ATTACK = 1l << 7; // must be only weapon
    // attacking
    public static final long F_SPLITABLE = 1l << 8; // Weapons that can be split
    // between locations
    public static final long F_MG = 1l << 9; // MGL; for rapid fire set up
    public static final long F_INFERNO = 1l << 10; // Inferno weapon
    public static final long F_INFANTRY = 1l << 11; // small calibre weapon, no
    // ammo, damage based on #
    // men shooting
    public static final long F_MISSILE_HITS = 1l << 13; // use missile rules or
    // # of hits
    public static final long F_ONESHOT = 1l << 14; // weapon is oneShot.
    public static final long F_ARTILLERY = 1l << 15;
    public static final long F_BALLISTIC = 1l << 16; // For Gunnery/Ballistic
    // skill
    public static final long F_ENERGY = 1l << 17; // For Gunnery/Energy skill
    public static final long F_MISSILE = 1l << 18; // For Gunnery/Missile skill
    public static final long F_PLASMA = 1l << 19; // For fires
    public static final long F_INCENDIARY_NEEDLES = 1l << 20; // For fires
    public static final long F_PROTOTYPE = 1l << 21; // for war of 3039
    // prototype weapons
    public static final long F_HEATASDICE = 1l << 22; // heat is listed in
    // dice, not points
    public static final long F_AMS = 1l << 23; // Weapon is an anti-missile
    // system.
    // public static final long F_UNUSED = 1l << 24;
    public static final long F_INFANTRY_ONLY = 1l << 25; // only target
    // infantry
    public static final long F_TAG = 1l << 26; // Target acquisition gear
    public static final long F_C3M = 1l << 27; // C3 Master with Target
    // acquisition gear
    public static final long F_PLASMA_MFUK = 1l << 28; // Plasma Rifle
    public static final long F_EXTINGUISHER = 1l << 29; // Fire extinguisher
    public static final long F_PULSE = 1l << 30; // pulse weapons
    public static final long F_BURST_FIRE = 1l << 31; // full damage vs
    // infantry
    public static final long F_MGA = 1l << 32; // machine gun array
    public static final long F_NO_AIM = 1l << 33;
    public static final long F_BOMBAST_LASER = 1l << 34;
    public static final long F_CRUISE_MISSILE = 1l << 35;
    public static final long F_B_POD = 1l << 36;
    public static final long F_TASER = 1l << 37;
    public static final long F_BA_WEAPON = 1l << 38; // Currently only used by
    // MegaMekLab
    public static final long F_ANTI_SHIP = 1l << 39; // for anti-ship missiles
    public static final long F_SPACE_BOMB = 1l << 40;
    public static final long F_MECH_WEAPON = 1L << 60;
    public static final long F_AERO_WEAPON = 1L << 61;
    public static final long F_PROTO_WEAPON = 1L << 62;
    public static final long F_TANK_WEAPON = 1L << 63;
    // public static final long F_VTOL_WEAPON = 1L << 64;

    // add maximum range for AT2
    public static final int RANGE_SHORT = 1;
    public static final int RANGE_MED = 2;
    public static final int RANGE_LONG = 3;
    public static final int RANGE_EXT = 4;

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
    public static final int NUM_CLASSES = 23;

    public static String[] classNames = { "Unknown", "Laser", "Point Defense", "PPC", "Pulse Laser", "Artilery", "AMS", "AC", "LBX", "LRM", "SRM", "MRM", "ATM", "Rocket Launcher", "Capital Laser", "Capital PPC", "Capital AC", "Capital Gauss", "Capital Missile", "AR10", "Screen", "Sub Capital Cannon" };

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
            return 5;
        } else if (hasFlag(F_PLASMA_MFUK)) {
            return 5;
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
        int minRange = minimumRange;
        int sRange = shortRange;
        int mRange = mediumRange;
        int lRange = longRange;
        int eRange = extremeRange;
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
        int[] weaponRanges = { minRange, sRange, mRange, lRange, eRange };
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
        return new int[] { minimumRange, waterShortRange, waterMediumRange, waterLongRange, waterExtremeRange };
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

    public int getMaxRange() {
        return maxRange;
    }

    public int[] getATRanges() {
        if (capital) {
            return new int[] { 0, 12, 24, 40, 50 };
        } else {
            return new int[] { 0, 6, 12, 20, 25 };
        }

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

    public static EquipmentType getSubCapBayType(int atclass) {
        // return the correct weapons bay for the given type of weapon
        switch (atclass) {
        case (CLASS_CAPITAL_AC):
            return EquipmentType.get("Sub-Capital Cannon Bay");
        case (CLASS_CAPITAL_LASER):
            return EquipmentType.get("Sub-Capital Laser Bay");
        default:
            return WeaponType.getBayType(atclass);
        }
    }

    // Probably not the best place for this
    public static EquipmentType getBayType(int atclass) {
        // return the correct weapons bay for the given type of weapon
        switch (atclass) {
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
            return EquipmentType.get("Capital Laser Bay");
        case (CLASS_CAPITAL_PPC):
            return EquipmentType.get("Capital PPC Bay");
        case (CLASS_CAPITAL_AC):
            return EquipmentType.get("Capital AC Bay");
        case (CLASS_CAPITAL_GAUSS):
            return EquipmentType.get("Capital Gauss Bay");
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
        EquipmentType.addType(new ISLargeLaser());
        EquipmentType.addType(new ISSmallLaser());
        EquipmentType.addType(new ISLargePulseLaser());
        EquipmentType.addType(new ISLargeXPulseLaser());
        EquipmentType.addType(new ISERLargeLaser());
        EquipmentType.addType(new ISERLargeLaserPrototype());
        EquipmentType.addType(new ISERMediumLaser());
        EquipmentType.addType(new ISMediumPulseLaser());
        EquipmentType.addType(new ISMediumPulseLaserPrototype());
        EquipmentType.addType(new ISMediumXPulseLaser());
        EquipmentType.addType(new ISSmallPulseLaser());
        EquipmentType.addType(new ISSmallXPulseLaser());
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
        EquipmentType.addType(new CLHeavyMediumLaser());
        EquipmentType.addType(new CLMediumPulseLaser());
        EquipmentType.addType(new CLERMediumPulseLaser());
        EquipmentType.addType(new CLERSmallLaser());
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
        EquipmentType.addType(new ISERPPC());
        EquipmentType.addType(new ISEHERPPC());
        EquipmentType.addType(new CLERPPC());
        EquipmentType.addType(new ISSnubNosePPC());
        EquipmentType.addType(new ISLightPPC());
        EquipmentType.addType(new ISHeavyPPC());
        EquipmentType.addType(new ISHERPPC());
        EquipmentType.addType(new ISSupportPPC());
        EquipmentType.addType(new CLSupportPPC());
        // Flamers
        EquipmentType.addType(new CLFlamer());
        EquipmentType.addType(new ISFlamer());
        EquipmentType.addType(new CLVehicleFlamer());
        EquipmentType.addType(new ISVehicleFlamer());
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
        EquipmentType.addType(new CLUAC5());
        EquipmentType.addType(new CLUAC10());
        EquipmentType.addType(new CLUAC20());
        // LBXs
        EquipmentType.addType(new ISLB2XAC());
        EquipmentType.addType(new ISLB5XAC());
        EquipmentType.addType(new ISLB10XAC());
        EquipmentType.addType(new ISLB10XACPrototype());
        EquipmentType.addType(new ISLB20XAC());
        EquipmentType.addType(new CLLB2XAC());
        EquipmentType.addType(new CLLB5XAC());
        EquipmentType.addType(new CLLB10XAC());
        EquipmentType.addType(new CLLB20XAC());
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
        EquipmentType.addType(new CLStreakLRM5());
        EquipmentType.addType(new CLStreakLRM10());
        EquipmentType.addType(new CLStreakLRM15());
        EquipmentType.addType(new CLStreakLRM20());
        EquipmentType.addType(new CLStreakLRM5OS());
        EquipmentType.addType(new CLStreakLRM10OS());
        EquipmentType.addType(new CLStreakLRM15OS());
        EquipmentType.addType(new CLStreakLRM20OS());
        EquipmentType.addType(new ISExtendedLRM5());
        EquipmentType.addType(new ISExtendedLRM10());
        EquipmentType.addType(new ISExtendedLRM15());
        EquipmentType.addType(new ISExtendedLRM20());
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
        EquipmentType.addType(new CLStreakSRM5());
        EquipmentType.addType(new CLStreakSRM6());
        EquipmentType.addType(new CLStreakSRM2OS());
        EquipmentType.addType(new CLStreakSRM4OS());
        EquipmentType.addType(new CLStreakSRM6OS());
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
        EquipmentType.addType(new ISRL20());
        // ATMs
        EquipmentType.addType(new CLATM3());
        EquipmentType.addType(new CLATM6());
        EquipmentType.addType(new CLATM9());
        EquipmentType.addType(new CLATM12());
        // MRMs
        EquipmentType.addType(new ISMRM1());
        EquipmentType.addType(new ISMRM2());
        EquipmentType.addType(new ISMRM3());
        EquipmentType.addType(new ISMRM4());
        EquipmentType.addType(new ISMRM5());
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
        EquipmentType.addType(new CLNarc());
        EquipmentType.addType(new CLNarcOS());
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
        EquipmentType.addType(new CLLightTAG());
        EquipmentType.addType(new CLTAG());
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

        // Infantry Attacks
        EquipmentType.addType(new LegAttack());
        EquipmentType.addType(new SwarmAttack());
        EquipmentType.addType(new StopSwarmAttack());

        // Infantry Weapons
        EquipmentType.addType(new InfantryRifleWeapon());
        EquipmentType.addType(new InfantrySRMWeapon());
        EquipmentType.addType(new InfantryInfernoSRMWeapon());
        EquipmentType.addType(new InfantryLRMWeapon());
        EquipmentType.addType(new InfantryLaserWeapon());
        EquipmentType.addType(new InfantryFlamerWeapon());
        EquipmentType.addType(new InfantryMGWeapon());
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
        EquipmentType.addType(new CLSmallLaser());
        EquipmentType.addType(new ISLightRecoillessRifle());
        EquipmentType.addType(new ISMediumRecoillessRifle());
        EquipmentType.addType(new ISHeavyRecoillessRifle());
        EquipmentType.addType(new CLLightRecoillessRifle());
        EquipmentType.addType(new CLMediumRecoillessRifle());
        EquipmentType.addType(new CLHeavyRecoillessRifle());
        EquipmentType.addType(new CLBearhunterSuperheavyAC());
        EquipmentType.addType(new ISKingDavidLightGaussRifle());
        EquipmentType.addType(new ISDavidLightGaussRifle());
        EquipmentType.addType(new ISFiredrakeNeedler());
        EquipmentType.addType(new ISBAPlasmaRifle());
        EquipmentType.addType(new ISHeavyMortar());
        EquipmentType.addType(new ISLightMortar());
        EquipmentType.addType(new ISAutoGrenadeLauncher());
        EquipmentType.addType(new ISMicroGrenadeLauncher());
        EquipmentType.addType(new CLHeavyGrenadeLauncher());
        EquipmentType.addType(new ISCompactNarc());
        EquipmentType.addType(new ISMineLauncher());
        EquipmentType.addType(new CLMicroBomb());
        EquipmentType.addType(new ISGrandMaulerGaussCannon());
        EquipmentType.addType(new ISTsunamiGaussRifle());
        EquipmentType.addType(new ISBAMG());
        EquipmentType.addType(new ISBALightMG());
        EquipmentType.addType(new ISBAHeavyMG());
        EquipmentType.addType(new CLBAMG());
        EquipmentType.addType(new CLBALightMG());
        EquipmentType.addType(new CLBAHeavyMG());
        EquipmentType.addType(new ISBAMagshotGaussRifle());
        EquipmentType.addType(new CLBAAPGaussRifle());
        EquipmentType.addType(new ISBAFlamer());
        EquipmentType.addType(new CLBAFlamer());
        EquipmentType.addType(new ISBATaser());

        // Cruise Missiles
        EquipmentType.addType(new ISCruiseMissile50());
        EquipmentType.addType(new ISCruiseMissile70());
        EquipmentType.addType(new ISCruiseMissile90());
        EquipmentType.addType(new ISCruiseMissile120());

        // Naval weapons
        EquipmentType.addType(new NL35Weapon());
        EquipmentType.addType(new NL45Weapon());
        EquipmentType.addType(new NL55Weapon());
        EquipmentType.addType(new LightNPPCWeapon());
        EquipmentType.addType(new MediumNPPCWeapon());
        EquipmentType.addType(new HeavyNPPCWeapon());
        EquipmentType.addType(new NAC10Weapon());
        EquipmentType.addType(new NAC20Weapon());
        EquipmentType.addType(new NAC25Weapon());
        EquipmentType.addType(new NAC30Weapon());
        EquipmentType.addType(new NAC35Weapon());
        EquipmentType.addType(new NAC40Weapon());
        EquipmentType.addType(new LightNGaussWeapon());
        EquipmentType.addType(new MediumNGaussWeapon());
        EquipmentType.addType(new HeavyNGaussWeapon());
        EquipmentType.addType(new BarracudaWeapon());
        EquipmentType.addType(new WhiteSharkWeapon());
        EquipmentType.addType(new KillerWhaleWeapon());
        EquipmentType.addType(new BarracudaTWeapon());
        EquipmentType.addType(new WhiteSharkTWeapon());
        EquipmentType.addType(new KillerWhaleTWeapon());
        EquipmentType.addType(new KrakenTWeapon());
        EquipmentType.addType(new AR10Weapon());
        EquipmentType.addType(new ScreenLauncherWeapon());
        EquipmentType.addType(new LightSCCWeapon());
        EquipmentType.addType(new MediumSCCWeapon());
        EquipmentType.addType(new HeavySCCWeapon());
        EquipmentType.addType(new SCL1Weapon());
        EquipmentType.addType(new SCL2Weapon());
        EquipmentType.addType(new SCL3Weapon());
        EquipmentType.addType(new PiranhaWeapon());
        EquipmentType.addType(new StingrayWeapon());
        EquipmentType.addType(new SwordfishWeapon());
        EquipmentType.addType(new MantaRayWeapon());

        // bomb-related weapons
        EquipmentType.addType(new AAAMissileWeapon());
        EquipmentType.addType(new ASMissileWeapon());
        EquipmentType.addType(new ASEWMissileWeapon());
        EquipmentType.addType(new LAAMissileWeapon());
        EquipmentType.addType(new BombArrowIV());
        EquipmentType.addType(new BombTAG());
        EquipmentType.addType(new BombISRL10());
        EquipmentType.addType(new AlamoMissileWeapon());
        EquipmentType.addType(new SpaceBombAttack());

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
        EquipmentType.addType(new AR10BayWeapon());
        EquipmentType.addType(new ScreenLauncherBayWeapon());
        EquipmentType.addType(new SCCBayWeapon());
        EquipmentType.addType(new SCLBayWeapon());
        EquipmentType.addType(new SubCapitalMissileBayWeapon());
        EquipmentType.addType(new MiscBayWeapon());
    }

    @Override
    public String toString() {
        return "WeaponType: " + name;
    }

    public int getExplosionDamage() {
        return explosionDamage;
    }

    @Override
    public double getCost(Entity entity, boolean isArmored) {
        if (isArmored) {
            double armoredCost = cost;
            armoredCost += 150000 * getCriticals(entity);

            return armoredCost;
        }

        return super.getCost(entity, isArmored);
    }

    @Override
    public double getBV(Entity entity) {
        double returnBV = bv;
        return returnBV;
    }
}
