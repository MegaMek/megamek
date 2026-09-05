package megamek.common.rules.totalwarfare;
/*
 * Copyright (C) 2004-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.battleValue.BVCalculator;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType;
import megamek.common.rules.RulesGame;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;

import java.util.EnumSet;

import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

public class TWRulesGame extends RulesGame {

    /**
     * Ammo dumping is allowed
     *
     * @return true if ammo dumping is allowed
     */
    @Override
    public boolean ammoDumping() { return true; }


    /**
     * Is an entity eligible for a phase
     * @param entity the unit being considered
     * @param phase what phase it is in
     * @return is it eligible
     */
    @Override
    public boolean eligibleForPhase(Entity entity, @Nullable GamePhase phase) {
        if (entity.isUnjammingRAC() || entity.isFindingClub()) {
            return false;
        }
        return true;
    }

    /**
     * Return the number of units to move.
     * Only do front-loaded init if the option is selected
     *
     * @param num_turns array of normal turns
     * @param index the current index
     * @param min the minimum value
     * @param frontLoadOption true if front load option is enabled
     * @return the initiative order
     */
    @Override
    public int getInitiativeOrder(int[] num_turns, int index, int min, boolean frontLoadOption) {
        return frontLoadOption ? ((int) Math.ceil(((double) num_turns[index]) / (double) min)) :
              (num_turns[index] / min);
    }

    /**
     * TAG can increase BV when Semi-guided or homing arrow IV is present
     * {@inheritDoc}
     */
    @Override
    public double tagBVBump(Entity entity, CalculationReport bvReport, double adjustedBV,
          long tagCount, boolean hasGuided) {

        for (Entity otherEntity : entity.getGame().getEntitiesVector()) {
            if ((otherEntity.getOwner() == null) || otherEntity.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            for (Mounted<?> mounted : otherEntity.getAmmo()) {
                AmmoType ammoType = (AmmoType) mounted.getType();
                EnumSet<AmmoType.Munitions> munitionType = ammoType.getMunitionType();
                if ((mounted.getUsableShotsLeft() > 0) &&
                      ((munitionType.contains(AmmoType.Munitions.M_SEMIGUIDED)) ||
                            (munitionType.contains(AmmoType.Munitions.M_HOMING)))) {
                    adjustedBV += mounted.getType().getBV(entity) * tagCount;
                    bvReport.addLine("- " + equipmentDescriptor(mounted, entity),
                          "+ " +
                                tagCount +
                                " x " +
                                formatForReport(mounted.getType().getBV(entity)) +
                                " (" +
                                otherEntity.getShortName() +
                                ")",
                          "= " + formatForReport(adjustedBV));
                    hasGuided = true;
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
     * Allow only if TO Minefields is enabled
     */
    @Override
    public boolean allowMinefields(boolean toMinefields) { return toMinefields; }
}
