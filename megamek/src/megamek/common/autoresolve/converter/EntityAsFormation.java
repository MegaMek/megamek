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
package megamek.common.autoresolve.converter;

import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASRange;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.role.Role;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.BaseFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.strategicBattleSystems.SBFUnitConverter;
import megamek.logging.MMLogger;

import java.util.ArrayList;

public class EntityAsFormation extends BaseFormationConverter<Formation> {
    private static final MMLogger logger = MMLogger.create(EntityAsFormation.class);
    private final Entity entity;

    public EntityAsFormation(Entity entity, SimulationContext game) {
        super(null, game, new Formation(), new DummyCalculationReport());
        this.entity = entity;
    }

    @Override
    public Formation convert() {
        var thisUnit = new ArrayList<AlphaStrikeElement>();
        if (entity instanceof GunEmplacement gun) {
            gun.initializeArmor(50, 0);
        }
        var element = ASConverter.convert(entity);
        if (element != null) {
            thisUnit.add(element);
        } else {
            var msg = String.format("Could not convert entity %s to AS element", entity);
            logger.error(msg);
            return null;
        }

        SBFUnit convertedUnit = new SBFUnitConverter(thisUnit, entity.getDisplayName() + " ID:" + entity.getId(), report).createSbfUnit();
        formation.addUnit(convertedUnit);
        formation.setEntity(entity);
        formation.setRole(Role.getRole(entity.getRole()));
        formation.setOwnerId(entity.getOwnerId());
        formation.setName(entity.getDisplayName());
        formation.setStdDamage(setStdDamageForFormation(formation));

        for (var unit : formation.getUnits()) {
            var health = 0;
            for (var el : unit.getElements()) {
                health += el.getFullArmor() + el.getFullStructure();
            }
            unit.setArmor(health);
            unit.setCurrentArmor(health);
        }
        formation.setStartingSize(formation.currentSize());
        return formation;
    }

    private ASDamageVector setStdDamageForFormation(Formation formation) {
        // Get the list of damage objects from the units in the formation
        var damages = formation.getUnits().stream().map(SBFUnit::getDamage).toList();
        var size = damages.size();

        // Initialize accumulators for the different damage types
        var l = 0;
        var m = 0;
        var s = 0;

        // Sum up the damage values for each type
        for (var damage : damages) {
            l += damage.getDamage(ASRange.LONG).damage;
            m += damage.getDamage(ASRange.MEDIUM).damage;
            s += damage.getDamage(ASRange.SHORT).damage;
        }
        return new ASDamageVector(
            new ASDamage(Math.ceil((double) s / size)),
            new ASDamage(Math.ceil((double) m / size)),
            new ASDamage(Math.ceil((double) l / size)),
            null,
            size,
            true);
    }

}
