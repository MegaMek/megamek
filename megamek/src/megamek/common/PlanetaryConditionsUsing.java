package megamek.common;

import megamek.common.planetaryconditions.PlanetaryConditions;

public interface PlanetaryConditionsUsing {

    PlanetaryConditions getPlanetaryConditions();

    void setPlanetaryConditions(PlanetaryConditions conditions);
}
