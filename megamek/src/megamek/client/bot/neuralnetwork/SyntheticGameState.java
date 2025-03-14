package megamek.client.bot.neuralnetwork;

import megamek.ai.dataset.ActionAndState;
import megamek.client.bot.common.BoardQuickRepresentation;
import megamek.client.bot.common.GameState;
import megamek.client.bot.common.StructOfUnitArrays;
import megamek.client.bot.common.formation.Formation;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.pathfinder.BoardClusterTracker;

import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SyntheticGameState implements GameState {

    private final ActionAndState currentState;

    public SyntheticGameState(ActionAndState currentState) {
        this.currentState = currentState;
    }

    @Override
    public List<Entity> getEnemyUnits() {
        return List.of();
    }

    @Override
    public Set<Coords> getUnexploredAreas() {
        return Set.of();
    }

    @Override
    public List<Entity> getFriendlyUnits() {
        return List.of();
    }

    @Override
    public List<Entity> getOwnedUnits() {
        return List.of();
    }

    @Override
    public List<Entity> getMyTeamUnits() {
        return List.of();
    }

    @Override
    public StructOfUnitArrays getEnemyUnitsSOU() {
        return null;
    }

    @Override
    public StructOfUnitArrays getFriendlyUnitsSOU() {
        return null;
    }

    @Override
    public StructOfUnitArrays getOwnUnitsSOU() {
        return null;
    }

    @Override
    public List<Coords> getStrategicPoints() {
        return List.of();
    }

    @Override
    public Game getGame() {
        return null;
    }

    @Override
    public BoardQuickRepresentation getBoardQuickRepresentation() {
        return null;
    }

    @Override
    public Player getLocalPlayer() {
        return null;
    }

    @Override
    public Optional<Formation> getFormationFor(Entity unit) {
        return Optional.empty();
    }

    @Override
    public Enumeration<ArtilleryAttackAction> getArtilleryAttacks() {
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public ArtilleryAttackAction nextElement() {
                return null;
            }
        };
    }
}
