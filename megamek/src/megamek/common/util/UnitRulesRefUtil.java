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
package megamek.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import megamek.common.RulesRef;
import megamek.common.SourceBookCode;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

/** Builds the minimal sourcebook combinations that cover every referenced component of a unit. */
public final class UnitRulesRefUtil {

    private UnitRulesRefUtil() {
    }

    /**
     * Collects rules references from equipment and intrinsic systems with structured reference data.
     *
     * @param entity the unit to inspect
     *
     * @return alternative minimal sourcebook combinations that each cover the unit
     */
    public static List<List<SourceBookCode>> collectRulesRefBuckets(Entity entity) {
        List<List<SourceBookCode>> componentRulesRefs = new ArrayList<>();
        Set<Mounted<?>> visitedMounts = Collections.newSetFromMap(new IdentityHashMap<>());

        addIntrinsicRulesRefs(componentRulesRefs, entity);
        for (Mounted<?> mounted : entity.getEquipment()) {
            addMountedRulesRefs(componentRulesRefs, mounted, visitedMounts);
        }
        if (entity instanceof ConvInfantry infantry) {
            addEquipmentRulesRefs(componentRulesRefs, infantry.getPrimaryWeapon());
            addEquipmentRulesRefs(componentRulesRefs, infantry.getSecondaryWeapon());
        }

        return createMinimalCombinations(componentRulesRefs, entity instanceof Mek,
              (entity instanceof Mek mek) && mek.isIndustrial());
    }

    /**
     * Builds the inclusion-minimal combinations that contain at least one referenced book for every component.
     * Components without references are neutral. Core and BMM references are removed when the unit is ineligible for
     * those books; if that leaves a referenced component with no available book, no combination can cover the unit.
     */
    static List<List<SourceBookCode>> createMinimalCombinations(
          List<? extends Collection<SourceBookCode>> componentRulesRefs, boolean isMek, boolean isIndustrialMek) {
        List<LinkedHashSet<SourceBookCode>> combinations = new ArrayList<>();
        combinations.add(new LinkedHashSet<>());
        boolean foundReference = false;

        for (Collection<SourceBookCode> componentBooks : componentRulesRefs) {
            LinkedHashSet<SourceBookCode> referencedBooks = new LinkedHashSet<>();
            if (componentBooks != null) {
                componentBooks.stream().filter(Objects::nonNull).forEach(referencedBooks::add);
            }
            if (referencedBooks.isEmpty()) {
                continue;
            }
            foundReference = true;

            referencedBooks.removeIf(book -> !isBookApplicable(book, isMek, isIndustrialMek));
            if (referencedBooks.isEmpty()) {
                return List.of();
            }

            List<LinkedHashSet<SourceBookCode>> nextCombinations = new ArrayList<>();
            for (LinkedHashSet<SourceBookCode> combination : combinations) {
                if (!Collections.disjoint(combination, referencedBooks)) {
                    addIfMinimal(nextCombinations, combination);
                } else {
                    for (SourceBookCode book : referencedBooks) {
                        LinkedHashSet<SourceBookCode> expanded = new LinkedHashSet<>(combination);
                        expanded.add(book);
                        addIfMinimal(nextCombinations, expanded);
                    }
                }
            }
            combinations = nextCombinations;
        }

        if (!foundReference) {
            return List.of();
        }

        combinations.sort((left, right) -> Integer.compare(left.size(), right.size()));
        return combinations.stream().map(List::copyOf).toList();
    }

    private static void addIfMinimal(List<LinkedHashSet<SourceBookCode>> combinations,
          LinkedHashSet<SourceBookCode> candidate) {
        if (combinations.stream().anyMatch(candidate::containsAll)) {
            return;
        }
        combinations.removeIf(existing -> existing.containsAll(candidate));
        combinations.add(new LinkedHashSet<>(candidate));
    }

    private static boolean isBookApplicable(SourceBookCode book, boolean isMek, boolean isIndustrialMek) {
        return switch (book) {
            case CORE -> isMek && !isIndustrialMek;
            case BMM -> isMek;
            default -> true;
        };
    }

    private static void addIntrinsicRulesRefs(List<List<SourceBookCode>> componentRulesRefs, Entity entity) {
        addArmorRulesRefs(componentRulesRefs, entity);
        addStructureRulesRefs(componentRulesRefs, entity);
    }

    private static void addMountedRulesRefs(List<List<SourceBookCode>> componentRulesRefs,
          @Nullable Mounted<?> mounted, Set<Mounted<?>> visitedMounts) {
        if ((mounted == null) || !visitedMounts.add(mounted)) {
            return;
        }

        addEquipmentRulesRefs(componentRulesRefs, mounted.getType());
        addMountedRulesRefs(componentRulesRefs, mounted.getLinked(), visitedMounts);
        addMountedRulesRefs(componentRulesRefs, mounted.getLinkedBy(), visitedMounts);
        addMountedRulesRefs(componentRulesRefs, mounted.getCrossLinkedBy(), visitedMounts);

        if (mounted instanceof WeaponMounted bay) {
            bay.getBayWeapons().forEach(member -> addMountedRulesRefs(componentRulesRefs, member, visitedMounts));
            bay.getBayAmmo().forEach(ammo -> addMountedRulesRefs(componentRulesRefs, ammo, visitedMounts));
        }
    }

    private static void addArmorRulesRefs(List<List<SourceBookCode>> componentRulesRefs, Entity entity) {
        if (entity.hasPatchworkArmor()) {
            addEquipmentRulesRefs(componentRulesRefs, ArmorType.of(EquipmentType.T_ARMOR_PATCHWORK, false));
        }
        for (int location = 0; location < entity.locations(); location++) {
            int armorType = entity.getArmorType(location);
            if (armorType >= 0) {
                addEquipmentRulesRefs(componentRulesRefs, ArmorType.of(armorType,
                      TechConstants.isClan(entity.getArmorTechLevel(location))));
            }
        }
    }

    private static void addStructureRulesRefs(List<List<SourceBookCode>> componentRulesRefs, Entity entity) {
        if (entity instanceof Mek mek) {
            for (int location = 0; location < mek.locations(); location++) {
                addStructureRulesRef(componentRulesRefs, mek.getStructureType(location),
                      TechConstants.isClan(mek.getFrankenMekStructureTechLevel(location)));
            }
        } else {
            addStructureRulesRef(componentRulesRefs, entity.getStructureType(),
                  TechConstants.isClan(entity.getStructureTechLevel()));
        }
    }

    private static void addStructureRulesRef(List<List<SourceBookCode>> componentRulesRefs, int structureType,
          boolean clan) {
        if (structureType >= 0) {
            addEquipmentRulesRefs(componentRulesRefs, EquipmentType.getStructureFromName(
                  EquipmentType.getStructureTypeName(structureType, clan)));
        }
    }

    private static void addEquipmentRulesRefs(List<List<SourceBookCode>> componentRulesRefs,
          @Nullable EquipmentType equipmentType) {
        if (equipmentType != null) {
            componentRulesRefs.add(equipmentType.getRulesRefs().stream()
                  .map(RulesRef::book)
                  .distinct()
                  .toList());
        }
    }
}
