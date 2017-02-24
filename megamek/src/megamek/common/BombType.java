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

    public static final int B_NONE    = -1;
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
//    public static final int B_FAE     = 16;  TODO - Implement Fuel Air Explosives

    public static final String[] bombNames = {"HE Bomb","Cluster Bomb","Laser-guided Bomb",
                                              "Rocket", "TAG", "AAA Missile", "AS Missile",
                                              "ASEW Missile", "Arrow IV Missile",
                                              "Arrow IV Homing Missile", "Inferno Bomb",
                                              "LAA Missile", "Thunder Bomb", "Torpedo Bomb",
                                              "Alamo Missile"};
    
    public static final String[] bombInternalNames = {"HEBomb","ClusterBomb","LGBomb",
                                                      "RL 10 Ammo (bomb)", "TAGBomb", "AAAMissile Ammo",
                                                      "ASMissile Ammo",
                                                      "ASEWMissile Ammo", "ArrowIVMissile Ammo",
                                                      "ArrowIVHomingMissile Ammo", "InfernoBomb",
                                                      "LAAMissile Ammo", "ThunderBomb", "TorpedoBomb",
                                                      "AlamoMissile Ammo"};

    public static final String[] bombWeaponNames = {null, null, null, "BombRL", "BombTAG", "AAAMissile",
                                                    "ASMissile", "ASEWMissile", "BombArrowIV", "BombArrowIV",
                                                    null,"LAAMissile",null,null,"AlamoMissile"};


    public static final int[] bombCosts = {1,1,1,1,1,5,6,6,5,5,1,2,1,1,10};
    private int bombType;

    public static String getBombName(int type) {
        if((type >= B_NUM) || (type < 0)) {
            return "Unknown bomb type";
        }
        return bombNames[type];
    }
    
    public static int getBombTypeFromName(String name) {
        for (int i = 0; i < B_NUM; i++) {
            if (bombNames[i].equals(name)) {
                return i;
            }
        }
        return B_NONE;
    }
    
    public static int getBombTypeFromInternalName(String name) {
        for (int i = 0; i < B_NUM; i++) {
            if (bombInternalNames[i].equals(name)) {
                return i;
            }
        }
        return B_NONE;
    }

    public static String getBombWeaponName(int type) {
        if((type >= B_NUM) || (type < 0)) {
            return "Unknown bomb weapon";
        }
        return bombWeaponNames[type];
    }

    public static String getBombInternalName(int type) {
        if((type >= B_NUM) || (type < 0)) {
            return "Unknown bomb type";
        }
        return bombInternalNames[type];
    }

    public static int getBombCost(int type) {
        if((type >= B_NUM) || (type < 0)) {
            return 0;
        }
        return bombCosts[type];
    }

    public static boolean canGroundBomb(int type) {
        switch(type) {
        case B_HE:
        case B_CLUSTER:
        case B_LG:
        case B_INFERNO:
        case B_THUNDER:
        case B_TORPEDO:
            return true;
        default:
            return false;
        }
    }

    public static boolean canSpaceBomb(int type) {
        switch(type) {
        case B_HE:
        case B_CLUSTER:
        case B_LG:
        case B_ARROW:
        case B_HOMING:
            return true;
        default:
            return false;
        }
    }

    public int getBombType() {
        return bombType;
    }

    public static void initializeTypes() {
        EquipmentType.addType(BombType.createHighExplosiveBomb());
        EquipmentType.addType(BombType.createClusterBomb());
        EquipmentType.addType(BombType.createISLaserGuidedBomb());
        EquipmentType.addType(BombType.createCLLaserGuidedBomb());
        EquipmentType.addType(BombType.createRocketBomb());
        EquipmentType.addType(BombType.createISTAGBomb());
        EquipmentType.addType(BombType.createCLTAGBomb());
        EquipmentType.addType(BombType.createISAAAMissileBomb());
        EquipmentType.addType(BombType.createCLAAAMissileBomb());
        EquipmentType.addType(BombType.createISASMissileBomb());
        EquipmentType.addType(BombType.createCLASMissileBomb());
        EquipmentType.addType(BombType.createISASEWMissileBomb());
        EquipmentType.addType(BombType.createCLASEWMissileBomb());
        EquipmentType.addType(BombType.createISArrowIVBomb());
        EquipmentType.addType(BombType.createCLArrowIVBomb());
        EquipmentType.addType(BombType.createISArrowIVHomingBomb());
        EquipmentType.addType(BombType.createCLArrowIVHomingBomb());
        EquipmentType.addType(BombType.createInfernoBomb());
        EquipmentType.addType(BombType.createISLAAMissileBomb());
        EquipmentType.addType(BombType.createCLLAAMissileBomb());
        EquipmentType.addType(BombType.createThunderBomb());
        EquipmentType.addType(BombType.createTorpedoBomb());
        EquipmentType.addType(BombType.createAlamoBomb());
    }
    
    public static BombType createBombByType(int bType, boolean isClan)    {
        switch (bType){
            case B_HE:
                return createHighExplosiveBomb();
            case B_CLUSTER:
                return createClusterBomb();
            case B_LG:
            	if(isClan){
            		return createCLLaserGuidedBomb(); 
            	}	else   {
        			return createISLaserGuidedBomb();           		
            	}
            case B_RL:
                return createRocketBomb();
            case B_TAG:
            	if(isClan){
            		return createCLTAGBomb(); 
            	}	else   {
        			return createISTAGBomb();           		
            	}
            case B_AAA:
            	if(isClan){
            		return createCLLaserGuidedBomb(); 
            	}	else   {
        			return createISLaserGuidedBomb();           		
            	}
            case B_AS:
            	if(isClan){
            		return createCLASMissileBomb(); 
            	}	else   {
        			return createISASMissileBomb();           		
            	}
            case B_ASEW:
            	if(isClan){
            		return createCLASEWMissileBomb(); 
            	}	else   {
        			return createISASEWMissileBomb();           		
            	}
            case B_ARROW:
            	if(isClan){
            		return createCLArrowIVBomb(); 
            	}	else   {
        			return createISArrowIVBomb();           		
            	}
            case B_HOMING:
            	if(isClan){
            		return createCLArrowIVHomingBomb(); 
            	}	else   {
        			return createISArrowIVHomingBomb();           		
            	}
            case B_INFERNO:
                return createInfernoBomb();
            case B_LAA:
            	if(isClan){
            		return createCLLAAMissileBomb(); 
            	}	else   {
        			return createISLAAMissileBomb();           		
            	}
            case B_THUNDER:
                return createThunderBomb();
            case B_TORPEDO:
                return createTorpedoBomb();
            case B_ALAMO:
                return createAlamoBomb();
            default:
                return null;
        }
    }

    private static BombType createHighExplosiveBomb() {
        BombType bomb = new BombType();

        bomb.name = "HE Bomb";
        bomb.shortName = "HEBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_HE));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_HE;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 12;
        bomb.cost = 5000;
        bomb.introDate = 1950;
        bomb.techLevel.put(1950, TechConstants.T_ALLOWED_ALL);
        bomb.availRating = new int[] { RATING_C ,RATING_C ,RATING_C ,RATING_C};
        bomb.techRating = RATING_B;
        bomb.rulesRefs = "246, TW";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        bomb.techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        bomb.techProgression.setTechRating(RATING_B);
        bomb.techProgression.setAvailability( new int[] { RATING_C, RATING_C, RATING_C, RATING_C });

        return bomb;
    }

    private static BombType createClusterBomb() {
        BombType bomb = new BombType();

        bomb.name = "Cluster Bomb";
        bomb.shortName = "ClusterBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_CLUSTER));
        bomb.damagePerShot = 5;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_CLUSTER;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 13;
        bomb.cost = 8000;
        bomb.introDate = 1950;
        bomb.techLevel.put(1950, TechConstants.T_ALLOWED_ALL);
        bomb.availRating = new int[] { RATING_D ,RATING_D ,RATING_D ,RATING_D};
        bomb.techRating = RATING_B;
        bomb.rulesRefs = "246, TW";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        bomb.techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        bomb.techProgression.setTechRating(RATING_B);
        bomb.techProgression.setAvailability( new int[] { RATING_D, RATING_D, RATING_D, RATING_D });

        return bomb;
    }

    private static BombType createISLaserGuidedBomb() {
        BombType bomb = new BombType();

        bomb.name = "Laser-Guided Bomb";
        bomb.shortName = "ISLGBomb";
        bomb.setInternalName("IS " + BombType.getBombInternalName(BombType.B_LG));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_LG;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 20;
        bomb.cost = 10000;
        bomb.introDate = 2200;
        bomb.extinctDate = 2800;
        bomb.reintroDate = 3060;
        bomb.techLevel.put(2200, TechConstants.T_IS_ADVANCED);
        bomb.techLevel.put(3065, TechConstants.T_IS_TW_NON_BOX);
        bomb.availRating = new int[] { RATING_E ,RATING_F ,RATING_E ,RATING_D};
        bomb.techRating = RATING_C;
        bomb.rulesRefs = "247, TW";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(DATE_NONE, 2200, 3065, 2800, 3060);
        bomb.techProgression.setTechRating(RATING_C);
        bomb.techProgression.setAvailability( new int[] { RATING_E, RATING_F, RATING_E, RATING_D });

        return bomb;
    }
    
    private static BombType createCLLaserGuidedBomb() {
        BombType bomb = new BombType();

        bomb.name = "Laser-Guided Bomb";
        bomb.shortName = "ClanLGBomb";
        bomb.setInternalName("Clan " + BombType.getBombInternalName(BombType.B_LG));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_LG;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 20;
        bomb.cost = 10000;
        bomb.introDate = 2807;
        bomb.techLevel.put(2807, TechConstants.T_CLAN_ADVANCED);
        bomb.techLevel.put(3065, TechConstants.T_CLAN_TW);
        bomb.availRating = new int[] { RATING_X,RATING_F ,RATING_E ,RATING_D};
        bomb.techRating = RATING_C;
        bomb.rulesRefs = "247, TW";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 2807, 3065);
        bomb.techProgression.setTechRating(RATING_C);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_D });

        return bomb;
    }

    private static BombType createISTAGBomb() {
        BombType bomb = new BombType();

        bomb.name = "TAG Pod";
        bomb.shortName = "ISTAGPod";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_TAG));
        bomb.addLookupName("ISTAGBomb");
        bomb.damagePerShot = 0;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_TAG;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 50000;
        bomb.introDate = 2595;
        bomb.extinctDate = 2835;
        bomb.reintroDate = 3035;
        bomb.techLevel.put(2595, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(2605, TechConstants.T_IS_ADVANCED);
        bomb.techLevel.put(2645, TechConstants.T_IS_TW_NON_BOX);
        bomb.availRating = new int[] { RATING_E ,RATING_F ,RATING_D ,RATING_D};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "238, TM";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(2595, 2605, 2645, 2835, 3035);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_E, RATING_F, RATING_D, RATING_D });

        return bomb;
    }
    
    private static BombType createCLTAGBomb() {
        BombType bomb = new BombType();

        bomb.name = "TAG Pod";
        bomb.shortName = "ClanTAGPod";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_TAG));
        bomb.addLookupName("CLTAGBomb");
        bomb.damagePerShot = 0;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_TAG;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 50000;
        bomb.introDate = 2807;
        bomb.techLevel.put(2807, TechConstants.T_CLAN_TW);
        bomb.availRating = new int[] { RATING_X,RATING_F ,RATING_D ,RATING_D};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "238, TM";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(2595, 2605, 2645);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_F, RATING_D, RATING_D });
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, DATE_NONE, 2807);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_F, RATING_D, RATING_D });

        return bomb;
    }

    private static BombType createRocketBomb() {
        BombType bomb = new BombType();

        bomb.name = "Rocket Launcher Pod";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_RL));
        bomb.addLookupName("RL 10 (Bomb)");
        bomb.damagePerShot = 1;
        bomb.rackSize = 10;
        bomb.ammoType = AmmoType.T_RL_BOMB;
        bomb.bombType = BombType.B_RL;
        bomb.shots = 1;
        bomb.bv = 18;
        bomb.cost = 15000;
        bomb.introDate = 3055;
        bomb.techLevel.put(3055, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(3064, TechConstants.T_IS_ADVANCED);
        bomb.techLevel.put(3067, TechConstants.T_IS_TW_NON_BOX);
        bomb.availRating = new int[] { RATING_X ,RATING_X ,RATING_B ,RATING_B};
        bomb.techRating = RATING_B;
        bomb.rulesRefs = "229, TM";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3055, 3064, 3067);
        bomb.techProgression.setTechRating(RATING_B);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_B, RATING_B });
        
        return bomb;
    }

    private static BombType createISAAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "AAA Missile Ammo";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_AAA));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AAA_MISSILE;
        bomb.bombType = BombType.B_AAA;;
        bomb.shots = 1;
        bomb.bv = 57;
        bomb.cost = 9000;
        bomb.introDate = 3064;
        bomb.techLevel.put(3064, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(3072, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "357, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3064, 3072, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });
     
        return bomb;
    }
    
    private static BombType createCLAAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "AAA Missile Ammo";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_AAA));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AAA_MISSILE;
        bomb.bombType = BombType.B_AAA;;
        bomb.shots = 1;
        bomb.bv = 57;
        bomb.cost = 9000;
        bomb.introDate = 3072;
        bomb.techLevel.put(3072, TechConstants.T_CLAN_ADVANCED);
        bomb.availRating = new int[] { RATING_X,RATING_X ,RATING_F ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "357, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 3072, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });
     
        return bomb;
    }


    private static BombType createISASMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship Missile Ammo";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_AS));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AS_MISSILE;
        bomb.bombType = BombType.B_AS;
        bomb.shots = 1;
        bomb.bv = 114;
        bomb.cost = 0;
        bomb.introDate = 3071;
        bomb.techLevel.put(3071, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(3070, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_E};
        bomb.techRating = RATING_D;
        bomb.rulesRefs = "358, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3071, DATE_NONE, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_D);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });

        return bomb;
    }
    
    private static BombType createCLASMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship Missile Ammo";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_AS));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AS_MISSILE;
        bomb.bombType = BombType.B_AS;
        bomb.shots = 1;
        bomb.bv = 114;
        bomb.cost = 0;
        bomb.introDate = 3076;
        bomb.techLevel.put(3076, TechConstants.T_CLAN_ADVANCED);
        bomb.availRating = new int[] { RATING_X,RATING_X ,RATING_F ,RATING_E};
        bomb.techRating = RATING_D;
        bomb.rulesRefs = "358, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 3076, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_D);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_E });

        return bomb;
    }
    
    private static BombType createISASEWMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship (EW) Missile Ammo";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_ASEW));
        bomb.damagePerShot = 0;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ASEW_MISSILE;
        bomb.bombType = BombType.B_ASEW;
        bomb.shots = 1;
        bomb.bv = 75;
        bomb.cost = 0;
        bomb.introDate = 3067;
        bomb.techLevel.put(3067, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(3073, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "358, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3067, 3073, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_E });

        return bomb;
    }
    
    private static BombType createCLASEWMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship (EW) Missile Ammo";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_ASEW));
        bomb.damagePerShot = 0;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ASEW_MISSILE;
        bomb.bombType = BombType.B_ASEW;
        bomb.shots = 1;
        bomb.bv = 75;
        bomb.cost = 0;
        bomb.introDate = 3073;
        bomb.techLevel.put(3073, TechConstants.T_CLAN_UNOFFICIAL);
        bomb.availRating = new int[] { RATING_X, RATING_X, RATING_F, RATING_F };
        bomb.techRating = RATING_F;
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 3073);
        bomb.techProgression.setUnofficial(true);
        bomb.techProgression.setTechRating(RATING_F);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_F });

        return bomb;
    }

    private static BombType createISArrowIVBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Non-Homing Missile (Air-Launched Version)";
        bomb.shortName = "IS Arrow IV (Air-Launched Version)";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_ARROW));
        bomb.damagePerShot=1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_ARROW;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 34;
        bomb.cost = 0;
        bomb.introDate = 2617;
        bomb.extinctDate = 2855;
        bomb.reintroDate = 3046;
        bomb.techLevel.put(2617, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(2623, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_E ,RATING_F ,RATING_E ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "359, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(2617, 2623, DATE_NONE, 2855, 3046);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_E, RATING_F, RATING_E, RATING_E });
       
        return bomb;
    }
    
    private static BombType createCLArrowIVBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Non-Homing Missile (Air-Launched Version)";
        bomb.shortName = "Clan Arrow IV (Air-Launched Version)";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_ARROW));
        bomb.damagePerShot=1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_ARROW;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 34;
        bomb.cost = 0;
        bomb.introDate = 2807;
        bomb.techLevel.put(2807, TechConstants.T_CLAN_ADVANCED);
        bomb.availRating = new int[] { RATING_X,RATING_F ,RATING_E ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "359, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 2807, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_E });
       
        return bomb;
    }

    private static BombType createISArrowIVHomingBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Homing Missile (Air-Launched Version)";
        bomb.shortName = "IS Arrow IV AL-Homing)";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_HOMING));
        bomb.damagePerShot=1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_HOMING;
        bomb.munitionType = AmmoType.M_HOMING;
        // Allow Homing munitions to instantly switch between modes
        bomb.instantModeSwitch = true;
        bomb.setModes(new String[] { "Homing", "Non-Homing" });
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;
        bomb.introDate = 2590;
        bomb.extinctDate = 2840;
        bomb.reintroDate = 3047;
        bomb.techLevel.put(2590, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(2600, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_E ,RATING_F ,RATING_E ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "358, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(2590, 2600, DATE_NONE, 2840, 3047);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_E, RATING_F, RATING_E, RATING_E });

        return bomb;
    }
    
    private static BombType createCLArrowIVHomingBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Homing Missile (Air-Launched Version)";
        bomb.shortName = "Clan Arrow IV AL-Homing";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_HOMING));
        bomb.damagePerShot=1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_HOMING;
        bomb.munitionType = AmmoType.M_HOMING;
        // Allow Homing munitions to instantly switch between modes
        bomb.instantModeSwitch = true;
        bomb.setModes(new String[] { "Homing", "Non-Homing" });
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;
        bomb.introDate = 2807;
        bomb.techLevel.put(2807, TechConstants.T_CLAN_ADVANCED);
        bomb.availRating = new int[] { RATING_X,RATING_F ,RATING_E ,RATING_E};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "358, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 2807, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_F, RATING_E, RATING_E });

        return bomb;
    }

    private static BombType createInfernoBomb() {
        BombType bomb = new BombType();

        bomb.name = "Inferno Bomb";
        bomb.shortName = "InfernoBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_INFERNO));
        bomb.damagePerShot = 5;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_INFERNO;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 16;
        bomb.cost = 0;
        bomb.introDate = 1950;
        bomb.techLevel.put(1950, TechConstants.T_ALLOWED_ALL);
        bomb.availRating = new int[] { RATING_D ,RATING_D ,RATING_D ,RATING_C};
        bomb.techRating = RATING_B;
        bomb.rulesRefs = "359, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        bomb.techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        bomb.techProgression.setTechRating(RATING_B);
        bomb.techProgression.setAvailability( new int[] { RATING_D, RATING_D, RATING_D, RATING_C });

        return bomb;
    }

    private static BombType createISLAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "LAA Missile Ammo";
        bomb.setInternalName("IS "+BombType.getBombInternalName(BombType.B_LAA));
        bomb.damagePerShot = 6;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_LAA_MISSILE;
        bomb.bombType = BombType.B_LAA;;
        bomb.shots = 1;
        bomb.bv = 17;
        bomb.cost = 0;
        bomb.introDate = 3064;
        bomb.techLevel.put(3064, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(3072, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_X ,RATING_X ,RATING_F ,RATING_D};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "359, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3064, 3072, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_D });

        return bomb;
    }
    
    private static BombType createCLLAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "LAA Missile Ammo";
        bomb.setInternalName("Clan "+BombType.getBombInternalName(BombType.B_LAA));
        bomb.damagePerShot = 6;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_LAA_MISSILE;
        bomb.bombType = BombType.B_LAA;;
        bomb.shots = 1;
        bomb.bv = 17;
        bomb.cost = 0;
        bomb.introDate = 3074;
        bomb.techLevel.put(3074, TechConstants.T_CLAN_ADVANCED);
        bomb.availRating = new int[] { RATING_X,RATING_X ,RATING_F ,RATING_D};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "359, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);
        bomb.techProgression.setClanProgression(DATE_NONE, 3074, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_F, RATING_D });

        return bomb;
    }

    private static BombType createThunderBomb() {
        BombType bomb = new BombType();

        bomb.name = "Thunder Bomb";
        bomb.shortName = "ThunderBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_THUNDER));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_THUNDER;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;
        bomb.introDate = 3055;
        bomb.techLevel.put(3055, TechConstants.T_IS_EXPERIMENTAL);
        bomb.techLevel.put(3065, TechConstants.T_IS_ADVANCED);
        bomb.availRating = new int[] { RATING_X ,RATING_X ,RATING_E ,RATING_D};
        bomb.techRating = RATING_E;
        bomb.rulesRefs = "360, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3055, 3065, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_E);
        bomb.techProgression.setAvailability( new int[] { RATING_X, RATING_X, RATING_E, RATING_D });


        return bomb;
    }

    private static BombType createTorpedoBomb() {
        BombType bomb = new BombType();

        bomb.name = "Torpedo Bomb";
        bomb.shortName = "TorpedoBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_TORPEDO));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_TORPEDO;
        bomb.shots = 1;
        bomb.bv = 10;
        bomb.cost = 0;
        bomb.introDate = 1950;
        bomb.techLevel.put(1950, TechConstants.T_ALLOWED_ALL);
        bomb.availRating = new int[] { RATING_C ,RATING_C ,RATING_C ,RATING_C};
        bomb.techRating = RATING_B;
        bomb.rulesRefs = "360, TO";
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_ALL);
        bomb.techProgression.setProgression(DATE_NONE, DATE_NONE, 1950);
        bomb.techProgression.setTechRating(RATING_B);
        bomb.techProgression.setAvailability( new int[] { RATING_C, RATING_C, RATING_C, RATING_C });

        return bomb;
    }

    private static BombType createAlamoBomb() {
        BombType bomb = new BombType();

        bomb.techLevel.put(3071,TechConstants.T_IS_EXPERIMENTAL);
        bomb.name = "Alamo Missile Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ALAMO));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ALAMO;
        bomb.bombType = BombType.B_ALAMO;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 0;
        bomb.flags = bomb.flags.or(F_NUCLEAR);
        bomb.capital = true;
        bomb.techProgression.setTechBase(TechProgression.TECH_BASE_IS);
        bomb.techProgression.setISProgression(3071, DATE_NONE, DATE_NONE);
        bomb.techProgression.setTechRating(RATING_C);
        bomb.techProgression.setAvailability( new int[] { RATING_E, RATING_E, RATING_E, RATING_E });

        return bomb;
    }

}