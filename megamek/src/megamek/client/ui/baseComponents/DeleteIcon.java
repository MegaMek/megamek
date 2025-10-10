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
package megamek.client.ui.baseComponents;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics2D;

import com.formdev.flatlaf.icons.FlatAbstractIcon;
import megamek.client.ui.clientGUI.GUIPreferences;

/**
 * An icon showing an x in the GUIPreferences warning color for a "delete" button
 */
public class DeleteIcon extends FlatAbstractIcon {
    private final int size;

    /**
     * Creates a delete icon at the standard size (16) suitable to place it in line with, e.g. a JTextfield and with a
     * GUIPreferences warning color.
     */
    public DeleteIcon() {
        this(16);
    }

    /**
     * Creates a books icon of the given size and with the GUIPreferences warning color.
     */
    public DeleteIcon(int size) {
        super(size, size, GUIPreferences.getInstance().getWarningColor());
        this.size = size;
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g) {
        float relativeSize = 0.8f;
        int lowerRight = (int) (size * relativeSize);
        int upperLeft = size - lowerRight;
        g.setStroke(new BasicStroke(size * 0.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
        g.drawLine(upperLeft, upperLeft, lowerRight, lowerRight);
        g.drawLine(upperLeft, lowerRight, lowerRight, upperLeft);
    }
}
