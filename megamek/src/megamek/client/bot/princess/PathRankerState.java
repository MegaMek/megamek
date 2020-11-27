package megamek.client.bot.princess;

import java.util.HashMap;
import java.util.Map;

import megamek.common.Coords;
import megamek.common.MovePath;

/**
 * This class handles state information for Princess' path ranking algorithms, as the pathranker and its 
 * subclasses are intended to be basically stateless.
 *
 */
public class PathRankerState {
    private Map<MovePath.Key, Double> pathSuccessProbabilities = new HashMap<>();
    private Map<Coords, Double> incomingFriendlyArtilleryDamage = new HashMap<>();
    
    /**
     * The map of success probabilities for given move paths.
     * The calculation of a move success probability is pretty complex, so we can cache them here.
     * @return Map of path keys to success probabilities.
     */
    public Map<MovePath.Key, Double> getPathSuccessProbabilities() {
        return pathSuccessProbabilities;
    }
    
    /**
     * Map of coordinates to known incoming artillery damage.
     * @return Map of coordinates.
     */
    public Map<Coords, Double> getIncomingFriendlyArtilleryDamage() {
        return incomingFriendlyArtilleryDamage;
    }
    
    /**
     * Convenience method that clears the current path ranker state. 
     */
    public void clearState() {
        pathSuccessProbabilities.clear();
        incomingFriendlyArtilleryDamage.clear();
    }
}
