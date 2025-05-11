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

import java.util.ArrayList;
import java.util.List;

import megamek.ai.utility.EntityFeatureUtils;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IAero;
import megamek.common.UnitRole;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

/**
 * Flexible container for unit state data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class UnitState extends EntityDataMap<UnitState.Field> {
    private static final MMLogger logger = MMLogger.create(UnitState.class);

    /**
     * Enum defining all available unit state fields.
     */
    public enum Field {
        ID,
        PHASE,
        TEAM_ID,
        ROUND,
        PLAYER_ID,
        CHASSIS,
        MODEL,
        TYPE,
        ROLE,
        X,
        Y,
        FACING,
        MP,
        HEAT,
        PRONE,
        AIRBORNE,
        OFF_BOARD,
        CRIPPLED,
        DESTROYED,
        ARMOR_P,
        INTERNAL_P,
        DONE,
        MAX_RANGE,
        TOTAL_DAMAGE,
        ARMOR,
        INTERNAL,
        BV,
        IS_BOT,
        HAS_ECM,
        ARMOR_FRONT_P,
        ARMOR_LEFT_P,
        ARMOR_RIGHT_P,
        ARMOR_BACK_P,
        WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE;
    }

    /**
     * Creates an empty UnitStateMap.
     */
    public UnitState() {
        super(Field.class);
    }

    /**
     * Creates a UnitStateMap from an Entity.
     * @param entity The entity to extract state from
     * @param game The game reference
     * @return A populated UnitStateMap
     */
    public static UnitState fromEntity(Entity entity, Game game) {
        UnitState map = new UnitState();

        // Basic entity information
        map.put(Field.ID, entity.getId())
              .put(Field.PHASE, game.getPhase())
              .put(Field.TEAM_ID, entity.getOwner().getTeam())
              .put(Field.ROUND, game.getCurrentRound())
              .put(Field.PLAYER_ID, entity.getOwner().getId())
              .put(Field.CHASSIS, entity.getChassis())
              .put(Field.MODEL, entity.getModel())
              .put(Field.TYPE, entity.getClass().getSimpleName())
              .put(Field.ROLE, firstNonNull(entity.getRole(), UnitRole.NONE));

        // Position and movement
        if (entity.getPosition() != null) {
            map.put(Field.X, entity.getPosition().getX())
                  .put(Field.Y, entity.getPosition().getY());
        } else {
            map.put(Field.X, -1)
                  .put(Field.Y, -1);
        }

        map.put(Field.FACING, entity.getFacing())
              .put(Field.MP, entity.getMpUsedLastRound())
              .put(Field.HEAT, entity.getHeat());

        // Status flags
        map.put(Field.PRONE, entity.isProne())
              .put(Field.AIRBORNE, entity.isAirborne())
              .put(Field.OFF_BOARD, entity.isOffBoard())
              .put(Field.CRIPPLED, entity.isCrippled())
              .put(Field.DESTROYED, entity.isDestroyed())
              .put(Field.DONE, entity.isDone());

        // Health and armor
        map.put(Field.ARMOR_P, entity.getArmorRemainingPercent())
              .put(Field.INTERNAL_P, entity.getInternalRemainingPercent())
              .put(Field.ARMOR, entity.getTotalArmor());

        if (entity instanceof IAero aero) {
            map.put(Field.INTERNAL, aero.getSI());
        } else {
            map.put(Field.INTERNAL, entity.getTotalInternal());
        }

        // Combat stats
        map.put(Field.MAX_RANGE, entity.getMaxWeaponRange())
              .put(Field.TOTAL_DAMAGE, Compute.computeTotalDamage(entity.getWeaponList()))
              .put(Field.BV, entity.getInitialBV());

        // Equipment and capabilities
        map.put(Field.IS_BOT, entity.getOwner().isBot())
              .put(Field.HAS_ECM, entity.hasActiveECM());

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
