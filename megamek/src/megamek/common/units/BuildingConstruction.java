/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.common.units;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;

import megamek.common.bays.Bay;
import megamek.common.bays.LiquidCargoBay;
import megamek.common.board.CubeCoords;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.units.BuildingDesign.Position;
import megamek.common.units.BuildingDesign.Space;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.attacks.InfantryAttack;
import megamek.common.weapons.bayWeapons.BayWeapon;

/** Shared static-building construction calculations, for the verifier, editor, and record sheet. */
public final class BuildingConstruction {
    private BuildingConstruction() { }

    public static boolean usesHexsides(BuildingEntity building) {
        return building.getBldgClass() == IBuilding.WALL || building.getBldgClass() == IBuilding.FENCE;
    }

    public static boolean hasNoInterior(BuildingEntity building) {
        return building.getBldgClass() == IBuilding.TENT || building.getBldgClass() == IBuilding.FENCE
              || building.getBldgClass() == IBuilding.BRIDGE;
    }

    public static int segmentsInHex(BuildingEntity building, CubeCoords hex) {
        return usesHexsides(building) ? Integer.bitCount(building.getDesign().wallSides(hex)) : 1;
    }

    public static List<Integer> mapLevels(BuildingEntity building) {
        return building.getBldgClass() == IBuilding.BRIDGE
              ? building.getInternalBuilding().getOriginalCoordsList().stream().map(building.getDesign()::bridgeDeck)
                    .distinct().sorted(java.util.Comparator.reverseOrder()).toList()
              : java.util.stream.IntStream.range(0, building.getInternalBuilding().getBuildingHeight()).boxed()
                    .sorted(java.util.Comparator.reverseOrder()).toList();
    }

    public static boolean occupiesMapLevel(BuildingEntity building, CubeCoords hex, int level) {
        return building.getBldgClass() == IBuilding.BRIDGE ? building.getDesign().bridgeDeck(hex) == level
              : level >= 0 && level < building.getInternalBuilding().getBuildingHeight();
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

    public static void setBridgeSlope(BuildingEntity building, int startLevel, int endLevel) {
        var span = bridgeSpan(building.getInternalBuilding().getOriginalCoordsList());
        if (span == null || startLevel < 0 || endLevel < 0 || Math.abs(startLevel - endLevel) > span.length()) {
            throw new IllegalArgumentException("A bridge needs connected hexes and no more than one level of rise per hex.");
        }
        building.getDesign().getBridgeDecks().clear();
        span.distances().keySet().forEach(hex -> building.getDesign().getBridgeDecks().put(hex, span.level(hex, startLevel, endLevel)));
    }

    public static double capacityInHex(BuildingEntity building, CubeCoords hex) {
        if (hasNoInterior(building)) {
            return 0;
        }
        int height = building.getInternalBuilding().getHeight(hex);
        double capacity = building.getInternalBuilding().getPhaseCF(hex) * building.getConstructionCFScale() * height;
        if (building.getBldgClass() == IBuilding.HANGAR) {
            capacity = Math.min(capacity * 3, 600 * Math.ceil(height / 4.0));
        }
        if (building.getDesign().isOpenSpace()) {
            capacity = Math.min(600, capacity);
        }
        capacity = building.getDesign().hasHeavyMetal() ? Math.floor(capacity * .75) : capacity;
        return capacity * segmentsInHex(building, hex);
    }

    public static Position position(BuildingEntity building, int location) {
        if (location < 0 || location >= building.locations()) {
            return null;
        }
        int height = building.getInternalBuilding().getBuildingHeight();
        return new Position(building.getInternalBuilding().getOriginalCoordsList().get(location / height), location % height);
    }

    public static int location(BuildingEntity building, Position position) {
        int index = building.getInternalBuilding().getOriginalCoordsList().indexOf(position.hex());
        int height = building.getInternalBuilding().getBuildingHeight();
        return index < 0 || position.level() < 0 || position.level() >= height ? Entity.LOC_NONE
              : index * height + position.level();
    }

    public static List<Position> equipmentPositions(BuildingEntity building, Mounted<?> mount) {
        Position anchor = position(building, mount.getLocation());
        return anchor == null ? List.of() : building.getDesign().getEquipmentSpace().getOrDefault(mount, List.of(anchor));
    }

    /** Spreadable equipment is one item. Its mass, not its count or output, is shared among its occupied hexes. */
    public static double equipmentWeightInHex(BuildingEntity building, Mounted<?> mount, CubeCoords hex) {
        List<Position> positions = equipmentPositions(building, mount);
        return positions.isEmpty() ? 0 : mount.getTonnage() * positions.stream().filter(p -> p.hex().equals(hex)).count()
              / positions.size();
    }

    public static List<Space> baySpaces(BuildingEntity building, Bay bay) {
        if (building.getDesign().getBaySpace().containsKey(bay)) {
            return building.getDesign().getBaySpace().get(bay);
        }
        // Older BLKs have no bay placement. Their accommodation is distributed evenly, which is legal (TO:AR p. 129).
        var locations = java.util.stream.IntStream.range(0, building.locations())
              .filter(loc -> !building.getDesign().isOpenSpace() || position(building, loc).level() == 0).boxed().toList();
        return locations.stream()
              .map(loc -> new Space(position(building, loc), bay.getWeight() / locations.size())).toList();
    }

    public static double bayWeightInHex(BuildingEntity building, CubeCoords hex) {
        return building.getTransportBays().stream().flatMap(bay -> baySpaces(building, bay).stream())
              .filter(space -> space.position().hex().equals(hex)).mapToDouble(Space::tons).sum()
              + building.getTroopCarryingSpace() / building.getInternalBuilding().getOriginalCoordsList().size();
    }

    /** TO:AUE p. 83: non-missile capital weapons require an additional ten percent for controls/stabilizers. */
    public static double capitalControls(BuildingEntity building, CubeCoords hex) {
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

    public record CrewRequirements(int crew, int gunners, int officers) {
        public int total() {
            return crew + gunners + officers;
        }
    }

    /** TO:AR p. 130. Residential quarters are optional; they do not determine the operating crew. */
    public static CrewRequirements crew(BuildingEntity building) {
        int crew = 0;
        int gunners = 0;
        for (Mounted<?> mount : building.getEquipment()) {
            if (mount.isOneShotAmmo() || mount.isWeaponGroup()) {
                continue;
            }
            EquipmentType type = mount.getType();
            if (type.hasFlag(MiscType.F_COMMUNICATIONS)) {
                crew += (int) Math.ceil(mount.getTonnage());
            }
            if (type.hasFlag(MiscType.F_FIELD_KITCHEN)) {
                crew += 3;
            }
            if (type.hasFlag(MiscType.F_MASH)) {
                crew += 5 * (int) mount.getSize();
            }
            if (type.hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                crew += 5;
            }
            if (type instanceof BuildingEquipmentType facility) {
                crew += switch (facility.getFacility()) {
                    case FLIGHT_DECK -> 20;
                    case HELIPAD -> 5;
                    case LANDING_DECK -> 3 * (int) mount.getSize();
                    default -> 0;
                };
            }
            if (requiresGunner(type) && !building.getDesign().getAutomatedWeapons().contains(mount)) {
                gunners += isCapital(type) ? 7 : type instanceof InfantryWeapon ? 1 : (int) Math.ceil(mount.getTonnage() / 5);
            }
        }
        boolean officers = building.getBldgClass() == IBuilding.FORTRESS || building.getBldgClass() == IBuilding.GUN_EMPLACEMENT
              || building.getBldgClass() == IBuilding.CASTLE_BRIAN
              || building.getDesign().hasCivilianOfficers();
        return new CrewRequirements(crew, gunners, officers ? Math.max(1, (int) Math.ceil((crew + gunners) / 10.0)) : 0);
    }

    public static int energyHeat(BuildingEntity building) {
        return building.getWeaponList().stream().filter(m -> m.getType().hasFlag(WeaponType.F_ENERGY)
              && !(m.getType() instanceof InfantryWeapon)).mapToInt(m -> m.getType().getHeat()).sum();
    }

    public static int heatDissipation(BuildingEntity building) {
        return building.getMisc().stream().mapToInt(m -> m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK) ? 2
              : m.getType().hasFlag(MiscType.F_HEAT_SINK) ? 1 : 0).sum();
    }

    public static double generatorTons(BuildingEntity building, StructureEngine engine) {
        return Math.ceil(building.getBaseGeneratorWeight() * engine.getBuildingWeightMultiplier());
    }

    /** TO:AR p. 132: a liquid-storage-only building does not need power. Reuse native liquid cargo bays. */
    public static boolean isLiquidStorageOnly(BuildingEntity building) {
        return building.getEquipment().isEmpty() && building.getTroopCarryingSpace() == 0
              && building.getDesign().getElevators().isEmpty() && !building.getTransportBays().isEmpty()
              && building.getTransportBays().stream().allMatch(bay -> bay instanceof LiquidCargoBay);
    }

    /** Daily fuel for this building; storage may be off site (TO:AR p. 133). Hours is a planning input, not saved data. */
    public static double dailyFuel(BuildingEntity building, StructureEngine engine, int combatHours) {
        if (hasNoInterior(building) || usesHexsides(building) || isLiquidStorageOnly(building)) {
            return 0;
        }
        long heavy = building.getWeaponList().stream().filter(m -> isHeavyWeapon(m.getType()) && !isCapital(m.getType())).count();
        long capital = building.getWeaponList().stream().filter(m -> isCapital(m.getType())).count();
        return (Math.ceil(building.getInternalBuilding().getOriginalCoordsList().size() / 5.0)
              + Math.clamp(combatHours, 0, 24) * (heavy + 10 * capital)) * engine.getBuildingDailyFuelWeight();
    }

    public static double installedWeightInHex(BuildingEntity building, CubeCoords hex) {
        return building.getEquipmentInHex(hex).stream().mapToDouble(m -> equipmentWeightInHex(building, m, hex)).sum()
              + building.armorWeightInHex(hex)
              + building.getPowerAmplifierWeight(hex) + building.getTurretWeight(hex) + building.getPintleWeight(hex)
              + capitalControls(building, hex) + bayWeightInHex(building, hex)
              + building.getDesign().getElevators().stream().filter(lift -> lift.hex().equals(hex))
                    .mapToDouble(BuildingDesign.Elevator::weight).sum();
    }

    public static double structureMultiplier(BuildingEntity building) {
        var design = building.getDesign();
        return (building.hasEnvironmentalSealing() ? 1.5 : 1) * (design.hasHeavyMetal() ? 1.25 : 1)
              * (design.getCeiling() == BuildingDesign.Ceiling.STANDARD ? 1 : 1.1)
              * (design.getSite() == BuildingDesign.Site.SURFACE ? 1 : 5) * (design.isTunnel() ? 1.875 : 1)
              * (design.isOpenSpace() ? 2.5 : 1);
    }

    public static double additionalCost(BuildingEntity building) {
        return building.getDesign().getDoors().stream().mapToInt(BuildingDesign.Door::height).sum() * 10000.0
              + building.getDesign().getElevators().stream().mapToDouble(BuildingDesign.Elevator::weight).sum() * 15000
              + building.getDesign().getAutomatedWeapons().stream().mapToDouble(Mounted::getTonnage).sum() * 1000
              + building.getEquipment().stream().filter(m -> m.getType() instanceof BuildingEquipmentType facility
                    && facility.getFacility() == BuildingEquipmentType.Facility.UNSPECIFIED)
                    .flatMap(m -> equipmentPositions(building, m).stream()).map(Position::hex).distinct().count()
                    * building.getOInternal(0) * 5000.0;
    }
}
