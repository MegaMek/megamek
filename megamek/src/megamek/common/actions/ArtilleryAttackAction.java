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
    /** IDs of possible spotters, won't know until it lands. */
    private Vector spotterIds;
    private final int playerId;
    private Coords firingCoords; //Coords of firing entity, needed for resolving attack direction.
    public ArtilleryAttackAction(WeaponResult wr, Game game,
                                 int playerId, Vector spotterIds,Coords coords) {
        this.wr = wr;
        this.playerId = playerId;
        this.spotterIds = spotterIds;
        this.firingCoords= coords;
        int distance = Compute.effectiveDistance
            (game, wr.waa.getEntity(game), wr.waa.getTarget(game));
        // Two boards is one turn of flight time, except on the same sheet.
        turnsTilHit = (distance<=17) ? 0 : ((distance/34)+1);
    }
    public void setWR(WeaponResult wr) {
        this.wr=wr;
    }
    public WeaponResult getWR() {
        return wr;
    }
    public Vector getSpotterIds() {
        return spotterIds;
    }
    public int getPlayerId() {
        return playerId;
    }
    public void setSpotterIds(Vector spotterIds) {
        this.spotterIds=spotterIds;
    }
    public void setCoords(Coords coords) {
        this.firingCoords=coords;
    }
    public Coords getCoords() {
        return this.firingCoords;
    }
}
