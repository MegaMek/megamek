/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.ai.dataset;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.util.List;
import java.util.stream.Collectors;

import megamek.ai.utility.EntityFeatureUtils;
import megamek.client.ui.SharedUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IAero;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.UnitRole;

/**
 * Flexible container for unit action data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class UnitAction extends EntityDataMap<UnitAction.Field> {

    /**
     * Enum defining all available unit action fields.
     */
    public enum Field {
        ID,
        TEAM_ID,
        PLAYER_ID,
        CHASSIS,
        MODEL,
        FACING,
        FROM_X,
        FROM_Y,
        TO_X,
        TO_Y,
        HEXES_MOVED,
        DISTANCE,
        MP_USED,
        MAX_MP,
        MP_P,
        HEAT_P,
        ARMOR_P,
        INTERNAL_P,
        JUMPING,
        PRONE,
        LEGAL,
        CHANCE_OF_FAILURE,
        STEPS,
        IS_BOT,
        HAS_ECM,
        ARMOR,
        INTERNAL,
        BV,
        MAX_RANGE,
        TOTAL_DAMAGE,
        ARMOR_FRONT_P,
        ARMOR_LEFT_P,
        ARMOR_RIGHT_P,
        ARMOR_BACK_P,
        ROLE,
        WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE;
    }

    /**
     * Creates an empty UnitActionMap.
     */
    public UnitAction() {
        super(Field.class);
    }

    /**
     * Creates a UnitActionMap from a MovePath.
     * @param movePath The MovePath to extract data from
     * @return A populated UnitActionMap
     */
    public static UnitAction fromMovePath(MovePath movePath) {
        Entity entity = movePath.getEntity();
        UnitAction map = new UnitAction();

        // Basic entity information
        map.put(Field.ID, entity.getId())
              .put(Field.TEAM_ID, entity.getOwner() != null ? entity.getOwner().getTeam() : -1)
              .put(Field.PLAYER_ID, entity.getOwner() != null ? entity.getOwner().getId() : -1)
              .put(Field.CHASSIS, entity.getChassis())
              .put(Field.MODEL, entity.getModel())
              .put(Field.FACING, movePath.getFinalFacing())
              .put(Field.ROLE, firstNonNull(entity.getRole(), UnitRole.NONE));

        // Position information
        if (movePath.getStartCoords() != null) {
            map.put(Field.FROM_X, movePath.getStartCoords().getX()).put(Field.FROM_Y, movePath.getStartCoords().getY());
        } else {
            map.put(Field.FROM_X, -1).put(Field.FROM_Y, -1);
        }

        if (movePath.getFinalCoords() != null) {
            map.put(Field.TO_X, movePath.getFinalCoords().getX()).put(Field.TO_Y, movePath.getFinalCoords().getY());
        } else {
            map.put(Field.TO_X, -1).put(Field.TO_Y, -1);
        }

        // Movement information
        map.put(Field.HEXES_MOVED, movePath.getHexesMoved())
              .put(Field.DISTANCE, movePath.getDistanceTravelled())
              .put(Field.MP_USED, movePath.getMpUsed())
              .put(Field.MAX_MP, movePath.getMaxMP())
              .put(Field.MP_P, movePath.getMaxMP() > 0 ? (double) movePath.getMpUsed() / movePath.getMaxMP() : 0.0)
              .put(Field.HEAT_P,
                    entity.getHeatCapacity() > 0 ? entity.getHeat() / (double) entity.getHeatCapacity() : 0.0);

        // Status information
        map.put(Field.ARMOR_P, entity.getArmorRemainingPercent())
              .put(Field.INTERNAL_P, entity.getInternalRemainingPercent())
              .put(Field.JUMPING, movePath.isJumping())
              .put(Field.PRONE, movePath.getFinalProne())
              .put(Field.LEGAL, movePath.isMoveLegal());

        // Failure chance calculation
        map.put(Field.CHANCE_OF_FAILURE,
              SharedUtility.getPSRList(movePath)
                    .stream()
                    .map(psr -> psr.getValue() / 36d)
                    .reduce(1.0, (a, b) -> a * b));

        // Movement steps
        map.put(Field.STEPS, movePath.getStepVector().stream().map(MoveStep::getType).collect(Collectors.toList()));

        // Entity capabilities
        map.put(Field.IS_BOT, entity.getOwner().isBot())
              .put(Field.HAS_ECM, entity.hasActiveECM())
              .put(Field.ARMOR, entity.getTotalArmor());

        // Internal structure
        if (entity instanceof IAero aero) {
            map.put(Field.INTERNAL, aero.getSI());
        } else {
            map.put(Field.INTERNAL, entity.getTotalInternal());
        }

        // Combat stats
        map.put(Field.BV, entity.getInitialBV())
              .put(Field.MAX_RANGE, entity.getMaxWeaponRange())
              .put(Field.TOTAL_DAMAGE, Compute.computeTotalDamage(entity.getWeaponList()));

        // Directional armor
        map.put(Field.ARMOR_FRONT_P, EntityFeatureUtils.getTargetFrontHealthStats(entity))
              .put(Field.ARMOR_LEFT_P, EntityFeatureUtils.getTargetLeftSideHealthStats(entity))
              .put(Field.ARMOR_RIGHT_P, EntityFeatureUtils.getTargetRightSideHealthStats(entity))
              .put(Field.ARMOR_BACK_P, EntityFeatureUtils.getTargetBackHealthStats(entity));

        // Weapon information
        List<Integer> weaponData = WeaponDataEncoder.getEncodedWeaponData(entity);

        map.put(Field.WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE, weaponData);

        return map;
    }
}
