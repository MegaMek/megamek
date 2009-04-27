/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
/*
 * IMapSettingsObserver.java
 *
 * Created on 25 May 2005, 21:17
 */

package megamek.client.ui.AWT;

import megamek.common.MapSettings;

/**
 * @author ShaneK
 */
public interface IMapSettingsObserver {
    public abstract void updateMapSettings(MapSettings mapSettings);
}
