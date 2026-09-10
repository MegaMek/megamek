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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import megamek.common.bays.Bay;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.Mounted;

/** Authored static-building construction choices (TO:AR pp. 129–138). Calculated values are not stored. */
public class BuildingDesign implements Serializable {
    public enum Ceiling { STANDARD, HIGH, LOW }

    public enum Site { SURFACE, UNDERGROUND, UNDERWATER }

    /** Native floor number; the roof is numbered height, immediately above the highest internal floor. */
    public record Position(CubeCoords hex, int level) implements Serializable { }

    /** An exterior door begins at the specified floor and extends upward for height levels. */
    public record Door(Position position, int facing, int height) implements Serializable { }

    /** Each served level records a bit mask of the internal hexsides through which the lift can be entered. */
    public record Elevator(CubeCoords hex, double capacity, Map<Integer, Integer> exits) implements Serializable {
        public Elevator {
            exits = Map.copyOf(exits);
        }

        public double weight() {
            return Math.ceil(capacity / 20) * Math.max(0, upperLevel() - Math.max(1, lowerLevel()) + 1);
        }

        public int lowerLevel() {
            return exits.keySet().stream().mapToInt(Integer::intValue).min().orElse(0);
        }

        public int upperLevel() {
            return exits.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        }

        public boolean reaches(int level) {
            return !exits.isEmpty() && level >= lowerLevel() && level <= upperLevel();
        }
    }

    /** Bay/quarters space can be divided among hexes and levels, independently of its occupant count. */
    public record Space(Position position, double tons) implements Serializable { }

    private boolean environmentalSealing;
    private boolean heavyMetal;
    private boolean civilianOfficers;
    private boolean tunnel;
    private boolean openSpace;
    private boolean roofClearance;
    private Ceiling ceiling = Ceiling.STANDARD;
    private Site site = Site.SURFACE;
    private int depth = 1;
    private Integer baseLevel;
    private final List<Door> doors = new ArrayList<>();
    private final List<Elevator> elevators = new ArrayList<>();
    private final Set<Mounted<?>> automatedWeapons = new HashSet<>();
    private final Map<Mounted<?>, List<Position>> equipmentSpace = new HashMap<>();
    private final Map<Bay, List<Space>> baySpace = new HashMap<>();
    private final Map<Mounted<?>, Double> pcmtSources = new HashMap<>();
    private final Map<CubeCoords, Integer> wallSides = new HashMap<>();
    private final Map<CubeCoords, Integer> bridgeDecks = new HashMap<>();

    /** Clockwise hexsides, bit 0 = north. Only walls/fences use these segments. */
    public Map<CubeCoords, Integer> getWallSides() {
        return wallSides;
    }

    /** Deck elevation above the design datum; bridges have no occupied interior floors. */
    public Map<CubeCoords, Integer> getBridgeDecks() {
        return bridgeDecks;
    }

    public int wallSides(CubeCoords hex) {
        return wallSides.getOrDefault(hex, 1);
    }

    public int bridgeDeck(CubeCoords hex) {
        return bridgeDecks.getOrDefault(hex, 0);
    }

    public boolean hasEnvironmentalSealing() {
        return environmentalSealing;
    }

    public void setEnvironmentalSealing(boolean value) {
        environmentalSealing = value;
    }

    public boolean hasHeavyMetal() {
        return heavyMetal;
    }

    public void setHeavyMetal(boolean value) {
        heavyMetal = value;
    }

    public boolean hasCivilianOfficers() {
        return civilianOfficers;
    }

    public boolean isTunnel() {
        return tunnel;
    }

    public void setTunnel(boolean value) {
        tunnel = value;
    }

    public boolean isOpenSpace() {
        return openSpace;
    }

    public void setOpenSpace(boolean value) {
        openSpace = value;
    }

    public boolean hasRoofClearance() {
        return roofClearance;
    }

    /** Underground roofs can carry equipment when they have at least one level of clearance in a larger cave. */
    public void setRoofClearance(boolean value) {
        roofClearance = value;
    }

    public void setCivilianOfficers(boolean value) {
        civilianOfficers = value;
    }

    public Ceiling getCeiling() {
        return ceiling;
    }

    public void setCeiling(Ceiling value) {
        ceiling = value;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site value) {
        site = value;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int value) {
        depth = value;
    }

    /** Display level of the lowest floor; null derives the reference from site and roof cover. */
    public Integer getBaseLevel() {
        return baseLevel;
    }

    public void setBaseLevel(Integer value) {
        baseLevel = value;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public Set<Mounted<?>> getAutomatedWeapons() {
        return automatedWeapons;
    }

    public Map<Mounted<?>, List<Position>> getEquipmentSpace() {
        return equipmentSpace;
    }

    public Map<Bay, List<Space>> getBaySpace() {
        return baySpace;
    }

    /** External transmitter tonnage cannot be derived from a receiver's own tonnage. */
    public Map<Mounted<?>, Double> getPcmtSources() {
        return pcmtSources;
    }

    public void replaceBay(Bay oldBay, Bay newBay) {
        var spaces = baySpace.remove(oldBay);
        if (spaces != null && oldBay.getWeight() > 0) {
            double scale = newBay.getWeight() / oldBay.getWeight();
            baySpace.put(newBay, spaces.stream().map(space -> new Space(space.position(), space.tons() * scale)).toList());
        }
    }

    /** Keep authored placements attached to physical hexes when the footprint is edited or transformed. */
    public void remap(UnaryOperator<Position> transform, java.util.function.IntUnaryOperator facing) {
        Map<CubeCoords, Integer> sides = new HashMap<>();
        wallSides.forEach((hex, mask) -> {
            Position target = transform.apply(new Position(hex, 0));
            if (target != null) {
                int transformed = 0;
                for (int side = 0; side < 6; side++) {
                    if ((mask & (1 << side)) != 0) {
                        transformed |= 1 << facing.applyAsInt(side);
                    }
                }
                sides.put(target.hex(), transformed);
            }
        });
        wallSides.clear();
        wallSides.putAll(sides);
        Map<CubeCoords, Integer> decks = new HashMap<>();
        bridgeDecks.forEach((hex, level) -> {
            Position target = transform.apply(new Position(hex, 0));
            if (target != null) {
                decks.put(target.hex(), level);
            }
        });
        bridgeDecks.clear();
        bridgeDecks.putAll(decks);
        doors.replaceAll(door -> new Door(transform.apply(door.position()), facing.applyAsInt(door.facing()), door.height()));
        doors.removeIf(door -> door.position() == null);
        elevators.replaceAll(lift -> {
            Map<Integer, Integer> exits = new HashMap<>();
            CubeCoords hex = null;
            for (var entry : lift.exits().entrySet()) {
                Position position = transform.apply(new Position(lift.hex(), entry.getKey()));
                if (position != null) {
                    hex = position.hex();
                    int mask = 0;
                    for (int side = 0; side < 6; side++) {
                        if ((entry.getValue() & (1 << side)) != 0) {
                            mask |= 1 << facing.applyAsInt(side);
                        }
                    }
                    exits.put(position.level(), mask);
                }
            }
            return new Elevator(hex, lift.capacity(), exits);
        });
        elevators.removeIf(lift -> lift.hex() == null || lift.exits().size() < 2);
        equipmentSpace.replaceAll((mount, positions) -> positions.stream().map(transform)
              .filter(java.util.Objects::nonNull).toList());
        equipmentSpace.values().removeIf(List::isEmpty);
        baySpace.replaceAll((bay, spaces) -> spaces.stream()
              .map(space -> new Space(transform.apply(space.position()), space.tons()))
              .filter(space -> space.position() != null).toList());
        baySpace.values().removeIf(List::isEmpty);
    }

    public void removeDeletedComponents(BuildingEntity entity) {
        automatedWeapons.retainAll(entity.getEquipment());
        equipmentSpace.keySet().retainAll(entity.getEquipment());
        baySpace.keySet().retainAll(entity.getTransportBays());
        pcmtSources.keySet().retainAll(entity.getEquipment());
    }

    public void removeEquipment(Mounted<?> mount) {
        automatedWeapons.remove(mount);
        equipmentSpace.remove(mount);
        pcmtSources.remove(mount);
    }
}
