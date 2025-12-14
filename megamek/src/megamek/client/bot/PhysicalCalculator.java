/*
 * Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.bot;

import java.util.Iterator;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.IHonorUtil;
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.actions.BrushOffAttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.KickAttackAction;
import megamek.common.actions.PunchAttackAction;
import megamek.common.actions.PushAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.MiscMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BuildingTarget;
import megamek.common.units.EjectedCrew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;

public final class PhysicalCalculator {
    private PhysicalCalculator() {
        super();
        // should never call this
    }

    public static PhysicalOption getBestPhysical(Entity entity, Game game, BehaviorSettings behaviorSettings,
          IHonorUtil honorUtil) {
        // Infantry can't conduct physical attacks.
        if (entity instanceof Infantry) {
            return null;
        }

        // if you're charging, it's already declared
        if (entity.isCharging() || entity.isMakingDfa()) {
            return null;
        }

        PhysicalOption best = null;
        ToHitData odds;
        double breach;
        double breach_a;
        double l_dmg;
        double r_dmg;
        double final_dmg;
        int best_brush = PhysicalOption.NONE;
        boolean aptPiloting = entity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);

        // If the attacker is a Mek

        if (entity instanceof Mek) {

            l_dmg = 0.0;
            r_dmg = 0.0;
            final_dmg = 0.0;
            breach_a = 0.0;

            // If the attacker is being swarmed

            if (entity.getSwarmAttackerId() != Entity.NONE) {

                // Check for left arm punch damage to self

                odds = BrushOffAttackAction.toHit(game,
                      entity.getId(),
                      game.getEntity(entity.getSwarmAttackerId()),
                      BrushOffAttackAction.LEFT);
                if (odds.getValue() != TargetRoll.IMPOSSIBLE) {

                    l_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                    l_dmg *= 1.0 - (Compute.oddsAbove(odds.getValue(), aptPiloting) / 100.0);
                    breach = punchThroughMod(entity, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT, l_dmg, l_dmg);
                    if (breach < 1.5) {
                        best_brush = PhysicalOption.BRUSH_LEFT;
                        breach_a = breach;
                        final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                    }
                }

                // Check for right arm punch damage to self
                odds = BrushOffAttackAction.toHit(game,
                      entity.getId(),
                      game.getEntity(entity.getSwarmAttackerId()),
                      BrushOffAttackAction.RIGHT);
                if (odds.getValue() != TargetRoll.IMPOSSIBLE) {

                    // If chance of breaching armor is minimal set brush left

                    r_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                    r_dmg *= 1.0 - (Compute.oddsAbove(odds.getValue(), aptPiloting) / 100.0);
                    breach = punchThroughMod(entity, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT, r_dmg, r_dmg);
                    if (breach < Math.min(breach_a, 1.5)) {
                        best_brush = PhysicalOption.BRUSH_RIGHT;
                        breach_a = breach;
                        final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                    }
                }

                // If both arms are capable of punching, check double punch
                // damage

                if ((l_dmg > 0) && (r_dmg > 0)) {

                    // If chance of breaching armor is minimal set double brush

                    breach = punchThroughMod(entity,
                          ToHitData.HIT_PUNCH,
                          ToHitData.SIDE_FRONT,
                          l_dmg + r_dmg,
                          (l_dmg + r_dmg) / 2.0);
                    if (breach < Math.min(breach_a, 1.5)) {
                        best_brush = PhysicalOption.BRUSH_BOTH;
                        breach_a = breach;
                        final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                        final_dmg += BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                    }
                }

                // Construct and return Physical option
                if (best_brush != PhysicalOption.NONE) {
                    return new PhysicalOption(entity,
                          game.getEntity(entity.getSwarmAttackerId()),
                          final_dmg,
                          best_brush,
                          null);
                }
            }

            // If the attacker has attached iNarc pods, assign
            // a competing damage value for comparison with other
            // attacks

            if (entity.hasINarcPodsAttached()) {
                double test_ranking;
                double pod_ranking;
                INarcPod test_pod;
                INarcPod best_pod;
                pod_ranking = 0.0;
                Iterator<INarcPod> pod_list = entity.getINarcPodsAttached();
                best_pod = pod_list.next();
                for (pod_list = entity.getINarcPodsAttached(); pod_list.hasNext(); ) {
                    test_ranking = 1.0;
                    test_pod = pod_list.next();
                    // If pod is homing and attacker has no ECM
                    if ((test_pod.type() == INarcPod.HOMING) && !entity.hasActiveECM()) {
                        // Pod is +1
                        test_ranking += 1.0;
                    }
                    // If pod is ECM and attacker has C3 link
                    if ((test_pod.type() == INarcPod.ECM) && (entity.hasC3() || entity.hasC3i())) {
                        // Pod is +2
                        test_ranking += 2.0;
                    }
                    // If pod is Nemesis
                    if (test_pod.type() == INarcPod.NEMESIS) {
                        // Pod is +variable, based on movement
                        test_ranking += (entity.getWalkMP() + entity.getAnyTypeMaxJumpMP()) / 2.0;
                    }
                    // If this pod is best, retain it and its ranking
                    if (test_ranking > pod_ranking) {
                        pod_ranking = test_ranking;
                        best_pod = test_pod;
                    }
                }
                if (best_pod != null) {
                    // Check for left arm punch damage to self
                    odds = BrushOffAttackAction.toHit(game, entity.getId(), best_pod, BrushOffAttackAction.LEFT);
                    if (odds.getValue() != TargetRoll.IMPOSSIBLE) {

                        l_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                        l_dmg *= 1.0 - (Compute.oddsAbove(odds.getValue(), aptPiloting) / 100.0);
                        breach = punchThroughMod(entity, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT, l_dmg, l_dmg);
                        if (breach < 1.5) {
                            best_brush = PhysicalOption.BRUSH_LEFT;
                            breach_a = breach;
                            final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                        }
                    }

                    // Check for right arm punch damage to self
                    odds = BrushOffAttackAction.toHit(game, entity.getId(), best_pod, BrushOffAttackAction.RIGHT);
                    if (odds.getValue() != TargetRoll.IMPOSSIBLE) {

                        // If chance of breaching armor is minimal set brush
                        // left

                        r_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                        r_dmg *= 1.0 - (Compute.oddsAbove(odds.getValue(), aptPiloting) / 100.0);
                        breach = punchThroughMod(entity, ToHitData.HIT_PUNCH, ToHitData.SIDE_FRONT, r_dmg, r_dmg);
                        if (breach < Math.min(breach_a, 1.5)) {
                            best_brush = PhysicalOption.BRUSH_RIGHT;
                            breach_a = breach;
                            final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                        }
                    }

                    // If both arms are capable of punching, check double punch
                    // damage

                    if ((l_dmg > 0) && (r_dmg > 0)) {

                        // If chance of breaching armor is minimal set double
                        // brush

                        breach = punchThroughMod(entity,
                              ToHitData.HIT_PUNCH,
                              ToHitData.SIDE_FRONT,
                              l_dmg + r_dmg,
                              (l_dmg + r_dmg) / 2.0);
                        if (breach < Math.min(breach_a, 1.5)) {
                            best_brush = PhysicalOption.BRUSH_BOTH;
                            final_dmg = BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.LEFT);
                            final_dmg += BrushOffAttackAction.getDamageFor(entity, BrushOffAttackAction.RIGHT);
                        }
                    }

                    // Construct and return Physical option
                    if (best_brush != PhysicalOption.NONE) {
                        return new PhysicalOption(entity, best_pod, final_dmg, best_brush, null);
                    }
                }
            }
        }

        for (Entity target : game.getEntitiesVector()) {
            // don't consider myself
            if (target.equals(entity)) {
                continue;
            }

            // don't consider friendly targets
            if (!target.isEnemyOf(entity)) {
                continue;
            }

            // don't consider targets not on the board
            if (target.getPosition() == null) {
                continue;
            }

            // don't consider targets beyond melee range
            if (Compute.effectiveDistance(game, entity, target) > 1) {
                continue;
            }

            // don't bother stomping EjectedCrew
            if (target instanceof EjectedCrew) {
                continue;
            }

            if (behaviorSettings.getIgnoredUnitTargets().contains(target.getId())) {
                continue;
            }

            if (honorUtil.isEnemyBroken(entity.getId(), entity.getOwnerId(), behaviorSettings.isForcedWithdrawal())) {
                continue;
            }

            PhysicalOption one = getBestPhysicalAttack(entity, target, game);
            if (one != null) {
                if ((best == null) || (one.expectedDmg > best.expectedDmg)) {
                    best = one;
                }
            }
        }
        if (best == null) {
            best = new PhysicalOption(entity);
        }
        return best;
    }

    static PhysicalOption getBestPhysicalAttack(Entity from, Entity to, Game game) {
        Targetable target = to;

        // if the object of our affections is in a building, we have to target the building instead
        if (Compute.isInBuilding(game, to) || (to.isBuildingEntityOrGunEmplacement())) {
            target = new BuildingTarget(to.getPosition(), game.getBoard(), false);
        }

        double bestDmg = 0.0;
        double dmg;
        int damage;
        int target_arc;
        int location_table;
        int bestType = PhysicalOption.NONE;
        MiscMounted bestClub = null;
        boolean targetConvInfantry = false;
        boolean fromAptPiloting = from.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        boolean toAptPiloting = to.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);

        // Infantry and tanks can't conduct any of these attacks
        if ((from instanceof Infantry) || (from instanceof Tank)) {
            return null;
        }

        if (to.isConventionalInfantry()) {
            targetConvInfantry = true;
        }

        // Find arc the attack comes in
        target_arc = getThreatHitArc(to.getPosition(), to.getFacing(), from.getPosition());

        // Check for punches If the target is a Mek, must determine if punch lands on the punch, kick, or full table
        if (to instanceof Mek) {
            if (!to.isProne()) {
                location_table = ToHitData.HIT_PUNCH;
                if (to.getElevation() == (from.getElevation() + 1)) {
                    location_table = ToHitData.HIT_KICK;
                }
            } else {
                location_table = ToHitData.HIT_NORMAL;
            }
        } else {
            location_table = ToHitData.HIT_NORMAL;
        }

        ToHitData odds = PunchAttackAction.toHit(game, from.getId(), target, PunchAttackAction.LEFT, false);
        if (odds.getValue() != TargetRoll.IMPOSSIBLE) {
            damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.LEFT, targetConvInfantry, false);
            bestDmg = (Compute.oddsAbove(odds.getValue(), fromAptPiloting) / 100.0) * damage;
            // Adjust damage for targets armor
            bestType = PhysicalOption.PUNCH_LEFT;
            bestDmg *= punchThroughMod(to, location_table, target_arc, bestDmg, bestDmg);
        }

        odds = PunchAttackAction.toHit(game, from.getId(), target, PunchAttackAction.RIGHT, false);
        if (odds.getValue() != TargetRoll.IMPOSSIBLE) {
            damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.RIGHT, targetConvInfantry, false);
            dmg = (Compute.oddsAbove(odds.getValue(), fromAptPiloting) / 100.0) * damage;
            // Adjust damage for targets armor
            dmg *= punchThroughMod(to, location_table, target_arc, dmg, dmg);
            if (dmg > bestDmg) {
                bestType = PhysicalOption.PUNCH_RIGHT;
                bestDmg = dmg;
            }
        }

        // Check for a double punch
        odds = PunchAttackAction.toHit(game, from.getId(), target, PunchAttackAction.LEFT, false);
        ToHitData odds_a = PunchAttackAction.toHit(game, from.getId(), to, PunchAttackAction.RIGHT, false);
        if ((odds.getValue() != TargetRoll.IMPOSSIBLE) && (odds_a.getValue() != TargetRoll.IMPOSSIBLE)) {
            damage = PunchAttackAction.getDamageFor(from, PunchAttackAction.LEFT, targetConvInfantry, false);
            dmg = (Compute.oddsAbove(odds.getValue(), fromAptPiloting) / 100.0) * damage;
            double dmg_a = (Compute.oddsAbove(odds_a.getValue(), fromAptPiloting) / 100.0) * damage;
            dmg += dmg_a;
            dmg *= punchThroughMod(to, location_table, target_arc, dmg, dmg / 2.0);
            if (dmg > bestDmg) {
                bestType = PhysicalOption.PUNCH_BOTH;
                bestDmg = dmg;
            }
        }

        // Check for a kick If the target is a Mek, must determine if it lands on the kick or punch table
        if (to instanceof Mek) {
            location_table = ToHitData.HIT_KICK;
            if (!to.isProne()) {
                if (to.getElevation() == (from.getElevation() - 1)) {
                    location_table = ToHitData.HIT_PUNCH;
                }
            } else {
                location_table = ToHitData.HIT_NORMAL;
            }
        }

        dmg = getExpectedKickDamage(from, to, game, location_table, target_arc, KickAttackAction.LEFT);
        if (dmg > bestDmg) {
            bestType = PhysicalOption.KICK_LEFT;
            bestDmg = dmg;
        }

        dmg = getExpectedKickDamage(from, to, game, location_table, target_arc, KickAttackAction.RIGHT);
        if (dmg > bestDmg) {
            bestType = PhysicalOption.KICK_RIGHT;
            bestDmg = dmg;
        }

        // Check for mounted club-type weapon or carried improvised club
        for (MiscMounted club : from.getClubs()) {
            // If the target is a Mek, must determine if it hits full body,
            // punch, or kick table
            if (to instanceof Mek) {
                location_table = ToHitData.HIT_NORMAL;
                if ((to.getElevation() == (from.getElevation() - 1)) && !to.isProne()) {
                    location_table = ToHitData.HIT_PUNCH;
                }
                if ((to.getElevation() == (from.getElevation() + 1)) && !to.isProne()) {
                    location_table = ToHitData.HIT_KICK;
                }
            } else {
                location_table = ToHitData.HIT_NORMAL;
            }
            odds = ClubAttackAction.toHit(game, from.getId(), target, club, ToHitData.HIT_NORMAL, false);
            if (odds.getValue() != TargetRoll.IMPOSSIBLE) {
                damage = ClubAttackAction.getDamageFor(from, club, targetConvInfantry, false);
                dmg = (Compute.oddsAbove(odds.getValue(), fromAptPiloting) / 100.0) * damage;
                // Adjust damage for targets armor
                dmg *= punchThroughMod(to, location_table, target_arc, dmg, dmg);
                // Some types of clubs, such as the mace, require a piloting
                // check on a missed attack
                // Calculate self damage in the same manner as a missed kick
                if (dmg > bestDmg) {
                    bestType = PhysicalOption.USE_CLUB;
                    bestDmg = dmg;
                    bestClub = club;
                }
            }
        }
        // Check for a push attack
        odds = PushAttackAction.toHit(game, from.getId(), target);
        if (odds.getValue() != TargetRoll.IMPOSSIBLE) {
            int elev_diff;
            double breach;
            boolean water_landing = false;
            dmg = 0.0;
            int displayDirection = from.getPosition().direction(to.getPosition());
            Coords displayCoords = to.getPosition().translated(displayDirection);
            // If the displacement hex is a valid one
            if (Compute.isValidDisplacement(game, to.getId(), to.getPosition(), displayCoords)) {
                // If the displacement hex is not on the map, credit damage
                // against full target armor
                if (!game.getBoard().contains(displayCoords)) {
                    dmg = (to.getTotalArmor() * Compute.oddsAbove(odds.getValue(), toAptPiloting)) / 100.0;
                }
                if (game.getBoard().contains(displayCoords)) {
                    // Find the elevation difference
                    elev_diff = game.getBoard().getHex(to.getPosition()).getLevel();
                    elev_diff -= game.getBoard().getHex(displayCoords).getLevel();
                    if (elev_diff < 0) {
                        elev_diff = 0;
                    }
                    // Set a flag if the displacement hex has water
                    if (game.getBoard().getHex(displayCoords).containsTerrain(Terrains.WATER)) {
                        water_landing = true;
                    }
                    // Get the base damage from target falling, multiplied by
                    // the elevation difference
                    dmg = calculateFallingDamage(Compute.oddsAbove(odds.getValue(), toAptPiloting) / 100.0, to) *
                          (1.0 + elev_diff);
                    // Calculate breach factor of falling damage
                    breach = punchThroughMod(to, ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT, dmg, Math.min(dmg, 5.0));
                    // If breach factor is > 1 and displacement hex has water
                    if ((breach > 1) && water_landing) {
                        breach *= 2.0;
                    }
                    // Modify damage to reflect how bad it is for target to be
                    // prone
                    if (to.getWalkMP() > 0) {
                        dmg = dmg * Math.sqrt((1.0 / to.getWalkMP()) + to.getAnyTypeMaxJumpMP());
                    } else {
                        dmg *= Math.max(1.0, Math.sqrt(to.getAnyTypeMaxJumpMP()));
                    }
                    // Modify damage by breach factor
                    dmg *= breach;
                }
            }
            // If the displacement hex is not valid
            if (!Compute.isValidDisplacement(game, to.getId(), to.getPosition(), displayCoords)) {
                // Set a flag if the displacement hex has water
                if (game.getBoard().getHex(to.getPosition()).containsTerrain(Terrains.WATER)) {
                    water_landing = true;
                }
                // Get falling in place
                dmg = calculateFallingDamage(Compute.oddsAbove(odds.getValue(), toAptPiloting) / 100.0, to);
                // Calculate breach factor of falling damage
                breach = punchThroughMod(to, ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT, dmg, Math.min(dmg, 5.0));
                // If breach factor is > 1 and target hex is in water
                if ((breach > 1) && water_landing) {
                    breach *= 2.0;
                }
                // Modify damage to reflect how bad it is for target to be prone
                if (to.getWalkMP() > 0) {
                    dmg = dmg * Math.sqrt((1.0 / to.getWalkMP()) + to.getAnyTypeMaxJumpMP());
                } else {
                    dmg = dmg * Math.max(1.0, Math.sqrt(to.getAnyTypeMaxJumpMP()));
                }
                // Modify damage by breach factor
                dmg *= breach;
            }
            // If damage is better than best damage
            if (dmg > bestDmg) {
                bestType = PhysicalOption.PUSH_ATTACK;
                bestDmg = dmg;
            }
        }

        // Conventional infantry in the open suffer double damage.
        if (to.isConventionalInfantry()) {
            Hex e_hex = game.getBoard().getHex(to.getPosition());
            if (!e_hex.containsTerrain(Terrains.WOODS) && !e_hex.containsTerrain(Terrains.BUILDING)) {
                bestDmg *= 2.0;
            }
        }

        if (bestDmg > 0) {
            return new PhysicalOption(from, target, bestDmg, bestType, bestClub);
        }
        return null;
    }

    /**
     * @param ent The {@link Entity} that is falling
     *
     * @return Falling damage after a successful To-Hit.
     */
    private static double calculateFallingDamage(double odds, Entity ent) {
        double dmg = odds;
        dmg *= 1.0 -
              (Compute.oddsAbove(ent.getBasePilotingRoll().getValue(),
                    ent.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)) / 100.0);
        dmg *= ent.getWeight() * 0.1;
        return dmg;
    }

    private static double getExpectedKickDamage(Entity from, Entity to, Game game, int locTable, int arc, int action) {
        double self_damage;
        double dmg;
        double coll_damage = 0.0;
        int damage;
        boolean targetConvInfantry = false;

        Targetable target = to;

        // if the object of our affections is in a building, we have to target the building instead
        if (Compute.isInBuilding(game, to) || (to.isBuildingEntityOrGunEmplacement())) {
            target = new BuildingTarget(to.getPosition(), game.getBoard(), false);
        }

        ToHitData odds = KickAttackAction.toHit(game, from.getId(), target, action);
        if (odds.getValue() > 12) {
            return 0.0;
        }

        if (to.isConventionalInfantry()) {
            targetConvInfantry = true;
        }

        // Calculate collateral damage, due to possible target fall
        if (to instanceof Mek) {
            boolean toAptPiloting = to.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
            coll_damage = calculateFallingDamage(Compute.oddsAbove(odds.getValue(), toAptPiloting) / 100.0, to);
        }

        boolean fromAptPiloting = from.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        damage = KickAttackAction.getDamageFor(from, action, targetConvInfantry);
        dmg = (Compute.oddsAbove(odds.getValue(), fromAptPiloting) / 100.0) * damage;
        // Adjust damage for targets armor
        dmg *= punchThroughMod(to, locTable, arc, dmg, dmg);
        // Calculate self damage, due to possible fall from missing a kick
        self_damage = calculateFallingDamage(1.0 - (Compute.oddsAbove(odds.getValue(), fromAptPiloting) / 100.0), from);
        if (from.getWalkMP() > 0) {
            self_damage = self_damage * Math.sqrt((1.0 / from.getWalkMP()) + from.getAnyTypeMaxJumpMP());
        } else {
            self_damage = self_damage * Math.sqrt(from.getAnyTypeMaxJumpMP());
        }
        // Add together damage values for comparison
        dmg = (dmg + coll_damage) - self_damage;
        return dmg;
    }

    /**
     * This checks to see if the damage will punch through armor anywhere in the attacked arc. damage argument is
     * divided into hits, using the group argument (ie, group = 5.0 for LRM). Each hit of group damage is checked
     * against each location; if it penetrates increase the multiplier to reflect potential for additional damage
     * Multiple passes are made with each hit being multiples of group damage to reflect shot grouping; as each pass is
     * made the increase to the multiplier is lowered due to the lower chance of hitting the same location
     */
    private static double punchThroughMod(Entity target, int hitTable, int hitSide, double damage, double group) {

        int[] armor_values = new int[8];
        int max_index = 1;
        armor_values[0] = 0;

        // Set the final multiplier as 1.0 (no chance of penetrating armor)
        double final_multiplier = 1.0;

        // Set the base multiplier as 0.5 (good bonus for penetrating with a
        // single hit)
        double base_multiplier = 0.5;

        if ((damage <= 0.0) || (group <= 0.0)) {
            return final_multiplier;
        }

        // If the target is a Mek
        if (target instanceof Mek) {
            // Create vector of body locations with targets current armor values
            // Use hit table and direction to determine locations that are hit
            if (hitTable == ToHitData.HIT_NORMAL) {
                max_index = 7;
                armor_values[0] = target.getArmor(Mek.LOC_HEAD, false);
                if (hitSide != ToHitData.SIDE_FRONT) {
                    armor_values[1] = target.getArmor(Mek.LOC_CENTER_TORSO, true);
                } else {
                    armor_values[1] = target.getArmor(Mek.LOC_CENTER_TORSO, false);
                }
                if (hitSide != ToHitData.SIDE_FRONT) {
                    armor_values[2] = target.getArmor(Mek.LOC_RIGHT_TORSO, true);
                } else {
                    armor_values[2] = target.getArmor(Mek.LOC_RIGHT_TORSO, false);
                }
                if (hitSide != ToHitData.SIDE_FRONT) {
                    armor_values[3] = target.getArmor(Mek.LOC_LEFT_TORSO, true);
                } else {
                    armor_values[3] = target.getArmor(Mek.LOC_LEFT_TORSO, false);
                }
                armor_values[4] = target.getArmor(Mek.LOC_RIGHT_ARM, false);
                armor_values[5] = target.getArmor(Mek.LOC_LEFT_ARM, false);
                armor_values[6] = target.getArmor(Mek.LOC_RIGHT_LEG, false);
                armor_values[7] = target.getArmor(Mek.LOC_RIGHT_LEG, false);
            }
            if (hitTable == ToHitData.HIT_PUNCH) {
                armor_values[0] = target.getArmor(Mek.LOC_HEAD, false);
                if (hitSide == ToHitData.SIDE_RIGHT) {
                    max_index = 3;
                    armor_values[1] = target.getArmor(Mek.LOC_CENTER_TORSO, false);
                    armor_values[2] = target.getArmor(Mek.LOC_RIGHT_TORSO, false);
                    armor_values[3] = target.getArmor(Mek.LOC_RIGHT_ARM, false);
                }
                if (hitSide == ToHitData.SIDE_LEFT) {
                    max_index = 3;
                    armor_values[1] = target.getArmor(Mek.LOC_CENTER_TORSO, false);
                    armor_values[2] = target.getArmor(Mek.LOC_LEFT_TORSO, false);
                    armor_values[3] = target.getArmor(Mek.LOC_LEFT_ARM, false);
                }
                if (hitSide == ToHitData.SIDE_FRONT) {
                    max_index = 5;
                    armor_values[1] = target.getArmor(Mek.LOC_CENTER_TORSO, false);
                    armor_values[2] = target.getArmor(Mek.LOC_RIGHT_TORSO, false);
                    armor_values[3] = target.getArmor(Mek.LOC_LEFT_TORSO, false);
                    armor_values[4] = target.getArmor(Mek.LOC_RIGHT_ARM, false);
                    armor_values[5] = target.getArmor(Mek.LOC_LEFT_ARM, false);
                }
                if (hitSide == ToHitData.SIDE_REAR) {
                    max_index = 5;
                    armor_values[1] = target.getArmor(Mek.LOC_CENTER_TORSO, true);
                    armor_values[2] = target.getArmor(Mek.LOC_RIGHT_TORSO, true);
                    armor_values[3] = target.getArmor(Mek.LOC_LEFT_TORSO, true);
                    armor_values[4] = target.getArmor(Mek.LOC_RIGHT_ARM, false);
                    armor_values[5] = target.getArmor(Mek.LOC_LEFT_ARM, false);
                }
            }
            if (hitTable == ToHitData.HIT_KICK) {
                max_index = -1;
                if ((hitSide == ToHitData.SIDE_FRONT) ||
                      (hitSide == ToHitData.SIDE_REAR) ||
                      (hitSide == ToHitData.SIDE_RIGHT)) {
                    max_index++;
                    armor_values[max_index] = target.getArmor(Mek.LOC_RIGHT_LEG, false);
                }
                if ((hitSide == ToHitData.SIDE_FRONT) ||
                      (hitSide == ToHitData.SIDE_REAR) ||
                      (hitSide == ToHitData.SIDE_LEFT)) {
                    max_index++;
                    armor_values[max_index] = target.getArmor(Mek.LOC_LEFT_LEG, false);
                }
            }
        }
        // If the target is a ProtoMek
        if (target instanceof ProtoMek) {
            max_index = 6;
            // Create vector of body locations with targets current armor values
            // Create two high-armor dummy locations to represent the 'near
            // miss' hit locations
            armor_values[0] = target.getArmor(ProtoMek.LOC_TORSO, false);
            armor_values[1] = target.getArmor(ProtoMek.LOC_LEG, false);
            armor_values[2] = target.getArmor(ProtoMek.LOC_RIGHT_ARM, false);
            armor_values[3] = target.getArmor(ProtoMek.LOC_LEFT_ARM, false);
            armor_values[4] = target.getArmor(ProtoMek.LOC_HEAD, false);
            armor_values[5] = 100;
            armor_values[6] = 100;
            if (((ProtoMek) target).hasMainGun()) {
                max_index++;
                armor_values[max_index] = target.getArmor(ProtoMek.LOC_MAIN_GUN, false);
            }
        }
        // If the target is a vehicle
        if (target instanceof Tank) {
            // Create vector of armor locations
            max_index = 0;
            switch (hitSide) {
                case ToHitData.SIDE_FRONT:
                    armor_values[0] = target.getArmor(Tank.LOC_FRONT);
                    break;
                case ToHitData.SIDE_RIGHT:
                    armor_values[0] = target.getArmor(Tank.LOC_RIGHT);
                    break;
                case ToHitData.SIDE_LEFT:
                    armor_values[0] = target.getArmor(Tank.LOC_LEFT);
                    break;
                case ToHitData.SIDE_REAR:
                    armor_values[0] = target.getArmor(Tank.LOC_REAR);
                    break;
            }
            if (!((Tank) target).hasNoTurret()) {
                max_index++;
                armor_values[max_index] = target.getArmor(((Tank) target).getLocTurret());
            }
            if (!((Tank) target).hasNoDualTurret()) {
                max_index++;
                armor_values[max_index] = target.getArmor(((Tank) target).getLocTurret2());
            }
        }
        // If the target is Battle Armor
        if (target instanceof BattleArmor) {
            // Create vector of armor of surviving troopers
            max_index = -1;
            for (int i = 1; i < ((BattleArmor) target).getShootingStrength(); i++) {
                if (target.getArmor(i) >= 0) {
                    max_index++;
                    armor_values[max_index] = target.getArmor(i);
                }
            }
        }
        // If the target is conventional infantry
        if (target.isConventionalInfantry() && target instanceof Infantry infantry) {
            // Create a single element vector with total number of troopers
            max_index = 0;
            armor_values[0] = infantry.getShootingStrength();
        }

        double hit_total = 0;
        // While hit damage is less than total damage applied, increment by group value
        while (hit_total <= damage) {
            hit_total += group;
            for (int i = 0; i <= max_index; i++) {
                // If hit damage can penetrate location
                if (hit_total > armor_values[i]) {
                    final_multiplier += base_multiplier;
                }
            }
            base_multiplier /= 2.0;
        }

        // Return final multiplier

        return final_multiplier;

    }

    public static int getThreatHitArc(Coords dest, int dest_facing, Coords src) {
        int fa = getFiringAngle(dest, dest_facing, src);

        if ((fa >= 300) || (fa <= 60)) {
            return ToHitData.SIDE_FRONT;
        } else if ((fa >= 240)) {
            return ToHitData.SIDE_LEFT;
        } else if ((fa <= 120)) {
            return ToHitData.SIDE_RIGHT;
        }

        return ToHitData.SIDE_REAR;
    }

    public static int getFiringAngle(final Coords dest, int dest_facing, final Coords src) {
        int fa = dest.degree(src) - ((dest_facing % 6) * 60);
        if (fa < 0) {
            fa += 360;
        } else if (fa >= 360) {
            fa -= 360;
        }
        return fa;
    }
}
