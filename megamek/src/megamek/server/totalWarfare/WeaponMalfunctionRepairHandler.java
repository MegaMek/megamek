/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalWarfare;

import megamek.common.Report;
import megamek.common.actions.RepairWeaponMalfunctionAction;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;

/**
 * Resolves a crew spending its turn clearing a weapon jammed by a Weapon Malfunction critical hit: a vehicle crew
 * (TW p. 195) or the gunners of an Advanced Building (TO:AR p. 119).
 */
class WeaponMalfunctionRepairHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(WeaponMalfunctionRepairHandler.class);

    private static final int REPORT_MALFUNCTION_REPAIRED = 3034;

    WeaponMalfunctionRepairHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Clears the jam named by the action, if the unit is one whose crew can do so.
     *
     * @param entity the unit whose crew is clearing the jam
     * @param action the repair action
     */
    void repair(Entity entity, RepairWeaponMalfunctionAction action) {
        Mounted<?> weapon = entity.getEquipment(action.getWeaponId());
        if (weapon == null) {
            LOGGER.warn("[WeaponJam] {} asked to repair equipment {}, which it does not mount",
                  entity.getShortName(), action.getWeaponId());
            return;
        }
        if (!weapon.isJammed() && !(entity instanceof AbstractBuildingEntity building
              && weapon instanceof WeaponMounted mountedWeapon && building.isTurretJammed(mountedWeapon))) {
            LOGGER.warn("[WeaponJam] {} asked to repair {}, which is not jammed", entity.getShortName(),
                  weapon.getName());
            return;
        }
        switch (entity) {
            case Tank tank -> {
                weapon.setJammed(false);
                tank.getJammedWeapons().remove(weapon);
                reportRepaired(entity, weapon);
            }
            case AbstractBuildingEntity building -> {
                if (!building.canUnjamWeapon()) {
                    LOGGER.warn("[WeaponJam] {} cannot clear a jam now (stunned, no gunners, or nothing jammed)",
                          building.getShortName());
                    return;
                }
                if (!(weapon instanceof WeaponMounted mountedWeapon) || !building.repairBuildingWeapon(mountedWeapon)) {
                    LOGGER.warn("[WeaponJam] {} cannot clear {}: the gunners in its location are dead or stunned",
                          building.getShortName(), weapon.getName());
                    return;
                }
                reportRepaired(entity, weapon);
            }
            default -> LOGGER.error("[WeaponJam] {} is neither a vehicle nor a building and cannot repair a weapon "
                  + "malfunction", entity.getShortName());
        }
    }

    private void reportRepaired(Entity entity, Mounted<?> weapon) {
        LOGGER.info("[WeaponJam] {} clears the jam on {}", entity.getShortName(), weapon.getName());
        Report report = new Report(REPORT_MALFUNCTION_REPAIRED);
        report.subject = entity.getId();
        report.addDesc(entity);
        report.add(weapon.getName());
        addReport(report);
    }
}
