/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons.handlers.artillery;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.LosEffects;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.SpecialHexDisplay.Type;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.NukeDetonatedAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.Minefield;
import megamek.common.event.player.GamePlayerStrategicActionEvent;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.ArtilleryHandlerHelper;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;
import megamek.common.weapons.handlers.AmmoWeaponHandler;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ArtilleryWeaponIndirectFireHandler extends AmmoWeaponHandler {
    private static final MMLogger logger = MMLogger.create(ArtilleryWeaponIndirectFireHandler.class);

    @Serial
    private static final long serialVersionUID = -1277649123562229298L;
    boolean handledAmmoAndReport = false;
    private int shootingBA = -1;

    public ArtilleryWeaponIndirectFireHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        if (w.getEntity(g) instanceof BattleArmor) {
            shootingBA = ((BattleArmor) w.getEntity(g)).getNumberActiveTroopers();
        }
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isOffboard() || phase.isTargeting();
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        String artyMsg;
        ArtilleryAttackAction artilleryAttackAction = (ArtilleryAttackAction) weaponAttackAction;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3121);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(weaponType.getName() + " (" + ammoType.getShortName() + ')');
                r.add(artilleryAttackAction.getTurnsTilHit());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;

                artyMsg = "Artillery fire Incoming, landing on round "
                      + (game.getRoundCount() + artilleryAttackAction.getTurnsTilHit())
                      + ", fired by "
                      + game.getPlayer(artilleryAttackAction.getPlayerId()).getName();
                if (artilleryAttackAction.getTarget(game) != null) {
                    game.getBoard(artilleryAttackAction.getTarget(game).getBoardId()).addSpecialHexDisplay(
                          artilleryAttackAction.getTarget(game).getPosition(),
                          new SpecialHexDisplay(
                                Type.ARTILLERY_INCOMING, game
                                .getRoundCount() + artilleryAttackAction.getTurnsTilHit(),
                                game.getPlayer(artilleryAttackAction.getPlayerId()), artyMsg,
                                SpecialHexDisplay.SHD_VISIBLE_TO_TEAM));
                }
            }
            // if this is the last targeting phase before we hit,
            // make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (artilleryAttackAction.getTurnsTilHit() == 0) {
                setAnnouncedEntityFiring(false);
            }
            return true;
        }
        if (artilleryAttackAction.getTurnsTilHit() > 0) {
            artilleryAttackAction.decrementTurnsTilHit();
            return true;
        }

        boolean targetIsEntity = target != null && target.getTargetType() == Targetable.TYPE_ENTITY;

        // Offboard shots are targeted at an entity rather than a hex. If null, the
        // target has disengaged. Same if the target is no longer in game. Dead?
        if ((target == null || (targetIsEntity && artilleryAttackAction.getTarget(game) == null))) {
            Report r = new Report(3158);
            r.indent();
            r.subject = subjectId;
            r.add(weaponType.getName());
            vPhaseReport.add(r);
            return true;
        }
        final Vector<Integer> spottersBefore = artilleryAttackAction.getSpotterIds();
        logger.debug("Artillery resolution: spottersBefore={}, size={}",
              spottersBefore, spottersBefore != null ? spottersBefore.size() : "null");

        Coords targetPos = target.getPosition();
        Coords finalPos;

        // Handle counter-battery on fleeing/fled off-board targets.
        if (null == targetPos) {
            logger.error("Artillery Target {} is missing; off-board target fled?", weaponAttackAction.getTargetId());
            return false;
        }

        boolean isFlak = targetIsEntity && Compute.isFlakAttack(attackingEntity, (Entity) target);
        boolean asfFlak = isFlak && target.isAirborne();
        if (attackingEntity == null) {
            logger.error("Artillery Entity is null!");
            return true;
        }

        // Use the Artillery skill for spotting if enabled, as per page 144 of the
        // third printing of A Time of War.
        boolean useArtillerySkill = game.getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL);

        // If at least one valid spotter, then get the benefits thereof.
        if (!isFlak) {
            Optional<Entity> bestSpotter = ArtilleryHandlerHelper.findSpotter(spottersBefore,
                  artilleryAttackAction.getPlayerId(), game, target);

            // Check if this is adjusted fire (previous shot has landed at this hex)
            // Spotter bonuses only apply to adjusted fire, not first shots
            int existingMod = attackingEntity.aTracker.getModifier(weapon, targetPos);
            boolean isAdjustedFire = (existingMod != 0);
            logger.debug("Artillery spotter check: existingMod={}, isAdjustedFire={}, bestSpotter={}",
                  existingMod, isAdjustedFire, bestSpotter.isPresent() ? bestSpotter.get().getDisplayName() : "none");

            // Remove informational message from first-shot preview
            toHit.removeModifier("spotter available");

            // If at least one valid spotter AND this is adjusted fire, apply spotter bonuses
            if (bestSpotter.isPresent() && isAdjustedFire) {
                logger.debug("Applying adjusted fire spotter bonuses");
                // Remove FO/comm implant if present from ComputeToHit preview
                // to avoid double-counting - we recalculate everything here
                toHit.removeModifier("forward observer");
                toHit.removeModifier("comm implant");

                // Add spotter gunnery modifier
                int spotterGunnery = useArtillerySkill ?
                      bestSpotter.get().getCrew().getArtillery() :
                      bestSpotter.get().getCrew().getGunnery();
                int gunneryMod = (spotterGunnery - 4) / 2;
                logger.debug("  Spotter gunnery: skill={}, modifier={}", spotterGunnery, gunneryMod);
                if (gunneryMod != 0) {
                    toHit.addModifier(gunneryMod, Messages.getString("WeaponAttackAction.SpotterGunnery"));
                }

                // Add Forward Observer modifier separately
                boolean hasFO = bestSpotter.get().hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER);
                logger.debug("  Spotter FO check: hasFO={}, modifier={}", hasFO, hasFO ? -2 : 0);
                if (hasFO) {
                    toHit.addModifier(-2, Messages.getString("WeaponAttackAction.SpotterFO"));
                }

                // Comm implant bonus only applies to non-infantry spotters
                // Boosted comm implant provides same benefit as regular comm implant
                boolean isInfantry = bestSpotter.get() instanceof Infantry;
                boolean hasCommImplant = bestSpotter.get().hasAbility(OptionsConstants.MD_COMM_IMPLANT) ||
                      bestSpotter.get().hasAbility(OptionsConstants.MD_BOOST_COMM_IMPLANT);
                int commImplantMod = (!isInfantry && hasCommImplant) ? -1 : 0;
                logger.debug("  Spotter comm implant check: isInfantry={}, hasCommImplant={}, modifier={}",
                      isInfantry, hasCommImplant, commImplantMod);
                if (!isInfantry && hasCommImplant) {
                    toHit.addModifier(-1, Messages.getString("WeaponAttackAction.CommImplantArtillerySpotter"));
                }

                logger.debug("  Final toHit value after spotter bonuses: {}", toHit.getValue());
            }

            // If the shot hit the target hex, then all subsequent
            // fire will hit the hex automatically.
            // This should only happen for indirect shots
            if (roll.getIntValue() >= toHit.getValue()
                  && !(this instanceof ArtilleryWeaponDirectFireHandler)) {
                attackingEntity.aTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, targetPos);
                logger.debug("Artillery HIT - setting AUTOMATIC_SUCCESS for pos={}", targetPos);
            }
            // If the shot missed, but was adjusted by a
            // spotter, future shots are more likely to hit.

            // Note: Because artillery fire is adjusted on a per-unit basis,
            // this can result in a unit firing multiple artillery weapons at
            // the same hex getting this bonus more than once per turn. Since
            // the Artillery Modifiers Table on TacOps p. 180 lists a -1 per
            // shot (not salvo!) previously fired at the target hex, this would
            // in fact appear to be correct.
            // Only apply these modifiers to indirect artillery
            else if ((bestSpotter.isPresent()) && !(this instanceof ArtilleryWeaponDirectFireHandler)) {
                // only add mods if it's not an automatic success
                if (attackingEntity.aTracker.getModifier(weapon, targetPos) != TargetRoll.AUTOMATIC_SUCCESS) {
                    if (bestSpotter.get().hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                        attackingEntity.aTracker.setSpotterHasForwardObs(true);
                    }
                    // Comm implant bonus only applies to non-infantry spotters
                    // Boosted comm implant provides same benefit as regular comm implant
                    if (!(bestSpotter.get() instanceof Infantry) &&
                          (bestSpotter.get().hasAbility(OptionsConstants.MD_COMM_IMPLANT) ||
                                bestSpotter.get().hasAbility(OptionsConstants.MD_BOOST_COMM_IMPLANT))) {
                        attackingEntity.aTracker.setSpotterHasCommImplant(true);
                    }
                    int newMod = attackingEntity.aTracker.getModifier(weapon, targetPos) - 1;
                    attackingEntity.aTracker.setModifier(newMod, targetPos);
                    logger.debug("Artillery MISSED with spotter - setting modifier={} for pos={}, spotter={}",
                          newMod, targetPos, bestSpotter.get().getDisplayName());
                }
            } else {
                logger.debug("Artillery MISSED - no spotter found, bestSpotter.isPresent()={}",
                      bestSpotter.isPresent());
            }
        }

        // Report weapon attack and its to-hit value.
        Report r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        if (weaponType != null) {
            r.add(weaponType.getName() + " (" + ammoType.getShortName() + ')');
        } else {
            r.add("Error: From Nowhere");
        }

        r.add(target.getDisplayName(), true);
        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else {
            // roll to hit
            r = new Report(3150);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit);
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledAmmoAndReport) {
            addHeat();
        }

        finalPos = handleReportsAndDirectScatter(isFlak, targetPos, vPhaseReport, artilleryAttackAction);

        if (finalPos == null) {
            return false;
        }

        // Absolute level / altitude, for blast calculations
        int height = 0;
        Hex finalHex = game.getBoard().getHex(finalPos);
        if (finalHex != null) {
            if (targetIsEntity) {
                if (!isFlak && !bMissed) {
                    height = finalHex.getLevel() + target.getElevation();
                } else if (isFlak) {
                    height = (asfFlak) ? target.getAltitude() : finalHex.getLevel() + target.getElevation();
                }
            } else {
                height = finalHex.getLevel();
            }
        }

        // if attacker is an off-board artillery piece, check to see if we need to set
        // observation flags
        if (artilleryAttackAction.getEntity(game).isOffBoard()) {
            handleCounterBatteryObservation(artilleryAttackAction, finalPos, vPhaseReport);
        }

        // if we have no ammo for this attack then don't bother doing anything else, but
        // log the error
        if (ammoType == null) {
            logger.warn("Artillery weapon fired with no ammo.\n\n{}", (Object) Thread.currentThread().getStackTrace());
            return false;
        }

        if (ammoType.getMunitionType().contains(Munitions.M_FAE)) {
            handleArtilleryDriftMarker(targetPos, finalPos, artilleryAttackAction,
                  AreaEffectHelper.processFuelAirDamage(
                        finalPos, height, ammoType, artilleryAttackAction.getEntity(game), vPhaseReport, gameManager));
            return false;
        }

        if (ammoType.getMunitionType().contains(Munitions.M_FLARE)) {
            int radius;
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV) {
                radius = 4;
            } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LONG_TOM) {
                radius = 3;
            } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SNIPER) {
                radius = 2;
            } else {
                radius = 1;
            }
            gameManager.deliverArtilleryFlare(finalPos, radius);
            return false;
        }
        if (ammoType.getMunitionType().contains(Munitions.M_DAVY_CROCKETT_M)) {
            if (target.isOffBoard()) {
                gameManager.getGame().processGameEvent(
                      new GamePlayerStrategicActionEvent(gameManager,
                            new NukeDetonatedAction(attackingEntity.getId(),
                                  attackingEntity.getOwnerId(),
                                  AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                AreaEffectHelper.doNuclearExplosion((Entity) artilleryAttackAction.getTarget(game),
                      finalPos,
                      1,
                      vPhaseReport,
                      gameManager);
            } else {
                gameManager.drawNukeHitOnBoard(targetPos);
                gameManager.getGame().processGameEvent(
                      new GamePlayerStrategicActionEvent(gameManager,
                            new NukeDetonatedAction(attackingEntity.getId(),
                                  attackingEntity.getOwnerId(),
                                  AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                gameManager.doNuclearExplosion(finalPos, 1, vPhaseReport);
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(Munitions.M_FASCAM)) {
            int rackSize = ammoType.getRackSize();
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV) {
                rackSize = ammoType.isClan() ? 30 : 20;
            }
            gameManager.deliverFASCAMMinefield(finalPos,
                  attackingEntity.getOwner().getId(),
                  rackSize,
                  attackingEntity.getId());
            return false;
        }
        if (ammoType.getMunitionType().contains(Munitions.M_INFERNO_IV)) {
            handleArtilleryDriftMarker(targetPos, finalPos, artilleryAttackAction,
                  gameManager.deliverArtilleryInferno(finalPos, attackingEntity, subjectId, vPhaseReport));
            return false;
        }
        if (ammoType.getMunitionType().contains(Munitions.M_VIBRABOMB_IV)) {
            gameManager.deliverThunderVibraMinefield(finalPos, attackingEntity.getOwner().getId(), 30,
                  weaponAttackAction.getOtherAttackInfo(), attackingEntity.getId());
            return false;
        }
        if (ammoType.getMunitionType().contains(Munitions.M_SMOKE)) {
            gameManager.deliverArtillerySmoke(finalPos, vPhaseReport);
            return false;
        }
        if (ammoType.getMunitionType().contains(Munitions.M_LASER_INHIB)) {
            gameManager.deliverLISmoke(finalPos, vPhaseReport);
            return false;
        }

        // check to see if this is a mine clearing attack
        // According to the RAW you have to hit the right hex to hit even if the
        // scatter hex has minefields
        boolean mineClear = target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR;
        if (mineClear && !isFlak && !bMissed) {
            r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

            AreaEffectHelper.clearMineFields(finalPos, Minefield.CLEAR_NUMBER_WEAPON,
                  attackingEntity, vPhaseReport, game,
                  gameManager);
        }

        Targetable updatedTarget = artilleryAttackAction.getTarget(game);
        // Calculate blast damage and falloff
        DamageFalloff damageFalloff = AreaEffectHelper.calculateDamageFallOff(ammoType, shootingBA, mineClear);

        // the attack's target may have been destroyed or fled since the attack was generated
        // so we need to carry out offboard/null checks against the "current" version of the target.
        // Note: currently this only damages the target and does not deal blast damage to "nearby" off-board units.
        // TODO: Sleet01: rename ammoType, attackingEntity, etc. to comport with current coding standards
        if ((updatedTarget != null) && updatedTarget.isOffBoard()) {
            // Calculate blast damage shape
            HashMap<Map.Entry<Integer, Coords>, Integer> blastShape = AreaEffectHelper.shapeBlast(
                  ammoType, finalPos, damageFalloff, 0, true, false, false, game, false
            );

            Map.Entry<Integer, Coords> entry = Map.entry(updatedTarget.getElevation(), updatedTarget.getPosition());
            if (blastShape.containsKey(entry)) {
                AreaEffectHelper.artilleryDamageEntity(
                      (Entity) updatedTarget, blastShape.get(entry), null,
                      0, false, asfFlak, isFlak, entry.getKey(),
                      finalPos, ammoType, entry.getValue(), false, attackingEntity, null, updatedTarget.getId(),
                      vPhaseReport, gameManager
                );
            }
        } else {
            handleArtilleryDriftMarker(targetPos, finalPos, artilleryAttackAction,
                  gameManager.artilleryDamageArea(finalPos, target.getBoardId(), ammoType,
                        subjectId, attackingEntity, damageFalloff, isFlak, height, vPhaseReport,
                        asfFlak));
        }

        // artillery may unintentionally clear minefields, but only if it wasn't trying
        // to
        if (!mineClear) {
            AreaEffectHelper.clearMineFields(finalPos, Minefield.CLEAR_NUMBER_WEAPON_ACCIDENT,
                  attackingEntity, vPhaseReport, game,
                  gameManager);
        }

        return false;
    }

    /**
     * Worker function that handles "artillery round landed here" reports, and direct artillery scatter.
     *
     * @return Whether we should continue attack resolution afterward
     */
    private Coords handleReportsAndDirectScatter(boolean isFlak, Coords targetPos, Vector<Report> vPhaseReport,
          ArtilleryAttackAction aaa) {
        Coords originalTargetPos = targetPos;

        Report r;
        // special report for off-board target
        if (target.isOffBoard()) {
            r = new Report(9994);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        if (!bMissed) {
            // off-board targets can just report direct hit and move on
            if (target.isOffBoard()) {
                r = new Report(9996);
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r);
                return targetPos;
            }

            if (!isFlak) {
                r = new Report(3190);
            } else {
                r = new Report(3191);
            }
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);

            if (!isFlak) {
                String artyMsg = "Artillery hit here on round " +
                      game.getRoundCount() +
                      ", fired by " +
                      game.getPlayer(aaa.getPlayerId()).getName() +
                      " (this hex is now an auto-hit)";
                game.getBoard()
                      .addSpecialHexDisplay(targetPos,
                            new SpecialHexDisplay(Type.ARTILLERY_HIT,
                                  game.getRoundCount(),
                                  game.getPlayer(aaa.getPlayerId()),
                                  artyMsg));
            }
        } else {
            // direct fire artillery only scatters by one d6
            // we do this here to avoid duplicating handle()
            // in the ArtilleryWeaponDirectFireHandler
            Coords origPos = targetPos;
            int moF = toHit.getMoS();
            if (attackingEntity.hasAbility("oblique_artillery")) {
                // getMoS returns a negative MoF
                // simple math is better so lets make it positive
                moF = Math.max(moF + 2, 0);
            }
            targetPos = Compute.scatterDirectArty(targetPos, moF);
            if (game.getBoard().contains(targetPos)) {
                // misses and scatters to another hex
                if (!isFlak) {
                    r = new Report(3195);

                    String artyMsg = "Artillery missed here on round "
                          + game.getRoundCount() + ", by "
                          + game.getPlayer(aaa.getPlayerId()).getName()
                          + ", drifted to " + targetPos.getBoardNum();
                    game.getBoard().addSpecialHexDisplay(
                          origPos,
                          new SpecialHexDisplay(Type.ARTILLERY_MISS,
                                game.getRoundCount(),
                                game.getPlayer(aaa.getPlayerId()),
                                artyMsg));
                } else {
                    r = new Report(3192);
                }
                r.subject = subjectId;
                r.add(targetPos.getBoardNum());
                vPhaseReport.addElement(r);
            } else if (target.isOffBoard()) {
                // off-board targets should report scatter distance
                r = new Report(9995);
                r.add(originalTargetPos.distance(targetPos));
                r.subject = subjectId;
                r.indent();
                vPhaseReport.addElement(r);
            } else if (!target.isOffBoard()) {
                // misses and scatters off-board
                if (isFlak) {
                    r = new Report(3193);
                } else {
                    r = new Report(3200);
                }
                r.subject = subjectId;
                vPhaseReport.addElement(r);

                String artyMsg = "Artillery missed here on round "
                      + game.getRoundCount() + ", by "
                      + game.getPlayer(aaa.getPlayerId()).getName()
                      + ", drifted off the board";
                game.getBoard().addSpecialHexDisplay(
                      origPos,
                      new SpecialHexDisplay(Type.ARTILLERY_MISS,
                            game.getRoundCount(),
                            game.getPlayer(aaa.getPlayerId()),
                            artyMsg));
                return null;
            }
        }
        return targetPos;
    }

    /**
     * Worker function that contains logic for "has my shot been observed so that I can be targeted by counter-battery
     * fire"
     */
    private void handleCounterBatteryObservation(WeaponAttackAction aaa, Coords targetPos,
          Vector<Report> vPhaseReport) {
        // if the round landed on the board, and the attacker is an off-board artillery piece
        // then check to see if the hex where it landed can be seen by anyone on an opposing team
        // if so, mark the attacker so that it can be targeted by counter-battery fire
        if (game.getBoard().contains(targetPos)) {
            HexTarget hexTarget = new HexTarget(targetPos, Targetable.TYPE_HEX_ARTILLERY);

            for (Entity entity : game.getEntitiesVector()) {

                // if the entity is hostile and the attacker has not been designated
                // as observed already by the entity's team
                if (entity.isEnemyOf(aaa.getEntity(game)) &&
                      !aaa.getEntity(game).isOffBoardObserved(entity.getOwner().getTeam())) {
                    boolean hasLoS = LosEffects.calculateLOS(game, entity, hexTarget).canSee();

                    if (hasLoS) {
                        aaa.getEntity(game).addOffBoardObserver(entity.getOwner().getTeam());
                        Report r = new Report(9997);
                        r.add(entity.getDisplayName());
                        r.subject = subjectId;
                        vPhaseReport.add(r);
                    }
                }
            }
            // an off-board target can observe counter-battery fire attacking it for
            // counter-battery fire (probably)
        } else if (target.isOffBoard()) {
            Entity attacker = aaa.getEntity(game);
            int targetTeam = ((Entity) target).getOwner().getTeam();

            if (attacker.isOffBoard() && !attacker.isOffBoardObserved(targetTeam)) {
                attacker.addOffBoardObserver(targetTeam);

                Report r = new Report(9997);
                r.add(target.getDisplayName());
                r.subject = subjectId;
                vPhaseReport.add(r);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getRackSize();
        if (weaponType instanceof CapitalMissileWeapon) {
            toReturn = weaponType.getRoundShortAV();
        }
        // BA Tube artillery is the only artillery that can be mounted by BA
        // so we do the multiplication here
        if (attackingEntity instanceof BattleArmor ba) {
            toReturn *= ba.getNumberActiveTroopers();
        }
        // area effect damage is double
        if (target.isConventionalInfantry()) {
            toReturn /= 0.5;
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        return (int) Math.ceil(toReturn);
    }
}
