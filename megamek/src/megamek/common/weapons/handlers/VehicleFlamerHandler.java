/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

import static java.lang.Math.floor;

import java.io.Serial;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.EquipmentMode;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.weapons.FlamerHandlerHelper;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.flamers.clan.CLHeavyFlamer;
import megamek.common.weapons.flamers.innerSphere.ISHeavyFlamer;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 * @since Sep 23, 2004
 */
public class VehicleFlamerHandler extends AmmoWeaponHandler {
    @Serial
    private static final long serialVersionUID = 1130274470571109915L;

    /**
     *
     */
    public VehicleFlamerHandler(ToHitData toHit, WeaponAttackAction waa, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(toHit, waa, g, m);
        generalDamageType = HitData.DAMAGE_ENERGY;
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
        boolean bmmFlamerDamage = game.getOptions().booleanOption(OptionsConstants.BASE_FLAMER_HEAT);
        Entity entity = game.getEntity(weaponAttackAction.getEntityId());

        if (entity == null) {
            return;
        }

        EquipmentMode currentWeaponMode = entity.getEquipment(weaponAttackAction.getWeaponId()).curMode();

        boolean flamerDoesHeatOnlyDamage = currentWeaponMode != null
              && currentWeaponMode.equals(Weapon.MODE_FLAMER_HEAT);
        boolean flamerDoesOnlyDamage = currentWeaponMode != null && currentWeaponMode.equals(Weapon.MODE_FLAMER_DAMAGE);

        if (bmmFlamerDamage || flamerDoesOnlyDamage || (flamerDoesHeatOnlyDamage && !entityTarget.tracksHeat())) {
            // PLAYTEST3 Heat-dissipating armor reduces damage
            if (game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3)) {
                if (hit != null) {
                    hit.setHeatWeapon(true);
                }
            }
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);

            if (bmmFlamerDamage && entityTarget.tracksHeat()) {
                FlamerHandlerHelper.doHeatDamage(entityTarget, vPhaseReport, weaponType, subjectId, hit);
            }
        } else if (flamerDoesHeatOnlyDamage) {
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                  toHit.getSideTable(), weaponAttackAction.getAimedLocation(),
                  weaponAttackAction.getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());

            if (entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                  .getCover(), ComputeSideTable.sideTable(attackingEntity, entityTarget,
                  weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg,
                      hits, nCluster, bldgAbsorbs);
                return;
            }
            Report r = new Report(3405);
            r.subject = subjectId;
            r.add(toHit.getTableDesc());
            r.add(entityTarget.getLocationAbbr(hit));
            vPhaseReport.addElement(r);

            FlamerHandlerHelper.doHeatDamage(entityTarget, vPhaseReport, weaponType, subjectId, hit);
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see megamek.common.weapons.handlers.WeaponHandler#calcDamagePerHit()
     */
    @Override
    protected int calcDamagePerHit() {
        double toReturn;
        if (target.isConventionalInfantry()) {
            if (attackingEntity instanceof BattleArmor) {
                toReturn = Compute.d6(3);
            } else if ((weaponType instanceof ISHeavyFlamer)
                  || (weaponType instanceof CLHeavyFlamer)) {
                toReturn = Compute.d6(6);
            } else {
                toReturn = Compute.d6(4);
            }
            if (bDirect) {
                toReturn += (int) floor(toHit.getMoS() / 3.0);
            }
            // pain shunted infantry get half damage
            if (((Entity) target).hasAbility(OptionsConstants.MD_PAIN_SHUNT)) {
                toReturn /= 2;
            }

            toReturn = applyGlancingBlowModifier(toReturn, true);
        } else {
            toReturn = super.calcDamagePerHit();
        }
        return (int) toReturn;
    }

    @Override
    protected void handleIgnitionDamage(Vector<Report> vPhaseReport, IBuilding bldg, int hits) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            r.newlines = 0;
            vPhaseReport.addElement(r);
        }
        TargetRoll tn = new TargetRoll(weaponType.getFireTN(), weaponType.getName());
        if (tn.getValue() != TargetRoll.IMPOSSIBLE) {
            Report.addNewline(vPhaseReport);
            gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, true, false,
                  tn, true, -1, vPhaseReport);
        }
    }

    @Override
    protected void handleClearDamage(Vector<Report> vPhaseReport, IBuilding bldg, int nDamage) {
        if (!bSalvo) {
            // hits!
            Report r = new Report(2270);
            r.subject = subjectId;
            vPhaseReport.addElement(r);
        }
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
              && gameManager.tryIgniteHex(target.getPosition(), target.getBoardId(), subjectId, true,
              false,
              new TargetRoll(weaponType.getFireTN(), weaponType.getName()), 5,
              vPhaseReport)) {
            return;
        }
        Vector<Report> clearReports = gameManager.tryClearHex(target.getPosition(),
              target.getBoardId(),
              nDamage,
              subjectId);
        if (!clearReports.isEmpty()) {
            vPhaseReport.lastElement().newlines = 0;
        }
        vPhaseReport.addAll(clearReports);
    }
}
