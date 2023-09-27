/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

/**
 * This is a base class for the very similar ASFBay and SmallCraftBay.
 */
public abstract class AbstractSmallCraftASFBay extends Bay {

    private final boolean hasArts;

    AbstractSmallCraftASFBay(boolean arts) {
        hasArts = arts;
    }

    /** @return True if this bay has ARTS (Advanced Robotic Transport System, IO p.147). */
    public boolean hasARTS() {
        return hasArts;
    }

    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load " + unit.getShortName() + " into this bay. " + getUnused());
        }
        currentSpace -= spaceForUnit(unit);
        troops.addElement(unit.getId());
    }

    @Override
    public double spaceForUnit(Entity unit) {
        return (unit instanceof FighterSquadron) ? unit.getSubEntities().size() : 1;
    }

    @Override
    public double getUnused() {
        // loaded fighter squadrons can change size, therefore always update this
        int used = troops.stream().map(game::getEntity).mapToInt(t -> (int) spaceForUnit(t)).sum();
        currentSpace = totalSpace - used;
        return currentSpace - getBayDamage();
    }

    /**
     * Recovery is different from loading in that it uses up a recovery slot
     * load is only used in deployment phase
     */
    public void recover(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not recover " + unit.getShortName() + " into this bay. " + getUnused());
        }

        load(unit);
        closeSingleRecoverySlot();
    }

    public void updateSlots() {
        recoverySlots.replaceAll(slot -> Math.max(0, slot - 1));
    }

    /** Sets the recovery slots to two unused slots per currently available door. */
    public void initializeRecoverySlots() {
        recoverySlots.clear();
        for (int i = 0; i < currentdoors; i++) {
            recoverySlots.add(0);
            recoverySlots.add(0);
        }
    }

    @Override
    public void destroyDoorNext() {
        if (getDoorsNext() > 0) {
            setDoorsNext(getDoorsNext() - 1);
        }
        destroyEmptyRecoverySlot();
        destroyEmptyRecoverySlot();
    }

    @Override
    public void destroyDoor() {
        if (getCurrentDoors() > 0) {
            setCurrentDoors(getCurrentDoors() - 1);
        }
        destroyEmptyRecoverySlot();
        destroyEmptyRecoverySlot();
    }

    protected void closeSingleRecoverySlot() {
        for (int i = 0; i < recoverySlots.size(); i++) {
            if (recoverySlots.get(i) == 0) {
                recoverySlots.remove(i);
                recoverySlots.add(5);
                break;
            }
        }
    }

    protected void destroyEmptyRecoverySlot() {
        recoverySlots.remove((Integer) 0);
    }

    protected int availableRecoverySlots() {
        return (recoverySlots == null) ? 0 : (int) recoverySlots.stream().filter(slot -> slot == 0).count();
    }

}
