package megamek.utilities.ai;


import megamek.utilities.PrincessFineTuning;

import java.util.List;

public record ActionAndState(int round, UnitAction unitAction, List<UnitState> boardUnitState){}
