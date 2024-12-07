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
package megamek.client.ui.mekview;

import megamek.MegaMek;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This manages the GUI strings for the MekView only.
 */
public class MekViewUiTexts {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("megamek.client.Mekview",
        MegaMek.getMMOptions().getLocale());

    /**
     * Returns the internationalized text for the given key in the resource bundle *without* throwing an exception for missing keys.
     */
    public static String uiString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException ignored) {
            return '!' + key + '!';
        }
    }
}
