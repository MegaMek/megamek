/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 29, 2004
 */
public class MGAWeaponHandler extends MGHandler {
    private static final long serialVersionUID = 8675420566952393440L;
    int howManyShots;
    HitData hit;

    /**
     * @param t
     * @param w
     * @param g
     */
    public MGAWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, GameManager m) {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            int damage = Compute.directBlowInfantryDamage(
                    wtype.getDamage(), bDirect ? toHit.getMoS() / 3 : 0,
                    wtype.getInfantryDamageClass(),
                    ((Infantry) target).isMechanized(),
                    toHit.getThruBldg() != null, ae.getId(), calcDmgPerHitReport, howManyShots);
            damage = applyGlancingBlowModifier(damage, true);
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)) {
                if (nRange > wtype.getRanges(weapon)[RangeType.RANGE_LONG]) {
                    damage *= 0.75;
                } else if (nRange > wtype.getRanges(weapon)[RangeType.RANGE_EXTREME]) {
                    damage *= 0.5;
                }
            }
            return damage;
        } else {
            return super.calcDamagePerHit();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeatUseAmmo()
     */
    @Override
    protected void useAmmo() {
        int shotsNeedFiring;
        setDone();
        checkAmmo();
        howManyShots = weapon.getCurrentShots();
        int total = ae.getTotalAmmoOfType(ammo.getType());
        if (total <= howManyShots) {
            howManyShots = total;
        }
        shotsNeedFiring = howManyShots;
        if (ammo.getUsableShotsLeft() == 0) {
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
            // there will be some ammo somewhere, otherwise shot will not have
            // been fired.
        }
        while (shotsNeedFiring > ammo.getUsableShotsLeft()) {
            shotsNeedFiring -= ammo.getBaseShotsLeft();
            ammo.setShotsLeft(0);
            ae.loadWeapon(weapon);
            ammo = weapon.getLinked();
        }
        ammo.setShotsLeft(ammo.getBaseShotsLeft() - shotsNeedFiring);
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#calcHits(java.util.Vector)
     */
    @Override
    protected int calcHits(Vector<Report> vPhaseReport) {
        int shotsHit;
        int nMod = getClusterModifiers(true);

        if ((howManyShots == 1) || target.isConventionalInfantry()) {
            shotsHit = 1;
        } else {
            shotsHit = allShotsHit() ? howManyShots : Compute.missilesHit(
                    howManyShots, nMod);
            Report r = new Report(3325);
            r.subject = subjectId;
            r.add(shotsHit);
            r.add(" shot(s) ");
            r.add(toHit.getTableDesc());
            r.newlines = 0;
            vPhaseReport.addElement(r);
            r = new Report(3345);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
        bSalvo = true;
        return shotsHit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see megamek.common.weapons.WeaponHandler#addHeat()
     */
    @Override
    protected void addHeat() {
        for (int x = 0; x < howManyShots; x++) {
            super.addHeat();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * megamek.common.weapons.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
            Vector<Report> vPhaseReport, Building bldg, int hits, int nCluster,
            int bldgAbsorbs) {
        int nDamage;
        if (hit == null) {
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                    toHit.getSideTable(), waa.getAimedLocation(),
                    waa.getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());
        }

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                .getCover(), Compute.targetSideTable(ae, entityTarget, weapon
                .getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                    nCluster, bldgAbsorbs);
            return;
        }

        hit.setGeneralDamageType(generalDamageType);
        if (!bSalvo) {
            // Each hit in the salvo gets its own hit location.
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);
        }

        if (hit.hitAimedLocation()) {
            Report r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        nDamage = nDamPerHit * Math.min(nCluster, hits);

        // Report calcDmgPerHitReports here
        if (!calcDmgPerHitReport.isEmpty()) {
            vPhaseReport.addAll(calcDmgPerHitReport);
            calcDmgPerHitReport.clear();
        }

        // if the target was in partial cover, then we already handled
        // damage absorption by the partial cover, if it would have happened
        Hex targetHex = game.getBoard().getHex(target.getPosition());
        boolean targetStickingOutOfBuilding = unitStickingOutOfBuilding(targetHex, entityTarget);
                
        nDamage = absorbBuildingDamage(nDamage, entityTarget, bldgAbsorbs, 
                vPhaseReport, bldg, targetStickingOutOfBuilding);

        nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);

        // some buildings scale remaining damage that is not absorbed
        // TODO: this isn't quite right for castles brian
        if ((null != bldg) && !targetStickingOutOfBuilding) {
            nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
        }

        // A building may absorb the entire shot.
        if (nDamage == 0) {
            Report r = new Report(3415);
            r.subject = subjectId;
            r.indent(2);
            r.addDesc(entityTarget);
            r.newlines = 0;
            vPhaseReport.addElement(r);
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }
            
            if (bLowProfileGlancing) {
                hit.makeGlancingBlow();
            }
            vPhaseReport
                    .addAll(gameManager.damageEntity(entityTarget, hit, nDamage,
                            false, ae.getSwarmTargetId() == entityTarget
                                    .getId() ? DamageType.IGNORE_PASSENGER
                                    : damageType, false, false, throughFront,
                            underWater));
        }
    }
}
