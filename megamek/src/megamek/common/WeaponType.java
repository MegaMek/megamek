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
    public static final int     DAMAGE_VARIABLE = -3;
    public static final int     WEAPON_NA = Integer.MIN_VALUE;
    
    // weapon flags (note: many weapons can be identified by their ammo type)
    public static final int     F_DIRECT_FIRE   = 0x0001; // marks any weapon affected by a targetting computer
    public static final int     F_FLAMER        = 0x0002;
    public static final int     F_LASER         = 0x0004; // for eventual glazed armor purposes
    public static final int     F_PPC           = 0x0008; //              "
    public static final int     F_AUTO_TARGET   = 0x0010; // for weapons that target automatically (AMS)
    
    // Need to distinguish infantry weapons from their bigger,
    // vehicle- and mech-mounted cousins.
    public static final int	F_INFANTRY	= 0x0F00; // there's no 0x0BAR
    
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
    
// + HentaiZonga
    public int getFireTN() {
        if(hasFlag(F_FLAMER))
          return 4;
        else if(hasFlag(F_PPC) || hasFlag(F_LASER))
          return 7;
        else if(ammoType == AmmoType.T_MG || ammoType == AmmoType.T_GAUSS_LIGHT || ammoType == AmmoType.T_GAUSS || ammoType == AmmoType.T_GAUSS_HEAVY )
          return TargetRoll.IMPOSSIBLE;
        else
          return 9;
    }
// - HentaiZonga
 
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
        
        // Start of Infantry weapons (Level1)
        EquipmentType.addType(createInfRifle());
        EquipmentType.addType(createInfMG());
        EquipmentType.addType(createInfSRM());
        EquipmentType.addType(createInfLRM());
        EquipmentType.addType(createInfLaser());
        EquipmentType.addType(createInfFlamer());
        
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
        EquipmentType.addType(createCLAMS());
    }
    
    public static WeaponType createFlamer() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Flamer";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Flamer";
        weapon.mtfName = "ISFlamer";
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
        weapon.mepName = "IS Large Laser";
        weapon.mtfName = "ISLargeLaser";
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
        weapon.mepName = "IS Medium Laser";
        weapon.mtfName = "ISMediumLaser";
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
        weapon.mepName = "IS Small Laser";
        weapon.mtfName = "ISSmallLaser";
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
        weapon.mepName = "IS PPC";
        weapon.mtfName = "ISPPC";
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
        weapon.mepName = "IS Machine Gun";
        weapon.mtfName = "ISMachine Gun";
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
        weapon.mepName = "IS Auto Cannon/2";
        weapon.mtfName = "ISAC2";
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
        weapon.mepName = "IS Auto Cannon/5";
        weapon.mtfName = "ISAC5";
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
        weapon.mepName = "IS Auto Cannon/10";
        weapon.mtfName = "ISAC10";
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
        weapon.mepName = "IS Auto Cannon/20";
        weapon.mtfName = "ISAC20";
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
        weapon.mepName = "IS LRM-5";
        weapon.mtfName = "ISLRM5";
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
        weapon.mepName = "IS LRM-10";
        weapon.mtfName = "ISLRM10";
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
        weapon.mepName = "IS LRM-15";
        weapon.mtfName = "ISLRM15";
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
        weapon.mepName = "IS LRM-20";
        weapon.mtfName = "ISLRM20";
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
        weapon.mepName = "IS SRM-2";
        weapon.mtfName = "ISSRM2";
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
        weapon.mepName = "IS SRM-4";
        weapon.mtfName = "ISSRM4";
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
        weapon.mepName = "IS SRM-6";
        weapon.mtfName = "ISSRM6";
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
        
        weapon.name = "ER PPC";
        weapon.internalName = "ISERPPC";
        weapon.mepName = "IS ER PPC";
        weapon.mtfName = "ISERPPC";
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
        weapon.mepName = "IS ER Large Laser";
        weapon.mtfName = "ISERLargeLaser";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "ISERMediumLaser";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "ISERSmallLaser";
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
        weapon.mepName = "IS Pulse Large Laser";
        weapon.mtfName = "ISLargePulseLaser";
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
        weapon.mepName = "IS Pulse Med Laser";
        weapon.mtfName = "ISMediumPulseLaser";
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
        weapon.mepName = "IS Pulse Small Laser";
        weapon.mtfName = "ISSmallPulseLaser";
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
        
        weapon.name = "LB 2-X AC";
        weapon.internalName = "ISLBXAC2";
        weapon.mepName = "IS LB 2-X AC";
        weapon.mtfName = "ISLBXAC2";
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 4;
        weapon.shortRange = 9;
        weapon.mediumRange = 18;
        weapon.longRange = 27;
        weapon.tonnage = 6.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 42;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LB 5-X AC";
        weapon.internalName = "ISLBXAC5";
        weapon.mepName = "IS LB 5-X AC";
        weapon.mtfName = "ISLBXAC5";
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 3;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
        weapon.tonnage = 8.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 83;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LB 10-X AC";
        weapon.internalName = "ISLBXAC10";
        weapon.mepName = "IS LB 10-X AC";
        weapon.mtfName = "ISLBXAC10";
        weapon.heat = 2;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 11.0f;
        weapon.criticals = 6;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 148;
        
        return weapon;
    }
    
    public static WeaponType createISLBXAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LB 20-X AC";
        weapon.internalName = "ISLBXAC20";
        weapon.mepName = "IS LB 20-X AC";
        weapon.mtfName = "ISLBXAC20";
        weapon.heat = 6;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 14.0f;
        weapon.criticals = 11;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 237;
        
        return weapon;
    }
    
    public static WeaponType createISGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Gauss Rifle";
        weapon.internalName = "ISGaussRifle";
        weapon.mepName = "IS Gauss Rifle";
        weapon.mtfName = "ISGaussRifle";
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
        weapon.explosive = true;
        weapon.bv = 321;
        
        return weapon;
    }
    
    public static WeaponType createISLightGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Light Gauss Rifle";
        weapon.internalName = "ISLightGaussRifle";
        weapon.mepName = "N/A";
        weapon.mtfName = "ISLightGaussRifle";
        weapon.heat = 1;
        weapon.damage = 8;
        weapon.ammoType = AmmoType.T_GAUSS_LIGHT;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 17;
        weapon.longRange = 25;
        weapon.tonnage = 12.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.explosive = true;
        weapon.bv = 159;
        
        return weapon;
    }
    
    public static WeaponType createISHeavyGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Heavy Gauss Rifle";
        weapon.internalName = "ISHeavyGaussRifle";
        weapon.mepName = weapon.internalName;
        weapon.mtfName = weapon.internalName;
        weapon.heat = 2;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_GAUSS_HEAVY;
        weapon.minimumRange = 4;
        weapon.shortRange = 6;
        weapon.mediumRange = 13;
        weapon.longRange = 20;
        weapon.tonnage = 18.0f;
        weapon.criticals = 11;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.explosive = true;
        weapon.bv = 346;
        
        return weapon;
    }
    
    public static WeaponType createISUltraAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra AC/2";
        weapon.internalName = "ISUltraAC2";
        weapon.mepName = "IS Ultra AC/2";
        weapon.mtfName = "ISUltraAC2";
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 17;
        weapon.longRange = 25;
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
        weapon.internalName = "ISUltraAC5";
        weapon.mepName = "IS Ultra AC/5";
        weapon.mtfName = "ISUltraAC5";
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 2;
        weapon.shortRange = 6;
        weapon.mediumRange = 13;
        weapon.longRange = 20;
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
        weapon.internalName = "ISUltraAC10";
        weapon.mepName = "IS Ultra AC/10";
        weapon.mtfName = "ISUltraAC10";
        weapon.heat = 4;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
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
        weapon.internalName = "ISUltraAC20";
        weapon.mepName = "IS Ultra AC/20";
        weapon.mtfName = "ISUltraAC20";
        weapon.heat = 8;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 0;
        weapon.shortRange = 3;
        weapon.mediumRange = 7;
        weapon.longRange = 10;
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
        weapon.internalName = "ISRotaryAC2";
        weapon.mepName = weapon.internalName;
        weapon.mtfName = weapon.internalName;
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_ROTARY;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
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
        weapon.internalName = "ISRotaryAC5";
        weapon.mepName = weapon.internalName;
        weapon.mtfName = weapon.internalName;
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_ROTARY;
        weapon.minimumRange = 0;
        weapon.shortRange = 5;
        weapon.mediumRange = 10;
        weapon.longRange = 15;
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
        weapon.internalName = "ISStreakSRM2";
        weapon.mepName = "IS Streak SRM-2";
        weapon.mtfName = "ISStreakSRM2";
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
        weapon.mepName = "IS Streak SRM-4";
        weapon.mtfName = "ISStreakSRM4";
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
        weapon.mepName = "IS Streak SRM-6";
        weapon.mtfName = "ISStreakSRM6";
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
        weapon.mepName = "MRM-10"; // NA in MEP
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
        weapon.mepName = "MRM-20"; // NA in MEP
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
        weapon.mepName = "MRM-30"; // NA in MEP
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
        weapon.mepName = "MRM-40"; // NA in MEP
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
    
    public static WeaponType createISAMS() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "AMS";
        weapon.internalName = "ISAntiMissileSystem";
        weapon.mepName = weapon.internalName; // ?
        weapon.mtfName = weapon.internalName;
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
    
    
    // Start of Clan Level2 weapons
    
    public static WeaponType createCLERPPC() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "ER PPC";
        weapon.internalName = "CLERPPC";
        weapon.mepName = "Clan ER PPC";
        weapon.mtfName = "CLERPPC";
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
        weapon.mepName = "Clan ER Large Laser";
        weapon.mtfName = "CLERLargeLaser";
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
        weapon.mepName = "Clan ER Medium Laser";
        weapon.mtfName = "CLERMediumLaser";
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
        weapon.mepName = "Clan ER Small Laser";
        weapon.mtfName = "CLERSmallLaser";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLERMicroLaser";
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
        weapon.mepName = "Clan Flamer";
        weapon.mtfName = "CLFlamer";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLHeavyLargeLaser";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLHeavyMediumLaser";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLHeavySmallLaser";
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
        weapon.mepName = "Clan Machine Gun";
        weapon.mtfName = "CLMG";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLLightMG";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLHeavyMG";
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
        weapon.mepName = "Clan LRM-5";
        weapon.mtfName = "CLLRM5";
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
        weapon.mepName = "Clan LRM-10";
        weapon.mtfName = "CLLRM10";
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
        weapon.mepName = "Clan LRM-15";
        weapon.mtfName = "CLLRM15";
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
        weapon.mepName = "Clan LRM-20";
        weapon.mtfName = "CLLRM20";
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
        weapon.mepName = "Clan SRM-2";
        weapon.mtfName = "CLSRM2";
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
        weapon.mepName = "Clan SRM-4";
        weapon.mtfName = "CLSRM4";
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
        weapon.mepName = "Clan SRM-6";
        weapon.mtfName = "CLSRM6";
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
        weapon.mepName = "Clan Pulse Large Laser";
        weapon.mtfName = "CLLargePulseLaser";
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
        weapon.mepName = "Clan Pulse Med Laser";
        weapon.mtfName = "CLMediumPulseLaser";
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
        weapon.mepName = "Clan Pulse Small Laser";
        weapon.mtfName = "CLSmallPulseLaser";
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
        weapon.mepName = "N/A";
        weapon.mtfName = "CLMicroPulseLaser";
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
        
        weapon.name = "LB 2-X AC";
        weapon.internalName = "CLLBXAC2";
        weapon.mepName = "Clan LB 2-X AC";
        weapon.mtfName = "CLLBXAC2";
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 4;
        weapon.shortRange = 10;
        weapon.mediumRange = 20;
        weapon.longRange = 30;
        weapon.tonnage = 5.0f;
        weapon.criticals = 3;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 47;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC5() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LB 5-X AC";
        weapon.internalName = "CLLBXAC5";
        weapon.mepName = "Clan LB 5-X AC";
        weapon.mtfName = "CLLBXAC5";
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 3;
        weapon.shortRange = 8;
        weapon.mediumRange = 15;
        weapon.longRange = 24;
        weapon.tonnage = 7.0f;
        weapon.criticals = 4;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 93;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC10() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LB 10-X AC";
        weapon.internalName = "CLLBXAC10";
        weapon.mepName = "Clan LB 10-X AC";
        weapon.mtfName = "CLLBXAC10";
        weapon.heat = 2;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
        weapon.tonnage = 10.0f;
        weapon.criticals = 5;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 148;
        
        return weapon;
    }
    
    public static WeaponType createCLLBXAC20() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "LB 20-X AC";
        weapon.internalName = "CLLBXAC20";
        weapon.mepName = "Clan LB 20-X AC";
        weapon.mtfName = "CLLBXAC20";
        weapon.heat = 6;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_LBX;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 12.0f;
        weapon.criticals = 9;
        weapon.flags |= F_DIRECT_FIRE;
        weapon.bv = 237;
        
        return weapon;
    }
    
    public static WeaponType createCLGaussRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Gauss Rifle";
        weapon.internalName = "CLGaussRifle";
        weapon.mepName = "Clan Gauss Rifle";
        weapon.mtfName = "CLGaussRifle";
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
        weapon.explosive = true;
        weapon.bv = 321;
        
        return weapon;
    }
    
    public static WeaponType createCLUltraAC2() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Ultra AC/2";
        weapon.internalName = "CLUltraAC2";
        weapon.mepName = "Clan Ultra AC/2";
        weapon.mtfName = "CLUltraAC2";
        weapon.heat = 1;
        weapon.damage = 2;
        weapon.rackSize = 2;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 2;
        weapon.shortRange = 9;
        weapon.mediumRange = 18;
        weapon.longRange = 27;
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
        weapon.internalName = "CLUltraAC5";
        weapon.mepName = "Clan Ultra AC/5";
        weapon.mtfName = "CLUltraAC5";;
        weapon.heat = 1;
        weapon.damage = 5;
        weapon.rackSize = 5;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 0;
        weapon.shortRange = 7;
        weapon.mediumRange = 14;
        weapon.longRange = 21;
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
        weapon.internalName = "CLUltraAC10";
        weapon.mepName = "Clan Ultra AC/10";
        weapon.mtfName = "CLUltraAC10";
        weapon.heat = 3;
        weapon.damage = 10;
        weapon.rackSize = 10;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 0;
        weapon.shortRange = 6;
        weapon.mediumRange = 12;
        weapon.longRange = 18;
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
        weapon.internalName = "CLUltraAC20";
        weapon.mepName = "Clan Ultra AC/20";
        weapon.mtfName = "CLUltraAC20";
        weapon.heat = 7;
        weapon.damage = 20;
        weapon.rackSize = 20;
        weapon.ammoType = AmmoType.T_AC_ULTRA;
        weapon.minimumRange = 0;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
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
        weapon.internalName = "CLStreakSRM2";
        weapon.mepName = "Clan Streak SRM-2";
        weapon.mtfName = "CLStreakSRM2";
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
        weapon.mepName = "Clan Streak SRM-4";
        weapon.mtfName = "CLStreakSRM4";
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
        weapon.mepName = "Clan Streak SRM-6";
        weapon.mtfName = "CLStreakSRM6";
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
    
    public static WeaponType createInfRifle() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Infantry Rifle";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Infantry Rifle";
        weapon.mtfName = "ISInfantryRifle";
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_AC;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 2; // No long range.
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY;
        weapon.bv = 4; // ???
        
        return weapon;
    }

    public static WeaponType createInfMG() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Infantry MG";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Infantry MG";
        weapon.mtfName = "ISInfantryMG";
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_MG;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY;
        weapon.bv = 4; // ???
        
        return weapon;
    }

    public static WeaponType createInfSRM() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Infantry SRM";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Infantry SRM";
        weapon.mtfName = "ISInfantrySRM";
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_SRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 2;
        weapon.mediumRange = 4;
        weapon.longRange = 6;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY;
        weapon.bv = 4; // ???
        
        return weapon;
    }

    public static WeaponType createInfLRM() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Infantry LRM";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Infantry LRM";
        weapon.mtfName = "ISInfantryLRM";
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_LRM;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 4;
        weapon.mediumRange = 8;
        weapon.longRange = 12;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_DIRECT_FIRE | F_INFANTRY;
        weapon.bv = 4; // ???
        
        return weapon;
    }

    public static WeaponType createInfLaser() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Infantry Laser";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Infantry Laser";
        weapon.mtfName = "ISInfantryLaser";
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 3;
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_LASER | F_DIRECT_FIRE | F_INFANTRY;
        weapon.bv = 4; // ???
        
        return weapon;
    }

    public static WeaponType createInfFlamer() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "Infantry Flamer";
        weapon.internalName = weapon.name;
        weapon.mepName = "IS Infantry Flamer";
        weapon.mtfName = "ISInfantryFlamer";
        weapon.heat = 0;
        weapon.damage = DAMAGE_VARIABLE;
        weapon.ammoType = AmmoType.T_NA;
        weapon.minimumRange = WEAPON_NA;
        weapon.shortRange = 1;
        weapon.mediumRange = 2;
        weapon.longRange = 2; // No long range.
        weapon.tonnage = 0.0f;
        weapon.criticals = 0;
        weapon.flags |= F_FLAMER | F_DIRECT_FIRE | F_INFANTRY;
        weapon.bv = 4; // ???
        
        return weapon;
    }
    
    public static WeaponType createCLAMS() {
        WeaponType weapon = new WeaponType();
        
        weapon.name = "AMS";
        weapon.internalName = "CLAntiMissileSystem";
        weapon.mepName = weapon.internalName; // ?
        weapon.mtfName = weapon.internalName;
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

    public String toString() {
        return "WeaponType: " + name;
    }
}
