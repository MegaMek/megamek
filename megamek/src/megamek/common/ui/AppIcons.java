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
 */
package megamek.common.ui;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * Applies an application's window icons (taskbar, title bar) from the {@code <name>-icon-<size>x<size>.png} files in
 * the misc images directory. Without this, Windows shows the generic Java icon for windows of the application.
 */
public final class AppIcons {
    private static final int[] SIZES = { 16, 32, 48, 256 };

    private AppIcons() {}

    /**
     * Sets the icon images of the given frame.
     *
     * @param frame   the frame to set the icons on
     * @param appName the icon file prefix, e.g. "megameklab" or "mekhq"
     */
    public static void apply(JFrame frame, String appName) {
        applyToWindow(frame, appName);
    }

    /**
     * Sets the icon images of the given window, such as a dialog. Separately named rather than an overload of
     * {@link #apply(JFrame, String)} so that the method called never depends on the static type of the argument.
     *
     * @param window  the window to set the icons on
     * @param appName the icon file prefix, e.g. "megameklab" or "mekhq"
     */
    public static void applyToWindow(Window window, String appName) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        List<Image> icons = new ArrayList<>();
        for (int size : SIZES) {
            String fileName = "%s-icon-%dx%d.png".formatted(appName, size, size);
            icons.add(toolkit.getImage(new MegaMekFile(Configuration.miscImagesDir(), fileName).toString()));
        }
        window.setIconImages(icons);
    }
}
