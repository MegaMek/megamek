/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import megamek.common.*;
import megamek.common.AmmoType.Munitions;
import megamek.common.SpecialHexDisplay.Type;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.NukeDetonatedAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePlayerStrategicActionEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AreaEffectHelper.DamageFalloff;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.logging.MMLogger;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ArtilleryWeaponIndirectFireHandler extends AmmoWeaponHandler {
    private static final MMLogger logger = MMLogger.create(ArtilleryBayWeaponIndirectFireHandler.class);

    private static final long serialVersionUID = -1277649123562229298L;
    boolean handledAmmoAndReport = false;
    private int shootingBA = -1;

    public ArtilleryWeaponIndirectFireHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        if (w.getEntity(g) instanceof BattleArmor) {
            shootingBA = ((BattleArmor) w.getEntity(g)).getNumberActiverTroopers();
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
        ArtilleryAttackAction artilleryAttackAction = (ArtilleryAttackAction) waa;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3121);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(wtype.getName() + " (" + atype.getShortName() + ')');
                r.add(artilleryAttackAction.getTurnsTilHit());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;

                artyMsg = "Artillery fire Incoming, landing on round "
                        + (game.getRoundCount() + artilleryAttackAction.getTurnsTilHit())
                        + ", fired by "
                        + game.getPlayer(aaa.getPlayerId()).getName();
                if (aaa.getTarget(game) != null) {
                    game.getBoard(aaa.getTarget(game).getBoardId()).addSpecialHexDisplay(
                            aaa.getTarget(game).getPosition(),
                            new SpecialHexDisplay(
                                    Type.ARTILLERY_INCOMING, game
                                            .getRoundCount() + aaa.getTurnsTilHit(),
                                    game.getPlayer(aaa.getPlayerId()), artyMsg,
                                    SpecialHexDisplay.SHD_VISIBLETO_TEAM));
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
            r.add(wtype.getName());
            vPhaseReport.add(r);
            return true;
        }
        final Vector<Integer> spottersBefore = artilleryAttackAction.getSpotterIds();

        Coords targetPos = target.getPosition();
        Coords finalPos = target.getPosition();

        // Handle counter-battery on fleeing/fled off-board targets.
        if (null == targetPos) {
            logger
                    .error(String.format("Artillery Target %s is missing; off-board target fled?", waa.getTargetId()));
            return false;
        }

        final int playerId = artilleryAttackAction.getPlayerId();
        boolean isFlak = targetIsEntity && Compute.isFlakAttack(ae, (Entity) target);
        boolean asfFlak = isFlak && target.isAirborne();
        Entity bestSpotter = null;
        if (ae == null) {
            logger.error("Artillery Entity is null!");
            return true;
        }

        // Trailers can share ammo, which means the entity carrying the ammo might not
        // be
        // the firing entity, so we get the specific ammo used from the ammo carrier
        // However, we only bother with this if the ammo carrier is actually different
        // from the attacker
        Entity ammoCarrier = ae;

        if (artilleryAttackAction.getAmmoCarrier() != ae.getId()) {
            ammoCarrier = artilleryAttackAction.getEntity(game, artilleryAttackAction.getAmmoCarrier());
        }

        // Use the Artillery skill for spotting if enabled, as per page 144 of the
        // third printing of A Time of War.
        boolean useArtillerySkill = game.getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL);

        // Are there any valid spotters?
        if ((null != spottersBefore) && !isFlak) {
            // fetch possible spotters now
            Iterator<Entity> spottersAfter = game
                    .getSelectedEntities(new EntitySelector() {
                        public int player = playerId;

                        public Targetable targ = target;

                        @Override
                        public boolean accept(Entity entity) {
                            Integer id = entity.getId();
                            return (player == entity.getOwnerId())
                                    && spottersBefore.contains(id)
                                    && !LosEffects.calculateLOS(game, entity, targ, true).isBlocked()
                                    && entity.isActive()
                            // airborne aeros can't spot for arty
                                    && !((entity.isAero()) && entity.isAirborne())
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
                } else if ((useArtillerySkill ? (ent.getCrew().getArtillery() < bestSpotter.getCrew().getArtillery())
                        : (ent.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()))
                        && !bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                    bestSpotter = ent;
                } else if (bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)
                        && ent.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                    if (useArtillerySkill ? (ent.getCrew().getArtillery() < bestSpotter.getCrew().getArtillery())
                            : (ent.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery())) {
                        bestSpotter = ent;
                    }
                }
            }
        }

        // If at least one valid spotter, then get the benefits thereof.
        if (null != bestSpotter) {
            int foMod = 0;
            if (bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                foMod = -1;
            }
            int mod = ((useArtillerySkill ? bestSpotter.getCrew().getArtillery() : bestSpotter.getCrew().getGunnery())
                    - 4) / 2;
            mod += foMod;
            toHit.addModifier(mod, "Spotting modifier");
        }

        // Is the attacker still alive and we're not shooting FLAK?
        // then adjust the target
        if (!isFlak) {

            // If the shot hit the target hex, then all subsequent
            // fire will hit the hex automatically.
            // This should only happen for indirect shots
            if (roll.getIntValue() >= toHit.getValue()
                    && !(this instanceof ArtilleryWeaponDirectFireHandler)) {
                ae.aTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, targetPos);
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
            else if ((null != bestSpotter) && !(this instanceof ArtilleryWeaponDirectFireHandler)) {
                // only add mods if it's not an automatic success
                if (ae.aTracker.getModifier(weapon, targetPos) != TargetRoll.AUTOMATIC_SUCCESS) {
                    if (bestSpotter.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)) {
                        ae.aTracker.setSpotterHasForwardObs(true);
                    }
                    ae.aTracker.setModifier(ae.aTracker.getModifier(weapon, targetPos) - 1, targetPos);
                }
            }
        }

        // Report weapon attack and its to-hit value.
        Report r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        if (wtype != null) {
            r.add(wtype.getName() + " (" + atype.getShortName() + ')');
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
        if (atype == null) {
            logger
                    .error("Artillery weapon fired with no ammo.\n\n" + Thread.currentThread().getStackTrace());
            return false;
        }

        if (atype.getMunitionType().contains(Munitions.M_FAE)) {
            handleArtilleryDriftMarker(targetPos, finalPos, artilleryAttackAction,
                    AreaEffectHelper.processFuelAirDamage(
                            finalPos, height, atype, artilleryAttackAction.getEntity(game), vPhaseReport, gameManager));
            return false;
        }

        if (atype.getMunitionType().contains(Munitions.M_FLARE)) {
            int radius;
            if (atype.getAmmoType() == AmmoType.T_ARROW_IV) {
                radius = 4;
            } else if (atype.getAmmoType() == AmmoType.T_LONG_TOM) {
                radius = 3;
            } else if (atype.getAmmoType() == AmmoType.T_SNIPER) {
                radius = 2;
            } else {
                radius = 1;
            }
            gameManager.deliverArtilleryFlare(finalPos, radius);
            return false;
        }
        if (atype.getMunitionType().contains(Munitions.M_DAVY_CROCKETT_M)) {
            // The appropriate term here is "Bwahahahahaha..."
            if (target.isOffBoard()) {
                gameManager.getGame().processGameEvent(
                    new GamePlayerStrategicActionEvent(gameManager,
                        new NukeDetonatedAction(ae.getId(), ae.getOwnerId(), AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                AreaEffectHelper.doNuclearExplosion((Entity) artilleryAttackAction.getTarget(game), finalPos, 1, vPhaseReport,
                        gameManager);
            } else {
                gameManager.drawNukeHitOnBoard(targetPos);
                gameManager.getGame().processGameEvent(
                    new GamePlayerStrategicActionEvent(gameManager,
                        new NukeDetonatedAction(ae.getId(), ae.getOwnerId(), AmmoType.Munitions.M_DAVY_CROCKETT_M)));
                gameManager.doNuclearExplosion(finalPos, 1, vPhaseReport);
            }
            return false;
        }
        if (atype.getMunitionType().contains(Munitions.M_FASCAM)) {
            int rackSize = atype.getRackSize();
            if (atype.getAmmoType() == AmmoType.T_ARROW_IV) {
                rackSize = atype.isClan() ? 30 : 20;
            }
            gameManager.deliverFASCAMMinefield(finalPos, ae.getOwner().getId(), rackSize, ae.getId());
            return false;
        }
        if (atype.getMunitionType().contains(Munitions.M_INFERNO_IV)) {
            handleArtilleryDriftMarker(targetPos, finalPos, artilleryAttackAction,
                    gameManager.deliverArtilleryInferno(finalPos, ae, subjectId, vPhaseReport));
            return false;
        }
        if (atype.getMunitionType().contains(Munitions.M_VIBRABOMB_IV)) {
            gameManager.deliverThunderVibraMinefield(finalPos, ae.getOwner().getId(), 30,
                    waa.getOtherAttackInfo(), ae.getId());
            return false;
        }
        if (atype.getMunitionType().contains(Munitions.M_SMOKE)) {
            gameManager.deliverArtillerySmoke(finalPos, vPhaseReport);
            return false;
        }
        if (atype.getMunitionType().contains(Munitions.M_LASER_INHIB)) {
            gameManager.deliverLIsmoke(finalPos, vPhaseReport);
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

            AreaEffectHelper.clearMineFields(finalPos, Minefield.CLEAR_NUMBER_WEAPON, ae, vPhaseReport, game,
                    gameManager);
        }

        Targetable updatedTarget = artilleryAttackAction.getTarget(game);
        // Calculate blast damage and falloff
        DamageFalloff damageFalloff = AreaEffectHelper.calculateDamageFallOff(atype, shootingBA, mineClear);

        // the attack's target may have been destroyed or fled since the attack was generated
        // so we need to carry out offboard/null checks against the "current" version of the target.
        // Note: currently this only damages the target and does not deal blast damage to "nearby" off-board units.
        // TODO: Sleet01: rename atype, ae, etc. to comport with current coding standards
        if ((updatedTarget != null) && updatedTarget.isOffBoard()) {
            // Calculate blast damage shape
            HashMap<Map.Entry<Integer, Coords>, Integer> blastShape = AreaEffectHelper.shapeBlast(
                atype, finalPos, damageFalloff, 0, true, false, false, game, false
            );

            Map.Entry<Integer, Coords> entry = Map.entry(updatedTarget.getElevation(), updatedTarget.getPosition());
            if (blastShape.containsKey(entry)) {
                AreaEffectHelper.artilleryDamageEntity(
                    (Entity) updatedTarget, blastShape.get(entry), null,
                    0, false, asfFlak, isFlak, entry.getKey(),
                    finalPos, atype, entry.getValue(), false, ae, null, updatedTarget.getId(),
                    vPhaseReport, gameManager
                );
            }
        } else {
            handleArtilleryDriftMarker(targetPos, finalPos, artilleryAttackAction,
                    gameManager.artilleryDamageArea(finalPos, artilleryAttackAction.getCoords(), atype,
                            subjectId, ae, damageFalloff, isFlak, height, vPhaseReport,
                            asfFlak));
        }

        // artillery may unintentionally clear minefields, but only if it wasn't trying
        // to
        if (!mineClear) {
            AreaEffectHelper.clearMineFields(finalPos, Minefield.CLEAR_NUMBER_WEAPON_ACCIDENT, ae, vPhaseReport, game,
                    gameManager);
        }

        return false;
    }

    /**
     * Worker function that handles "artillery round landed here" reports,
     * and direct artillery scatter.
     *
     * @return Whether or not we should continue attack resolution afterwards
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
            if (ae.hasAbility("oblique_artillery")) {
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
     * Worker function that contains logic for "has my shot been observed so that I
     * can be targeted by counter-battery fire"
     *
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
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.rackSize;
        if (wtype instanceof CapitalMissileWeapon) {
            toReturn = wtype.getRoundShortAV();
        }
        // BA Tube artillery is the only artillery that can be mounted by BA
        // so we do the multiplication here
        if (ae instanceof BattleArmor ba) {
            toReturn *= ba.getNumberActiverTroopers();
        }
        // area effect damage is double
        if (target.isConventionalInfantry()) {
            toReturn /= 0.5;
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        return (int) Math.ceil(toReturn);
    }
}
