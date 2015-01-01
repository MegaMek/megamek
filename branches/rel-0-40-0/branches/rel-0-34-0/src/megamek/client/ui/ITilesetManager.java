/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
 * 
 * This file (C) 2008 Jörg Walter <j.walter@syntax-k.de>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui;

import java.awt.Component;
import java.awt.Image;
import megamek.common.Entity;
import megamek.common.Player;

/**
 *
 * @author jwalt
 */
public interface ITilesetManager {

    public Image getPlayerCamo(Player player);

    public Image iconFor(Entity e);

    public Image loadPreviewImage(Entity entity, Image camo, int tint, Component bp);

}
