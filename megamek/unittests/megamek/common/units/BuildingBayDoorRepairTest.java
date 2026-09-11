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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import megamek.common.Player;
import megamek.common.bays.ASFBay;
import megamek.common.bays.AbstractSmallCraftASFBay;
import megamek.common.bays.MekBay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.util.SerializationHelper;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Repairs and door-specific recovery must preserve both authored identity and ordinary aerospace behavior. */
class BuildingBayDoorRepairTest {
    private static final Coords ORIGIN = new Coords(4, 4);
    private static final BuildingDesign.BayDoor NORTH =
          new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 0);
    private static final BuildingDesign.BayDoor SOUTH =
          new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 3);

    private record Fixture(Game game, MobileStructure carrier, ASFBay bay) { }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private Game game() {
        var game = new Game();
        game.setBoard(BoardLoader.initializeBoard("size 10 10\nend\n"));
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.MOVEMENT);
        return game;
    }

    private Fixture fixture(boolean duplicates) {
        var game = game();
        int classification = duplicates ? IBuilding.HANGAR : IBuilding.FORTRESS;
        var carrier = new MobileStructure(BuildingType.HEAVY, classification);
        carrier.configureConstruction(BuildingType.HEAVY, classification, 3, duplicates ? 45 : 90, 0,
              List.of(CubeCoords.ZERO));
        carrier.setId(1);
        carrier.setOwner(game.getPlayer(0));
        carrier.setPosition(ORIGIN);
        var bay = new ASFBay(10, 2, 7);
        carrier.addTransporter(bay);
        carrier.getDesign().getBayDoors().add(NORTH);
        carrier.getDesign().getBayDoors().add(duplicates ? NORTH : SOUTH);
        game.addEntity(carrier);
        return new Fixture(game, carrier, bay);
    }

    private AeroSpaceFighter fighter(Game game, int id) {
        var fighter = new AeroSpaceFighter();
        fighter.setId(id);
        fighter.setOwner(game.getPlayer(0));
        game.addEntity(fighter);
        return fighter;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ordinaryAerospaceCarriersKeepTheirTwoRecoverySlotsAndDoorBehavior(boolean smallCraft) {
        var fixture = fixture(false);
        fixture.bay().setCurrentDoors(0);
        var ordinary = new Dropship();
        ordinary.setId(2);
        ordinary.setOwner(fixture.game().getPlayer(0));
        AbstractSmallCraftASFBay bay = smallCraft ? new SmallCraftBay(10, 1, 7) : new ASFBay(10, 1, 7);
        ordinary.addTransporter(bay);
        fixture.game().addEntity(ordinary);
        bay.recover(fighter(fixture.game(), 10));
        bay.recover(fighter(fixture.game(), 11));
        var waiting = fighter(fixture.game(), 12);
        assertFalse(bay.canLoad(waiting));
        for (int turn = 0; turn < 4; turn++) { bay.updateSlots(); }
        assertFalse(bay.canLoad(waiting));
        bay.updateSlots();
        assertTrue(bay.canLoad(waiting), "the separate building's closed doors cannot affect an ordinary carrier");
        bay.destroyDoor();
        assertEquals(0, bay.getUsableDoors());
        assertFalse(bay.canLoad(waiting));
        bay.restoreAllDoors();
        assertEquals(1, bay.getCurrentDoors());
        bay.initializeRecoverySlots();
        assertTrue(bay.canLoad(waiting));
    }

    @Test void repairingAnAuthoredDoorPreservesTheOtherDoorsRecoveryTimer() {
        var fixture = fixture(false);
        fixture.bay().recover(fighter(fixture.game(), 10), 1);
        assertTrue(BuildingBayDoors.damage(fixture.carrier(), fixture.bay(), NORTH));
        assertEquals(0, fixture.bay().availableRecoverySlots(0));
        assertEquals(1, fixture.bay().availableRecoverySlots(1));
        fixture.bay().restoreDoor();
        assertEquals(2, fixture.bay().getCurrentDoors());
        assertTrue(fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().isEmpty());
        assertEquals(2, fixture.bay().availableRecoverySlots(0));
        assertEquals(1, fixture.bay().availableRecoverySlots(1), "repair must not finish another aircraft's recovery");
    }

    @Test void duplicateDoorsKeepTheSurvivingTimersWhenAnotherOccurrenceIsDamagedAndSaved() {
        var fixture = fixture(true);
        fixture.bay().recover(fighter(fixture.game(), 10), 0);
        fixture.bay().recover(fighter(fixture.game(), 11), 0);
        fixture.bay().updateSlots();
        fixture.bay().updateSlots();
        fixture.bay().recover(fighter(fixture.game(), 12), 1);
        fixture.bay().damageRecoveryDoor(1);
        assertEquals(List.of(1), BuildingBayDoors.usablePlacementIndices(fixture.carrier(), fixture.bay()));
        assertEquals(0, fixture.bay().availableRecoverySlots(1));
        String xml = SerializationHelper.getSaveGameXStream().toXML(fixture.game());
        var restored = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        restored.getEntitiesVector().forEach(entity -> entity.setGame(restored));
        var carrier = (MobileStructure) restored.getEntity(1);
        var bay = (ASFBay) carrier.getTransportBays().getFirst();
        assertEquals(0, bay.availableRecoverySlots(1));
        bay.updateSlots();
        bay.updateSlots();
        assertEquals(0, bay.availableRecoverySlots(1));
        bay.updateSlots();
        assertEquals(2, bay.availableRecoverySlots(1), "the surviving pair had three turns left, not five");
        assertEquals(0, bay.availableRecoverySlots(0));
    }

    @Test void restoringDuplicateDoorsKeepsOnlyTheRemainingDamagedPrefix() {
        var fixture = fixture(true);
        assertTrue(BuildingBayDoors.damage(fixture.carrier(), fixture.bay(), NORTH));
        assertTrue(BuildingBayDoors.damage(fixture.carrier(), fixture.bay(), NORTH));
        fixture.bay().restoreDoor();
        assertEquals(1, fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().get(NORTH));
        assertEquals(List.of(1), BuildingBayDoors.usablePlacementIndices(fixture.carrier(), fixture.bay()));
        fixture.bay().restoreAllDoors();
        assertEquals(List.of(0, 1), BuildingBayDoors.usablePlacementIndices(fixture.carrier(), fixture.bay()));
    }

    @Test void aRepairDoesNotMakeAnOffBoardDoorUsable() {
        var fixture = fixture(false);
        fixture.carrier().setPosition(new Coords(0, 0));
        assertTrue(BuildingBayDoors.damage(fixture.carrier(), fixture.bay(), NORTH));
        fixture.bay().restoreAllDoors();
        assertEquals(2, fixture.bay().getCurrentDoors());
        assertEquals(List.of(1), BuildingBayDoors.usablePlacementIndices(fixture.carrier(), fixture.bay()));
    }

    @Test void damageEditorRepairsOnlyTheNamedBayAndPreservesRecoveryState() {
        var fixture = fixture(false);
        var other = new MekBay(3, 1, 8);
        fixture.carrier().addTransporter(other);
        var otherDoor = new BuildingDesign.BayDoor(8, NORTH.position(), 2);
        fixture.carrier().getDesign().getBayDoors().add(otherDoor);
        fixture.bay().recover(fighter(fixture.game(), 10), 1);
        assertTrue(BuildingBayDoors.damage(fixture.carrier(), fixture.bay(), NORTH));
        assertTrue(BuildingBayDoors.damage(fixture.carrier(), other, otherDoor));
        var spec = new DamageEditSpec();
        spec.bayDoorHits = new Integer[] {null, 0};
        new DamageEditApplier(fixture.carrier(), spec).applyToEntity();
        assertEquals(1, other.getUsableDoors());
        assertEquals(1, fixture.bay().getCurrentDoors());
        assertTrue(fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().containsKey(NORTH));
        assertFalse(fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().containsKey(otherDoor));
        spec.bayDoorHits = new Integer[] {0, null};
        new DamageEditApplier(fixture.carrier(), spec).applyToEntity();
        assertEquals(2, fixture.bay().availableRecoverySlots(0));
        assertEquals(1, fixture.bay().availableRecoverySlots(1));
    }

    @Test void damageEditorCanApplyAndRepairPhysicalDoorsWithoutARunningGame() {
        var fixture = fixture(false);
        fixture.carrier().setGame(null);
        var spec = new DamageEditSpec();
        spec.bayDoorHits = new Integer[] {2};
        new DamageEditApplier(fixture.carrier(), spec).applyToEntity();
        assertEquals(0, fixture.bay().getCurrentDoors());
        assertEquals(2, fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().size());
        spec.bayDoorHits[0] = 1;
        new DamageEditApplier(fixture.carrier(), spec).applyToEntity();
        assertEquals(1, fixture.bay().getCurrentDoors());
        assertEquals(1, fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().size());
        spec.bayDoorHits[0] = 0;
        new DamageEditApplier(fixture.carrier(), spec).applyToEntity();
        assertEquals(2, fixture.bay().getCurrentDoors());
        assertTrue(fixture.carrier().getBuildingRuntimeState().getDamagedBayDoors().isEmpty());
    }

    @Test void ordinaryAerospaceDamageEditorCanPartiallyRepairABayWithNoRemainingDoors() {
        var game = game();
        var ordinary = new Dropship();
        ordinary.setId(1);
        ordinary.setOwner(game.getPlayer(0));
        var bay = new ASFBay(10, 2, 7);
        ordinary.addTransporter(bay);
        game.addEntity(ordinary);
        bay.setCurrentDoors(0);
        var spec = new DamageEditSpec();
        spec.bayDoorHits = new Integer[] {1};
        new DamageEditApplier(ordinary, spec).applyToEntity();
        assertEquals(1, bay.getCurrentDoors());
        assertTrue(bay.canLoad(fighter(game, 10)));
    }
}
