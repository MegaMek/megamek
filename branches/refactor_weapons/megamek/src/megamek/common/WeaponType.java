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

//TODO add XML support back in.

/*import java.io.File;
import java.text.NumberFormat;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;  
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
*/
/**
 * A type of mech or vehicle weapon.  There is only one instance of this
 * weapon for all weapons of this type.
 */
public class WeaponType extends EquipmentType {
    public static final int     DAMAGE_MISSILE = -2;
    public static final int     DAMAGE_VARIABLE = -3;
    public static final int     DAMAGE_SPECIAL = -4;
    public static final int     DAMAGE_ARTILLERY = -5;
    public static final int     WEAPON_NA = Integer.MIN_VALUE;

    // weapon flags (note: many weapons can be identified by their ammo type)
    public static final long     F_DIRECT_FIRE        = 0x000000001L; // marks any weapon affected by a targetting computer
    public static final long     F_FLAMER             = 0x000000002L;
    public static final long     F_LASER              = 0x000000004L; // for eventual glazed armor purposes
    public static final long     F_PPC                = 0x000000008L; //              "
    public static final long     F_AUTO_TARGET        = 0x000000010L; // for weapons that target automatically (AMS)
    public static final long     F_NO_FIRES           = 0x000000020L; // cannot start fires
    public static final long     F_PROTOMECH          = 0x000000040L; // Protomech weapons, which need weird ammo stuff.
    public static final long     F_SOLO_ATTACK        = 0x000000080L; // must be only weapon attacking
    public static final long     F_SPLITABLE          = 0x000000100L; // Weapons that can be split between locations
    public static final long     F_MG                 = 0x000000200L; // MGL; for rapid fire set up
    public static final long     F_INFERNO            = 0x000000400L; // Inferno weapon
    public static final long     F_INFANTRY           = 0x000000800L; // small calibre weapon, no ammo, damage based on # men shooting
    public static final long     F_BATTLEARMOR        = 0x000001000L; // BA
    public static final long     F_DOUBLE_HITS        = 0x000002000L; // two shots hit per one rolled
    public static final long     F_MISSILE_HITS       = 0x000004000L; // use missile rules or # of hits
    public static final long     F_ONESHOT            = 0x000008000L; // weapon is oneShot.
    public static final long     F_ARTILLERY          = 0x000010000L;
    public static final long     F_BALLISTIC          = 0x000020000L; // For Gunnery/Ballistic skill
    public static final long     F_ENERGY             = 0x000040000L; // For Gunnery/Energy skill
    public static final long     F_MISSILE            = 0x000080000L; // For Gunnery/Missile skill
    public static final long     F_PLASMA             = 0x000100000L; // For fires
    public static final long     F_INCENDIARY_NEEDLES = 0x000200000L; // For fires
    public static final long     F_PROTOTYPE          = 0x000400000L; // for war of 3039 prototype weapons
    public static final long     F_HEATASDICE         = 0x000800000L; // heat is listed in dice, not points
    public static final long     F_AMS                = 0x001000000L; // Weapon is an anti-missile system.
    public static final long     F_BOOST_SWARM        = 0x002000000L; // boost leg & swarm
    public static final long     F_INFANTRY_ONLY      = 0x004000000L; // only target infantry
    public static final long     F_TAG                = 0x008000000L; // Target acquisition gear
    public static final long     F_C3M                = 0x010000000L; // C3 Master with Target acquisition gear
    public static final long     F_PLASMA_MFUK        = 0x020000000L; // Plasma Rifle
    public static final long     F_EXTINGUISHER       = 0x040000000L; // Fire extinguisher
    public static final long     F_SINGLE_TARGET      = 0x080000000L; // Does less damage to PBI in maxtech rules
    public static final long     F_PULSE              = 0x100000000L; // pulse weapons
    public static final long     F_BURST_FIRE         = 0x200000000L; // full damage vs infantry
    public static final long     F_MGA                = 0x400000000L; // machine gun array
    public static final long     F_NO_AIM             = 0x800000000L; // machine gun array

    //protected RangeType rangeL;
    protected int   heat;
    protected int   damage;
    public int      rackSize; // or AC size, or whatever
    public int      ammoType;

    public int      minimumRange;
    public int      shortRange;
    public int      mediumRange;
    public int      longRange;
    public int      extremeRange;
    public int      waterShortRange;
    public int      waterMediumRange;
    public int      waterLongRange;
    public int      waterExtremeRange;

    public void setDamage(int inD) {
        damage = inD;
    }

    public void setName (String inN) {
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

    public int getDamage() {
        return damage;
    }

    public int getRackSize() {
        return rackSize;
    }

    public int getAmmoType() {
        return ammoType;
    }

    public int[] getRanges() {
        return new int[] {minimumRange, shortRange, mediumRange,
                          longRange, extremeRange};
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
        return new int[] {minimumRange, waterShortRange, waterMediumRange,
                          waterLongRange, waterExtremeRange};
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

    /**
     * Add all the types of weapons we can create to the list
     */
    public static void initializeTypes() {
/*
        EquipmentType.addType(createISPlasmaRifle());

        EquipmentType.addType(createFireExtinguisher());
        EquipmentType.addType(createISSBGaussRifle());
        EquipmentType.addType(createISRAC10()); //MFUK
        EquipmentType.addType(createISRAC20()); //MFUK
        EquipmentType.addType(createISLAC10()); //MFUK
        EquipmentType.addType(createISLAC20()); //MFUK
        EquipmentType.addType(createISStreakMRM10()); //guessed from fluff
        EquipmentType.addType(createISStreakMRM20()); //guessed from fluff
        EquipmentType.addType(createISStreakMRM30()); //guessed from fluff
        EquipmentType.addType(createISStreakMRM40()); //guessed from fluff
        EquipmentType.addType(createISHawkSRM2()); //guessed from fluff
        EquipmentType.addType(createISHawkSRM4()); //guessed from fluff
        EquipmentType.addType(createISHawkSRM6()); //guessed from fluff
        EquipmentType.addType(createISPXLRM5()); //guessed from fluff
        EquipmentType.addType(createISPXLRM10()); //guessed from fluff
        EquipmentType.addType(createISPXLRM15()); //guessed from fluff
        EquipmentType.addType(createISPXLRM20()); //guessed from fluff
        EquipmentType.addType(createISMPod());

        EquipmentType.addType(createCLPlasmaCannon());
        EquipmentType.addType(createCLAPGaussRifle());

        EquipmentType.addType(createCLMPod());

        // Start BattleArmor weapons
        EquipmentType.addType(createBAMG());
        EquipmentType.addType(createBASingleMG());
        EquipmentType.addType(createBASingleFlamer());
        EquipmentType.addType(createBAFlamer());
        EquipmentType.addType(createBATwinFlamers());
        EquipmentType.addType(createBAInfernoSRM());
        EquipmentType.addType(createBACLMicroPulseLaser());
        EquipmentType.addType(createBAMicroBomb());
        EquipmentType.addType(createBACLERMicroLaser());
        EquipmentType.addType(createCLTorpedoLRM5());
        EquipmentType.addType(createBAISMediumPulseLaser());
        EquipmentType.addType(createTwinSmallPulseLaser());
        EquipmentType.addType(createTripleSmallLaser());
        EquipmentType.addType(createTripleMG());
        EquipmentType.addType(createBAAutoGL());
        EquipmentType.addType(createBAMagshotGR());
        EquipmentType.addType(createBAISMediumLaser());
        EquipmentType.addType(createBAISERSmallLaser());
        EquipmentType.addType(createBACompactNARC());
        EquipmentType.addType(createSlothSmallLaser());
        EquipmentType.addType(createBAMineLauncher());

        EquipmentType.addType(createBASupportPPC());
        EquipmentType.addType(createBABearhunterAC());
        EquipmentType.addType(createBATwinBearhunterAC());
        EquipmentType.addType(createBACLMediumPulseLaser());
        EquipmentType.addType(createBAIncendiaryNeedler());
        EquipmentType.addType(createBALightRecRifle());
        EquipmentType.addType(createBAKingDavidLightGaussRifle());
        EquipmentType.addType(createBAMediumRecRifle());
        EquipmentType.addType(createBAPlasmaRifle());
        EquipmentType.addType(createBASRM1());
        EquipmentType.addType(createBASRM2());
        EquipmentType.addType(createBASRM3());
        EquipmentType.addType(createBASRM5());
        EquipmentType.addType(createBASRM6());
        EquipmentType.addType(createBAVibroClaws1());
        EquipmentType.addType(createBAVibroClaws2());
        EquipmentType.addType(createBAHeavyMG());
        EquipmentType.addType(createBALightMG());
        EquipmentType.addType(createBACLHeavyMediumLaser());
        EquipmentType.addType(createBAHeavyRecRifle());
        EquipmentType.addType(createBACLHeavySmallLaser());
        EquipmentType.addType(createBACLERMediumLaser());
        EquipmentType.addType(createBAISERMediumLaser());
        EquipmentType.addType(createBACLSmallPulseLaser());
        EquipmentType.addType(createBAISLightMortar());
        EquipmentType.addType(createBAISHeavyMortar());
        EquipmentType.addType(createBAMicroGrenade());
        EquipmentType.addType(createBAGrandMaulerGauss());
        EquipmentType.addType(createBAAdvancedSRM1());
        EquipmentType.addType(createBAAdvancedSRM2());
        EquipmentType.addType(createBAAdvancedSRM3());
        EquipmentType.addType(createBAAdvancedSRM4());
        EquipmentType.addType(createBAAdvancedSRM5());
        EquipmentType.addType(createBAAdvancedSRM6());
        EquipmentType.addType(createBARL1());
        EquipmentType.addType(createBARL2());
        EquipmentType.addType(createBARL3());
        EquipmentType.addType(createBARL4());
        EquipmentType.addType(createBARL5());
        EquipmentType.addType(createISMRM1());
        EquipmentType.addType(createISMRM2());
        EquipmentType.addType(createISMRM3());
        EquipmentType.addType(createISMRM4());
        EquipmentType.addType(createISMRM5());
        EquipmentType.addType(createLRM1());
        EquipmentType.addType(createLRM2());
        EquipmentType.addType(createLRM3());
        EquipmentType.addType(createLRM4());
        */

        // Write out the weapons to XML.  Mostly because we can.
        //FIXME
        //writeWeaponsToXML(EquipmentType.getAllTypes(), "c:\\temp\\weapons.xml");
    }

    // Start BattleArmor weapons
    private static WeaponType createBAMG() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Machine Gun";
        weapon.setInternalName("BAMachineGun");
        weapon.addLookupName("BA-Machine Gun");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_BA_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 5;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    private static WeaponType createBASingleMG() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Machine Gun";
        weapon.setInternalName("BASingleMachineGun");
        weapon.addLookupName("BA-Single Machine Gun");
        weapon.heat = 0;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_BA_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 5;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC;

        return weapon;
    }
    private static WeaponType createBASingleFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Flamer";
        weapon.setInternalName("ISBASingleFlamer");
        weapon.addLookupName("IS BA-Single Flamer");
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_FLAMER | F_ENERGY;
        weapon.bv = 6;

        return weapon;
    }
    private static WeaponType createBAFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Flamer";
        weapon.setInternalName("BAFlamer");
        weapon.addLookupName("BA-Flamer");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 6;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_FLAMER | F_ENERGY;

        return weapon;
    }
    private static WeaponType createBATwinFlamers() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Twin Flamers";
        weapon.setInternalName("BATwinFlamers");
        weapon.addLookupName("BA-Twin Flamers");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 12;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_FLAMER | F_DOUBLE_HITS | F_ENERGY;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }
    private static WeaponType createBAInfernoSRM() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Inferno SRM";
        weapon.setInternalName("BAInfernoSRM");
        weapon.addLookupName("BA-Inferno SRM");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_BA_INFERNO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_BATTLEARMOR | F_INFERNO | F_MISSILE;

        return weapon;
    }
    private static WeaponType createBAMicroBomb() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Micro Bomb";
        weapon.setInternalName("BAMicroBomb");
        weapon.addLookupName("BA-Micro Bomb");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_BA_MICRO_BOMB;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_BATTLEARMOR | F_NO_FIRES;

        return weapon;
    }
    private static WeaponType createCLTorpedoLRM5() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Torpedo/LRM 5";
        weapon.setInternalName("CLTorpedoLRM5");
        weapon.addLookupName("Clan Torpedo/LRM-5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO_COMBO;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;        
        weapon.extremeRange = 28;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 55;

        return weapon;
    }
    private static WeaponType createTwinSmallPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Twin Small Pulse Lasers";
        weapon.setInternalName("TwinSmallPulseLaser");
        weapon.addLookupName("Twin Small Pulse Lasers");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_BA_SMALL_LASER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_NO_FIRES| F_ENERGY | F_DIRECT_FIRE | F_PULSE;
        weapon.bv = 24;

        return weapon;
    }
    private static WeaponType createTripleSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Triple Small Lasers";
        weapon.setInternalName("TripleSmallLaser");
        weapon.addLookupName("Triple Small Lasers");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_BA_SMALL_LASER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES| F_ENERGY;
        weapon.bv = 27;

        return weapon;
    }
    private static WeaponType createTripleMG() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Triple Machine Guns";
        weapon.setInternalName("TripleMachineGun");
        weapon.addLookupName("Triple Machine Guns");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_BA_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_MISSILE_HITS | F_DIRECT_FIRE| F_BALLISTIC;
        weapon.bv = 15;

        return weapon;
    }
    private static WeaponType createBAAutoGL() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Auto Grenade Launcher";
        weapon.setInternalName("BAAutoGL");
        weapon.addLookupName("BA-Auto GL");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_BA_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 1;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }
    private static WeaponType createBAMagshotGR() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Magshot Gauss Rifle";
        weapon.setInternalName("BAMagshotGR");
        weapon.addLookupName("BA-Magshot GR");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC | F_SINGLE_TARGET;

        return weapon;

    }
    private static WeaponType createCLAPGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "AP Gauss Rifle";
        weapon.setInternalName("CLAPGaussRifle");
        weapon.addLookupName("Clan AP Gauss Rifle");
        weapon.addLookupName("Clan Anti-Personnel Gauss Rifle");
        weapon.heat = 1;
        weapon.damage = 3;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_MAGSHOT;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.5f;
        weapon.explosive = true;
        weapon.criticals = 1;
        weapon.bv = 21;
        weapon.cost = 8500;
        weapon.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC | F_BURST_FIRE;

        return weapon;
    }


    private static WeaponType createBACompactNARC() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Compact Narc";
        weapon.setInternalName("BACompactNarc");
        weapon.addLookupName("BA-Compact Narc");
        weapon.heat = 0;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_NARC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 5;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 16;
        weapon.flags |= F_NO_FIRES | F_BATTLEARMOR | F_MISSILE;

        return weapon;
    }
    private static WeaponType createSlothSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Twin Small Lasers";
        weapon.setInternalName("SlothSmallLaser");
        weapon.addLookupName("Sloth Small Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_BA_SMALL_LASER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES | F_ENERGY | F_SINGLE_TARGET;
        weapon.bv = 18;

        return weapon;
    }
    private static WeaponType createBAMineLauncher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Mine Launcher";
        weapon.setInternalName(BattleArmor.MINE_LAUNCHER);
        weapon.addLookupName("BA-Mine Launcher");
        weapon.heat = 0;
        weapon.damage = DAMAGE_SPECIAL;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_MINE;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        String[] modes = { "Single", "2-shot", "3-shot", "4-shot" };
        weapon.setModes(modes);
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_SOLO_ATTACK;

        return weapon;
    }

    private static WeaponType createBATwinBearhunterAC() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Twin Bearhunter ACs";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Twin Bearhunter Superheavy ACs");
        weapon.heat = 0;
        weapon.toHitModifier = 1;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 1;
        weapon.longRange = 2;
        weapon.extremeRange = 2;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 8;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC | F_DOUBLE_HITS;

        return weapon;
    }

    private static WeaponType createBAIncendiaryNeedler() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Firedrake Needler";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Firedrake Incendiary Needler");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_INCENDIARY_NEEDLES | F_BATTLEARMOR | F_DIRECT_FIRE;
        weapon.bv = 2;

        return weapon;
    }

    private static WeaponType createBAKingDavidLightGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "King David Light Gauss Rifle";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-King David Light Gauss Rifle");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 7;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC | F_SINGLE_TARGET;

        return weapon;
    }

    private static WeaponType createBAPlasmaRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Plasma Rifle";
        weapon.setInternalName("BAPlasmaRifle");
        weapon.addLookupName("BA-Plasma Rifle");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_PLASMA | F_BATTLEARMOR | F_DIRECT_FIRE | F_ENERGY;
        weapon.bv = 12;

        return weapon;
    }


    private static WeaponType createBAVibroClaws1() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Single Vibroclaw";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Vibro Claws (1)");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR
            | F_NO_FIRES | F_BALLISTIC
            | F_BOOST_SWARM | F_INFANTRY_ONLY;

        return weapon;
    }

    private static WeaponType createBAVibroClaws2() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Vibroclaws";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Vibro Claws (2)");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR
            | F_NO_FIRES | F_BALLISTIC
            | F_BOOST_SWARM | F_INFANTRY_ONLY;

        return weapon;
    }
    
    // War Of 3039 Prototype weapons
    
    private static WeaponType createC3M() {
        WeaponType weapon = new WeaponType();
        
        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "C3 Master with TAG";
        weapon.setInternalName("ISC3MasterUnit");
        weapon.addLookupName("IS C3 Computer");
        weapon.addLookupName("ISC3MasterComputer");
        weapon.tonnage = 5;
        weapon.criticals = 5;
        weapon.hittable = true;
        weapon.spreadable = false;
        weapon.cost = 1500000;
        weapon.bv = 0;
        weapon.flags |= F_C3M | F_TAG | F_NO_FIRES;
        weapon.heat = 0;
        weapon.damage = 0;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        String[] modes = { "1-shot", "2-shot", "3-shot", "4-shot" };
        weapon.setModes( modes );
        
        return weapon;
    }
    
    private static WeaponType createBAHeavyMG() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Semi-Portable Autocannon";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-HeavyMG");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 2;
        weapon.extremeRange = 2;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 6;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    private static WeaponType createBALightMG() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Semi-Portable Machine Gun";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-LightMG");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 5;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    private static WeaponType createBAGrandMaulerGauss() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Grand Mauler Gauss Cannon";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-ISGrandMauler");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 5;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 6;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC | F_SINGLE_TARGET;

        return weapon;
    }

    private static WeaponType createBATsunamiGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Tsunami Heavy Gauss Rifle";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-ISTsunamiHeavyGaussRifle");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 5;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 6;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC | F_SINGLE_TARGET;

        return weapon;
    }

    private static WeaponType createBAMicroGrenade() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Micro Grenade Launcher";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-ISMicroGrenadeLauncher");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 2;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    private static WeaponType createBAISHeavyMortar() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Heavy Mortar";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-ISHeavyMortar");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = 2;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 17;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    private static WeaponType createBAISLightMortar() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Light Mortar";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-ISLightMortar");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = 1;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 9;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    private static WeaponType createISMRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "MRM 4";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISMRM4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_MISSILE;
        weapon.bv = 23;

        return weapon;
    }



    
    
    private static WeaponType createBADavidGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "David Light Gauss Rifle";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-ISDavidLightGauss");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 5;
        weapon.longRange = 8;
        weapon.extremeRange = 10;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 7;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC | F_SINGLE_TARGET;

        return weapon;
    
    }

    private static WeaponType createFireExtinguisher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Fire Extinguisher";
        weapon.setInternalName(weapon.name);
        weapon.heat = 0;
        weapon.damage = 0;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 1;
        weapon.longRange = 1; // No long range.
        weapon.extremeRange = 1; // No Extreme Range
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_SOLO_ATTACK | F_NO_FIRES | F_EXTINGUISHER;
        // Warning: this BV is unofficial.
        //FIXME
        weapon.bv = 0;
        weapon.cost = 0;

        return weapon;
    }

    private static WeaponType createISRAC10() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Rotary AC/10";
        weapon.setInternalName("ISRotaryAC10");
        weapon.addLookupName("IS Rotary AC/10");
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_ROTARY;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 16.0f;
        weapon.criticals = 12;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC;
        weapon.bv = 296;
        String[] modes = { "Single", "2-shot", "3-shot", "4-shot", "5-shot", "6-shot" };
        weapon.setModes(modes);

        // explosive when jammed
        weapon.explosive = true;
        weapon.cost = 450000;

        return weapon;
    }

    private static WeaponType createISRAC20() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Rotary AC/20";
        weapon.setInternalName("ISRotaryAC20");
        weapon.addLookupName("IS Rotary AC/20");
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_ROTARY;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 18.0f;
        weapon.criticals = 14;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC | F_SPLITABLE;
        weapon.bv = 474;
        String[] modes = { "Single", "2-shot", "3-shot", "4-shot", "5-shot", "6-shot" };
        weapon.setModes(modes);

        // explosive when jammed
        weapon.explosive = true;
        weapon.cost = 800000;

        return weapon;
    }

    private static WeaponType createISLAC10() {
        WeaponType weapon = new WeaponType();
    
        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Light Auto Cannon/10";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Light AutoCannon/10");
        weapon.addLookupName("ISLAC10");
        weapon.addLookupName("IS Light Autocannon/10");
        weapon.addLookupName("Light AC/10");
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_LAC;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 8.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC;
        weapon.bv = 74;
        weapon.cost = 225000;
        weapon.explosive = true; //when firing incendiary ammo

        return weapon;
    }
    
    private static WeaponType createISLAC20() {
        WeaponType weapon = new WeaponType();
    
        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Light Auto Cannon/20";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Light Auto Cannon/20");
        weapon.addLookupName("ISLAC20");
        weapon.addLookupName("IS Light Autocannon/20");
        weapon.addLookupName("Light AC/20");
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LAC;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 9.0f;
        weapon.criticals = 6;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC;
        weapon.bv = 118;
        weapon.cost = 325000;
        weapon.explosive = true; //when firing incendiary ammo
    
        return weapon;
    }
    private static WeaponType createISStreakMRM10() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Streak MRM 10";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("StreakMRM-10");
        weapon.addLookupName("ISStreakMRM10");
        weapon.addLookupName("IS Streak MRM 10");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_MRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 4.5f;
        weapon.criticals = 2;
        weapon.bv = 88;
        weapon.flags |= F_MISSILE;
        weapon.cost = 100000;

        return weapon;
    }

    private static WeaponType createISStreakMRM20() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Streak MRM 20";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("StreakMRM-20");
        weapon.addLookupName("ISStreakMRM20");
        weapon.addLookupName("IS Streak MRM 20");
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_MRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 10.5f;
        weapon.criticals = 3;
        weapon.bv = 177;
        weapon.flags |= F_MISSILE;
        weapon.cost = 250000;

        return weapon;
    }

    private static WeaponType createISStreakMRM30() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Streak MRM 30";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("StreakMRM-30");
        weapon.addLookupName("ISStreakMRM30");
        weapon.addLookupName("IS Streak MRM 30");
        weapon.heat = 10;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 30;
        weapon.ammoType = AmmoType.T_MRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 15.0f;
        weapon.criticals = 6;
        weapon.bv = 265;
        weapon.flags |= F_MISSILE;
        weapon.cost = 450000;

        return weapon;
    }

    private static WeaponType createISStreakMRM40() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Streak MRM 40";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("StreakMRM-40");
        weapon.addLookupName("ISStreakMRM40");
        weapon.addLookupName("IS Streak MRM 40");
        weapon.heat = 12;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 40;
        weapon.ammoType = AmmoType.T_MRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 18.0f;
        weapon.criticals = 8;
        weapon.bv = 353;
        weapon.flags |= F_MISSILE;
        weapon.cost = 700000;

        return weapon;
    }
    
    private static WeaponType createISHawkSRM2() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Hawk SRM 2";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISHawkSRM2");
        weapon.addLookupName("IS Hawk SRM 2");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_HSRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_NO_FIRES | F_MISSILE;
        weapon.bv = 28;
        weapon.cost = 20000;

        return weapon;
    }

    private static WeaponType createISHawkSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Hawk SRM 4";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISHawkSRM4");
        weapon.addLookupName("IS Hawk SRM 4");
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_HSRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.flags |= F_NO_FIRES | F_MISSILE;
        weapon.bv = 52;
        weapon.cost = 120000;

        return weapon;
    }

    private static WeaponType createISHawkSRM6() {
        WeaponType weapon = new WeaponType();
        
        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Hawk SRM 6";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISHawkSRM6");
        weapon.addLookupName("IS Hawk SRM 6");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_HSRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.flags |= F_NO_FIRES | F_MISSILE;
        weapon.bv = 79;
        weapon.cost = 160000;
        
        return weapon;
    }

    private static WeaponType createISPXLRM5() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Phoenix LRM 5";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISPhoenixLRM5");
        weapon.addLookupName("IS Phoenix LRM 5");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = -1;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_PXLRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 56;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;
        weapon.cost = 60000;

        return weapon;
    }

    private static WeaponType createISPXLRM10() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Phoenix LRM 10";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISPhoenixLRM10");
        weapon.addLookupName("IS Phoenix LRM 10");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = -1;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_PXLRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.bv = 111;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;
        weapon.cost = 200000;

        return weapon;
    }

    private static WeaponType createISPXLRM15() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Phoenix LRM 15";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISPhoenixLRM15");
        weapon.addLookupName("IS Phoenix LRM 15");
        weapon.heat = 5;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = -1;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_PXLRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.bv = 167;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;
        weapon.cost = 350000;

        return weapon;
    }

    private static WeaponType createISPXLRM20() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Phoenix LRM 20";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISPhoenixLRM20");
        weapon.addLookupName("IS Phoenix LRM 20");
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = -1;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_PXLRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.bv = 223;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;
        weapon.cost = 500000;

        return weapon;
    }

    private static WeaponType createISMPod() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "M-Pod";
        weapon.setInternalName("ISMPod");
        weapon.addLookupName("ISMPod");
        weapon.addLookupName("ISM-Pod");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_MPOD;
        weapon.minimumRange = 0;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC | F_ONESHOT;
        weapon.bv = 5;
        weapon.cost = 6000;

        return weapon;
    }
    
    private static WeaponType createCLMPod() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_3;
        weapon.name = "M-Pod";
        weapon.setInternalName("CLMPod");
        weapon.addLookupName("CLMPod");
        weapon.addLookupName("CLM-Pod");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_MPOD;
        weapon.minimumRange = 0;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC | F_ONESHOT;
        weapon.bv = 5;
        weapon.cost = 6000;

        return weapon;
    }    

    private static WeaponType createISSBGaussRifle() {
        WeaponType weapon = new WeaponType();
    
        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Silver Bullet Gauss Rifle";
        weapon.setInternalName("ISSBGaussRifle");
        weapon.addLookupName("ISSBGaussRifle");
        weapon.heat = 1;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_SBGAUSS;
        weapon.minimumRange = 2;
        weapon.shortRange = 7;
        weapon.mediumRange = 15;
        weapon.longRange = 22;
        weapon.extremeRange = 30;
        weapon.tonnage = 15.0f;
        weapon.criticals = 7;
        weapon.flags |= F_DIRECT_FIRE | F_NO_FIRES | F_BALLISTIC | F_MISSILE_HITS;
        weapon.explosive = true;
        weapon.bv = 169;
        weapon.cost = 350000;
    
        return weapon;
    }

    private static WeaponType createCLPlasmaCannon() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Plasma Cannon";
        weapon.setInternalName("CLPlasmaCannon");
        weapon.heat = 7;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_PLASMA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 3.0f;
        weapon.criticals = 1;
        weapon.flags |= F_DIRECT_FIRE | F_ENERGY | F_BURST_FIRE;
        weapon.bv = 170;
        weapon.cost = 480000;

        return weapon;
    }

    private static WeaponType createISPlasmaRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Plasma Rifle";
        weapon.setInternalName("ISPlasmaRifle");
        weapon.heat = 10;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_PLASMA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 6.0f;
        weapon.criticals = 2;
        weapon.flags |= F_DIRECT_FIRE | F_ENERGY | F_BURST_FIRE;
        weapon.bv = 210;
        weapon.cost = 480000;

        return weapon;
    }


    public String toString() {
        return "WeaponType: " + name;
    }
}
