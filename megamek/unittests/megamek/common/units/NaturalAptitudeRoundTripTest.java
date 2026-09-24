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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.copy.CrewRefBreak;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MULParser;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that Natural Aptitudes survive being written to and read back from a unit list (MUL), that older unit lists
 * holding the retired Natural Aptitude SPAs are converted, and that copied crews keep their aptitudes.
 */
class NaturalAptitudeRoundTripTest {

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    /**
     * A pilot already on foot. As in {@code CrewPersonalEquipmentRoundTripTest}, this is the one kind of unit whose
     * crew can be written and read back without the unit cache.
     */
    private MekWarrior pilotOnFoot() {
        MekWarrior pilot = new MekWarrior();
        pilot.setGame(game);
        pilot.setId(game.getNextEntityId());
        pilot.setModel("Test Pilot");
        pilot.setOwner(game.getPlayer(0));
        return pilot;
    }

    private static String toMul(Entity entity) throws Exception {
        StringWriter writer = new StringWriter();
        ArrayList<Entity> entities = new ArrayList<>();
        entities.add(entity);
        EntityListFile.writeEntityList(writer, entities);
        return writer.toString();
    }

    private static Crew crewReadBackFrom(String mul) throws Exception {
        MULParser parser = new MULParser(new ByteArrayInputStream(mul.getBytes(StandardCharsets.UTF_8)), null);
        Vector<Entity> entities = parser.getEntities();
        assertEquals(1, entities.size(), "one unit was written, so one should come back: " + parser.getWarningMessage());
        return entities.firstElement().getCrew();
    }

    /** Adds an attribute to the unit list's pilot element, as an older or hand-edited file might have. */
    private static String withPilotAttribute(String mul, String attribute, String value) {
        String pilotTag = '<' + MULParser.ELE_PILOT + ' ';
        assertTrue(mul.contains(pilotTag), "the unit list should have a pilot element: " + mul);
        return mul.replaceFirst(pilotTag, pilotTag + attribute + "=\"" + value + "\" ");
    }

    // region Writing

    @Test
    void aptitudesAreWrittenToTheUnitList() throws Exception {
        MekWarrior pilot = pilotOnFoot();
        pilot.getCrew().setHasNaturalAptitudeGunnery(true, 0);
        pilot.getCrew().setHasNaturalAptitudeArtillery(true, 0);
        pilot.getCrew().setHasNaturalAptitudePiloting(true, 0);
        pilot.getCrew().setHasNaturalAptitudeSmallArms(true, 0);

        String mul = toMul(pilot);

        assertTrue(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_GUNNERY + "=\"true\""), mul);
        assertTrue(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_ARTILLERY + "=\"true\""), mul);
        assertTrue(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_PILOTING + "=\"true\""), mul);
        assertTrue(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_SMALL_ARMS + "=\"true\""), mul);
    }

    @Test
    void absentAptitudesWriteNoAttributes() throws Exception {
        String mul = toMul(pilotOnFoot());

        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_GUNNERY + "=\""), mul);
        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_ARTILLERY + "=\""), mul);
        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_PILOTING + "=\""), mul);
        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_SMALL_ARMS + "=\""), mul);
    }

    @Test
    void onlyTheAptitudesHeldAreWritten() throws Exception {
        MekWarrior pilot = pilotOnFoot();
        pilot.getCrew().setHasNaturalAptitudePiloting(true, 0);

        String mul = toMul(pilot);

        assertTrue(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_PILOTING + "=\"true\""), mul);
        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_GUNNERY + "=\""), mul);
        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_ARTILLERY + "=\""), mul);
        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_SMALL_ARMS + "=\""), mul);
    }

    // endregion Writing

    // region Reading

    @Test
    void aptitudesComeBackFromTheUnitList() throws Exception {
        MekWarrior pilot = pilotOnFoot();
        pilot.getCrew().setHasNaturalAptitudeGunnery(true, 0);
        pilot.getCrew().setHasNaturalAptitudeArtillery(true, 0);
        pilot.getCrew().setHasNaturalAptitudePiloting(true, 0);
        pilot.getCrew().setHasNaturalAptitudeSmallArms(true, 0);

        Crew readBack = crewReadBackFrom(toMul(pilot));

        assertTrue(readBack.isHasNaturalAptitudeGunnery(0));
        assertTrue(readBack.isHasNaturalAptitudeArtillery(0));
        assertTrue(readBack.isHasNaturalAptitudePiloting(0));
        assertTrue(readBack.isHasNaturalAptitudeSmallArms(0));
    }

    @Test
    void eachAptitudeComesBackIndependently() throws Exception {
        MekWarrior gunneryOnly = pilotOnFoot();
        gunneryOnly.getCrew().setHasNaturalAptitudeGunnery(true, 0);
        Crew readBack = crewReadBackFrom(toMul(gunneryOnly));
        assertTrue(readBack.isHasNaturalAptitudeGunnery(0));
        assertFalse(readBack.isHasNaturalAptitudeArtillery(0));
        assertFalse(readBack.isHasNaturalAptitudePiloting(0));
        assertFalse(readBack.isHasNaturalAptitudeSmallArms(0));

        MekWarrior artilleryOnly = pilotOnFoot();
        artilleryOnly.getCrew().setHasNaturalAptitudeArtillery(true, 0);
        readBack = crewReadBackFrom(toMul(artilleryOnly));
        assertFalse(readBack.isHasNaturalAptitudeGunnery(0));
        assertTrue(readBack.isHasNaturalAptitudeArtillery(0));

        MekWarrior smallArmsOnly = pilotOnFoot();
        smallArmsOnly.getCrew().setHasNaturalAptitudeSmallArms(true, 0);
        readBack = crewReadBackFrom(toMul(smallArmsOnly));
        assertFalse(readBack.isHasNaturalAptitudeGunnery(0));
        assertTrue(readBack.isHasNaturalAptitudeSmallArms(0));
    }

    @Test
    void aUnitListWithoutAptitudesGivesNone() throws Exception {
        Crew readBack = crewReadBackFrom(toMul(pilotOnFoot()));

        assertFalse(readBack.isHasNaturalAptitudeGunnery(0));
        assertFalse(readBack.isHasNaturalAptitudeArtillery(0));
        assertFalse(readBack.isHasNaturalAptitudePiloting(0));
        assertFalse(readBack.isHasNaturalAptitudeSmallArms(0));
    }

    @Test
    void anExplicitFalseAttributeIsReadAsFalse() throws Exception {
        String mul = withPilotAttribute(toMul(pilotOnFoot()), MULParser.ATTR_NATURAL_APTITUDE_GUNNERY, "false");

        assertFalse(crewReadBackFrom(mul).isHasNaturalAptitudeGunnery(0));
    }

    // endregion Reading

    // region Legacy SPAs

    @Test
    void legacyGunnerySpaBecomesGunneryAndArtilleryAptitudes() throws Exception {
        String mul = withPilotAttribute(toMul(pilotOnFoot()), MULParser.ATTR_ADVANTAGES,
              OptionsConstants.PILOT_APTITUDE_GUNNERY);

        Crew readBack = crewReadBackFrom(mul);

        assertTrue(readBack.isHasNaturalAptitudeGunnery(0));
        assertTrue(readBack.isHasNaturalAptitudeArtillery(0), "the old SPA covered artillery rolls too");
        assertFalse(readBack.isHasNaturalAptitudePiloting(0));
    }

    @Test
    void legacyPilotingSpaBecomesPilotingAptitude() throws Exception {
        String mul = withPilotAttribute(toMul(pilotOnFoot()), MULParser.ATTR_ADVANTAGES,
              OptionsConstants.PILOT_APTITUDE_PILOTING);

        Crew readBack = crewReadBackFrom(mul);

        assertTrue(readBack.isHasNaturalAptitudePiloting(0));
        assertFalse(readBack.isHasNaturalAptitudeGunnery(0));
        assertFalse(readBack.isHasNaturalAptitudeArtillery(0));
    }

    @Test
    void bothLegacySpasAreConvertedAlongsideOtherAdvantages() throws Exception {
        String advantages = OptionsConstants.PILOT_APTITUDE_GUNNERY + "::" + OptionsConstants.PILOT_MELEE_MASTER
              + "::" + OptionsConstants.PILOT_APTITUDE_PILOTING;
        String mul = withPilotAttribute(toMul(pilotOnFoot()), MULParser.ATTR_ADVANTAGES, advantages);

        Crew readBack = crewReadBackFrom(mul);

        assertTrue(readBack.isHasNaturalAptitudeGunnery(0));
        assertTrue(readBack.isHasNaturalAptitudeArtillery(0));
        assertTrue(readBack.isHasNaturalAptitudePiloting(0));
    }

    @Test
    void legacySpaIsKeptWhenTheFileHasNoAptitudeAttributes() throws Exception {
        // Reading the (absent) per-crew-member attributes must not clear an aptitude converted from the old SPA
        String mul = withPilotAttribute(toMul(pilotOnFoot()), MULParser.ATTR_ADVANTAGES,
              OptionsConstants.PILOT_APTITUDE_PILOTING);

        assertFalse(mul.contains(MULParser.ATTR_NATURAL_APTITUDE_PILOTING + "=\""), mul);
        assertTrue(crewReadBackFrom(mul).isHasNaturalAptitudePiloting(0));
    }

    @Test
    void legacySpaIsNotReportedAsAnUnknownAdvantage() throws Exception {
        String mul = withPilotAttribute(toMul(pilotOnFoot()), MULParser.ATTR_ADVANTAGES,
              OptionsConstants.PILOT_APTITUDE_GUNNERY);
        MULParser parser = new MULParser(new ByteArrayInputStream(mul.getBytes(StandardCharsets.UTF_8)), null);

        assertFalse(parser.getWarningMessage() != null && parser.getWarningMessage().contains("aptitude"),
              "the retired SPA should be converted quietly: " + parser.getWarningMessage());
    }

    // endregion Legacy SPAs

    // region Copies

    @Test
    void copiedCrewKeepsEachSlotsAptitudes() {
        Crew crew = new Crew(CrewType.SUPERHEAVY_TRIPOD);
        crew.setHasNaturalAptitudePiloting(true, 0);
        crew.setHasNaturalAptitudeGunnery(true, 1);
        crew.setHasNaturalAptitudeArtillery(true, 2);
        crew.setHasNaturalAptitudeSmallArms(true, 1);

        Crew copy = new CrewRefBreak(crew).copy();

        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            assertEquals(crew.isHasNaturalAptitudePiloting(slot), copy.isHasNaturalAptitudePiloting(slot),
                  "piloting, slot " + slot);
            assertEquals(crew.isHasNaturalAptitudeGunnery(slot), copy.isHasNaturalAptitudeGunnery(slot),
                  "gunnery, slot " + slot);
            assertEquals(crew.isHasNaturalAptitudeArtillery(slot), copy.isHasNaturalAptitudeArtillery(slot),
                  "artillery, slot " + slot);
            assertEquals(crew.isHasNaturalAptitudeSmallArms(slot), copy.isHasNaturalAptitudeSmallArms(slot),
                  "small arms, slot " + slot);
        }
    }

    @Test
    void copiedCrewIsIndependentOfTheOriginal() {
        Crew crew = new Crew(CrewType.SINGLE);
        Crew copy = new CrewRefBreak(crew).copy();

        crew.setHasNaturalAptitudeGunnery(true, 0);

        assertFalse(copy.isHasNaturalAptitudeGunnery(0));
    }

    // endregion Copies
}
