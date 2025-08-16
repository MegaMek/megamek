/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.forceDisplay;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.buttons.MMToggleButton;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayDialog;
import megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility;
import megamek.client.ui.util.ScalingPopup;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.event.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

/**
 * Shows force display
 */
public class ForceDisplayPanel extends JPanel implements GameListener, IPreferenceChangeListener {
    private static final MMLogger logger = MMLogger.create(ForceDisplayPanel.class);

    private ForceDisplayMekTreeModel forceTreeModel;
    JTree forceTree;
    private ClientGUI clientgui;
    private Client client;
    private Game game;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    public ForceDisplayPanel(ClientGUI clientgui) {
        this(clientgui == null ? null : clientgui.getClient());
        if (client == null) {
            return;
        }
        this.clientgui = clientgui;
    }

    public ForceDisplayPanel(Client client) {
        if (client == null) {
            return;
        }
        this.client = client;
        this.game = client.getGame();

        JPanel pnlTop = new JPanel();
        MMToggleButton btnId = new MMToggleButton(Messages.getString("ForceDisplay.Button.ID"),
              GUIP.getForceDisplayBtnID());
        MMToggleButton btnPilot = new MMToggleButton(Messages.getString("ForceDisplay.Button.Pilot"),
              GUIP.getForceDisplayBtnPilot()); // and skill
        MMToggleButton btnMP = new MMToggleButton(Messages.getString("ForceDisplay.Button.MP"),
              GUIP.getForceDisplayBtnMP());
        MMToggleButton btnHeat = new MMToggleButton(Messages.getString("ForceDisplay.Button.Heat"),
              GUIP.getForceDisplayBtnHeat());
        MMToggleButton btnWeapons = new MMToggleButton(Messages.getString("ForceDisplay.Button.Weapons"),
              GUIP.getForceDisplayBtnWeapons());
        MMToggleButton btnDmgDesc = new MMToggleButton(Messages.getString("ForceDisplay.Button.DamageDesc"),
              GUIP.getForceDisplayBtnDamageDesc());
        MMToggleButton btnArmor = new MMToggleButton(Messages.getString("ForceDisplay.Button.Armor"),
              GUIP.getForceDisplayBtnArmor());
        MMToggleButton btnTonnage = new MMToggleButton(Messages.getString("ForceDisplay.Button.Tonnage"),
              GUIP.getForceDisplayBtnTonnage());
        MMToggleButton btnRole = new MMToggleButton(Messages.getString("ForceDisplay.Button.Role"),
              GUIP.getForceDisplayBtnRole());
        MMToggleButton btnECM = new MMToggleButton(Messages.getString("ForceDisplay.Button.ECM"),
              GUIP.getForceDisplayBtnECM());
        MMToggleButton btnQuirks = new MMToggleButton(Messages.getString("ForceDisplay.Button.Quirks"),
              GUIP.getForceDisplayBtnQuirks());
        MMToggleButton btnC3 = new MMToggleButton(Messages.getString("ForceDisplay.Button.C3"),
              GUIP.getForceDisplayBtnC3());
        MMToggleButton btnMisc = new MMToggleButton(Messages.getString("ForceDisplay.Button.Misc"),
              GUIP.getForceDisplayBtnMisc());
        ActionListener toggleListener = e -> {
            if (e.getSource().equals(btnId)) {GUIP.setForceDisplayBtnID(btnId.isSelected());}
            if (e.getSource().equals(btnPilot)) {GUIP.setForceDisplayBtnPilot(btnPilot.isSelected());}
            if (e.getSource().equals(btnMP)) {GUIP.setForceDisplayBtnMP(btnMP.isSelected());}
            if (e.getSource().equals(btnHeat)) {GUIP.setForceDisplayBtnHeat(btnHeat.isSelected());}
            if (e.getSource().equals(btnWeapons)) {GUIP.setForceDisplayBtnWeapons(btnWeapons.isSelected());}
            if (e.getSource().equals(btnDmgDesc)) {GUIP.setForceDisplayBtnDamageDesc(btnDmgDesc.isSelected());}
            if (e.getSource().equals(btnArmor)) {GUIP.setForceDisplayBtnArmor(btnArmor.isSelected());}
            if (e.getSource().equals(btnTonnage)) {GUIP.setForceDisplayBtnTonnage(btnTonnage.isSelected());}
            if (e.getSource().equals(btnRole)) {GUIP.setForceDisplayBtnRole(btnRole.isSelected());}
            if (e.getSource().equals(btnECM)) {GUIP.setForceDisplayBtnECM(btnECM.isSelected());}
            if (e.getSource().equals(btnQuirks)) {GUIP.setForceDisplayBtnQuirks(btnQuirks.isSelected());}
            if (e.getSource().equals(btnC3)) {GUIP.setForceDisplayBtnC3(btnC3.isSelected());}
            if (e.getSource().equals(btnMisc)) {GUIP.setForceDisplayBtnMisc(btnMisc.isSelected());}
            refreshTree();
        };

        btnId.addActionListener(toggleListener);
        btnPilot.addActionListener(toggleListener);
        btnMP.addActionListener(toggleListener);
        btnHeat.addActionListener(toggleListener);
        btnWeapons.addActionListener(toggleListener);
        btnDmgDesc.addActionListener(toggleListener);
        btnArmor.addActionListener(toggleListener);
        btnTonnage.addActionListener(toggleListener);
        btnRole.addActionListener(toggleListener);
        btnECM.addActionListener(toggleListener);
        btnQuirks.addActionListener(toggleListener);
        btnC3.addActionListener(toggleListener);
        btnMisc.addActionListener(toggleListener);

        setupForce();
        refreshTree();

        setLayout(new BorderLayout());
        pnlTop.setLayout(new GridLayout());
        pnlTop.add(btnId);
        pnlTop.add(btnPilot);
        pnlTop.add(btnMP);
        pnlTop.add(btnHeat);
        pnlTop.add(btnWeapons);
        pnlTop.add(btnDmgDesc);
        pnlTop.add(btnArmor);
        pnlTop.add(btnTonnage);
        pnlTop.add(btnRole);
        pnlTop.add(btnECM);
        pnlTop.add(btnQuirks);
        pnlTop.add(btnC3);
        pnlTop.add(btnMisc);
        add(pnlTop, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(forceTree);
        add(sp, BorderLayout.CENTER);

        ForceTreeMouseAdapter mekForceTreeMouseListener = new ForceTreeMouseAdapter();
        forceTree.addMouseListener(mekForceTreeMouseListener);
        client.getGame().addGameListener(this);
        GUIP.addPreferenceChangeListener(this);
    }

    private void setupForce() {
        forceTreeModel = new ForceDisplayMekTreeModel(client);
        forceTree = new JTree(forceTreeModel);
        forceTree.setRootVisible(false);
        forceTree.setDragEnabled(false);
        if (clientgui != null) {
            forceTree.setCellRenderer(new ForceDisplayMekTreeRenderer(clientgui, forceTree));
        } else {
            forceTree.setCellRenderer(new ForceDisplayMekTreeRenderer(client, forceTree));
        }
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
            TreePath[] treePaths = forceTree.getSelectionPaths();

            if (treePaths != null) {
                for (TreePath path : treePaths) {
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
     * Returns a TreePath in the force tree for a possibly outdated entity or force. Outdated means a new object of the
     * type was sent by the server and has replaced this object. Also works for the game's current objects though. Uses
     * the force's/entity's id to get the game's real object with the same id. Used to reconstruct the selection and
     * expansion state of the force tree after an update.
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
                JFrame frame = null;
                if (clientgui != null) {
                    frame = clientgui.getFrame();
                } else {
                    Window windowAncestor = SwingUtilities.getWindowAncestor(this);
                    if (windowAncestor instanceof JFrame) {
                        frame = (JFrame) windowAncestor;
                    }
                }
                LobbyUtility.mekReadout(entity, 0, false, frame);
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
                    if (clientgui != null) {
                        clientgui.getUnitDisplay().displayEntity(entity);
                    } else {
                        JFrame frame = null;
                        Window windowAncestor = SwingUtilities.getWindowAncestor(ForceDisplayPanel.this);
                        if (windowAncestor instanceof JFrame) {
                            frame = (JFrame) windowAncestor;
                        }
                        UnitDisplayDialog.showEntity(frame, entity, e.isShiftDown());
                    }
                    GUIP.setUnitDisplayEnabled(true);

                    if (clientgui != null
                          && entity.isDeployed()
                          && !entity.isOffBoard()
                          && entity.getPosition() != null) {
                        clientgui.getBoardView().centerOnHex(entity.getPosition());
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
        // no action default
    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
        // no action default
    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent e) {
        // no action default
    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent e) {
        // no action default
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
        // no action default
    }

    @Override
    public void gameEnd(GameEndEvent e) {
        // no action default
    }

    @Override
    public void gameBoardNew(GameBoardNewEvent e) {
        // no action default
    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent e) {
        // no action default
    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent e) {
        // no action default
    }

    @Override
    public void gameMapQuery(GameMapQueryEvent e) {
        // no action default
    }

    @Override
    public void gameEntityNew(GameEntityNewEvent e) {
        // no action default
    }

    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent e) {
        // no action default
    }

    @Override
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // no action default
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent e) {
        // no action default
    }

    @Override
    public void gameNewAction(GameNewActionEvent e) {
        // no action default
    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent evt) {
        // no action default
    }

    @Override
    public void gameVictory(PostGameResolution e) {
        // no action default
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.FORCE_DISPLAY_ENABLED)) {
            refreshTree();
        }

        forceTree.setBackground(GUIP.getUnitToolTipBGColor());
    }
}
