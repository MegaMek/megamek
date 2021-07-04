package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.Targetable;

public interface IPathRanker {

    ArrayList<RankedPath> rankPaths(List<MovePath> movePaths, IGame game, int maxRange, double fallTolerance,
            List<Entity> enemies, List<Entity> friends);

    /**
     * Performs initialization to help speed later calls of rankPath for this
     * unit on this turn. Rankers that extend this class should override this
     * function
     */
    void initUnitTurn(Entity unit, IGame game);

    double distanceToClosestEnemy(Entity entity, Coords position, IGame game);
 
    /**
     * Returns distance to the unit's home edge.
     * Gives the distance to the closest edge
     *
     * @param position Final coordinates of the proposed move.
     * @param homeEdge Unit's home edge.
     * @param game     The {@link IGame} currently in play.
     * @return The distance, in hexes to the unit's home edge.
     */
    int distanceToHomeEdge(Coords position, CardinalEdge homeEdge, IGame game);
    
    /**
     * Returns the best path of a list of ranked paths.
     * 
     * @param ps The list of ranked paths to process
     * @return "Best" out of those paths
     */
    RankedPath getBestPath(List<RankedPath> ps);
    
    /**
     * Find the closest enemy to a unit with a path
     */
    Targetable findClosestEnemy(Entity me, Coords position, IGame game);
    
    /**
     * Find the closest enemy to a unit with a path
     */
    Targetable findClosestEnemy(Entity me, Coords position, IGame game, boolean includeStrategicTargets);
}