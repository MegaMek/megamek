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
package megamek.common.units;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.annotations.Nullable;

/**
 * The crane loading and unloading work in progress for one grounded Small Craft or DropShip (TW p.90-91). Kept as its own
 * state object, like {@code BridgeLayerState}, so the carrier class only holds a single field for it.
 */
public final class CraneOperations implements Serializable {

    @Serial
    private static final long serialVersionUID = -6011894219383260558L;

    private final List<CraneOperation> operations = new ArrayList<>();

    /**
     * @return the operations in progress, as a read-only view
     */
    public List<CraneOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    /**
     * Adds an operation, replacing any operation already in progress for the same unit.
     *
     * @param operation the operation to add
     */
    public void add(CraneOperation operation) {
        remove(operation.getUnitId());
        operations.add(operation);
    }

    /**
     * Removes the operation for a unit, if there is one.
     *
     * @param unitId the id of the unit whose crane operation ends
     */
    public void remove(int unitId) {
        operations.removeIf(operation -> operation.getUnitId() == unitId);
    }

    /**
     * Finds the operation for a unit.
     *
     * @param unitId the id of the unit
     *
     * @return the unit's operation, or {@code null} if the cranes are not working on it
     */
    public @Nullable CraneOperation findFor(int unitId) {
        for (CraneOperation operation : operations) {
            if (operation.getUnitId() == unitId) {
                return operation;
            }
        }
        return null;
    }

    /** @return {@code true} if the cranes are not working on any unit */
    public boolean isEmpty() {
        return operations.isEmpty();
    }
}
