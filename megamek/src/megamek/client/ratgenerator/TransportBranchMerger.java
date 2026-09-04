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
import java.util.function.ToIntFunction;

import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Keeps a command built from several rolls to one "Naval Units" branch.
 *
 * <p>The transport stage attaches the ships it generates to the force it generated them for. When rolls are
 * accumulated into one command, each roll arrives with its own naval branch, so a vehicle company rolled on top of
 * a Mek battalion lists its carrier under "Vehicle Company" rather than beside the battalion's DropShips, and it
 * looks as though the carrier never came. This moves every roll's ships into a single branch under the command's
 * top node: the first roll's branch is kept, and later rolls' ships are filed into its categories - WarShips,
 * JumpShips, DropShips - by name, with a category created when the branch has none of that kind yet.</p>
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
            LOGGER.info("[ForceGen][Naval] folded {} ship group(s) from '{}' into the command's naval branch",
                  moved, roll.parseName());
        }
        if (commandBranch == null) {
            return;
        }
        // The branch belongs at the top of the command, whichever roll it came in with; a later roll of a higher
        // echelon becomes the new top, and the branch moves up with it.
        detach(commandBranch);
        modelTop.addAttached(commandBranch);
        // A DropShip from one roll and a JumpShip with a spare collar from another only meet here.
        int docked = dockLooseDropShips(commandBranch, TransportBranchMerger::collarsOf);
        if (docked > 0) {
            LOGGER.info("[ForceGen][Naval] docked {} DropShip(s) left loose by earlier rolls", docked);
        }
    }

    /**
     * Docks every DropShip in the branch that is not under a carrier onto a JumpShip or WarShip in the branch with a
     * collar to spare, then drops the groups left empty - a DropShips category whose only ship has just moved.
     *
     * @param branch    the command's naval branch
     * @param collarsOf how many docking collars a ship element has
     *
     * @return how many DropShips were docked
     */
    static int dockLooseDropShips(ForceDescriptor branch, ToIntFunction<ForceDescriptor> collarsOf) {
        List<ForceDescriptor> collarShips = new ArrayList<>();
        List<ForceDescriptor> looseDropShips = new ArrayList<>();
        sortShips(branch, null, collarShips, looseDropShips, collarsOf);
        int docked = 0;
        for (ForceDescriptor dropShip : looseDropShips) {
            for (ForceDescriptor ship : collarShips) {
                int freeCollars = collarsOf.applyAsInt(ship) - dockedDropShips(ship);
                if (freeCollars > 0) {
                    detach(dropShip);
                    ship.addAttached(dropShip);
                    docked++;
                    break;
                }
            }
        }
        if (docked > 0) {
            pruneEmptyGroups(branch);
        }
        return docked;
    }

    /**
     * Walks the branch sorting ship elements into those with collars and DropShips not sitting under a carrier.
     *
     * @param carrier the nearest ship element above {@code node}, or {@code null} outside any
     */
    private static void sortShips(ForceDescriptor node, @Nullable ForceDescriptor carrier,
          List<ForceDescriptor> collarShips, List<ForceDescriptor> looseDropShips,
          ToIntFunction<ForceDescriptor> collarsOf) {
        ForceDescriptor carrierForChildren = carrier;
        if (node.isElement()) {
            if (collarsOf.applyAsInt(node) > 0) {
                collarShips.add(node);
            }
            if (isDropShip(node) && (carrier == null)) {
                looseDropShips.add(node);
            }
            carrierForChildren = node;
        }
        for (ForceDescriptor child : new ArrayList<>(node.getSubForces())) {
            sortShips(child, carrierForChildren, collarShips, looseDropShips, collarsOf);
        }
        for (ForceDescriptor child : new ArrayList<>(node.getAttached())) {
            sortShips(child, carrierForChildren, collarShips, looseDropShips, collarsOf);
        }
    }

    private static boolean isDropShip(ForceDescriptor node) {
        return (node.getUnitType() != null) && (node.getUnitType() == UnitType.DROPSHIP);
    }

    private static int dockedDropShips(ForceDescriptor ship) {
        int docked = 0;
        for (ForceDescriptor attached : ship.getAttached()) {
            if (isDropShip(attached)) {
                docked++;
            }
        }
        return docked;
    }

    /** Removes every formation node under the branch that no longer holds anything, bottom up. */
    private static void pruneEmptyGroups(ForceDescriptor node) {
        for (ForceDescriptor child : new ArrayList<>(node.getSubForces())) {
            pruneEmptyGroups(child);
        }
        for (ForceDescriptor child : new ArrayList<>(node.getAttached())) {
            pruneEmptyGroups(child);
        }
        boolean isEmptyGroup = !node.isElement() && node.getSubForces().isEmpty() && node.getAttached().isEmpty();
        if (isEmptyGroup && !node.isTransportRoot()) {
            detach(node);
        }
    }

    /**
     * @return how many docking collars the ship element's design has; 0 for anything that is not a known ship
     */
    private static int collarsOf(ForceDescriptor ship) {
        if (!ship.isElement() || (ship.getModelName() == null)) {
            return 0;
        }
        MekSummary summary = MekSummaryCache.getInstance().getMek(ship.getModelName());
        return (summary == null) ? 0 : TransportCalculator.dockingCollars(summary);
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
