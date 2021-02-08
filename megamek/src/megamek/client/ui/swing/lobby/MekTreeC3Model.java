/*  
 * MegaMek - Copyright (C) 2021 - The MegaMek Team  
 *  
 * This program is free software; you can redistribute it and/or modify it under  
 * the terms of the GNU General Public License as published by the Free Software  
 * Foundation; either version 2 of the License, or (at your option) any later  
 * version.  
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT  
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
 * details.  
 */ 
package megamek.client.ui.swing.lobby;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.icons.Camouflage;

public class MekTreeC3Model extends DefaultTreeModel {

    private static final long serialVersionUID = 8874552241751416734L;
    
    private static final int MEKTREE_IMGHEIGHT = 35;
    
    private IGame game;
    protected EventListenerList listenerList = new EventListenerList();
    List<Entity> c3Entities = new ArrayList<>();
    List<String> c3Nets = new ArrayList<>();
    List<String> connectedC3Nets = new ArrayList<>();
    List<Entity> unconnectedC3Entities = new ArrayList<>();
    MekTreeC3TransferHandler transferHandler;
    ChatLounge lobby;

    public MekTreeC3Model(IGame g, ChatLounge cl) {
        super(new DefaultMutableTreeNode("Root"));
        game = g;
        lobby = cl;
        transferHandler = new MekTreeC3TransferHandler(lobby);
    }
    
    public TransferHandler getTransferHandler()  {
        return transferHandler;
    }
    
    public void refreshData(Collection<Entity> entities) {
        c3Entities = entities.stream().filter(e -> e.hasAnyC3System()).collect(Collectors.toList());
        c3Nets = entities.stream().map(e -> e.getC3NetId()).filter(s -> s != null).collect(Collectors.toList());
        Set<String> distinctC3Nets = new HashSet<String>(c3Nets);
        distinctC3Nets.removeIf(s -> Collections.frequency(c3Nets, s) == 1);
        connectedC3Nets = new ArrayList<String>(distinctC3Nets);
        unconnectedC3Entities = c3Entities.stream().filter(e -> !connectedC3Nets.contains(e.getC3NetId())).collect(Collectors.toList());
        nodeStructureChanged(root);
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (index < 0) {
            return null;
        }
        if (parent == root) {
            if (index < connectedC3Nets.size()) {
                return connectedC3Nets.get(index);
            } else {
                return unconnectedC3Entities.get(index - connectedC3Nets.size());
            }
        } else if (parent instanceof String) {
            List<Entity> entities;
            if (((String) parent).contains("C3i") || ((String) parent).contains("NC3")) {
                entities = c3Entities.stream()
                        .filter(e -> parent.equals(e.getC3NetId()))
                        .collect(Collectors.toList());
            } else {
                entities = c3Entities.stream()
                        .filter(e -> parent.equals(e.getC3NetId()))
                        .filter(e -> e.hasC3M() || e.hasC3MM())
                        .collect(Collectors.toList());
            }
            return entities.get(index);
        } else if (parent instanceof Entity) {
            Entity entity = (Entity) parent;
            if (entity.hasC3M() || entity.hasC3MM()) {
                Vector<Entity> members = game.getC3SubNetworkMembers(entity);
                members.remove(entity);
                return members.get(index);
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == root) {
            return connectedC3Nets.size() + unconnectedC3Entities.size();
            
        } else if (parent instanceof String) { // Network
            if (((String) parent).contains("C3i") || ((String) parent).contains("NC3")) {
                return (int) c3Entities.stream().filter(e -> parent.equals(e.getC3NetId())).count();
            } else {
                return (int) c3Entities.stream()
                        .filter(e -> parent.equals(e.getC3NetId()))
                        .filter(e -> e.hasC3M() || e.hasC3MM())
                        .count();
            }
            
        } else { // Entity
            Entity entity = (Entity) parent;
            if (entity.hasC3M() || entity.hasC3MM()) {
                return game.getC3SubNetworkMembers(entity).size() - 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node == root) {
            return false;
        } else if (node instanceof String) {
            return false;
        } else if (node instanceof Entity) {
            Entity entity = (Entity) node;
            return !entity.hasC3M() && !entity.hasC3MM();
        }
        return true; 
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return -1;
        //        if (parent == null || child == null || child == root 
        //                || !(parent instanceof Force) 
        //                || !((child instanceof Force) || (child instanceof Entity))) {
        //            return -1;
        //        }
        //        Force pnt = (Force) parent;
        //        if (child instanceof Entity) {
        //            Entity entity = (Entity) child;
        //            return pnt.entityIndex(entity);
        //        } else {
        //            Force subForce = (Force) child;
        //            return pnt.subForceIndex(subForce);
        //        }
    }
    
    
    //
    // Drag and Drop
    //
    
    
    /** 
     * The TransferHandler manages drag-and-drop for the C3 tree. 
     * Partly taken from https://coderanch.com/t/346509/java/JTree-drag-drop-tree-Java
     */
    private class MekTreeC3TransferHandler extends TransferHandler {

        private static final long serialVersionUID = -8554631524997824381L;
        
        public final DataFlavor flavor = DataFlavor.stringFlavor;
        final ChatLounge lobby;
        DefaultMutableTreeNode[] nodesToRemove;
        DataFlavor[] flavors = new DataFlavor[1];

        public MekTreeC3TransferHandler(ChatLounge cl) {
            lobby = cl;
            flavors[0] = flavor;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return DnDConstants.ACTION_COPY_OR_MOVE;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath path = tree.getSelectionPath();
            if (path == null) {
                return null;
            }
            Object node = path.getLastPathComponent();
            if (node instanceof Entity) {
                return new NodesTransferable(Integer.toString(((Entity)node).getId()));
            } else if (node instanceof String) {
                return new NodesTransferable((String)node);
            }

            return null;
        }
        
        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            support.setShowDropLocation(true);
            if (!support.isDataFlavorSupported(flavor)) {
                return false;
            }
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            TreePath dest = dl.getPath();
            
            try {
                String source = (String)support.getTransferable().getTransferData(flavor);
                int entityId = getEntity(source);
                // Cannot drag and drop a network
                if (entityId == -1) {
                    return false;
                }
                // Dragging to a free space = disconnect, only for connected units
                if (dest == null) {
                    return !unconnectedC3Entities.contains(game.getEntity(entityId));
                }
                // 
                
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            
            
            return true;
        }
        
        /** Returns the entity which is dragged from the Transferable string or -1 if its a network. */
        private int getEntity(String source) {
            try {
                return Integer.parseInt(source);
            } catch (NumberFormatException n) {
                return -1;
            }
        }
        
        public class NodesTransferable implements Transferable {
            String node;

            public NodesTransferable(String source) {
              node = source;
          }

            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return node;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            public boolean isDataFlavorSupported(DataFlavor f) {
                return flavor.equals(f);
            }
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            // Extract the transfer data
            int entityID = -1;
            try {
                Transferable t = support.getTransferable();
                String node = (String)t.getTransferData(flavor);
                entityID = Integer.parseInt(node);
            } catch (NumberFormatException n) {
                // source data is a network
            } catch (Exception e) {
                System.out.println("Drag-and-Drop Error: " + e.getMessage());
            }
            // Get drop location info.
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            TreePath dest = dl.getPath();
            // No Path means remove from network, must drag an entity
            if (dest == null && entityID != -1) {
                Entity e = (Entity) (game.getEntity(entityID));
                lobby.disconnectC3FromNetwork(e);
                return true;
            }
            
            // Path and entity, target is a network
            if (dest != null && entityID != -1 && (dest.getLastPathComponent() instanceof String)) {
                Entity e = (Entity) (game.getEntity(entityID));
                String netID = (String)dest.getLastPathComponent();
                boolean isC3i = netID.contains("C3i") || netID.contains("NC3");
                netID = netID.substring(netID.indexOf(".") + 1);
                try {
                    int masterID = Integer.parseInt(netID);
                    if (isC3i) {
                        lobby.joinC3i(e, masterID);
                    } else {
                        lobby.connectToC3(e, masterID);
                    }
                    return true;
                } catch (NumberFormatException n) {
                    return false;
                }
            } 
            
            // Path and entity, target is an entity 
            if (dest != null && entityID != -1 && (dest.getLastPathComponent() instanceof Entity)) {
                Entity e = (Entity) (game.getEntity(entityID));
                Entity target = (Entity) (dest.getLastPathComponent());
                String netID = target.getC3NetId();
                boolean isC3i = netID.contains("C3i") || netID.contains("NC3");
                netID = netID.substring(netID.indexOf(".") + 1);
                try {
                    int masterID = Integer.parseInt(netID);
                    if (isC3i) {
                        lobby.joinC3i(e, masterID);
                    } else {
                        lobby.connectToC3(e, masterID);
                    }
                    return true;
                } catch (NumberFormatException n) {
                    return false;
                }
            }
            return false;
        }
    }
    
    /** A specialized renderer for the mek C3 tree. */
    public class MekTreeC3Renderer extends DefaultTreeCellRenderer {
        
        private static final long serialVersionUID = -2002064111324279609L;
        
        private Color bgColor;
        private final Color TRANSPARENT = new Color(0,0,0,0);
        private int width = 10;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            
            if (value instanceof Entity) {
                Entity entity = (Entity) value;
                setText(MekTreeC3CellFormatter.formatUnitCompact(entity));
                bgColor = TRANSPARENT;
                
                int size = UIUtil.scaleForGUI(MEKTREE_IMGHEIGHT);
                if (!lobby.isCompact()) {
                    Camouflage camo = entity.getCamouflageOrElse(entity.getOwner().getCamouflage());
                    Image icon = lobby.getClientgui().bv.getTilesetManager().loadPreviewImage(entity, camo, this);
                    setIcon(new ImageIcon(icon.getScaledInstance(-1, size, Image.SCALE_SMOOTH)));
                    setIconTextGap(UIUtil.scaleForGUI(10));
                } else {
                    Camouflage camo = entity.getCamouflageOrElse(entity.getOwner().getCamouflage());
                    Image icon = lobby.getClientgui().bv.getTilesetManager().loadPreviewImage(entity, camo, this);
                    setIcon(new ImageIcon(icon.getScaledInstance(-1, size/2, Image.SCALE_SMOOTH)));
                    setIconTextGap(UIUtil.scaleForGUI(5));
                }

            } else if (value instanceof String) {
                setText(MekTreeC3CellFormatter.formatNetworkCompact((String)value));
                bgColor = new Color(150, 150, 150, 150);
                setIcon(null);
            }
            
            width = tree.getWidth();
            return this;
        }
        
//        @Override
//        public Dimension getPreferredSize() {
//            // TODO Auto-generated method stub
//            return new Dimension(width, super.getPreferredSize().height);
//        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, TRANSPARENT, width, 0, bgColor, false);
            g2.setPaint(gp);
            g.fillRect(0, 0, width, getBounds().height);
            super.paintComponent(g);
        }
        
    }

    public TreeCellRenderer MekTreeC3Renderer() {
        return new MekTreeC3Renderer();
    }

}
