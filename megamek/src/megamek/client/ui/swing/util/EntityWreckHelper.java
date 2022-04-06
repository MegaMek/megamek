/*
 * MegaMek - Copyright (C) 2020 - The MegaMek Team
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

package megamek.client.ui.swing.util;

import megamek.client.ui.swing.tileset.TilesetManager;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.GunEmplacement;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Hex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.Terrains;

/**
 * This class handles logic for displaying various kinds of damage and destruction decals
 * @author NickAragua
 * 
 */
public class EntityWreckHelper {    
    /**
     * Logic that determines if we should be display "destroyed" decals below the destroyed entity.
     * Assumes that the entity is destroyed.
     */
    public static boolean displayDestroyedDecal(Entity entity) {
        // don't display "generic" destroyed decals in the following situations:
        //  in space (looks weird)
        //  for mechs/infantry/VTOLs (needs specialized icons) 
        //  for units that were destroyed by ejection rather than unit destruction
        //  for units on top of a bridge (looks kind of stupid)
        
        if (entity.getGame().getBoard().inSpace() ||
                (entity instanceof Mech) ||
                (entity instanceof Infantry) ||
                (entity instanceof GunEmplacement) ||
                !entity.getSecondaryPositions().isEmpty() ||
                entityOnBridge(entity)) {
            return false;
        }
        
        return true;
    }
    
    public static boolean useExplicitWreckImage(Entity entity) {
        return entity instanceof Mech;
    }
    
    /**
     * Logic that determines whether we should display a 'fuel leak' for the given entity.
     */
    public static boolean displayFuelLeak(Entity entity) {
        return (entity instanceof Tank) &&
                (entity.getMovementMode() != EntityMovementMode.VTOL) && 
                (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) &&
                entity.isPermanentlyImmobilized(false) &&
                !entity.getGame().getBoard().inSpace() &&
                !entityOnBridge(entity);
    }
    
    /**
     * Whether we should display 'motive damage' for the given entity, meaning loose treads and such
     */
    public static boolean displayMotiveDamage(Entity entity) {
        return entity.isPermanentlyImmobilized(false) && 
                ((entity.getMovementMode() == EntityMovementMode.WHEELED) ||
                (entity.getMovementMode() == EntityMovementMode.TRACKED)) && 
                entity.getSecondaryPositions().isEmpty() &&
                !entity.getGame().getBoard().inSpace() &&
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
        
        switch (entity.getMovementMode()) {
            case WHEELED:
                return "wheels";
            case TRACKED:
                return "treads";
            default:
                return null;
        }
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
                // additionally, gun emplacements are "light" but should really have a little more debris.
                if ((entity.getWeight() > 0) && (entity.getWeight() < 20)) {
                   return TilesetManager.FILENAME_SUFFIX_WRECKS_ULTRALIGHT; 
                } else {
                    return TilesetManager.FILENAME_SUFFIX_WRECKS_ASSAULTPLUS;
                }
            default:
                return TilesetManager.FILENAME_SUFFIX_WRECKS_ASSAULTPLUS;
        }
    }
    
    /**
     * Utility function that determines if the entity is on a bridge
     */
    public static boolean entityOnBridge(Entity entity) {   
        Hex hex = entity.getGame().getBoard().getHex(entity.getPosition());
        if (hex != null) {
            boolean hexHasBridge = hex.containsTerrain(Terrains.BRIDGE_CF);
            
            if (hexHasBridge && entity.getElevation() >= hex.ceiling()) {
                return true;
            }
        }
        
        return false;
    }
}
