/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.planetaryConditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the check behind the deployment warning asked for by RFE #5579: a crew that ejects arrives outside its unit as
 * unprotected foot infantry, so the conditions that kill foot infantry kill them.
 */
class LethalToEjectedCrewTest {

    private static PlanetaryConditions survivableConditions() {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setAtmosphere(Atmosphere.STANDARD);
        conditions.setAtmosphericTaint(AtmosphericTaint.BREATHABLE);
        conditions.setWind(Wind.LIGHT_GALE);
        conditions.setTemperature(20);
        return conditions;
    }

    @Test
    void ordinaryConditionsAreSurvivable() {
        assertFalse(survivableConditions().isLethalToEjectedCrew());
    }

    @Test
    void vacuumKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphere(Atmosphere.VACUUM);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void traceAtmosphereKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphere(Atmosphere.TRACE);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void thinAtmosphereIsStillBreathableEnough() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphere(Atmosphere.THIN);
        assertFalse(conditions.isLethalToEjectedCrew());
    }

    @Test
    void taintedAirKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphericTaint(AtmosphericTaint.CAUSTIC_TAINTED);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void toxicAirKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphericTaint(AtmosphericTaint.CAUSTIC_TOXIC);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void aStormKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setWind(Wind.STORM);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void aTornadoKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setWind(Wind.TORNADO_F1_TO_F3);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void aStrongGaleIsSurvivable() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setWind(Wind.STRONG_GALE);
        assertFalse(conditions.isLethalToEjectedCrew());
    }

    @Test
    void extremeHeatKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setTemperature(60);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void extremeColdKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setTemperature(-40);
        assertTrue(conditions.isLethalToEjectedCrew());
    }
}
