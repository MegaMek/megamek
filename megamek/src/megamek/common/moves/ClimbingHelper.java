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
package megamek.common.moves;

import megamek.common.Hex;
import megamek.common.Messages;
import megamek.common.board.Coords;
import megamek.common.equipment.MiscMounted;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Terrains;

/**
 * Utility class for Mek climbing rules (TO:AR p.20).
 *
 * <p>Climbing allows a Mek to enter a hex that is 3 or more levels higher or lower
 * than the hex it occupies, provided it has at least one arm with all four actuators
 * functional and the hand free. Only walking MP can be used when climbing.</p>
 */
public final class ClimbingHelper {

    /** MP cost per level when climbing with two functional hands. */
    public static final int MP_COST_TWO_HANDS = 2;

    /** MP cost per level when climbing with one functional hand. */
    public static final int MP_COST_ONE_HAND = 3;

    /** PSR modifier applied to all piloting rolls while climbing. */
    public static final int CLIMBING_PSR_MODIFIER = 1;

    /** Additional PSR modifier when climbing with only one functional arm. */
    public static final int ONE_ARM_PSR_MODIFIER = 2;

    /** To-hit modifier applied against a climbing target (easier to hit). */
    public static final int TARGET_CLIMBING_MODIFIER = -2;

    /** Number of levels lowered per Dangle-and-Drop turn. */
    public static final int DANGLE_LEVELS_PER_TURN = 2;

    /** MP cost for dropping from a dangle position. */
    public static final int DROP_MP_COST = 4;

    /** Minimum level difference that requires climbing or dangle-and-drop. */
    public static final int MIN_CLIMBING_LEVELS = 3;

    private ClimbingHelper() {
        // Utility class - no instantiation
    }

    /**
     * Checks if an entity is at the edge of a cliff or building roof, with an adjacent hex that is 3+ levels lower.
     * Returns the level difference to the lowest adjacent hex in the given direction, or 0 if not at an edge.
     *
     * @param entity       the entity to check
     * @param targetCoords the adjacent hex to check (the potential lower hex)
     * @param game         the current game
     *
     * @return the level difference (positive = entity is higher), or 0 if not an edge
     */
    public static int getEdgeDropHeight(Entity entity, Coords targetCoords, Game game) {
        if ((entity == null) || (targetCoords == null) || (game == null)) {
            return 0;
        }
        Hex entityHex = game.getBoard(entity).getHex(entity.getPosition());
        Hex targetHex = game.getBoard(entity).getHex(targetCoords);
        if ((entityHex == null) || (targetHex == null)) {
            return 0;
        }
        int entityAlt = entityHex.getLevel() + entity.getElevation();
        // The drop's destination is the highest surface the Mek would land on in the target
        // hex — building roof if present, bridge surface if present, otherwise the hex floor
        // (water bottom for water hexes, basement floor for basements, hex level for dry).
        // Pre-fix this measured only to hex.getLevel(), so stepping off a bridge into adjacent
        // water:2 reported drop=2 (below the 3-level edge threshold) instead of the actual
        // drop=4 down to the water floor — and the edge-descent dialog never fired.
        int targetAlt;
        if (targetHex.containsTerrain(Terrains.BUILDING)) {
            targetAlt = targetHex.getLevel() + targetHex.terrainLevel(Terrains.BLDG_ELEV);
        } else if (targetHex.containsTerrain(Terrains.BRIDGE)) {
            targetAlt = targetHex.getLevel() + targetHex.terrainLevel(Terrains.BRIDGE_ELEV);
        } else {
            targetAlt = targetHex.floor();
        }
        int levelDiff = entityAlt - targetAlt;
        return Math.max(0, levelDiff);
    }

    /**
     * Returns true if the entity is at the edge of a cliff or building roof, with the target hex being 3+ levels
     * lower.
     *
     * @param entity       the entity to check
     * @param targetCoords the adjacent hex to check
     * @param game         the current game
     *
     * @return true if the entity is at a climbable/dangleable edge
     */
    public static boolean isAtEdge(Entity entity, Coords targetCoords, Game game) {
        return getEdgeDropHeight(entity, targetCoords, game) >= MIN_CLIMBING_LEVELS;
    }

    /**
     * Returns true if the entity is standing on a building roof.
     *
     * @param entity the entity to check
     * @param game   the current game
     *
     * @return true if the entity is on a building roof
     */
    public static boolean isOnBuildingRoof(Entity entity, Game game) {
        if ((entity == null) || (game == null)) {
            return false;
        }
        Hex hex = game.getBoard(entity).getHex(entity.getPosition());
        if ((hex == null) || !hex.containsTerrain(Terrains.BUILDING)) {
            return false;
        }
        return entity.getElevation() >= hex.terrainLevel(Terrains.BLDG_ELEV);
    }

    /**
     * Checks whether a specific arm on a Mek has all four actuators functional and
     * the hand is free (not holding a physical weapon or carried object).
     *
     * @param mek the Mek to check
     * @param location the arm location ({@link Mek#LOC_LEFT_ARM} or {@link Mek#LOC_RIGHT_ARM})
     * @return true if the arm is fully functional and hand is free
     */
    public static boolean isArmClimbCapable(Mek mek, int location) {
        if ((location != Mek.LOC_LEFT_ARM) && (location != Mek.LOC_RIGHT_ARM)) {
            return false;
        }

        boolean allActuatorsFunctional = mek.hasWorkingSystem(Mek.ACTUATOR_SHOULDER, location)
              && mek.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, location)
              && mek.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, location)
              && mek.hasWorkingSystem(Mek.ACTUATOR_HAND, location);

        if (!allActuatorsFunctional) {
            return false;
        }

        // Check for carried objects in this arm
        if (mek.getCarriedObjects().containsKey(location)) {
            return false;
        }

        // Check for physical weapons (clubs/hatchets/swords) mounted in this arm
        for (MiscMounted club : mek.getClubs()) {
            if ((club.getLocation() == location) && !club.isDestroyed()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the number of arms (0, 1, or 2) eligible for climbing on the given Mek.
     *
     * @param mek the Mek to check
     * @return the number of climbable arms
     */
    public static int countClimbableArms(Mek mek) {
        int count = 0;
        if (isArmClimbCapable(mek, Mek.LOC_LEFT_ARM)) {
            count++;
        }
        if (isArmClimbCapable(mek, Mek.LOC_RIGHT_ARM)) {
            count++;
        }
        return count;
    }

    /**
     * Returns true if the given entity is a Mek capable of climbing (TO:AR p.20).
     * Requires at least one arm with all four actuators functional and hand free,
     * and the unit must not be prone or shut down.
     *
     * @param entity the entity to check
     * @return true if this entity can climb
     */
    public static boolean canClimb(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return false;
        }
        return canClimb(entity, mek.isProne());
    }

    /**
     * Path-aware variant of {@link #canClimb(Entity)} that evaluates against the supplied prone state rather than the
     * entity's live prone state. Used during MovePath compilation so a GET_UP step earlier in the path lets the
     * subsequent climbing branch engage — even though the entity itself is still prone on the board until the turn
     * commits.
     *
     * @param entity  the entity to check
     * @param isProne the per-step prone state to evaluate against
     *
     * @return true if this entity can climb given the supplied prone state
     */
    public static boolean canClimb(Entity entity, boolean isProne) {
        if (!(entity instanceof Mek mek)) {
            return false;
        }
        if (mek.isSuperHeavy()) {
            return false;
        }
        return (countClimbableArms(mek) >= 1) && !isProne && !mek.isShutDown();
    }

    /**
     * Returns a human-readable reason why the entity cannot climb, or null if climbing is possible.
     *
     * @param entity the entity to check
     * @return the reason climbing is impossible, or null if climbing is allowed
     */
    public static String getClimbingImpossibleReason(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return Messages.getString("ClimbingHelper.onlyMeksClimb");
        }
        if (mek.isSuperHeavy()) {
            return Messages.getString("ClimbingHelper.superheavyNoClimb", mek.getDisplayName());
        }
        if (mek.isProne()) {
            return Messages.getString("ClimbingHelper.proneNoClimb", mek.getDisplayName());
        }
        if (mek.isShutDown()) {
            return Messages.getString("ClimbingHelper.shutdownNoClimb", mek.getDisplayName());
        }
        int climbableArms = countClimbableArms(mek);
        if (climbableArms == 0) {
            return Messages.getString("ClimbingHelper.noArmsClimb", mek.getDisplayName());
        }
        return null;
    }

    /**
     * Returns true if the given entity can perform a Dangle-and-Drop maneuver (TO:AR p.20). Requires two arms with all
     * four actuators functional and hands free.
     *
     * @param entity the entity to check
     *
     * @return true if this entity can dangle
     */
    public static boolean canDangle(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return false;
        }
        if (mek.isSuperHeavy()) {
            return false;
        }
        return (countClimbableArms(mek) >= 2) && !mek.isProne() && !mek.isShutDown();
    }

    /**
     * Returns a human-readable reason why the entity cannot dangle, or null if dangling is possible.
     *
     * @param entity the entity to check
     *
     * @return the reason dangling is impossible, or null if dangling is allowed
     */
    public static String getDangleImpossibleReason(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return Messages.getString("ClimbingHelper.onlyMeksDangle");
        }
        if (mek.isSuperHeavy()) {
            return Messages.getString("ClimbingHelper.superheavyNoDangle", mek.getDisplayName());
        }
        if (mek.isProne()) {
            return Messages.getString("ClimbingHelper.proneNoDangle", mek.getDisplayName());
        }
        if (mek.isShutDown()) {
            return Messages.getString("ClimbingHelper.shutdownNoDangle", mek.getDisplayName());
        }
        int climbableArms = countClimbableArms(mek);
        if (climbableArms < 2) {
            return Messages.getString("ClimbingHelper.noArmsDangle", mek.getDisplayName());
        }
        return null;
    }

    /**
     * Returns the MP cost per level climbed for the given Mek (TO:AR p.20).
     * 2 MP per level with two functional hands, 3 MP per level with one.
     *
     * @param mek the Mek to check
     * @return the MP cost per level of climbing
     */
    public static int getClimbingMPCostPerLevel(Mek mek) {
        return (countClimbableArms(mek) >= 2) ? MP_COST_TWO_HANDS : MP_COST_ONE_HAND;
    }

    /**
     * Returns the absolute level of the top climbable surface in the given hex — the building roof,
     * the bridge surface, or the bare hex level if neither is present. Used by the climbing dialog
     * to compute remaining levels of an in-progress climb; without the bridge branch a continuation
     * climb toward a bridge hex measures only to the bare hex level (the water/ground beneath the
     * bridge), reports 0 levels remaining, and skips the dialog entirely.
     *
     * @param hex the hex whose climbable top to measure
     *
     * @return the absolute level of the building roof, bridge surface, or bare hex level
     */
    public static int getClimbDestinationLevel(Hex hex) {
        int level = hex.getLevel();
        if (hex.containsTerrain(Terrains.BUILDING)) {
            return level + hex.terrainLevel(Terrains.BLDG_ELEV);
        }
        if (hex.containsTerrain(Terrains.BRIDGE)) {
            return level + hex.terrainLevel(Terrains.BRIDGE_ELEV);
        }
        return level;
    }

    /**
     * Same as {@link #getClimbDestinationLevel(Hex)} but expressed as elevation relative to the hex's own level — so a
     * building roof returns {@code BLDG_ELEV}, a bridge surface returns {@code BRIDGE_ELEV}, and a bare hex (cliff top,
     * dry ground) returns 0. Used by {@link megamek.common.moves.MoveStep#compile} to place a continuation-climbing Mek
     * on the destination hex's top surface, bypassing the water-emergence math in
     * {@link megamek.common.units.Entity#calcElevation} that doesn't apply when the Mek is on a cliff face above the
     * waterline.
     *
     * @param hex the destination hex
     *
     * @return the elevation of the climbable top surface, relative to the hex's own level
     */
    public static int getClimbDestinationElevation(Hex hex) {
        return getClimbDestinationLevel(hex) - hex.getLevel();
    }
}
