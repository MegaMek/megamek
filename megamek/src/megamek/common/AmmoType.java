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
    public static final int     T_AC_LBX            = 19;
    public static final int     T_AC_ULTRA          = 20;
    public static final int     T_GAUSS_LIGHT       = 21;
    
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
        EquipmentType.addType(createISAC2Ammo());
        EquipmentType.addType(createISAC5Ammo());
        EquipmentType.addType(createISAC10Ammo());
        EquipmentType.addType(createISAC20Ammo());
        EquipmentType.addType(createISMGAmmo());
        EquipmentType.addType(createISMGAmmoHalf());
        EquipmentType.addType(createISLRM5Ammo());
        EquipmentType.addType(createISLRM10Ammo());
        EquipmentType.addType(createISLRM15Ammo());
        EquipmentType.addType(createISLRM20Ammo());
        EquipmentType.addType(createISSRM2Ammo());
        EquipmentType.addType(createISSRM4Ammo());
        EquipmentType.addType(createISSRM6Ammo());
        
        // Start of Level2 Ammo
        EquipmentType.addType(createISLB2XAmmo());
        EquipmentType.addType(createISLB5XAmmo());
        EquipmentType.addType(createISLB10XAmmo());
        EquipmentType.addType(createISLB20XAmmo());
        EquipmentType.addType(createISLB2XClusterAmmo());
        EquipmentType.addType(createISLB5XClusterAmmo());
        EquipmentType.addType(createISLB10XClusterAmmo());
        EquipmentType.addType(createISLB20XClusterAmmo());
        EquipmentType.addType(createISUltra2Ammo());
        EquipmentType.addType(createISUltra5Ammo());
        EquipmentType.addType(createISUltra10Ammo());
        EquipmentType.addType(createISUltra20Ammo());
        EquipmentType.addType(createISGaussAmmo());
        EquipmentType.addType(createISLTGaussAmmo());
        EquipmentType.addType(createISStreakSRM2Ammo());
        EquipmentType.addType(createISStreakSRM4Ammo());
        EquipmentType.addType(createISStreakSRM6Ammo());
        EquipmentType.addType(createISMRM10Ammo());
        EquipmentType.addType(createISMRM20Ammo());
        EquipmentType.addType(createISMRM30Ammo());
        EquipmentType.addType(createISMRM40Ammo());
        
        EquipmentType.addType(createCLLB2XAmmo());
        EquipmentType.addType(createCLLB5XAmmo());
        EquipmentType.addType(createCLLB10XAmmo());
        EquipmentType.addType(createCLLB20XAmmo());
        EquipmentType.addType(createCLLB2XClusterAmmo());
        EquipmentType.addType(createCLLB5XClusterAmmo());
        EquipmentType.addType(createCLLB10XClusterAmmo());
        EquipmentType.addType(createCLLB20XClusterAmmo());
        EquipmentType.addType(createCLUltra2Ammo());
        EquipmentType.addType(createCLUltra5Ammo());
        EquipmentType.addType(createCLUltra10Ammo());
        EquipmentType.addType(createCLUltra20Ammo());
        EquipmentType.addType(createCLGaussAmmo());
        EquipmentType.addType(createCLStreakSRM2Ammo());
        EquipmentType.addType(createCLStreakSRM4Ammo());
        EquipmentType.addType(createCLStreakSRM6Ammo());
        EquipmentType.addType(createCLMGAmmo());
        EquipmentType.addType(createCLMGAmmoHalf());
        EquipmentType.addType(createCLLRM5Ammo());
        EquipmentType.addType(createCLLRM10Ammo());
        EquipmentType.addType(createCLLRM15Ammo());
        EquipmentType.addType(createCLLRM20Ammo());
        EquipmentType.addType(createCLSRM2Ammo());
        EquipmentType.addType(createCLSRM4Ammo());
        EquipmentType.addType(createCLSRM6Ammo());
    }
    
    public static AmmoType createISAC2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/2 Ammo";
        ammo.internalName = "IS Ammo AC/2";
        ammo.mepName = "IS Ammo AC/2";
        ammo.mtfName = "ISAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 45;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createISAC5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/5 Ammo";
        ammo.internalName = "IS Ammo AC/5";
        ammo.mepName = "IS Ammo AC/5";
        ammo.mtfName = "ISAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 20;
        ammo.bv = 9;
        
        return ammo;
    }
    
    public static AmmoType createISAC10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/10 Ammo";
        ammo.internalName = "IS Ammo AC/10";
        ammo.mepName = "IS Ammo AC/10";
        ammo.mtfName = "ISAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 10;
        ammo.bv = 15;
        
        return ammo;
    }
    
    public static AmmoType createISAC20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/20 Ammo";
        ammo.internalName = "IS Ammo AC/20";
        ammo.mepName = "IS Ammo AC/20";
        ammo.mtfName = "ISAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 5;
        ammo.bv = 20;
        
        return ammo;
    }
    
    public static AmmoType createISMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Machine Gun Ammo";
        ammo.internalName = "IS Ammo MG - Full";
        ammo.mepName = "IS Ammo MG - Full";
        ammo.mtfName = "ISMG Ammo (200)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.shots = 200;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createISMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Machine Gun Ammo";
        ammo.internalName = "IS Machine Gun Ammo - Half";
        ammo.mepName = "IS Ammo MG - Half";
        ammo.mtfName = "ISMG Ammo (100)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.shots = 100;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        
        return ammo;
    }
    
    public static AmmoType createISLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Ammo";
        ammo.internalName = "IS Ammo LRM-5";
        ammo.mepName = "IS Ammo LRM-5";
        ammo.mtfName = "ISLRM5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.bv = 6;
        
        return ammo;
    }
    
    public static AmmoType createISLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Ammo";
        ammo.internalName = "IS Ammo LRM-10";
        ammo.mepName = "IS Ammo LRM-10";
        ammo.mtfName = "ISLRM10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 11;
        
        return ammo;
    }
    
    public static AmmoType createISLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Ammo";
        ammo.internalName = "IS Ammo LRM-15";
        ammo.mepName = "IS Ammo LRM-15";
        ammo.mtfName = "ISLRM15 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 17;
        
        return ammo;
    }
    
    public static AmmoType createISLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Ammo";
        ammo.internalName = "IS Ammo LRM-20";
        ammo.mepName = "IS Ammo LRM-20";
        ammo.mtfName = "ISLRM20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 23;
        
        return ammo;
    }
    
    public static AmmoType createISSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = "IS Ammo SRM-2";
        ammo.mepName = "IS Ammo SRM-2";
        ammo.mtfName = "ISSRM2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        
        return ammo;
    }
    
    public static AmmoType createISSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = "IS Ammo SRM-4";
        ammo.mepName = "IS Ammo SRM-4";
        ammo.mtfName = "ISSRM4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createISSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 6 Ammo";
        ammo.internalName = "IS Ammo SRM-6";
        ammo.mepName = "IS Ammo SRM-6";
        ammo.mtfName = "ISSRM6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        
        return ammo;
    }
    
    // Start of Level2 Ammo
    
    public static AmmoType createISLB2XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X AC Ammo";
        ammo.internalName = "IS LB 2-X AC Ammo";
        ammo.mepName = "IS Ammo 2-X";
        ammo.mtfName = "ISLBXAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 45;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createISLB5XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X AC Ammo";
        ammo.internalName = "IS LB 5-X AC Ammo";
        ammo.mepName = "IS Ammo 5-X";
        ammo.mtfName = "ISLBXAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 20;
        ammo.bv = 10;
        
        return ammo;
    }
    
    public static AmmoType createISLB10XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X AC Ammo";
        ammo.internalName = "IS LB 10-X AC Ammo";
        ammo.mepName = "IS Ammo 10-X";
        ammo.mtfName = "ISLBXAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        
        return ammo;
    }
    
    public static AmmoType createISLB20XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X AC Ammo";
        ammo.internalName = "IS LB 20-X AC Ammo";
        ammo.mepName = "IS Ammo 20-X";
        ammo.mtfName = "ISLBXAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 5;
        ammo.bv = 27;
        
        return ammo;
    }
    
    public static AmmoType createISLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X Cluster Ammo";
        ammo.internalName = "IS LB 2-X Cluster Ammo";
        ammo.mepName = "IS Ammo 2-X (CL)";
        ammo.mtfName = "ISLBXAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 45;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createISLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X Cluster Ammo";
        ammo.internalName = "IS LB 5-X Cluster Ammo";
        ammo.mepName = "IS Ammo 5-X (CL)";
        ammo.mtfName = "ISLBXAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 20;
        ammo.bv = 10;
        
        return ammo;
    }
    
    public static AmmoType createISLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X Cluster Ammo";
        ammo.internalName = "IS LB 10-X Cluster Ammo";
        ammo.mepName = "IS Ammo 10-X (CL)";
        ammo.mtfName = "ISLBXAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 10;
        ammo.bv = 19;
        
        return ammo;
    }
    
    public static AmmoType createISLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X Cluster Ammo";
        ammo.internalName = "IS LB 20-X Cluster Ammo";
        ammo.mepName = "IS Ammo 20-X (CL)";
        ammo.mtfName = "ISLBXAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 5;
        ammo.bv = 27;
        
        return ammo;
    }
    
    public static AmmoType createISUltra2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/2 Ammo";
        ammo.internalName = "IS Ultra AC/2 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/2";
        ammo.mtfName = "ISUltraAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 7;
        
        return ammo;
    }
    
    public static AmmoType createISUltra5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/5 Ammo";
        ammo.internalName = "IS Ultra AC/5 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/5";
        ammo.mtfName = "ISUltraAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 14;
        
        return ammo;
    }
    
    public static AmmoType createISUltra10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/10 Ammo";
        ammo.internalName = "IS Ultra AC/10 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/10";
        ammo.mtfName = "ISUltraAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 29;
        
        return ammo;
    }
    
    public static AmmoType createISUltra20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/20 Ammo";
        ammo.internalName = "IS Ultra AC/20 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/20";
        ammo.mtfName = "ISUltraAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 32;
        
        return ammo;
    }
    
    public static AmmoType createISGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Gauss Ammo";
        ammo.internalName = "IS Gauss Ammo";
        ammo.mepName = "IS Ammo Gauss";
        ammo.mtfName = "ISGauss Ammo";
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS;
        ammo.shots = 8;
        ammo.bv = 37;
        
        return ammo;
    }
    
    public static AmmoType createISLTGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Light Gauss Ammo";
        ammo.internalName = "IS Light Gauss Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISLightGauss Ammo";
        ammo.damagePerShot = 8;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS_LIGHT;
        ammo.shots = 16;
        ammo.bv = 20;
        
        return ammo;
    }
    
    public static AmmoType createISStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 2 Ammo";
        ammo.internalName = "IS Streak SRM 2 Ammo";
        ammo.mepName = "IS Ammo Streak-2";
        ammo.mtfName = "ISStreakSRM2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 4;
        
        return ammo;
    }
    
    public static AmmoType createISStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 4 Ammo";
        ammo.internalName = "IS Streak SRM 4 Ammo";
        ammo.mepName = "IS Ammo Streak-4";
        ammo.mtfName = "ISStreakSRM4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 7;
        
        return ammo;
    }
    
    public static AmmoType createISStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 6 Ammo";
        ammo.internalName = "IS Streak SRM 6 Ammo";
        ammo.mepName = "IS Ammo Streak-6";
        ammo.mtfName = "ISStreakSRM6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 11;
        
        return ammo;
    }
    
    public static AmmoType createISMRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 10 Ammo";
        ammo.internalName = "IS MRM 10 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 24;
        ammo.bv = 7;
        
        return ammo;
    }
    
    public static AmmoType createISMRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 20 Ammo";
        ammo.internalName = "IS MRM 20 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 12;
        ammo.bv = 14;
        
        return ammo;
    }
    
    public static AmmoType createISMRM30Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 30 Ammo";
        ammo.internalName = "IS MRM 30 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM30 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 8;
        ammo.bv = 21;
        
        return ammo;
    }
    
    public static AmmoType createISMRM40Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 40 Ammo";
        ammo.internalName = "IS MRM 40 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM40 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 6;
        ammo.bv = 28;
        
        return ammo;
    }
    
    public static AmmoType createCLGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Gauss Ammo";
        ammo.internalName = "Clan Gauss Ammo";
        ammo.mepName = "Clan Ammo Gauss";
        ammo.mtfName = "CLGauss Ammo";
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS;
        ammo.shots = 8;
        ammo.bv = 33;
        
        return ammo;
    }
    
    public static AmmoType createCLLB2XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X AC Ammo";
        ammo.internalName = "Clan LB 2-X AC Ammo";
        ammo.mepName = "Clan Ammo 2-X";
        ammo.mtfName = "CLLBXAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 45;
        ammo.bv = 6;
        
        return ammo;
    }
    
    public static AmmoType createCLLB5XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X AC Ammo";
        ammo.internalName = "Clan LB 5-X AC Ammo";
        ammo.mepName = "Clan Ammo 5-X";
        ammo.mtfName = "CLLBXAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 20;
        ammo.bv = 12;
        
        return ammo;
    }
    
    public static AmmoType createCLLB10XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X AC Ammo";
        ammo.internalName = "Clan LB 10-X AC Ammo";
        ammo.mepName = "Clan Ammo 10-X";
        ammo.mtfName = "CLLBXAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        
        return ammo;
    }
    
    public static AmmoType createCLLB20XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X AC Ammo";
        ammo.internalName = "Clan LB 20-X AC Ammo";
        ammo.mepName = "Clan Ammo 20-X";
        ammo.mtfName = "CLLBXAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 5;
        ammo.bv = 33;
        
        return ammo;
    }
    
    public static AmmoType createCLLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X Cluster Ammo";
        ammo.internalName = "Clan LB 2-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 2-X (CL)";
        ammo.mtfName = "CLLBXAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 45;
        ammo.bv = 6;
        
        return ammo;
    }
    
    public static AmmoType createCLLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X Cluster Ammo";
        ammo.internalName = "Clan LB 5-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 5-X (CL)";
        ammo.mtfName = "CLLBXAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 20;
        ammo.bv = 12;
        
        return ammo;
    }
    
    public static AmmoType createCLLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X Cluster Ammo";
        ammo.internalName = "Clan LB 10-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 10-X (CL)";
        ammo.mtfName = "CLLBXAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 10;
        ammo.bv = 19;
        
        return ammo;
    }
    
    public static AmmoType createCLLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X Cluster Ammo";
        ammo.internalName = "Clan LB 20-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 20-X (CL)";
        ammo.mtfName = "CLLBXAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.flags |= F_CLUSTER;
        ammo.shots = 5;
        ammo.bv = 33;
        
        return ammo;
    }
    
    public static AmmoType createCHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Heavy Machine Gun Ammo";
        ammo.internalName = "Clan Heavy Machine Gun Ammo - Full";
        ammo.mepName = "N/A";
        ammo.mtfName = "CLHeavyMG Ammo (100)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.shots = 100;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createCLMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Machine Gun Ammo";
        ammo.internalName = "Clan Machine Gun Ammo - Full";
        ammo.mepName = "Clan Ammo MG - Full";
        ammo.mtfName = "CLMG Ammo (200)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.shots = 200;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createCLMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Machine Gun Ammo";
        ammo.internalName = "Clan Machine Gun Ammo - Half";
        ammo.mepName = "Clan Ammo MG - Half";
        ammo.mtfName = "CLMG Ammo (100)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createCLLightMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Light Machine Gun Ammo";
        ammo.internalName = "Clan Light Machine Gun Ammo - Full";
        ammo.mepName = "N/A";
        ammo.mtfName = "CLLightMG Ammo (200)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.shots = 200;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/2 Ammo";
        ammo.internalName = "Clan Ultra AC/2 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/2";
        ammo.mtfName = "CLUltraAC2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 6;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/5 Ammo";
        ammo.internalName = "Clan Ultra AC/5 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/5";
        ammo.mtfName = "CLUltraAC5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 15;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/10 Ammo";
        ammo.internalName = "Clan Ultra AC/10 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/10";
        ammo.mtfName = "CLUltraAC10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 26;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/20 Ammo";
        ammo.internalName = "Clan Ultra AC/20 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/20";
        ammo.mtfName = "CLUltraAC20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 35;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Ammo";
        ammo.internalName = "Clan Ammo LRM-5";
        ammo.mepName = "Clan Ammo LRM-5";
        ammo.mtfName = "CLLRM5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.bv = 7;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Ammo";
        ammo.internalName = "Clan Ammo LRM-10";
        ammo.mepName = "Clan Ammo LRM-10";
        ammo.mtfName = "CLLRM10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 14;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Ammo";
        ammo.internalName = "Clan Ammo LRM-15";
        ammo.mepName = "Clan Ammo LRM-15";
        ammo.mtfName = "CLLRM15 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 21;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Ammo";
        ammo.internalName = "Clan Ammo LRM-20";
        ammo.mepName = "Clan Ammo LRM-20";
        ammo.mtfName = "CLLRM20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 27;
        
        return ammo;
    }
    
    public static AmmoType createCLSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = "Clan Ammo SRM-2";
        ammo.mepName = "Clan Ammo SRM-2";
        ammo.mtfName = "CLSRM2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        
        return ammo;
    }
    
    public static AmmoType createCLSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = "Clan Ammo SRM-4";
        ammo.mepName = "Clan Ammo SRM-4";
        ammo.mtfName = "CLSRM4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createCLSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 6 Ammo";
        ammo.internalName = "Clan Ammo SRM-6";
        ammo.mepName = "Clan Ammo SRM-6";
        ammo.mtfName = "CLSRM6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        
        return ammo;
    }

    public static AmmoType createCLStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 2 Ammo";
        ammo.internalName = "Clan Streak SRM 2 Ammo";
        ammo.mepName = "Clan Ammo Streak-2";
        ammo.mtfName = "CLStreakSRM2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createCLStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 4 Ammo";
        ammo.internalName = "Clan Streak SRM 4 Ammo";
        ammo.mepName = "Clan Ammo Streak-4";
        ammo.mtfName = "CLStreakSRM4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 10;
        
        return ammo;
    }
    
    public static AmmoType createCLStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 6 Ammo";
        ammo.internalName = "Clan Streak SRM 6 Ammo";
        ammo.mepName = "Clan Ammo Streak-6";
        ammo.mtfName = "CLStreakSRM6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 15;
        
        return ammo;
    }
    
    public String toString() {
        return "Ammo: " + name;
    }
}
