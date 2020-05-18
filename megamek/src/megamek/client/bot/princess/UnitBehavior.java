package megamek.client.bot.princess;

import megamek.common.Entity;

public class UnitBehavior {
    public enum BehaviorType {
        // this unit is under 'forced withdrawal' due to being crippled
        ForcedWithdrawal,
        
        // this unit will do its best to get to a destination
        MoveToDestination,
        
        // this unit will move either toward the nearest enemy or towards the "opposite" edge of the board 
        MoveToContact,
        
        // this unit is engaged in battle
        Engaged
    }
    
    public static BehaviorType calculateUnitBehavior(Entity entity, BehaviorSettings botSettings) {
        if(botSettings.isForcedWithdrawal() && entity.isCrippled()) {
            return BehaviorType.ForcedWithdrawal;
        } else if(botSettings.getDestinationEdge() != CardinalEdge.NEAREST_OR_NONE) {
            return BehaviorType.MoveToDestination;
        } else {
            return BehaviorType.Engaged;
        }
    }
}
