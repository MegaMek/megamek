/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers.srm;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.IArmorState;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 */
public class SRMTandemChargeHandler extends SRMHandler {
    @Serial
    private static final long serialVersionUID = 6292692766500970690L;

    public SRMTandemChargeHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        sSalvoType = " tandem charge missile(s) ";
        generalDamageType = HitData.DAMAGE_ARMOR_PIERCING_MISSILE;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        missed = false;

        HitData hit = entityTarget.rollHitLocation(toHit.getHitTable(),
              toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
              weaponAttackAction.getAimingMode(), toHit.getCover());
        hit.setGeneralDamageType(generalDamageType);
        hit.setAttackerId(getAttackerId());
        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
              .getCover(), ComputeSideTable.sideTable(attackingEntity, entityTarget, weapon
              .getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                  nCluster, bldgAbsorbs);
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
        // Resolve damage normally.
        int nDamage = nDamPerHit * Math.min(nCluster, hits);

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
            missed = true;
        } else {
            if (bGlancing) {
                hit.makeGlancingBlow();
            }

            if (bLowProfileGlancing) {
                hit.makeGlancingBlow();
            }

            if (bDirect && !target.isConventionalInfantry()) {
                hit.makeDirectBlow(toHit.getMoS() / 3);
            }

            if ((target instanceof BattleArmor)
                  && (((BattleArmor) target).getInternal(hit.getLocation()) != IArmorState.ARMOR_DOOMED)) {
                int critRoll = Compute.d6(2);
                int loc = hit.getLocation();
                if (critRoll >= 10) {
                    hit = new HitData(loc, false, HitData.EFFECT_CRITICAL);
                }
            } else if ((target instanceof Tank) || (target instanceof Mek)) {

                if (bGlancing || bLowProfileGlancing) {
                    // this will be either -4 or -8
                    hit.setSpecCriticalModifier(-2 * (int) getTotalGlancingBlowFactor());
                } else if (bDirect) {
                    hit.setSpecCriticalModifier((toHit.getMoS() / 3) - 2);
                } else {
                    hit.setSpecCriticalModifier(-2);
                }
            }
            vPhaseReport
                  .addAll(gameManager.damageEntity(entityTarget, hit, nDamage,
                        false, attackingEntity.getSwarmTargetId() == entityTarget
                              .getId() ? DamageType.IGNORE_PASSENGER
                              : DamageType.NONE, false, false,
                        throughFront, underWater));
        }
    }

    @Override
    protected int calcDamagePerHit() {
        if (target.isConventionalInfantry()) {
            double toReturn = Compute.directBlowInfantryDamage(
                  weaponType.getRackSize(), bDirect ? toHit.getMoS() / 3 : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);

            toReturn = applyGlancingBlowModifier(toReturn, true);

            return (int) Math.floor(toReturn);
        }
        return 2;
    }
}
