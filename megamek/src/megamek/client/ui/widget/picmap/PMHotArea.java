/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget.picmap;

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;

/**
 * Generic Hot area of PicMap component
 */
public interface PMHotArea extends PMElement {
    String MOUSE_CLICK_LEFT = "mouse_click_left";
    String MOUSE_CLICK_RIGHT = "mouse_click_right";
    String MOUSE_DOUBLE_CLICK = "mouse_double_click";
    String MOUSE_OVER = "mouse_over";
    String MOUSE_EXIT = "mouse_exit";
    String MOUSE_UP = "mouse_up";
    String MOUSE_DOWN = "mouse_down";

    Cursor getCursor();

    void setCursor(Cursor c);

    Shape getAreaShape();

    void onMouseClick(MouseEvent e);

    void onMouseOver(MouseEvent e);

    void onMouseExit(MouseEvent e);

    void onMouseDown(MouseEvent e);

    void onMouseUp(MouseEvent e);
}
