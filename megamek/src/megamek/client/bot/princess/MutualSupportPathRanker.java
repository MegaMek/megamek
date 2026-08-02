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

import megamek.common.analysis.DamageProfile;
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
 * <p>Three changes relative to {@link BasicPathRanker}:</p>
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
 * </ol>
 *
 * <p><b>Invariant: mutual support never impairs closing with or fighting the enemy.</b> The cohesion term
 * applies no penalty to a path that closes toward the unit's engagement band; its weight is capped below the
 * closing signal's weight so off-axis it only breaks ties between comparably aggressive paths; and cover is
 * a bonus among advances, never a gate.</p>
 */
public class MutualSupportPathRanker extends BasicPathRanker {
    private final static MMLogger logger = MMLogger.create(MutualSupportPathRanker.class);

    /** A friend supports out to the range where it still deals this share of its peak expected damage. */
    private static final double SUPPORT_ENVELOPE_FRACTION = 0.5;

    /**
     * Invariant cap: the cohesion weight never exceeds this fraction of the aggression weight, so being
     * out of support can shade a choice between comparably aggressive paths but can never outbid closing.
     */
    private static final double COHESION_WEIGHT_CAP_FACTOR = 0.8;

    /** Utility bonus per set (already moved) friend whose envelope covers the destination. */
    private static final double COVER_BONUS = 2.0;

    /** At most this many covering friends earn the bonus; a whole company stacking adds nothing. */
    private static final int COVER_BONUS_MAX_FRIENDS = 2;

    /**
     * Cover shaping only applies once the destination is within this range of an enemy; out of contact the
     * force moves loose and fast (traveling, not bounding overwatch).
     */
    private static final int THREAT_CONTACT_RANGE = 15;

    /**
     * Reference movement rate used to scale the turns-to-close tempo term back to the magnitude the raw-hex
     * aggression term had for an average Mek, keeping the slider's feel comparable.
     */
    private static final double TEMPO_REFERENCE_MP = 6.0;

    /**
     * A friendly element's engagement envelope, derived from its damage profile once per round.
     *
     * @param peakRange      the range of the profile's peak expected damage - the range the unit closes to
     * @param effectiveRange the longest range at which the unit still deals a meaningful share of its peak -
     *                       the range out to which it supports a friend
     */
    record SupportEnvelope(int peakRange, int effectiveRange) {}

    private final Map<Integer, SupportEnvelope> envelopeCache = new HashMap<>();
    private int envelopeCacheRound = -1;

    // Per-ranking-pass caches. rankPath is called once per candidate path for a single mover, and a
    // company-scale turn evaluates thousands of paths per unit, so anything that depends only on the
    // mover (its friends list, the gap from where it currently stands) must be computed once, not per
    // path. Keyed by mover id and invalidated when the round changes.
    private final Map<Integer, List<Entity>> supportingFriendsCache = new HashMap<>();
    private final Map<Integer, Double> currentGapCache = new HashMap<>();
    private int perMoverCacheRound = -1;

    private void invalidatePerMoverCaches(Game game) {
        int currentRound = game.getCurrentRound();
        if (currentRound != perMoverCacheRound) {
            supportingFriendsCache.clear();
            currentGapCache.clear();
            perMoverCacheRound = currentRound;
        }
    }

    public MutualSupportPathRanker(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Returns the engagement envelope for the given unit, computed from its {@link DamageProfile} (with its
     * own pilot's gunnery) and cached for the round. Overridable for tests.
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
        return envelopeCache.computeIfAbsent(entity.getId(), unitId -> computeSupportEnvelope(entity));
    }

    private SupportEnvelope computeSupportEnvelope(Entity entity) {
        int gunnery = (entity.getCrew() != null) ? entity.getCrew().getGunnery() : 4;
        DamageProfile profile = DamageProfile.of(entity, false, gunnery);
        if (!profile.hasWeapons()) {
            return new SupportEnvelope(0, 0);
        }
        int peakRange = profile.peakExpectedRange();
        double supportThreshold = profile.peakExpectedDamage() * SUPPORT_ENVELOPE_FRACTION;
        int effectiveRange = peakRange;
        for (int range = profile.maxRange(); range > peakRange; range--) {
            if (profile.expectedDamage(range) >= supportThreshold) {
                effectiveRange = range;
                break;
            }
        }
        return new SupportEnvelope(peakRange, effectiveRange);
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
            List<Entity> candidates = getOwner().getBehaviorSettings().isExclusiveHerding()
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
     * (penalties for being beyond every friend's supporting range on a non-closing path) are subtracted from
     * the path utility like the stock herding modifier; negative values are the set-friend cover bonus.
     *
     * <p>Invariant: a path that closes toward the unit's own engagement band pays NO cohesion penalty,
     * and the cohesion weight is capped below the aggression weight - mutual support selects among
     * advancing paths, it never vetoes the advance.</p>
     *
     * @param friendsCoords the stock ally anchor; unused - support is measured to actual nearest elements
     * @param path          the movement path being evaluated
     *
     * @return the mutual-support modifier (subtracted from utility; negative values are a bonus)
     */
    @Override
    protected double calculateHerdingMod(Coords friendsCoords, MovePath path) {
        Entity movingUnit = path.getEntity();
        Game game = getOwner().getGame();

        List<Entity> friends = getSupportingFriends(movingUnit, game);
        if (friends.isEmpty()) {
            logger.trace("[MutualSupport] mod [0: no friends]");
            return 0;
        }

        double supportPenalty = 0;
        boolean closing = engagementGap(movingUnit, path.getFinalCoords(), game)
              < currentEngagementGap(movingUnit, game);
        if (!closing) {
            int hexesBeyondNearestSupport = Integer.MAX_VALUE;
            for (Entity friend : friends) {
                // The friends list is cached for the ranking pass, but a unit can be destroyed during the
                // movement phase (falls, charges, minefields), which clears its position.
                Coords friendPosition = friend.getPosition();
                if (friendPosition == null) {
                    continue;
                }
                int distanceToFriend = friendPosition.distance(path.getFinalCoords());
                int beyondThisFriend = distanceToFriend - getSupportEnvelope(friend).effectiveRange();
                hexesBeyondNearestSupport = Math.min(hexesBeyondNearestSupport, beyondThisFriend);
                if (hexesBeyondNearestSupport <= 0) {
                    break;
                }
            }
            if ((hexesBeyondNearestSupport > 0) && (hexesBeyondNearestSupport != Integer.MAX_VALUE)) {
                double cohesionWeight = Math.min(getOwner().getBehaviorSettings().getHerdMentalityValue(),
                      getOwner().getBehaviorSettings().getHyperAggressionValue() * COHESION_WEIGHT_CAP_FACTOR);
                supportPenalty = hexesBeyondNearestSupport * cohesionWeight;
            }
        }

        double coverBonus = calculateCoverBonus(movingUnit, path, friends, game);

        double mutualSupportMod = supportPenalty - coverBonus;
        logger.trace("[MutualSupport] mod [{} = penalty {} - cover {}{}]",
              mutualSupportMod, supportPenalty, coverBonus, closing ? " (closing: no penalty)" : "");
        return mutualSupportMod;
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

        double aggression = getOwner().getBehaviorSettings().getHyperAggressionValue();
        double aggressionMod = turnsToClose * TEMPO_REFERENCE_MP * aggression;
        logger.trace("[MutualSupport] aggression mod [{} = {} turns to band * {} * {}]",
              aggressionMod, turnsToClose, TEMPO_REFERENCE_MP, aggression);
        return aggressionMod;
    }
}
