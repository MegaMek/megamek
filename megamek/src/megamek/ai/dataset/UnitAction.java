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

import megamek.client.ui.SharedUtility;
import megamek.common.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a unit action.
 * @param id unit id
 * @param facing facing direction
 * @param fromX x coordinate of the starting position
 * @param fromY y coordinate of the starting position
 * @param toX x coordinate of the final position
 * @param toY y coordinate of the final position
 * @param hexesMoved number of hexes moved
 * @param distance distance moved
 * @param mpUsed movement points used
 * @param maxMp maximum movement points
 * @param mpP movement points percentage
 * @param heatP heat percentage
 * @param armorP armor percentage
 * @param internalP internal percentage
 * @param jumping jumping
 * @param prone prone
 * @param legal is move legal
 * @author Luana Coppio
 */
public record UnitAction(int id, int teamId, int playerId, String chassis, String model, int facing, int fromX, int fromY, int toX, int toY, int hexesMoved, int distance, int mpUsed,
                         int maxMp, double mpP, double heatP, double armorP, double internalP, boolean jumping, boolean prone,
                         boolean legal, double chanceOfFailure, List<MovePath.MoveStepType> steps) implements Targetable {

    public static  UnitAction fromMovePath(MovePath movePath) {
        Entity entity = movePath.getEntity();
        Map<EntityMovementType, PilotingRollData> cachedPilotBaseRoll = new HashMap<>();
        double chanceOfFailure = SharedUtility.getPSRList(cachedPilotBaseRoll, movePath).stream().map(psr -> psr.getValue() / 36d).reduce(0.0, (a, b) -> a * b);
        var steps = movePath.getStepVector().stream().map(MoveStep::getType).toList();
        return new UnitAction(
            entity.getId(),
            entity.getOwner() != null ? entity.getOwner().getTeam() : -1,
            entity.getOwner() != null ? entity.getOwner().getId() : -1,
            entity.getChassis(),
            entity.getModel(),
            movePath.getFinalFacing(),
            movePath.getStartCoords() != null ? movePath.getStartCoords().getX() : -1,
            movePath.getStartCoords() != null ? movePath.getStartCoords().getY() : -1,
            movePath.getFinalCoords() != null ? movePath.getFinalCoords().getX() : -1,
            movePath.getFinalCoords() != null ? movePath.getFinalCoords().getY() : -1,
            movePath.getHexesMoved(),
            movePath.getDistanceTravelled(),
            movePath.getMpUsed(),
            movePath.getMaxMP(),
            movePath.getMaxMP() > 0 ? (double) movePath.getMpUsed() / movePath.getMaxMP() : 0.0,
            entity.getHeatCapacity() > 0 ? entity.getHeat() / (double) entity.getHeatCapacity() : 0.0,
            entity.getArmorRemainingPercent(),
            entity.getInternalRemainingPercent(),
            movePath.isJumping(),
            movePath.getFinalProne(),
            movePath.isMoveLegal(),
            chanceOfFailure,
            steps
        );
    }

    public Coords currentPosition() {
        return new Coords(fromX, fromY);
    }

    public Coords finalPosition() {
        return new Coords(toX, toY);
    }

    @Override
    public int getTargetType() {
        return 0;
    }

    @Override
    public Coords getPosition() {
        return currentPosition();
    }

    @Override
    public Map<Integer, Coords> getSecondaryPositions() {
        return Map.of();
    }

    @Override
    public int relHeight() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getElevation() {
        return 0;
    }

    @Override
    public int getAltitude() {
        return 0;
    }

    @Override
    public boolean isImmobile() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return model + " " + chassis;
    }

    @Override
    public int sideTable(Coords src) {
        return 0;
    }

    @Override
    public int sideTable(Coords src, boolean usePrior) {
        return 0;
    }

    @Override
    public boolean isOffBoard() {
        return false;
    }

    @Override
    public boolean isAirborne() {
        return false;
    }

    @Override
    public boolean isAirborneVTOLorWIGE() {
        return false;
    }

    @Override
    public boolean isEnemyOf(Entity other) {
        if (other.getOwner() == null) {
            return true;
        }
        return other.getOwner().getTeam() != teamId;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int newId) {

    }

    @Override
    public int getOwnerId() {
        return playerId;
    }

    @Override
    public void setOwnerId(int newOwnerId) {

    }

    @Override
    public int getStrength() {
        return 0;
    }

    @Override
    public String generalName() {
        return model + " " + chassis;
    }

    @Override
    public String specificName() {
        return model + " " + chassis;
    }
}
