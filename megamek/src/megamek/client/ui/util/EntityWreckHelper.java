/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.client.ui.util;

import megamek.client.ui.tileset.TilesetManager;
import megamek.common.Hex;
import megamek.common.equipment.Engine;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.CombatVehicleEscapePod;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;

/**
 * This class handles logic for displaying various kinds of damage and destruction decals
 *
 * @author NickAragua
 */
public class EntityWreckHelper {
    /**
     * Logic that determines if we should be display "destroyed" decals below the destroyed entity. Assumes that the
     * entity is destroyed.
     */
    public static boolean displayDestroyedDecal(Entity entity) {
        // don't display "generic" destroyed decals in the following situations:
        // in space (looks weird)
        // for meks/infantry/VTOLs (needs specialized icons)
        // for units that were destroyed by ejection rather than unit destruction
        // for units on top of a bridge (looks kind of stupid)
        // Exception: CVEP should display wrecks even though it extends Infantry

        boolean isInfantryButNotCVEP = (entity instanceof Infantry) && !(entity instanceof CombatVehicleEscapePod);

        return !entity.getGame().getBoard(entity).isSpace() &&
              (!(entity instanceof Mek)) &&
              (!isInfantryButNotCVEP) &&
              (!(entity.isBuildingEntityOrGunEmplacement())) &&
              entity.getSecondaryPositions().isEmpty() &&
              !entityOnBridge(entity);
    }

    public static boolean useExplicitWreckImage(Entity entity) {
        // Meks have specialized wreck images; CVEP uses wreckset.txt lookup for small_boom.png
        return (entity instanceof Mek) || (entity instanceof CombatVehicleEscapePod);
    }

    /**
     * Logic that determines whether we should display a 'fuel leak' for the given entity.
     */
    public static boolean displayFuelLeak(Entity entity) {
        return (entity instanceof Tank) &&
              (entity.getMovementMode() != EntityMovementMode.VTOL) &&
              (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) &&
              entity.isPermanentlyImmobilized(false) &&
              !entity.getGame().getBoard(entity).isSpace() &&
              !entityOnBridge(entity);
    }

    /**
     * Whether we should display 'motive damage' for the given entity, meaning loose treads and such
     */
    public static boolean displayMotiveDamage(Entity entity) {
        return entity.isPermanentlyImmobilized(false) &&
              ((entity.getMovementMode() == EntityMovementMode.WHEELED) ||
                    (entity.getMovementMode() == EntityMovementMode.TRACKED))
              &&
              entity.getSecondaryPositions().isEmpty() &&
              !entity.getGame().getBoard(entity).isSpace() &&
              !entityOnBridge(entity);
    }

    /**
     * Whether a given entity should display a crater instead of its standard wreckage marker.
     */
    public static boolean displayDevastation(Entity entity) {
        return (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED);
    }

    /**
     * Gets the prefix used to retrieve image files for motive-damaged entities
     */
    public static String getMotivePrefix(Entity entity) {
        if (!displayMotiveDamage(entity)) {
            return null;
        }

        return switch (entity.getMovementMode()) {
            case WHEELED -> "wheels";
            case TRACKED -> "treads";
            default -> null;
        };
    }

    /**
     * Gets the weight class suffix for destruction decals for the given entity
     */
    public static String getWeightSuffix(Entity entity) {
        switch (entity.getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                return TilesetManager.FILENAME_SUFFIX_WRECKS_ULTRALIGHT;
            case EntityWeightClass.WEIGHT_LIGHT:
                // this is a "hack" as some units < 20 tons are classified as 'light'
                // additionally, gun emplacements are "light" but should really have a little
                // more debris.
                if ((entity.getWeight() > 0) && (entity.getWeight() < 20)) {
                    return TilesetManager.FILENAME_SUFFIX_WRECKS_ULTRALIGHT;
                } else {
                    return TilesetManager.FILENAME_SUFFIX_WRECKS_ASSAULT_PLUS;
                }
            default:
                return TilesetManager.FILENAME_SUFFIX_WRECKS_ASSAULT_PLUS;
        }
    }

    /**
     * Utility function that determines if the entity is on a bridge
     */
    public static boolean entityOnBridge(Entity entity) {
        Hex hex = entity.getGame().getBoard(entity).getHex(entity.getPosition());
        if (hex != null) {
            boolean hexHasBridge = hex.containsTerrain(Terrains.BRIDGE_CF);

            return hexHasBridge && entity.getElevation() >= hex.ceiling();
        }

        return false;
    }
}
