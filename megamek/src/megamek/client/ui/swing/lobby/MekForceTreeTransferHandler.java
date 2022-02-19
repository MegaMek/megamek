/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.lobby;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import megamek.common.Entity;
import megamek.common.force.Force;
import org.apache.logging.log4j.LogManager;

/** 
 * The TransferHandler manages drag-and-drop for the C3 tree. 
 * Partly taken from https://coderanch.com/t/346509/java/JTree-drag-drop-tree-Java
 */
public class MekForceTreeTransferHandler extends TransferHandler {

    private static final long serialVersionUID = -2872981855792727691L;
    private static final DataFlavor FLAVOR = DataFlavor.stringFlavor;
    
    private final ChatLounge lobby;

    public MekForceTreeTransferHandler(ChatLounge cl, MekTreeForceModel model) {
        lobby = cl;
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
            for (TreePath path: selection) {
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
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        // no destination means drop below the last tree element -> promote/remove from force
        if (dest == null || dest.getLastPathComponent() == null) {
            return true;
        }
        
        try {
            String source = (String) support.getTransferable().getTransferData(FLAVOR);
            List<Integer> entityIdList = getselectedEntityIds(source);
            List<Integer> forceIdList = getselectedForceIds(source);
            // Only drag either entities or forces
            if (!entityIdList.isEmpty() && !forceIdList.isEmpty()) {
                return false;
            }
            
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
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
            List<Integer> entityIdList = getselectedEntityIds(source);
            List<Integer> forceIdList = getselectedForceIds(source);
            StringTokenizer outer = new StringTokenizer(source, ":");
            String entityIds = outer.nextToken();
            
            //TODO: call the LobbyMekPopupActions? Like FATTACH|2|5
            
            // Cannot drag both entities and forces and cannot drag none
            if ((!entityIdList.isEmpty() && !forceIdList.isEmpty())
                || (entityIdList.isEmpty() && forceIdList.isEmpty())) {
                return false;
            }

            // Get drop location info.
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            TreePath dest = dl.getPath();

            // No Path means remove entity from forces or promote force to top-level
            if (dest == null && !entityIdList.isEmpty()) {
                lobby.lobbyActions.forceRemoveEntity(LobbyUtility.getEntities(lobby.game(), entityIds));
                return true;
            }
            if (dest == null && !forceIdList.isEmpty()) {
                lobby.lobbyActions.forcePromote(forceIdList);
                return true;
            }

            // Add entities to a force (Drop onto force)
            if (dest != null && dest.getLastPathComponent() instanceof Force && !entityIdList.isEmpty()) {
                int forceId = ((Force) dest.getLastPathComponent()).getId();
                lobby.lobbyActions.forceAddEntity(LobbyUtility.getEntities(lobby.game(), entityIds), forceId);
            }
            
            // Add entities to a force (Drop onto entities in a force)
            if (dest != null && dest.getLastPathComponent() instanceof Entity && !entityIdList.isEmpty()) {
                int forceId = ((Entity) dest.getLastPathComponent()).getForceId();
                if (forceId != Force.NO_FORCE) {
                    lobby.lobbyActions.forceAddEntity(LobbyUtility.getEntities(lobby.game(), entityIds), forceId);
                } else {
                    lobby.lobbyActions.forceRemoveEntity(LobbyUtility.getEntities(lobby.game(), entityIds));
                }
            }
            
            // Attach a force to a new parent
            if (dest != null && dest.getLastPathComponent() instanceof Force && forceIdList.size() == 1) {
                int newParentId = ((Force) dest.getLastPathComponent()).getId();
                lobby.lobbyActions.forceAttach(forceIdList.get(0), newParentId);
            }
            
            
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return false;
        }
        return false;
    }
    
    private List<Integer> getselectedForceIds(String source) {
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
    
    private List<Integer> getselectedEntityIds(String source) {
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
    
    private class StringTransferable implements Transferable {

        private final DataFlavor[] supported = { DataFlavor.stringFlavor };
        private String selectionIds;

        StringTransferable(String str) {
            this.selectionIds = str;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
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

