package megamek.client.bot.caspar;

import megamek.client.bot.common.AdvancedAgent;
import megamek.client.bot.common.Agent;
import megamek.client.bot.common.BoardQuickRepresentation;
import megamek.client.bot.common.GameState;
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.client.bot.common.formation.Formation;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.pathfinder.BoardClusterTracker;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CasparGameState implements GameState {
    private final AdvancedAgent agent;

    public CasparGameState(AdvancedAgent agent) {
        this.agent = agent;
    }

    @Override
    public List<Entity> getEnemyUnits() {
        return agent.getEnemyEntities();
    }

    @Override
    public Set<Coords> getUnexploredAreas() {
        throw new NotImplementedException("TODO - Implement");
    }

    @Override
    public List<Entity> getFriendlyUnits() {
        return agent.getFriendEntities();
    }

    @Override
    public List<Entity> getOwnedUnits() {
        return agent.getEntitiesOwned();
    }

    @Override
    public List<Entity> getMyTeamUnits() {
        List<Entity> units = agent.getEntitiesOwned();
        units.removeAll(agent.getFriendEntities());
        return units;
    }

    @Override
    public StructOfUnitArrays getEnemyUnitsSOU() {
        return agent.getEnemyUnitsSOU();
    }

    @Override
    public StructOfUnitArrays getFriendlyUnitsSOU() {
        return agent.getFriendlyUnitsSOU();
    }

    @Override
    public StructOfUnitArrays getOwnUnitsSOU() {
        return agent.getOwnUnitsSOU();
    }

    @Override
    public List<Hex> getStrategicPoints() {
        return List.of();
    }

    @Override
    public Optional<Formation> getFormationFor(Entity unit) {
        return agent.getFormationFor(unit);
    }

    @Override
    public Game getGame() {
        return agent.getGame();
    }

    @Override
    public BoardQuickRepresentation getBoardQuickRepresentation() {
        return agent.getBoardQuickRepresentation();
    }

    @Override
    public Player getLocalPlayer() {
        return agent.getLocalPlayer();
    }

    @Override
    public BoardClusterTracker getClusterTracker() {
        return agent.getClusterTracker();
    }
}
