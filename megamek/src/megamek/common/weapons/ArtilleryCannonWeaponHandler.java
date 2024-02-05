/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
import megamek.server.GameManager;
import org.apache.logging.log4j.LogManager;

import java.util.Vector;

/**
 * @author Numien, based work by Sebastian Brocks
 */
public class ArtilleryCannonWeaponHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 1L;
    boolean handledAmmoAndReport = false;

    /**
     * This constructor can only be used for deserialization.
     */
    protected ArtilleryCannonWeaponHandler() {
        super();
    }

    public ArtilleryCannonWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!cares(phase)) {
            return true;
        }
        if (ae == null) {
            LogManager.getLogger().error("Artillery Entity is null!");
            return true;
        }

        Coords targetPos = target.getPosition();
        boolean targetIsEntity = target.getTargetType() == Targetable.TYPE_ENTITY;
        boolean isFlak = targetIsEntity && Compute.isFlakAttack(ae, (Entity) target);
        boolean asfFlak = isFlak && target.isAirborne();
        Mounted ammoUsed = ae.getEquipment(waa.getAmmoId());
        final AmmoType ammoType = (ammoUsed == null) ? null : (AmmoType) ammoUsed.getType();

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
        if (!bMissed) {
            if (!isFlak) {
                r = new Report(3190);
            } else {
                r = new Report(3191);
            }
            r.subject = subjectId;
            r.add(targetPos.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            targetPos = Compute.scatter(targetPos, (Math.abs(toHit.getMoS()) + 1) / 2);
            if (game.getBoard().contains(targetPos)) {
                // misses and scatters to another hex
                if (!isFlak) {
                    r = new Report(3195);
                } else {
                    r = new Report(3192);
                }
                r.subject = subjectId;
                r.add(targetPos.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                if (isFlak) {
                    r = new Report(3193);
                } else {
                    r = new Report(3200);
                }
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return !bMissed;
            }
        }

        // According to TacOps eratta, artillery cannons can only fire standard
        // rounds and fuel-air cannon shells (Interstellar Ops p165).
        // But, they're still in as unofficial tech, because they're fun. :)
        if(null != ammoType) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FLARE)) {
                int radius;
                if (ammoType.getAmmoType() == AmmoType.T_LONG_TOM) {
                    radius = 3;
                } else if (ammoType.getAmmoType() == AmmoType.T_SNIPER) {
                    radius = 2;
                } else {
                    radius = 1;
                }
                gameManager.deliverArtilleryFlare(targetPos, radius);
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DAVY_CROCKETT_M)) {
                // The appropriate term here is "Bwahahahahaha..."
                gameManager.doNuclearExplosion(targetPos, 1, vPhaseReport);
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FASCAM)) {
                gameManager.deliverFASCAMMinefield(targetPos, ae.getOwner().getId(),
                        ammoType.getRackSize(), ae.getId());
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_SMOKE)) {
                gameManager.deliverArtillerySmoke(targetPos, vPhaseReport);
                return false;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_FAE)) {
                AreaEffectHelper.processFuelAirDamage(targetPos,
                        ammoType, ae, vPhaseReport, gameManager);

                return false;
            }
        }

        int altitude = 0;
        if (isFlak) {
            altitude = target.getElevation();
        }

        // check to see if this is a mine clearing attack
        // According to the RAW you have to hit the right hex to hit even if the
        // scatter hex has minefields
        // TODO: Does this apply to arty cannons?
        boolean mineClear = target.getTargetType() == Targetable.TYPE_MINEFIELD_CLEAR;
        if (mineClear && !isFlak && !bMissed) {
            r = new Report(3255);
            r.indent(1);
            r.subject = subjectId;
            vPhaseReport.addElement(r);

            AreaEffectHelper.clearMineFields(targetPos, Minefield.CLEAR_NUMBER_WEAPON, ae, vPhaseReport, game, gameManager);
        }

        gameManager.artilleryDamageArea(targetPos, ae.getPosition(), ammoType,
            subjectId, ae, isFlak, altitude, mineClear, vPhaseReport,
            asfFlak, -1);

        // artillery may unintentionally clear minefields, but only if it wasn't trying to
        // TODO : Does this apply to arty cannons?
        if (!mineClear) {
            AreaEffectHelper.clearMineFields(targetPos, Minefield.CLEAR_NUMBER_WEAPON_ACCIDENT, ae, vPhaseReport, game, gameManager);
        }

        return false;
    }

    @Override
    protected int calcDamagePerHit() {
        double toReturn = wtype.getDamage();
        // area effect damage is double
        if (target.isConventionalInfantry()) {
            toReturn /= 0.5;
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        return (int) Math.ceil(toReturn);
    }
}
