/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

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
    public static final int B_FAE_SMALL = 15;
    public static final int B_FAE_LARGE = 16;
    public static final int B_NUM     = 17;

    public static final String[] bombNames = { "HE Bomb","Cluster Bomb","Laser-guided Bomb",
                                              "Rocket", "TAG", "AAA Missile", "AS Missile",
                                              "ASEW Missile", "Arrow IV Missile",
                                              "Arrow IV Homing Missile", "Inferno Bomb",
                                              "LAA Missile", "Thunder Bomb", "Torpedo Bomb",
                                              "Alamo Missile", "Fuel-Air Bomb (small)", "Fuel-Air Bomb (large)" };

    public static final String[] bombInternalNames = { "HEBomb","ClusterBomb","LGBomb",
                                                      "RL 10 Ammo (Bomb)", "TAGBomb", "AAAMissile Ammo",
                                                      "ASMissile Ammo",
                                                      "ASEWMissile Ammo", "ArrowIVMissile Ammo",
                                                      "ArrowIVHomingMissile Ammo", "InfernoBomb",
                                                      "LAAMissile Ammo", "ThunderBomb", "TorpedoBomb",
                                                      "AlamoMissile Ammo", "FABombSmall Ammo", "FABombLarge Ammo" };

    public static final String[] bombWeaponNames = { null, null, null, "BombRL", "BombTAG", "AAA Missile",
                                                    "AS Missile", "ASEWMissile", "BombArrowIV", "BombArrowIV",
                                                    null, "LAAMissile", null, null, "AlamoMissile", null, null };

    public static final int[] bombCosts = { 1, 1, 1, 1, 1, 5, 6, 6, 5, 5, 1, 2, 1, 1, 10, 1, 2 };

    public static final Map<String, Integer> blastRadius;
    private int bombType;

    static {
        blastRadius = new HashMap<>();
    }

    /**
     * Get the blast radius of a particular bomb type, given the internal name.
     */
    public static int getBombBlastRadius(String name) {
        return blastRadius.getOrDefault(name, 0);
    }

    public static String getBombName(int type) {
        if ((type >= B_NUM) || (type < 0)) {
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
            if (bombInternalNames[i].equalsIgnoreCase(name)) {
                return i;
            }
        }
        return B_NONE;
    }

    public static String getBombWeaponName(int type) {
        if ((type >= B_NUM) || (type < 0)) {
            return "Unknown bomb weapon";
        }
        return bombWeaponNames[type];
    }

    public static int getBombTypeForWeapon(EquipmentType weapon) {
        for (int i = 0; i < B_NUM; i++) {
            if (bombWeaponNames[i] != null
                    && bombWeaponNames[i].equals(weapon.getInternalName())) {
                return i;
            }
        }
        return -1;
    }

    public static String getBombInternalName(int type) {
        if ((type >= B_NUM) || (type < 0)) {
            return "Unknown bomb type";
        }
        return bombInternalNames[type];
    }

    public static int getBombCost(int type) {
        if ((type >= B_NUM) || (type < 0)) {
            return 0;
        }
        return bombCosts[type];
    }

    public static boolean canGroundBomb(int type) {
        switch (type) {
            case B_HE:
            case B_CLUSTER:
            case B_LG:
            case B_INFERNO:
            case B_THUNDER:
            case B_TORPEDO:
            case B_FAE_SMALL:
            case B_FAE_LARGE:
                return true;
            default:
                return false;
        }
    }

    public static boolean canSpaceBomb(int type) {
        switch (type) {
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
        EquipmentType.addType(BombType.createLaserGuidedBomb());
//        EquipmentType.addType(BombType.createCLLaserGuidedBomb());
        EquipmentType.addType(BombType.createRocketBomb());
        EquipmentType.addType(BombType.createTAGBomb());
//        EquipmentType.addType(BombType.createCLTAGBomb());
        EquipmentType.addType(BombType.createAAAMissileBomb());
//        EquipmentType.addType(BombType.createCLAAAMissileBomb());
        EquipmentType.addType(BombType.createASMissileBomb());
//        EquipmentType.addType(BombType.createCLASMissileBomb());
        EquipmentType.addType(BombType.createISASEWMissileBomb());
//        EquipmentType.addType(BombType.createCLASEWMissileBomb());
        EquipmentType.addType(BombType.createArrowIVBomb());
//        EquipmentType.addType(BombType.createCLArrowIVBomb());
        EquipmentType.addType(BombType.createArrowIVHomingBomb());
//        EquipmentType.addType(BombType.createCLArrowIVHomingBomb());
        EquipmentType.addType(BombType.createInfernoBomb());
        EquipmentType.addType(BombType.createLAAMissileBomb());
//        EquipmentType.addType(BombType.createCLLAAMissileBomb());
        EquipmentType.addType(BombType.createThunderBomb());
        EquipmentType.addType(BombType.createTorpedoBomb());
        EquipmentType.addType(BombType.createAlamoBomb());
        EquipmentType.addType(BombType.createSmallFuelAirBomb());
        EquipmentType.addType(BombType.createLargeFuelAirBomb());
    }

    /** @return All BombType equipment types as a List. The list is a copy and can safely be modified. */
    public static List<BombType> allBombTypes() {
        if (EquipmentType.allTypes == null) {
            EquipmentType.initializeTypes();
        }
        return EquipmentType.allTypes.stream()
                .filter(eType -> eType instanceof BombType)
                .map(eType -> (BombType) eType)
                .collect(toList());
    }

    public static BombType createBombByType(int bType)    {
        switch (bType) {
            case B_HE:
                return createHighExplosiveBomb();
            case B_CLUSTER:
                return createClusterBomb();
            case B_LG:
                return createLaserGuidedBomb();
            case B_RL:
                return createRocketBomb();
            case B_TAG:
                return createTAGBomb();
            case B_AAA:
                return createAAAMissileBomb();
            case B_AS:
                return createASMissileBomb();
            case B_ASEW:
                return createISASEWMissileBomb();
            case B_ARROW:
                return createArrowIVBomb();
            case B_HOMING:
                return createArrowIVHomingBomb();
            case B_INFERNO:
                return createInfernoBomb();
            case B_LAA:
                return createLAAMissileBomb();
            case B_THUNDER:
                return createThunderBomb();
            case B_TORPEDO:
                return createTorpedoBomb();
            case B_ALAMO:
                return createAlamoBomb();
            case B_FAE_SMALL:
                return createSmallFuelAirBomb();
            case B_FAE_LARGE:
                return createLargeFuelAirBomb();
            default:
                return null;
        }
    }

    // START OF BOMBS

    private static BombType createAAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Air-to-Air (AAA) Arrow Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_AAA));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_AAA));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_AAA));
        bomb.addLookupName("AAAMissile Ammo");
        bomb.damagePerShot = 20;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AAA_MISSILE;
        bomb.bombType = BombType.B_AAA;
        bomb.shots = 1;
        bomb.bv = 57;
        bomb.cost = 9000;
        bomb.rulesRefs = "357, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3069, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false)
                .setClanAdvancement(3069, DATE_NONE, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_LC, F_CWX)
                .setProductionFactions(F_LC);

        return bomb;
    }

    private static BombType createASMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship (AS) Missiles Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_AS));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_AS));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_AS));
        bomb.addLookupName("ASMissile Ammo");
        bomb.damagePerShot = 30;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_AS_MISSILE;
        bomb.bombType = BombType.B_AS;
        bomb.shots = 1;
        bomb.bv = 114;
        bomb.cost = 15000;
        bomb.rulesRefs = "358, TO";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_D)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3071, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, true, false, false, false)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3072, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, true, false, false)
                .setPrototypeFactions(F_FS).setProductionFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return bomb;
    }

    private static BombType createISASEWMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship Electronic Warfare (ASEW) Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ASEW));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_ASEW));
        bomb.addLookupName("ASEWMissile Ammo");
        bomb.damagePerShot = 0;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ASEW_MISSILE;
        bomb.bombType = BombType.B_ASEW;
        bomb.shots = 1;
        bomb.bv = 75;
        bomb.cost = 20000;
        bomb.rulesRefs = "358, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
                .setISAdvancement(3067, 3073, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false).setPrototypeFactions(F_LC)
                .setProductionFactions(F_LC);

        return bomb;
    }

    private static BombType createArrowIVHomingBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Homing Missile (Air-Launched Version)";
        bomb.shortName = "Arrow IV Homing (Air-Launch)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_HOMING));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_HOMING));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_HOMING));
        bomb.addLookupName("ArrowIVHomingMissile Ammo");
        bomb.damagePerShot = 1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_HOMING;
        bomb.munitionType = EnumSet.of(AmmoType.Munitions.M_HOMING);
        // Allow Homing munitions to instantly switch between modes
        bomb.instantModeSwitch = true;
        bomb.setModes("Homing", "Non-Homing");
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 30;
        bomb.cost = 3000;
        bomb.rulesRefs = "358, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(2595, 2600, DATE_NONE, 2835, 3047)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2595, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_CC);

        return bomb;
    }

    private static BombType createArrowIVBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Non-Homing Missile (Air-Launched Version)";
        bomb.shortName = "Arrow IV (Air-Launched Version)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ARROW));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_ARROW));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_ARROW));
        bomb.addLookupName("ArrowIVMissile Ammo");
        bomb.damagePerShot = 1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoType.T_ARROW_IV_BOMB;
        bomb.bombType = BombType.B_ARROW;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 34;
        bomb.cost = 2000;
        bomb.rulesRefs = "359, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E,RATING_F, RATING_E, RATING_E)
                .setISAdvancement(2622, 2623, DATE_NONE, 2850, 3046)
                .setISApproximate(true, false, false, true, false)
                .setClanAdvancement(2622, 2623, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_CC);

        blastRadius.put(BombType.getBombInternalName(BombType.B_ARROW), 1);

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
        bomb.rulesRefs = "246, TW";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);

        blastRadius.put(BombType.getBombInternalName(BombType.B_CLUSTER), 1);

        return bomb;
    }

    private static BombType createSmallFuelAirBomb() {
        BombType bomb = new BombType();

        bomb.name = "Fuel-Air Bomb (Small)";
        bomb.shortName = "FAE Bomb (s)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_FAE_SMALL));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_FAE_SMALL;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 37;
        bomb.cost = 18000;
        bomb.tonnage = .5;
        bomb.rulesRefs = "166, IO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);

        blastRadius.put(BombType.getBombInternalName(BombType.B_FAE_SMALL), 2);

        return bomb;
    }

    private static BombType createLargeFuelAirBomb() {
        BombType bomb = new BombType();

        bomb.name = "Fuel-Air Bomb (Large)";
        bomb.shortName = "FAE Bomb (L)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_FAE_LARGE));
        bomb.damagePerShot = 30;
        bomb.rackSize = 2;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_FAE_LARGE;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 2;
        bomb.bv = 63;
        bomb.cost = 35000;
        bomb.tonnage = 1.0;
        bomb.rulesRefs = "165, IO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_F, RATING_E, RATING_E)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);

        blastRadius.put(BombType.getBombInternalName(BombType.B_FAE_LARGE), 3);

        return bomb;
    }

    private static BombType createHighExplosiveBomb() {
        BombType bomb = new BombType();

        bomb.name = "High-Explosive (Standard) Bomb";
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
        bomb.rulesRefs = "246, TW";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);

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
        bomb.cost = 6000;
        bomb.rulesRefs = "359, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_D, RATING_D, RATING_D, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);

        return bomb;
    }

    private static BombType createLaserGuidedBomb() {
        BombType bomb = new BombType();

        bomb.name = "Laser-Guided (LG) Bomb";
        bomb.shortName = "LGBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_LG));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_LG));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_LG));
        bomb.addLookupName("LGBomb");
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_LG;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 20;
        bomb.cost = 10000;
        bomb.rulesRefs = "247, TW";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_C)
                .setAvailability(RATING_E, RATING_F, RATING_E, RATING_D)
                .setISAdvancement(DATE_ES, DATE_ES, 3065, 2800, 3060)
                .setISApproximate(false, false, false, true, false)
                .setClanAdvancement(DATE_ES, DATE_ES, 3065, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setReintroductionFactions(F_FW);

        return bomb;
    }

    private static BombType createLAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Light Air-to-Air (LAA) Missiles Ammo";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_LAA));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_LAA));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_LAA));
        bomb.addLookupName("LAAMissile Ammo");
        bomb.damagePerShot = 6;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_LAA_MISSILE;
        bomb.bombType = BombType.B_LAA;
        bomb.shots = 1;
        bomb.bv = 17;
        bomb.cost = 6000;
        bomb.rulesRefs = "359, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_IS).setIntroLevel(false).setUnofficial(false).setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_D)
                .setISAdvancement(3069, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
                .setISApproximate(true, false, false, false, false).setPrototypeFactions(F_FW)
                .setProductionFactions(F_FW);

        return bomb;
    }

    // TODO Mine Bombs

    private static BombType createRocketBomb() {
        BombType bomb = new BombType();

        bomb.name = "Rocket Launcher Pod";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_RL));
        bomb.addLookupName("RL 10 (Bomb)");
        bomb.damagePerShot = 1;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 10;
        bomb.ammoType = AmmoType.T_RL_BOMB;
        bomb.bombType = BombType.B_RL;
        bomb.shots = 1;
        bomb.bv = 18;
        bomb.cost = 15000;
        bomb.rulesRefs = "229, TM";
        bomb.techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS);
        bomb.techAdvancement.setISAdvancement(3055, 3064, 3067);
        bomb.techAdvancement.setTechRating(RATING_B);
        bomb.techAdvancement.setAvailability(RATING_X, RATING_X, RATING_B, RATING_B);

        return bomb;
    }

    private static BombType createTAGBomb() {
        BombType bomb = new BombType();

        bomb.name = "TAG Pod";
        bomb.shortName = "TAGPod";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_TAG));
        bomb.addLookupName("IS " + BombType.getBombInternalName(BombType.B_TAG));
        bomb.addLookupName("Clan " + BombType.getBombInternalName(BombType.B_TAG));
        bomb.addLookupName("CLTAGBomb");
        bomb.addLookupName("ISTAGBomb");
        bomb.addLookupName("TAGBomb");
        bomb.damagePerShot = 0;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_TAG;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 50000;
        bomb.rulesRefs = "238, TM";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_E, RATING_F, RATING_D, RATING_D)
                .setISAdvancement(2600, 2605, 2645, 2835, 3035)
                .setISApproximate(false, false, false, true, false)
                .setClanAdvancement(2600, 2605, 2645, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH)
                .setReintroductionFactions(F_FW);

        return bomb;
    }

    /** Thunder Standard Bombs, TO:AUE pg. 172, 197 */
    private static BombType createThunderBomb() {
        BombType bomb = new BombType();

        bomb.name = "Thunder (FASCAM) Bombs";
        bomb.shortName = "ThunderBomb";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_THUNDER));
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_BOMB;
        bomb.bombType = BombType.B_THUNDER;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 112;
        bomb.cost = 12000;
        bomb.rulesRefs = "172, TO:AUE";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_C).setAvailability(RATING_E, RATING_E, RATING_D, RATING_D)
                .setISAdvancement(2600, 2623, DATE_NONE, 2850, 3052).setISApproximate(true, false, false, false, false)
                .setClanAdvancement(2600, 2623, DATE_NONE, DATE_NONE, DATE_NONE)
                .setClanApproximate(true, false, false, false, false).setPrototypeFactions(F_TH)
                .setProductionFactions(F_TH);

        blastRadius.put(BombType.getBombInternalName(BombType.B_THUNDER), 1);

        return bomb;
    }

    // TODO Thunder Active Bombs, Thunder Vibro Bombs - See IO pg 60 and TO pg
    // 360

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
        bomb.cost = 7000;
        bomb.rulesRefs = "360, TO";
        bomb.techAdvancement.setTechBase(TECH_BASE_ALL).setIntroLevel(false).setUnofficial(false)
                .setTechRating(RATING_B).setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);

        return bomb;
    }

    private static BombType createAlamoBomb() {
        BombType bomb = new BombType();

        bomb.name = "Std. Nuclear Weapon (Type II/Alamo)";
        bomb.setInternalName(BombType.getBombInternalName(BombType.B_ALAMO));
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoType.T_ALAMO;
        bomb.bombType = BombType.B_ALAMO;
        bomb.shots = 1;
        bomb.bv = 100;
        bomb.cost = 1000000;
        bomb.flags = bomb.flags.or(F_NUCLEAR).or(AmmoType.F_OTHER_BOMB);
        bomb.capital = true;
        bomb.techAdvancement.setTechBase(TechAdvancement.TECH_BASE_IS).setTechRating(RATING_E)
                .setISAdvancement(2200).setPrototypeFactions(F_TA)
                .setAvailability(RATING_F, RATING_F, RATING_F, RATING_F)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return bomb;
    }
}
