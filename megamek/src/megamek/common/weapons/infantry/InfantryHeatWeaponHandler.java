/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.weapons.infantry;

import java.io.Serial;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.compute.ComputeSideTable;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.Weapon;
import megamek.server.totalWarfare.TWGameManager;


/**
 * @author Jay Lawson
 */
public class InfantryHeatWeaponHandler extends InfantryWeaponHandler {


    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 8430370552107061610L;

    /**
     *
     */
    public InfantryHeatWeaponHandler(ToHitData toHitData, WeaponAttackAction weaponAttackAction, Game game,
          TWGameManager twGameManager) throws EntityLoadingException {
        super(toHitData, weaponAttackAction, game, twGameManager);
        bSalvo = true;
    }

    @Override
    protected void handleEntityDamage(Entity entityTarget, Vector<Report> vPhaseReport, IBuilding bldg, int hits,
          int nCluster, int bldgAbsorbs) {
        if (entityTarget.tracksHeat()) {
            Report.addNewline(vPhaseReport);
            // heat
            hit = entityTarget.rollHitLocation(toHit.getHitTable(),
                  toHit.getSideTable(), weaponAttackAction.getAimedLocation(), weaponAttackAction
                        .getAimingMode(), toHit.getCover());
            hit.setAttackerId(getAttackerId());

            Hex targetHex = game.getBoard().getHex(target.getPosition());
            boolean indirect = weapon.hasModes() && weapon.curMode().getName().equals(Weapon.MODE_INDIRECT_HEAT);
            boolean partialCoverForIndirectFire = indirect &&
                  (unitGainsPartialCoverFromWater(targetHex, entityTarget)
                        || (WeaponAttackAction.targetInShortCoverBuilding(target)
                        && entityTarget.locationIsLeg(hit.getLocation())));

            if ((!indirect || partialCoverForIndirectFire)
                  && entityTarget.removePartialCoverHits(hit.getLocation(), toHit
                        .getCover(),
                  ComputeSideTable.sideTable(attackingEntity, entityTarget, weapon.getCalledShot().getCall()))) {
                // Weapon strikes Partial Cover.            
                handlePartialCoverHit(entityTarget, vPhaseReport, hit, bldg, hits,
                      nCluster, bldgAbsorbs);
                return;
            }

            Report report = new Report(3400);
            report.subject = subjectId;
            report.indent(2);
            int nDamage = nDamPerHit * Math.min(nCluster, hits);
            // Building may absorb some damage.
            boolean targetStickingOutOfBuilding = unitStickingOutOfBuilding(targetHex, entityTarget);
            nDamage = absorbBuildingDamage(nDamage, entityTarget, bldgAbsorbs,
                  vPhaseReport, bldg, targetStickingOutOfBuilding);
            nDamage = checkTerrain(nDamage, entityTarget, vPhaseReport);
            nDamage = checkLI(nDamage, entityTarget, vPhaseReport);
            if ((bldg != null) && !targetStickingOutOfBuilding) {
                nDamage = (int) Math.floor(bldg.getDamageToScale() * nDamage);
            }

            // If using BMM heat option, do damage as well as heat
            if (game.getOptions().booleanOption(OptionsConstants.BASE_INFANTRY_DAMAGE_HEAT)) {
                vPhaseReport.addAll(gameManager.damageEntity(entityTarget, hit, nDamage, false,
                      attackingEntity.getSwarmTargetId() == entityTarget.getId() ?
                            DamageType.IGNORE_PASSENGER :
                            damageType,
                      false, false, throughFront, underWater, nukeS2S));
            }
            if (entityTarget.getArmor(hit) > 0 &&
                  (entityTarget.getArmorType(hit.getLocation()) ==
                        EquipmentType.T_ARMOR_HEAT_DISSIPATING)) {
                entityTarget.heatFromExternal += nDamage / 2;
                report.add(nDamage / 2);
                report.choose(true);
                report.messageId = 3406;
                report.add(ArmorType.forEntity(entityTarget, hit.getLocation()).getName());
            } else {
                entityTarget.heatFromExternal += nDamage;
                report.add(nDamage);
                report.choose(true);
            }
            vPhaseReport.addElement(report);
        } else {
            super.handleEntityDamage(entityTarget, vPhaseReport, bldg, hits, nCluster, bldgAbsorbs);
        }
    }
}
