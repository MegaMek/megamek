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
package megamek.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents individual modifiers for a {@link TargetRoll}. Each modifier has a value, the
 * actual modifier like +1 or -2 or one of the finalizers like {@link TargetRoll#AUTOMATIC_FAIL},
 * a description (attacker jumped) and indicates if it is cumulative.
 * Note: This class is immutable.
 */
public class TargetRollModifier implements Serializable {

    private final int value;
    private final String description;
    private final boolean cumulative;

    public TargetRollModifier(int value, String description) {
        this(value, description, true);
    }

    public TargetRollModifier(int value, String description, boolean cumulative) {
        this.value = value;
        this.description = description;
        this.cumulative = cumulative;
    }

    /**
     * @return The modifier value like +1 or -2 or {@link TargetRoll#AUTOMATIC_FAIL}
     */
    public int getValue() {
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
    public boolean isCumulative() {
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
        return (value == that.value) && (cumulative == that.cumulative) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, description, cumulative);
    }
}
