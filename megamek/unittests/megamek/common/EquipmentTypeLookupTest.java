/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;

import org.junit.jupiter.api.Test;

import megamek.common.EquipmentTypeLookup.EquipmentName;

class EquipmentTypeLookupTest {
    @Test
    void allLookupKeysValid() throws IllegalAccessException {
        // Collect all failed fields so the test results will show which field(s) failed
        final StringJoiner sj = new StringJoiner(", ");

        for (Field field : EquipmentTypeLookup.class.getFields()) {
            if (field.isAnnotationPresent(EquipmentName.class)
                    && ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC)) {
                String eqName = field.get(null).toString();
                if (EquipmentType.get(eqName) == null) {
                    sj.add(eqName);
                }
            }
        }

        assertEquals("", sj.toString());
    }
}
