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
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.ASArcs;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.Arrays;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASLargeAeroSpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final boolean[] hasExplosiveArcComponent = new boolean[4];

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead.
     * Constructs a special ability converter for large aerospace units.
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASLargeAeroSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        super(entity, element, report);
    }

    @Override
    protected void processENE() {
        Arrays.fill(hasExplosiveArcComponent, false);
        for (Mounted<?> equipment : entity.getEquipment()) {
            processArcENE(equipment);
            if (equipment.getType() instanceof BayWeapon) {
                var bayEquipmentList = new ArrayList<>(equipment.getBayWeapons());
                bayEquipmentList.addAll(equipment.getBayAmmo());
                for (int index : bayEquipmentList) {
                    Mounted bayEquipment = entity.getEquipment(index);
                    processArcENE(bayEquipment);
                }
            }
        }

        for (ASArcs arc : ASArcs.values()) {
            if (!hasExplosiveArcComponent[ASConverter.toInt(arc)]) {
                report.addLine("No Explosive Component", arc + "", "ENE");
                element.getArc(arc).setSUA(ENE);
            }
        }
    }

    private void processArcENE(Mounted equipment) {
        if (isExplosive(equipment)) {
            for (ASArcs arc : ASArcs.values()) {
                if (ASLocationMapper.damageLocationMultiplier(entity, ASConverter.toInt(arc), equipment) > 0) {
                    hasExplosiveArcComponent[ASConverter.toInt(arc)] = true;
                }
            }
        }
    }

    @Override
    protected void processMFB(Transporter transporter) { }

    @Override
    protected void processUnitFeatures() {
        super.processUnitFeatures();

        if (element.isType(SC, DS, DA)) {
            if (element.getSize() == 1) {
                assign("Size 1 SC/Dropship", LG);
            } else if (element.getSize() == 2) {
                assign("Size 2 SC/Dropship", VLG);
            } else {
                assign("Size 3 SC/Dropship", SLG);
            }
        }

        if (entity instanceof Jumpship) {
            if (((Jumpship) entity).getDriveCoreType() != Jumpship.DRIVE_CORE_NONE) {
                assign("KF Drive", KF);
            }
            if (((Jumpship) entity).hasLF()) {
                assign("Lithium-Fusion Battery", LF);
            }
            if (entity.getNCrew() >= 60) {
                assign("Sufficient Crew", CRW, (int) Math.round(entity.getNCrew() / 120.0));
            }
        }

        if (entity instanceof Dropship) {
            if (entity.getNCrew() >= 30) {
                assign("Sufficient Crew", CRW, (int) Math.round(entity.getNCrew() / 60.0));
            }
        }

        Aero aero = (Aero) entity;
        if (aero.isVSTOL() || aero.isSTOL() || (aero instanceof SmallCraft && aero.isAerodyne())) {
            assign("VSTOL or STOL gear or capable", VSTOL);
        }

        assign("Space capable", SPC);
    }
}