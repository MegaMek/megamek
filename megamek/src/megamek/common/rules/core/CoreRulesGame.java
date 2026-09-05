package megamek.common.rules.core;
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


import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.equipment.enums.BombType;
import megamek.common.rules.RulesGame;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;

import java.util.EnumSet;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

public class CoreRulesGame extends RulesGame {

    /**
     * {@inheritDoc}
     * Ammo dumping is not in Core
     */
    @Override
    public boolean ammoDumping() {
        return false;
    }

    /**
     * {@inheritDoc}
     * Immobile not eligible in movement Core p.49
     * RAC Unjamming does not prevent usage (only limits movement) Core p.183
     * Finding a club can use in physical phase Core p.79
     */
    @Override
    public boolean eligibleForPhase(Entity entity, @Nullable GamePhase phase) {
        if (phase != null) {
            if (entity.isImmobile() && phase.isMovement()) {
                return false;
            }
            if (phase.isPhysical() && (entity.isCharging() || entity.isMakingDfa())) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * Front loaded initiative Core p.41
     */
    @Override
    public int getInitiativeOrder(int[] num_turns, int index, int min, boolean frontLoadOption) {
        return ((int) Math.ceil(((double) num_turns[index]) / (double) min));
    }

    /**
     * TAG can increase BV when homing arrow IV is present
     * {@inheritDoc}
     * 
     * Core rules errata v0.1
     * https://www.battletech.com/forums/index.php/topic,91365.0.html
     */
    @Override
    public double tagBVBump(Entity entity, CalculationReport bvReport, double adjustedBV,
          long tagCount, boolean hasGuided) {
        for (Entity otherEntity : entity.getGame().getEntitiesVector()) {
            if ((otherEntity.getOwner() == null) || otherEntity.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            for (Mounted<?> mounted : otherEntity.getWeaponList()) {
                boolean foundHoming = false;
                if (mounted.getType().hasFlag(WeaponTypeFlag.F_ARROW_IV) || mounted.getType().hasFlag(WeaponTypeFlag.F_ARTILLERY)) {
                    for (Mounted<?> mountedAmmo : otherEntity.getAmmo()) {
                        AmmoType ammoType = (AmmoType) mountedAmmo.getType();
                        EnumSet<AmmoType.Munitions> munitionType = ammoType.getMunitionType();
                        if ((mountedAmmo.getUsableShotsLeft() > 0) &&
                              (munitionType.contains(AmmoType.Munitions.M_HOMING))) {
                            // Once we know it has homing ammo on this unit, we can break out
                            foundHoming = true;
                            break;
                        }
                    }
                    if (foundHoming) {
                        // Each Arrow IV or artillery launcher with homing ammo adds 50 BV per TAG in the force
                        adjustedBV += 50 * tagCount;
                        bvReport.addLine("- " + equipmentDescriptor(mounted, entity),
                              "+ " +
                                    tagCount +
                                    " x " +
                                    formatForReport(50) +
                                    " (" +
                                    otherEntity.getShortName() +
                                    ")",
                              "= " + formatForReport(adjustedBV));
                        hasGuided = true;
                    }
                }
            }
            
            if (otherEntity instanceof IBomber asBomber) {
                BombType bomb = BombType.createBombByType(BombType.BombTypeEnum.HOMING);
                if (bomb != null) {
                    int homingCount = asBomber.getBombChoices().getCount(BombType.BombTypeEnum.HOMING);
                    if (homingCount > 0) {
                        adjustedBV += bomb.getBV(otherEntity) * homingCount * tagCount;
                        bvReport.addLine("- " + bomb.getName(),
                              "+ " +
                                    tagCount +
                                    " x " +
                                    formatForReport(bomb.getBV(otherEntity)) +
                                    " (" +
                                    otherEntity.getShortName() +
                                    ")",
                              "= " + formatForReport(adjustedBV));
                        hasGuided = true;
                    }
                }
            }
        }
        return adjustedBV;
    }

    /**
     * {@inheritDoc}
     * Core rules allows minefields
     */
    @Override
    public boolean allowMinefields(boolean toMinefields) { return true; }
}
