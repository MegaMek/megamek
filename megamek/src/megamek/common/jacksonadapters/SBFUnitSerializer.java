package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import megamek.common.ForceAssignable;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitRole;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.IOException;

public class SBFUnitSerializer extends StdSerializer<SBFUnit> {

    public SBFUnitSerializer() {
        this(null);
    }

    public SBFUnitSerializer(Class<SBFUnit> t) {
        super(t);
    }

    @Override
    public void serialize(SBFUnit unit, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        boolean hasElements = !unit.getElements().isEmpty();

        jgen.writeStartObject();
        jgen.writeStringField(MMUReader.TYPE, MMUReader.SBF_UNIT);
        jgen.writeStringField("name", unit.getName());

        if (hasElements) {
            jgen.writeObjectField("elements", unit.getElements());
        } else {
            if (unit.getSkill() != 4) {
                jgen.writeNumberField("skill", unit.getSkill());
            }
            jgen.writeStringField("type", unit.getType().name());
            jgen.writeNumberField("size", unit.getSize());
            jgen.writeNumberField("tmm", unit.getTmm());
            jgen.writeStringField("mv", unit.getMovement() + unit.getMovementCode());
            jgen.writeNumberField("jump", unit.getJumpMove());
            jgen.writeStringField("trspmv", unit.getTrspMovement() + unit.getTrspMovementCode());
            jgen.writeObjectField("damage", unit.getDamage());
            jgen.writeNumberField("armor", unit.getArmor());
            jgen.writeStringField("spec", unit.getSpecialAbilities().getSpecialsDisplayString(unit));
            jgen.writeNumberField("pv", unit.getPointValue());

            //TODO damage
        }

        jgen.writeEndObject();
    }
}
