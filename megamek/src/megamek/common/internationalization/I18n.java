/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.internationalization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.text.Transliterator;
import megamek.MegaMek;

/**
 * Class to handle internationalization (you will find online material on that looking for i18n) It makes use of some
 * short names to make it easier to use since it is used in many places
 */
public class I18n {

    private final String defaultBundle;
    private final ConcurrentHashMap<String, ResourceBundle> resourceBundles = new ConcurrentHashMap<>();
    protected static volatile I18n instance;

    static {
        instance = new I18n("megamek.common.messages");
    }

    protected I18n(String defaultBundle) {
        this.defaultBundle = defaultBundle;
    }

    public static I18n getInstance() {
        return instance;
    }

    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
              boolean reload)
              throws IOException {
            // The below is one approach; there are multiple ways to do this
            String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            InputStream is = loader.getResourceAsStream(resourceName);
            if (is == null) {
                return null; // Resource not found, let ResourceBundle try fallback
            }
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(isr);
            } finally {
                is.close();
            }
        }
    }

    ResourceBundle getResourceBundle(String bundleName) {
        return resourceBundles.computeIfAbsent(bundleName, k ->
              ResourceBundle.getBundle(bundleName, MegaMek.getMMOptions().getLocale(), new UTF8Control()));
    }

    /**
     * Get a localized string from a specific bundle
     *
     * @param bundleName the name of the bundle
     * @param key        the key of the string
     *
     * @return the localized string
     */
    public static String getTextAt(String bundleName, String key) {
        if (I18n.getInstance().getResourceBundle(bundleName).containsKey(key)) {
            return I18n.getInstance().getResourceBundle(bundleName).getString(key);
        }
        return "!" + key + "!";
    }

    /**
     * Get the keys of all localized strings in a specific bundle
     *
     * @param bundleName the name of the bundle
     *
     * @return the set of keys with localization
     */
    public static Set<String> getKeys(String bundleName) {
        return I18n.getInstance().getResourceBundle(bundleName).keySet();
    }

    /**
     * Get a localized string from the default bundle
     *
     * @param key the key of the string
     *
     * @return the localized string
     */
    public static String getText(String key) {
        return getTextAt(getInstance().defaultBundle, key);
    }

    /**
     * Get a formatted localized string from the default bundle
     *
     * @param key  the key of the string
     * @param args the arguments to format the string
     *
     * @return the localized string
     */
    public static String getFormattedText(String key, Object... args) {
        return MessageFormat.format(getFormattedTextAt(getInstance().defaultBundle, key), args);
    }

    /**
     * Get a formatted localized string from the default bundle
     *
     * @param bundleName the name of the bundle
     * @param key        the key of the string
     * @param args       the arguments to format the string
     *
     * @return the localized string
     */
    public static String getFormattedTextAt(String bundleName, String key, Object... args) {
        return MessageFormat.format(getTextAt(bundleName, key), args);
    }

    // Only handles Latin characters like ø.
    // Characters from other scripts will be left unchanged.
    // This is probably unnecessary at this time, but if it becomes relevant, replace "Latin-ASCII" with "Any-Latin;
    // Latin-ASCII" to attempt to convert other scripts to ASCII.
    // The Any-Latin transliteration will attempt phonetic transliteration based on the most likely pronunciation for
    // the given characters,
    private static final Transliterator normalizer = Transliterator.getInstance("Latin-ASCII");

    private static final ConcurrentHashMap<String, String> normalizationCache = new ConcurrentHashMap<>();

    /**
     * Takes a string of Unicode text and attempts to convert it to an ASCII representation of that string. Characters
     * such as ø and ö will be converted to o.
     *
     * @param text  A String, such as <i>Gún</i> or <i>Götterdämmerung</i>
     * @param cache Set to try to cache the result. The memoization cache can grow indefinitely, so care should be taken
     *              to not fill the cache with strings that might never be referenced again. For example, strings typed
     *              by the user shouldn't be cached, but unit names should be.
     *
     * @return The normalized String, such as <i>Gun</i> or <i>Götterdämmerung</i>.<br/> The returned string is
     *       <i>not</i> guaranteed to be only ASCII. Normalization will fail if there's no direct mapping from a
     *       character to its ASCII equivalent.
     */
    public static String normalizeTextToASCII(String text, boolean cache) {
        return cache
              ? normalizationCache.computeIfAbsent(text, normalizer::transliterate)
              : normalizer.transliterate(text);
    }
}
