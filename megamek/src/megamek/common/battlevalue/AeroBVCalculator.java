/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.battlevalue;

import megamek.common.*;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

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
        Set<AmmoType> ammos = new HashSet<>();
        for (Mounted mounted : aero.getEquipment()) {
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
                    bvReport.addLine("- " + equipmentDescriptor(mounted), "- 15",
                            "= " + formatForReport(defensiveValue));
                }
            } else {
                defensiveValue -= 1;
                bvReport.addLine("- " + equipmentDescriptor(mounted), "- 1",
                        "= " + formatForReport(defensiveValue));
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

        bvReport.addLine("Type Modifier:", calculation,
                "= " + formatForReport(defensiveValue * typeModifier));
        defensiveValue *= typeModifier;
    }

    @Override
    protected void processDefensiveFactor() { }

    @Override
    protected int heatEfficiency() {
        bvReport.addLine("Heat Efficiency:", " = " + (6 + aero.getHeatCapacity()), "");
        return 6 + aero.getHeatCapacity();
    }

    @Override
    protected boolean isNominalRear(Mounted weapon) {
        return switchRearAndFront ^ rearWeaponFilter().test(weapon);
    }

    @Override
    protected Predicate<Mounted> frontWeaponFilter() {
        return weapon -> !weapon.isRearMounted() && !(weapon.getLocation() == Aero.LOC_AFT);
    }

    @Override
    protected Predicate<Mounted> rearWeaponFilter() {
        return weapon -> weapon.isRearMounted() || (weapon.getLocation() == Aero.LOC_AFT);
    }

    @Override
    protected void processSummarize() {
        super.processSummarize();

        double cockpitMod = 1;
        String modifier = "";
        if ((aero.getCockpitType() == Aero.COCKPIT_SMALL)
                || (aero.getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE)) {
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
        return aero.getRunMP();
    }
}