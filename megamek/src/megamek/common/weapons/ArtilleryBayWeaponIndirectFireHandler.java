/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * @author Sebastian Brocks
 */
public class ArtilleryBayWeaponIndirectFireHandler extends AmmoBayWeaponHandler {
    private static final long serialVersionUID = -1277649123562229298L;
    boolean handledAmmoAndReport = false;

    /**
     * This constructor can only be used for deserialization.
     */
    protected ArtilleryBayWeaponIndirectFireHandler() {
        super();
    }

    public ArtilleryBayWeaponIndirectFireHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    public boolean cares(final GamePhase phase) {
        return phase.isOffboard() || phase.isTargeting();
    }

    @Override
    protected void useAmmo() {
        nweaponsHit = weapon.getBayWeapons().size();
        for (WeaponMounted bayW : weapon.getBayWeapons()) {
            // check the currently loaded ammo
            AmmoMounted bayWAmmo = bayW.getLinkedAmmo();

            if (bayWAmmo == null) {// Can't happen. w/o legal ammo, the weapon
                // *shouldn't* fire.
                LogManager.getLogger().debug("Handler can't find any ammo! Oh no!");
                return;
            }

            int shots = bayW.getCurrentShots();
            //if this option is on, we may have odd amounts of ammo in multiple bins. Only fire rounds that we have.
            if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS)) {
                if (bayWAmmo.getUsableShotsLeft() < 1) {
                    nweaponsHit--;
                } else {
                    bayWAmmo.setShotsLeft(bayWAmmo.getBaseShotsLeft() - 1);
                }
            } else {
                //By default rules, we have just one ammo bin with at least 10 shots for each weapon in the bay,
                //so we'll track ammo normally and need to resolve attacks for all bay weapons.
                for (int i = 0; i < shots; i++) {
                    if (null == bayWAmmo
                            || bayWAmmo.getUsableShotsLeft() < 1) {
                        // try loading something else
                        ae.loadWeaponWithSameAmmo(bayW);
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
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        String artyMsg;
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
        if (phase.isTargeting()) {
            if (!handledAmmoAndReport) {
                addHeat();
                // Report the firing itself
                Report r = new Report(3121);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(wtype.getName());
                r.add(aaa.getTurnsTilHit());
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledAmmoAndReport = true;

                artyMsg = "Artillery bay fire Incoming, landing on round "
                        + (game.getRoundCount() + aaa.getTurnsTilHit())
                        + ", fired by "
                        + game.getPlayer(aaa.getPlayerId()).getName();
                game.getBoard().addSpecialHexDisplay(
                        aaa.getTarget(game).getPosition(),
                        new SpecialHexDisplay(
                                SpecialHexDisplay.Type.ARTILLERY_INCOMING, game
                                        .getRoundCount() + aaa.getTurnsTilHit(),
                                game.getPlayer(aaa.getPlayerId()), artyMsg,
                                SpecialHexDisplay.SHD_OBSCURED_TEAM));
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
        // Offboard shots are targeted at an entity rather than a hex. If null, the target has disengaged.
        if (target == null) {
            Report r = new Report(3158);
            r.indent();
            r.subject = subjectId;
            r.add(wtype.getName());
            vPhaseReport.add(r);
            return true;
        }
        final Vector<Integer> spottersBefore = aaa.getSpotterIds();
        Coords targetPos = target.getPosition();
        final int playerId = aaa.getPlayerId();
        boolean targetIsEntity = target.getTargetType() == Targetable.TYPE_ENTITY;
        boolean isFlak = targetIsEntity && Compute.isFlakAttack(ae, (Entity) target);
        boolean asfFlak = isFlak && target.isAirborne();
        boolean mineClear = target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR;
        Entity bestSpotter = null;
        if (ae == null) {
            LogManager.getLogger().error("Artillery Entity is null!");
            return true;
        }

        Mounted ammoUsed = ae.getEquipment(aaa.getAmmoId());
        final AmmoType atype = (AmmoType) ammoUsed.getType();

        // Are there any valid spotters?
        if ((null != spottersBefore) && !isFlak) {
            // fetch possible spotters now
            Iterator<Entity> spottersAfter = game.getSelectedEntities(new EntitySelector() {
                        public int player = playerId;

                        public Targetable targ = target;

                        @Override
                        public boolean accept(Entity entity) {
                            Integer id = entity.getId();
                            if ((player == entity.getOwnerId())
                                    && spottersBefore.contains(id)
                                    && !LosEffects.calculateLOS(game, entity, targ, true).isBlocked()
                                    && entity.isActive()
                                    // airborne aeros can't spot for arty
                                    && !(entity.isAero() && entity.isAirborne())
                                    && !entity.isINarcedWith(INarcPod.HAYWIRE)) {
                                return true;
                            }
                            return false;
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
                foMod = -1;
            }
            int mod = (bestSpotter.getCrew().getGunnery() - 4) / 2;
            mod += foMod;
            toHit.addModifier(mod, "Spotting modifier");
        }

        // Is the attacker still alive and we're not shooting FLAK?
        // then adjust the target
        if (!isFlak) {
            // If the shot hit the target hex, then all subsequent
            // fire will hit the hex automatically.
            if (roll.getIntValue() >= toHit.getValue()) {
                ae.aTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, targetPos);
            } else if (null != bestSpotter) {
                // If the shot missed, but was adjusted by a spotter, future shots are more likely
                // to hit.
                // Note: Because artillery fire is adjusted on a per-unit basis, this can result in
                // a unit firing multiple artillery weapons at the same hex getting this bonus more
                // than once per turn. Since the Artillery Modifiers Table on TacOps p. 180 lists a
                // -1 per shot (not salvo!) previously fired at the target hex, this would in fact
                // appear to be correct.

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
            r.add(wtype.getName());
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

        // In the case of misses, we'll need to hit multiple hexes
        List<Coords> targets = new ArrayList<>();
        if (!bMissed) {
            r = new Report(3199);
            r.subject = subjectId;
            r.add(nweaponsHit);
            r.add(targetPos.getBoardNum());
            r.add(atype.getShortName());
            // Mine clearance has its own report which will get added
            if (!mineClear) {
                vPhaseReport.addElement(r);
            }
            artyMsg = "Artillery hit here on round " + game.getRoundCount()
                    + ", fired by " + game.getPlayer(aaa.getPlayerId()).getName()
                    + " (this hex is now an auto-hit)";
            game.getBoard().addSpecialHexDisplay(targetPos,
                    new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_HIT,
                            game.getRoundCount(), game.getPlayer(aaa.getPlayerId()), artyMsg));
        } else {
            Coords origPos = targetPos;
            int moF = toHit.getMoS();
            if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY)) {
                // getMoS returns a negative MoF
                // simple math is better so lets make it positive
                if ((-moF - 2) < 1) {
                    moF = 0;
                } else {
                    moF = moF + 2;
                }
            }
            // We're only going to display one missed shot hex on the board, at the intended target
            artyMsg = "Artillery missed here on round "
                    + game.getRoundCount() + ", fired by "
                    + game.getPlayer(aaa.getPlayerId()).getName();
            game.getBoard().addSpecialHexDisplay(origPos,
                    new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_HIT, game.getRoundCount(),
                            game.getPlayer(aaa.getPlayerId()), artyMsg));
            while (nweaponsHit > 0) {
                //We'll generate a new report and scatter for each weapon fired
                targetPos = Compute.scatterDirectArty(targetPos, moF);
                if (game.getBoard().contains(targetPos)) {
                    targets.add(targetPos);
                    // misses and scatters to another hex
                    if (!isFlak) {
                        r = new Report(3202);
                        r.subject = subjectId;
                        r.newlines = 1;
                        r.add(atype.getShortName());
                        r.add(targetPos.getBoardNum());
                        vPhaseReport.addElement(r);
                    } else {
                        r = new Report(3192);
                        r.subject = subjectId;
                        r.newlines = 1;
                        r.add(targetPos.getBoardNum());
                        vPhaseReport.addElement(r);
                    }
                } else {
                    // misses and scatters off-board
                    r = new Report(3200);
                    r.subject = subjectId;
                    r.newlines = 1;
                    vPhaseReport.addElement(r);
                }
            nweaponsHit--;
            }
            // If we managed to land everything off the board, stop
            if (targets.isEmpty()) {
                return !bMissed;
            }
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_FLARE)) {
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
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_DAVY_CROCKETT_M)) {
            // The appropriate term here is "Bwahahahahaha..."
            if (!bMissed) {
                // Keep blasting the target hex with each weapon in the bay that fired
                while (nweaponsHit > 0) {
                    gameManager.doNuclearExplosion(targetPos, 1, vPhaseReport);
                    nweaponsHit--;
                }
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.doNuclearExplosion(c, 1, vPhaseReport);
                }
            }
            return false;
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_FASCAM)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverFASCAMMinefield(targetPos, ae.getOwner().getId(),
                        atype.getRackSize(), ae.getId());
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverFASCAMMinefield(c, ae.getOwner().getId(),
                            atype.getRackSize(), ae.getId());
                }
            }
            return false;
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_INFERNO_IV)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverArtilleryInferno(targetPos, ae, subjectId, vPhaseReport);
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverArtilleryInferno(c, ae, subjectId, vPhaseReport);
                }
            }
            return false;
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV)) {
            if (!bMissed) {
                // If we hit, only one effect will stack in the target hex
                gameManager.deliverThunderVibraMinefield(targetPos, ae.getOwner().getId(),
                        atype.getRackSize(), waa.getOtherAttackInfo(), ae.getId());
            } else {
                // Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverThunderVibraMinefield(c, ae.getOwner().getId(),
                            atype.getRackSize(), waa.getOtherAttackInfo(), ae.getId());
                }
            }
            return false;
        }
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_SMOKE)) {
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
        if (atype.getMunitionType().contains(AmmoType.Munitions.M_LASER_INHIB)) {
            if (!bMissed) {
                //If we hit, only one effect will stack in the target hex
                gameManager.deliverLIsmoke(targetPos, vPhaseReport);
            } else {
                //Deliver a round to each target hex
                for (Coords c : targets) {
                    gameManager.deliverLIsmoke(c, vPhaseReport);
                }
            }
            return false;
        }
        int altitude = 0;
        if (isFlak) {
            altitude = target.getElevation();
        }

        // check to see if this is a mine clearing attack
        // According to the RAW you have to hit the right hex to hit even if the
        // scatter hex has minefields
        if (mineClear && game.containsMinefield(targetPos) && !isFlak && !bMissed) {
            r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

            Enumeration<Minefield> minefields = game.getMinefields(targetPos).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (gameManager.clearMinefield(mf, ae, Minefield.CLEAR_NUMBER_WEAPON, vPhaseReport)) {
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
                Enumeration<Minefield> minefields = game.getMinefields(targetPos).elements();
                ArrayList<Minefield> mfRemoved = new ArrayList<>();
                while (minefields.hasMoreElements()) {
                    Minefield mf = minefields.nextElement();
                    if (gameManager.clearMinefield(mf, ae, 10, vPhaseReport)) {
                        mfRemoved.add(mf);
                    }
                }
                // we have to do it this way to avoid a concurrent error problem
                for (Minefield mf : mfRemoved) {
                    gameManager.removeMinefield(mf);
                }
            }
            // Here we're doing damage for each hit with more standard artillery shells
            while (nweaponsHit > 0) {
                gameManager.artilleryDamageArea(targetPos, aaa.getCoords(), atype,
                        subjectId, ae, isFlak, altitude, mineClear, vPhaseReport,
                        asfFlak, -1);
                nweaponsHit--;
            }
        } else {
            // Now if we missed, resolve a strike on each scatter hex
            for (Coords c : targets) {
                // Accidental mine clearance...
                if (!mineClear && game.containsMinefield(c)) {
                    Enumeration<Minefield> minefields = game.getMinefields(c).elements();
                    ArrayList<Minefield> mfRemoved = new ArrayList<>();
                    while (minefields.hasMoreElements()) {
                        Minefield mf = minefields.nextElement();
                        if (gameManager.clearMinefield(mf, ae, 10, vPhaseReport)) {
                            mfRemoved.add(mf);
                        }
                    }
                    for (Minefield mf : mfRemoved) {
                        gameManager.removeMinefield(mf);
                    }
                }
                gameManager.artilleryDamageArea(c, aaa.getCoords(), atype, subjectId, ae, isFlak,
                        altitude, mineClear, vPhaseReport, asfFlak, -1);
            }

        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // area effect damage is double
        if (target.isConventionalInfantry()) {
            toReturn /= 0.5;
        }

        toReturn = applyGlancingBlowModifier(toReturn, false);

        return (int) Math.ceil(toReturn);
    }
}
