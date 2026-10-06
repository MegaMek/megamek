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
package megamek.common.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import megamek.client.ratgenerator.FactionRecord;
import org.junit.jupiter.api.Test;

/**
 * Verifies that subunits declared inside a faction file are registered as standalone factions, inherit only what they
 * cannot resolve for themselves, and keep whatever they declare.
 */
class SubunitRegistrarTest {

    private static final String PARENT_KEY = "TESTCMD";

    private static Factions2 getTestFactions() {
        return new Factions2("testresources/data/universe/factions");
    }

    private static Faction2 getRequiredFaction(Factions2 factions, String factionKey) {
        Optional<Faction2> faction = factions.getFaction(factionKey);
        assertTrue(faction.isPresent(), "Expected a faction registered under " + factionKey);
        return faction.get();
    }

    @Test
    void subunitIsRegisteredUnderComposedKey() {
        Factions2 factions = getTestFactions();

        Faction2 firstRegiment = getRequiredFaction(factions, "TESTCMD.1st");

        assertEquals("TESTCMD.1st", firstRegiment.getKey());
        assertEquals("1st Test Regiment", firstRegiment.getName());
    }

    @Test
    void subunitKeepsAnExplicitlyDeclaredKey() {
        Factions2 factions = getTestFactions();

        Faction2 thirdRegiment = getRequiredFaction(factions, "TESTCMD.CustomKey");

        assertEquals("TESTCMD.CustomKey", thirdRegiment.getKey());
        assertEquals("3rd Test Regiment", thirdRegiment.getName());
        assertTrue(factions.getFaction("TESTCMD.3rd").isEmpty(),
              "A subunit with its own key must not also register under the composed key");
    }

    @Test
    void subunitFallsBackToItsParentByDefault() {
        Factions2 factions = getTestFactions();

        Faction2 firstRegiment = getRequiredFaction(factions, "TESTCMD.1st");

        assertIterableContainsExactly(PARENT_KEY, firstRegiment);
    }

    @Test
    void subunitKeepsItsOwnFallBackFactions() {
        Factions2 factions = getTestFactions();

        Faction2 secondRegiment = getRequiredFaction(factions, "TESTCMD.2nd");

        assertIterableContainsExactly("CBS", secondRegiment);
    }

    @Test
    void subunitInheritsTagsAndNameGeneratorWhenItDeclaresNone() {
        Factions2 factions = getTestFactions();

        Faction2 firstRegiment = getRequiredFaction(factions, "TESTCMD.1st");

        assertTrue(firstRegiment.isClan(),
              "A regiment of a Clan command must count as Clan, or it gets Inner Sphere formation sizes");
        assertEquals("Clan", firstRegiment.getNameGenerator());
    }

    @Test
    void subunitTagsAndNameGeneratorAreNotOverwrittenWhenDeclared() {
        Factions2 factions = getTestFactions();

        Faction2 secondRegiment = getRequiredFaction(factions, "TESTCMD.2nd");

        assertFalse(secondRegiment.isClan(), "A subunit's own tags must win over the parent's");
        assertEquals("Generic", secondRegiment.getNameGenerator());
    }

    @Test
    void subunitKeepsItsOwnYearsActive() {
        Factions2 factions = getTestFactions();

        Faction2 parentCommand = getRequiredFaction(factions, PARENT_KEY);
        Faction2 firstRegiment = getRequiredFaction(factions, "TESTCMD.1st");

        assertTrue(parentCommand.isActiveInYear(2600));
        assertFalse(parentCommand.isActiveInYear(2400),
              "The parent started in 2600 and must not be active before it");
        assertTrue(firstRegiment.isActiveInYear(2400),
              "The regiment started in 2300 and is active independently of its parent");
    }

    @Test
    void subunitInheritsRatingLevelsThroughTheFallbackChain() {
        Factions2 factions = getTestFactions();

        Faction2 firstRegiment = getRequiredFaction(factions, "TESTCMD.1st");

        assertTrue(firstRegiment.getRatingLevels().isEmpty(),
              "The regiment declares no rating levels of its own");
        assertEquals(5, firstRegiment.getFormationBaseSize(),
              "A Clan regiment must resolve a point of five through its parent");
    }

    @Test
    void nestedSubunitsAreRegisteredAtEveryLevel() {
        Factions2 factions = getTestFactions();

        Faction2 fourthRegiment = getRequiredFaction(factions, "TESTCMD.4th");
        Faction2 battalion = getRequiredFaction(factions, "TESTCMD.4th.A");

        assertEquals("A Battalion, 4th Test Regiment", battalion.getName());
        assertIterableContainsExactly("TESTCMD.4th", battalion);
        assertNotNull(fourthRegiment.getSubunits().get("A"));
        assertSame(battalion, fourthRegiment.getSubunits().get("A"),
              "The registered battalion must be the same object the parent declares");
    }

    @Test
    void subunitsAreMarkedAsSuchAndParentsAreNot() {
        Factions2 factions = getTestFactions();

        Faction2 parentCommand = getRequiredFaction(factions, PARENT_KEY);
        Faction2 firstRegiment = getRequiredFaction(factions, "TESTCMD.1st");
        Faction2 battalion = getRequiredFaction(factions, "TESTCMD.4th.A");

        assertFalse(parentCommand.isSubunit(), "A command with its own file is not a subunit");
        assertNull(parentCommand.getParentCommand());

        assertTrue(firstRegiment.isSubunit());
        assertEquals(PARENT_KEY, firstRegiment.getParentCommand());

        assertTrue(battalion.isSubunit(), "Nesting below the first level is still a subunit");
        assertEquals("TESTCMD.4th", battalion.getParentCommand());
    }

    @Test
    void aCommandThatMerelyFallsBackToAnotherCommandIsNotASubunit() {
        Factions2 factions = getTestFactions();

        Faction2 parentCommand = getRequiredFaction(factions, PARENT_KEY);

        assertTrue(parentCommand.getFallBackFactions().contains("CBS"),
              "The fixture command falls back to another faction");
        assertFalse(parentCommand.isSubunit(),
              "Falling back to another faction must not be mistaken for being declared inside it");
    }

    @Test
    void parentRetainsItsSubunitDeclarations() {
        Factions2 factions = getTestFactions();

        Faction2 parentCommand = getRequiredFaction(factions, PARENT_KEY);

        assertEquals(4, parentCommand.getSubunits().size());
    }

    @Test
    void factionsWithoutSubunitsAreUnaffected() {
        Factions2 factions = getTestFactions();

        Faction2 bloodSpirit = getRequiredFaction(factions, "CBS");

        assertTrue(bloodSpirit.getSubunits().isEmpty());
        assertEquals("Clan Blood Spirit", bloodSpirit.getName());
        assertFalse(bloodSpirit.getFallBackFactions().contains("CBS"),
              "A faction without subunits must not gain a fallback to itself");
    }

    @Test
    void theSubunitMarkerSurvivesConversionToAFactionRecord() {
        Factions2 factions = getTestFactions();

        FactionRecord parentRecord = new FactionRecord(getRequiredFaction(factions, PARENT_KEY));
        FactionRecord regimentRecord = new FactionRecord(getRequiredFaction(factions, "TESTCMD.1st"));

        // The Force Generator's sub-faction list filters on this, so it has to survive the conversion.
        assertFalse(parentRecord.isSubunit());
        assertTrue(regimentRecord.isSubunit());
    }

    @Test
    void registeringAFactionWithoutSubunitsAddsNothing() {
        Map<String, Faction2> factions = new HashMap<>();
        SubunitRegistrar registrar = new SubunitRegistrar(factions);

        Faction2 plainCommand = new Faction2();
        plainCommand.setKey("PLAIN");
        factions.put(plainCommand.getKey(), plainCommand);

        registrar.registerSubunits(plainCommand);

        assertEquals(1, factions.size(), "A command with no subunits must not add any entry");
        assertSame(plainCommand, factions.get("PLAIN"));
        assertTrue(plainCommand.getFallBackFactions().isEmpty(),
              "A command with no subunits must be left exactly as loaded");
        assertTrue(plainCommand.getTags().isEmpty());
        assertTrue(plainCommand.getSubunits().isEmpty());
    }

    @Test
    void aLaterSubunitDeclarationReplacesAnEarlierOne() {
        Map<String, Faction2> factions = new HashMap<>();
        SubunitRegistrar registrar = new SubunitRegistrar(factions);

        Faction2 originalSubunit = new Faction2();
        Faction2 originalParent = buildParentWithSubunit(originalSubunit);
        factions.put(originalParent.getKey(), originalParent);
        registrar.registerSubunits(originalParent);

        Faction2 overridingSubunit = new Faction2();
        Faction2 overridingParent = buildParentWithSubunit(overridingSubunit);
        factions.put(overridingParent.getKey(), overridingParent);
        registrar.registerSubunits(overridingParent);

        assertSame(overridingSubunit, factions.get("OVR.1st"),
              "A faction file loaded later - a user directory copy - must override the earlier subunit too");
    }

    @Test
    void aSubunitDoesNotOverwriteAFactionThatHasItsOwnFile() {
        Map<String, Faction2> factions = new HashMap<>();
        SubunitRegistrar registrar = new SubunitRegistrar(factions);

        Faction2 standaloneFaction = new Faction2();
        standaloneFaction.setKey("OVR.1st");
        factions.put("OVR.1st", standaloneFaction);

        Faction2 parent = buildParentWithSubunit(new Faction2());
        factions.put(parent.getKey(), parent);
        registrar.registerSubunits(parent);

        assertSame(standaloneFaction, factions.get("OVR.1st"),
              "A faction with its own file must not be replaced by a subunit claiming the same key");
    }

    @Test
    void aSubunitDoesNotOverwriteAFactionFileThatLoadedAfterAnEarlierSubunit() {
        Map<String, Faction2> factions = new HashMap<>();
        SubunitRegistrar registrar = new SubunitRegistrar(factions);

        // A command declares OVR.1st as a subunit.
        Faction2 earlierParent = buildParentWithSubunit(new Faction2());
        factions.put(earlierParent.getKey(), earlierParent);
        registrar.registerSubunits(earlierParent);

        // A faction with a file of its own then claims the same key. Factions2.loadFaction puts
        // top-level factions in unconditionally, so this genuinely happens on the wrong file order.
        Faction2 standaloneFaction = new Faction2();
        standaloneFaction.setKey("OVR.1st");
        factions.put("OVR.1st", standaloneFaction);

        // A second command declaring the same subunit key must not displace that faction.
        Faction2 laterParent = buildParentWithSubunit(new Faction2());
        factions.put(laterParent.getKey(), laterParent);
        registrar.registerSubunits(laterParent);

        assertSame(standaloneFaction, factions.get("OVR.1st"),
              "A faction with its own file must survive a later subunit claiming its key, "
                    + "whatever order the files loaded in");
    }

    /**
     * Builds a parent keyed {@code OVR} declaring the given faction as its {@code 1st} subunit, which the registrar
     * will therefore register under {@code OVR.1st}.
     */
    private static Faction2 buildParentWithSubunit(Faction2 subunit) {
        Faction2 parent = new Faction2();
        parent.setKey("OVR");
        parent.getSubunits().put("1st", subunit);
        return parent;
    }

    private static void assertIterableContainsExactly(String expectedFallback, Faction2 faction) {
        assertEquals(1, faction.getFallBackFactions().size(),
              "Expected exactly one fallback faction on " + faction.getKey());
        assertEquals(expectedFallback, faction.getFallBackFactions().iterator().next());
    }
}
