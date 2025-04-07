/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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
import megamek.client.ui.SharedUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.UnitRole;

/**
 * UnitAction is a record that represents the action of a unit in the game.
 * @author Luana Coppio
 */
public record UnitAction(int id, int teamId, int playerId, String chassis, String model, int facing,
      int fromX, int fromY, int toX, int toY, int hexesMoved, int distance,
      int mpUsed, int maxMp, double mpP, double heatP, double armorP, double internalP,
      boolean jumping, boolean prone, boolean legal, double chanceOfFailure,
      List<MovePath.MoveStepType> steps, boolean bot,
      boolean hasEcm, int armor, int internal, int bv, int maxRange, int totalDamage,
      float armorFrontP, float armorLeftP, float armorRightP, float armorBackP, UnitRole role,
      List<Integer> weaponDamageFacingShortMediumLongRange) {

    /**
     * Creates a UnitAction from a MovePath.
     * @param movePath The MovePath to convert
     * @return The UnitAction
     */
    public static UnitAction fromMovePath(MovePath movePath) {
        Entity entity = movePath.getEntity();
        double chanceOfFailure = SharedUtility.getPSRList(movePath).stream()
                                       .map(psr -> psr.getValue() / 36d)
                                       .reduce(1.0, (a, b) -> a * b);

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
              steps,
              entity.getOwner().isBot(),
              entity.hasActiveECM(),
              entity.getTotalArmor(),
              entity instanceof IAero aero ? aero.getSI() : entity.getTotalInternal(),
              entity.getInitialBV(),
              entity.getMaxWeaponRange(),
              Compute.computeTotalDamage(entity.getWeaponList()),
              EntityFeatureUtils.getTargetFrontHealthStats(entity),
              EntityFeatureUtils.getTargetLeftSideHealthStats(entity),
              EntityFeatureUtils.getTargetRightSideHealthStats(entity),
              EntityFeatureUtils.getTargetBackHealthStats(entity),
              entity.getRole(),
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
