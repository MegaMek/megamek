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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.AerospaceGeometry.AltitudeBand;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.MMConstants;
import megamek.common.compute.ComputeArc;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.ManeuverType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IAero;

/**
 * Movement doctrine for aerospace units flying in an atmosphere.
 *
 * <p>The stock ranker prices distance, facing and cover on a flat board. None of that decides an air-to-air
 * engagement. What decides it is altitude: two fighters more than a hex or so apart vertically cannot shoot
 * at each other at all until they are far enough apart horizontally to clear the dead zone (TW p.241), and
 * over a ground mapsheet "far enough" means seventeen hexes for one level of separation and thirty-three for
 * two. A flight that spreads across altitudes fights as a collection of singletons that cannot help each
 * other, and a flight holding the wrong altitude cannot fight at all.</p>
 *
 * <h2>Certainty is the other half</h2>
 *
 * <p>Matching an altitude is only worth anything if the opponent is still going to be at it. Aerospace units
 * move last and as their own turn class, interleaved between players, so when this ranker runs some enemy
 * fighters have committed to an altitude this turn and some have not. The ones that have are worth reacting
 * to; the ones that have not are a guess. Every credit below is weighted by which it is.</p>
 *
 * <p>That distinction does not exist in the stock ranker, which reports <i>every</i> airborne aero on a
 * ground map as already moved whether it has or not - see {@link #evaluateAsMoved}.</p>
 */
public class AerospacePathRanker extends BasicPathRanker {

    /**
     * How much of an enemy's damage potential an engageable pose is credited with.
     *
     * <p>Deliberately a fraction: being able to shoot is worth less than the damage estimate the bravery
     * term already books for a path that actually shoots. This credit exists to break ties between poses the
     * damage estimate cannot tell apart, which on a ground map is most of them - out at aerospace ranges
     * nearly every hex is "in range", and altitude is what actually decides the shot.</p>
     */
    private static final double ENGAGEMENT_WEIGHT = 0.35;

    /**
     * What an enemy that has not moved yet is worth relative to one that has.
     *
     * <p>Not zero, because an unmoved opponent is still probably somewhere near where it is now, and not one,
     * because it is about to move and may well leave. Half says: react to what has committed, hedge toward
     * what has not.</p>
     */
    private static final double UNMOVED_ENEMY_CONFIDENCE = 0.5;

    /** Credit for holding an arc the target cannot answer, as a fraction of the engagement credit. */
    private static final double ARC_ADVANTAGE_WEIGHT = 0.25;

    /**
     * Credit for a path that overflies an enemy ground unit inside the legal attack window.
     *
     * <p>Weighted above the air-to-air engagement credit because an overflight is not an opportunity, it
     * is the whole attack: every air-to-ground weapon requires the target's hex on this turn's flown
     * line ({@code passedOver}), ground units have all moved by the time a fighter plans, and the flown
     * hexes reset every round. Miss the overflight and there is no attack this turn, full stop. Measured
     * without this term: two unopposed fighters averaged one bombing pass every fourteen rounds.</p>
     */
    private static final double ATTACK_RUN_WEIGHT = 0.6;

    /** Strike attacks (guns, air-to-ground) are impossible above this altitude. */
    private static final int STRIKE_MAX_ALTITUDE = 5;

    /** A dive-bomb attack costs the attacker this many altitude levels (TW). */
    private static final int DIVE_BOMB_ALTITUDE_TOLL = 2;

    /** A gun strike costs the attacker this many altitude levels (TW). */
    private static final int STRIKE_ALTITUDE_TOLL = 1;

    /**
     * Fraction of the control-loss cost charged as a standing hazard for flying low with armed
     * enemies about. Enemy fire forces control rolls, and at low altitude a failed one is the ground
     * - a fighter loitering at NoE was one hit from death every round and the ranker priced it at
     * zero. Scales with the same crash odds every other risk term uses.
     */
    private static final double LOW_ALTITUDE_EXPOSURE_WEIGHT = 0.5;

    /**
     * What each level of banked altitude is worth, up to {@link #ALTITUDE_BANK_CEILING}. Climbing to
     * gain altitude for future things is good flying (Dave): altitude is stored energy and safety
     * margin, spent on attack runs and refilled between them. Deliberately small - a live attack run
     * must always outbid the bank - but nonzero, so a fighter with no run this turn climbs instead
     * of sagging.
     */
    private static final double ALTITUDE_BANK_WEIGHT = 1.5;

    /** Altitude above which the bank credit stops growing - high enough; go fight. */
    private static final int ALTITUDE_BANK_CEILING = 7;

    /** What reaching the ground out of control is priced at, before the odds of it happening. */
    private static final double CONTROL_LOSS_COST = 40.0;

    /**
     * What leaving the board is priced at.
     *
     * <p>Deliberately the largest cost in the ranker. Flying off removes the unit from the fight for at
     * least {@code 1 + ceil(velocity/4)} turns under return flyovers, and under common victory settings it
     * simply concedes - an observed live game was lost exactly this way, the bot drifting into a heading it
     * was then committed to. The stock ranker prices this at literally zero for atmospheric aerospace:
     * {@code BasicPathRanker.calculateOffBoardMod} returns 0.0, and the path generator keeps one fly-off
     * path in every candidate set.</p>
     */
    private static final double OFF_BOARD_COST = 80.0;

    /** Fraction of the off-board cost charged, per committed hex the pose cannot stop short of the edge. */
    private static final double EDGE_PRESSURE_WEIGHT = 0.5;

    /**
     * Fraction of the off-board cost charged for hugging an edge the pose is not even pointing at.
     *
     * <p>The directional walk missed this case live: a damaged fighter fled to the westernmost column facing
     * north, and because its straight run exited far away it paid almost nothing - while sitting one hexside
     * of drift from an exit the whole time. Proximity to any edge inside the unsteerable straight run is a
     * standing risk whatever the nose points at.</p>
     */
    private static final double EDGE_HUG_WEIGHT = 0.4;

    /** Faces on a d6, for the odds an out-of-control unit falls far enough to hit the ground. */
    private static final double DIE_FACES = 6.0;

    /**
     * What each point of carried velocity above the floor costs, as a fraction of the damage the fighter
     * could deliver up close, whenever there is enemy air on the board.
     *
     * <p>Velocity is not speed in the ground sense, it is committed displacement. One point carries a
     * fighter sixteen ground hexes whether it wants them or not (TW p.92), the free-turn threshold grows
     * with it (8/12/16/20/24 straight hexes), and a paid turn costs {@code ceil(velocity/2)} thrust. All
     * three push the same way: to stay in a dogfight, slow down. This is the discipline human players apply
     * by hand - a dogfight on a ground map is flown at velocity 1 or 2 - and the term exists to teach the
     * bot the same habit.</p>
     *
     * <p>Deliberately NOT scaled by the engagement credit the path earned. An early version was, which
     * silently disabled it in exactly the case it exists for: a fighter carrying too much velocity to end
     * any path in a firing position earned no engagement credit, so the penalty that would have told it to
     * slow down was multiplied by zero. Measured on a 10-game batch: 96,319 ranked paths, not one of them
     * engageable, every velocity penalty 0.00.</p>
     */
    private static final double VELOCITY_AGILITY_COST = 0.12;

    // Per-path reasoning, recorded for the TSV. Reset at the top of every calculateAerospaceMod call, before
    // any early return, so a path can never inherit the previous path's figures.
    private double lastEngagementCredit;
    private double lastArcAdvantage;
    private double lastControlRiskPenalty;
    private double lastVelocityPenalty;
    private double lastEdgePenalty;
    private double lastManeuverRisk;
    private int lastManeuverType;
    private double lastManeuverOdds;

    /**
     * The penalty that buries a maneuver path whose doctrine gate is closed this turn (see
     * {@link #maneuverSanctioned}). Large enough that no engagement credit can outbid it.
     */
    static final double UNSANCTIONED_MANEUVER_COST = 1_000;

    /** Control-roll modifier for a stalled aerodyne (TW p.81). */
    static final int STALL_CONTROL_MODIFIER = 2;

    /** An offensive maneuver below this success chance is gambling, not flying, and is not sanctioned. */
    static final double MINIMUM_STUNT_SUCCESS_CHANCE = 0.5;
    private int lastEngageableEnemies;
    private int lastCommittedEnemies;
    private int lastAirEnemies;
    int lastGroundTargets;
    int lastOverflownTargets;
    private double lastAttackRunCredit;

    /**
     * The altitude this path really ends the round at: the flown final altitude, minus the two-level
     * toll of the dive bomb the pose is credited for. Every crash-odds term prices against this - a
     * run bombed from altitude 3 exits at 1, where any failed roll reaches the ground, and pricing
     * that honestly is what pushes runs to the top of the window (attack from 5, exit at 3).
     */
    private int lastPostAttackAltitude;
    private double lastExposurePenalty;
    private double lastAltitudeBank;

    /** Test seam: read or set the post-attack altitude the risk terms will price against. */
    int lastPostAttackAltitudeForTest() {
        return lastPostAttackAltitude;
    }

    void lastPostAttackAltitudeForTest(int altitude) {
        lastPostAttackAltitude = altitude;
    }
    private int lastVenueGround;
    private double lastCloseRangeDamage;
    private int lastFinalAltitude;
    private int lastFinalVelocity;
    private CombatPosture lastPosture;

    // The flight's own attack-or-defend stance, resolved once per round per board and kept separate from the
    // ground force's. Multi-round history, so it needs the round-went-backwards reset in resolveAerospacePosture.
    private final Map<Integer, PostureResolver> aerospacePostureResolvers = new HashMap<>();
    private final Map<Integer, CombatPosture> aerospacePostureByBoard = new HashMap<>();
    private int aerospacePostureRound = -1;

    public AerospacePathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Airborne aerospace paths do not answer to the ground units' fall machinery.
     *
     * <p>This method feeds two consumers in the stock ranker, both built for falling: paths whose success
     * product drops below the fall tolerance are culled before ranking, and survivors are charged
     * fallShame - up to {@code UNIT_DESTRUCTION_FACTOR} when the product reaches zero. An airborne
     * aerospace unit's piloting rolls are control rolls, and this ranker already prices them at crash
     * scale, graded by altitude ({@link #controlRiskPenalty}, {@link #maneuverRiskPenalty}) - a failed
     * roll at altitude 8 is a scare, at altitude 2 a crater. Left in force, the ground pricing charged
     * every maneuver path twice and read a post-Hammerhead stall as certain destruction (482 points
     * against a winning path at 120) - which is why CASPAR ranked maneuver paths for days and never flew
     * one.</p>
     */
    @Override
    protected double getMovePathSuccessProbability(MovePath movePath) {
        Entity mover = movePath.getEntity();
        if (mover.isAero() && mover.isAirborne() && !mover.isSpaceborne()) {
            return 1.0;
        }
        return super.getMovePathSuccessProbability(movePath);
    }

    /**
     * The debrief: after every auction, one durable record of the decision and the roads not taken.
     *
     * <p>Aerospace is complex enough that the story is in the alternatives - the mek was in this hex, the
     * chosen path ended in that one, and a maneuver that overflew the target lost by nine points. The
     * per-path TSV holds all of it but rotates away mid-game; this line survives in the bot log, one per
     * aero turn, greppable by DEBRIEF. Every engagement gets its debrief, and the data is the story.</p>
     */
    @Override
    public java.util.TreeSet<RankedPath> rankPaths(List<MovePath> movePaths, Game game, int maxRange,
          double fallTolerance, List<Entity> enemies, List<Entity> friends) {
        java.util.TreeSet<RankedPath> ranked = super.rankPaths(movePaths, game, maxRange, fallTolerance,
              enemies, friends);
        try {
            logDebrief(ranked);
        } catch (Exception exception) {
            DEBRIEF_LOGGER.error(exception, "Debrief logging failed; the ranking itself is unaffected");
        }
        return ranked;
    }

    private static final megamek.logging.MMLogger DEBRIEF_LOGGER =
          megamek.logging.MMLogger.create(AerospacePathRanker.class);

    private void logDebrief(java.util.TreeSet<RankedPath> ranked) {
        if (ranked.isEmpty()) {
            return;
        }
        RankedPath chosen = ranked.first();
        Entity mover = chosen.getPath().getEntity();
        if (!mover.isAero() || !mover.isAirborne() || mover.isSpaceborne()) {
            return;
        }
        double chosenRank = chosen.getRank();
        RankedPath bestOverflight = null;
        RankedPath bestManeuver = null;
        int maneuverPaths = 0;
        int overflightPaths = 0;
        for (RankedPath candidate : ranked) {
            Map<String, Double> scores = candidate.getScores();
            if (scores.getOrDefault("aeroManeuverType", 0.0) != 0.0) {
                maneuverPaths++;
                // A buried maneuver (doctrine gate closed) never bid in the auction; the debrief's
                // "best rejected" must name a real contender, not the burial constant.
                boolean buried = scores.getOrDefault("aeroManeuverRisk", 0.0) >= UNSANCTIONED_MANEUVER_COST;
                if ((bestManeuver == null) && (candidate != chosen) && !buried) {
                    bestManeuver = candidate;
                }
            }
            if (scores.getOrDefault("aeroOverflownTargets", 0.0) > 0.0) {
                overflightPaths++;
                if ((bestOverflight == null) && (candidate != chosen)) {
                    bestOverflight = candidate;
                }
            }
        }
        StringBuilder debrief = new StringBuilder("DEBRIEF ").append(mover.getDisplayName())
              .append(": chose ").append(describe(chosen))
              .append(String.format(" rank=%.1f", chosenRank))
              .append(" | ").append(ranked.size()).append(" paths, ")
              .append(maneuverPaths).append(" maneuvers, ")
              .append(overflightPaths).append(" overflights");
        if ((bestOverflight != null)
              && (chosen.getScores().getOrDefault("aeroOverflownTargets", 0.0) == 0.0)) {
            debrief.append(" | best rejected OVERFLIGHT: ").append(describe(bestOverflight))
                  .append(String.format(" lost by %.1f", chosenRank - bestOverflight.getRank()));
        }
        if ((bestManeuver != null)
              && (chosen.getScores().getOrDefault("aeroManeuverType", 0.0) == 0.0)) {
            debrief.append(" | best rejected MANEUVER: ").append(describe(bestManeuver))
                  .append(String.format(" lost by %.1f", chosenRank - bestManeuver.getRank()));
        }
        DEBRIEF_LOGGER.info(debrief.toString());
    }

    /** One phrase for a ranked path: maneuver name and odds if any, end hex, altitude, velocity. */
    private static String describe(RankedPath rankedPath) {
        MovePath path = rankedPath.getPath();
        Map<String, Double> scores = rankedPath.getScores();
        StringBuilder text = new StringBuilder();
        int maneuverType = (int) Math.round(scores.getOrDefault("aeroManeuverType", 0.0));
        if (maneuverType != ManeuverType.MAN_NONE) {
            text.append(ManeuverType.getTypeName(maneuverType))
                  .append(String.format(" (odds %.0f%%) ", scores.getOrDefault("aeroManeuverOdds", 1.0) * 100));
        }
        text.append(path.getFinalCoords() != null ? path.getFinalCoords().getBoardNum() : "off-board")
              .append(" alt ").append(path.getFinalAltitude())
              .append(" vel ").append(path.getFinalVelocity());
        double overflown = scores.getOrDefault("aeroOverflownTargets", 0.0);
        if (overflown > 0) {
            text.append(String.format(" overflying %.0f target(s)", overflown));
        }
        return text.toString();
    }

    /**
     * Prices the geometry the stock terms cannot see: whether this pose can shoot anybody, whether it does so
     * from an arc they cannot answer, and what it is risking to get there.
     */
    @Override
    protected double calculateAerospaceMod(MovePath path, Game game, List<Entity> enemies) {
        lastEngagementCredit = 0;
        lastArcAdvantage = 0;
        lastControlRiskPenalty = 0;
        lastVelocityPenalty = 0;
        lastEdgePenalty = 0;
        lastManeuverRisk = 0;
        lastManeuverType = 0;
        lastManeuverOdds = 1.0;
        lastEngageableEnemies = 0;
        lastCommittedEnemies = 0;
        lastAirEnemies = 0;
        lastGroundTargets = 0;
        lastOverflownTargets = 0;
        lastAttackRunCredit = 0;
        lastPostAttackAltitude = 0;
        lastExposurePenalty = 0;
        lastAltitudeBank = 0;
        lastVenueGround = 0;
        lastCloseRangeDamage = 0;
        lastFinalAltitude = 0;
        lastFinalVelocity = 0;
        lastPosture = null;

        Entity mover = path.getEntity();
        if (!mover.isAero() || !mover.isAirborne() || mover.isSpaceborne()) {
            return 0;
        }

        lastFinalAltitude = path.getFinalAltitude();
        lastPostAttackAltitude = lastFinalAltitude;
        lastFinalVelocity = path.getFinalVelocity();
        AerospaceVenue venue = AerospaceVenue.of(game, mover);
        // Standing diagnosis columns: which venue this ranking believed it was in, and what the damage
        // helper thinks this airframe can do up close. Every aero term multiplies one of these two factors,
        // so when the TSV shows the terms at zero these columns say which factor collapsed without an
        // instrumented rebuild. They earned their keep: a batch of forensic columns built on exactly this
        // pattern traced 40 bloodless games to fighters whose effective weapon list was empty (the
        // stratops_capital_fighter option inherited from the user's saved gameoptions.xml).
        lastVenueGround = venue.isGroundMap() ? 1 : 0;
        lastCloseRangeDamage = getMaxDamageAtRange(mover, 1, isExtremeRange(game), isLosRange(game));
        // Resolving here is what gives an aerospace force a stance at all. The ground code's two calls to
        // resolvePosture both sit behind guards an airborne aero never passes, so until now a flight had no
        // attack-or-defend answer, and nothing that reads posture applied to it.
        lastPosture = resolveAerospacePosture(game, mover.getBoardId(), venue);
        scoreEngagements(path, game, enemies, venue);
        scoreAttackRuns(path, game, enemies, venue);
        lastControlRiskPenalty = controlRiskPenalty(path);
        lastVelocityPenalty = velocityPenalty(path, venue);
        lastEdgePenalty = edgePenalty(path, game, venue);
        lastManeuverRisk = maneuverRiskPenalty(path, game, venue);
        // Flying low with armed enemies about is a standing bet: any hit forces a control roll, and
        // the crash odds are the same d6 every other term prices. Flying high is the banked inverse.
        if ((lastGroundTargets + lastAirEnemies) > 0) {
            lastExposurePenalty = CONTROL_LOSS_COST * LOW_ALTITUDE_EXPOSURE_WEIGHT
                  * oddsOfReachingTheGround(lastPostAttackAltitude);
        }
        lastAltitudeBank = ALTITUDE_BANK_WEIGHT * Math.min(lastPostAttackAltitude, ALTITUDE_BANK_CEILING);

        // The pro-con of any maneuver, priced as flown: its gains - the pose it reaches, the arc it
        // claims - only exist if the control roll passes, so they are worth their expected value, not
        // their face value. Its costs are certain either way. A 58% Immelmann onto a committed enemy
        // offers 58% of its pose; the crash risk and the spent turn are owed in full.
        double gains = lastEngagementCredit + lastArcAdvantage + lastAttackRunCredit + lastAltitudeBank;
        if (lastManeuverType != ManeuverType.MAN_NONE) {
            gains *= lastManeuverOdds;
        }
        return gains - lastControlRiskPenalty - lastVelocityPenalty - lastEdgePenalty - lastManeuverRisk
              - lastExposurePenalty;
    }

    /**
     * Works out whether this flight is attacking or defending, from the aircraft alone.
     *
     * <p>Kept apart from the force posture the ground code resolves, for two reasons. A mixed force's Meks and
     * its fighters are not fighting the same battle - the ground line can be standing off while the flight is
     * pressing an attack over it - and the closing rate that decides the question is measured at a completely
     * different scale in the air, so pooling the two would answer neither.</p>
     *
     * @param game    the current game
     * @param boardId the board the flight is fighting over
     * @param venue   which set of atmospheric rules is in force, which sets the measuring scale
     *
     * @return the posture this flight fights under this round
     */
    protected CombatPosture resolveAerospacePosture(Game game, int boardId, AerospaceVenue venue) {
        int round = game.getCurrentRound();
        if (round != aerospacePostureRound) {
            if (round < aerospacePostureRound) {
                // The round going backwards means a new game on a reused bot client: a server reset keeps
                // bots connected, and the resolvers carry several rounds of closing-rate history that
                // belongs to the game that just ended.
                aerospacePostureResolvers.clear();
            }
            aerospacePostureRound = round;
            aerospacePostureByBoard.clear();
        }
        return aerospacePostureByBoard.computeIfAbsent(boardId, id -> {
            PostureResolver resolver = aerospacePostureResolvers.computeIfAbsent(id,
                  newBoard -> new PostureResolver());
            return resolver.resolve(getOwner().getBehaviorSettings(), round,
                  airbornePositions(getOwner().getEntitiesOwned(), id),
                  airbornePositions(getOwner().getEnemyEntities(), id),
                  venue.hexesPerVelocityPoint());
        });
    }

    /**
     * The positions of the airborne aerospace units among the given list that are deployed on the given board.
     *
     * <p>Board-filtered because entity lists are game-wide, and aircraft-only because a flight's stance is
     * about the air battle: a Mek standing still on the ground below would otherwise drag the flight's
     * measured closing rate toward zero.</p>
     */
    private static List<Coords> airbornePositions(List<Entity> units, int boardId) {
        List<Coords> positions = new ArrayList<>();
        for (Entity unit : units) {
            if (!unit.isAero() || !unit.isAirborne() || (unit.getPosition() == null)) {
                continue;
            }
            if (unit.getBoardId() != boardId) {
                continue;
            }
            positions.add(unit.getPosition());
        }
        return positions;
    }

    /**
     * Reports the flight's own posture in preference to the ground force's, so the log says what this
     * aircraft was actually flying under.
     */
    @Override
    protected CombatPosture resolvedPostureFor(Game game, int boardId) {
        if (game.getCurrentRound() == aerospacePostureRound) {
            CombatPosture flightPosture = aerospacePostureByBoard.get(boardId);
            if (flightPosture != null) {
                return flightPosture;
            }
        }
        return super.resolvedPostureFor(game, boardId);
    }

    /**
     * Walks the airborne enemies and credits this pose for the ones it can actually shoot.
     *
     * <p>Only air-to-air targets are considered. A fighter making a ground attack shoots along its flight
     * path rather than at a range, so the dead zone has nothing to say about it, and penalising a
     * ground-attack pose for having no air target would ground the whole doctrine.</p>
     */
    private void scoreEngagements(MovePath path, Game game, List<Entity> enemies, AerospaceVenue venue) {
        Entity mover = path.getEntity();
        Coords finalCoords = path.getFinalCoords();
        int finalAltitude = path.getFinalAltitude();
        boolean fliesAsSpheroid = Compute.useSpheroidAtmosphere(game, mover);
        boolean extremeRange = isExtremeRange(game);
        boolean losRange = isLosRange(game);

        for (Entity enemy : enemies) {
            if (!isAirToAirCandidate(mover, enemy, game)) {
                continue;
            }
            lastAirEnemies++;
            boolean committed = evaluateAsMoved(enemy);
            if (committed) {
                lastCommittedEnemies++;
            }
            double confidence = committed ? 1.0 : UNMOVED_ENEMY_CONFIDENCE;
            int enemyAltitude = committed
                  ? enemy.getAltitude()
                  : AerospaceGeometry.reachableAltitudeBand(enemy).midpoint();

            if (AerospaceGeometry.deadZoneBlocksAttack(venue, finalCoords, finalAltitude, fliesAsSpheroid,
                  enemy.getPosition(), enemyAltitude)) {
                continue;
            }
            // Arc is a precondition, not a bonus. Aerospace guns sit in four fixed wedges and every weapon
            // that does not bear is dropped from the firing plan, so a path that ends without the enemy in
            // an arc has no attack at all - however close, however level, however good the odds would be.
            // Scoring it as an engagement is what let the bot fly elegant passes and never shoot.
            if (!anyWeaponBears(mover, finalCoords, path.getFinalFacing(), enemy.getPosition(),
                  fliesAsSpheroid)) {
                continue;
            }
            int range = AerospaceGeometry.effectiveRange(venue, finalCoords, finalAltitude,
                  enemy.getPosition(), enemyAltitude);
            // getMaxDamageAtRange reads the weapon brackets directly and answers zero outside all of them,
            // so it is the range check as well as the damage figure. Do not gate it on
            // Princess.getMaxWeaponRange: that carries an x8 multiplier for airborne aero over a ground map,
            // which is a different unit from the low-altitude hexes the brackets and effectiveRange use.
            double reachableDamage = getMaxDamageAtRange(mover, range, extremeRange, losRange);
            if (reachableDamage <= 0) {
                continue;
            }

            lastEngageableEnemies++;
            lastEngagementCredit += reachableDamage * ENGAGEMENT_WEIGHT * confidence;

            if (committed && holdsUnanswerableArc(path, enemy, game)) {
                lastArcAdvantage += reachableDamage * ENGAGEMENT_WEIGHT * ARC_ADVANTAGE_WEIGHT;
            }
        }
    }

    /**
     * Credits this path for every enemy ground unit whose hex its flown line crosses inside the legal
     * attack window.
     *
     * <p>The mechanic this prices: every air-to-ground attack requires {@code passedOver(target)} - the
     * target's hex must be on the line the fighter physically flew THIS round, and the line resets every
     * round. Ground units move before aerospace on a ground map, so their final positions are known facts
     * when this ranker runs: an overflight is never a guess, always a plan. The legal window is read from
     * the armament - dive bombing needs a final altitude of {@value MMConstants#DIVE_BOMB_MIN_ALTITUDE} to
     * {@value MMConstants#DIVE_BOMB_MAX_ALTITUDE} with ground bombs aboard, gun strikes work up to
     * {@value #STRIKE_MAX_ALTITUDE} - and a pose outside every window earns nothing, because it cannot
     * attack no matter what it flew over.</p>
     */
    void scoreAttackRuns(MovePath path, Game game, List<Entity> enemies, AerospaceVenue venue) {
        if (!venue.isGroundMap()) {
            return;
        }
        Entity mover = path.getEntity();
        int finalAltitude = path.getFinalAltitude();
        boolean hasGroundBombs = !mover.getBombs(AmmoType.F_GROUND_BOMB).isEmpty();
        boolean inDiveBombWindow = hasGroundBombs
              && (finalAltitude >= MMConstants.DIVE_BOMB_MIN_ALTITUDE)
              && (finalAltitude <= MMConstants.DIVE_BOMB_MAX_ALTITUDE);
        // A strike still costs 1 altitude, so the pose must keep a level in hand above the deck.
        boolean inStrikeWindow = (finalAltitude > AerospaceGeometry.MINIMUM_ALTITUDE)
              && (finalAltitude <= STRIKE_MAX_ALTITUDE);

        double bombDamage = inDiveBombWindow ? groundBombDamage(mover) : 0;
        double gunDamage = inStrikeWindow
              ? getMaxDamageAtRange(mover, 1, isExtremeRange(game), isLosRange(game)) : 0;
        double runDamage = Math.max(bombDamage, gunDamage);

        boolean inAnyWindow = inDiveBombWindow || inStrikeWindow;
        for (Entity enemy : enemies) {
            if (!isGroundTargetCandidate(mover, enemy, game)) {
                continue;
            }
            lastGroundTargets++;
            if (!inAnyWindow || !path.getCoordsSet().contains(enemy.getPosition())) {
                continue;
            }
            // Counted whenever the geometry works, credited only for what the armament can deliver -
            // the count is the diagnostic (did we line runs up?), the credit is the incentive.
            lastOverflownTargets++;
            lastAttackRunCredit += runDamage * ATTACK_RUN_WEIGHT;
            // The credit assumes the attack, so the risk pricing must assume its altitude toll too:
            // 2 levels for a dive bomb, 1 for a gun strike (TW), never below the deck the engine
            // itself enforces. Unpriced, the strike toll walked a live Chippewa down 4-3-2 after its
            // bombs ran out - each strike exiting one level lower - into a fatal control roll.
            int toll = (inDiveBombWindow && (bombDamage >= gunDamage))
                  ? DIVE_BOMB_ALTITUDE_TOLL : STRIKE_ALTITUDE_TOLL;
            lastPostAttackAltitude = Math.max(AerospaceGeometry.MINIMUM_ALTITUDE, finalAltitude - toll);
        }
    }

    /** The summed damage of every ground bomb aboard - the payload one full dive-bomb pass can deliver. */
    private static double groundBombDamage(Entity mover) {
        double total = 0;
        for (BombMounted bomb : mover.getBombs(AmmoType.F_GROUND_BOMB)) {
            total += bomb.getType().getDamagePerShot();
        }
        return total;
    }

    /**
     * Whether this enemy is a ground unit an attack run could target: on the ground, on our board, and
     * worth attacking. Airborne enemies belong to {@link #scoreEngagements}; the two sets never overlap.
     */
    private boolean isGroundTargetCandidate(Entity mover, Entity enemy, Game game) {
        return !enemy.isAirborne()
              && (enemy.getPosition() != null)
              && (enemy.getBoardId() == mover.getBoardId())
              && !enemy.isOffBoard()
              && !isIgnorableEnemy(mover, enemy, game);
    }

    /**
     * Whether this enemy is one the dead zone has any say about: an airborne aerospace unit sharing our board.
     *
     * <p>The board filter is not optional. Entity lists are game-wide, and on a multi-board game an enemy
     * flying over a different ground map would otherwise contribute its altitude to a comparison that means
     * nothing.</p>
     */
    private boolean isAirToAirCandidate(Entity mover, Entity enemy, Game game) {
        return enemy.isAero()
              && enemy.isAirborne()
              && !enemy.isSpaceborne()
              && (enemy.getPosition() != null)
              && (enemy.getBoardId() == mover.getBoardId())
              && !isIgnorableEnemy(mover, enemy, game);
    }

    /**
     * Whether this pose sits outside the target's main firing arc while holding the target inside ours.
     *
     * <p>Aerospace arcs are four discrete wedges rather than a continuous field of fire, so an arc the target
     * has nothing bearing in is worth more than simply being behind it. Only asked of an enemy that has
     * already moved - an unmoved one's facing is not yet a fact. Spheroids are skipped because they bear in
     * every direction and there is no arc to get outside of.</p>
     */
    private boolean holdsUnanswerableArc(MovePath path, Entity enemy, Game game) {
        if (Compute.useSpheroidAtmosphere(game, enemy) || (enemy.getFacing() < 0)) {
            return false;
        }
        Coords finalCoords = path.getFinalCoords();
        boolean targetInMyNose = ComputeArc.isInArc(finalCoords, path.getFinalFacing(), enemy.getPosition(),
              Compute.ARC_NOSE);
        boolean iAmInTheirNose = ComputeArc.isInArc(enemy.getPosition(), enemy.getFacing(), finalCoords,
              Compute.ARC_NOSE);
        return targetInMyNose && !iAmInTheirNose;
    }

    /**
     * Whether anything the unit is carrying can actually shoot at the target from this pose.
     *
     * <p>Checked per weapon rather than by a single nose test, because what bears depends on the airframe:
     * a Stuka has aft guns and can shoot at something behind it, a Cheetah carries nose and wing only and
     * cannot. Spheroids in atmosphere bear in every direction and are simply allowed.</p>
     *
     * @param mover           the unit whose path is being ranked
     * @param finalCoords     the hex the path ends in
     * @param finalFacing     the facing the path ends on
     * @param targetPosition  the target's hex
     * @param fliesAsSpheroid {@code true} if the unit is behaving as a spheroid in atmosphere
     *
     * @return {@code true} if at least one ready weapon has the target in its arc
     */
    private boolean anyWeaponBears(Entity mover, Coords finalCoords, int finalFacing, Coords targetPosition,
          boolean fliesAsSpheroid) {
        if (fliesAsSpheroid) {
            return true;
        }
        for (WeaponMounted weapon : mover.getWeaponList()) {
            if (!weapon.canFire()) {
                continue;
            }
            int arc = mover.getWeaponArc(mover.getEquipmentNum(weapon));
            if (ComputeArc.isInArc(finalCoords, finalFacing, targetPosition, arc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What the velocity this path carries away will cost the next turn, once there is an air fight on.
     *
     * <p>Aerospace velocity is not ground speed. It is displacement the unit is committed to spending next
     * turn whether it wants to or not - sixteen ground hexes per point (TW p.92) - and it raises the price
     * of turning twice over: the free-turn threshold stretches from 8 straight hexes at velocity 1 to 24 at
     * velocity 5, and a paid turn costs {@code ceil(velocity/2)} thrust. A fighter that comes off a pass at
     * velocity 5 is eighty hexes away and can barely turn; one that slows to 1 stays in the fight and turns
     * for a single point.</p>
     *
     * <p>Priced from what the fighter could deliver at close range - the engagement its excess velocity is
     * costing it - NOT from the engagement credit this path happened to earn. Gating on earned credit is the
     * bug this replaces: a fighter too fast to end any path in a firing position earned no credit anywhere,
     * so the one term that would have told it to slow down was zero exactly when it mattered.</p>
     *
     * <p>Charged only over a ground mapsheet with enemy air on the board. At low altitude a velocity point
     * is one hex, not sixteen, and displacement is not the problem; and with no air opposition a fighter
     * crossing the board to a ground target should not be taxed for getting there. Velocity 1 is the
     * ground-map floor, so the charge starts above it.</p>
     *
     * @param path  the path being ranked
     * @param venue which set of atmospheric rules is in force
     *
     * @return the penalty to subtract from this path's utility
     */
    double velocityPenalty(MovePath path, AerospaceVenue venue) {
        // Ground targets discipline velocity for a different reason than enemy air does: a bombing run
        // must thread the target's exact hex, and at velocity 3 a facing change comes only every 16
        // hexes - the run cannot be aimed. Measured without this: fourteen-round gaps between passes.
        if (!venue.isGroundMap() || ((lastAirEnemies == 0) && (lastGroundTargets == 0))) {
            return 0;
        }
        int excessVelocity = Math.max(0, path.getFinalVelocity() - 1);
        if (excessVelocity == 0) {
            return 0;
        }
        Game game = path.getGame();
        double closeRangeDamage = getMaxDamageAtRange(path.getEntity(), 1, isExtremeRange(game),
              isLosRange(game));
        return closeRangeDamage * ENGAGEMENT_WEIGHT * VELOCITY_AGILITY_COST * excessVelocity;
    }

    /**
     * What a special maneuver on this path is betting on the airframe.
     *
     * <p>A maneuver's real price is its control modifier, not its thrust (TW p.85): a failed control roll
     * costs 1d6 altitude, so the same +3 Hammerhead is nearly free at altitude 8 and a coin-flip on the
     * airframe at altitude 3. Priced as the chance the roll fails times the chance the resulting drop
     * reaches the ground, against the same airframe cost the overthrust term uses. The half-roll's -1
     * modifier prices cheaper than not maneuvering, which is exactly what the table says about it.</p>
     *
     * @param path the path being ranked
     *
     * @return the penalty to subtract from this path's utility
     */
    private double maneuverRiskPenalty(MovePath path, Game game, AerospaceVenue venue) {
        for (MoveStep step : path.getStepVector()) {
            if (step.getType() != MoveStepType.MANEUVER) {
                continue;
            }
            lastManeuverType = step.getManeuverType();
            lastManeuverOdds = maneuverSuccessChance(path, lastManeuverType);
            if (!maneuverSanctioned(lastManeuverType, lastCommittedEnemies, lastAirEnemies, path, game, venue)) {
                return UNSANCTIONED_MANEUVER_COST;
            }
            double failureChance = 1.0 - lastManeuverOdds;
            return failureChance * (CONTROL_LOSS_COST * oddsOfReachingTheGround(lastPostAttackAltitude)
                  + failedManeuverExitCost(path, game, venue));
        }
        return 0;
    }

    /**
     * What a FAILED roll actually does to this pose, beyond the altitude gamble: the server forces the
     * fighter to fly out straight for half its remaining velocity, no steering. Seen live: a failed
     * Split-S carried a Cheetah clean off the map. If that forced run exits the board, the failure
     * costs a disengage - priced like the deliberate one, by how much fight the unit has left - and
     * that cost is charged at the odds of failing. A maneuver whose failure is cheap (high, mid-board)
     * prices nearly free here; the same roll at the edge prices itself out.
     *
     * @param path  the maneuver path being ranked
     * @param game  the current game
     * @param venue which set of atmospheric rules is in force
     *
     * @return the fly-off cost of the forced straight run, or zero when it stays on the board
     */
    double failedManeuverExitCost(MovePath path, Game game, AerospaceVenue venue) {
        Entity mover = path.getEntity();
        if (!(mover instanceof IAero aero) || (mover.getPosition() == null)) {
            return 0;
        }
        // MovePathHandler on a failed maneuver: forward = max(velocityLeft / 2, 1), x16 on ground maps.
        int forcedHexes = Math.max(aero.getCurrentVelocity() / 2, 1) * venue.hexesPerVelocityPoint();
        int exitDistance = AerospaceGeometry.hexesUntilOffBoard(mover.getPosition(), mover.getFacing(),
              game.getBoard(path.getFinalBoardId()), forcedHexes + 1);
        if (exitDistance > forcedHexes) {
            return 0;
        }
        return OFF_BOARD_COST * disengageCostFraction(mover);
    }

    /**
     * Whether this maneuver is permitted this turn at all.
     *
     * <p>The doctrine gates live here and not in path generation because generation runs inside Precognition
     * at the start of the movement phase, before any enemy has moved - and an aero unit's paths are never
     * re-enumerated when an enemy commits. Rank time is the only moment the game state is current.</p>
     *
     * <p>Reactive maneuvers (Split-S, Side Slips, Half Roll) require a committed - already moved - enemy
     * and decent odds: they exist to exploit a position the opponent can no longer take back. The escape
     * pair (Hammerhead, Immelmann) reacts at any odds. Before anyone commits, only the energy hedges
     * (Immelmann, Loop) are sanctioned, with enemy air present and the odds in hand - moving first hands
     * the opponent a predictable line, and the hedge is how a first mover stays unexploitable.</p>
     *
     * @param maneuverType     the {@link ManeuverType} constant found on the path
     * @param committedEnemies enemies already evaluated as moved this turn
     * @param airEnemies       enemy aerospace sharing this fight, moved or not
     * @param path             the path being ranked
     * @param game             the current game
     * @param venue            which set of atmospheric rules is in force
     *
     * @return {@code true} when the maneuver may be scored on its merits, {@code false} to bury the path
     */
    boolean maneuverSanctioned(int maneuverType, int committedEnemies, int airEnemies, MovePath path,
          Game game, AerospaceVenue venue) {
        if (committedEnemies > 0) {
            // Every maneuver is a stunt, and a stunt at bad odds is gambling: the ranker scores the
            // pose the maneuver reaches, and a failed roll never reaches it. Two live games opened
            // with sub-30% Hammerheads under a blanket escape exemption; both fighters later died to
            // control-roll spirals. The exemption now requires being genuinely cornered - the board
            // edge inside the unsteerable straight run - where a 17% reversal really does beat the
            // alternatives. Everywhere else, the escape pair answers to the same floor as the rest.
            boolean escapePair = (maneuverType == ManeuverType.MAN_HAMMERHEAD)
                  || (maneuverType == ManeuverType.MAN_IMMELMAN);
            if (escapePair && cornered(path, game, venue)) {
                return true;
            }
            return maneuverSuccessChance(path, maneuverType) >= MINIMUM_STUNT_SUCCESS_CHANCE;
        }
        // Nobody has moved yet. Reactive maneuvers exploit a committed position and are pointless on
        // spec - but the energy hedges are flown precisely because nobody has committed. Moving first
        // hands the opponent a long predictable line to answer; an Immelmann ends slow, high, and
        // free-facing - banked energy and nothing to exploit - and a Loop dumps overshoot velocity in
        // place. Both are sensible first moves when enemy air is present and the pilot has the odds.
        // (An earlier version read the committed-enemy rule as absolute; corrected on Dave's call -
        // "Only Sith deal in absolutes" - 2026-08-13.)
        boolean energyHedge = (maneuverType == ManeuverType.MAN_IMMELMAN)
              || (maneuverType == ManeuverType.MAN_LOOP);
        if (!energyHedge || (airEnemies == 0)) {
            return false;
        }
        return maneuverSuccessChance(path, maneuverType) >= MINIMUM_STUNT_SUCCESS_CHANCE;
    }

    /**
     * Whether this pose has the board edge inside its unsteerable straight run - the one situation
     * where a bad-odds escape maneuver still beats every alternative.
     */
    private static boolean cornered(MovePath path, Game game, AerospaceVenue venue) {
        Entity mover = path.getEntity();
        if (mover.getPosition() == null) {
            return false;
        }
        int minStraight = venue.isGroundMap() ? 8 : 1;
        int exitDistance = AerospaceGeometry.hexesUntilOffBoard(mover.getPosition(), mover.getFacing(),
              game.getBoard(path.getFinalBoardId()), minStraight + 1);
        return exitDistance <= minStraight;
    }

    /**
     * The chance this unit's pilot makes the maneuver's control roll, matching the server's target
     * number: base piloting, +2 atmospheric operations, -1 for a fighter or small craft, plus the
     * maneuver's own modifier (the live game 6 roll read "Needs 9 [6 + 2 - 1 + 2]").
     *
     * @param mover        the maneuvering unit
     * @param maneuverType the {@link ManeuverType} constant
     *
     * @return the probability (0.0 to 1.0) of passing the control roll
     */
    static double maneuverSuccessChance(Entity mover, int maneuverType) {
        int target = mover.getCrew().getPiloting() + 2
              + ManeuverType.getMod(maneuverType, false)
              - (mover.isFighter() ? 1 : 0);
        return Compute.oddsAbove(target) / 100.0;
    }

    /**
     * The chance this path's maneuver control roll passes, preferring the server's own target number.
     *
     * <p>{@link IAero#checkManeuver} is the exact math the server rolls against, and its base piloting
     * roll carries what the flat formula cannot see: avionics hits, damaged controls, planetary
     * conditions. A fighter with a shot-up cockpit should be far more reluctant to stunt, and with the
     * flat formula it was not. Falls back to the flat formula when the path carries no maneuver step or
     * the engine declines to price the roll.</p>
     *
     * @param path         the path whose maneuver is being priced
     * @param maneuverType the {@link ManeuverType} constant found on the path
     *
     * @return the probability (0.0 to 1.0) of passing the control roll
     */
    static double maneuverSuccessChance(MovePath path, int maneuverType) {
        Entity mover = path.getEntity();
        if (mover instanceof IAero aero) {
            List<MoveStep> steps = path.getStepVector();
            if (steps != null) {
                for (MoveStep step : steps) {
                    if (step.getType() != MoveStepType.MANEUVER) {
                        continue;
                    }
                    PilotingRollData rollTarget = aero.checkManeuver(step, path.getLastStepMovementType());
                    int target = rollTarget.getValue();
                    if (target == TargetRoll.AUTOMATIC_SUCCESS) {
                        return 1.0;
                    }
                    if ((target == TargetRoll.IMPOSSIBLE) || (target == TargetRoll.AUTOMATIC_FAIL)) {
                        return 0.0;
                    }
                    if (target != TargetRoll.CHECK_FALSE) {
                        return Compute.oddsAbove(target) / 100.0;
                    }
                    break;
                }
            }
        }
        return maneuverSuccessChance(mover, maneuverType);
    }

    /**
     * What this pose is conceding to the board edge.
     *
     * <p>A path that flies off outright pays the full cost. A path that stays on board pays by how trapped
     * it is: the velocity it ends with is displacement it must spend next turn, the first
     * {@code minStraight} hexes of it dead ahead before any facing change is allowed (TW p.92 on a ground
     * mapsheet). If the edge is inside that committed, unsteerable run, the exit has effectively already
     * happened and the full cost applies; if the edge merely falls inside the committed distance, the cost
     * scales with how much of the run cannot stop short of it.</p>
     *
     * @param path  the path being ranked
     * @param game  the current game
     * @param venue which set of atmospheric rules is in force
     *
     * @return the penalty to subtract from this path's utility
     */
    double edgePenalty(MovePath path, Game game, AerospaceVenue venue) {
        if (path.fliesOffBoard()) {
            // Flying off is not an absolute sin (Dave, 2026-08-13: "I'm ok with CASPAR flying off if
            // it needs to"). A fighter that flies off returns some rounds later, untargetable in the
            // meantime - which is a disengage, not a defeat. A healthy fighter still pays full price,
            // because wandering off mid-fight hands the opponent the sky for free; a mauled one
            // leaves cheap, because staying is how it dies.
            return OFF_BOARD_COST * disengageCostFraction(path.getEntity());
        }
        int committed = Math.max(0, path.getFinalVelocity()) * venue.hexesPerVelocityPoint();
        if (committed == 0) {
            return 0;
        }
        int minStraightForHug = venue.isGroundMap() ? 8 : 1;
        int exitDistance = AerospaceGeometry.hexesUntilOffBoard(path.getFinalCoords(), path.getFinalFacing(),
              game.getBoard(path.getFinalBoardId()), committed + 1);
        if (exitDistance > committed) {
            return edgeHugPenalty(path, game, minStraightForHug);
        }
        // The straight run required before the first facing change: nothing inside it can be steered.
        int minStraight = venue.isGroundMap() ? 8 : 1;
        if (exitDistance <= minStraight) {
            return OFF_BOARD_COST;
        }
        double directional = OFF_BOARD_COST * EDGE_PRESSURE_WEIGHT * (committed - exitDistance) / committed;
        return Math.min(OFF_BOARD_COST, directional + edgeHugPenalty(path, game, minStraight));
    }

    /**
     * How much of the full off-board cost a deliberate fly-off pays, by how much fight the unit has
     * left: full price while healthy, half below half armor, a fifth once crippled.
     *
     * @param mover the unit flying off
     *
     * @return the fraction of {@code OFF_BOARD_COST} this unit pays to leave
     */
    private static double disengageCostFraction(Entity mover) {
        if (mover.isCrippled()) {
            return 0.2;
        }
        double armorRemaining = mover.getArmorRemainingPercent();
        if ((armorRemaining >= 0) && (armorRemaining < 0.5)) {
            return 0.5;
        }
        return 1.0;
    }

    /**
     * The standing charge for ending beside an edge, whatever the pose points at.
     *
     * @param path        the path being ranked
     * @param game        the current game
     * @param minStraight the unsteerable straight run for this venue
     *
     * @return zero when the nearest edge is outside the unsteerable run, scaling to
     *       {@code OFF_BOARD_COST * EDGE_HUG_WEIGHT} when standing on one
     */
    private double edgeHugPenalty(MovePath path, Game game, int minStraight) {
        int nearestEdge = AerospaceGeometry.hexesToNearestEdge(path.getFinalCoords(),
              game.getBoard(path.getFinalBoardId()));
        if (nearestEdge >= minStraight) {
            return 0;
        }
        return OFF_BOARD_COST * EDGE_HUG_WEIGHT * (minStraight - nearestEdge) / minStraight;
    }

    /**
     * What this path is betting on the airframe by spending more thrust than it safely can.
     *
     * <p>Overthrusting risks a control roll, and a failed one costs 1d6 altitude. That is nearly free high
     * up and close to fatal low down, which is the graded risk the stock code does not model: it treats a
     * control roll the same at altitude 9 as at altitude 2. The odds here are the odds of the drop reaching
     * the ground - a d6 rolling at least the unit's current altitude.</p>
     */
    double controlRiskPenalty(MovePath path) {
        Entity mover = path.getEntity();
        if (!(mover instanceof IAero aero)) {
            return 0;
        }
        // Both fighters lost in live game 8 died to exactly the risks priced here, because the old
        // pricing used a healthy airframe's arithmetic: flat costs that ignored avionics hits and
        // damage modifiers (a real End Phase read "3 control rolls: stalled out; avionics hit;
        // 40 damage +2" - every roll a 9), and the pre-attack altitude when a planned dive bomb was
        // about to cost two more levels. "Should I do this move" is only answerable at the real odds
        // in the real post-move state.
        double penalty = 0;
        double crashOdds = oddsOfReachingTheGround(lastPostAttackAltitude);
        int safeThrust = AeroPathUtil.calculateMaxSafeThrust(aero);
        if (path.getMpUsed() > safeThrust) {
            penalty += CONTROL_LOSS_COST * controlRollFailureChance(path, 0) * crashOdds;
        }
        // An aerodyne that ends its move at velocity zero stalls (TW p.81): a control roll at +2,
        // altitude lost on a failure. Priced in the same crash-scale family as the other control
        // risks, because the stock fall machinery is switched off for airborne aeros (see
        // getMovePathSuccessProbability).
        if ((path.getFinalVelocity() == 0) && !aero.isSpheroid() && !aero.isVSTOL()) {
            penalty += CONTROL_LOSS_COST * controlRollFailureChance(path, STALL_CONTROL_MODIFIER) * crashOdds;
        }
        return penalty;
    }

    /**
     * The chance THIS airframe, in its current state, fails a control roll with the given extra
     * modifier. Built on {@link Entity#getBasePilotingRoll} - the same base the server rolls from -
     * so avionics hits, damaged controls, pilot wounds, and conditions all raise the price. The flat
     * crew-skill formula answered for a fighter that no longer exists once the armor starts coming
     * off.
     *
     * @param path          the path being priced
     * @param extraModifier the roll's own modifier on top of the base (stall +2, maneuver mods, ...)
     *
     * @return the probability (0.0 to 1.0) that the roll fails
     */
    private static double controlRollFailureChance(MovePath path, int extraModifier) {
        Entity mover = path.getEntity();
        PilotingRollData baseRoll = mover.getBasePilotingRoll(path.getLastStepMovementType());
        int target = baseRoll.getValue();
        if (target == TargetRoll.AUTOMATIC_SUCCESS) {
            return 0.0;
        }
        if ((target == TargetRoll.IMPOSSIBLE) || (target == TargetRoll.AUTOMATIC_FAIL)) {
            return 1.0;
        }
        if (target == TargetRoll.CHECK_FALSE) {
            // The engine declines to price this roll; fall back to the bare crew number.
            target = mover.getCrew().getPiloting() + 2;
        }
        return 1.0 - (Compute.oddsAbove(target + extraModifier) / 100.0);
    }

    /**
     * The chance a 1d6 altitude loss puts a unit at this altitude on the deck.
     *
     * @param altitude the altitude the unit would be falling from
     *
     * @return a probability between 0 and 1
     */
    static double oddsOfReachingTheGround(int altitude) {
        if (altitude > DIE_FACES) {
            return 0;
        }
        // Reaching the ground needs the die to come up at least as high as the altitude.
        return Math.clamp((DIE_FACES - altitude + 1) / DIE_FACES, 0.0, 1.0);
    }

    /**
     * Reports an enemy as already moved only when it actually has.
     *
     * <p>The stock ranker adds {@code isAirborneAeroOnGroundMap()} to this test, which makes every enemy
     * fighter over a ground map report as committed whether it has moved or not. The bot then reads a stale
     * altitude as settled fact, matches it, and watches the opponent move somewhere else - and because the
     * flag never changes, it cannot tell that case apart from a genuine read on an opponent who has
     * committed. Dropping the clause is what makes reacting possible at all.</p>
     *
     * <p>{@code NewtonianAerospacePathRanker} already omits it; the shortcut is the ground-map ranker's
     * alone.</p>
     */
    @Override
    protected boolean evaluateAsMoved(Entity enemy) {
        if (enemy.isAero() && enemy.isAirborne()) {
            return !enemy.isSelectableThisTurn() || enemy.isImmobile();
        }
        return super.evaluateAsMoved(enemy);
    }

    /**
     * Estimates an enemy fighter that has not committed yet, across the altitudes it could still reach.
     *
     * <p>The stock version returns nothing at all for an airborne aero on a ground map, which - now that
     * {@link #evaluateAsMoved} lets such units reach this method - would read as "that fighter is no threat
     * and I can do nothing to it". Neither is true. What is true is that we do not know its altitude yet, so
     * it is evaluated at the altitude in its reachable band closest to ours: the most dangerous thing it can
     * choose to do is come and meet us.</p>
     */
    @Override
    EntityEvaluationResponse evaluateUnmovedEnemy(Entity enemy, MovePath path, boolean useExtremeRange,
          boolean useLOSRange) {
        if (!enemy.isAero() || !enemy.isAirborne()) {
            return super.evaluateUnmovedEnemy(enemy, path, useExtremeRange, useLOSRange);
        }

        EntityEvaluationResponse response = new EntityEvaluationResponse();
        Entity mover = path.getEntity();
        if ((enemy.getPosition() == null) || (mover.getPosition() == null)) {
            return response;
        }

        AerospaceVenue venue = AerospaceVenue.of(path.getGame(), mover);
        int finalAltitude = path.getFinalAltitude();
        AltitudeBand band = AerospaceGeometry.reachableAltitudeBand(enemy);
        // The altitude in its band nearest ours - the choice that puts it in a position to fight us.
        int closingAltitude = Math.clamp(finalAltitude, band.lowest(), band.highest());

        int range = AerospaceGeometry.effectiveRange(venue, path.getFinalCoords(), finalAltitude,
              enemy.getPosition(), closingAltitude);
        boolean blocked = AerospaceGeometry.inDeadZone(venue, path.getFinalCoords(), finalAltitude,
              enemy.getPosition(), closingAltitude);
        if (blocked) {
            return response;
        }

        // An unmoved opponent is a guess, so both sides of the exchange are discounted by the same
        // confidence the engagement credit uses. getMaxDamageAtRange answers zero outside every bracket, so
        // it is the range check too - see the note in scoreEngagements about unit mismatch.
        //
        // The arc gate is NOT optional here. Without it this estimate feeds bravery a damage figure for
        // poses no weapon can fire from, and since aggression and mutual support are disabled for airborne
        // aero on ground maps, that phantom credit is the biggest term left - the ranker then maximises it
        // by standing far away, nose elsewhere, "able" to deal damage while taking none. Observed live as an
        // undamaged Chippewa crossing the map to an edge corner past a 44-point edge penalty: the exact
        // pose its own engagement credit scored as unable to shoot, bravery scored at +51.
        boolean myGunsBear = anyWeaponBears(mover, path.getFinalCoords(), path.getFinalFacing(),
              enemy.getPosition(), Compute.useSpheroidAtmosphere(path.getGame(), mover));
        if (myGunsBear) {
            response.addToMyEstimatedDamage(
                  getMaxDamageAtRange(mover, range, useExtremeRange, useLOSRange) * UNMOVED_ENEMY_CONFIDENCE);
        }
        response.addToEstimatedEnemyDamage(
              getMaxDamageAtRange(enemy, range, useExtremeRange, useLOSRange) * UNMOVED_ENEMY_CONFIDENCE);
        return response;
    }

    /**
     * Records why the doctrine scored this path the way it did, as extra TSV columns.
     *
     * <p>The totals alone cannot answer "why did the fighter go there". These are the inputs: the altitude it
     * chose, how many enemies it could actually shoot from there, how many of those had already committed to
     * an altitude, and what the path risked to arrive.</p>
     */
    @Override
    protected Map<String, Double> doctrineScores() {
        Map<String, Double> scores = new HashMap<>(super.doctrineScores());
        // The base records the ground force's posture field, which nothing ever sets for an aircraft. Report
        // the flight's own instead, so the column means what it says on an aerospace row.
        if (lastPosture != null) {
            scores.put("combatPosture", (double) lastPosture.ordinal());
        }
        scores.put("aeroFinalAltitude", (double) lastFinalAltitude);
        scores.put("aeroEngageableEnemies", (double) lastEngageableEnemies);
        scores.put("aeroCommittedEnemies", (double) lastCommittedEnemies);
        scores.put("aeroAirEnemies", (double) lastAirEnemies);
        scores.put("aeroVenueGround", (double) lastVenueGround);
        scores.put("aeroCloseRangeDamage", lastCloseRangeDamage);
        scores.put("aeroEngagementCredit", lastEngagementCredit);
        scores.put("aeroArcAdvantage", lastArcAdvantage);
        scores.put("aeroControlRiskPenalty", lastControlRiskPenalty);
        scores.put("aeroVelocityPenalty", lastVelocityPenalty);
        scores.put("aeroEdgePenalty", lastEdgePenalty);
        scores.put("aeroManeuverRisk", lastManeuverRisk);
        scores.put("aeroManeuverType", (double) lastManeuverType);
        scores.put("aeroManeuverOdds", lastManeuverOdds);
        scores.put("aeroGroundTargets", (double) lastGroundTargets);
        scores.put("aeroOverflownTargets", (double) lastOverflownTargets);
        scores.put("aeroAttackRunCredit", lastAttackRunCredit);
        scores.put("aeroExposurePenalty", lastExposurePenalty);
        scores.put("aeroAltitudeBank", lastAltitudeBank);
        scores.put("aeroFinalVelocity", (double) lastFinalVelocity);
        return scores;
    }
}
