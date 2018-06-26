/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import megamek.client.RandomSkillsGenerator;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ui.Messages;
import megamek.common.UnitType;

/**
 * Presents controls for selecting parameters of the force to generate and a tree structure showing
 * the generated force.
 * 
 * @author Neoancient
 *
 */

public class ForceGeneratorDialog extends JDialog {
	
	private static final long serialVersionUID = 6855878459680509594L;
	
	private ForceGeneratorView panControls;
	private JPanel panForce;
	private JLabel lblOrganization;
	private JLabel lblFaction;
	private JLabel lblRating;
	private JScrollPane paneForceTree;
	private JTree forceTree;
	
	ClientGUI clientGui;
	
	public ForceGeneratorDialog(ClientGUI gui) {
		super(gui.frame, Messages.getString("ForceGeneratorDialog.title"), true);
		clientGui = gui;
		initUi();
	}

	private void initUi() {
		panControls = new ForceGeneratorView(clientGui, fd -> setGeneratedForce(fd));
		
		panForce = new JPanel();
		panForce = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		panForce.add(new JLabel(Messages.getString("ForceGeneratorDialog.organization")), gbc);
		lblOrganization = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 0;
		panForce.add(lblOrganization, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		panForce.add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc);
		lblFaction = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 1;
		panForce.add(lblFaction, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		panForce.add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc);
		lblRating = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 2;
		panForce.add(lblRating, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		paneForceTree = new JScrollPane();
		paneForceTree.setViewportView(forceTree);
		paneForceTree.setPreferredSize(new Dimension(600, 800));
		paneForceTree.setMinimumSize(new Dimension(600, 800));
		panForce.add(paneForceTree, gbc);		
		
		forceTree = new JTree(new ForceTreeModel(null));
		forceTree.setCellRenderer(new UnitRenderer());
		forceTree.setRowHeight(80);
		forceTree.setVisibleRowCount(12);
		forceTree.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeCollapsed(TreeExpansionEvent arg0) {
			}

			@Override
			public void treeExpanded(TreeExpansionEvent arg0) {
				if (forceTree.getPreferredSize().getWidth() > paneForceTree.getSize().getWidth()) {
					panForce.setMinimumSize(new Dimension(forceTree.getMinimumSize().width, panForce.getMinimumSize().height));
					panForce.setPreferredSize(new Dimension(forceTree.getPreferredSize().width, panForce.getPreferredSize().height));
				}
				panForce.revalidate();
			}
		});
		forceTree.addMouseListener(mouseListener);
		
		panForce = new JPanel(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		panForce.add(new JLabel(Messages.getString("ForceGeneratorDialog.organization")), gbc);
		lblOrganization = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 0;
		panForce.add(lblOrganization, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		panForce.add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc);
		lblFaction = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 1;
		panForce.add(lblFaction, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		panForce.add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc);
		lblRating = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 2;
		panForce.add(lblRating, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		paneForceTree = new JScrollPane();
		paneForceTree.setViewportView(forceTree);
		paneForceTree.setPreferredSize(new Dimension(600, 800));
		paneForceTree.setMinimumSize(new Dimension(600, 800));
		panForce.add(paneForceTree, gbc);

		JSplitPane panSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panControls, panForce);
		panSplit.setOneTouchExpandable(true);
		panSplit.setResizeWeight(1.0);

		add(panSplit);
		pack();
	}
	
	private void setGeneratedForce(ForceDescriptor fd) {
		forceTree.setModel(new ForceTreeModel(fd));
		
		lblOrganization.setText(Ruleset.findRuleset(fd).getEschelonNames(fd.getUnitType() == null? "" : UnitType.getTypeName(fd.getUnitType())).get(fd.getEschelonCode()));
		lblFaction.setText(RATGenerator.getInstance().getFaction(fd.getFaction()).getName(fd.getYear()));
		lblRating.setText(RandomSkillsGenerator.getLevelDisplayableName(fd.getExperience()) + "/"
				+ ((fd.getRating() == null)?"":"/" + fd.getRating()));
				
	}
	
	private MouseListener mouseListener = new MouseAdapter() {

        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }
        
        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                TreePath path = forceTree.getPathForLocation(e.getX(), e.getY());
                Object node = path.getLastPathComponent();
                if (node instanceof ForceDescriptor) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem item = new JMenuItem("Export as MUL");
                    item.addActionListener(ev -> panControls.exportMUL((ForceDescriptor) node));
                    menu.add(item);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
	    
	};

    static class ForceTreeModel implements TreeModel {
    	
    	private ForceDescriptor root;
    	private ArrayList<TreeModelListener> listeners;
    	
    	public ForceTreeModel(ForceDescriptor root) {
    		this.root = root;
    		listeners = new ArrayList<TreeModelListener>();		
    	}

    	@Override
    	public void addTreeModelListener(TreeModelListener listener) {
    		if (null != listener && !listeners.contains(listener)) {
    			listeners.add(listener);
    		}
    	}

    	@Override
    	public Object getChild(Object parent, int index) {
    		if (parent instanceof ForceDescriptor) {
    			return ((ForceDescriptor)parent).getAllChildren().get(index);
    		}
    		return null;
    	}

    	@Override
    	public int getChildCount(Object parent) {
    		if (parent instanceof ForceDescriptor) {
    			return ((ForceDescriptor)parent).getAllChildren().size();
    		}
    		return 0;
    	}

    	@Override
    	public int getIndexOfChild(Object parent, Object child) {
    		if (parent instanceof ForceDescriptor) {
    			return ((ForceDescriptor)parent).getAllChildren().indexOf(child);
    		}
    		return 0;
    	}

    	@Override
    	public Object getRoot() {
    		return root;
    	}

    	@Override
    	public boolean isLeaf(Object node) {
    	    return (getChildCount(node) == 0)
    	            || ((node instanceof ForceDescriptor)
    	                    && (((ForceDescriptor) node).getEschelon() != null)
    	                    && (((ForceDescriptor)node).getEschelon() == 0));
    	}

    	@Override
    	public void removeTreeModelListener(TreeModelListener listener) {
    		if (null != listener) {
    			listeners.remove(listener);
    		}
    	}

    	@Override
    	public void valueForPathChanged(TreePath arg0, Object arg1) {
    		// TODO Auto-generated method stub

    	}

    }
    
    class UnitRenderer extends DefaultTreeCellRenderer {
    	/**
    	 * 
    	 */
    	private static final long serialVersionUID = -5915350078441133119L;
    	
    	private MechTileset mt;
    	
    	public UnitRenderer() {
            mt = new MechTileset(new File("data/images/units"));
            try {
                mt.loadFromFile("mechset.txt");
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
    	}

        @Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);
            setOpaque(true);
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            if(sel) {
                setBackground(Color.DARK_GRAY);
                setForeground(Color.WHITE);
            }

            ForceDescriptor fd = (ForceDescriptor)value;
            if(fd.isElement()) {
                StringBuilder name = new StringBuilder();
                String uname = "";
                if(fd.getCo() == null) {
                    name.append("<font color='red'>")
                        .append(Messages.getString("ForceGeneratorDialog.noCrew"))
                        .append("</font>");
                } else {
                    name.append(fd.getCo().getName());
                    name.append(" (").append(fd.getCo().getGunnery()).append("/").append(fd.getCo().getPiloting()).append(")");
                }
                uname = "<i>" + fd.getModelName() + "</i>";
                if (fd.getFluffName() != null) {
                	uname += "<br /><i>" + fd.getFluffName() + "</i>";
                }
                setText("<html>" + name.toString() + ", " + uname + "</html>");
                if (fd.getEntity() != null) {
                	clientGui.loadPreviewImage(this, fd.getEntity(),
                			clientGui.getClient().getLocalPlayer());
                }
            } else {
            	StringBuilder desc = new StringBuilder("<html>");
            	desc.append(fd.parseName()).append("<br />").append(fd.getDescription());
            	if (fd.getCo() != null) {
            		desc.append("<br />").append(fd.getCo().getTitle() == null?"CO: ":fd.getCo().getTitle());
            		desc.append(fd.getCo().getName());
            	}
            	if (fd.getXo() != null) {
            		desc.append("<br />").append(fd.getXo().getTitle() == null?"XO: ":fd.getXo().getTitle());
            		desc.append(fd.getXo().getName());
            	}
           		setText(desc.append("</html>").toString());
            }
            return this;
        }
    }    
}
