package megamek.client.generator;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class MunitionTreeTest {

    HashMap<String, String> lrmHash = new HashMap<>();
    HashMap<String, String> acHash = new HashMap<>();
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
        assertEquals(1, mt.getCountOfAmmoForKey("Catapult", "C5", "any", "LRM", "Standard"));
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
        HashMap<String, Integer> binCounts = mt.getCountsofAmmosForKey("Mauler", "any", "any", "AC20");
        assertEquals(3, binCounts.values().size());
        assertEquals(1, binCounts.get("Precision"));

        // Should be zero ATM
        binCounts = mt.getCountsofAmmosForKey("Mauler", "any", "any", "ATM");
        assertEquals(0, binCounts.values().size());
        assertEquals(0, binCounts.getOrDefault("Precision", 0));


    }
}