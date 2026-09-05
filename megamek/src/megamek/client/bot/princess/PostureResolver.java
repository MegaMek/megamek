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
package megamek.client.bot.princess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.board.Coords;
import megamek.logging.MMLogger;

/**
 * Decides each round whether the force is attacking or defending, when the player has left the call to the
 * bot ({@link CombatPosture#AUTO}).
 *
 * <p>The bot reads its posture from two things it already knows. A force that has somewhere to go - a
 * destination edge set in its behavior - is attacking: the mission requires movement, so obstacles are a cost
 * to pay, not a line to hold. Otherwise the enemy's own movement makes the call: an enemy that is closing on
 * the force is coming to fight on our ground, and the right answer is to hold that ground and make them pay
 * for every obstacle on the way in. An enemy holding its distance is not coming, and someone has to make the
 * battle happen, so the force attacks.</p>
 *
 * <p>The closing test compares the enemy's mean distance to our force against where it stood a few rounds
 * ago, so one unit's jockeying does not flip the whole force's stance, and leaving the defensive takes a
 * much weaker signal than entering it ({@link #DEFEND_EXIT_THRESHOLD_HEXES_PER_ROUND}) so the stance does
 * not flip-flop while a mutual approach hovers around the entry threshold. Resolution is cached per round:
 * every unit the bot moves in a round moves under the same posture.</p>
 */
public class PostureResolver {
    private final static MMLogger logger = MMLogger.create(PostureResolver.class);

    /**
     * How fast the enemy's mean distance must be shrinking, in hexes per round, before the force reads it as
     * an advance and stands on the defensive. Below this the enemy is holding or maneuvering in place, and
     * waiting for them wins nobody a game.
     */
    static final double CLOSING_THRESHOLD_HEXES_PER_ROUND = 0.5;

    /**
     * Below this closing rate a force already on the defensive reads the enemy's advance as stopped and goes
     * back over to the attack. Deliberately far below the entry threshold: measured on a 30-game river run,
     * a single threshold left the closing rate hovering around it and the posture flip-flopped round to
     * round, each defend round wrongly discouraging that round's movement. Entering and leaving a stance are
     * different decisions - a defense stands until the assault is spent, not until it slackens.
     */
    static final double DEFEND_EXIT_THRESHOLD_HEXES_PER_ROUND = 0.1;

    /**
     * How many rounds back the closing test looks. Long enough to smooth out one round of jockeying, short
     * enough that a real assault flips the force to the defensive while it still matters.
     */
    static final int CLOSING_WINDOW_ROUNDS = 3;

    private final Map<Integer, Double> meanEnemyDistanceByRound = new HashMap<>();
    private int resolvedRound = -1;
    private int explicitLoggedRound = -1;
    private CombatPosture resolvedPosture = CombatPosture.ATTACK;
    private String resolvedReason = "";

    /**
     * @return why the last {@link #resolve} call answered the way it did, in plain words - for the bot to say
     *       out loud and for the logs
     */
    public String resolutionReason() {
        return resolvedReason;
    }

    /**
     * The posture the force fights under this round.
     *
     * @param settings       the bot's behavior settings; an explicit posture is simply obeyed
     * @param round          the current game round
     * @param ownPositions   positions of the bot's own deployed units
     * @param enemyPositions positions of the enemy units the bot knows about
     *
     * @return {@link CombatPosture#ATTACK} or {@link CombatPosture#DEFEND}, never {@link CombatPosture#AUTO}
     */
    public CombatPosture resolve(BehaviorSettings settings, int round,
          List<Coords> ownPositions, List<Coords> enemyPositions) {
        return resolve(settings, round, ownPositions, enemyPositions, 1);
    }

    /**
     * The posture the force fights under this round, measured at a given map scale.
     *
     * <p>The closing thresholds are written in engagement hexes a round, which for ground units is simply map
     * hexes. Aerospace units over a ground mapsheet are the exception: a hex there is 30 metres against the
     * 500 of the low-altitude map the aircraft is really flying on (TW p.91), so one velocity point carries it
     * sixteen hexes and a Mek-scale rate of half a hex a round says nothing at all. Dividing by the scale puts
     * both on the same footing, so the doctrine reads the same whatever is flying it.</p>
     *
     * @param settings              the bot's behavior settings; an explicit posture is simply obeyed
     * @param round                 the current game round
     * @param ownPositions          positions of the bot's own deployed units
     * @param enemyPositions        positions of the enemy units the bot knows about
     * @param hexesPerEngagementHex map hexes that make up one hex at the scale the fight is fought on
     *
     * @return {@link CombatPosture#ATTACK} or {@link CombatPosture#DEFEND}, never {@link CombatPosture#AUTO}
     */
    public CombatPosture resolve(BehaviorSettings settings, int round,
          List<Coords> ownPositions, List<Coords> enemyPositions, int hexesPerEngagementHex) {
        CombatPosture explicitPosture = settings.getCombatPosture();
        if (CombatPosture.AUTO != explicitPosture) {
            // Obeyed immediately, not cached: a commander's mid-round order applies to every unit that has
            // not moved yet. Still logged once per round, so the log reads the same whoever made the call.
            resolvedReason = "set by my commander";
            if (round != explicitLoggedRound) {
                explicitLoggedRound = round;
                logger.info("[Posture] round {}: {} (set by my commander)", round, explicitPosture.name());
            }
            return explicitPosture;
        }
        if (round == resolvedRound) {
            return resolvedPosture;
        }
        resolvedRound = round;
        resolvedPosture = resolveAuto(settings, round, ownPositions, enemyPositions,
              Math.max(1, hexesPerEngagementHex));
        return resolvedPosture;
    }

    private CombatPosture resolveAuto(BehaviorSettings settings, int round,
          List<Coords> ownPositions, List<Coords> enemyPositions, int hexesPerEngagementHex) {
        // A flee order with a destination edge is a movement mission: the force is going somewhere
        // whatever the enemy does. Both halves are required, matching the engine's own MoveToDestination
        // condition in UnitBehavior: the config dialog stores the flee-edge dropdown even when fleeing is
        // off, so an edge alone is a leftover setting, not a mission - reading it as one locked every such
        // force out of ever defending.
        if (settings.shouldAutoFlee() && settings.shouldGoHome()) {
            return resolved(round, CombatPosture.ATTACK, "the mission requires movement");
        }

        Double meanDistance = meanEnemyDistance(ownPositions, enemyPositions);
        if (null == meanDistance) {
            // No enemy in sight: go find them.
            return resolved(round, CombatPosture.ATTACK, "no enemy in sight");
        }
        meanEnemyDistanceByRound.put(round, meanDistance);
        meanEnemyDistanceByRound.keySet().removeIf(recorded -> recorded < round - CLOSING_WINDOW_ROUNDS);

        int baselineRound = round;
        for (int past = round - CLOSING_WINDOW_ROUNDS; past < round; past++) {
            if (meanEnemyDistanceByRound.containsKey(past)) {
                baselineRound = past;
                break;
            }
        }
        if (baselineRound == round) {
            // First reading: nothing to compare against yet.
            return resolved(round, CombatPosture.ATTACK,
                  String.format("no reading on the enemy yet, mean distance %.1f", meanDistance));
        }

        double baselineDistance = meanEnemyDistanceByRound.get(baselineRound);
        double closingPerRound = (baselineDistance - meanDistance)
              / (double) (round - baselineRound) / hexesPerEngagementHex;

        // Entering and leaving the defensive are different decisions (hysteresis): during a mutual
        // approach the measured closing rate hovers around any single threshold and the posture would
        // flip-flop round to round. A defense, once stood, holds until the advance actually stops.
        if (CombatPosture.DEFEND == resolvedPosture) {
            if (closingPerRound > DEFEND_EXIT_THRESHOLD_HEXES_PER_ROUND) {
                return resolved(round, CombatPosture.DEFEND,
                      String.format("the enemy advance has not stopped (%.1f hexes a round)", closingPerRound));
            }
            return resolved(round, CombatPosture.ATTACK,
                  String.format("the enemy advance has stopped (%.1f hexes a round)", closingPerRound));
        }
        if (closingPerRound >= CLOSING_THRESHOLD_HEXES_PER_ROUND) {
            return resolved(round, CombatPosture.DEFEND,
                  String.format("the enemy is closing %.1f hexes a round", closingPerRound));
        }
        return resolved(round, CombatPosture.ATTACK,
              String.format("the enemy is not coming to us (%.1f hexes a round)", closingPerRound));
    }

    /** Records the reason next to the answer and writes the one {@code [Posture]} log line for the round. */
    private CombatPosture resolved(int round, CombatPosture posture, String reason) {
        resolvedReason = reason;
        logger.info("[Posture] round {}: {} ({})", round, posture.name(), reason);
        return posture;
    }

    /**
     * The enemy's mean distance to the centre of our force, or {@code null} when either side has no known
     * positions.
     */
    private static Double meanEnemyDistance(List<Coords> ownPositions, List<Coords> enemyPositions) {
        Coords ownCentre = FormationGeometry.centroid(ownPositions);
        if ((null == ownCentre) || enemyPositions.isEmpty()) {
            return null;
        }
        double total = 0;
        for (Coords enemyPosition : enemyPositions) {
            total += ownCentre.distance(enemyPosition);
        }
        return total / enemyPositions.size();
    }
}
