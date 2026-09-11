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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.game.Game;
import megamek.common.enums.GamePhase;
import megamek.common.units.BuildingDesign.Door;

/** Mutable game state for authored doors and environmental integrity; never written into construction BLKs. */
public class BuildingRuntimeState implements Serializable {
    private final Set<Door> openDoors = new HashSet<>();
    private final Map<Door, Integer> doorChangedRound = new HashMap<>();
    private final Map<Door, Integer> doorControllers = new HashMap<>();
    private final Map<Door, Set<Integer>> infantryAtTurnStart = new HashMap<>();
    private final Set<CubeCoords> breachedHexes = new HashSet<>();
    private final Map<CubeCoords, Set<Integer>> floodedLevels = new HashMap<>();
    private final Map<CubeCoords, Integer> floodDirections = new HashMap<>();
    private final Map<CubeCoords, Integer> floodAdvancedRound = new HashMap<>();
    private final Map<CubeCoords, Integer> phaseDamage = new HashMap<>();
    private GamePhase damagePhase;
    private int damageRound = -1;
    private Map<CubeCoords, Integer> collapsedHeights;
    private Integer atmosphereExposureRound;
    private boolean atmosphereExposed;
    private Integer openFloodStartRound;
    private int openFloodDepth;
    private int openFloodRate;
    private Set<Integer> inactiveSurvivalGear;
    private Set<Integer> failedSurvivalGear;
    private Set<CubeCoords> structuralBreaches;
    private MobileStructurePortalRules.Connection portalConnection;
    private Integer interiorComplexId;
    private Map<Integer, BuildingFlightDeckRules.State> flightDecks;
    private Map<BuildingDesign.BayDoor, Integer> damagedBayDoors;

    /** Hangars may have two identical door placements, so damage counts physical doors at that placement. */
    public Map<BuildingDesign.BayDoor, Integer> getDamagedBayDoors() {
        if (damagedBayDoors == null) { damagedBayDoors = new HashMap<>(); }
        return damagedBayDoors;
    }

    public Map<Integer, BuildingFlightDeckRules.State> getFlightDecks() {
        if (flightDecks == null) { flightDecks = new HashMap<>(); }
        return flightDecks;
    }

    public MobileStructurePortalRules.Connection getPortalConnection() {
        return portalConnection;
    }

    public void setPortalConnection(MobileStructurePortalRules.Connection connection) {
        portalConnection = connection;
    }

    public Integer getInteriorComplexId() {
        return interiorComplexId;
    }

    public void setInteriorComplexId(Integer id) {
        interiorComplexId = id;
    }

    public boolean hasStructuralBreach(CubeCoords hex) {
        return structuralBreaches != null && structuralBreaches.contains(hex);
    }

    public void markStructuralBreach(CubeCoords hex) {
        if (structuralBreaches == null) {
            structuralBreaches = new HashSet<>();
        }
        structuralBreaches.add(hex);
    }

    /** Heights survive loss of ceiling/CF so environment processing still knows the affected interior volume. */
    public void rememberCollapsedHex(CubeCoords hex, int height) {
        if (collapsedHeights == null) {
            collapsedHeights = new HashMap<>();
        }
        collapsedHeights.putIfAbsent(hex, height);
    }

    public int enclosureHeight(AbstractBuildingEntity building, CubeCoords hex) {
        return Math.max(building.getInternalBuilding().getHeight(hex),
              collapsedHeights == null ? 0 : collapsedHeights.getOrDefault(hex, 0));
    }

    public boolean encloses(AbstractBuildingEntity building, Entity entity) {
        if (entity.getPosition() == null || entity.getBoardId() != building.getBoardId() || entity.isAirborne()) {
            return false;
        }
        int floor = BuildingElevation.floor(building, entity.getPosition(), entity.getElevation());
        return floor >= 0 && floor < enclosureHeight(building, building.boardToRelative(entity.getPosition()));
    }

    public void loseOpenSpaceAtmosphere(int round) {
        int exposure = round + 1;
        atmosphereExposureRound = atmosphereExposureRound == null ? exposure : Math.min(atmosphereExposureRound, exposure);
    }

    public boolean atmosphereGrace(int round) {
        return atmosphereExposureRound != null && !atmosphereExposed;
    }

    /** Called only by End Phase processing: the air remains safe throughout the following turn. */
    public boolean advanceAtmosphere(int round) {
        if (!atmosphereExposed && atmosphereExposureDue(round)) {
            atmosphereExposed = true;
            return true;
        }
        return false;
    }

    public boolean atmosphereExposureDue(int round) {
        return atmosphereExposureRound != null && round >= atmosphereExposureRound;
    }

    /** Only needed when survival equipment is explicitly carried but not in use; ordinary equipped units are ready. */
    public void setSurvivalGearInactive(int entityId, boolean inactive) {
        if (inactiveSurvivalGear == null) {
            inactiveSurvivalGear = new HashSet<>();
        }
        if (inactive) {
            inactiveSurvivalGear.add(entityId);
        } else {
            inactiveSurvivalGear.remove(entityId);
        }
    }

    public boolean needsSurvivalGearRoll(int entityId) {
        return inactiveSurvivalGear != null && inactiveSurvivalGear.contains(entityId);
    }

    public void resolveSurvivalGear(int entityId, boolean success) {
        setSurvivalGearInactive(entityId, false);
        if (!success) {
            if (failedSurvivalGear == null) {
                failedSurvivalGear = new HashSet<>();
            }
            failedSurvivalGear.add(entityId);
        }
    }

    public boolean survivalGearFailed(int entityId) {
        return failedSurvivalGear != null && failedSurvivalGear.contains(entityId);
    }

    public void startOpenSpaceFlood(int round, int waterAboveRoof) {
        if (openFloodStartRound == null) {
            openFloodStartRound = round;
        }
        openFloodRate = Math.max(openFloodRate, Math.max(1, (waterAboveRoof + 4) / 5));
    }

    public int getOpenFloodDepth() {
        return openFloodDepth;
    }

    public boolean advanceOpenSpaceFlood(AbstractBuildingEntity building, int round) {
        if (openFloodStartRound == null || round < openFloodStartRound) {
            return false;
        }
        int next = Math.min(building.getInternalBuilding().getBuildingHeight(),
              Math.max(0, round - openFloodStartRound) * openFloodRate);
        boolean changed = next != openFloodDepth;
        openFloodDepth = next;
        for (CubeCoords hex : building.getInternalBuilding().getOriginalCoordsList()) {
            for (int level = 0; level < Math.min(next, enclosureHeight(building, hex)); level++) {
                floodedLevels.computeIfAbsent(hex, ignored -> new HashSet<>()).add(level);
            }
        }
        return changed;
    }

    public boolean isDoorOpen(Door door) {
        return openDoors.contains(door);
    }

    public boolean isBreached(CubeCoords hex) {
        return breachedHexes.contains(hex);
    }

    public boolean breach(CubeCoords hex) {
        return breachedHexes.add(hex);
    }

    /** Subsurface structures also test a cumulative ten CF lost during one phase (TO:AR p.138). */
    public boolean crossesSubsurfaceDamageThreshold(CubeCoords hex, int damage, int round, GamePhase phase) {
        if (damagePhase != phase || damageRound != round) {
            phaseDamage.clear();
            damageRound = round;
            damagePhase = phase;
        }
        int previous = phaseDamage.getOrDefault(hex, 0);
        phaseDamage.put(hex, previous + damage);
        return previous < 10 && previous + damage >= 10;
    }

    public boolean isFlooded(CubeCoords hex, int level) {
        return floodedLevels.getOrDefault(hex, Set.of()).contains(level);
    }

    public boolean flood(CubeCoords hex, int level, int direction, int round) {
        boolean added = floodedLevels.computeIfAbsent(hex, key -> new HashSet<>()).add(level);
        floodDirections.putIfAbsent(hex, direction);
        floodAdvancedRound.putIfAbsent(hex, round);
        return added;
    }

    /** Flooding advances once on each subsequent turn, toward the bottom or up from a connected tunnel. */
    public boolean advanceFloods(AbstractBuildingEntity building, int round) {
        boolean changed = false;
        for (var entry : floodedLevels.entrySet()) {
            CubeCoords hex = entry.getKey();
            if (floodAdvancedRound.getOrDefault(hex, round) >= round || entry.getValue().isEmpty()) {
                continue;
            }
            int direction = floodDirections.getOrDefault(hex, -1);
            int boundary = direction < 0 ? entry.getValue().stream().mapToInt(Integer::intValue).min().orElse(0)
                  : entry.getValue().stream().mapToInt(Integer::intValue).max().orElse(0);
            int next = boundary + direction;
            if (next >= 0 && next < building.getInternalBuilding().getHeight(hex)) {
                changed |= entry.getValue().add(next);
            }
            floodAdvancedRound.put(hex, round);
        }
        return changed;
    }

    public void newRound(AbstractBuildingEntity building) {
        infantryAtTurnStart.clear();
        if (building.getGame() == null || building.getPosition() == null) {
            return;
        }
        for (Door door : building.getDesign().getDoors()) {
            Set<Integer> infantry = new HashSet<>();
            for (Entity entity : building.getGame().getEntitiesVector(building.relativeToBoard(door.position().hex()),
                  building.getBoardId())) {
                if (entity instanceof Infantry && inside(building, entity)) {
                    infantry.add(entity.getId());
                }
            }
            infantryAtTurnStart.put(door, infantry);
        }
    }

    public boolean canChangeDoor(AbstractBuildingEntity building, Door door, Player player) {
        Game game = building.getGame();
        if (game == null || player == null || !game.getPhase().isEnd()
              || !building.getDesign().getDoors().contains(door)
              || !building.isIn(building.relativeToBoard(door.position().hex()))
              || BuildingElevation.doorwayHeight(building, door) == 0
              || java.util.Objects.equals(doorChangedRound.get(door), game.getRoundCount())) {
            return false;
        }
        Player controller = game.getPlayer(doorControllers.getOrDefault(door, building.getOwnerId()));
        if (controller != null && !controller.isEnemyOf(player)) {
            return true;
        }
        var occupants = game.getEntitiesVector(building.relativeToBoard(door.position().hex()), building.getBoardId())
              .stream().filter(entity -> !(entity instanceof IBuilding) && !entity.isDestroyed() && inside(building, entity))
              .toList();
        // TO:AR p.136: clear the door's hex and keep infantry there for the whole turn before seizing its controls.
        return occupants.stream().noneMatch(entity -> entity.getOwner().isEnemyOf(player))
              && occupants.stream().anyMatch(entity -> entity instanceof Infantry && entity.delta_distance == 0
                    && !entity.getOwner().isEnemyOf(player)
                    && infantryAtTurnStart.getOrDefault(door, Set.of()).contains(entity.getId()));
    }

    public boolean changeDoor(AbstractBuildingEntity building, Door door, Player player, boolean open) {
        if (isDoorOpen(door) == open || !canChangeDoor(building, door, player)) {
            return false;
        }
        if (open) {
            openDoors.add(door);
        } else {
            openDoors.remove(door);
        }
        doorChangedRound.put(door, building.getGame().getRoundCount());
        doorControllers.put(door, player.getId());
        return true;
    }

    /** Does this boundary have an open doorway tall enough for the moving unit? */
    public boolean openPassage(AbstractBuildingEntity building, Entity entity, Coords from, Coords to, int elevation) {
        if (from == null || to == null || from.distance(to) != 1) {
            return false;
        }
        int requiredHeight = entity instanceof Mek ? Math.max(2, entity.height() + 1) : 1;
        for (Door door : openDoors) {
            int doorwayHeight = BuildingElevation.doorwayHeight(building, door);
            if (doorwayHeight == 0) { continue; }
            Coords inner = building.relativeToBoard(door.position().hex());
            Coords outer = inner.translated((door.facing() + building.getFacing()) % 6);
            if ((inner.equals(from) && outer.equals(to)) || (inner.equals(to) && outer.equals(from))) {
                int bottom = BuildingElevation.base(building, inner)
                      + BuildingElevation.currentFloor(building, door.position().hex(), door.position().level());
                if (elevation >= bottom && elevation + requiredHeight <= bottom + doorwayHeight) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean inside(AbstractBuildingEntity building, Entity entity) {
        if (entity.getPosition() == null || entity.isAirborne() || entity.isAirborneVTOLorWIGE()
              || entity.getBoardId() != building.getBoardId() || !building.isIn(entity.getPosition())) {
            return false;
        }
        return BuildingElevation.contains(building, entity.getPosition(), entity.getElevation());
    }

    public static boolean protectsFromEnvironment(Game game, Entity entity) {
        if (game == null || entity.getPosition() == null || !game.hasBoardLocation(entity.getPosition(), entity.getBoardId())) {
            return false;
        }
        return game.getBoard(entity.getBoardId()).getBuildingsVector().stream()
              .anyMatch(shelter -> shelter instanceof AbstractBuildingEntity building && building.hasEnvironmentalSealing()
                    && building.getBuildingRuntimeState().encloses(building, entity)
                    && (!building.getBuildingRuntimeState().isBreached(building.boardToRelative(entity.getPosition()))
                          || building.getBuildingRuntimeState().atmosphereGrace(game.getRoundCount())));
    }
}
