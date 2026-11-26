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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import megamek.common.GameBoardTestCase;
import megamek.common.HexTarget;
import megamek.common.HitData;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Warship;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.bayWeapons.capital.CapitalACBayWeapon;
import megamek.common.weapons.bayWeapons.capital.CapitalLaserBayWeapon;
import megamek.common.weapons.bayWeapons.capital.CapitalMDBayWeapon;
import megamek.common.weapons.bayWeapons.subCapital.SubCapLaserBayWeapon;
import megamek.common.weapons.capitalWeapons.CapMissKillerWhaleWeapon;
import megamek.common.weapons.capitalWeapons.naval.NAC10Weapon;
import megamek.common.weapons.capitalWeapons.naval.NL35Weapon;
import megamek.common.weapons.capitalWeapons.naval.NL45Weapon;
import megamek.common.weapons.capitalWeapons.naval.NL55Weapon;
import megamek.common.weapons.subCapitalWeapons.SubCapLaserWeapon1;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

/**
 * Attacks on an airborne aerospace unit with non-cluster weapons apply the Attack Value as one block of damage. A
 * weapon bay's own damage is the {@code DAMAGE_VARIABLE} marker (-3), so a bay that dealt its damage per hit in place
 * of its Attack Value healed the target (issue #9457). A charged PPC Capacitor must still add its 5 points (issue
 * #8362).
 */
class AeroAttackValueDamageTest extends GameBoardTestCase {

    /** Short range Attack Value of one Inner Sphere PPC. */
    private static final int PPC_ATTACK_VALUE = 10;
    private static final int PPC_CAPACITOR_BONUS = 5;
    private static final int STARTING_ARMOR = 40;

    static {
        initializeBoard("AERO_ATTACK_VALUE_BOARD", """
              size 16 17
              hex 0501 0 "" ""
              hex 0505 0 "" ""
              end"""
        );
    }

    private final RulesManager originalRules = Game.rulesManager;
    private TWGameManager gameManager;
    private Game game;
    private Player attackerOwner;
    private AeroSpaceFighter target;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setBoard(getBoard("AERO_ATTACK_VALUE_BOARD"));
        attackerOwner = new Player(0, "Attacker");
        Player targetOwner = new Player(1, "Defender");
        attackerOwner.setTeam(1);
        targetOwner.setTeam(2);
        game.addPlayer(0, attackerOwner);
        game.addPlayer(1, targetOwner);

        target = new AeroSpaceFighter();
        target.setGame(game);
        target.setId(game.getNextEntityId());
        target.setChassis("Test Fighter");
        target.setModel("Target");
        target.setCrew(new Crew(CrewType.SINGLE));
        target.setOwner(targetOwner);
        target.setWeight(50);
        for (int location = 0; location < target.locations(); location++) {
            target.initializeArmor(STARTING_ARMOR, location);
        }
        placeAirborne(target, new Coords(4, 4));
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = originalRules;
    }

    private void placeAirborne(Entity entity, Coords position) {
        game.addEntity(entity);
        entity.setPosition(position);
        entity.setFacing(0);
        entity.setAltitude(3);
        entity.setDeployed(true);
    }

    private Dropship addDropShip() {
        Dropship dropShip = new Dropship();
        dropShip.setGame(game);
        dropShip.setId(game.getNextEntityId());
        dropShip.setChassis("Test DropShip");
        dropShip.setModel("Attacker");
        dropShip.setCrew(new Crew(CrewType.VESSEL));
        dropShip.setOwner(attackerOwner);
        dropShip.setWeight(3000);
        placeAirborne(dropShip, new Coords(4, 0));
        return dropShip;
    }

    private AeroSpaceFighter addFighter() {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Test Fighter");
        fighter.setModel("Attacker");
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(attackerOwner);
        fighter.setWeight(50);
        placeAirborne(fighter, new Coords(4, 0));
        return fighter;
    }

    /**
     * Fires the weapon with the handler the game itself would pick for it. A to-hit target of 2 always hits.
     */
    private Vector<Report> fire(Entity attacker, Mounted<?> weapon) {
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(weapon));
        AttackHandler handler = ((Weapon) weapon.getType())
              .getCorrectHandler(new ToHitData(2, "test"), attack, game, gameManager);
        Vector<Report> reports = new Vector<>();
        handler.handle(GamePhase.FIRING, reports);
        return reports;
    }

    private int armorLost() {
        int remaining = 0;
        for (int location = 0; location < target.locations(); location++) {
            remaining += Math.max(0, target.getArmor(location));
        }
        return (STARTING_ARMOR * target.locations()) - remaining;
    }

    @Test
    void dropShipPpcBayDealsItsAttackValue() throws Exception {
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PPC_BAY),
              Aero.LOC_NOSE);
        for (int count = 0; count < 2; count++) {
            WeaponMounted ppc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISPPC"), Aero.LOC_NOSE);
            bay.addWeaponToBay(ppc);
        }
        assertTrue(dropShip.usesWeaponBays(), "an airborne DropShip fires by bay");
        assertTrue(target.isAirborne(), "the target is airborne");

        fire(dropShip, bay);

        assertEquals(2 * PPC_ATTACK_VALUE, armorLost(), "the bay deals the Attack Value of both PPCs");
    }

    /** A capacitor in a bay counts as charged for the bay's damage and heat (TO:AUE p.149) */
    @Test
    void dropShipPpcBayCountsItsCapacitorsAsCharged() throws Exception {
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PPC_BAY),
              Aero.LOC_NOSE);
        for (int count = 0; count < 2; count++) {
            WeaponMounted ppc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISPPC"), Aero.LOC_NOSE);
            dropShip.addEquipment(EquipmentType.get("ISPPCCapacitor"), Aero.LOC_NOSE).setLinked(ppc);
            bay.addWeaponToBay(ppc);
        }

        fire(dropShip, bay);

        assertEquals(2 * (PPC_ATTACK_VALUE + PPC_CAPACITOR_BONUS), armorLost(), "each capacitor adds 5");
        assertEquals(2 * (10 + 5), dropShip.heatBuildup, "each PPC makes 10 heat and its capacitor 5");
    }

    /** Firing a bay releases the charge of its PPCs' capacitors, as firing one PPC does (TO:AUE p.149) */
    @Test
    void firingABayReleasesItsChargedCapacitors() throws Exception {
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PPC_BAY),
              Aero.LOC_NOSE);
        WeaponMounted ppc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISPPC"), Aero.LOC_NOSE);
        Mounted<?> capacitor = dropShip.addEquipment(EquipmentType.get("ISPPCCapacitor"), Aero.LOC_NOSE);
        capacitor.setLinked(ppc);
        capacitor.setModeImmediately(Mounted.MODE_CAPACITOR_CHARGE);
        bay.addWeaponToBay(ppc);

        fire(dropShip, bay);

        assertFalse(ppc.holdsCapacitorCharge(), "a hit after the shot finds no charge");
        assertEquals(Mounted.MODE_OFF, capacitor.pendingMode().getName(), "the capacitor is empty next turn");
    }

    @Test
    void fighterPpcDealsItsAttackValue() throws Exception {
        AeroSpaceFighter fighter = addFighter();
        Mounted<?> ppc = fighter.addEquipment(EquipmentType.get("ISPPC"), AeroSpaceFighter.LOC_NOSE);

        fire(fighter, ppc);

        assertEquals(PPC_ATTACK_VALUE, armorLost(), "a single PPC deals its Attack Value");
    }

    @Test
    void chargedPpcCapacitorAddsToTheAttackValue() throws Exception {
        AeroSpaceFighter fighter = addFighter();
        Mounted<?> ppc = fighter.addEquipment(EquipmentType.get("ISPPC"), AeroSpaceFighter.LOC_NOSE);
        Mounted<?> capacitor = fighter.addEquipment(EquipmentType.get("ISPPCCapacitor"), AeroSpaceFighter.LOC_NOSE);
        capacitor.setLinked(ppc);
        capacitor.setModeImmediately("Charge");
        assertEquals(1, ppc.hasChargedCapacitor(), "the capacitor is charged");

        fire(fighter, ppc);

        assertEquals(PPC_ATTACK_VALUE + PPC_CAPACITOR_BONUS, armorLost(), "the capacitor adds 5 to the Attack Value");
    }

    /**
     * A to-hit roll of 2 burns out a capacitor-charged PPC before it fires, inflicting no damage, and a bay's
     * capacitors count as charged (TO:AUE p.149): the bay's own roll checks them, with or without Aero Sanity. At a
     * to-hit number of 2 the bay still hits with its other weapons.
     */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void aBayRollOfTwoBurnsOutOnlyCapacitorPpcsInRange(boolean aeroSanity) throws Exception {
        Game.rulesManager = new TWRulesManager();
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(aeroSanity);
        game.setBoard(Board.getSpaceBoard(16, 17));
        target.setPosition(new Coords(4, 13));
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PPC_BAY),
              Aero.LOC_NOSE);
        WeaponMounted ppc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISERPPC"), Aero.LOC_NOSE);
        Mounted<?> capacitor = dropShip.addEquipment(EquipmentType.get("ISPPCCapacitor"), Aero.LOC_NOSE);
        capacitor.setLinked(ppc);
        WeaponMounted plainPpc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISERPPC"), Aero.LOC_NOSE);
        WeaponMounted shortPpc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISPPC"), Aero.LOC_NOSE);
        Mounted<?> shortCapacitor = dropShip.addEquipment(EquipmentType.get("ISPPCCapacitor"), Aero.LOC_NOSE);
        shortCapacitor.setLinked(shortPpc);
        shortCapacitor.setModeImmediately(Mounted.MODE_CAPACITOR_CHARGE);
        bay.addWeaponToBay(ppc);
        bay.addWeaponToBay(plainPpc);
        bay.addWeaponToBay(shortPpc);
        fireBayOnTwo(dropShip, bay, target, 2);

        assertTrue(ppc.isHit(), "the ER PPC burns out at long range");
        assertTrue(capacitor.isHit());
        assertFalse(plainPpc.isHit(), "a PPC without a capacitor can't burn out");
        assertFalse(shortPpc.isHit());
        assertFalse(shortCapacitor.isHit());
        assertFalse(shortPpc.isUsedThisRound());
        assertTrue(shortPpc.holdsCapacitorCharge());
        assertFalse(shortCapacitor.pendingMode().equals(Mounted.MODE_OFF));
        assertEquals(PPC_ATTACK_VALUE, armorLost(), "only the plain ER PPC deals damage");
    }

    /** If the whole bay burns out, neither a deliberate ignition nor a miss against a ground target can start a fire. */
    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void aBayThatBurnsOutCannotStartAFire(boolean aeroSanity, boolean intentionalIgnition) throws Exception {
        Game.rulesManager = new TWRulesManager();
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(aeroSanity);
        target.setAltitude(0);
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PPC_BAY),
              Aero.LOC_NOSE);
        WeaponMounted ppc = (WeaponMounted) dropShip.addEquipment(EquipmentType.get("ISPPC"), Aero.LOC_NOSE);
        Mounted<?> capacitor = dropShip.addEquipment(EquipmentType.get("ISPPCCapacitor"), Aero.LOC_NOSE);
        capacitor.setLinked(ppc);
        bay.addWeaponToBay(ppc);

        if (intentionalIgnition) {
            fireBayOnTwo(dropShip, bay, new HexTarget(target.getBoardLocation(), Targetable.TYPE_HEX_IGNITE), 2);
        } else {
            fireBayOnTwo(dropShip, bay, target, 7);
        }

        assertTrue(ppc.isHit());
        assertTrue(capacitor.isHit());
        Mockito.verify(gameManager, Mockito.never()).tryIgniteHex(any(), anyInt(), anyInt(), anyBoolean(), anyBoolean(),
              any(), anyBoolean(), anyInt(), any());
    }

    /** Bracketing and waypoint launch are separate optional rules with different eligible weapons (SO pp. 87–88). */
    @Test
    void massDriverCannotSelectBracketingOrMissileModes() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BRACKET_FIRE).setValue(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_WAYPOINT_LAUNCH).setValue(true);
        Weapon massDriverBay = new CapitalMDBayWeapon();
        Weapon capitalMissile = new CapMissKillerWhaleWeapon();
        Weapon navalAutocannon = new NAC10Weapon();
        for (Weapon type : List.of(massDriverBay, capitalMissile, navalAutocannon)) {
            type.adaptToGameOptions(game.getOptions());
        }

        assertFalse(massDriverBay.hasModeType(Weapon.MODE_CAPITAL_BRACKET_80));
        assertFalse(massDriverBay.hasModeType(Weapon.MODE_CAP_MISSILE_WAYPOINT));
        assertFalse(capitalMissile.hasModeType(Weapon.MODE_CAPITAL_BRACKET_80));
        assertTrue(capitalMissile.hasModeType(Weapon.MODE_CAP_MISSILE_WAYPOINT));
        assertTrue(navalAutocannon.hasModeType(Weapon.MODE_CAPITAL_BRACKET_80));
    }

    /** Bracketing changes a capital bay's damage. Aero Sanity applies each weapon in standard-scale points. */
    @ParameterizedTest
    @CsvSource({ "false, 3, 100, 170", "false, 3, 80, 130", "false, 3, 60, 100", "false, 4, 40, 80",
                 "true, 3, 100, 165", "true, 3, 80, 132", "true, 3, 60, 99", "true, 4, 40, 88" })
    void navalLaserBayBracketsDamageOnImpact(boolean aeroSanity, int laserCount, int percent, int expectedDamage)
          throws Exception {
        Warship warship = setUpBracketingAttack(aeroSanity);
        WeaponMounted bay = addBracketingBay(warship, new CapitalLaserBayWeapon(), percent,
              Collections.nCopies(laserCount, new NL55Weapon()));
        int startingArmor = target.getTotalArmor();

        fireBayOnTwo(warship, bay, target, 2);

        assertEquals(expectedDamage, startingArmor - target.getTotalArmor());
    }

    /** Each laser strikes a separate location. A glancing bay hit halves each laser before bracketing drops fractions. */
    @ParameterizedTest
    @CsvSource({ "false, 28, 36, 44", "true, 13, 17, 21" })
    void mixedBracketedLasersHitDifferentLocations(boolean glancing, int nose, int leftWing, int rightWing)
          throws Exception {
        Warship warship = setUpBracketingAttack(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_GLANCING_BLOWS).setValue(glancing);
        target = Mockito.spy(target);
        game.setEntity(target.getId(), target);
        Mockito.doReturn(new HitData(Aero.LOC_NOSE), new HitData(Aero.LOC_LEFT_WING), new HitData(Aero.LOC_RIGHT_WING))
              .when(target).rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
        WeaponMounted bay = addBracketingBay(warship, new CapitalLaserBayWeapon(), 80,
              List.of(new NL35Weapon(), new NL45Weapon(), new NL55Weapon()));

        fireBayOnTwo(warship, bay, target, 2);

        assertEquals(nose, target.getOArmor(Aero.LOC_NOSE) - target.getArmor(Aero.LOC_NOSE));
        assertEquals(leftWing, target.getOArmor(Aero.LOC_LEFT_WING) - target.getArmor(Aero.LOC_LEFT_WING));
        assertEquals(rightWing, target.getOArmor(Aero.LOC_RIGHT_WING) - target.getArmor(Aero.LOC_RIGHT_WING));
    }

    @Test
    void bracketedNavalAutocannonsEachDealEightyStandardDamageAndSpendTheirRound() throws Exception {
        Warship warship = setUpBracketingAttack(true);
        WeaponMounted bay = addBracketingBay(warship, new CapitalACBayWeapon(), 80,
              List.of(new NAC10Weapon(), new NAC10Weapon()));
        for (WeaponMounted gun : bay.getBayWeapons()) {
            AmmoMounted ammo = (AmmoMounted) warship.addEquipment(EquipmentType.get("Ammo NAC/10"), Aero.LOC_NOSE);
            ammo.setShotsLeft(1);
            bay.addAmmoToBay(ammo);
            gun.setLinked(ammo);
        }
        target = Mockito.spy(target);
        game.setEntity(target.getId(), target);
        Mockito.doReturn(new HitData(Aero.LOC_NOSE), new HitData(Aero.LOC_AFT))
              .when(target).rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());

        fireBayOnTwo(warship, bay, target, 2);

        assertEquals(80, target.getOArmor(Aero.LOC_NOSE) - target.getArmor(Aero.LOC_NOSE));
        assertEquals(80, target.getOArmor(Aero.LOC_AFT) - target.getArmor(Aero.LOC_AFT));
        assertTrue(bay.getBayAmmo().stream().allMatch(ammo -> ammo.getBaseShotsLeft() == 0));
    }

    @Test
    void twoSubCapitalLasersRetainEightStandardDamageEachAtBracketEighty() throws Exception {
        Warship warship = setUpBracketingAttack(true);
        WeaponMounted bay = addBracketingBay(warship, new SubCapLaserBayWeapon(), 80,
              List.of(new SubCapLaserWeapon1(), new SubCapLaserWeapon1()));
        int startingArmor = target.getTotalArmor();

        fireBayOnTwo(warship, bay, target, 2);

        assertEquals(16, startingArmor - target.getTotalArmor());
    }

    private Warship setUpBracketingAttack(boolean aeroSanity) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(aeroSanity);
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BRACKET_FIRE).setValue(true);
        game.setBoard(Board.getSpaceBoard(16, 17));
        // Bracketing a fighter is legal beyond short range (SO p. 88).
        target.setPosition(new Coords(4, 13));
        for (int location = 0; location < target.locations(); location++) {
            target.initializeArmor(500, location);
        }

        Warship warship = new Warship();
        warship.setGame(game);
        warship.setId(game.getNextEntityId());
        warship.setCrew(new Crew(CrewType.VESSEL));
        warship.setOwner(attackerOwner);
        warship.setWeight(500000);
        placeAirborne(warship, new Coords(4, 0));
        warship.setFacing(3);
        return warship;
    }

    private WeaponMounted addBracketingBay(Warship warship, Weapon bayType, int percent, List<? extends Weapon> guns)
          throws Exception {
        WeaponMounted bay = (WeaponMounted) warship.addEquipment(bayType, Aero.LOC_NOSE);
        for (Weapon gun : guns) {
            bay.addWeaponToBay((WeaponMounted) warship.addEquipment(gun, Aero.LOC_NOSE));
        }
        warship.setGameOptions();
        if (percent < 100) {
            String bracket = "Bracket " + percent + "%";
            assertTrue(bay.setModeImmediately(bracket) >= 0);
            assertEquals(bracket, bay.curMode().getName());
        }
        return bay;
    }

    /** A plasma rifle's heat meets the armor at impact, even when the same hit removes its last point. */
    @ParameterizedTest
    @CsvSource({ "false, false, 2", "false, true, 0", "true, false, 2", "true, true, 0" })
    void plasmaBayHeatMeetsTheLastArmorBeforeDamage(boolean aeroSanity, boolean core, int expectedHeat)
          throws Exception {
        Game.rulesManager = core ? new CoreRulesManager() : new TWRulesManager();
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(aeroSanity);
        Mek mek = addPlasmaTarget(EquipmentType.T_ARMOR_HEAT_DISSIPATING, 1);
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PLASMA_BAY),
              Aero.LOC_NOSE);
        addPlasmaWeapon(dropShip, bay, "ISPlasmaRifle");
        try (var dice = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(Compute::d6).thenReturn(5);
            fireBayOnTwo(dropShip, bay, mek, 2);
        }

        assertTrue(mek.getArmor(Mek.LOC_LEFT_TORSO) <= 0);
        assertEquals(expectedHeat, mek.heatFromExternal);
    }

    /** A plasma cannon heats a Mek without doing armor damage (TW pp. 139, 238). */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void plasmaCannonBayHeatsAMekWithoutDamagingItsArmor(boolean aeroSanity) throws Exception {
        game.getOptions().getOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY).setValue(aeroSanity);
        Mek mek = addPlasmaTarget(EquipmentType.T_ARMOR_STANDARD, STARTING_ARMOR);
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PLASMA_BAY),
              Aero.LOC_NOSE);
        addPlasmaWeapon(dropShip, bay, "CLPlasmaCannon");
        try (var dice = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(8);
            fireBayOnTwo(dropShip, bay, mek, 2);
        }

        assertEquals(8, mek.heatFromExternal);
        assertEquals(STARTING_ARMOR, mek.getArmor(Mek.LOC_LEFT_TORSO));
    }

    @Test
    void mixedPlasmaBayRollsHeatForEachRifleThatFires() throws Exception {
        Mek mek = addPlasmaTarget(EquipmentType.T_ARMOR_STANDARD, STARTING_ARMOR);
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PLASMA_BAY),
              Aero.LOC_NOSE);
        addPlasmaWeapon(dropShip, bay, "ISLightPlasmaRifle");
        addPlasmaWeapon(dropShip, bay, "ISPlasmaRifle");
        addPlasmaWeapon(dropShip, bay, "ISHeavyPlasmaRifle");
        addPlasmaWeapon(dropShip, bay, "CLPlasmaCannon").getLinkedAmmo().setShotsLeft(0);
        try (var dice = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(Compute::d6).thenReturn(5);
            dice.when(() -> Compute.d6(2)).thenReturn(6);
            fireBayOnTwo(dropShip, bay, mek, 2);
        }

        assertEquals(3 + 5 + 6, mek.heatFromExternal);
        assertEquals(STARTING_ARMOR - 4 - 10 - 12, mek.getArmor(Mek.LOC_LEFT_TORSO));
    }

    /** A space bay resolves as one hit, including heat from a cannon whose physical AV against a fighter is zero. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void spacePlasmaBayAppliesHeatFromTheLastRoundOfEachFiringWeapon(boolean includeRifle) throws Exception {
        game.setBoard(Board.getSpaceBoard(16, 17));
        Dropship dropShip = addDropShip();
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PLASMA_BAY),
              Aero.LOC_NOSE);
        AmmoMounted cannonAmmo = addPlasmaWeapon(dropShip, bay, "CLPlasmaCannon").getLinkedAmmo();
        cannonAmmo.setShotsLeft(1);
        if (includeRifle) {
            addPlasmaWeapon(dropShip, bay, "ISPlasmaRifle").getLinkedAmmo().setShotsLeft(1);
        }

        try (var dice = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            dice.when(Compute::d6).thenReturn(5);
            dice.when(() -> Compute.d6(2)).thenReturn(8);
            fireBayOnTwo(dropShip, bay, target, 2);
        }

        assertEquals(includeRifle ? 13 : 8, target.heatFromExternal);
        assertEquals(includeRifle ? 10 : 0, armorLost());
        assertTrue(bay.getBayAmmo().stream().allMatch(ammo -> ammo.getBaseShotsLeft() == 0));
    }

    @Test
    void buildingInPartialCoverTakesPlasmaCannonDamageAndShieldsTheMekFromHeat() throws Exception {
        Board board = new Board();
        board.load("""
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "building:2;bldg_elev:1;bldg_cf:40" ""
              hex 0103 0 "" ""
              end""", null);
        game.setBoard(board);
        Coords cover = new Coords(0, 1);
        IBuilding building = board.getBuildingAt(cover);
        Mek mek = addPlasmaTarget(EquipmentType.T_ARMOR_STANDARD, STARTING_ARMOR);
        mek.setPosition(new Coords(0, 2));
        Mockito.doReturn(new HitData(Mek.LOC_LEFT_LEG)).when(mek)
              .rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
        Dropship dropShip = addDropShip();
        dropShip.setPosition(new Coords(0, 0));
        WeaponMounted bay = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(EquipmentTypeLookup.PLASMA_BAY),
              Aero.LOC_NOSE);
        addPlasmaWeapon(dropShip, bay, "CLPlasmaCannon");

        ToHitData toHit = new ToHitData(2, "partial cover");
        toHit.setCover(LosEffects.COVER_HORIZONTAL);
        toHit.setDamagableCoverTypePrimary(LosEffects.DAMAGABLE_COVER_BUILDING);
        toHit.setCoverBuildingPrimary(building);
        toHit.setCoverLocPrimary(cover);
        fireBayOnTwo(dropShip, bay, mek, toHit);

        assertEquals(30, building.getCurrentCF(cover));
        assertEquals(0, mek.heatFromExternal);
        assertEquals(STARTING_ARMOR, mek.getTotalArmor());
    }

    private Mek addPlasmaTarget(int armorType, int torsoArmor) {
        Mek mek = Mockito.spy(new BipedMek());
        mek.setOwner(target.getOwner());
        mek.setWeight(50);
        mek.autoSetInternal();
        mek.setArmorType(armorType);
        mek.initializeArmor(torsoArmor, Mek.LOC_LEFT_TORSO);
        game.addEntity(mek);
        mek.setPosition(target.getPosition());
        Mockito.doReturn(new HitData(Mek.LOC_LEFT_TORSO)).when(mek)
              .rollHitLocation(anyInt(), anyInt(), anyInt(), any(), anyInt());
        return mek;
    }

    private WeaponMounted addPlasmaWeapon(Dropship dropShip, WeaponMounted bay, String type) throws Exception {
        WeaponMounted member = (WeaponMounted) dropShip.addEquipment(EquipmentType.get(type), Aero.LOC_NOSE);
        AmmoMounted ammo = (AmmoMounted) dropShip.addEquipment(EquipmentType.get(type + "Ammo"), Aero.LOC_NOSE);
        bay.addWeaponToBay(member);
        bay.addAmmoToBay(ammo);
        member.setLinked(ammo);
        return member;
    }

    private void fireBayOnTwo(Entity attacker, WeaponMounted bay, Targetable attackTarget, int toHitNumber) {
        fireBayOnTwo(attacker, bay, attackTarget, new ToHitData(toHitNumber, "test"));
    }

    private void fireBayOnTwo(Entity attacker, WeaponMounted bay, Targetable attackTarget, ToHitData toHit) {
        WeaponAttackAction attack = new WeaponAttackAction(attacker.getId(), attackTarget.getTargetType(),
              attackTarget.getId(), attacker.getEquipmentNum(bay));
        WeaponHandler handler = (WeaponHandler) ((Weapon) bay.getType())
              .getCorrectHandler(toHit, attack, game, gameManager);
        Roll two = mock(Roll.class);
        when(two.getIntValue()).thenReturn(2);
        when(two.getReport()).thenReturn("2");
        handler.roll = two;
        handler.handle(GamePhase.FIRING, new Vector<>());
    }
}
