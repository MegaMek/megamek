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

import megamek.ai.utility.EntityFeatureUtils;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IAero;
import megamek.common.UnitRole;
import megamek.logging.MMLogger;

import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * Flexible container for unit state data using a map-based approach with enum keys.
 * @author Luana Coppio
 */
public class UnitEnrichment extends EntityDataMap<UnitEnrichment.Field> {

    /**
     * Enum defining all available unit state fields.
     * "chassis", "model", "bv", "armor", "internal", "ecm",
     *           "max_range", "total_damage", "role", "armor_front", "armor_right", "armor_left", "armor_back", "arc_0",
     *           "arc_1", "arc_2", "arc_3", "arc_4", "arc_5"
     */
    public enum Field {
        CHASSIS,
        MODEL,
        TYPE,
        ROLE,
        BV,
        WALK_MP,
        RUN_MP,
        JUMP_MP,
        HEAT,
        ARMOR,
        INTERNAL,
        HEIGHT,
        HAS_ECM,
        HAS_AMS,
        MAX_RANGE,
        MAX_DAMAGE,
        ARMOR_FRONT,
        ARMOR_LEFT,
        ARMOR_RIGHT,
        ARMOR_BACK,
        WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE,
    }

    /**
     * Creates an empty UnitStateMap.
     */
    public UnitEnrichment() {
        super(Field.class);
    }

    /**
     * Creates a UnitStateMap from an Entity.
     * @param entity The entity to extract state from
     * @return A populated UnitStateMap
     */
    public static UnitEnrichment fromEntity(Entity entity) {
        UnitEnrichment map = new UnitEnrichment();
        List<Integer> weaponData = WeaponDataEncoder.getEncodedWeaponData(entity);
        // Basic entity information
        map.put(Field.CHASSIS, entity.getChassis())
              .put(Field.MODEL, entity.getModel())
              .put(Field.TYPE, entity.getClass().getSimpleName())
              .put(Field.ROLE, entity.getRole().name())
              .put(Field.BV, entity.getInitialBV())
              .put(Field.WALK_MP, entity.getOriginalWalkMP())
              .put(Field.RUN_MP, entity.getOriginalRunMP())
              .put(Field.JUMP_MP, entity.getOriginalJumpMP())
              .put(Field.HEAT, entity.getHeatCapacity())
              .put(Field.ARMOR, entity.getTotalOArmor())
              .put(Field.INTERNAL, entity.getTotalOInternal())
              .put(Field.HEIGHT, entity.getHeight() + 1)
              .put(Field.HAS_ECM, entity.hasECM())
              .put(Field.HAS_AMS, !entity.getActiveAMS().isEmpty())
              .put(Field.MAX_RANGE, entity.getMaxWeaponRange())
              .put(Field.MAX_DAMAGE, Compute.computeTotalDamage(entity.getWeaponList()))
              .put(Field.ARMOR_FRONT, EntityFeatureUtils.getDiscreteTargetFrontHealthStats(entity))
              .put(Field.ARMOR_LEFT, EntityFeatureUtils.getDiscreteTargetLeftSideHealthStats(entity))
              .put(Field.ARMOR_RIGHT, EntityFeatureUtils.getDiscreteTargetRightSideHealthStats(entity))
              .put(Field.ARMOR_BACK, EntityFeatureUtils.getDiscreteTargetBackHealthStats(entity))
              .put(Field.WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE, weaponData);

        return map;
    }
}
