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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Messages;
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
        conditions.setAtmosphericTaint(AtmosphericTaint.TAINTED_CAUSTIC);
        assertTrue(conditions.isLethalToEjectedCrew());
    }

    @Test
    void toxicAirKillsAnEjectingCrew() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphericTaint(AtmosphericTaint.TOXIC_CAUSTIC);
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

    /**
     * The dialog names the condition rather than saying "the conditions", so each one has to come back with its own
     * wording. Heat and cold are told apart by the sign of the temperature, not by one shared "extreme" message.
     */
    @Test
    void eachConditionNamesItself() {
        PlanetaryConditions vacuum = survivableConditions();
        vacuum.setAtmosphere(Atmosphere.VACUUM);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.Vacuum"),
              vacuum.whyLethalToEjectedCrew());

        PlanetaryConditions tainted = survivableConditions();
        tainted.setAtmosphericTaint(AtmosphericTaint.TAINTED_CAUSTIC);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.TaintedAir"),
              tainted.whyLethalToEjectedCrew());

        PlanetaryConditions toxic = survivableConditions();
        toxic.setAtmosphericTaint(AtmosphericTaint.TOXIC_CAUSTIC);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.ToxicAir"),
              toxic.whyLethalToEjectedCrew());

        PlanetaryConditions tornado = survivableConditions();
        tornado.setWind(Wind.TORNADO_F1_TO_F3);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.Tornado"),
              tornado.whyLethalToEjectedCrew());

        PlanetaryConditions storm = survivableConditions();
        storm.setWind(Wind.STORM);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.Storm"),
              storm.whyLethalToEjectedCrew());

        PlanetaryConditions heat = survivableConditions();
        heat.setTemperature(60);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.ExtremeHeat"),
              heat.whyLethalToEjectedCrew());

        PlanetaryConditions cold = survivableConditions();
        cold.setTemperature(-40);
        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.ExtremeCold"),
              cold.whyLethalToEjectedCrew());
    }

    @Test
    void survivableConditionsNameNothing() {
        assertNull(survivableConditions().whyLethalToEjectedCrew());
    }

    @Test
    void twoConditionsAreBothNamed() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphere(Atmosphere.VACUUM);
        conditions.setTemperature(-40);

        assertEquals("the vacuum and the extreme cold", conditions.whyLethalToEjectedCrew(),
              "naming only the vacuum would invite the player to warm the map up and think the crew was safe");
    }

    @Test
    void threeConditionsReadAsAList() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphere(Atmosphere.VACUUM);
        conditions.setWind(Wind.TORNADO_F1_TO_F3);
        conditions.setTemperature(60);

        assertEquals("the vacuum, the tornado and the extreme heat", conditions.whyLethalToEjectedCrew());
    }

    @Test
    void aTaintIsNotNamedAlongsideVacuum() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setAtmosphere(Atmosphere.VACUUM);
        conditions.setAtmosphericTaint(AtmosphericTaint.TOXIC_CAUSTIC);

        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.Vacuum"),
              conditions.whyLethalToEjectedCrew(),
              "with no atmosphere there is nothing for a taint to be carried in, so saying both repeats itself");
    }

    @Test
    void aTornadoIsNamedInsteadOfAStormRatherThanAsWell() {
        PlanetaryConditions conditions = survivableConditions();
        conditions.setWind(Wind.TORNADO_F4);

        assertEquals(Messages.getString("PlanetaryConditions.LethalToEjectedCrew.Tornado"),
              conditions.whyLethalToEjectedCrew());
    }
}
