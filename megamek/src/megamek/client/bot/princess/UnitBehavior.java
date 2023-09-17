package megamek.client.bot.princess;

import java.util.HashMap;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.Mech;

public class UnitBehavior {
    public enum BehaviorType {
        // this unit is under 'forced withdrawal' due to being crippled
        ForcedWithdrawal,
        
        // this unit will do its best to get to a destination
        MoveToDestination,
        
        // this unit will move either toward the nearest enemy or towards the "opposite" edge of the board 
        MoveToContact,
        
        // this unit is engaged in battle
        Engaged,
        
        // this unit has no path to its destination
        NoPathToDestination
    }
    
    private Map<Integer, BehaviorType> entityBehaviors = new HashMap<>();
    
    /**
     * Worker function that calculates a unit's desired behavior
     */
    private BehaviorType calculateUnitBehavior(Entity entity, Princess owner) {
        BehaviorSettings botSettings = owner.getBehaviorSettings();

        if (botSettings.isForcedWithdrawal() && entity.isCrippled()) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                return BehaviorType.NoPathToDestination;
            }
            
            return BehaviorType.ForcedWithdrawal;
        } else if (botSettings.shouldAutoFlee() && botSettings.getDestinationEdge() != CardinalEdge.NONE) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                return BehaviorType.NoPathToDestination;
            }
            
            return BehaviorType.MoveToDestination;
        } else if ((entity instanceof Mech) && ((Mech) entity).isJustMovedIntoIndustrialKillingWater()) {
            if (owner.getClusterTracker().getDestinationCoords(entity, owner.getHomeEdge(entity), true).isEmpty()) {
                return BehaviorType.NoPathToDestination;
            }

            return BehaviorType.ForcedWithdrawal;
        } else {
            // if we can't see anyone, move to contact
            if (!entity.getGame().getAllEnemyEntities(entity).hasNext()) {
                return BehaviorType.MoveToContact;
            }
            
            return BehaviorType.Engaged;
        }
    }
    
    /**
     * Gets (and calculates, if necessary), the behavior type for the given entity.
     */
    public BehaviorType getBehaviorType(Entity entity, Princess owner) {
        if (!entityBehaviors.containsKey(entity.getId())) {
            entityBehaviors.put(entity.getId(), calculateUnitBehavior(entity, owner));
        }
        
        return entityBehaviors.get(entity.getId());
    }
    
    public void overrideBehaviorType(Entity entity, BehaviorType behaviorType) {
        entityBehaviors.put(entity.getId(), behaviorType);
    }
    
    /**
     * Clears the entity behavior cache, should be done at the start of each movement phase
     */
    public void clear() {
        entityBehaviors.clear();
    }
    
}
