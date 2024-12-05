/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common.actions;

import megamek.common.AmmoType;

import java.io.Serial;

public class NukeDetonatedAction extends AbstractEntityAction {

    @Serial
    private static final long serialVersionUID = 918785269096319255L;

    private final AmmoType.Munitions nukeType;
    private final int playerID;

    public NukeDetonatedAction(int entityId, int playerID, AmmoType.Munitions nukeType) {
        super(entityId);
        this.nukeType = nukeType;
        this.playerID = playerID;
    }

    public AmmoType.Munitions typeOfNuke() {
        return nukeType;
    }
}
