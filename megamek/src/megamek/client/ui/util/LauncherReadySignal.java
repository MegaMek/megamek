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

package megamek.client.ui.util;

import java.awt.AWTEvent;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import megamek.logging.MMLogger;

/**
 * Tells the MegaMek Launcher that this application's first window is on screen, so the launcher can step aside.
 * <p>
 * The launcher starts applications whose jar manifest carries {@code MegaMek-Launcher-Ready: 1} with the system
 * property {@value #READY_FILE_PROPERTY} naming a file that does not exist yet. Once the first window of any kind
 * opens, the application creates that file, empty. Without the property, as when started any other way, nothing
 * happens. Failures are ignored: the launcher stops waiting on its own.
 */
public final class LauncherReadySignal {
    /** The system property naming the file to create once the first window is on screen. */
    public static final String READY_FILE_PROPERTY = "megamek.launcher.readyFile";

    private static final MMLogger LOGGER = MMLogger.create(LauncherReadySignal.class);
    private static final AtomicBoolean installed = new AtomicBoolean();

    private LauncherReadySignal() {
    }

    /**
     * Creates the launcher's ready file when the first window opens, if the launcher asked for one. Safe to call more
     * than once; only the first call has any effect.
     */
    public static void install() {
        String readyFile = System.getProperty(READY_FILE_PROPERTY);
        if (readyFile == null || readyFile.isBlank() || GraphicsEnvironment.isHeadless()
              || !installed.compareAndSet(false, true)) {
            return;
        }
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        listen(readyFile, toolkit::addAWTEventListener, toolkit::removeAWTEventListener);
    }

    /**
     * Registers, through {@code add}, a window-event listener that removes itself through {@code remove} and creates
     * {@code readyFile} when the first window opens.
     */
    static void listen(String readyFile, BiConsumer<AWTEventListener, Long> add, Consumer<AWTEventListener> remove) {
        add.accept(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event.getID() == WindowEvent.WINDOW_OPENED) {
                    remove.accept(this);
                    signal(readyFile);
                }
            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    /** Creates the empty file at {@code readyFile}; a file that cannot be created is only logged. */
    static void signal(String readyFile) {
        try {
            Files.createFile(Path.of(readyFile));
        } catch (IOException | RuntimeException exception) {
            // Expected when the launcher has gone or the file exists; the launcher stops waiting on its own.
            LOGGER.debug("Could not tell the launcher that the first window is on screen: {}", exception.toString());
        }
    }
}
