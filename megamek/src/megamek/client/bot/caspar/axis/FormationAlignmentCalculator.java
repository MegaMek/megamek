/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.client.bot.common.formation.Formation;
import megamek.common.Coords;
import megamek.common.CubeCoords;
import megamek.common.Entity;

import java.util.Optional;
import java.util.Set;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * Calculates the formation alignment of the unit
 * @author Luana Coppio
 */
public class FormationAlignmentCalculator extends BaseAxisCalculator {

    // TODO -- make a bunch of tests that evaluate the different formation types and if they are doing what they are
    //  supposed to do, I just copied and pasted alot of code from boid formation and I am not sure if it works as
    //  intended

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {

        double[] formationAlignment = axis();
        Entity unit = pathing.getEntity();

        Optional<Formation> formationOpt = gameState.getFormationFor(unit);
        if (formationOpt.isPresent()) {
            Formation formation = formationOpt.get();

            double alignment = switch (formation.getFormationType()) {
                case BOX -> calculateBoxAlignment(formation, pathing);
                case LINE -> calculateLineAlignment(formation, pathing);
                case WEDGE -> calculateWedgeAlignment(formation, pathing);
                case COLUMN -> calculateColumnAlignment(formation, pathing);
                case SCATTERED -> calculateScatteredAlignment(formation, pathing);
            };

            formationAlignment[0] = alignment;
        }

        return formationAlignment;
    }

    /**
     * Calculates alignment for a Box formation.
     * In a box formation, units should maintain similar relative positions
     * and facings in a rectangular pattern around the formation center.
     *
     * @param formation The formation
     * @param pathing The unit's pathing
     * @return Alignment score between 0.0 and 1.0
     */
    private double calculateBoxAlignment(Formation formation, Pathing pathing) {
        Entity unit = pathing.getEntity();
        Entity leader = formation.getLeader();
        Coords finalPosition = pathing.getFinalCoords();
        int unitFinalFacing = pathing.getFinalFacing();
        int leaderFacing = leader.getFacing();
        double facingAlignment = getFacingAlignment(unitFinalFacing, leaderFacing);
        double positionAlignment = calculateBoxPositionAlignment(formation, unit, finalPosition, leader);

        // Combine facing and position alignment
        return (0.4 * facingAlignment) + (0.6 * positionAlignment);
    }

    /**
     * Calculates facing alignment between a unit and the formation leader.
     * @param unitFinalFacing The unit's final facing
     * @param leaderFacing The leader's facing
     * @return Facing alignment score between 0.0 and 1.0, where 1.0 is perfect alignment and 0.0 is opposite
     */
    private static double getFacingAlignment(int unitFinalFacing, int leaderFacing) {
        int facingDiff = Math.min(
              Math.abs(unitFinalFacing - leaderFacing),
              6 - Math.abs(unitFinalFacing - leaderFacing)
        );
        return 1.0 - (facingDiff / 3.0);
    }

    /**
     * Calculates how well a unit is positioned in a box formation.
     * In a box formation, units should maintain a rectangular pattern with:
     * - Units distributed evenly in a rectangular area
     * - Appropriate spacing between units
     * - Maintaining relative positions during movement
     *
     * @param formation The formation
     * @param unit The unit being evaluated
     * @param unitPos The unit's position
     * @param leader The formation leader
     * @return Position alignment score between 0.0 and 1.0
     */
    private double calculateBoxPositionAlignment(Formation formation, Entity unit,
                                                 Coords unitPos, Entity leader) {
        Coords formationCenter = formation.getFormationCenter();
        Coords leaderPos = leader.getPosition();

        // If this is the leader, position them near the center
        if (unit.getId() == leader.getId()) {
            // Leader should be close to formation center
            int distanceToCenter = leaderPos.distance(formationCenter);
            return Math.max(0.3, 1.0 - (distanceToCenter / 4.0));
        }

        // 1. Calculate ideal distribution for a box formation
        // We'll use cube coordinates for more precise spatial calculations
        CubeCoords unitPosCube = unitPos.toCube();
        CubeCoords leaderPosCube = leaderPos.toCube();

        // 2. Calculate relative position from leader
        CubeCoords relativePos = unitPosCube.subtract(leaderPosCube);

        // 3. For a box formation, units should be in a grid pattern
        // The coordinate axes for cube coordinates are 60° apart, but we
        // can check alignment by seeing if a unit is along the main axes

        // Extract components of the relative position
        double q = Math.abs(relativePos.q);
        double r = Math.abs(relativePos.r);
        double s = Math.abs(relativePos.s);

        // 4. Compute how well the unit fits in a box grid
        // In a perfect grid, one of the components would be close to the sum of the other two
        // (This is due to the constraint that q + r + s = 0 in cube coordinates)
        double boxGridAlignment;

        double offGridDistance = Math.min(Math.abs(q - (r + s)), Math.min(Math.abs(r - (q + s)), Math.abs(s - (q + r))));
        if (offGridDistance < 0.5) {
            // Unit is well-aligned with the grid
            boxGridAlignment = 1.0;
        } else {
            // Unit is off-grid, calculate how far off
            boxGridAlignment = clamp01(1.0 - (offGridDistance / 3.0));
        }

        int distanceToLeader = unitPos.distance(leaderPos);
        double spacingFactor = getBoxSpacingFactor(distanceToLeader);

        double symmetryFactor = calculateBoxSymmetryFactor(formation, unit, unitPos);

        // 7. Combine all factors
        return (0.4 * boxGridAlignment) + (0.3 * spacingFactor) + (0.3 * symmetryFactor);
    }
    /**
     * Calculates a true symmetry factor for box formations by evaluating how well
     * units are distributed around the formation center.
     *
     * @param formation The formation
     * @param unit The unit being evaluated
     * @param unitPos The unit's position
     * @return Symmetry factor between 0.0 and 1.0
     */
    private double calculateBoxSymmetryFactor(Formation formation, Entity unit, Coords unitPos) {
        Set<Entity> members = formation.getMembers();
        Coords formationCenter = formation.getFormationCenter();

        // If there are only a few units, symmetry is less meaningful
        if (members.size() < 4) {
            // Simple distance check for small formations
            int distanceToCenter = unitPos.distance(formationCenter);
            return getSimpleDistanceSymmetryFactor(distanceToCenter);
        }

        // 1. Calculate this unit's position relative to formation center
        CubeCoords centerCube = formationCenter.toCube();
        CubeCoords unitCube = unitPos.toCube();
        CubeCoords relativePos = unitCube.subtract(centerCube);

        // 2. Check if there are units in symmetrically opposite positions
        // For true box symmetry, we want units to be balanced on all sides

        // Find the opposite direction vector
        CubeCoords oppositeVector = new CubeCoords(-relativePos.q, -relativePos.r, -relativePos.s);
        double oppositeDistance = oppositeVector.magnitude();

        // If unit is at center, no opposite needed
        if (oppositeDistance < 0.1) {
            return 1.0;
        }

        // 3. Find the closest unit to the ideal opposite position
        double bestOppositeMatch = Double.MAX_VALUE;
        Coords oppositeIdealPos = centerCube.add(oppositeVector).toOffset();

        for (Entity otherUnit : members) {
            if (otherUnit.getId() == unit.getId()) {
                continue;
            }

            Coords otherPos = otherUnit.getPosition();
            int distanceToIdealOpposite = otherPos.distance(oppositeIdealPos);

            bestOppositeMatch = Math.min(bestOppositeMatch, distanceToIdealOpposite);
        }

        // Calculate symmetry score based on how well the opposite position is covered
        double oppositeMatchScore;
        if (bestOppositeMatch <= 1) {
            // Perfect or near-perfect opposite match
            oppositeMatchScore = 1.0;
        } else if (bestOppositeMatch > 5) {
            // No good opposite match
            oppositeMatchScore = 0.0;
        } else {
            // Partial match
            oppositeMatchScore = 1.0 - ((bestOppositeMatch - 1) / 4.0);
        }

        // 4. Check quadrant balance (are units evenly distributed in all directions?)
        int[] quadrantCounts = new int[4];

        // Count units in each quadrant relative to formation center
        for (Entity member : members) {
            if (member.getId() == unit.getId()) {
                continue;
            }

            Coords memberPos = member.getPosition();
            CubeCoords memberCube = memberPos.toCube();
            CubeCoords memberRelative = memberCube.subtract(centerCube);

            // Determine quadrant (simplified)
            int quadrant;
            if (memberRelative.q >= 0 && memberRelative.r >= 0) {
                quadrant = 0; // Northeast
            } else if (memberRelative.q < 0 && memberRelative.r >= 0) {
                quadrant = 1; // Northwest
            } else if (memberRelative.q < 0 && memberRelative.r < 0) {
                quadrant = 2; // Southwest
            } else {
                quadrant = 3; // Southeast
            }

            quadrantCounts[quadrant]++;
        }

        // Calculate evenness of distribution
        double idealQuadrantCount = members.size() / 4.0;
        double quadrantImbalance = 0;

        for (int count : quadrantCounts) {
            quadrantImbalance += Math.abs(count - idealQuadrantCount);
        }

        // Normalize imbalance score (1 = perfectly balanced, 0 = completely imbalanced)
        double maxImbalance = members.size(); // Worst case: all units in one quadrant
        double balanceScore = quadrantImbalance / maxImbalance;

        // 5. Combine opposite matching and quadrant balance
        return (0.6 * oppositeMatchScore) + (0.4 * balanceScore);
    }

    /**
     * Simple distance-based symmetry factor (fallback for small formations)
     * @param distanceToCenter The distance to the formation center
     * @return Symmetry factor between 0.0 and 1.0
     */
    private double getSimpleDistanceSymmetryFactor(int distanceToCenter) {
        if (distanceToCenter > Formation.FormationType.BOX.getMaxDistance()) {
            // Unit is too far from formation center
            return 1.0 - clamp01((distanceToCenter - Formation.FormationType.BOX.getMaxDistance()) / 3.0);
        } else {
            // Unit is within expected distance
            return 1.0;
        }
    }

    /**
     * Calculate the box spacing factor for a unit in a box formation.
     * @param distanceToLeader The distance to the leader
     * @return Spacing factor between 0.0 and 1.0
     */
    private static double getBoxSpacingFactor(int distanceToLeader) {
        double spacingFactor;
        int maxDistanceToLeader = (int) Math.round(Math.sqrt(2 * Math.pow(Formation.FormationType.BOX.getMaxDistance(), 2)));
        // Ideal spacing for box formation - adjust these values based on preferences
        if (distanceToLeader < Formation.FormationType.BOX.getIdealDistance()) {
            // Too close to leader
            spacingFactor = distanceToLeader / (double) Formation.FormationType.BOX.getIdealDistance();
        } else if (distanceToLeader > maxDistanceToLeader) {
            // Too far from leader
            spacingFactor = clamp01(1 - ((distanceToLeader - maxDistanceToLeader) / (double) maxDistanceToLeader));
        } else {
            // Good spacing range
            spacingFactor = 1.0;
        }
        return spacingFactor;
    }

    /**
     * Calculates alignment for a Line formation.
     * In a line formation, units should face the same direction and maintain a line
     * perpendicular to the direction of movement.
     *
     * @param formation The formation
     * @param pathing The unit's pathing
     * @return Alignment score between 0.0 and 1.0
     */
    private double calculateLineAlignment(Formation formation, Pathing pathing) {
        Entity leader = formation.getLeader();
        Entity unit = pathing.getEntity();
        Coords finalPosition = pathing.getFinalCoords();
        int unitFinalFacing = pathing.getFinalFacing();
        int leaderFacing = leader.getFacing();

        double facingAlignmentScore = clamp01(getFacingAlignment(unitFinalFacing, leaderFacing));

        double positionAlignmentScore = clamp01(calculateLinePositionAlignment(formation, unit, finalPosition, leader,
              leaderFacing));

        return (0.4 * facingAlignmentScore) + (0.6 * positionAlignmentScore);
    }

    /**
     * Calculates how well a unit is positioned in a line formation.
     * In a line formation, units should be aligned along a line perpendicular
     * to the leader's facing direction.
     *
     * @param unit The unit being evaluated
     * @param unitPos The unit's position
     * @param leader The formation leader
     * @param leaderFacing The leader's facing
     * @return Position alignment score between 0.0 and 1.0
     */
    private double calculateLinePositionAlignment(Formation formation, Entity unit,
                                                  Coords unitPos, Entity leader, int leaderFacing) {
        if (unit.getId() == leader.getId()) {
            // For line formations, leader should be in the middle of the line
            return calculateLineLeaderPositionAlignment(formation, leader, leader.getPosition(), leaderFacing);
        }

        Coords leaderPos = leader.getPosition();

        // Get direction vectors using the Coords API
        Coords headingDirection = leaderPos.translated(leaderFacing);
        // Calculate perpendicular vector using cube coordinates
        CubeCoords leaderCube = leaderPos.toCube();
        CubeCoords headingCube = headingDirection.toCube();
        CubeCoords headingVector = headingCube.subtract(leaderCube);

        // Calculate perpendicular directions (60° and 300° from heading)
        // In cube coordinates, the six directions are 60° apart
        int perpendicularFacing1 = (leaderFacing + 2) % 6; // 120° clockwise
        int perpendicularFacing2 = (leaderFacing + 4) % 6; // 240° clockwise

        Coords perpendicularPos1 = leaderPos.translated(perpendicularFacing1);
        Coords perpendicularPos2 = leaderPos.translated(perpendicularFacing2);

        // Calculate vectors for the line direction
        CubeCoords perpVector1 = perpendicularPos1.toCube().subtract(leaderCube);
        CubeCoords perpVector2 = perpendicularPos2.toCube().subtract(leaderCube);

        // Vector from leader to unit
        CubeCoords unitVector = unitPos.toCube().subtract(leaderCube);

        // 1. Check alignment with the perpendicular line
        // Calculate projections onto the perpendicular vectors
        double dot1 = perpVector1.dot(unitVector);
        double dot2 = perpVector2.dot(unitVector);

        // Take the maximum alignment (unit should align with one of the perpendicular directions)
        double lineAlignmentFactor = Math.max(
              Math.abs(dot1) / (perpVector1.magnitude() * unitVector.magnitude()),
              Math.abs(dot2) / (perpVector2.magnitude() * unitVector.magnitude())
        );

        // 2. Check for deviation from the line (unit should not be forward/backward of the line)
        // Project onto the heading vector
        double headingDot = (headingVector.q * unitVector.q) + (headingVector.r * unitVector.r) + (headingVector.s * unitVector.s);
        double headingMagnitude = headingVector.magnitude();
        double unitMagnitude = unitVector.magnitude();

        // Calculate normalized deviation from the line
        double lineDeviation = Math.abs(headingDot) / (headingMagnitude * unitMagnitude);
        double linePositionScore = Math.max(0.0, 1.0 - lineDeviation);

        // 3. Check spacing along the line
        int distanceToLeader = unitPos.distance(leaderPos);
        int idealSpacing = Formation.FormationType.LINE.getIdealDistance();
        int minSpacing = Formation.FormationType.LINE.getMinDistance();
        int maxSpacing = Formation.FormationType.LINE.getMaxDistance();
        double spacingFactor;

        if (distanceToLeader <= minSpacing) {
            // Too close to leader
            spacingFactor = 0.3;
        } else if (distanceToLeader > maxSpacing) {
            // Too far from leader
            spacingFactor = clamp01(1.0 - ((distanceToLeader - idealSpacing) / (double) idealSpacing));
        } else {
            // Good spacing
            spacingFactor = 1.0;
        }

        // Combine factors with weights
        return (0.6 * linePositionScore) + (0.2 * lineAlignmentFactor) + (0.2 * spacingFactor);
    }

    /**
     * Calculates position alignment for the leader in a line formation.
     * The leader should ideally be positioned in the middle of the line.
     *
     * @param formation The formation
     * @param leader The leader entity
     * @param leaderPos The leader's position
     * @param leaderFacing The leader's facing
     * @return Position alignment score between 0.0 and 1.0
     */
    private double calculateLineLeaderPositionAlignment(Formation formation, Entity leader,
                                                        Coords leaderPos, int leaderFacing) {
        Set<Entity> members = formation.getMembers();

        // If there are only 1-2 units, leader position doesn't matter much
        if (members.size() <= 2) {
            return 1.0;
        }

        // Calculate perpendicular directions to the leader's facing
        int perpendicularFacing1 = (leaderFacing + 3) % 6; // 180° clockwise
        int perpendicularFacing2 = (leaderFacing + 6) % 6; // 360° clockwise

        // Find the most distant units in both perpendicular directions
        int maxDistance1 = 0;
        int maxDistance2 = 0;

        // Convert to cube coordinates for better calculations
        CubeCoords leaderCube = leaderPos.toCube();

        // Get perpendicular vectors
        Coords perpPos1 = leaderPos.translated(perpendicularFacing1);
        Coords perpPos2 = leaderPos.translated(perpendicularFacing2);
        CubeCoords perpVector1 = perpPos1.toCube().subtract(leaderCube);
        CubeCoords perpVector2 = perpPos2.toCube().subtract(leaderCube);

        for (Entity member : members) {
            if (member.getId() == leader.getId()) {
                continue;
            }

            Coords memberPos = member.getPosition();
            CubeCoords memberCube = memberPos.toCube().subtract(leaderCube);

            // Project the member's position onto both perpendicular vectors
            double dot1 = memberCube.dot(perpVector1) / perpVector1.magnitude();
            double dot2 = memberCube.dot(perpVector2) / perpVector2.magnitude();

            // Update maximum distances in both directions
            if (dot1 > 0) {
                maxDistance1 = Math.max(maxDistance1, memberPos.distance(leaderPos));
            } else if (dot2 > 0) {
                maxDistance2 = Math.max(maxDistance2, memberPos.distance(leaderPos));
            }
        }

        // Calculate balance ratio between the two sides
        double balance;
        if (maxDistance1 == 0 || maxDistance2 == 0) {
            // All units are on one side - poor leader positioning
            balance = 0.1;
        } else {
            // Calculate how balanced the two sides are (1.0 = perfectly balanced)
            double ratio = Math.min(maxDistance1, maxDistance2) / (double) Math.max(maxDistance1, maxDistance2);
            balance = 0.5 + (0.5 * ratio); // Scale to range 0.5-1.0
        }

        return balance;
    }

    /**
     * Calculates alignment for a Wedge formation.
     * In a wedge formation, units should maintain a V formation with the leader at the point.
     *
     * @param formation The formation
     * @param pathing The unit's pathing
     * @return Alignment score between 0.0 and 1.0
     */
    private double calculateWedgeAlignment(Formation formation, Pathing pathing) {
        Entity unit = pathing.getEntity();
        Entity leader = formation.getLeader();

        // Get the final facing and position
        Coords finalPosition = pathing.getFinalCoords();
        int unitFinalFacing = pathing.getFinalFacing();
        int leaderFacing = leader.getFacing();

        // For wedge, facing should be roughly the same as the leader
        int facingDiff = Math.min(
              Math.abs(unitFinalFacing - leaderFacing),
              6 - Math.abs(unitFinalFacing - leaderFacing)
        );

        // Less strict on facing for wedge formations
        double facingAlignment = 1.0 - (facingDiff / 4.0);

        // Calculate position alignment based on wedge formation principles
        double positionAlignment = calculateWedgePositionAlignment(unit, finalPosition, leader, leaderFacing);

        // Combine facing and position alignment
        return (0.5 * facingAlignment) + (0.5 * positionAlignment);
    }

    /**
     * Calculates how well a unit is positioned in a wedge formation.
     * In a wedge formation, units form a V-shape with:
     * - Leader at the front point
     * - Units spread out behind at roughly 45-degree angles on both sides
     *
     * @param unit The unit being evaluated
     * @param unitPos The unit's position
     * @param leader The formation leader
     * @param leaderFacing The leader's facing
     * @return Position alignment score between 0.0 and 1.0
     */
    private double calculateWedgePositionAlignment(Entity unit,
                                                   Coords unitPos, Entity leader, int leaderFacing) {
        Coords leaderPos = leader.getPosition();

        // If this is the leader, they're perfectly positioned by definition
        if (unit.getId() == leader.getId()) {
            return 1.0;
        }

        // Calculate the reverse direction (180° from leader's facing)
        int reverseDirection = (leaderFacing + 3) % 6;

        // Calculate the wedge wing directions (45° offset from reverse)
        // 45° is between hexes, so we'll use directions that approximate this
        int leftWingDirection = (reverseDirection + 1) % 6;
        int rightWingDirection = (reverseDirection + 5) % 6;

        // Get the wing vectors using the Coords API
        Coords leftWingPos = leaderPos.translated(leftWingDirection);
        Coords rightWingPos = leaderPos.translated(rightWingDirection);

        // Convert to cube coordinates for better vector operations
        CubeCoords leaderCube = leaderPos.toCube();
        CubeCoords unitCube = unitPos.toCube();
        CubeCoords leftWingCube = leftWingPos.toCube();
        CubeCoords rightWingCube = rightWingPos.toCube();

        // Calculate wing vectors
        CubeCoords leftWingVector = leftWingCube.subtract(leaderCube);
        CubeCoords rightWingVector = rightWingCube.subtract(leaderCube);

        // Vector from leader to unit
        CubeCoords unitVector = unitCube.subtract(leaderCube);

        // 1. Check if unit is behind leader (using heading vector)
        Coords headingPos = leaderPos.translated(leaderFacing);
        CubeCoords headingVector = headingPos.toCube().subtract(leaderCube);

        // Calculate dot product with heading vector
        double headingDot = (headingVector.q * unitVector.q) +
              (headingVector.r * unitVector.r) +
              (headingVector.s * unitVector.s);

        // Unit should be behind leader (negative dot product with heading)
        if (headingDot >= 0) {
            return 0.2; // Severe penalty for being in front of leader
        }

        // 2. Calculate alignment with wing vectors
        // Normalize vectors for dot product
        double unitMagnitude = unitVector.distanceTo(new CubeCoords(0,0,0));
        double leftWingMagnitude = leftWingVector.distanceTo(new CubeCoords(0,0,0));
        double rightWingMagnitude = rightWingVector.distanceTo(new CubeCoords(0,0,0));

        // Calculate normalized dot products (cosine similarity)
        double leftWingAlignment = ((unitVector.q * leftWingVector.q) +
              (unitVector.r * leftWingVector.r) +
              (unitVector.s * leftWingVector.s)) /
              (unitMagnitude * leftWingMagnitude);

        double rightWingAlignment = ((unitVector.q * rightWingVector.q) +
              (unitVector.r * rightWingVector.r) +
              (unitVector.s * rightWingVector.s)) /
              (unitMagnitude * rightWingMagnitude);

        // Take the maximum alignment (unit should align with one of the wings)
        double wingAlignment = Math.max(leftWingAlignment, rightWingAlignment);

        // Convert to a score (1.0 = perfect alignment)
        double wingAlignmentScore = Math.max(0.0, wingAlignment);

        // 3. Check distance from leader (not too close, not too far)
        int distanceToLeader = unitPos.distance(leaderPos);
        double distanceFactor;

        if (distanceToLeader < 2) {
            // Too close to leader
            distanceFactor = 0.5 * (distanceToLeader / 2.0);
        } else if (distanceToLeader > 8) {
            // Too far from leader
            distanceFactor = Math.max(0.0, 1.0 - ((distanceToLeader - 8.0) / 4.0));
        } else {
            // Good distance range
            distanceFactor = 1.0;
        }

        // Combine scores with emphasis on wing alignment
        return (0.7 * wingAlignmentScore) + (0.3 * distanceFactor);
    }

    /**
     * Calculates alignment for a Column formation.
     * In a column formation, units should follow the leader in a line.
     *
     * @param formation The formation
     * @param pathing The unit's pathing
     * @return Alignment score between 0.0 and 1.0
     */
    private double calculateColumnAlignment(Formation formation, Pathing pathing) {
        Entity unit = pathing.getEntity();
        Entity leader = formation.getLeader();

        // Get the final facing and position
        Coords finalPosition = pathing.getFinalCoords();
        int unitFinalFacing = pathing.getFinalFacing();
        int leaderFacing = leader.getFacing();

        // For column, facing should be the same as the leader
        double facingAlignment = getFacingAlignment(unitFinalFacing, leaderFacing);

        // Calculate position alignment based on how well the unit is positioned in the column
        double positionAlignment = calculateColumnPositionAlignment(formation, unit, finalPosition, leader, leaderFacing);

        // Combine facing and position alignment with emphasis on facing
        return (0.7 * facingAlignment) + (0.3 * positionAlignment);
    }

    /**
     * Calculates how well a unit is positioned in a column formation.
     *
     * @param formation The formation
     * @param unit The unit being evaluated
     * @param unitPos The unit's position
     * @param leader The formation leader
     * @param leaderFacing The leader's facing
     * @return Position alignment score between 0.0 and 1.0
     */
    private double calculateColumnPositionAlignment(Formation formation, Entity unit,
                                                    Coords unitPos, Entity leader, int leaderFacing) {
        Coords leaderPos = leader.getPosition();

        // If this is the leader, they should be at the front of the column
        if (unit.getId() == leader.getId()) {
            // Check if leader is at the front relative to formation center
            Coords formationCenter = formation.getFormationCenter();

            // Leader should be in front of formation center along facing direction
            Coords reversePos = leaderPos.translated((leaderFacing + 3) % 6);
            boolean leaderInFront = formationCenter.distance(reversePos) < formationCenter.distance(leaderPos);

            return leaderInFront ? 1.0 : 0.7;
        }

        // 1. Check if unit is behind leader along the leader's facing direction
        // Get reverse direction of leader facing (where the column should extend)
        int columnDirection = (leaderFacing + 3) % 6;

        // Get the column line using Coords API
        Coords columnLinePos = leaderPos.translated(columnDirection);

        // Convert to cube coordinates for vector operations
        CubeCoords leaderCube = leaderPos.toCube();
        CubeCoords unitCube = unitPos.toCube();
        CubeCoords columnLineCube = columnLinePos.toCube();

        // Calculate vectors
        CubeCoords columnVector = columnLineCube.subtract(leaderCube);
        CubeCoords unitVector = unitCube.subtract(leaderCube);

        // 2. Check alignment with column direction
        double columnMagnitude = columnVector.magnitude();
        double unitMagnitude = unitVector.magnitude();

        // Calculate dot product (alignment with column direction)
        double columnAlignment = ((unitVector.q * columnVector.q) +
              (unitVector.r * columnVector.r) +
              (unitVector.s * columnVector.s)) /
              (columnMagnitude * unitMagnitude);

        // Column extends behind leader, so alignment should be positive
        if (columnAlignment <= 0) {
            // Unit is in front of or beside the leader, not behind
            return 0.3; // Poor alignment
        }

        // 3. Calculate cross product magnitude to determine distance from column line
        double crossMagnitude = unitVector.getCrossMagnitude(columnVector);

        // Normalize by the magnitudes to get the sine of the angle
        double normalizedCross = crossMagnitude / (unitMagnitude * columnMagnitude);

        double lineAlignment = clamp01(normalizedCross);
        int distanceToLeader = unitPos.distance(leaderPos);
        double distanceFactor;

        // Check if the unit is at a good distance
        int formationSize = formation.getMembers().size();
        int idealSpacing = Formation.FormationType.COLUMN.getIdealDistance();
        int maxSpacing = formationSize * idealSpacing;

        if (distanceToLeader < idealSpacing) {
            // Too close to leader
            distanceFactor = 0.3;
        } else if (distanceToLeader > maxSpacing) {
            // Too far from leader
            distanceFactor = clamp01(1.0 - ((distanceToLeader - maxSpacing) / (double) maxSpacing));
        } else {
            // Good distance
            distanceFactor = 1.0;
        }

        // Combine scores with emphasis on line alignment
        return (0.7 * lineAlignment) + (0.3 * distanceFactor);
    }

    /**
     * Calculates alignment for a Scattered formation.
     * In a scattered formation, alignment is less important than maintaining distance
     * and coverage.
     *
     * @param formation The formation
     * @param pathing The unit's pathing
     * @return Alignment score between 0.0 and 1.0
     */
    private double calculateScatteredAlignment(Formation formation, Pathing pathing) {
        Entity unit = pathing.getEntity();
        Entity leader = formation.getLeader();

        // Get the final facing and position
        Coords finalPosition = pathing.getFinalCoords();
        int unitFinalFacing = pathing.getFinalFacing();
        int leaderFacing = leader.getFacing();

        // For scattered formations, facing is less important
        int facingDiff = Math.min(
              Math.abs(unitFinalFacing - leaderFacing),
              6 - Math.abs(unitFinalFacing - leaderFacing)
        );

        // More lenient alignment for scattered formation
        double facingAlignment = Math.max(0.5, 1.0 - (facingDiff / 6.0));

        // Calculate position score based on scattered formation principles
        double positionAlignment = calculateScatteredPositionAlignment(formation, unit, finalPosition);

        // Combine facing and position alignment with emphasis on position for scattered formations
        return (0.3 * facingAlignment) + (0.7 * positionAlignment);
    }

    /**
     * Calculates how well a unit is positioned in a scattered formation.
     * In a scattered formation, units should:
     * - Maintain minimum distance from each other
     * - Cover a wide area
     * - Not bunch up together
     *
     * @param formation The formation
     * @param unit The unit being evaluated
     * @param unitPos The unit's position
     * @return Position alignment score between 0.0 and 1.0
     */
    private double calculateScatteredPositionAlignment(Formation formation, Entity unit, Coords unitPos) {
        Set<Entity> members = formation.getMembers();
        Coords formationCenter = formation.getFormationCenter();

        // 1. Check distance to other units (should be scattered)
        double minDistanceToOther = Double.MAX_VALUE;
        for (Entity other : members) {
            if (other.getId() == unit.getId()) {
                continue;
            }

            int distance = unitPos.distance(other.getPosition());
            minDistanceToOther = Math.min(minDistanceToOther, distance);
        }

        // Units should maintain some minimum distance in a scattered formation
        double separationFactor;
        if (minDistanceToOther < 3) {
            // Too close to another unit
            separationFactor = Math.max(0.0, minDistanceToOther / 3.0);
        } else {
            // Good separation
            separationFactor = 1.0;
        }

        // 2. Check coverage (distance from formation center)
        int distanceToCenter = unitPos.distance(formationCenter);
        double coverageFactor = getCoverageFactor(distanceToCenter);

        // 3. Check distribution (evaluate sector coverage)
        // For scattered formations, units should be distributed in different directions

        // Count units in each of the 6 directional sectors from formation center
        int[] sectorCounts = new int[6];

        // Determine which sector this unit is in
        int unitSector = -1;
        if (distanceToCenter > 0) {
            unitSector = formationCenter.direction(unitPos);
        }

        // Count units in each sector
        for (Entity member : members) {
            if (member.getId() == unit.getId()) {
                continue;
            }

            Coords memberPos = member.getPosition();
            int distance = formationCenter.distance(memberPos);

            if (distance > 0) {
                int sector = formationCenter.direction(memberPos);
                sectorCounts[sector]++;
            }
        }

        // Calculate distribution factor
        double distributionFactor;
        if (unitSector >= 0) {
            // Prefer sectors with fewer units
            distributionFactor = 1.0 - (Math.min(sectorCounts[unitSector], 3) / 3.0) * 0.5;
        } else {
            // Unit is at center
            distributionFactor = 0.5;
        }

        // Combine scores for scattered formation
        return (0.4 * separationFactor) + (0.3 * coverageFactor) + (0.3 * distributionFactor);
    }

    private static double getCoverageFactor(int distanceToCenter) {
        double coverageFactor;

        // Calculate ideal scattered radius based on formation size
        double idealRadius = Formation.FormationType.SCATTERED.getIdealDistance();
        int minDistance = Formation.FormationType.SCATTERED.getMinDistance();
        if (distanceToCenter > idealRadius) {
            // Too far from formation center
            coverageFactor = 1 - clamp01((distanceToCenter - idealRadius) / idealRadius);
        } else if (distanceToCenter < minDistance) {
            // Too close to formation center
            coverageFactor = distanceToCenter / (double) minDistance;
        } else {
            // Good coverage
            coverageFactor = 1.0;
        }
        return coverageFactor;
    }
}
