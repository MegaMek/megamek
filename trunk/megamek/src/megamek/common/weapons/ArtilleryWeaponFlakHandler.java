/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
/*
 * Created on Sep 24, 2004
 *
 */
package megamek.common.weapons;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.HitData;
import megamek.common.IEntityMovementMode;
import megamek.common.IGame;
import megamek.common.INarcPod;
import megamek.common.LosEffects;
import megamek.common.Mounted;
import megamek.common.Report;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;
import megamek.server.Server.DamageType;

/**
 * @author Sebastian Brocks
 */
public class ArtilleryWeaponFlakHandler extends
        ArtilleryWeaponDirectFireHandler implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7795254956703302239L;
    boolean handledHeatAndReport = false;

    /**
     * @param t
     * @param w
     * @param g
     */
    public ArtilleryWeaponFlakHandler(ToHitData t, WeaponAttackAction w,
            IGame g, Server s) {
        super(t, w, g, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#cares(int)
     */
    public boolean cares(int phase) {
        if (phase == IGame.PHASE_OFFBOARD || phase == IGame.PHASE_TARGETING) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.AttackHandler#handle(int, java.util.Vector)
     */
    public boolean handle(int phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
        if (phase == IGame.PHASE_TARGETING) {
            ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
            if (!handledHeatAndReport) {
                addHeat();
                // Report the firing itself
                r = new Report(3121);
                r.indent();
                r.newlines = 0;
                r.subject = subjectId;
                r.add(wtype.getName());
                r.add(aaa.turnsTilHit);
                vPhaseReport.addElement(r);
                Report.addNewline(vPhaseReport);
                handledHeatAndReport = true;
            }
            // if this is the last targeting phase before we hit,
            // make it so the firing entity is announced in the
            // off-board attack phase that follows.
            if (aaa.turnsTilHit == 0) {
                announcedEntityFiring = false;
            }
            return true;
        }
        ArtilleryAttackAction aaa = (ArtilleryAttackAction) waa;
        if (aaa.turnsTilHit > 0) {
            aaa.turnsTilHit--;
            return true;
        }
        final Vector spottersBefore = aaa.getSpotterIds();
        final Targetable target = aaa.getTarget(game);
        final Coords targetPos = target.getPosition();
        final int playerId = aaa.getPlayerId();
        Entity bestSpotter = null;
        Entity ae = game.getEntity(aaa.getEntityId());
        if (ae == null) {
            ae = game.getOutOfGameEntity(aaa.getEntityId());
        }
        Mounted ammo = ae.getEquipment(aaa.getAmmoId());
        final AmmoType atype = ammo == null ? null : (AmmoType) ammo.getType();
        // Are there any valid spotters?
        if (null != spottersBefore) {
            // fetch possible spotters now
            Enumeration spottersAfter = game
                    .getSelectedEntities(new EntitySelector() {
                        public int player = playerId;

                        public Targetable targ = target;

                        public boolean accept(Entity entity) {
                            Integer id = new Integer(entity.getId());
                            if (player == entity.getOwnerId()
                                    && spottersBefore.contains(id)
                                    && !(LosEffects.calculateLos(game, entity
                                            .getId(), targ)).isBlocked()
                                    && entity.isActive()
                                    && !entity.isINarcedWith(INarcPod.HAYWIRE)) {
                                return true;
                            }
                            return false;
                        }
                    });

            // Out of any valid spotters, pick the best.
            while (spottersAfter.hasMoreElements()) {
                Entity ent = (Entity) spottersAfter.nextElement();
                if (bestSpotter == null
                        || ent.crew.getGunnery() < bestSpotter.crew
                                .getGunnery()) {
                    bestSpotter = ent;
                }
            }

        } // End have-valid-spotters

        // If at least one valid spotter, then get the benefits thereof.
        if (null != bestSpotter) {
            int mod = (bestSpotter.crew.getGunnery() - 4) / 2;
            toHit.addModifier(mod, "Spotting modifier");
        }

        // Is the attacker still alive?
        Entity artyAttacker = aaa.getEntity(game);
        if (null != artyAttacker) {

            // Get the arty weapon.
            Mounted weapon = artyAttacker.getEquipment(aaa.getWeaponId());

            // If the shot hit the target hex, then all subsequent
            // fire will hit the hex automatically.
            if (roll >= toHit.getValue()) {
                artyAttacker.aTracker.setModifier(weapon,
                        ToHitData.AUTOMATIC_SUCCESS, targetPos);
            }
            // If the shot missed, but was adjusted by a
            // spotter, future shots are more likely to hit.
            else if (null != bestSpotter) {
                artyAttacker.aTracker.setModifier(weapon, artyAttacker.aTracker
                        .getModifier(weapon, targetPos) - 1, targetPos);
            }

        } // End artyAttacker-alive

        // Report weapon attack and its to-hit value.
        r = new Report(3120);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        r.add(target.getDisplayName(), true);
        vPhaseReport.addElement(r);
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
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
            r.add(toHit.getValue());
            vPhaseReport.addElement(r);
        }

        // dice have been rolled, thanks
        r = new Report(3155);
        r.newlines = 0;
        r.subject = subjectId;
        r.add(roll);
        vPhaseReport.addElement(r);

        // do we hit?
        bMissed = roll < toHit.getValue();

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        if (!handledHeatAndReport) {
            addHeat();
        }
        Coords coords = target.getPosition();
        if (!bMissed) {
            r = new Report(3190);
            r.subject = subjectId;
            r.add(coords.getBoardNum());
            vPhaseReport.addElement(r);
        } else {
            coords = Compute.scatter(coords, (game.getOptions()
                    .booleanOption("margin_scatter_distance")) ? (toHit
                    .getValue() - roll) : -1);
            if (game.getBoard().contains(coords)) {
                // misses and scatters to another hex
                r = new Report(3195);
                r.subject = subjectId;
                r.add(coords.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                // misses and scatters off-board
                r = new Report(3200);
                r.subject = subjectId;
                vPhaseReport.addElement(r);
                return !bMissed;
            }
        }

        if (atype.getMunitionType() == AmmoType.M_FLARE) {
            int radius;
            if (atype.getAmmoType() == AmmoType.T_ARROW_IV)
                radius = 4;
            else if (atype.getAmmoType() == AmmoType.T_LONG_TOM)
                radius = 3;
            else
                radius = Math.max(1, atype.getRackSize() / 5);
            server.deliverArtilleryFlare(coords, radius);
            return false;
        }

        int nCluster = 5;
        int hits;

        int ratedDamage = wtype.getDamage();
        Building bldg = null;
        bldg = game.getBoard().getBuildingAt(coords);
        int bldgAbsorbs = (bldg != null) ? bldg.getPhaseCF() / 10 : 0;
        bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
        ratedDamage -= bldgAbsorbs;
        if ((bldg != null) && (bldgAbsorbs > 0)) {
            // building absorbs some of the damage
            r = new Report(6425);
            r.subject = subjectId;
            r.add(bldgAbsorbs);
            vPhaseReport.addElement(r);
            Vector<Report> buildingReport = server.damageBuilding(bldg,
                    ratedDamage);
            for (Report report : buildingReport) {
                report.subject = subjectId;
            }
            vPhaseReport.addAll(buildingReport);
        }

        for (Enumeration impactHexHits = game.getEntities(coords); impactHexHits
                .hasMoreElements();) {
            Entity entity = (Entity) impactHexHits.nextElement();
            hits = ratedDamage;

            while (hits > 0) {
                toHit.setSideTable(entity.sideTable(aaa.getCoords()));
                if (entity.getMovementMode() == IEntityMovementMode.VTOL) {
                    // VTOLs take no damage from normal artillery unless landed
                    if (entity.getElevation() > 0) {
                        break;
                    }
                }
                HitData hit = entity.rollHitLocation(toHit.getHitTable(), toHit
                        .getSideTable(), waa.getAimedLocation(), waa
                        .getAimingMode());

                vPhaseReport.addAll(server.damageEntity(entity, hit, Math.min(
                        nCluster, hits), false, DamageType.NONE, false, true,
                        throughFront));
                hits -= Math.min(nCluster, hits);
            }
        }
        for (int dir = 0; dir <= 5; dir++) {
            Coords tempcoords = coords.translated(dir);
            if (!game.getBoard().contains(tempcoords)) {
                continue;
            }
            if (coords.equals(tempcoords)) {
                continue;
            }

            ratedDamage = wtype.getRackSize() / 2;
            bldg = null;
            bldg = game.getBoard().getBuildingAt(tempcoords);
            bldgAbsorbs = (bldg != null) ? bldg.getPhaseCF() / 10 : 0;
            bldgAbsorbs = Math.min(bldgAbsorbs, ratedDamage);
            ratedDamage -= bldgAbsorbs;
            if ((bldg != null) && (bldgAbsorbs > 0)) {
                // building absorbs some of the damage
                r = new Report(6425);
                r.subject = subjectId;
                r.add(bldgAbsorbs);
                vPhaseReport.addElement(r);
                Vector<Report> buildingReport = server.damageBuilding(bldg,
                        ratedDamage);
                for (Report report : buildingReport) {
                    report.subject = subjectId;
                }
                vPhaseReport.addAll(buildingReport);
            }

            Enumeration splashHexHits = game.getEntities(tempcoords);
            if (splashHexHits.hasMoreElements()) {
                r = new Report(3210);
                r.newlines = 0;
                r.subject = subjectId;
                r.add(tempcoords.getBoardNum());
                r.indent();
                vPhaseReport.addElement(r);
            }
            for (; splashHexHits.hasMoreElements();) {
                Entity entity = (Entity) splashHexHits.nextElement();
                hits = ratedDamage;
                while (hits > 0) {
                    HitData hit = entity.rollHitLocation(toHit.getHitTable(),
                            toHit.getSideTable(), waa.getAimedLocation(), waa
                                    .getAimingMode());
                    vPhaseReport.addAll(server.damageEntity(entity, hit, Math
                            .min(nCluster, hits)));
                    hits -= Math.min(nCluster, hits);
                }
            }
        }
        return false;
    }
}
