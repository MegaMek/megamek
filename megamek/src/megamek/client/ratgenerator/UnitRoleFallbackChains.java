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
package megamek.client.ratgenerator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.common.units.UnitRole;

/**
 * Ranked substitute roles for each {@link UnitRole}, used when a requested role cannot be filled from the units
 * available to a force at its weight class.
 *
 * <p>Chains are derived from the role descriptions in Alpha Strike Companion and ranked by three criteria in order:
 * explicit linkage in the source text, then the shared primary axis (speed band, then armour band, then preferred
 * range), then measured availability in the random assignment tables.</p>
 *
 * <p>Each chain degrades correctly across all four weight classes without any weight-specific handling, because the
 * caller skips roles with no coverage in the leaf's own weight band. Walking the chain therefore lands on the closest
 * substitute that the band can actually supply. For example a Skirmisher, which does not exist at Light in any
 * measured table, falls to Striker - the role its own Alpha Strike Companion description names as its lighter
 * counterpart.</p>
 *
 * <p>Two chains preserve battlefield shape but lose the role's defining capability, and callers that care about that
 * capability should pair the role target with the matching {@link MissionRole} filter instead of relying on the
 * chain:</p>
 * <ul>
 *     <li>Missile Boat to Sniper keeps the long range but drops indirect fire, since
 *         {@link UnitRole#qualifiesForRole} defines a Sniper as doing long-range damage that specifically excludes
 *         LRMs.</li>
 *     <li>Scout to Striker keeps the speed and fragility but drops the sensor suite.</li>
 * </ul>
 */
final class UnitRoleFallbackChains {

    /** Ranked substitutes per role. Roles absent from this map have no substitute. */
    private static final Map<UnitRole, List<UnitRole>> CHAINS = buildChains();

    private UnitRoleFallbackChains() {}

    /**
     * The ranked substitutes for a role, best first.
     *
     * <p>Returns an empty list for {@link UnitRole#TRANSPORT}, which names a class of unit rather than a battlefield
     * posture and so has no meaningful substitute, and for the {@code UNDETERMINED} and {@code NONE} placeholders.</p>
     *
     * @param role the role that could not be filled
     *
     * @return the substitutes to try in order, never {@code null} and never containing {@code role} itself
     */
    static List<UnitRole> chainFor(UnitRole role) {
        return CHAINS.getOrDefault(role, List.of());
    }

    private static Map<UnitRole, List<UnitRole>> buildChains() {
        Map<UnitRole, List<UnitRole>> chains = new EnumMap<>(UnitRole.class);

        // Ground roles. Ambusher and Striker share light armour and a short-range preference, differing only in
        // speed; Juggernaut is the heavy end of "slow, short range, controls its own ground".
        chains.put(UnitRole.AMBUSHER, List.of(UnitRole.STRIKER, UnitRole.JUGGERNAUT, UnitRole.BRAWLER));
        // Both of the Brawler's neighbours hold a line: Skirmisher trades up in speed, Juggernaut in toughness.
        // Striker rescues the Light band, where neither of the first two exists.
        chains.put(UnitRole.BRAWLER, List.of(UnitRole.SKIRMISHER, UnitRole.JUGGERNAUT, UnitRole.STRIKER));
        // A Brawler is the line unit that can "hold the line if it is charged" - the Juggernaut's job with less mass.
        chains.put(UnitRole.JUGGERNAUT, List.of(UnitRole.BRAWLER, UnitRole.AMBUSHER, UnitRole.SKIRMISHER));
        // "Missile Boats are similar to Snipers". Scout follows because Scouts are the spotters that make indirect
        // fire work.
        chains.put(UnitRole.MISSILE_BOAT, List.of(UnitRole.SNIPER, UnitRole.SCOUT, UnitRole.BRAWLER));
        // Striker is the same fast, lightly armoured chassis with teeth. Skirmisher covers Heavy and Assault, where
        // Scouts are nearly absent.
        chains.put(UnitRole.SCOUT, List.of(UnitRole.STRIKER, UnitRole.SKIRMISHER, UnitRole.AMBUSHER));
        // "Skirmishers are often commanders of lighter Strikers".
        chains.put(UnitRole.SKIRMISHER, List.of(UnitRole.STRIKER, UnitRole.BRAWLER, UnitRole.SCOUT));
        // The reciprocal of the Missile Boat link. Brawlers follow because they are "usually able to do some damage
        // at longer ranges".
        chains.put(UnitRole.SNIPER, List.of(UnitRole.MISSILE_BOAT, UnitRole.BRAWLER, UnitRole.SKIRMISHER));
        // The Skirmisher pairing read the other way. Skirmisher is the only one of the three present at Assault.
        chains.put(UnitRole.STRIKER, List.of(UnitRole.SKIRMISHER, UnitRole.SCOUT, UnitRole.AMBUSHER));

        // Aerospace roles. Fire Support sits second for Attack Fighters because those units are "often conscripted
        // for heavy ground support missions as well" - the Attack Fighter's own job.
        chains.put(UnitRole.ATTACK_FIGHTER,
              List.of(UnitRole.DOGFIGHTER, UnitRole.FIRE_SUPPORT, UnitRole.FAST_DOGFIGHTER));
        // Fast Dogfighters exist to "assist and support dedicated Interceptors once battle is joined".
        chains.put(UnitRole.DOGFIGHTER,
              List.of(UnitRole.FAST_DOGFIGHTER, UnitRole.ATTACK_FIGHTER, UnitRole.FIRE_SUPPORT));
        // The Fast Dogfighter's definition names both of its neighbours; it sits between them.
        chains.put(UnitRole.FAST_DOGFIGHTER,
              List.of(UnitRole.INTERCEPTOR, UnitRole.DOGFIGHTER, UnitRole.ATTACK_FIGHTER));
        chains.put(UnitRole.FIRE_SUPPORT,
              List.of(UnitRole.ATTACK_FIGHTER, UnitRole.DOGFIGHTER, UnitRole.FAST_DOGFIGHTER));
        // "Second shell interceptors" is the canonical fallback by definition.
        chains.put(UnitRole.INTERCEPTOR,
              List.of(UnitRole.FAST_DOGFIGHTER, UnitRole.DOGFIGHTER, UnitRole.ATTACK_FIGHTER));

        // TRANSPORT is deliberately absent: it names a class of unit (Small Craft, DropShips, large airborne
        // support vehicles) rather than a battlefield posture, so no fighter role substitutes for it.

        return chains;
    }
}
