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
package megamek.common.planetaryconditions;

import megamek.common.Messages;

public enum EMI {
    EMI_NONE("EMI_NONE", "PlanetaryConditions.DisplayableName.EMI.false", "\u2312"),
    EMI("EMI", "PlanetaryConditions.DisplayableName.EMI.true", "\u2301");
    private final String externalId;
    private final String name;
    private final String indicator;

    EMI(final String externalId, final String name, final String indicator) {
        this.externalId = externalId;
        this.name = name;
        this.indicator = indicator;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return Messages.getString(name);
    }

    public boolean isEMINone() {
        return this == EMI_NONE;
    }

    public boolean isEMI() {
        return this == EMI;
    }

    public static EMI getEMI(int i) {
        return EMI.values()[i];
    }

    public static EMI getEMI(String s) {
        for (EMI condition : EMI.values()) {
            if (condition.getExternalId().equals(s)) {
                return condition;
            }
        }
        return EMI.EMI_NONE;
    }
}
