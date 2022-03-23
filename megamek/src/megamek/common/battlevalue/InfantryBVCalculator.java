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

import megamek.common.Compute;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.weapons.infantry.InfantryWeapon;

import java.text.DecimalFormat;

public class InfantryBVCalculator extends BVCalculator {

    public static int calculateBV(Infantry infantry, boolean ignoreC3, boolean ignoreSkill, StringBuffer bvText) {
        bvText.delete(0, bvText.length());
        bvText.append("<HTML><BODY><CENTER><b>Battle Value Calculations For ");
        DecimalFormat df = new DecimalFormat("0.##");
        bvText.append(infantry.getChassis());
        bvText.append(" ");
        bvText.append(infantry.getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

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

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Base Target Movement Modifier:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(tmmFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        if (infantry.hasDEST()) {
            tmmFactor += 0.2;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("DEST:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+0.2");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSneakCamo()) {
            tmmFactor += 0.2;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Camo (Sneak):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+0.2");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSneakIR()) {
            tmmFactor += 0.2;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Camo (IR):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+0.2");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSneakECM()) {
            tmmFactor += 0.1;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Camo (ECM):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+0.1");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        dbr *= tmmFactor;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Target Movement Modifier:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(tmmFactor));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Damage Divisor:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(infantry.calcDamageDivisor()));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Number of Troopers x 1.5 x TMM x DD");
        bvText.append(endColumn + startColumn);
        bvText.append(men);
        bvText.append(" x 1.5 x ");
        bvText.append(tmmFactor);
        bvText.append(" x ");
        bvText.append(infantry.calcDamageDivisor());
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("= ");
        bvText.append(df.format(dbr));
        bvText.append(endColumn);
        bvText.append(endRow);

        // double weaponbv;
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
        wbv = wbv * (men/squadsize);
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
            ambv *= men/squadsize;
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("<b>Offensive Battle Rating Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapon BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        if (null != primaryW) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(primaryW.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append((squadsize - secondn) * squadn);
            bvText.append(" x " );
            bvText.append(df.format(primaryW.getBV(infantry)));
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(df.format(primaryW.getBV(infantry) * (squadsize - secondn) * squadn));
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (null != secondW) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(secondW.getName());
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(secondn * squadn);
            bvText.append(" x " );
            bvText.append(df.format(secondW.getBV(infantry)));
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(df.format(secondW.getBV(infantry) * secondn * squadn));
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        // add in field gun BV
        for (Mounted mounted : infantry.getEquipment()) {
            if (mounted.getLocation() == Infantry.LOC_FIELD_GUNS) {
                wbv += mounted.getType().getBV(infantry);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(mounted.getType().getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(mounted.getType().getBV(infantry));
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        obr = (wbv + ambv) * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapon BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(wbv));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Anti-Mek BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(ambv));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Speed Factor:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(speedFactor));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Weapons BV x Speed Factor:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(wbv + ambv));
        bvText.append(" x ");
        bvText.append(df.format(speedFactor));
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(obr));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        double bv;
        if (infantry.useGeometricMeanBV()) {
            bv = 2 * Math.sqrt(obr * dbr);
            if (bv == 0) {
                bv = dbr + obr;
            }
            bvText.append("SQRT(Defensive BR * Offensive BR) x 2:");
            bvText.append(endColumn);
            bvText.append(startColumn);
        } else {
            bv = obr + dbr;
            bvText.append("Defensive BR + Offensive BR:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(df.format(dbr));
            bvText.append(" + ");
            bvText.append(df.format(obr));
        }

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(bv));
        bvText.append(endColumn);
        bvText.append(endRow);

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

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Base Unit Type Modifier:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(utm));
        bvText.append(endColumn);
        bvText.append(endRow);

        if (infantry.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
            utm += 0.1;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Combat Engineers:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("0.1");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSpecialization(Infantry.MARINES)) {
            utm += 0.3;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Marines:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("0.3");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
            utm += 0.2;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Mountain Troops:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("0.2");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSpecialization(Infantry.PARATROOPS)) {
            utm += 0.1;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Paratroops:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("0.1");
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        
        if (infantry.hasSpecialization(Infantry.SCUBA)) {
            utm += 0.1;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("SCUBA:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("0.1");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        if (infantry.hasSpecialization(Infantry.XCT)) {
            utm += 0.1;
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("XCT:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("0.1");
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Unit Type Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(utm));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(df.format(bv));
        bvText.append(" x ");
        bvText.append(df.format(utm));
        bvText.append(endColumn);

        bv *= utm;
        bvText.append(startColumn);
        bvText.append((int) Math.round(bv));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = infantry.getCrew().getBVSkillMultiplier(infantry.isAntiMekTrained(), infantry.getGame());
        }
        return (int) Math.round((bv) * pilotFactor);
    }
}
