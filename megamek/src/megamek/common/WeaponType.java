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
public class WeaponType extends EquipmentType
{
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
        weapon.flags |= F_LASER;
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
        weapon.flags |= F_LASER;
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
        weapon.flags |= F_LASER;
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
        weapon.flags |= F_PPC;
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
        weapon.flags |= F_LASER;
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
    
    
    public String toString() {
        return "WeaponType: " + name;
    }
}
