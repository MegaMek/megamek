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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.BuildingElevation;
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
        List<CubeCoords> hullHexes = unit.getInternalBuilding().getCoordsList();
        if (!hullHexes.contains(hex)) {
            return false;
        }
        boolean isHexStanding = unit.getInternalBuilding().getCurrentCF(hex) > 0;
        boolean isOuterHex = hasNeighborOutsideHull(hex, hullHexes);
        boolean hasWorkingLinkage = hasWorkingModularLinkage(unit, hex);
        return isHexStanding && isOuterHex && hasWorkingLinkage;
    }

    /** @return {@code true} when at least one neighbor of the hex is not part of the hull */
    private static boolean hasNeighborOutsideHull(CubeCoords hex, List<CubeCoords> hullHexes) {
        for (CubeCoords neighbor : hex.neighbors()) {
            if (!hullHexes.contains(neighbor)) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} when the hex mounts a Modular Linkage that is not destroyed, missing or breached */
    private static boolean hasWorkingModularLinkage(MobileStructure unit, CubeCoords hex) {
        for (Mounted<?> mounted : unit.getEquipmentInHex(hex)) {
            boolean isWorking = !mounted.isDestroyed() && !mounted.isMissing() && !mounted.isBreached();
            boolean isModularLinkage = (mounted.getType() instanceof BuildingEquipmentType facility)
                  && (facility.getFacility() == BuildingEquipmentType.Facility.MODULAR_LINKAGE);
            if (isWorking && isModularLinkage) {
                return true;
            }
        }
        return false;
    }

    private static boolean compatible(MobileStructure module, MobileStructure otherModule) {
        boolean isDifferentModule = (module != otherModule) && (module.getId() != otherModule.getId());
        boolean hasSameOwner = module.getOwnerId() == otherModule.getOwnerId();
        boolean isOnSameBoard = module.getBoardId() == otherModule.getBoardId();
        boolean areBothDeployed = (module.getPosition() != null) && (otherModule.getPosition() != null);
        if (!isDifferentModule || !hasSameOwner || !isOnSameBoard || !areBothDeployed) {
            return false;
        }
        boolean areBothIntact = isIntact(module) && isIntact(otherModule);
        boolean hasSameMovementMode = module.getMovementMode() == otherModule.getMovementMode();
        boolean hasSameMaximumMP = module.getMaximumMPQuarters() == otherModule.getMaximumMPQuarters();
        boolean hasSamePowerSystem = module.getPowerSystem() == otherModule.getPowerSystem();
        boolean isAtSameHeight = isAtSameHeight(module, otherModule);
        return areBothIntact && hasSameMovementMode && hasSameMaximumMP && hasSamePowerSystem && isAtSameHeight;
    }

    private static boolean isIntact(MobileStructure module) {
        return !module.isDestroyed() && !module.isDoomed();
    }

    /** Air modules compare their absolute base level; all others compare their elevation. */
    private static boolean isAtSameHeight(MobileStructure module, MobileStructure otherModule) {
        if (MobileStructureAirMovement.isAir(module)) {
            int moduleBase = MobileStructureAirMovement.absoluteBase(module, module.getPosition(), module.getElevation());
            int otherModuleBase = MobileStructureAirMovement.absoluteBase(otherModule, otherModule.getPosition(),
                  otherModule.getElevation());
            return moduleBase == otherModuleBase;
        }
        return module.getElevation() == otherModule.getElevation();
    }

    private static boolean mates(MobileStructure module, CubeCoords hex, MobileStructure otherModule,
          CubeCoords otherHex) {
        return compatible(module, otherModule) && hasPort(module, hex) && hasPort(otherModule, otherHex)
              && module.relativeToBoard(hex).distance(otherModule.relativeToBoard(otherHex)) == 1;
    }

    private static boolean active(MobileStructure owner, Link link) {
        return owner.getGame() != null && owner.getGame().getEntity(link.otherId()) instanceof MobileStructure other
              && mates(owner, link.hex(), other, link.otherHex());
    }

    /** Check physical contact when making a new connection; existing groups supply their shared support plane. */
    private static boolean overlaps(MobileStructure module, CubeCoords hex, MobileStructure otherModule,
          CubeCoords otherHex) {
        Coords moduleCoords = module.relativeToBoard(hex);
        Coords otherModuleCoords = otherModule.relativeToBoard(otherHex);
        int moduleBottom = BuildingElevation.altitude(module.getGame(), module, moduleCoords, 0);
        int moduleTop = BuildingElevation.altitude(module.getGame(), module, moduleCoords,
              module.getHeight(moduleCoords));
        int otherModuleBottom = BuildingElevation.altitude(otherModule.getGame(), otherModule, otherModuleCoords, 0);
        int otherModuleTop = BuildingElevation.altitude(otherModule.getGame(), otherModule, otherModuleCoords,
              otherModule.getHeight(otherModuleCoords));
        boolean startsBelowOtherTop = moduleBottom < otherModuleTop;
        boolean otherStartsBelowTop = otherModuleBottom < moduleTop;
        return startsBelowOtherTop && otherStartsBelowTop;
    }

    public static List<MobileStructure> group(MobileStructure unit) {
        Map<Integer, List<MobileStructure>> linkedModulesById = activeLinksById(modules(unit));
        Set<MobileStructure> found = new HashSet<>();
        ArrayDeque<MobileStructure> pending = new ArrayDeque<>();
        pending.add(unit);
        while (!pending.isEmpty()) {
            MobileStructure current = pending.removeFirst();
            if (found.add(current)) {
                pending.addAll(linkedModulesById.getOrDefault(current.getId(), List.of()));
            }
        }
        return found.stream().sorted(Comparator.comparingInt(MobileStructure::getId)).toList();
    }

    /**
     * Builds, once per {@link #group(MobileStructure)} call, the modules each module is joined to by an active link, in
     * both directions, so the group search looks each module up instead of rescanning every link of every module.
     *
     * @return the directly linked modules, keyed by the ID of the module they are linked to
     */
    private static Map<Integer, List<MobileStructure>> activeLinksById(List<MobileStructure> allModules) {
        Map<Integer, List<MobileStructure>> linkedModulesById = new HashMap<>();
        for (MobileStructure owner : allModules) {
            for (Link link : owner.getModuleLinks()) {
                if (!active(owner, link)) {
                    continue;
                }
                MobileStructure other = (MobileStructure) owner.getGame().getEntity(link.otherId());
                linkedModulesById.computeIfAbsent(owner.getId(), id -> new ArrayList<>()).add(other);
                linkedModulesById.computeIfAbsent(link.otherId(), id -> new ArrayList<>()).add(owner);
            }
        }
        return linkedModulesById;
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
