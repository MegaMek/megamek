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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.util.CrewSkillSummaryUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the skill bonuses implants give a unit, as gathered for display.
 *
 * <p>What matters is that the numbers a player is shown are the numbers the engine rolls against, so the piloting
 * side is checked against the Mek's own piloting-roll bonuses rather than against a copy of the rule.</p>
 */
class ImplantSkillModifiersTest {

    private static final int GUNNERY = 5;
    private static final int PILOTING = 4;

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        setImplantsOption(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY);
    }

    @Test
    void aWarriorWithNoImplantsChangesNothing() {
        Mek mek = mekWithImplants();

        assertSame(ImplantSkillModifiers.NONE, ImplantSkillModifiers.of(mek));
        assertEquals("5/4", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
    }

    /** The reported case: a 5/4 VDNI pilot is really rolling against 4/3, and the display says so. */
    @Test
    void vdniLowersBothSkillsAndMarksThem() {
        Mek mek = mekWithImplants(OptionsConstants.MD_VDNI);

        ImplantSkillModifiers modifiers = ImplantSkillModifiers.of(mek);

        assertEquals(-1, modifiers.gunnery());
        assertEquals(-1, modifiers.piloting());
        assertEquals("4*/3*", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
    }

    /** Buffered VDNI keeps the gunnery bonus but gives up the piloting one, so only gunnery is marked. */
    @Test
    void bufferedVdniLowersGunneryOnly() {
        Mek mek = mekWithImplants(OptionsConstants.MD_VDNI, OptionsConstants.MD_BVDNI);

        ImplantSkillModifiers modifiers = ImplantSkillModifiers.of(mek);

        assertEquals(-1, modifiers.gunnery());
        assertEquals(0, modifiers.piloting());
        assertEquals(1, modifiers.sources().size(), "a warrior with both holds one DNI, not two");
        assertEquals("4*/4", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
    }

    @Test
    void prototypeDniIsTheStrongestOfTheFamily() {
        Mek mek = mekWithImplants(OptionsConstants.MD_PROTO_DNI);

        ImplantSkillModifiers modifiers = ImplantSkillModifiers.of(mek);

        assertEquals(-2, modifiers.gunnery());
        assertEquals(-3, modifiers.piloting());
        assertEquals("3*/1*", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
    }

    @Test
    void enhancedImagingSteadiesPilotingOnly() {
        Mek mek = mekWithImplants(OptionsConstants.MD_EI_IMPLANT);

        ImplantSkillModifiers modifiers = ImplantSkillModifiers.of(mek);

        assertEquals(0, modifiers.gunnery());
        assertEquals(-1, modifiers.piloting());
        assertEquals("5/3*", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
    }

    @Test
    void enhancedImagingAndVdniStack() {
        Mek mek = mekWithImplants(OptionsConstants.MD_VDNI, OptionsConstants.MD_EI_IMPLANT);

        ImplantSkillModifiers modifiers = ImplantSkillModifiers.of(mek);

        assertEquals(-1, modifiers.gunnery());
        assertEquals(-2, modifiers.piloting());
        assertEquals(2, modifiers.sources().size());
    }

    /** The whole point of the merged option: Off means the implant does nothing, so nothing is marked. */
    @Test
    void withTheImplantsOptionOffNothingIsReported() {
        setImplantsOption(OptionsConstants.NEURAL_INTERFACE_MODE_OFF);
        Mek mek = mekWithImplants(OptionsConstants.MD_VDNI, OptionsConstants.MD_EI_IMPLANT);

        assertSame(ImplantSkillModifiers.NONE, ImplantSkillModifiers.of(mek));
        assertEquals("5/4", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
    }

    /** Under Full Tracking the implant is inert until the Mek carries the cockpit hardware. */
    @Test
    void underFullTrackingTheCockpitHardwareDecides() {
        setImplantsOption(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING);
        Mek bareMek = mekWithImplants(OptionsConstants.MD_VDNI);
        Mek fittedMek = mekWithImplants(OptionsConstants.MD_VDNI);
        fitDniCockpitMod(fittedMek);

        assertFalse(ImplantSkillModifiers.of(bareMek).isAny());
        assertEquals("5/4", CrewSkillSummaryUtil.getEffectiveSkillsAsString(bareMek, false));
        assertTrue(ImplantSkillModifiers.of(fittedMek).isAny());
        assertEquals("4*/3*", CrewSkillSummaryUtil.getEffectiveSkillsAsString(fittedMek, false));
    }

    /** With RPG gunnery on, every gunnery number carries the bonus, because the modifier applies to every attack. */
    @Test
    void rpgGunneryMarksEveryGunneryNumber() {
        Mek mek = mekWithImplants(OptionsConstants.MD_VDNI);

        assertEquals("4*/4*/4*/3*", CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, true));
    }

    /** A single warrior's marked skills come with the legend that explains the marks. */
    @Test
    void aMarkedWarriorGetsTheLegend() {
        Mek mek = mekWithImplants(OptionsConstants.MD_VDNI);

        assertFalse(CrewSkillSummaryUtil.getImplantAdjustmentsDescription(mek).isEmpty());
        assertEquals("", CrewSkillSummaryUtil.getImplantAdjustmentsDescription(mekWithImplants()));
    }

    /** A multi-slot crew is shown unmarked, so it gets no legend either; a legend without marks would mislead. */
    @Test
    void aMultiSlotCrewGetsNeitherMarksNorLegend() {
        Mek mek = mekWithImplants();
        mek.setCrew(crewWithImplants(CrewType.DUAL, OptionsConstants.MD_VDNI));

        assertTrue(ImplantSkillModifiers.of(mek).isAny(), "the implant itself is still active");
        assertEquals(mek.getCrew().getSkillsAsString(false),
              CrewSkillSummaryUtil.getEffectiveSkillsAsString(mek, false));
        assertEquals("", CrewSkillSummaryUtil.getImplantAdjustmentsDescription(mek));
    }

    /** A vehicle crew gets the gunnery bonus, but the engine gives it no driving bonus, so driving is unmarked. */
    @Test
    void aVehicleCrewGetsGunneryButNotDriving() {
        Tank tank = new Tank();
        tank.setGame(game);
        tank.setCrew(crewWithImplants(CrewType.CREW, OptionsConstants.MD_VDNI));

        ImplantSkillModifiers modifiers = ImplantSkillModifiers.of(tank);

        assertEquals(-1, modifiers.gunnery());
        assertEquals(0, modifiers.piloting());
        assertEquals("4*/4", CrewSkillSummaryUtil.getEffectiveSkillsAsString(tank, false));
    }

    /**
     * Guards against drift: the piloting side is the Mek's own piloting-roll bonus, so if the engine's rule changes
     * and this copy does not, the display would lie.
     */
    @Test
    void thePilotingSideMatchesTheMeksOwnPilotingRollBonuses() {
        assertPilotingMatchesEngine(mekWithImplants(OptionsConstants.MD_VDNI));
        assertPilotingMatchesEngine(mekWithImplants(OptionsConstants.MD_VDNI, OptionsConstants.MD_BVDNI));
        assertPilotingMatchesEngine(mekWithImplants(OptionsConstants.MD_PROTO_DNI));
        assertPilotingMatchesEngine(mekWithImplants(OptionsConstants.MD_EI_IMPLANT));
        assertPilotingMatchesEngine(mekWithImplants(OptionsConstants.MD_VDNI, OptionsConstants.MD_EI_IMPLANT));
    }

    private static void assertPilotingMatchesEngine(Mek mek) {
        PilotingRollData roll = mek.addEntityBonuses(new PilotingRollData(mek.getId(), 0, "base"));
        assertEquals(roll.getValue(), ImplantSkillModifiers.of(mek).piloting(),
              "piloting modifier for " + mek.getCrew().getOptions().getOptionList(", "));
    }

    private void setImplantsOption(String setting) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE).setValue(setting);
    }

    private Mek mekWithImplants(String... implants) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(crewWithImplants(CrewType.SINGLE, implants));
        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();
        return mek;
    }

    private static Crew crewWithImplants(CrewType crewType, String... implants) {
        Crew crew = new Crew(crewType);
        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            crew.setGunnery(GUNNERY, slot);
            crew.setGunneryL(GUNNERY, slot);
            crew.setGunneryM(GUNNERY, slot);
            crew.setGunneryB(GUNNERY, slot);
            crew.setPiloting(PILOTING, slot);
        }
        for (String implant : implants) {
            crew.getOptions().getOption(implant).setValue(true);
        }
        return crew;
    }

    private static void fitDniCockpitMod(Mek mek) {
        try {
            MiscType dniCockpitMod = (MiscType) EquipmentType.get("DNICockpitModification");
            mek.addEquipment(dniCockpitMod, Entity.LOC_NONE);
        } catch (Exception exception) {
            throw new AssertionError("could not fit the DNI cockpit modification", exception);
        }
    }
}
