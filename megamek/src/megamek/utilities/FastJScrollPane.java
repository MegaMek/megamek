/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import java.awt.Component;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import megamek.client.ui.clientGUI.GUIPreferences;

/**
 * A custom {@link JScrollPane} that automatically adjusts its scroll step size based on the current UI scale setting.
 *
 * <p>The scroll speed is set proportionally to the UI scale as provided by {@link GUIPreferences}. This ensures a
 * consistent and user-friendly scrolling experience regardless of UI scaling.</p>
 *
 * @author Weaver
 */
public class FastJScrollPane extends JScrollPane {
    static final int BASE_INCREMENT = 16;

    /**
     * Constructs a {@code FastJScrollPane} with no view component. The scroll increments are set based on the current
     * UI scale.
     */
    public FastJScrollPane() {
        super(null);
        setScaleIncrement();
    }

    /**
     * Constructs a {@code FastJScrollPane} with the specified component as the view. The scroll increments are set
     * based on the current UI scale.
     *
     * @param view the component to display in the scroll pane's viewport
     */
    public FastJScrollPane(Component view) {
        super(view);
        setScaleIncrement();
    }

    /**
     * Constructs a {@code FastJScrollPane} with the specified component as the view and with specified vertical and
     * horizontal scroll bar policies. The scroll increments are set based on the current UI scale.
     *
     * @param view      the component to display in the scroll pane's viewport
     * @param vsbPolicy an integer specifying the vertical scroll bar policy. Should be one of:
     *                  {@link ScrollPaneConstants#VERTICAL_SCROLLBAR_AS_NEEDED}, or
     *                  {@link ScrollPaneConstants #VERTICAL_SCROLLBAR_NEVER}
     * @param hsbPolicy an integer specifying the horizontal scroll bar policy. Should be one of:
     *                  {@link ScrollPaneConstants#HORIZONTAL_SCROLLBAR_AS_NEEDED}, or
     *                  {@link ScrollPaneConstants#HORIZONTAL_SCROLLBAR_NEVER}
     */
    public FastJScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
        setScaleIncrement();
    }

    /**
     * Sets the scroll unit increments for both vertical and horizontal scroll bars  according to the current UI scale
     * retrieved from {@link GUIPreferences}.
     */
    private void setScaleIncrement() {
        float scale = GUIPreferences.getInstance().getGUIScale();
        int increment = (int) (scale * BASE_INCREMENT);
        getVerticalScrollBar().setUnitIncrement(increment);
        getHorizontalScrollBar().setUnitIncrement(increment);
    }
}
