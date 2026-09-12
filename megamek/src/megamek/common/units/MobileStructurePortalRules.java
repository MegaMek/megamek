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
import java.util.List;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.game.Game;
import megamek.common.moves.MobileStructureLinkage;

/** Large Portals and the physical tunnel behind them, TO:AUE pp.36,76. Ordinary large doors do not qualify. */
public final class MobileStructurePortalRules {
    private MobileStructurePortalRules() { }

    /** A deployed portal's closed position remains fixed when the portal moves aside. */
    public record Connection(List<Coords> closedHexes, int facing, int complexId, Integer openedRound)
          implements Serializable {
        public Connection {
            // Native savegames require an ordinary collection, not JDK immutable-collection serialization proxies.
            closedHexes = new ArrayList<>(closedHexes);
        }
    }

    public static boolean isPortal(IBuilding building) {
        return building instanceof MobileStructure mobile && mobile.getBldgClass() == IBuilding.HANGAR
              && mobile.getDesign().isOpenSpace();
    }

    public static List<String> validationIssues(AbstractBuildingEntity building) {
        if (!isPortal(building)) {
            return List.of();
        }
        var design = building.getDesign();
        var hexes = building.getInternalBuilding().getOriginalCoordsList();
        return design.getPortalHex2() == null || design.getPortalHex3() == null
              || !hexes.contains(design.getPortalHex2()) || !hexes.contains(design.getPortalHex3())
              || design.getPortalHex2().equals(design.getPortalHex3())
              ? List.of("Large Portals must identify two distinct occupied equipment-template hexes (Hex 2 and Hex 3, TO:AUE p.76).")
              : List.of();
    }

    private static java.util.Map<String, Long> equipmentAt(AbstractBuildingEntity building, CubeCoords hex) {
        return building.getEquipment().stream()
              .filter(mount -> BuildingConstruction.equipmentPositions(building, mount).stream().anyMatch(p -> p.hex().equals(hex)))
              .collect(java.util.stream.Collectors.groupingBy(mount -> mount.getType().getInternalName() + ";" + mount.getSize()
                    + ";" + mount.getOriginalShots(), java.util.stream.Collectors.counting()));
    }

    /** The middle and following hex of each complete five-hex tunnel section repeat the two authored templates. */
    public static boolean tunnelEquipmentMatches(MobileStructure portal, AbstractBuildingEntity complex,
          List<Coords> closed, int facing) {
        if (!validationIssues(portal).isEmpty() || closed.isEmpty()) {
            return false;
        }
        var lateral = closed.stream().sorted(java.util.Comparator.comparingDouble(c -> oriented(c, facing).q())).toList();
        Coords entrance = lateral.get((lateral.size() - 1) / 2);
        int rear = Math.floorMod(facing + 3, 6);
        var template2 = equipmentAt(portal, portal.getDesign().getPortalHex2());
        var template3 = equipmentAt(portal, portal.getDesign().getPortalHex3());
        for (int start = 1; ; start += 5) {
            final int first = start;
            if (java.util.stream.IntStream.range(first, first + 5).anyMatch(distance -> !complex.isIn(entrance.translated(rear, distance)))) {
                return true;
            }
            if (!equipmentAt(complex, complex.boardToRelative(entrance.translated(rear, start + 2))).equals(template3)
                  || !equipmentAt(complex, complex.boardToRelative(entrance.translated(rear, start + 3))).equals(template2)) {
                return false;
            }
        }
    }

    private static boolean isComplex(IBuilding building) {
        return building instanceof AbstractBuildingEntity authored && !(building instanceof MobileStructure)
              && building.getBldgClass() == IBuilding.CASTLE_BRIAN && authored.getDesign().isOpenSpace()
              && authored.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND;
    }

    /** Portal height includes its two motive levels when ground based. */
    private static int clearance(MobileStructure unit) {
        return unit.getInternalBuilding().getBuildingHeight() + Math.max(0, unit.getStructureBaseElevation());
    }

    private static CubeCoords oriented(Coords coords, int facing) {
        CubeCoords cube = coords.toCube();
        for (int i = 0; i < Math.floorMod(6 - facing, 6); i++) {
            cube = new CubeCoords(-cube.r(), -cube.s(), -cube.q());
        }
        return cube;
    }

    static int width(List<Coords> footprint, int facing) {
        return (int) (footprint.stream().mapToDouble(c -> oriented(c, facing).q()).max().orElse(0)
              - footprint.stream().mapToDouble(c -> oriented(c, facing).q()).min().orElse(0)) + 1;
    }

    static int depth(List<Coords> footprint, int facing) {
        var longitudinal = footprint.stream().map(c -> oriented(c, facing)).mapToDouble(c -> 2 * c.r() + c.q()).summaryStatistics();
        return footprint.isEmpty() ? 0 : (int) Math.ceil((longitudinal.getMax() - longitudinal.getMin()) / 2.0) + 1;
    }

    private static AbstractBuildingEntity complexBehind(MobileStructure portal, List<Coords> closed, int facing) {
        Game game = portal.getGame();
        if (game == null || closed.isEmpty()) {
            return null;
        }
        int rear = Math.floorMod(facing + 3, 6);
        Coords firstRear = closed.getFirst().translated(rear);
        return game.getBoard(portal.getBoardId()).getBuildingsAt(firstRear).stream()
              .filter(MobileStructurePortalRules::isComplex).map(AbstractBuildingEntity.class::cast)
              .filter(complex -> closed.stream().allMatch(gate -> {
                  Hex face = game.getHex(gate, portal.getBoardId());
                  Coords back = gate.translated(rear);
                  Hex hill = game.getHex(back, portal.getBoardId());
                  return face != null && hill != null && !closed.contains(back) && complex.isIn(back)
                        && hill.getLevel() >= face.getLevel() + clearance(portal)
                        && BuildingElevation.altitude(game, complex, back, 0) == face.getLevel()
                        && BuildingElevation.altitude(game, complex, back, complex.getHeight(back))
                              >= face.getLevel() + clearance(portal);
              })).filter(complex -> tunnelEquipmentMatches(portal, complex, closed, facing)).findFirst().orElse(null);
    }

    /** Deploy the underground tunnel first, then put its portal flat against the hill with its rear toward it. */
    public static boolean validDeployment(MobileStructure portal, Coords position, int facing) {
        return !isPortal(portal) || complexBehind(portal,
              portal.computeBuildingCoordsForPositionAndFacing(position, facing), facing) != null;
    }

    /** Called when either half is published; returns portal entities whose replicated connection changed. */
    public static List<MobileStructure> published(Game game) {
        List<MobileStructure> changed = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (!(entity instanceof MobileStructure portal) || !isPortal(portal) || portal.getPosition() == null) {
                continue;
            }
            Connection old = portal.getBuildingRuntimeState().getPortalConnection();
            Connection connection = old == null ? new Connection(portal.getCoordsList(), portal.getFacing(), Entity.NONE, null) : old;
            if (connection.complexId() == Entity.NONE) {
                var complex = complexBehind(portal, connection.closedHexes(), connection.facing());
                if (complex != null) {
                    connection = new Connection(connection.closedHexes(), connection.facing(), complex.getId(), null);
                }
            }
            boolean clear = connection.closedHexes().stream().noneMatch(portal.getCoordsList()::contains);
            Integer opened = clear ? connection.openedRound() == null ? game.getRoundCount() : connection.openedRound() : null;
            connection = new Connection(connection.closedHexes(), connection.facing(), connection.complexId(), opened);
            if (!connection.equals(old)) {
                portal.getBuildingRuntimeState().setPortalConnection(connection);
                changed.add(portal);
            }
        }
        return changed;
    }

    private static boolean openPreviously(MobileStructure portal, AbstractBuildingEntity complex) {
        Connection connection = portal.getBuildingRuntimeState().getPortalConnection();
        return isPortal(portal) && !portal.isDestroyed() && !portal.isDoomed() && connection != null
              && connection.complexId() == complex.getId() && connection.openedRound() != null
              && connection.openedRound() < portal.getGame().getRoundCount()
              && connection.closedHexes().stream().noneMatch(portal.getCoordsList()::contains);
    }

    private static List<Coords> currentFootprint(MobileStructure unit) {
        return MobileStructureLinkage.group(unit).stream().flatMap(module -> module.getCoordsList().stream()).distinct().toList();
    }

    private static int groupClearance(MobileStructure unit) {
        return MobileStructureLinkage.group(unit).stream().mapToInt(MobileStructurePortalRules::clearance).max().orElse(0);
    }

    private static boolean admitted(MobileStructure unit, AbstractBuildingEntity complex) {
        return MobileStructureLinkage.group(unit).stream().anyMatch(module -> {
            Integer id = module.getBuildingRuntimeState().getInteriorComplexId();
            return id != null && id == complex.getId();
        });
    }

    private static boolean throughMouth(List<Coords> current, List<Coords> next, IBuilding host, Connection connection) {
        var mouth = connection.closedHexes().stream().map(c -> c.translated(Math.floorMod(connection.facing() + 3, 6))).toList();
        return current.stream().anyMatch(connection.closedHexes()::contains)
              && next.stream().filter(host::isIn).filter(c -> !current.contains(c)).allMatch(mouth::contains);
    }

    /** All geometry is checked before authorizing the paid movement; an ordinary door can never satisfy this. */
    public static boolean canEnter(MobileStructure unit, IBuilding host, List<Coords> next, int facing) {
        if (!isComplex(host) || unit.getGame() == null || isPortal(unit) || next.stream().noneMatch(host::isIn)) {
            return false;
        }
        AbstractBuildingEntity complex = (AbstractBuildingEntity) host;
        if (next.stream().filter(host::isIn).anyMatch(c -> host.getHeight(c) < groupClearance(unit))) {
            return false;
        }
        if (width(host.getCoordsList(), facing) < width(next, facing)
              || depth(host.getCoordsList(), facing) < depth(next, facing)) {
            return false;
        }
        // Closing the portal does not remove the tunnel floor or stop a structure already inside from moving within it.
        if (admitted(unit, complex)) {
            return true;
        }
        for (Entity candidate : unit.getGame().getEntitiesVector()) {
            if (!(candidate instanceof MobileStructure portal) || !openPreviously(portal, complex)
                  || clearance(portal) < groupClearance(unit)) {
                continue;
            }
            Connection connection = portal.getBuildingRuntimeState().getPortalConnection();
            if (width(connection.closedHexes(), connection.facing()) >= width(next, connection.facing())
                  && throughMouth(currentFootprint(unit), next, host, connection)) {
                return true;
            }
        }
        return false;
    }
    /** Local surface-relative elevation of an authorized underground passage's floor, or null outside it. */
    public static Integer supportElevation(MobileStructure unit, Coords coords) {
        if (unit.getGame() == null || coords == null || isPortal(unit)) {
            return null;
        }
        for (IBuilding host : unit.getGame().getBoard(unit.getBoardId()).getBuildingsAt(coords)) {
            if (isComplex(host) && (admitted(unit, (AbstractBuildingEntity) host)
                  || canEnter(unit, host, List.of(coords), unit.getFacing()))) {
                return BuildingElevation.base(host, coords);
            }
        }
        return null;
    }

    public static void recordMovement(MobileStructure unit, List<Coords> next, int facing) {
        Integer admitted = null;
        for (Coords coords : next) {
            for (IBuilding host : unit.getGame().getBoard(unit.getBoardId()).getBuildingsAt(coords)) {
                if (canEnter(unit, host, next, facing)) {
                    admitted = ((Entity) host).getId();
                }
            }
        }
        unit.getBuildingRuntimeState().setInteriorComplexId(admitted);
    }
}
