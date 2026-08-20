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
package megamek.client.formation;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.UnitRole;

/**
 * Everything the {@link FormationAssembler} needs to know about one unit, captured up front so the
 * partition search never touches a live {@link Entity}. Tests build these directly with synthetic
 * {@link MekSummary} data; the lobby builds them through {@link #of(Entity)}.
 *
 * @param entityId       the lobby entity id, carried through to the force assignment
 * @param displayName    the unit's short name, for reports and logs
 * @param role           the unit's battlefield role, {@link UnitRole#UNDETERMINED} when unknown
 * @param weightClass    the {@link megamek.common.units.EntityWeightClass} constant
 * @param walkMp         walking movement points, the speed-spread input
 * @param battleValue    the unit's current battle value, the balance input
 * @param carriesEcm     whether ECM equipment is installed (installed, not active - lobby-safe)
 * @param clan           whether the unit is Clan tech, the organization-detection input
 * @param c3NetworkId    the C3 network the unit belongs to, {@code null} when it has none
 * @param transportId    the entity id of the unit carrying this one, {@link Entity#NONE} when unloaded
 * @param towedById      the entity id of the unit towing this one, {@link Entity#NONE} when untowed
 * @param family         the combined-arms family the unit partitions under
 * @param summary        the catalog entry backing {@code FormationType.qualifies()}; null for units the
 *                       cache does not know (customs, MUL imports), which then match no formation type
 *                       but still partition normally
 */
public record AssemblyUnit(int entityId, String displayName, UnitRole role, int weightClass, int walkMp,
      int battleValue, boolean carriesEcm, boolean clan, @Nullable String c3NetworkId, int transportId,
      int towedById, Family family, @Nullable MekSummary summary) {

    /**
     * The combined-arms families that partition separately: Campaign Operations builds type-pure
     * elements, and aerospace never shares an element with ground units.
     */
    public enum Family {
        MEK, VEHICLE, INFANTRY, AERO
    }

    /**
     * Captures a lobby entity. The catalog lookup can miss (custom units, hand-edited files); the unit
     * still assembles, it just cannot qualify for a named formation type.
     *
     * @param entity the lobby entity to capture
     *
     * @return the assembly view of the entity
     */
    public static AssemblyUnit of(Entity entity) {
        MekSummary summary = MekSummaryCache.getInstance().getMek(entity.getShortNameRaw());
        return new AssemblyUnit(entity.getId(), entity.getShortNameRaw(), entity.getRole(),
              entity.getWeightClass(), entity.getWalkMP(), entity.calculateBattleValue(),
              entity.hasECM(), entity.isClan(), entity.getC3NetId(), entity.getTransportId(),
              entity.getTowedBy(), familyOf(entity), summary);
    }

    private static Family familyOf(Entity entity) {
        if (entity.isAero() || entity.isFighter()) {
            return Family.AERO;
        }
        if (entity.isInfantry() || entity.isBattleArmor()) {
            return Family.INFANTRY;
        }
        if (entity.isVehicle() || entity.isSupportVehicle()) {
            return Family.VEHICLE;
        }
        // Meks and ProtoMeks: most CamOps ground formation types admit both.
        return Family.MEK;
    }
}
