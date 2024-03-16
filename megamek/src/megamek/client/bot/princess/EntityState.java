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

import megamek.client.bot.princess.BotGeometry.CoordFacingCombo;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.MovePath;
import megamek.common.Targetable;
import megamek.common.options.OptionsConstants;

/**
 * EntityState describes a hypothetical situation an entity could be in when firing
 *
 * @author Deric Page (deric dot page at usa dot net)
 * @since 12/18/13 9:28 AM
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
    private boolean building;
    private boolean aero;
    private boolean airborne;
    private boolean naturalAptGun;
    private boolean naturalAptPilot;

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
            building = false;
            aero = target.isAero();
            airborne = entity.isAirborne() || entity.isAirborneVTOLorWIGE();
            naturalAptGun = entity.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
            naturalAptPilot = entity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
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
            building = (target instanceof BuildingTarget);
            aero = false;
            naturalAptGun = false;
            naturalAptPilot = false;
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
        naturalAptGun = path.getEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
        naturalAptPilot = path.getEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        setSecondaryFacing(getFacing());
    }

    /**
     * Create an entity state from a Targetable, but pretend it's in a different hex facing in a different direction.
     */
    EntityState(Targetable target, CoordFacingCombo projectedTargetLocation) {
        this(target);
        position = projectedTargetLocation.getCoords();
        facing = projectedTargetLocation.getFacing();
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

    public boolean isBuilding() {
        return building;
    }

    public boolean isAero() {
        return aero;
    }

    public boolean isAirborne() {
        return airborne;
    }

    public boolean isAirborneAero() {
        return aero && airborne;
    }

    public boolean hasNaturalAptGun() {
        return naturalAptGun;
    }

    public boolean hasNaturalAptPiloting() {
        return naturalAptPilot;
    }
}
