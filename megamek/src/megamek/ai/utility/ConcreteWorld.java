package megamek.ai.utility;

import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitState;
import megamek.client.bot.caspar.ai.utility.tw.ClusteringService;
import megamek.client.bot.caspar.ai.utility.tw.context.StructOfArraysEntity;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Targetable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConcreteWorld implements World {

    private List<Entity> myUnits;
    private List<Entity> alliedUnits;
    private List<Entity> enemyUnits;
    private QuickBoardRepresentation quickBoardRepresentation;
    private ClusteringService clusteringService;
    private StructOfUnitArrays structOfEnemiesArrays;
    private StructOfUnitArrays structOfAllyArrays;
    private StructOfUnitArrays structOfOwnUnitsArrays;

    public ConcreteWorld(
        UnitAction unitAction,
        Map<Integer, UnitState> currentUnitStates,
        QuickBoardRepresentation quickBoardRepresentation,
        ClusteringService clusteringService)
    {
        this.myUnits = new ArrayList<>();
        this.alliedUnits = new ArrayList<>();
        this.enemyUnits = new ArrayList<>();
        int currentPlayerId = currentUnitStates.get(unitAction.id()).playerId();
        int currentTeamId = currentUnitStates.get(unitAction.id()).teamId();
        for (UnitState unitState : currentUnitStates.values()) {
            if (unitState.teamId() == currentTeamId) {
                if (unitState.playerId() == currentPlayerId) {
                    myUnits.add(unitState.entity());
                } else {
                    alliedUnits.add(unitState.entity());
                }
            } else {
                enemyUnits.add(unitState.entity());
            }
        }

        this.alliedUnits = new ArrayList<>();
        this.enemyUnits = new ArrayList<>();
        this.quickBoardRepresentation = quickBoardRepresentation;
        this.clusteringService = clusteringService;
        this.clusteringService.buildClusters(new ArrayList<>(myUnits));

        this.structOfEnemiesArrays = new StructOfArraysEntity(enemyUnits);
        this.structOfAllyArrays = new StructOfArraysEntity(alliedUnits);
        this.structOfOwnUnitsArrays = new StructOfArraysEntity(myUnits);
    }

    @Override
    public List<Targetable> getMyUnits() {
        return new ArrayList<>(myUnits);
    }

    @Override
    public List<Targetable> getAlliedUnits() {
        return new ArrayList<>(alliedUnits);
    }

    @Override
    public List<Targetable> getEnemyUnits() {
        return new ArrayList<>(enemyUnits);
    }

    @Override
    public boolean useBooleanOption(String option) {
        return false;
    }

    @Override
    public double[] getHeatmap() {
        return quickBoardRepresentation.getNormalizedThreatLevelHeatmap();
    }

    @Override
    public QuickBoardRepresentation getQuickBoardRepresentation() {
        return quickBoardRepresentation;
    }

    @Override
    public StructOfUnitArrays getStructOfEnemyUnitArrays() {
        return structOfEnemiesArrays;
    }

    @Override
    public StructOfUnitArrays getStructOfAllyUnitArrays() {
        return structOfAllyArrays;
    }

    @Override
    public StructOfUnitArrays getStructOfOwnUnitsArrays() {
        return structOfOwnUnitsArrays;
    }

    @Override
    public Coords getEntityClusterCentroid(Targetable entity) {
        return clusteringService.getClusterMidpoint(entity);
    }
}
