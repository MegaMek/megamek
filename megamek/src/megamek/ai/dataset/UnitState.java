/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.ai.dataset;

import megamek.common.*;
import megamek.common.enums.GamePhase;

import java.util.Map;

/**
 * Represents the state of a unit.
 * @param id unit id
 * @param teamId team id
 * @param round round number
 * @param playerId player id
 * @param chassis chassis
 * @param model model
 * @param type type is actually the simple name of the class of the entity
 * @param role UnitRole
 * @param x x coordinate
 * @param y y coordinate
 * @param facing facing direction
 * @param mp movement points
 * @param heat heat points
 * @param prone prone
 * @param airborne airborne
 * @param offBoard off board
 * @param crippled crippled
 * @param destroyed destroyed
 * @param armorP armor percent
 * @param internalP internal percent
 * @param done done
 * @param maxRange max weapon range
 * @param totalDamage total damage it can cause
 * @param entity entity
 * @author Luana Coppio
 */
public record UnitState(int id, GamePhase phase, int teamId, int round, int playerId, String chassis, String model, String type,
                        UnitRole role, int x, int y, int facing, double mp, double heat, boolean prone, boolean airborne,
                        boolean offBoard, boolean crippled, boolean destroyed, double armorP,
                        double internalP, boolean done, int maxRange, int totalDamage, Entity entity) implements Targetable {

    public static UnitState fromEntity(Entity entity, Game game) {
        return new UnitState(
            entity.getId(),
            game.getPhase(),
            entity.getOwner().getTeam(),
            game.getCurrentRound(),
            entity.getOwner().getId(),
            entity.getChassis(),
            entity.getModel(),
            entity.getClass().getSimpleName(),
            entity.getRole(),
            entity.getPosition() == null ? -1 : entity.getPosition().getX(),
            entity.getPosition() == null ? -1 : entity.getPosition().getY(),
            entity.getFacing(),
            entity.getMpUsedLastRound(),
            entity.getHeat(),
            entity.isProne(),
            entity.isAirborne(),
            entity.isOffBoard(),
            entity.isCrippled(),
            entity.isDestroyed(),
            entity.getArmorRemainingPercent(),
            entity.getInternalRemainingPercent(),
            entity.isDone(),
            entity.getMaxWeaponRange(),
            Compute.computeTotalDamage(entity.getWeaponList()),
            entity);
    }

    public Coords position() {
        return new Coords(x, y);
    }

    @Override
    public int getTargetType() {
        return TYPE_ENTITY;
    }

    @Override
    public Coords getPosition() {
        return position();
    }

    @Override
    public Map<Integer, Coords> getSecondaryPositions() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int relHeight() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int getHeight() {
        return entity.getHeight();
    }

    @Override
    public int getElevation() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int getAltitude() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isImmobile() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public int sideTable(Coords src) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int sideTable(Coords src, boolean usePrior) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public boolean isOffBoard() {
        return false;
    }

    @Override
    public boolean isAirborne() {
        return airborne;
    }

    @Override
    public boolean isAirborneVTOLorWIGE() {
        return airborne;
    }

    @Override
    public boolean isEnemyOf(Entity other) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int newId) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int getOwnerId() {
        return playerId;
    }

    @Override
    public void setOwnerId(int newOwnerId) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public int getStrength() {
        return entity.getStrength();
    }

    @Override
    public String generalName() {
        return entity.generalName();
    }

    @Override
    public String specificName() {
        return entity.specificName();
    }
}
