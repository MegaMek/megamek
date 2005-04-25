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
 * AmmoState
 *
 * 
 */

package megamek.common.equip;

import megamek.common.*;

/**
 * ########################################
 *
 * @author  Dave
 * @version 
 */

public class AmmoState extends EquipmentState implements AmmoBin,RoundUpdated {
 
    protected int shots;
    protected boolean pending_dump = false;
    protected boolean dumping = false; 

    public AmmoState(Mounted location, AmmoType type) {
  super(location,type);
  // Default the number of shots to the normal number
  this.shots = type.getShots();
    }

    public AmmoState(Mounted location, AmmoType type, int shots) {
  super(location,type);
  this.shots = shots;
    }

    public void depleteAmmo() {
  shots--;
    }
    public int shotsLeft() {
  return shots;
    }

    public AmmoType getAmmoType() {
  return (AmmoType) type;
    }
  
    public boolean isPendingDump() {
  return pending_dump;
    }

    public void setPendingDump(boolean b) {
  this.pending_dump = b;
    }

    public boolean isDumping () {
  return dumping;
    }
    
    // If a dump was pending, set dumping and clear pending.
    // If a dump has completed, clear the shots, clear both flags
    public void newRound(int roundNumber) {
      if (dumping) {
      shots = 0;
      dumping = false;
      pending_dump = false;
  }
  if (pending_dump) {
      dumping = true;
      pending_dump = false;
  }
    }

}
