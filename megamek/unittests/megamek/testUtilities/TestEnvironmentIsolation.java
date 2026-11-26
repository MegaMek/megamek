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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

import megamek.common.preference.PreferenceManager;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Cuts the unit tests off from everything on the developer's machine that would otherwise change what they see.
 *
 * <p>
 * The tests assert on defaults, so the language, display options, the user directory and so on must be the same
 * on every machine. This is registered with the JUnit Platform through {@code META-INF/services} in
 * {@code testresources}, so it applies however the tests are launched (Gradle or an IDE), with no build script or run
 * configuration to keep in step.
 * </p>
 *
 * <ul>
 *   <li><b>Client settings</b> ({@code mmconf/clientsettings.xml}: language, user directory, GUI settings) - the
 *   {@value PreferenceManager#CFG_FILE_OPTION_NAME} system property is pointed at a file in a fresh temporary
 *   directory, so the defaults load, and anything a test saves lands there instead of in the developer's file.</li>
 *   <li><b>JVM locale</b> - set to {@link Locale#US}, so number and date formatting does not depend on the operating
 *   system's region. MegaMek sets the same format locale at startup, which the tests never run.</li>
 *   <li><b>{@code java.util.prefs}</b> ({@code MMOptions}) - replaced by {@link InMemoryPreferencesFactory}, and
 *   emptied before each top-level test class so options set by one class cannot leak into the next.</li>
 * </ul>
 *
 * <p>
 * The session hook runs before any test class is loaded, which matters: {@link PreferenceManager} reads its settings
 * file once, in a static initializer.
 * </p>
 *
 * <p>
 * This must remain {@code public} with a public no-argument constructor, as {@link java.util.ServiceLoader} requires.
 * </p>
 */
public final class TestEnvironmentIsolation implements LauncherSessionListener, TestExecutionListener {
    /** The client settings file name, kept the same as the real one so a saved file is easy to recognise. */
    private static final String CLIENT_SETTINGS_FILE_NAME = "clientsettings.xml";

    /** Some launchers open a session per request, so the settings directory is created once per JVM. */
    private static Path settingsDirectory;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        isolate();
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (isTopLevelClass(testIdentifier)) {
            InMemoryPreferencesFactory.clearAll();
        }
    }

    private static synchronized void isolate() {
        Locale.setDefault(Locale.US);

        if (settingsDirectory == null) {
            try {
                settingsDirectory = Files.createTempDirectory("megamek-test-settings");
            } catch (IOException ex) {
                throw new UncheckedIOException("Could not create the test client settings directory", ex);
            }
            Runtime.getRuntime()
                  .addShutdownHook(new Thread(() -> deleteRecursively(settingsDirectory), "test-settings-cleanup"));
        }
        System.setProperty(PreferenceManager.CFG_FILE_OPTION_NAME,
              settingsDirectory.resolve(CLIENT_SETTINGS_FILE_NAME).toString());
    }

    /**
     * A nested class shares its outer class's lifecycle ({@code @BeforeAll} set-up included), so only a top-level class
     * marks a clean boundary.
     */
    private static boolean isTopLevelClass(TestIdentifier testIdentifier) {
        return testIdentifier.getSource()
                     .filter(ClassSource.class::isInstance)
                     .map(source -> ((ClassSource) source).getClassName().indexOf('$') < 0)
                     .orElse(false);
    }

    private static void deleteRecursively(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // Best effort: it is a temporary directory.
        }
    }
}
