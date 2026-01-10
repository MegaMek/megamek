/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.handlers;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.RangeType;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sept 29, 2004
 */
public class MGAWeaponHandler extends MGHandler {
    @Serial
    private static final long serialVersionUID = 8675420566952393440L;
    int howManyShots;
    HitData hit;

    /**
     *
     */
    public MGAWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) throws EntityLoadingException {
        super(t, w, g, m);
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            calcDmgPerHitReport.add(new Report(950));
            int damage = Compute.directBlowInfantryDamage(
                  weaponType.getDamage(), bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, weaponEntity.getId(), calcDmgPerHitReport, howManyShots);
            damage = applyGlancingBlowModifier(damage, true);
            if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)) {
                if (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG]) {
                    damage = (int) Math.floor(damage * 0.75);
                } else if (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME]) {
                    damage = (int) Math.floor(damage * 0.5);
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
     * @see megamek.common.weapons.handlers.WeaponHandler#addHeatUseAmmo()
     */
    @Override
    protected void useAmmo() {
        int shotsNeedFiring;
        setDone();
        checkAmmo();
        howManyShots = weapon.getCurrentShots();
        int total = weaponEntity.getTotalAmmoOfType(ammo.getType());
        if (total <= howManyShots) {
            howManyShots = total;
        }
        shotsNeedFiring = howManyShots;
        if (ammo.getUsableShotsLeft() == 0) {
            weaponEntity.loadWeapon(weapon);
            ammo = (AmmoMounted) weapon.getLinked();
            // there will be some ammo somewhere, otherwise shot will not have
            // been fired.
        }
        while (shotsNeedFiring > ammo.getUsableShotsLeft()) {
            shotsNeedFiring -= ammo.getBaseShotsLeft();
            ammo.setShotsLeft(0);
            weaponEntity.loadWeapon(weapon);
            ammo = (AmmoMounted) weapon.getLinked();
        }
        ammo.setShotsLeft(ammo.getBaseShotsLeft() - shotsNeedFiring);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcHits(java.util.Vector)
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
     * @see megamek.common.weapons.handlers.WeaponHandler#addHeat()
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
     * megamek.common.weapons.handlers.WeaponHandler#handleEntityDamage(megamek.common
     * .Entity, java.util.Vector, megamek.common.units.Building, int, int, int, int)
     */
    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        int nDamage;
        if (hit == null) {
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                  toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
                  weaponAttackAction.getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());
        }

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
              .getCover(), ComputeSideTable.sideTable(attackingEntity, entityTarget, weapon
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
        // damage absorption by the partial cover, if it had happened
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
                        false, weaponEntity.getSwarmTargetId() == entityTarget
                              .getId() ? DamageType.IGNORE_PASSENGER
                              : damageType, false, false, throughFront,
                        underWater));
        }
    }
}
