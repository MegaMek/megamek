package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;

import java.io.IOException;
import java.util.List;

import static megamek.common.jacksonadapters.ASElementSerializer.FULL_NAME;
import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class EntityDeserializer extends StdDeserializer<Entity> {

    private static final List<String> movementModes = List.of("qt", "qw", "t", "w",
            "h", "v", "n", "s", "m", "j", "f", "g", "a", "p", "k");

    public EntityDeserializer() {
        this(null);
    }

    public EntityDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Entity deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        requireFields("TW Unit", node, FULL_NAME);

        // A TW unit (=Entity) must be loaded from the cache
        String fullName = node.get(FULL_NAME).textValue();
        MechSummary unit = MechSummaryCache.getInstance().getMech(fullName);
        try {
            if (unit != null) {
                return new MechFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();
            } else {
                throw new IllegalArgumentException("Could not retrieve unit " + fullName + " from cache!");
            }
        } catch (EntityLoadingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}