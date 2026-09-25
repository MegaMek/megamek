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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.game.Game;

/** Shared spatial and movement rules for stand-alone wall/fence hexsides (TO:AR pp.113-116). */
public final class WallRules {
    private WallRules() { }

    public static IBuilding getBuilding(Game game, Targetable target) {
        if (target instanceof WallTarget wall) {
            Segment segment = wall.segment(game);
            return segment == null ? null : segment.building();
        }
        if (target instanceof IBuilding building) {
            return building;
        }
        if (target instanceof BuildingTarget buildingTarget && buildingTarget.getBuildingEntityId() != null
              && game.getEntity(buildingTarget.getBuildingEntityId()) instanceof IBuilding building) {
            return building;
        }
        if (target instanceof Entity entity) {
            return BuildingElevation.at(game, entity.getPosition(), entity.getBoardId(), entity.getElevation());
        }
        return game.getBoard(target.getBoardId()).getBuildingAt(target.getPosition());
    }

    /** A live view of one authored segment. Relative direction is retained for its independent combat state. */
    public record Segment(AbstractBuildingEntity building, CubeCoords relativeHex, int relativeSide) {
        public Coords hex() { return building.relativeToBoard(relativeHex); }
        public int side() { return Math.floorMod(relativeSide + building.getFacing(), 6); }
        public Coords otherHex() { return hex().translated(side()); }
        public int cf() { return building.getWallSegmentState().getCF(relativeHex, relativeSide); }
        public int armor() { return building.getWallSegmentState().getArmor(relativeHex, relativeSide); }
        public int phaseCF() { return building.getWallSegmentState().getPhaseCF(relativeHex, relativeSide); }
        public int height() { return building.getInternalBuilding().getHeight(relativeHex); }
        public boolean fence() { return building.getBldgClass() == IBuilding.FENCE; }
        public int baseAltitude() {
            Hex hex = building.getGame().getBoard(building.getBoardId()).getHex(hex());
            return (hex == null ? 0 : hex.getLevel()) + building.getElevation();
        }
        public int topAltitude() { return baseAltitude() + height(); }
        public boolean borders(Coords coords) { return hex().equals(coords) || otherHex().equals(coords); }
        public void setCF(int value) { building.getWallSegmentState().setCF(relativeHex, relativeSide, value); }
        public void setArmor(int value) { building.getWallSegmentState().setArmor(relativeHex, relativeSide, value); }
        public WallTarget target(Coords observer) { return new WallTarget(this).viewFrom(observer); }
    }

    public static List<Segment> segments(Game game, int boardId) {
        if (game == null || !game.hasBoard(boardId)) {
            return List.of();
        }
        List<Segment> segments = new ArrayList<>();
        for (IBuilding candidate : game.getBoard(boardId).getBuildingsVector()) {
            if (!(candidate instanceof AbstractBuildingEntity building) || !BuildingConstruction.usesHexsides(building)
                  || building.isDestroyed()) {
                continue;
            }
            building.getWallSegmentState().initialize(building);
            for (CubeCoords hex : building.getInternalBuilding().getOriginalCoordsList()) {
                for (int side = 0; side < 6; side++) {
                    if ((building.getDesign().wallSides(hex) & (1 << side)) != 0) {
                        Segment segment = new Segment(building, hex, side);
                        if (segment.cf() > 0) {
                            segments.add(segment);
                        }
                    }
                }
            }
        }
        return segments;
    }

    public static List<Segment> at(Game game, int boardId, Coords hex, int side) {
        return segments(game, boardId).stream().filter(segment -> segment.hex().equals(hex) && segment.side() == side
              || segment.otherHex().equals(hex) && (segment.side() + 3) % 6 == side).toList();
    }

    public static List<Segment> beside(Game game, int boardId, Coords hex) {
        return segments(game, boardId).stream().filter(segment -> segment.borders(hex)).toList();
    }

    /** One physical edge has one owner, including the same edge authored from its opposite hex. */
    public static boolean hasOverlappingSegment(AbstractBuildingEntity building, Coords origin, int facing, int boardId) {
        var layout = building.computeLayoutForPositionAndFacing(origin, facing);
        for (var cell : layout.entrySet()) {
            for (int side = 0; side < 6; side++) {
                if ((building.getDesign().wallSides(cell.getKey()) & (1 << side)) != 0
                      && at(building.getGame(), boardId, cell.getValue(), Math.floorMod(side + facing, 6)).stream()
                            .anyMatch(segment -> segment.building() != building)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Includes every surviving segment touched by a moving structure's newly occupied footprint. */
    public static List<Segment> intersecting(Game game, int boardId, Set<Coords> footprint) {
        return segments(game, boardId).stream()
              .filter(segment -> footprint.contains(segment.hex()) || footprint.contains(segment.otherHex())).toList();
    }

    public static List<Segment> crossed(Entity entity, Coords from, Coords to, int fromElevation, int toElevation) {
        if (entity.getGame() == null || from == null || to == null || from.distance(to) != 1 || entity.isAirborne()) {
            return List.of();
        }
        var board = entity.getGame().getBoard(entity.getBoardId());
        if (board.getHex(from) == null || board.getHex(to) == null) {
            return List.of();
        }
        int lowerAltitude = Math.min(board.getHex(from).getLevel() + fromElevation,
              board.getHex(to).getLevel() + toElevation);
        int upperAltitude = Math.max(board.getHex(from).getLevel() + fromElevation,
              board.getHex(to).getLevel() + toElevation) + entity.getHeight();
        return at(entity.getGame(), entity.getBoardId(), from, from.direction(to)).stream()
              .filter(segment -> segment.topAltitude() > lowerAltitude && segment.baseAltitude() <= upperAltitude)
              .filter(segment -> !segment.building().getBuildingRuntimeState().openPassage(segment.building(), entity,
                    from, to, toElevation)).toList();
    }

    /** Conventional infantry moving only with Ground MP are the sole units impeded by a fence. */
    public static boolean affectedByFence(Entity entity) {
        return entity instanceof ConvInfantry && switch (entity.getMovementMode()) {
            case INF_LEG, INF_MOTORIZED, TRACKED, WHEELED, HOVER -> true;
            default -> false;
        };
    }

    public static int movementCost(Entity entity, Coords from, Coords to, int fromElevation, int toElevation,
          boolean climbing) {
        int cost = 0;
        for (Segment segment : crossed(entity, from, to, fromElevation, toElevation)) {
            if (segment.fence()) {
                if (affectedByFence(entity)) {
                    cost += entity.getMovementMode() == EntityMovementMode.INF_LEG ? 0 : 1;
                }
            } else if (entity instanceof ProtoMek || entity instanceof Infantry infantry && infantry.isMechanized()) {
                cost++;
            } else if (!(entity instanceof Infantry)) {
                cost += segment.building().getBuildingType().getTypeValue();
            }
        }
        return cost;
    }

    public static boolean canCross(Entity entity, Coords from, Coords to, int fromElevation, int toElevation,
          boolean climbing) {
        for (Segment segment : crossed(entity, from, to, fromElevation, toElevation)) {
            if (segment.fence()) {
                if (affectedByFence(entity) && entity.getMovementMode() == EntityMovementMode.INF_LEG) {
                    return false;
                }
            } else if (segment.armor() > 0 && !(entity instanceof Infantry)) {
                return false;
            }
        }
        return true;
    }

    public static int pilotingModifier(Segment segment) {
        return switch (segment.building().getBuildingType()) {
            case LIGHT, MEDIUM -> 0;
            case HEAVY -> 1;
            case HARDENED -> 3;
            default -> 0;
        };
    }

    public static boolean canClimb(Entity entity) {
        if (entity instanceof Infantry infantry) {
            return !infantry.isMechanized() && entity.getMovementMode() != EntityMovementMode.INF_MOTORIZED
                  && (!(infantry instanceof megamek.common.battleArmor.BattleArmor armor)
                        || armor.getChassisType() != megamek.common.battleArmor.BattleArmor.CHASSIS_TYPE_QUAD);
        }
        return megamek.common.moves.ClimbingHelper.canClimb(entity)
              || entity instanceof ProtoMek proto && !proto.isQuad() && !proto.isProne()
                    && proto.getInternal(ProtoMek.LOC_LEFT_ARM) > 0 && proto.getInternal(ProtoMek.LOC_RIGHT_ARM) > 0;
    }

    public static boolean canClimbStep(Entity entity, WallTarget target, Coords position, int elevation) {
        Segment segment = target == null ? null : target.segment(entity.getGame());
        if (segment == null || !canClimb(entity) || !segment.borders(position)
              || segment.fence() && !(entity instanceof ConvInfantry
                    && entity.getMovementMode() == EntityMovementMode.INF_LEG)) {
            return false;
        }
        int absolute = entity.getGame().getBoard(entity.getBoardId()).getHex(position).getLevel() + elevation;
        return absolute >= segment.baseAltitude() && absolute <= segment.topAltitude();
    }

    public static boolean canStandOn(Entity entity, WallTarget target, Coords position, int elevation) {
        Segment segment = target == null ? null : target.segment(entity.getGame());
        return segment != null && !segment.fence() && segment.borders(position)
              && elevation + entity.getGame().getBoard(entity).getHex(position).getLevel() == segment.topAltitude();
    }

    public static Point2D.Double center(Coords coords) {
        return new Point2D.Double(1.5 * coords.getX(), Math.sqrt(3) * (coords.getY() + .5 * (coords.getX() & 1)));
    }

    /** Two endpoints of the physical hexside, in flat-topped hex coordinates with unit radius. */
    public static Line2D.Double edge(Segment segment) {
        var center = center(segment.hex());
        double firstAngle = Math.toRadians(-120 + 60 * segment.side());
        double lastAngle = firstAngle + Math.PI / 3;
        return new Line2D.Double(center.x + Math.cos(firstAngle), center.y + Math.sin(firstAngle),
              center.x + Math.cos(lastAngle), center.y + Math.sin(lastAngle));
    }

    /** Exact edge intersections prevent a wall on another side of the hex from blocking an open sight line. */
    public static boolean blocksLOS(Game game, int boardId, Coords from, Coords to, int fromHeight, int toHeight,
          boolean diagramLOS, WallTarget targetedWall) {
        if (from == null || to == null || from.equals(to)) {
            return false;
        }
        var a = center(from);
        var b = center(to);
        var sight = new Line2D.Double(a, b);
        for (Segment segment : segments(game, boardId)) {
            if (segment.fence() || targetedWall != null && segment.borders(targetedWall.anchor())
                  && segment.target(null).getId() == targetedWall.getId()
                  && segment.target(null).getTargetType() == targetedWall.getTargetType()) {
                continue;
            }
            Line2D.Double edge = edge(segment);
            if (!sight.intersectsLine(edge)) {
                continue;
            }
            int top = segment.topAltitude();
            if (diagramLOS) {
                double ex = edge.x2 - edge.x1;
                double ey = edge.y2 - edge.y1;
                double denominator = (b.x - a.x) * ey - (b.y - a.y) * ex;
                if (Math.abs(denominator) < 1e-9) {
                    continue;
                }
                double fraction = ((edge.x1 - a.x) * ey - (edge.y1 - a.y) * ex) / denominator;
                double height = fromHeight + (toHeight - fromHeight) * fraction;
                if (top > height && segment.baseAltitude() <= height) {
                    return true;
                }
            } else if (segment.baseAltitude() <= Math.max(fromHeight, toHeight)
                  && (top > Math.max(fromHeight, toHeight) || segment.borders(from) && top > fromHeight
                        || segment.borders(to) && top > toHeight)) {
                return true;
            }
        }
        return false;
    }

    /** A one-level wall bordering a standing Mek provides the same horizontal cover as a one-level hill. */
    public static WallTarget targetCover(Game game, int boardId, Coords from, Coords to, int targetHeight) {
        if (from == null || to == null || from.equals(to)) {
            return null;
        }
        var sight = new Line2D.Double(center(from), center(to));
        return beside(game, boardId, to).stream().filter(segment -> !segment.fence()
                    && segment.topAltitude() == targetHeight && sight.intersectsLine(edge(segment)))
              .findFirst().map(segment -> segment.target(from)).orElse(null);
    }
}
