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
import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Keeps a command built from several rolls to one "Naval Units" branch.
 *
 * <p>The transport stage attaches the ships it generates to the force it generated them for. When rolls are
 * accumulated into one command, each roll arrives with its own naval branch, so a vehicle company rolled on top of
 * a Mek battalion lists its carrier under "Vehicle Company" rather than beside the battalion's DropShips, and it
 * looks as though the carrier never came. This moves every roll's ships into a single branch under the command's
 * top node: the first roll's branch is kept, and later rolls' ships are filed into its categories - WarShips,
 * JumpShips, Troopships - by name, with a category created when the branch has none of that kind yet.</p>
 *
 * <p>Ships keep their own nodes, so a carrier keeps the fighter complement nested under it. The flotillas each roll
 * brought are kept as they were rather than re-dealt, so the branch reads as the ships each roll added.</p>
 */
public final class TransportBranchMerger {

    private static final MMLogger LOGGER = MMLogger.create(TransportBranchMerger.class);

    private TransportBranchMerger() {
    }

    /**
     * Folds the ships a roll brought into the command's naval branch, and keeps that branch at the top.
     *
     * @param modelTop the top node of the accumulated command; the roll is already merged beneath it
     * @param roll     the roll just merged, whose own naval branch is to be folded in; {@code null} is ignored
     */
    public static void foldInto(ForceDescriptor modelTop, @Nullable ForceDescriptor roll) {
        if (roll == null) {
            return;
        }
        // The roll's own branch comes off first, so the search below finds only what the command had before.
        ForceDescriptor rollBranch = detachTransportRoot(roll);
        ForceDescriptor commandBranch = findTransportRoot(modelTop);
        if (commandBranch == null) {
            commandBranch = rollBranch;
        } else if (rollBranch != null) {
            int moved = mergeCategories(rollBranch, commandBranch);
            LOGGER.debug("[ForceGen][Naval] folded {} ship group(s) from '{}' into the command's naval branch",
                  moved, roll.parseName());
        }
        if (commandBranch == null) {
            return;
        }
        // The branch belongs at the top of the command, whichever roll it came in with; a later roll of a higher
        // echelon becomes the new top, and the branch moves up with it.
        detach(commandBranch);
        modelTop.addAttached(commandBranch);
    }

    /**
     * Moves every ship group the roll's branch holds into the command's branch, category by category.
     *
     * @return how many ship groups were moved
     */
    private static int mergeCategories(ForceDescriptor rollBranch, ForceDescriptor commandBranch) {
        int moved = 0;
        for (ForceDescriptor category : new ArrayList<>(rollBranch.getSubForces())) {
            ForceDescriptor target = categoryNamed(commandBranch, category.parseName());
            if (target == null) {
                detach(category);
                commandBranch.addSubForce(category);
                moved += category.getSubForces().size() + category.getAttached().size();
                continue;
            }
            for (ForceDescriptor group : new ArrayList<>(category.getSubForces())) {
                detach(group);
                target.addSubForce(group);
                moved++;
            }
            for (ForceDescriptor group : new ArrayList<>(category.getAttached())) {
                detach(group);
                target.addAttached(group);
                moved++;
            }
        }
        return moved;
    }

    private static @Nullable ForceDescriptor categoryNamed(ForceDescriptor branch, String name) {
        for (ForceDescriptor category : branch.getSubForces()) {
            if (Objects.equals(category.parseName(), name)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Removes the transport branch attached directly to the roll, if it has one.
     *
     * @return the branch, no longer attached to anything, or {@code null} when the roll brought no ships
     */
    private static @Nullable ForceDescriptor detachTransportRoot(ForceDescriptor roll) {
        for (ForceDescriptor attached : new ArrayList<>(roll.getAttached())) {
            if (attached.isTransportRoot()) {
                detach(attached);
                return attached;
            }
        }
        return null;
    }

    /**
     * @return the first transport branch anywhere in the tree, or {@code null} when there is none
     */
    private static @Nullable ForceDescriptor findTransportRoot(ForceDescriptor node) {
        List<ForceDescriptor> children = new ArrayList<>(node.getAttached());
        children.addAll(node.getSubForces());
        for (ForceDescriptor child : children) {
            if (child.isTransportRoot()) {
                return child;
            }
            ForceDescriptor found = findTransportRoot(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Takes a node out of its parent's lists, leaving it free to be attached elsewhere. */
    private static void detach(ForceDescriptor node) {
        ForceDescriptor parent = node.getParent();
        if (parent != null) {
            parent.getAttached().remove(node);
            parent.getSubForces().remove(node);
        }
        node.setParent(null);
    }
}
