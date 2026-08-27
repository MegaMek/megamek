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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Wires the C3 and C3i networks of a generated force.
 *
 * <p>Generation picks units carrying network equipment but leaves them unlinked, so a force arrives
 * with the hardware and no network. This walks the tree and connects what it finds: a formation flagged
 * for C3 gets a master with up to three slaves, and any other formation has whatever C3i-capable units
 * it holds joined into one network.</p>
 *
 * <p>Links are written twice over, and both are needed. The net id is what the current game reads, and
 * the C3 UUIDs are what survive being saved and reloaded - a campaign rebuilds its networks from those
 * UUIDs long after the generating run has gone. Writing only the net id gives a network that vanishes
 * on save; writing only the UUIDs gives one that does nothing until something rebuilds it.</p>
 *
 * <p>A unit whose crew carries the Boosted Cybernetic Comm Implant counts as a C3i node without any
 * equipment, so an infantry or BattleArmor trooper can be networked - {@link Entity#hasC3i()} reports
 * the implant the same as the hardware.</p>
 */
public final class C3NetworkConfigurator {

    private static final MMLogger LOGGER = MMLogger.create(C3NetworkConfigurator.class);

    /** The ruleset flag marking a formation built around a hierarchic C3 network. */
    private static final String C3_FORMATION_FLAG = "c3";

    /** A C3 master supports three slaves. */
    private static final int MAX_C3_SLAVES = 3;

    private C3NetworkConfigurator() {
    }

    /**
     * Wires every network in the tree rooted at the given formation.
     *
     * @param root the generated force to configure; {@code null} is ignored
     */
    public static void configure(@Nullable ForceDescriptor root) {
        configure(root, entity -> true);
    }

    /**
     * Wires every network in the tree rooted at the given formation, considering only the units the
     * caller accepts.
     *
     * @param root      the generated force to configure; {@code null} is ignored
     * @param isIncluded decides whether a unit takes part - a caller showing a selection wires only what
     *                   the user actually took
     */
    public static void configure(@Nullable ForceDescriptor root, Predicate<Entity> isIncluded) {
        if (root == null) {
            return;
        }
        int networked = configureFormation(root, isIncluded);
        for (ForceDescriptor subFormation : root.getSubForces()) {
            configure(subFormation, isIncluded);
        }
        for (ForceDescriptor attached : root.getAttached()) {
            configure(attached, isIncluded);
        }
        if (root.isTopLevel() && (networked > 0)) {
            LOGGER.info("[C3Network] configured networks under '{}'", root.parseName());
        }
    }

    /**
     * Wires the one formation's own children, without recursing.
     *
     * @return how many units were joined to a network here
     */
    private static int configureFormation(ForceDescriptor formation, Predicate<Entity> isIncluded) {
        List<Entity> members = includedMembers(formation, isIncluded);
        if (members.isEmpty()) {
            return 0;
        }
        if (formation.getFlags().contains(C3_FORMATION_FLAG)) {
            return linkHierarchicC3(formation, members);
        }
        return linkC3i(formation, members);
    }

    /**
     * @return the entities of this formation's immediate children that the caller accepts
     */
    private static List<Entity> includedMembers(ForceDescriptor formation, Predicate<Entity> isIncluded) {
        List<Entity> members = new ArrayList<>();
        for (ForceDescriptor subFormation : formation.getSubForces()) {
            Entity entity = subFormation.getEntity();
            if ((entity != null) && isIncluded.test(entity)) {
                members.add(entity);
            }
        }
        return members;
    }

    /**
     * Attaches up to three C3 slaves to the first master in the formation.
     *
     * @return how many slaves were attached
     */
    private static int linkHierarchicC3(ForceDescriptor formation, List<Entity> members) {
        Entity master = members.stream()
                              .filter(entity -> entity.hasC3M() || entity.hasC3MM())
                              .findFirst()
                              .orElse(null);
        if (master == null) {
            LOGGER.debug("[C3Network] '{}' is flagged for C3 but holds no master; nothing linked",
                  formation.parseName());
            return 0;
        }
        master.setC3UUID();

        int slaves = 0;
        for (Entity member : members) {
            if (slaves >= MAX_C3_SLAVES) {
                break;
            }
            if (member.getExternalIdAsString().equals(master.getExternalIdAsString())
                  || !member.hasC3S()) {
                continue;
            }
            member.setC3UUID();
            // The UUID always: it is what survives a save and what a campaign rebuilds from.
            member.setC3MasterIsUUIDAsString(master.getC3UUIDAsString());
            // The live link only once the unit is in a game. setC3Master walks the game's entities to
            // update everyone already on the master's network, so calling it on a force that has not
            // been added to one yet throws. Generation runs before that for the lobby, and after it
            // for a MUL export, so both orders reach here.
            if (member.getGame() != null) {
                member.setC3Master(master, false);
            }
            slaves++;
        }
        LOGGER.debug("[C3Network] '{}': C3 master '{}' with {} slave(s)",
              formation.parseName(), master.getShortName(), slaves);
        return slaves;
    }

    /**
     * Joins every C3i-capable unit in the formation into one network, up to the node limit.
     *
     * <p>Runs whether or not the formation was flagged for a network: a C3i unit that happens to be
     * present is worth connecting, and a ComStar or Word of Blake Level II is six units, which is
     * exactly {@link Entity#MAX_C3i_NODES}.</p>
     *
     * @return how many units joined the network
     */
    private static int linkC3i(ForceDescriptor formation, List<Entity> members) {
        Entity firstNode = null;
        int nodes = 0;
        for (Entity member : members) {
            if (nodes >= Entity.MAX_C3i_NODES) {
                break;
            }
            if (!member.hasC3i()) {
                continue;
            }
            member.setC3UUID();
            if (firstNode == null) {
                member.setC3NetIdSelf();
                firstNode = member;
            } else {
                member.setC3NetId(firstNode);
                int freeSlot = member.getFreeC3iUUID();
                if (freeSlot >= 0) {
                    member.setC3iNextUUIDAsString(freeSlot, firstNode.getC3UUIDAsString());
                }
            }
            nodes++;
        }
        if (nodes > 1) {
            LOGGER.debug("[C3Network] '{}': C3i network of {} node(s)", formation.parseName(), nodes);
        }
        return (nodes > 1) ? nodes : 0;
    }
}
