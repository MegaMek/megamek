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
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AimingMode;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;

import java.util.Vector;

public class PlasmaCannonHandler extends AmmoWeaponHandler {
    private static final long serialVersionUID = 2304364403526293671L;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public PlasmaCannonHandler(ToHitData toHit, WeaponAttackAction waa, Game g, GameManager m) {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
    }

    /**
     * Largely the same as the method in <code>WeaponHandler</code>, however we
     * need to adjust the <code>target</code> state variable so damage is
     * applied properly.
     * 
     * @param entityTarget  The target Entity
     * @param vPhaseReport
     * @param hit
     * @param bldg
     * @param hits
     * @param nCluster
     * @param bldgAbsorbs
     */
    @Override
    protected void handlePartialCoverHit(Entity entityTarget, Vector<Report> vPhaseReport,
                                         HitData hit, Building bldg, int hits, int nCluster,
                                         int bldgAbsorbs) {
        // Report the hit and table description, if this isn't part of a salvo
        Report r;
        if (!bSalvo) {
            r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
        } else {
            // Keep spacing consistent
            Report.addNewline(vPhaseReport);
        }

        r = new Report(3460);
        r.subject = subjectId;
        r.add(entityTarget.getShortName());
        r.add(entityTarget.getLocationAbbr(hit));
        r.indent(2);
        vPhaseReport.addElement(r);

        int damageableCoverType = LosEffects.DAMAGABLE_COVER_NONE;
        Building coverBuilding = null;
        Entity coverDropShip = null;
        Coords coverLoc = null;

        // Determine if there is primary and secondary cover,
        // and then determine which one gets hit
        if ((toHit.getCover() == LosEffects.COVER_75RIGHT || toHit.getCover() == LosEffects.COVER_75LEFT)
                ||
                // 75% cover has a primary and secondary
                (toHit.getCover() == LosEffects.COVER_HORIZONTAL && toHit
                        .getDamagableCoverTypeSecondary() != LosEffects.DAMAGABLE_COVER_NONE)) {
            // Horizontal cover provided by two 25%'s, so primary and secondary
            int hitLoc = hit.getLocation();
            // Primary stores the left side, from the perspective of the
            // attacker
            if (hitLoc == Mech.LOC_RLEG || hitLoc == Mech.LOC_RT
                    || hitLoc == Mech.LOC_RARM) {
                // Left side is primary
                damageableCoverType = toHit.getDamagableCoverTypePrimary();
                coverBuilding = toHit.getCoverBuildingPrimary();
                coverDropShip = toHit.getCoverDropshipPrimary();
                coverLoc = toHit.getCoverLocPrimary();
            } else {
                // If not left side, then right side, which is secondary
                damageableCoverType = toHit.getDamagableCoverTypeSecondary();
                coverBuilding = toHit.getCoverBuildingSecondary();
                coverDropShip = toHit.getCoverDropshipSecondary();
                coverLoc = toHit.getCoverLocSecondary();
            }
        } else { // Only primary cover exists
            damageableCoverType = toHit.getDamagableCoverTypePrimary();
            coverBuilding = toHit.getCoverBuildingPrimary();
            coverDropShip = toHit.getCoverDropshipPrimary();
            coverLoc = toHit.getCoverLocPrimary();
        }
        // Check if we need to damage the cover that absorbed the hit.
        if (damageableCoverType == LosEffects.DAMAGABLE_COVER_DROPSHIP) {
            // We need to adjust some state and then restore it later
            // This allows us to make a call to handleEntityDamage
            ToHitData savedToHit = toHit;
            AimingMode savedAimingMode = waa.getAimingMode();
            waa.setAimingMode(AimingMode.NONE);

            int savedAimedLocation = waa.getAimedLocation();
            waa.setAimedLocation(Entity.LOC_NONE);
            boolean savedSalvo = bSalvo;
            bSalvo = true;
            Targetable origTarget = target;
            target = coverDropShip;
            hits = calcHits(vPhaseReport);
            // Create new toHitData
            toHit = new ToHitData(0, "", ToHitData.HIT_NORMAL,
                    Compute.targetSideTable(ae, coverDropShip));
            // Report cover was damaged
            int sizeBefore = vPhaseReport.size();
            r = new Report(3465);
            r.subject = subjectId;
            r.add(coverDropShip.getShortName());
            r.newlines++;
            vPhaseReport.add(r);
            // Damage the DropShip
            handleEntityDamage(coverDropShip, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
            // Remove a blank line in the report list
            if (vPhaseReport.elementAt(sizeBefore).newlines > 0) {
                vPhaseReport.elementAt(sizeBefore).newlines--;
            }
            // Indent reports related to the damage absorption
            while (sizeBefore < vPhaseReport.size()) {
                vPhaseReport.elementAt(sizeBefore).indent(3);
                sizeBefore++;
            }
            // Restore state
            toHit = savedToHit;
            waa.setAimingMode(savedAimingMode);
            waa.setAimedLocation(savedAimedLocation);
            bSalvo = savedSalvo;
            target = origTarget;
            // Damage a building that blocked a shot
        } else if (damageableCoverType == LosEffects.DAMAGABLE_COVER_BUILDING) {
            // Normal damage
            Targetable origTarget = target;
            target = new BuildingTarget(coverLoc, game.getBoard(), false);
            hits = calcHits(vPhaseReport);
            // Plasma Cannons do double damage per-hit to buildings
            int nDamage = 2 * hits;
            Vector<Report> buildingReport = gameManager.damageBuilding(coverBuilding, nDamage,
                    " blocks the shot and takes ", coverLoc);
            target = origTarget;
            for (Report report : buildingReport) {
                report.subject = subjectId;
                report.indent();
            }
            vPhaseReport.addAll(buildingReport);
            // Damage any infantry in the building.
            Vector<Report> infantryReport = gameManager.damageInfantryIn(coverBuilding, nDamage,
                    coverLoc, wtype.getInfantryDamageClass());
            for (Report report : infantryReport) {
                report.indent(2);
            }
            vPhaseReport.addAll(infantryReport);
        }
        missed = true;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport,
                                      Building bldg, int hits, int nCluster, int bldgAbsorbs) {

        if (entityTarget.tracksHeat()) {
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(),
                    waa.getAimingMode(), toHit.getCover());
            hit.setGeneralDamageType(generalDamageType);
            hit.setAttackerId(getAttackerId());
            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit.getCover(),
                    Compute.targetSideTable(ae, entityTarget, weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits, nCluster, bldgAbsorbs);
                return;
            }

            if (!bSalvo) {
                // Each hit in the salvo gets its own hit location.
                Report r = new Report(3405);
                r.subject = subjectId;
                r.add(toHit.getTableDesc());
                r.add(entityTarget.getLocationAbbr(hit));
                vPhaseReport.addElement(r);
            }
            Report r = new Report(3400);
            r.subject = subjectId;
            r.indent(2);
            int extraHeat = Compute.d6(2);
            if (entityTarget.getArmor(hit) > 0 &&                        
                    (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_REFLECTIVE)) {
               entityTarget.heatFromExternal += Math.max(1, extraHeat / 2);
               r.add(Math.max(1, extraHeat / 2));
               r.choose(true);
               r.messageId = 3406;
               r.add(extraHeat);
               r.add(EquipmentType.armorNames
                       [entityTarget.getArmorType(hit.getLocation())]);
            } else if (entityTarget.getArmor(hit) > 0 &&  
                   (entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_HEAT_DISSIPATING)) {
               entityTarget.heatFromExternal += extraHeat / 2;
               r.add(extraHeat / 2);
               r.choose(true);
               r.messageId = 3406;
               r.add(extraHeat);
               r.add(EquipmentType.armorNames[entityTarget.getArmorType(hit.getLocation())]);
            } else {
               entityTarget.heatFromExternal += extraHeat;
               r.add(extraHeat);
               r.choose(true);
            }
            vPhaseReport.addElement(r);            
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
        }
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.tracksHeat()) {
            return 0;
        }
        int toReturn = 1;
        if (target.isConventionalInfantry()) {
            toReturn = Compute.d6(3);
            // pain shunted infantry get half damage
            if (bDirect) {
                toReturn += toHit.getMoS() / 3;
            }
            if (((Entity) target).hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
                toReturn = Math.max(toReturn / 2, 1);
            }
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (toHit.getMoS() / 3), toReturn * 2);
        }
        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());
        return toReturn;
    }

    @Override
    protected int calcnCluster() {
        if (target.tracksHeat()) {
            bSalvo = false;
            return 1;
        }
        bSalvo = true;
        return 5;
    }

    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        // conventional infantry gets hit in one lump
        // BAs can't mount Plasma Cannons
        if (target.isConventionalInfantry() || target.tracksHeat()) {
            return 1;
        } else if ((target instanceof BattleArmor) && ((BattleArmor) target).isFireResistant()) {
            return 0;
        } else {
            return Compute.d6(3);
        }
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, Building bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = new TargetRoll(wtype.getFireTN(), wtype.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(), subjectId, true, false,
                    tn, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }

        nDamage *= 2; // Plasma weapons deal double damage to woods.

        // report that damage was "applied" to terrain
        Report r = new Report(3385);
        r.indent(2);
        r.subject = subjectId;
        r.add(nDamage);
        vPhaseReport.addElement(r);

        // Any clear attempt can result in accidental ignition, even
        // weapons that can't normally start fires. that's weird.
        // Buildings can't be accidentally ignited.
        // TODO: change this for TacOps - now you roll another 2d6 first and on
        // a 5 or less
        // you do a normal ignition as though for intentional fires
        if ((bldg != null)
                && gameManager.tryIgniteHex(target.getPosition(), subjectId, true,
                        false,
                        new TargetRoll(wtype.getFireTN(), wtype.getName()), 5,
                        vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(), nDamage, subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }

    @Override
    protected void handleBuildingDamage(Vector<Report> vPhaseReport, Building bldg, int nDamage,
                                        Coords coords) {
        // Plasma weapons deal double damage to buildings.
        super.handleBuildingDamage(vPhaseReport, bldg, nDamage * 2, coords);
    }
}
