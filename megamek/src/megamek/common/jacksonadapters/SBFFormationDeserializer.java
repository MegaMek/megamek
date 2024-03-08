package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.IOException;

public class SBFFormationDeserializer extends StdDeserializer<SBFFormation> {

    private static final String UNITS = "units";

    public SBFFormationDeserializer() {
        this(null);
    }

    public SBFFormationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SBFFormation deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(MMUReader.TYPE) || !node.get(MMUReader.TYPE).textValue().equalsIgnoreCase(MMUReader.SBF_FORMATION)) {
            throw new IOException("SBFFormationDeserializer: Wrong Deserializer chosen!");
        }

        if (node.has(UNITS)) {
            SBFFormation formation = new SBFFormation();
            new MMUReader().read(node.get(UNITS)).stream()
                    .filter(o -> o instanceof SBFUnit)
                    .map(o -> (SBFUnit) o)
                    .forEach(formation::addUnit);
            formation.setName(node.get("name").textValue());
            SBFFormationConverter.calculateStatsFromUnits(formation);
            return formation;
        } else {
            return null;
        }
    }
}