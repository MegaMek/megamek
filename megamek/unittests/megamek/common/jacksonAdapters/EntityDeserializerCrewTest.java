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
package megamek.common.jacksonAdapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

/**
 * A V2 scenario unit can give its pilot Edge points and abilities (SPAs, implants, Edge triggers), and can set the
 * unit's commander flag and a Mek's auto-eject. V1 scenarios could set these; V2 could not.
 */
class EntityDeserializerCrewTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final String ATLAS = "fullname: Atlas AS7-D\n";

    @Test
    void edgePointsAreSet() throws Exception {
        Entity atlas = readUnit(ATLAS + "crew:\n  edge: 3\n");

        assertEquals(3, atlas.getCrew().getOptions().intOption(OptionsConstants.EDGE));
    }

    @Test
    void bareAbilityNameSwitchesItOn() throws Exception {
        Entity atlas = readUnit(ATLAS + "crew:\n  abilities:\n    - dodge_maneuver\n");

        assertTrue(atlas.getCrew().getOptions().booleanOption(OptionsConstants.PILOT_DODGE_MANEUVER));
    }

    @Test
    void abilityWithAValueTakesThatValue() throws Exception {
        Entity atlas = readUnit(ATLAS + "crew:\n  abilities:\n    - weapon_specialist: Medium Laser\n");

        IOption weaponSpecialist = atlas.getCrew().getOptions().getOption(OptionsConstants.GUNNERY_WEAPON_SPECIALIST);
        assertEquals("Medium Laser", weaponSpecialist.stringValue());
    }

    @Test
    void edgeTriggerCanBeSwitchedOff() throws Exception {
        Entity atlas = readUnit(ATLAS + "crew:\n  edge: 1\n  abilities:\n    - edge_when_tac: false\n");

        assertFalse(atlas.getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_TAC));
        assertTrue(atlas.getCrew().getOptions().booleanOption(OptionsConstants.EDGE_WHEN_HEAD_HIT),
              "the other triggers keep their defaults");
    }

    @Test
    void unknownAbilityStopsTheLoad() {
        Exception exception = assertThrows(Exception.class,
              () -> readUnit(ATLAS + "crew:\n  abilities:\n    - dodge_manoeuvre\n"));

        assertTrue(rootMessage(exception).contains("Unknown ability dodge_manoeuvre"), rootMessage(exception));
    }

    @Test
    void abilityThatNeedsAValueStopsTheLoadWhenGivenNone() {
        Exception exception = assertThrows(Exception.class,
              () -> readUnit(ATLAS + "crew:\n  abilities:\n    - weapon_specialist\n"));

        assertTrue(rootMessage(exception).contains("needs a value"), rootMessage(exception));
    }

    @Test
    void commanderFlagIsSet() throws Exception {
        assertTrue(readUnit(ATLAS + "commander: true\n").isCommander());
        assertFalse(readUnit(ATLAS).isCommander());
    }

    @Test
    void autoEjectCanBeSwitchedOffOnAMek() throws Exception {
        Entity atlas = readUnit(ATLAS + "autoeject: false\n");

        assertFalse(((Mek) atlas).isAutoEject());
    }

    @Test
    void autoEjectCanBeSwitchedOffOnAFighter() throws Exception {
        Entity cheetah = readUnit("fullname: Cheetah F-11\nautoeject: false\n");

        assertFalse(((Aero) cheetah).isAutoEject());
    }

    @Test
    void autoEjectCanBeSwitchedOnOnAFighter() throws Exception {
        Entity cheetah = readUnit("fullname: Cheetah F-11\nautoeject: true\n");

        assertTrue(((Aero) cheetah).isAutoEject());
    }

    private Entity readUnit(String yaml) throws Exception {
        return YAML_MAPPER.readValue(yaml, Entity.class);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return String.valueOf(cause.getMessage());
    }
}
