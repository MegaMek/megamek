/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.verifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import megamek.common.bays.Bay;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingConstruction;
import megamek.common.units.BuildingDesign;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.weapons.infantry.InfantryWeapon;

public class TestBuilding extends TestEntity {
    private final AbstractBuildingEntity building;

    public TestBuilding(AbstractBuildingEntity building, TestEntityOption option, String fs) {
        super(option, building.getEngine(), getStructure(building));
        this.building = building;
        fileString = fs;
    }

    private static Structure getStructure(AbstractBuildingEntity building) {
        int type = building.getStructureType();
        return new Structure(type, false, building.getMovementMode());
    }

    @Override
    public Entity getEntity() {
        return building;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMek() {
        return false;
    }

    @Override
    public boolean isAero() {
        return false;
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }

    @Override
    public boolean isProtoMek() {
        return false;
    }

    @Override
    public double getWeightControls() {
        return building instanceof BuildingEntity entity ? building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(hex -> BuildingConstruction.capitalControls(entity, hex)).sum() : 0;
    }

    @Override
    public double getWeightMisc() {
        return building instanceof BuildingEntity entity ? entity.getDesign().getElevators().stream()
              .mapToDouble(BuildingDesign.Elevator::weight).sum() : 0;
    }

    @Override
    public double getWeightHeatSinks() {
        return building.getMisc().stream().filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
              || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)).mapToDouble(Mounted::getTonnage).sum();
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return building.getMisc().stream().anyMatch(m -> m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK));
    }

    @Override
    public int getCountHeatSinks() {
        return (int) building.getMisc().stream().filter(m -> m.getType().hasFlag(MiscType.F_HEAT_SINK)
              || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)).count();
    }

    @Override
    public String printWeightMisc() {
        return "";
    }

    @Override
    public String printWeightControls() {
        return "";
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        if (!(building instanceof BuildingEntity)) {
            // Mobile Structures use a different construction table; their verifier remains to be implemented.
            return true;
        }
        List<String> issues = constructionIssues();
        issues.forEach(issue -> buff.append(issue).append('\n'));
        boolean correct = issues.isEmpty();
        correct &= !hasIllegalTechLevels(buff, ammoTechLvl);
        if (showIncorrectIntroYear()) {
            correct &= !hasIncorrectIntroYear(buff);
        }
        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer result = new StringBuffer(getName()).append('\n');
        result.append("Installed weight: ").append(calculateWeight()).append(" / ")
              .append(building.getWeight()).append(" tons\n");
        correctEntity(result, building.getTechLevel());
        return result;
    }

    @Override
    public String getName() {
        return building.getDisplayName();
    }

    @Override
    public double getWeightPowerAmp() {
        return building instanceof BuildingEntity entity ? building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(entity::getPowerAmplifierWeight).sum() : 0;
    }

    /** TO:AR p. 113. These limits concern static buildings; Mobile Structures have their own table. */
    public record Limits(int minimumCF, int maximumCF, int hexes, int levels) {
    }

    public static Limits limits(BuildingType type, int classification) {
        if (classification == IBuilding.BRIDGE && type == BuildingType.RAIL) {
            return new Limits(151, 650, Integer.MAX_VALUE, 1);
        }
        if (classification == IBuilding.TENT) {
            return type == BuildingType.LIGHT ? new Limits(1, 2, 1, 1) : null;
        }
        if (classification == IBuilding.FENCE) {
            return type == BuildingType.LIGHT ? new Limits(1, 1, Integer.MAX_VALUE, 3) : null;
        }
        int index = type.getTypeValue() - 1;
        if (index < 0 || index > 3) {
            return null;
        }
        if (classification == IBuilding.HANGAR) {
            return new Limits(new int[] { 1, 9, 17, 46 }[index], new int[] { 8, 16, 45, 75 }[index],
                  new int[] { 10, 14, 18, 20 }[index], new int[] { 7, 10, 13, 14 }[index]);
        }
        int hexes;
        int levels;
        switch (classification) {
            case IBuilding.STANDARD -> {
                if (type == BuildingType.HARDENED) {
                    return null;
                }
                hexes = new int[] { 6, 8, 10 }[index];
                levels = new int[] { 5, 8, 10 }[index];
            }
            case IBuilding.FORTRESS -> {
                if (type == BuildingType.LIGHT) {
                    return null;
                }
                hexes = new int[] { 0, 12, 15, 20 }[index];
                levels = new int[] { 0, 15, 20, 30 }[index];
            }
            case IBuilding.GUN_EMPLACEMENT -> {
                hexes = 1;
                levels = 1;
            }
            case IBuilding.CASTLE_BRIAN -> {
                // The construction rules refer to p. 113 (35/70 hexes); p. 212's recap conflicts (20/30).
                // The supplied TO:AR errata v7 does not change either table.
                return type == BuildingType.HEAVY ? new Limits(35, 90, 35, 10)
                      : type == BuildingType.HARDENED ? new Limits(91, 150, 70, 15) : null;
            }
            case IBuilding.WALL -> {
                hexes = Integer.MAX_VALUE;
                levels = new int[] { 4, 6, 8, 10 }[index];
            }
            case IBuilding.BRIDGE -> {
                hexes = Integer.MAX_VALUE;
                levels = 1;
            }
            default -> {
                return null;
            }
        }
        return new Limits(type.getMinimumCF(), type.getMaximumCF(), hexes, levels);
    }

    @Override
    public double getWeightArmor() {
        return building.getArmorWeight();
    }

    @Override
    public double getWeightCarryingSpace() {
        return building.getTroopCarryingSpace() + building.getTransportBays().stream().mapToDouble(Bay::getWeight).sum();
    }

    @Override
    public double calculateWeightExact() {
        double weight = building.getEquipment().stream().filter(m -> !m.isOneShotAmmo() && !m.isWeaponGroup())
              .mapToDouble(Mounted::getTonnage).sum();
        double turrets = building instanceof BuildingEntity entity ? building.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(hex -> entity.getTurretWeight(hex) + entity.getPintleWeight(hex)).sum() : 0;
        return weight + getWeightArmor() + getWeightPowerAmp() + getWeightCarryingSpace() + turrets
              + getWeightControls() + getWeightMisc();
    }

    @Override
    public double calculateWeight() {
        return calculateWeightExact();
    }

    /** Validate the construction represented by the native static-building model. */
    public List<String> constructionIssues() {
        List<String> issues = new ArrayList<>();
        if (!(building instanceof BuildingEntity entity)) {
            return issues;
        }
        var structure = building.getInternalBuilding();
        List<CubeCoords> hexes = structure.getOriginalCoordsList();
        int height = structure.getBuildingHeight();
        int classification = building.getBldgClass();
        Limits limits = limits(building.getBuildingType(), classification);
        if (limits == null) {
            issues.add("This building type/classification is not supported by the static-building construction table.");
        } else {
            int cf = building.getOInternal(0);
            if (cf < limits.minimumCF() || cf > limits.maximumCF()) {
                issues.add("CF must be between " + limits.minimumCF() + " and " + limits.maximumCF() + ".");
            }
            boolean reduceSize = entity.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND
                  && classification != IBuilding.CASTLE_BRIAN;
            int maximumHexes = reduceSize
                  ? (limits.hexes() + 1) / 2 : limits.hexes();
            int maximumLevels = reduceSize
                  ? (limits.levels() + 1) / 2 : limits.levels();
            if (hexes.size() > maximumHexes || height > maximumLevels) {
                issues.add("Maximum size is " + maximumHexes + " hexes and " + maximumLevels + " levels.");
            }
        }
        if (calculateWeight() > building.getWeight() + .00001) {
            issues.add("Installed components exceed the building's carrying capacity.");
        }
        if (!entity.hasPower()) {
            issues.add("The installed generators do not meet the building's power requirements.");
        }
        if (BuildingConstruction.hasNoInterior(entity) && (!entity.getEquipment().isEmpty()
              || !entity.getTransportBays().isEmpty() || entity.getTroopCarryingSpace() > 0
              || !entity.getDesign().getElevators().isEmpty())) {
            issues.add("Tents, fences and bridges cannot install equipment, armor, elevators or bays.");
        }
        for (int index = 0; index < hexes.size(); index++) {
            int cf = building.getOInternal(index * height);
            int armor = building.getOArmor(index * height);
            int maximumArmor = classification == IBuilding.CASTLE_BRIAN ? cf * 2
                  : classification == IBuilding.FORTRESS || classification == IBuilding.GUN_EMPLACEMENT
                        || classification == IBuilding.WALL ? cf : 0;
            if (armor < 0 || armor > maximumArmor) {
                issues.add("Hex " + (index + 1) + ": armor must be between 0 and " + maximumArmor + ".");
            }
            CubeCoords hex = hexes.get(index);
            List<Mounted<?>> equipment = entity.getEquipmentInHex(hex);
            double installed = BuildingConstruction.installedWeightInHex(entity, hex);
            if (installed > BuildingConstruction.capacityInHex(entity, hex) + .00001) {
                issues.add("Hex " + (index + 1) + ": components exceed this hex's carrying capacity.");
            }
            double heavyWeapons = equipment.stream().filter(m -> m.getType() instanceof WeaponType
                  && !(m.getType() instanceof InfantryWeapon) && !BuildingConstruction.isCapital(m.getType()) && m.getTonnage() >= .25)
                  .mapToDouble(m -> BuildingConstruction.equipmentWeightInHex(entity, m, hex)).sum();
            double weaponLimit = classification == IBuilding.GUN_EMPLACEMENT ? Math.floor(cf / 3.0)
                  : classification == IBuilding.FORTRESS ? cf * height / 10.0
                        : classification == IBuilding.CASTLE_BRIAN ? cf * height : 0;
            if (heavyWeapons > weaponLimit + .00001) {
                issues.add("Hex " + (index + 1) + ": heavy weapons exceed the " + weaponLimit + " ton limit.");
            }
            long capital = equipment.stream().filter(m -> BuildingConstruction.isNonMissileCapital(m.getType())).count();
            if (capital > 1) {
                issues.add("Hex " + (index + 1) + ": only one non-missile capital weapon is allowed.");
            }
            boolean hasCapital = equipment.stream().anyMatch(m -> BuildingConstruction.isCapital(m.getType()));
            boolean reservedRoof = (entity.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND
                  && !entity.getDesign().hasRoofClearance())
                  || entity.getMisc().stream().anyMatch(m -> m.getType() instanceof PowerGeneratorType generator
                        && (generator.getStructureEngine() == StructureEngine.SOLAR
                              || generator.getStructureEngine() == StructureEngine.EXTERNAL_PCMT));
            if (equipment.stream().anyMatch(m -> (BuildingConstruction.occupiesRoof(m) && (reservedRoof || hasCapital))
                  || (m.isPintleTurretMounted() && hasCapital))) {
                issues.add("Hex " + (index + 1) + ": turret/pintle conflicts with capital weapons or reserved roof space.");
            }
            long decks = equipment.stream().filter(m -> m.getType() instanceof BuildingEquipmentType facility
                  && facility.getFacility().isRoof()).count();
            if (decks > 1 || (decks > 0 && equipment.stream().anyMatch(Mounted::isSponsonTurretMounted))) {
                issues.add("Hex " + (index + 1) + ": decks/helipads and turrets cannot share roof space.");
            }
            for (int level = 0; level < height; level++) {
                int location = index * height + level;
                long smallWeapons = equipment.stream().filter(m -> m.getLocation() == location
                      && m.getType() instanceof InfantryWeapon).count();
                boolean infantryWeapons = classification == IBuilding.STANDARD || classification == IBuilding.HANGAR
                      || classification == IBuilding.WALL;
                if (smallWeapons > (infantryWeapons ? 6 * BuildingConstruction.segmentsInHex(entity, hex) : 0)) {
                    issues.add("Hex " + (index + 1) + ", level " + level + ": too many infantry weapons for this classification.");
                }
            }
        }
        for (Mounted<?> mount : building.getEquipment()) {
            if (mount.isOneShotAmmo() || mount.isWeaponGroup()) {
                continue;
            }
            if (mount.getLocation() < 0 || mount.getLocation() >= building.locations()) {
                issues.add(mount.getName() + " has no valid hex/level.");
            }
            if (!BuildingConstruction.canMount(mount.getType())) {
                issues.add(mount.getName() + " is not equipment available to buildings.");
            }
            if (mount.getType() instanceof PowerGeneratorType && (!Double.isFinite(mount.getSize())
                  || mount.getSize() < 1 || mount.getSize() != Math.rint(mount.getSize()))) {
                issues.add("Power generators must be purchased in whole tons.");
            }
            if (mount.getType() instanceof PowerGeneratorType generator && generator.getStructureEngine() == StructureEngine.EXTERNAL_PCMT) {
                double source = entity.getDesign().getPcmtSources().getOrDefault(mount, 0.0);
                if (!Double.isFinite(source) || source <= 0 || hexes.size() < Math.ceil(source * 5)) {
                    issues.add("A PCMT receiver needs its transmitting PCMT's tonnage; roof area must be at least five times that tonnage, rounded up.");
                }
            }
            if (mount.getType() instanceof BuildingEquipmentType facility) {
                if (facility.getFacility().isRoof() && BuildingConstruction.equipmentPositions(entity, mount).stream()
                      .anyMatch(p -> p.level() != height - 1)) {
                    issues.add("Decks and helipads must be assigned to the highest internal level (the roof above it).");
                }
                var positions = BuildingConstruction.equipmentPositions(entity, mount);
                var covered = positions.stream().map(BuildingDesign.Position::hex).toList();
                if (facility.getFacility() == BuildingEquipmentType.Facility.FLIGHT_DECK) {
                    boolean straight = covered.size() == 3 && covered.stream().anyMatch(center -> {
                        for (int side = 0; side < 3; side++) {
                            if (covered.contains(center.toOffset().translated(side).toCube())
                                  && covered.contains(center.toOffset().translated(side + 3).toCube())) {
                                return true;
                            }
                        }
                        return false;
                    });
                    if (!straight) {
                        issues.add("A flight deck requires three consecutive roof hexes in a straight line.");
                    }
                }
                if (facility.getFacility() == BuildingEquipmentType.Facility.LANDING_DECK) {
                    int radius = covered.size() == 7 ? 1 : covered.size() == 19 ? 2 : covered.size() == 37 ? 3 : 0;
                    var anchor = BuildingConstruction.position(entity, mount.getLocation());
                    if (radius == 0 || mount.getSize() != covered.size() || anchor == null || covered.stream().anyMatch(hex ->
                          Math.max(Math.max(Math.abs(hex.q() - anchor.hex().q()), Math.abs(hex.r() - anchor.hex().r())),
                                Math.abs(hex.s() - anchor.hex().s())) > radius)) {
                        issues.add("A landing deck needs 7, 19 or 37 roof hexes filling a radius of 1, 2 or 3 around its primary hex.");
                    }
                }
            }
            if (mount.getType() instanceof WeaponType weapon) {
                if (BuildingConstruction.isCapital(weapon) && classification != IBuilding.FORTRESS
                      && classification != IBuilding.CASTLE_BRIAN) {
                    issues.add("Capital weapons require a Fortress or Castles Brian.");
                }
                if (BuildingConstruction.isNonMissileCapital(weapon) && !entity.hasFusionOrFissionPower()) {
                    issues.add("Non-missile capital weapons require fusion or fission power.");
                }
                if (BuildingConstruction.isCapital(weapon) && (mount.isSponsonTurretMounted() || mount.isPintleTurretMounted())) {
                    issues.add("Capital weapons cannot be turret or pintle mounted.");
                }
                if (mount.isPintleTurretMounted() && !(weapon instanceof InfantryWeapon)) {
                    issues.add("Pintles can mount only infantry weapons.");
                }
                if (mount.isSponsonTurretMounted() && mount.getLocation() % height != height - 1) {
                    issues.add("Turret weapons must be on the highest level.");
                }
                var position = BuildingConstruction.position(entity, mount.getLocation());
                if (!BuildingConstruction.isCapital(weapon) && !mount.isSponsonTurretMounted() && position != null
                      && (mount.getFacing() < 0 || mount.getFacing() > 5
                      || hexes.contains(position.hex().toOffset().translated(mount.getFacing()).toCube()))) {
                    issues.add(mount.getName() + ": wall/pintle weapons need a facing toward an exterior hexside.");
                }
            }
        }
        if (!entity.hasFusionOrFissionPower()) {
            int heat = BuildingConstruction.energyHeat(entity);
            int sinks = BuildingConstruction.heatDissipation(entity);
            if (heat > sinks) {
                issues.add("Energy weapons require heat sinks dissipating " + heat + " heat with this power supply.");
            }
        }
        checkDesign(entity, issues);
        return issues;
    }

    private void checkDesign(BuildingEntity entity, List<String> issues) {
        var design = entity.getDesign();
        var hexes = entity.getInternalBuilding().getOriginalCoordsList();
        int height = entity.getInternalBuilding().getBuildingHeight();
        var connected = new HashSet<CubeCoords>();
        var pending = new ArrayList<CubeCoords>();
        pending.add(hexes.getFirst());
        for (int i = 0; i < pending.size(); i++) {
            CubeCoords hex = pending.get(i);
            if (connected.add(hex)) {
                hex.neighbors().stream().filter(hexes::contains).filter(h -> !connected.contains(h)).forEach(pending::add);
            }
        }
        if (connected.size() != hexes.size() && !BuildingConstruction.usesHexsides(entity)) {
            issues.add("All hexes of one building must form a connected footprint.");
        }
        if (BuildingConstruction.usesHexsides(entity)) {
            if (hexes.stream().mapToInt(hex -> BuildingConstruction.segmentsInHex(entity, hex)).sum() == 0) {
                issues.add("A wall or fence needs at least one occupied hexside.");
            }
            for (CubeCoords hex : hexes) {
                int mask = design.wallSides(hex);
                if (mask < 0 || mask > 63) {
                    issues.add("Wall and fence sides must be valid clockwise hexsides.");
                }
                for (int side = 0; side < 6; side++) {
                    CubeCoords neighbor = hex.toOffset().translated(side).toCube();
                    if ((mask & (1 << side)) != 0 && hexes.contains(neighbor)
                          && (design.wallSides(neighbor) & (1 << ((side + 3) % 6))) != 0) {
                        issues.add("A shared wall/fence hexside must be drawn only once.");
                    }
                }
            }
        }
        if (entity.getBldgClass() == IBuilding.BRIDGE) {
            var span = BuildingConstruction.bridgeSpan(hexes);
            if (span != null && hexes.stream().anyMatch(hex -> design.bridgeDeck(hex)
                  != span.level(hex, design.bridgeDeck(span.start()), design.bridgeDeck(span.end())))) {
                issues.add("Bridge elevations must follow one steady linear slope between the endpoints, rounded per hex.");
            }
            for (CubeCoords hex : hexes) {
                if (design.bridgeDeck(hex) < 0 || hex.neighbors().stream().filter(hexes::contains)
                      .anyMatch(other -> Math.abs(design.bridgeDeck(hex) - design.bridgeDeck(other)) > 1)) {
                    issues.add("Bridge decks need nonnegative elevations and may change at most one level per hex.");
                }
            }
        }
        if (design.isOpenSpace()) {
            if (entity.getBldgClass() != IBuilding.CASTLE_BRIAN) {
                issues.add("Open-space construction requires Castles Brian.");
            }
            if (entity.getEquipment().stream().anyMatch(m -> BuildingConstruction.occupiesRoof(m)
                  || BuildingConstruction.equipmentPositions(entity, m).stream().anyMatch(p -> p.level() != 0))
                  || entity.getTransportBays().stream().flatMap(bay -> BuildingConstruction.baySpaces(entity, bay).stream())
                        .anyMatch(space -> space.tons() > 0 && space.position().level() != 0)
                  || !design.getElevators().isEmpty()) {
                issues.add("Open-space equipment and bays must be on ground level; rooftop equipment and internal elevators are not allowed.");
            }
        }
        if (design.hasHeavyMetal() && entity.getBuildingType() != BuildingType.HEAVY
              && entity.getBuildingType() != BuildingType.HARDENED) {
            issues.add("Heavy-metal superstructure requires a Heavy or Hardened building.");
        }
        if (design.getCeiling() != BuildingDesign.Ceiling.STANDARD
              && entity.getBldgClass() != IBuilding.STANDARD && entity.getBldgClass() != IBuilding.FORTRESS
              && entity.getBldgClass() != IBuilding.CASTLE_BRIAN) {
            issues.add("High/low ceilings require a Standard building, Fortress or Castles Brian.");
        }
        if (design.hasEnvironmentalSealing() && (BuildingConstruction.hasNoInterior(entity) || BuildingConstruction.usesHexsides(entity))) {
            issues.add("This classification has no enclosed interior to seal.");
        }
        if (design.getSite() != BuildingDesign.Site.SURFACE) {
            if (design.getDepth() < 1 || (entity.getBldgClass() != IBuilding.STANDARD
                  && entity.getBldgClass() != IBuilding.HANGAR && entity.getBldgClass() != IBuilding.FORTRESS
                  && entity.getBldgClass() != IBuilding.CASTLE_BRIAN)) {
                issues.add("Subsurface construction needs cover and a Standard, Hangar, Fortress or Castles Brian building.");
            }
            if (design.getSite() == BuildingDesign.Site.UNDERWATER
                  && (!entity.hasEnvironmentalSealing() || design.getDepth() + height > entity.getOInternal(0))) {
                issues.add("Underwater buildings require environmental sealing and a total depth no greater than CF.");
            }
        }
        if (design.hasRoofClearance() && design.getSite() != BuildingDesign.Site.UNDERGROUND) {
            issues.add("The cave roof-clearance exception applies to underground buildings only.");
        }
        if (design.isTunnel()) {
            if (entity.getBldgClass() != IBuilding.HANGAR || !entity.getEquipment().isEmpty()
                  || !entity.getTransportBays().isEmpty() || entity.getTroopCarryingSpace() > 0 || !design.getElevators().isEmpty()) {
                issues.add("Tunnels use Hangar construction and cannot install equipment, elevators or bays.");
            }
            if (design.getDoors().size() < 2) {
                issues.add("A tunnel needs doors for at least two connections to separate buildings/surface structures.");
            }
        }
        var doorSides = new HashSet<String>();
        for (var door : design.getDoors()) {
            if (BuildingConstruction.location(entity, door.position()) < 0 || door.height() < 1
                  || door.position().level() + door.height() > height || door.facing() < 0 || door.facing() > 5
                  || hexes.contains(door.position().hex().toOffset().translated(door.facing()).toCube())
                  || !doorSides.add(door.position().hex() + ":" + door.facing())
                  || entity.getBldgClass() == IBuilding.GUN_EMPLACEMENT || BuildingConstruction.hasNoInterior(entity)) {
                issues.add("Large doors require a unique exterior hexside and valid height in an eligible building.");
            }
        }
        for (var lift : design.getElevators()) {
            if (!hexes.contains(lift.hex()) || !Double.isFinite(lift.capacity())
                  || lift.capacity() <= 0 || lift.capacity() > entity.getOInternal(0) * entity.getConstructionCFScale()
                  || lift.exits().size() < 2 || lift.lowerLevel() < 0 || lift.upperLevel() > height
                  || lift.exits().size() != lift.upperLevel() - lift.lowerLevel() + 1) {
                issues.add("Industrial elevators need a continuous range of at least two valid levels and a capacity no greater than CF.");
            }
            if (design.getElevators().stream().anyMatch(other -> other != lift && other.hex().equals(lift.hex())
                  && other.lowerLevel() <= lift.upperLevel() && other.upperLevel() >= lift.lowerLevel())) {
                issues.add("Industrial elevator shafts cannot overlap within a hex.");
            }
            for (var exit : lift.exits().entrySet()) {
                if (exit.getValue() <= 0 || exit.getValue() > 63) {
                    issues.add("Each elevator stop needs an internal access hexside.");
                }
                for (int side = 0; side < 6; side++) {
                    if ((exit.getValue() & (1 << side)) != 0
                          && !hexes.contains(lift.hex().toOffset().translated(side).toCube())) {
                        issues.add("Elevators can be entered only through internal hexsides.");
                    }
                }
            }
            boolean occupied = entity.getEquipmentInHex(lift.hex()).stream().anyMatch(m ->
                  BuildingConstruction.equipmentPositions(entity, m).stream().anyMatch(p -> p.hex().equals(lift.hex())
                        && (lift.reaches(p.level()) || (BuildingConstruction.occupiesRoof(m) && lift.reaches(height)))))
                  || entity.getTransportBays().stream().flatMap(bay -> BuildingConstruction.baySpaces(entity, bay).stream())
                        .anyMatch(space -> space.tons() > 0 && space.position().hex().equals(lift.hex())
                              && lift.reaches(space.position().level()));
            if (occupied) {
                issues.add("An elevator's served levels must be free of equipment and occupied bay/quarters space.");
            }
        }
        for (var mount : design.getAutomatedWeapons()) {
            if (!entity.getEquipment().contains(mount) || !BuildingConstruction.canAutomate(mount.getType())) {
                issues.add("Only Heavy weapons requiring gunners, excluding artillery, can be automated.");
            }
        }
        for (var entry : design.getEquipmentSpace().entrySet()) {
            var mount = entry.getKey();
            var positions = entry.getValue();
            var anchor = BuildingConstruction.position(entity, mount.getLocation());
            if (!BuildingConstruction.canSpread(mount.getType())) {
                issues.add("Only generators, capital weapons and decks can be spread across hexes.");
            }
            if (positions.isEmpty() || !positions.contains(anchor)
                  || positions.stream().map(BuildingDesign.Position::hex).distinct().count() != positions.size()
                  || positions.stream().anyMatch(p -> BuildingConstruction.location(entity, p) < 0)) {
                issues.add("Distributed equipment needs unique occupied hexes including its primary location.");
            }
            if (BuildingConstruction.isCapital(mount.getType()) && anchor != null && positions.stream()
                  .anyMatch(p -> !p.hex().equals(anchor.hex()) && !anchor.hex().neighbors().contains(p.hex()))) {
                issues.add("Capital weapon mass may be shared only with immediately adjacent building hexes.");
            }
            if (BuildingConstruction.isCapital(mount.getType()) && anchor != null && positions.size() > 1
                  && hexes.stream().filter(hex -> anchor.hex().neighbors().contains(hex))
                        .anyMatch(hex -> positions.stream().noneMatch(p -> p.hex().equals(hex)))) {
                issues.add("Distributed capital weapon mass must include all immediately adjacent building hexes.");
            }
        }
        for (var bay : entity.getTransportBays()) {
            var spaces = BuildingConstruction.baySpaces(entity, bay);
            if (Math.abs(spaces.stream().mapToDouble(BuildingDesign.Space::tons).sum() - bay.getWeight()) > .00001
                  || spaces.stream().anyMatch(space -> !Double.isFinite(space.tons()) || space.tons() < 0
                        || BuildingConstruction.location(entity, space.position()) < 0)) {
                issues.add(bay.getUnusedString() + ": distribute exactly " + bay.getWeight() + " tons among valid hexes/floors.");
            }
        }
        if (entity.getEquipment().stream().filter(m -> m.getType() instanceof BuildingEquipmentType facility
              && facility.getFacility() == BuildingEquipmentType.Facility.LANDING_DECK).count() > 1) {
            issues.add("A building may mount only one landing deck.");
        }
    }
}
