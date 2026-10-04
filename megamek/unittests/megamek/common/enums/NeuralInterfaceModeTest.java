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
package megamek.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

/**
 * Covers the bridge from carrying a neural implant to gaining anything by it.
 *
 * <p>This is the one definition both applications read, so what matters is that each setting says what
 * the rules say, and that anything unreadable settles on the rules being off rather than on.</p>
 */
class NeuralInterfaceModeTest {

    private static GameOptions optionsSetTo(String storedValue) {
        GameOptions gameOptions = new GameOptions();
        gameOptions.getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE).setValue(storedValue);
        return gameOptions;
    }

    /** With the rules off nothing is granted, whatever the warrior and machine carry. */
    @Test
    void offGrantsNothing() {
        assertFalse(NeuralInterfaceMode.OFF.grantsBenefit(true, true));
        assertFalse(NeuralInterfaceMode.OFF.isOn());
    }

    /** The implant alone is enough, which is the whole of the difference from Full Tracking. */
    @Test
    void pilotAbilitiesOnlyNeedsNoHardware() {
        assertTrue(NeuralInterfaceMode.PILOT_ABILITIES_ONLY.grantsBenefit(true, false));
        assertFalse(NeuralInterfaceMode.PILOT_ABILITIES_ONLY.requiresInterfaceHardware());
    }

    @Test
    void fullTrackingNeedsBothImplantAndHardware() {
        assertTrue(NeuralInterfaceMode.FULL_TRACKING.grantsBenefit(true, true));
        assertFalse(NeuralInterfaceMode.FULL_TRACKING.grantsBenefit(true, false),
              "the machine must carry the interface under this setting");
        assertTrue(NeuralInterfaceMode.FULL_TRACKING.requiresInterfaceHardware());
    }

    /** No setting grants anything to a warrior who is not implanted. */
    @Test
    void noSettingGrantsAnythingWithoutTheImplant() {
        for (NeuralInterfaceMode mode : NeuralInterfaceMode.values()) {
            assertFalse(mode.grantsBenefit(false, true), mode + " must need the implant");
        }
    }

    @Test
    void eachSettingIsReadBackFromTheGameOptions() {
        for (NeuralInterfaceMode mode : NeuralInterfaceMode.values()) {
            assertEquals(mode, NeuralInterfaceMode.from(optionsSetTo(mode.optionValue())));
        }
    }

    /**
     * An unreadable setting must switch the rules off rather than on: guessing "on" would hand out a
     * benefit the game never granted.
     */
    @Test
    void anythingUnreadableSettlesOnOff() {
        assertEquals(NeuralInterfaceMode.OFF, NeuralInterfaceMode.fromOptionValue(null));
        assertEquals(NeuralInterfaceMode.OFF, NeuralInterfaceMode.fromOptionValue("  "));
        assertEquals(NeuralInterfaceMode.OFF, NeuralInterfaceMode.fromOptionValue("Nonsense"));
        assertEquals(NeuralInterfaceMode.OFF, NeuralInterfaceMode.from(null));
    }

    /** The old Manei Domini switch lives here now: Off forbids every implant, either on setting allows them all. */
    @Test
    void offForbidsImplantsAndEitherOnSettingAllowsThem() {
        assertFalse(NeuralInterfaceMode.OFF.allowsImplants());
        assertTrue(NeuralInterfaceMode.PILOT_ABILITIES_ONLY.allowsImplants());
        assertTrue(NeuralInterfaceMode.FULL_TRACKING.allowsImplants());
        assertFalse(NeuralInterfaceMode.from(null).allowsImplants(),
              "with no game to read, implants are not allowed");
    }

    /** Stored values carry stray whitespace often enough to be worth tolerating. */
    @Test
    void aStoredValueIsTrimmedBeforeItIsRead() {
        assertEquals(NeuralInterfaceMode.FULL_TRACKING, NeuralInterfaceMode.fromOptionValue(
              "  " + OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING + " "));
    }
}
