/*
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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
 * EquipmentType.java
 *
 * Created on April 1, 2002, 1:35 PM
 */

package megamek.common.equip;

import megamek.common.*;

/**
 * ########################################################
 *
 * @author  Dave
 * @version 
 */
public class EquipmentPendingState extends EquipmentState implements RoundUpdated {
    protected int pending_mode;

    public EquipmentPendingState(Mounted location, EquipmentType type) {
  super(location,type);
  pending_mode = mode;
    }

    public String curMode() {
  return type.getModes()[pending_mode];
    }

    public int switchMode() {
        if (type.hasModes()) {
            int nMode = 0;
      nMode = (pending_mode + 1) % type.getModes().length;
            setMode(nMode);
            return nMode;
        }
        return -1;
    }

    public void setMode(int n) {
        if (type.hasModes()) {
      pending_mode = n;
  }
    }

    public void newRound(int roundNumber) {
  mode = pending_mode;
    }

}
