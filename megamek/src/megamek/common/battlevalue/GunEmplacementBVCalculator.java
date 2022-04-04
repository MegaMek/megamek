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
import megamek.common.*;

public class GunEmplacementBVCalculator {

    public static int calculateBV(GunEmplacement gunEmplacement, boolean ignoreC3, boolean ignoreSkill, CalculationReport bvReport) {
        bvReport.addHeader("Battle Value Calculations For");
        bvReport.addHeader(gunEmplacement.getChassis() + " " + gunEmplacement.getModel());
        bvReport.addLine("There is currently no report available for Gun Emplacements.");

        // using structures BV rules from MaxTech
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        dbv += gunEmplacement.getTotalArmor();

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : gunEmplacement.getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_AMS))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS))
                    || etype.hasFlag(MiscType.F_ECM)) {
                dEquipmentBV += etype.getBV(gunEmplacement);
            }
        }
        dbv += dEquipmentBV;
        dbv *= 0.5; // structure modifier
        double weaponBV = 0;

        // figure out base weapon bv
        boolean hasTargComp = gunEmplacement.hasTargComp();
        for (Mounted mounted : gunEmplacement.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(gunEmplacement);

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.1;
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.15;
                }
            }

            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                }
            }

            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.2;
            }

            weaponBV += dBV;
        }
        obv += weaponBV;

        // add ammo bv
        double ammoBV = 0;
        for (Mounted mounted : gunEmplacement.getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(gunEmplacement);
        }
        obv += ammoBV;

        // structure modifier
        obv *= 0.44;
        double finalBV = obv + dbv;
        double xbv = 0.0;
        if (!ignoreC3 && (gunEmplacement.getGame() != null)) {
            xbv += gunEmplacement.getExtraC3BV((int) Math.round(finalBV));
        }

        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignoreSkill) {
            pilotFactor = gunEmplacement.getCrew().getBVSkillMultiplier(gunEmplacement.getGame());
        }

        return (int) Math.round(finalBV * pilotFactor);
    }
}
