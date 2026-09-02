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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the shared answers about automatic ejection.
 * <p>
 * The dialog and the server both read these, so a unit the dialog offers to change has to be one the server would
 * actually eject, and every unit that owns the setting has to be visible in the list whether it is switched on or not.
 */
class AutomaticEjectionRulesTest {

    private Game game;
    private Mek mek;

    @BeforeEach
    void beforeEach() {
        game = new Game();
        mek = new BipedMek();
        mek.setAutoEject(true);
    }

    private void setConditionalEjection(boolean inPlay) {
        game.getOptions().getOption(OptionsConstants.RPG_CONDITIONAL_EJECTION).setValue(inPlay);
    }

    private void disarmEveryTrigger() {
        mek.setCondEjectAmmo(false);
        mek.setCondEjectEngine(false);
        mek.setCondEjectCTDest(false);
        mek.setCondEjectHeadshot(false);
    }

    @Test
    void aMekHasAnEjectionSystem() {
        assertTrue(AutomaticEjectionRules.hasEjectionSystem(mek));
    }

    @Test
    void aVehicleHasNoEjectionSystem() {
        assertFalse(AutomaticEjectionRules.hasEjectionSystem(new Tank()));
    }

    @Test
    void nothingHasAnEjectionSystem() {
        assertFalse(AutomaticEjectionRules.hasEjectionSystem(null));
    }

    @Test
    void aMekWithTheSwitchOnWillEject() {
        assertTrue(AutomaticEjectionRules.willEjectAutomatically(mek, game));
    }

    @Test
    void aMekWithTheSwitchOffWillNotEject() {
        mek.setAutoEject(false);

        assertFalse(AutomaticEjectionRules.willEjectAutomatically(mek, game));
    }

    @Test
    void everyTriggerDisarmedStillEjectsWithoutTheConditionalOption() {
        setConditionalEjection(false);
        disarmEveryTrigger();

        assertTrue(AutomaticEjectionRules.willEjectAutomatically(mek, game),
              "without the Conditional Ejection option the individual triggers do not apply");
    }

    @Test
    void everyTriggerDisarmedUnderTheConditionalOptionWillNotEject() {
        setConditionalEjection(true);
        disarmEveryTrigger();

        assertFalse(AutomaticEjectionRules.willEjectAutomatically(mek, game),
              "with the option in play and nothing armed, the server would never eject this crew");
    }

    @Test
    void oneArmedTriggerUnderTheConditionalOptionWillEject() {
        setConditionalEjection(true);
        disarmEveryTrigger();
        mek.setCondEjectAmmo(true);

        assertTrue(AutomaticEjectionRules.willEjectAutomatically(mek, game));
    }

    @Test
    void aMekWithNoEjectionSwitchStillOwnsTheSetting() {
        mek.setAutoEject(false);

        assertTrue(AutomaticEjectionRules.hasEjectionSystem(mek),
              "a Mek that is currently set not to eject must still be listed, so the player can turn it back on");
    }

    @Test
    void theSettingCanBeChangedOnAMek() {
        assertTrue(AutomaticEjectionRules.setAutomaticEjection(mek, false));
        assertFalse(mek.isAutoEject());

        assertTrue(AutomaticEjectionRules.setAutomaticEjection(mek, true));
        assertTrue(mek.isAutoEject());
    }

    @Test
    void theSettingCannotBeChangedOnAVehicle() {
        assertFalse(AutomaticEjectionRules.setAutomaticEjection(new Tank(), false),
              "a vehicle has no ejection system, so there is nothing to change");
    }
}
