/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.EquipmentTypeLookup.EquipmentName;
import org.apache.logging.log4j.LogManager;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;

public class EquipmentTypeLookupTest {

    @Test
    public void allLookupKeysValid() throws IllegalAccessException {
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

    /**
     * This test is disabled because it fails to meet the expectation that unit tests should be quick,
     * but is here because it is valuable as an integration test to check whether any units have equipment
     * that cannot be loaded.
     */
    @Test
    @Ignore
    public void testFailedEquipment() {
        final Set<String> failedEquipment = new HashSet<>();

        final MechSummaryCache msc = MechSummaryCache.getInstance();
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }

        for (MechSummary ms : msc.getAllMechs()) {
            try {
                Entity entity = new MechFileParser(ms.getSourceFile(),
                        ms.getEntryName()).getEntity();
                failedEquipment.addAll(entity.failedEquipmentList);
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }

        assertEquals("", String.join(",", failedEquipment));
    }
}
