/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
    public static final int     F_DIRECT_FIRE   = 0x0001; // marks any weapon affected by a targetting computer
    public static final int     F_FLAMER        = 0x0002;
    public static final int     F_LASER         = 0x0004; // for eventual glazed armor purposes
    public static final int     F_PPC           = 0x0008; //              "
    public static final int     F_AUTO_TARGET   = 0x0010; // for weapons that target automatically (AMS)
    public static final int     F_NO_FIRES      = 0x0020; // cannot start fires
    public static final int     F_ONESHOT       = 0x8000; //weapon is oneShot.
    public static final int     F_ARTILLERY     = 0x10000;

    // Need to distinguish infantry weapons from their bigger,
    // vehicle- and mech-mounted cousins.
    public static final int     F_INFERNO       = 0x0400; // Inferno weapon
    public static final int     F_INFANTRY      = 0x0800; // small calibre weapon, no ammo, damage based on # men shooting
    public static final int     F_PROTOMECH     = 0x0040; //Protomech weapons, which need weird ammo stuff.

    // Flags for implementing the vast number of BattleArmor special rules.
    public static final int     F_SOLO_ATTACK   = 0x0080; // must be only weapon attacking
    public static final int     F_BATTLEARMOR   = 0x1000; // multiple shots resolved in one to-hit (kinda like RAC, only not)
    public static final int     F_DOUBLE_HITS   = 0x2000; // two shots hit per one rolled
    public static final int     F_MISSILE_HITS  = 0x4000; // use missile rules or # of hits

    protected RangeType range;
    protected int   heat;
    protected int   damage;
    private int     rackSize; // or AC size, or whatever
    private int     ammoType;

    private int     minimumRange;
    private int     shortRange;
    private int     mediumRange;
    private int     longRange;
    private int     extremeRange;
    private int     waterShortRange;
    private int     waterMediumRange;
    private int     waterLongRange;
    private int     waterExtremeRange;


    protected WeaponType() {
        ;
    }

    public int getHeat() {
        return heat;
    }

    public int getFireTN() {
        if (hasFlag(F_NO_FIRES)) {
            return TargetRoll.IMPOSSIBLE;
        } else if (hasFlag(F_FLAMER)) {
            return 4;
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
        // all tech level 1 weapons
        EquipmentType.addType(createFlamer());
        EquipmentType.addType(createVehicleFlamer());
        EquipmentType.addType(createSmallLaser());
        EquipmentType.addType(createMediumLaser());
        EquipmentType.addType(createLargeLaser());
        EquipmentType.addType(createPPC());
        EquipmentType.addType(createAC2());
        EquipmentType.addType(createAC5());
        EquipmentType.addType(createAC10());
        EquipmentType.addType(createAC20());
        EquipmentType.addType(createMG());
        EquipmentType.addType(createLRM5());
        EquipmentType.addType(createLRM10());
        EquipmentType.addType(createLRM15());
        EquipmentType.addType(createLRM20());
        EquipmentType.addType(createSRM2());
        EquipmentType.addType(createSRM4());
        EquipmentType.addType(createSRM6());

        // Start of Infantry weapons (Level1)
        EquipmentType.addType(createInfRifle());
        EquipmentType.addType(createInfMG());
        EquipmentType.addType(createInfSRM());
        EquipmentType.addType(createInfLRM());
        EquipmentType.addType(createInfLaser());
        EquipmentType.addType(createInfFlamer());
        EquipmentType.addType(createInfInfernoSRM());

        // Start of Inner Sphere Level2 weapons
        EquipmentType.addType(createISERPPC());
        EquipmentType.addType(createISERLargeLaser());
        EquipmentType.addType(createISERMediumLaser());
        EquipmentType.addType(createISERSmallLaser());
        EquipmentType.addType(createISLargePulseLaser());
        EquipmentType.addType(createISMediumPulseLaser());
        EquipmentType.addType(createISSmallPulseLaser());
        EquipmentType.addType(createISLBXAC2());
        EquipmentType.addType(createISLBXAC5());
        EquipmentType.addType(createISLBXAC10());
        EquipmentType.addType(createISLBXAC20());
        EquipmentType.addType(createISGaussRifle());
        EquipmentType.addType(createISLightGaussRifle());
        EquipmentType.addType(createISHeavyGaussRifle());
        EquipmentType.addType(createISUltraAC2());
        EquipmentType.addType(createISUltraAC5());
        EquipmentType.addType(createISUltraAC10());
        EquipmentType.addType(createISUltraAC20());
        EquipmentType.addType(createISRAC2());
        EquipmentType.addType(createISRAC5());
        EquipmentType.addType(createISStreakSRM2());
        EquipmentType.addType(createISStreakSRM4());
        EquipmentType.addType(createISStreakSRM6());
        EquipmentType.addType(createISMRM10());
        EquipmentType.addType(createISMRM20());
        EquipmentType.addType(createISMRM30());
        EquipmentType.addType(createISMRM40());
        EquipmentType.addType(createISAMS());
        EquipmentType.addType(createISNarc());
        EquipmentType.addType(createISRL10());
        EquipmentType.addType(createISRL15());
        EquipmentType.addType(createISRL20());
        EquipmentType.addType(createISArrowIVSystem());
        EquipmentType.addType(createISLongTom());
        EquipmentType.addType(createISSniper());
        EquipmentType.addType(createISThumper());

        // Start of Clan Level2 weapons
        EquipmentType.addType(createCLERPPC());
        EquipmentType.addType(createCLERLargeLaser());
        EquipmentType.addType(createCLERMediumLaser());
        EquipmentType.addType(createCLERSmallLaser());
        EquipmentType.addType(createCLERMicroLaser());
        EquipmentType.addType(createCLFlamer());
        EquipmentType.addType(createCLVehicleFlamer());
        EquipmentType.addType(createCLHeavyLargeLaser());
        EquipmentType.addType(createCLHeavyMediumLaser());
        EquipmentType.addType(createCLHeavySmallLaser());
        EquipmentType.addType(createCLLargePulseLaser());
        EquipmentType.addType(createCLMediumPulseLaser());
        EquipmentType.addType(createCLSmallPulseLaser());
        EquipmentType.addType(createCLMicroPulseLaser());
        EquipmentType.addType(createCLLBXAC2());
        EquipmentType.addType(createCLLBXAC5());
        EquipmentType.addType(createCLLBXAC10());
        EquipmentType.addType(createCLLBXAC20());
        EquipmentType.addType(createCLMG());
        EquipmentType.addType(createCLLightMG());
        EquipmentType.addType(createCLHeavyMG());
        EquipmentType.addType(createCLLRM5());
        EquipmentType.addType(createCLLRM10());
        EquipmentType.addType(createCLLRM15());
        EquipmentType.addType(createCLLRM20());
        EquipmentType.addType(createCLSRM2());
        EquipmentType.addType(createCLSRM4());
        EquipmentType.addType(createCLSRM6());
        EquipmentType.addType(createCLGaussRifle());
        EquipmentType.addType(createCLUltraAC2());
        EquipmentType.addType(createCLUltraAC5());
        EquipmentType.addType(createCLUltraAC10());
        EquipmentType.addType(createCLUltraAC20());
        EquipmentType.addType(createCLStreakSRM2());
        EquipmentType.addType(createCLStreakSRM4());
        EquipmentType.addType(createCLStreakSRM6());
        EquipmentType.addType(createCLATM3());
        EquipmentType.addType(createCLATM6());
        EquipmentType.addType(createCLATM9());
        EquipmentType.addType(createCLATM12());
        EquipmentType.addType(createCLAMS());
        EquipmentType.addType(createCLNarc());
        EquipmentType.addType(createCLArrowIVSystem());
        EquipmentType.addType(createCLLongTom());
        EquipmentType.addType(createCLSniper());
        EquipmentType.addType(createCLThumper());

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
        EquipmentType.addType(createCLPROSRM1() );
        EquipmentType.addType(createCLPROStreakSRM1() );
        //EquipmentType.addType(createCLPROSRM2() );
        //EquipmentType.addType(createCLPROStreakSRM2() );
        EquipmentType.addType(createCLPROSRM3() );
        EquipmentType.addType(createCLPROStreakSRM3() );
        //EquipmentType.addType(createCLPROSRM4() );
        //EquipmentType.addType(createCLPROStreakSRM4() );
        EquipmentType.addType(createCLPROSRM5() );
        EquipmentType.addType(createCLPROStreakSRM5() );
        //EquipmentType.addType(createCLPROSRM6() );
        //EquipmentType.addType(createCLPROStreakSRM6() );

        // Anti-Mek attacks are weapon-like in nature.
        EquipmentType.addType( createLegAttack() );
        EquipmentType.addType( createSwarmMek() );
        EquipmentType.addType( createStopSwarm() );

        // Start BattleArmor weapons
        EquipmentType.addType( createBAMG() );
        EquipmentType.addType( createBASingleMG() );
        EquipmentType.addType( createBASingleFlamer());
        EquipmentType.addType( createBAFlamer() );
        EquipmentType.addType( createBASmallLaser() );
        EquipmentType.addType( createBACLERSmallLaser() );
        EquipmentType.addType( createCLAdvancedSRM2() );
        EquipmentType.addType( createBATwinFlamers() );
        EquipmentType.addType( createBAInfernoSRM() );
        EquipmentType.addType( createBACLMicroPulseLaser() );
        EquipmentType.addType( createBAMicroBomb() );
        EquipmentType.addType( createBACLERMicroLaser() );
        EquipmentType.addType( createCLTorpedoLRM5() );
        EquipmentType.addType( createBAISMediumPulseLaser() );
        EquipmentType.addType( createTwinSmallPulseLaser() );
        EquipmentType.addType( createTripleSmallLaser() );
        EquipmentType.addType( createTripleMG() );
        EquipmentType.addType( createFenrirSRM4() );
        EquipmentType.addType( createBAAutoGL() );
        EquipmentType.addType( createBAMagshotGR() );
        EquipmentType.addType( createBAISMediumLaser() );
        EquipmentType.addType( createBAISERSmallLaser() );
        EquipmentType.addType( createBACompactNARC() );
        EquipmentType.addType( createSlothSmallLaser() );
        EquipmentType.addType( createBAMineLauncher() );
    }

    public static WeaponType createFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Flamer";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Flamer");
        weapon.addLookupName("ISFlamer");
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_FLAMER;
        weapon.bv = 6;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createVehicleFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Vehicle Flamer";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Vehicle Flamer");
        weapon.addLookupName("ISVehicleFlamer");
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_VEHICLE_FLAMER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange =4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_FLAMER;
        weapon.bv = 5;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createLargeLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Large Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Large Laser");
        weapon.addLookupName("ISLargeLaser");
        weapon.heat = 8;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.waterShortRange = 3;
        weapon.waterMediumRange = 6;
        weapon.waterLongRange = 9;
        weapon.waterExtremeRange = 12;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 124;

        return weapon;
    }

    public static WeaponType createMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Medium Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Medium Laser");
        weapon.addLookupName("ISMediumLaser");
        weapon.heat = 3;
        weapon.damage = 5;
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
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 46;

        return weapon;
    }

    public static WeaponType createSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Small Laser";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Small Laser");
        weapon.addLookupName("ISSmallLaser");
        weapon.heat = 1;
        weapon.damage = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 2;
        weapon.waterExtremeRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 9;

        return weapon;
    }

    public static WeaponType createPPC() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Particle Cannon";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS PPC");
        weapon.addLookupName("ISPPC");
        weapon.heat = 10;
        weapon.damage = 10;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = 3;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.waterShortRange = 4;
        weapon.waterMediumRange = 7;
        weapon.waterLongRange = 10;
        weapon.waterExtremeRange = 14;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_PPC | F_DIRECT_FIRE;
        weapon.bv = 176;

        return weapon;
    }

    public static WeaponType createMG() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Machine Gun";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Machine Gun");
        weapon.addLookupName("ISMachine Gun");
        weapon.heat = 0;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 5;

        return weapon;
    }

    public static WeaponType createAC2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Auto Cannon/2";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Auto Cannon/2");
        weapon.addLookupName("ISAC2");
        weapon.addLookupName("IS Autocannon/2");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 4;
        weapon.shortRange = 8;
        weapon.mediumRange = 16;
        weapon.longRange = 24;
        weapon.extremeRange = 32;
        weapon.tonnage = 6.0f;
        weapon.criticals = 1;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 37;

        return weapon;
    }

    public static WeaponType createAC5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Auto Cannon/5";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Auto Cannon/5");
        weapon.addLookupName("ISAC5");
        weapon.addLookupName("IS Autocannon/5");
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 3;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 8.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 70;

        return weapon;
    }

    public static WeaponType createAC10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Auto Cannon/10";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Auto Cannon/10");
        weapon.addLookupName("ISAC10");
        weapon.addLookupName("IS Autocannon/10");
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 12.0f;
        weapon.criticals = 7;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 124;

        return weapon;
    }

    public static WeaponType createAC20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Auto Cannon/20";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS Auto Cannon/20");
        weapon.addLookupName("ISAC20");
        weapon.addLookupName("IS Autocannon/20");
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 14.0f;
        weapon.criticals = 10;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 178;

        return weapon;
    }

    public static WeaponType createLRM5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 5";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-5");
        weapon.addLookupName("ISLRM5");
        weapon.addLookupName("IS LRM 5");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 45;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createLRM10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 10";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-10");
        weapon.addLookupName("ISLRM10");
        weapon.addLookupName("IS LRM 10");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.bv = 90;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createLRM15() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 15";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-15");
        weapon.addLookupName("ISLRM15");
        weapon.addLookupName("IS LRM 15");
        weapon.heat = 5;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.bv = 136;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createLRM20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 20";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS LRM-20");
        weapon.addLookupName("ISLRM20");
        weapon.addLookupName("IS LRM 20");
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.bv = 181;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createISRL10()  {
      WeaponType weapon = new WeaponType();

      weapon.name = "RL 10";
        weapon.setInternalName("RL10");
        weapon.addLookupName("ISRocketLauncher10");
        weapon.addLookupName("IS RLauncher-10");
      weapon.heat = 3;
      weapon.damage= DAMAGE_MISSILE;
      weapon.rackSize= 10;
      weapon.minimumRange = WEAPON_NA;
      weapon.shortRange= 5;
      weapon.mediumRange= 11;
      weapon.longRange = 18;
      weapon.extremeRange = 22;
      weapon.tonnage = .5f;
      weapon.criticals = 1;
      weapon.bv= 18;
      weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
      weapon.flags |= F_ONESHOT;
      weapon.toHitModifier=1;

      return weapon;
    }
    public static WeaponType createISRL15()  {
      WeaponType weapon = new WeaponType();

      weapon.name = "RL 15";
        weapon.setInternalName("RL15");
        weapon.addLookupName("ISRocketLauncher15");
        weapon.addLookupName("IS RLauncher-15");
      weapon.heat = 4;
      weapon.damage= DAMAGE_MISSILE;
      weapon.rackSize= 15;
      weapon.minimumRange = WEAPON_NA;
      weapon.shortRange= 4;
      weapon.mediumRange= 9;
      weapon.longRange = 15;
      weapon.extremeRange = 18;
      weapon.tonnage = 1.0f;
      weapon.criticals = 2;
      weapon.bv= 23;
      weapon.flags |= F_ONESHOT;
      weapon.toHitModifier=1;
      weapon.ammoType= AmmoType.T_ROCKET_LAUNCHER;

      return weapon;
    }
    public static WeaponType createISRL20()  {
     WeaponType weapon = new WeaponType();

     weapon.name = "RL 20";
        weapon.setInternalName("RL20");
        weapon.addLookupName("ISRocketLauncher20");
        weapon.addLookupName("IS RLauncher-20");
     weapon.heat = 5;
     weapon.damage= DAMAGE_MISSILE;
     weapon.rackSize= 20;
     weapon.minimumRange = WEAPON_NA;
     weapon.shortRange= 3;
     weapon.mediumRange= 7;
     weapon.longRange = 12;
     weapon.extremeRange = 14;
     weapon.tonnage = 1.5f;
     weapon.criticals = 3;
     weapon.bv= 24;
     weapon.ammoType = AmmoType.T_ROCKET_LAUNCHER;
     weapon.flags |= F_ONESHOT;
     weapon.toHitModifier=1;

     return weapon;
   }

    public static WeaponType createSRM2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "SRM 2";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS SRM-2");
        weapon.addLookupName("ISSRM2");
        weapon.addLookupName("IS SRM 2");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_NO_FIRES;
        weapon.bv = 21;

        return weapon;
    }

    public static WeaponType createSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.name = "SRM 4";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS SRM-4");
        weapon.addLookupName("ISSRM4");
        weapon.addLookupName("IS SRM 4");
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 39;

        return weapon;
    }

    public static WeaponType createSRM6() {
        WeaponType weapon = new WeaponType();

        weapon.name = "SRM 6";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("IS SRM-6");
        weapon.addLookupName("ISSRM6");
        weapon.addLookupName("IS SRM 6");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 59;

        return weapon;
    }


    //Start of Inner Sphere Level2 weapons

    public static WeaponType createISERPPC() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER PPC";
        weapon.setInternalName("ISERPPC");
        weapon.addLookupName("IS ER PPC");
        weapon.heat = 15;
        weapon.damage = 10;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 23;
        weapon.extremeRange = 28;
        weapon.waterShortRange = 4;
        weapon.waterMediumRange = 10;
        weapon.waterLongRange = 16;
        weapon.waterExtremeRange = 20;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_PPC | F_DIRECT_FIRE;
        weapon.bv = 229;

        return weapon;
    }

    public static WeaponType createISERLargeLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Large Laser";
        weapon.setInternalName("ISERLargeLaser");
        weapon.addLookupName("IS ER Large Laser");
        weapon.heat = 12;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 19;
        weapon.extremeRange = 28;
        weapon.waterShortRange = 3;
        weapon.waterMediumRange = 9;
        weapon.waterLongRange = 12;
        weapon.waterExtremeRange = 18;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 163;

        return weapon;
    }

    public static WeaponType createISERMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Medium Laser";
        weapon.setInternalName("ISERMediumLaser");
        weapon.addLookupName("IS ER Medium Laser");
        weapon.heat = 5;
        weapon.damage = 5;
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
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 62;

        return weapon;
    }

    public static WeaponType createISERSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Small Laser";
        weapon.setInternalName("ISERSmallLaser");
        weapon.addLookupName("IS ER Small Laser");
        weapon.heat = 2;
        weapon.damage = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 5;
        weapon.extremeRange = 8;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 3;
        weapon.waterExtremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 17;

        return weapon;
    }

    public static WeaponType createISLargePulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Large Pulse Laser";
        weapon.setInternalName("ISLargePulseLaser");
        weapon.addLookupName("IS Pulse Large Laser");
        weapon.addLookupName("IS Large Pulse Laser");
        weapon.heat = 10;
        weapon.damage = 9;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 10;
        weapon.extremeRange = 14;
        weapon.waterShortRange = 2;
        weapon.waterMediumRange = 5;
        weapon.waterLongRange = 7;
        weapon.waterExtremeRange = 10;
        weapon.tonnage = 7.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 119;

        return weapon;
    }

    public static WeaponType createISMediumPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Medium Pulse Laser";
        weapon.setInternalName("ISMediumPulseLaser");
        weapon.addLookupName("IS Pulse Med Laser");
        weapon.addLookupName("IS Medium Pulse Laser");
        weapon.heat = 4;
        weapon.damage = 6;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.waterShortRange = 2;
        weapon.waterMediumRange = 3;
        weapon.waterLongRange = 4;
        weapon.waterExtremeRange = 6;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 48;

        return weapon;
    }

    public static WeaponType createISSmallPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Small Pulse Laser";
        weapon.setInternalName("ISSmallPulseLaser");
        weapon.addLookupName("IS Pulse Small Laser");
        weapon.addLookupName("IS Small Pulse Laser");
        weapon.heat = 2;
        weapon.damage = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 2;
        weapon.waterExtremeRange = 4;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 12;

        return weapon;
    }

    public static WeaponType createISLBXAC2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 2-X AC";
        weapon.setInternalName("ISLBXAC2");
        weapon.addLookupName("IS LB 2-X AC");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 4;
        weapon.shortRange = 9;
        weapon.mediumRange = 18;
        weapon.longRange = 27;
        weapon.extremeRange = 36;
        weapon.tonnage = 6.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 42;

        return weapon;
    }

    public static WeaponType createISLBXAC5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 5-X AC";
        weapon.setInternalName("ISLBXAC5");
        weapon.addLookupName("IS LB 5-X AC");
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 3;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 8.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 83;

        return weapon;
    }

    public static WeaponType createISLBXAC10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 10-X AC";
        weapon.setInternalName("ISLBXAC10");
        weapon.addLookupName("IS LB 10-X AC");
        weapon.heat = 2;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 11.0f;
        weapon.criticals = 6;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 148;

        return weapon;
    }

    public static WeaponType createISLBXAC20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 20-X AC";
        weapon.setInternalName("ISLBXAC20");
        weapon.addLookupName("IS LB 20-X AC");
        weapon.heat = 6;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 14.0f;
        weapon.criticals = 11;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 237;

        return weapon;
    }

    public static WeaponType createISGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Gauss Rifle";
        weapon.setInternalName("ISGaussRifle");
        weapon.addLookupName("IS Gauss Rifle");
        weapon.heat = 1;
        weapon.damage = 15;
        weapon.ammoType = AmmoType.T_GAUSS;
        weapon.minimumRange = 2;
        weapon.shortRange = 7;
        weapon.mediumRange = 15;
        weapon.longRange = 22;
        weapon.extremeRange = 30;
        weapon.tonnage = 15.0f;
        weapon.criticals = 7;
        weapon.flags |= F_DIRECT_FIRE | F_NO_FIRES;
        weapon.explosive = true;
        weapon.bv = 321;

        return weapon;
    }

    public static WeaponType createISLightGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Light Gauss Rifle";
        weapon.setInternalName("ISLightGaussRifle");
        weapon.addLookupName("IS Light Gauss Rifle");
        weapon.heat = 1;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_GAUSS_LIGHT;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 17;
        weapon.longRange = 25;
        weapon.extremeRange = 34;
        weapon.tonnage = 12.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE | F_NO_FIRES;
        weapon.explosive = true;
        weapon.bv = 159;

        return weapon;
    }

    public static WeaponType createISHeavyGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Heavy Gauss Rifle";
        weapon.setInternalName("ISHeavyGaussRifle");
        weapon.addLookupName("IS Heavy Gauss Rifle");
        weapon.heat = 2;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_GAUSS_HEAVY;
        weapon.minimumRange = 4;
        weapon.shortRange = 6;
        weapon.mediumRange = 13;
        weapon.longRange = 20;
        weapon.extremeRange = 26;
        weapon.tonnage = 18.0f;
        weapon.criticals = 11;
        weapon.flags |= F_DIRECT_FIRE | F_NO_FIRES;
        weapon.explosive = true;
        weapon.bv = 346;

        return weapon;
    }

    public static WeaponType createISUltraAC2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/2";
        weapon.setInternalName("ISUltraAC2");
        weapon.addLookupName("IS Ultra AC/2");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 17;
        weapon.longRange = 25;
        weapon.extremeRange = 34;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 56;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createISUltraAC5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/5";
        weapon.setInternalName("ISUltraAC5");
        weapon.addLookupName("IS Ultra AC/5");
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 2;
        weapon.shortRange = 6;
        weapon.mediumRange = 13;
        weapon.longRange = 20;
        weapon.extremeRange = 26;
        weapon.tonnage = 9.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 113;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createISUltraAC10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/10";
        weapon.setInternalName("ISUltraAC10");
        weapon.addLookupName("IS Ultra AC/10");
        weapon.heat = 4;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 13.0f;
        weapon.criticals = 7;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 253;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createISUltraAC20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/20";
        weapon.setInternalName("ISUltraAC20");
        weapon.addLookupName("IS Ultra AC/20");
        weapon.heat = 8;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 10;
        weapon.extremeRange = 14;
        weapon.tonnage = 15.0f;
        weapon.criticals = 10;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 282;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createISRAC2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Rotary AC/2";
        weapon.setInternalName("ISRotaryAC2");
        weapon.addLookupName("IS Rotary AC/2");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_ROTARY;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 8.0f;
        weapon.criticals = 3;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 118;
        String[] modes = { "Single", "2-shot", "4-shot", "6-shot" };
        weapon.setModes(modes);

        // explosive when jammed
        weapon.explosive = true;

        return weapon;
    }

    public static WeaponType createISRAC5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Rotary AC/5";
        weapon.setInternalName("ISRotaryAC5");
        weapon.addLookupName("IS Rotary AC/5");
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_ROTARY;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 10.0f;
        weapon.criticals = 6;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 247;
        String[] modes = { "Single", "2-shot", "4-shot", "6-shot" };
        weapon.setModes(modes);

        // explosive when jammed
        weapon.explosive = true;

        return weapon;
    }

    public static WeaponType createISStreakSRM2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Streak SRM 2";
        weapon.setInternalName("ISStreakSRM2");
        weapon.addLookupName("IS Streak SRM-2");
        weapon.addLookupName("IS Streak SRM 2");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.5f;
        weapon.criticals = 1;
        weapon.flags |= F_NO_FIRES;
        weapon.bv = 30;

        return weapon;
    }

    public static WeaponType createISStreakSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Streak SRM 4";
        weapon.setInternalName("ISStreakSRM4");
        weapon.addLookupName("IS Streak SRM-4");
        weapon.addLookupName("IS Streak SRM 4");
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 3.0f;
        weapon.criticals = 1;
        weapon.bv = 59;

        return weapon;
    }

    public static WeaponType createISStreakSRM6() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Streak SRM 6";
        weapon.setInternalName("ISStreakSRM6");
        weapon.addLookupName("IS Streak SRM-6");
        weapon.addLookupName("IS Streak SRM 6");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 4.5f;
        weapon.criticals = 2;
        weapon.bv = 89;

        return weapon;
    }


    public static WeaponType createISMRM10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "MRM 10";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("MRM-10");
        weapon.addLookupName("ISMRM10");
        weapon.addLookupName("IS MRM 10");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 56;

        return weapon;
    }


    public static WeaponType createISMRM20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "MRM 20";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("MRM-20");
        weapon.addLookupName("ISMRM20");
        weapon.addLookupName("IS MRM 20");
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.bv = 112;

        return weapon;
    }



    public static WeaponType createISMRM30() {
        WeaponType weapon = new WeaponType();

        weapon.name = "MRM 30";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("MRM-30");

        weapon.addLookupName("ISMRM30");
        weapon.addLookupName("IS MRM 30");
        weapon.heat = 10;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 30;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.bv = 168;

        return weapon;
    }



    public static WeaponType createISMRM40() {
        WeaponType weapon = new WeaponType();

        weapon.name = "MRM 40";
        weapon.setInternalName(weapon.name);
        weapon.addLookupName("MRM-40");
        weapon.addLookupName("ISMRM40");
        weapon.addLookupName("IS MRM 40");
        weapon.heat = 12;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 40;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.extremeRange = 16;
        weapon.tonnage = 12.0f;
        weapon.criticals = 7;
        weapon.bv = 224;

        return weapon;
    }

    public static WeaponType createISAMS() {
        WeaponType weapon = new WeaponType();

        weapon.name = "AMS";
        weapon.setInternalName("ISAntiMissileSystem");
        weapon.addLookupName("IS Anti-Missile System");
        weapon.addLookupName("IS AMS");
        weapon.heat = 1;
        weapon.rackSize = 2;
        weapon.damage = 1;  // # of d6 of missiles affected
        weapon.ammoType = AmmoType.T_AMS;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 32;
        weapon.flags |= F_AUTO_TARGET;
        String[] modes = { "On", "Off" };
        weapon.setModes(modes);
        weapon.setInstantModeSwitch(false);

        return weapon;
    }

    public static WeaponType createISNarc() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Narc";
        weapon.setInternalName("ISNarcBeacon");
        weapon.addLookupName("IS Narc Beacon");
        weapon.addLookupName("IS Narc Missile Beacon");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NARC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 30;
        weapon.flags |= F_NO_FIRES;

        return weapon;
    }


    // Start of Clan Level2 weapons

    public static WeaponType createCLERPPC() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER PPC";
        weapon.setInternalName("CLERPPC");
        weapon.addLookupName("Clan ER PPC");
        weapon.heat = 15;
        weapon.damage = 15;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 23;
        weapon.extremeRange = 28;
        weapon.waterShortRange = 4;
        weapon.waterMediumRange = 10;
        weapon.waterLongRange = 16;
        weapon.waterExtremeRange = 20;
        weapon.tonnage = 6.0f;
        weapon.criticals = 2;
        weapon.flags |= F_PPC | F_DIRECT_FIRE;
        weapon.bv = 412;

        return weapon;
    }

    public static WeaponType createCLERLargeLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Large Laser";
        weapon.setInternalName("CLERLargeLaser");
        weapon.addLookupName("Clan ER Large Laser");
        weapon.heat = 12;
        weapon.damage = 10;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 8;
        weapon.mediumRange = 15;
        weapon.longRange = 25;
        weapon.extremeRange = 30;
        weapon.waterShortRange = 5;
        weapon.waterMediumRange = 10;
        weapon.waterLongRange = 16;
        weapon.waterExtremeRange = 20;
        weapon.tonnage = 4.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 249;

        return weapon;
    }

    public static WeaponType createCLERMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Medium Laser";
        weapon.setInternalName("CLERMediumLaser");
        weapon.addLookupName("Clan ER Medium Laser");
        weapon.heat = 5;
        weapon.damage = 7;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.waterShortRange = 3;
        weapon.waterMediumRange = 7;
        weapon.waterLongRange = 10;
        weapon.waterExtremeRange = 14;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 108;

        return weapon;
    }

    public static WeaponType createCLERSmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Small Laser";
        weapon.setInternalName("CLERSmallLaser");
        weapon.addLookupName("Clan ER Small Laser");
        weapon.heat = 2;
        weapon.damage = 5;
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
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 31;

        return weapon;
    }

    public static WeaponType createCLERMicroLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ER Micro Laser";
        weapon.setInternalName("CLERMicroLaser");
        weapon.addLookupName("Clan ER Micro Laser");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 4;
        weapon.extremeRange = 4;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 2;
        weapon.waterExtremeRange = 4;
        weapon.tonnage = 0.25f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 7;

        return weapon;
    }

    public static WeaponType createCLFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Flamer";
        weapon.setInternalName("CLFlamer");
        weapon.addLookupName("Clan Flamer");
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_FLAMER;
        weapon.bv = 6;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createCLVehicleFlamer() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Vehicle Flamer";
        weapon.setInternalName("CLVehicleFlamer");
        weapon.addLookupName("Clan Vehicle Flamer");
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_VEHICLE_FLAMER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_FLAMER;
        weapon.bv = 5;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }


    public static WeaponType createCLHeavyLargeLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Heavy Large Laser";
        weapon.setInternalName("CLHeavyLargeLaser");
        weapon.addLookupName("Clan Large Heavy Laser");
        weapon.heat = 18;
        weapon.damage = 16;
        weapon.toHitModifier = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.waterShortRange = 3;
        weapon.waterMediumRange = 6;
        weapon.waterLongRange = 9;
        weapon.waterExtremeRange = 12;
        weapon.tonnage = 4.0f;
        weapon.criticals = 3;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 243;

        return weapon;
    }

    public static WeaponType createCLHeavyMediumLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Heavy Medium Laser";
        weapon.setInternalName("CLHeavyMediumLaser");
        weapon.addLookupName("Clan Medium Heavy Laser");
        weapon.heat = 7;
        weapon.damage = 10;
        weapon.toHitModifier = 1;
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
        weapon.tonnage = 1.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 76;

        return weapon;
    }

    public static WeaponType createCLHeavySmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Heavy Small Laser";
        weapon.setInternalName("CLHeavySmallLaser");
        weapon.addLookupName("Clan Small Heavy Laser");
        weapon.heat = 3;
        weapon.damage = 6;
        weapon.toHitModifier = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 2;
        weapon.waterExtremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 15;

        return weapon;
    }

    public static WeaponType createCLMG() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Machine Gun";
        weapon.setInternalName("CLMG");
        weapon.addLookupName("Clan Machine Gun");
        weapon.heat = 0;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.25f;
        weapon.criticals = 1;
        weapon.bv = 5;

        return weapon;
    }

    public static WeaponType createCLLightMG() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Light Machine Gun";
        weapon.setInternalName("CLLightMG");
        weapon.addLookupName("Clan Light Machine Gun");
        weapon.heat = 0;
        weapon.damage = 1;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_MG_LIGHT;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.extremeRange = 8;
        weapon.tonnage = 0.25f;
        weapon.criticals = 1;
        weapon.bv = 5;

        return weapon;
    }

    public static WeaponType createCLHeavyMG() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Heavy Machine Gun";
        weapon.setInternalName("CLHeavyMG");
        weapon.addLookupName("Clan Heavy Machine Gun");
        weapon.heat = 0;
        weapon.damage = 3;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_MG_HEAVY;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 6;

        return weapon;
    }

    public static WeaponType createCLLRM5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 5";
        weapon.setInternalName("CLLRM5");
        weapon.addLookupName("Clan LRM-5");
        weapon.addLookupName("Clan LRM 5");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.bv = 55;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLLRM10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 10";
        weapon.setInternalName("CLLRM10");
        weapon.addLookupName("Clan LRM-10");
        weapon.addLookupName("Clan LRM 10");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 2.5f;
        weapon.criticals = 1;
        weapon.bv = 109;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLLRM15() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 15";
        weapon.setInternalName("CLLRM15");
        weapon.addLookupName("Clan LRM-15");
        weapon.addLookupName("Clan LRM 15");
        weapon.heat = 5;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 3.5f;
        weapon.criticals = 2;
        weapon.bv = 164;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLLRM20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LRM 20";
        weapon.setInternalName("CLLRM20");
        weapon.addLookupName("Clan LRM-20");
        weapon.addLookupName("Clan LRM 20");
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 5.0f;
        weapon.criticals = 4;
        weapon.bv = 220;
		weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLSRM2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "SRM 2";
        weapon.setInternalName("CLSRM2");
        weapon.addLookupName("Clan SRM-2");
        weapon.addLookupName("Clan SRM 2");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_NO_FIRES;
        weapon.bv = 21;

        return weapon;
    }

    public static WeaponType createCLSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.name = "SRM 4";
        weapon.setInternalName("CLSRM4");
        weapon.addLookupName("Clan SRM-4");
        weapon.addLookupName("Clan SRM 4");
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.bv = 39;

        return weapon;
    }

    public static WeaponType createCLSRM6() {
        WeaponType weapon = new WeaponType();

        weapon.name = "SRM 6";
        weapon.setInternalName("CLSRM6");
        weapon.addLookupName("Clan SRM-6");
        weapon.addLookupName("Clan SRM 6");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.extremeRange = 12;
        weapon.tonnage = 1.5f;
        weapon.criticals = 1;
        weapon.bv = 59;

        return weapon;
    }

    public static WeaponType createCLLargePulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Large Pulse Laser";
        weapon.setInternalName("CLLargePulseLaser");
        weapon.addLookupName("Clan Pulse Large Laser");
        weapon.addLookupName("Clan Large Pulse Laser");
        weapon.heat = 10;
        weapon.damage = 10;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 14;
        weapon.longRange = 20;
        weapon.extremeRange = 28;
        weapon.waterShortRange = 4;
        weapon.waterMediumRange = 10;
        weapon.waterLongRange = 14;
        weapon.waterExtremeRange = 20;
        weapon.tonnage = 6.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 265;

        return weapon;
    }

    public static WeaponType createCLMediumPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Medium Pulse Laser";
        weapon.setInternalName("CLMediumPulseLaser");
        weapon.addLookupName("Clan Pulse Med Laser");
        weapon.addLookupName("Clan Medium Pulse Laser");
        weapon.heat = 4;
        weapon.damage = 7;
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
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 111;

        return weapon;
    }

    public static WeaponType createCLSmallPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Small Pulse Laser";
        weapon.setInternalName("CLSmallPulseLaser");
        weapon.addLookupName("Clan Pulse Small Laser");
        weapon.addLookupName("Clan Small Pulse Laser");
        weapon.heat = 2;
        weapon.damage = 3;
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
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 24;

        return weapon;
    }

    public static WeaponType createCLMicroPulseLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Micro Pulse Laser";
        weapon.setInternalName("CLMicroPulseLaser");
        weapon.addLookupName("Clan Micro Pulse Laser");
        weapon.heat = 1;
        weapon.damage = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.waterShortRange = 1;
        weapon.waterMediumRange = 2;
        weapon.waterLongRange = 2;
        weapon.waterExtremeRange = 4;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 12;

        return weapon;
    }

    public static WeaponType createCLLBXAC2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 2-X AC";
        weapon.setInternalName("CLLBXAC2");
        weapon.addLookupName("Clan LB 2-X AC");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 4;
        weapon.shortRange = 10;
        weapon.mediumRange = 20;
        weapon.longRange = 30;
        weapon.extremeRange = 40;
        weapon.tonnage = 5.0f;
        weapon.criticals = 3;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 47;

        return weapon;
    }

    public static WeaponType createCLLBXAC5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 5-X AC";
        weapon.setInternalName("CLLBXAC5");
        weapon.addLookupName("Clan LB 5-X AC");
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 15;
        weapon.longRange = 24;
        weapon.extremeRange = 30;
        weapon.tonnage = 7.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 93;

        return weapon;
    }

    public static WeaponType createCLLBXAC10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 10-X AC";
        weapon.setInternalName("CLLBXAC10");
        weapon.addLookupName("Clan LB 10-X AC");
        weapon.heat = 2;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 148;

        return weapon;
    }

    public static WeaponType createCLLBXAC20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "LB 20-X AC";
        weapon.setInternalName("CLLBXAC20");
        weapon.addLookupName("Clan LB 20-X AC");
        weapon.heat = 6;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 12.0f;
        weapon.criticals = 9;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 237;

        return weapon;
    }

    public static WeaponType createCLGaussRifle() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Gauss Rifle";
        weapon.setInternalName("CLGaussRifle");
        weapon.addLookupName("Clan Gauss Rifle");
        weapon.heat = 1;
        weapon.damage = 15;
        weapon.ammoType = AmmoType.T_GAUSS;
        weapon.minimumRange = 2;
        weapon.shortRange = 7;
        weapon.mediumRange = 15;
        weapon.longRange = 22;
        weapon.extremeRange = 30;
        weapon.tonnage = 12.0f;
        weapon.criticals = 6;
        weapon.flags |= F_DIRECT_FIRE | F_NO_FIRES;
        weapon.explosive = true;
        weapon.bv = 321;

        return weapon;
    }

    public static WeaponType createCLUltraAC2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/2";
        weapon.setInternalName("CLUltraAC2");
        weapon.addLookupName("Clan Ultra AC/2");
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 2;
        weapon.shortRange = 9;
        weapon.mediumRange = 18;
        weapon.longRange = 27;
        weapon.extremeRange = 36;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 62;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createCLUltraAC5() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/5";
        weapon.setInternalName("CLUltraAC5");
        weapon.addLookupName("Clan Ultra AC/5");
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.extremeRange = 28;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 123;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createCLUltraAC10() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/10";
        weapon.setInternalName("CLUltraAC10");
        weapon.addLookupName("Clan Ultra AC/10");
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.extremeRange = 24;
        weapon.tonnage = 10.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 211;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createCLUltraAC20() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Ultra AC/20";
        weapon.setInternalName("CLUltraAC20");
        weapon.addLookupName("Clan Ultra AC/20");
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 12.0f;
        weapon.criticals = 8;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 337;
        String[] modes = { "Single", "Ultra" };
        weapon.setModes(modes);

        return weapon;
    }

    public static WeaponType createCLStreakSRM2() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Streak SRM 2";
        weapon.setInternalName("CLStreakSRM2");
        weapon.addLookupName("Clan Streak SRM-2");
        weapon.addLookupName("Clan Streak SRM 2");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_NO_FIRES;
        weapon.bv = 40;

        return weapon;
    }

    public static WeaponType createCLStreakSRM4() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Streak SRM 4";
        weapon.setInternalName("CLStreakSRM4");
        weapon.addLookupName("Clan Streak SRM-4");
        weapon.addLookupName("Clan Streak SRM 4");
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 79;

        return weapon;
    }

    public static WeaponType createCLStreakSRM6() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Streak SRM 6";
        weapon.setInternalName("CLStreakSRM6");
        weapon.addLookupName("Clan Streak SRM-6");
        weapon.addLookupName("Clan Streak SRM 6");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 119;

        return weapon;
    }

    public static WeaponType createCLATM3() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ATM 3";
        weapon.setInternalName("CLATM3");
        weapon.addLookupName("Clan ATM-3");
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_ATM;
        weapon.minimumRange = 4;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 1.5f;
        weapon.criticals = 2;
        weapon.bv = 53;

        return weapon;
    }

    public static WeaponType createCLATM6() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ATM 6";
        weapon.setInternalName("CLATM6");
        weapon.addLookupName("Clan ATM-6");
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_ATM;
        weapon.minimumRange = 4;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 3.5f;
        weapon.criticals = 3;
        weapon.bv = 105;

        return weapon;
    }

    public static WeaponType createCLATM9() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ATM 9";
        weapon.setInternalName("CLATM9");
        weapon.addLookupName("Clan ATM-9");
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 9;
        weapon.ammoType = AmmoType.T_ATM;
        weapon.minimumRange = 4;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 5.0f;
        weapon.criticals = 4;
        weapon.bv = 147;

        return weapon;
    }

    public static WeaponType createCLATM12() {
        WeaponType weapon = new WeaponType();

        weapon.name = "ATM 12";
        weapon.setInternalName("CLATM12");
        weapon.addLookupName("Clan ATM-12");
        weapon.heat = 8;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 12;
        weapon.ammoType = AmmoType.T_ATM;
        weapon.minimumRange = 4;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.extremeRange = 20;
        weapon.tonnage = 7.0f;
        weapon.criticals = 5;
        weapon.bv = 212;

        return weapon;
    }

    public static WeaponType createInfRifle() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfMG() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfSRM() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfInfernoSRM() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_INFERNO;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfLRM() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES;
        weapon.bv = 4; // ???
		weapon.setModes(new String[] {"", "Indirect"}); // ?

        return weapon;
    }

    public static WeaponType createInfLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_INFANTRY | F_NO_FIRES;
        weapon.bv = 4; // ???

        return weapon;
    }

    public static WeaponType createInfFlamer() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_FLAMER | F_DIRECT_FIRE | F_INFANTRY;
        // In www.classicbattletech.com/PDF/AskPMForumArchiveandFAQ.pdf,
        // pg. 23, Randall Bills says "No" to flamer-equipped infantry
        // doing heat instead of damage.

        return weapon;
    }

    public static WeaponType createCLAMS() {
        WeaponType weapon = new WeaponType();

        weapon.name = "AMS";
        weapon.setInternalName("CLAntiMissileSystem");
        weapon.addLookupName("Clan Anti-Missile Sys");
        weapon.addLookupName("Clan AMS");
        weapon.heat = 1;
        weapon.rackSize = 2;
        weapon.damage = 2; // # of d6 of missiles affected
        weapon.ammoType = AmmoType.T_AMS;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 63;
        weapon.flags |= F_AUTO_TARGET;
        String[] modes = { "On", "Off" };
        weapon.setModes(modes);
        weapon.setInstantModeSwitch(false);

        return weapon;
    }

    public static WeaponType createCLNarc() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Narc";
        weapon.setInternalName("CLNarcBeacon");
        weapon.addLookupName("Clan Narc Beacon");
        weapon.addLookupName("Clan Narc Missile Beacon");
        weapon.heat = 0;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 1;
        weapon.ammoType = AmmoType.T_NARC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.extremeRange = 16;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 30;
        weapon.flags |= F_NO_FIRES;

        return weapon;
    }

    // Anti-Mek attacks
    public static WeaponType createLegAttack() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Leg Attack";
        weapon.setInternalName(Infantry.LEG_ATTACK);
        weapon.heat = 0;
        weapon.damage = DAMAGE_SPECIAL;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_SOLO_ATTACK | F_NO_FIRES;

        return weapon;
    }

    public static WeaponType createSwarmMek() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Swarm Mek";
        weapon.setInternalName(Infantry.SWARM_MEK);
        weapon.heat = 0;
        weapon.damage = DAMAGE_SPECIAL;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_SOLO_ATTACK | F_NO_FIRES;

        return weapon;
    }

    public static WeaponType createStopSwarm() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Stop Swarm Attack";
        weapon.setInternalName(Infantry.STOP_SWARM);
        weapon.heat = 0;
        weapon.damage = DAMAGE_SPECIAL;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 0;
        weapon.mediumRange = 0;
        weapon.longRange = 0;
        weapon.extremeRange = 0;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_SOLO_ATTACK | F_NO_FIRES;

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
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR;

        return weapon;
    }

    public static WeaponType createBASingleMG() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE;

        return weapon;
    }
    public static WeaponType createBASingleFlamer() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_FLAMER;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAFlamer() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_FLAMER;

        return weapon;
    }
    public static WeaponType createBASmallLaser() {
        WeaponType weapon = new WeaponType();

        weapon.name = "Small Laser";
        weapon.setInternalName("BASmallLaser");
        weapon.addLookupName("BA-Small Laser");
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.rackSize = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.extremeRange = 4;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.bv = 0;
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_LASER | F_NO_FIRES;

        return weapon;
    }
    public static WeaponType createBACLERSmallLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createCLAdvancedSRM2() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBATwinFlamers() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR | F_FLAMER | F_DOUBLE_HITS;
        String[] modes = { "Damage", "Heat" };
        weapon.setModes(modes);

        return weapon;
    }
    public static WeaponType createBAInfernoSRM() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_BATTLEARMOR | F_INFERNO;

        return weapon;
    }
    public static WeaponType createBACLMicroPulseLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAMicroBomb() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createCLTorpedoLRM5() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createTwinSmallPulseLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createTripleSmallLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createTripleMG() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_MISSILE_HITS | F_DIRECT_FIRE;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createFenrirSRM4() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DOUBLE_HITS;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAAutoGL() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR;

        return weapon;
    }
    public static WeaponType createBAMagshotGR() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_DIRECT_FIRE | F_BATTLEARMOR;

        return weapon;
    }
    public static WeaponType createBAISMediumLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAISERSmallLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_BATTLEARMOR | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBACompactNARC() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_NO_FIRES | F_BATTLEARMOR;

        return weapon;
    }
    public static WeaponType createSlothSmallLaser() {
        WeaponType weapon = new WeaponType();

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
        weapon.flags |= F_LASER | F_MISSILE_HITS | F_DIRECT_FIRE | F_NO_FIRES;
        weapon.bv = 0;

        return weapon;
    }
    public static WeaponType createBAMineLauncher() {
        WeaponType weapon = new WeaponType();

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

    public static WeaponType createCLPROLRM1() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM2() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM3() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM4() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM5() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM6() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM7() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM8() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM9() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM10() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM11() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM12() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM13() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM14() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM15() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM16() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM17() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM18() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM19() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }

    public static WeaponType createCLPROLRM20() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        weapon.setModes(new String[] {"", "Indirect"});

        return weapon;
    }
    public static WeaponType createCLPROSRM1() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROSRM2() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROSRM3() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROSRM4() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROSRM5() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROSRM6() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM1() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM2() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM3() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM4() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM5() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createCLPROStreakSRM6() {
        WeaponType weapon = new WeaponType();
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
        weapon.flags |= F_PROTOMECH;
        return weapon;
    }

    public static WeaponType createISArrowIVSystem() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Arrow IV";
        weapon.setInternalName("ISArrowIV");
        weapon.addLookupName("ISArrowIVSystem");
        weapon.addLookupName("IS Arrow IV System");
        weapon.addLookupName("IS Arrow IV Missile System");
        weapon.heat = 10;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_ARROW_IV;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 5;
        weapon.extremeRange = 5; // No extreme range.
        weapon.tonnage = 15f;
        weapon.criticals = 15;
        weapon.bv = 171;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createCLArrowIVSystem() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Arrow IV";
        weapon.setInternalName("CLArrowIV");
        weapon.addLookupName("CLArrowIVSystem");
        weapon.addLookupName("Clan Arrow IV System");
        weapon.addLookupName("Clan Arrow IV Missile System");
        weapon.heat = 10;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_ARROW_IV;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;  //
        weapon.mediumRange = 2;
        weapon.longRange = 6;
        weapon.extremeRange = 6; // No extreme range.
        weapon.tonnage = 12f;
        weapon.criticals = 12;
        weapon.bv = 171;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createISLongTom() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Long Tom";
        weapon.setInternalName("ISLongTom");
        weapon.addLookupName("ISLongTomArtillery");
        weapon.addLookupName("IS Long Tom");
        weapon.heat = 20;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LONG_TOM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 20;
        weapon.extremeRange = 20; // No extreme range.
        weapon.tonnage = 30f;
        weapon.criticals = 30;
        weapon.bv = 171;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createCLLongTom() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Long Tom";
        weapon.setInternalName("CLLongTom");
        weapon.addLookupName("CLLongTomArtillery");
        weapon.addLookupName("Clan Long Tom");
        weapon.heat = 20;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LONG_TOM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;  //
        weapon.mediumRange = 2;
        weapon.longRange = 20;
        weapon.extremeRange = 20; // No extreme range.
        weapon.tonnage = 30f;
        weapon.criticals = 30;
        weapon.bv = 171;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createISSniper() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Sniper";
        weapon.setInternalName("ISSniper");
        weapon.addLookupName("ISSniperArtillery");
        weapon.addLookupName("IS Sniper");
        weapon.heat = 10;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_SNIPER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 12;
        weapon.extremeRange = 12; // No extreme range.
        weapon.tonnage = 20f;
        weapon.criticals = 20;
        weapon.bv = 86;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createCLSniper() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Sniper";
        weapon.setInternalName("CLSniper");
        weapon.addLookupName("CLSniperArtillery");
        weapon.addLookupName("Clan Sniper");
        weapon.heat = 10;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_SNIPER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;  //
        weapon.mediumRange = 2;
        weapon.longRange = 12;
        weapon.extremeRange = 12; // No extreme range.
        weapon.tonnage = 20f;
        weapon.criticals = 20;
        weapon.bv = 86;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createISThumper() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Thumper";
        weapon.setInternalName("ISThumper");
        weapon.addLookupName("ISThumperArtillery");
        weapon.addLookupName("IS Thumper");
        weapon.heat = 5;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_THUMPER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 14;
        weapon.extremeRange = 14; // No extreme range.
        weapon.tonnage = 15f;
        weapon.criticals = 15;
        weapon.bv = 40;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public static WeaponType createCLThumper() {
        WeaponType weapon = new WeaponType();
        weapon.name = "Thumper";
        weapon.setInternalName("CLThumper");
        weapon.addLookupName("CLThumperArtillery");
        weapon.addLookupName("Clan Thumper");
        weapon.heat = 5;
        weapon.damage = DAMAGE_ARTILLERY;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_THUMPER;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;  //
        weapon.mediumRange = 2;
        weapon.longRange = 14;
        weapon.extremeRange = 14; // No extreme range.
        weapon.tonnage = 15f;
        weapon.criticals = 15;
        weapon.bv = 40;
        weapon.flags |= F_ARTILLERY;
        return weapon;
    }

    public String toString() {
        return "WeaponType: " + name;
    }
}
