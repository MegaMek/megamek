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
package megamek.testUtilities;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * A {@link PreferencesFactory} that keeps every {@link Preferences} node in memory, so the unit tests never read or
 * write the developer's real preferences.
 *
 * <p>
 * Without this, {@code MMOptions} (via {@code SuiteOptions}) reads the real per-user store (a file under the home
 * directory on Linux, a property list on macOS, the registry on Windows), and any non-default option a developer has
 * set (a display option, for example) changes what the tests see. The JDK's own {@code java.util.prefs.userRoot}
 * switch is not an option: it is honoured only by the Linux/Unix implementation, and ignored on macOS and Windows.
 * </p>
 *
 * <p>
 * It is picked up automatically through {@code testresources/META-INF/services/java.util.prefs.PreferencesFactory},
 * which the JDK consults before its platform default. That means it applies to Gradle and to tests run from an IDE
 * alike, with no JVM argument to remember. A fresh JVM always starts empty, and {@link TestEnvironmentIsolation}
 * empties it again before each top-level test class, so every class begins from the option defaults.
 * </p>
 *
 * <p>
 * This must remain {@code public} with a public no-argument constructor, as {@link java.util.ServiceLoader} requires.
 * </p>
 */
public final class InMemoryPreferencesFactory implements PreferencesFactory {
    private final Preferences userRoot = new InMemoryPreferences(null, "");
    private final Preferences systemRoot = new InMemoryPreferences(null, "");

    @Override
    public Preferences userRoot() {
        return userRoot;
    }

    @Override
    public Preferences systemRoot() {
        return systemRoot;
    }

    /**
     * Empties the user and system stores, so the next test starts from the option defaults.
     *
     * <p>
     * Does nothing unless both roots are in-memory nodes: if this factory was not picked up, they are the developer's
     * real store, which must never be wiped.
     * </p>
     */
    public static void clearAll() {
        clear(Preferences.userRoot());
        clear(Preferences.systemRoot());
    }

    private static void clear(Preferences root) {
        if (!(root instanceof InMemoryPreferences)) {
            return;
        }
        try {
            for (String childName : root.childrenNames()) {
                root.node(childName).removeNode();
            }
            root.clear();
        } catch (BackingStoreException ex) {
            // Unreachable: an in-memory node has no backing store to fail.
            throw new IllegalStateException(ex);
        }
    }

    /** A node whose values live in a map. Child nodes are cached and tracked by {@link AbstractPreferences} itself. */
    private static final class InMemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();

        InMemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
            newNode = true;
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi() {
            // Live children are held in AbstractPreferences' own cache, and nothing is ever persisted.
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return new InMemoryPreferences(this, name);
        }

        @Override
        protected void syncSpi() {
            // Nothing to synchronize with: there is no backing store.
        }

        @Override
        protected void flushSpi() {
            // Nothing to flush: there is no backing store.
        }
    }
}
