/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.floor;

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
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.weapons.DamageType;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Jason Tighe
 */
public class RifleWeaponHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 7468287406174862534L;

    private HitData hit;

    /**
     *
     */
    public RifleWeaponHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {

        double toReturn = weaponType.getDamage();
        // we default to direct fire weapons for anti-infantry damage
        if (target.isConventionalInfantry()) {
            toReturn = Compute.directBlowInfantryDamage(toReturn,
                  bDirect ? toHit.getMoS() : 0,
                  weaponType.getInfantryDamageClass(),
                  ((Infantry) target).isMechanized(),
                  toHit.getThruBldg() != null, attackingEntity.getId(), calcDmgPerHitReport);
        } else if (bDirect) {
            toReturn = Math.min(toReturn + (int) floor(toHit.getMoS() / 3.0), toReturn * 2);
        }
        Entity te;
        if (target instanceof Entity) {
            te = (Entity) target;
            hit = te.rollHitLocation(toHit.getHitTable(), toHit.getSideTable(),
                  weaponAttackAction.getAimedLocation(), weaponAttackAction.getAimingMode(),
                  toHit.getCover());
            hit.setAttackerId(getAttackerId());
            if (!(te instanceof Infantry)
                  && (!te.hasBARArmor(hit.getLocation()) || (te.getBARRating(hit.getLocation()) >= 8))) {
                toReturn = Math.max(0, toReturn - 3);
            }
        }

        toReturn = applyGlancingBlowModifier(toReturn, target.isConventionalInfantry());

        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_LONG])) {
            toReturn = (int) Math.floor(toReturn * .75);
        }
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)
              && (nRange > weaponType.getRanges(weapon)[RangeType.RANGE_EXTREME])) {
            toReturn = (int) Math.floor(toReturn * .5);
        }

        return (int) toReturn;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget,
          Vector<Report> vPhaseReport, IBuilding bldg, int hits, int nCluster,
          int bldgAbsorbs) {
        missed = false;

        hit.setGeneralDamageType(generalDamageType);
        hit.setBoxCars(roll.getIntValue() == 12);

        if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
              .getCover(), ComputeSideTable.sideTable(attackingEntity, entityTarget, weapon
              .getCalledShot().getCall()))) {
            // Weapon strikes Partial Cover.
            handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                  nCluster, bldgAbsorbs);
            return;
        }

        Report r = new Report(3405);
        r.subject = subjectId;
        r.add(toHit.getTableDesc());
        r.add(entityTarget.getLocationAbbr(hit));
        vPhaseReport.addElement(r);

        // for non-salvo shots, report that the aimed shot was successfully
        // before applying damage
        if (hit.hitAimedLocation() && !bSalvo) {
            r = new Report(3410);
            r.subject = subjectId;
            vPhaseReport.lastElement().newlines = 0;
            vPhaseReport.addElement(r);
        }
        // Resolve damage normally.
        int nDamage = nDamPerHit * Math.min(nCluster, hits);

        if (bDirect) {
            hit.makeDirectBlow(toHit.getMoS() / 3);
        }

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
            r = new Report(3415);
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
            vPhaseReport
                  .addAll(gameManager.damageEntity(entityTarget, hit, nDamage,
                        false, attackingEntity.getSwarmTargetId() == entityTarget
                              .getId() ? DamageType.IGNORE_PASSENGER
                              : damageType, false, false, throughFront,
                        underWater, nukeS2S));
        }
    }
}
