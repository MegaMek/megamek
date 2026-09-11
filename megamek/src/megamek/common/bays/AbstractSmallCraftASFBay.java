/*
 * Copyright (c) 2003-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.bays;

import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.BuildingBayDoors;

/**
 * This is a base class for the very similar ASFBay and SmallCraftBay.
 */
public abstract class AbstractSmallCraftASFBay extends UnitBay {

    private final boolean hasArts;

    AbstractSmallCraftASFBay(boolean arts) {
        hasArts = arts;
    }

    /** @return True if this bay has ARTS (Advanced Robotic Transport System, IO p.147). */
    public boolean hasARTS() {
        return hasArts;
    }

    /** Fighter and small-craft bays retain their existing type and space rules in one place. */
    protected abstract boolean canCarry(Entity unit);

    @Override
    public boolean canLoad(Entity unit, int usableDoors) {
        return usableDoors > 0 && canCarry(unit) && availableRecoverySlots() > 0;
    }

    @Override
    public boolean canLoadAt(Entity unit, megamek.common.board.Coords position, int facing) {
        var building = buildingCarrier();
        if (building == null || BuildingBayDoors.placements(building, this).isEmpty()) { return canLoad(unit); }
        return canCarry(unit) && BuildingBayDoors.usablePlacementIndices(building, this, position, facing).stream()
              .anyMatch(index -> recoverySlots != null && 2 * index + 1 < recoverySlots.size()
                    && (recoverySlots.get(2 * index) == 0 || recoverySlots.get(2 * index + 1) == 0));
    }

    @Override
    public void load(Entity unit) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not load "
                  + unit.getShortName()
                  + " into this bay. "
                  + getUnused());
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
        int used = 0;
        if (game != null) {
            used = troops.stream()
                  .map(game::getEntity)
                  .mapToInt(t -> (int) spaceForUnit(t))
                  .sum();
        }
        currentSpace = totalSpace - used;
        return currentSpace - getBayDamage();
    }

    /**
     * Recovery is different from loading in that it uses up a recovery slot load is only used in deployment phase
     */
    public void recover(Entity unit) throws IllegalArgumentException {
        var building = buildingCarrier();
        int doorIndex = building == null ? -1 : BuildingBayDoors.usablePlacementIndices(building, this).stream()
              .filter(index -> availableRecoverySlots(index) > 0).findFirst().orElse(-1);
        recover(unit, doorIndex);
    }

    /** Authored building doors keep two stable recovery slots per physical placement. */
    public void recover(Entity unit, int doorIndex) throws IllegalArgumentException {
        if (!canLoad(unit)) {
            throw new IllegalArgumentException("Can not recover "
                  + unit.getShortName()
                  + " into this bay. "
                  + getUnused());
        }

        if (doorIndex >= 0 && availableRecoverySlots(doorIndex) == 0) {
            throw new IllegalArgumentException("The selected bay door has no free recovery slot.");
        }
        load(unit);
        if (doorIndex < 0) {
            closeSingleRecoverySlot();
        } else {
            for (int index = 2 * doorIndex; index < 2 * doorIndex + 2; index++) {
                if (recoverySlots.get(index) == 0) {
                    recoverySlots.set(index, 5);
                    break;
                }
            }
        }
    }

    public void updateSlots() {
        recoverySlots.replaceAll(slot -> Math.max(0, slot - 1));
    }

    /** Sets the recovery slots to two unused slots per currently available door. */
    public void initializeRecoverySlots() {
        recoverySlots.clear();
        int count = buildingCarrier() == null ? currentDoors : doors;
        for (int i = 0; i < count; i++) {
            recoverySlots.add(0);
            recoverySlots.add(0);
        }
    }

    @Override
    public void destroyDoorNext() {
        if (buildingCarrier() != null) {
            super.destroyDoor();
            return;
        }
        if (getDoorsNext() > 0) {
            setDoorsNext(getDoorsNext() - 1);
        }
        destroyEmptyRecoverySlot();
        destroyEmptyRecoverySlot();
    }

    @Override
    public void destroyDoor() {
        if (buildingCarrier() != null) {
            super.destroyDoor();
            return;
        }
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
        var building = buildingCarrier();
        if (building != null && !BuildingBayDoors.placements(building, this).isEmpty()) {
            return BuildingBayDoors.usablePlacementIndices(building, this).stream()
                  .mapToInt(this::availableRecoverySlots).sum();
        }
        // A linked module can block a door without damaging it or resetting its recovery timer.
        int occupied = recoverySlots == null ? 0 : (int) recoverySlots.stream().filter(slot -> slot > 0).count();
        int empty = recoverySlots == null ? 0 : (int) recoverySlots.stream().filter(slot -> slot == 0).count();
        return Math.max(0, Math.min(empty, 2 * getUsableDoors() - occupied));
    }

    /** A busy or broken door does not consume the independent recovery slots of a different door. */
    public int availableRecoverySlots(int doorIndex) {
        if (doorIndex < 0) { return availableRecoverySlots(); }
        var building = buildingCarrier();
        if (building == null || recoverySlots == null || 2 * doorIndex + 1 >= recoverySlots.size()) { return 0; }
        if (!BuildingBayDoors.usablePlacementIndices(building, this).contains(doorIndex)) { return 0; }
        return (recoverySlots.get(2 * doorIndex) == 0 ? 1 : 0) + (recoverySlots.get(2 * doorIndex + 1) == 0 ? 1 : 0);
    }

    /** Preserve the surviving door's timers when equal Hangar placements are damaged in a different order. */
    public void damageRecoveryDoor(int doorIndex) {
        var building = buildingCarrier();
        if (building == null) { destroyDoor(); return; }
        var placements = BuildingBayDoors.placements(building, this);
        if (doorIndex < 0) { BuildingBayDoors.damage(building, this, null); return; }
        var usable = BuildingBayDoors.usablePlacementIndices(building, this);
        if (!usable.contains(doorIndex)) { return; }
        var door = placements.get(doorIndex);
        int canonical = usable.stream().filter(index -> placements.get(index).equals(door)).findFirst().orElseThrow();
        if (canonical != doorIndex) {
            // Damage uses a count for identical doors. Keep its canonical ordering without losing either timer.
            java.util.Collections.swap(recoverySlots, 2 * canonical, 2 * doorIndex);
            java.util.Collections.swap(recoverySlots, 2 * canonical + 1, 2 * doorIndex + 1);
        }
        BuildingBayDoors.damage(building, this, door);
    }

}
