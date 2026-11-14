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

package megamek.client.ui.dialogs.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests when the ejection warning has something to say.
 * <p>
 * The deployment and movement displays both ask the same question, so a unit walking on from the map edge gets the
 * same answer as one being deployed.
 */
class AutomaticEjectionWarningTest {

    private static final String HOSTILE_ENVIRONMENT_SUIT = "Environment Suit, Hostile";

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(true);
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
    }

    private void setAtmosphere(Atmosphere atmosphere) {
        PlanetaryConditions conditions = new PlanetaryConditions();
        conditions.setAtmosphere(atmosphere);
        game.setPlanetaryConditions(conditions);
    }

    private Mek mekSetToEject(boolean shouldEject) {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.setAutoEject(shouldEject);
        return mek;
    }

    @Test
    void anUnprotectedCrewSetToEjectInVacuumIsWarnedAbout() {
        setAtmosphere(Atmosphere.VACUUM);

        assertTrue(AutomaticEjectionWarning.isWarningDue(game, List.of(mekSetToEject(true))));
    }

    @Test
    void breathableAirNeedsNoWarning() {
        setAtmosphere(Atmosphere.STANDARD);

        assertFalse(AutomaticEjectionWarning.isWarningDue(game, List.of(mekSetToEject(true))));
    }

    @Test
    void aCrewThatWillNotEjectNeedsNoWarning() {
        setAtmosphere(Atmosphere.VACUUM);

        assertFalse(AutomaticEjectionWarning.isWarningDue(game, List.of(mekSetToEject(false))));
    }

    @Test
    void aCrewInAVacuumRatedSuitNeedsNoWarning() {
        setAtmosphere(Atmosphere.VACUUM);
        Mek protectedMek = mekSetToEject(true);
        protectedMek.getCrew().setArmorKitName(HOSTILE_ENVIRONMENT_SUIT, 0);

        assertFalse(AutomaticEjectionWarning.isWarningDue(game, List.of(protectedMek)));
    }

    @Test
    void oneUnprotectedCrewAmongProtectedOnesIsEnough() {
        setAtmosphere(Atmosphere.VACUUM);
        Mek protectedMek = mekSetToEject(true);
        protectedMek.getCrew().setArmorKitName(HOSTILE_ENVIRONMENT_SUIT, 0);

        assertTrue(AutomaticEjectionWarning.isWarningDue(game, List.of(protectedMek, mekSetToEject(true))));
    }

    @Test
    void onlyUnitsWithAnEjectionSystemAreListed() {
        Mek mekWithSwitchOff = mekSetToEject(false);
        Tank tank = new Tank();
        List<Entity> force = List.of(mekSetToEject(true), tank, mekWithSwitchOff);

        List<Entity> listed = AutomaticEjectionWarning.unitsWithAnEjectionSystem(force);

        assertEquals(2, listed.size(), "the vehicle has no ejection system");
        assertTrue(listed.contains(mekWithSwitchOff), "a Mek with the switch off is listed so it can be turned on");
        assertFalse(listed.contains(tank));
    }
}
