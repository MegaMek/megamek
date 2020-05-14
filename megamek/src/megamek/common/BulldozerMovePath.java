package megamek.common;

import java.util.Map;
import java.util.Set;

public class BulldozerMovePath extends MovePath {
    Map<Coords, Integer> coordLevelingCosts;

    public BulldozerMovePath(IGame game, Entity entity) {
        super(game, entity);
    }
    
    @Override
    public int getMpUsed() {
        int totalCost = super.getMpUsed();
        
        for(int levelingCost : coordLevelingCosts.values()) {
            totalCost += levelingCost;
        }
        
        return totalCost;
    }
}
