/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
 * ArtilleryAttackAction.
 *  Holds the data needed for an artillery attack in flight.
 */
public class ArtilleryAttackAction extends WeaponAttackAction
implements Serializable
{
    public int turnsTilHit;
    /** IDs of possible spotters, won't know until it lands. */
    private Vector spotterIds;
    private final int playerId;
    private Coords firingCoords;
    
    public ArtilleryAttackAction(int entityId, int targetType, int targetId, int weaponId, IGame game) { 
        super(entityId, targetType, targetId, weaponId);
        this.playerId = game.getEntity(entityId).getOwnerId();
        this.firingCoords = game.getEntity(entityId).getPosition();
        int distance = Compute.effectiveDistance
            (game, game.getEntity(entityId), game.getTarget(targetType,targetId));
        // Two boards is one turn of flight time, except on the same sheet.
        turnsTilHit = (distance<=17) ? 0 : ((distance/34)+1);
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
