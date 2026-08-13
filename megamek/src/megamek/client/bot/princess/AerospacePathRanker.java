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
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
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

    /** What reaching the ground out of control is priced at, before the odds of it happening. */
    private static final double CONTROL_LOSS_COST = 40.0;

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
    private int lastEngageableEnemies;
    private int lastCommittedEnemies;
    private int lastAirEnemies;
    private int lastVenueGround;
    private double lastCloseRangeDamage;
    private int lastWeaponCount;
    private int lastCrippledWeapons;
    private int lastTotalWeapons;
    private int lastCapitalFighter;
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
     * Prices the geometry the stock terms cannot see: whether this pose can shoot anybody, whether it does so
     * from an arc they cannot answer, and what it is risking to get there.
     */
    @Override
    protected double calculateAerospaceMod(MovePath path, Game game, List<Entity> enemies) {
        lastEngagementCredit = 0;
        lastArcAdvantage = 0;
        lastControlRiskPenalty = 0;
        lastVelocityPenalty = 0;
        lastEngageableEnemies = 0;
        lastCommittedEnemies = 0;
        lastAirEnemies = 0;
        lastVenueGround = 0;
        lastCloseRangeDamage = 0;
        lastWeaponCount = 0;
        lastCrippledWeapons = 0;
        lastTotalWeapons = 0;
        lastCapitalFighter = 0;
        lastFinalAltitude = 0;
        lastFinalVelocity = 0;
        lastPosture = null;

        Entity mover = path.getEntity();
        if (!mover.isAero() || !mover.isAirborne() || mover.isSpaceborne()) {
            return 0;
        }

        lastFinalAltitude = path.getFinalAltitude();
        lastFinalVelocity = path.getFinalVelocity();
        AerospaceVenue venue = AerospaceVenue.of(game, mover);
        // Diagnosis columns, kept cheap: which venue this ranking believed it was in, and what the damage
        // helper thinks this airframe can do up close. Every aero term multiplies one of these, so when the
        // TSV shows the terms at zero these two columns say which factor collapsed - the venue read or the
        // damage model - without another instrumented rebuild.
        lastVenueGround = venue.isGroundMap() ? 1 : 0;
        lastCloseRangeDamage = getMaxDamageAtRange(mover, 1, isExtremeRange(game), isLosRange(game));
        for (WeaponMounted weapon : mover.getWeaponList()) {
            lastWeaponCount++;
            if (weapon.isCrippled()) {
                lastCrippledWeapons++;
            }
        }
        lastTotalWeapons = mover.getTotalWeaponList().size();
        lastCapitalFighter = mover.isCapitalFighter() ? 1 : 0;
        // Resolving here is what gives an aerospace force a stance at all. The ground code's two calls to
        // resolvePosture both sit behind guards an airborne aero never passes, so until now a flight had no
        // attack-or-defend answer, and nothing that reads posture applied to it.
        lastPosture = resolveAerospacePosture(game, mover.getBoardId(), venue);
        scoreEngagements(path, game, enemies, venue);
        lastControlRiskPenalty = controlRiskPenalty(path);
        lastVelocityPenalty = velocityPenalty(path, venue);

        return lastEngagementCredit + lastArcAdvantage - lastControlRiskPenalty - lastVelocityPenalty;
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
    private double velocityPenalty(MovePath path, AerospaceVenue venue) {
        if (!venue.isGroundMap() || (lastAirEnemies == 0)) {
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
     * What this path is betting on the airframe by spending more thrust than it safely can.
     *
     * <p>Overthrusting risks a control roll, and a failed one costs 1d6 altitude. That is nearly free high
     * up and close to fatal low down, which is the graded risk the stock code does not model: it treats a
     * control roll the same at altitude 9 as at altitude 2. The odds here are the odds of the drop reaching
     * the ground - a d6 rolling at least the unit's current altitude.</p>
     */
    private double controlRiskPenalty(MovePath path) {
        Entity mover = path.getEntity();
        if (!(mover instanceof IAero aero)) {
            return 0;
        }
        int safeThrust = AeroPathUtil.calculateMaxSafeThrust(aero);
        if (path.getMpUsed() <= safeThrust) {
            return 0;
        }
        return CONTROL_LOSS_COST * oddsOfReachingTheGround(path.getFinalAltitude());
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
        response.addToMyEstimatedDamage(
              getMaxDamageAtRange(mover, range, useExtremeRange, useLOSRange) * UNMOVED_ENEMY_CONFIDENCE);
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
        scores.put("aeroWeaponCount", (double) lastWeaponCount);
        scores.put("aeroCrippledWeapons", (double) lastCrippledWeapons);
        scores.put("aeroTotalWeapons", (double) lastTotalWeapons);
        scores.put("aeroCapitalFighter", (double) lastCapitalFighter);
        scores.put("aeroEngagementCredit", lastEngagementCredit);
        scores.put("aeroArcAdvantage", lastArcAdvantage);
        scores.put("aeroControlRiskPenalty", lastControlRiskPenalty);
        scores.put("aeroVelocityPenalty", lastVelocityPenalty);
        scores.put("aeroFinalVelocity", (double) lastFinalVelocity);
        return scores;
    }
}
