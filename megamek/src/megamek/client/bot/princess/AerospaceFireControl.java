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

import megamek.common.HexTarget;
import megamek.common.TargetRollModifier;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.rolls.TargetRoll;
import java.util.Map;

import megamek.common.units.Entity;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Aero;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * Gunnery for aerospace units flying in an atmosphere.
 *
 * <p>Two corrections to the stock guess, both of which make the bot's expectations match what the server
 * will actually allow:</p>
 *
 * <ul>
 *     <li><b>The dead zone.</b> The stock guess never checks it, so the bot plans shots the server answers
 *     with {@code IMPOSSIBLE}, books the damage it expected to do, and moves somewhere on the strength of an
 *     attack it was never going to make. Over a ground mapsheet, where one level of altitude blocks fire
 *     within seventeen hexes, that describes most of the board.</li>
 *     <li><b>Range.</b> The stock guess converts ground hexes to aerospace range with integer division while
 *     the engine rounds up, and never adds the altitude difference the rules charge for (TW p.241). Both make
 *     the bot believe it is closer than it is.</li>
 * </ul>
 */
public class AerospaceFireControl extends FireControl {

    private static final MMLogger RATION_LOGGER = MMLogger.create(AerospaceFireControl.class);

    /**
     * Credit for the battle value a salvo is expected to remove, scaled by BV/1000 and by the
     * SQUARE of the kill fraction, so a near-certain kill of a cheap unit outearns a shallow dent
     * in an expensive one. A dent does not remove a unit from the fight; a kill does.
     */
    static final double BOMB_KILL_UTILITY = 100.0;

    /**
     * What one released bomb charges the plan when other bomb-worthy enemies remain: its
     * next-best-future-use value, roughly half an HE bomb's damage at even odds on a later pass.
     * Zero when this is the last bombable target - the mission is over, spend freely.
     */
    static final double BOMB_OPPORTUNITY_COST_PER_BOMB = 5.0;

    /** Refusal reason for a shot the geometry forbids, mirroring the server's own wording. */
    static final TargetRollModifier TH_IN_DEAD_ZONE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "target in dead zone");

    public AerospaceFireControl(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Rejects a shot the dead zone forbids before the stock guess prices it.
     *
     * <p>Only air-to-air geometry is consulted. A ground attack is made along the flight path rather than at
     * a range, and spheroids are exempt from the block entirely - they fire nose weapons at what is above
     * them and aft weapons at what is below (TW p.241), which is exactly the shot the cone would otherwise
     * deny them.</p>
     */
    @Override
    ToHitData guessToHitModifierForWeapon(final Entity shooter,
          @Nullable EntityState shooterState,
          final Targetable target,
          @Nullable EntityState targetState,
          final WeaponMounted weapon,
          @Nullable final AmmoMounted ammo,
          final Game game) {
        EntityState resolvedShooterState = (shooterState == null) ? new EntityState(shooter) : shooterState;
        EntityState resolvedTargetState = (targetState == null) ? new EntityState(target) : targetState;

        if (isBlockedByDeadZone(shooter, resolvedShooterState, target, resolvedTargetState, game)) {
            return new ToHitData(TH_IN_DEAD_ZONE);
        }

        return super.guessToHitModifierForWeapon(shooter, resolvedShooterState, target, resolvedTargetState,
              weapon, ammo, game);
    }

    /**
     * The range this shot is resolved at, measured the way the rules measure it.
     *
     * <p>Falls back to the stock calculation for anything that is not one airborne aerospace unit shooting at
     * another, so ground gunnery and ground-to-air are untouched.</p>
     */
    /**
     * Drops on the best blast-footprint hex of the flown line, not on the target's own hex.
     *
     * <p>The movement half steers the fighter over the seam of a formation; this half makes the drop
     * actually land there. Worked from the pilot's seat: against a box lance with two hexes of
     * spacing, cluster bombs (5 damage across all seven hexes, no falloff) on a corner mek's hex
     * reach one target - its neighbors are empty - while the seam hex between two meks delivers full
     * damage to both. Every hex the fighter physically flew through is a legal aim point
     * ({@code passedOver}), so the drop target is a search over the flown line with the same ring
     * tables the ranker's footprint credit uses. Either way the payload is rationed - to the single
     * victim's hit points on a direct drop, to the summed hit points of everything under the blast
     * rings on a seam drop.</p>
     */
    @Override
    protected FiringPlan getDiveBombPlan(final Entity shooter, final MovePath flightPath,
          final Targetable target, final Game game, final boolean passedOverTarget, final boolean guess) {
        Coords bestAim = bestFootprintAimHex(shooter, flightPath, game);
        if ((bestAim != null) && (target.getPosition() != null) && !bestAim.equals(target.getPosition())) {
            HexTarget seam = new HexTarget(bestAim, shooter.getBoardId(), Targetable.TYPE_HEX_AERO_BOMB);
            // The seam hex is on the flown line by construction, so the fly-over requirement holds.
            FiringPlan seamPlan = super.getDiveBombPlan(shooter, flightPath, seam, game, true, guess);
            // Rationed to the WHOLE footprint: every enemy inside the blast rings at this aim point
            // funds the salvo. Exempting seam drops entirely was a live bug - Princess builds one
            // candidate plan per enemy, so every enemy not standing on the best hex produced an
            // unrationed full-load twin aimed there, and the auction always preferred the twin.
            rationBombPayloadForFootprint(seamPlan, shooter, bestAim, game);
            return seamPlan;
        }
        FiringPlan plan = super.getDiveBombPlan(shooter, flightPath, target, game, passedOverTarget, guess);
        rationBombPayload(plan, target);
        return plan;
    }

    /**
     * Considers a strafing run alongside the stock per-target plans and flies whichever bids
     * higher. The stock chooser never strafes at all; this is the whole of PC-16-01's strafing
     * half for the bot.
     */
    @Override
    @Nullable
    FiringPlan getBestFiringPlan(final Entity shooter, final IHonorUtil honorUtil, final Game game,
          final Map<WeaponMounted, Double> ammoConservation) {
        FiringPlan best = super.getBestFiringPlan(shooter, honorUtil, game, ammoConservation);
        FiringPlan strafe = getStrafePlan(shooter, game);
        if ((strafe != null) && !strafe.isEmpty()
              && ((best == null) || (strafe.getUtility() > best.getUtility()))) {
            RATION_LOGGER.info("STRAFE {}: {} shots along the run, utility {} over {}",
                  shooter.getDisplayName(), strafe.size(), Math.round(strafe.getUtility()),
                  (best == null) ? 0 : Math.round(best.getUtility()));
            return strafe;
        }
        return best;
    }

    /**
     * Builds the best legal strafing run over this turn's flown line (TW p.243): the straight
     * window of at most five passed-through hexes covering the most enemy ground units, every
     * eligible energy weapon rolling once against every unit under it. Each shot carries the
     * strafing flags so the server resolves it as a strafe, and heat lands once per weapon
     * however many targets its run crosses. Nap-of-the-earth strafing is deliberately out of this
     * first pass - the +2 and its dead-zone rules make altitude 2-3 the honest window.
     *
     * @return the strafe plan, or {@code null} when no legal run covers anyone
     */
    @Nullable
    FiringPlan getStrafePlan(final Entity shooter, final Game game) {
        if (!shooter.isAero() || !shooter.isAirborne() || shooter.isSpheroid()
              || (shooter.getAltitude() < 2) || (shooter.getAltitude() > 3)) {
            RATION_LOGGER.debug("STRAFE-PLAN {}: out of window (airborne={} alt={})",
                  shooter.getDisplayName(), shooter.isAirborne(), shooter.getAltitude());
            return null;
        }
        List<WeaponMounted> strafeWeapons = new ArrayList<>();
        for (WeaponMounted weapon : shooter.getWeaponList()) {
            if (weapon.canFire() && !weapon.isRearMounted()
                  && (weapon.getLocation() != Aero.LOC_AFT)
                  && AerospacePathRanker.isStrafeEligible(weapon.getType())) {
                strafeWeapons.add(weapon);
            }
        }
        if (strafeWeapons.isEmpty()) {
            RATION_LOGGER.debug("STRAFE-PLAN {}: no strafe-eligible weapons", shooter.getDisplayName());
            return null;
        }
        List<Coords> flownLine = new ArrayList<>(shooter.getPassedThrough());
        // One entity lookup per flown hex; windows then read the map instead of re-querying the
        // game per window (the review's per-window cost question - IllianiBird, PR #8728).
        Map<Coords, List<Entity>> victimsByHex = new HashMap<>();
        for (Coords hex : flownLine) {
            List<Entity> victims = strafeVictims(shooter, game, List.of(hex));
            if (!victims.isEmpty()) {
                victimsByHex.put(hex, victims);
            }
        }
        List<Entity> bestVictims = List.of();
        for (List<Coords> window : AerospacePathRanker.straightWindows(flownLine, 5)) {
            List<Entity> victims = new ArrayList<>();
            for (Coords hex : window) {
                victims.addAll(victimsByHex.getOrDefault(hex, List.of()));
            }
            if (victims.size() > bestVictims.size()) {
                bestVictims = victims;
            }
        }
        if (bestVictims.isEmpty()) {
            RATION_LOGGER.debug("STRAFE-PLAN {}: no victims under any window ({} flown hexes)",
                  shooter.getDisplayName(), flownLine.size());
            return null;
        }
        // The nominal target anchors the auction's value math: the biggest thing under the line.
        Entity nominalTarget = bestVictims.getFirst();
        for (Entity victim : bestVictims) {
            if (victim.calculateBattleValue() > nominalTarget.calculateBattleValue()) {
                nominalTarget = victim;
            }
        }
        FiringPlan strafePlan = new FiringPlan(nominalTarget);
        for (WeaponMounted weapon : strafeWeapons) {
            boolean firstShot = true;
            for (Entity victim : bestVictims) {
                WeaponFireInfo shot = new WeaponFireInfo(shooter, victim, weapon, null, game,
                      false, owner);
                shot.convertToStrafe(firstShot);
                if (shot.getProbabilityToHit() <= 0) {
                    RATION_LOGGER.debug("STRAFE-SHOT {} vs {}: {}", weapon.getName(),
                          victim.getDisplayName(), shot.getToHit().getDesc());
                    continue;
                }
                strafePlan.add(shot);
                firstShot = false;
            }
        }
        if (strafePlan.isEmpty()) {
            RATION_LOGGER.info("STRAFE-PLAN {}: {} victims but every shot impossible",
                  shooter.getDisplayName(), bestVictims.size());
            return null;
        }
        calculateUtility(strafePlan, calcHeatTolerance(shooter, null), true);
        RATION_LOGGER.info("STRAFE-PLAN {}: built {} shots on {} victims, utility {}",
              shooter.getDisplayName(), strafePlan.size(), bestVictims.size(),
              Math.round(strafePlan.getUtility()));
        return strafePlan;
    }

    /** The enemy ground units a strafing window can hit: on it, not airborne, not dug into a building. */
    private List<Entity> strafeVictims(Entity shooter, Game game, List<Coords> window) {
        List<Entity> victims = new ArrayList<>();
        for (Coords hex : window) {
            for (Entity unit : game.getEntitiesVector(hex, shooter.getPassedThroughBoardId())) {
                if (unit.getOwner().isEnemyOf(shooter.getOwner()) && !unit.isAirborne()
                      && !unit.isDestroyed()
                      && !(unit instanceof EjectedCrew)
                      && !((unit instanceof Infantry) && Compute.isInBuilding(game, unit))) {
                    victims.add(unit);
                }
            }
        }
        return victims;
    }

    /**
     * Rations a hex-aimed drop to the sum of every enemy ground unit inside the payload's blast
     * rings at the aim point. For single-hex ordnance this degenerates to the one victim standing
     * there; for cluster and fuel-air seams it funds killing everything under the footprint, and
     * no more.
     */
    private void rationBombPayloadForFootprint(FiringPlan plan, Entity shooter, Coords aimPoint,
          Game game) {
        int footprintHitPoints = footprintHitPoints(shooter.getBombs(AmmoType.F_GROUND_BOMB),
              aimPoint, liveEnemyGroundUnits(shooter, game));
        rationPayloadToHitPoints(plan, Math.max(1, footprintHitPoints));
    }

    /** Every live enemy ground unit on the shooter's board - the bombable target set. */
    private static List<Entity> liveEnemyGroundUnits(Entity shooter, Game game) {
        List<Entity> enemies = new ArrayList<>();
        for (Entity enemy : game.getEntitiesVector()) {
            // Ejected crews carry no footprint value and the utility math vetoes shooting them;
            // counting their hit points here would misprice drops (and the ranker excludes them
            // from attack runs for the same both-halves reason).
            if (enemy.getOwner().isEnemyOf(shooter.getOwner()) && !enemy.isAirborne()
                  && !(enemy instanceof EjectedCrew)
                  && (enemy.getPosition() != null) && (enemy.getBoardId() == shooter.getBoardId())
                  && !enemy.isDestroyed()) {
                enemies.add(enemy);
            }
        }
        return enemies;
    }

    /**
     * <p>The bomb opportunity-cost block: with a Locust, a Rifleman, a Centurion and an Atlas on
     * the board, what is the value of the target? The stock auction scores a bomb plan by raw
     * expected damage, so the biggest target always monopolizes the rack - a ten-bomb dent in an
     * Atlas outbids a three-bomb Locust kill every time. Three corrections, applied after the stock
     * utility:</p>
     *
     * <ul>
     *     <li><b>Overkill earns nothing.</b> Expected damage past the target's effective hit points
     *     is phantom value (the plan's estimate keeps the pre-ration payload) and is refunded.</li>
     *     <li><b>Kills are worth battle value.</b> Credit scales with the square of the kill
     *     fraction times BV/1000: removing a unit from the fight is the prize, denting one is
     *     not.</li>
     *     <li><b>A bomb spent here is a bomb not spent later.</b> While other bombable enemies
     *     remain, each released bomb charges its next-best-future-use value; on the last target the
     *     charge is zero. This is what banks seven bombs off the Locust kill for the Atlas.</li>
     * </ul>
     */
    @Override
    void calculateUtility(final FiringPlan firingPlan, final int overheatTolerance,
          final boolean shooterIsAero) {
        super.calculateUtility(firingPlan, overheatTolerance, shooterIsAero);
        BombTally tally = tallyBombPlan(firingPlan);
        int released = tally.released();
        if ((released == 0) || firingPlan.isEmpty()) {
            // Gun plans still carry the player's focus order - the dial covers the whole
            // firing half, not just the bomb bay.
            firingPlan.setUtility(applyFocus(firingPlan.getUtility(), firingPlan));
            return;
        }
        Entity shooter = firingPlan.get(0).getShooter();
        if ((shooter == null) || (shooter.getGame() == null)) {
            return;
        }
        List<Entity> enemies = liveEnemyGroundUnits(shooter, shooter.getGame());
        int targetHitPoints;
        int targetBattleValue = 0;
        boolean othersRemain = false;
        if (firingPlan.getTarget() instanceof Entity victim) {
            targetHitPoints = victim.getTotalArmor() + victim.getTotalInternal();
            targetBattleValue = victim.calculateBattleValue();
            for (Entity enemy : enemies) {
                if (enemy.getId() != victim.getId()) {
                    othersRemain = true;
                    break;
                }
            }
        } else if (firingPlan.getTarget().getPosition() != null) {
            List<Entity> underFootprint = enemiesUnderFootprint(
                  shooter.getBombs(AmmoType.F_GROUND_BOMB), firingPlan.getTarget().getPosition(),
                  enemies);
            targetHitPoints = 0;
            for (Entity enemy : underFootprint) {
                targetHitPoints += enemy.getTotalArmor() + enemy.getTotalInternal();
                targetBattleValue += enemy.calculateBattleValue();
            }
            othersRemain = enemies.size() > underFootprint.size();
        } else {
            return;
        }
        double adjustment = bombPlanUtilityAdjustment(firingPlan.getExpectedDamage(),
              tally.expectedDamage(), released, targetHitPoints, targetBattleValue, othersRemain);
        RATION_LOGGER.debug("AUCTION {}: utility {} {} {} ({} bombs worth {}, {} HP, {} BV, others={})",
              firingPlan.getTarget().getDisplayName(), Math.round(firingPlan.getUtility()),
              (adjustment >= 0) ? "+" : "-", Math.round(Math.abs(adjustment)), released,
              Math.round(tally.expectedDamage()), targetHitPoints, targetBattleValue, othersRemain);
        firingPlan.setUtility(applyFocus(firingPlan.getUtility() + adjustment, firingPlan));
    }

    /**
     * The firing half of the focus order, so both halves of the doctrine agree (the footprint
     * lesson: movement steering without firing agreement, or the reverse, is half a feature). An
     * airborne target is the air credit set, anything else the ground set; AUTO changes nothing.
     * Applied to positive utility only - a focus order makes disfavored work less attractive, it
     * must never make a bad plan look better by shrinking its badness.
     */
    private double applyFocus(double utility, FiringPlan plan) {
        AerospaceFocus focus = owner.getAerospaceFocus();
        if ((focus == AerospaceFocus.AUTO) || (utility <= 0)) {
            return utility;
        }
        boolean airTarget = (plan.getTarget() instanceof Entity targetEntity) && targetEntity.isAirborne();
        return utility * AerospacePathRanker.focusMultiplier(focus, airTarget);
    }

    /**
     * The value-of-the-target arithmetic, pure. The honest damage figure is the larger of the stock
     * estimate and the payload's own worth (per-bomb damage times hit odds) - the stock code scores
     * hex-aimed bomb plans at zero, and an auction fed zero credits bombing at pure penalty. From
     * the honest figure: value past the target's hit points is refunded as overkill, kills earn the
     * square of the kill fraction times battle value, and each released bomb charges its future use
     * while other bombable targets remain.
     */
    static double bombPlanUtilityAdjustment(double stockExpectedDamage, double payloadExpectedDamage,
          int bombsReleased, int targetEffectiveHitPoints, int targetBattleValue,
          boolean otherBombTargetsRemain) {
        int effectiveHitPoints = Math.max(1, targetEffectiveHitPoints);
        double honestDamage = Math.max(stockExpectedDamage, payloadExpectedDamage);
        double cappedDamage = Math.min(honestDamage, effectiveHitPoints);
        double killFraction = cappedDamage / effectiveHitPoints;
        // BV/1000 normalizes battle value into utility-scale units: a full kill of a 1,000-BV
        // target earns exactly BOMB_KILL_UTILITY points, a 2,000-BV target twice that.
        double killCredit = BOMB_KILL_UTILITY * killFraction * killFraction
              * (targetBattleValue / 1000.0);
        double opportunityCost = otherBombTargetsRemain
              ? BOMB_OPPORTUNITY_COST_PER_BOMB * bombsReleased : 0.0;
        return (DAMAGE_UTILITY * (cappedDamage - stockExpectedDamage)) + killCredit - opportunityCost;
    }

    /** What a plan's racks are actually worth: bombs released and their honest expected damage. */
    private record BombTally(int released, double expectedDamage) {
    }

    /**
     * Walks the plan's payloads once, counting released bombs and pricing the drop from the
     * ordnance itself: per-bomb damage times the plan's real to-hit odds. The live game of
     * 2026-08-14 proved the stock estimate cannot be trusted here - it scores hex-aimed bomb plans
     * at ZERO expected damage, which fed the auction zero kill credit and full opportunity cost,
     * and a bomber died at round 24 with fifteen bombs still racked.
     */
    private static BombTally tallyBombPlan(FiringPlan plan) {
        int released = 0;
        double expectedDamage = 0;
        for (WeaponFireInfo info : plan) {
            // getAction(), not getWeaponAttackAction(): the latter lazily CREATES an action and
            // recomputes a real to-hit - a side effect gun plans must not pay during an auction.
            if ((info.getAction() == null) || (info.getAction().getBombPayloads() == null)) {
                continue;
            }
            int payloadDamage = 0;
            for (BombLoadout loadout : info.getAction().getBombPayloads().values()) {
                for (Map.Entry<BombType.BombTypeEnum, Integer> entry : loadout.entrySet()) {
                    released += entry.getValue();
                    payloadDamage += BombType.createBombByType(entry.getKey()).getDamagePerShot()
                          * entry.getValue();
                }
            }
            expectedDamage += payloadDamage * Math.max(0.05, info.getProbabilityToHit());
        }
        return new BombTally(released, expectedDamage);
    }

    /** Sums effective hit points of every enemy inside any bomb's blast ring at the aim point. */
    static int footprintHitPoints(List<BombMounted> groundBombs, Coords aimPoint,
          List<Entity> enemies) {
        int footprintHitPoints = 0;
        for (Entity enemy : enemiesUnderFootprint(groundBombs, aimPoint, enemies)) {
            footprintHitPoints += enemy.getTotalArmor() + enemy.getTotalInternal();
        }
        return footprintHitPoints;
    }

    /** The enemies standing inside any bomb's blast ring at the aim point. */
    static List<Entity> enemiesUnderFootprint(List<BombMounted> groundBombs, Coords aimPoint,
          List<Entity> enemies) {
        List<Entity> underFootprint = new ArrayList<>();
        double[] blastProfile = AerospacePathRanker.bombRingProfile(groundBombs);
        for (Entity enemy : enemies) {
            int ring = aimPoint.distance(enemy.getPosition());
            if ((ring <= AerospacePathRanker.MAXIMUM_BLAST_RING) && (blastProfile[ring] > 0)) {
                underFootprint.add(enemy);
            }
        }
        return underFootprint;
    }

    /**
     * Sizes the salvo to the victim, closing the stock code's own TODO ("more intelligent bomb
     * drops"): it loads every bomb aboard into one attack, so ten HE fell on anything that was
     * overflown. Ten bombs on a Locust is dumb; ten on an Atlas is smart (Dave). The drop is trimmed
     * to the count expected to destroy the target - effective armor plus structure over expected
     * damage per bomb - and the rest stay racked for the next pass, turning one overkill alpha into
     * several lethal ones. The plan's damage estimate intentionally keeps the pre-trim value: it
     * over-promises by at most the overkill this exists to remove.
     */
    private void rationBombPayload(FiringPlan plan, Targetable target) {
        if (!(target instanceof Entity victim)) {
            return;
        }
        rationPayloadToHitPoints(plan, victim.getTotalArmor() + victim.getTotalInternal());
    }

    private void rationPayloadToHitPoints(FiringPlan plan, int effectiveHitPoints) {
        for (WeaponFireInfo info : plan) {
            HashMap<String, BombLoadout> payloads = info.getWeaponAttackAction().getBombPayloads();
            if (payloads == null) {
                continue;
            }
            int aboard = 0;
            for (BombLoadout loadout : payloads.values()) {
                aboard += loadout.getTotalBombs();
            }
            // Combine the internal and external racks, choose type-aware, then write back per rack.
            BombLoadout combined = new BombLoadout();
            for (BombLoadout loadout : payloads.values()) {
                for (Map.Entry<BombType.BombTypeEnum, Integer> entry : loadout.entrySet()) {
                    combined.addBombs(entry.getKey(), entry.getValue());
                }
            }
            if (combined.getTotalBombs() <= 1) {
                continue;
            }
            BombLoadout selection = rationSelection(combined,
                  info.getProbabilityToHit(), effectiveHitPoints);
            int released = selection.getTotalBombs();
            for (BombLoadout loadout : payloads.values()) {
                for (Map.Entry<BombType.BombTypeEnum, Integer> entry : loadout.entrySet()) {
                    int grant = Math.min(entry.getValue(), selection.getCount(entry.getKey()));
                    selection.addBombs(entry.getKey(), -grant);
                    entry.setValue(grant);
                }
            }
            // Observability first: the alpha-dump bypass survived a live game unnoticed because
            // nothing logged the decision. RATION lines are the debrief for the bomb bay.
            RATION_LOGGER.debug("RATION {}: releasing {} of {} bombs against {} effective HP",
                  plan.getTarget().getDisplayName(), released, aboard, effectiveHitPoints);
        }
    }

    /**
     * Chooses WHICH bombs a kill honestly asks for, by type (Dave): heaviest damage-per-bomb first
     * until the target's effective hit points are funded at the expected hit rate, so a mixed rack
     * spends its big ordnance on the hard target and keeps the small stuff racked for softer work.
     * Zero-damage ordnance - TAG, mine-layers, infernos - is never released as generic tonnage.
     * Always releases at least one damaging bomb when any is aboard.
     */
    static BombLoadout rationSelection(BombLoadout available, double hitChance, int effectiveHitPoints) {
        double odds = Math.max(0.05, hitChance);
        List<Map.Entry<BombType.BombTypeEnum, Integer>> byDamage =
              new ArrayList<>(available.entrySet());
        byDamage.sort((a, b) -> Integer.compare(
              BombType.createBombByType(b.getKey()).getDamagePerShot(),
              BombType.createBombByType(a.getKey()).getDamagePerShot()));
        BombLoadout selection = new BombLoadout();
        double funded = 0;
        for (Map.Entry<BombType.BombTypeEnum, Integer> entry : byDamage) {
            int perBomb = BombType.createBombByType(entry.getKey()).getDamagePerShot();
            if (perBomb <= 0) {
                continue;
            }
            for (int i = 0; i < entry.getValue(); i++) {
                if ((funded >= effectiveHitPoints) && (selection.getTotalBombs() >= 1)) {
                    return selection;
                }
                selection.addBombs(entry.getKey(), 1);
                funded += perBomb * odds;
            }
        }
        return selection;
    }

    /**
     * The hex on the flown line whose blast footprint delivers the most total damage across every
     * enemy ground unit, or {@code null} when no flown hex delivers anything. Uses the executed
     * move's own hexes ({@link Entity#getPassedThrough}) in preference to the planning-time path,
     * because at firing time the flown line is a fact.
     */
    private @Nullable Coords bestFootprintAimHex(Entity shooter, MovePath flightPath, Game game) {
        // The candidate path FIRST: during movement-phase plan evaluation, getPassedThrough() is the
        // PREVIOUS round's flown line, so preferring it priced candidate paths' bombing utility
        // against stale hexes. At actual firing time the callers pass a null flightPath and the
        // executed move's passedThrough is exactly right.
        List<Coords> flownLine = (flightPath != null)
              ? new ArrayList<>(flightPath.getCoordsSet())
              : shooter.getPassedThrough();
        List<BombMounted> groundBombs = shooter.getBombs(AmmoType.F_GROUND_BOMB);
        if ((flownLine == null) || flownLine.isEmpty() || groundBombs.isEmpty()) {
            return null;
        }
        List<Entity> targets = new ArrayList<>();
        for (Entity enemy : game.getEntitiesVector()) {
            if (enemy.getOwner().isEnemyOf(shooter.getOwner())
                  && !enemy.isAirborne()
                  && (enemy.getPosition() != null)
                  && (enemy.getBoardId() == shooter.getBoardId())
                  && !enemy.isDestroyed()) {
                targets.add(enemy);
            }
        }
        if (targets.isEmpty()) {
            return null;
        }
        Coords bestAimHex = null;
        double bestFootprint = 0;
        // Blast profile computed once; the former line x targets x bombs triple loop is now
        // line x targets with an O(1) ring lookup (IllianiBird, PR #8728).
        double[] blastProfile = AerospacePathRanker.bombRingProfile(groundBombs);
        for (Coords aimPoint : flownLine) {
            double footprint = 0;
            for (Entity target : targets) {
                int ring = aimPoint.distance(target.getPosition());
                if (ring <= AerospacePathRanker.MAXIMUM_BLAST_RING) {
                    footprint += blastProfile[ring];
                }
            }
            if (footprint > bestFootprint) {
                bestFootprint = footprint;
                bestAimHex = aimPoint;
            }
        }
        return bestAimHex;
    }

    @Override
    protected int guessDistance(final Entity shooter, final EntityState shooterState, final Targetable target,
          final EntityState targetState, final Game game) {
        if (!isAtmosphericAirToAir(shooter, shooterState, target, targetState)) {
            return super.guessDistance(shooter, shooterState, target, targetState, game);
        }
        return AerospaceGeometry.effectiveRange(AerospaceVenue.of(game, shooter),
              shooterState.getPosition(), shooterState.getAltitude(),
              targetState.getPosition(), targetState.getAltitude());
    }

    /**
     * Whether the dead zone bars this particular shot.
     *
     * @return {@code true} if the shot is air-to-air in an atmosphere and the geometry forbids it
     */
    private boolean isBlockedByDeadZone(Entity shooter, EntityState shooterState, Targetable target,
          EntityState targetState, Game game) {
        if (!isAtmosphericAirToAir(shooter, shooterState, target, targetState)) {
            return false;
        }
        return AerospaceGeometry.deadZoneBlocksAttack(AerospaceVenue.of(game, shooter),
              shooterState.getPosition(), shooterState.getAltitude(),
              Compute.useSpheroidAtmosphere(game, shooter),
              targetState.getPosition(), targetState.getAltitude());
    }

    /**
     * Whether this is one airborne aerospace unit shooting at another, in an atmosphere and on the same board.
     *
     * <p>Space has no altitude levels and therefore no dead zone, and a cross-board shot has its positions
     * substituted by the engine in ways this geometry does not model.</p>
     */
    private boolean isAtmosphericAirToAir(Entity shooter, EntityState shooterState, Targetable target,
          EntityState targetState) {
        if ((shooterState.getPosition() == null) || (targetState.getPosition() == null)) {
            return false;
        }
        if (!shooterState.isAero() || !shooterState.isAirborne() || shooter.isSpaceborne()) {
            return false;
        }
        if (!targetState.isAirborneAero()) {
            return false;
        }
        return shooter.getBoardId() == target.getBoardId();
    }
}
