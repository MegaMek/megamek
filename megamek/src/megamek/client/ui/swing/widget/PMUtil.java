/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
        } catch (InterruptedException ignored) {
            return false;
        }

        if (mt.isErrorID(0)) {
            logger.warn("Could not load image");
            return false;
        }

        return true;
    }
}
