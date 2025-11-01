/*
 * Copyright (c) 2023-2025 - The MegaMek Team. All Rights Reserved.
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


package megamek.common.units;

import megamek.common.CriticalSlot;
import megamek.common.bays.CargoBay;
import megamek.common.equipment.Mounted;

/**
 * AeroSpaceFighter subclass of Aero that encapsulates Fighter functionality
 */
public class AeroSpaceFighter extends Aero {
    public AeroSpaceFighter() {
        super();
    }

    @Override
    public int getUnitType() {
        return UnitType.AEROSPACE_FIGHTER;
    }

    @Override
    public void autoSetMaxBombPoints() {
        // Aerospace fighters can carry both external and internal ordnance's, if configured and quirked appropriately
        maxExtBombPoints = (int) Math.round(getWeight() / 5);
        // Can't check quirk here, as they don't exist in unit files yet.
        maxIntBombPoints = getTransportBays().stream()
              .filter(tb -> tb instanceof CargoBay)
              .mapToInt(tb -> (int) Math.floor(tb.getUnused()))
              .sum();
    }

    @Override
    public int reduceMPByBombLoad(int t) {
        return Math.max(0, t - (int) Math.ceil(getExternalBombPoints() / 5.0));
    }

    @Override
    public boolean isSpheroid() {
        return false;
    }

    @Override
    public boolean isBomber() {
        return true;
    }

    @Override
    public boolean isFighter() {
        return true;
    }

    @Override
    public boolean isAerospaceFighter() {
        return true;
    }

    /**
     * Method to enable mass location damaging, mainly for Fighter Squadrons
     *
     * @param loc that every fighter in the squadron needs to damage, for MekHQ tracking
     */
    public void damageLocation(int loc) {
        weaponList.stream().filter(x -> x.getLocation() == loc).forEach((weapon) -> {
            // Damage the weapon
            weapon.setHit(true);
            // Damage the critical slot
            for (int i = 0; i < getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot slot1 = getCritical(loc, i);
                if ((slot1 == null) || (slot1.getType() == CriticalSlot.TYPE_SYSTEM)) {
                    continue;
                }
                Mounted<?> mounted = slot1.getMount();
                if (mounted.equals(weapon)) {
                    hitAllCriticalSlots(loc, i);
                    break;
                }
            }
        });
    }

    @Override
    public long getEntityType() {
        return super.getEntityType() | Entity.ETYPE_AEROSPACE_FIGHTER;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.729 + 0.898 * Math.log(getWeight())));
    }

    @Override
    public void setOriginalWalkMP(int walkMP) {
        super.setOriginalWalkMP(walkMP);
        autoSetSI();
    }

    @Override
    public void setWeight(double weight) {
        super.setWeight(weight);
        autoSetMaxBombPoints();
    }

    @Override
    public boolean isCarryableObject() {
        return false;
        // TODO: Make (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_PICKING_UP_AND_THROWING_UNITS))
        //  once we implement TO:AR 88 Grappling's missed attack consequences
    }

    @Override
    public int getRecoveryTime() {
        return 60;
    }
}
