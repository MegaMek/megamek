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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the optional MekWarrior Combat Suit rule.
 * <p>
 * The suit protects against what its description says it handles - air a crew would otherwise have to breathe, and
 * heat - and nothing else. It also has to stay inert unless the optional rule is switched on, because a platoon can
 * be built wearing one in MegaMekLab and a game that never opted in must not see its infantry change behaviour.
 */
class CombatSuitRulesTest {

    // CHECKSTYLE IGNORE ForbiddenWords FOR 1 LINES
    private static final String COMBAT_SUIT = "MechWarrior Combat Suit";

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
    }

    private void setCombatSuitRule(boolean inPlay) {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(inPlay);
    }

    /** A crew already on the board wearing the suit, which is how an ejected crew carries it. */
    private ConvInfantry suitedPlatoon() throws LocationFullException {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setSquadCount(1);
        platoon.setSquadSize(1);
        platoon.autoSetInternal();
        platoon.setGame(game);
        EquipmentType suit = EquipmentType.get(COMBAT_SUIT);
        assertNotNull(suit, "the MekWarrior Combat Suit equipment must exist");
        platoon.addEquipment(suit, ConvInfantry.LOC_INFANTRY);
        return platoon;
    }

    private ConvInfantry unsuitedPlatoon() {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setSquadCount(1);
        platoon.setSquadSize(1);
        platoon.autoSetInternal();
        platoon.setGame(game);
        return platoon;
    }

    @Test
    void aMekCrewMayBeIssuedASuit() {
        assertTrue(CombatSuitRules.canWearCombatSuit(new BipedMek()));
    }

    @Test
    void aVehicleCrewMayBeIssuedASuit() {
        assertTrue(CombatSuitRules.canWearCombatSuit(new Tank()));
    }

    @Test
    void aProtoMekPilotMayNotBeIssuedASuit() {
        assertFalse(CombatSuitRules.canWearCombatSuit(new ProtoMek()),
              "a ProtoMek has no ejection system, so there is nothing to wear a suit out of");
    }

    @Test
    void aCrewlessUnitMayNotBeIssuedASuit() {
        assertFalse(CombatSuitRules.canWearCombatSuit(null));
    }

    @Test
    void theSuitDoesNothingWithTheOptionOff() throws LocationFullException {
        setCombatSuitRule(false);
        ConvInfantry platoon = suitedPlatoon();

        assertTrue(platoon.hasCombatSuit(), "the armor kit is fitted either way");
        assertFalse(CombatSuitRules.isWearingCombatSuit(platoon),
              "with the optional rule off the suit must not change anything");
        assertFalse(CombatSuitRules.protectsAgainstAtmosphericTaint(platoon));
        assertFalse(CombatSuitRules.protectsAgainstTemperature(platoon, 60));
    }

    @Test
    void theSuitProtectsAgainstAirWithTheOptionOn() throws LocationFullException {
        setCombatSuitRule(true);

        assertTrue(CombatSuitRules.protectsAgainstAtmosphericTaint(suitedPlatoon()),
              "the sealed helmet carries six hours of its own air");
    }

    @Test
    void theSuitProtectsAgainstHeatButNotCold() throws LocationFullException {
        setCombatSuitRule(true);
        ConvInfantry platoon = suitedPlatoon();

        assertTrue(CombatSuitRules.protectsAgainstTemperature(platoon, 60),
              "it is an armored cooling suit, so heat is what it is for");
        assertFalse(CombatSuitRules.protectsAgainstTemperature(platoon, -40),
              "a cooling suit is not described as keeping anyone warm");
    }

    @Test
    void aPlatoonWithoutASuitIsNeverProtected() {
        setCombatSuitRule(true);
        ConvInfantry platoon = unsuitedPlatoon();

        assertFalse(CombatSuitRules.isWearingCombatSuit(platoon));
        assertFalse(CombatSuitRules.protectsAgainstAtmosphericTaint(platoon));
    }

    @Test
    void aCrewIsOnlyReadAsSuitedWhenTheRuleIsInPlay() {
        Mek mek = new BipedMek();
        mek.getCrew().setHasCombatSuit(true, 0);

        setCombatSuitRule(false);
        assertFalse(CombatSuitRules.isCrewWearingCombatSuit(mek, game));

        setCombatSuitRule(true);
        assertTrue(CombatSuitRules.isCrewWearingCombatSuit(mek, game));
    }

    /**
     * The suit is fitted as the real armor kit rather than as a flag, so everything that reads armor kits sees it.
     * Its printed divisor is 1 (TO:AUE p.129), the same as going without, so this pins the plumbing rather than a
     * change in the numbers - if the divisor is ever edited, the ejected crew gets the benefit without further work.
     */
    @Test
    void theSuitIsTheCrewsArmorKitAndCarriesItsDamageDivisor() throws LocationFullException {
        setCombatSuitRule(true);
        ConvInfantry platoon = suitedPlatoon();

        assertNotNull(platoon.getArmorKit(), "the suit must be found as the platoon's armor kit");
        assertTrue(platoon.hasArmor(), "wearing the suit counts as wearing armor");
        assertEquals(1.0, platoon.calcDamageDivisor(), 0.001,
              "the divisor comes from the kit itself, so it tracks whatever the equipment says");
    }

    private void setYear(int year) {
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(year);
    }

    /**
     * The rule defers to the equipment's own tech progression rather than repeating dates: TO:AUE p.129 puts Inner
     * Sphere prototypes at 2690 and production at 2790, and has the Clans lose it in 2820.
     */
    @Test
    void theSuitIsNotAvailableBeforeItIsInvented() {
        setCombatSuitRule(true);
        setYear(2600);
        Mek mek = new BipedMek();
        mek.getCrew().setHasCombatSuit(true, 0);

        assertFalse(CombatSuitRules.isCombatSuitAvailable(mek, game),
              "nobody had one in 2600");
        assertFalse(CombatSuitRules.isCrewWearingCombatSuit(mek, game),
              "a force carried into an earlier era must not bring suits with it");
    }

    @Test
    void theSuitIsAvailableOnceInProduction() {
        setCombatSuitRule(true);
        setYear(3025);
        Mek mek = new BipedMek();
        mek.getCrew().setHasCombatSuit(true, 0);

        assertTrue(CombatSuitRules.isCombatSuitAvailable(mek, game));
        assertTrue(CombatSuitRules.isCrewWearingCombatSuit(mek, game));
    }

    /**
     * The master equipment table gives the suit a technology base of All and no extinction date, so a Clan crew is
     * as entitled to one as an Inner Sphere crew.
     */
    @Test
    void aClanCrewMayHaveOneToo() {
        setCombatSuitRule(true);
        setYear(3025);
        Mek clanMek = new BipedMek();
        clanMek.setTechLevel(TechConstants.T_CLAN_TW);
        clanMek.getCrew().setHasCombatSuit(true, 0);

        assertTrue(CombatSuitRules.isCombatSuitAvailable(clanMek, game),
              "the suit is All tech base and never goes extinct");
    }

    @Test
    void aFullKitCostsTheSumOfItsThreePieces() {
        assertEquals(20000 + 1400 + 175, CombatSuitRules.FULL_KIT_COST_C_BILLS,
              "combat suit, combat neurohelmet and plasteel boots (A Time of War p.294)");
    }

    @Test
    void aCrewWithNoSuitIssuedIsNotReadAsSuited() {
        setCombatSuitRule(true);

        assertFalse(CombatSuitRules.isCrewWearingCombatSuit(new BipedMek(), game));
    }
}
