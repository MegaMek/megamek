/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */
package megamek.ai.dataset;

import megamek.common.*;
import megamek.common.enums.GamePhase;

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
                        double internalP, boolean done, int maxRange, int totalDamage, Entity entity) {

    /**
     * Creates a UnitState from an {@code entity}.
     * @param entity The entity to which the state belongs
     * @param game The game reference
     * @return The UnitState
     */
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

    /**
     * Returns the position of the unit.
     * @return The position
     */
    public Coords position() {
        return new Coords(x, y);
    }
}
