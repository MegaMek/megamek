package megamek.utils;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MekFileParser;

public class EntityLoader {

    private static final String RESOURCE_PATH = "testresources/megamek/common/units/";

    private EntityLoader() {
    }

    /**
     * Load the entity from the provided filename, the file must be present in the folder at
     * {@link EntityLoader#RESOURCE_PATH}.
     * @param filename name of the file with the file type extension
     * @return instantiated entity in the specified class of the unit
     * @param <UNIT> the specific class of the unit
     */
    @SuppressWarnings("unchecked")
    public static <UNIT extends Entity> UNIT loadFromFile(String filename, Class<UNIT> classType) {
        EquipmentType.initializeTypes();
        try {
            File file = new File(RESOURCE_PATH + filename);
            MekFileParser mfParser = new MekFileParser(file);
            Entity entity = mfParser.getEntity();
            if (classType.isInstance(entity)) {
                return (UNIT) entity;
            }
            fail("Entity for " + filename + " is of type " + entity.getClass().getSimpleName() +
                  " instead of " + classType.getSimpleName());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        throw new RuntimeException();
    }
}
