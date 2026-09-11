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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;

/** Placed flight decks/helipads and their exposed aircraft, TO:AUE pp.37,124–125. */
public final class BuildingFlightDeckRules {
    private BuildingFlightDeckRules() { }

    public record Occupant(int entityId, CubeCoords hex, int landedRound) implements Serializable { }

    public static final class State implements Serializable {
        private Integer lastOperationRound;
        private final Map<Integer, Occupant> occupants = new HashMap<>();
        public Integer lastOperationRound() { return lastOperationRound; }
        public Map<Integer, Occupant> occupants() { return occupants; }
        public void operated(int round) { lastOperationRound = round; }
    }

    public record Deck(AbstractBuildingEntity carrier, Mounted<?> mount, List<Coords> hexes, boolean helipad) {
        public int id() { return carrier.getEquipmentNum(mount); }
        public State state() { return carrier.getBuildingRuntimeState().getFlightDecks().computeIfAbsent(id(), k -> new State()); }
        public int elevation(Coords coords) { return BuildingElevation.roof(carrier, coords); }
        public boolean landingDeck() {
            return ((BuildingEquipmentType) mount.getType()).getFacility() == BuildingEquipmentType.Facility.LANDING_DECK;
        }
        public double capacity() {
            if (!landingDeck()) { return 200; }
            return 1000.0 * hexes.size();
        }
        public boolean available() {
            if (landingDeck()) {
                int original = carrier.getOriginalHexCount();
                return (original - carrier.getCoordsList().size()) * 3 < original;
            }
            int round = carrier.getGame().getRoundCount();
            return state().lastOperationRound() == null || round - state().lastOperationRound() >= 6;
        }
        public List<Occupant> occupants() {
            return hexes.stream().flatMap(coords -> carrier.getGame().getEntitiesVector(coords, carrier.getBoardId()).stream())
                  .filter(unit -> (unit instanceof IAero || unit instanceof VTOL) && !unit.isDestroyed()
                        && unit.getTransportId() == Entity.NONE && unit.getAltitude() == 0
                        && unit.getElevation() == elevation(unit.getPosition()))
                  .map(unit -> state().occupants().getOrDefault(unit.getId(), new Occupant(unit.getId(),
                        carrier.boardToRelative(unit.getPosition()), carrier.getGame().getRoundCount() - 6)))
                  .distinct().toList();
        }
    }

    public static List<Deck> decks(AbstractBuildingEntity building) {
        if (building.getGame() == null || building.getPosition() == null || building.isDestroyed() || building.isDoomed()) {
            return List.of();
        }
        var result = new ArrayList<Deck>();
        for (var mount : building.getEquipment()) {
            if (!(mount.getType() instanceof BuildingEquipmentType equipment) || mount.isInoperable()
                  || equipment.getFacility() != BuildingEquipmentType.Facility.FLIGHT_DECK
                        && equipment.getFacility() != BuildingEquipmentType.Facility.HELIPAD
                        && equipment.getFacility() != BuildingEquipmentType.Facility.LANDING_DECK) {
                continue;
            }
            boolean helipad = equipment.getFacility() == BuildingEquipmentType.Facility.HELIPAD;
            boolean landingDeck = equipment.getFacility() == BuildingEquipmentType.Facility.LANDING_DECK;
            var positions = BuildingConstruction.equipmentPositions(building, mount);
            var hexes = positions.stream().map(p -> building.relativeToBoard(p.hex())).distinct().toList();
            if ((landingDeck ? !Set.of(7, 19, 37).contains(hexes.size()) : hexes.size() != (helipad ? 1 : 3))
                  || positions.stream().filter(p -> !landingDeck || building.getInternalBuilding().getHeight(p.hex()) > 0).anyMatch(p ->
                  BuildingElevation.currentFloor(building, p.hex(), p.level()) != building.getInternalBuilding().getHeight(p.hex()) - 1)
                  || hexes.stream().anyMatch(c -> !building.getGame().getBoard(building).contains(c)
                        || !landingDeck && (!building.isIn(c) || building.getCurrentCF(c) <= 0)
                        || building.isIn(c) && BuildingElevation.roof(building, c) < 0)) {
                continue;
            }
            result.add(new Deck(building, mount, hexes, helipad));
        }
        return result;
    }

    public static List<Deck> decksAt(Game game, int boardId, Coords coords) {
        if (game == null || coords == null || !game.hasBoardLocation(coords, boardId)) { return List.of(); }
        return game.getBoard(boardId).getBuildingsAt(coords).stream()
              .filter(AbstractBuildingEntity.class::isInstance).map(AbstractBuildingEntity.class::cast)
              .flatMap(b -> decks(b).stream()).filter(d -> d.hexes().contains(coords)).toList();
    }

    public static Deck onDeck(Entity unit) {
        if (unit == null || unit.getGame() == null || unit.getPosition() == null || unit.getAltitude() != 0) { return null; }
        return decksAt(unit.getGame(), unit.getBoardId(), unit.getPosition()).stream()
              .filter(d -> d.occupants().stream().anyMatch(o -> o.entityId() == unit.getId())).findFirst().orElse(null);
    }

    public static boolean canEnterBay(Entity unit) {
        Deck deck = onDeck(unit);
        if (deck == null || deck.landingDeck()) { return true; }
        var arrived = deck.state().occupants().get(unit.getId());
        return arrived == null || unit.getGame().getRoundCount() - arrived.landedRound() >= 5;
    }

    private static boolean compatible(Deck deck, Entity unit, boolean vertical) {
        if (unit.getWeight() > deck.capacity() || unit.isDestroyed() || unit.isDoomed() || !unit.getCrew().isActive()) { return false; }
        if (unit.getMovementMode() == EntityMovementMode.VTOL && !(unit instanceof MobileStructure)) { return vertical; }
        return unit instanceof IAero aero && (vertical ? aero.canLandVertically() : !deck.helipad() && aero.canLandHorizontally());
    }

    private static boolean room(Deck deck, Entity unit) {
        var occupants = deck.occupants().stream().filter(o -> o.entityId() != unit.getId()).toList();
        if (deck.landingDeck()) {
            return unit.getWeight() + occupants.stream().mapToDouble(o -> deck.carrier().getGame().getEntity(o.entityId()).getWeight()).sum()
                  <= deck.capacity();
        }
        if (deck.helipad()) { return occupants.isEmpty(); }
        boolean vtol = unit instanceof VTOL;
        return vtol ? occupants.size() < 4 && occupants.stream().allMatch(o -> deck.carrier().getGame().getEntity(o.entityId()) instanceof VTOL)
              : occupants.isEmpty();
    }

    public static Deck landingDeck(Entity unit, int boardId, Coords coords, int facing, boolean vertical) {
        return decksAt(unit.getGame(), boardId, coords).stream()
              .filter(d -> d.available() && compatible(d, unit, vertical) && room(d, unit))
              .filter(d -> clearOperation(d, unit, coords, facing, vertical, false))
              .findFirst().orElse(null);
    }

    /** Landing Decks are paved surfaces and use ordinary runway/DropShip footprint requirements (TO:AUE p.131). */
    public static Set<Coords> operationHexes(Deck deck, Entity unit, Coords start, int facing, boolean vertical, boolean takeoff) {
        Set<Coords> result = new HashSet<>();
        int length = vertical ? 1 : deck.landingDeck() && unit instanceof IAero aero
              ? (takeoff ? aero.getTakeOffLength() : aero.getLandingLength()) : 3;
        for (int distance = 0; distance < length; distance++) {
            Coords coords = start.translated(facing, distance);
            result.add(coords);
            if (unit instanceof Dropship) {
                if (vertical) { result.addAll(coords.allAdjacent()); }
                else {
                    result.add(coords.translated((facing + 2) % 6));
                    result.add(coords.translated((facing + 4) % 6));
                }
            }
        }
        return result;
    }

    private static boolean clearOperation(Deck deck, Entity unit, Coords start, int facing, boolean vertical, boolean takeoff) {
        var required = operationHexes(deck, unit, start, facing, vertical, takeoff);
        int altitude = BuildingElevation.altitude(unit.getGame(), deck.carrier(), start, deck.carrier().getHeight(start));
        if (!deck.hexes().containsAll(required) || required.stream().anyMatch(coords -> !deck.carrier().isIn(coords)
              || BuildingElevation.altitude(unit.getGame(), deck.carrier(), coords, deck.carrier().getHeight(coords)) != altitude)) {
            return false;
        }
        return deck.occupants().stream().filter(o -> o.entityId() != unit.getId())
              .map(o -> unit.getGame().getEntity(o.entityId()))
              .noneMatch(other -> !(unit instanceof VTOL && other instanceof VTOL && !deck.helipad() && !deck.landingDeck())
                    && other.getOccupiedCoords().stream().anyMatch(required::contains));
    }

    public static boolean canTakeOff(Entity unit, boolean vertical) {
        Deck deck = onDeck(unit);
        if (deck == null || !deck.available() || unit.getWeight() > deck.capacity() || unit.isDestroyed() || unit.isDoomed()
              || unit.isShutDown() || unit.isManualShutdown() || !unit.getCrew().isActive()) { return false; }
        if (unit instanceof IAero aero) {
            return (vertical ? aero.canTakeOffVertically() : !deck.helipad() && aero.canTakeOffHorizontally())
                  && clearOperation(deck, unit, unit.getPosition(), unit.getFacing(), vertical, true);
        }
        return vertical && unit instanceof VTOL;
    }

    public static boolean canLeaveDeckForBay(AbstractBuildingEntity carrier, Entity unit, Coords position, int absoluteElevation) {
        Deck deck = onDeck(unit);
        return deck != null && deck.carrier() == carrier && !unit.isEnemyOf(carrier) && canEnterBay(unit)
              && !unit.isDestroyed() && !unit.isDoomed() && unit.getCrew().isActive()
              && unit.getPosition().equals(position)
              && unit.getGame().getBoard(unit).getHex(position).getLevel() + deck.elevation(position) == absoluteElevation;
    }

    public static boolean canStow(AbstractBuildingEntity carrier, Entity unit, Coords position, int absoluteElevation) {
        return canLeaveDeckForBay(carrier, unit, position, absoluteElevation)
              && carrier.getTransportBays().stream().anyMatch(bay -> bay.canLoad(unit)
                    && (unit.getTargetBay() < 0 || unit.getTargetBay() == bay.getBayNumber()));
    }

    /** Bay cargo reaches a working deck via its internal handling equipment, ready for its next launch operation. */
    public static List<Deck> stagingDecks(AbstractBuildingEntity carrier, Entity unit) {
        if (!(unit instanceof IAero) && !(unit instanceof VTOL)
              || unit.isShutDown() || unit.isManualShutdown() || !unit.getCrew().isActive()) { return List.of(); }
        return decks(carrier).stream().filter(d -> d.available() && room(d, unit)
              && (compatible(d, unit, true) || compatible(d, unit, false))).toList();
    }

    public static void staged(Deck deck, Entity unit, Coords coords) {
        // The five exposed turns apply after arrival from flight; moving stored cargo onto a deck is not a landing.
        deck.state().occupants().put(unit.getId(), new Occupant(unit.getId(), deck.carrier().boardToRelative(coords),
              deck.carrier().getGame().getRoundCount() - 5));
    }
    public static void landed(Deck deck, Entity unit, Coords coords) {
        int round = deck.carrier().getGame().getRoundCount();
        deck.state().operated(round);
        deck.state().occupants().put(unit.getId(), new Occupant(unit.getId(), deck.carrier().boardToRelative(coords), round));
    }

    public static void departed(Deck deck, Entity unit) {
        deck.state().operated(deck.carrier().getGame().getRoundCount());
        deck.state().occupants().remove(unit.getId());
    }
}
