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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import megamek.common.bays.Bay;
import megamek.common.bays.LiquidCargoBay;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.units.BuildingDesign.Position;
import megamek.common.units.BuildingDesign.Space;
import megamek.common.weapons.attacks.InfantryAttack;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

/** Shared static-building construction calculations, for the verifier, editor, and record sheet. */
public final class BuildingConstruction {
    private BuildingConstruction() { }

    public static boolean usesHexsides(AbstractBuildingEntity building) {
        return building.getBldgClass() == IBuilding.WALL || building.getBldgClass() == IBuilding.FENCE;
    }

    public static boolean hasNoInterior(AbstractBuildingEntity building) {
        return building.getBldgClass() == IBuilding.TENT || building.getBldgClass() == IBuilding.FENCE
              || building.getBldgClass() == IBuilding.BRIDGE;
    }

    public static int segmentsInHex(AbstractBuildingEntity building, CubeCoords hex) {
        return usesHexsides(building) ? Integer.bitCount(building.getDesign().wallSides(hex)) : 1;
    }

    public static List<Integer> mapLevels(AbstractBuildingEntity building) {
        return building.getBldgClass() == IBuilding.BRIDGE
              ? building.getInternalBuilding().getOriginalCoordsList().stream().map(building.getDesign()::bridgeDeck)
                    .distinct().sorted(java.util.Comparator.reverseOrder()).toList()
              : java.util.stream.IntStream.range(0, building.getInternalBuilding().getBuildingHeight()).boxed()
                    .sorted(java.util.Comparator.reverseOrder()).toList();
    }

    /** Physical elevation of native floor zero, also used for sheet numbering. Bridge decks retain their own datum. */
    public static int baseLevel(AbstractBuildingEntity building) {
        if (building.getBldgClass() == IBuilding.BRIDGE) {
            return 0;
        }
        var design = building.getDesign();
        if (design.getBaseLevel() != null) {
            return design.getBaseLevel();
        }
        if (building instanceof MobileStructure mobile) {
            return mobile.getStructureBaseElevation();
        }
        return design.getSite() == BuildingDesign.Site.SURFACE ? 0
              : -building.getInternalBuilding().getBuildingHeight() - design.getDepth();
    }

    public static boolean occupiesMapLevel(AbstractBuildingEntity building, CubeCoords hex, int level) {
        return building.getBldgClass() == IBuilding.BRIDGE ? building.getDesign().bridgeDeck(hex) == level
              : level >= 0 && level < building.getInternalBuilding().getHeight(hex);
    }

    /** Heavy-metal structure interferes with lines passing through its hexes or adjacent hexes (TO:AR p.135). */
    public static boolean hasHeavyMetalInterference(Entity source, megamek.common.board.Coords from,
          megamek.common.board.Coords to) {
        if (source.getGame() == null || from == null || to == null) {
            return false;
        }
        var board = source.getGame().getBoard(source.getBoardId());
        if (board == null) {
            return false;
        }
        var path = megamek.common.board.Coords.intervening(from, to);
        for (IBuilding candidate : board.getBuildingsVector()) {
            if (candidate instanceof AbstractBuildingEntity building && building != source && !building.isDestroyed()
                  && building.getDesign().hasHeavyMetal()
                  && building.getCoordsList().stream().anyMatch(hex -> path.stream().anyMatch(point -> point.distance(hex) <= 1))) {
                return true;
            }
        }
        return false;
    }

    public record BridgeSpan(CubeCoords start, CubeCoords end, Map<CubeCoords, Integer> distances, int length) {
        public int level(CubeCoords hex, int startLevel, int endLevel) {
            return length == 0 ? startLevel : (int) Math.round(startLevel
                  + (endLevel - startLevel) * distances.get(hex) / (double) length);
        }
    }

    /** Endpoints are the furthest connected hexes. Intermediate deck heights follow TO:AR p. 115's unrounded slope. */
    public static BridgeSpan bridgeSpan(List<CubeCoords> hexes) {
        var occupied = new HashSet<>(hexes);
        BridgeSpan result = null;
        for (var start : hexes) {
            Map<CubeCoords, Integer> distances = new HashMap<>();
            var pending = new ArrayList<CubeCoords>();
            distances.put(start, 0);
            pending.add(start);
            for (int index = 0; index < pending.size(); index++) {
                var hex = pending.get(index);
                for (var neighbor : hex.neighbors()) {
                    if (occupied.contains(neighbor) && !distances.containsKey(neighbor)) {
                        distances.put(neighbor, distances.get(hex) + 1);
                        pending.add(neighbor);
                    }
                }
            }
            if (distances.size() != hexes.size()) {
                return null;
            }
            for (var end : hexes) {
                int distance = distances.get(end);
                if (result == null || distance > result.length()) {
                    result = new BridgeSpan(start, end, distances, distance);
                }
            }
        }
        return result;
    }

    public static void setBridgeSlope(AbstractBuildingEntity building, int startLevel, int endLevel) {
        var span = bridgeSpan(building.getInternalBuilding().getOriginalCoordsList());
        if (span == null || startLevel < 0 || endLevel < 0 || Math.abs(startLevel - endLevel) > span.length()) {
            throw new IllegalArgumentException("A bridge needs connected hexes and no more than one level of rise per hex.");
        }
        building.getDesign().getBridgeDecks().clear();
        span.distances().keySet().forEach(hex -> building.getDesign().getBridgeDecks().put(hex, span.level(hex, startLevel, endLevel)));
    }

    public static double capacityInHex(AbstractBuildingEntity building, CubeCoords hex) {
        if (hasNoInterior(building)) {
            return 0;
        }
        int height = building instanceof MobileStructure ? building.getInternalBuilding().getBuildingHeight()
              : building.getInternalBuilding().getHeight(hex);
        double capacity = building.getInternalBuilding().getPhaseCF(hex) * building.getConstructionCFScale() * height;
        if (building.getBldgClass() == IBuilding.HANGAR) {
            capacity = Math.min(capacity * 3, (building instanceof MobileStructure ? 300 : 600) * Math.ceil(height / 4.0));
        }
        if (building.getDesign().isOpenSpace()) {
            capacity = Math.min(600, capacity);
        }
        capacity = building.getDesign().hasHeavyMetal() ? Math.floor(capacity * .75) : capacity;
        return capacity * segmentsInHex(building, hex);
    }

    public static Position position(AbstractBuildingEntity building, int location) {
        if (location < 0 || location >= building.locations()) {
            return null;
        }
        int height = building.getInternalBuilding().getBuildingHeight();
        return new Position(building.getInternalBuilding().getOriginalCoordsList().get(location / height), location % height);
    }

    public static int location(AbstractBuildingEntity building, Position position) {
        int index = building.getInternalBuilding().getOriginalCoordsList().indexOf(position.hex());
        int height = building.getInternalBuilding().getBuildingHeight();
        return index < 0 || position.level() < 0
              || position.level() >= building.getInternalBuilding().getHeight(position.hex()) ? Entity.LOC_NONE
              : index * height + position.level();
    }

    public static List<Position> equipmentPositions(AbstractBuildingEntity building, Mounted<?> mount) {
        Position anchor = position(building, mount.getLocation());
        return anchor == null ? List.of() : building.getDesign().getEquipmentSpace().getOrDefault(mount, List.of(anchor));
    }

    /** Spreadable equipment is one item. Its mass, not its count or output, is shared among its occupied hexes. */
    public static double equipmentWeightInHex(AbstractBuildingEntity building, Mounted<?> mount, CubeCoords hex) {
        List<Position> positions = equipmentPositions(building, mount);
        double tons = positions.isEmpty() ? 0
              : mount.getTonnage() * positions.stream().filter(p -> p.hex().equals(hex)).count() / positions.size();
        return building instanceof MobileStructure && positions.size() > 1 ? Math.ceil(tons * 2) / 2 : tons;
    }

    public static List<Space> baySpaces(AbstractBuildingEntity building, Bay bay) {
        if (building.getDesign().getBaySpace().containsKey(bay)) {
            return building.getDesign().getBaySpace().get(bay);
        }
        // Older BLKs have no bay placement. Their accommodation is distributed evenly, which is legal (TO:AR p. 129).
        var locations = java.util.stream.IntStream.range(0, building.locations())
              .filter(loc -> location(building, position(building, loc)) >= 0)
              .filter(loc -> !building.getDesign().isOpenSpace() || position(building, loc).level() == 0).boxed().toList();
        return locations.stream()
              .map(loc -> new Space(position(building, loc), bay.getWeight() / locations.size())).toList();
    }

    public static double bayWeightInHex(AbstractBuildingEntity building, CubeCoords hex) {
        return building.getTransportBays().stream().flatMap(bay -> baySpaces(building, bay).stream())
              .filter(space -> space.position().hex().equals(hex)).mapToDouble(Space::tons).sum()
              + building.getTroopCarryingSpace() / building.getInternalBuilding().getOriginalCoordsList().size();
    }

    /** TO:AUE p. 83: non-missile capital weapons require an additional ten percent for controls/stabilizers. */
    public static double capitalControls(AbstractBuildingEntity building, CubeCoords hex) {
        return building.getEquipmentInHex(hex).stream().filter(m -> isNonMissileCapital(m.getType()))
              .mapToDouble(m -> equipmentWeightInHex(building, m, hex) * .1).sum();
    }

    public static boolean isCapital(EquipmentType type) {
        return type instanceof WeaponType weapon && (weapon.isCapital() || weapon.isSubCapital());
    }

    public static boolean isNonMissileCapital(EquipmentType type) {
        return isCapital(type) && !type.hasFlag(WeaponType.F_MISSILE);
    }

    public static boolean isHeavyWeapon(EquipmentType type) {
        return type instanceof WeaponType && !(type instanceof InfantryWeapon);
    }

    public static boolean requiresGunner(EquipmentType type) {
        return type instanceof WeaponType && !type.hasFlag(WeaponType.F_AMS) && !type.hasFlag(WeaponType.F_AMS_BAY)
              && !type.hasFlag(WeaponType.F_B_POD) && !type.hasFlag(MiscType.F_AP_POD);
    }

    public static boolean canAutomate(EquipmentType type) {
        return isHeavyWeapon(type) && !isCapital(type) && requiresGunner(type) && !type.hasFlag(WeaponType.F_ARTILLERY);
    }

    public static boolean canSpread(EquipmentType type) {
        return type instanceof PowerGeneratorType || isCapital(type)
              || (type instanceof BuildingEquipmentType facility && facility.getFacility().isSpreadable());
    }

    public static boolean occupiesRoof(Mounted<?> mount) {
        return mount.isSponsonTurretMounted()
              || (mount.getType() instanceof BuildingEquipmentType facility && facility.getFacility().isRoof());
    }

    /** TechManual DropShip/support-vehicle items and the native advanced-building power generators. */
    public static boolean canMount(EquipmentType type) {
        if (type instanceof AmmoType) {
            return !(type instanceof BombType) && !type.hasFlag(AmmoType.F_BATTLEARMOR) && !type.hasFlag(AmmoType.F_PROTOMEK);
        }
        if (type instanceof WeaponType weapon) {
            return !(weapon instanceof InfantryAttack) && !(weapon instanceof BayWeapon)
                  && (weapon instanceof InfantryWeapon || weapon.hasFlag(WeaponType.F_TANK_WEAPON)
                        || weapon.hasFlag(WeaponType.F_MEK_WEAPON) || isCapital(weapon));
        }
        return type instanceof PowerGeneratorType || type instanceof BuildingEquipmentType || (type instanceof MiscType
              && (type.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT) || type.hasFlag(MiscType.F_DS_EQUIPMENT)
                    || type.hasFlag(MiscType.F_HEAT_SINK) || type.hasFlag(MiscType.F_DOUBLE_HEAT_SINK))
              && !type.hasFlag(MiscType.F_TURRET) && !type.hasFlag(MiscType.F_CHASSIS_MODIFICATION)
              && !type.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING) && !type.hasFlag(MiscType.F_BASIC_FIRE_CONTROL)
              && !type.hasFlag(MiscType.F_ADVANCED_FIRE_CONTROL));
    }

    public static int energyHeat(AbstractBuildingEntity building) {
        return building.getWeaponList().stream().filter(m -> m.getType().hasFlag(WeaponType.F_ENERGY)
              && !(m.getType() instanceof InfantryWeapon)).mapToInt(m -> m.getType().getHeat()).sum();
    }

    public static int heatDissipation(AbstractBuildingEntity building) {
        return building.getMisc().stream().mapToInt(m -> m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK) ? 2
              : m.getType().hasFlag(MiscType.F_HEAT_SINK) ? 1 : 0).sum();
    }

    public static double generatorTons(AbstractBuildingEntity building, StructureEngine engine) {
        return Math.ceil(building.getBaseGeneratorWeight() * engine.getBuildingWeightMultiplier());
    }

    /** TO:AR p. 132: a liquid-storage-only building does not need power. Reuse native liquid cargo bays. */
    public static boolean isLiquidStorageOnly(AbstractBuildingEntity building) {
        return building.getEquipment().isEmpty() && building.getTroopCarryingSpace() == 0
              && building.getDesign().getElevators().isEmpty() && !building.getTransportBays().isEmpty()
              && building.getTransportBays().stream().allMatch(bay -> bay instanceof LiquidCargoBay);
    }

    /** Daily fuel for this building; storage may be off site (TO:AR p. 133). Hours is a planning input, not saved data. */
    public static double dailyFuel(AbstractBuildingEntity building, StructureEngine engine, int combatHours) {
        if (hasNoInterior(building) || usesHexsides(building) || isLiquidStorageOnly(building)) {
            return 0;
        }
        long heavy = building.getWeaponList().stream().filter(m -> isHeavyWeapon(m.getType()) && !isCapital(m.getType())).count();
        long capital = building.getWeaponList().stream().filter(m -> isCapital(m.getType())).count();
        return (Math.ceil(building.getInternalBuilding().getOriginalCoordsList().size() / 5.0)
              + Math.clamp(combatHours, 0, 24) * (heavy + 10 * capital)) * engine.getBuildingDailyFuelWeight();
    }

    public static double installedWeightInHex(AbstractBuildingEntity building, CubeCoords hex) {
        double equipment = building.getEquipmentInHex(hex).stream().mapToDouble(m -> equipmentWeightInHex(building, m, hex)).sum()
              + building.getPintleWeight(hex);
        // TO:AUE pp. 71, 83: combine the small items (including pintles) before rounding their final total.
        if (building instanceof MobileStructure) {
            equipment = Math.ceil(equipment * 2 - 1e-9) / 2;
        }
        return equipment
              + (building instanceof MobileStructure mobile ? mobile.systemWeightInHex(hex) : 0)
              + building.armorWeightInHex(hex)
              + building.getPowerAmplifierWeight(hex) + building.getTurretWeight(hex)
              + capitalControls(building, hex) + bayWeightInHex(building, hex)
              + building.getDesign().getElevators().stream().filter(lift -> lift.hex().equals(hex))
                    .mapToDouble(BuildingDesign.Elevator::weight).sum();
    }

    public static double structureMultiplier(AbstractBuildingEntity building) {
        var design = building.getDesign();
        return (building.hasEnvironmentalSealing() ? 1.5 : 1) * (design.hasHeavyMetal() ? 1.25 : 1)
              * (design.getCeiling() == BuildingDesign.Ceiling.STANDARD ? 1 : 1.1)
              * (design.getSite() == BuildingDesign.Site.SURFACE ? 1 : 5) * (design.isTunnel() ? 1.875 : 1)
              * (design.isOpenSpace() ? 2.5 : 1);
    }

    public static double additionalCost(AbstractBuildingEntity building) {
        return building.getDesign().getDoors().stream().mapToInt(BuildingDesign.Door::height).sum() * 10000.0
              + building.getDesign().getElevators().stream().mapToDouble(BuildingDesign.Elevator::weight).sum() * 15000
              + building.getDesign().getAutomatedWeapons().stream().mapToDouble(Mounted::getTonnage).sum() * 1000
              + building.getEquipment().stream().filter(m -> m.getType() instanceof BuildingEquipmentType facility
                    && facility.getFacility() == BuildingEquipmentType.Facility.UNSPECIFIED)
                    .flatMap(m -> equipmentPositions(building, m).stream()).map(Position::hex).distinct().count()
                    * building.getOInternal(0) * 5000.0;
    }
}
