/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.equipment.AmmoType.isAmmoValid;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.ArtilleryCommandAndControl.SpecialAmmo;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * This class handles the creation of firing plans for indirect-fire artillery and other weapons that get used during
 * the targeting phase.
 *
 * @author NickAragua
 */
public class ArtilleryTargetingControl {
    private static final MMLogger LOGGER = MMLogger.create(ArtilleryTargetingControl.class);

    private static final int NO_AMMO = -1;

    // biggest known kaboom is the 120 cruise missile with a 4-hex radius, but it's not very common and greatly
    // increases the number of spaces we need to check
    private static final int MAX_ARTILLERY_BLAST_RADIUS = 2;

    // Minimum number of enemies in/around the predicted impact for auto-mode artillery to bother firing, so it does
    // not waste rounds long-shotting a single unit.
    private static final int MIN_AUTO_CLUSTER_UNITS = 2;

    // How much an enemy already covered by another tube's shell this turn is worth when scoring a further shot, so the
    // bot spreads fire across a dispersed cluster instead of dog-piling the densest hex. Covered enemies still carry
    // some value (not zero) so a tube with no fresh target will overlap for extra damage rather than hold.
    private static final double COVERAGE_OVERLAP_DISCOUNT = 0.2;

    // How strongly to prioritize counter-battery fire at an observed off-board enemy battery. The battery keeps shelling
    // us every turn, so its full damage is bumped by this factor before the hit-chance scaling - enough to beat a
    // lone-unit potshot, while a strong on-board cluster (whose summed value is higher) still wins.
    private static final double COUNTER_BATTERY_PRIORITY = 1.0;

    // Multiplier applied to a special damaging munition (FA, cluster, inferno, flechette, etc.) when scoring automatic,
    // non-ordered fire, so the bot prefers plain HE and does not waste situational rounds on a random tie-break. Just
    // below 1.0 so standard wins the tie, but a special still fires when it is the only ammo loaded for that tube.
    private static final double SPECIAL_MUNITION_AUTO_PENALTY = 0.9;

    // Effective hit odds we credit a homing round when a friendly TAG unit can designate a target near its aim point:
    // the round homes onto the TAG instead of scattering, so it is treated as a near-certain hit rather than the ~8%
    // of unobserved indirect fire. This makes the bot spend homing ammo when - and only when - a spotter is in place.
    private static final double TAG_GUIDED_HIT_ODDS = 0.75;

    // per TacOps, this is the to-hit modifier for indirect artillery attacks.
    private static final int ARTILLERY_ATTACK_MODIFIER = 7;

    // The main principle here isn't to try to anticipate enemy movement: that's unlikely, especially for faster or
    // jump-capable units. The main principle instead is to put down fire that
    //
    // - a) may land on enemy units
    // - b) is less likely to land on my units
    //
    // Each potential hex is evaluated as follows:
    // (summed over all units within blast radius of hex) (1/unit run speed + 1) * odds of hitting hex * unit
    // friendliness factor (1 for enemy, -1 for ally) repeat and sum over all hexes within scatter pattern

    // this is a data structure that maps artillery damage value (which directly correlates with blast radius) to a
    // dictionary containing sets of coordinates and the damage value if one of those coordinates were hit by a shell
    // does not take into account hit odds or anything like that
    private Map<Integer, HashMap<Coords, Double>> damageValues = new HashMap<>();

    // The hexes each enemy was predicted to advance to this phase, mapped to the number of enemies feeding that hex
    // (its "heat"), for the optional heat-map visualization
    private final Map<Coords, Integer> heatMapPredictedHexes = new HashMap<>();

    // Ids of enemies already covered by an artillery shell committed earlier this targeting phase (across all the bot's
    // tubes). Subsequent shots discount these enemies so fire spreads to cover more of the cluster. Cleared each phase.
    private final Set<Integer> volleyCoveredEnemyIds = new HashSet<>();

    // Ids of off-board enemy batteries already assigned an opportunistic (Auto) counter-battery shot this targeting
    // phase, so a second tube does not dog-pile a battery one tube already covered. Forced counter-battery ignores this.
    // Cleared each phase.
    private final Set<Integer> counterBatteryAssignedBatteryIds = new HashSet<>();

    private Set<Targetable> targetSet;

    // ordered fire mission hexes that were already reported as impossible to the commanding player,
    // keyed by hex + to-hit failure reason, so a standing order does not spam the chat every round
    private final Set<String> reportedFireMissionWarnings = new HashSet<>();

    /**
     * Wrapper for calculateDamageValue that accounts for leading with artillery shots by accounting for both the
     * original target hexTarget and the computed target hexTarget in damage calculations.
     *
     * @param damage    Base damage of artillery shot
     * @param hexTarget Target {@link HexTarget}
     * @param shooter   Attacking {@link Entity}
     * @param game      {@link Game} instance
     * @param owner     {@link Princess} instance that owns shooter
     *
     * @return Total possible damage this shot can deal based on AE size and base damage
     */
    public double calculateDamageValue(int damage, HexTarget hexTarget, Entity shooter, Game game, Princess owner) {
        // For leading shots, this will be the computed end point. Might contain friendlies. For non-leading shots,
        // this is the original target hexTarget.
        double totalDamage = calculateDamageValue(damage, hexTarget.getPosition(), shooter, game, owner);

        if (null != hexTarget.getOriginalTarget()) {
            // For leading shots, the expected damage is based on the units in and around the _current_ location,
            // which is stored as the "getOriginalTarget".
            totalDamage += calculateDamageValue(damage,
                  hexTarget.getOriginalTarget().getPosition(),
                  shooter,
                  game,
                  owner);
        }
        return totalDamage;
    }

    /**
     * Worker function that calculates the total damage that would be done if a shot with the given damage value would
     * hit the target coordinates. Caches computation results to avoid repeat
     *
     * @param damage  Base damage to artillery shot
     * @param coords  {@link Coords} of the target {@link Hex}, used if Hex is off the board
     * @param shooter Attacking {@link Entity}
     * @param game    The current {@link Game}
     * @param owner   the {@link Princess} bot to calculate for
     */
    public double calculateDamageValue(int damage, Coords coords, Entity shooter, Game game,
          Princess owner) {

        Double damageValue = getDamageValue(damage, coords);

        if (damageValue != null) {
            return damageValue;
        }

        // calculate blast radius = ceiling(damage / 10) - 1
        // for each hex in blast radius, value is
        // (damage - (distance from center * 10)) * [over all units] 1/(unit run MP + 1) * +/-1 (depending on if unit
        // is friendly or not it's not correct for cruise missiles, but I don't think the bot will be using those.
        int blastRadius = (int) Math.ceil(damage / 10.0) - 1;
        double totalDamage = calculateDamageValueForHex(damage, coords, shooter, game, owner);

        // loop around each concentric hex centered on the given coords
        for (int distanceFromCenter = 1; distanceFromCenter <= blastRadius; distanceFromCenter++) {
            // the damage done is actual damage - 10 * # hexes from center
            int currentDamage = damage - distanceFromCenter * 10;

            for (Coords currentCoords : coords.allAtDistance(distanceFromCenter)) {
                totalDamage += calculateDamageValueForHex(currentDamage, currentCoords, shooter, game, owner);
            }
        }

        cacheDamageValue(damage, coords, totalDamage);
        return totalDamage;
    }

    /**
     * Worker function that calculates the "damage value" of a single hex. The formula is (summed over all units in
     * target hex) [incoming damage] * [1 / (unit run mp + 1)] * [-1 if friendly, +1 if enemy]
     *
     * @param damage  How much damage will we do
     * @param coords  Coordinates to hit
     * @param shooter Entity doing the shooting
     * @param game    The current {@link Game}
     */
    private double calculateDamageValueForHex(int damage, Coords coords, Entity shooter, Game game, Princess owner) {
        double value = 0;

        for (Entity entity : game.getEntitiesVector(coords, true)) {
            // ignore aircraft for now, and also transported entities
            if (entity.isAirborne() || entity.isAirborneVTOLorWIGE() || entity.getTransportId() != Entity.NONE) {
                continue;
            }

            // Disincentivize hitting friendlies _strongly_.
            int friendlyMultiplier = -2;

            // try to avoid shooting at friendlies
            // ignore routed enemies who haven't resumed fire
            if (entity.isEnemyOf(shooter)) {
                boolean enemyUnitBroken = owner.getHonorUtil().isEnemyBroken(entity.getId(),
                      shooter.getOwnerId(),
                      owner.getBehaviorSettings().isForcedWithdrawal());

                boolean enemyDishonored = owner.getHonorUtil().isEnemyDishonored(entity.getOwnerId());

                if (!enemyUnitBroken || enemyDishonored) {
                    friendlyMultiplier = 1;
                } else {
                    friendlyMultiplier = 0;
                }
            }

            double speedMultiplier = 1.0 / (entity.getRunMP() + 1);
            double contribution = damage * speedMultiplier * friendlyMultiplier;
            // If another tube already covers this enemy this turn, discount it so fire spreads to uncovered enemies
            // (it keeps some value, so a tube with no fresh target still overlaps for extra damage instead of holding).
            if (entity.isEnemyOf(shooter) && volleyCoveredEnemyIds.contains(entity.getId())) {
                contribution *= COVERAGE_OVERLAP_DISCOUNT;
            }
            value += contribution;
        }

        return value;
    }

    /**
     * Cache a calculated damage value for the given damage/coordinates combo
     *
     * @param damage Base damage of artillery shot
     * @param coords {@link Coords} of the target {@link Hex}, used if Hex is off the board
     * @param value  Total damage for this attack
     */
    private void cacheDamageValue(int damage, Coords coords, Double value) {
        if (!damageValues.containsKey(damage)) {
            damageValues.put(damage, new HashMap<>());
        }

        damageValues.get(damage).put(coords, value);
    }

    /**
     * Retrieve a calculated damage value for the given damage/coords combo
     *
     * @param damage Base damage of artillery shot
     * @param coords {@link Coords} of the target {@link Hex}, used if Hex is off the board
     *
     * @return Calculated total damage
     */
    private Double getDamageValue(int damage, Coords coords) {
        if (damageValues.containsKey(damage)) {
            return damageValues.get(damage).get(coords);
        }

        return null;
    }

    /**
     * Clears out all cached elements in preparation for a new targeting phase.
     */
    public void initializeForTargetingPhase() {
        damageValues = new HashMap<>();
        targetSet = null;
        volleyCoveredEnemyIds.clear();
        counterBatteryAssignedBatteryIds.clear();
    }

    private boolean getAmmoTypeAvailable(Entity shooter, Munitions munitions) {
        boolean available = false;

        for (WeaponMounted weapon : shooter.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                for (AmmoMounted ammo : shooter.getAmmo(weapon)) {
                    if (ammo.getType().getMunitionType().contains(munitions)
                          && !weapon.isFired() && ammo.getUsableShotsLeft() > 0) {
                        available = true;
                        break;
                    }
                }
            }
        }
        return available;

    }

    /**
     * Iterates over all artillery weapons and checks if it can make an ADA attack later in the turn.
     *
     * @param shooter who will make the attack
     *
     * @return true if ADA rounds are available for any weapons, false otherwise
     */
    private boolean getADAAvailable(Entity shooter) {
        return getAmmoTypeAvailable(shooter, Munitions.M_ADA);
    }

    /**
     * Builds a list of eligible targets for artillery strikes. This includes hexes on and within the max radius of all
     * non-airborne enemy entities and hexes on and within the max radius of all strategic targets.
     *
     * @param shooter Entity doing the shooting
     * @param game    The current {@link Game}
     * @param owner   Bot pointer
     */
    private void buildTargetList(Entity shooter, Game game, Princess owner) {
        targetSet = new HashSet<>();
        heatMapPredictedHexes.clear();
        boolean adaAvailable = getADAAvailable(shooter);

        // if we're not in auto mode, we're going to shoot at the targets we've been given.
        if (owner.getArtilleryCommandAndControl().isArtilleryVolley()
              || owner.getArtilleryCommandAndControl().isArtilleryBarrage()
              || owner.getArtilleryCommandAndControl().isArtillerySingle()) {
            for (Coords coords : owner.getArtilleryCommandAndControl().getArtilleryTargets()) {
                targetSet.add(new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY));
            }
            if (!targetSet.isEmpty()) {
                LOGGER.info("{}: artillery fire mission target list built with {} ordered hex(es): {}",
                      owner.getLocalPlayer().getName(), targetSet.size(),
                      owner.getArtilleryCommandAndControl().getArtilleryTargets());
                return;
            }
            LOGGER.warn("{}: artillery fire mission ordered but no target hexes are set - "
                  + "falling through to automatic targeting", owner.getLocalPlayer().getName());
        }
        // Auto mode will target all enemy units it can target
        for (Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext(); ) {
            Entity e = enemies.next();

            // Skip enemies that are not actually on the board yet (still deploying / entering): they have no real
            // position to target, so firing at their phantom hex just wastes rounds.
            if (!e.isDeployed()) {
                continue;
            }

            // Given how accurate and long-ranged ADA missiles are, prioritize airborne targets if ADA is available
            if (adaAvailable) {
                // We will check these first, but still look at other possible shots.
                if (e.isAirborne() || e.isAirborneVTOLorWIGE() || e.isAirborneAeroOnGroundMap()) {
                    targetSet.add(e);
                }
            }

            // Otherwise skip airborne entities, and those off board - we'll handle them later and don't target
            // ignored units
            if (!e.isAirborne() &&
                  !e.isAirborneVTOLorWIGE() &&
                  !e.isOffBoard() &&
                  !owner.getBehaviorSettings().getIgnoredUnitTargets().contains(e.getId())) {

                HexTarget hex = new HexTarget(e.getPosition(), Targetable.TYPE_HEX_ARTILLERY);

                // Lead the shot to where this enemy is predicted to be when the rounds land: advancing toward the
                // nearest friendly unit (us) at its OWN move rate, scaled by the flight time. This aims the heat map
                // at where the enemy is actually heading rather than the direction it last jinked. Homing rounds use
                // the same hex - they home onto a TAG within blast radius of it - so there is no separate (and
                // previously wildly over-led) homing lead.
                Coords predictedPosition = predictEnemyAdvance(e, shooter, game, owner);
                if (!predictedPosition.equals(e.getPosition())) {
                    HexTarget leadHex = new HexTarget(predictedPosition, Targetable.TYPE_HEX_ARTILLERY);
                    leadHex.setOriginalTarget(hex);
                    hex = leadHex;
                }
                targetSet.add(hex);

                // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
                // of the final target.
                addHexDonuts(hex.getPosition(), targetSet, game);
            }
        }

        // TODO: Counter-battery fire must target a hex (TO:AR p 154); needs better off-board unit deploy logic
        int counterBatteryTargets = 0;
        int offBoardEnemiesKnown = 0;
        for (Entity enemy : game.getAllOffboardEnemyEntities(shooter.getOwner())) {
            offBoardEnemiesKnown++;
            if (enemy.isOffBoardObserved(shooter.getOwner().getTeam())) {
                targetSet.add(enemy);
                counterBatteryTargets++;
            }
        }
        // Distinguish the failure modes for a playtest: "0 of 0 known" means the bot cannot even see an enemy off-board
        // battery (none exists, or double-blind hides it); "0 of N known" means it sees the battery but no friendly unit
        // has observed its fall of shot yet; "M of N" means targets are available (the firing loop logs the shot/hold).
        LOGGER.debug("[CounterBattery] {}: {} of {} known off-board enemy batteries are observed and targetable for "
                    + "counter-battery (shooter team {})",
              owner.getLocalPlayer().getName(), counterBatteryTargets, offBoardEnemiesKnown,
              shooter.getOwner().getTeam());

        for (Coords coords : owner.getStrategicBuildingTargets()) {
            targetSet.add(new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY));

            // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS" of the strategic targets.
            addHexDonuts(coords, targetSet, game);
        }
    }

    /**
     * Predicts where an enemy will be when the rounds land: it advances toward our force's centre of mass at its OWN
     * walking speed (a conservative estimate - units rarely sprint straight at the guns every turn), over the shot's
     * flight time (from the TO:AR flight-times table, which already accounts for an off-board battery's distance from
     * the battlefield). The lead is capped a blast radius short of the anchor so it never shells our own position.
     * Falls back to the enemy's current hex when there is no friendly anchor.
     *
     * @param enemy   The enemy being led
     * @param shooter The firing artillery unit
     * @param game    The current game
     * @param owner   The bot
     *
     * @return The predicted impact hex for this enemy
     */
    private Coords predictEnemyAdvance(Entity enemy, Entity shooter, Game game, Princess owner) {
        Coords enemyPos = enemy.getPosition();
        Coords anchor = friendlyForceCentre(enemy, game);
        if ((enemyPos == null) || (anchor == null)) {
            return enemyPos;
        }
        // Lead by the average of walking and the unit's fastest movement (run or jump): walking alone falls behind a
        // unit that runs or jumps toward us, running alone overshoots a cautious advance.
        int fastMove = Math.max(enemy.getRunMP(), enemy.getJumpMP());
        int moveRate = (int) Math.round((enemy.getWalkMP() + fastMove) / 2.0);
        // Flight time per the Indirect Artillery Flight Times Table (TO:AR p149), via turnsTilHit. The shooter's
        // effective distance already accounts for an off-board battery's distance from the battlefield, so this is
        // the real number of turns the enemy gets to move before the rounds land (0 = lands this turn, no lead).
        int flightTime = Compute.turnsTilHit(Compute.effectiveDistance(game, shooter, enemy));
        int distanceToAnchor = enemyPos.distance(anchor);
        // Keep the lead a blast radius short of the anchor so an over-eager prediction never collapses onto - and
        // shells - our own force.
        int maxLead = Math.max(0, distanceToAnchor - MAX_ARTILLERY_BLAST_RADIUS);
        int leadHexes = Math.min(moveRate * flightTime, maxLead);
        if (leadHexes <= 0) {
            return enemyPos;
        }
        int direction = enemyPos.direction(anchor);
        Coords predicted = enemyPos.translated(direction, leadHexes);
        // Count how many enemies are predicted to converge on this hex (accumulate, do not overwrite) so the heat -
        // and the marker opacity it drives - reflects every enemy feeding the hex.
        heatMapPredictedHexes.merge(predicted, 1, Integer::sum);
        LOGGER.info("{}: leading {} (move {}, flight {}) {} hexes toward force centre {} -> predicted impact {}",
              owner.getLocalPlayer().getName(), enemy.getDisplayName(), moveRate, flightTime, leadHexes, anchor,
              predicted);
        return predicted;
    }

    /**
     * Finds the centre of mass of our force - the deployed, on-board units the given enemy opposes - weighted by each
     * unit's battle value (falling back to tonnage) so the enemy is assumed to advance on our main force rather than
     * being pulled toward a single light scout sitting forward.
     *
     * @param enemy The enemy whose opponents (our force) we want the centre of
     * @param game  The current game
     *
     * @return The battle-value-weighted centre of our on-board force, or {@code null} if we have no on-board units
     */
    private Coords friendlyForceCentre(Entity enemy, Game game) {
        long totalWeight = 0;
        long weightedX = 0;
        long weightedY = 0;
        for (Entity friendly : game.getEntitiesVector()) {
            if (friendly.isDeployed() && !friendly.isOffBoard() && (friendly.getPosition() != null)
                  && friendly.isEnemyOf(enemy)) {
                // Battle value best captures combat weight; fall back to tonnage if BV is unavailable.
                long unitWeight = Math.max(friendly.getInitialBV(), Math.round(friendly.getWeight()));
                unitWeight = Math.max(1, unitWeight);
                totalWeight += unitWeight;
                weightedX += (long) friendly.getPosition().getX() * unitWeight;
                weightedY += (long) friendly.getPosition().getY() * unitWeight;
            }
        }
        if (totalWeight == 0) {
            return null;
        }
        return new Coords((int) Math.round(weightedX / (double) totalWeight),
              (int) Math.round(weightedY / (double) totalWeight));
    }

    /**
     * Counts the deployed, on-board enemies within the artillery blast radius of a hex.
     *
     * @param centre  The hex to count around
     * @param shooter The firing artillery unit (used to identify its enemies)
     * @param game    The current game
     *
     * @return The number of enemies within the blast radius of the centre (0 if the centre is {@code null})
     */
    private int enemyCountNear(Coords centre, Entity shooter, Game game) {
        if (centre == null) {
            return 0;
        }
        int count = 0;
        for (Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext(); ) {
            Entity enemy = enemies.next();
            if (enemy.isDeployed() && !enemy.isOffBoard() && (enemy.getPosition() != null)
                  && (centre.distance(enemy.getPosition()) <= MAX_ARTILLERY_BLAST_RADIUS)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the enemies clustered around a candidate shot's impact, used to gate auto-mode fire on a meaningful
     * target. For a leading shot the units are clustered around the shot's original (current) position, so that hex is
     * used as the centre; otherwise the impact hex itself is used.
     *
     * @param fireInfo The candidate shot
     * @param shooter  The firing artillery unit
     * @param game     The current game
     *
     * @return The number of deployed, on-board enemies within the artillery blast radius of the cluster centre
     */
    private int clusterSizeNear(WeaponFireInfo fireInfo, Entity shooter, Game game) {
        Coords centre = fireInfo.getTarget().getPosition();
        if ((fireInfo.getTarget() instanceof HexTarget hexTarget) && (hexTarget.getOriginalTarget() != null)) {
            centre = hexTarget.getOriginalTarget().getPosition();
        }
        return enemyCountNear(centre, shooter, game);
    }

    /**
     * Finds the positions of enemies that one of our own TAG-equipped units could designate by the time the rounds
     * land. This is deliberately predictive: it checks TAG range only, not current line of sight, because the spotter
     * fires TAG in the offboard phase from its post-movement position - so at artillery-targeting time it may be in
     * range but not yet have a clear shot. A homing round aimed within {@link Compute#HOMING_RADIUS} of one of these
     * can be expected to home onto the designated target rather than scatter.
     *
     * @param shooter The firing artillery unit (used to tell friend from foe)
     * @param game    The current game
     * @param owner   The bot
     *
     * @return The set of enemy hexes a friendly TAG unit could designate (its TAG range plus one move's reach)
     */
    private Set<Coords> tagSpottedEnemyPositions(Entity shooter, Game game, Princess owner) {
        Set<Coords> spotted = new HashSet<>();
        List<Targetable> enemies = FireControl.getAllTargetableEnemyEntities(owner.getLocalPlayer(), game,
              owner.getFireControlState());
        for (Entity ally : game.getEntitiesVector()) {
            if (!ally.isDeployed() || ally.isOffBoard() || (ally.getPosition() == null) || ally.isEnemyOf(shooter)) {
                continue;
            }
            int tagRange = maxTagRange(ally);
            if (tagRange <= 0) {
                continue;
            }
            // The spotter fires TAG from its POST-movement position in a later phase (and can move again before the
            // round lands next turn), so credit the reach it can move into, not just the hex it sits in now. Otherwise a
            // mobile spotter that only maneuvers into TAG range during the movement phase - the common case - is
            // invisible at artillery-targeting time, so the bot never trusts a homing round and the ammo goes unused.
            int tagReach = tagRange + ally.getWalkMP();
            for (Targetable enemy : enemies) {
                if ((enemy.getPosition() != null)
                      && (ally.getPosition().distance(enemy.getPosition()) <= tagReach)) {
                    spotted.add(enemy.getPosition());
                }
            }
        }
        return spotted;
    }

    /**
     * @param unit The unit to check
     *
     * @return The longest range of the unit's undamaged TAG weapons, or 0 if it carries no operational TAG
     */
    private int maxTagRange(Entity unit) {
        int range = 0;
        for (WeaponMounted weapon : unit.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_TAG) && !weapon.isDestroyed()) {
                range = Math.max(range, weapon.getType().getLongRange());
            }
        }
        return range;
    }

    /**
     * @param target                A candidate impact hex
     * @param spottedEnemyPositions The hexes a friendly TAG unit can designate this phase
     *
     * @return {@code true} if a homing round aimed at the target hex could home onto a TAG-designated enemy (one within the
     *       homing radius)
     */
    private boolean isGuidedByTag(Coords target, Set<Coords> spottedEnemyPositions) {
        if (target == null) {
            return false;
        }
        for (Coords spotted : spottedEnemyPositions) {
            if (target.distance(spotted) <= Compute.HOMING_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds on-board HexTargets within the MAX_ARTILLERY_BLAST_RADIUS of the given coordinates to the given HexTarget
     * set.
     *
     * @param coords     Center coordinates
     * @param targetList List of target hexes
     * @param game       The current {@link Game}
     */
    private void addHexDonuts(Coords coords, Set<Targetable> targetList, Game game) {
        // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
        // of the designated coordinates
        for (int radius = 1; radius <= MAX_ARTILLERY_BLAST_RADIUS; radius++) {
            for (Coords donutHex : coords.allAtDistance(radius)) {
                // don't bother adding off-board donuts.
                if (game.getBoard().contains(donutHex)) {
                    targetList.add(new HexTarget(donutHex, Targetable.TYPE_HEX_ARTILLERY));
                }
            }
        }
    }

    /**
     * Checks whether the given target is one of the hexes the player ordered a fire mission at via the artillery chat
     * command (barrage/volley/single orders).
     *
     * @param artilleryCommandAndControl The bot's artillery command state
     * @param target                     The target being evaluated
     *
     * @return {@code true} if the target is an ordered fire mission hex
     */
    boolean isOrderedFireMissionTarget(ArtilleryCommandAndControl artilleryCommandAndControl,
          Targetable target) {
        boolean orderedFireMission = artilleryCommandAndControl.isArtilleryBarrage()
              || artilleryCommandAndControl.isArtilleryVolley()
              || artilleryCommandAndControl.isArtillerySingle();
        return orderedFireMission && artilleryCommandAndControl.contains(target.getPosition());
    }

    /**
     * Tells the commanding player in the chat why an ordered fire mission hex cannot be attacked (e.g. inside the
     * 17-hex minimum range for indirect artillery). Each hex/reason combination is only reported once so a standing
     * barrage order does not spam the chat every round.
     *
     * @param owner   The Princess bot
     * @param shooter The artillery unit that cannot make the attack
     * @param target  The ordered fire mission hex
     * @param reason  The to-hit failure reason from the rules engine
     */
    private void reportImpossibleFireMission(Princess owner, Entity shooter, Targetable target, String reason) {
        String warningKey = target.getPosition().toString() + "|" + reason;
        if (reportedFireMissionWarnings.add(warningKey)) {
            owner.sendChat(Messages.getString("Princess.artillery.fireMissionImpossible",
                  shooter.getDisplayName(), target.getPosition().getBoardNum(), reason));
        }
    }

    /**
     * Calculate an indirect artillery "fire plan", taking into account the possibility of rotating the torso or
     * turret: if no attack is possible with the current facing (e.g. an ordered fire mission hex is out of arc),
     * the valid secondary facing changes are tried and the first one that produces attacks is used. The resulting
     * plan includes the necessary torso twist action.
     *
     * @param shooter Entity doing the shooting
     * @param game    The current {@link Game}
     * @param owner   Princess pointer
     *
     * @return Firing plan
     */
    public FiringPlan calculateIndirectArtilleryPlan(Entity shooter, Game game, Princess owner) {
        FiringPlan plan = calculateIndirectArtilleryPlan(shooter, game, owner, 0);
        if (plan.isEmpty()) {
            // only retry with facing changes when a twist could actually help - not when the artillery is
            // halted, the unit already took its volley shot, or there is nothing to shoot at
            ArtilleryCommandAndControl artilleryCommandAndControl = owner.getArtilleryCommandAndControl();
            boolean twistCouldHelp = !artilleryCommandAndControl.isArtilleryHalted()
                  && !(artilleryCommandAndControl.isArtilleryVolley()
                  && artilleryCommandAndControl.hasAlreadyFired(shooter))
                  && (targetSet != null) && !targetSet.isEmpty();
            if (twistCouldHelp) {
                for (int facingChange : FireControl.getValidFacingChanges(shooter)) {
                    FiringPlan twistedPlan = calculateIndirectArtilleryPlan(shooter, game, owner, facingChange);
                    if (!twistedPlan.isEmpty()) {
                        LOGGER.info("{}: {} twisting {} to bring artillery to bear",
                              owner.getLocalPlayer().getName(), shooter.getDisplayName(), facingChange);
                        plan = twistedPlan;
                        break;
                    }
                }
            }
        }
        recordCoveredEnemies(plan, shooter, game);
        showArtilleryHeatMap(shooter, plan, owner);
        return plan;
    }

    /**
     * Records every enemy covered by the blast of each shot in the chosen plan into {@link #volleyCoveredEnemyIds}, so
     * a later tube (on this or another of the bot's units this targeting phase) discounts those enemies and spreads its
     * fire to cover more of the cluster. ADA (anti-air) shots are skipped - they do not blanket a ground hex. Clears
     * the per-hex damage-value cache when the covered set grows so the next shooter re-scores with the discount
     * applied.
     *
     * @param plan    The chosen firing plan
     * @param shooter The firing unit
     * @param game    The current game
     */
    private void recordCoveredEnemies(FiringPlan plan, Entity shooter, Game game) {
        boolean coveredSetGrew = false;
        for (WeaponFireInfo fireInfo : plan) {
            Targetable target = fireInfo.getTarget();
            if ((target == null) || (target.getPosition() == null)
                  || !(fireInfo.getWeapon() != null && fireInfo.getWeapon()
                  .getType() instanceof WeaponType weaponType)) {
                continue;
            }
            // ADA is anti-air and does not blanket the ground hex, so it does not cover ground enemies.
            if ((fireInfo.getAmmo() != null)
                  && fireInfo.getAmmo().getType().getMunitionType().contains(Munitions.M_ADA)) {
                continue;
            }
            // Homing/Copperhead are single-target (no area), so they claim only the unit in their impact hex.
            boolean homing = (fireInfo.getAmmo() != null)
                  && fireInfo.getAmmo().getType().getMunitionType().contains(Munitions.M_HOMING);
            int blastRadius = homing ? 0 : Math.max(0, (int) Math.ceil(weaponType.getRackSize() / 10.0) - 1);
            Coords impactHex = target.getPosition();
            for (Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext(); ) {
                Entity enemy = enemies.next();
                if (enemy.isDeployed() && !enemy.isOffBoard() && (enemy.getPosition() != null)
                      && !enemy.isAirborne() && !enemy.isAirborneVTOLorWIGE()
                      && (impactHex.distance(enemy.getPosition()) <= blastRadius)) {
                    coveredSetGrew |= volleyCoveredEnemyIds.add(enemy.getId());
                }
            }
        }
        if (coveredSetGrew) {
            // The covered set changed, so the cached per-hex damage values are stale for the next shooter.
            damageValues = new HashMap<>();
        }
    }

    /**
     * Collects every hex a friendly (same-team) artillery shell is already aimed at, so this unit spreads its fire
     * instead of dog-piling a hex the team is already hitting. Includes attacks declared by other team units this
     * targeting phase (which coordinates across separate bot players on the team, and the player's own team artillery)
     * and shells already in flight from earlier turns.
     *
     * @param shooter The firing artillery unit (used to tell friend from foe by team)
     * @param game    The current game
     *
     * @return The set of hexes friendly artillery is already targeting (empty if none)
     */
    private Set<Coords> friendlyArtilleryTargetHexes(Entity shooter, Game game) {
        Set<Coords> hexes = new HashSet<>();
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            if (actions.nextElement() instanceof ArtilleryAttackAction artilleryAttack) {
                addFriendlyArtilleryHex(artilleryAttack, shooter, game, hexes);
            }
        }
        for (Enumeration<ArtilleryAttackAction> inFlight = game.getArtilleryAttacks(); inFlight.hasMoreElements(); ) {
            addFriendlyArtilleryHex(inFlight.nextElement(), shooter, game, hexes);
        }
        return hexes;
    }

    /**
     * Adds the target hex of one artillery attack to {@code hexes} if its shooter is on the firing unit's team.
     *
     * @param attack  The artillery attack to inspect
     * @param shooter The firing artillery unit, whose team defines "friendly"
     * @param game    The current game
     * @param hexes   The accumulating set of friendly-targeted hexes
     */
    private void addFriendlyArtilleryHex(ArtilleryAttackAction attack, Entity shooter, Game game, Set<Coords> hexes) {
        Entity attacker = attack.getEntity(game);
        if ((attacker == null) || attacker.isEnemyOf(shooter)) {
            return;
        }
        Targetable attackTarget = attack.getTarget(game);
        if ((attackTarget != null) && (attackTarget.getPosition() != null)) {
            hexes.add(attackTarget.getPosition());
        }
    }

    /**
     * Hands the optional heat-map visualization the hexes each enemy was predicted to advance to and the hexes this
     * plan actually fires at - each tagged with a turn value (the current round for predictions, a countdown of turns
     * until impact for shots) - so a local Princess can paint and track them on the board for testing.
     *
     * @param shooter The firing artillery unit
     * @param plan    The chosen firing plan
     * @param owner   The bot
     */
    private void showArtilleryHeatMap(Entity shooter, FiringPlan plan, Princess owner) {
        Game game = owner.getGame();
        int currentRound = game.getRoundCount();
        Map<Coords, Princess.HeatMapMarker> predictedTargets = new HashMap<>();
        for (Map.Entry<Coords, Integer> predicted : heatMapPredictedHexes.entrySet()) {
            predictedTargets.putIfAbsent(predicted.getKey(),
                  new Princess.HeatMapMarker(currentRound, predicted.getValue()));
        }
        Map<Coords, Princess.HeatMapMarker> chosenTargets = new HashMap<>();
        // Shots being planned this turn are not yet in the in-flight list, so tag them with their full flight time.
        for (WeaponFireInfo fireInfo : plan) {
            if ((fireInfo.getTarget() == null) || (fireInfo.getTarget().getPosition() == null)) {
                continue;
            }
            int turnsTilImpact = 0;
            if (fireInfo.getAction() instanceof ArtilleryAttackAction artilleryAttack) {
                turnsTilImpact = artilleryAttack.getTurnsTilHit();
            }
            chosenTargets.put(fireInfo.getTarget().getPosition(),
                  new Princess.HeatMapMarker(turnsTilImpact, clusterSizeNear(fireInfo, shooter, game)));
        }
        // This shooter's already-in-flight shells: re-tag them each turn with the engine's decremented turns-til-hit so
        // the firing crosshair counts down to impact instead of vanishing after the turn it was fired.
        for (Enumeration<ArtilleryAttackAction> inFlightAttacks = game.getArtilleryAttacks();
              inFlightAttacks.hasMoreElements(); ) {
            ArtilleryAttackAction inFlightAttack = inFlightAttacks.nextElement();
            if (inFlightAttack.getEntityId() != shooter.getId()) {
                continue;
            }
            // getCoords() is the firing position, not the impact hex - use the attack's target so the crosshair lands
            // on the target hex rather than on the firing unit.
            Targetable inFlightTarget = inFlightAttack.getTarget(game);
            if ((inFlightTarget == null) || (inFlightTarget.getPosition() == null)) {
                continue;
            }
            Coords impactHex = inFlightTarget.getPosition();
            chosenTargets.putIfAbsent(impactHex,
                  new Princess.HeatMapMarker(inFlightAttack.getTurnsTilHit(),
                        enemyCountNear(impactHex, shooter, game)));
        }
        owner.showArtilleryHeatMap(predictedTargets, chosenTargets, shooter.getBoardId());
    }

    /**
     * Put together an indirect artillery "fire plan".
     *
     * @param shooter Entity doing the shooting
     * @param game    The current {@link Game}
     * @param owner   Princess pointer
     *
     * @return Firing plan
     */
    private FiringPlan calculateIndirectArtilleryPlan(Entity shooter, Game game, Princess owner, int facingChange) {
        FiringPlan returnValue = new FiringPlan();
        FiringPlan TAGPlan = new FiringPlan();
        ArtilleryCommandAndControl artilleryCommandAndControl = owner.getArtilleryCommandAndControl();
        // if we're fleeing and haven't been shot at, then try not to agitate guys that
        // may pursue us.
        if (artilleryCommandAndControl.isArtilleryHalted()) {
            LOGGER.info("{}: {} artillery is halted, not firing",
                  owner.getLocalPlayer().getName(), shooter.getDisplayName());
            return returnValue;
        }
        if (owner.isFallingBack(shooter) && !owner.canShootWhileFallingBack(shooter)) {
            LOGGER.info("{}: {} is falling back and was not shot at, not firing artillery",
                  owner.getLocalPlayer().getName(), shooter.getDisplayName());
            return returnValue;
        }

        // set the plan's torso twist/turret rotation
        // also set the
        // make sure to remember the entity's original rotation as we're manipulating it
        // directly
        returnValue.setTwist(facingChange);
        int originalFacing = shooter.getSecondaryFacing();
        shooter.setSecondaryFacing(FireControl.correctFacing(originalFacing + facingChange));

        // if we haven't built a target list yet, do so now.
        // potential target list is the same regardless of the entity doing the shooting
        // TODO: allow for counter-battery fire on spotted off-board shooters.
        if (targetSet == null) {
            buildTargetList(shooter, game, owner);
            // If we decided not to shoot this phase, no reason to continue calculating.
            if (targetSet == null || targetSet.isEmpty()) {
                LOGGER.info("{}: {} has no artillery targets this phase",
                      owner.getLocalPlayer().getName(), shooter.getDisplayName());
                return returnValue;
            }
        }
        // when doing volleys, each unit can shoot only once, after all of them have shot, they will have to sit
        // and wait. The shooter is only marked as having fired once we know it actually has an attack to make.
        if (artilleryCommandAndControl.isArtilleryVolley() && artilleryCommandAndControl.hasAlreadyFired(shooter)) {
            LOGGER.info("{}: {} has already fired its volley shot, not firing",
                  owner.getLocalPlayer().getName(), shooter.getDisplayName());
            return returnValue;
        }
        // loop through all weapons on entity
        // each indirect artillery piece randomly picks a target from the priority list
        // by the end of this loop, we either have 0 max damage/0 top valued
        // coordinates, which indicates there's nothing worth shooting at
        // or we have a 1+ top valued coordinates.
        // Track ADA and Flak WFIs separately.
        EnumSet<AmmoType.Munitions> aaMunitions = EnumSet.of(AmmoType.Munitions.M_CLUSTER, AmmoType.Munitions.M_FLAK);
        List<WeaponFireInfo> topValuedFlakInfos = new ArrayList<>();
        boolean artilleryAttackPlanned = false;
        // For an ordered volley, spread this unit's tubes across the ordered hexes: each weapon takes a hex no other
        // weapon on this unit has already been assigned, so one shot is fed to one weapon instead of every tube
        // dog-piling the single best hex.
        Set<Coords> volleyAssignedHexes = new HashSet<>();
        // Hexes a friendly (same-team) artillery shell is already aimed at - declared by other team units this targeting
        // phase or already in flight - so this unit spreads its fire instead of stacking another shell on a hex the team
        // is already hitting. Coordinates across separate bot players on the team and the player's own team artillery.
        Set<Coords> teamArtilleryHexes = friendlyArtilleryTargetHexes(shooter, game);
        // Hexes a friendly TAG unit can designate this phase: a homing round aimed near one of these will home onto the
        // designated target instead of scattering, so it is worth firing homing ammo there.
        Set<Coords> tagSpottedPositions = tagSpottedEnemyPositions(shooter, game, owner);
        // Key homing diagnostic: if this is 0, no friendly TAG is (or can move) in range to guide a homing round, so the
        // bot will not fire homing ammo - the answer to "why didn't it use homing?" A "firing TAG-guided homing" line
        // later means a guided shot was actually chosen.
        LOGGER.debug("[Homing] {}: {} sees {} enemy position(s) a friendly TAG could designate for homing guidance",
              owner.getLocalPlayer().getName(), shooter.getDisplayName(), tagSpottedPositions.size());
        // Homing regression diagnostics: when the shooter carries homing and saw a TAG-spotted enemy but still fires no
        // guided homing, split WHY - shots blocked as impossible (inside min indirect range / no LOS) vs shots that were
        // legal but had no aim hex within the homing radius of a spotted enemy (lead overshoot / aim mismatch).
        boolean shooterHasHomingAmmo = false;
        boolean guidedHomingEligible = false;
        int homingImpossibleCount = 0;
        int homingAimMissCount = 0;
        for (WeaponMounted currentWeapon : shooter.getWeaponList()) {
            List<WeaponFireInfo> topValuedFireInfos = new ArrayList<>();
            double maxDamage = 0;
            // a SINGLE fire mission halts itself after the first weapon fires, which must also stop the
            // remaining artillery weapons on this unit (TAG weapons are still processed below)
            if (currentWeapon.getType().hasFlag(WeaponType.F_ARTILLERY)
                  && !artilleryCommandAndControl.isArtilleryHalted()) {
                WeaponType wType = currentWeapon.getType();
                int damage = wType.getRackSize(); // crazy, but rack size appears to correspond to given damage values
                // for arty pieces in TacOps

                // Iterate over all loaded Artillery ammo so we can compare various options
                for (final AmmoMounted ammo : shooter.getAmmo(currentWeapon)) {
                    // for each enemy unit, evaluate damage value of firing at its hex.
                    // keep track of top target hexes with the same value and fire at them
                    boolean isADA = ammo.getType().getMunitionType().contains(Munitions.M_ADA);
                    boolean isHoming = ammo.getType().getMunitionType().contains(Munitions.M_HOMING);
                    // Classify this ammo bin and note whether it is the special ammo the player ordered, so the
                    // ordered ammo is preferred for ordered hexes and utility (zero-damage) munitions only fire when
                    // explicitly commanded.
                    SpecialAmmo binCategory = SpecialAmmo.forMunitions(ammo.getType().getMunitionType());
                    boolean playerOrderedThisAmmo = (artilleryCommandAndControl.getAmmo() == binCategory);
                    boolean isZeroDamageMunition = binCategory.isUtility();


                    for (Targetable target : targetSet) {
                        // Skip ordered hexes already taken by another tube on this unit during this volley
                        if (artilleryCommandAndControl.isArtilleryVolley()
                              && isOrderedFireMissionTarget(artilleryCommandAndControl, target)
                              && volleyAssignedHexes.contains(target.getPosition())) {
                            continue;
                        }
                        boolean attackOnEntity = (target.getTargetType() == Targetable.TYPE_ENTITY);
                        boolean attackOnAirborneEntity = attackOnEntity &&
                              (target instanceof Entity targetedEntity) &&
                              ((targetedEntity.isAirborne()) ||
                                    (targetedEntity.isAirborneVTOLorWIGE()) ||
                                    (targetedEntity.isAirborneAeroOnGroundMap()));
                        double damageValue;
                        if (isZeroDamageMunition) {
                            // Utility munitions do no damage, so only fire them at an ordered hex when the player
                            // explicitly ordered this utility type.
                            damageValue = 0.0;
                            if (artilleryCommandAndControl.contains(target.getPosition()) && playerOrderedThisAmmo) {
                                damageValue = Integer.MAX_VALUE;
                            }
                        } else {
                            // Flak Artillery need to be made during direct fire, not as Indirect
                            // Other indirect-fire entity-targeting attacks are likely Counter-Battery Fire
                            // and should ignore surrounding targets when computing damage.
                            if (attackOnAirborneEntity || attackOnEntity) {
                                // Homing rounds can't hit flying Aerospace units because TAG can't hit them.
                                boolean homing = ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_HOMING);
                                if (target.isAirborne() && homing) {
                                    damageValue = 0;
                                } else if (attackOnEntity && !attackOnAirborneEntity && target.isOffBoard()) {
                                    // Counter-battery fire at an observed off-board enemy battery. Homing is never usable
                                    // here - there is no off-board TAG to guide it - so a homing bin scores zero. In
                                    // forced counter-battery mode the player-ordered ammo is valued so high it always
                                    // wins. Otherwise it is opportunistic: valued at the weapon's damage so it competes
                                    // fairly with on-board targets (it does not abandon a real on-board threat for a
                                    // battery), and a battery another tube already engaged this phase is heavily
                                    // discounted so the bot does not dog-pile both tubes onto one battery.
                                    if (isHoming) {
                                        damageValue = 0;
                                    } else if (artilleryCommandAndControl.isCounterBattery() && playerOrderedThisAmmo) {
                                        damageValue = Integer.MAX_VALUE;
                                    } else if (counterBatteryAssignedBatteryIds.contains(target.getId())) {
                                        damageValue = damage * COVERAGE_OVERLAP_DISCOUNT;
                                    } else {
                                        damageValue = damage * COUNTER_BATTERY_PRIORITY;
                                    }
                                } else {
                                    damageValue = damage;
                                }
                            } else {
                                if (!isADA) {
                                    if (isOrderedFireMissionTarget(artilleryCommandAndControl, target)) {
                                        // Hexes from a player fire mission order (barrage/volley/single) are
                                        // commands: fire at them even if no units are near them (e.g. area
                                        // denial), instead of valuing them by expected damage to nearby units.
                                        damageValue = damage;
                                        // When the player ordered a specific damage munition, prefer that ammo for
                                        // the ordered hexes so it is chosen over standard rounds.
                                        if (playerOrderedThisAmmo
                                              && (artilleryCommandAndControl.getAmmo() != SpecialAmmo.STANDARD)) {
                                            damageValue = Integer.MAX_VALUE;
                                        }
                                    } else {
                                        damageValue = calculateDamageValue(damage,
                                              (HexTarget) target,
                                              shooter,
                                              game,
                                              owner);
                                        // Prefer plain HE for automatic (non-ordered) fire: a special damaging munition
                                        // (FA, cluster, inferno, flechette) scores the same as standard here because the
                                        // value is launcher-damage based, so without this the bot would pick among the
                                        // tied bins at random and waste situational rounds. Discount non-standard,
                                        // non-homing bins so standard wins the tie, but a special still fires if it is
                                        // the only ammo loaded.
                                        if (!playerOrderedThisAmmo
                                              && (binCategory != SpecialAmmo.STANDARD)
                                              && !isHoming) {
                                            damageValue *= SPECIAL_MUNITION_AUTO_PENALTY;
                                        }
                                        // Spread the barrage: if a friendly shell (this unit's other tube, an allied
                                        // bot, or the player's own team artillery) is already aimed at this hex,
                                        // discount it so the tube picks an offset hex - two offset blasts cover more
                                        // ground than two stacked on the same spot.
                                        if (volleyAssignedHexes.contains(target.getPosition())
                                              || teamArtilleryHexes.contains(target.getPosition())) {
                                            damageValue *= COVERAGE_OVERLAP_DISCOUNT;
                                        }
                                    }
                                } else {
                                    // No ADA attacks except at Entities; no Flak attacks except direct fire
                                    continue;
                                }
                            }
                        }

                        // ADA attacks should be handled as Direct Fire but we'll calc hits here for
                        // comparison.
                        WeaponFireInfo wfi = new WeaponFireInfo(shooter,
                              target,
                              currentWeapon,
                              ammo,
                              game,
                              false,
                              owner);

                        // A homing round aimed within homing radius of a hex a friendly TAG unit can designate will
                        // home onto that target, so it counts as a near-certain hit rather than scatter - but only if
                        // the shot is actually legal: a tube inside its minimum indirect range cannot fire at all, no
                        // matter how good the spotter, so don't credit an impossible shot as a guided hit.
                        boolean guidedByTag = isHoming
                              && (target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
                              && !wfi.getToHit().cannotSucceed()
                              && isGuidedByTag(target.getPosition(), tagSpottedPositions);
                        if (isHoming) {
                            shooterHasHomingAmmo = true;
                            if (guidedByTag) {
                                guidedHomingEligible = true;
                            } else if ((target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY)
                                  && !tagSpottedPositions.isEmpty()) {
                                if (wfi.getToHit().cannotSucceed()) {
                                    homingImpossibleCount++;
                                } else {
                                    homingAimMissCount++;
                                }
                            }
                        }

                        // factor the chance to hit when picking a target - if we've got a spotted hex
                        // or an auto-hit hex
                        // we should prefer to hit that over something that may scatter to who knows
                        // where
                        if (guidedByTag) {
                            // Credit the homing round as a TAG-guided hit so it is chosen over unguided scatter shots.
                            double guidedValue = damage * TAG_GUIDED_HIT_ODDS;
                            wfi.getAmmo().setSwitchedReason(1505);
                            if (guidedValue > maxDamage) {
                                topValuedFireInfos.clear();
                                maxDamage = guidedValue;
                                topValuedFireInfos.add(wfi);
                            } else if (guidedValue == maxDamage) {
                                topValuedFireInfos.add(wfi);
                            }
                        } else if (wfi.getProbabilityToHit() > 0) {
                            damageValue *= wfi.getProbabilityToHit();

                            if (damageValue > maxDamage) {
                                if ((wfi.getAmmo().getType()).getMunitionType()
                                      .contains(Munitions.M_HOMING)) {
                                    wfi.getAmmo().setSwitchedReason(1505);
                                } else {
                                    wfi.getAmmo().setSwitchedReason(1503);
                                }
                                if (attackOnAirborneEntity &&
                                      (isADA ||
                                            wfi.getAmmo()
                                                  .getType()
                                                  .getMunitionType()
                                                  .stream()
                                                  .anyMatch(aaMunitions::contains) ||
                                            wfi.getAmmo().getType().countsAsFlak())) {
                                    // Handle Flak attacks during Direct Fire
                                    topValuedFlakInfos.clear();
                                    maxDamage = damage;
                                    topValuedFlakInfos.add(wfi);
                                } else {
                                    topValuedFireInfos.clear();
                                    maxDamage = damageValue;
                                    topValuedFireInfos.add(wfi);
                                }
                            } else if ((damageValue == maxDamage) && (damageValue > 0)) {
                                if (attackOnAirborneEntity && (wfi.getAmmo().getType().getMunitionType()
                                      .contains(Munitions.M_ADA) || wfi.getAmmo()
                                      .getType()
                                      .getMunitionType()
                                      .stream()
                                      .anyMatch(aaMunitions::contains)
                                      || wfi.getAmmo().getType().countsAsFlak())) {
                                    topValuedFlakInfos.add(wfi);
                                } else {
                                    topValuedFireInfos.add(wfi);
                                }
                            }
                        } else if (artilleryCommandAndControl.isHomingAmmo()
                              && ammo.getType().getMunitionType().contains(AmmoType.Munitions.M_HOMING)
                              && isOrderedFireMissionTarget(artilleryCommandAndControl, target)
                              && !wfi.getToHit().cannotSucceed()) {
                            // Player ordered a homing fire mission: fire it trusting a friendly TAG will designate
                            // this turn, even though no spotter is confirmed now. We still require the shot itself to
                            // be legal (e.g. not too short for indirect fire) - only the TAG confirmation is relaxed.
                            // The round simply fails at impact if no TAG lands (the wasted-round risk the player took).
                            if (damageValue > maxDamage) {
                                topValuedFireInfos.clear();
                                maxDamage = damageValue;
                                topValuedFireInfos.add(wfi);
                            } else if (damageValue == maxDamage) {
                                topValuedFireInfos.add(wfi);
                            }
                            LOGGER.info("{}: {} firing ordered homing mission at {} without a confirmed TAG - "
                                        + "trusting the player to designate",
                                  owner.getLocalPlayer().getName(), shooter.getDisplayName(), target.getPosition());
                        } else if (isOrderedFireMissionTarget(artilleryCommandAndControl, target)) {
                            // an ordered fire mission hex the weapon cannot hit at all is worth surfacing
                            LOGGER.warn("{}: {} cannot hit ordered fire mission hex {} with {} ({}): {}",
                                  owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                                  target.getPosition(), currentWeapon.getName(),
                                  ammo.getType().getShortName(), wfi.getToHit().getDesc());
                            reportImpossibleFireMission(owner, shooter, target, wfi.getToHit().getDesc());
                        }
                    }
                }
                // this section is long and obnoxious:
                // Pick a random fire info out of the ones with the top damage level
                // Use that to create an artillery attack action, set the action's ammo
                // then set the fire info's attack action to the created attack action
                // add the fire info to the firing plan
                if (!topValuedFireInfos.isEmpty()) {
                    WeaponFireInfo actualFireInfo;
                    if (topValuedFireInfos.size() == 1) {
                        actualFireInfo = topValuedFireInfos.getFirst();
                    } else {
                        // lets choose from the top 5 if we have that many
                        int topValues = Math.min(5, topValuedFireInfos.size());
                        actualFireInfo = topValuedFireInfos.get(Compute.randomInt(topValues));
                        if (!actualFireInfo.getAmmo().equals(actualFireInfo.getWeapon().getLinked())) {
                            // Announce why we switched
                            actualFireInfo.getAmmo().setSwitchedReason(1507);
                        }
                    }
                    // In auto mode, only fire when the predicted impact covers a meaningful cluster of enemies -
                    // don't waste rounds on a long-shot at a single unit. Ordered fire missions and TAG-guided homing
                    // shots (a precise, near-certain hit on a designated target) bypass this.
                    boolean guidedHomingShot = actualFireInfo.getAmmo().getType().getMunitionType()
                          .contains(Munitions.M_HOMING)
                          && isGuidedByTag(actualFireInfo.getTarget().getPosition(), tagSpottedPositions);
                    // Counter-battery fire at an observed off-board battery is a precision strike at a known target, not
                    // area fire at a cluster, so it bypasses the "needs 2+ enemies in the blast" auto gate.
                    boolean counterBatteryShot =
                          (actualFireInfo.getTarget().getTargetType() == Targetable.TYPE_ENTITY)
                                && actualFireInfo.getTarget().isOffBoard();
                    if (!isOrderedFireMissionTarget(artilleryCommandAndControl, actualFireInfo.getTarget())
                          && !guidedHomingShot
                          && !counterBatteryShot
                          && (clusterSizeNear(actualFireInfo, shooter, game) < MIN_AUTO_CLUSTER_UNITS)) {
                        LOGGER.info("{}: {} holding {} - predicted impact at {} covers fewer than {} units",
                              owner.getLocalPlayer().getName(), shooter.getDisplayName(), currentWeapon.getName(),
                              actualFireInfo.getTarget().getPosition(), MIN_AUTO_CLUSTER_UNITS);
                        continue;
                    }
                    if (guidedHomingShot) {
                        LOGGER.info("{}: {} firing TAG-guided homing at {} - friendly spotter can designate a target "
                                    + "within homing radius",
                              owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                              actualFireInfo.getTarget().getPosition());
                    }
                    if (counterBatteryShot) {
                        // Record the battery so another tube this phase does not dog-pile it (opportunistic fire only;
                        // a forced counter-battery order ignores this and still commits every tube).
                        counterBatteryAssignedBatteryIds.add(actualFireInfo.getTarget().getId());
                        LOGGER.info("{}: {} firing counter-battery at observed off-board enemy battery {}",
                              owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                              actualFireInfo.getTarget().getDisplayName());
                    }

                    ArtilleryAttackAction aaa = (ArtilleryAttackAction) actualFireInfo.buildWeaponAttackAction();
                    HelperAmmo ammo = findAmmo(shooter, actualFireInfo.getWeapon(), actualFireInfo.getAmmo());

                    if (ammo.equipmentNum > NO_AMMO) {
                        // This can happen if princess is towing ammo trailers, which she really
                        // shouldn't be doing...
                        aaa.setAmmoId(ammo.equipmentNum);
                        aaa.setAmmoMunitionType(ammo.munitionType);
                        aaa.setAmmoCarrier(shooter.getId());
                        actualFireInfo.setAction(aaa);
                        returnValue.add(actualFireInfo);
                        returnValue.setUtility(returnValue.getUtility() + maxDamage);
                        artilleryAttackPlanned = true;
                        // Reserve this hex so the unit's remaining tubes spread to other hexes - for ordered volleys and
                        // for automatic fire alike - instead of stacking another shell on the same spot.
                        volleyAssignedHexes.add(actualFireInfo.getTarget().getPosition());
                        LOGGER.info("{}: {} firing {} ({}) at {} (expected value {}, probability {})",
                              owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                              currentWeapon.getName(), actualFireInfo.getAmmo().getType().getShortName(),
                              actualFireInfo.getTarget().getPosition(),
                              maxDamage, actualFireInfo.getProbabilityToHit());
                        owner.sendAmmoChange(
                              shooter.getId(),
                              shooter.getEquipmentNum(actualFireInfo.getWeapon()),
                              ammo.equipmentNum,
                              actualFireInfo.getAmmo().getSwitchedReason());

                        if (artilleryCommandAndControl.isArtillerySingle()) {
                            // a single fire mission is complete after one weapon has actually fired:
                            // halt the artillery and clear the ordered targets
                            artilleryCommandAndControl.setArtilleryOrder(ArtilleryCommandAndControl.ArtilleryOrder.HALT);
                            artilleryCommandAndControl.removeArtilleryTargets();
                            LOGGER.info("{}: single fire mission complete, artillery halted",
                                  owner.getLocalPlayer().getName());
                        }
                    } else {
                        LOGGER.warn("{}: {} found a fire solution for {} but could not match its ammo bin - "
                                    + "shot dropped",
                              owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                              currentWeapon.getName());
                    }
                } else {
                    LOGGER.info("{}: {} found no valid fire solution for {} against {} target(s) "
                                + "(all zero expected value or zero hit probability)",
                          owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                          currentWeapon.getName(), targetSet.size());
                }
            } else if (currentWeapon.getType().hasFlag(WeaponType.F_TAG)) {
                WeaponFireInfo tagInfo = getTAGInfo(currentWeapon, shooter, game, owner);

                if (tagInfo != null) {
                    boolean designated = owner.getDesignatedTagTargets().contains(tagInfo.getTarget().getId());
                    LOGGER.info("[TAG] {}: {} firing TAG at {} (probability {}){}",
                          owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                          tagInfo.getTarget().getDisplayName(), tagInfo.getProbabilityToHit(),
                          designated ? " - player-designated TAG target" : "");
                    TAGPlan.add(tagInfo);
                    TAGPlan.setUtility(returnValue.getUtility() + tagInfo.getProbabilityToHit());
                } else {
                    LOGGER.info("[TAG] {}: {} has a TAG but found no target to TAG this phase",
                          owner.getLocalPlayer().getName(), shooter.getDisplayName());
                }
            }
        }

        // Clear all artillery attacks if we have valid ADA or Flak attacks that do damage, but
        // keep any TAG attacks.
        if (!topValuedFlakInfos.isEmpty()) {
            if (topValuedFlakInfos.getFirst().getExpectedDamage() > 0) {
                returnValue = TAGPlan;
            }
        } else {
            for (WeaponFireInfo tagInfo : TAGPlan) {
                returnValue.add(tagInfo);
                returnValue.setUtility(returnValue.getUtility() + tagInfo.getProbabilityToHit());
            }
        }

        // when doing a volley, only mark the shooter as having taken its shot if it actually fired -
        // otherwise a unit that could not fire this round would lose its volley shot without shooting
        if (artilleryCommandAndControl.isArtilleryVolley() && artilleryAttackPlanned) {
            artilleryCommandAndControl.setShooter(shooter);
        }

        shooter.setSecondaryFacing(originalFacing);

        if (shooterHasHomingAmmo && !guidedHomingEligible && !tagSpottedPositions.isEmpty()) {
            LOGGER.debug("[Homing] {}: {} carries homing and saw {} TAG-spotted enemies but NO shot was guidable - "
                        + "{} homing shots impossible (inside min indirect range / no LOS), {} legal but no aim hex "
                        + "within homing radius of a spotted enemy (lead overshoot)",
                  owner.getLocalPlayer().getName(), shooter.getDisplayName(), tagSpottedPositions.size(),
                  homingImpossibleCount, homingAimMissCount);
        }

        return returnValue;
    }

    /**
     * Worker function that calculates the shooter's "best" actions that result in a TAG being fired.
     *
     * @param shooter Attacking {@link Entity}
     * @param game    The current {@link Game}
     * @param owner   {@link Princess} instance that owns shooter
     *
     * @return Highest hit-chance TAG attack's {@link WeaponFireInfo}, preferring a player-designated target when one
     *       is hittable
     */
    private WeaponFireInfo getTAGInfo(WeaponMounted weapon, Entity shooter, Game game, Princess owner) {
        WeaponFireInfo returnValue = null;
        WeaponFireInfo designatedFireInfo = null;
        double hitOdds = 0.0;
        double designatedHitOdds = 0.0;
        int enemiesEvaluated = 0;
        // Accumulate why each unhittable enemy was rejected (built in the loop, logged once after) so a playtest can
        // tell why a TAG unit did not designate - out of range, no line of sight, bad arc, etc.
        StringBuilder rejectedReasons = new StringBuilder();

        // take the best shot you have, but prefer an enemy the player designated for TAG
        for (Targetable target : FireControl.getAllTargetableEnemyEntities(owner.getLocalPlayer(), game,
              owner.getFireControlState())) {
            enemiesEvaluated++;
            WeaponFireInfo wfi = new WeaponFireInfo(shooter, target, weapon, null, game, false, owner);
            boolean isDesignated = owner.getDesignatedTagTargets().contains(target.getId());
            if (isDesignated && (wfi.getProbabilityToHit() > designatedHitOdds)) {
                designatedHitOdds = wfi.getProbabilityToHit();
                designatedFireInfo = wfi;
            }
            if (wfi.getProbabilityToHit() > hitOdds) {
                hitOdds = wfi.getProbabilityToHit();
                returnValue = wfi;
            } else if (wfi.getProbabilityToHit() <= 0) {
                if (rejectedReasons.length() > 0) {
                    rejectedReasons.append("; ");
                }
                rejectedReasons.append(target.getDisplayName()).append(": ").append(wfi.getToHit().getDesc());
            }
        }

        // a designated target the bot can actually hit wins; otherwise fall back to the best available shot
        WeaponFireInfo chosen = (designatedFireInfo != null) ? designatedFireInfo : returnValue;
        if (chosen == null) {
            LOGGER.debug("[TAG] {}: {} found no TAG target ({} enemies evaluated){}",
                  owner.getLocalPlayer().getName(), shooter.getDisplayName(), enemiesEvaluated,
                  (rejectedReasons.length() > 0) ? " - " + rejectedReasons : "");
        } else {
            LOGGER.debug("[TAG] {}: {} best TAG target {} at probability {}{}",
                  owner.getLocalPlayer().getName(), shooter.getDisplayName(),
                  chosen.getTarget().getDisplayName(), chosen.getProbabilityToHit(),
                  (designatedFireInfo != null) ? " (player-designated)" : "");
        }
        return chosen;
    }

    private static class HelperAmmo {
        public int equipmentNum;
        public EnumSet<Munitions> munitionType;

        public HelperAmmo(int equipmentNum, EnumSet<Munitions> munitionType) {
            this.equipmentNum = equipmentNum;
            this.munitionType = munitionType;
        }
    }

    /**
     * Worker function that selects the appropriate ammo for the given entity and weapon.
     *
     * @param shooter       Attacking {@link Entity}
     * @param currentWeapon {@link Mounted} instance being used for this attack
     *
     * @return {@link AmmoMounted} to be used to attack with this weapon
     */
    private HelperAmmo findAmmo(Entity shooter, Mounted<?> currentWeapon, Mounted<?> preferredAmmo) {
        int ammoEquipmentNum = NO_AMMO;
        EnumSet<Munitions> ammoMunitionType = EnumSet.noneOf(Munitions.class);

        if (preferredAmmo != null && preferredAmmo.isAmmoUsable() &&
              isAmmoValid(preferredAmmo, (WeaponType) currentWeapon.getType())) {
            // Use the ammo we used for calculations.
            ammoEquipmentNum = shooter.getEquipmentNum(preferredAmmo);
            ammoMunitionType = ((AmmoType) preferredAmmo.getType()).getMunitionType();
        } else {
            // simply grab the first valid ammo and let 'er rip.
            for (Mounted<?> ammo : shooter.getAmmo()) {
                if (!ammo.isAmmoUsable() || !isAmmoValid(ammo, (WeaponType) currentWeapon.getType())) {
                    continue;
                }

                ammoEquipmentNum = shooter.getEquipmentNum(ammo);
                ammoMunitionType = ((AmmoType) ammo.getType()).getMunitionType();
                break;

                // TODO: Attempt to select homing ammo if the target is tagged.
                // To do so, check
                // ammoType.getMunitionType().contains(Munitions.M_HOMING)
            }
        }

        return new HelperAmmo(ammoEquipmentNum, ammoMunitionType);
    }

    /**
     * Function that calculates the potential damage if an artillery attack were to land on target.
     *
     * @param coords   {@link Coords} of the target {@link Hex}, used if Hex is off the board
     * @param operator {@link Princess} instance who is checking for incoming artillery damage
     *
     * @return Damage value calculated from incoming shots that may hit these coordinates
     */
    public static double evaluateIncomingArtilleryDamage(Coords coords, Princess operator) {
        double sum = 0;

        for (Enumeration<ArtilleryAttackAction> attackEnum = operator.getGame().getArtilleryAttacks(); attackEnum
              .hasMoreElements(); ) {
            ArtilleryAttackAction aaa = attackEnum.nextElement();

            // calculate damage: damage - (10 * distance to me), floored at 0
            // Count attacks landing this movement phase (turnsTilHit 0) AND those just fired this turn that land next
            // turn (turnsTilHit 1) - otherwise a unit moving in the same turn its own side fired sees no danger and
            // walks straight into the impact area.
            double actualDamage = 0.0;

            if ((aaa.getTurnsTilHit() <= 1) && (aaa.getTarget(operator.getGame()) != null)) {
                // damage for artillery weapons is, for some reason, derived from the weapon
                // type's rack size
                int damage;
                Mounted<?> weapon = aaa.getEntity(operator.getGame()).getEquipment(aaa.getWeaponId());
                if (null == weapon) {
                    // The weaponId couldn't get us a weapon; probably a bomb Arrow IV dropped on a
                    // prior turn.
                    BombType bombType = BombType.createBombByType(BombType.BombTypeEnum.ARROW);
                    damage = (bombType != null) ? bombType.getRackSize() : 0;
                } else {
                    if (weapon.getType() instanceof BombType) {
                        damage = (weapon.getExplosionDamage());
                    } else {
                        damage = ((WeaponType) weapon.getType()).getRackSize();
                    }
                }

                // distance from given coordinates reduces damage
                Coords attackDestination = aaa.getTarget(operator.getGame()).getPosition();
                int distance = coords.distance(attackDestination);

                // calculate odds of attack actually hitting
                // artillery skill may be gunnery or artillery depending on game options
                int artySkill = aaa.getEntity(operator.getGame()).getCrew().getGunnery();
                if (operator.getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
                    artySkill = aaa.getEntity(operator.getGame()).getCrew().getArtillery();
                }

                double hitOdds;

                Player localPlayer = operator.getLocalPlayer();
                if (localPlayer != null && localPlayer.getArtyAutoHitHexes().contains(BoardLocation.of(coords, 0))) {
                    hitOdds = 1.0;
                } else {
                    hitOdds = Compute.oddsAbove(artySkill + ARTILLERY_ATTACK_MODIFIER);
                }

                actualDamage = Math.max(damage - (10 * distance), 0) * hitOdds;
            }

            sum += actualDamage;
        }

        return sum;
    }
}
