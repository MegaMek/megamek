/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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
import megamek.*;
import megamek.common.*;
import java.io.Serializable;
import java.util.*;
/**
 *
 * ArtilleryAttackAction--not *actually* an action, but here's just as good as anywhere.  Holds the data needed for an artillery attack in flight.
 */
public class ArtilleryAttackAction
implements Serializable
{
    private WeaponResult wr;
    public int turnsTilHit;
    private Vector spotters;
    public ArtilleryAttackAction(WeaponResult wr,Game game,Vector spotters) {
        this.wr=wr;
        this.spotters=spotters;//possible spotters, won't know until it lands.
        int distance=Compute.effectiveDistance(game, wr.waa.getEntity(game),wr.waa.getTarget(game));//get distance
        turnsTilHit=(distance<=17)? 0 : ((distance/34)+1);  //Two board increments are flight time, UNLESS it's on the same sheet.
    }
      public void setWR(WeaponResult wr) {
        this.wr=wr;
    }
    public WeaponResult getWR() {
        return wr;
    }
    public Vector getSpotters() {
        return spotters;
    }
    public void setSpotters(Vector spotters) {
        this.spotters=spotters;
    }
}
