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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Mek;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.handlers.plasma.PlasmaCannonHandler;
import megamek.common.weapons.lrms.innerSphere.ISLRM20;
import megamek.common.weapons.ppc.clan.CLPlasmaCannon;
import megamek.common.weapons.ppc.innerSphere.ISERPPC;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapitalBuildingWeaponHandlerTest {
    private static final Coords HEX = new Coords(0, 0);
    private TWGameManager manager;
    private IBuilding building;
    private BipedMek attacker;
    private BipedMek target;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setup() {
        manager = spy(new TWGameManager());
        doReturn(new Vector<Report>()).when(manager).damageEntity(any(Entity.class), any(HitData.class), anyInt(),
              anyBoolean(), any(DamageType.class), anyBoolean(), anyBoolean(), anyBoolean());
        Game game = manager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_TW);
        game.setBoard(BoardLoader.initializeBoard("""
              size 3 3
              hex 0101 0 "bldg_elev:4;building:3;bldg_class:4;bldg_cf:40" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              end"""));
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        building = game.getBoard().getBuildingAt(HEX);
        attacker = mek(1, new Coords(1, 0));
        target = mek(2, HEX);
    }

    private BipedMek mek(int id, Coords coords) {
        BipedMek mek = new BipedMek();
        mek.setId(id);
        mek.setChassis("Test " + id);
        mek.setOwner(manager.getGame().getPlayer(0));
        manager.getGame().addEntity(mek);
        mek.setPosition(coords);
        return mek;
    }

    private ConvInfantry infantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(3);
        infantry.setChassis("Test Infantry");
        infantry.setOwner(manager.getGame().getPlayer(0));
        manager.getGame().addEntity(infantry);
        infantry.setPosition(HEX);
        return infantry;
    }

    /** Exercise the shared building paths without depending on random to-hit rolls or a particular weapon. */
    private class TestHandler extends WeaponHandler {
        TestHandler(int damage, boolean capital) {
            super();
            gameManager = manager;
            game = manager.getGame();
            attackingEntity = attacker;
            subjectId = attacker.getId();
            toHit = new ToHitData();
            weaponType = mock(WeaponType.class);
            when(weaponType.isCapital()).thenReturn(capital);
            nDamPerHit = damage;
            bSalvo = true;
        }

        void fireAtBuilding(int hits) {
            target = new megamek.common.units.BuildingTarget(HEX, game.getBoard(), false);
            handleBuildingDamage(new Vector<>(), building, nDamPerHit * hits, HEX);
        }

        int fireAtOccupant(int damage, boolean stickingOut) {
            target = CapitalBuildingWeaponHandlerTest.this.target;
            int remaining = absorbBuildingDamage(damage, CapitalBuildingWeaponHandlerTest.this.target,
                  toHit.getThruBldg() == null ? building.getAbsorption(HEX) : 0, new Vector<>(), building, stickingOut);
            return getBuildingDamageAdjustment(CapitalBuildingWeaponHandlerTest.this.target, building,
                  stickingOut, remaining);
        }

        void shootFromInside() {
            toHit.setThruBldg(building);
        }
    }

    @Test
    void separateWeaponHandlersShareTheAttackerTotal() {
        new TestHandler(5, false).fireAtBuilding(1);
        new TestHandler(5, false).fireAtBuilding(1);
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void missileSalvoTotalsDoNotBecomeOneThresholdHit() {
        new TestHandler(5, false).fireAtBuilding(10);
        assertEquals(37, building.getCurrentCF(HEX));
        verify(manager, never()).damageEntity(eq(target), any(), anyInt(), anyBoolean(), any(), anyBoolean(),
              anyBoolean(), anyBoolean());
    }

    @Test
    void capitalWeaponsAreConvertedToStandardDamageBeforeBuildingResolution() {
        new TestHandler(5, true).fireAtBuilding(1);
        assertEquals(37, building.getCurrentCF(HEX));
        verify(manager, times(2)).damageEntity(eq(target), any(), eq(5), eq(false), eq(DamageType.NONE),
              eq(false), eq(true), eq(false));
    }

    @Test
    void targetingAnOccupantHitsTheBuildingAndAllOccupantsOnce() {
        BipedMek other = mek(3, HEX);
        assertEquals(0, new TestHandler(50, false).fireAtOccupant(50, false));
        assertEquals(37, building.getCurrentCF(HEX));
        for (BipedMek occupant : new BipedMek[] { target, other }) {
            verify(manager, times(2)).damageEntity(eq(occupant), any(), eq(5), eq(false), eq(DamageType.NONE),
                  eq(false), eq(true), eq(false));
        }
    }

    @Test
    void internalCombatDoesNotApplyExternalCapitalProtection() {
        attacker.setPosition(HEX);
        TestHandler handler = new TestHandler(20, false);
        handler.shootFromInside();
        assertEquals(20, handler.fireAtOccupant(20, false));
        assertEquals(40, building.getCurrentCF(HEX));
    }

    @Test
    void exposedPartOfAUnitDoesNotReceiveCapitalProtection() {
        assertEquals(20, new TestHandler(20, false).fireAtOccupant(20, true));
        assertEquals(40, building.getCurrentCF(HEX));
    }

    @Test
    void weaponAttacksFromInsideBypassCapitalArmor() {
        building.setArmor(10, HEX);
        attacker.setPosition(HEX);
        new TestHandler(20, false).fireAtBuilding(1);
        assertEquals(10, building.getArmor(HEX));
        assertEquals(39, building.getCurrentCF(HEX));
    }

    @Test
    void ppcUsesFullDamageAgainstShelteredInfantry() throws Exception {
        ConvInfantry infantry = infantry();
        WeaponMounted ppc = (WeaponMounted) attacker.addEquipment(new ISERPPC(), Mek.LOC_RIGHT_TORSO);
        WeaponAttackAction action = new WeaponAttackAction(attacker.getId(), infantry.getId(),
              attacker.getEquipmentNum(ppc));
        PPCHandler handler = new PPCHandler(new ToHitData(), action, manager.getGame(), manager);

        assertEquals(10, handler.calcDamagePerHit());
        assertEquals(0, handler.absorbBuildingDamage(handler.calcDamagePerHit(), infantry,
              building.getAbsorption(HEX), new Vector<>(), building, false));
        assertEquals(39, building.getCurrentCF(HEX));

        infantry.setElevation(4);
        assertEquals(1, handler.calcDamagePerHit(), "Infantry on the roof use ordinary infantry damage");
    }

    @Test
    void missileDamageAgainstShelteredInfantryKeepsNormalClusters() throws Exception {
        ConvInfantry infantry = infantry();
        WeaponMounted lrm = (WeaponMounted) attacker.addEquipment(new ISLRM20(), Mek.LOC_RIGHT_TORSO);
        lrm.setLinked(attacker.addEquipment(EquipmentType.get("IS Ammo LRM-20"), Mek.LOC_LEFT_TORSO));
        WeaponAttackAction action = new WeaponAttackAction(attacker.getId(), infantry.getId(),
              attacker.getEquipmentNum(lrm));
        MissileWeaponHandler handler = spy(new MissileWeaponHandler(new ToHitData(), action, manager.getGame(), manager));
        doReturn(true).when(handler).allShotsHit();

        assertEquals(1, handler.calcDamagePerHit());
        assertEquals(20, handler.calcHits(new Vector<>()));
        assertEquals(5, handler.calculateNumCluster());

        infantry.setElevation(4);
        assertTrue(handler.calcDamagePerHit() > 1);
        assertEquals(1, handler.calcHits(new Vector<>()), "Exposed infantry still receive the salvo in one group");
    }

    @Test
    void plasmaHitsTheStructureInsteadOfHeatingAShelteredMek() throws Exception {
        WeaponMounted plasma = (WeaponMounted) attacker.addEquipment(new CLPlasmaCannon(), Mek.LOC_RIGHT_TORSO);
        plasma.setLinked(attacker.addEquipment(EquipmentType.get("CLPlasmaCannonAmmo"), Mek.LOC_LEFT_TORSO));
        WeaponAttackAction action = new WeaponAttackAction(attacker.getId(), target.getId(),
              attacker.getEquipmentNum(plasma));
        WeaponHandler handler = new PlasmaCannonHandler(new ToHitData(), action, manager.getGame(), manager);
        assertEquals(1, handler.calcDamagePerHit());
        assertTrue(handler.calcHits(new Vector<>()) >= 3, "Roll the cannon's 3D6 structural damage");
        assertEquals(5, handler.calculateNumCluster());

        handler.nDamPerHit = 1;
        handler.bSalvo = false;
        handler.handleEntityDamage(target, new Vector<>(), building, 10, 5, building.getAbsorption(HEX));
        handler.handleEntityDamage(target, new Vector<>(), building, 5, 5, building.getAbsorption(HEX));
        assertEquals(39, building.getCurrentCF(HEX));
        assertEquals(0, target.heatFromExternal);
    }
}
