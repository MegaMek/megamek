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
package megamek.common;

import megamek.client.bot.princess.MinefieldUtil;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.pathfinder.MovementType;
import megamek.common.units.Terrains;

/**
 * The extra pathfinding costs a single {@link BulldozerMovePath} step incurs beyond its raw MP: the cost to
 * "level" impassable-but-destructible terrain in the way, plus additional costs that slow the unit without
 * raising its MP total (wading into deep water, jumping into water, or crossing a minefield). This logic used
 * to be inlined in {@link BulldozerMovePath#addStep}; it is pulled out here so it is cohesive and
 * unit-testable, and so the (previously dead) deep-water cost can be exercised in isolation.
 *
 * @param levelingCost   MP-equivalent cost to level destructible terrain at the step's hex, or
 *                       {@link BulldozerMovePath#CANNOT_LEVEL} when no leveling is needed or possible
 * @param additionalCost extra cost that slows the move without raising MP (deep water, minefield),
 *                       {@code 0} if none
 */
public record MovePathTerrainCost(int levelingCost, int additionalCost) {

    /**
     * Computes the leveling and additional costs incurred by the most recently added step of the given path.
     *
     * @param path the bulldozer path whose final step is being costed
     *
     * @return the {@link MovePathTerrainCost} for that step
     */
    public static MovePathTerrainCost forFinalStep(BulldozerMovePath path) {
        Coords finalCoords = path.getFinalCoords();
        Hex hex = path.getGame().getBoard(path.getFinalBoardId()).getHex(finalCoords);
        int waterDepth = ((hex != null) && hex.containsTerrain(Terrains.WATER)) ? hex.depth() : Integer.MIN_VALUE;

        int levelingCost = BulldozerMovePath.CANNOT_LEVEL;
        if (!path.isMoveLegal() && !path.isJumping()) {
            // The step is illegal only because the terrain would need to be "reduced"; record what leveling
            // it would take to get through. Not relevant for jumping paths.
            levelingCost = BulldozerMovePath.calculateLevelingCost(finalCoords, path.getEntity());
        }

        int additionalCost;
        if (path.isJumping()) {
            additionalCost = jumpIntoWaterCost(hex, waterDepth, path);
        } else {
            additionalCost = wadingCost(MovementType.getMovementType(path.getEntity()), waterDepth,
                  path.getCachedEntityState().getRunMP(), path.getCachedEntityState().getWalkMP());
        }

        // A minefield in the hex overrides the water cost for this step, preserving the original
        // put-overwrite ordering of BulldozerMovePath.addStep.
        double minefieldFactor = MinefieldUtil.calcMinefieldHazardForHex(path.getLastStep(), path.getEntity(),
              path.isJumping(), false);
        if (minefieldFactor > 0) {
            additionalCost = (int) Math.ceil(minefieldFactor);
        }

        return new MovePathTerrainCost(levelingCost, additionalCost);
    }

    /**
     * Cost of a ground Mek or amphibious vehicle wading into a Depth 1+ water hex. Entering deep water costs
     * extra MP (1 for Depth 1, 3 for Depth 2 or deeper), forbids running, and forces a Piloting Skill Roll
     * (TW p.49), none of which the raw MP total captures. Charge the forfeited running speed plus a per-depth
     * penalty so the A* prefers a Depth 0 ford or a land route over wading straight across (issue #7627).
     *
     * @param movementType the unit's {@link MovementType}
     * @param waterDepth   the destination hex's water depth ({@code <= 0} means no deep water)
     * @param runMP        the unit's running MP
     * @param walkMP       the unit's walking MP
     *
     * @return the extra cost, or {@code 0} if this unit type is not slowed by wading
     */
    static int wadingCost(MovementType movementType, int waterDepth, int runMP, int walkMP) {
        if (waterDepth <= 0) {
            return 0;
        }
        if ((movementType != MovementType.Walker) && (movementType != MovementType.WheeledAmphibious)
              && (movementType != MovementType.TrackedAmphibious)) {
            return 0;
        }
        int forfeitedRunningSpeed = Math.max(0, runMP - walkMP);
        int depthPenalty = (waterDepth >= 2) ? 3 : 1;
        return forfeitedRunningSpeed + depthPenalty;
    }

    /**
     * Extracts the jump-cost inputs from the path's cached entity state and delegates to
     * {@link #jumpIntoWaterCost(int, int, int, boolean)}.
     */
    private static int jumpIntoWaterCost(@Nullable Hex hex, int waterDepth, BulldozerMovePath path) {
        boolean landingOnBridge = (hex != null) && hex.containsTerrain(Terrains.BRIDGE);
        return jumpIntoWaterCost(waterDepth, path.getCachedEntityState().getJumpMP(),
              path.getCachedEntityState().getTorsoJumpJets(), landingOnBridge);
    }

    /**
     * Cost of jumping into a water hex: jump jets are impeded, so future jump movement suffers. Depth 1 costs
     * the jump MP not provided by torso jets; Depth 2 or deeper (full submersion) effectively costs a turn's
     * worth of jump MP while the unit clambers out. Landing on a bridge is free.
     *
     * @param waterDepth      the destination hex's water depth ({@code <= 0} means no deep water)
     * @param jumpMP          the unit's current jump MP (already reduced by heat or damage)
     * @param torsoJumpJets   the number of the unit's jump jets mounted in the torso
     * @param landingOnBridge {@code true} if the step lands on a bridge, which negates the water cost
     *
     * @return the extra cost, never negative
     */
    static int jumpIntoWaterCost(int waterDepth, int jumpMP, int torsoJumpJets, boolean landingOnBridge) {
        if (landingOnBridge) {
            return 0;
        }
        if (waterDepth == 1) {
            // Clamp at 0: heat or jump-jet damage can pull current jump MP below the torso-jet count, and a
            // negative "additional cost" would wrongly make jumping into water look cheaper than staying dry.
            return Math.max(0, jumpMP - torsoJumpJets);
        }
        if (waterDepth > 1) {
            return jumpMP;
        }
        return 0;
    }

    /**
     * @return {@code true} if the step needs (and is able to perform) terrain leveling to be traversable
     */
    public boolean requiresLeveling() {
        return levelingCost > BulldozerMovePath.CANNOT_LEVEL;
    }
}
