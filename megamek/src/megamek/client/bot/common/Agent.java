package megamek.client.bot.common;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.FireControlState;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.pathfinder.BoardClusterTracker;

import java.util.List;
import java.util.Set;

public interface Agent {
    Coords getWaypointForEntity(Entity mover);

    Player getLocalPlayer();

    UnitBehavior.BehaviorType getBehaviorType(Entity mover);
    // get homeEdge
    Set<Coords> getDestinationCoords(Entity mover);
    // get homeEdge
    Set<Coords> getDestinationCoordsWithTerrainReduction(Entity mover);
    // CardinalEdge oppositeEdge = BoardUtilities.determineOppositeEdge(mover);
    Set<Coords> getOppositeSideDestinationCoordsWithTerrainReduction(Entity mover);

    /**
     * Returns the edge of the map that the unit should move towards to reach its home edge.
     * @param unit The unit to move
     * @return The edge of the map that the unit should move towards
     */
    CardinalEdge getHomeEdge(Entity unit);

    List<Coords> getEnemyHotSpots();

    FireControlState getFireControlState();

    BoardClusterTracker getClusterTracker();

    Game getGame();

    List<Entity> getEnemyEntities();

    List<Entity> getEntitiesOwned();

    List<Entity> getFriendEntities();
}
