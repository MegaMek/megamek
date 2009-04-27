/*
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

public class BombType extends AmmoType {

    public static final int B_HE      = 0;
    public static final int B_CLUSTER = 1;
    public static final int B_LG      = 2;
    public static final int B_RL      = 3;
    public static final int B_TAG     = 4;
    public static final int B_AAA     = 5;
    public static final int B_AS      = 6;
    public static final int B_ASEW    = 7;
    public static final int B_ARROW   = 8;
    public static final int B_HOMING  = 9;
    public static final int B_INFERNO = 10;
    public static final int B_LAA     = 11;
    public static final int B_THUNDER = 12;
    public static final int B_TORPEDO = 13;
    public static final int B_ALAMO   = 14;
    public static final int B_NUM     = 15;

    public static final String[] bombNames = {"HE Bomb","Cluster Bomb","Laser-guided Bomb",
                                              "Rocket", "TAG", "AAA Missile", "AS Missile",
                                              "ASEW Missile", "Arrow IV Missile",
                                              "Arrow IV Homing Missile", "Inferno Bomb",
                                              "LAA Missile", "Thunder Bomb", "Torpedo Bomb",
                                              "Alamo Missile"};

    public static final String[] bombInternalNames = {"HEBomb","ClusterBomb","LGBomb",
                                                      "RocketBomb", "TAGBomb", "AAAMissile Ammo",
                                                      "ASMissile Ammo",
                                                      "ASEWMissile Ammo", "ArrowIVMissile Ammo",
                                                      "ArrowIVHomingMissile Ammo", "InfernoBomb",
                                                      "LAAMissile Ammo", "ThunderBomb", "TorpedoBomb",
                                                      "AlamoMissile Ammo"};

    public static final String[] bombWeaponNames = {null, null, null, "BombRL", "BombTAG", "AAAMissile",
                                                    "ASMissile", "ASEWMissile", "BombArrowIV", "BombArrowIV",
                                                    null,"LAAMissile",null,null,"AlamoMissile"};


    public static final int[] bombCosts = {1,1,1,1,1,5,6,6,5,5,1,1,1,1,10};
    private int bombType;

    public static String getBombName(int type) {
        if(type >= B_NUM || type < 0) {
            return "Unknown bomb type";
        }
        return bombNames[type];
    }

    public static String getBombWeaponName(int type) {
        if(type >= B_NUM || type < 0) {
            return "Unknown bomb weapon";
        }
        return bombWeaponNames[type];
    }

    public static String getBombInternalName(int type) {
        if(type >= B_NUM || type < 0) {
            return "Unknown bomb type";
        }
        return bombInternalNames[type];
    }

    public static int getBombCost(int type) {
        if(type >= B_NUM || type < 0) {
            return 0;
        }
        return bombCosts[type];
    }

    public int getBombType() {
        return bombType;
    }

    public static void initializeTypes() {
        EquipmentType.addType(BombType.createHighExplosiveBomb());
        EquipmentType.addType(BombType.createClusterBomb());
        EquipmentType.addType(BombType.createLaserGuidedBomb());
        EquipmentType.addType(BombType.createRocketBomb());
        EquipmentType.addType(BombType.createTAGBomb());
        EquipmentType.addType(BombType.createAAAMissileBomb());
        EquipmentType.addType(BombType.createASMissileBomb());
        EquipmentType.addType(BombType.createASEWMissileBomb());
        EquipmentType.addType(BombType.createArrowIVBomb());
        EquipmentType.addType(BombType.createArrowIVHomingBomb());
        EquipmentType.addType(BombType.createInfernoBomb());
        EquipmentType.addType(BombType.createLAAMissileBomb());
        EquipmentType.addType(BombType.createThunderBomb());
        EquipmentType.addType(BombType.createTorpedoBomb());
        EquipmentType.addType(BombType.createAlamoBomb());
    }

    private static BombType createHighExplosiveBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_TW_ALL;
        bomb.name = "HE Bomb";
        bomb.shortName = "HEBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_HE));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_HE;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createClusterBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_TW_ALL;
        bomb.name = "Cluster Bomb";
        bomb.shortName = "ClusterBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_CLUSTER));
        bomb.damagePerShot = 5;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_CLUSTER;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createLaserGuidedBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_TW_ALL;
        bomb.name = "Laser-Guided Bomb";
        bomb.shortName = "LGBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_LG));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_LG;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createTAGBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_TW_ALL;
        bomb.name = "TAGBomb";
        bomb.shortName = "TAGBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_TAG));
        bomb.damagePerShot = 0;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_TAG;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createRocketBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_TW_NON_BOX;
        bomb.name = "RL 10 Ammo (bomb)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_RL));
        bomb.damagePerShot = 1;
        bomb.rackSize = 10;
        bomb.ammoType = AmmoType.T_RL_BOMB;
        bomb.bombType = BombType.B_RL;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createAAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "AAA Missile Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_AAA));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AAA_MISSILE;
        bomb.bombType = BombType.B_AAA;;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createASMissileBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        bomb.name = "Anti-Ship Missile Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_AS));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AS_MISSILE;
        bomb.bombType = BombType.B_AS;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createASEWMissileBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "Anti-Ship (EW) Missile Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ASEW));
        bomb.damagePerShot = 0;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ASEW_MISSILE;
        bomb.bombType = BombType.B_ASEW;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createArrowIVBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "Arrow IV Ammo (Bomb)";
        bomb.shortName = "Arrow IV (Bomb)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ARROW));
        bomb.damagePerShot=1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_ARROW;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createArrowIVHomingBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "Arrow IV Homing Ammo (Bomb)";
        bomb.shortName = "Arrow IV Homing (Bomb)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_HOMING));
        bomb.damagePerShot=1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_HOMING;
        bomb.munitionType = AmmoType.M_HOMING;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createInfernoBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "Inferno Bomb";
        bomb.shortName = "InfernoBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_INFERNO));
        bomb.damagePerShot = 5;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_INFERNO;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createLAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "LAA Missile Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_LAA));
        bomb.damagePerShot = 6;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_LAA_MISSILE;
        bomb.bombType = BombType.B_LAA;;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createThunderBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "Thunder Bomb";
        bomb.shortName = "ThunderBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_THUNDER));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_THUNDER;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createTorpedoBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_ADVANCED;
        bomb.name = "Torpedo Bomb";
        bomb.shortName = "TorpedoBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_TORPEDO));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_TORPEDO;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;

        return bomb;
    }

    private static BombType createAlamoBomb() {
        BombType bomb = new BombType();

        bomb.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        bomb.name = "Alamo Missile Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ALAMO));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ALAMO;
        bomb.bombType = BombType.B_ALAMO;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;
        bomb.flags |= F_NUCLEAR;
        bomb.capital = true;

        return bomb;
    }

}