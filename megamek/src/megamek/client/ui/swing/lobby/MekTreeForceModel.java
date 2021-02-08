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

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiC3Color;
import static megamek.client.ui.swing.util.UIUtil.uiGray;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import megamek.client.ui.swing.lobby.MekTreeC3Model.MekTreeC3Renderer;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.force.*;
import megamek.common.icons.Camouflage;

public class MekTreeForceModel extends DefaultTreeModel {

    private static final long serialVersionUID = -6458173460367645667L;
    
    private Forces forces;
    
    public MekTreeForceModel() {
        super(new DefaultMutableTreeNode("Root"));
    }
    
    public void setForces(Forces f) {
        forces = f;
    }
    
    @Override
    public Object getChild(Object parent, int index) {
        if (index < 0) {
            return null;
        }
        if (parent == root) {
            List<Force> toplevel = forces.getTopLevelForces();
            if (index < toplevel.size()) {
                return toplevel.get(index); //TODO Sort correctly
            }
            if (index < toplevel.size() + forces.forcelessEntities().size()) {
                return forces.forcelessEntities().get(index - toplevel.size());
            }
        } else if (parent instanceof Force) {
            Force pnt = (Force) parent;
            if (index < pnt.entityCount()) {
                return forces.getEntity(pnt.getEntityId(index));
            } else if (index < pnt.getChildCount()) {
                return forces.getForce(pnt.getSubForceId(index - pnt.entityCount()));
            } 
        } 
        return null;
    }
    
    public void refreshData() {
        nodeStructureChanged(root);
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == root) {
            return forces.getTopLevelForceCount() + forces.forcelessEntities().size();
        } else if (parent instanceof Force) {
            Force pnt = (Force) parent;
            return pnt.getChildCount(); 
        } else { // Entity
            return 0;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof Entity; 
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null || child == root 
                || !(parent instanceof Force) 
                || !((child instanceof Force) || (child instanceof Entity))) {
            return -1;
        }
        Force pnt = (Force) parent;
        if (child instanceof Entity) {
            Entity entity = (Entity) child;
            return pnt.entityIndex(entity);
        } else {
            Force subForce = (Force) child;
            return pnt.subForceIndex(subForce);
        }
    }

    /** A specialized renderer for the mek C3 tree. */
    public class MekForceTreeRenderer extends DefaultTreeCellRenderer {
        
        private static final long serialVersionUID = -2002064111324279609L;
        
        private Color bgColor;
        private final Color TRANSPARENT = new Color(0,0,0,0);
        private int width = 10;
        private ChatLounge lobby;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            
            if (value instanceof Entity) {
                Entity entity = (Entity) value;
                setText(MekTreeC3CellFormatter.formatUnitCompact(entity));
                bgColor = TRANSPARENT;
                
                int size = UIUtil.scaleForGUI(40);
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

            } else if (value instanceof Force) {
                StringBuilder result = new StringBuilder("<HTML><NOBR>&nbsp;&nbsp;");
                result.append(guiScaledFontHTML(uiC3Color()));
                result.append(((Force) value).getName());
                result.append(guiScaledFontHTML(uiGray()));
                result.append("[").append(((Force) value).getId()).append("]");
                setText( result.toString());
                bgColor = new Color(150, 150, 150, 150);
                setIcon(null);
            }
            
            width = tree.getWidth();
            return this;
        }
        
        private MekForceTreeRenderer(ChatLounge cl) {
            lobby = cl;
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

    public TreeCellRenderer MekForceTreeRenderer(ChatLounge cl) {
        return new MekForceTreeRenderer(cl);
    }
    
    
    


}
