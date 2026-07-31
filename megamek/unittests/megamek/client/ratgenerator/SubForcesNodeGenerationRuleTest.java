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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Covers a force node honouring the {@code generate} rule of each {@code <subforces>} block.
 *
 * <p>A force node may hold several blocks, and each may declare its own rule. The rule used to be
 * stored once on the parent behind a null guard, so only the first block that declared one was ever
 * honoured and every later block generated under a rule it had not asked for. ComStar's Level II has
 * ten such blocks, which is where this was found: setting the aerospace block to {@code model} to make
 * a matched fighter pair did nothing, because the Mek block above it had already claimed the node.</p>
 */
class SubForcesNodeGenerationRuleTest {

    /** Parses one {@code <subforces>} element. */
    private static SubForcesNode blockFromXml(String xml) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                                  .newDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return SubForcesNode.createFromXml(document.getDocumentElement());
    }

    private static List<String> rulesOf(List<ForceDescriptor> children) {
        return children.stream().map(ForceDescriptor::getGenerationRule).toList();
    }

    /**
     * The regression. Two blocks, two rules, one parent: each block's children must carry the rule
     * their own block declared.
     */
    @Test
    void eachBlockTagsItsOwnChildrenWithItsOwnRule() throws Exception {
        ForceDescriptor parent = new ForceDescriptor();
        SubForcesNode meks = blockFromXml("""
              <subforces generate="group"><subforce num="4">1</subforce></subforces>""");
        SubForcesNode fighters = blockFromXml("""
              <subforces generate="model"><subforce num="2">1</subforce></subforces>""");

        List<ForceDescriptor> children = new ArrayList<>(meks.generateSubForces(parent, false));
        children.addAll(fighters.generateSubForces(parent, false));

        assertEquals(List.of("group", "group", "group", "group", "model", "model"),
              rulesOf(children),
              "each block's children must carry that block's rule, not the first block's");
    }

    /**
     * The parent's own rule is still taken from the first block that declares one. The formation path
     * reads it, so it has to keep behaving as it did.
     */
    @Test
    void theParentKeepsTheFirstDeclaredRule() throws Exception {
        ForceDescriptor parent = new ForceDescriptor();
        blockFromXml("""
              <subforces generate="group"><subforce num="1">1</subforce></subforces>""")
              .generateSubForces(parent, false);
        blockFromXml("""
              <subforces generate="model"><subforce num="1">1</subforce></subforces>""")
              .generateSubForces(parent, false);

        assertEquals("group", parent.getGenerationRule());
    }

    /** A block that declares no rule leaves its children untagged rather than borrowing another's. */
    @Test
    void aBlockWithoutARuleLeavesItsChildrenUntagged() throws Exception {
        ForceDescriptor parent = new ForceDescriptor();
        blockFromXml("""
              <subforces generate="model"><subforce num="1">1</subforce></subforces>""")
              .generateSubForces(parent, false);
        List<ForceDescriptor> untagged = blockFromXml("""
              <subforces><subforce num="2">1</subforce></subforces>""")
              .generateSubForces(parent, false);

        untagged.forEach(child -> assertNull(child.getGenerationRule(),
              "a block declaring no rule must not inherit the earlier block's"));
    }

    /**
     * Attached forces are generated by their parent's rule and were never tagged; that is unchanged.
     */
    @Test
    void attachedForcesAreNotTagged() throws Exception {
        ForceDescriptor parent = new ForceDescriptor();
        List<ForceDescriptor> attached = blockFromXml("""
              <subforces generate="model"><subforce num="2">1</subforce></subforces>""")
              .generateSubForces(parent, true);

        attached.forEach(child -> assertNull(child.getGenerationRule()));
        assertNull(parent.getGenerationRule(), "an attached block must not claim the parent's rule");
    }
}
