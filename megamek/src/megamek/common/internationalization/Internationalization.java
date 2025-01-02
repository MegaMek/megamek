/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.internationalization;

import megamek.MegaMek;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to handle internationalization (you will find online material on that looking for i18n)
 * It makes use of some short names to make it easier to use since it is used in many places
 */
public class Internationalization {

    private final String prefix;
    protected static final String DEFAULT = "messages";
    private final ConcurrentHashMap<String, ResourceBundle> resourceBundles = new ConcurrentHashMap<>();
    private static Internationalization instance;

    {
        instance = new Internationalization();
    }

    protected Internationalization() {
        prefix = "megamek.client.";
    }

    protected Internationalization(String prefix) {
        this.prefix = prefix;
    }

    public static Internationalization getInstance() {
        return instance;
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IOException {
            // The below is one approach; there are multiple ways to do this
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            try (InputStream is = loader.getResourceAsStream(resourceName);
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(isr);
            }
        }
    }

    ResourceBundle getResourceBundle(String bundleName) {
        return resourceBundles.computeIfAbsent(bundleName, k ->
            ResourceBundle.getBundle(prefix + bundleName, MegaMek.getMMOptions().getLocale(), new UTF8Control()));
    }

    /**
     * Get a localized string from a specific bundle
     * @param bundleName the name of the bundle
     * @param key the key of the string
     * @return the localized string
     */
    public static String getTextAt(String bundleName, String key) {
        if (Internationalization.getInstance().getResourceBundle(bundleName).containsKey(key)) {
            return Internationalization.getInstance().getResourceBundle(bundleName).getString(key);
        }
        return "!" + key + "!";
    }

    /**
     * Get a localized string from the default bundle
     * @param key the key of the string
     * @return the localized string
     */
    public static String getText(String key) {
        return getTextAt(DEFAULT, key);
    }

    /**
     * Get a formatted localized string from the default bundle
     * @param key the key of the string
     * @param args the arguments to format the string
     * @return the localized string
     */
    public static String getFormattedText(String key, Object... args) {
        return MessageFormat.format(getFormattedTextAt(DEFAULT, key), args);
    }

    /**
     * Get a formatted localized string from the default bundle
     * @param bundleName the name of the bundle
     * @param key the key of the string
     * @param args the arguments to format the string
     * @return the localized string
     */
    public static String getFormattedTextAt(String bundleName, String key, Object... args) {
        return MessageFormat.format(getTextAt(bundleName, key), args);
    }


}
