package megamek.utilities.ai;

import java.util.Map;

public interface CostFunction {
    double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, BehaviorParameters behaviorParameters);

    default double resolve(UnitAction unitAction, Map<Integer, UnitState> currentUnitStates, Map<Integer, UnitState> nextUnitState, BehaviorParameters behaviorParameters) {
        return resolve(unitAction, currentUnitStates, behaviorParameters);
    }
}
