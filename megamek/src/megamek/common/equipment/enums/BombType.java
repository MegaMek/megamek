/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.equipment.enums;

import static java.util.stream.Collectors.toList;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;

public class BombType extends AmmoType {

    public enum BombTypeEnum {
        NONE(-1, "None", "None", null, false, 0, false, false, false),
        HE(0, "HE Bomb", "HEBomb", null, false, 1, true, true, false),
        CLUSTER(1, "Cluster Bomb", "ClusterBomb", null, false, 1, true, true, false),
        LG(2, "Laser-guided Bomb", "LGBomb", null, false, 1, true, true, true),
        RL(3, "Rocket", "RL 10 Ammo (Bomb)", "BombRL", false, 1, false, false, false),
        TAG(4, "TAG", "TAGBomb", "BombTAG", false, 1, false, false, true),
        AAA(5, "AAA Missile", "AAAMissile Ammo", "AAA Missile", true, 5, false, false, false),
        AS(6, "AS Missile", "ASMissile Ammo", "AS Missile", true, 6, false, false, false),
        ASEW(7, "ASEW Missile", "ASEWMissile Ammo", "ASEWMissile", true, 6, false, false, false),
        ARROW(8, "Arrow IV Missile", "ArrowIVMissile Ammo", "BombArrowIV", true, 5, true, false, false),
        HOMING(9, "Arrow IV Homing Missile", "ArrowIVHomingMissile Ammo", "BombArrowIV", true, 5, true, false, false),
        INFERNO(10, "Inferno Bomb", "InfernoBomb", null, true, 1, false, true, false),
        LAA(11, "LAA Missile", "LAAMissile Ammo", "LAAMissile", true, 2, false, false, false),
        THUNDER(12, "Thunder Bomb", "ThunderBomb", null, true, 1, false, true, false),
        TORPEDO(13, "Torpedo Bomb", "TorpedoBomb", null, true, 1, false, true, false),
        ALAMO(14, "Alamo Missile", "AlamoMissile Ammo", "AlamoMissile", true, 10, false, false, false),
        FAE_SMALL(15, "Fuel-Air Bomb (small)", "FABombSmall Ammo", null, true, 1, false, true, false),
        FAE_LARGE(16, "Fuel-Air Bomb (large)", "FABombLarge Ammo", null, true, 2, false, true, false),
        RLP(17, "Prototype Rocket", "RL-P 10 Ammo (Bomb)", "BombRLP", true, 1, false, false, false);

        private static final Map<Integer, BombTypeEnum> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, BombTypeEnum> INTERNAL_NAME_LOOKUP = new HashMap<>();

        static {
            for (BombTypeEnum bt : values()) {
                INDEX_LOOKUP.put(bt.index, bt);
                INTERNAL_NAME_LOOKUP.put(bt.internalName, bt);
            }
        }

        private final int index;
        private final String displayName;
        private final String internalName;
        private final String weaponName;
        private final boolean advancedAmmo;
        private final int cost;
        private final boolean canSpaceBomb;
        private final boolean canGroundBomb;
        private final boolean isGuided;

        BombTypeEnum(int index, String displayName, String internalName, String weaponName, boolean advancedAmmo,
              int cost, boolean canSpaceBomb, boolean canGroundBomb, boolean isGuided) {
            this.index = index;
            this.displayName = displayName;
            this.internalName = internalName;
            this.weaponName = weaponName;
            this.advancedAmmo = advancedAmmo;
            this.cost = cost;
            this.canSpaceBomb = canSpaceBomb;
            this.canGroundBomb = canGroundBomb;
            this.isGuided = isGuided;
        }

        public int getIndex() {
            return index;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getInternalName() {
            return internalName;
        }

        public String getWeaponName() {
            return weaponName;
        }

        /**
         * @return The amount of hardpoint space that one of this bomb type takes up when carried by a fighter unit.
         */
        public int getCost() {
            return cost;
        }

        public boolean canSpaceBomb() {
            return canSpaceBomb;
        }

        public boolean canGroundBomb() {
            return canGroundBomb;
        }

        public boolean isGuided() {
            return isGuided;
        }

        public boolean isAdvancedAmmo() {
            return advancedAmmo;
        }

        /**
         * Checks if a bomb type is allowed given current game settings.
         *
         * @param gameOptions the current game options
         *
         * @return true if the bomb type is allowed, false otherwise
         */
        public boolean isAllowedByGameOptions(GameOptions gameOptions) {
            if (this == BombTypeEnum.ALAMO &&
                  !gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES)) {
                return false;
            }
            int gameTL = TechConstants.getSimpleLevel(
                  gameOptions.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)
            );
            return !this.isAdvancedAmmo() || (gameTL >= TechConstants.T_SIMPLE_ADVANCED);
        }

        public static BombTypeEnum fromIndex(int index) {
            return INDEX_LOOKUP.getOrDefault(index, NONE);
        }

        public static BombTypeEnum fromInternalName(String name) {
            return INTERNAL_NAME_LOOKUP.getOrDefault(name, NONE);
        }

    }

    private BombTypeEnum bombType;
    private int blastRadius = 0;

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static String getBombName(int type) {
        return BombTypeEnum.fromIndex(type).getDisplayName();
    }

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static BombTypeEnum getBombTypeFromName(String name) {
        if (name == null || name.isEmpty()) {
            return BombTypeEnum.NONE;
        }
        for (BombTypeEnum bType : BombTypeEnum.values()) {
            if (bType.getDisplayName().equalsIgnoreCase(name)) {
                return bType;
            }
        }
        return BombTypeEnum.NONE;
    }

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static BombTypeEnum getBombTypeFromInternalName(String name) {
        return BombTypeEnum.fromInternalName(name);
    }

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static String getBombWeaponName(int type) {
        BombTypeEnum bombType = BombTypeEnum.fromIndex(type);
        if (bombType == BombTypeEnum.NONE) {
            return "Unknown bomb weapon";
        }
        return bombType.getWeaponName();
    }

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static BombTypeEnum getBombTypeForWeapon(EquipmentType weapon) {
        if (!(weapon instanceof BombType)) {
            return BombTypeEnum.NONE;
        }
        String weaponName = weapon.getInternalName();
        for (BombTypeEnum bType : BombTypeEnum.values()) {
            if (bType.getInternalName().equalsIgnoreCase(weaponName)) {
                return bType;
            }
        }
        return BombTypeEnum.NONE;
    }

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static String getBombInternalName(int type) {
        BombTypeEnum bombType = BombTypeEnum.fromIndex(type);
        if (bombType == BombTypeEnum.NONE) {
            return "Unknown bomb type";
        }
        return bombType.getInternalName();
    }

    @Deprecated(
          forRemoval = true,
          since = "0.50.7")
    public static int getBombCost(int type) {
        return BombTypeEnum.fromIndex(type).getCost();
    }

    public BombTypeEnum getBombType() {
        return bombType;
    }

    public static void initializeTypes() {
        EquipmentType.addType(BombType.createHighExplosiveBomb());
        EquipmentType.addType(BombType.createClusterBomb());
        EquipmentType.addType(BombType.createLaserGuidedBomb());
        //        EquipmentType.addType(BombType.createCLLaserGuidedBomb());
        EquipmentType.addType(BombType.createRocketBomb());
        EquipmentType.addType(BombType.createPrototypeRocketBomb());
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

    public static BombType createBombByType(BombTypeEnum bType) {
        return switch (bType) {
            case HE -> createHighExplosiveBomb();
            case CLUSTER -> createClusterBomb();
            case LG -> createLaserGuidedBomb();
            case RL -> createRocketBomb();
            case TAG -> createTAGBomb();
            case AAA -> createAAAMissileBomb();
            case AS -> createASMissileBomb();
            case ASEW -> createISASEWMissileBomb();
            case ARROW -> createArrowIVBomb();
            case HOMING -> createArrowIVHomingBomb();
            case INFERNO -> createInfernoBomb();
            case LAA -> createLAAMissileBomb();
            case THUNDER -> createThunderBomb();
            case TORPEDO -> createTorpedoBomb();
            case ALAMO -> createAlamoBomb();
            case FAE_SMALL -> createSmallFuelAirBomb();
            case FAE_LARGE -> createLargeFuelAirBomb();
            case RLP -> createPrototypeRocketBomb();
            default -> null;
        };
    }

    // START OF BOMBS

    private static BombType createAAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Air-to-Air (AAA) Arrow Ammo";
        bomb.setInternalName(BombTypeEnum.AAA.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.AAA.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.AAA.getInternalName());
        bomb.addLookupName("AAAMissile Ammo");
        bomb.damagePerShot = 20;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.AAA_MISSILE;
        bomb.bombType = BombTypeEnum.AAA;
        bomb.shots = 1;
        bomb.bv = 57;
        bomb.cost = 9000;
        bomb.rulesRefs = "169, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3069, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(3069, DATE_NONE, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.LC, Faction.CWX)
              .setProductionFactions(Faction.LC);

        return bomb;
    }

    private static BombType createASMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship (AS) Missiles Ammo";
        bomb.setInternalName(BombTypeEnum.AS.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.AS.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.AS.getInternalName());
        bomb.addLookupName("ASMissile Ammo");
        bomb.damagePerShot = 30;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.AS_MISSILE;
        bomb.bombType = BombTypeEnum.AS;
        bomb.shots = 1;
        bomb.bv = 114;
        bomb.cost = 15000;
        bomb.rulesRefs = "170, TO:AUE";
        // Tech Progression tweaked to combine IntOps with TRO Prototypes/3145 NTNU RS
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3071, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, true, false, false, false)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 3072, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, true, false, false)
              .setPrototypeFactions(Faction.FS).setProductionFactions(Faction.FS)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

        return bomb;
    }

    private static BombType createISASEWMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Anti-Ship Electronic Warfare (ASEW) Ammo";
        bomb.setInternalName(BombTypeEnum.ASEW.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.ASEW.getInternalName());
        bomb.addLookupName("ASEWMissile Ammo");
        bomb.damagePerShot = 0;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.ASEW_MISSILE;
        bomb.bombType = BombTypeEnum.ASEW;
        bomb.shots = 1;
        bomb.bv = 75;
        bomb.cost = 20000;
        bomb.rulesRefs = "170, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(3067, 3073, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);

        return bomb;
    }

    private static BombType createArrowIVHomingBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Homing Missile (Air-Launched Version)";
        bomb.shortName = "Arrow IV Homing Air";
        bomb.setInternalName(BombTypeEnum.HOMING.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.HOMING.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.HOMING.getInternalName());
        bomb.addLookupName("ArrowIVHomingMissile Ammo");
        bomb.damagePerShot = 1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoTypeEnum.ARROW_IV_BOMB;
        bomb.bombType = BombTypeEnum.HOMING;
        bomb.munitionType = EnumSet.of(AmmoType.Munitions.M_HOMING);
        // Allow Homing munitions to instantly switch between modes
        bomb.instantModeSwitch = true;
        bomb.setModes("Homing", "Non-Homing");
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 30;
        bomb.cost = 3000;
        bomb.rulesRefs = "171, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2595, 2600, DATE_NONE, 2835, 3047)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2595, 2600, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.CC);

        return bomb;
    }

    private static BombType createArrowIVBomb() {
        BombType bomb = new BombType();

        bomb.name = "Arrow IV Non-Homing Missile (Air-Launched Version)";
        bomb.shortName = "Arrow IV Air";
        bomb.setInternalName(BombTypeEnum.ARROW.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.ARROW.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.ARROW.getInternalName());
        bomb.addLookupName("ArrowIVMissile Ammo");
        bomb.damagePerShot = 1;
        bomb.rackSize = 20;
        bomb.ammoType = AmmoTypeEnum.ARROW_IV_BOMB;
        bomb.bombType = BombTypeEnum.ARROW;
        bomb.blastRadius = 1;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB);
        bomb.shots = 1;
        bomb.bv = 34;
        bomb.cost = 2000;
        bomb.rulesRefs = "171, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(2622, 2623, DATE_NONE, 2850, 3046)
              .setISApproximate(true, false, false, true, false)
              .setClanAdvancement(2622, 2623, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.CC);

        return bomb;
    }

    private static BombType createClusterBomb() {
        BombType bomb = new BombType();

        bomb.name = "Cluster Bomb";
        bomb.shortName = "ClusterBomb";
        bomb.setInternalName(BombTypeEnum.CLUSTER.getInternalName());
        bomb.damagePerShot = 5;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.CLUSTER;
        bomb.blastRadius = 1;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 13;
        bomb.cost = 8000;
        bomb.rulesRefs = "246, TW";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);

        return bomb;
    }

    private static BombType createSmallFuelAirBomb() {
        BombType bomb = new BombType();

        bomb.name = "Fuel-Air Bomb (Small)";
        bomb.shortName = "FAE Bomb (s)";
        bomb.setInternalName(BombTypeEnum.FAE_SMALL.getInternalName());
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.FAE_SMALL;
        bomb.blastRadius = 2;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 37;
        bomb.cost = 18000;
        bomb.tonnage = .5;
        bomb.rulesRefs = "159, IO:AE";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);

        return bomb;
    }

    private static BombType createLargeFuelAirBomb() {
        BombType bomb = new BombType();

        bomb.name = "Fuel-Air Bomb (Large)";
        bomb.shortName = "FAE Bomb (L)";
        bomb.setInternalName(BombTypeEnum.FAE_LARGE.getInternalName());
        bomb.damagePerShot = 30;
        bomb.rackSize = 2;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.FAE_LARGE;
        bomb.blastRadius = 3;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 2;
        bomb.bv = 63;
        bomb.cost = 35000;
        bomb.tonnage = 1.0;
        bomb.rulesRefs = "159, IO:AE";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);

        return bomb;
    }

    private static BombType createHighExplosiveBomb() {
        BombType bomb = new BombType();

        bomb.name = "High-Explosive (Standard) Bomb";
        bomb.shortName = "HEBomb";
        bomb.setInternalName(BombTypeEnum.HE.getInternalName());
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.HE;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 12;
        bomb.cost = 5000;
        bomb.rulesRefs = "246, TW";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
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
        bomb.setInternalName(BombTypeEnum.INFERNO.getInternalName());
        bomb.damagePerShot = 5;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.INFERNO;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 16;
        bomb.cost = 6000;
        bomb.rulesRefs = "171, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.D, AvailabilityValue.C)
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
        bomb.setInternalName(BombTypeEnum.LG.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.LG.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.LG.getInternalName());
        bomb.addLookupName("LGBomb");
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.LG;
        bomb.flags = bomb.flags.or(AmmoType.F_SPACE_BOMB).or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 20;
        bomb.cost = 10000;
        bomb.rulesRefs = "247, TW";
        // Tech Progression adjusted to match future errata. While called Laser-Guided this is aligned
        // with TAG for progression once TAG is common.
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(DATE_ES, DATE_ES, 2645, 2835, 3035)
              .setISApproximate(false, false, false, true, false)
              .setClanAdvancement(DATE_ES, DATE_ES, 3065, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setReintroductionFactions(Faction.FW);

        return bomb;
    }

    private static BombType createLAAMissileBomb() {
        BombType bomb = new BombType();

        bomb.name = "Light Air-to-Air (LAA) Missiles Ammo";
        bomb.setInternalName(BombTypeEnum.LAA.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.LAA.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.LAA.getInternalName());
        bomb.addLookupName("LAAMissile Ammo");
        bomb.damagePerShot = 6;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.LAA_MISSILE;
        bomb.bombType = BombTypeEnum.LAA;
        bomb.shots = 1;
        bomb.bv = 17;
        bomb.cost = 6000;
        bomb.rulesRefs = "171, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.D)
              .setISAdvancement(3069, 3072, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);

        return bomb;
    }

    // TODO Mine Bombs

    private static BombType createRocketBomb() {
        BombType bomb = new BombType();

        bomb.name = "Rocket Launcher Pod";
        bomb.setInternalName(BombTypeEnum.RL.getInternalName());
        bomb.addLookupName("RL 10 (Bomb)");
        bomb.damagePerShot = 1;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 10;
        bomb.ammoType = AmmoTypeEnum.RL_BOMB;
        bomb.bombType = BombTypeEnum.RL;
        bomb.shots = 1;
        bomb.bv = 18;
        bomb.cost = 15000;
        bomb.rulesRefs = "229, TM";
        bomb.techAdvancement.setTechBase(TechBase.IS)
              .setISAdvancement(3055, 3064, 3067)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.B, AvailabilityValue.B);

        return bomb;
    }

    private static BombType createPrototypeRocketBomb() {
        BombType bomb = new BombType();

        bomb.name = "Rocket Launcher (Prototype) Pod";
        bomb.setInternalName(BombTypeEnum.RLP.getInternalName());
        bomb.addLookupName("RL-P 10 (Bomb)");
        bomb.damagePerShot = 1;
        // This works but is fragile
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB).or(WeaponType.F_PROTOTYPE);
        bomb.rackSize = 10;
        bomb.ammoType = AmmoTypeEnum.RL_BOMB;
        bomb.bombType = BombTypeEnum.RLP;
        bomb.shots = 1;
        bomb.bv = 15;
        bomb.cost = 15000;
        bomb.rulesRefs = "67, IO:AE";
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.X)
              .setISAdvancement(DATE_ES, DATE_NONE, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(DATE_ES, DATE_NONE, DATE_NONE, 2823, DATE_NONE)
              .setClanApproximate(true, false, false, true, false)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
        return bomb;
    }

    private static BombType createTAGBomb() {
        BombType bomb = new BombType();

        bomb.name = "TAG Pod";
        bomb.shortName = "TAGPod";
        bomb.setInternalName(BombTypeEnum.TAG.getInternalName());
        bomb.addLookupName("IS " + BombTypeEnum.TAG.getInternalName());
        bomb.addLookupName("Clan " + BombTypeEnum.TAG.getInternalName());
        bomb.addLookupName("CLTAGBomb");
        bomb.addLookupName("ISTAGBomb");
        bomb.addLookupName("TAGBomb");
        bomb.damagePerShot = 0;
        bomb.flags = bomb.flags.or(AmmoType.F_OTHER_BOMB);
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.TAG;
        bomb.shots = 1;
        bomb.bv = 0;
        bomb.cost = 50000;
        bomb.rulesRefs = "238, TM";
        bomb.techAdvancement.setTechBase(TechBase.ALL)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.F, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2600, 2605, 2645, 2835, 3035)
              .setISApproximate(false, false, false, true, false)
              .setClanAdvancement(2600, 2605, 2645, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH)
              .setReintroductionFactions(Faction.FW);

        return bomb;
    }

    /** Thunder Standard Bombs, TO:AUE pg. 172, 197 */
    private static BombType createThunderBomb() {
        BombType bomb = new BombType();

        bomb.name = "Thunder (FASCAM) Bombs";
        bomb.shortName = "ThunderBomb";
        bomb.setInternalName(BombTypeEnum.THUNDER.getInternalName());
        bomb.damagePerShot = 20;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.THUNDER;
        bomb.blastRadius = 1;
        bomb.flags = bomb.flags.or(AmmoType.F_GROUND_BOMB);
        bomb.shots = 1;
        bomb.bv = 112;
        bomb.cost = 12000;
        bomb.rulesRefs = "172, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
              .setISAdvancement(2600, 2623, DATE_NONE, 2850, 3052)
              .setISApproximate(true, false, false, false, false)
              .setClanAdvancement(2600, 2623, DATE_NONE, DATE_NONE, DATE_NONE)
              .setClanApproximate(true, false, false, false, false).setPrototypeFactions(Faction.TH)
              .setProductionFactions(Faction.TH);

        return bomb;
    }

    // TODO Thunder Active Bombs, Thunder Vibro Bombs - See IO pg 60 and TO pg
    // 360

    private static BombType createTorpedoBomb() {
        BombType bomb = new BombType();

        bomb.name = "Torpedo Bomb";
        bomb.shortName = "TorpedoBomb";
        bomb.setInternalName(BombTypeEnum.TORPEDO.getInternalName());
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.BOMB;
        bomb.bombType = BombTypeEnum.TORPEDO;
        bomb.shots = 1;
        bomb.bv = 10;
        bomb.cost = 7000;
        bomb.rulesRefs = "172, TO:AUE";
        bomb.techAdvancement.setTechBase(TechBase.ALL).setIntroLevel(false).setUnofficial(false)
              .setTechRating(TechRating.B)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C)
              .setISAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setClanAdvancement(DATE_PS, DATE_PS, DATE_PS, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);

        return bomb;
    }

    private static BombType createAlamoBomb() {
        BombType bomb = new BombType();

        bomb.name = "Std. Nuclear Weapon (Type II/Alamo)";
        bomb.setInternalName(BombTypeEnum.ALAMO.getInternalName());
        bomb.damagePerShot = 10;
        bomb.rackSize = 1;
        bomb.ammoType = AmmoTypeEnum.ALAMO;
        bomb.bombType = BombTypeEnum.ALAMO;
        bomb.shots = 1;
        bomb.bv = 100;
        bomb.cost = 1000000;
        bomb.flags = bomb.flags.or(F_NUCLEAR).or(AmmoType.F_OTHER_BOMB);
        bomb.capital = true;
        bomb.techAdvancement.setTechBase(TechBase.IS).setTechRating(TechRating.E)
              .setISAdvancement(2200).setPrototypeFactions(Faction.TA)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

        return bomb;
    }

    /**
     * @return True when this bomb type is either a large or small FAE.
     */
    public boolean isFaeBomb() {
        return (bombType == BombTypeEnum.FAE_LARGE) || (bombType == BombTypeEnum.FAE_SMALL);
    }
}
