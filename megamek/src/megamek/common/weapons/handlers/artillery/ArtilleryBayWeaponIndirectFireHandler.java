/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.NukeDetonatedAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.event.player.GamePlayerStrategicActionEvent;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntitySelector;
import megamek.common.units.Infantry;
import megamek.common.units.Targetable;
import megamek.common.weapons.ArtilleryHandlerHelper;
import megamek.common.weapons.handlers.AmmoBayWeaponHandler;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class ArtilleryBayWeaponIndirectFireHandler extends AmmoBayWeaponHandler {
    private static final MMLogger LOGGER = MMLogger.create(ArtilleryBayWeaponIndirectFireHandler.class);

    @Serial
    private static final long serialVersionUID = -1277649123562229298L;
    boolean handledAmmoAndReport = false;

    /**
     * This constructor can only be used for deserialization.
     */
    protected ArtilleryBayWeaponIndirectFireHandler() {
        super();
    }

    public ArtilleryBayWeaponIndirectFireHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    public boolean cares(final GamePhase phase) {
        return phase.isOffboard() || phase.isTargeting();
    }

    @Override
    protected void useAmmo() {
        numWeaponsHit = weapon.getBayWeapons().size();
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                LOGGER.debug("Handler can't find any ammo! Oh no!");
                return;
            }

            int shots = bayW.getCurrentShots();
            // if this option is on, we may have odd amounts of ammo in multiple bins. Only
            // fire rounds that we have.
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_ARTILLERY_MUNITIONS)) {
                if (bayWAmmo.getUsableShotsLeft() < 1) {
                    numWeaponsHit--;
                } else {
                    bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                }
            } else {
                // By default, rules, we have just one ammo bin with at least 10 shots for each
                // weapon in the bay,
                // so we'll track ammo normally and need to resolve attacks for all bay weapons.
                for (int i = 0; i < shots; i++) {
                    if (null == bayWAmmo
                          || bayWAmmo.getUsableShotsLeft() < 1) {
                        // try loading something else
                        attackingEntity.loadWeaponWithSameAmmo(bayW);
                        bayWAmmo = bayW.getLinkedAmmo();
                    }
                    if (null != bayWAmmo) {
                        bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        String artyMsg;
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) weaponAttackAction;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3121);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(weaponType.getName() + (ammoType != null ? " (" + ammoType.getShortName() + ")" : ""));
                r.add(aaa.getTurnsTilHit());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;

                artyMsg = "Artillery bay fire Incoming, landing on round "
                      + (game.getRoundCount() + aaa.getTurnsTilHit())
                      + ", fired by "
                      + game.getPlayer(aaa.getPlayerId()).getName();
                game.getBoard(aaa.getTarget(game).getBoardId()).addSpecialHexDisplay(
                      aaa.getTarget(game).getPosition(),
                      new SpecialHexDisplay(
                            SpecialHexDisplay.Type.ARTILLERY_INCOMING, game
                            .getRoundCount() + aaa.getTurnsTilHit(),
                            game.getPlayer(aaa.getPlayerId()), artyMsg,
                            SpecialHexDisplay.SHD_VISIBLE_TO_TEAM));
            }
            // if this is the last targeting phase before we hit,
            // make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (aaa.getTurnsTilHit() == 0) {
                setAnnouncedEntityFiring(false);
            }
            return true;
        }

        if (aaa.getTurnsTilHit() > 0) {
            aaa.decrementTurnsTilHit();
            return true;
        }
        // Offboard shots are targeted at an entity rather than a hex. If null, the
        // target has disengaged.
        if (target == null) {
            Report r = new Report(3158);
            r.indent();
            r.subject = subjectId;
            r.add(weaponType.getName());
            vPhaseReport.add(r);
            return true;
        }
        final Vector<Integer> spottersBefore = aaa.getSpotterIds();
        Coords origPos = target.getPosition();
        Coords targetPos = target.getPosition();
        final int playerId = aaa.getPlayerId();
        boolean targetIsEntity = target.getTargetType() == Targetable.TYPE_ENTITY;
        boolean isFlak = targetIsEntity && Compute.isFlakAttack(attackingEntity, (Entity) target);
        boolean asfFlak = isFlak && target.isAirborne();
        boolean mineClear = target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR;
        Entity bestSpotter = null;
        if (attackingEntity == null) {
            LOGGER.error("Artillery Entity is null!");
            return true;
        }

        Mounted<?> ammoUsed = attackingEntity.getEquipment(aaa.getAmmoId());
        final AmmoType ammoType = (AmmoType) ammoUsed.getType();

        // Are there any valid spotters?
        if ((null != spottersBefore) && !isFlak) {
            // fetch possible spotters now
            Iterator<Entity> spottersAfter = game.getSelectedEntities(new EntitySelector() {
                public final int player = playerId;

                public final Targetable targ = target;

                @Override
                public boolean accept(Entity entity) {
                    Integer id = entity.getId();
                    return (player == entity.getOwnerId())
                          && spottersBefore.contains(id)
                          && !LosEffects.calculateLOS(game, entity, targ, true).isBlocked()
                          && entity.isActive()
                          // airborne aerospace can't spot for arty
                          && !(entity.isAero() && entity.isAirborne())
                          && !entity.isINarcedWith(INarcPod.HAYWIRE);
                }
            });

            // Out of any valid spotters, pick the best.
            while (spottersAfter.hasNext()) {
                Entity ent = spottersAfter.next();
                if (bestSpotter == null) {
                    bestSpotter = ent;
                } else if (ent.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)
                      && !bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                    bestSpotter = ent;
                } else if (ent.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()
                      && !bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                    bestSpotter = ent;
                } else if (bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)
                      && ent.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                    if (ent.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()) {
                        bestSpotter = ent;
                    }
                }
            }
        }

        // If at least one valid spotter, then get the benefits thereof.
        if (null != bestSpotter) {
            int foMod = 0;
            if (bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                foMod = -2;
            }
            // Comm implant bonus only applies to non-infantry spotters
            // Boosted comm implant provides same benefit as regular comm implant
            int commImplantMod = 0;
            if (!(bestSpotter instanceof Infantry) &&
                  (bestSpotter.hasAbility(OptionsConstants.MD_COMM_IMPLANT) ||
                        bestSpotter.hasAbility(OptionsConstants.MD_BOOST_COMM_IMPLANT))) {
                commImplantMod = -1;
            }
            int mod = (bestSpotter.getCrew().getGunnery() - 4) / 2;
            mod += foMod + commImplantMod;
            toHit.addModifier(mod, "Spotting modifier");
        }

        // Is the attacker still alive, and we're not shooting FLAK?
        // then adjust the target
        if (!isFlak) {
            // If the shot hit the target hex, then all subsequent
            // fire will hit the hex automatically.
            if (roll.getIntValue() >= toHit.getValue()) {
                attackingEntity.aTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, targetPos);
            } else if (null != bestSpotter) {
                // If the shot missed, but was adjusted by a spotter, future shots are more likely to hit. Note:
                // Because artillery fire is adjusted on a per-unit basis, this can result in a unit firing multiple
                // artillery weapons at the same hex getting this bonus more than once per turn. Since the Artillery
                // Modifiers Table on TacOps p. 180 lists a -1 per shot (not salvo!) previously fired at the target
                // hex, this would in fact appear to be correct.

                // only add mods if it's not an automatic success
                if (attackingEntity.aTracker.getModifier(weapon, targetPos) != TargetRoll.AUTOMATIC_SUCCESS) {
                    if (bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                        attackingEntity.aTracker.setSpotterHasForwardObs(true);
                    }
                    // Comm implant bonus only applies to non-infantry spotters
                    // Boosted comm implant provides same benefit as regular comm implant
                    if (!(bestSpotter instanceof Infantry) &&
                          (bestSpotter.hasAbility(OptionsConstants.MD_COMM_IMPLANT) ||
                                bestSpotter.hasAbility(OptionsConstants.MD_BOOST_COMM_IMPLANT))) {
                        attackingEntity.aTracker.setSpotterHasCommImplant(true);
                    }
                    attackingEntity.aTracker.setModifier(attackingEntity.aTracker.getModifier(weapon, targetPos) - 1,
                          targetPos);
                }
            }
        }

        // Report weapon attack and its to-hit value.
        Report report = new Report(3120);
        report.indent();
        report.newlines = 0;
        report.subject = subjectId;
        if (weaponType != null) {
            report.add(weaponType.getName());
        } else {
            report.add("Error: From Nowhere");
        }

        report.add(target.getDisplayName(), true);
        vPhaseReport.addElement(report);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            report = new Report(3135);
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            report = new Report(3140);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            report = new Report(3145);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit.getDesc());
            vPhaseReport.addElement(report);
        } else {
            // roll to hit
            report = new Report(3150);
            report.newlines = 0;
            report.subject = subjectId;
            report.add(toHit);
            vPhaseReport.addElement(report);
        }

        // dice have been rolled, thanks
        report = new Report(3155);
        report.newlines = 0;
        report.subject = subjectId;
        report.add(roll);
        vPhaseReport.addElement(report);

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledAmmoAndReport) {
            addHeat();
        }

        // In the case of misses, we'll need to hit multiple hexes
        List<Coords> targets = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        Hex targetHex;

        if (!bMissed) {
            report = new Report(3199);
            report.subject = subjectId;
            report.add(numWeaponsHit);
            report.add(targetPos.getBoardNum());
            report.add(ammoType.getShortName());
            // Mine clearance has its own report which will get added
            if (!mineClear) {
                vPhaseReport.addElement(report);
            }
            targetHex = game.getBoard(target.getBoardId()).getHex(targetPos);
            heights.add((targetHex != null) ? game.getBoard(target.getBoardId()).getHex(targetPos).getLevel() : 0);
            artyMsg = "Artillery hit here on round " + game.getRoundCount()
                  + ", fired by " + game.getPlayer(aaa.getPlayerId()).getName()
                  + " (this hex is now an auto-hit)";
            game.getBoard(target.getBoardId()).addSpecialHexDisplay(targetPos,
                  new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_HIT,
                        game.getRoundCount(), game.getPlayer(aaa.getPlayerId()), artyMsg));
        } else {
            int moF = toHit.getMoS();
            if (attackingEntity.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY)) {
                // getMoS returns a negative MoF
                // simple math is better so lets make it positive
                if ((-moF - 2) < 1) {
                    moF = 0;
                } else {
                    moF = moF + 2;
                }
            }
            // We're only going to display one missed shot hex on the board, at the intended
            // target
            // Any drifted shots will be indicated at their end points
            artyMsg = "Bay Artillery missed here on round "
                  + game.getRoundCount() + ", by "
                  + game.getPlayer(aaa.getPlayerId()).getName();
            game.getBoard().addSpecialHexDisplay(origPos,
                  new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_MISS, game.getRoundCount(),
                        game.getPlayer(aaa.getPlayerId()), artyMsg));
            while (numWeaponsHit > 0) {
                // We'll generate a new report and scatter for each weapon fired
                targetPos = Compute.scatterDirectArty(origPos, moF);
                if (game.getBoard().contains(targetPos)) {
                    targets.add(targetPos);
                    targetHex = game.getBoard().getHex(targetPos);
                    if (targetHex != null) {
                        heights.add(
                              (isFlak) ? (
                                    (asfFlak) ? target.getAltitude() : targetHex.getLevel() + target.getElevation()
                              ) : targetHex.getLevel()
                        );
                    } else {
                        heights.add(0);
                    }
                    // misses and scatters to another hex
                    if (!isFlak) {
                        report = new Report(3202);
                        report.subject = subjectId;
                        report.newlines = 1;
                        report.add(ammoType.getShortName());
                    } else {
                        report = new Report(3192);
                        report.subject = subjectId;
                        report.newlines = 1;
                    }
                    report.add(targetPos.getBoardNum());
                    vPhaseReport.addElement(report);
                } else {
                    // misses and scatters off-board
                    report = new Report(3200);
                    report.subject = subjectId;
                    report.newlines = 1;
                    vPhaseReport.addElement(report);
                }
                numWeaponsHit--;
            }
            // If we managed to land everything off the board, stop
            if (targets.isEmpty()) {
                return !bMissed;
            }
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLARE)) {
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

            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverArtilleryFlare(targetPos, radius);
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverArtilleryFlare(c, radius);
                }
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DAVY_CROCKETT_M)) {
            if (!bMissed) {
                // Keep blasting the target hex with each weapon in the bay that fired
                while (numWeaponsHit > 0) {
                    gameManager.getGame().processGameEvent(
                          new GamePlayerStrategicActionEvent(gameManager,
                                new NukeDetonatedAction(attackingEntity.getId(),
                                      attackingEntity.getOwnerId(),
                                      AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                    gameManager.doNuclearExplosion(targetPos, 1, vPhaseReport);
                    numWeaponsHit--;
                }
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.getGame().processGameEvent(
                          new GamePlayerStrategicActionEvent(gameManager,
                                new NukeDetonatedAction(attackingEntity.getId(),
                                      attackingEntity.getOwnerId(),
                                      AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                    gameManager.doNuclearExplosion(c, 1, vPhaseReport);
                }
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FASCAM)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverFASCAMMinefield(targetPos, attackingEntity.getOwner().getId(),
                      ammoType.getRackSize(), attackingEntity.getId());
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverFASCAMMinefield(c, attackingEntity.getOwner().getId(),
                          ammoType.getRackSize(), attackingEntity.getId());
                }
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverArtilleryInferno(targetPos, attackingEntity, subjectId, vPhaseReport);
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {

                    handleArtilleryDriftMarker(origPos, c, aaa,
                          gameManager.deliverArtilleryInferno(c, attackingEntity, subjectId, vPhaseReport));
                }
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverThunderVibraMinefield(targetPos, attackingEntity.getOwner().getId(),
                      ammoType.getRackSize(), weaponAttackAction.getOtherAttackInfo(), attackingEntity.getId());
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverThunderVibraMinefield(c, attackingEntity.getOwner().getId(),
                          ammoType.getRackSize(), weaponAttackAction.getOtherAttackInfo(), attackingEntity.getId());
                }
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverArtillerySmoke(targetPos, vPhaseReport);
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverArtillerySmoke(c, vPhaseReport);
                }
            }
            return false;
        }
        if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_LASER_INHIB)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverLISmoke(targetPos, vPhaseReport);
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverLISmoke(c, vPhaseReport);
                }
            }
            return false;
        }

        // check to see if this is a mine clearing attack
        // According to the RAW you have to hit the right hex to hit even if the
        // scatter hex has minefields
        if (mineClear && game.containsMinefield(targetPos) && !isFlak && !bMissed) {
            report = new Report(3255);
            report.indent(1);
            report.subject = subjectId;
            vPhaseReport.addElement(report);

            Enumeration<Minefield> minefields = game.getMinefields(targetPos).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (gameManager.clearMinefield(mf, attackingEntity, Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
                    mfRemoved.add(mf);
                }
            }
            // we have to do it this way to avoid a concurrent error problem
            for (Minefield mf : mfRemoved) {
                gameManager.removeMinefield(mf);
            }
        }
        if (!bMissed) {
            // artillery may unintentionally clear minefields, but only if it wasn't
            // trying to. For a hit on the target, just do this once.
            if (!mineClear && game.containsMinefield(targetPos)) {
                ArtilleryHandlerHelper.getMinefields(vPhaseReport, targetPos, game, attackingEntity, gameManager);
            }
            // Here we're doing damage for each hit with more standard artillery shells
            while (numWeaponsHit > 0) {
                gameManager.artilleryDamageArea(targetPos, ammoType,
                      subjectId, attackingEntity, isFlak, heights.get(0), mineClear, vPhaseReport,
                      asfFlak);
                numWeaponsHit--;
            }
        } else {
            // Now if we missed, resolve a strike on each scatter hex
            Coords c;
            int height;
            for (int index = 0; index < targets.size(); index++) {
                c = targets.get(index);
                height = heights.get(index);
                // Accidental mine clearance...
                if (!mineClear && game.containsMinefield(c)) {
                    ArtilleryHandlerHelper.getMinefields(vPhaseReport, c, game, attackingEntity, gameManager);
                }
                handleArtilleryDriftMarker(origPos, c, aaa,
                      gameManager.artilleryDamageArea(c, ammoType, subjectId, attackingEntity, isFlak,
                            height, mineClear, vPhaseReport, asfFlak));
            }

        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = weaponType.getDamage();
        // area effect damage is double
        if (target.isConventionalInfantry()) {
            toReturn /= 0.5;
        }

        toReturn = applyGlancingBlowModifier(toReturn, false);

        return (int) Math.ceil(toReturn);
    }
}
