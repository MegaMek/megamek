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

    public String toString() {
        return "WeaponType: " + name;
    }
}
