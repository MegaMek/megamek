package megamek.client.bot;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.FireControlState;
import megamek.client.bot.princess.UnitBehavior;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.pathfinder.BoardClusterTracker;

import java.util.List;
import java.util.Set;

public interface Agent {
    Coords getWaypointForEntity(Entity mover);

    Player getLocalPlayer();

    UnitBehavior.BehaviorType getBehaviorType(Entity mover, Agent owner);
    // get homeEdge
    Set<Coords> getDestinationCoords(Entity mover);
    // get homeEdge
    Set<Coords> getDestinationCoordsWithTerrainReduction(Entity mover);
    // CardinalEdge oppositeEdge = BoardUtilities.determineOppositeEdge(mover);
    Set<Coords> getOppositeSideDestinationCoordsWithTerrainReduction(Entity mover);
    CardinalEdge getHomeEdge(Entity unit);

    List<Coords> getEnemyHotSpots();

    FireControlState getFireControlState();

    BoardClusterTracker getClusterTracker();
}
