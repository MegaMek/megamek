/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.battlevalue;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MPCalculationSetting;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SmallCraft;

public class AeroBVCalculator extends HeatTrackingBVCalculator {

    protected final Aero aero;

    AeroBVCalculator(Entity entity) {
        super(entity);
        aero = (Aero) entity;
    }

    @Override
    protected void processStructure() {
        String calculation = "+ " + aero.getSI() + " x 2";
        if (hasBlueShield) {
            calculation += " x 1.2 (Blue Shield)";
        }
        defensiveValue += aero.getSI() * 2 * (hasBlueShield ? 1.2 : 1);
        bvReport.addLine("Structural Integrity:", calculation, "= " + formatForReport(defensiveValue));
    }

    @Override
    protected boolean validArmorLocation(int location) {
        return location <= 4;
    }

    @Override
    protected void processExplosiveEquipment() {
        boolean hasExplosiveEquipment = false;
        bvReport.startTentativeSection();
        bvReport.addLine("Explosive Equipment:", "", "");

        // We iterate over a copy to avoid any risk of ConcurrentModificationException
        List<Mounted<?>> equipmentCopy = new ArrayList<>(aero.getEquipment());
        Set<AmmoType> ammos = new HashSet<>();

        for (Mounted<?> mounted : equipmentCopy) {
            if (!countsAsExplosive(mounted)) {
                continue;
            }

            if (entity.hasCase() || entity.hasCASEII() || aero.isClan()) {
                bvReport.discardTentativeSection();
                bvReport.addLine("Explosive Equipment:", "(CASE)", "--");
                return;
            }

            EquipmentType eType = mounted.getType();
            if (eType instanceof AmmoType) {
                if (ammos.add((AmmoType) eType)) {
                    defensiveValue -= 15;
                    bvReport.addLine("- " + equipmentDescriptor(mounted),
                          "- 15",
                          "= " + formatForReport(defensiveValue));
                }
            } else {
                defensiveValue -= 1;
                bvReport.addLine("- " + equipmentDescriptor(mounted), "- 1", "= " + formatForReport(defensiveValue));
            }
            hasExplosiveEquipment = true;
        }
        bvReport.finalizeTentativeSection(hasExplosiveEquipment);
        super.processExplosiveEquipment();
    }

    @Override
    protected void processTypeModifier() {
        double typeModifier = 1.2;
        if (entity.isSupportVehicle() || (entity instanceof SmallCraft)) {
            typeModifier = 1;
        } else if (entity instanceof ConvFighter) {
            typeModifier = 1.1;
        }
        String calculation = formatForReport(typeModifier);
        if (aero.hasStealth()) {
            calculation = "(" + calculation + " + 0.3 (Stealth))";
            typeModifier += 0.3;
        }
        calculation = formatForReport(defensiveValue) + " x " + calculation;

        bvReport.addLine("Type Modifier:", calculation, "= " + formatForReport(defensiveValue * typeModifier));
        defensiveValue *= typeModifier;
    }

    @Override
    protected void processDefensiveFactor() {
    }

    @Override
    protected int heatEfficiency() {
        bvReport.addLine("Heat Efficiency:", " = " + (6 + aero.getHeatCapacity()), "");
        return 6 + aero.getHeatCapacity();
    }

    @Override
    protected boolean isNominalRear(Mounted<?> weapon) {
        return switchRearAndFront ^ rearWeaponFilter().test(weapon);
    }

    @Override
    protected Predicate<Mounted<?>> frontWeaponFilter() {
        return weapon -> !weapon.isRearMounted() && !(weapon.getLocation() == Aero.LOC_AFT);
    }

    @Override
    protected Predicate<Mounted<?>> rearWeaponFilter() {
        return weapon -> weapon.isRearMounted() || (weapon.getLocation() == Aero.LOC_AFT);
    }

    @Override
    protected void processSummarize() {
        super.processSummarize();

        double cockpitMod = 1;
        String modifier = "";
        if ((aero.getCockpitType() == Aero.COCKPIT_SMALL) || (aero.getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE)) {
            cockpitMod = 0.95;
            modifier = " (" + aero.getCockpitTypeString() + ")";
        } else if (entity.hasWorkingMisc(MiscType.F_DRONE_OPERATING_SYSTEM)) {
            cockpitMod = 0.95;
            modifier = " (Drone Op. Sys.)";
        }
        if (cockpitMod != 1) {
            baseBV *= cockpitMod;
            bvReport.addLine("Cockpit Modifier:",
                  formatForReport(baseBV) + " x " + formatForReport(cockpitMod) + modifier,
                  "= " + baseBV);
            bvReport.addResultLine("Base BV:", "", formatForReport(baseBV));
        }
    }

    @Override
    protected int offensiveSpeedFactorMP() {
        return aero.getRunMP(MPCalculationSetting.BV_CALCULATION);
    }
}
