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

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.game.Game;

/** Physical building elevations. All returned elevations use the local hex surface as zero, like Entity elevation. */
public final class BuildingElevation {
    private BuildingElevation() { }

    public static int base(IBuilding building) {
        return base(building, building instanceof Entity entity ? entity.getPosition() : null);
    }

    public static int base(IBuilding building, Coords coords) {
        if (building instanceof MobileStructure mobile) {
            return mobile.getBaseElevation(coords);
        }
        if (building instanceof BuildingTerrain bridge && bridge.getBldgClass() == IBuilding.BRIDGE) {
            return bridge.getBridgeDeck(coords);
        }
        if (building instanceof AbstractBuildingEntity bridge && bridge.getBldgClass() == IBuilding.BRIDGE) {
            CubeCoords relative = coords == null || bridge.getPosition() == null ? CubeCoords.ZERO : bridge.boardToRelative(coords);
            return bridge.getElevation() + bridge.getDesign().bridgeDeck(relative);
        }
        return building instanceof AbstractBuildingEntity authored
              ? BuildingConstruction.baseLevel(authored) + authored.getElevation() : 0;
    }

    public static int roof(IBuilding building, Coords coords) {
        // A bridge is a deck plane, not a solid building extending up from ground level (TO:AR p.115).
        return base(building, coords) + (building.getBldgClass() == IBuilding.BRIDGE ? 0 : building.getHeight(coords));
    }

    public static int floor(IBuilding building, int elevation) {
        return elevation - base(building);
    }

    public static int floor(IBuilding building, Coords coords, int elevation) {
        return elevation - base(building, coords);
    }

    /** Resolve a saved original floor identity after optional floor collapse; destroyed floors have no position. */
    public static int currentFloor(IBuilding building, CubeCoords relativeHex, int originalFloor) {
        BuildingFloorState floors = building.getInternalBuilding().getFloorState(relativeHex);
        if (floors != null) {
            return floors.getCF(originalFloor) > 0 ? floors.getLevel(originalFloor) : -1;
        }
        return originalFloor >= 0 && originalFloor < building.getInternalBuilding().getHeight(relativeHex)
              && building.getInternalBuilding().getCurrentCF(relativeHex) > 0 ? originalFloor : -1;
    }

    /** Surviving contiguous height of a doorway whose original floor identities remain unchanged in the design. */
    public static int doorwayHeight(IBuilding building, BuildingDesign.Door door) {
        int bottom = currentFloor(building, door.position().hex(), door.position().level());
        if (bottom < 0) { return 0; }
        int height = 1;
        for (int floor = door.position().level() + 1; floor < door.position().level() + door.height(); floor++) {
            int current = currentFloor(building, door.position().hex(), floor);
            if (current == bottom + height) { height++; }
        }
        return height;
    }

    public static boolean contains(IBuilding building, Coords coords, int elevation) {
        if (building == null || coords == null || !building.isIn(coords)
              || building.getBldgClass() == IBuilding.BRIDGE
              || building.getBldgClass() == IBuilding.WALL || building.getBldgClass() == IBuilding.FENCE) {
            return false;
        }
        int bottom = base(building, coords);
        if (!(building instanceof AbstractBuildingEntity) && building.getBasement(coords) != null) {
            bottom -= building.getBasement(coords).getDepth();
        }
        return elevation >= bottom && elevation < roof(building, coords);
    }

    /** Elevation-aware lookup also works where separate subsurface/surface structures share a hex. */
    public static IBuilding at(Game game, Coords coords, int boardId, int elevation) {
        return game == null || !game.hasBoardLocation(coords, boardId) ? null
              : game.getBoard(boardId).getBuildingAt(coords, elevation);
    }

    public static int altitude(Game game, IBuilding building, Coords coords, int floor) {
        Hex hex = game == null ? null : game.getHex(coords, building.getBoardId());
        return (hex == null ? 0 : hex.getLevel()) + base(building, coords) + floor;
    }

    /** Earth over an underground roof includes any water-bed depth above it. */
    public static int groundCover(Game game, IBuilding building, Coords coords) {
        Hex hex = game == null ? null : game.getHex(coords, building.getBoardId());
        return Math.max(0, (hex == null ? 0 : -hex.depth()) - roof(building, coords));
    }

    public static boolean undergroundAt(IBuilding building, Coords coords, int elevation) {
        return building instanceof AbstractBuildingEntity authored
              && (authored.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND
                    || authored.getDesign().getSite() == BuildingDesign.Site.SURFACE && elevation < 0)
              && contains(building, coords, elevation);
    }

    /** TO:AR p.138: earth protects buried structures except at a tunnel mouth or inside a larger cave. */
    public static boolean canAttack(Game game, IBuilding target, Entity attacker, Coords coords, int elevation) {
        if (!(target instanceof AbstractBuildingEntity building)) {
            return true;
        }
        Hex hex = game == null ? null : game.getHex(coords, building.getBoardId());
        boolean buried = building.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND
              || building.getDesign().getSite() == BuildingDesign.Site.SURFACE && hex != null && elevation < -hex.depth();
        if (!buried || attacker != null && BuildingRuntimeState.inside(building, attacker)) {
            return true;
        }
        if (attacker == null || attacker.getPosition() == null || attacker.getBoardId() != building.getBoardId()) {
            return false;
        }
        if (building.getDesign().hasRoofClearance()) {
            IBuilding enclosure = BuildingInteriorRules.shared(game, attacker.getPosition(), attacker.getElevation(),
                  coords, elevation, building.getBoardId());
            if (enclosure instanceof AbstractBuildingEntity cave && cave.getDesign().isOpenSpace()) {
                return true;
            }
        }
        return building.getDesign().isTunnel() && building.getDesign().getDoors().stream().anyMatch(door ->
              building.relativeToBoard(door.position().hex()).equals(coords)
                    && coords.direction(attacker.getPosition()) == Math.floorMod(door.facing() + building.getFacing(), 6)
                    && doorwayHeight(building, door) > 0
                    && elevation >= base(building, coords) + currentFloor(building, door.position().hex(), door.position().level())
                    && elevation < base(building, coords) + currentFloor(building, door.position().hex(), door.position().level())
                          + doorwayHeight(building, door));
    }
    /** An enclosure may contain buildings; separate surface/subsurface parts may also share their footprint. */
    public static boolean canCoexist(AbstractBuildingEntity first, IBuilding second) {
        if (first.getBldgClass() == IBuilding.BRIDGE || second.getBldgClass() == IBuilding.BRIDGE) {
            return bridgeCanCoexist(first, first.getCoordsList(), BuildingConstruction.baseLevel(first) + first.getElevation(), second);
        }
        return canCoexist(first, first.getCoordsList(), base(first), second);
    }

    public static boolean canCoexist(AbstractBuildingEntity first, java.util.List<Coords> footprint, int firstBase,
          IBuilding second) {
        if (first.getBldgClass() == IBuilding.BRIDGE || second.getBldgClass() == IBuilding.BRIDGE) {
            return bridgeCanCoexist(first, footprint, firstBase, second);
        }
        if (!(second instanceof AbstractBuildingEntity other)) {
            return false;
        }
        if (BuildingConstruction.usesHexsides(first) || BuildingConstruction.usesHexsides(other)) {
            return true;
        }
        if (first instanceof MobileStructure mobile && MobileStructurePortalRules.canEnter(mobile, other, footprint, first.getFacing())) {
            return true;
        }
        int firstRoof = firstBase + first.getInternalBuilding().getBuildingHeight();
        int otherRoof = base(other) + other.getInternalBuilding().getBuildingHeight();
        if (first.getDesign().isOpenSpace() && footprint.containsAll(other.getCoordsList()) && base(other) >= firstBase
                    && otherRoof <= firstRoof
              || other.getDesign().isOpenSpace() && other.getCoordsList().containsAll(footprint) && firstBase >= base(other)
                    && firstRoof <= otherRoof) {
            return true;
        }
        boolean firstBelow = firstRoof <= base(other);
        if (!firstBelow && otherRoof > firstBase) {
            return false;
        }
        AbstractBuildingEntity lower = firstBelow ? first : other;
        AbstractBuildingEntity upper = firstBelow ? other : first;
        java.util.List<Coords> lowerHexes = firstBelow ? footprint : other.getCoordsList();
        java.util.List<Coords> upperHexes = firstBelow ? other.getCoordsList() : footprint;
        if (!lowerHexes.containsAll(upperHexes)) {
            return false;
        }
        boolean connected = firstBelow ? firstRoof == base(other) : otherRoof == firstBase;
        if (!connected) {
            return true;
        }
        int lowerHeight = lower.getInternalBuilding().getBuildingHeight();
        int upperHeight = upper.getInternalBuilding().getBuildingHeight();
        int lowerType = lower.getBuildingType().getTypeValue();
        int upperType = upper.getBuildingType().getTypeValue();
        if (lower.getDesign().getSite() == BuildingDesign.Site.UNDERWATER) {
            return lowerType >= upperType && lower.getOInternal(0) > upper.getOInternal(0)
                  && upper.hasEnvironmentalSealing() && upperHeight <= lowerHeight / 2;
        }
        return lowerType > upperType || lowerHeight <= upperHeight / 2;
    }

    /** Passing below a span does not require the passing unit to contain the bridge's two banks. */
    private static boolean bridgeCanCoexist(AbstractBuildingEntity first, java.util.List<Coords> footprint,
          int firstBase, IBuilding second) {
        if (BuildingConstruction.usesHexsides(first)
              || second.getBldgClass() == IBuilding.WALL || second.getBldgClass() == IBuilding.FENCE) {
            return true;
        }
        boolean currentPose = footprint.equals(first.getCoordsList())
              && firstBase == BuildingConstruction.baseLevel(first) + first.getElevation();
        var relativeHexes = first.getInternalBuilding().getCoordsList().stream()
              .filter(hex -> !currentPose || first.getInternalBuilding().hasCFIn(hex)).toList();
        for (int index = 0; index < footprint.size(); index++) {
            Coords coords = footprint.get(index);
            if (!second.isIn(coords)) { continue; }
            CubeCoords relative = relativeHexes.get(index);
            int bottom = firstBase;
            if (currentPose) {
                bottom = base(first, coords);
            } else if (first.getBldgClass() == IBuilding.BRIDGE) {
                bottom += first.getDesign().bridgeDeck(relative);
            } else if (first instanceof MobileStructure mobile && mobile.getMovementMode() == EntityMovementMode.TRACKED
                  && first.getGame() != null) {
                var board = first.getGame().getBoard(first);
                int surface = board.getHex(coords) == null ? 0 : board.getHex(coords).getLevel();
                bottom = mobile.getStructureBaseElevation()
                      + megamek.common.moves.MobileStructureSupport.level(first.getGame(), mobile, footprint, coords) - surface;
            }
            int top = bottom + (first.getBldgClass() == IBuilding.BRIDGE ? 0 : first.getInternalBuilding().getHeight(relative));
            if (first instanceof MobileStructure mobile && mobile.getMovementMode() == EntityMovementMode.TRACKED) {
                bottom -= 2;
            }
            int otherBottom = base(second, coords);
            int otherTop = roof(second, coords);
            if (second instanceof MobileStructure mobile && mobile.getMovementMode() == EntityMovementMode.TRACKED) {
                otherBottom -= 2;
            }
            if (first.getBldgClass() == IBuilding.BRIDGE && second.getBldgClass() == IBuilding.BRIDGE
                  ? bottom == otherBottom : top > otherBottom && otherTop > bottom) {
                return false;
            }
        }
        return true;
    }
}
