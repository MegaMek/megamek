package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.Test;

class RulesRefMigrationAuditTest {

    private static final Pattern PAGE_SPEC = Pattern.compile("\\d+(?:-\\d+)?");
    private static final Pattern BOOK_THEN_PAGE = Pattern.compile("^(.*?)\\s+(\\d+(?:-\\d+)?)$");

    @Test
    void everyExportedEquipmentReferenceMatchesTheOriginalFixture() throws IOException {
        String fixture = System.getenv("RULES_REFS_FIXTURE");
        assertNotNull(fixture, "RULES_REFS_FIXTURE must be set");

        JsonNode equipment = new ObjectMapper().readTree(Path.of(fixture).toFile()).path("equipment");
        assertEquals(4193, equipment.size());

        EquipmentType.initializeTypes();
        int checked = 0;
        for (var fields = equipment.fields(); fields.hasNext(); ) {
            var field = fields.next();
            JsonNode value = field.getValue().get("rulesRefs");
            if (value == null || value.isNull()) {
                continue;
            }

            EquipmentType type = EquipmentType.get(field.getKey());
            assertNotNull(type, () -> "Missing equipment " + field.getKey());
            String original = value.asText();
            assertEquals(parse(original), type.getRulesRefs(),
                  () -> field.getKey() + " originally used " + original);
            checked++;
        }
        assertEquals(4131, checked);
    }

    private static List<RulesRef> parse(String original) {
        if (original == null || original.isBlank()) {
            return List.of();
        }

        String rulesRef = original.trim();
        if (rulesRef.contains(",")) {
            List<String> pageParts = new ArrayList<>();
            List<String> bookParts = new ArrayList<>();
            for (String part : rulesRef.split(",")) {
                String trimmed = part.trim();
                (PAGE_SPEC.matcher(trimmed).matches() ? pageParts : bookParts).add(trimmed);
            }
            assertEquals(1, bookParts.size(), original);
            List<RulesRef> result = new ArrayList<>();
            for (String pagePart : pageParts) {
                addPages(result, sourceBook(bookParts.getFirst()), pagePart);
            }
            return List.copyOf(result);
        }

        Matcher bookThenPage = BOOK_THEN_PAGE.matcher(rulesRef);
        if (bookThenPage.matches()) {
            List<RulesRef> result = new ArrayList<>();
            addPages(result, sourceBook(bookThenPage.group(1).trim()), bookThenPage.group(2));
            return List.copyOf(result);
        }

        return List.of(new RulesRef(sourceBook(rulesRef), null));
    }

    private static void addPages(List<RulesRef> result, SourceBookCode book, String pageSpec) {
        String[] bounds = pageSpec.split("-");
        int first = Integer.parseInt(bounds[0]);
        int last = bounds.length == 1 ? first : Integer.parseInt(bounds[1]);
        for (int page = first; page <= last; page++) {
            result.add(new RulesRef(book, page));
        }
    }

    private static SourceBookCode sourceBook(String abbrev) {
        return Arrays.stream(SourceBookCode.values())
              .filter(book -> book.getAbbrev().equals(abbrev))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Missing SourceBookCode for " + abbrev));
    }
}
