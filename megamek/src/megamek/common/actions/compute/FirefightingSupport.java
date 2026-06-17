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
package megamek.common.actions.compute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * Resolves the multi-platoon firefighting choice from TO:AuE p.153: when more than one firefighting-engineer platoon
 * fights the same blaze, the controlling player may either have each platoon roll separately, or pool the platoons into
 * a single roll that gets a -1 modifier for each additional platoon.
 *
 * <p>The choice is expressed per platoon through the Fire Extinguisher weapon's firing mode:</p>
 * <ul>
 *     <li>{@link #MODE_FIREFIGHT} (default) - the platoon makes its own roll (the "separate roll" option).</li>
 *     <li>{@link #MODE_SUPPORT} - the platoon yields its roll and instead lends -1 to the lead platoon's roll on the
 *     same hex (the "single roll" option).</li>
 * </ul>
 *
 * <p>For a given burning hex the lead (rolling) platoon is the lowest-id platoon in {@link #MODE_FIREFIGHT} mode; if
 * every platoon on the hex is in {@link #MODE_SUPPORT} mode, the lowest-id platoon is promoted to lead so the attack is
 * not wasted. Each remaining support platoon contributes -1 to the lead's roll. Additional platoons that chose
 * {@link #MODE_FIREFIGHT} simply roll on their own.</p>
 *
 * <p>This is evaluated at weapon-attack resolution, where every declared attack for the phase is present in
 * {@link Game#getActions()}, so the partition of platoons into lead/support is stable and order-independent.</p>
 */
public final class FirefightingSupport {

    private static final MMLogger LOGGER = MMLogger.create(FirefightingSupport.class);

    /** Fire Extinguisher mode: the platoon makes its own firefighting roll. This is the default mode. */
    public static final String MODE_FIREFIGHT = "Firefight";

    /** Fire Extinguisher mode: the platoon yields its roll and lends -1 to the lead platoon's roll on the same hex. */
    public static final String MODE_SUPPORT = "Support";

    private FirefightingSupport() {}

    /**
     * The number of supporting platoons whose -1 modifiers apply to this attacker's firefighting roll (TO:AuE p.153).
     * Returns a value greater than zero only when this attacker is the lead (rolling) platoon for the targeted hex;
     * non-lead platoons (separate rollers and yielding supporters) get 0.
     *
     * @param game     the current {@link Game}
     * @param attacker the firefighting platoon making the roll
     * @param target   the hex being extinguished
     *
     * @return the count of additional platoons supporting this roll, or 0 if this attacker is not the lead roller
     */
    public static int supportingPlatoons(Game game, Entity attacker, @Nullable Targetable target) {
        if (!appliesTo(attacker, target)) {
            return 0;
        }
        Parties parties = partiesFor(game, target.getPosition(), target.getBoardId());
        if (attacker.getId() != parties.leadId()) {
            return 0;
        }
        // The lead may itself be a promoted supporter (when no separate-roller exists), so exclude it from the count.
        int supporting = (int) parties.supporters().stream().filter(id -> id != attacker.getId()).count();
        if (supporting > 0) {
            LOGGER.debug("[Firefight] lead platoon {} on hex {} gets -{} from supporting platoons",
                  attacker.getId(), target.getPosition(), supporting);
        }
        return supporting;
    }

    /**
     * Whether this attacker yields its firefighting roll to a lead platoon (TO:AuE p.153). A yielding supporter is in
     * {@link #MODE_SUPPORT} mode and is not the lead platoon for the hex, so it makes no roll of its own and instead
     * lends -1 to the lead.
     *
     * @param game     the current {@link Game}
     * @param attacker the firefighting platoon being resolved
     * @param target   the hex being extinguished
     *
     * @return true if this attacker supports another platoon's roll instead of rolling itself
     */
    public static boolean isYieldingSupporter(Game game, Entity attacker, @Nullable Targetable target) {
        if (!appliesTo(attacker, target) || !isInSupportMode(attacker)) {
            return false;
        }
        Parties parties = partiesFor(game, target.getPosition(), target.getBoardId());
        boolean yielding = attacker.getId() != parties.leadId();
        if (yielding) {
            LOGGER.debug("[Firefight] platoon {} supports the lead on hex {} (no separate roll)",
                  attacker.getId(), target.getPosition());
        }
        return yielding;
    }

    private static boolean appliesTo(@Nullable Entity attacker, @Nullable Targetable target) {
        return (attacker != null) && attacker.isFirefighter()
              && (target != null) && (target.getTargetType() == Targetable.TYPE_HEX_EXTINGUISH)
              && (target.getPosition() != null);
    }

    /** @return true if the platoon's Fire Extinguisher weapon is set to {@link #MODE_SUPPORT}. */
    private static boolean isInSupportMode(Entity attacker) {
        return attacker.getWeaponList().stream()
              .filter(weapon -> weapon.getType().hasFlag(WeaponType.F_EXTINGUISHER))
              .findFirst()
              .map(weapon -> weapon.curMode().equals(MODE_SUPPORT))
              .orElse(false);
    }

    /**
     * Partitions every firefighting-engineer extinguish attack declared against the given hex this phase into separate
     * rollers and yielding supporters, each sorted by entity id for a stable lead choice.
     */
    private static Parties partiesFor(Game game, Coords hex, int boardId) {
        List<Integer> rollers = new ArrayList<>();
        List<Integer> supporters = new ArrayList<>();
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            if (!(actions.nextElement() instanceof WeaponAttackAction waa)
                  || !isFirefightingExtinguish(game, waa, hex, boardId)) {
                continue;
            }
            Entity platoon = game.getEntity(waa.getEntityId());
            if (isInSupportMode(platoon)) {
                supporters.add(platoon.getId());
            } else {
                rollers.add(platoon.getId());
            }
        }
        Collections.sort(rollers);
        Collections.sort(supporters);
        return new Parties(rollers, supporters);
    }

    /** @return true if the action is a firefighting-engineer attack extinguishing the given hex on the given board. */
    private static boolean isFirefightingExtinguish(Game game, WeaponAttackAction waa, Coords hex, int boardId) {
        if (waa.getTargetType() != Targetable.TYPE_HEX_EXTINGUISH) {
            return false;
        }
        Targetable target = waa.getTarget(game);
        if ((target == null) || !hex.equals(target.getPosition()) || (target.getBoardId() != boardId)) {
            return false;
        }
        Entity attacker = game.getEntity(waa.getEntityId());
        return (attacker != null) && attacker.isFirefighter();
    }

    /**
     * The firefighting platoons fighting one hex, split by chosen mode and sorted by entity id.
     *
     * @param rollers    ids of platoons making their own roll ({@link #MODE_FIREFIGHT})
     * @param supporters ids of platoons lending -1 to the lead ({@link #MODE_SUPPORT})
     */
    private record Parties(List<Integer> rollers, List<Integer> supporters) {
        /** @return the id of the lead (rolling) platoon, or {@link Entity#NONE} if no platoon is fighting the hex. */
        int leadId() {
            if (!rollers.isEmpty()) {
                return rollers.get(0);
            }
            if (!supporters.isEmpty()) {
                return supporters.get(0);
            }
            return Entity.NONE;
        }
    }
}
