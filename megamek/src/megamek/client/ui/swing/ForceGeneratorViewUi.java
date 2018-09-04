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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import megamek.client.Client;
import megamek.client.RandomSkillsGenerator;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.IGame.Phase;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.UnitType;

/**
 * Presents controls for selecting parameters of the force to generate and a tree structure showing
 * the generated force. The left and right sides of the view are made available separately for use by
 * RandomArmyDialog.
 * 
 * @author Neoancient
 *
 */

public class ForceGeneratorViewUi {
	
	private JPanel leftPanel;
    private JPanel rightPanel;
	
	private ForceGeneratorOptionsView panControls;
	private JLabel lblOrganization;
	private JLabel lblFaction;
	private JLabel lblRating;
	private JScrollPane paneForceTree;
	private JTree forceTree;
	
	private JTable tblChosen;
	private ChosenEntityModel modelChosen;
	
	ClientGUI clientGui;
	
	public ForceGeneratorViewUi(ClientGUI gui) {
		clientGui = gui;
		initUi();
	}

	private void initUi() {
		panControls = new ForceGeneratorOptionsView(clientGui, fd -> setGeneratedForce(fd));
		
		rightPanel = new JPanel();
		rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.organization")), gbc);
		lblOrganization = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 0;
		rightPanel.add(lblOrganization, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc);
		lblFaction = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 1;
		rightPanel.add(lblFaction, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc);
		lblRating = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 2;
		rightPanel.add(lblRating, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		paneForceTree = new JScrollPane();
		paneForceTree.setViewportView(forceTree);
		paneForceTree.setPreferredSize(new Dimension(600, 800));
		paneForceTree.setMinimumSize(new Dimension(600, 800));
		rightPanel.add(paneForceTree, gbc);		
		
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
					rightPanel.setMinimumSize(new Dimension(forceTree.getMinimumSize().width, rightPanel.getMinimumSize().height));
					rightPanel.setPreferredSize(new Dimension(forceTree.getPreferredSize().width, rightPanel.getPreferredSize().height));
				}
				rightPanel.revalidate();
			}
		});
		forceTree.addMouseListener(treeMouseListener);
		
		rightPanel = new JPanel(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.organization")), gbc);
		lblOrganization = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 0;
		rightPanel.add(lblOrganization, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc);
		lblFaction = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 1;
		rightPanel.add(lblFaction, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc);
		lblRating = new JLabel();
		gbc.gridx = 1;
		gbc.gridy = 2;
		rightPanel.add(lblRating, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		paneForceTree = new JScrollPane();
		paneForceTree.setViewportView(forceTree);
		paneForceTree.setPreferredSize(new Dimension(600, 800));
		paneForceTree.setMinimumSize(new Dimension(600, 800));
		rightPanel.add(paneForceTree, gbc);
		
		modelChosen = new ChosenEntityModel();
		tblChosen = new JTable(modelChosen);
		tblChosen.setIntercellSpacing(new Dimension(0, 0));
		tblChosen.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(tblChosen);
        scroll.setBorder(BorderFactory.createTitledBorder(Messages.getString("RandomArmyDialog.Army")));
        tblChosen.addMouseListener(tableMouseListener);
        tblChosen.addKeyListener(tableKeyListener);
        
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(panControls);
        leftPanel.add(scroll);
	}
	
	public Component getLeftPanel() {
	    return new JScrollPane(leftPanel);
	}
	
	public Component getRightPanel() {
	    return rightPanel;
	}
	
	public void setYear(int year) {
	    panControls.setCurrentYear(year);
	}
	
	/**
	 * Adds the chosen units to the game
	 */
	public void addChosenUnits(String playerName) {
        if ((null != forceTree.getModel().getRoot())
                && (forceTree.getModel().getRoot() instanceof ForceDescriptor)) {
            configureNetworks((ForceDescriptor) forceTree.getModel().getRoot());
        }
        
	    List<Entity> entities = new ArrayList<Entity>(
	            modelChosen.allEntities().size());
	    Client c = null;
	    if (null != playerName) {
	        c = clientGui.getBots().get(playerName);
	    }
	    if (null == c) {
	        c = clientGui.getClient();
	    }
        for (Entity e : modelChosen.allEntities()) {
            e.setOwner(c.getLocalPlayer());
            if (c.getGame().getPhase() != Phase.PHASE_LOUNGE){
                e.setDeployRound(c.getGame().getRoundCount()+1);
                e.setGame(c.getGame());
                // Set these to true, otherwise units reinforced in
                // the movement turn are considered selectable
                e.setDone(true);
                e.setUnloaded(true);
            }
            entities.add(e);
        }
        c.sendAddEntity(entities);
        
        modelChosen.clearData();
	}
	
	private void configureNetworks(ForceDescriptor fd) {
	    if (fd.getFlags().contains("c3")) {
	        Entity master = fd.getSubforces().stream().map(ForceDescriptor::getEntity)
	                .filter(en -> modelChosen.hasEntity(en)
	                        && (en.hasC3M() || en.hasC3MM()))
	                .findFirst().orElse(null);
	        if (null != master) {
	            master.setC3UUID();
	            int c3s = 0;
	            for (ForceDescriptor sf : fd.getSubforces()) {
	                if (modelChosen.hasEntity(sf.getEntity())
	                        && !sf.getEntity().getExternalIdAsString().equals(master.getExternalIdAsString())
	                        && sf.getEntity().hasC3S()) {
	                    sf.getEntity().setC3UUID();
	                    sf.getEntity().setC3MasterIsUUIDAsString(master.getC3UUIDAsString());
	                    c3s++;
	                    if (c3s == 3) {
	                        break;
	                    }
	                }
	            }
	        }
	    } else {
            // Even if we haven't reworked this into a full C3i network, we can still connect
            // any C3i units that happen to be present.
	        String netId = null;
	        int nodes = 0;
	        for (ForceDescriptor sf : fd.getSubforces()) {
	            if (modelChosen.hasEntity(sf.getEntity())
	                    && sf.getEntity().hasC3i()) {
	                sf.getEntity().setC3UUID();
	                if (null == netId) {
	                    netId = sf.getEntity().getC3UUIDAsString();
	                    nodes++;
	                } else {
	                    int pos = sf.getEntity().getFreeC3iUUID();
	                    if (pos >= 0) {
	                        sf.getEntity().setC3iNextUUIDAsString(pos, netId);
	                        nodes++;
	                    }
	                }
	            }
	            if (nodes >= Entity.MAX_C3i_NODES) {
	                break;
	            }
	        }
	    }
        fd.getSubforces().forEach(sf -> configureNetworks(sf));
        fd.getAttached().forEach(sf -> configureNetworks(sf));
	}
	
	private void setGeneratedForce(ForceDescriptor fd) {
		forceTree.setModel(new ForceTreeModel(fd));

		if (null != fd) {
    		lblOrganization.setText(Ruleset.findRuleset(fd).getEschelonNames(fd.getUnitType() == null? "" : UnitType.getTypeName(fd.getUnitType())).get(fd.getEschelonCode()));
    		lblFaction.setText(RATGenerator.getInstance().getFaction(fd.getFaction()).getName(fd.getYear()));
    		lblRating.setText(RandomSkillsGenerator.getLevelDisplayableName(fd.getExperience())
    				+ ((fd.getRating() == null)?"":"/" + fd.getRating()));
		} else {
            lblOrganization.setText("");
            lblFaction.setText("");
            lblRating.setText("");
		}
				
	}
	
    private MouseListener treeMouseListener = new MouseAdapter() {

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
                    final ForceDescriptor fd = (ForceDescriptor) node;
                    JPopupMenu menu = new JPopupMenu();
                    
                    JMenuItem item = new JMenuItem("Add to game");
                    item.addActionListener(ev -> modelChosen.addEntities(fd));
                    menu.add(item);
                    
                    item = new JMenuItem("Export as MUL");
                    item.addActionListener(ev -> panControls.exportMUL(fd));
                    menu.add(item);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    };

    private MouseListener tableMouseListener = new MouseAdapter() {

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
                if (tblChosen.getSelectedRowCount() > 0) {
                    JPopupMenu menu = new JPopupMenu();
                    
                    JMenuItem item = new JMenuItem("Remove");
                    item.addActionListener(ev -> modelChosen.removeEntities(tblChosen.getSelectedRows()));
                    menu.add(item);
                    
                    menu.show(e.getComponent(), e.getX(), e.getY());
                    
                }
            }
        }
        
    };
    
    private KeyListener tableKeyListener = new KeyListener() {

        @Override
        public void keyTyped(KeyEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void keyPressed(KeyEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_DELETE)
                    && (tblChosen.getSelectedRowCount() > 0)) {
                modelChosen.removeEntities(tblChosen.getSelectedRows());
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
                    try {
                    	clientGui.loadPreviewImage(this, fd.getEntity(),
                    			clientGui.getClient().getLocalPlayer());
                    } catch (NullPointerException ex) {
                        DefaultMmLogger.getInstance().log(getClass(),
                                "getTreeCellRendererComponent(JTree, Object, boolean, boolean, boolean, int, boolean)",
                                LogLevel.WARNING, "No image found for " + fd.getEntity().getShortNameRaw());
                    }
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
    
    private static class ChosenEntityModel extends AbstractTableModel {
        
        private static final long serialVersionUID = 779497693159590878L;
        
        public static final int COL_ENTITY = 0;
        public static final int COL_BV     = 1;
        public static final int COL_MOVE   = 2;
        public static final int NUM_COLS   = 3;
        
        private List<Entity> entities = new ArrayList<>();
        private Set<String> entityIds = new HashSet<>();
        
        public boolean hasEntity(Entity en) {
            return (null != en) && entityIds.contains(en.getExternalIdAsString());
        }
        
        public void addEntity(Entity en) {
            if (!entityIds.contains(en.getExternalIdAsString())) {
                entities.add(en);
                entityIds.add(en.getExternalIdAsString());
            }
            fireTableDataChanged();
        }
        
        public void clearData() {
            entityIds.clear();
            entities.clear();
            fireTableDataChanged();
        }

        public void removeEntities(int[] selectedRows) {
            for (int r : selectedRows) {
                if ((r >= 0) && (r < entities.size())) {
                    entityIds.remove(entities.get(r).getExternalIdAsString());
                }
            }
            List<Entity> newList = entities.stream().filter(e -> entityIds.contains(e.getExternalIdAsString()))
                    .collect(Collectors.toList());
            entities = newList;
            fireTableDataChanged();
        }

        public void addEntities(ForceDescriptor fd) {
            if (fd.isElement()) {
                if (fd.getEntity() != null) {
                    addEntity(fd.getEntity());
                }
            }
            fd.getSubforces().stream().forEach(sf -> addEntities(sf));
            fd.getAttached().stream().forEach(sf -> addEntities(sf));
            
        }
        
        public List<Entity> allEntities() {
            return entities;
        }

        @Override
        public int getRowCount() {
            return entities.size();
        }

        @Override
        public int getColumnCount() {
            return NUM_COLS;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final Entity en = entities.get(rowIndex);
            switch (columnIndex) {
                case COL_ENTITY:
                    return en.getShortNameRaw();
                case COL_BV:
                    return en.calculateBattleValue();
                case COL_MOVE:
                    return en.getWalkMP() + "/" + en.getRunMPasString() + "/" + en.getJumpMP();
            }
            return "";
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case (COL_ENTITY):
                    return Messages.getString("RandomArmyDialog.colUnit");
                case (COL_MOVE):
                    return Messages.getString("RandomArmyDialog.colMove");
                case (COL_BV):
                    return Messages.getString("RandomArmyDialog.colBV");
            }
            return "??";
        }

    }
}
