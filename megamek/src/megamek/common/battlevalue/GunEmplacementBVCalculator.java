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

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;

public class GunEmplacementBVCalculator extends BVCalculator {

    private final GunEmplacement gunEmplacement;

    GunEmplacementBVCalculator(Entity entity) {
        super(entity);
        gunEmplacement = (GunEmplacement) entity;
    }

    @Override
    protected void processDefensiveValue() {
        defensiveValue = gunEmplacement.getTotalArmor();
        bvReport.addLine("Total Armor:", "", "= " + formatForReport(defensiveValue));

        for (Mounted equipment : gunEmplacement.getEquipment()) {
            if (equipment.isDestroyed()) {
                continue;
            }

            EquipmentType eType = equipment.getType();
            boolean isAMS = (eType instanceof WeaponType) && eType.hasFlag(WeaponType.F_AMS);
            boolean isAMSAmmo = (eType instanceof AmmoType) && (((AmmoType) eType).getAmmoType() == AmmoType.T_AMS);

            if (isAMS || isAMSAmmo || eType.hasFlag(MiscType.F_ECM)) {
                double equipmentBV = eType.getBV(gunEmplacement);
                defensiveValue += equipmentBV;
                bvReport.addLine(equipment.getDesc(), "+ " + formatForReport(equipmentBV),
                        "= " + formatForReport(defensiveValue));
            }
        }
        bvReport.addLine("Structure Modifier:", formatForReport(defensiveValue) + " x 0.5",
                "= " + formatForReport(defensiveValue * 0.5));
        defensiveValue *= 0.5;
    }

    @Override
    protected void processOffensiveValue() {
        boolean hasTargComp = gunEmplacement.hasTargComp();
        for (Mounted weapon : gunEmplacement.getWeaponList()) {
            WeaponType weaponType = (WeaponType) weapon.getType();
            if (weapon.isDestroyed() || (weaponType.hasFlag(WeaponType.F_AMS))) {
                continue;
            }

            // artemis bumps up the value
            double weaponBV = weaponType.getBV(gunEmplacement);
            String calculation = "+ " + formatForReport(weaponBV);
            if ((weapon.getLinkedBy() != null) && (weapon.getLinkedBy().getType() instanceof MiscType)) {
                Mounted linkedBy = weapon.getLinkedBy();
                if (linkedBy.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    weaponBV *= 1.2;
                    calculation += " x 1.2 (Artemis)";
                } else if (linkedBy.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    weaponBV *= 1.1;
                    calculation += " x 1.1 (P-Artemis)";
                } else if (linkedBy.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    weaponBV *= 1.3;
                    calculation += " x 1.3 (Artemis V)";
                } else if (linkedBy.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)
                        || linkedBy.getType().hasFlag(MiscType.F_APOLLO)) {
                    weaponBV *= 1.15;
                    calculation += " x 1.15 (" + (linkedBy.getType().hasFlag(MiscType.F_APOLLO) ? "Apollo)" : "RISC LPM)");
                }
            }

            if (weaponType.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                weaponBV *= 1.2;
                calculation += " x 1.2 (TC)";
            }

            offensiveValue += weaponBV;
            bvReport.addLine(weapon.getDesc(), calculation, "= " + formatForReport(offensiveValue));
        }

        processAmmoValue();

//        bvReport.addEmptyLine();
        bvReport.addLine("Structure Modifier:", formatForReport(offensiveValue) + " x 0.44",
                "= " + formatForReport(offensiveValue * 0.44));
        offensiveValue *= 0.44;
    }
}