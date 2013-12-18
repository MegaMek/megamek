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

/**
 * EntityState describes a hypothetical situation an entity could be in when firing
 */
public class EntityState {
    private Coords position;
    private int facing;
    private int secondaryFacing; // to account for torso twists
    private int heat;
    private int hexesMoved;
    private boolean prone;
    private boolean immobile;
    private boolean jumping;
    private EntityMovementType movementType;

    /**
     * Initialize an entity state from the state an entity is actually in
     * (or something that isn't an entity)
     */
    EntityState(Targetable target) {
        if (target instanceof Entity) { // mechs and planes and tanks etc
            Entity entity = (Entity) target;
            position = entity.getPosition();
            facing = entity.getFacing();
            hexesMoved = entity.delta_distance;
            heat = entity.heat;
            prone = entity.isProne() || entity.isHullDown();
            immobile = entity.isImmobile();
            jumping = (entity.moved == EntityMovementType.MOVE_JUMP);
            movementType = entity.moved;
            setSecondaryFacing(entity.getSecondaryFacing());
        } else { // for buildings and such
            position = target.getPosition();
            facing = 0;
            hexesMoved = 0;
            heat = 0;
            prone = false;
            immobile = true;
            jumping = false;
            movementType = EntityMovementType.MOVE_NONE;
            setSecondaryFacing(0);
        }
    }

    /**
     * Initialize an entity state from a movement path
     */
    EntityState(MovePath path) {
        position = path.getFinalCoords();
        facing = path.getFinalFacing();
        hexesMoved = path.getHexesMoved();
        heat = path.getEntity().heat;
        if (path.getLastStepMovementType() == EntityMovementType.MOVE_WALK) {
            heat = getHeat() + 1;
        } else if (path.getLastStepMovementType() == EntityMovementType.MOVE_RUN) {
            heat = getHeat() + 2;
        } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                && (getHexesMoved() <= 3)) {
            heat = getHeat() + 3;
        } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                && (getHexesMoved() > 3)) {
            heat = getHeat() + getHexesMoved();
        }
        prone = path.getFinalProne() || path.getFinalHullDown();
        immobile = path.getEntity().isImmobile();
        jumping = path.isJumping();
        movementType = path.getLastStepMovementType();
        setSecondaryFacing(getFacing());
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

    public int getHeat() {
        return heat;
    }

    public int getHexesMoved() {
        return hexesMoved;
    }

    public boolean isProne() {
        return prone;
    }

    public boolean isImmobile() {
        return immobile;
    }

    public boolean isJumping() {
        return jumping;
    }

    public EntityMovementType getMovementType() {
        return movementType;
    }

    public void setSecondaryFacing(int secondaryFacing) {
        this.secondaryFacing = secondaryFacing;
    }
}
