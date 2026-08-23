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
package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import megamek.client.bot.princess.AerospaceGeometry;
import megamek.common.ManeuverType;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.IAero;
import megamek.logging.MMLogger;

/**
 * Ground-mapsheet aerospace paths that offer the ranker an altitude to choose from.
 *
 * <p>The stock finder drives every path it generates to a single altitude - {@code OPTIMAL_STRIKE_ALTITUDE},
 * 5 - so however good a ranker is, it never sees an alternative and altitude is settled before scoring
 * begins. Its author left the intent in a comment next to the call: <i>"repeat with 1, 3, 7 when we settle
 * things down?"</i></p>
 *
 * <p>Altitude is the one thing a fighter over a ground mapsheet can actually change inside a turn. It must
 * fly eight to fifty-two hexes in a straight line between single-hexside facing changes (TW p.92), so a
 * velocity-3 fighter crossing the board gets about three turns; it can change altitude every hex. Fixing
 * altitude at a constant removes the only responsive control it has - and, because the air-to-air dead zone
 * is measured in low-altitude hexes, one level of mismatch blocks all fire within seventeen ground hexes.</p>
 *
 * <h2>Keeping the path count down</h2>
 *
 * <p>Every extra candidate multiplies an already large path set, so two things bound it. The candidate list
 * is short and intent-driven rather than a sweep of altitudes 1 to 10, and the generated paths are deduplicated
 * by the altitude they actually reach. That second check does most of the work: the stock altitude adjustment
 * will only climb as far as thrust allows and will only descend one level per turn, so several distinct
 * wishes routinely collapse onto the same reachable altitude and only one copy survives.</p>
 */
public class AeroGroundDoctrinePathFinder extends AeroGroundPathFinder {

    /**
     * Ceiling on distinct altitudes attempted per path.
     *
     * <p>Three covers the cases that matter - hold the strike altitude, meet an opponent who has committed to
     * a different one, and drop to the deck to use bombs - without turning path generation into a sweep.</p>
     */
    static final int MAXIMUM_CANDIDATE_ALTITUDES = 3;

    private static final MMLogger DOCTRINE_LOGGER = MMLogger.create(AeroGroundDoctrinePathFinder.class);

    /**
     * Wing armor difference, as a fraction of original, above which a half-roll is worth offering.
     * TW p.85: the half-roll swaps left and right sides, trading a stripped wing for a fresh one between
     * passes, at 1 thrust and a control modifier of -1 - the only maneuver that makes the roll easier.
     */
    static final double HALF_ROLL_ASYMMETRY = 0.25;

    /**
     * Where the climb-out candidate tops out: high enough that a 1d6 control-loss cannot reach the
     * ground (see the ranker's crash-odds grading), low enough to re-enter the attack windows in one
     * to two turns of descent.
     */
    static final int CLIMB_OUT_CEILING = 7;

    protected AeroGroundDoctrinePathFinder(Game game) {
        super(game);
    }

    /**
     * Runs the stock generation, then adds the special maneuvers (TW p.85) the stock generator has never
     * offered.
     *
     * <p>The maneuver set is intent-gated twice over, because every root multiplies the path count:</p>
     * <ul>
     *     <li><b>Offensive maneuvers require a committed enemy</b> - one that has already moved this turn.
     *     A maneuver spends real thrust and a control roll to buy one specific geometry; buying it against
     *     an opponent who has not moved yet is paying full price for a guess. This mirrors the doctrine
     *     everywhere else in the ranker: react to what has committed, hedge on what has not.</li>
     *     <li><b>Escape maneuvers require an edge trap</b> - the pose's committed straight run crossing the
     *     board edge. A trapped fighter does not need an enemy's permission to save itself: a live game
     *     hung exactly here, a cornered fighter whose every ordinary path left the board, while an
     *     Immelmann out of the corner was sitting in the rules unused.</li>
     * </ul>
     */
    @Override
    public void run(MovePath startingEdge) {
        super.run(startingEdge);
        try {
            getAllComputedPathsUncategorized().addAll(maneuverPaths(startingEdge));
        } catch (Exception exception) {
            // Maneuvers enrich the candidate set; they must never be the reason there is no set at all.
            DOCTRINE_LOGGER.error(exception, "Maneuver generation failed; continuing with ordinary paths");
        }
    }

    /**
     * The maneuver-rooted paths this pose supports, fully extended into ordinary movement.
     */
    private List<MovePath> maneuverPaths(MovePath start) {
        Entity mover = start.getEntity();
        if (!(mover instanceof IAero aero) || !mover.isAirborne()) {
            return List.of();
        }
        // An out-of-control aircraft cannot fly a maneuver, and offering one anyway produced live
        // turns whose ONLY candidate was a doctrine-buried Hammerhead the server would never accept.
        if (aero.isOutControl()) {
            return List.of();
        }
        Board board = game.getBoard(start.getFinalBoardId());
        int velocity = aero.getCurrentVelocity();
        int altitude = start.getFinalAltitude();

        // Generation carries NO enemy-state gates. This finder runs inside Precognition at the start of
        // the movement phase, before any enemy has moved, and an aero unit is never re-enumerated when an
        // enemy commits (markUnitAsDirty skips its neighbor-dirtying step for aero units) - so a gate on
        // committed enemies here would never open in a live game. Every root is generated, and
        // AerospacePathRanker.maneuverSanctioned enforces the doctrine at rank time with fresh state:
        // offensive maneuvers need a committed enemy, escapes need a cornered pose or a committed enemy.
        List<Coords> committedEnemies = committedEnemyPositions(mover);

        // The facing a purposeful maneuver comes out on: at the nearest committed enemy when attacking,
        // at the middle of the board when escaping - anywhere but the edge.
        int purposeFacing = purposeFacing(start, committedEnemies, board);

        List<MovePath> roots = new ArrayList<>();

        // Hammerhead: 180 degrees in the same hex for thrust equal to velocity (+3 control). The overshoot
        // answer - reverse onto an enemy behind you, or out of a corner, without crossing the map to turn.
        if (canPerform(ManeuverType.MAN_HAMMERHEAD, velocity, altitude, board, start)
              && (velocity <= maxThrust)) {
            MovePath root = start.clone();
            root.addManeuver(ManeuverType.MAN_HAMMERHEAD);
            root.addStep(MoveStepType.YAW, true, true, ManeuverType.MAN_HAMMERHEAD);
            roots.add(root);
        }

        // Immelmann: 4 thrust, +1 control - up two altitudes, out on any facing, velocity down two. The
        // reset maneuver: escape the corner, or come around above a committed enemy at controllable speed.
        if (canPerform(ManeuverType.MAN_IMMELMAN, velocity, altitude, board, start)
              && (4 <= maxThrust)) {
            MovePath root = start.clone();
            root.addManeuver(ManeuverType.MAN_IMMELMAN);
            root.addStep(MoveStepType.UP, true, true, ManeuverType.MAN_IMMELMAN);
            root.addStep(MoveStepType.UP, true, true, ManeuverType.MAN_IMMELMAN);
            root.addStep(MoveStepType.DEC, true, true, ManeuverType.MAN_IMMELMAN);
            root.addStep(MoveStepType.DEC, true, true, ManeuverType.MAN_IMMELMAN);
            root.rotatePathfinder(purposeFacing, true, ManeuverType.MAN_IMMELMAN);
            roots.add(root);
        }

        // Split-S: 2 thrust, +2 control - down two altitudes, any facing, velocity up one. Offensive only:
        // dive out of the dead zone onto an enemy below. The altitude guard matches the engine legality
        // check (must end above the ground after dropping 2), kept explicit here as defense-in-depth.
        if (canPerform(ManeuverType.MAN_SPLIT_S, velocity, altitude, board, start)
              && (altitude - 2 >= AerospaceGeometry.MINIMUM_ALTITUDE)
              && (2 <= maxThrust)) {
            MovePath root = start.clone();
            root.addManeuver(ManeuverType.MAN_SPLIT_S);
            root.addStep(MoveStepType.DOWN, true, true, ManeuverType.MAN_SPLIT_S);
            root.addStep(MoveStepType.DOWN, true, true, ManeuverType.MAN_SPLIT_S);
            root.addStep(MoveStepType.ACC, true, true, ManeuverType.MAN_SPLIT_S);
            root.rotatePathfinder(purposeFacing, true, ManeuverType.MAN_SPLIT_S);
            roots.add(root);
        }

        // Loop: 4 thrust, +1 control - spends four velocity going nowhere. The loiter tool for a fighter
        // carrying too much speed for the dogfight it is already in.
        if (canPerform(ManeuverType.MAN_LOOP, velocity, altitude, board, start)
              && (4 <= maxThrust)) {
            MovePath root = start.clone();
            root.addManeuver(ManeuverType.MAN_LOOP);
            root.addStep(MoveStepType.LOOP, true, true, ManeuverType.MAN_LOOP);
            roots.add(root);
        }

        // Side-slips: 1 thrust, no control modifier - on a ground map, eight hexes front-left or
        // front-right and eight more ahead, facing unchanged. Leaves an enemy's arc while keeping the nose
        // on them, and shifts the flown line without a turn.
        for (int type : new int[] { ManeuverType.MAN_SIDE_SLIP_LEFT, ManeuverType.MAN_SIDE_SLIP_RIGHT }) {
            if (canPerform(type, velocity, altitude, board, start)) {
                MovePath root = start.clone();
                root.addManeuver(type);
                MoveStepType lateral = (type == ManeuverType.MAN_SIDE_SLIP_LEFT)
                      ? MoveStepType.LATERAL_LEFT : MoveStepType.LATERAL_RIGHT;
                for (int hex = 0; hex < 8; hex++) {
                    root.addStep(lateral, true, true, type);
                }
                for (int hex = 0; hex < 8; hex++) {
                    root.addStep(MoveStepType.FORWARDS, true, true, type);
                }
                roots.add(root);
            }
        }

        // Half-roll: 1 thrust, -1 control - the only maneuver that makes the roll easier. Swaps a stripped
        // wing for a fresh one, so it is only worth a root when the wings are meaningfully uneven.
        if (wingsAreUneven(mover)
              && canPerform(ManeuverType.MAN_HALF_ROLL, velocity, altitude, board, start)) {
            MovePath root = start.clone();
            root.addManeuver(ManeuverType.MAN_HALF_ROLL);
            root.addStep(MoveStepType.ROLL, true, true, ManeuverType.MAN_HALF_ROLL);
            roots.add(root);
        }

        // Each root then flies out like any other prefix. No altitude fan on top: Immelmann and Split-S
        // choose their own altitude, and the rest keep the pose's - the candidate cap stays honest.
        List<MovePath> results = new ArrayList<>();
        for (MovePath root : roots) {
            results.addAll(GenerateAllPaths(root));
        }
        return results;
    }

    private boolean canPerform(int maneuverType, int velocity, int altitude, Board board, MovePath start) {
        // Ceiling as the human UI supplies it: hex terrain ceiling, and always 0 over a ground map,
        // where aerospace ignores hex elevations (TW p.91).
        return ManeuverType.canPerform(maneuverType, velocity, altitude, 0,
              ((IAero) start.getEntity()).isVSTOL(), 0, board, start);
    }

    /**
     * The facing a maneuver should come out on: toward the nearest committed enemy when there is one,
     * otherwise toward the middle of the board - the whole point of an escape is to end up pointing at
     * open air.
     */
    private int purposeFacing(MovePath start, List<Coords> committedEnemies, Board board) {
        Coords from = start.getFinalCoords();
        if (from == null) {
            return start.getFinalFacing();
        }
        Coords target = null;
        int best = Integer.MAX_VALUE;
        for (Coords enemy : committedEnemies) {
            int distance = from.distance(enemy);
            if (distance < best) {
                best = distance;
                target = enemy;
            }
        }
        if (target == null) {
            target = new Coords(board.getWidth() / 2, board.getHeight() / 2);
        }
        return from.equals(target) ? start.getFinalFacing() : from.direction(target);
    }

    /** Positions of enemy aircraft that have already moved this turn, on this board. */
    private List<Coords> committedEnemyPositions(Entity mover) {
        List<Coords> positions = new ArrayList<>();
        Iterator<Entity> enemies = game.getAllEnemyEntities(mover);
        while (enemies.hasNext()) {
            Entity enemy = enemies.next();
            if (!enemy.isAero() || !enemy.isAirborne() || (enemy.getPosition() == null)) {
                continue;
            }
            if (enemy.getBoardId() != mover.getBoardId()) {
                continue;
            }
            if (enemy.isSelectableThisTurn() && !enemy.isImmobile()) {
                continue;
            }
            positions.add(enemy.getPosition());
        }
        return positions;
    }

    /**
     * @return {@code true} when one wing has lost {@value #HALF_ROLL_ASYMMETRY} more of its armor than the
     *       other, which is what a half-roll exists to fix
     */
    private static boolean wingsAreUneven(Entity mover) {
        double left = wingArmorFraction(mover, Aero.LOC_LEFT_WING);
        double right = wingArmorFraction(mover, Aero.LOC_RIGHT_WING);
        return Math.abs(left - right) >= HALF_ROLL_ASYMMETRY;
    }

    private static double wingArmorFraction(Entity mover, int location) {
        int original = mover.getOArmor(location);
        if (original <= 0) {
            return 1.0;
        }
        return Math.max(0, mover.getArmor(location)) / (double) original;
    }

    public static AeroGroundDoctrinePathFinder getInstance(Game game) {
        return new AeroGroundDoctrinePathFinder(game);
    }

    /**
     * Produces each path at every candidate altitude it can actually reach, instead of only at the strike
     * altitude.
     */
    @Override
    protected List<MovePath> getAltitudeAdjustedPaths(List<MovePath> startingPaths) {
        List<MovePath> adjustedPaths = new ArrayList<>();

        for (MovePath start : startingPaths) {
            boolean choppedOffFlyOff = false;

            // Going off board needs the tail chopped before altitude is applied, and put back after.
            if (start.fliesOffBoard()) {
                start.removeLastStep();
                choppedOffFlyOff = true;
            }

            Set<Integer> altitudesReached = new LinkedHashSet<>();
            for (int desiredAltitude : candidateAltitudes(start.getEntity())) {
                MovePath candidate = adjustTowardsDesiredAltitude(start, desiredAltitude);
                // Several wishes collapse onto one reachable altitude; keep the first and drop the rest.
                if (!altitudesReached.add(candidate.getFinalAltitude())) {
                    continue;
                }
                if (choppedOffFlyOff) {
                    candidate.addStep(MoveStepType.RETURN);
                }
                adjustedPaths.add(candidate);
            }
        }

        return adjustedPaths;
    }

    /**
     * The altitudes worth trying for this unit, most useful first.
     *
     * <p>Ordered by how much is known. An opponent that has already moved has committed to an altitude for
     * the turn, so matching it is the one choice guaranteed to produce an engagement; that comes first. The
     * strike altitude is the fallback that works when nothing is known. Nap-of-the-earth is only worth
     * generating for a unit carrying something to drop, since the stock ranker discards low paths from an
     * aircraft with no bombs anyway.</p>
     *
     * @param mover the unit whose paths are being generated
     *
     * @return distinct candidate altitudes, at most {@value #MAXIMUM_CANDIDATE_ALTITUDES} of them
     */
    protected List<Integer> candidateAltitudes(Entity mover) {
        Set<Integer> altitudes = new LinkedHashSet<>();

        for (int committedAltitude : committedEnemyAltitudes(mover)) {
            altitudes.add(committedAltitude);
            if (altitudes.size() >= MAXIMUM_CANDIDATE_ALTITUDES) {
                break;
            }
        }

        altitudes.add(OPTIMAL_STRIKE_ALTITUDE);

        // The strafe window. Altitude 3 sits inside both the strafe window (1-3) and the dive-bomb
        // window (3-5), so one candidate serves both attacks. Offered only when the airframe
        // carries strafe-eligible guns - the ranker's strafe bid can only buy an altitude that
        // generation offers (the maneuver-gates lesson, second verse: the first live strafe hunt
        // watched the movement side see a five-platoon column, roll in astern, and choose altitude
        // 5, because no strafe-window path ever existed to outbid it).
        if (hasStrafeEligibleGuns(mover)) {
            altitudes.add(STRAFE_WINDOW_ALTITUDE);
        }

        // The climb-out candidate: altitude is banked energy and safety margin, refilled between
        // attack runs and spent on dives. Without it the fighter literally cannot generate a climbing
        // path for ground work, and the only direction the attack windows pull is down - a live
        // Chippewa descended 4-3-2 after its bombs ran out and died to a control roll it could not
        // afford. Two levels up, capped at the safe-recovery ceiling, keeps the porpoise profile
        // available: climb high between runs, drop into the window, climb out again.
        altitudes.add(Math.min(mover.getAltitude() + 2, CLIMB_OUT_CEILING));

        // NoE is deliberately NOT offered. The strafe window candidate above covers gun work at
        // altitude 3; dropping to 1 buys nothing but the NoE +2 to-hit, the strafing dead-zone
        // rules, and a ground that catches every failed control roll.
        // A live Cheetah loitered at altitude 1 for five rounds on this candidate, where any failed
        // control roll is the ground, and died there.

        List<Integer> candidates = new ArrayList<>(altitudes);
        if (candidates.size() > MAXIMUM_CANDIDATE_ALTITUDES) {
            return candidates.subList(0, MAXIMUM_CANDIDATE_ALTITUDES);
        }
        return candidates;
    }

    /** Inside both the strafe (1-3) and dive-bomb (3-5) windows; the gun-work altitude. */
    static final int STRAFE_WINDOW_ALTITUDE = 3;

    /**
     * Whether the airframe carries anything the strafing rules accept: forward-mounted direct-fire
     * energy (TW p.243). Mirrors the server's own legality test, like the bot-side
     * {@code AerospacePathRanker.isStrafeEligible} - the server is the canon both copy.
     */
    private static boolean hasStrafeEligibleGuns(Entity mover) {
        for (WeaponMounted weapon : mover.getWeaponList()) {
            if (!weapon.canFire() || weapon.isRearMounted()
                  || (weapon.getLocation() == Aero.LOC_AFT)) {
                continue;
            }
            WeaponType weaponType = weapon.getType();
            boolean directFireEnergy = (weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                  && (weaponType.hasFlag(WeaponType.F_LASER)
                        || weaponType.hasFlag(WeaponType.F_PPC)))
                  || weaponType.hasFlag(WeaponType.F_FLAMER);
            if (directFireEnergy) {
                return true;
            }
        }
        return false;
    }

    /**
     * Altitudes held by enemy aircraft that have already moved this turn, nearest to the mover first.
     *
     * <p>Only units that have moved are consulted. An opponent still to move may be anywhere in its own
     * reachable band by the time the shooting starts, so matching where it happens to be sitting now is not
     * reacting to anything - it is committing to a guess.</p>
     *
     * <p>Board-filtered, because the game's entity lists span every board and an aircraft over a different
     * map contributes an altitude that means nothing here.</p>
     */
    private List<Integer> committedEnemyAltitudes(Entity mover) {
        List<Entity> committed = new ArrayList<>();
        Iterator<Entity> enemies = game.getAllEnemyEntities(mover);
        while (enemies.hasNext()) {
            Entity enemy = enemies.next();
            if (!enemy.isAero() || !enemy.isAirborne() || (enemy.getPosition() == null)) {
                continue;
            }
            if (enemy.getBoardId() != mover.getBoardId()) {
                continue;
            }
            if (enemy.isSelectableThisTurn() && !enemy.isImmobile()) {
                continue;
            }
            committed.add(enemy);
        }

        if ((committed.size() > 1) && (mover.getPosition() != null)) {
            committed.sort((left, right) -> Integer.compare(mover.getPosition().distance(left.getPosition()),
                  mover.getPosition().distance(right.getPosition())));
        }

        List<Integer> altitudes = new ArrayList<>();
        for (Entity enemy : committed) {
            altitudes.add(enemy.getAltitude());
        }
        return altitudes;
    }

    /**
     * @param mover the unit to check
     *
     * @return {@code true} if the unit still has bombs it could drop on a ground target
     */
    private boolean carriesGroundBombs(Entity mover) {
        return !mover.getBombs(BombType.F_GROUND_BOMB).isEmpty();
    }
}
