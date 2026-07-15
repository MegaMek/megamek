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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the failure diagnostics emitted by {@link ForceDescriptor#generate()}.
 *
 * <p>These exercise the two private helpers by reflection rather than driving {@code generate()} end to
 * end: reaching the failure path requires a loaded {@link Ruleset} and an initialized
 * {@link RATGenerator}, which would make a guard test slow and data-dependent. {@code FormationTypeTest}
 * sets the precedent for reflective access in this package.</p>
 */
class ForceDescriptorTest {

    private static Object invokePrivateStatic(String methodName, Class<?> parameterType, Object argument)
          throws Exception {
        Method method = ForceDescriptor.class.getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(null, argument);
    }

    /**
     * {@code unitType} is a nullable {@link Integer} while {@code UnitType.getTypeDisplayableName} takes a
     * primitive, so passing the field straight through unboxes {@code null} and throws. Logger arguments
     * are evaluated eagerly, so the throw happened even with {@code DEBUG} disabled.
     */
    @Test
    void describeUnitTypeReturnsPlaceholderForNullUnitType() throws Exception {
        assertEquals("unspecified", invokePrivateStatic("describeUnitType", Integer.class, null));
    }

    @Test
    void describeUnitTypeReturnsDisplayableNameForKnownUnitType() throws Exception {
        assertEquals(UnitType.getTypeDisplayableName(UnitType.MEK),
              invokePrivateStatic("describeUnitType", Integer.class, UnitType.MEK));
    }

    @Test
    void describeUnitTypeHandlesNonMekUnitTypes() throws Exception {
        assertEquals(UnitType.getTypeDisplayableName(UnitType.TANK),
              invokePrivateStatic("describeUnitType", Integer.class, UnitType.TANK));
    }

    @Test
    void formatFailureTraceReturnsEmptyStringForNoAttempts() throws Exception {
        assertEquals("", invokePrivateStatic("formatFailureTrace", List.class, List.of()));
    }

    /**
     * Every attempt must land in a single log record. A per-attempt loop would flood the log, because
     * {@code generate()} runs once per leaf of the force tree.
     */
    @Test
    void formatFailureTraceJoinsAllAttemptsIntoOneRecord() throws Exception {
        List<String> failureTrace = List.of("rating=A tableEntries=0", "rating=B tableEntries=0");

        String formattedTrace = (String) invokePrivateStatic("formatFailureTrace", List.class, failureTrace);

        assertTrue(formattedTrace.contains("rating=A tableEntries=0"), "first attempt missing from trace");
        assertTrue(formattedTrace.contains("rating=B tableEntries=0"), "second attempt missing from trace");
        assertEquals(failureTrace.size(), formattedTrace.lines().filter(line -> line.contains("attempt:")).count(),
              "each attempt should appear exactly once");
    }
}
