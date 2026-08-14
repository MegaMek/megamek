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
package megamek.client.ui.settings;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import megamek.client.ui.Messages;
import megamek.common.internationalization.I18n;

/**
 * Resolves localized text for the shared settings UI without coupling it to a consumer's resource-bundle loader.
 * Implementations define where text comes from while the framework retains one missing-key and formatting contract.
 */
public interface SettingsTextProvider {
    /**
     * @param key the resource key to test
     *
     * @return {@code true} when this provider contains text for {@code key}
     */
    boolean containsKey(String key);

    /**
     * Returns localized text, or {@code !key!} when the key is missing.
     *
     * @param key the resource key to resolve
     *
     * @return the localized text or a visible missing-resource marker
     */
    String getText(String key);

    /**
     * Resolves text and, when arguments are supplied, formats it using {@link MessageFormat}.
     *
     * @param key       the resource key to resolve
     * @param arguments values substituted into the resolved pattern
     *
     * @return the formatted localized text
     */
    default String getFormattedText(String key, Object... arguments) {
        String text = getText(key);
        return arguments.length == 0 ? text : MessageFormat.format(text, arguments);
    }

    /**
     * Creates a provider backed by an arbitrary {@link ResourceBundle}.
     *
     * @param resourceBundle the bundle to resolve text from
     *
     * @return a provider for {@code resourceBundle}
     */
    static SettingsTextProvider fromResourceBundle(ResourceBundle resourceBundle) {
        Objects.requireNonNull(resourceBundle);
        return new SettingsTextProvider() {
            @Override
            public boolean containsKey(String key) {
                return resourceBundle.containsKey(key);
            }

            @Override
            public String getText(String key) {
                return containsKey(key) ? resourceBundle.getString(key) : missingResourceMarker(key);
            }
        };
    }

    /**
     * @return a provider backed by MegaMek's standard client message bundle
     */
    static SettingsTextProvider megaMek() {
        String bundleName = Messages.CLIENT_BUNDLE;
        Set<String> resourceKeys = Set.copyOf(I18n.getKeys(bundleName));
        return new SettingsTextProvider() {
            @Override
            public boolean containsKey(String key) {
                return resourceKeys.contains(key);
            }

            @Override
            public String getText(String key) {
                return I18n.getTextAt(bundleName, key);
            }

            @Override
            public String getFormattedText(String key, Object... arguments) {
                return arguments.length == 0 ? getText(key) : I18n.getFormattedTextAt(bundleName, key, arguments);
            }
        };
    }

    private static String missingResourceMarker(String key) {
        return '!' + key + '!';
    }
}
