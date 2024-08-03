package megamek.client.generator;

import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.containers.MunitionTree;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MunitionTreeTest {

    HashMap<String, String> lrmHash = new HashMap<>();
    HashMap<String, String> acHash = new HashMap<>();
    HashMap<String, String> ltHash = new HashMap<>();

    @BeforeAll
    static void setUp() {

    }

    @Test
    void testInsertImperatives() {
        MunitionTree mt = new MunitionTree();
        assertEquals(0, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM", "Standard"));
        lrmHash.put("LRM", "Standard:Smoke");
        mt.insertImperatives("Catapult", "any", "any", lrmHash);

        // Should return 1
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM", "Standard"));
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM", "Smoke"));

        // Should return 0
        assertEquals(0, mt.getCountOfAmmoForKey("Catapult", "any", "any", "SRM", "Smoke"));
        assertEquals(0, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM", "Inferno"));
        assertEquals(0, mt.getCountOfAmmoForKey("Mauler", "any", "any", "LRM", "Standard"));

        // Should return 1 using most-to-least-explicit lookup
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM5", "Standard"));
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "CPLT-C5", "any", "LRM", "Standard"));

        // Overwrite LRM imperative
        lrmHash.put("LRM", "Dead-Fire");
        mt.insertImperatives("Catapult", "any", "any", lrmHash);
        assertEquals(0, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM", "Inferno"));
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "any", "any", "LRM", "Dead-Fire"));
    }

    @Test
    void testTopLevelImperatives() {
        MunitionTree mt = new MunitionTree();
        acHash.put("AC", "Standard:Precision");
        mt.insertImperatives("any", "any", "any", acHash);

        // Expect 0 for non-AC lookup
        assertEquals(0, mt.getCountOfAmmoForKey("any", "any", "any", "LRM", "Standard"));
        // Expect 1 for all regular AC combinations
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "MAL-5X", "Bobbit Schwintz", "AC5", "Standard"));
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "any", "any", "AC20", "Precision"));
        assertEquals(1, mt.getCountOfAmmoForKey("any", "any", "any", "L/AC5", "Standard"));
    }

    @Test
    void testCompositeLevelImperatives() {
        // We want to be able to composite more- and less-explicit imperatives, e.g.
        // 1. A specific pilot in a specific model of a chassis has one set of imperatives
        // 2. All units have a more broad set of imperatives
        // We want anything not covered in the specific case to look for the higher-level imperatives
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("any", "any", "any", "AC", "Standard:Precision");
        mt.insertImperative("any", "any", "any", "LRM", "Standard:Heat-Seeking:Semi-Guided");
        mt.insertImperative("Mauler", "MAL-5X", "Tsubaki Yonjuro", "AC", "Precision:Tracer:Armor-Piercing");
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "MAL-5X", "Tsubaki Yonjuro", "AC", "Precision"));
        // Only defined for "any"
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "MAL-5X", "Tsubaki Yonjuro", "LRM", "Standard"));
    }

    @Test
    void testGetCountsOfAmmosForKey() {
        MunitionTree mt = new MunitionTree();
        lrmHash.put("LRM", "Standard:Smoke:Thunder:");
        acHash.put("AC", "Standard:Precision:Caseless");
        mt.insertImperatives("Mauler", "any", "any", lrmHash);
        mt.insertImperatives("Mauler", "any", "any", acHash);

        // Expect 0
        assertEquals(0, mt.getCountOfAmmoForKey("Mauler", "any", "any", "ATM", "LR"));

        // Expect 1
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "any", "any", "AC20", "Precision"));
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "any", "any", "LRM", "Standard"));

        // Should be three AC ammo types defined, each with a count of 1.
        HashMap<String, Integer> binCounts = mt.getCountsOfAmmosForKey("Mauler", "any", "any", "AC20");
        assertEquals(3, binCounts.values().size());
        assertEquals(1, binCounts.get("Precision"));
        assertEquals(1, binCounts.get("Caseless"));
        assertEquals(1, binCounts.get("Standard"));

        // Should be zero ATM
        binCounts = mt.getCountsOfAmmosForKey("Mauler", "any", "any", "ATM");
        assertEquals(0, binCounts.values().size());
        assertEquals(0, binCounts.getOrDefault("Precision", 0));
    }

    @Test
    void testOrderingOfImperatives() {
        MunitionTree mt = new MunitionTree();
        ltHash.put("Long Tom", "Standard:FAE:Flak:Thunder:Flare:Smoke");
        mt.insertImperatives("Paladin Defense System", "any", "any", ltHash);

        // Expect Standard to be first; this will be used if all imperatives are fulfilled and bins remain
        List<String> ammoOrdering = mt.getPriorityList("Paladin Defense System","any", "any", "Long Tom");
        assertEquals("Standard", ammoOrdering.get(0));
        assertEquals("Thunder", ammoOrdering.get(3));
        assertEquals("Smoke", ammoOrdering.get(ammoOrdering.size() - 1));
    }

    @Test
    @Disabled("Runtime is > 20 seconds")
    void testPopulateAllPossibleUnits() {
        MechSummaryCache instance = MechSummaryCache.getInstance(true);
        // Make sure no units failed loading
        assertTrue(instance.getFailedFiles().isEmpty());
        // Sanity check to make sure the loader thread didn't fail outright
        // assertTrue(instance.getAllMechs().length > 100);

        MunitionTree mt = new MunitionTree();

        // Populates one entry for each _specific_ chassis and model; this will not create an "any" entry for a given Chassis
        for (MechSummary unit: instance.getAllMechs()) {
            mt.insertImperative(unit.getFullChassis(), unit.getModel(), "any", "Machine Gun", "Standard");
        }

        // Random lookups
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "CPLT-C1", "any", "Machine Gun", "Standard"));
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "MAL-1R", "any", "Machine Gun", "Standard"));

        // Don't expect "any" variant lookups to work as they are not defined
        assertEquals(0, mt.getCountOfAmmoForKey("Demolisher", "any", "any", "Machine Gun", "Standard"));
    }

    @Test
    void testADFFileFormatReading() throws IOException {
        StringReader sr = new StringReader(
            String.join("\\\n",
        "Mauler:any:any::LRM:Smoke::AC/5:AP",
                "Catapult:CPLT-C1:any::LRM-15:Standard:Dead-Fire:inferno",
                "Shadow Hawk:any:any::SRM:Inferno::LRM:Dead-Fire::AC:Precision"
            )
        );

        BufferedReader br = new BufferedReader(sr);
        MunitionTree mt = new MunitionTree();
        mt.readFromADF(br);
        // 3 ammo types set for Catapult C1
        assertEquals(3, mt.getCountsOfAmmosForKey("Catapult", "CPLT-C1", "any", "LRM-15").size());

        // 1 ammo type each for each of three Shadow Hawk weapons
        assertEquals(1, mt.getCountsOfAmmosForKey("Shadow Hawk", "SHD-2D", "any", "LRM-5").size());
        assertEquals(1, mt.getCountsOfAmmosForKey("Shadow Hawk", "SHD-2D", "any", "SRM-2").size());
        assertEquals(1, mt.getCountsOfAmmosForKey("Shadow Hawk", "SHD-2D", "any", "AC-5").size());
    }

    @Test
    void testADFFormatTextOutput() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter bw = new BufferedWriter(sw);
        MunitionTree mt = new MunitionTree();

        mt.insertImperative("any", "any", "any", "AC", "Standard:Precision");
        mt.insertImperative("any", "any", "any", "LRM", "Standard:Heat-Seeking:Semi-Guided");
        mt.insertImperative("Mauler", "MAL-5X", "Tsubaki Yonjuro", "AC/5", "Precision:Tracer:Armor-Piercing");
        mt.insertImperative("Shadow Hawk", "SHD-2D", "any", "LRM-5", "Dead-Fire");
        mt.insertImperative("Shadow Hawk", "SHD-2D", "any", "SRM", "Inferno");
        mt.insertImperative("Shadow Hawk", "SHD-2D", "any", "AC", "Precision");

        mt.writeToADFFormat(bw);
        String[] lines = sw.toString().split("\\n");
        // Lines are generated in map key order, so basically random.
        for (String line: lines) {
            if (line.startsWith("any:any:any::")) {
                assertTrue(line.toLowerCase().contains("any:any:any::AC:Standard:Precision::LRM:Standard:Heat-Seeking:Semi-Guided".toLowerCase()));
            } else if (line.startsWith("Mauler:")) {
                assertTrue(line.toLowerCase().contains("Mauler:MAL-5X:Tsubaki Yonjuro::AC/5:Precision:Tracer:".toLowerCase()));
            } else if (line.startsWith("Shadow Hawk:")) {
                assertTrue(line.toLowerCase().contains("Shadow Hawk:SHD-2D:any::".toLowerCase()));
            }
        }
    }

    @Test
    void testLoadNodeCopyConstructor() {
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Shadow Hawk", "SHD-2D", "any", "LRM-5", "Dead-Fire");
        MunitionTree copy = new MunitionTree(mt);
        assertNotEquals(mt, copy);
        assertEquals(copy.getCountOfAmmoForKey("Shadow Hawk", "SHD-2D", "any", "LRM-5", "Dead-Fire"), 1);

        // Add another imperative to mt; copy shouldn't see it.
        mt.insertImperative("any", "any", "any", "LRM", "Standard:Heat-Seeking:Semi-Guided");
        assertEquals(copy.getCountOfAmmoForKey("Catapult", "CPLT-C1", "Werner Herzgod", "LRM-15", "Standard"), 0);
        assertEquals(mt.getCountOfAmmoForKey("Catapult", "CPLT-C1", "Werner Herzgod", "LRM-15", "Standard"), 1);

    }

    @Test
    void testRegex() {
        // I despise Java regexes now.
        String regex = "\\w*[ -/\\\\](\\d{1,3})";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher("LRM-15");
        // Have to find before can get group ;_;
        assertTrue(m.find());
        assertNotNull(m.group(1));
    }
}