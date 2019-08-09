package megamek.common;

import megamek.common.loaders.EntityLoadingException;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import static org.junit.Assert.*;

public class EquipmentTypeLookupTest {

    @Test
    public void allLookupKeysValid() throws IllegalAccessException {
        // Collect all failed fields so the test results will show which field(s) failed
        final StringJoiner sj = new StringJoiner(", ");

        for (Field field : EquipmentTypeLookup.class.getFields()) {
            if (field.isAnnotationPresent(EquipmentTypeLookup.EquipmentName.class)
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
    @Ignore
    @Test
    public void testFailedEquipment() {
        final Set<String> failedEquipment = new HashSet<>();

        final MechSummaryCache msc = MechSummaryCache.getInstance();
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (MechSummary ms : msc.getAllMechs()) {
            try {
                Entity entity = new MechFileParser(ms.getSourceFile(),
                        ms.getEntryName()).getEntity();
                failedEquipment.addAll(entity.failedEquipmentList);
            } catch (EntityLoadingException e) {
                e.printStackTrace();
            }
        }

        assertEquals("", String.join(",", failedEquipment));
    }
}