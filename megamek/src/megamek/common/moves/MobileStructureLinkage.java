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
 * Catalyst Game Labs and the Catalyst Games Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.moves;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.units.MobileStructure;

/** TO:AUE pp.79–81: independent modules joined at paired outer-hex linkages move as one structure. */
public final class MobileStructureLinkage {
    private MobileStructureLinkage() { }

    /** Stored only on the lower-ID module. Coordinates stay local to their respective module as it moves. */
    public record Link(CubeCoords hex, int otherId, CubeCoords otherHex) implements Serializable { }

    public record Pose(Coords position, int facing, int elevation) { }

    private static List<MobileStructure> modules(MobileStructure unit) {
        return unit.getGame() == null ? List.of(unit) : unit.getGame().getEntitiesVector().stream()
              .filter(MobileStructure.class::isInstance).map(MobileStructure.class::cast)
              .sorted(Comparator.comparingInt(MobileStructure::getId)).toList();
    }

    private static boolean hasPort(MobileStructure unit, CubeCoords hex) {
        return unit.getInternalBuilding().getCoordsList().contains(hex)
              && unit.getInternalBuilding().getCurrentCF(hex) > 0
              && hex.neighbors().stream().anyMatch(h -> !unit.getInternalBuilding().getCoordsList().contains(h))
              && unit.getEquipmentInHex(hex).stream().anyMatch(m -> !m.isDestroyed() && !m.isMissing() && !m.isBreached()
                    && m.getType() instanceof BuildingEquipmentType facility
                    && facility.getFacility() == BuildingEquipmentType.Facility.MODULAR_LINKAGE);
    }

    private static boolean compatible(MobileStructure a, MobileStructure b) {
        return a != b && a.getId() != b.getId() && a.getOwnerId() == b.getOwnerId()
              && a.getBoardId() == b.getBoardId() && a.getPosition() != null && b.getPosition() != null
              && !a.isDestroyed() && !a.isDoomed() && !b.isDestroyed() && !b.isDoomed()
              && a.getMovementMode() == b.getMovementMode() && a.getMaximumMPQuarters() == b.getMaximumMPQuarters()
              && a.getPowerSystem() == b.getPowerSystem()
              && (MobileStructureAirMovement.isAir(a)
                    ? MobileStructureAirMovement.absoluteBase(a, a.getPosition(), a.getElevation())
                          == MobileStructureAirMovement.absoluteBase(b, b.getPosition(), b.getElevation())
                    : a.getElevation() == b.getElevation());
    }

    private static boolean mates(MobileStructure a, CubeCoords hex, MobileStructure b, CubeCoords otherHex) {
        return compatible(a, b) && hasPort(a, hex) && hasPort(b, otherHex)
              && a.relativeToBoard(hex).distance(b.relativeToBoard(otherHex)) == 1;
    }

    private static boolean active(MobileStructure owner, Link link) {
        return owner.getGame() != null && owner.getGame().getEntity(link.otherId()) instanceof MobileStructure other
              && mates(owner, link.hex(), other, link.otherHex());
    }

    /** Check physical contact when making a new connection; existing groups supply their shared support plane. */
    private static boolean overlaps(MobileStructure a, CubeCoords hex, MobileStructure b, CubeCoords otherHex) {
        Coords aCoords = a.relativeToBoard(hex), bCoords = b.relativeToBoard(otherHex);
        return megamek.common.units.BuildingElevation.altitude(a.getGame(), a, aCoords, 0)
              < megamek.common.units.BuildingElevation.altitude(b.getGame(), b, bCoords, b.getHeight(bCoords))
              && megamek.common.units.BuildingElevation.altitude(b.getGame(), b, bCoords, 0)
                    < megamek.common.units.BuildingElevation.altitude(a.getGame(), a, aCoords, a.getHeight(aCoords));
    }

    public static List<MobileStructure> group(MobileStructure unit) {
        var all = modules(unit);
        Set<MobileStructure> found = new HashSet<>();
        var pending = new ArrayDeque<MobileStructure>();
        pending.add(unit);
        while (!pending.isEmpty()) {
            MobileStructure current = pending.removeFirst();
            if (!found.add(current)) {
                continue;
            }
            for (var owner : all) {
                for (var link : owner.getModuleLinks()) {
                    if (active(owner, link) && (owner == current || link.otherId() == current.getId())) {
                        pending.add(owner == current ? (MobileStructure) current.getGame().getEntity(link.otherId()) : owner);
                    }
                }
            }
        }
        return found.stream().sorted(Comparator.comparingInt(MobileStructure::getId)).toList();
    }

    /** Direct partners, rather than the whole transitive group, can be detached from this module. */
    public static List<MobileStructure> partners(MobileStructure unit) {
        return modules(unit).stream().filter(other -> other != unit && directlyLinked(unit, other)).toList();
    }

    public static boolean directlyLinked(MobileStructure a, MobileStructure b) {
        MobileStructure owner = a.getId() < b.getId() ? a : b;
        MobileStructure other = owner == a ? b : a;
        return owner.getModuleLinks().stream().anyMatch(link -> link.otherId() == other.getId() && active(owner, link));
    }

    public static boolean occupiedPort(MobileStructure unit, CubeCoords hex) {
        return modules(unit).stream().anyMatch(owner -> owner.getModuleLinks().stream().anyMatch(link -> active(owner, link)
              && ((owner == unit && link.hex().equals(hex)) || (link.otherId() == unit.getId() && link.otherHex().equals(hex)))));
    }

    private static List<Link> availableLinks(MobileStructure a, MobileStructure b) {
        if (!compatible(a, b) || a.isDone() || b.isDone() || a.mpUsed != 0 || b.mpUsed != 0
              || a.getMovementProgress() != null || b.getMovementProgress() != null
              || a.getWaterSpeedQuarters() != b.getWaterSpeedQuarters()) {
            return List.of();
        }
        MobileStructure owner = a.getId() < b.getId() ? a : b;
        MobileStructure other = owner == a ? b : a;
        var result = new ArrayList<Link>();
        Set<CubeCoords> usedOther = new HashSet<>();
        var order = Comparator.comparingDouble(CubeCoords::q).thenComparingDouble(CubeCoords::r);
        for (var hex : owner.getInternalBuilding().getCoordsList().stream().sorted(order).toList()) {
            if (occupiedPort(owner, hex)) {
                continue;
            }
            other.getInternalBuilding().getCoordsList().stream().sorted(order)
                  .filter(h -> !usedOther.contains(h) && !occupiedPort(other, h) && mates(owner, hex, other, h)
                        && overlaps(owner, hex, other, h))
                  .findFirst().ifPresent(h -> {
                      result.add(new Link(hex, other.getId(), h));
                      usedOther.add(h);
                  });
        }
        return result;
    }

    public static List<MobileStructure> candidates(MobileStructure unit) {
        return modules(unit).stream().filter(other -> !availableLinks(unit, other).isEmpty()).toList();
    }

    public static boolean canLink(MobileStructure a, MobileStructure b) {
        return !availableLinks(a, b).isEmpty();
    }

    /** Engage every available mating pair between the chosen modules, including redundant neighboring ports. */
    public static boolean link(MobileStructure a, MobileStructure b) {
        var additions = availableLinks(a, b);
        if (additions.isEmpty()) {
            return false;
        }
        MobileStructure owner = a.getId() < b.getId() ? a : b;
        var links = new ArrayList<>(owner.getModuleLinks());
        links.addAll(additions);
        owner.setModuleLinks(links);
        group(a).forEach(MobileStructure::cancelMovementProgress);
        return true;
    }

    public static boolean unlink(MobileStructure a, MobileStructure b) {
        if (a == b) {
            return false;
        }
        MobileStructure owner = a.getId() < b.getId() ? a : b;
        MobileStructure other = owner == a ? b : a;
        var links = new ArrayList<>(owner.getModuleLinks());
        if (!links.removeIf(link -> link.otherId() == other.getId())) {
            return false;
        }
        group(a).forEach(MobileStructure::cancelMovementProgress);
        owner.setModuleLinks(links);
        return true;
    }

    /** A destroyed linkage hex breaks only its own connections; another neighboring pair can still hold. */
    public static void removeHex(MobileStructure unit, CubeCoords hex) {
        for (var owner : modules(unit)) {
            var links = new ArrayList<>(owner.getModuleLinks());
            links.removeIf(link -> (owner == unit && link.hex().equals(hex))
                  || (link.otherId() == unit.getId() && link.otherHex().equals(hex)));
            owner.setModuleLinks(links);
        }
    }

    /** Nuclear modules have unlimited range; each fuel-burning module retains its own independent supply. */
    public static double operatingRange(MobileStructure unit) {
        return group(unit).stream().filter(m -> !m.hasFusionOrFissionPower()).mapToDouble(MobileStructure::getOperatingRange)
              .min().orElse(Double.POSITIVE_INFINITY);
    }

    public static CubeCoords rotate(CubeCoords hex, int turns) {
        for (int n = 0; n < Math.floorMod(turns, 6); n++) {
            hex = new CubeCoords(-hex.r(), -hex.s(), -hex.q());
        }
        return hex;
    }

    /** Rigidly transform a module from the leader's actual pose into a hypothetical pose. Never changes game state. */
    public static Pose pose(MobileStructure leader, MobileStructure module, Coords position, int facing, int elevation) {
        int turns = Math.floorMod(facing - leader.getFacing(), 6);
        CubeCoords offset = module.getPosition().toCube().subtract(leader.getPosition().toCube());
        Coords destination = position.toCube().add(rotate(offset, turns)).toOffset();
        int localElevation = module.getElevation() + elevation - leader.getElevation();
        if (MobileStructureAirMovement.isAir(leader)) {
            int change = MobileStructureAirMovement.absoluteBase(leader, position, elevation)
                  - MobileStructureAirMovement.absoluteBase(leader, leader.getPosition(), leader.getElevation());
            localElevation = MobileStructureAirMovement.translatedElevation(module, module.getPosition(), destination,
                  module.getElevation()) + change;
        }
        return new Pose(destination, Math.floorMod(module.getFacing() + turns, 6), localElevation);
    }

    public static MoveStep step(MobileStructure module, MoveStep source, Pose pose) {
        MoveStep result = new MoveStep(new MovePath(module.getGame(), module), source.getType());
        result.copy(module.getGame(), source);
        result.setPosition(pose.position());
        result.setFacing(pose.facing());
        result.setElevation(pose.elevation());
        return result;
    }

    public static List<Coords> footprint(MobileStructure leader, Coords position, int facing, int elevation) {
        return group(leader).stream().flatMap(module -> {
            var pose = pose(leader, module, position, facing, elevation);
            return module.computeBuildingCoordsForPositionAndFacing(pose.position(), pose.facing()).stream();
        }).distinct().toList();
    }
}
