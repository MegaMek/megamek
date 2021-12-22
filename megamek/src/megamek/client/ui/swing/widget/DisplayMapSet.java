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

import megamek.common.Entity;

import java.util.Vector;

/**
 * Generic set of PicMap areas do represent various units in MechDisplay class
 */
public interface DisplayMapSet {
    PMAreasGroup getContentGroup();

    Vector<BackGroundDrawer> getBackgroundDrawers();

    void setEntity(Entity e);
}
