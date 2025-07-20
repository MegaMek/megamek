/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.unitreadout;

import megamek.client.ui.Messages;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.EntityWeightClass;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.weapons.infantry.InfantryWeapon;

import java.util.List;

class TankReadout extends GeneralEntityReadout {

    private final Tank tank;

    protected TankReadout(Tank tank, boolean showDetail, boolean useAlternateCost, boolean ignorePilotBV,
          ViewFormatting formatting) {

        super(tank, showDetail, useAlternateCost, ignorePilotBV, formatting);
        this.tank = tank;
    }

    @Override
    protected String createMovementString() {
        StringBuilder moveString = new StringBuilder();

        moveString.append(" (")
              .append(Messages.getString("MovementType." + tank.getMovementModeAsString()))
              .append(")");

        if ((tank.getMotiveDamage() > 0) || (tank.getMotivePenalty() > 0)) {
            String penalty = "(motive damage: -%dMP/-%d piloting)"
                  .formatted(tank.getMotiveDamage(), tank.getMotivePenalty());

            moveString.append(" ")
                  .append(ViewElement.warningStart(formatting))
                  .append(penalty)
                  .append(ViewElement.warningEnd(formatting));
        }
        return super.createMovementString() + moveString;
    }

    @Override
    protected List<ViewElement> getMisc() {
        List<ViewElement> result = super.getMisc();
        if (tank.getExtraCrewSeats() > 0) {
            result.add(new PlainLine(Messages.getString("MekView.ExtraCrewSeats") + tank.getExtraCrewSeats()));
        }
        return result;
    }

    @Override
    protected boolean skipArmorLocation(int location) {
        // Skip nonexistent turrets by vehicle type, as well as the body location.
        return super.skipArmorLocation(location) || (location == Tank.LOC_BODY)
              || ((location == tank.getLocTurret()) && tank.hasNoTurret());
    }

    @Override
    protected TableElement getAmmo() {
        TableElement ammoTable = super.getAmmo();
        if (tank.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            for (Mounted<?> mounted : tank.getWeaponList()) {
                String[] row = { mounted.getName(),
                                 tank.getLocationAbbr(mounted.getLocation()),
                                 String.valueOf((int) mounted.getSize()
                                       * ((InfantryWeapon) mounted.getType()).getShots()),
                                 "" };
                if (tank.isOmni()) {
                    row[3] = mounted.isOmniPodMounted() ? Messages.getString("MekView.Pod")
                          : Messages.getString("MekView.Fixed");
                }
                int shotsLeft = 0;
                for (Mounted<?> current = mounted.getLinked(); current != null; current = current.getLinked()) {
                    shotsLeft += current.getUsableShotsLeft();
                }
                if (mounted.isDestroyed()) {
                    ammoTable.addRowWithColor("red", row);
                } else if (shotsLeft < 1) {
                    ammoTable.addRowWithColor("yellow", row);
                } else {
                    ammoTable.addRow(row);
                }
            }
        }
        return ammoTable;
    }

    @Override
    protected List<ViewElement> createSpecialMiscElements() {
        return ReadoutUtils.createChassisModList(tank);
    }

    @Override
    protected List<ViewElement> getWeapons(boolean showDetail) {
        return ReadoutUtils.getWeaponsNoHeat(tank, showDetail, formatting);
    }
}
