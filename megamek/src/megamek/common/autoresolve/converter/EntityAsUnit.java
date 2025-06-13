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

import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.UnitRole;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.ASRange;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.role.Role;
import megamek.common.autoresolve.component.Formation;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.strategicBattleSystems.BaseFormationConverter;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.strategicBattleSystems.SBFUnitConverter;
import megamek.common.util.Counter;
import megamek.logging.MMLogger;

import java.util.ArrayList;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class EntityAsUnit extends BaseFormationConverter<Formation> {
    private static final MMLogger logger = MMLogger.create(EntityAsUnit.class);

    public EntityAsUnit(Force force, SimulationContext game) {
        super(force, game, new Formation());
    }

    @Override
    public Formation convert() {
        Forces forces = game.getForces();
        var player = game.getPlayer(force.getOwnerId());
        Counter<Role> counter = new Counter<>();
        for (Force subforce : forces.getFullSubForces(force)) {
            if (!subforce.getSubForces().isEmpty() || subforce.getEntities().isEmpty()) {
                continue;
            }
            for (ForceAssignable entity : forces.getFullEntities(subforce)) {
                var thisUnit = new ArrayList<AlphaStrikeElement>();
                var unitName = "UNKNOWN";
                if (entity instanceof Entity entityCast) {
                    entityCast.setOwner(player);
                    formation.setEntity(entityCast);
                    unitName = entityCast.getDisplayName() + " ID:" + entityCast.getId();
                    var element = ASConverter.convert(entityCast);
                    if (element != null) {
                        thisUnit.add(element);
                        counter.add(Role.getRole(entityCast.getRole()));
                    } else {
                        var msg = String.format("Could not convert entity %s to AS element", entityCast);
                        logger.error(msg);
                    }
                }
                SBFUnit convertedUnit = new SBFUnitConverter(thisUnit, unitName, report).createSbfUnit();
                formation.addUnit(convertedUnit);
            }
        }
        formation.setOwnerId(force.getOwnerId());
        formation.setName(force.getName());
        formation.setRole(firstNonNull(counter.top(), Role.getRole(UnitRole.SKIRMISHER)));
        formation.setStdDamage(setStdDamageForFormation(formation));
        for (var unit : formation.getUnits()) {
            var health = 0;
            for (var element : unit.getElements()) {
                health += element.getCurrentArmor() + element.getCurrentStructure();
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
