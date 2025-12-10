/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.io.Serializable;
import java.util.Objects;

import megamek.common.rolls.TargetRoll;

/**
 * This class represents individual modifiers for a {@link TargetRoll}. Each modifier has a value, the actual modifier
 * like +1 or -2 or one of the finalizers like {@link TargetRoll#AUTOMATIC_FAIL}, a description (attacker jumped) and
 * indicates if it is cumulative. Note: This class is immutable.
 *
 * <p><b>WARNING:</b> it is important to note that this is a modifier to the <b>Target Number</b> and <i>not</i>
 * the roll. That means that a {@code positive number} is bad and a {@code negative number} is good.</p>
 */
public record TargetRollModifier(int value, String description, boolean cumulative) implements Serializable {

    /**
     * Constructs a {@link TargetRollModifier} with the specified value and description.
     *
     * <p><b>WARNING:</b> it is important to note that this is a modifier to the <b>Target Number</b> and <i>not</i>
     * the roll. That means that a {@code positive number} is bad and a {@code negative number} is good.</p>
     *
     * @param value       the numeric value of the modifier
     * @param description a description of the modifier's purpose or source
     */
    public TargetRollModifier(int value, String description) {
        this(value, description, true);
    }

    /**
     * @return The modifier value like +1 or -2 or {@link TargetRoll#AUTOMATIC_FAIL}
     */
    @Override
    public int value() {
        return value;
    }

    /**
     * @return The modifier description (attacker jumped)
     */
    public String getDesc() {
        return description;
    }

    /**
     * @return True when this modifier is cumulative
     */
    @Override
    public boolean cumulative() {
        return cumulative;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TargetRollModifier that = (TargetRollModifier) o;
        return (value == that.value) && (cumulative == that.cumulative) && Objects.equals(description,
              that.description);
    }

}
