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
package megamek.common.modifiers;

/**
 * This is a base class for EquipmentModifiers that deals with part of the implementation requirements.
 */
public abstract class AbstractEquipmentModifier implements EquipmentModifier {

    private final Reason reason;

    public AbstractEquipmentModifier(Reason reason) {
        this.reason = reason;
    }

    @Override
    public Reason reason() {
        return reason;
    }

    /**
     * @return Returns the given number as a String with a leading "+" if it is positive, i.e. "+2" or "-1" or "0".
     */
    protected String formattedModifier(int modifier) {
        return (modifier > 0 ? "+" : "") + modifier;
    }
}
