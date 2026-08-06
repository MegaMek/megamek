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

import megamek.client.bot.Messages;
import megamek.common.analysis.DamageProfile;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Path ranker implementing the Mutual Support movement doctrine (CASPAR divergence #1): maneuver elements
 * advance on the enemy while remaining within supporting range of one another - close enough that any enemy
 * engaging one element can itself be engaged by another - and commit their combat power together rather than
 * sequentially.
 *
 * <p>Four changes relative to {@link BasicPathRanker}:</p>
 * <ol>
 *     <li><b>Supporting-range cohesion</b> replaces herding: instead of a pull toward the (historical)
 *     friendly center of mass, the unit is penalized only for ending BEYOND the effective weapons envelope
 *     of its nearest friendly element. Inside the envelope, spacing is free - normal combat spread and
 *     flanking cost nothing, so the force cannot collapse into a blob. The envelope is derived per friend
 *     from its {@link DamageProfile}, so staying supported by a short-range brawler means staying closer
 *     than staying supported by a fire-support platform.</li>
 *     <li><b>Set-friend cover bonus</b>: inside the threat envelope, destinations covered by the engagement
 *     envelope of a friend that has already moved (a set base of fire) rank above uncovered ones, so the
 *     covered advance is the best advance and a lone splinter push is the worst one - but still an advance.</li>
 *     <li><b>Uniform closing tempo</b>: the aggression term scores the remaining gap to the unit's own peak
 *     engagement range in TURNS AT ITS OWN SPEED rather than raw hexes, so a 3/5 assault and a 6/9 medium
 *     share one commit tempo (a full move closes one turn's worth for either), and each unit closes to its
 *     own band - brawlers to knife range, fire-support to its optimum - not blindly to contact.</li>
 *     <li><b>Combat posture at water</b>: a defending force does not cross the water it is defending behind -
 *     any water, fords included, since a fordable river is one the enemy can cross anywhere. Whether the
 *     force is attacking or defending is set explicitly in {@link BehaviorSettings}, or read each round from
 *     the mission and the enemy's movement ({@link PostureResolver}); a defender's crossing paths are charged
 *     a full turn of advance, so it holds its bank and fights the enemy in the water instead of wading into
 *     the same trap (see {@code calculatePosturePenalty}).</li>
 * </ol>
 *
 * <p><b>The rule that must always hold: keeping formation never stops a unit closing with the enemy.</b> Two things
 * enforce it. The cohesion term charges nothing for a path ending inside the force's formation, and the centre
 * travels with the force,
 * so advancing <em>as a body</em> is free however fast the body moves - what the term costs is leaving the force
 * behind. And the penalty is bounded: it can never exceed what a turn's advance is worth, however far out of position
 * a unit is (see {@code maximumFormationPenalty}). Cohesion therefore chooses between ways of closing.</p>
 *
 * <p><b>Where "advance as a body" stops being possible, the doctrine gives way.</b> Terrain that a force can only
 * pass a few units at a time - a river ford, a bridge, a city gate - makes someone go first, and going first means
 * leaving the formation. Two things keep that from stalling a crossing: the bound above, and {@link FormationSide},
 * which stops a force split by deep water being measured against a centre sitting in the river. Measured on a river
 * crossing at default settings, one turn charged 13.7 against crossing where a whole turn of advance is worth 37.5 -
 * better than a third of a turn, given up to hold a formation - and companies hesitated at the bank a round at a
 * time. This does not make a crossing free: water entry piloting risk and wading cost are far larger than either
 * figure and still argue against it. It stops cohesion being the term that decides.</p>
 *
 * <p>An earlier version instead exempted any closing path from cohesion entirely. That sounded like the same
 * guarantee and was much weaker: during an approach nearly every path closes, so cohesion switched off for exactly
 * the phase where formation matters. Measured, a company handed a formation 10.5 hexes tighter than its opponent's
 * had given almost all of it back within four rounds.</p>
 */
public class MutualSupportPathRanker extends BasicPathRanker {
    private final static MMLogger logger = MMLogger.create(MutualSupportPathRanker.class);

    /**
     * Ceiling on the cohesion weight, as a fraction of the aggression weight. Being out of position can tip a
     * choice between paths that close by a similar amount, but can never make a unit refuse to close.
     */
    private static final double COHESION_WEIGHT_CAP_FACTOR = 0.8;

    /**
     * How hard a unit is pulled back into its force's formation, per hex it ends up outside it.
     *
     * <p>Sized against the closing tempo it competes with, because a smaller value shapes nothing. The tempo term
     * awards a full move's worth of commitment for closing one turn's travel, so a fast unit that holds back to stay
     * with the force gives up a real fraction of that. To make holding back the better choice the formation term has
     * to be worth a comparable amount per hex, which the raw cohesion weight - about 1.0 at default settings - is
     * nowhere near. This multiplies it to that scale.</p>
     *
     * <p>Raising it further makes the force move at its slowest element's pace even when spread out would serve it
     * better; lowering it returns to a company that arrives in pieces.</p>
     */
    private static final double FORMATION_HOLD_FACTOR = 5.0;

    /**
     * How much say a withdrawing unit keeps in where the fighting formation is.
     *
     * <p>It is leaving the line, so it should not anchor the line - but dropping it to nothing moves the centre of
     * mass discontinuously, and that jump happens exactly when a force starts taking casualties. Measured over four
     * runs, excluding withdrawers outright cost the units still fighting about 0.3 supporting friends each. Its
     * influence fades instead of vanishing: it is walking off the line, not teleporting off it.</p>
     */
    private static final double WITHDRAWING_CENTRE_WEIGHT = 0.25;

    /**
     * Utility bonus per set (already moved) friend whose envelope covers the destination.
     *
     * <p><b>Measured to change nothing at any value, and kept only pending evidence from a scenario this one cannot
     * provide.</b> Isolating it over 200 games moved neither the formation, nor fire support overlap, nor how evenly
     * the force advanced, nor the win rate - a sixfold change in the constant was smaller than the difference between
     * two arms of identical code.</p>
     *
     * <p>It is redundant because the formation term took over its job and does it directly. This was raised to 12.0
     * when cohesion still exempted every closing path, and at 2.0 it sat under the noise floor of fall risk (about 50
     * for one risky piloting roll) and sprint exposure (about 250), shaping nothing. At 12.0 it did shape choices, and
     * badly: it became a second attractor and collapsed the formation back to stock spacing. Now that cohesion is
     * charged against the formation on every path, a unit is already held in a body that is inside its friends'
     * envelopes, so by the time this is consulted the paths it would promote are the ones cohesion has promoted
     * already.</p>
     *
     * <p>Set to the smaller of the two measured values, since the larger is the one shown to distort. It is a
     * candidate for removal: a term that does nothing is a term someone will later tune believing it does something.
     * Removing it wants evidence from a scenario where units genuinely need covering fire, which a company meeting
     * another company in the open does not provide.</p>
     */
    private static final double COVER_BONUS = 2.0;

    /** At most this many covering friends earn the bonus; a whole company stacking adds nothing. */
    private static final int COVER_BONUS_MAX_FRIENDS = 2;

    /**
     * Cover shaping only applies once the destination is within this range of an enemy; out of contact the
     * force moves loose and fast (traveling, not bounding overwatch).
     */
    private static final int THREAT_CONTACT_RANGE = 15;

    /**
     * Reference movement rate used to scale the turns-to-close tempo term: a full move's advance is worth
     * {@code TEMPO_REFERENCE_MP * hyperAggressionValue} to every unit regardless of its speed, which is what
     * puts a 3/5 assault and a 6/9 medium on one commit tempo.
     *
     * <p>Sized against the noise floor rather than for slider parity. The mechanism study measured the
     * competing rank terms at roughly 50 for one risky piloting roll, up to 100 for facing and 250 for
     * sprint exposure, while stock aggression gave a slow assault a whole-turn commit signal of about 7.5 -
     * which is why heavy companies dithered instead of committing. The first benchmark used 6.0 (15 points
     * per move at default aggression), still under that floor, and arrival stagger only improved 12%.
     * Fifteen gives 37.5 per move, above the single-roll term and below the sprint penalty.</p>
     */
    private static final double TEMPO_REFERENCE_MP = 15.0;

    private final Map<Integer, SupportEnvelope> envelopeCache = new HashMap<>();
    private int envelopeCacheRound = -1;

    // Posture is a force-level call, made once per round and per board: every unit on a board moves
    // under the same answer, and enemies on another board have no say in it - a game-wide entity list
    // would blend boards into a meaningless closing rate.
    private final Map<Integer, PostureResolver> postureResolverByBoard = new HashMap<>();
    private final Map<Integer, CombatPosture> postureByBoard = new HashMap<>();
    private int postureResolvedRound = -1;
    private CombatPosture posture = CombatPosture.ATTACK;
    private CombatPosture announcedPosture;
    private double lastPosturePenalty;

    // Bank labels are a property of the board, recomputed per round (ice can break) and shared by
    // every path of every mover. Keyed by board id.
    private final Map<Integer, BankRegions> bankRegionsByBoard = new HashMap<>();
    private int bankRegionsRound = -1;

    // Per-ranking-pass caches. rankPath is called once per candidate path for a single mover, and a
    // company-scale turn evaluates thousands of paths per unit, so anything that depends only on the
    // mover (its friends list, the gap from where it currently stands) must be computed once, not per
    // path. Keyed by mover id and invalidated when the round changes.
    private final Map<Integer, List<Entity>> supportingFriendsCache = new HashMap<>();
    private final Map<Integer, Double> currentGapCache = new HashMap<>();
    private final Map<Integer, Coords> formationCentreCache = new HashMap<>();

    // The reasoning behind the modifiers for the path currently being judged, handed to the TSV in
    // recordDoctrineScores. Path ranking is sequential for a given mover, so plain fields are safe.
    private Coords lastFormationCentre;
    private int lastFormationRadius;
    private int lastHexesOutOfFormation;
    private double lastCoverBonus;
    private int lastCoveringFriends;
    private double lastTurnsToBand;
    private int formationRadiusRound = -1;
    private int cachedFormationRadius = 0;
    private int perMoverCacheRound = -1;

    private void invalidatePerMoverCaches(Game game) {
        int currentRound = game.getCurrentRound();
        if (currentRound != perMoverCacheRound) {
            supportingFriendsCache.clear();
            currentGapCache.clear();
            formationCentreCache.clear();
            perMoverCacheRound = currentRound;
        }
    }

    public MutualSupportPathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Returns the given unit's {@link SupportEnvelope}, cached for the round because computing one walks the
     * unit's whole weapon list and a company turn asks about every unit repeatedly. Overridable for tests.
     *
     * @param entity the unit whose envelope is wanted
     *
     * @return the unit's engagement envelope; zero ranges for a weaponless unit
     */
    protected SupportEnvelope getSupportEnvelope(Entity entity) {
        int currentRound = getOwner().getGame().getCurrentRound();
        if (currentRound != envelopeCacheRound) {
            envelopeCache.clear();
            envelopeCacheRound = currentRound;
        }
        return envelopeCache.computeIfAbsent(entity.getId(), unitId -> SupportEnvelope.of(entity));
    }

    /**
     * The distance still to be closed before the unit reaches its own peak engagement range of the nearest
     * enemy: zero once the unit is inside its band.
     */
    private double engagementGap(Entity movingUnit, Coords position, Game game) {
        double distanceToEnemy = distanceToClosestEnemy(movingUnit, position, game);
        return Math.max(0, distanceToEnemy - getSupportEnvelope(movingUnit).peakRange());
    }

    /**
     * Friendly elements considered for mutual support: the bot's own units (or all friendly units when
     * exclusive herding is off, matching the stock friends list), excluding the mover and anything without
     * a usable position.
     */
    private List<Entity> getSupportingFriends(Entity movingUnit, Game game) {
        invalidatePerMoverCaches(game);
        return supportingFriendsCache.computeIfAbsent(movingUnit.getId(), moverId -> {
            List<Entity> candidates = getOwner().getBehaviorSettings().isExclusiveMutualSupport()
                  ? getOwner().getEntitiesOwned()
                  : getOwner().getFriendEntities();
            List<Entity> friends = new ArrayList<>();
            for (Entity friend : candidates) {
                if ((friend.getId() != moverId)
                      && (friend.getPosition() != null)
                      && !friend.isOffBoard()
                      && game.onTheSameBoard(movingUnit, friend)) {
                    friends.add(friend);
                }
            }
            return friends;
        });
    }

    /** The mover's engagement gap from where it currently stands; constant for the whole ranking pass. */
    private double currentEngagementGap(Entity movingUnit, Game game) {
        invalidatePerMoverCaches(game);
        return currentGapCache.computeIfAbsent(movingUnit.getId(),
              moverId -> engagementGap(movingUnit, movingUnit.getPosition(), game));
    }

    /**
     * Mutual-support modifier, replacing the herding pull toward a center-of-mass point. Positive values
     * (a penalty for ending outside the force's formation) are subtracted from the path utility like the stock
     * cohesion modifier; negative values are the set-friend cover bonus.
     *
     * <p>The rule: ending inside the formation costs nothing, and the formation travels with the force, so moving
     * with your own force is always free - mutual support selects among advancing paths, it never vetoes the
     * advance.</p>
     *
     * @param friendsCoords the stock ally anchor; unused - the formation centre is computed from live positions
     * @param path          the movement path being evaluated
     *
     * @return the mutual-support modifier (subtracted from utility; negative values are a bonus)
     */
    @Override
    protected double calculateMutualSupportMod(Coords friendsCoords, MovePath path) {
        Entity movingUnit = path.getEntity();
        Game game = getOwner().getGame();

        double posturePenalty = calculatePosturePenalty(movingUnit, path, game);

        List<Entity> friends = getSupportingFriends(movingUnit, game);
        if (friends.isEmpty()) {
            // Nothing to form up on, so the doctrine scores nothing - but they are recorded for every path
            // whether or not this ran, so they have to say "nothing" rather than repeat the last path's.
            // Posture is a force-level call, not a formation one, so it still applies to a lone unit.
            clearDoctrineScores();
            logger.trace("[MutualSupport] mod [{}: no friends]", posturePenalty);
            return posturePenalty;
        }

        double supportPenalty = 0;
        Coords formationCentre = formationCentre(movingUnit, friends);
        lastFormationCentre = formationCentre;
        lastFormationRadius = formationRadius(game);
        lastHexesOutOfFormation = 0;
        if (formationCentre != null) {
            int hexesOutOfFormation = formationCentre.distance(path.getFinalCoords()) - formationRadius(game);
            lastHexesOutOfFormation = Math.max(0, hexesOutOfFormation);
            if (hexesOutOfFormation > 0) {
                double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
                double cohesionWeight = Math.min(mutualSupportSetting(), aggression * COHESION_WEIGHT_CAP_FACTOR);
                supportPenalty = Math.min(hexesOutOfFormation * cohesionWeight * FORMATION_HOLD_FACTOR,
                      maximumFormationPenalty(aggression));
            }
        }

        double coverBonus = calculateCoverBonus(movingUnit, path, friends, game);
        lastCoverBonus = coverBonus;

        double mutualSupportMod = supportPenalty - coverBonus + posturePenalty;
        logger.trace("[MutualSupport] mod [{} = out-of-formation {} - cover {} + posture {}]",
              mutualSupportMod, supportPenalty, coverBonus, posturePenalty);
        return mutualSupportMod;
    }

    /**
     * What a defending force charges a path for taking a unit into or across the water it is defending
     * behind - any water, fords included.
     *
     * <p>To a defender the river is its best weapon: the enemy arrives slowed, split into single units by
     * the crossing, and with most of its weapons underwater, and the defender gets to fight that enemy from
     * dry ground. Wading in itself throws all of that away, so a crossing path is charged a full turn of
     * advance ({@link #TEMPO_REFERENCE_MP} times aggression - the same scale the tempo term pays for
     * closing). The charge is finite, not a ban: a big enough prize on the far bank can still buy a
     * crossing.</p>
     *
     * <p>Three kinds of path pay nothing. An attacking force pays nothing anywhere - posture is resolved per
     * round by {@link PostureResolver} unless set explicitly. A unit with somewhere specific to be
     * (forced withdrawal, a destination edge, a waypoint) pays nothing, because its route does not move to
     * suit the terrain. And a unit already standing in the river pays nothing, because the water pricing
     * already argues for the nearest bank and charging every dry destination would trap it mid-stream.</p>
     *
     * @param movingUnit the unit being moved
     * @param path       the path being ranked
     * @param game       the current game
     *
     * @return the posture penalty for this path; zero unless a defending unit is crossing water
     */
    private double calculatePosturePenalty(Entity movingUnit, MovePath path, Game game) {
        lastPosturePenalty = computePosturePenalty(movingUnit, path, game);
        return lastPosturePenalty;
    }

    private double computePosturePenalty(Entity movingUnit, MovePath path, Game game) {
        if (CombatPosture.DEFEND != resolvePosture(game, movingUnit.getBoardId())) {
            return 0;
        }
        BehaviorType behaviorType = getOwner().getUnitBehaviorTracker().getBehaviorType(movingUnit, getOwner());
        if ((BehaviorType.ForcedWithdrawal == behaviorType) || (BehaviorType.MoveToDestination == behaviorType)) {
            return 0;
        }
        if (getOwner().getUnitBehaviorTracker().getWaypointForEntity(movingUnit).isPresent()) {
            return 0;
        }

        // Any water counts as the defender's line, fords included. The formation rules ignore depth 1
        // because a ford does not split a force; a defender's river is the opposite case - a fordable
        // river is one the enemy can cross anywhere, so it is even more a line to hold. Measured: on a
        // river of depth-1 fords, a depth-2 test never fired and the defending company crossed at will.
        //
        // "Same side" is walkable connectivity (BankRegions), not a straight line: on a meandering
        // river the chord between two positions on one bank clips the bends, and a line test charged
        // the defender for repositioning along its own shore - worst exactly at the water's edge, so
        // the force drifted out of its firing positions into dead ground.
        BankRegions banks = bankRegions(game, movingUnit.getBoardId());
        int currentBank = banks.regionOf(movingUnit.getPosition());
        if (BankRegions.WATER == currentBank) {
            return 0;
        }
        if (currentBank == banks.regionOf(path.getFinalCoords())) {
            return 0;
        }

        double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
        double posturePenalty = TEMPO_REFERENCE_MP * aggression;
        logger.trace("[Posture] DEFEND holds the bank: path to {} enters or crosses water, penalty {}",
              path.getFinalCoords(), posturePenalty);
        return posturePenalty;
    }

    /**
     * The bank labels for a board, computed once per round and shared by every path of every mover.
     */
    private BankRegions bankRegions(Game game, int boardId) {
        int round = game.getCurrentRound();
        if (round != bankRegionsRound) {
            bankRegionsRound = round;
            bankRegionsByBoard.clear();
        }
        return bankRegionsByBoard.computeIfAbsent(boardId,
              id -> BankRegions.of(game.getBoard(id), FormationSide.ANY_WATER_DEPTH));
    }

    /**
     * The posture the force fights under this round on the given board, resolved once per round per board
     * and shared by every unit there. Only units on that board have a say: entity lists are game-wide, and
     * in a multi-board game mixing boards would make the closing rate meaningless. When the answer changes -
     * a flip of the auto-resolution or a new explicit order taking effect - the bot says so in the chat,
     * with its reason, so an observer can follow the force's intent without reading logs.
     */
    private CombatPosture resolvePosture(Game game, int boardId) {
        int round = game.getCurrentRound();
        if (round != postureResolvedRound) {
            postureResolvedRound = round;
            postureByBoard.clear();
        }
        posture = postureByBoard.computeIfAbsent(boardId, id -> {
            PostureResolver resolver = postureResolverByBoard.computeIfAbsent(id,
                  newBoard -> new PostureResolver());
            CombatPosture resolved = resolver.resolve(getOwner().getBehaviorSettings(), round,
                  deployedPositions(getOwner().getEntitiesOwned(), id),
                  deployedPositions(getOwner().getEnemyEntities(), id));
            if (resolved != announcedPosture) {
                announcedPosture = resolved;
                getOwner().sendChat(Messages.getString("Princess.posture.announce",
                      resolved, resolver.resolutionReason()));
            }
            return resolved;
        });
        return posture;
    }

    /** The positions of the given units that are deployed on the given board; the rest have no say. */
    static List<Coords> deployedPositions(List<Entity> units, int boardId) {
        List<Coords> positions = new ArrayList<>(units.size());
        for (Entity unit : units) {
            Coords position = unit.getPosition();
            if ((null != position) && unit.isDeployed() && (unit.getBoardId() == boardId)) {
                positions.add(position);
            }
        }
        return positions;
    }

    /**
     * The most utility the formation term may ever cost a single path.
     *
     * <p>The class promises that cohesion can shade a choice between comparably aggressive paths but can never
     * outbid closing. {@link #COHESION_WEIGHT_CAP_FACTOR} alone does not deliver that: it bounds the cost
     * <em>per hex</em>, and the hex count is unbounded, so a unit far enough out of position faced an unbounded
     * penalty. Bounding the total is what makes the promise true by construction.</p>
     *
     * <p>The bound is one turn of closing, which is what {@link #calculateAggressionMod} awards for advancing a
     * full move's worth ({@link #TEMPO_REFERENCE_MP} times aggression), scaled by the same cap factor. So holding
     * formation can never be worth more to a unit than a turn's advance is.</p>
     *
     * <p>It matters at a chokepoint. Where terrain lets a force through only a few hexes at a time - a river ford,
     * a bridge, a city gate - somebody has to go first, and going first means leaving the formation.</p>
     *
     * <p>At default settings the bound is 30, against the 37.5 a full turn of advance is worth. A unit pays it once
     * it is about ten hexes outside its formation; below that the raw penalty is smaller and the bound never
     * applies. The worst single turn measured on a river crossing charged 13.7 - real, better than a third of a
     * turn's advance, but under the bound. <b>So the case for the bound is structural rather than that measurement:
     * without it the penalty grows without limit, and a unit far enough out of position would refuse to advance at
     * any price.</b> The bound is what makes the promise above true by construction.</p>
     *
     * @param aggression the bot's hyper aggression setting
     *
     * @return the ceiling on the formation penalty, in the same utility units as the rest of the ranking
     */
    private static double maximumFormationPenalty(double aggression) {
        return TEMPO_REFERENCE_MP * aggression * COHESION_WEIGHT_CAP_FACTOR;
    }

    /**
     * Resets the recorded reasoning to "nothing applied".
     *
     * <p>{@code BasicPathRanker} records {@link #doctrineScores()} for every path it ranks, including the ones
     * this doctrine bows out of. The fields are per-mover state reused across paths, so a path that scores
     * nothing must say so rather than leave the previous path's figures standing - a reader cannot tell a stale
     * number from a real one, and these columns exist to answer why a unit moved where it did.</p>
     */
    private void clearDoctrineScores() {
        lastFormationCentre = null;
        lastFormationRadius = 0;
        lastHexesOutOfFormation = 0;
        lastCoverBonus = 0;
        lastCoveringFriends = 0;
    }

    /**
     * The point the mover's force is currently gathered on: the centre of mass of its friends.
     *
     * <p>Measured against the centre and never against the nearest friend, for the same reason deployment is.
     * "Stay near <em>somebody</em>" is satisfied by a chain, where every unit has a neighbour while the company is
     * strung across the map. The centre is also what makes this safe for the advance: it moves forward with the
     * force, so a company travelling together is never penalised however fast it goes - only a unit outrunning its
     * own force is.</p>
     *
     * <p><b>A force split by water forms up on its own bank.</b> Averaging across a river puts the centre in the
     * water, which is the one place the formation cannot be, and holds every unit to it. Friends cut off by deep
     * water are therefore left out of the mover's centre - see {@link FormationSide}. Each bank forms up with
     * itself, and the force reassembles on the far side as units cross.</p>
     */
    private @Nullable Coords formationCentre(Entity movingUnit, List<Entity> friends) {
        return formationCentreCache.computeIfAbsent(movingUnit.getId(), moverId -> {
            Board board = getOwner().getGame().getBoard(movingUnit.getBoardId());
            Coords moverPosition = movingUnit.getPosition();
            List<Coords> positions = new ArrayList<>(friends.size());
            List<Double> weights = new ArrayList<>(friends.size());
            for (Entity friend : friends) {
                // Cached for the ranking pass, but a unit can die mid-phase (falls, charges, minefields).
                Coords friendPosition = friend.getPosition();
                if (friendPosition == null) {
                    continue;
                }
                if (!FormationSide.sameSide(board, moverPosition, friendPosition)) {
                    continue;
                }
                double weight = formationWeight(friend);
                if (weight > 0) {
                    positions.add(friendPosition);
                    weights.add(weight);
                }
            }
            return FormationGeometry.weightedCentroid(positions, weights);
        });
    }

    /**
     * Records why the doctrine scored this path the way it did, as extra TSV columns.
     *
     * <p>The modifier totals alone cannot answer "why did the bot do this". These are the inputs behind them: where
     * the force's formation actually was, how wide it was allowed to be, how far outside it this path ended, and how
     * many friends covered the destination. Note that the stock {@code friendsCoords} columns record the heat-map
     * anchor, which this doctrine does not use - {@code formationCentre} is the point it actually measured against.</p>
     */
    @Override
    protected Map<String, Double> doctrineScores() {
        Map<String, Double> scores = new HashMap<>();
        scores.put("formationCentre_x", lastFormationCentre == null ? -1.0 : lastFormationCentre.getX());
        scores.put("formationCentre_y", lastFormationCentre == null ? -1.0 : lastFormationCentre.getY());
        scores.put("formationRadius", (double) lastFormationRadius);
        scores.put("hexesOutOfFormation", (double) lastHexesOutOfFormation);
        scores.put("coverBonus", lastCoverBonus);
        scores.put("coveringFriends", (double) lastCoveringFriends);
        scores.put("turnsToOwnBand", lastTurnsToBand);
        // The force-level posture this path was ranked under (CombatPosture ordinal: 0 attack, 1 defend)
        // and what the posture charged this particular path. Fresh for every path: the penalty is computed
        // at the top of calculateMutualSupportMod before anything can return early.
        scores.put("combatPosture", (double) posture.ordinal());
        scores.put("posturePenalty", lastPosturePenalty);
        return scores;
    }




    /**
     * Whether a unit is part of the formation, and so gets a say in where its centre is.
     *
     * <p>Deliberately the same set of units that {@code BasicPathRanker} holds to the formation: a unit exempt from
     * the pull must not define what everyone else is pulled toward. Three kinds are exempt, and each would otherwise
     * drag the centre somewhere the fighting line is not:</p>
     *
     * <ul>
     *     <li><b>Withdrawing units</b> are running for their home edge. Counting them pulls the centre rearward and
     *     charges the units still advancing for being out of position - a healthy force dragged back toward its own
     *     retreating wounded.</li>
     *     <li><b>Standoff artillery</b> holds at range on purpose, and is exempt so it is never dragged into the
     *     line. It should not drag the line out to it either.</li>
     *     <li><b>Airborne aerospace</b> covers ground it is not holding, so it makes a misleading anchor.</li>
     * </ul>
     */
    private double formationWeight(Entity friend) {
        if (friend.isAirborneAeroOnGroundMap()) {
            // Covers ground it is not holding, so it would anchor the formation somewhere nobody is standing.
            return 0;
        }
        if (isWithdrawing(friend)) {
            return WITHDRAWING_CENTRE_WEIGHT;
        }
        return 1.0;
    }

    /** Whether a unit has left the fighting line to pull back. */
    private boolean isWithdrawing(Entity unit) {
        return getOwner().isFallingBack(unit)
              || getOwner().getUnitBehaviorTracker()
                    .getBehaviorType(unit, getOwner()).equals(BehaviorType.ForcedWithdrawal);
    }

    /**
     * How far from its centre of mass the force may spread, in hexes.
     *
     * <p>Deliberately the same figure {@link MutualSupportDeployment} uses to place the force in the first place, so
     * movement holds the formation deployment handed it rather than inventing its own idea of one. Cached per round:
     * computing it walks every unit's weapons. Overridable for tests.</p>
     */
    protected int formationRadius(Game game) {
        int currentRound = game.getCurrentRound();
        if (currentRound != formationRadiusRound) {
            formationRadiusRound = currentRound;
            cachedFormationRadius = FormationGeometry.formationRadius(getOwner().getEntitiesOwned(),
                  mutualSupportSetting());
        }
        return cachedFormationRadius;
    }

    /**
     * The player's mutual support setting: how tightly this bot keeps its force together.
     *
     * <p>The doctrine's single read of the underlying behavior setting, so no other code here needs to know what it
     * is currently called. The stored name is being changed separately; it is a serialized element in saved behavior
     * presets and appears in Princess's own rankers and configuration UI.</p>
     */
    private double mutualSupportSetting() {
        return getOwner().getBehaviorSettings().getMutualSupportValue();
    }

    /**
     * Bonus for ending within the engagement envelope of a set (already moved) friend while inside the
     * threat envelope: the covered advance is the best advance. Out of contact there is no shaping - the
     * force travels loose and fast.
     */
    private double calculateCoverBonus(Entity movingUnit, MovePath path, List<Entity> friends, Game game) {
        // distanceToClosestEnemy returns -1 when no enemy is on the board; with no one to fight there is
        // nothing to take cover from, so no shaping applies.
        double distanceToEnemy = distanceToClosestEnemy(movingUnit, path.getFinalCoords(), game);
        if ((distanceToEnemy < 0) || (distanceToEnemy > THREAT_CONTACT_RANGE)) {
            lastCoveringFriends = 0;
            return 0;
        }

        int coveringFriends = 0;
        for (Entity friend : friends) {
            Coords friendPosition = friend.getPosition();
            if (!friend.isDone() || (friendPosition == null)) {
                continue;
            }
            int distanceToFriend = friendPosition.distance(path.getFinalCoords());
            if (distanceToFriend <= getSupportEnvelope(friend).effectiveRange()) {
                coveringFriends++;
                if (coveringFriends >= COVER_BONUS_MAX_FRIENDS) {
                    break;
                }
            }
        }
        lastCoveringFriends = coveringFriends;
        return coveringFriends * COVER_BONUS;
    }

    /**
     * Uniform closing tempo, replacing the raw-hex aggression scoring. The remaining gap to the unit's own
     * peak engagement range is measured in turns of movement at the unit's own speed, so every element of
     * the force shares one commit tempo: a full move closes one turn's worth whether the unit walks 3 or
     * runs 9. Zero once inside the band - nothing pulls a fire-support unit past its optimum toward
     * point-blank range.
     *
     * @param movingUnit the unit being moved
     * @param path       the path being evaluated
     * @param game       the current game
     *
     * @return the aggression modifier (subtracted from utility)
     */
    @Override
    protected double calculateAggressionMod(Entity movingUnit, MovePath path, Game game) {
        double remainingGap = engagementGap(movingUnit, path.getFinalCoords(), game);
        int ownSpeed = Math.max(1, Math.max(movingUnit.getRunMP(), movingUnit.getAnyTypeMaxJumpMP()));
        double turnsToClose = remainingGap / ownSpeed;
        lastTurnsToBand = turnsToClose;

        double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
        double aggressionMod = turnsToClose * TEMPO_REFERENCE_MP * aggression;
        logger.trace("[MutualSupport] aggression mod [{} = {} turns to band * {} * {}]",
              aggressionMod, turnsToClose, TEMPO_REFERENCE_MP, aggression);
        return aggressionMod;
    }
}
