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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the sidearm half of the crew personal equipment rule: which weapons one person may be issued, and when a
 * named weapon actually reaches an ejected crew.
 */
class CrewSidearmRulesTest {

    private static final String AUTO_PISTOL = "Auto-Pistol";
    private static final String LASER_PISTOL = "Laser Pistol";
    private static final String CLAN_ER_LASER_PISTOL = "Laser Pistol (ER)";
    private static final String KNIFE = "Blade (Dagger/Knife/Bayonet)";
    private static final String AUTO_RIFLE = "InfantryAssaultRifle";
    private static final String LIGHT_MORTAR = "InfantryLightMortar";
    private static final String LAW = "Rocket Launcher (LAW)";

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(true);
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
    }

    private static EquipmentType weapon(String internalName) {
        EquipmentType equipment = EquipmentType.get(internalName);
        assertNotNull(equipment, internalName + " must exist in the equipment tables");
        return equipment;
    }

    private Mek mekCarrying(String sidearmName) {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.getCrew().setSidearmName(sidearmName, 0);
        return mek;
    }

    @Test
    void theSameCrewsWhoCanWearAKitCanCarryASidearm() {
        assertTrue(CrewSidearmRules.canCarrySidearm(new BipedMek()));
        assertTrue(CrewSidearmRules.canCarrySidearm(new Tank()));
        assertFalse(CrewSidearmRules.canCarrySidearm(new ConvInfantry()),
              "a platoon's weapon is a fact about the unit, not a possession of one soldier");
        assertFalse(CrewSidearmRules.canCarrySidearm(new ProtoMek()));
        assertFalse(CrewSidearmRules.canCarrySidearm(null));
    }

    @Test
    void onePersonWeaponsAreCandidatesAndTeamWeaponsAreNot() {
        assertTrue(CrewSidearmRules.isSidearmCandidate(weapon(AUTO_PISTOL)));
        assertTrue(CrewSidearmRules.isSidearmCandidate(weapon(KNIFE)),
              "a blade is what a support team carries today, so it must stay issuable");
        assertTrue(CrewSidearmRules.isSidearmCandidate(weapon(AUTO_RIFLE)),
              "the rifle every crew is handed by default must be something a crew can also be issued");

        assertFalse(CrewSidearmRules.isSidearmCandidate(weapon(LIGHT_MORTAR)),
              "a mortar is a support weapon served by a team");
        assertFalse(CrewSidearmRules.isSidearmCandidate(weapon(LAW)),
              "a disposable one-shot is the platoon's, issued through its own rule");
        assertFalse(CrewSidearmRules.isSidearmCandidate(weapon("Spacesuit")),
              "an armor kit is not a weapon");
        assertFalse(CrewSidearmRules.isSidearmCandidate(null));
    }

    @Test
    void theChooserDrawsOnMegaMeksOwnWeaponTables() {
        List<InfantryWeapon> sidearms = CrewSidearmRules.availableSidearms();
        assertTrue(sidearms.size() > 100,
              "pistols, rifles, shotguns, SMGs and blades together run well past a hundred entries");
        assertTrue(sidearms.stream().allMatch(CrewSidearmRules::isSidearmCandidate));
    }

    @Test
    void theSidearmIsIgnoredWithTheRuleOff() {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);

        assertNull(CrewSidearmRules.crewSidearm(mekCarrying(AUTO_PISTOL), game));
    }

    @Test
    void theSidearmComesBackWithTheRuleOn() {
        assertEquals(weapon(AUTO_PISTOL), CrewSidearmRules.crewSidearm(mekCarrying(AUTO_PISTOL), game));
    }

    @Test
    void aCrewCarryingNothingHasNoSidearm() {
        assertNull(CrewSidearmRules.crewSidearm(mekCarrying(null), game));
        assertNull(CrewSidearmRules.crewSidearm(mekCarrying(""), game));
    }

    @Test
    void aNameThatIsNotAOnePersonWeaponIsRefused() {
        assertNull(CrewSidearmRules.crewSidearm(mekCarrying(LIGHT_MORTAR), game),
              "a campaign cannot hand a pilot a mortar by writing its name on the crew");
        assertNull(CrewSidearmRules.crewSidearm(mekCarrying("No Such Weapon"), game));
    }

    @Test
    void aWeaponNotInventedYetIsNotCarried() {
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(2000);

        assertNull(CrewSidearmRules.crewSidearm(mekCarrying(LASER_PISTOL), game),
              "the laser pistol is dated 2100, so nobody had one in 2000");
        assertNotNull(CrewSidearmRules.crewSidearm(mekCarrying(AUTO_PISTOL), game),
              "the auto-pistol has been around since 1950");
    }

    @Test
    void aClanCrewMayCarryAClanWeapon() {
        Mek clanMek = mekCarrying(CLAN_ER_LASER_PISTOL);
        clanMek.setTechLevel(TechConstants.T_CLAN_TW);

        assertNotNull(CrewSidearmRules.crewSidearm(clanMek, game),
              "the ER laser pistol is Clan tech from 2835, available to a Clan crew in 3025");
    }

    @Test
    void anInfantryPlatoonNeverHasACrewSidearm() {
        ConvInfantry platoon = new ConvInfantry();
        platoon.setGame(game);
        platoon.getCrew().setSidearmName(AUTO_PISTOL, 0);

        assertNull(CrewSidearmRules.crewSidearm(platoon, game),
              "whatever is written on a platoon's crew, the platoon keeps the weapon in its unit file");
    }

    @Test
    void eachKindOfCrewHasItsOwnDefaultSmallArms() {
        assertEquals(6, CrewSidearmRules.defaultSmallArms(new BipedMek()), "MekWarriors are trained to 6");
        assertEquals(5, CrewSidearmRules.defaultSmallArms(new Tank()), "vehicle crews to 5");
        assertEquals(6, CrewSidearmRules.defaultSmallArms(new AeroSpaceFighter()), "aerospace crews to 6");
        assertEquals(Crew.SMALL_ARMS_UNSET, CrewSidearmRules.defaultSmallArms(new ConvInfantry()),
              "a platoon never fires as a dismounted crew, so it has no default");
        assertEquals(Crew.SMALL_ARMS_UNSET, CrewSidearmRules.defaultSmallArms(new ProtoMek()));
        assertEquals(Crew.SMALL_ARMS_UNSET, CrewSidearmRules.defaultSmallArms(null));
    }

    @Test
    void eachKindOfCrewHasItsOwnDefaultSidearm() {
        assertEquals(LASER_PISTOL, CrewSidearmRules.defaultSidearmName(new BipedMek()),
              "a MekWarrior has a cockpit to fit a pistol in and little else");
        assertEquals(AUTO_RIFLE, CrewSidearmRules.defaultSidearmName(new Tank()),
              "a vehicle crew has a whole vehicle, and no generic submachine gun reaches past its own hex");
        assertEquals(LASER_PISTOL, CrewSidearmRules.defaultSidearmName(new AeroSpaceFighter()));
        assertNull(CrewSidearmRules.defaultSidearmName(new ConvInfantry()));
        assertNull(CrewSidearmRules.defaultSidearmName(null));
        assertTrue(CrewSidearmRules.isSidearmCandidate(weapon(LASER_PISTOL)),
              "the default must itself be something one person can carry");
    }

    @Test
    void theDefaultWeaponsReachPastTheirOwnHex() {
        // A weapon whose range value is 0 fires only at a target in its own hex, which is what made the first
        // choice of defaults useless in play: the crew showed "Target out of range" at everything.
        for (Entity ride : List.of(new BipedMek(), new Tank(), new AeroSpaceFighter())) {
            InfantryWeapon defaultWeapon = (InfantryWeapon) weapon(CrewSidearmRules.defaultSidearmName(ride));
            assertTrue(defaultWeapon.getInfantryRange() >= 1,
                  defaultWeapon.getName() + " must have a range value of at least 1");
        }
    }

    @Test
    void theDefaultsFillOnlyTheSlotsNobodyRecorded() {
        Mek mek = mekCarrying(null);

        CrewSidearmRules.recordDefaultEquipment(mek, game);

        assertEquals(LASER_PISTOL, mek.getCrew().getSidearmName(0));
        assertEquals(6, mek.getCrew().getSmallArms(0));

        Mek mekAlreadyEquipped = mekCarrying(AUTO_PISTOL);
        mekAlreadyEquipped.getCrew().setSmallArms(3, 0);
        CrewSidearmRules.recordDefaultEquipment(mekAlreadyEquipped, game);

        assertEquals(AUTO_PISTOL, mekAlreadyEquipped.getCrew().getSidearmName(0), "a named weapon is kept");
        assertEquals(3, mekAlreadyEquipped.getCrew().getSmallArms(0), "a recorded skill is never overwritten");
    }

    @Test
    void theDefaultsAreNotRecordedWithTheRuleOff() {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);
        Mek mek = mekCarrying(null);

        CrewSidearmRules.recordDefaultEquipment(mek, game);

        assertNull(mek.getCrew().getSidearmName(0));
        assertFalse(mek.getCrew().hasSmallArms(0),
              "with the rule off every crew leaves exactly as before: the rifle, fired with their gunnery");
    }

    @Test
    void aVehicleCrewIsAnsweredByTheFirstSlotThatNamesAWeapon() {
        Crew tankCrew = new Crew(CrewType.CREW);
        tankCrew.setSidearmName(null, 0);

        assertNull(CrewSidearmRules.crewSidearm(tankCrew, false, game));

        tankCrew.setSidearmName(AUTO_PISTOL, 0);
        assertEquals(weapon(AUTO_PISTOL), CrewSidearmRules.crewSidearm(tankCrew, false, game),
              "the crew is one body on the board, so one slot answers for all of them");
    }
}
