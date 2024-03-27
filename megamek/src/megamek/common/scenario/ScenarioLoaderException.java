/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.scenario;

import megamek.client.ui.swing.scenario.Messages;

import java.util.IllegalFormatException;

public class ScenarioLoaderException extends Exception {

    private final Object[] params;

    public ScenarioLoaderException(String errorKey) {
        super(errorKey);
        this.params = null;
    }

    public ScenarioLoaderException(String errorKey, Object... params) {
        super(errorKey);
        this.params = params;
    }

    @Override
    public String getMessage() {
        String result = Messages.getString(super.getMessage());
        if (params != null) {
            try {
                return String.format(result, params);
            } catch (IllegalFormatException ignored) {
                // Ignore, return the base translation instead
            }
        }
        return result;
    }
}
