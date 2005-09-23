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
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Building;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.InfernoTracker;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 *
 */
public class SRMInfernoHandler extends SRMHandler {

    /**
     * @param t
     * @param w
     * @param g
     * @param s
     */
    public SRMInfernoHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
        nSalvoBonus = 0;
        sSalvoType = " inferno missile(s) ";
        bSalvo = false;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#reportMiss(java.util.Vector)
     */
    protected void reportMiss(Vector vPhaseReport) {
        super.reportMiss(vPhaseReport);
        server.tryIgniteHex(target.getPosition(), ae.getId(), true, 11);
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#calcDamagePerHit()
     */
    protected int calcDamagePerHit() {
        return 0;
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleAccidentalBuildingDamage(java.util.Vector, megamek.common.Building, int, int)
     */
    protected void handleAccidentalBuildingDamage(Vector vPhaseReport,
            Building bldg, int hits, int nDamPerHit) {
        // Is the building hit by Inferno rounds?
        if ( hits > 0 ) {
            // start a fire in the targets hex
            Coords c = target.getPosition();
            IHex h = game.getBoard().getHex(c);
            // Is there a fire in the hex already?
            if ( h.containsTerrain( Terrains.FIRE ) ) {
                r = new Report(3285);
                r.indent(2);
                r.subject = subjectId;
                r.add(hits);
                r.add(c.getBoardNum());
                vPhaseReport.addElement(r);
            } else {
                r = new Report(3290);
                r.indent(2);
                r.subject = subjectId;
                r.add(hits);
                r.add(c.getBoardNum());
                vPhaseReport.addElement(r);
                h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            }
            game.getBoard().addInfernoTo
                ( c, InfernoTracker.STANDARD_ROUND, hits );
            server.sendChangedHex(c);
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see megamek.common.weapons.WeaponHandler#handleIgnitionDamage(java.util.Vector,
     *          megamek.common.Building, boolean, int)
     */
    protected void handleIgnitionDamage(Vector vPhaseReport,
            Building bldg, boolean bSalvo, int hits) {
        // targeting a hex for ignition
        if (target.getTargetType() == Targetable.TYPE_HEX_IGNITE ||
            target.getTargetType() == Targetable.TYPE_BLDG_IGNITE ) {

            // Unless there a fire in the hex already, start one.
            Coords c = target.getPosition();
            IHex h = game.getBoard().getHex(c);
            if ( !h.containsTerrain( Terrains.FIRE ) ) {
                Report.addNewline(vPhaseReport);
                r = new Report(3005);
                r.subject = subjectId;
                r.add(c.getBoardNum());
                vPhaseReport.addElement(r);
                h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            }
            game.getBoard().addInfernoTo
                ( c, InfernoTracker.STANDARD_ROUND, hits );
            server.sendChangedHex(c);
        }
    }
    
    protected void handleEntityDamage(Entity entityTarget,
            Vector vPhaseReport, Building bldg, int hits, int nCluster,
            int nDamPerHit, int bldgAbsorbs) {
        // Targeting an entity
        if (entityTarget != null ) {

            if (game.getOptions().booleanOption("vehicle_fires")
                    && entityTarget instanceof Tank) {
                server.checkForVehicleFire((Tank)entityTarget, true);
            } else {
                entityTarget.infernos.add( InfernoTracker.STANDARD_ROUND,
                        hits );
                Report.addNewline(vPhaseReport);
                r = new Report(3205);
                r.indent(2);
                r.subject = subjectId;
                r.addDesc(entityTarget);
                r.add(entityTarget.infernos.getTurnsLeftToBurn());
                vPhaseReport.addElement(r);
            }

            // Start a fire in the targets hex, unless already on fire.
            Coords c = target.getPosition();
            IHex h = game.getBoard().getHex(c);

            // Unless there a fire in the hex already, start one.
            if ( !h.containsTerrain( Terrains.FIRE ) ) {
                r = new Report(3005);
                r.subject = subjectId;
                r.add(c.getBoardNum());
                vPhaseReport.addElement(r);
                h.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.FIRE, 1));
            }
            game.getBoard().addInfernoTo
                ( c, InfernoTracker.STANDARD_ROUND, 1 );
            server.sendChangedHex(c);
        }
    }
    
    public boolean handle(int phase, Vector vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
        Entity entityTarget = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target
                : null;
        final boolean targetInBuilding = Compute.isInBuilding(game,
                entityTarget);

        // Which building takes the damage?
        Building bldg = game.getBoard().getBuildingAt(target.getPosition());

        // Report weapon attack and its to-hit value.
        r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        if (entityTarget != null) {
            r.addDesc(entityTarget);
        } else {
            r.messageId = 3120;
            r.add(target.getDisplayName(), true);
        }
        vPhaseReport.addElement(r);
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        }
        else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }
        else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }
        else {
            //roll to hit
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

        // Any necessary PSRs, jam checks, etc.
        // If this boolean is true, don't report
        // the miss later, as we already reported
        // it in doChecks
        boolean missReported = doChecks(vPhaseReport);

        // do we hit?
        bMissed = roll < toHit.getValue();
        
        // are we a glancing hit?
        if (game.getOptions().booleanOption("maxtech_glancing_blows")) {
            if (roll == toHit.getValue()) {
                bGlancing = true;
                r = new  Report(3186);
                r.subject = ae.getId();
                r.newlines = 0;
                vPhaseReport.addElement(r);
            } else {
                bGlancing = false;
            }
        } else {
            bGlancing = false;
        }
 

        // Do this stuff first, because some weapon's miss report reference the
        // amount of shots fired and stuff.
        nDamPerHit = calcDamagePerHit();
        boolean bAllShotsHit = allShotsHit();
        useAmmo();
        addHeat();
        
        if (bMissed && !missReported) {
            reportMiss(vPhaseReport);

            // Works out fire setting, AMS shots, and whether continuation is
            // necessary.
            if (!handleSpecialMiss(entityTarget, targetInBuilding, bldg, vPhaseReport)) {
                return false;
            }

        }

        // yeech. handle damage. . different weapons do this in very different
        // ways
        int hits = calcHits(vPhaseReport), nCluster = calcnCluster();
        Report.addNewline(vPhaseReport);
        


        // We've calculated how many hits. At this point, any missed
        // shots damage the building instead of the target.
        if (bMissed) {
            if (targetInBuilding && bldg != null) {

                handleAccidentalBuildingDamage(vPhaseReport, bldg, hits,
                        nDamPerHit);
            } // End missed-target-in-building
            return false;

        } // End missed-target

        // The building shields all units from a certain amount of damage.
        // The amount is based upon the building's CF at the phase's start.
        int bldgAbsorbs = 0;
        if (targetInBuilding && bldg != null) {
            bldgAbsorbs = (int) Math.ceil(bldg.getPhaseCF() / 10.0);
        }

        // Make sure the player knows when his attack causes no damage.
        if ( hits == 0 ) {
            r = new Report(3365);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        // light inferno missiles all at once
        if (hits > 0) {
            int nDamage;
            // targeting a hex for igniting
            if (target.getTargetType() == Targetable.TYPE_HEX_IGNITE
                    || target.getTargetType() == Targetable.TYPE_BLDG_IGNITE) {
                handleIgnitionDamage(vPhaseReport, bldg, bSalvo, hits);
                return false;
            }
            // Targeting a building.
            if (target.getTargetType() == Targetable.TYPE_BUILDING) {
                // The building takes the full brunt of the attack.
                nDamage = nDamPerHit * hits;
                handleBuildingDamage(vPhaseReport, bldg, nDamage, bSalvo);
                // And we're done!
                return false;
            }
            if (entityTarget != null) {
                handleEntityDamage(entityTarget, vPhaseReport, bldg, hits,
                        nCluster, nDamPerHit, bldgAbsorbs);
                return false;
            }
        }
        return false;
    }

}
