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

import java.io.Serializable;

public class AmmoType extends EquipmentType {
    // ammo types
    public static final int     T_NA                = -1;
    public static final int     T_AC                = 1;
    public static final int     T_VEHICLE_FLAMER    = 2;
    public static final int     T_MG                = 3;
    public static final int     T_MG_HEAVY          = 4;
    public static final int     T_MG_LIGHT          = 5;
    public static final int     T_GAUSS             = 6;
    public static final int     T_LRM               = 7;
    public static final int     T_LRM_TORPEDO       = 8;
    public static final int     T_SRM               = 9;
    public static final int     T_SRM_TORPEDO       = 10;
    public static final int     T_SRM_STREAK        = 11;
    public static final int     T_MRM               = 12;
    public static final int     T_NARC              = 13;
    public static final int     T_AMS               = 14;
    public static final int     T_ARROW_IV          = 15;
    public static final int     T_LONG_TOM          = 16;
    public static final int     T_SNIPER            = 17;
    public static final int     T_THUMPER           = 18;
    
    // ammo flags
    public static final int     F_CLUSTER           = 0x0001; // for lbx
    public static final int     F_FLARE             = 0x0002;
    public static final int     F_FRAGMENTATION     = 0x0004;
    public static final int     F_INCENDIARY        = 0x0008;
    public static final int     F_SEMIGUIDED        = 0x0010;
    public static final int     F_SWARM             = 0x0020;
    public static final int     F_SWARM_I           = 0x0040;
    public static final int     F_THUNDER           = 0x0080;
    public static final int     F_INFERNO           = 0x0100;
    public static final int     F_EXPLOSIVE         = 0x0200; // for narc
    
    private int damagePerShot;
    private int rackSize;
    private int ammoType;
    private int shots;
    
    public AmmoType() {
        criticals = 1;
        tonnage = 1.0f;
        explosive = true;
    }
    
    public int getAmmoType() {
        return ammoType;
    }
    
    public int getDamagePerShot() {
        return damagePerShot;
    }
    
    public int getRackSize() {
        return rackSize;
    }
    
    public int getShots() {
        return shots;
    }
    
    public static void initializeTypes() {
        // all level 1 ammo
        EquipmentType.addType(createAC2Ammo());
        EquipmentType.addType(createAC5Ammo());
        EquipmentType.addType(createAC10Ammo());
        EquipmentType.addType(createAC20Ammo());
        EquipmentType.addType(createMGAmmo());
        EquipmentType.addType(createMGAmmoHalf());
        EquipmentType.addType(createLRM5Ammo());
        EquipmentType.addType(createLRM10Ammo());
        EquipmentType.addType(createLRM15Ammo());
        EquipmentType.addType(createLRM20Ammo());
        EquipmentType.addType(createSRM2Ammo());
        EquipmentType.addType(createSRM4Ammo());
        EquipmentType.addType(createSRM6Ammo());
    }
    
    public static AmmoType createAC2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/2 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo AC/2";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 45;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createAC5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/5 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo AC/5";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 20;
        ammo.bv = 9;
        
        return ammo;
    }
    
    public static AmmoType createAC10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/10 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo AC/10";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 10;
        ammo.bv = 15;
        
        return ammo;
    }
    
    public static AmmoType createAC20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/20 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo AC/20";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 5;
        ammo.bv = 20;
        
        return ammo;
    }
    
    public static AmmoType createMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Machine Gun Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo MG - Full";
        ammo.mtfName = "MG Ammo (200)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.shots = 200;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Machine Gun Ammo";
        ammo.internalName = "Machine Gun Ammo - Half";
        ammo.mepName = "Ammo MG - Half";
        ammo.mtfName = "MG Ammo (100)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.shots = 100;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        
        return ammo;
    }
    
    public static AmmoType createLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo LRM-5";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.bv = 6;
        
        return ammo;
    }
    
    public static AmmoType createLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo LRM-10";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 11;
        
        return ammo;
    }
    
    public static AmmoType createLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo LRM-15";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 17;
        
        return ammo;
    }
    
    public static AmmoType createLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo LRM-20";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 23;
        
        return ammo;
    }
    
    public static AmmoType createSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo SRM-2";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        
        return ammo;
    }
    
    public static AmmoType createSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo SRM-4";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 6 Ammo";
        ammo.internalName = ammo.name;
        ammo.mepName = "Ammo SRM-6";
        ammo.mtfName = ammo.name;
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        
        return ammo;
    }
    
    public String toString() {
        return "Ammo: " + name;
    }
}
