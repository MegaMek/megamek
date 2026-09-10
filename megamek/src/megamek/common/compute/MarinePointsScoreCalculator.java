/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.compute;

import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Building;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;

/**
 * The Marine Points Score of a unit taking part in an infantry vs. infantry action, from the Marine Points Tables
 * (TO:AR p. 170) and the Building Modifiers Table (TO:AR p. 171).
 *
 * <p>Scores carry fractions, as in the book's worked examples (a Star of Elementals scores 186.75). Callers that
 * need a whole number round the total of a side up, per the building modifier rule "round all fractions up".</p>
 *
 * <p>Not applied here: the microgravity-only entries of the Battle Armor Modifiers table (quad {@code -2},
 * magnetic clamps {@code +1}, Space Operations Adaptation {@code +1}), which belong to boarding actions in space,
 * and the Manei Domini trooper value, which the unit data cannot identify.</p>
 */
public final class MarinePointsScoreCalculator {

    private static final MMLogger LOGGER = MMLogger.create(MarinePointsScoreCalculator.class);

    // Base Trooper Values (TO:AR p. 170)
    static final double ELEMENTAL_TROOPER = 2.0;
    static final double INNER_SPHERE_BATTLE_ARMOR_TROOPER = 1.0;
    static final double MARINE = 1.0;
    static final double NON_MARINE_SOLDIER = 0.75;
    static final double NON_COMBAT_CREW = 0.5;
    static final double CIVILIAN = 0.15;
    static final double ARMORED_TROOPER = 0.5;
    /** The armor Damage Divisor from which conventional infantry count as armored (TO:AUE p. 129). */
    static final double ARMORED_DAMAGE_DIVISOR = 2.0;

    // Battle Armor Modifiers, per trooper (TO:AR p. 170)
    static final double WEIGHT_CLASS_PAL = 1.0;
    static final double WEIGHT_CLASS_LIGHT_OR_MEDIUM = 2.0;
    static final double WEIGHT_CLASS_HEAVY = 3.0;
    static final double WEIGHT_CLASS_ASSAULT = 4.0;
    static final double INTACT_ARMOR_POINT = 0.5;
    static final double BURST_FIRE_WEAPONS = 2.0;
    static final double FLAME_WEAPONS = 1.0;
    static final double PAIRED_MAGNETIC_OR_VIBRO_CLAWS = 3.0;
    static final double PAIRED_OTHER_CLAWS = 2.0;
    static final double HEAVY_BATTLE_CLAWS = 0.5;
    static final double CUTTING_TORCHES = 0.5;
    static final double INDUSTRIAL_DRILLS = 0.5;
    static final double ANTI_PERSONNEL_WEAPON_MOUNTS = 0.25;

    // Building Modifiers Table (TO:AR p. 171)
    static final double MODIFIER_PER_STEP = 0.1;
    static final int STANDARD_LEVELS_PER_STEP = 6;
    static final int FORTRESS_LEVELS_PER_STEP = 3;
    /** A level only counts toward the building modifier when it is made of at least this many hexes. */
    static final int MINIMUM_HEXES_PER_LEVEL = 60;

    private static final String HEAVY_BATTLE_CLAW_INTERNAL_NAME_PREFIX = "BAHeavyBattleClaw";
    private static final String INDUSTRIAL_DRILL_INTERNAL_NAME = "BAIndustrialDrill";
    private static final int CLAWS_IN_A_PAIR = 2;

    private MarinePointsScoreCalculator() {}

    /**
     * The Marine Points Score of a unit before any building modifier.
     *
     * @param entity the unit, or {@code null} for no unit
     *
     * @return the score, {@code 0} for {@code null} or a unit with nobody left to fight
     */
    public static double calculateScore(@Nullable Entity entity) {
        return calculateScore(entity, null);
    }

    /**
     * The Marine Points Score of a unit defending a building, with the Building Modifiers Table applied. Only the
     * defender's score is modified (TO:AR p. 171); pass {@code null} for an attacker.
     *
     * @param entity           the unit, or {@code null} for no unit
     * @param defendedBuilding the building the unit is defending, or {@code null} for no building modifier
     *
     * @return the score, {@code 0} for {@code null} or a unit with nobody left to fight
     */
    public static double calculateScore(@Nullable Entity entity, @Nullable AbstractBuildingEntity defendedBuilding) {
        return breakdown(entity, defendedBuilding).modifiedScore();
    }

    /**
     * The working behind a unit's Marine Points Score, for a report that shows how the total was reached.
     *
     * @param entity           the unit, or {@code null} for no unit
     * @param defendedBuilding the building the unit is defending, or {@code null} for no building modifier
     *
     * @return the breakdown; {@link MarinePointsBreakdown#NOBODY} for {@code null}
     */
    public static MarinePointsBreakdown breakdown(@Nullable Entity entity,
          @Nullable AbstractBuildingEntity defendedBuilding) {
        if (entity == null) {
            return MarinePointsBreakdown.NOBODY;
        }
        double modifier = buildingModifier(defendedBuilding);
        MarinePointsBreakdown breakdown = switch (entity) {
            case BattleArmor squad -> battleArmorScore(squad, modifier);
            case ConvInfantry platoon -> conventionalInfantryScore(platoon, modifier);
            default -> crewScore(entity, modifier);
        };
        LOGGER.debug("[MarinePoints] {}: score {} (with building modifier {})", entity.getShortName(),
              breakdown.score(), breakdown.modifiedScore());
        return breakdown;
    }

    /**
     * The Marine Points Score of a unit before any building modifier, rounded up to a whole number.
     *
     * @param entity the unit, or {@code null} for no unit
     *
     * @return the rounded-up score
     */
    public static int calculateMPS(@Nullable Entity entity) {
        return (int) Math.ceil(calculateScore(entity));
    }

    /**
     * The Marine Points Score of a unit defending a building, rounded up to a whole number.
     *
     * @param entity   the unit, or {@code null} for no unit
     * @param building the defended building, or {@code null} for no building modifier
     *
     * @return the rounded-up score
     */
    public static int calculateMPS(@Nullable Entity entity, @Nullable AbstractBuildingEntity building) {
        return (int) Math.ceil(calculateScore(entity, building));
    }

    private static MarinePointsBreakdown conventionalInfantryScore(ConvInfantry platoon, double buildingModifier) {
        int troopers = Math.max(0, platoon.getInternal(ConvInfantry.LOC_INFANTRY));
        double perTrooper = platoon.hasSpecialization(ConvInfantry.MARINES) ? MARINE : NON_MARINE_SOLDIER;
        if (platoon.calcDamageDivisor() >= ARMORED_DAMAGE_DIVISOR) {
            perTrooper += ARMORED_TROOPER;
        }
        return MarinePointsBreakdown.conventionalInfantry(troopers, perTrooper, buildingModifier);
    }

    private static MarinePointsBreakdown battleArmorScore(BattleArmor squad, double buildingModifier) {
        int activeTroopers = squad.getNumberActiveTroopers();
        if (activeTroopers == 0) {
            return MarinePointsBreakdown.battleArmor(0, 0, 0, 0, 0, 0, buildingModifier);
        }
        double baseValue = squad.isClan() ? ELEMENTAL_TROOPER : INNER_SPHERE_BATTLE_ARMOR_TROOPER;
        int intactArmor = intactArmor(squad);
        return MarinePointsBreakdown.battleArmor(activeTroopers, baseValue, weightClassModifier(squad.getWeightClass()),
              squadEquipmentModifier(squad), intactArmor, intactArmor * INTACT_ARMOR_POINT, buildingModifier);
    }

    private static double weightClassModifier(int weightClass) {
        return switch (weightClass) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT -> WEIGHT_CLASS_PAL;
            case EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_MEDIUM -> WEIGHT_CLASS_LIGHT_OR_MEDIUM;
            case EntityWeightClass.WEIGHT_HEAVY -> WEIGHT_CLASS_HEAVY;
            case EntityWeightClass.WEIGHT_ASSAULT -> WEIGHT_CLASS_ASSAULT;
            default -> 0;
        };
    }

    /** Intact armor on the troopers still standing; the squad location is bookkeeping and carries none. */
    private static int intactArmor(BattleArmor squad) {
        int armor = 0;
        for (int trooper = 1; trooper < squad.locations(); trooper++) {
            if (squad.isTrooperActive(trooper)) {
                armor += Math.max(0, squad.getArmor(trooper));
            }
        }
        return armor;
    }

    /**
     * The "Mounts one or more ..." rows of the Battle Armor Modifiers table. Each row is checked once for the squad
     * and applies to every trooper.
     */
    private static double squadEquipmentModifier(BattleArmor squad) {
        boolean mountsBurstFireWeapon = false;
        boolean mountsFlameWeapon = false;
        boolean mountsHeavyBattleClaw = false;
        boolean mountsCuttingTorch = false;
        boolean mountsIndustrialDrill = false;
        boolean mountsAntiPersonnelMount = false;
        int claws = 0;
        int magneticOrVibroClaws = 0;
        for (Mounted<?> mounted : squad.getEquipment()) {
            if (mounted.getType() instanceof WeaponType weapon) {
                mountsBurstFireWeapon |= weapon.hasFlag(WeaponType.F_BURST_FIRE);
                mountsFlameWeapon |= isFlameWeapon(weapon);
            } else if (mounted.getType() instanceof MiscType equipment) {
                if (equipment.hasFlag(MiscTypeFlag.F_BATTLE_CLAW)) {
                    claws++;
                    boolean isMagneticOrVibro = equipment.hasFlag(MiscTypeFlag.F_MAGNET_CLAW)
                          || equipment.hasFlag(MiscTypeFlag.F_VIBROCLAW);
                    if (isMagneticOrVibro) {
                        magneticOrVibroClaws++;
                    }
                    mountsHeavyBattleClaw |= equipment.getInternalName()
                          .startsWith(HEAVY_BATTLE_CLAW_INTERNAL_NAME_PREFIX);
                }
                mountsCuttingTorch |= equipment.hasFlag(MiscTypeFlag.F_CUTTING_TORCH);
                mountsIndustrialDrill |= INDUSTRIAL_DRILL_INTERNAL_NAME.equals(equipment.getInternalName());
                mountsAntiPersonnelMount |= equipment.hasFlag(MiscTypeFlag.F_AP_MOUNT);
            }
        }

        double modifier = 0;
        if (mountsBurstFireWeapon) {
            modifier += BURST_FIRE_WEAPONS;
        }
        if (mountsFlameWeapon) {
            modifier += FLAME_WEAPONS;
        }
        if (magneticOrVibroClaws >= CLAWS_IN_A_PAIR) {
            modifier += PAIRED_MAGNETIC_OR_VIBRO_CLAWS;
        } else if (claws >= CLAWS_IN_A_PAIR) {
            modifier += PAIRED_OTHER_CLAWS;
        }
        if (mountsHeavyBattleClaw) {
            modifier += HEAVY_BATTLE_CLAWS;
        }
        if (mountsCuttingTorch) {
            modifier += CUTTING_TORCHES;
        }
        if (mountsIndustrialDrill) {
            modifier += INDUSTRIAL_DRILLS;
        }
        if (mountsAntiPersonnelMount) {
            modifier += ANTI_PERSONNEL_WEAPON_MOUNTS;
        }
        return modifier;
    }

    /** Plasma, flamer or Firedrake, the flame-based weapons the table names. */
    private static boolean isFlameWeapon(WeaponType weapon) {
        return weapon.hasFlag(WeaponType.F_FLAMER)
              || weapon.hasFlag(WeaponType.F_PLASMA)
              || weapon.hasFlag(WeaponType.F_INCENDIARY_NEEDLES);
    }

    /**
     * Crew-based units (a building or a vessel): marines, then the non-combat crew, bay personnel and any
     * passengers, who are taken to be civilians.
     */
    private static MarinePointsBreakdown crewScore(Entity entity, double buildingModifier) {
        int crew;
        int bayPersonnel;
        if (entity instanceof AbstractBuildingEntity building) {
            // A building's crew count for nothing until the defender commits them, and the action's casualties come
            // off the committed crew, so the committed and living are what it counts (TO:AR pp. 169 to 172)
            crew = building.getCommittedCrew();
            bayPersonnel = 0;
        } else {
            crew = entity.getNCrew();
            bayPersonnel = entity.getBayPersonnel();
        }
        double score = (entity.getNMarines() * MARINE)
              + (crew * NON_COMBAT_CREW)
              + (bayPersonnel * NON_COMBAT_CREW)
              + (entity.getNPassenger() * CIVILIAN);
        return MarinePointsBreakdown.crewed(entity.getNMarines(), crew, bayPersonnel, entity.getNPassenger(), score,
              buildingModifier);
    }

    /**
     * The multiplier from the Building Modifiers Table (TO:AR p. 171): {@code 0.1} per six levels of a standard
     * building, per three levels of a fortress or gun emplacement, and nothing for a hangar, counting only the
     * levels beyond the first that are made of at least sixty hexes.
     *
     * @param building the defended building, or {@code null} for no building
     *
     * @return the multiplier to apply to the defender's score, {@code 1.0} when nothing applies
     */
    public static double buildingModifier(@Nullable AbstractBuildingEntity building) {
        if (building == null) {
            return 1.0;
        }
        int levelsPerStep = switch (building.getBldgClass()) {
            case IBuilding.HANGAR -> 0;
            case IBuilding.FORTRESS, IBuilding.GUN_EMPLACEMENT -> FORTRESS_LEVELS_PER_STEP;
            default -> STANDARD_LEVELS_PER_STEP;
        };
        if (levelsPerStep == 0) {
            LOGGER.debug("[MarinePoints] {} is a hangar; no building modifier", building.getShortName());
            return 1.0;
        }
        int qualifyingLevels = levelsWithEnoughHexes(building);
        int levelsBeyondTheFirst = Math.max(0, qualifyingLevels - 1);
        double modifier = 1.0 + (MODIFIER_PER_STEP * (levelsBeyondTheFirst / levelsPerStep));
        LOGGER.debug("[MarinePoints] {}: {} levels of {}+ hexes, one step per {} levels beyond the first, modifier {}",
              building.getShortName(), qualifyingLevels, MINIMUM_HEXES_PER_LEVEL, levelsPerStep, modifier);
        return modifier;
    }

    /** Counts the levels, from the ground up, that at least {@value #MINIMUM_HEXES_PER_LEVEL} hexes reach. */
    private static int levelsWithEnoughHexes(AbstractBuildingEntity building) {
        Building footprint = building.getInternalBuilding();
        int qualifyingLevels = 0;
        for (int level = 1; ; level++) {
            int hexesReachingLevel = 0;
            for (CubeCoords coords : footprint.getCoordsList()) {
                if (footprint.getHeight(coords) >= level) {
                    hexesReachingLevel++;
                }
            }
            if (hexesReachingLevel < MINIMUM_HEXES_PER_LEVEL) {
                return qualifyingLevels;
            }
            qualifyingLevels++;
        }
    }
}
