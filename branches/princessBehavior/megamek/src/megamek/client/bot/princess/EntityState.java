/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.util.Logger;

/**
 * EntityState describes a hypothetical situation an entity could be in when firing
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/8/13 1:19 PM
 */
public class EntityState {
    private Coords position;
    private int facing;
    private int secondaryFacing; // to account for torso twists
    private int heat;
    private int hexesMoved;
    private boolean isProne;
    private boolean isImmobile;
    private boolean isJumping;
    private EntityMovementType movementType;

    protected EntityState() {}

    /**
     * Initialize an entity state from the state an entity is actually in
     * (or something that isn't an entity)
     */
    EntityState(Targetable t) {
        if (t instanceof Entity) { // mechs and planes and tanks etc
            Entity e = (Entity)t;
            init(e);
        } else { // for buildings and such
            init(t);
        }
    }

    protected void init(Entity entity) {
        position = entity.getPosition();
        facing = entity.getFacing();
        hexesMoved = entity.getDeltaDistance();
        heat = entity.getHeat();
        isProne = entity.isProne() || entity.isHullDown();
        isImmobile = entity.isImmobile();
        isJumping = (entity.getMoved() == EntityMovementType.MOVE_JUMP);
        movementType = entity.getMoved();
        secondaryFacing = entity.getSecondaryFacing();
    }

    protected void init(Targetable t) {
        position = t.getPosition();
        facing = 0;
        hexesMoved = 0;
        heat = 0;
        isProne = false;
        isImmobile = true;
        isJumping = false;
        movementType = EntityMovementType.MOVE_NONE;
        secondaryFacing = 0;
    }

    /**
     * Initialize an entity state from a movement path
     */
    EntityState(MovePath path) {
        init(path);
    }

    protected void init(MovePath path) {
        position = path.getFinalCoords();
        facing = path.getFinalFacing();
        hexesMoved = path.getHexesMoved();
        heat = path.getEntity().getHeat();
        if (path.getLastStepMovementType() == EntityMovementType.MOVE_WALK) {
            heat += 1;
        } else if (path.getLastStepMovementType() == EntityMovementType.MOVE_RUN) {
            heat += 2;
        } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                   && (hexesMoved <= 3)) {
            heat += 3;
        } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                   && (hexesMoved > 3)) {
            heat += hexesMoved;
        }
        isProne = path.getFinalProne() || path.getFinalHullDown();
        isImmobile = path.getEntity().isImmobile();
        isJumping = path.isJumping();
        movementType = path.getLastStepMovementType();
        secondaryFacing = facing;
    }

    public Coords getPosition() {
        return position;
    }

    public int getFacing() {
        return facing;
    }

    public int getSecondaryFacing() {
        return secondaryFacing;
    }

    public void setSecondaryFacing(int secondaryFacing) {
        this.secondaryFacing = secondaryFacing;
    }

    public int getHeat() {
        return heat;
    }

    public int getHexesMoved() {
        return hexesMoved;
    }

    public boolean isProne() {
        return isProne;
    }

    public boolean isImmobile() {
        return isImmobile;
    }

    public boolean isJumping() {
        return isJumping;
    }

    public EntityMovementType getMovementType() {
        return movementType;
    }
}
