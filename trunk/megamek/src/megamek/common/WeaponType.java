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
    //public static final long F_UNUSED = 1l << 24;
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
    public static final long F_B_POD          = 1l << 36;
    public static final long F_TASER          = 1l << 37;
    public static final long F_BA_WEAPON      = 1l << 38; //Currently only used by MegaMekLab
    public static final long F_ANTI_SHIP      = 1l << 39; //for anti-ship missiles
    public static final long F_SPACE_BOMB     = 1l << 40;
    
//  add maximum range for AT2
    public static final int     RANGE_SHORT = 1;
    public static final int     RANGE_MED   = 2;
    public static final int     RANGE_LONG  = 3;
    public static final int     RANGE_EXT   = 4;
    
    //add weapon classes for AT2
    public static final int     CLASS_NONE = 0;
    public static final int     CLASS_LASER = 1;
    public static final int     CLASS_POINT_DEFENSE = 2;
    public static final int     CLASS_PPC = 3;
    public static final int     CLASS_PULSE_LASER = 4;
    public static final int     CLASS_ARTILLERY = 5;
    public static final int     CLASS_PLASMA = 6;
    public static final int     CLASS_AC = 7;
    public static final int     CLASS_LBX_AC = 8;
    public static final int     CLASS_LRM = 9;
    public static final int     CLASS_SRM = 10;
    public static final int     CLASS_MRM = 11;
    public static final int     CLASS_MML = 12;
    public static final int     CLASS_ATM = 13;
    public static final int     CLASS_ROCKET_LAUNCHER = 14;
    public static final int     CLASS_CAPITAL_LASER = 15;
    public static final int     CLASS_CAPITAL_PPC = 16;
    public static final int     CLASS_CAPITAL_AC = 17;
    public static final int     CLASS_CAPITAL_GAUSS = 18;
    public static final int     CLASS_CAPITAL_MISSILE = 19;
    public static final int     CLASS_AR10 = 20;
    public static final int     CLASS_SCREEN = 21;
    public static final int     CLASS_SUB_CAPITAL_CANNON = 22;
    public static final int     NUM_CLASSES = 23;
    
    public static String[] classNames = {"Unknown", "Laser","Point Defense", "PPC","Pulse Laser",
                                         "Artilery","AMS","AC","LBX","LRM","SRM","MRM","ATM",
                                         "Rocket Launcher","Capital Laser","Capital PPC",
                                         "Capital AC", "Capital Gauss", "Capital Missile",
                                         "AR10","Screen", "Sub Capital Cannon"};
    
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

    //get stuff for AT2
    //separate attack value by range. It will make weapon bays easier
    public double      shortAV = 0.0;
    public double      medAV = 0;
    public double      longAV = 0;
    public double      extAV = 0;
    public int      maxRange = RANGE_SHORT;
    public boolean  capital = false;
    public boolean subCapital = false;
    public int     atClass = CLASS_NONE;
    
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
        if (this.getAmmoType() == AmmoType.T_ATM) {
            AmmoType atype = (AmmoType) weapon.getLinked().getType();
            if ((atype.getAmmoType() == AmmoType.T_ATM) && atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                minRange = 4;
                sRange = 9;
                mRange = 18;
                lRange = 27;
                eRange = 36;
            } else if ((atype.getAmmoType() == AmmoType.T_ATM) && atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                minRange = 0;
                sRange = 3;
                mRange = 6;
                lRange = 9;
                eRange = 12;
            }
        }
        if (this.getAmmoType() == AmmoType.T_MML) {
            AmmoType atype = (AmmoType) weapon.getLinked().getType();
            if (atype.hasFlag(AmmoType.F_MML_LRM) || this.getAmmoType() == AmmoType.T_LRM_TORPEDO) {
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
        return new int[] { minimumRange, waterShortRange, waterMediumRange,
                waterLongRange, waterExtremeRange };
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
        if(capital) {
            return new int[] { 0, 12, 24, 40, 50 };
        } else {
            return new int[] { 0, 6, 12, 20, 25 };
        }
  
    }
    
    public double getShortAV() {
        return shortAV;
    }
    
    public int getRoundShortAV() {
        return (int)Math.ceil(shortAV);
    }
    
    public double getMedAV() {
        return medAV;
    }
    
    public int getRoundMedAV() {
        return (int)Math.ceil(medAV);
    }
    
    public double getLongAV() {
        return longAV;
    }
    
    public int getRoundLongAV() {
        return (int)Math.ceil(longAV);
    }
    
    public double getExtAV() {
        return extAV;
    }
    
    public int getRoundExtAV() {
        return (int)Math.ceil(extAV);
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
        //return the correct weapons bay for the given type of weapon
        switch(atclass) {
        case(CLASS_CAPITAL_AC):
            return EquipmentType.get("Sub-Capital Cannon Bay");
        case(CLASS_CAPITAL_LASER):
            return EquipmentType.get("Sub-Capital Laser Bay");
        default:
            return getBayType(atclass);
        }
    }
    
    //Probably not the best place for this
    public static EquipmentType getBayType(int atclass) {    
        //return the correct weapons bay for the given type of weapon
        switch(atclass) {
        case(CLASS_LASER):
            return EquipmentType.get("Laser Bay");
        case(CLASS_POINT_DEFENSE):
            return EquipmentType.get("Point Defense Bay");
        case(CLASS_PPC):
            return EquipmentType.get("PPC Bay");
        case(CLASS_PULSE_LASER):
            return EquipmentType.get("Pulse Laser Bay");
        case(CLASS_ARTILLERY):
            return EquipmentType.get("Artillery Bay");
        case(CLASS_PLASMA):
            return EquipmentType.get("Plasma Bay");
        case(CLASS_AC):
            return EquipmentType.get("AC Bay");
        case(CLASS_LBX_AC):
            return EquipmentType.get("LBX AC Bay");
        case(CLASS_LRM):
            return EquipmentType.get("LRM Bay");
        case(CLASS_SRM):
            return EquipmentType.get("SRM Bay");
        case(CLASS_MRM):
            return EquipmentType.get("MRM Bay");
        case(CLASS_MML):
            return EquipmentType.get("MML Bay");
        case(CLASS_ATM):
            return EquipmentType.get("ATM Bay");
        case(CLASS_ROCKET_LAUNCHER):
            return EquipmentType.get("Rocket Launcher Bay");
        case(CLASS_CAPITAL_LASER):
            return EquipmentType.get("Capital Laser Bay");
        case(CLASS_CAPITAL_PPC):
            return EquipmentType.get("Capital PPC Bay");
        case(CLASS_CAPITAL_AC):
            return EquipmentType.get("Capital AC Bay");
        case(CLASS_CAPITAL_GAUSS):
            return EquipmentType.get("Capital Gauss Bay");
        case(CLASS_CAPITAL_MISSILE):
            return EquipmentType.get("Capital Missile Bay");
        case(CLASS_AR10):
            return EquipmentType.get("AR10 Bay");
        case(CLASS_SCREEN):
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
        addType(new ISMediumLaser());
        addType(new ISLargeLaser());
        addType(new ISSmallLaser());
        addType(new ISLargePulseLaser());
        addType(new ISLargeXPulseLaser());
        addType(new ISERLargeLaser());
        addType(new ISERLargeLaserPrototype());
        addType(new ISERMediumLaser());
        addType(new ISMediumPulseLaser());
        addType(new ISMediumPulseLaserPrototype());
        addType(new ISMediumXPulseLaser());
        addType(new ISSmallPulseLaser());
        addType(new ISSmallXPulseLaser());
        addType(new ISERSmallLaser());
        addType(new ISMediumVariableSpeedPulseLaser());
        addType(new ISSmallVariableSpeedPulseLaser());
        addType(new ISLargeVariableSpeedPulseLaser());
        addType(new ISBinaryLaserCannon());
        addType(new ISBombastLaser());
        addType(new CLERLargeLaser());
        addType(new CLHeavyLargeLaser());
        addType(new CLLargePulseLaser());
        addType(new CLERLargePulseLaser());
        addType(new CLERMediumLaser());
        addType(new CLHeavyMediumLaser());
        addType(new CLMediumPulseLaser());
        addType(new CLERMediumPulseLaser());
        addType(new CLERSmallLaser());
        addType(new CLSmallPulseLaser());
        addType(new CLERSmallPulseLaser());
        addType(new CLHeavySmallLaser());
        addType(new CLERMicroLaser());
        addType(new CLMicroPulseLaser());
        addType(new CLImprovedHeavyLargeLaser());
        addType(new CLImprovedHeavyMediumLaser());
        addType(new CLImprovedHeavySmallLaser());
        addType(new CLLargeChemicalLaser());
        addType(new CLMediumChemicalLaser());
        addType(new CLSmallChemicalLaser());
        // PPC types
        addType(new ISPPC());
        addType(new ISERPPC());
        addType(new ISEHERPPC());
        addType(new CLERPPC());
        addType(new ISSnubNosePPC());
        addType(new ISLightPPC());
        addType(new ISHeavyPPC());
        addType(new ISHERPPC());
        addType(new ISSupportPPC());
        addType(new CLSupportPPC());
        // Flamers
        addType(new CLFlamer());
        addType(new ISFlamer());
        addType(new CLVehicleFlamer());
        addType(new ISVehicleFlamer());
        addType(new ISHeavyFlamer());
        // Autocannons
        addType(new ISAC2());
        addType(new ISAC5());
        addType(new ISAC10());
        addType(new ISAC20());
        // Ultras
        addType(new ISUAC2());
        addType(new ISUAC5());
        addType(new ISUAC5Prototype());
        addType(new ISUAC10());
        addType(new ISUAC20());
        addType(new ISTHBUAC2());
        addType(new ISTHBUAC10());
        addType(new ISTHBUAC20());
        addType(new CLUAC2());
        addType(new CLUAC5());
        addType(new CLUAC10());
        addType(new CLUAC20());
        // LBXs
        addType(new ISLB2XAC());
        addType(new ISLB5XAC());
        addType(new ISLB10XAC());
        addType(new ISLB10XACPrototype());
        addType(new ISLB20XAC());
        addType(new CLLB2XAC());
        addType(new CLLB5XAC());
        addType(new CLLB10XAC());
        addType(new CLLB20XAC());
        addType(new ISTHBLB2XAC());
        addType(new ISTHBLB5XAC());
        addType(new ISTHBLB20XAC());
        // RACs
        addType(new ISRAC2());
        addType(new ISRAC5());
        // LACs
        addType(new ISLAC2());
        addType(new ISLAC5());
        addType(new ISLAC10());
        addType(new ISLAC20());
        //HVACs
        addType(new ISHVAC2());
        addType(new ISHVAC5());
        addType(new ISHVAC10());
        // Gausses
        addType(new ISGaussRifle());
        addType(new ISGaussRiflePrototype());
        addType(new ISSilverBulletGauss());
        addType(new CLGaussRifle());
        addType(new ISLGaussRifle());
        addType(new ISHGaussRifle());
        addType(new ISIHGaussRifle());
        addType(new CLHAG20());
        addType(new CLHAG30());
        addType(new CLHAG40());
        addType(new CLAPGaussRifle());
        // MGs
        addType(new ISMG());
        addType(new ISLightMG());
        addType(new ISHeavyMG());
        addType(new ISMGA());
        addType(new ISLightMGA());
        addType(new ISHeavyMGA());
        addType(new CLMG());
        addType(new CLLightMG());
        addType(new CLHeavyMG());
        addType(new CLMGA());
        addType(new CLLightMGA());
        addType(new CLHeavyMGA());
        // LRMs
        addType(new ISLRM1());
        addType(new ISLRM1OS());
        addType(new ISLRM2());
        addType(new ISLRM2OS());
        addType(new ISLRM3());
        addType(new ISLRM3OS());
        addType(new ISLRM4());
        addType(new ISLRM4OS());
        addType(new ISLRM5());
        addType(new ISLRM10());
        addType(new ISLRM15());
        addType(new ISLRM20());
        addType(new ISLRM5OS());
        addType(new ISLRM10OS());
        addType(new ISLRM15OS());
        addType(new ISLRM20OS());
        addType(new CLLRM1());
        addType(new CLLRM1OS());
        addType(new CLLRM2());
        addType(new CLLRM2OS());
        addType(new CLLRM3());
        addType(new CLLRM3OS());
        addType(new CLLRM4());
        addType(new CLLRM4OS());
        addType(new CLLRM5());
        addType(new CLLRM6());
        addType(new CLLRM7());
        addType(new CLLRM8());
        addType(new CLLRM9());
        addType(new CLLRM10());
        addType(new CLLRM11());
        addType(new CLLRM12());
        addType(new CLLRM13());
        addType(new CLLRM14());
        addType(new CLLRM15());
        addType(new CLLRM16());
        addType(new CLLRM17());
        addType(new CLLRM18());
        addType(new CLLRM19());
        addType(new CLLRM20());
        addType(new CLLRM5OS());
        addType(new CLLRM10OS());
        addType(new CLLRM15OS());
        addType(new CLLRM20OS());
        addType(new CLStreakLRM5());
        addType(new CLStreakLRM10());
        addType(new CLStreakLRM15());
        addType(new CLStreakLRM20());
        addType(new CLStreakLRM5OS());
        addType(new CLStreakLRM10OS());
        addType(new CLStreakLRM15OS());
        addType(new CLStreakLRM20OS());
        // LRTs
        addType(new ISLRT5());
        addType(new ISLRT10());
        addType(new ISLRT15());
        addType(new ISLRT20());
        addType(new ISLRT5OS());
        addType(new ISLRT10OS());
        addType(new ISLRT15OS());
        addType(new ISLRT20OS());
        addType(new CLLRT1());
        addType(new CLLRT2());
        addType(new CLLRT3());
        addType(new CLLRT4());
        addType(new CLLRT5());
        addType(new CLLRT6());
        addType(new CLLRT7());
        addType(new CLLRT8());
        addType(new CLLRT9());
        addType(new CLLRT10());
        addType(new CLLRT11());
        addType(new CLLRT12());
        addType(new CLLRT13());
        addType(new CLLRT14());
        addType(new CLLRT15());
        addType(new CLLRT16());
        addType(new CLLRT17());
        addType(new CLLRT18());
        addType(new CLLRT19());
        addType(new CLLRT20());
        addType(new CLLRT5OS());
        addType(new CLLRT10OS());
        addType(new CLLRT15OS());
        addType(new CLLRT20OS());
        // SRMs
        addType(new ISSRM1());
        addType(new ISSRM2());
        addType(new ISSRM3());
        addType(new ISSRM4());
        addType(new ISSRM5());
        addType(new ISSRM6());
        addType(new ISSRM1OS());
        addType(new ISSRM2OS());
        addType(new ISSRM3OS());
        addType(new ISSRM4OS());
        addType(new ISSRM5OS());
        addType(new ISSRM6OS());
        addType(new CLSRM1());
        addType(new CLSRM1OS());
        addType(new CLSRM2());
        addType(new CLSRM3());
        addType(new CLSRM3OS());
        addType(new CLSRM4());
        addType(new CLSRM5());
        addType(new CLSRM5OS());
        addType(new CLSRM6());
        addType(new CLSRM2OS());
        addType(new CLSRM4OS());
        addType(new CLSRM6OS());
        addType(new CLAdvancedSRM1());
        addType(new CLAdvancedSRM1OS());
        addType(new CLAdvancedSRM2());
        addType(new CLAdvancedSRM2OS());
        addType(new CLAdvancedSRM3());
        addType(new CLAdvancedSRM3OS());
        addType(new CLAdvancedSRM4());
        addType(new CLAdvancedSRM4OS());
        addType(new CLAdvancedSRM5());
        addType(new CLAdvancedSRM5OS());
        addType(new CLAdvancedSRM6());
        addType(new CLAdvancedSRM6OS());
        addType(new ISStreakSRM2());
        addType(new ISStreakSRM4());
        addType(new ISStreakSRM6());
        addType(new ISStreakSRM2OS());
        addType(new ISStreakSRM4OS());
        addType(new ISStreakSRM6OS());
        addType(new CLStreakSRM1());
        addType(new CLStreakSRM2());
        addType(new CLStreakSRM3());
        addType(new CLStreakSRM4());
        addType(new CLStreakSRM5());
        addType(new CLStreakSRM6());
        addType(new CLStreakSRM2OS());
        addType(new CLStreakSRM4OS());
        addType(new CLStreakSRM6OS());
        // SRTs
        addType(new ISSRT2());
        addType(new ISSRT4());
        addType(new ISSRT6());
        addType(new ISSRT2OS());
        addType(new ISSRT4OS());
        addType(new ISSRT6OS());
        addType(new CLSRT2());
        addType(new CLSRT4());
        addType(new CLSRT6());
        addType(new CLSRT2OS());
        addType(new CLSRT4OS());
        addType(new CLSRT6OS());
        // RLs
        addType(new ISRL1());
        addType(new ISRL2());
        addType(new ISRL3());
        addType(new ISRL4());
        addType(new ISRL5());
        addType(new ISRL10());
        addType(new ISRL15());
        addType(new ISRL20());
        // ATMs
        addType(new CLATM3());
        addType(new CLATM6());
        addType(new CLATM9());
        addType(new CLATM12());
        // MRMs
        addType(new ISMRM1());
        addType(new ISMRM2());
        addType(new ISMRM3());
        addType(new ISMRM4());
        addType(new ISMRM5());
        addType(new ISMRM10());
        addType(new ISMRM20());
        addType(new ISMRM30());
        addType(new ISMRM40());
        addType(new ISMRM10OS());
        addType(new ISMRM20OS());
        addType(new ISMRM30OS());
        addType(new ISMRM40OS());
        // NARCs
        addType(new ISNarc());
        addType(new ISNarcOS());
        addType(new CLNarc());
        addType(new CLNarcOS());
        addType(new ISImprovedNarc());
        addType(new ISImprovedNarcOS());
        // AMSs
        addType(new ISAMS());
        addType(new ISLaserAMS());
        addType(new ISLaserAMSTHB());
        addType(new CLAMS());
        addType(new CLLaserAMS());
        // TAGs
        addType(new ISLightTAG());
        addType(new ISTAG());
        addType(new ISC3M());
        addType(new CLLightTAG());
        addType(new CLTAG());
        // MMLs
        addType(new ISMML3());
        addType(new ISMML5());
        addType(new ISMML7());
        addType(new ISMML9());
        // Arty
        addType(new ISLongTom());
        addType(new ISThumper());
        addType(new ISSniper());
        addType(new ISArrowIV());
        addType(new CLLongTom());
        addType(new CLSniper());
        addType(new CLThumper());
        addType(new CLArrowIV());
        // MFUK weapons
        addType(new CLPlasmaRifle());
        addType(new CLRAC2());
        addType(new CLRAC5());
        addType(new CLRAC10());
        addType(new CLRAC20());
        // misc lvl3 stuff
        addType(new ISRailGun());
        // MapPack Solaris VII
        addType(new ISMagshotGaussRifle());
        addType(new CLMagshotGaussRifle());
        addType(new ISMPod());
        addType(new CLMPod());
        addType(new ISBPod());
        addType(new CLBPod());
        // Thunderbolts
        addType(new ISThunderBolt5());
        addType(new ISThunderBolt10());
        addType(new ISThunderBolt15());
        addType(new ISThunderBolt20());
        // Infantry Attacks
        addType(new LegAttack());
        addType(new SwarmAttack());
        addType(new StopSwarmAttack());

        // Infantry Weapons
        addType(new InfantryRifleWeapon());
        addType(new InfantrySRMWeapon());
        addType(new InfantryInfernoSRMWeapon());
        addType(new InfantryLRMWeapon());
        addType(new InfantryLaserWeapon());
        addType(new InfantryFlamerWeapon());
        addType(new InfantryMGWeapon());
        addType(new ISFireExtinguisher());
        addType(new CLFireExtinguisher());

        // plasma weapons
        addType(new ISPlasmaRifle());
        addType(new CLPlasmaCannon());
        
        //MekMortarWeapons
        addType(new ISMekMortar1());
        addType(new ISMekMortar2());
        addType(new ISMekMortar4());
        addType(new ISMekMortar8());
        addType(new CLMekMortar1());
        addType(new CLMekMortar2());
        addType(new CLMekMortar4());
        addType(new CLMekMortar8());
        
        // BA weapons
        addType(new CLSmallLaser());
        addType(new ISLightRecoillessRifle());
        addType(new ISMediumRecoillessRifle());
        addType(new ISHeavyRecoillessRifle());
        addType(new CLLightRecoillessRifle());
        addType(new CLMediumRecoillessRifle());
        addType(new CLHeavyRecoillessRifle());
        addType(new CLBearhunterSuperheavyAC());
        addType(new ISKingDavidLightGaussRifle());
        addType(new ISDavidLightGaussRifle());
        addType(new ISFiredrakeNeedler());
        addType(new ISBAPlasmaRifle());
        addType(new ISHeavyMortar());
        addType(new ISLightMortar());
        addType(new ISAutoGrenadeLauncher());
        addType(new ISMicroGrenadeLauncher());
        addType(new CLHeavyGrenadeLauncher());
        addType(new ISCompactNarc());
        addType(new ISMineLauncher());
        addType(new CLMicroBomb());
        addType(new ISGrandMaulerGaussCannon());
        addType(new ISTsunamiGaussRifle());
        addType(new ISBAMG());
        addType(new ISBALightMG());
        addType(new ISBAHeavyMG());
        addType(new CLBAMG());
        addType(new CLBALightMG());
        addType(new CLBAHeavyMG());
        addType(new ISBAMagshotGaussRifle());
        addType(new CLBAAPGaussRifle());
        addType(new ISBAFlamer());
        addType(new CLBAFlamer());
        addType(new ISBATaser());
        
        //Naval weapons
        addType(new NL35Weapon());
        addType(new NL45Weapon());
        addType(new NL55Weapon());
        addType(new LightNPPCWeapon());
        addType(new MediumNPPCWeapon());
        addType(new HeavyNPPCWeapon());
        addType(new NAC10Weapon());
        addType(new NAC20Weapon());
        addType(new NAC25Weapon());
        addType(new NAC30Weapon());
        addType(new NAC35Weapon());
        addType(new NAC40Weapon());
        addType(new LightNGaussWeapon());
        addType(new MediumNGaussWeapon());
        addType(new HeavyNGaussWeapon());
        addType(new BarracudaWeapon());
        addType(new WhiteSharkWeapon());
        addType(new KillerWhaleWeapon());
        addType(new BarracudaTWeapon());
        addType(new WhiteSharkTWeapon());
        addType(new KillerWhaleTWeapon());
        addType(new KrakenTWeapon());
        addType(new AR10Weapon());
        addType(new ScreenLauncherWeapon());
        addType(new LightSCCWeapon());
        addType(new MediumSCCWeapon());
        addType(new HeavySCCWeapon());
        addType(new SCL1Weapon());
        addType(new SCL2Weapon());
        addType(new SCL3Weapon());
        addType(new PiranhaWeapon());
        addType(new StingrayWeapon());
        addType(new SwordfishWeapon());
        addType(new MantaRayWeapon());
        
        //bomb-related weapons
        addType(new AAAMissileWeapon());
        addType(new ASMissileWeapon());
        addType(new ASEWMissileWeapon());
        addType(new LAAMissileWeapon());
        addType(new BombArrowIV());
        addType(new BombTAG());
        addType(new BombISRL10());
        addType(new AlamoMissileWeapon());
        addType(new SpaceBombAttack());
        
        //Weapon Bays
        addType(new LaserBayWeapon());
        addType(new PointDefenseBayWeapon());
        addType(new PPCBayWeapon());
        addType(new PulseLaserBayWeapon());
        addType(new ArtilleryBayWeapon());
        addType(new PlasmaBayWeapon());
        addType(new ACBayWeapon());
        addType(new LBXBayWeapon());
        addType(new LRMBayWeapon());
        addType(new SRMBayWeapon());
        addType(new MRMBayWeapon());
        addType(new MMLBayWeapon());
        addType(new ATMBayWeapon());
        addType(new RLBayWeapon());
        addType(new CapitalLaserBayWeapon());
        addType(new CapitalACBayWeapon());
        addType(new CapitalGaussBayWeapon());
        addType(new CapitalPPCBayWeapon());
        addType(new CapitalMissileBayWeapon());
        addType(new AR10BayWeapon());
        addType(new ScreenLauncherBayWeapon());
        addType(new SCCBayWeapon());
        addType(new SCLBayWeapon());
        addType(new SubCapitalMissileBayWeapon());
        addType(new MiscBayWeapon());
    }

    public String toString() {
        return "WeaponType: " + name;
    }
    
    public int getExplosionDamage() {
        return explosionDamage;
    }
}
