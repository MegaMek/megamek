/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An augmented Mek size (a Nova, Supernova Binary or Supernova Trinary) must not be caught by the plain size's rule.
 * When it was, the Formation menu listed the size twice: Jade Falcon showed "Binary" twice, Snow Raven (and so the Raven
 * Alliance) "Star" twice, and Steel Viper "Trinary" twice (MegaMek/mekhq#10376 items 6 and 22).
 *
 * <p>Checks every shipped ruleset. A ruleset with no rule of its own for the augmented size falls through to its
 * parent, which is fine; only a rule here that names the augmented size the same as the plain one is a failure.</p>
 */
class AugmentedSizeNameTest {

    private static final Pattern FACTION_KEY = Pattern.compile("<ruleset\\s+faction=\"([^\"]+)\"");
    private static final Pattern AUGMENTED_SIZE = Pattern.compile("(\\d+)\\^");

    @TempDir
    Path temporaryDirectory;

    @Test
    void anAugmentedMekSizeIsNotNamedLikeThePlainOne() throws Exception {
        List<String> failures = new ArrayList<>();
        File[] rulesetFiles = ShippedRulesetLoader.factionRulesDir().listFiles((dir, name) -> name.endsWith(".xml"));
        assertTrue((rulesetFiles != null) && (rulesetFiles.length > 0), "no shipped rulesets found");
        for (File rulesetFile : rulesetFiles) {
            Matcher key = FACTION_KEY.matcher(Files.readString(rulesetFile.toPath(), StandardCharsets.UTF_8));
            if (!key.find()) {
                continue;
            }
            String faction = key.group(1);
            Path ownDirectory = Files.createDirectories(temporaryDirectory.resolve(faction));
            Ruleset ruleset = ShippedRulesetLoader.load(rulesetFile.getName(), faction, ownDirectory);
            if (ruleset.getTOCNode() == null) {
                continue;
            }
            ForceDescriptor mek = new ForceDescriptor();
            mek.setFaction(faction);
            mek.setYear(3150);
            mek.setUnitType(UnitType.MEK);
            ValueNode sizes = ruleset.getTOCNode().findEchelons(mek);
            if (sizes == null) {
                continue;
            }
            Matcher augmented = AUGMENTED_SIZE.matcher(sizes.getContent());
            while (augmented.find()) {
                int echelon = Integer.parseInt(augmented.group(1));
                ForceNode augmentedRule = ruleset.findForceNode(mek, echelon, true);
                ForceNode plainRule = ruleset.findForceNode(mek, echelon, false);
                if ((augmentedRule != null) && (plainRule != null)
                      && augmentedRule.getEchelonName().equals(plainRule.getEchelonName())) {
                    failures.add(rulesetFile.getName() + " echelon " + echelon + " is \""
                          + plainRule.getEchelonName() + "\" both plain and augmented");
                }
            }
        }
        assertTrue(failures.isEmpty(), String.join("; ", failures));
    }
}
