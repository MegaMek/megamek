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
 * ArtilleryAttackAction
 *  Holds the data needed for an artillery attack in flight.
 */
public class ArtilleryAttackAction extends WeaponAttackAction
implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -3893844894076028005L;
    public int turnsTilHit;
    private Vector<Integer> spotterIds; // IDs of possible spotters, won't know until it lands.
    protected int playerId;
    private Coords firingCoords;

    public ArtilleryAttackAction(int entityId, int targetType, int targetId, int weaponId, IGame game) { 
        super(entityId, targetType, targetId, weaponId);
        this.playerId = game.getEntity(entityId).getOwnerId();
        this.firingCoords = game.getEntity(entityId).getPosition();
        int distance = Compute.effectiveDistance
            (game, getEntity(game), getTarget(game));
        if(game.getOptions().booleanOption("maxtech_artillery")) {
            if(distance <=17)
                turnsTilHit = 0;
            else if(distance <=(6*17))
                turnsTilHit = 1;
            else if(distance <=(14*17))
                turnsTilHit = 2;
            else if(distance <=(20*17))
                turnsTilHit = 3;
            else if(distance <=(25*17))
                turnsTilHit = 4;
            else 
                turnsTilHit = 5;
        } else {
            // Two boards is one turn of flight time, except on the same sheet.
            turnsTilHit = (distance<=17) ? 0 : ((distance/34)+1);
        }
    }

    public Vector<Integer> getSpotterIds() {
        return spotterIds;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setSpotterIds(Vector<Integer> spotterIds) {
        this.spotterIds = spotterIds;
    }

    public void setCoords(Coords coords) {
        this.firingCoords=coords;
    }

    public Coords getCoords() {
        return this.firingCoords;
    }
}
