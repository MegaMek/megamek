package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import megamek.common.strategicBattleSystems.SBFFormation;

import java.io.IOException;

public class SBFFormationSerializer extends StdSerializer<SBFFormation> {

    public SBFFormationSerializer() {
        this(null);
    }

    public SBFFormationSerializer(Class<SBFFormation> t) {
        super(t);
    }

    @Override
    public void serialize(SBFFormation formation, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        jgen.writeStartObject();
        jgen.writeStringField("type", "SBFFormation");
        jgen.writeStringField("name", formation.getName());
        jgen.writeObjectField("units", formation.getUnits());
        //TODO damage
        jgen.writeEndObject();
    }
}

