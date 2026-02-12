/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

/**
 * Calculates Marine Points Score (MPS) for infantry vs. infantry combat.
 *
 * <p>Based on TOAR Marine Points Table (page 170), this calculator determines
 * the combat strength of infantry units accounting for:
 * <ul>
 *   <li>Base trooper type (Elemental, IS BA, Marine, etc.)</li>
 *   <li>Battle armor weight class and armor points</li>
 *   <li>Mounted equipment (burst-fire weapons, flame weapons, etc.)</li>
 *   <li>Building modifiers for multi-level buildings</li>
 * </ul>
 *
 * <p>This calculator is context-agnostic and can be used for both building
 * clearing and naval boarding actions.</p>
 */
public class MarinePointsScoreCalculator {

    /**
     * Calculate Marine Points Score for an entity.
     * Generic version without building-specific modifiers.
     *
     * @param entity the entity (Infantry or BattleArmor)
     * @return Marine Points Score
     */
    public static int calculateMPS(Entity entity) {
        return calculateMPS(entity, null);
    }

    /**
     * Calculate Marine Points Score for an entity inside a building.
     * Includes building-specific modifiers.
     *
     * @param entity the entity (Infantry or BattleArmor)
     * @param building the building (for building modifier), can be null
     * @return Marine Points Score
     */
    public static int calculateMPS(Entity entity, AbstractBuildingEntity building) {
        if (entity == null) {
            return 0;
        }

        int mps = 0;

        // Calculate based on entity type
        if (entity instanceof BattleArmor ba) {
            mps = calculateBattleArmorMPS(ba);
        } else if (entity instanceof Infantry inf) {
            mps = calculateInfantryMPS(inf);
        } else {
            // For other entities (potential naval vessels), use crew
            mps = calculateCrewMPS(entity);
        }

        // Apply building modifier if applicable
        if (building != null) {
            double buildingMod = calculateBuildingModifier(building);
            mps = (int) Math.round(mps * (1.0 + buildingMod));
        }

        return Math.max(0, mps);
    }

    /**
     * Calculate MPS for Battle Armor squad.
     *
     * @param ba the battle armor entity
     * @return base MPS before building modifier
     */
    private static int calculateBattleArmorMPS(BattleArmor ba) {
        int mps = 0;

        // Base value per trooper (varies by type - simplified here)
        // TODO: Distinguish between Elemental (2 points) and IS BA (1 point) based on type
        int baseTrooperValue = 1;  // Default IS BA trooper value

        // Count active troopers
        int activeTroopers = 0;
        for (int i = 0; i < ba.getTroopers(); i++) {
            if (ba.isTrooperActive(i)) {
                activeTroopers++;
            }
        }
        mps = activeTroopers * baseTrooperValue;

        // Battle Armor weight class modifiers (cumulative per trooper)
        double weight = ba.getWeight();
        int weightMod = 0;
        if (weight <= 400) {
            weightMod = 1;  // PA(L)
        } else if (weight <= 750) {
            weightMod = 2;  // Light
        } else if (weight <= 1000) {
            weightMod = 2;  // Medium
        } else if (weight <= 1500) {
            weightMod = 3;  // Heavy
        } else {
            weightMod = 4;  // Assault
        }
        mps += activeTroopers * weightMod;

        // Armor points modifier
        // Each point of intact armor adds modifier
        // Simplified: Count total remaining armor
        int armorPoints = 0;
        for (int loc = 0; loc < ba.locations(); loc++) {
            armorPoints += ba.getArmor(loc);
        }
        // Each armor point contributes (simplified - actual table has per-weight-class values)
        mps += armorPoints / 2;  // Approximate modifier

        // Equipment modifiers
        mps += calculateEquipmentModifiers(ba);

        return mps;
    }

    /**
     * Calculate MPS for conventional infantry platoon.
     *
     * @param inf the infantry entity
     * @return base MPS before building modifier
     */
    private static int calculateInfantryMPS(Infantry inf) {
        int mps = 0;

        // Check if marines (specialized infantry)
        boolean areMarines = (inf.getSpecializations() & Infantry.MARINES) != 0;

        // Base trooper value
        int baseTrooperValue = areMarines ? 1 : 1;  // Marines and non-marines both 1 in basic table

        // Get active strength
        int activeTroopers = inf.getShootingStrength();
        mps = activeTroopers * baseTrooperValue;

        // Equipment modifiers for infantry weapons
        mps += calculateEquipmentModifiers(inf);

        return mps;
    }

    /**
     * Calculate MPS for crew-based entities (non-infantry/BA).
     *
     * @param entity the entity
     * @return base MPS before building modifier
     */
    private static int calculateCrewMPS(Entity entity) {
        double mps = 0;

        // Get crew composition from entity
        int marines = entity.getNMarines();
        int crew = entity.getNCrew();
        int bayPersonnel = entity.getBayPersonnel();  // Bay crew from transport bays
        int passengers = entity.getNPassenger();

        // Marine Points Table values (page 170):
        // Marines: 1 point each
        // Non-marine soldiers: 0.75 each
        // Crew/pilots: 0.5 each
        // Bay personnel: 0.5 each (non-combat unit crew)
        // Civilians: 0.15 each

        mps += marines * 1.0;           // Marines
        mps += crew * 0.5;             // Crew (officers, enlisted)
        mps += bayPersonnel * 0.5;     // Bay personnel (technicians, bay crew)
        // Passengers assumed to be civilians
        mps += passengers * 0.15;

        return (int) Math.round(mps);
    }

    /**
     * Calculate equipment modifiers for mounted weapons and equipment.
     *
     * @param entity the entity
     * @return equipment modifier points
     */
    private static int calculateEquipmentModifiers(Entity entity) {
        int modifier = 0;

        for (Mounted<?> mounted : entity.getEquipment()) {
            if (mounted.getType() instanceof WeaponType weapon) {
                // Burst-fire weapons: +2
                if (weapon.hasFlag(WeaponType.F_BURST_FIRE)) {
                    modifier += 2;
                }
                // Flame-based weapons: +1
                if (weapon.hasFlag(WeaponType.F_FLAMER) ||
                    weapon.hasFlag(WeaponType.F_PLASMA)) {
                    modifier += 1;
                }
                // TODO: Add other weapon modifiers from Marine Points Table
            } else if (mounted.getType().hasFlag(MiscTypeFlag.F_BA_EQUIPMENT)) {
                // BA-specific equipment modifiers
                // TODO: Identify specific equipment types and apply modifiers
            }
        }

        return modifier;
    }

    /**
     * Calculate building modifier based on building size and height.
     * Only applies to buildings with 60+ hexes.
     *
     * @param building the building entity
     * @return modifier multiplier (e.g., 0.1 per level = 10% bonus)
     */
    private static double calculateBuildingModifier(AbstractBuildingEntity building) {
        if (building == null) {
            return 0.0;
        }

        // Count building hexes
        int hexCount = building.getCoordsList().size();

        // Only applies to buildings with 60+ hexes
        if (hexCount < 60) {
            return 0.0;
        }

        // Get building height (number of levels)
        // Use average height across all hexes
        int totalLevels = 0;
        int hexesWithLevels = 0;
        for (var coords : building.getCoordsList()) {
            int levels = building.getHeight(coords);
            if (levels > 0) {
                totalLevels += levels;
                hexesWithLevels++;
            }
        }

        if (hexesWithLevels == 0) {
            return 0.0;
        }

        int avgLevels = totalLevels / hexesWithLevels;

        // 0.1 modifier per level
        return avgLevels * 0.1;
    }
}
