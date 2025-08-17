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

package megamek.common.alphaStrike.conversion;

import static megamek.common.alphaStrike.ASUnitType.DA;
import static megamek.common.alphaStrike.ASUnitType.DS;
import static megamek.common.alphaStrike.ASUnitType.SC;
import static megamek.common.alphaStrike.BattleForceSUA.CRW;
import static megamek.common.alphaStrike.BattleForceSUA.ENE;
import static megamek.common.alphaStrike.BattleForceSUA.KF;
import static megamek.common.alphaStrike.BattleForceSUA.LF;
import static megamek.common.alphaStrike.BattleForceSUA.LG;
import static megamek.common.alphaStrike.BattleForceSUA.SLG;
import static megamek.common.alphaStrike.BattleForceSUA.SPC;
import static megamek.common.alphaStrike.BattleForceSUA.VLG;
import static megamek.common.alphaStrike.BattleForceSUA.VSTOL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.equipment.Mounted;
import megamek.common.units.SmallCraft;
import megamek.common.alphaStrike.ASArcs;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.bayweapons.BayWeapon;

public class ASLargeAeroSpecialAbilityConverter extends ASSpecialAbilityConverter {

    private final boolean[] hasExplosiveArcComponent = new boolean[4];

    /**
     * Do not call this directly. Use ASSpecialAbilityConverter.getConverter instead. Constructs a special ability
     * converter for large aerospace units.
     *
     * @param entity  The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report  The calculation report to write to
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
                List<Mounted<?>> bayEquipmentList = new ArrayList<>(((WeaponMounted) equipment).getBayWeapons());
                bayEquipmentList.addAll(((WeaponMounted) equipment).getBayAmmo());
                for (Mounted<?> bayEquipment : bayEquipmentList) {
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

    private void processArcENE(Mounted<?> equipment) {
        if (isExplosive(equipment)) {
            for (ASArcs arc : ASArcs.values()) {
                if (ASLocationMapper.damageLocationMultiplier(entity, ASConverter.toInt(arc), equipment) > 0) {
                    hasExplosiveArcComponent[ASConverter.toInt(arc)] = true;
                }
            }
        }
    }

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
