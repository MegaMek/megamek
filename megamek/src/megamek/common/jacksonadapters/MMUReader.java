package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.BTObject;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MMUReader {

    static final String TYPE = "type";
    static final String SBF_FORMATION = "SBFFormation";
    static final String SBF_UNIT = "SBFUnit";
    static final String AS_ELEMENT = "ASElement";
    static final String SKILL = "skill";
    static final String SIZE = "size";
    static final String ROLE = "role";
    static final String FORCE = "force";
    static final String MODEL = "model";
    static final String CHASSIS = "chassis";
    static final String DAMAGE = "damage";
    static final String MOVE = "move";
    static final String ARMORDAMAGE = "armordamage";
    static final String SPECIALS = "specials";
    static final String ARMOR = "armor";

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // This is currently only for testing purposes with a file format for AS and SBF units
    public List<BTObject> read(File file) {
        try {
            JsonNode node = mapper.readTree(file);
            List<BTObject> list = read(node);
            return read(node);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public List<BTObject> read(JsonNode node) {
        List<BTObject> result = new ArrayList<>();
        if (node.isArray()) {
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                JsonNode arrayNode = it.next();
                parseNode(arrayNode).ifPresent(result::add);
            }
        } else {
            parseNode(node).ifPresent(result::add);
        }
        return result;
    }

    static Optional<BTObject> parseNode(JsonNode node) {
        if (!node.has(TYPE)) {
            return Optional.empty();
        }
        try {
            switch (node.get(TYPE).textValue()) {
                case SBF_FORMATION:
                    return Optional.of(mapper.treeToValue(node, SBFFormation.class));
                case AS_ELEMENT:
                    return Optional.of(mapper.treeToValue(node, AlphaStrikeElement.class));
                case SBF_UNIT:
                    return Optional.of(mapper.treeToValue(node, SBFUnit.class));
                default:
                    return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}