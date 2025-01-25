/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.client.ui.swing;

import javax.swing.*;

public interface IHasCurrentPanel {
    /**
     * The ClientGUI is split into the main panel (view) at the top, which takes up
     * the majority of
     * the view and the "current panel" which has different controls based on the
     * phase.
     *
     * @return the panel for the current phase
     */
    JComponent getCurrentPanel();
}
