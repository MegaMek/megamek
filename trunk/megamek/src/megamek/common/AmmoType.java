/*s
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
    public static final int    TYPE_NA = 0;
    public static final int    TYPE_MG = 1;
    public static final int    TYPE_AC = 2;
    public static final int    TYPE_LRM = 3;
    public static final int    TYPE_SRM = 4;
    
    private int damagePerShot;
    private int rackSize;
    private int ammoType;
    private int shots;
    
    public AmmoType() {
        criticals = 1;
        tonnage = 1.0f;
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
        ammo.ammoType = AmmoType.TYPE_AC;
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
        ammo.ammoType = AmmoType.TYPE_AC;
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
        ammo.ammoType = AmmoType.TYPE_AC;
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
        ammo.ammoType = AmmoType.TYPE_AC;
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
        ammo.ammoType = AmmoType.TYPE_MG;
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
        ammo.ammoType = AmmoType.TYPE_MG;
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
        ammo.ammoType = AmmoType.TYPE_LRM;
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
        ammo.ammoType = AmmoType.TYPE_LRM;
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
        ammo.ammoType = AmmoType.TYPE_LRM;
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
        ammo.ammoType = AmmoType.TYPE_LRM;
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
        ammo.ammoType = AmmoType.TYPE_SRM;
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
        ammo.ammoType = AmmoType.TYPE_SRM;
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
        ammo.ammoType = AmmoType.TYPE_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        
        return ammo;
    }
    
    public String toString() {
        return "Ammo: " + name;
    }
}
