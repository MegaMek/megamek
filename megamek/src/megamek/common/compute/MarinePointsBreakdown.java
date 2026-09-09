/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

/**
 * How one unit's Marine Points Score was arrived at (TO:AR pp. 170 to 171), so a report can show the working
 * rather than only the total. Which fields carry meaning depends on the {@link #kind()}: a platoon has troopers at a
 * value each; a battle armor squad adds weight-class and equipment modifiers per trooper and its intact armor; a
 * crewed unit or building counts marines, crew, bay personnel and civilians.
 *
 * @param kind                the shape of the calculation
 * @param headCount           troopers for infantry and battle armor; marines, crew, bay personnel and civilians
 *                            together for a crewed unit
 * @param baseValue           the value of one trooper before modifiers, {@code 0} for a crewed unit
 * @param weightClassModifier the Battle Armor Modifiers Table weight-class row per trooper, {@code 0} otherwise
 * @param equipmentModifier   the "mounts one or more" rows of that table per trooper, {@code 0} otherwise
 * @param perTrooper          base value plus both modifiers, what each trooper is worth
 * @param intactArmor         the squad's intact armor points, {@code 0} for anything else
 * @param armorPoints         the Marine Points those armor points add
 * @param marines             marines aboard a crewed unit
 * @param crew                non-combat crew aboard a crewed unit
 * @param bayPersonnel        bay personnel aboard a crewed unit
 * @param civilians           passengers, taken to be civilians
 * @param score               the unit's score before any building modifier
 * @param buildingModifier    the Building Modifiers Table multiplier applied, {@code 1.0} for none
 * @param modifiedScore       the score after the building modifier, what the side total uses
 */
public record MarinePointsBreakdown(Kind kind, int headCount, double baseValue, double weightClassModifier,
      double equipmentModifier, double perTrooper, int intactArmor, double armorPoints, int marines, int crew,
      int bayPersonnel, int civilians, double score, double buildingModifier, double modifiedScore) {

    /** The three shapes the Marine Points Tables give a unit. */
    public enum Kind {
        /** Conventional infantry: troopers at a value each. */
        CONVENTIONAL_INFANTRY,
        /** Battle armor: troopers at a base plus modifiers each, and their intact armor. */
        BATTLE_ARMOR,
        /** A building or vessel: marines, crew, bay personnel and civilians at their own values. */
        CREW
    }

    /** Nobody to count: the breakdown of a {@code null} or empty unit. */
    static final MarinePointsBreakdown NOBODY = new MarinePointsBreakdown(Kind.CREW, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 1.0, 0);

    /**
     * A platoon of conventional infantry.
     *
     * @param troopers   the troopers standing
     * @param perTrooper the value of each, marine or not, armored or not
     */
    static MarinePointsBreakdown conventionalInfantry(int troopers, double perTrooper, double buildingModifier) {
        double score = troopers * perTrooper;
        return new MarinePointsBreakdown(Kind.CONVENTIONAL_INFANTRY, troopers, perTrooper, 0, 0, perTrooper, 0, 0,
              0, 0, 0, 0, score, buildingModifier, Math.max(0, score * buildingModifier));
    }

    /**
     * A battle armor squad.
     *
     * @param troopers            the troopers still active
     * @param baseValue           Elemental or Inner Sphere trooper value
     * @param weightClassModifier the weight-class row per trooper
     * @param equipmentModifier   the equipment rows per trooper
     * @param intactArmor         the intact armor points on the active troopers
     * @param armorPoints         what that armor is worth in Marine Points
     */
    static MarinePointsBreakdown battleArmor(int troopers, double baseValue, double weightClassModifier,
          double equipmentModifier, int intactArmor, double armorPoints, double buildingModifier) {
        double perTrooper = baseValue + weightClassModifier + equipmentModifier;
        double score = (troopers * perTrooper) + armorPoints;
        return new MarinePointsBreakdown(Kind.BATTLE_ARMOR, troopers, baseValue, weightClassModifier,
              equipmentModifier, perTrooper, intactArmor, armorPoints, 0, 0, 0, 0, score, buildingModifier,
              Math.max(0, score * buildingModifier));
    }

    /**
     * A crewed unit or building.
     *
     * @param score the marines, crew, bay personnel and civilians at their values, already added up
     */
    static MarinePointsBreakdown crewed(int marines, int crew, int bayPersonnel, int civilians, double score,
          double buildingModifier) {
        return new MarinePointsBreakdown(Kind.CREW, marines + crew + bayPersonnel + civilians, 0, 0, 0, 0, 0, 0,
              marines, crew, bayPersonnel, civilians, score, buildingModifier, Math.max(0, score * buildingModifier));
    }

    /** @return {@code true} when the building modifier changed the score */
    public boolean hasBuildingModifier() {
        return buildingModifier != 1.0;
    }
}
