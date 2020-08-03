package megamek.client.ui.swing.util;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Tank;

public class EntityWreckHelper {    
    /**
     * Logic that determines if we should be display "destroyed" decals below the destroyed entity.
     * Assumes that the entity is destroyed.
     * @param entity Entity to check
     * @return Whether we want to display decals indicating that the entity has been destroyed
     */
    public static boolean displayDestroyedDecal(Entity entity) {
        if(entity.getGame().getBoard().inSpace() ||
                (entity instanceof Mech) ||
                (entity instanceof Infantry) ||
                (entity.getMovementMode() == EntityMovementMode.VTOL) ||
                (entity.getDamageLevel(false) == Entity.DMG_NONE)) {
            return false;
        }
        
        return true;
    }
    
    public static boolean displayFuelLeak(Entity entity) {
        return (entity instanceof Tank) &&
                (entity.getMovementMode() != EntityMovementMode.VTOL) && 
                (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) &&
                entity.isPermanentlyImmobilized(false);
    }
    
    public static boolean displayMotiveDamage(Entity entity) {
        return entity.isPermanentlyImmobilized(false) && 
                ((entity.getMovementMode() == EntityMovementMode.WHEELED) ||
                (entity.getMovementMode() == EntityMovementMode.TRACKED));
    }
    
    public static boolean displayDevastation(Entity entity) {
        return (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_DEVASTATED);
    }
    
    public static String getMotivePrefix(Entity entity) {
        if(!displayMotiveDamage(entity)) {
            return null;
        }
        
        switch(entity.getMovementMode()) {
        case WHEELED:
            return "wheels";
        case TRACKED:
            return "treads";
        default:
            return null;
        }
    }
    
    public static String getWeightPostfix(Entity entity) {
        switch(entity.getWeightClass()) {
        case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
            return "ultralight";
        case EntityWeightClass.WEIGHT_LIGHT:
            // this is a "hack" as some units < 20 tons are classified as 'light'
            if (entity.getWeight() < 20) {
               return "ultralight"; 
            } else {
                return "assaultplus";
            }
        default:
            return "assaultplus";
        }
    }
    
    public static String getDevastatedPrefix(Entity entity) {
        if(!displayDevastation(entity)) {
            return null;
        } else {
            switch(entity.getWeightClass()) {
            case EntityWeightClass.WEIGHT_ULTRA_LIGHT:
                return "ultralight";
            case EntityWeightClass.WEIGHT_LIGHT:
                // this is a "hack" as some units < 20 tons are classified as 'light'
                if (entity.getWeight() < 20) {
                   return "ultralight"; 
                } else {
                    return "assaultplus";
                }
            default:
                return "assaultplus";
            }
        }
    }
}
