/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.containers.MunitionTree;
import org.junit.jupiter.api.Test;

class MunitionTreeTest {

    HashMap<String, String> lrmHash = new HashMap<>();
    HashMap<String, String> acHash = new HashMap<>();
    HashMap<String, String> ltHash = new HashMap<>();

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
        // 1. A specific pilot in a specific model of a chassis has one set of
        // imperatives
        // 2. All units have a more broad set of imperatives
        // We want anything not covered in the specific case to look for the
        // higher-level imperatives
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("any", "any", "any", "AC", "Standard:Precision");
        mt.insertImperative("any", "any", "any", "LRM", "Standard:Heat-Seeking:Semi-Guided");
        mt.insertImperative("Mauler", "MAL-5X", "Tsubaki Yonjuro", "AC", "Precision:Tracer:Armor-Piercing");
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "MAL-5X", "Tsubaki Yonjuro", "AC", "Precision"));
        // Only defined for "any"
        assertEquals(1, mt.getCountOfAmmoForKey("Mauler", "MAL-5X", "Tsubaki Yonjuro", "LRM", "Standard"));
    }

    @Test
    void testGetCountsOfAmmunitionForKey() {
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
        HashMap<String, Integer> binCounts = mt.getCountsOfAmmunitionForKey("Mauler", "any", "any", "AC20");
        assertEquals(3, binCounts.values().size());
        assertEquals(1, binCounts.get("Precision"));
        assertEquals(1, binCounts.get("Caseless"));
        assertEquals(1, binCounts.get("Standard"));

        // Should be zero ATM
        binCounts = mt.getCountsOfAmmunitionForKey("Mauler", "any", "any", "ATM");
        assertEquals(0, binCounts.values().size());
        assertEquals(0, binCounts.getOrDefault("Precision", 0));
    }

    @Test
    void testOrderingOfImperatives() {
        MunitionTree mt = new MunitionTree();
        ltHash.put("Long Tom", "Standard:FAE:Flak:Thunder:Flare:Smoke");
        mt.insertImperatives("Paladin Defense SystemFluff", "any", "any", ltHash);

        // Expect Standard to be first; this will be used if all imperatives are
        // fulfilled and bins remain
        List<String> ammoOrdering = mt.getPriorityList("Paladin Defense SystemFluff", "any", "any", "Long Tom");
        assertEquals("Standard", ammoOrdering.get(0));
        assertEquals("Thunder", ammoOrdering.get(3));
        assertEquals("Smoke", ammoOrdering.get(ammoOrdering.size() - 1));
    }

    @Test
    void testADFFileFormatReading() throws IOException {
        StringReader sr = new StringReader(
              String.join("\\\n",
                    "Mauler:any:any::LRM:Smoke::AC/5:AP",
                    "Catapult:CPLT-C1:any::LRM-15:Standard:Dead-Fire:inferno",
                    "Shadow Hawk:any:any::SRM:Inferno::LRM:Dead-Fire::AC:Precision"));

        BufferedReader br = new BufferedReader(sr);
        MunitionTree mt = new MunitionTree();
        mt.readFromADF(br);
        // 3 ammo types set for Catapult C1
        assertEquals(3, mt.getCountsOfAmmunitionForKey("Catapult", "CPLT-C1", "any", "LRM-15").size());

        // 1 ammo type each for each of three Shadow Hawk weapons
        assertEquals(1, mt.getCountsOfAmmunitionForKey("Shadow Hawk", "SHD-2D", "any", "LRM-5").size());
        assertEquals(1, mt.getCountsOfAmmunitionForKey("Shadow Hawk", "SHD-2D", "any", "SRM-2").size());
        assertEquals(1, mt.getCountsOfAmmunitionForKey("Shadow Hawk", "SHD-2D", "any", "AC-5").size());
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
        for (String line : lines) {
            if (line.startsWith("any:any:any::")) {
                assertTrue(line.toLowerCase().contains(
                      "any:any:any::AC:Standard:Precision::LRM:Standard:Heat-Seeking:Semi-Guided".toLowerCase()));
            } else if (line.startsWith("Mauler:")) {
                assertTrue(line.toLowerCase()
                      .contains("Mauler:MAL-5X:Tsubaki Yonjuro::AC/5:Precision:Tracer:".toLowerCase()));
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
        // I despise Java regex's now.
        String regex = "\\w*[ -/\\\\](\\d{1,3})";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher("LRM-15");
        // Have to find before can get group ;_;
        assertTrue(m.find());
        assertNotNull(m.group(1));
    }
}
