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

package megamek.common;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Faction;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.interfaces.ITechnology;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.common.weapons.attacks.InfantryAttack;

/**
 * A {@link CompositeTechLevel} that remembers every component folded into it, together with the state of the composite
 * immediately after that component was added.
 * <p>
 * This exists so that a tech level report can show which component is responsible for a unit's tech level, rather than
 * only the final answer. Because it records the real {@code addComponent} calls instead of re-deriving the component
 * list, it cannot drift from the calculation it is describing.
 * <p>
 * This class is a reporting aid and is never part of game state: it is not sent to clients and never reaches a saved
 * game. The recorded components are therefore held in a {@code transient} list and {@link ComponentTechRecord} is
 * deliberately not serializable.
 *
 * @since 0.51.01
 */
public class RecordingCompositeTechLevel extends CompositeTechLevel {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String UNNAMED_COMPONENT = Messages.getString("CompositeTechLevel.component.unnamed");

    private final transient List<ComponentTechRecord> componentRecords = new ArrayList<>();
    private final int evaluationYear;

    /**
     * A single component of a unit, as recorded while the composite tech level was being built.
     *
     * @param componentName          The name the component was reported under
     * @param prototypeDate          The component's own prototype date, or {@link ITechnology#DATE_NONE}
     * @param productionDate         The component's own production date, or {@link ITechnology#DATE_NONE}
     * @param commonDate             The component's own common date, or {@link ITechnology#DATE_NONE}
     * @param staticTechLevel        The component's tech level when variable tech level is off
     * @param variableTechLevel      The component's tech level in the evaluated year, or {@code null} when the
     *                               component has no advancement dates and therefore no era progression
     * @param extinctionDate         The component's own extinction date, or {@link ITechnology#DATE_NONE}
     * @param reintroductionDate     The component's own reintroduction date, or {@link ITechnology#DATE_NONE}
     * @param compositePrototypeDate The unit's prototype date after this component was added
     * @param compositeProductionDate The unit's production date after this component was added
     * @param compositeCommonDate    The unit's common date after this component was added
     * @param compositeExtinctionDate The unit's extinction date after this component was added
     * @param compositeReintroductionDate The unit's reintroduction date after this component was added
     */
    public record ComponentTechRecord(String componentName, int prototypeDate, int productionDate, int commonDate,
                                      SimpleTechLevel staticTechLevel, @Nullable SimpleTechLevel variableTechLevel,
                                      int extinctionDate, int reintroductionDate, int compositePrototypeDate,
                                      int compositeProductionDate, int compositeCommonDate,
                                      int compositeExtinctionDate, int compositeReintroductionDate) {}

    /**
     * Creates a composite tech level for the given unit that records each component as it is added.
     *
     * @param entity         The unit whose tech level is being built
     * @param techFaction    The faction to evaluate faction-specific dates for
     * @param evaluationYear The year to evaluate each component's variable tech level in
     */
    public RecordingCompositeTechLevel(Entity entity, Faction techFaction, int evaluationYear) {
        super(entity, techFaction);
        this.evaluationYear = evaluationYear;
        // The unit's construction advancement seeds the composite rather than being added to it, so it is
        // recorded here; without it the report would start part-way through the story it is telling.
        addRecord(entity.getConstructionTechAdvancement(), chassisName(entity));
    }

    @Override
    protected void recordComponent(ITechnology tech, @Nullable String componentName) {
        // Every infantry and battle armor unit automatically carries the anti-Mek attack pseudo-weapons (Swarm
        // Mek, Leg Attack and the like). They are noise in a tech level report, so they are not recorded as
        // rows. Their dates are still folded into the composite by the base class before this point, so the
        // unit's overall tech level is unaffected -- they simply never limit it.
        if (tech instanceof InfantryAttack) {
            return;
        }
        addRecord(tech, componentName);
    }

    private void addRecord(ITechnology tech, @Nullable String componentName) {
        boolean isMixed = isMixedTech();
        int prototypeDate = isMixed ? tech.getPrototypeDate() : tech.getPrototypeDate(isClan());
        int productionDate = isMixed ? tech.getProductionDate() : tech.getProductionDate(isClan());
        int commonDate = isMixed ? tech.getCommonDate() : tech.getCommonDate(isClan());

        // A component with no advancement dates at all has no era progression, so it has no meaningful
        // variable tech level. Reporting the level that would be calculated for it (Unofficial) reads as an
        // alarming result for what is in fact an entry that contributes nothing to the unit's dates.
        boolean hasNoDates = (prototypeDate == DATE_NONE)
              && (productionDate == DATE_NONE)
              && (commonDate == DATE_NONE);
        SimpleTechLevel variableTechLevel = hasNoDates ? null
              : (isMixed ? tech.getSimpleLevel(evaluationYear) : tech.getSimpleLevel(evaluationYear, isClan()));

        int extinctionDate = isMixed ? tech.getExtinctionDate() : tech.getExtinctionDate(isClan());
        int reintroductionDate = isMixed ? tech.getReintroductionDate() : tech.getReintroductionDate(isClan());

        componentRecords.add(new ComponentTechRecord(resolveName(tech, componentName),
              prototypeDate,
              productionDate,
              commonDate,
              tech.getStaticTechLevel(),
              variableTechLevel,
              extinctionDate,
              reintroductionDate,
              getPrototypeDate(),
              getProductionDate(),
              getCommonDate(),
              getExtinctionDate(),
              getReintroductionDate()));
    }

    /**
     * @param entity The unit being reported on
     *
     * @return A name for the unit's construction advancement, such as {@code "Chassis (Medium BattleArmor)"}
     */
    private static String chassisName(Entity entity) {
        String unitTypeName = UnitType.getTypeDisplayableName(entity.getUnitType());
        String weightClassName = entity.getWeightClassName();
        if ((weightClassName == null) || weightClassName.isBlank()) {
            return Messages.getString("CompositeTechLevel.component.chassis", unitTypeName);
        }
        return Messages.getString("CompositeTechLevel.component.chassisWithWeightClass", weightClassName,
              unitTypeName);
    }

    /**
     * Returns the name to report a component under. Equipment and engines know their own name; anything else must be
     * named by the caller that added it.
     *
     * @param tech          The component that was added
     * @param componentName The name supplied by the caller, or {@code null} if none was given
     *
     * @return The name to display for this component
     */
    private static String resolveName(ITechnology tech, @Nullable String componentName) {
        if (componentName != null) {
            return componentName;
        }
        return switch (tech) {
            case EquipmentType equipmentType -> equipmentType.getName();
            case Engine engine -> engine.getEngineName();
            default -> UNNAMED_COMPONENT;
        };
    }

    /**
     * @return Every component of this unit, in the order it was added to the composite
     */
    public List<ComponentTechRecord> getComponentRecords() {
        return Collections.unmodifiableList(componentRecords);
    }

    /**
     * @return The year each component's variable tech level was evaluated in
     */
    public int getEvaluationYear() {
        return evaluationYear;
    }
}
