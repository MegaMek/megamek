/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.forceDisplay;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.IDisplayedUnit;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.lobby.LobbyUtility;
import megamek.client.ui.swing.tileset.TilesetManager;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.event.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Shows force display
 */
public class PlainForceDisplayPanel extends JPanel implements GameListener, IPreferenceChangeListener {
    private static final MMLogger logger = MMLogger.create(PlainForceDisplayPanel.class);
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    JTree forceTree;
    private ForceDisplayMekTreeModel forceTreeModel;
    private final Client client;
    private final Game game;
    private final JFrame parentFrame;

    private final BoardView boardViewRef;
    private final IDisplayedUnit unitDisplayRef;
    private final TilesetManager tilesetManager;

    public PlainForceDisplayPanel(JFrame parentFrame, Game game, Client client, TilesetManager tilesetManager, BoardView boardViewRef, IDisplayedUnit unitDisplayRef) {
        this.game = game;
        this.client = client;
        this.parentFrame = parentFrame;
        this.boardViewRef = boardViewRef;
        this.unitDisplayRef = unitDisplayRef;
        this.tilesetManager = tilesetManager;
        setupForce();
        refreshTree();

        setLayout(new BorderLayout());
        JScrollPane sp = new JScrollPane(forceTree);
        add(sp, BorderLayout.CENTER);

        ForceTreeMouseAdapter mekForceTreeMouseListener = new ForceTreeMouseAdapter();
        forceTree.addMouseListener(mekForceTreeMouseListener);
        game.addGameListener(this);
        GUIP.addPreferenceChangeListener(this);
    }

    private void setupForce() {
        forceTreeModel = new ForceDisplayMekTreeModel(client);
        forceTree = new JTree(forceTreeModel);
        forceTree.setRootVisible(false);
        forceTree.setDragEnabled(false);
        forceTree.setCellRenderer(new PlainForceDisplayMekTreeRenderer(client.getLocalPlayer(), tilesetManager, game));
        forceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        forceTree.setExpandsSelectedPaths(true);
        ToolTipManager.sharedInstance().registerComponent(forceTree);
    }

    /** Refreshes the Mek Tree, restoring expansion state and selection. */
    public void refreshTree() {
        if (!GUIP.getForceDisplayEnabled()) {
            return;
        }

        // Refresh the force tree and restore selection/expand status
        HashSet<Object> selections = new HashSet<>();
        if (!forceTree.isSelectionEmpty()) {
            var selectionPaths = forceTree.getSelectionPaths();
            if (selectionPaths != null) {
                for (TreePath path : selectionPaths) {
                    Object sel = path.getLastPathComponent();
                    if (sel instanceof Force || sel instanceof Entity) {
                        selections.add(path.getLastPathComponent());
                    }
                }
            }
        }

        Forces forces = game.getForces();
        forces.correct();
        List<Integer> expandedForces = new ArrayList<>();

        for (int i = 0; i < forceTree.getRowCount(); i++) {
            TreePath currPath = forceTree.getPathForRow(i);
            if (forceTree.isExpanded(currPath)) {
                Object entry = currPath.getLastPathComponent();
                if (entry instanceof Force) {
                    expandedForces.add(((Force) entry).getId());
                }
            }
        }

        forceTree.setUI(null);
        try {
            forceTreeModel.refreshData();
        } finally {
            forceTree.updateUI();
        }
        for (int id : expandedForces) {
            if (!forces.contains(id)) {
                continue;
            }
            forceTree.expandPath(getPath(forces.getForce(id)));
        }

        forceTree.clearSelection();
        for (Object sel : selections) {
            forceTree.addSelectionPath(getPath(sel));
        }
    }

    /**
     * Returns a TreePath in the force tree for a possibly outdated entity
     * or force. Outdated means a new object of the type was sent by the server
     * and has replaced this object. Also works for the game's current objects
     * though.
     * Uses the force's/entity's id to get the
     * game's real object with the same id. Used to reconstruct the selection
     * and expansion state of the force tree after an update.
     */
    private TreePath getPath(Object outdatedEntry) {
        Forces forces = game.getForces();
        if (outdatedEntry instanceof Force) {
            if (!forces.contains((Force) outdatedEntry)) {
                return null;
            }
            int forceId = ((Force) outdatedEntry).getId();
            List<Force> chain = forces.forceChain(forces.getForce(forceId));
            Object[] pathObjs = new Object[chain.size() + 1];
            int index = 0;
            pathObjs[index++] = forceTreeModel.getRoot();
            for (Force force : chain) {
                pathObjs[index++] = force;
            }
            return new TreePath(pathObjs);
        } else if (outdatedEntry instanceof Entity) {
            int entityId = ((Entity) outdatedEntry).getId();
            if (game.getEntity(entityId) == null) {
                return null;
            }
            List<Force> chain = forces.forceChain(game.getEntity(entityId));
            Object[] pathObjs = new Object[chain.size() + 2];
            int index = 0;
            pathObjs[index++] = forceTreeModel.getRoot();
            for (Force force : chain) {
                pathObjs[index++] = force;
            }
            pathObjs[index++] = game.getEntity(entityId);
            return new TreePath(pathObjs);
        } else {
            throw new IllegalArgumentException(Messages.getString("ChatLounge.TreePath.methodRequiresEntityForce"));
        }
    }

    private JMenuItem viewReadoutJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.viewReadoutMenuItem")
                + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(evt -> {
            try {
                Entity entity = game.getEntity(Integer.parseInt(evt.getActionCommand()));
                LobbyUtility.mekReadout(entity, 0, false, parentFrame);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    public class ForceTreeMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int row = forceTree.getRowForLocation(e.getX(), e.getY());
                TreePath path = forceTree.getPathForRow(row);
                if (path != null && path.getLastPathComponent() instanceof Entity entity) {
                    if (unitDisplayRef != null) {
                        unitDisplayRef.setDisplayedUnit(entity);
                        GUIP.setUnitDisplayEnabled(true);
                    }
                    if (boardViewRef != null && entity.isDeployed() && !entity.isOffBoard() && entity.getPosition() != null) {
                        boardViewRef.centerOnHex(entity.getPosition());
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // If the right mouse button is pressed over an unselected entity,
                // clear the selection and select that entity instead
                int row = forceTree.getRowForLocation(e.getX(), e.getY());
                if (!forceTree.isRowSelected(row)) {
                    forceTree.setSelectionRow(row);
                }
                showPopup(e);
            }
        }

        /** Shows the right-click menu on the mek table */
        private void showPopup(MouseEvent e) {
            int row = forceTree.getRowForLocation(e.getX(), e.getY());
            TreePath path = forceTree.getPathForRow(row);
            if (path != null && path.getLastPathComponent() instanceof Entity entity) {
                ScalingPopup popup = new ScalingPopup();
                popup.add(viewReadoutJMenuItem(entity));
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent e) {
        // noaction default
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
        // noaction default
    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        // noaction default
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
        // noaction default
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        refreshTree();
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        refreshTree();
    }

    @Override
    public void gameReport(GameReportEvent e) {
        // noaction default
    }

    @Override
    public void gameEnd(GameEndEvent e) {
        // noaction default
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
        // noaction default
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        // noaction default
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
        // noaction default
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
        // noaction default
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // noaction default
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
        // noaction default
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // noaction default
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        // noaction default
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
        // noaction default
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
        // noaction default
    }

    @Override
    public void gameVictory(PostGameResolution e) {
        // noaction default
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.FORCE_DISPLAY_ENABLED)) {
            refreshTree();
        }

        forceTree.setBackground(GUIP.getUnitToolTipBGColor());
    }
}
