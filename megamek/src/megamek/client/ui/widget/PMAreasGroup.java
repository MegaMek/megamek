/*
 * Copyright (c) 2000-2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Vector;

/**
 * PMAreasGroup allows to group handle PicMap elements as single entity.
 */
public class PMAreasGroup implements PMElement {
    private Vector<PMElement> gr = new Vector<>();

    /**
     * Adds area to group
     */
    public void addArea(PMElement ha) {
        gr.addElement(ha);
    }

    /**
     * Removes area from group
     */
    public boolean removeArea(PMElement ag) {
        return gr.removeElement(ag);
    }

    /**
     * Removes all elements from group
     */
    public void removeAll() {
        gr.removeAllElements();
    }

    /**
     * Returns element at specific index.
     */
    public PMElement elementAt(int i) {
        return gr.elementAt(i);
    }

    /**
     * Returns enumeration of all elements in group.
     */
    public Enumeration<PMElement> elements() {
        return gr.elements();
    }

    /**
     * Translates all elements in group by x, y.
     */
    @Override
    public void translate(int x, int y) {
        Enumeration<PMElement> iter = gr.elements();
        while (iter.hasMoreElements()) {
            PMElement pme = iter.nextElement();
            if (pme != null) {
                pme.translate(x, y);
            }
        }
    }

    /**
     * Sets bounding box of all elements in group at (x, y)
     */
    public void moveTo(int x, int y) {
        Rectangle r = getBounds();
        translate(x - r.x, y - r.y);
    }

    /**
     * Returns bounding box which includes all elements in group.
     */
    @Override
    public Rectangle getBounds() {
        Rectangle bounds = null;
        Enumeration<PMElement> iter = gr.elements();
        while (iter.hasMoreElements()) {
            PMElement pme = iter.nextElement();
            if ((pme != null) && (pme.getBounds() != null)) {
                if (bounds == null) {
                    bounds = pme.getBounds();
                } else {
                    bounds = bounds.union(pme.getBounds());
                }
            }
        }
        return bounds;
    }

    /**
     * Draws all elements in group into specified Graphics
     */
    @Override
    public void drawInto(Graphics g) {
        Enumeration<PMElement> iter = gr.elements();
        while (iter.hasMoreElements()) {
            PMElement pme = iter.nextElement();
            if (pme != null) {
                pme.drawInto(g);
            }
        }
    }

    /**
     * Sets visibility of all elements in the group to true or false.
     */
    @Override
    public void setVisible(boolean v) {
        Enumeration<PMElement> iter = gr.elements();
        while (iter.hasMoreElements()) {
            PMElement pme = iter.nextElement();
            pme.setVisible(v);
        }
    }
}
