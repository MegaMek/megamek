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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.Compute;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.weapons.infantry.InfantryWeapon;

import java.text.NumberFormat;
import java.util.Locale;

public class InfantryBVCalculator {

    public static int calculateBV(Infantry infantry, boolean ignoreSkill, CalculationReport bvReport) {
        NumberFormat df = NumberFormat.getNumberInstance(Locale.US);
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(0);

        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(infantry.getChassis() + " " + infantry.getModel());
        bvReport.addSubHeader("Defensive Battle Rating Calculation:");
        double dbr; // defensive battle rating
        int men = Math.max(0, infantry.getInternal(Infantry.LOC_INFANTRY));
        dbr = men * 1.5 * infantry.calcDamageDivisor();
        int tmmRan = Compute.getTargetMovementModifier(
                infantry.getRunMP(false, true, true),
                false, false, infantry.getGame()).getValue();

        final int jumpMP = infantry.getJumpMP(false);
        final int tmmJumped = (jumpMP > 0) ?
                Compute.getTargetMovementModifier(jumpMP, true, false, infantry.getGame()).getValue()
                : 0;

        final int umuMP = infantry.getActiveUMUCount();
        final int tmmUMU = (umuMP > 0) ?
                Compute.getTargetMovementModifier(umuMP, false, false, infantry.getGame()).getValue()
                : 0;

        double targetMovementModifier = Math.max(tmmRan, Math.max(tmmJumped, tmmUMU));
        double tmmFactor = 1 + (targetMovementModifier / 10);
        bvReport.addLine("Base Target Movement Modifier:", "", tmmFactor);

        if (infantry.hasDEST()) {
            tmmFactor += 0.2;
            bvReport.addLine("DEST:", "+0.2");
        }

        if (infantry.hasSneakCamo()) {
            tmmFactor += 0.2;
            bvReport.addLine("Camo (Sneak):", "+0.2");
        }

        if (infantry.hasSneakIR()) {
            tmmFactor += 0.2;
            bvReport.addLine("Camo (IR):", "+0.2");
        }

        if (infantry.hasSneakECM()) {
            tmmFactor += 0.1;
            bvReport.addLine("Camo (ECM):", "+0.1");
        }
        dbr *= tmmFactor;

        bvReport.addResultLine("Target Movement Modifier:", "", df.format(tmmFactor));
        bvReport.addLine("Damage Divisor:", "", df.format(infantry.calcDamageDivisor()));
        bvReport.addLine("Defensive Battle Rating:",
                men + " x 1.5 x " + tmmFactor + " x " + infantry.calcDamageDivisor(),
                "= " + df.format(dbr));

        double obr; // offensive battle rating

        int speedFactorTableLookup = Math.max(infantry.getRunMP(false, true, true), Math.max(jumpMP, umuMP));
        double speedFactor = Math.pow(1 + ((speedFactorTableLookup - 5) / 10.0), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        double wbv = 0;
        InfantryWeapon primaryW = infantry.getPrimaryWeapon();
        InfantryWeapon secondW = infantry.getSecondaryWeapon();
        int squadsize = infantry.getSquadSize();
        int secondn = infantry.getSecondaryN();
        int squadn = infantry.getSquadN();

        if (null != primaryW) {
            wbv += primaryW.getBV(infantry) * (squadsize - secondn);
        }
        if (null != secondW) {
            wbv += secondW.getBV(infantry) * (secondn);
        }
        wbv = wbv * (men / squadsize);
        // if anti-mek then double this
        // TODO : need to factor archaic weapons out of this
        double ambv = 0;
        if (infantry.canMakeAntiMekAttacks()) {
            if (primaryW != null && !primaryW.hasFlag(InfantryWeapon.F_INF_ARCHAIC)) {
                ambv += primaryW.getBV(infantry) * (squadsize - secondn);
            }
            if (secondW != null && !secondW.hasFlag(InfantryWeapon.F_INF_ARCHAIC)) {
                ambv += secondW.getBV(infantry) * (secondn);
            }
            ambv *= men / squadsize;
        }

        bvReport.addEmptyLine();
        bvReport.addSubHeader("Offensive Battle Rating Calculation:");
        if (primaryW != null) {
            bvReport.addLine(primaryW.getName(),
                    (squadsize - secondn) * squadn + " x " + df.format(primaryW.getBV(infantry)),
                    "= " + df.format(primaryW.getBV(infantry) * (squadsize - secondn) * squadn));
        }

        if (secondW != null) {
            bvReport.addLine(secondW.getName(),
                    secondn * squadn + " x " + df.format(secondW.getBV(infantry)),
                    "= " + df.format(secondW.getBV(infantry) * secondn * squadn));
        }

        // add in field gun BV
        for (Mounted mounted : infantry.getEquipment()) {
            if (mounted.getLocation() == Infantry.LOC_FIELD_GUNS) {
                wbv += mounted.getType().getBV(infantry);
                bvReport.addLine(mounted.getType().getName(), "", mounted.getType().getBV(infantry));
            }
        }
        obr = (wbv + ambv) * speedFactor;
        bvReport.addResultLine("Weapon BV:", "", df.format(wbv));
        bvReport.addLine("Anti-Mek BV:", "", df.format(ambv));
        bvReport.addLine("Speed Factor:", "", df.format(speedFactor));
        bvReport.addLine("Offensive Battle Rating:",
                df.format(wbv + ambv) + " x " + df.format(speedFactor),
                "= " + df.format(obr));
        bvReport.addEmptyLine();

        double bv = obr + dbr;
        bvReport.addLine("Defensive BR + Offensive BR:",
                df.format(dbr) + " + " + df.format(obr),
                "= " + df.format(bv));

        double utm; // unit type modifier
        switch (infantry.getMovementMode()) {
            case INF_MOTORIZED:
            case WHEELED:
                utm = 0.8;
                break;
            case TRACKED:
                utm = 0.9;
                break;
            case HOVER:
            case VTOL:
                utm = 0.7;
                break;
            case SUBMARINE:
                utm = 0.6;
                break;
            default:
                utm = 1.0;
                break;
        }

        bvReport.addLine("Base Unit Type Modifier:", "", df.format(utm));

        if (infantry.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
            utm += 0.1;
            bvReport.addLine("Combat Engineers:", "0.1");
        }

        if (infantry.hasSpecialization(Infantry.MARINES)) {
            utm += 0.3;
            bvReport.addLine("Marines:", "0.3");
        }

        if (infantry.hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
            utm += 0.2;
            bvReport.addLine("Mountain Troops:", "0.2");
        }

        if (infantry.hasSpecialization(Infantry.PARATROOPS)) {
            utm += 0.1;
            bvReport.addLine("Paratroops:", "0.1");
        }

        if (infantry.hasSpecialization(Infantry.SCUBA)) {
            utm += 0.1;
            bvReport.addLine("SCUBA:", "0.1");
        }

        if (infantry.hasSpecialization(Infantry.XCT)) {
            utm += 0.1;
            bvReport.addLine("XCT:", "0.1");
        }

        bvReport.addResultLine("Total Unit Type Modifier", "", df.format(utm));

        bvReport.addEmptyLine();
        bvReport.addLine("Final BV:",
                df.format(bv) + " x " + df.format(utm),
                "" + (int) Math.round(bv * utm));
        bv *= utm;

        double pilotFactor = ignoreSkill ? 1 : infantry.getCrew().getBVSkillMultiplier();
        return (int) Math.round(bv * pilotFactor);
    }
}