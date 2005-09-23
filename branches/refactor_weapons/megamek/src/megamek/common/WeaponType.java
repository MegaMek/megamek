/**
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
    public static final int     F_DIRECT_FIRE        = 0x00000001; // marks any weapon affected by a targetting computer
    public static final int     F_FLAMER             = 0x00000002;
    public static final int     F_LASER              = 0x00000004; // for eventual glazed armor purposes
    public static final int     F_PPC                = 0x00000008; //              "
    public static final int     F_AUTO_TARGET        = 0x00000010; // for weapons that target automatically (AMS)
    public static final int     F_NO_FIRES           = 0x00000020; // cannot start fires
    public static final int     F_PROTOMECH          = 0x00000040; //Protomech weapons, which need weird ammo stuff.
    public static final int     F_SOLO_ATTACK        = 0x00000080; // must be only weapon attacking
    public static final int     F_SPLITABLE          = 0x00000100; // Weapons that can be split between locations
    public static final int     F_MG                 = 0x00000200; // MG; for rapid fire set up
    public static final int     F_INFERNO            = 0x00000400; // Inferno weapon
    public static final int     F_INFANTRY           = 0x00000800; // small calibre weapon, no ammo, damage based on # men shooting
    public static final int     F_BATTLEARMOR        = 0x00001000; // multiple shots resolved in one to-hit (kinda like RAC, only not)
    public static final int     F_DOUBLE_HITS        = 0x00002000; // two shots hit per one rolled
    public static final int     F_MISSILE_HITS       = 0x00004000; // use missile rules or # of hits
    public static final int     F_ONESHOT            = 0x00008000; //weapon is oneShot.
    public static final int     F_ARTILLERY          = 0x00010000;
    public static final int     F_BALLISTIC          = 0x00020000; // For Gunnery/Ballistic skill
    public static final int     F_ENERGY             = 0x00040000; // For Gunnery/Energy skill
    public static final int     F_MISSILE            = 0x00080000; // For Gunnery/Missile skill
    public static final int     F_PLASMA             = 0x00100000; // For fires
    public static final int     F_INCENDIARY_NEEDLES = 0x00200000; // For fires
    public static final int     F_PROTOTYPE          = 0x00400000; // for war of 3039 prototype weapons
    public static final int     F_HEATASDICE         = 0x00800000; // heat is listed in dice, not points
    public static final int     F_AMS                = 0x01000000; // Weapon is an anti-missile system.
    public static final int     F_BOOST_SWARM        = 0x02000000; // boost leg & swarm
    public static final int     F_INFANTRY_ONLY      = 0x04000000; // only target infantry
    public static final int     F_TAG                = 0x08000000; // Target acquisition gear
    public static final int     F_C3M                = 0x10000000; // C3 Master with Target acquisition gear
    public static final int     F_PLASMA_MFUK        = 0x20000000; // Plasma Rifle

    protected RangeType range;
    protected int   heat;
    protected int   damage;
    protected int   rackSize; // or AC size, or whatever
    protected int   ammoType;

    protected int   minimumRange;
    protected int   shortRange;
    protected int   mediumRange;
    protected int   longRange;
    protected int   extremeRange;
    protected int   waterShortRange;
    protected int   waterMediumRange;
    protected int   waterLongRange;
    protected int   waterExtremeRange;

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
        
        // Start of Infantry weapons (Level1)
        EquipmentType.addType(createInfRifle());
        EquipmentType.addType(createInfMG());
        EquipmentType.addType(createInfSRM());
        EquipmentType.addType(createInfLRM());
        EquipmentType.addType(createInfLaser());
        EquipmentType.addType(createInfFlamer());
        EquipmentType.addType(createInfInfernoSRM());


        //Protomech weapons
        EquipmentType.addType(createCLPROLRM1() );
        EquipmentType.addType(createCLPROLRM2() );
        EquipmentType.addType(createCLPROLRM3() );
        EquipmentType.addType(createCLPROLRM4() );
        //EquipmentType.addType(createCLPROLRM5() );
        EquipmentType.addType(createCLPROLRM6() );
        EquipmentType.addType(createCLPROLRM7() );
        EquipmentType.addType(createCLPROLRM8() );
        EquipmentType.addType(createCLPROLRM9() );
        //EquipmentType.addType(createCLPROLRM10() );
        EquipmentType.addType(createCLPROLRM11() );
        EquipmentType.addType(createCLPROLRM12() );
        EquipmentType.addType(createCLPROLRM13() );
        EquipmentType.addType(createCLPROLRM14() );
        //EquipmentType.addType(createCLPROLRM15() );
        EquipmentType.addType(createCLPROLRM16() );
        EquipmentType.addType(createCLPROLRM17() );
        EquipmentType.addType(createCLPROLRM18() );
        EquipmentType.addType(createCLPROLRM19() );
        //EquipmentType.addType(createCLPROLRM20() );
        EquipmentType.addType(createCLPROLRT1() );
        EquipmentType.addType(createCLPROLRT2() );
        EquipmentType.addType(createCLPROLRT3() );
        EquipmentType.addType(createCLPROLRT4() );
        //EquipmentType.addType(createCLPROLRT5() );
        EquipmentType.addType(createCLPROLRT6() );
        EquipmentType.addType(createCLPROLRT7() );
        EquipmentType.addType(createCLPROLRT8() );
        EquipmentType.addType(createCLPROLRT9() );
        //EquipmentType.addType(createCLPROLRT10() );
        EquipmentType.addType(createCLPROLRT11() );
        EquipmentType.addType(createCLPROLRT12() );
        EquipmentType.addType(createCLPROLRT13() );
        EquipmentType.addType(createCLPROLRT14() );
        //EquipmentType.addType(createCLPROLRT15() );
        EquipmentType.addType(createCLPROLRT16() );
        EquipmentType.addType(createCLPROLRT17() );
        EquipmentType.addType(createCLPROLRT18() );
        EquipmentType.addType(createCLPROLRT19() );
        //EquipmentType.addType(createCLPROLRT20() );
        EquipmentType.addType(createCLPROSRM1() );
        EquipmentType.addType(createCLPROSRT1() );
        EquipmentType.addType(createCLPROStreakSRM1() );
        //EquipmentType.addType(createCLPROSRM2() );
        //EquipmentType.addType(createCLPROStreakSRM2() );
        //EquipmentType.addType(createCLPROSRT2() );
        EquipmentType.addType(createCLPROSRM3() );
        EquipmentType.addType(createCLPROStreakSRM3() );
        EquipmentType.addType(createCLPROSRT3() );
        //EquipmentType.addType(createCLPROSRM4() );
        //EquipmentType.addType(createCLPROStreakSRM4() );
        //EquipmentType.addType(createCLPROSRT4() );
        EquipmentType.addType(createCLPROSRM5() );
        EquipmentType.addType(createCLPROStreakSRM5() );
        EquipmentType.addType(createCLPROSRT5() );
        //EquipmentType.addType(createCLPROSRM6() );
        //EquipmentType.addType(createCLPROStreakSRM6() );
        //EquipmentType.addType(createCLPROSRT6() );

        // Start BattleArmor weapons
        EquipmentType.addType(createBAMG());
        EquipmentType.addType(createBASingleMG());
        EquipmentType.addType(createBASingleFlamer());
        EquipmentType.addType(createBAFlamer());
        EquipmentType.addType(createBACLERSmallLaser());
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
        EquipmentType.addType(createFenrirSRM4());
        EquipmentType.addType(createBAAutoGL());
        EquipmentType.addType(createBAMagshotGR());
        EquipmentType.addType(createBAISMediumLaser());
        EquipmentType.addType(createBAISERSmallLaser());
        EquipmentType.addType(createBACompactNARC());
        EquipmentType.addType(createSlothSmallLaser());

        EquipmentType.addType(createBABearhunterAC());
        EquipmentType.addType(createBACLMediumPulseLaser());
        EquipmentType.addType(createBAIncendiaryNeedler());
        EquipmentType.addType(createBALightRecRifle());
        EquipmentType.addType(createBAKingDavidLightGaussRifle());
        EquipmentType.addType(createBAMediumRecRifle());
        EquipmentType.addType(createBAPlasmaRifle());
        EquipmentType.addType(createBASRM4());
        EquipmentType.addType(createBAVibroClaws1());
        EquipmentType.addType(createBAVibroClaws2());
        EquipmentType.addType(createPhalanxSRM4());
        EquipmentType.addType(createBAHeavyMG());
        EquipmentType.addType(createBALightMG());
        EquipmentType.addType(createBACLHeavyMediumLaser());
        EquipmentType.addType(createBAHeavyRecRifle());
        EquipmentType.addType(createBACLHeavySmallLaser());
        EquipmentType.addType(createBACLERMediumLaser());
        EquipmentType.addType(createBACLSmallPulseLaser());
        EquipmentType.addType(createBAISLightMortar());
        EquipmentType.addType(createBAISHeavyMortar());
        EquipmentType.addType(createBAMicroGrenade());
        EquipmentType.addType(createBAGrandMaulerGauss());
        EquipmentType.addType(createBATsunamiGaussRifle());
        EquipmentType.addType(createCLAdvancedSRM1());
        EquipmentType.addType(createCLAdvancedSRM2());
        EquipmentType.addType(createCLAdvancedSRM3());
        EquipmentType.addType(createCLAdvancedSRM4());
        EquipmentType.addType(createBAAdvancedSRM5());
        EquipmentType.addType(createCLAdvancedSRM6());
        EquipmentType.addType(createISLAWLauncher());
        EquipmentType.addType(createISLAW2Launcher());
        EquipmentType.addType(createISLAW3Launcher());
        EquipmentType.addType(createISLAW4Launcher());
        EquipmentType.addType(createISLAW5Launcher());
        EquipmentType.addType(createISMRM1());
        EquipmentType.addType(createISMRM2());
        EquipmentType.addType(createISMRM3());
        EquipmentType.addType(createISMRM4());
        EquipmentType.addType(createISMRM5());
        EquipmentType.addType(createLRM1());
        EquipmentType.addType(createLRM2());
        EquipmentType.addType(createLRM3());
        EquipmentType.addType(createLRM4());

    }

    public static WeaponType createInfRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "Infantry Rifle";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantryRifle");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 2; // No long range.
        weapon.extremeRange = 2; // No Extreme Range
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES | F_BALLISTIC;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfMG() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "Infantry MG";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantryMG");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES | F_BALLISTIC;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfSRM() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "Infantry SRM";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantrySRM");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES| F_MISSILE;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfInfernoSRM() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Infantry Inferno SRM";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantryInfernoSRM");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_INFERNO | F_MISSILE;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfLRM() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_3;
        weapon.name = "Infantry LRM";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantryLRM");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 3;
        weapon.shortRange = 6;
        weapon.mediumRange = 9;
        weapon.longRange = 12;
        weapon.extremeRange = 18;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES| F_MISSILE;
        weapon.bv = 4; // ???
        weapon.setModes(new String[] {"", "Indirect"}); // ?

        return weapon;
    }

    public static WeaponType createInfLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "Infantry Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantryLaser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES | F_ENERGY;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "Infantry Flamer";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("InfantryFlamer");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 2; // No long range.
        weapon.extremeRange = 2; // No Extreme Range.
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_FLAMER | F_DIRECT_FIRE | F_INFANTRY | F_ENERGY;
        // In www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf,
        // pg. 23, Randall Bills says "No" to flamer-equipped infantry
        // doing heat instead of damage.

        return weapon;
    }


    // Start BattleArmor weapons
    public static WeaponType createBAMG() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBASingleMG() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BALLISTIC;

        return weapon;
    }
    public static WeaponType createBASingleFlamer() {
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
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAFlamer() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_FLAMER | F_ENERGY;

        return weapon;
    }

    public static WeaponType createBACLERSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "ER Small Laser";
        weapon.setInternalName("BACLERSmallLaser");
        weapon.addLookupName("BA-Clan ER Small Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES | F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createCLAdvancedSRM2() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Advanced SRM 2";
        weapon.setInternalName("CLAdvancedSRM2");
        weapon.addLookupName("Clan Advanced SRM-2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_ADVANCED;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_NO_FIRES | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBATwinFlamers() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_FLAMER | F_DOUBLE_HITS | F_ENERGY;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }
    public static WeaponType createBAInfernoSRM() {
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
    public static WeaponType createBACLMicroPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Micro Pulse Laser";
        weapon.setInternalName("BACLMicroPulseLaser");
        weapon.addLookupName("BA-Clan Micro Pulse Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAMicroBomb() {
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
    public static WeaponType createBACLERMicroLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "ER Micro Laser";
        weapon.setInternalName("BACLERMicroLaser");
        weapon.addLookupName("BA-Clan ER Micro Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 4;
        weapon.extremeRange = 4; // No Extreme Range
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES | F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createCLTorpedoLRM5() {
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
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAISMediumPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Medium Pulse Laser";
        weapon.setInternalName("BAISMediumPulseLaser");
        weapon.addLookupName("BA-IS Medium Pulse Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 6;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createTwinSmallPulseLaser() {
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
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createTripleSmallLaser() {
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
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createTripleMG() {
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
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createFenrirSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Fenrir SRM 4";
        weapon.setInternalName("FenrirSRM4");
        weapon.addLookupName("Fenrir SRM-4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DOUBLE_HITS | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAAutoGL() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }
    public static WeaponType createBAMagshotGR() {
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
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }
    public static WeaponType createBAISMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Medium Laser";
        weapon.setInternalName("BAISMediumLaser");
        weapon.addLookupName("BA-IS Medium Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAISERSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "ER Small Laser";
        weapon.setInternalName("BAISERSmallLaser");
        weapon.addLookupName("BA-IS ER Small Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 5;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES | F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBACompactNARC() {
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
        weapon.bv = 0;
        weapon.flags |= F_NO_FIRES | F_BATTLEARMOR | F_MISSILE;

        return weapon;
    }
    public static WeaponType createSlothSmallLaser() {
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
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES | F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }


    public static WeaponType createCLPROLRT1() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 1";
        weapon.setInternalName("CLLRTorpedo1");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=1;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 0.2f;
        weapon.criticals=0;
        weapon.bv = 17;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT2() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 2";
        weapon.setInternalName("CLLRTorpedo2");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=2;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 0.4f;
        weapon.criticals=0;
        weapon.bv = 25;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT3() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 3";
        weapon.setInternalName("CLLRTorpedo3");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=3;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 0.6f;
        weapon.criticals=0;
        weapon.bv = 35;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT4() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 4";
        weapon.setInternalName("CLLRTorpedo4");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=4;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 0.8f;
        weapon.criticals=0;
        weapon.bv = 46;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT5() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 5";
        weapon.setInternalName("CLCLLRTorpedo5");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=5;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 1f;
        weapon.criticals=0;
        weapon.bv = 55;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT6() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 6";
        weapon.setInternalName("CLLRTorpedo6");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=6;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 1.2f;
        weapon.criticals=0;
        weapon.bv = 69;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT7() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 7";
        weapon.setInternalName("CLLRTorpedo7");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=7;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 1.4f;
        weapon.criticals=0;
        weapon.bv = 92;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT8() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 8";
        weapon.setInternalName("CLLRTorpedo8");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=8;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 1.6f;
        weapon.criticals=0;
        weapon.bv = 93;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT9() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 9";
        weapon.setInternalName("CLLRTorpedo9");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=9;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 1.8f;
        weapon.criticals=0;
        weapon.bv = 95;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT10() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 10";
        weapon.setInternalName("CLLRTorpedo10");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=10;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 2f;
        weapon.criticals=0;
        weapon.bv = 109;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT11() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 11";
        weapon.setInternalName("CLLRTorpedo11");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=11;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 2.2f;
        weapon.criticals=0;
        weapon.bv = 139;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT12() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 12";
        weapon.setInternalName("CLLRTorpedo12");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=12;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 2.4f;
        weapon.criticals=0;
        weapon.bv = 141;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT13() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 13";
        weapon.setInternalName("CLLRTorpedo13");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=13;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 2.6f;
        weapon.criticals=0;
        weapon.bv = 161;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT14() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 14";
        weapon.setInternalName("CLLRTorpedo14");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=14;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 2.8f;
        weapon.criticals=0;
        weapon.bv = 163;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT15() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 15";
        weapon.setInternalName("CLLRTorpedo15");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=15;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 3f;
        weapon.criticals=0;
        weapon.bv = 164;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT16() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 16";
        weapon.setInternalName("CLLRTorpedo16");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=16;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 3.2f;
        weapon.criticals=0;
        weapon.bv = 214;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT17() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 17";
        weapon.setInternalName("CLLRTorpedo17");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=17;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 3.4f;
        weapon.criticals=0;
        weapon.bv = 215;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT18() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 18";
        weapon.setInternalName("CLLRTorpedo18");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=18;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 3.6f;
        weapon.criticals=0;
        weapon.bv = 217;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT19() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 19";
        weapon.setInternalName("CLLRTorpedo19");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=19;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 3.8f;
        weapon.criticals=0;
        weapon.bv = 218;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRT20() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRT 20";
        weapon.setInternalName("CLLRTorpedo20");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=20;
        weapon.ammoType = AmmoType.T_LRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.waterShortRange = 7;
        weapon.waterMediumRange = 14;
        weapon.waterLongRange = 21;
        weapon.waterExtremeRange = 28;
        weapon.tonnage = 4f;
        weapon.criticals=0;
        weapon.bv = 220;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }
    public static WeaponType createCLPROSRM1() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRM 1";
        weapon.setInternalName("CLSRM1");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.25f;
        weapon.criticals = 0;
        weapon.bv = 15;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRM2() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRM 2";
        weapon.setInternalName("CLSRM2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.5f;
        weapon.criticals = 0;
        weapon.bv = 21;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRM3() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRM 3";
        weapon.setInternalName("CLSRM3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.75f;
        weapon.criticals = 0;
        weapon.bv = 30;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRM4() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRM 4";
        weapon.setInternalName("CLSRM4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1f;
        weapon.criticals = 0;
        weapon.bv = 39;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRM5() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRM 5";
        weapon.setInternalName("CLSRM5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.25f;
        weapon.criticals = 0;
        weapon.bv = 47;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRM6() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRM 6";
        weapon.setInternalName("CLSRM6");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.5f;
        weapon.criticals = 0;
        weapon.bv = 59;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM1() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Streak SRM 1";
        weapon.setInternalName("CLStreakSRM1");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.5f;
        weapon.criticals = 0;
        weapon.bv = 20;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM2() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Streak SRM 2";
        weapon.setInternalName("CLStreakSRM2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 1f;
        weapon.criticals = 0;
        weapon.bv = 40;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM3() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Streak SRM 3";
        weapon.setInternalName("CLStreakSRM3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 1.5f;
        weapon.criticals = 0;
        weapon.bv = 59;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM4() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Streak SRM 4";
        weapon.setInternalName("CLStreakSRM4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 2f;
        weapon.criticals = 0;
        weapon.bv = 79;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM5() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Streak SRM 5";
        weapon.setInternalName("CLStreakSRM5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 2.5f;
        weapon.criticals = 0;
        weapon.bv = 99;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM6() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Streak SRM 6";
        weapon.setInternalName("CLStreakSRM6");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 3f;
        weapon.criticals = 0;
        weapon.bv = 119;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }
    
    public static WeaponType createCLPROLRM1() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 1";
        weapon.setInternalName("CLLRM1");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=1;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.2f;
        weapon.criticals=0;
        weapon.bv = 17;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM2() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 2";
        weapon.setInternalName("CLLRM2");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=2;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.4f;
        weapon.criticals=0;
        weapon.bv = 25;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM3() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 3";
        weapon.setInternalName("CLLRM3");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=3;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.6f;
        weapon.criticals=0;
        weapon.bv = 35;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM4() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 4";
        weapon.setInternalName("CLLRM4");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=4;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.8f;
        weapon.criticals=0;
        weapon.bv = 46;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM5() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 5";
        weapon.setInternalName("CLLRM5");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=5;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 1f;
        weapon.criticals=0;
        weapon.bv = 55;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM6() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 6";
        weapon.setInternalName("CLLRM6");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=6;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 1.2f;
        weapon.criticals=0;
        weapon.bv = 69;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM7() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 7";
        weapon.setInternalName("CLLRM7");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=7;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 1.4f;
        weapon.criticals=0;
        weapon.bv = 92;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM8() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 8";
        weapon.setInternalName("CLLRM8");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=8;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 1.6f;
        weapon.criticals=0;
        weapon.bv = 93;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM9() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 9";
        weapon.setInternalName("CLLRM9");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=9;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 1.8f;
        weapon.criticals=0;
        weapon.bv = 95;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM10() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 10";
        weapon.setInternalName("CLLRM10");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=10;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2f;
        weapon.criticals=0;
        weapon.bv = 109;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM11() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 11";
        weapon.setInternalName("CLLRM11");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=11;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.2f;
        weapon.criticals=0;
        weapon.bv = 139;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM12() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 12";
        weapon.setInternalName("CLLRM12");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=12;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.4f;
        weapon.criticals=0;
        weapon.bv = 141;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM13() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 13";
        weapon.setInternalName("CLLRM13");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=13;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.6f;
        weapon.criticals=0;
        weapon.bv = 161;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM14() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 14";
        weapon.setInternalName("CLLRM14");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=14;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.8f;
        weapon.criticals=0;
        weapon.bv = 163;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM15() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 15";
        weapon.setInternalName("CLLRM15");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=15;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 3f;
        weapon.criticals=0;
        weapon.bv = 164;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM16() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 16";
        weapon.setInternalName("CLLRM16");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=16;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 3.2f;
        weapon.criticals=0;
        weapon.bv = 214;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM17() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 17";
        weapon.setInternalName("CLLRM17");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=17;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 3.4f;
        weapon.criticals=0;
        weapon.bv = 215;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM18() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 18";
        weapon.setInternalName("CLLRM18");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=18;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 3.6f;
        weapon.criticals=0;
        weapon.bv = 217;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM19() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 19";
        weapon.setInternalName("CLLRM19");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=19;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 3.8f;
        weapon.criticals=0;
        weapon.bv = 218;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM20() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "LRM 20";
        weapon.setInternalName("CLLRM20");
        weapon.heat = 0;
        weapon.damage=DAMAGE_MISSILE;
        weapon.rackSize=20;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange=14;
        weapon.longRange =21;
        weapon.extremeRange = 28;
        weapon.tonnage = 4f;
        weapon.criticals=0;
        weapon.bv = 220;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }
    public static WeaponType createCLPROSRT1() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRT 1";
        weapon.setInternalName("CLSRTorpedo1");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_SRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.25f;
        weapon.criticals = 0;
        weapon.bv = 15;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRT2() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRT 2";
        weapon.setInternalName("CLSRTorpedo2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.5f;
        weapon.criticals = 0;
        weapon.bv = 21;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRT3() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRT 3";
        weapon.setInternalName("CLSRTorpedo3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_SRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.75f;
        weapon.criticals = 0;
        weapon.bv = 30;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRT4() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRT 4";
        weapon.setInternalName("CLSRTorpedo4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1f;
        weapon.criticals = 0;
        weapon.bv = 39;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRT5() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRT 5";
        weapon.setInternalName("CLSRTorpedo5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_SRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.25f;
        weapon.criticals = 0;
        weapon.bv = 47;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createCLPROSRT6() {
        WeaponType weapon = new WeaponType();
        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "SRT 6";
        weapon.setInternalName("CLSRTorpedo6");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_TORPEDO;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.5f;
        weapon.criticals = 0;
        weapon.bv = 59;
        weapon.flags |= F_PROTOMECH | F_MISSILE;
        return weapon;
    }

    public static WeaponType createBAAdvancedSRM5() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Advanced SRM 5";
        weapon.setInternalName("AdvancedSRM5");
        weapon.addLookupName("BA-Advanced SRM-5");
        weapon.addLookupName("Clan Advanced SRM-5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_SRM_ADVANCED;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        weapon.flags |= F_NO_FIRES | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBABearhunterAC() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Bearhunter AC";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Bearhunter Superheavy AC");
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBACLMediumPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Medium Pulse Laser";
        weapon.setInternalName("BACLMediumPulseLaser");
        weapon.addLookupName("BA-CL Medium Pulse Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 7;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.waterShortRange = 3;
        weapon.waterMediumRange = 5;
        weapon.waterLongRange = 8;
        weapon.waterExtremeRange = 10;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBAIncendiaryNeedler() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Firedrake Needler";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Firedrake Incendiary Needler");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 0;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_INCENDIARY_NEEDLES | F_BATTLEARMOR | F_DIRECT_FIRE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBALightRecRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Light Recoilless";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Light Recoilless Rifle");
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBAKingDavidLightGaussRifle() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBAMediumRecRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Medium Recoilless";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Medium Recoilless Rifle");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBAPlasmaRifle() {
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
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBASRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "SRM 4";
        weapon.setInternalName("BA-SRM4");
        weapon.addLookupName("BA-SRM4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_MISSILE;

        return weapon;
    }

    public static WeaponType createBAVibroClaws1() {
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

    public static WeaponType createBAVibroClaws2() {
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

    public static WeaponType createPhalanxSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Phalanx SRM 4";
        weapon.setInternalName("PhalanxSRM4");
        weapon.addLookupName("Phalanx SRM4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DOUBLE_HITS | F_MISSILE;

        return weapon;
    }
    
    // War Of 3039 Prototype weapons
    
    public static WeaponType createBAHeavyRecRifle() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "Heavy Recoilless";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-Heavy Recoilless Rifle");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 5;
        weapon.longRange = 7;
        weapon.extremeRange = 10;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBAHeavyMG() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBALightMG() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBACLHeavyMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Ultra Heavy Support Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-CLHeavyMediumLaser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 10;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.waterShortRange = 2;
        weapon.waterMediumRange = 4;
        weapon.waterLongRange = 6;
        weapon.waterExtremeRange = 8;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBACLHeavySmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Semi-Portable Heavy Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-CLHeavySmallLaser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 6;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 2;
        weapon.waterExtremeRange = 2;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBACLERMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "ER Heavy Support Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-CLERMediumLaser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 7;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_LASER | F_ENERGY;

        return weapon;
    }

    public static WeaponType createBACLSmallPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Support Pulse Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("BA-CLSmallPulseLaser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 4;
        weapon.waterExtremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE| F_ENERGY;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createBAGrandMaulerGauss() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBATsunamiGaussRifle() {
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
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_NO_FIRES | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBAMicroGrenade() {
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

    public static WeaponType createBAISHeavyMortar() {
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
        weapon.bv = 0;
        weapon.flags |= F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createBAISLightMortar() {
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
        weapon.bv = 0;
        weapon.flags |= F_BATTLEARMOR | F_BALLISTIC;

        return weapon;
    }

    public static WeaponType createCLAdvancedSRM1() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Advanced SRM 1";
        weapon.setInternalName("CLAdvancedSRM1");
        weapon.addLookupName("Clan Advanced SRM-1");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_SRM_ADVANCED;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_NO_FIRES | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createCLAdvancedSRM3() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Advanced SRM 3";
        weapon.setInternalName("CLAdvancedSRM3");
        weapon.addLookupName("Clan Advanced SRM-3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_SRM_ADVANCED;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createCLAdvancedSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Advanced SRM 4";
        weapon.setInternalName("CLAdvancedSRM4");
        weapon.addLookupName("Clan Advanced SRM-4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_ADVANCED;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createCLAdvancedSRM6() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_CLAN_LEVEL_2;
        weapon.name = "Advanced SRM 6";
        weapon.setInternalName("CLAdvancedSRM6");
        weapon.addLookupName("Clan Advanced SRM-6");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_ADVANCED;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISLAWLauncher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "LAW Launcher";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISLAW");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 12;
        weapon.extremeRange = 14;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_ONESHOT | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISLAW2Launcher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "LAW 2 Launcher";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISLAW2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 12;
        weapon.extremeRange = 14;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_ONESHOT | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISLAW3Launcher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "LAW 3 Launcher";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISLAW3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 12;
        weapon.extremeRange = 14;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_ONESHOT | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISLAW4Launcher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "LAW 4 Launcher";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISLAW4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 12;
        weapon.extremeRange = 14;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_ONESHOT | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISLAW5Launcher() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "LAW 5 Launcher";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISLAW5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.toHitModifier = +1;
        weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 12;
        weapon.extremeRange = 14;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        // someone please explain why these misbehave if the have the flag F_BATTLEARMOR
        // because they are battlearmor weapons.
        // Server#resolveWeaponsAttack is a mess :-)
        weapon.flags |= F_ONESHOT | F_MISSILE;
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISMRM1() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "MRM 1";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISMRM1");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
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
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISMRM2() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "MRM 2";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISMRM2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
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
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISMRM3() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "MRM 3";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISMRM3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
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
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISMRM4() {
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
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createISMRM5() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_2;
        weapon.name = "MRM 5";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("ISMRM5");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
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
        weapon.bv = 0;

        return weapon;
    }

    public static WeaponType createLRM1() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "LRM 1";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-1");
        weapon.addLookupName("ISLRM1");
        weapon.addLookupName("IS LRM 1");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;

        return weapon;
    }

    public static WeaponType createLRM2() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "LRM 2";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-2");
        weapon.addLookupName("ISLRM2");
        weapon.addLookupName("IS LRM 2");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;

        return weapon;
    }

    public static WeaponType createLRM3() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "LRM 3";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-3");
        weapon.addLookupName("ISLRM3");
        weapon.addLookupName("IS LRM 3");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;

        return weapon;
    }

    public static WeaponType createLRM4() {
        WeaponType weapon = new WeaponType();

        weapon.techLevel = TechConstants.T_IS_LEVEL_1;
        weapon.name = "LRM 4";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-4");
        weapon.addLookupName("ISLRM4");
        weapon.addLookupName("IS LRM 4");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.setModes(new String[] {"", "Indirect"});
        weapon.flags |= F_MISSILE;

        return weapon;
    }

    public String toString() {
        return "WeaponType: " + name;
    }
}
