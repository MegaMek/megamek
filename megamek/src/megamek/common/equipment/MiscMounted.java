/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import megamek.common.*;

public class MiscMounted extends Mounted<MiscType> {

    // New stuff for shields
    private int baseDamageAbsorptionRate = 0;
    private int baseDamageCapacity = 0;
    private int damageTaken = 0;

    public MiscMounted(Entity entity, MiscType type) {
        super(entity, type);

        if (type.hasFlag(MiscType.F_MINE)) {
            setMineType(MINE_CONVENTIONAL);
            // Used to keep track of the # of mines
            setShotsLeft(1);
        }
        if (type.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
            setMineType(MINE_CONVENTIONAL);
            // Used to keep track of the # of mines
            setShotsLeft(2);
        }
        if (type.hasFlag(MiscType.F_SENSOR_DISPENSER)) {
            setShotsLeft(type.hasFlag(MiscType.F_BA_EQUIPMENT) ? 6 : 30);
        }
        if (((type.isShield() || type.hasFlag(MiscType.F_MODULAR_ARMOR)))) {
            baseDamageAbsorptionRate = type.getBaseDamageAbsorptionRate();
            baseDamageCapacity = type.getBaseDamageCapacity();
        }
        if (type.hasFlag(MiscType.F_MINESWEEPER)) {
            setArmorValue(30);
        }
    }

    public int getBaseDamageAbsorptionRate() {
        return baseDamageAbsorptionRate;
    }

    public int getBaseDamageCapacity() {
        return baseDamageCapacity;
    }


    /**
     * Rules state that every time the shield takes a crit its damage absorption
     * for each attack is reduced by 1. Also for every Arm actuator critted
     * damage absorption is reduced by 1 and finally if the shoulder is hit the
     * damage absorption is reduced by 2 making it possble to kill a shield
     * before its gone through its full damage capacity.
     *
     * @param entity    Entity mounted the shield
     * @param location  The shield location index
     * @return          The shield's damage absorption value
     */
    public int getDamageAbsorption(Entity entity, int location) {
        // Shields can only be used in arms so if you've got a shield in a
        // location
        // other then an arm your SOL --Torren.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return 0;
        }

        int base = baseDamageAbsorptionRate;

        for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            Mounted<?> m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                if (cs.isDamaged()) {
                    base--;
                }
            }
        }

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, location)) {
            base -= 2;
        }

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_HAND, location)) {
            base--;
        }

        return Math.max(0, base);
    }

    /**
     * Rules say every time a shield is critted it loses 5 points from its
     * Damage Capacity. basically count down from the top then subtract the
     * amount of damage its already take. The damage capacity is used to
     * determine if the shield is still viable.
     *
     * @param entity
     * @param location
     * @return damage capacity(no less then 0)
     */
    public int getCurrentDamageCapacity(Entity entity, int location) {
        // Shields can only be used in arms so if you've got a shield in a
        // location
        // other then an arm your SOL --Torren.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return 0;
        }

        int base = baseDamageCapacity;

        for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            Mounted<?> m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                if (cs.isDamaged()) {
                    base -= 5;
                }
            }
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, location)) {
            base -= 2;
        }

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_HAND, location)) {
            base--;
        }

        return Math.max(0, base - damageTaken);
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public void setDamageTaken(int damage) {
        damageTaken = damage;
    }

    public void takeDamage(int damage) {
        damageTaken += damage;
    }
}
