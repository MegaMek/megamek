/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * The TransferHandler manages drag-and-drop for the lobby's force tree.
 * <p>
 * The tree uses {@link javax.swing.DropMode#ON_OR_INSERT}. A drop onto the middle of a row attaches a force, adds
 * units to a force or takes them out of it. A drop between two rows of the list the dragged force or unit is in
 * reorders it within that list. Every drop logs what it decided.
 * </p>
 */
public class MekForceTreeTransferHandler extends TransferHandler {
    private static final MMLogger logger = MMLogger.create(MekForceTreeTransferHandler.class);

    @Serial
    private static final long serialVersionUID = -2872981855792727691L;
    private static final DataFlavor FLAVOR = DataFlavor.stringFlavor;

    private final ChatLounge lobby;
    private final MekTreeForceModel model;
    /** The hover last written to the log, so that only a change is logged. */
    private transient String lastLoggedHover = "";

    /** What a drop does. */
    enum DropAction {
        REORDER_TOP_LEVEL,
        REORDER_SUB_FORCE,
        REORDER_UNIT,
        PROMOTE,
        REMOVE_FROM_FORCE,
        ADD_TO_FORCE,
        ATTACH,
        NONE;

        boolean isReorder() {
            return switch (this) {
                case REORDER_TOP_LEVEL, REORDER_SUB_FORCE, REORDER_UNIT -> true;
                default -> false;
            };
        }
    }

    /**
     * What a drop will do: the action, the force it is dropped onto or within (if any), the new place for a reorder,
     * and where the drop landed, in words for the log.
     */
    record DropPlan(DropAction action, @Nullable Force targetForce, int targetIndex, String where) {}

    public MekForceTreeTransferHandler(ChatLounge chatLounge, MekTreeForceModel model) {
        lobby = chatLounge;
        this.model = model;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return DnDConstants.ACTION_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] selection = tree.getSelectionPaths();
        List<String> entities = new ArrayList<>();
        List<String> forces = new ArrayList<>();

        if (selection != null) {
            for (TreePath path : selection) {
                Object selected = path.getLastPathComponent();
                if (selected instanceof Entity) {
                    entities.add("" + ((Entity) selected).getId());
                } else if (selected instanceof Force) {
                    forces.add("" + ((Force) selected).getId());
                }
            }
        }

        // Add something that the parser can find
        if (entities.isEmpty()) {
            entities.add("-1");
        }
        if (forces.isEmpty()) {
            forces.add("-1");
        }

        String result = String.join(",", entities) + ":" + String.join(",", forces);
        return new StringTransferable(result);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        support.setShowDropLocation(false);
        if (!support.isDataFlavorSupported(FLAVOR)) {
            return false;
        }

        try {
            String source = (String) support.getTransferable().getTransferData(FLAVOR);
            List<Integer> entityIdList = getSelectedEntityIds(source);
            List<Integer> forceIdList = getSelectedForceIds(source);
            // Only drag either entities or forces
            if (!entityIdList.isEmpty() && !forceIdList.isEmpty()) {
                return false;
            }
            // The insertion line is drawn only where the drop reorders
            DropPlan plan = planDrop(entityIdList, forceIdList, (JTree.DropLocation) support.getDropLocation());
            support.setShowDropLocation(plan.action().isReorder());
            logHoverIfChanged(entityIdList, forceIdList, plan);
        } catch (Exception exception) {
            logger.error(exception, "");
            return false;
        }
        return true;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        try {
            // Source info, dragged forces and entities
            String source = (String) support.getTransferable().getTransferData(FLAVOR);
            List<Integer> entityIdList = getSelectedEntityIds(source);
            List<Integer> forceIdList = getSelectedForceIds(source);
            StringTokenizer outer = new StringTokenizer(source, ":");
            String entityIds = outer.nextToken();

            // Cannot drag both entities and forces and cannot drag none
            if ((!entityIdList.isEmpty() && !forceIdList.isEmpty())
                  || (entityIdList.isEmpty() && forceIdList.isEmpty())) {
                return false;
            }

            DropPlan plan = planDrop(entityIdList, forceIdList, (JTree.DropLocation) support.getDropLocation());
            logger.info("[ForceOrder] Drop of {} {}: {}{}", describeDragged(entityIdList, forceIdList), plan.where(),
                  plan.action(), plan.action().isReorder() ? (" to place " + plan.targetIndex()) : "");

            switch (plan.action()) {
                case REORDER_TOP_LEVEL, REORDER_SUB_FORCE -> lobby.lobbyActions.forceMoveTo(
                      lobby.game().getForces().getForce(forceIdList.getFirst()), plan.targetIndex());
                case REORDER_UNIT ->
                      lobby.lobbyActions.forceMoveTo(lobby.game().getEntity(entityIdList.getFirst()), plan.targetIndex());
                case PROMOTE -> lobby.lobbyActions.forcePromote(forceIdList);
                case REMOVE_FROM_FORCE ->
                      lobby.lobbyActions.forceRemoveEntity(LobbyUtility.getEntities(lobby.game(), entityIds));
                case ADD_TO_FORCE -> lobby.lobbyActions.forceAddEntity(
                      LobbyUtility.getEntities(lobby.game(), entityIds), plan.targetForce().getId());
                case ATTACH -> lobby.lobbyActions.forceAttach(forceIdList.getFirst(), plan.targetForce().getId());
                case NONE -> {
                    return false;
                }
            }
            return true;
        } catch (Exception exception) {
            logger.error(exception, "");
            return false;
        }
    }

    /**
     * Works out what a drop does. Between two rows of the list the dragged force or unit is already in, it reorders.
     * Between top-level rows or below the last row, a sub force is promoted and units leave their force, as before.
     * Anywhere else the drop acts on the row, or the force whose rows it lands between.
     */
    private DropPlan planDrop(List<Integer> entityIdList, List<Integer> forceIdList, JTree.DropLocation location) {
        Forces forces = lobby.game().getForces();
        TreePath path = location.getPath();
        int childIndex = location.getChildIndex();
        Object target = (path == null) ? null : path.getLastPathComponent();
        boolean between = (target != null) && (childIndex != -1);
        Force draggedForce = ((forceIdList.size() == 1) && entityIdList.isEmpty())
              ? forces.getForce(forceIdList.getFirst()) : null;
        Entity draggedUnit = ((entityIdList.size() == 1) && forceIdList.isEmpty())
              ? lobby.game().getEntity(entityIdList.getFirst()) : null;

        // Below the last row, or between top-level rows
        if ((target == null) || (between && (target == model.getRoot()))) {
            String where = (target == null) ? "below the last row" : ("between top-level rows at " + childIndex);
            if ((draggedForce != null) && draggedForce.isTopLevel() && (target != null)) {
                int targetIndex = topLevelTargetIndex(shownTopLevel(), draggedForce, childIndex);
                return new DropPlan(DropAction.REORDER_TOP_LEVEL, null, targetIndex, where);
            }
            DropAction action = forceIdList.isEmpty() ? DropAction.REMOVE_FROM_FORCE : DropAction.PROMOTE;
            return new DropPlan(action, null, -1, where);
        }

        // Between two rows inside a force. A force lists its units first, then its sub forces, so a unit only
        // reorders among the unit rows and a sub force only among the sub force rows; a gap in the other block
        // would draw a line where the item cannot land.
        if (between && (target instanceof Force parent)) {
            String where = "between rows of " + parent.getName() + " at " + childIndex;
            int entityCount = parent.entityCount();
            if ((draggedUnit != null) && (draggedUnit.getForceId() == parent.getId()) && (childIndex <= entityCount)) {
                int targetIndex = placeAfterMove(childIndex, parent.entityIndex(draggedUnit));
                return new DropPlan(DropAction.REORDER_UNIT, parent, targetIndex, where);
            }
            if ((draggedForce != null) && (draggedForce.getParentId() == parent.getId())
                  && (childIndex >= entityCount)) {
                int insertIndex = Math.min(childIndex - entityCount, parent.subForceCount());
                int targetIndex = placeAfterMove(insertIndex, parent.subForceIndex(draggedForce));
                return new DropPlan(DropAction.REORDER_SUB_FORCE, parent, targetIndex, where);
            }
            return planDropOnto(parent, entityIdList, forceIdList, where + ", taken as a drop onto it");
        }

        String where;
        if (target instanceof Force force) {
            where = "onto " + force.getName();
        } else if (target instanceof Entity entity) {
            where = "onto " + entity.getShortName();
        } else {
            where = "onto " + target;
        }
        return planDropOnto(target, entityIdList, forceIdList, where);
    }

    /** Works out a drop onto a row: units join the force (or leave theirs), a single force is attached. */
    private DropPlan planDropOnto(Object target, List<Integer> entityIdList, List<Integer> forceIdList,
          String where) {
        if (target instanceof Force force) {
            if (!entityIdList.isEmpty()) {
                return new DropPlan(DropAction.ADD_TO_FORCE, force, -1, where);
            }
            if (forceIdList.size() == 1) {
                return new DropPlan(DropAction.ATTACH, force, -1, where);
            }
        } else if ((target instanceof Entity entity) && !entityIdList.isEmpty()) {
            Force force = lobby.game().getForces().getForce(entity.getForceId());
            if (force == null) {
                return new DropPlan(DropAction.REMOVE_FROM_FORCE, null, -1, where);
            }
            return new DropPlan(DropAction.ADD_TO_FORCE, force, -1, where);
        }
        return new DropPlan(DropAction.NONE, null, -1, where);
    }

    /** The top-level rows as the tree shows them: forces and units outside any force. */
    private List<Object> shownTopLevel() {
        List<Object> result = new ArrayList<>();
        Object root = model.getRoot();
        for (int index = 0; index < model.getChildCount(root); index++) {
            result.add(model.getChild(root, index));
        }
        return result;
    }

    /**
     * Returns the new place of a top-level force dropped between top-level rows: the number of its owner's other
     * top-level forces shown above the drop point. A drop among other players' forces, or among units outside any
     * force, therefore lands at the nearest end of the owner's own forces.
     *
     * @param shownTopLevel the top-level rows in the order the tree shows them
     * @param draggedForce  the dragged top-level force
     * @param insertIndex   the row the drop is inserted above, 0 meaning above the first row
     *
     * @return the place among the owner's top-level forces, 0 being the first
     */
    static int topLevelTargetIndex(List<?> shownTopLevel, Force draggedForce, int insertIndex) {
        int result = 0;
        int end = Math.min(insertIndex, shownTopLevel.size());
        for (int index = 0; index < end; index++) {
            if ((shownTopLevel.get(index) instanceof Force force) && (force != draggedForce)
                  && (force.getOwnerId() == draggedForce.getOwnerId())) {
                result++;
            }
        }
        return result;
    }

    /**
     * Returns the place an item ends up in when it is moved within its own list, given the insertion point counted
     * in that list before the move. An insertion point below the item's own place counts the item itself, so the
     * place is one less.
     *
     * @param insertIndex  the insertion point, 0 meaning above the first item
     * @param currentIndex the item's place before the move
     *
     * @return the item's place after the move, 0 being the first
     */
    static int placeAfterMove(int insertIndex, int currentIndex) {
        return (insertIndex > currentIndex) ? (insertIndex - 1) : insertIndex;
    }

    /**
     * Logs where a drag is hovering and what a drop there would do, each time that changes, so the drop zones can be
     * checked from the log while dragging.
     */
    private void logHoverIfChanged(List<Integer> entityIdList, List<Integer> forceIdList, DropPlan plan) {
        String hover = describeDragged(entityIdList, forceIdList) + " " + plan.where() + ": " + plan.action()
              + (plan.action().isReorder() ? (" to place " + plan.targetIndex() + ", line shown") : "");
        if (!hover.equals(lastLoggedHover)) {
            logger.info("[ForceOrder] Hovering {}", hover);
            lastLoggedHover = hover;
        }
    }

    /** Describes what is being dragged, for the log. */
    private String describeDragged(List<Integer> entityIdList, List<Integer> forceIdList) {
        if (forceIdList.size() == 1) {
            Force force = lobby.game().getForces().getForce(forceIdList.getFirst());
            return "force " + ((force == null) ? forceIdList.getFirst() : force.getName());
        } else if (!forceIdList.isEmpty()) {
            return forceIdList.size() + " forces";
        } else if (entityIdList.size() == 1) {
            Entity entity = lobby.game().getEntity(entityIdList.getFirst());
            return "unit " + ((entity == null) ? entityIdList.getFirst() : entity.getShortName());
        }
        return entityIdList.size() + " units";
    }

    private List<Integer> getSelectedForceIds(String source) {
        StringTokenizer outer = new StringTokenizer(source, ":");
        outer.nextToken();
        String forceIds = outer.nextToken();
        StringTokenizer forceSt = new StringTokenizer(forceIds, ",");
        List<Integer> forceIdList = new ArrayList<>();
        while (forceSt.hasMoreTokens()) {
            forceIdList.add(Integer.parseInt(forceSt.nextToken()));
        }

        // Remove token that signals no force selected
        forceIdList.remove((Integer) (-1));
        return forceIdList;
    }

    private List<Integer> getSelectedEntityIds(String source) {
        StringTokenizer outer = new StringTokenizer(source, ":");
        String entityIds = outer.nextToken();
        StringTokenizer entitySt = new StringTokenizer(entityIds, ",");
        List<Integer> entityIdList = new ArrayList<>();
        while (entitySt.hasMoreTokens()) {
            entityIdList.add(Integer.parseInt(entitySt.nextToken()));
        }
        // Remove tokens that signal no force/entity selected
        entityIdList.remove((Integer) (-1));
        return entityIdList;
    }

    private static class StringTransferable implements Transferable {

        private final DataFlavor[] supported = { DataFlavor.stringFlavor };
        private final String selectionIds;

        StringTransferable(String str) {
            this.selectionIds = str;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor);
        }

        @Override
        @Nonnull
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return selectionIds;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return supported;
        }

    }
}
