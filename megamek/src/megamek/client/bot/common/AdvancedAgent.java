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

public interface AdvancedAgent extends Agent {


    /**
     * Get the StructOfUnitArrays for the enemy units, this is a special data structure that allows for quick access
     * to units and their positional data for fast processing.
     * @return The StructOfUnitArrays for the enemy units
     */
    StructOfUnitArrays getEnemyUnitsSOU();

    /**
     * Get the StructOfUnitArrays for the friendly units, this is a special data structure that allows for quick access
     * to units and their positional data for fast processing.
     * @return The StructOfUnitArrays for the friendly units
     */
    StructOfUnitArrays getFriendlyUnitsSOU();

    /**
     * Get the StructOfUnitArrays for the own units, this is a special data structure that allows for quick access
     * to units and their positional data for fast processing.
     * @return The StructOfUnitArrays for the own units
     */
    StructOfUnitArrays getOwnUnitsSOU();

    BoardQuickRepresentation getBoardQuickRepresentation();
}
