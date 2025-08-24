/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.game.InGameObject;

/**
 * This class is a record of pair of a (InGameObject) unit and a (Targetable) target. Note that two such pairs are equal
 * when their contents count as equal which is when the IDs of unit and target are equal. This object itself is
 * immutable although the underlying objects are not.
 */
public record UnitTargetPair(InGameObject unit, Targetable target) {

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        } else if ((null == o) || (getClass() != o.getClass())) {
            return false;
        } else {
            final UnitTargetPair other = (UnitTargetPair) o;
            return Objects.equals(unit, other.unit) && Objects.equals(target, other.target);
        }
    }

}
