/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

import java.util.List;
import java.util.stream.Stream;

import megamek.ai.utility.EntityFeatureUtils;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IAero;
import megamek.common.UnitRole;
import megamek.common.enums.GamePhase;

/**
 * Represents the state of a unit.
 * @author Luana Coppio
 */
public record UnitState(int id, GamePhase phase, int teamId, int round, int playerId,
      String chassis, String model, String type,
      UnitRole role, int x, int y, int facing, double mp, double heat, boolean prone, boolean airborne,
      boolean offBoard, boolean crippled, boolean destroyed, double armorP, double internalP,
      boolean done, int maxRange, int totalDamage, int armor, int internal, int bv,
      boolean bot, boolean hasEcm, float armorFrontP, float armorLeftP, float armorRightP, float armorBackP,
      List<Integer> weaponDamageFacingShortMediumLongRange) {

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
              entity.getTotalArmor(),
              entity instanceof IAero aero ? aero.getSI() : entity.getTotalInternal(),
              entity.getInitialBV(),
              entity.getOwner().isBot(),
              entity.hasActiveECM(),
              EntityFeatureUtils.getTargetFrontHealthStats(entity),
              EntityFeatureUtils.getTargetLeftSideHealthStats(entity),
              EntityFeatureUtils.getTargetRightSideHealthStats(entity),
              EntityFeatureUtils.getTargetBackHealthStats(entity),
              entity.getWeaponList().stream().flatMap(weapon -> {
                  int damage = Compute.computeTotalDamage(weapon);
                  int facing = weapon.isRearMounted() ? -entity.getWeaponArc(weapon.getLocation()) :
                                     entity.getWeaponArc(weapon.getLocation());
                  int shortRange = weapon.getType().getShortRange();
                  int mediumRange = weapon.getType().getMediumRange();
                  int longRange = weapon.getType().getLongRange();
                  return Stream.of(damage, facing, shortRange, mediumRange, longRange);
              }).toList()
        );
    }
}
