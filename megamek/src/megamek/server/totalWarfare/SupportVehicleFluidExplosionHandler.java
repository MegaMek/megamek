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

import java.util.Vector;

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.AmmoMounted;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Resolves the Support Vehicle Inferno Fuel cook-off rule (TO:AUE p.173). A Support Vehicle that lacks
 * both an Armored Chassis and Environmental Sealing risks detonating any Inferno Fuel it carries whenever
 * it suffers external heat damage or a transport-bay critical hit: roll 2D6, and on a 10 or higher the
 * Inferno Fuel explodes. Better-protected Support Vehicles (and any non-Support-Vehicle) are immune to
 * this check.
 *
 * @author The MegaMek Team
 */
class SupportVehicleFluidExplosionHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(SupportVehicleFluidExplosionHandler.class);

    /** A roll of this or higher on 2D6 detonates the Inferno Fuel (TO:AUE p.173). */
    private static final int COOK_OFF_TARGET_NUMBER = 10;

    SupportVehicleFluidExplosionHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Checks whether an unsealed Support Vehicle carrying Inferno Fuel cooks off after a heat-shock event.
     *
     * @param entity  the unit that just suffered external heat damage or a transport-bay critical hit
     * @param trigger a short description of what prompted the check (for diagnostic logging)
     *
     * @return the reports describing the cook-off roll and any resulting explosion; empty if the unit is
     *       not eligible or the roll is failed
     */
    Vector<Report> checkInfernoFuelCookOff(Entity entity, String trigger) {
        Vector<Report> reports = new Vector<>();
        if (!entity.isSupportVehicle()) {
            return reports;
        }
        if (entity.hasArmoredChassis() || entity.hasEnvironmentalSealing()) {
            LOGGER.debug("[InfernoFuel] {}: protected by Armored Chassis / Environmental Sealing - "
                  + "no cook-off from {}", entity.getShortName(), trigger);
            return reports;
        }
        AmmoMounted infernoFuel = findLoadedInfernoFuel(entity);
        if (infernoFuel == null) {
            return reports;
        }

        Roll diceRoll = Compute.rollD6(2);
        int rollValue = diceRoll.getIntValue();
        boolean avoided = rollValue < COOK_OFF_TARGET_NUMBER;
        LOGGER.debug("[InfernoFuel] {}: unsealed Support Vehicle cook-off check ({}); rolled {} vs {}+ - {}",
              entity.getShortName(), trigger, rollValue, COOK_OFF_TARGET_NUMBER,
              avoided ? "no explosion" : "DETONATES");

        Report report = new Report(3381);
        report.subject = entity.getId();
        report.addDesc(entity);
        report.add(diceRoll);
        report.choose(avoided);
        report.indent(2);
        reports.add(report);

        if (!avoided) {
            reports.addAll(gameManager.explodeEquipment(entity, infernoFuel.getLocation(), infernoFuel));
        }
        return reports;
    }

    /**
     * @param entity the unit to scan
     *
     * @return the first non-empty Inferno Fuel ammo bin carried by the unit, or {@code null} if it carries
     *       no Inferno Fuel
     */
    private AmmoMounted findLoadedInfernoFuel(Entity entity) {
        for (AmmoMounted ammo : entity.getAmmo()) {
            if (ammo.getType().isInfernoFuel() && (ammo.getHittableShotsLeft() > 0)) {
                return ammo;
            }
        }
        return null;
    }
}
