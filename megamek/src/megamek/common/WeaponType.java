/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
    public static final int     WEAPON_NA = Integer.MIN_VALUE;
    
    // weapon flags (note: many weapons can be identified by their ammo type)
    public static final int     F_DIRECT_FIRE   = 0x0001; // marks any weapon affected by a targetting computer
    public static final int     F_FLAMER        = 0x0002;
    public static final int     F_LASER         = 0x0004; // for eventual glazed armor purposes
    public static final int     F_PPC           = 0x0008; //              "
    public static final int     F_LBX           = 0x0010;
    public static final int     F_ULTRA         = 0x0020;
    
    private int     heat;
    private int     damage;
    private int     rackSize; // or AC size, or whatever
    private int     ammoType;
    
    private int     minimumRange;
    private int     shortRange;
    private int     mediumRange;
    private int     longRange;
    
    private WeaponType() {
        ;
    }
    
    public int getHeat() {
        return heat;
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
    
    /**
     * Add all the types of weapons we can create to the list
     */
    public static void initializeTypes() {
        // all tech level 1 weapons
        EquipmentType.addType(createFlamer());
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
        EquipmentType.addType(createISUltraAC2());
        EquipmentType.addType(createISUltraAC5());
        EquipmentType.addType(createISUltraAC10());
        EquipmentType.addType(createISUltraAC20());
        EquipmentType.addType(createISStreakSRM2());
        EquipmentType.addType(createISStreakSRM4());
        EquipmentType.addType(createISStreakSRM6());
        EquipmentType.addType(createISMRM10());
        EquipmentType.addType(createISMRM20());
        EquipmentType.addType(createISMRM30());
        EquipmentType.addType(createISMRM40());
        
        // Start of Clan Level2 weapons
        EquipmentType.addType(createCLERPPC());
        EquipmentType.addType(createCLERLargeLaser());
        EquipmentType.addType(createCLERMediumLaser());
        EquipmentType.addType(createCLERSmallLaser());
        EquipmentType.addType(createCLERMicroLaser());
        EquipmentType.addType(createCLFlamer());
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
        
    }
    
    public static WeaponType createFlamer() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Flamer";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = weapon.name;
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_FLAMER;
        weapon.bv = 6;
        
        return weapon;
    }
    
    public static WeaponType createLargeLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Large Laser";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = weapon.name;
        weapon.heat = 8;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 124;
        
        return weapon;
    }
    
    public static WeaponType createMediumLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Medium Laser";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = weapon.name;
        weapon.heat = 3;
        weapon.damage = 5;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 46;
        
        return weapon;
    }
    
    public static WeaponType createSmallLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Small Laser";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = weapon.name;
        weapon.heat = 1;
        weapon.damage = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 9;
        
        return weapon;
    }
    
    public static WeaponType createPPC() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Particle Cannon";
        weapon.internalName = weapon.name;
        weapon.mepName = "PPC";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 10;
        weapon.damage = 10;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = 3;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_PPC | F_DIRECT_FIRE;
        weapon.bv = 176;
        
        return weapon;
    }
    
    public static WeaponType createMG() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Machine Gun";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = weapon.name;
        weapon.heat = 0;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 5;
        
        return weapon;
    }
    
    public static WeaponType createAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Auto Cannon/2";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = "AC/2";
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 4;
        weapon.shortRange = 8;
        weapon.mediumRange = 16;
        weapon.longRange = 24;
        weapon.tonnage = 6.0f;
        weapon.criticals = 1;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 37;
        
        return weapon;
    }
    
    public static WeaponType createAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Auto Cannon/5";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = "AC/5";
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 3;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 8.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 70;
        
        return weapon;
    }
    
    public static WeaponType createAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Auto Cannon/10";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = "AC/10";
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.tonnage = 12.0f;
        weapon.criticals = 7;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 124;
        
        return weapon;
    }
    
    public static WeaponType createAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Auto Cannon/20";
        weapon.internalName = weapon.name;
        weapon.mepName = weapon.name;
        weapon.mtfName = "AC/20";
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 14.0f;
        weapon.criticals = 10;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 178;
        
        return weapon;
    }
    
    public static WeaponType createLRM5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 5";
        weapon.internalName = weapon.name;
        weapon.mepName = "LRM-5";
        weapon.mtfName = weapon.name;
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 45;
        
        return weapon;
    }
    
    public static WeaponType createLRM10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 10";
        weapon.internalName = weapon.name;
        weapon.mepName = "LRM-10";
        weapon.mtfName = weapon.name;
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.bv = 90;
        
        return weapon;
    }
    
    public static WeaponType createLRM15() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 15";
        weapon.internalName = weapon.name;
        weapon.mepName = "LRM-15";
        weapon.mtfName = weapon.name;
        weapon.heat = 5;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.bv = 136;
        
        return weapon;
    }
    
    public static WeaponType createLRM20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 20";
        weapon.internalName = weapon.name;
        weapon.mepName = "LRM-20";
        weapon.mtfName = weapon.name;
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 6;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.bv = 181;
        
        return weapon;
    }
    
    public static WeaponType createSRM2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "SRM 2";
        weapon.internalName = weapon.name;
        weapon.mepName = "SRM-2";
        weapon.mtfName = weapon.name;
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.bv = 21;
        
        return weapon;
    }
    
    public static WeaponType createSRM4() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "SRM 4";
        weapon.internalName = weapon.name;
        weapon.mepName = "SRM-4";
        weapon.mtfName = weapon.name;
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 39;
        
        return weapon;
    }
    
    public static WeaponType createSRM6() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "SRM 6";
        weapon.internalName = weapon.name;
        weapon.mepName = "SRM-6";
        weapon.mtfName = weapon.name;
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 59;
        
        return weapon;
    }
    
    
    //Start of Inner Sphere Level2 weapons
    
    public static WeaponType createISERPPC() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Particle Projector Cannon";
        weapon.internalName = "ISERPPC";
        weapon.mepName = "ISERPPC";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 15;
        weapon.damage = 10;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 23;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_PPC | F_DIRECT_FIRE;
        weapon.bv = 229;
        
        return weapon;
    }
    
    public static WeaponType createISERLargeLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Large Laser";
        weapon.internalName = "ISERLargeLaser";
        weapon.mepName = "ISERLargeLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 12;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 19;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 163;
        
        return weapon;
    }
    
    public static WeaponType createISERMediumLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Medium Laser";
        weapon.internalName = "ISERMediumLaser";
        weapon.mepName = "ISERMediumLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 5;
        weapon.damage = 5;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 62;
        
        return weapon;
    }
    
    public static WeaponType createISERSmallLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Small Laser";
        weapon.internalName = "ISERSmallLaser";
        weapon.mepName = "ISERSmallLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = 3;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 5;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 17;
        
        return weapon;
    }
    
    public static WeaponType createISLargePulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Large Pulse Laser";
        weapon.internalName = "ISLargePulseLaser";
        weapon.mepName = "ISLargePulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 10;
        weapon.damage = 9;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 10;
        weapon.tonnage = 7.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 119;
        
        return weapon;
    }
    
    public static WeaponType createISMediumPulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Medium Pulse Laser";
        weapon.internalName = "ISMediumPulseLaser";
        weapon.mepName = "ISMediumPulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = 6;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 48;
        
        return weapon;
    }
    
    public static WeaponType createISSmallPulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Small Pulse Laser";
        weapon.internalName = "ISSmallPulseLaser";
        weapon.mepName = "ISSmallPulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 12;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/2";
        weapon.internalName = "ISLBXAC2";
        weapon.mepName = "ISLBXAC2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 4;
        weapon.shortRange = 9;
        weapon.mediumRange = 18;
        weapon.longRange = 27;
        weapon.tonnage = 6.0f;
        weapon.criticals = 4;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 42;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/5";
        weapon.internalName = "ISLBXAC5";
        weapon.mepName = "ISLBXAC5";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 3;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 8.0f;
        weapon.criticals = 5;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 83;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/10";
        weapon.internalName = "ISLBXAC10";
        weapon.mepName = "ISLBXAC10";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 11.0f;
        weapon.criticals = 6;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 148;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/20";
        weapon.internalName = "ISLBXAC20";
        weapon.mepName = "ISLBXAC20";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 6;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 14.0f;
        weapon.criticals = 11;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 237;
        
        return weapon;
    }
    
    public static WeaponType createISGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Gauss Rifle";
        weapon.internalName = "ISGaussRifle";
        weapon.mepName = "ISGaussRifle";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 15;
        weapon.ammoType = AmmoType.T_GAUSS;
        weapon.minimumRange = 2;
        weapon.shortRange = 7;
        weapon.mediumRange = 15;
        weapon.longRange = 22;
        weapon.tonnage = 15.0f;
        weapon.criticals = 7;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 321;
        
        return weapon;
    }
    
    public static WeaponType createISLightGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Light Gauss Rifle";
        weapon.internalName = "ISLightGaussRifle";
        weapon.mepName = "ISLightGaussRifle";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_GAUSS;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 17;
        weapon.longRange = 25;
        weapon.tonnage = 12.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 159;
        
        return weapon;
    }
    
    public static WeaponType createISUltraAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/2";
        weapon.internalName = "ISUltraAC2";
        weapon.mepName = "ISUltraAC2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 17;
        weapon.longRange = 25;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 56;
        
        return weapon;
    }
    
    public static WeaponType createISUltraAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/5";
        weapon.internalName = "ISUltraAC5";
        weapon.mepName = "ISUltraAC5";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 2;
        weapon.shortRange = 6;
        weapon.mediumRange = 13;
        weapon.longRange = 20;
        weapon.tonnage = 9.0f;
        weapon.criticals = 5;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 113;
        
        return weapon;
    }
    
    public static WeaponType createISUltraAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/10";
        weapon.internalName = "ISUltraAC10";
        weapon.mepName = "ISUltraAC10";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 13.0f;
        weapon.criticals = 7;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 253;
        
        return weapon;
    }
    
    public static WeaponType createISUltraAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/20";
        weapon.internalName = "ISUltraAC20";
        weapon.mepName = "ISUltraAC20";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 8;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 10;
        weapon.tonnage = 15.0f;
        weapon.criticals = 10;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 282;
        
        return weapon;
    }
    
    public static WeaponType createISStreakSRM2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Streak SRM 2";
        weapon.internalName = "ISStreakSRM2";
        weapon.mepName = "ISStreakSRM2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 1.5f;
        weapon.criticals = 1;
        weapon.bv = 30;
        
        return weapon;
    }
    
    public static WeaponType createISStreakSRM4() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Streak SRM 4";
        weapon.internalName = "ISStreakSRM4";
        weapon.mepName = "ISStreakSRM4";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 3.0f;
        weapon.criticals = 1;
        weapon.bv = 59;
        
        return weapon;
    }
    
    public static WeaponType createISStreakSRM6() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Streak SRM 6";
        weapon.internalName = "ISStreakSRM6";
        weapon.mepName = "ISStreakSRM6";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 4.5f;
        weapon.criticals = 2;
        weapon.bv = 89;
        
        return weapon;
    }
    
    
    public static WeaponType createISMRM10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "MRM 10";
        weapon.internalName = weapon.name;
        weapon.mepName = "MRM-10";
        weapon.mtfName = "ISMRM10";
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 56;
        
        return weapon;
    }
    
    
    public static WeaponType createISMRM20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "MRM 20";
        weapon.internalName = weapon.name;
        weapon.mepName = "MRM-20";
        weapon.mtfName = "ISMRM20";
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.bv = 112;
        
        return weapon;
    }
    
    
    
    public static WeaponType createISMRM30() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "MRM 30";
        weapon.internalName = weapon.name;
        weapon.mepName = "MRM-30";
        weapon.mtfName = "ISMRM30";
        weapon.heat = 10;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 30;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.bv = 168;
        
        return weapon;
    }
    
    
    
    public static WeaponType createISMRM40() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "MRM 40";
        weapon.internalName = weapon.name;
        weapon.mepName = "MRM-40";
        weapon.mtfName = "ISMRM40";
        weapon.heat = 12;
        weapon.damage = DAMAGE_MISSILE;
        weapon.toHitModifier = 1;
        weapon.rackSize = 40;
        weapon.ammoType = AmmoType.T_MRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 8;
        weapon.longRange = 15;
        weapon.tonnage = 12.0f;
        weapon.criticals = 7;
        weapon.bv = 224;
        
        return weapon;
    }
    
    
    // Start of Clan Level2 weapons
    
    public static WeaponType createCLERPPC() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Particle Projector Cannon";
        weapon.internalName = "CLERPPC";
        weapon.mepName = "CLERPPC";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 15;
        weapon.damage = 15;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 23;
        weapon.tonnage = 6.0f;
        weapon.criticals = 2;
        weapon.flags |= F_PPC | F_DIRECT_FIRE;
        weapon.bv = 412;
        
        return weapon;
    }
    
    public static WeaponType createCLERLargeLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Large Laser";
        weapon.internalName = "CLERLargeLaser";
        weapon.mepName = "CLERLargeLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 12;
        weapon.damage = 10;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 8;
        weapon.mediumRange = 15;
        weapon.longRange = 25;
        weapon.tonnage = 4.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 249;
        
        return weapon;
    }
    
    public static WeaponType createCLERMediumLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Medium Laser";
        weapon.internalName = "CLERMediumLaser";
        weapon.mepName = "CLERMediumLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 5;
        weapon.damage = 7;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 108;
        
        return weapon;
    }
    
    public static WeaponType createCLERSmallLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Small Laser";
        weapon.internalName = "CLERSmallLaser";
        weapon.mepName = "CLERSmallLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = 5;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 31;
        
        return weapon;
    }
    
    public static WeaponType createCLERMicroLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER Micro Laser";
        weapon.internalName = "CLERMicroLaser";
        weapon.mepName = "CLERMicroLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 4;
        weapon.tonnage = 0.25f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 7;
        
        return weapon;
    }
    
    public static WeaponType createCLFlamer() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Flamer";
        weapon.internalName = "CLFlamer";
        weapon.mepName = "CLFlamer";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 3;
        weapon.damage = 2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_FLAMER;
        weapon.bv = 6;
        
        return weapon;
    }
    
    public static WeaponType createCLHeavyLargeLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Heavy Large Laser";
        weapon.internalName = "CLHeavyLargeLaser";
        weapon.mepName = "CLHeavyLargeLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 18;
        weapon.damage = 16;
        weapon.toHitModifier = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
        weapon.tonnage = 4.0f;
        weapon.criticals = 3;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 243;
        
        return weapon;
    }
    
    public static WeaponType createCLHeavyMediumLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Heavy Medium Laser";
        weapon.internalName = "CLHeavyMediumLaser";
        weapon.mepName = "CLHeavyMediumLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 7;
        weapon.damage = 10;
        weapon.toHitModifier = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 1.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 76;
        
        return weapon;
    }
    
    public static WeaponType createCLHeavySmallLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Heavy Small Laser";
        weapon.internalName = "CLHeavySmallLaser";
        weapon.mepName = "CLHeavySmallLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 3;
        weapon.damage = 6;
        weapon.toHitModifier = 1;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 15;
        
        return weapon;
    }
    
    public static WeaponType createCLMG() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Machine Gun";
        weapon.internalName = "CLMG";
        weapon.mepName = "CLMG";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 0;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.25f;
        weapon.criticals = 1;
        weapon.bv = 5;
        
        return weapon;
    }
    
    public static WeaponType createCLLightMG() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Light Machine Gun";
        weapon.internalName = "CLLightMG";
        weapon.mepName = "CLLightMG";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 0;
        weapon.damage = 1;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_MG_LIGHT;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.tonnage = 0.25f;
        weapon.criticals = 1;
        weapon.bv = 5;
        
        return weapon;
    }
    
    public static WeaponType createCLHeavyMG() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Heavy Machine Gun";
        weapon.internalName = "CLHeavyMG";
        weapon.mepName = "CLHeavyMG";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 0;
        weapon.damage = 3;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_MG_HEAVY;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 6;
        
        return weapon;
    }
    
    public static WeaponType createCLLRM5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 5";
        weapon.internalName = "CLLRM5";
        weapon.mepName = "CLLRM5";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.bv = 55;
        
        return weapon;
    }
    
    public static WeaponType createCLLRM10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 10";
        weapon.internalName = "CLLRM10";
        weapon.mepName = "CLLRM10";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 2.5f;
        weapon.criticals = 1;
        weapon.bv = 109;
        
        return weapon;
    }
    
    public static WeaponType createCLLRM15() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 15";
        weapon.internalName = "CLLRM15";
        weapon.mepName = "CLLRM15";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 5;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 15;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 3.5f;
        weapon.criticals = 2;
        weapon.bv = 164;
        
        return weapon;
    }
    
    public static WeaponType createCLLRM20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LRM 20";
        weapon.internalName = "CLLRM20";
        weapon.mepName = "CLLRM20";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 6;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 5.0f;
        weapon.criticals = 4;
        weapon.bv = 220;
        
        return weapon;
    }
    
    public static WeaponType createCLSRM2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "SRM 2";
        weapon.internalName = "CLSRM2";
        weapon.mepName = "CLSRM2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.bv = 21;
        
        return weapon;
    }
    
    public static WeaponType createCLSRM4() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "SRM 4";
        weapon.internalName = "CLSRM4";
        weapon.mepName = "CLSRM4";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.bv = 39;
        
        return weapon;
    }
    
    public static WeaponType createCLSRM6() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "SRM 6";
        weapon.internalName = "CLSRM6";
        weapon.mepName = "CLSRM6";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 6;
        weapon.longRange = 9;
        weapon.tonnage = 1.5f;
        weapon.criticals = 1;
        weapon.bv = 59;
        
        return weapon;
    }
    
    public static WeaponType createCLLargePulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Large Pulse Laser";
        weapon.internalName = "CLLargePulseLaser";
        weapon.mepName = "CLLargePulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 10;
        weapon.damage = 10;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 6;
        weapon.mediumRange = 14;
        weapon.longRange = 20;
        weapon.tonnage = 6.0f;
        weapon.criticals = 2;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 265;
        
        return weapon;
    }
    
    public static WeaponType createCLMediumPulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Medium Pulse Laser";
        weapon.internalName = "CLMediumPulseLaser";
        weapon.mepName = "CLMediumPulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = 7;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 111;
        
        return weapon;
    }
    
    public static WeaponType createCLSmallPulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Small Pulse Laser";
        weapon.internalName = "CLSmallPulseLaser";
        weapon.mepName = "CLSmallPulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 24;
        
        return weapon;
    }
    
    public static WeaponType createCLMicroPulseLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Micro Pulse Laser";
        weapon.internalName = "CLMicroPulseLaser";
        weapon.mepName = "CLMicroPulseLaser";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 3;
        weapon.toHitModifier = -2;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.5f;
        weapon.criticals = 1;
        weapon.flags |= F_LASER | F_DIRECT_FIRE;
        weapon.bv = 12;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/2";
        weapon.internalName = "CLLBXAC2";
        weapon.mepName = "CLLBXAC2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 4;
        weapon.shortRange = 10;
        weapon.mediumRange = 20;
        weapon.longRange = 30;
        weapon.tonnage = 5.0f;
        weapon.criticals = 3;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 47;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/5";
        weapon.internalName = "CLLBXAC5";
        weapon.mepName = "CLLBXAC5";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 15;
        weapon.longRange = 24;
        weapon.tonnage = 7.0f;
        weapon.criticals = 4;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 93;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/10";
        weapon.internalName = "CLLBXAC10";
        weapon.mepName = "CLLBXAC10";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 148;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LBX Auto Cannon/20";
        weapon.internalName = "CLLBXAC20";
        weapon.mepName = "CLLBXAC20";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 6;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 12.0f;
        weapon.criticals = 9;
        weapon.flags |= F_LBX | F_DIRECT_FIRE;
        weapon.bv = 237;
        
        return weapon;
    }
    
    public static WeaponType createCLGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Gauss Rifle";
        weapon.internalName = "CLGaussRifle";
        weapon.mepName = "CLGaussRifle";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 15;
        weapon.ammoType = AmmoType.T_GAUSS;
        weapon.minimumRange = 2;
        weapon.shortRange = 7;
        weapon.mediumRange = 15;
        weapon.longRange = 22;
        weapon.tonnage = 12.0f;
        weapon.criticals = 6;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 321;
        
        return weapon;
    }
    
    public static WeaponType createCLUltraAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/2";
        weapon.internalName = "CLUltraAC2";
        weapon.mepName = "CLUltraAC2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 2;
        weapon.shortRange = 9;
        weapon.mediumRange = 18;
        weapon.longRange = 27;
        weapon.tonnage = 5.0f;
        weapon.criticals = 2;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 62;
        
        return weapon;
    }
    
    public static WeaponType createCLUltraAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/5";
        weapon.internalName = "CLUltraAC5";
        weapon.mepName = "CLUltraAC5";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 7.0f;
        weapon.criticals = 3;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 123;
        
        return weapon;
    }
    
    public static WeaponType createCLUltraAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/10";
        weapon.internalName = "CLUltraAC10";
        weapon.mepName = "CLUltraAC10";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 10.0f;
        weapon.criticals = 4;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 211;
        
        return weapon;
    }
    
    public static WeaponType createCLUltraAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra Auto Cannon/20";
        weapon.internalName = "CLUltraAC20";
        weapon.mepName = "CLUltraAC20";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 12.0f;
        weapon.criticals = 8;
        weapon.flags |= F_ULTRA | F_DIRECT_FIRE;
        weapon.bv = 337;
        
        return weapon;
    }
    
    public static WeaponType createCLStreakSRM2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Streak SRM 2";
        weapon.internalName = "CLStreakSRM2";
        weapon.mepName = "CLStreakSRM2";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 2;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 1.0f;
        weapon.criticals = 1;
        weapon.bv = 40;
        
        return weapon;
    }
    
    public static WeaponType createCLStreakSRM4() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Streak SRM 4";
        weapon.internalName = "CLStreakSRM4";
        weapon.mepName = "CLStreakSRM4";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 3;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 4;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 2.0f;
        weapon.criticals = 1;
        weapon.bv = 79;
        
        return weapon;
    }
    
    public static WeaponType createCLStreakSRM6() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Streak SRM 6";
        weapon.internalName = "CLStreakSRM6";
        weapon.mepName = "CLStreakSRM6";
        weapon.mtfName = weapon.mepName;
        weapon.heat = 4;
        weapon.damage = DAMAGE_MISSILE;
        weapon.rackSize = 6;
        weapon.ammoType = AmmoType.T_SRM_STREAK;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 3.0f;
        weapon.criticals = 2;
        weapon.bv = 119;
        
        return weapon;
    }
    
    public String toString() {
        return "WeaponType: " + name;
    }
}
