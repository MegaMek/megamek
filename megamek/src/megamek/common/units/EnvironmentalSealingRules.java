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

import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.Engine;

/**
 * The Environmental Sealing rules, expressed as plain questions about a unit, TM p.216 and TO:AUE p.115.
 * <p>
 * Sealing answers two separate questions, and the rules answer them differently. The first is whether the unit is
 * closed against the outside air at all: some unit types buy that with equipment, and others are built that way and
 * cannot install the equipment. The second is whether the unit's powerplant keeps running once it is shut away from
 * the outside air, which decides whether the sealing is any use in vacuum or under water.
 * <p>
 * These live in a shared class rather than on the server so that the client asks exactly the same questions the
 * server enforces: deployment, movement legality, the bot's path ranking and the server's end-of-turn resolution all
 * read the same answers.
 */
public final class EnvironmentalSealingRules {

    private EnvironmentalSealingRules() {
    }

    /**
     * Whether this unit's engine keeps running when the unit is sealed away from the outside air, which is what
     * lets Environmental Sealing be any use in vacuum or fully submerged. An internal combustion engine and a steam
     * plant both have to breathe, so sealing alone never gets them off an airless world or onto a lake bed.
     * <p>
     * The rules name fission, fusion and fuel cell engines for BattleMeks and Combat Vehicles (TO:AUE p.115), and
     * fission, fusion and electric engines for Support Vehicles (TM p.122). MegaMek's Support Vehicle "Electric"
     * engine is {@link Engine#BATTERY}, and a fuel cell is an electric powerplant too, so the union of the two lists
     * is fission, fusion, fuel cell and battery.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the engine runs sealed off from the outside air
     */
    public static boolean hasSealedOperationEngine(@Nullable Entity entity) {
        if ((entity == null) || !entity.hasEngine()) {
            return false;
        }
        Engine engine = entity.getEngine();
        int engineType = engine.getEngineType();
        return engine.isFusion()
              || (engineType == Engine.FISSION)
              || (engineType == Engine.FUEL_CELL)
              || (engineType == Engine.BATTERY);
    }

    /**
     * Whether this unit is closed against the outside atmosphere, whether it bought the sealing or was built with it.
     * <p>
     * BattleMeks, ProtoMeks, battle armor, fighters, Small Craft and DropShips are sealed as part of their basic
     * construction, which is exactly why the rules forbid them from installing Environmental Sealing (TM p.216).
     * IndustrialMeks are the one kind of Mek that is not, which is what the 10 percent of mass buys them. Vehicles
     * answer through {@link Tank#hasEnvironmentalSealing()}, which already hands submarines their automatic sealing.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the outside air cannot reach the unit's crew
     */
    public static boolean isSealedAgainstAtmosphere(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Mek mek) {
            return !mek.isIndustrial() || mek.hasEnvironmentalSealing();
        }
        if (entity.isConventionalInfantry()) {
            // Infantry are protected by their gear, not by sealing; the XCT rules cover them.
            return false;
        }
        if ((entity instanceof ProtoMek) || (entity instanceof BattleArmor) || entity.isAero()) {
            return true;
        }
        return entity.hasEnvironmentalSealing();
    }

    /**
     * Whether this unit can be exposed to vacuum without dying: it must be sealed, and its engine must keep running
     * with no outside air to breathe (TO:AUE p.115).
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the unit survives in vacuum
     */
    public static boolean canOperateInVacuum(@Nullable Entity entity) {
        return isSealedAgainstAtmosphere(entity) && hasSealedOperationEngine(entity);
    }

    /**
     * Whether this unit can be completely under water without drowning. The rules ask the same two questions as
     * vacuum does: an IndustrialMek "cannot be fully submerged unless [it] incorporate[s] both environmental sealing
     * and a Fission, Fusion or Fuel Cell engine" (TM p.216), and Support Vehicles may operate underwater on the same
     * engines that let them operate in vacuum (TM p.122).
     * <p>
     * Note that this asks about being <em>fully</em> submerged. A unit tall enough to keep its head above the surface
     * is wading, not submerged, and this rule does not apply to it.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the unit survives fully submerged
     */
    public static boolean canOperateFullySubmerged(@Nullable Entity entity) {
        return isSealedAgainstAtmosphere(entity) && hasSealedOperationEngine(entity);
    }

}
