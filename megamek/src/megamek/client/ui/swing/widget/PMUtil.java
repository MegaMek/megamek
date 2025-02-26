/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.widget;

import java.awt.Image;
import java.awt.MediaTracker;

import javax.swing.JComponent;

import megamek.logging.MMLogger;

/**
 * Set of useful function.
 */
public final class PMUtil {
    private static final MMLogger logger = MMLogger.create(PMUtil.class);

    /**
     * Ensures that Images is completely loaded
     */
    public static boolean setImage(Image im, JComponent c) {
        MediaTracker mt = new MediaTracker(c);
        mt.addImage(im, 0);
        try {
            mt.waitForID(0);
        } catch (InterruptedException e) {
            logger.error("", e);
            return false;
        }

        if (mt.isErrorID(0)) {
            logger.warn("Could not load image");
            return false;
        }

        return true;
    }
}
