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
package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.HitData;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.moves.MobileStructureGeometry;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MobileStructureMovement;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.*;

/** Automatic charges, undercarriage sweeps, grounding and displacement (TO:AUE pp.33–41). */
final class MobileStructureCollisionHandler extends AbstractTWRuleHandler {
    MobileStructureCollisionHandler(TWGameManager manager) {
        super(manager);
    }

    boolean resolve(MobileStructure attacker, MoveStep step) {
        return resolve(attacker, step, Set.of());
    }

    boolean resolve(MobileStructure attacker, MoveStep step, Set<Integer> descendingTogether) {
        var contacts = new ArrayList<>(MobileStructureGeometry.contacts(attacker, attacker.getPosition(),
              attacker.getFacing(), step.getPosition(), step.getFacing()));
        boolean vertical = collisionElevation(attacker, step) != attacker.getElevation();
        if (vertical) {
            attacker.computeLayoutForPositionAndFacing(step.getPosition(), step.getFacing()).forEach((hex, coords) ->
                  contacts.add(new MobileStructureGeometry.Contact(hex, coords, 1)));
        }
        Set<Integer> avoidanceAttempted = new HashSet<>();
        Set<Integer> collidedMobiles = new HashSet<>();
        // Binary sweep times have a finite precision. Shuffle equal-time contacts without reordering earlier impacts.
        for (int first = 0; first < contacts.size();) {
            int end = first + 1;
            while (end < contacts.size() && Math.abs(contacts.get(end).time() - contacts.get(first).time()) < 1.0 / 65536) {
                end++;
            }
            for (int i = end - 1; i > first; i--) {
                java.util.Collections.swap(contacts, i, first + Compute.randomInt(i - first + 1));
            }
            first = end;
        }
        for (var contact : contacts) {
            if (!getGame().getBoard(attacker).contains(contact.boardHex())) {
                continue;
            }
            // A pivot can sweep across an obstacle before ending clear of it. Resolve those contacts in order.
            List<Entity> targets = new ArrayList<>(getGame().getEntitiesVector(contact.boardHex(), attacker.getBoardId()));
            getGame().getEntitiesVector().stream().filter(vessel -> vessel.getBoardId() == attacker.getBoardId()
                  && LargeNavalVesselRules.applies(vessel)
                  && LargeNavalVesselRules.footprint(vessel).contains(contact.boardHex()) && !targets.contains(vessel))
                  .forEach(targets::add);
            for (IBuilding building : getGame().getBoard(attacker).getBuildingsAt(contact.boardHex())) {
                if (building instanceof MobileStructure mobile && !targets.contains(mobile)) {
                    targets.add(mobile);
                }
            }
            while (!targets.isEmpty()) {
                Entity target = targets.remove(Compute.randomInt(targets.size()));
                if (target == attacker || target.isDestroyed() || target.isDoomed() || descendingTogether.contains(target.getId())
                      || (target instanceof MobileStructure mobile && MobileStructureLinkage.group(attacker).contains(mobile))) {
                    continue;
                }
                if (!(target instanceof IBuilding) && attacker.getCoordsList().contains(target.getPosition())
                      && !target.isAirborne() && !target.isAirborneVTOLorWIGE()
                      && target.getElevation() >= attacker.getBaseElevation(target.getPosition())
                      && target.getElevation() <= attacker.getBaseElevation(target.getPosition())
                            + attacker.getHeight(target.getPosition())) {
                    continue; // Interior occupants and roof riders move with their carrier.
                }
                if (!intersects(attacker, contact.hex(), target, contact.boardHex(), collisionElevation(attacker, step))) {
                    if (attacker.getMovementMode() == EntityMovementMode.TRACKED
                          && target instanceof Tank tank && !target.isAirborneVTOLorWIGE()
                          && target.getElevation() + target.height() + 1 <= attacker.getBaseElevation(contact.boardHex())
                          && !MobileStructureMovement.canPassUnder(tank)) {
                        sweepUnderneath(tank);
                    }
                    continue;
                }
                if (avoidanceAttempted.add(target.getId()) && avoidCollision(target)) {
                    if (!(LargeNavalVesselRules.applies(target) ? LargeNavalVesselRules.footprint(target)
                          : target.getOccupiedCoords()).contains(contact.boardHex())
                          || !intersects(attacker, contact.hex(), target, contact.boardHex(), collisionElevation(attacker, step))) {
                        continue;
                    }
                }
                if (target instanceof MobileStructure mobile) {
                    if (collidedMobiles.add(target.getId()) && !collideMobiles(attacker, mobile, step, contact)) {
                        return false;
                    }
                } else if (!(target instanceof IBuilding)) {
                    int damage = attacker.isWaterStructure() ? 50 : 100;
                    boolean submerged = fullyUnderwater(attacker, step) && fullyUnderwater(target);
                    damage = submerged ? damage / 2 : damage;
                    damageUnit(target, damage, attacker);
                    int returned = target instanceof Dropship ? dropshipReturnDamage(target)
                          : (int) Math.ceil(target.getWeight() / 10.0);
                    damageHex(attacker, contact.hex(), submerged ? returned / 2 : returned);
                    target.applyDamage();
                    if (!target.isDestroyed() && !target.isDoomed()) {
                        if (vertical && step.getElevation() > attacker.getElevation() && target.getElevation() == 0
                              && LargeNavalVesselRules.applies(target) && LargeNavalVesselRules.template(target) > 0) {
                            if (!new LargeNavalVesselCollisionHandler(gameManager)
                                  .afterMobileSurfacing((Tank) target, attacker, step)) {
                                return false;
                            }
                        } else if (vertical && step.getElevation() > attacker.getElevation() && isSmallVessel(target)) {
                            strand(target, attacker, contact.hex(), step);
                        } else if (LargeNavalVesselRules.applies(target)) {
                            if (!new LargeNavalVesselCollisionHandler(gameManager)
                                  .displaceAfterCollision((Tank) target, attacker, step, false)) {
                                return false;
                            }
                        } else {
                            int direction = attacker.getPosition().equals(step.getPosition())
                                  ? attacker.getPosition().direction(contact.boardHex())
                                  : attacker.getPosition().direction(step.getPosition());
                            Coords destination = contact.boardHex().translated(direction);
                            addReport(gameManager.doEntityDisplacement(target, target.getPosition(), destination, null));
                        }
                    }
                    gameManager.entityUpdate(target.getId());
                }
                if (attacker.isDestroyed() || attacker.isDoomed()) {
                    return false;
                }
            }
        }
        gameManager.applyBuildingDamage();
        return !attacker.isDestroyed() && !attacker.isDoomed();
    }

    private static int collisionElevation(MobileStructure mobile, MoveStep step) {
        if (mobile.getMovementMode() == EntityMovementMode.VTOL) {
            return step.getElevation() + megamek.common.moves.MobileStructureAirMovement.surface(mobile, step.getPosition())
                  - megamek.common.moves.MobileStructureAirMovement.surface(mobile, mobile.getPosition());
        }
        return step.getElevation();
    }

    static boolean intersects(MobileStructure mobile, CubeCoords relative, Entity target, Coords coords, int elevation) {
        var game = mobile.getGame();
        Coords source = relative == null ? mobile.getPosition() : mobile.relativeToBoard(relative);
        var sourceHex = game.getBoard(mobile).getHex(source);
        int ground = sourceHex == null ? MobileStructureMovement.terrain(game, mobile, source).getLevel()
              : sourceHex.getLevel();
        int bottom = ground + mobile.getBaseElevation(source) + elevation - mobile.getElevation();
        int height = relative == null ? mobile.getInternalBuilding().getBuildingHeight()
              : mobile.getInternalBuilding().getHeight(relative);
        int top = bottom + height;
        int targetGround = target instanceof MobileStructure other
              ? MobileStructureMovement.terrain(game, other, coords).getLevel()
              : game.getBoard(target).getHex(target.getPosition()).getLevel();
        int targetBottom = targetGround + target.getElevation();
        int targetTop = targetBottom + target.height() + 1;
        if (LargeNavalVesselRules.applies(target)) {
            targetBottom -= LargeNavalVesselRules.belowSurface(target);
            targetTop = targetGround + target.getElevation() + LargeNavalVesselRules.aboveSurface(target);
        }
        if (target instanceof MobileStructure other) {
            targetBottom = targetGround + other.getBaseElevation(coords);
            targetTop = targetBottom + other.getHeight(coords);
        }
        if (mobile.getMovementMode() == EntityMovementMode.TRACKED) {
            return targetTop > bottom && targetBottom <= top;
        }
        return targetTop >= bottom && targetBottom <= top;
    }

    /** A successful roll grants the owner's whole walking/cruising move before the structure continues. */
    private boolean avoidCollision(Entity target) {
        if (target.isDone() || target.isImmobile() || target.isProne() || target.getWalkMP() <= 0) {
            return false;
        }
        int skill = target instanceof Infantry ? target.getCrew().getPiloting()
              : target.getBasePilotingRoll().getValue();
        if (!(target instanceof Infantry) && skill == megamek.common.rolls.TargetRoll.CHECK_FALSE) {
            skill = target.getCrew().getGunnery();
        }
        if (Compute.d6(2) < skill) {
            return false; // This failure does not cause a fall.
        }
        MovePath path = gameManager.requestMobileCollisionAvoidance(target);
        if (path == null) {
            return false;
        }
        path.setGame(getGame());
        path.setEntity(target);
        path.compile(getGame(), target, false);
        if (!isAvoidancePathLegal(target, path)) {
            return false;
        }
        getGame().removeTurnFor(target);
        new MovePathHandler(gameManager, target, path, new java.util.HashMap<>()).processMovement();
        target.setDone(true);
        gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
        gameManager.entityUpdate(target.getId());
        return true;
    }

    static boolean isAvoidancePathLegal(Entity target, MovePath path) {
        if (path.getMpUsed() > target.getWalkMP() || path.isJumping()) {
            return false;
        }
        return path.getStepVector().stream().allMatch(step -> step.isLegal(path)
              && switch (step.getType()) {
                  case FORWARDS, BACKWARDS, LATERAL_LEFT, LATERAL_RIGHT, LATERAL_LEFT_BACKWARDS,
                        LATERAL_RIGHT_BACKWARDS, TURN_LEFT, TURN_RIGHT, UP, DOWN, GET_UP, CAREFUL_STAND,
                        CLIMB_MODE_ON, CLIMB_MODE_OFF -> true;
                  default -> false;
              });
    }

    private boolean collideMobiles(MobileStructure attacker, MobileStructure defender, MoveStep step,
          MobileStructureGeometry.Contact contact) {
        int comparison = Integer.compare(onBoardHexes(attacker), onBoardHexes(defender));
        while (comparison == 0) {
            comparison = Integer.compare(Compute.d6(2), Compute.d6(2));
        }
        var defenders = MobileStructureLinkage.group(defender);
        Set<Coords> overlap = new HashSet<>(defenders.stream().flatMap(m -> m.getCoordsList().stream()).toList());
        overlap.retainAll(MobileStructureMovement.enteredHexes(attacker, attacker.getPosition(), attacker.getFacing(),
              step.getPosition(), step.getFacing()));
        if (overlap.isEmpty()) {
            overlap.add(contact.boardHex());
        }
        var next = attacker.computeLayoutForPositionAndFacing(step.getPosition(), step.getFacing());
        boolean submerged = fullyUnderwater(attacker, step) && fullyUnderwater(defender);
        for (Coords coords : overlap) {
            MobileStructure struck = defenders.stream().filter(m -> m.getCoordsList().contains(coords))
                  .findFirst().orElse(defender);
            int dealt = comparison > 0 ? 100 : 50;
            while (dealt > 0) {
                int cluster = Math.min(10, dealt);
                damageHex(struck, struck.boardToRelative(coords), submerged ? cluster / 2 : cluster);
                dealt -= Math.min(10, dealt);
            }
            CubeCoords impacting = next.entrySet().stream().filter(e -> e.getValue().equals(coords))
                  .map(java.util.Map.Entry::getKey).findFirst().orElse(contact.hex());
            int returned = comparison > 0 ? 50 : 100;
            damageHex(attacker, impacting, submerged ? returned / 2 : returned);
        }
        gameManager.applyBuildingDamage();
        if (defender.isDestroyed() || defender.isDoomed()) {
            return !attacker.isDestroyed() && !attacker.isDoomed();
        }
        MobileStructure displaced = comparison > 0 ? defender : attacker;
        Set<Coords> forbidden = new HashSet<>(comparison > 0
              ? MobileStructureLinkage.footprint(attacker, step.getPosition(), step.getFacing(), step.getElevation())
              : defenders.stream().flatMap(m -> m.getCoordsList().stream()).toList());
        Coords destination = displacement(displaced, forbidden);
        if (destination != null) {
            new MobileStructureMovementHandler(gameManager).relocate(displaced, destination,
                  displaced.getFacing(), displaced.getElevation());
            ground(displaced);
        }
        return comparison > 0 && destination != null && !attacker.isDestroyed() && !attacker.isDoomed();
    }

    private int onBoardHexes(MobileStructure unit) {
        return (int) MobileStructureLinkage.group(unit).stream().flatMap(m -> m.getCoordsList().stream())
              .distinct().filter(getGame().getBoard(unit)::contains).count();
    }

    Coords displacement(MobileStructure unit, Set<Coords> forbidden) {
        return displacement(unit, forbidden, unit.getElevation());
    }

    private Coords displacement(MobileStructure unit, Set<Coords> forbidden, int elevation) {
        var board = getGame().getBoard(unit);
        int radius = Math.max(board.getWidth(), board.getHeight());
        for (int distance = 1; distance <= radius; distance++) {
            List<Coords> candidates = new ArrayList<>();
            for (Coords coords : unit.getPosition().allAtDistance(distance)) {
                List<Coords> footprint = MobileStructureLinkage.footprint(unit, coords, unit.getFacing(), elevation);
                if (footprint.stream().noneMatch(board::contains) || footprint.stream().anyMatch(forbidden::contains)) {
                    continue;
                }
                if (canDisplaceTo(unit, coords, elevation)) {
                    candidates.add(coords);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(Compute.randomInt(candidates.size()));
            }
        }
        return null;
    }

    /** Forced movement still needs a legal complete footprint; it cannot overwrite another published structure. */
    static boolean canDisplaceTo(MobileStructure unit, Coords origin) {
        return canDisplaceTo(unit, origin, unit.getElevation());
    }

    private static boolean canDisplaceTo(MobileStructure unit, Coords origin, int elevation) {
        var members = MobileStructureLinkage.group(unit);
        var footprint = MobileStructureLinkage.footprint(unit, origin, unit.getFacing(), elevation);
        return footprint.stream().anyMatch(unit.getGame().getBoard(unit)::contains)
              && members.stream().allMatch(member -> canDisplaceModuleTo(member,
              MobileStructureLinkage.pose(unit, member, origin, unit.getFacing(), elevation), members, footprint));
    }

    private static boolean canDisplaceModuleTo(MobileStructure unit, MobileStructureLinkage.Pose pose,
          List<MobileStructure> members, List<Coords> footprint) {
        var game = unit.getGame();
        var board = game.getBoard(unit);
        var layout = unit.computeLayoutForPositionAndFacing(pose.position(), pose.facing());
        for (var cell : layout.entrySet()) {
            Coords coords = cell.getValue();
            var terrain = board.getHex(coords);
            if (terrain == null) {
                continue;
            }
            if (terrain.containsTerrain(Terrains.IMPASSABLE)) {
                return false;
            }
            if (unit.isWaterStructure() && !terrain.containsTerrain(Terrains.WATER)
                  && coords.allAdjacent().stream().map(board::getHex).filter(java.util.Objects::nonNull)
                        .noneMatch(hex -> hex.containsTerrain(Terrains.WATER))) {
                return false; // TO:AUE p.41: displacement may not put a water vessel more than one hex inland.
            }
            int base = displacementBase(unit, pose, cell.getKey(), coords, footprint);
            int top = base + unit.getInternalBuilding().getHeight(cell.getKey());
            for (IBuilding obstacle : board.getBuildingsAt(coords)) {
                if (obstacle instanceof MobileStructure mobile && members.contains(mobile)) {
                    continue;
                }
                int collisionBase = unit.getMovementMode() == EntityMovementMode.TRACKED
                      ? unit.getNavalState().isSinking() ? base - 2 : -terrain.depth() : base;
                if (obstacle != unit && BuildingElevation.base(obstacle, coords) < top
                      && BuildingElevation.roof(obstacle, coords) > collisionBase) {
                    return false;
                }
            }
            for (Entity target : game.getEntitiesVector(coords, unit.getBoardId())) {
                if (target instanceof MobileStructure mobile && members.contains(mobile)) {
                    continue;
                }
                if (!target.isAirborne() && !target.isAirborneVTOLorWIGE() && members.stream().anyMatch(member ->
                      member.getCoordsList().contains(target.getPosition())
                            && target.getElevation() >= member.getBaseElevation(target.getPosition())
                            && target.getElevation() <= member.getBaseElevation(target.getPosition())
                                  + member.getHeight(target.getPosition()))) {
                    continue; // This unit moves with the displaced structure.
                }
                if (target != unit && !target.isDestroyed() && !target.isDoomed()
                      && target.getElevation() < top && target.getElevation() + target.height() + 1 > base) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int displacementBase(MobileStructure unit, MobileStructureLinkage.Pose pose, CubeCoords relative,
          Coords coords, List<Coords> footprint) {
        if (unit.getNavalState().isSinking()) {
            return pose.elevation() + unit.getNavalState().getBaseOffsets()
                  .getOrDefault(relative, unit.getStructureBaseElevation());
        }
        var game = unit.getGame();
        var terrain = game.getBoard(unit).getHex(coords);
        return switch (unit.getMovementMode()) {
            case TRACKED -> 2 + megamek.common.moves.MobileStructureSupport.level(game, unit, footprint, coords)
                  - terrain.getLevel();
            case VTOL -> megamek.common.moves.MobileStructureAirMovement.absoluteBase(unit, pose.position(),
                  pose.elevation()) - terrain.getLevel();
            default -> pose.elevation() + unit.getStructureBaseElevation();
        };
    }

    static boolean fullyUnderwater(Entity entity) {
        if (entity instanceof MobileStructure mobile) {
            return fullyUnderwater(mobile, mobile.getPosition(), mobile.getFacing(), mobile.getElevation());
        }
        if (LargeNavalVesselRules.applies(entity)) {
            return LargeNavalVesselCollisionHandler.fullyUnderwater(entity, entity.getElevation());
        }
        return entity.getElevation() + entity.height() < 0;
    }

    private static boolean fullyUnderwater(MobileStructure unit, MoveStep step) {
        return fullyUnderwater(unit, step.getPosition(), step.getFacing(), step.getElevation());
    }

    static boolean fullyUnderwater(MobileStructure unit, Coords position, int facing, int elevation) {
        boolean onBoard = false;
        for (var module : MobileStructureLinkage.group(unit)) {
            var pose = MobileStructureLinkage.pose(unit, module, position, facing, elevation);
            for (var cell : module.computeLayoutForPositionAndFacing(pose.position(), pose.facing()).entrySet()) {
                var hex = unit.getGame().getBoard(unit).getHex(cell.getValue());
                if (hex == null) { continue; }
                onBoard = true;
                Coords source = module.relativeToBoard(cell.getKey());
                int change = module.getMovementMode() == EntityMovementMode.VTOL
                      ? megamek.common.moves.MobileStructureAirMovement.absoluteBase(module, pose.position(), pose.elevation())
                            - megamek.common.moves.MobileStructureAirMovement.absoluteBase(module, module.getPosition(), module.getElevation())
                      : pose.elevation() - module.getElevation();
                int roof = MobileStructureMovement.terrain(unit.getGame(), module, source).getLevel()
                      + module.getBaseElevation(source) + module.getInternalBuilding().getHeight(cell.getKey())
                      + change;
                if (hex.depth() <= 0 || roof >= hex.getLevel()) { return false; }
            }
        }
        return onBoard;
    }

    record FallContact(MobileStructure target, Coords coords, int roof, int levels) { }

    /** The first roof crossed by each falling hex, including elevated/submerged and non-pivot structure hexes. */
    static List<FallContact> fallContacts(Entity unit, Coords source, int sourceElevation, Coords destination,
          int reduction) {
        var game = unit.getGame();
        var board = game.getBoard(unit);
        var sourceHex = board.getHex(source);
        if (sourceHex == null || destination == null) { return List.of(); }
        java.util.Map<Coords, Integer> starts = new java.util.HashMap<>();
        var members = unit instanceof MobileStructure mobile ? MobileStructureLinkage.group(mobile)
              : List.<MobileStructure>of();
        if (unit instanceof MobileStructure mobile) {
            for (var member : members) {
                var pose = MobileStructureLinkage.pose(mobile, member, destination, mobile.getFacing(), sourceElevation);
                for (var cell : member.computeLayoutForPositionAndFacing(pose.position(), pose.facing()).entrySet()) {
                    Coords original = member.relativeToBoard(cell.getKey());
                    var originalHex = board.getHex(original);
                    if (originalHex == null) { continue; }
                    int bottom = originalHex.getLevel() + member.getBaseElevation(original)
                          + sourceElevation - mobile.getElevation()
                          - (member.getMovementMode() == EntityMovementMode.TRACKED ? 2 : 0);
                    starts.put(cell.getValue(), bottom);
                }
            }
        } else {
            starts.put(destination, sourceHex.getLevel() + sourceElevation);
        }
        var contacts = new java.util.ArrayList<FallContact>();
        for (var entry : starts.entrySet()) {
            Coords coords = entry.getKey();
            var hex = board.getHex(coords);
            if (hex == null) { continue; }
            MobileStructure first = null;
            int highest = Integer.MIN_VALUE;
            for (var candidate : board.getBuildingsAt(coords)) {
                if (!(candidate instanceof MobileStructure mobile) || members.contains(mobile)
                      || mobile.isDestroyed() || mobile.isDoomed()) { continue; }
                int roof = hex.getLevel() + BuildingElevation.roof(mobile, coords);
                if (roof <= entry.getValue() && roof >= hex.floor() && roof > highest) {
                    highest = roof;
                    first = mobile;
                }
            }
            if (first != null) {
                contacts.add(new FallContact(first, coords, highest - hex.getLevel(),
                      Math.max(0, entry.getValue() - highest - reduction)));
            }
        }
        int firstDistance = contacts.stream().mapToInt(FallContact::levels).min().orElse(0);
        return contacts.stream().filter(contact -> contact.levels() == firstDistance).toList();
    }

    /** A unit standing on a roof can receive the AFFA before the supporting structure, just as on ordinary terrain. */
    static Entity roofOccupant(Entity faller, List<FallContact> contacts) {
        var candidates = contacts.stream().flatMap(contact -> faller.getGame()
              .getEntitiesVector(contact.coords(), faller.getBoardId()).stream()
              .filter(unit -> unit != faller && !(unit instanceof IBuilding) && !(unit instanceof Infantry)
                    && unit.isTargetable() && unit.getAltitude() == 0 && unit.getElevation() == contact.roof()))
              .distinct().toList();
        return candidates.isEmpty() ? null : candidates.get(Compute.randomInt(candidates.size()));
    }

    /** Apply AFFA to the contacted hex/floor rather than the building entity's generic LOC_BASE. */
    java.util.Vector<megamek.common.Report> damageFallTargets(Entity faller, List<FallContact> contacts) {
        var reports = new java.util.Vector<megamek.common.Report>();
        for (var contact : contacts) {
            int damage = Compute.getAccidentalFallFromAboveDamageFor(faller, contact.levels());
            int level = Math.max(0, contact.target().getHeight(contact.coords()) - 1);
            while (damage > 0) {
                int cluster = Math.min(5, damage);
                reports.addAll(gameManager.damageBuilding(contact.target(), cluster, "accidental fall from above",
                      contact.coords(), level, faller, false));
                damage -= cluster;
            }
        }
        return reports;
    }

    /** The falling structure's own damage and size-based displacement, after incoming AFFA has been applied. */
    java.util.Vector<megamek.common.Report> fall(MobileStructure unit, Coords destination, int levels) {
        return fall(unit, destination, levels, fallContacts(unit, unit.getPosition(), unit.getElevation(), destination, 0));
    }

    java.util.Vector<megamek.common.Report> fall(MobileStructure unit, Coords destination, int levels,
          List<FallContact> contacts) {
        var reports = new java.util.Vector<megamek.common.Report>();
        var members = MobileStructureLinkage.group(unit);
        for (var member : members) {
            for (Coords coords : List.copyOf(member.getCoordsList())) {
                reports.addAll(gameManager.damageBuilding(member, 10 * Math.max(0, levels), "mobile structure fall",
                      coords, 0, null, false));
            }
        }
        gameManager.applyBuildingDamage();
        if (unit.isDestroyed() || unit.isDoomed()) { return reports; }
        Set<Coords> footprint = new HashSet<>(MobileStructureLinkage.footprint(unit, destination, unit.getFacing(), 0));
        var others = java.util.stream.Stream.concat(contacts.stream().map(FallContact::target), members.stream().flatMap(member -> {
            var pose = MobileStructureLinkage.pose(unit, member, destination, unit.getFacing(), 0);
            return member.computeLayoutForPositionAndFacing(pose.position(), pose.facing()).entrySet().stream()
                  .filter(cell -> getGame().getBoard(unit).contains(cell.getValue())).flatMap(cell -> {
                      int base = displacementBase(member, pose, cell.getKey(), cell.getValue(), List.copyOf(footprint));
                      int bottom = base - (member.getMovementMode() == EntityMovementMode.TRACKED ? 2 : 0);
                      int top = base + member.getInternalBuilding().getHeight(cell.getKey());
                      return getGame().getBoard(unit).getBuildingsAt(cell.getValue()).stream()
                            .filter(other -> BuildingElevation.base(other, cell.getValue()) < top
                                  && BuildingElevation.roof(other, cell.getValue()) >= bottom);
                  });
        }).filter(MobileStructure.class::isInstance).map(MobileStructure.class::cast))
              .filter(other -> !members.contains(other) && !other.isDoomed() && !other.isDestroyed()).distinct().toList();
        for (var other : others) {
            int comparison = Integer.compare(onBoardHexes(unit), onBoardHexes(other));
            while (comparison == 0) { comparison = Integer.compare(Compute.d6(2), Compute.d6(2)); }
            var displaced = comparison > 0 ? other : unit;
            Set<Coords> forbidden = comparison > 0 ? footprint
                  : new HashSet<>(MobileStructureLinkage.footprint(other, other.getPosition(), other.getFacing(), other.getElevation()));
            Coords escape = displacement(displaced, forbidden, displaced == unit ? 0 : displaced.getElevation());
            if (escape == null) {
                reports.addAll(gameManager.destroyEntity(unit, "no legal footprint after falling onto a mobile structure", false));
                return reports;
            }
            new MobileStructureMovementHandler(gameManager).relocate(displaced, escape, displaced.getFacing(),
                  displaced == unit ? 0 : displaced.getElevation());
            if (displaced == unit) { return reports; }
        }
        if (canDisplaceTo(unit, destination, 0)) {
            new MobileStructureMovementHandler(gameManager).relocate(unit, destination, unit.getFacing(), 0);
        } else {
            reports.addAll(gameManager.destroyEntity(unit, "no legal footprint after a fall", false));
        }
        return reports;
    }

    boolean allowSurfacing(Entity mover, Coords from, int fromElevation, MoveStep step) {
        if (LargeNavalVesselRules.applies(mover)) {
            return new LargeNavalVesselCollisionHandler(gameManager).entering((Tank) mover, from, fromElevation, step);
        }
        if (!from.equals(step.getPosition())) {
            return true;
        }
        MobileStructure target = MobileStructureMovement.surfacingObstacle(mover, step.getPosition(), fromElevation,
              step.getElevation());
        if (target == null) {
            return true;
        }
        boolean submerged = step.getElevation() + mover.height() < 0 && fullyUnderwater(target);
        int dealt = standardChargeDamage(mover, step.getDistance());
        int returned = standardChargeReturnDamage(mover, target);
        if (submerged) {
            dealt /= 2;
            returned /= 2;
        }
        while (dealt > 0) {
            int cluster = Math.min(10, dealt);
            damageHex(target, target.boardToRelative(step.getPosition()), cluster);
            dealt -= cluster;
        }
        damageUnit(mover, returned, target);
        mover.applyDamage();
        gameManager.applyBuildingDamage();
        gameManager.entityUpdate(mover.getId());
        return false;
    }

    /**
     * TO:AUE p.41 halves collision damage only when both units are fully underwater at impact. The general charge
     * helper uses the attacker's previous location exposure, which would apply that reduction a second time, or
     * retain it after surfacing. Calculate the TW p.148 amount before the caller applies the impact-depth modifier.
     */
    static int standardChargeDamage(Entity attacker, int hexesMoved) {
        return (int) Math.ceil((attacker.getWeight() / 10.0) * Math.max(0, hexesMoved - 1));
    }

    static int standardChargeReturnDamage(Entity attacker, IBuilding target) {
        int damage = (int) Math.ceil(attacker.getWeight() / 10.0);
        if (attacker.hasWorkingBulldozer()) {
            damage /= 2;
        }
        return target.usesCapitalScale() ? damage * 10 : damage;
    }

    void damageHex(MobileStructure unit, CubeCoords relative, int standardDamage) {
        if (relative == null || !unit.getInternalBuilding().hasCFIn(relative)) {
            return;
        }
        int damage = unit.isWaterStructure() ? standardDamage / 2 : standardDamage;
        Coords coords = unit.relativeToBoard(relative);
        addReport(gameManager.damageBuilding(unit, damage, "mobile structure collision", coords, 0, null, false));
    }

    void damageUnit(Entity target, int damage, Entity attacker) {
        if (target instanceof BattleArmor armor) {
            for (int trooper = 1; trooper < armor.locations(); trooper++) {
                addReport(gameManager.damageEntity(target, new HitData(trooper), damage));
            }
        } else if (target instanceof Infantry) {
            addReport(gameManager.damageEntity(target, new HitData(ConvInfantry.LOC_INFANTRY), damage));
        } else {
            while (damage > 0) {
                HitData hit = target.rollHitLocation(ToHitData.HIT_NORMAL, target.sideTable(attacker.getPosition()));
                hit.setGeneralDamageType(HitData.DAMAGE_PHYSICAL);
                addReport(gameManager.damageEntity(target, hit, Math.min(10, damage)));
                damage -= Math.min(10, damage);
            }
        }
    }

    private static int dropshipReturnDamage(Entity dropship) {
        return dropship.getWeight() < 2500 ? 25 : dropship.getWeight() <= 10000 ? 50 : 75;
    }

    private void sweepUnderneath(Tank tank) {
        for (int location = 0; location < tank.locations(); location++) {
            if (tank.getOInternal(location) > 0) {
                addReport(gameManager.damageEntity(tank, new HitData(location),
                      tank instanceof VTOL && location == VTOL.LOC_ROTOR ? 1 : 5));
            }
        }
        addReport(gameManager.vehicleMotiveDamage(tank, 0));
        gameManager.entityUpdate(tank.getId());
    }

    /** Shallow-water entry remains legal, but immediately grounds and damages every offending hex. */
    void ground(MobileStructure unit) {
        if (!unit.isWaterStructure() || unit.isGrounded()) {
            return;
        }
        boolean grounded = false;
        for (Coords coords : List.copyOf(unit.getCoordsList())) {
            var hex = getGame().getBoard(unit).getHex(coords);
            if (hex != null && hex.depth() < -unit.getStructureBaseElevation() - unit.getElevation()) {
                // Grounding is a specified 50 damage, not the water collision reduction.
                addReport(gameManager.damageBuilding(unit, 50, "running aground", coords, 0, null, false));
                grounded = true;
            }
        }
        if (grounded) {
            unit.setGrounded(true);
            gameManager.applyBuildingDamage();
            gameManager.entityUpdate(unit.getId());
        }
    }

    private static boolean isSmallVessel(Entity target) {
        return target.getElevation() == 0 && (target.getMovementMode().isNaval() || target.getMovementMode().isSubmarine())
              && (!(target instanceof LargeSupportTank) || target.getWeight() <= 500);
    }

    private void strand(Entity vessel, MobileStructure mobile, CubeCoords relative, MoveStep step) {
        vessel.setElevation(mobile.getElevation() + mobile.getStructureBaseElevation()
              + mobile.getInternalBuilding().getHeight(relative));
        vessel.setStuck(true);
        mobile.getNavalState().getStrandedVessels().put(vessel.getId(), relative);
        for (var weapon : mobile.getWeaponList()) {
            if (mobile.relativeToBoard(relative).equals(mobile.getLocationCoords(weapon.getLocation()))
                  && mobile.isTurretMounted(weapon)) {
                weapon.setDestroyed(true);
            }
        }
        gameManager.entityUpdate(vessel.getId());
    }
}
