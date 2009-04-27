/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.AmmoType;
import megamek.common.BipedMech;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EquipmentMode;
import megamek.common.HexTarget;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Mech;
import megamek.common.MinefieldTarget;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.BAVibroClawAttackAction;
import megamek.common.actions.BreakGrappleAttackAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.common.weapons.CLFireExtinguisher;
import megamek.common.weapons.ISFireExtinguisher;

/**
 * Context menu for the board.
 */
public class MapMenu extends JPopupMenu implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 2879345079968414986L;

    private Coords coords;
    private IGame game;
    private Component currentPanel;
    private IBoard board;
    private Client client;
    private ClientGUI gui;
    private Entity selectedEntity;
    private Entity myEntity;
    private Targetable myTarget = null;
    private boolean hasMenu = false;

    public MapMenu(Coords coords, Client client, Component panel, ClientGUI gui) {
        this.coords = coords;
        this.game = client.game;
        this.currentPanel = panel;
        this.board = client.getBoard();
        this.client = client;
        this.gui = gui;
        this.selectedEntity = this.myEntity = game.getEntity(gui.getSelectedEntityNum());

        hasMenu = createMenu();
    }

    private boolean canSelectEntities() {
        return client.isMyTurn() && (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof MovementDisplay || currentPanel instanceof TargetingPhaseDisplay);
    }

    private boolean canTargetEntities() {
        return client.isMyTurn() && (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof TargetingPhaseDisplay);
    }

    private boolean createMenu() {
        this.removeAll();
        int itemCount = 0;
        JMenu menu = createSelectMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
            itemCount++;
        }

        menu = createViewMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
            itemCount++;
        }

        if (client.isMyTurn() && myEntity != null) {
            selectTarget();

            menu = createTargetMenu();
            if (menu.getItemCount() > 0) {
                this.add(menu);
                itemCount++;
            }

            if (currentPanel instanceof MovementDisplay) {
                menu = createMovementMenu(myEntity.getPosition().equals(coords));

                if (itemCount > 0) {
                    this.addSeparator();
                    itemCount++;
                }

                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }

                menu = createTurnMenu();

                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }

                menu = createStandMenu();

                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }

                menu = createPhysicalMenu(true);

                if (menu.getItemCount() > 0) {
                    this.addSeparator();
                    this.add(menu);
                    itemCount++;
                }

            } else if (currentPanel instanceof FiringDisplay && client.isMyTurn()) {

                if (itemCount > 0) {
                    this.addSeparator();
                    itemCount++;
                }

                menu = createWeaponsFireMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }

                menu = createModeMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }

                menu = createTorsoTwistMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }
            } else if (currentPanel instanceof PhysicalDisplay && client.isMyTurn()) {
                menu = createPhysicalMenu(false);

                if (menu.getItemCount() > 0) {
                    this.addSeparator();
                    this.add(menu);
                    itemCount++;
                }

            }
        }
        return itemCount > 0;
    }

    private JMenuItem TargetMenuItem(Targetable t) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.targetMenuItem") + t.getDisplayName());

        String targetCode = "";

        if (t instanceof Entity) {
            targetCode = "E|" + ((Entity) t).getId();
        } else if (t instanceof BuildingTarget) {
            targetCode = "B|" + t.getPosition().x + "|" + t.getPosition().y + "|" + (t.getTargetType() == Targetable.TYPE_BLDG_IGNITE ? true : false);
        } else if (t instanceof MinefieldTarget) {
            targetCode = "M|" + t.getPosition().x + "|" + t.getPosition().y;
        } else {
            targetCode = "H|" + t.getPosition().x + "|" + t.getPosition().y + "|" + t.getTargetType();
        }

        item.setActionCommand(targetCode);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                myTarget = decodeTargetInfo(e.getActionCommand());
                if (currentPanel instanceof FiringDisplay) {
                    ((FiringDisplay) currentPanel).target(myTarget);
                } else if (currentPanel instanceof PhysicalDisplay) {
                    ((PhysicalDisplay) currentPanel).target(myTarget);
                } else if (currentPanel instanceof TargetingPhaseDisplay) {
                    ((TargetingPhaseDisplay) currentPanel).target(myTarget);
                }
            }
        });

        return item;
    }

    private JMenuItem createChargeMenuItem() {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butCharge"));

        if (!client.game.getEntities(coords).hasMoreElements()) {
            return null;
        }
        item.setActionCommand(MovementDisplay.MOVE_CHARGE);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotCourse(e);
            }
        });

        return item;
    }

    private JMenuItem createDFAJMenuItem() {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butDfa"));

        if (!client.game.getEntities(coords).hasMoreElements()) {
            return null;
        }
        item.setActionCommand(MovementDisplay.MOVE_DFA);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotCourse(e);
            }
        });

        return item;
    }

    private JMenuItem SelectJMenuItem(Entity en) {

        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.selectMenuItem") + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Entity en = game.getEntity(Integer.parseInt(e.getActionCommand()));
                    selectedEntity = en;
                    if (currentPanel instanceof MovementDisplay) {
                        ((MovementDisplay) currentPanel).selectEntity(selectedEntity.getId());
                    } else if (currentPanel instanceof FiringDisplay) {
                        ((FiringDisplay) currentPanel).selectEntity(selectedEntity.getId());
                    } else if (currentPanel instanceof PhysicalDisplay) {
                        ((PhysicalDisplay) currentPanel).selectEntity(selectedEntity.getId());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem ViewJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.viewMenuItem") + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    selectedEntity = game.getEntity(Integer.parseInt(e.getActionCommand()));
                    gui.setDisplayVisible(true);
                    gui.mechD.displayEntity(selectedEntity);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenu createSelectMenu() {
        JMenu menu = new JMenu("Select");
        // add select options
        if (canSelectEntities()) {
            for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = i.nextElement();
                if (client.getMyTurn().isValidEntity(entity, client.game)) {
                    menu.add(SelectJMenuItem(entity));
                }
            }
        }

        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
            final Entity entity = i.nextElement();
            menu.add(ViewJMenuItem(entity));
        }

        return menu;

    }

    private JMenu createMovementMenu(boolean entityInHex) {
        JMenu menu = new JMenu("Movement");

        if (myEntity == null) {
            return menu;
        }

        if (entityInHex) {
            JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butWalk"));

            item.setActionCommand(MovementDisplay.MOVE_WALK);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        ((MovementDisplay) currentPanel).actionPerformed(e);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.butBackup"));

            item.setActionCommand(MovementDisplay.MOVE_BACK_UP);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        ((MovementDisplay) currentPanel).actionPerformed(e);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            menu.add(item);

            if (myEntity.getJumpMP() > 0) {
                item = new JMenuItem(Messages.getString("CommonMenuBar.moveJump"));

                item.setActionCommand(MovementDisplay.MOVE_JUMP);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ((MovementDisplay) currentPanel).actionPerformed(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                menu.add(item);
            }

            if (game.getOptions().booleanOption("tacops_evade")) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butEvade"));

                item.setActionCommand(MovementDisplay.MOVE_EVADE);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ((MovementDisplay) currentPanel).actionPerformed(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                menu.add(item);
            }

            if (game.getPlanetaryConditions().isRecklessConditions() && !game.getBoard().inSpace()) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butReckless"));

                item.setActionCommand(MovementDisplay.MOVE_RECKLESS);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ((MovementDisplay) currentPanel).actionPerformed(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                menu.add(item);
            }

        } else {

            JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butWalk"));

            item.setActionCommand(MovementDisplay.MOVE_WALK);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        plotCourse(e);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.butBackup"));

            item.setActionCommand(MovementDisplay.MOVE_BACK_UP);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        plotCourse(e);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            menu.add(item);

            if (myEntity.getJumpMP() > 0) {
                item = new JMenuItem(Messages.getString("CommonMenuBar.moveJump"));

                item.setActionCommand(MovementDisplay.MOVE_JUMP);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            plotCourse(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                menu.add(item);
            }

            if (game.getOptions().booleanOption("tacops_evade")) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butEvade"));

                item.setActionCommand(MovementDisplay.MOVE_EVADE);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            plotCourse(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                menu.add(item);
            }

            if (game.getPlanetaryConditions().isRecklessConditions() && !game.getBoard().inSpace()) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butReckless"));

                item.setActionCommand(MovementDisplay.MOVE_RECKLESS);
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            plotCourse(e);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                menu.add(item);
            }
        }

        return menu;
    }

    private JMenu createTurnMenu() {
        JMenu menu = new JMenu(Messages.getString("MovementDisplay.butTurn"));

        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butTurnRight"));

        item.setActionCommand(MovementDisplay.MOVE_TURN_RIGHT);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(item);

        item = new JMenuItem(Messages.getString("MovementDisplay.butTurnLeft"));

        item.setActionCommand(MovementDisplay.MOVE_TURN_LEFT);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(item);

        item = new JMenuItem("About Face");

        item.setActionCommand(MovementDisplay.MOVE_TURN_RIGHT);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(item);

        return menu;
    }

    private JMenu createWeaponsFireMenu() {
        JMenu menu = new JMenu("Weapons");

        /*
         * if ( myTarget == null || (myTarget instanceof Entity &&
         * !myEntity.isEnemyOf((Entity)myTarget)) ){ return menu; }
         */
        menu.add(createFireJMenuItem());
        menu.add(createSkipJMenuItem());
        menu.add(createAlphaStrikeJMenuItem());
        
        if ( myEntity.canFlipArms() ) {
            menu.add(createFlipArmsJMenuItem());
        }

        return menu;
    }

    private JMenuItem createSkipJMenuItem() {
        JMenuItem item = new JMenuItem("Skip");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((FiringDisplay) currentPanel).nextWeapon();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createAlphaStrikeJMenuItem() {
        JMenuItem item = new JMenuItem("Alpha Strike");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    FiringDisplay panel = (FiringDisplay) currentPanel;
                    int Weapons = myEntity.getWeaponList().size();
                    // Energy Weapons
                    for (int pos = 0; pos < Weapons; pos++) {
                        int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
                        Mounted mounted = myEntity.getEquipment(weaponNum);

                        if (mounted.getType().hasFlag(WeaponType.F_ENERGY) && mounted.usedInPhase() == IGame.Phase.PHASE_UNKNOWN) {
                            panel.fire();
                        } else {
                            panel.nextWeapon();
                        }
                    }
                    // Ballistic Weapons
                    for (int pos = 0; pos < Weapons; pos++) {
                        int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
                        Mounted mounted = myEntity.getEquipment(weaponNum);

                        if (mounted.getType().hasFlag(WeaponType.F_BALLISTIC) && mounted.usedInPhase() == IGame.Phase.PHASE_UNKNOWN) {
                            panel.fire();
                        } else {
                            panel.nextWeapon();
                        }
                    }
                    // Missile Weapons
                    for (int pos = 0; pos < Weapons; pos++) {
                        int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
                        Mounted mounted = myEntity.getEquipment(weaponNum);

                        if (mounted.usedInPhase() == IGame.Phase.PHASE_UNKNOWN) {
                            panel.fire();
                        } else {
                            panel.nextWeapon();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createFlipArmsJMenuItem() {
        JMenuItem item = new JMenuItem("Flip Arms");

        item.setActionCommand(Integer.toString(myEntity.getId()));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    FiringDisplay display = (FiringDisplay)currentPanel;
                    
                    int id = Integer.parseInt(e.getActionCommand());
                    display.updateFlipArms(!game.getEntity(id).getArmsFlipped());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createFireJMenuItem() {
        JMenuItem item = new JMenuItem("Fire");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((FiringDisplay) currentPanel).fire();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenu createPhysicalMenu(boolean isMovementPhase) {
        JMenu menu = new JMenu("Physicals");

        if (isMovementPhase) {
            if (myEntity.canCharge()) {

                JMenuItem item = createChargeMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            if (myEntity.canDFA()) {
                JMenuItem item = createDFAJMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }
        } else {

            JMenuItem item = null;

            if (!myEntity.isHullDown() && !myEntity.isProne() && !myEntity.hasHipCrit()) {
                item = createKickJMenuItem();

                if (item != null) {
                    menu.add(item);
                }

                item = createTripJMenuItem();

                if (item != null) {
                    menu.add(item);
                }

            }

            if (myEntity instanceof BipedMech 
                    && (!myEntity.isLocationBad(Mech.LOC_LARM) 
                    || !myEntity.isLocationBad(Mech.LOC_RARM)) ) {
                item = createPunchJMenuItem();

                if (item != null) {
                    menu.add(item);
                }

            }

            if (myEntity instanceof BipedMech 
                    && !myEntity.isLocationBad(Mech.LOC_LARM) && !myEntity.isLocationBad(Mech.LOC_RARM) ) {
                item = createPushJMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            if (myEntity.getJumpMP() > 0) {
                item = createJumpJetAttackJMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            if (myEntity.isProne()) {
                item = createThrashJMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            item = createDodgeJMenuItem();

            if (item != null) {
                menu.add(item);
            }

            if (myEntity.getClubs().size() > 0) {

                JMenu clubMenu = createClubMenu();

                if (clubMenu.getItemCount() > 0) {
                    menu.add(clubMenu);
                }
            }

            ToHitData grap = GrappleAttackAction.toHit(client.game, myEntity.getId(), myTarget);
            ToHitData bgrap = BreakGrappleAttackAction.toHit(client.game, myEntity.getId(), myTarget);
            if (grap.getValue() != TargetRoll.IMPOSSIBLE || bgrap.getValue() != TargetRoll.IMPOSSIBLE) {

                item = createGrappleJMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }
            if (myTarget != null) {
                ToHitData vibro = BAVibroClawAttackAction.toHit(client.game, myEntity.getId(), myTarget);
                if (vibro.getValue() != TargetRoll.IMPOSSIBLE) {
                    item = createVibroClawMenuItem();
                    if (item != null) {
                        menu.add(item);
                    }
                }
            }

        }

        return menu;
    }

    public void actionPerformed(ActionEvent arg0) {
    }

    private JMenu createStandMenu() {
        JMenu menu = new JMenu();

        if (selectedEntity.isProne()) {
            menu.setText("Stand");
            menu.add(createStandJMenuItem(false));

            if (game.getOptions().booleanOption("tacops_careful_stand") && myEntity.getWalkMP() > 2 && myEntity.moved < 1) {
                menu.add(createStandJMenuItem(true));
            }

            if (game.getOptions().booleanOption("tacops_hull_down")) {
                menu.add(createHullDownJMenuItem());
            }

        } else if (selectedEntity.isHullDown()) {
            menu.setText("Stand");
            menu.add(createStandJMenuItem(false));

            if (game.getOptions().booleanOption("tacops_careful_stand")) {
                menu.add(createStandJMenuItem(true));
            }

            menu.add(createProneJMenuItem());
        } else {
            menu.setText("Prone");
            menu.add(createProneJMenuItem());

            if (game.getOptions().booleanOption("tacops_hull_down")) {
                menu.add(createHullDownJMenuItem());
            }
        }

        return menu;
    }

    private JMenuItem createStandJMenuItem(boolean carefulStand) {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butUp"));

        if (carefulStand) {
            item.setText("Careful Stand");
            item.setActionCommand(MovementDisplay.MOVE_CAREFUL_STAND);
        } else {
            item.setActionCommand(MovementDisplay.MOVE_GET_UP);
        }
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createHullDownJMenuItem() {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butHullDown"));

        item.setActionCommand(MovementDisplay.MOVE_HULL_DOWN);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createProneJMenuItem() {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butDown"));

        item.setActionCommand(MovementDisplay.MOVE_GO_PRONE);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenu createTargetMenu() {
        JMenu menu = new JMenu("Target");

        // add target options
        if (canTargetEntities()) {
            for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = i.nextElement();
                menu.add(TargetMenuItem(entity));
            }
            // Can target weapons at the hex if it contains woods or building.
            // Can target physical attacks at the hex if it contains building.
            if (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof TargetingPhaseDisplay) {
                IHex h = board.getHex(coords);
                if (h != null && currentPanel instanceof FiringDisplay && !board.inSpace() && !board.inAtmosphere()) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_CLEAR)));
                    if (client.game.getOptions().booleanOption("tacops_start_fire") && (h.containsTerrain(Terrains.WOODS) || h.containsTerrain(Terrains.JUNGLE) || h.containsTerrain(Terrains.FIELDS) || hasMunitionType(AmmoType.M_INFERNO) || hasMunitionType(AmmoType.M_INFERNO_IV) || hasMunitionType(AmmoType.M_THUNDER_INFERNO))) { //$NON-NLS-1$
                        menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_IGNITE)));
                    }
                } if (h != null && h.containsTerrain(Terrains.FUEL_TANK)) {
                    menu.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
                    if (client.game.getOptions().booleanOption("tacops_start_fire")) { //$NON-NLS-1$
                        menu.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
                    }
                } if (h != null && h.containsTerrain(Terrains.BUILDING)) {
                    menu.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
                    if (client.game.getOptions().booleanOption("tacops_start_fire")) { //$NON-NLS-1$
                        menu.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
                    }
                }
                if (h != null && currentPanel instanceof FiringDisplay) {
                    if (board.inSpace() && hasAmmoType(AmmoType.T_SCREEN_LAUNCHER)) {
                        menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_SCREEN)));
                    } else {
                        if ( (hasAmmoType(AmmoType.T_LRM) || hasAmmoType(AmmoType.T_MML)) && (hasMunitionType(AmmoType.M_FASCAM) || hasMunitionType(AmmoType.M_THUNDER) || hasMunitionType(AmmoType.M_THUNDER_ACTIVE) || hasMunitionType(AmmoType.M_THUNDER_AUGMENTED) || hasMunitionType(AmmoType.M_THUNDER_INFERNO) || hasMunitionType(AmmoType.M_THUNDER_VIBRABOMB))) {
                            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_MINEFIELD_DELIVER)));
                        }

                        if (hasMunitionType(AmmoType.M_FLARE)) {
                            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_FLARE_DELIVER)));
                        }

                        if (hasAmmoType(AmmoType.T_BA_MICRO_BOMB)) {
                            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_BOMB)));
                        }

                        if (hasAmmoType(AmmoType.T_ARROW_IV) || hasAmmoType(AmmoType.T_SNIPER) || hasAmmoType(AmmoType.T_CRUISE_MISSILE) || hasAmmoType(AmmoType.T_ALAMO) || hasAmmoType(AmmoType.T_KILLER_WHALE) || hasAmmoType(AmmoType.T_LONG_TOM) || hasAmmoType(AmmoType.T_THUMPER)) {
                            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_ARTILLERY)));
                        }
                        if (client.game.getOptions().booleanOption("tacops_start_fire") && h.containsTerrain(Terrains.FIRE) && hasFireExtinguisher()) {
                            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_EXTINGUISH)));
                        }
                    }
                }
                if (h != null && currentPanel instanceof TargetingPhaseDisplay && !board.inSpace() && !board.inAtmosphere() && (hasAmmoType(AmmoType.T_ARROW_IV) || hasAmmoType(AmmoType.T_SNIPER) || hasAmmoType(AmmoType.T_CRUISE_MISSILE) || hasAmmoType(AmmoType.T_ALAMO) || hasAmmoType(AmmoType.T_KILLER_WHALE) || hasAmmoType(AmmoType.T_LONG_TOM) || hasAmmoType(AmmoType.T_THUMPER))) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_ARTILLERY)));
                }
            }
        }
        return menu;
    }

    private void plotCourse(ActionEvent e) {
        ((MovementDisplay) currentPanel).actionPerformed(e);

        // Drag
        ((BoardView1) gui.bv).mouseAction(coords, 3, 16);
        // Click
        ((BoardView1) gui.bv).mouseAction(coords, 1, 16);
    }

    private Targetable decodeTargetInfo(String info) {

        StringTokenizer target = new StringTokenizer(info, "|");
        String type = target.nextToken();

        if (type.equalsIgnoreCase("E")) {
            return game.getEntity(Integer.parseInt(target.nextToken()));
        }

        Coords coords = new Coords(Integer.parseInt(target.nextToken()), Integer.parseInt(target.nextToken()));

        if (type.equals("B")) {
            return new BuildingTarget(coords, board, Boolean.parseBoolean(target.nextToken()));
        }

        if (type.equals("M")) {
            return new MinefieldTarget(coords, board);
        }

        return new HexTarget(coords, board, Integer.parseInt(target.nextToken()));
    }

    private boolean hasAmmoType(int ammoType) {

        if (myEntity.getAmmo().size() < 1)
            return false;

        for (Mounted ammo : myEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == ammoType) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMunitionType(long munition) {

        if (myEntity.getAmmo().size() < 1)
            return false;

        for (Mounted ammo : myEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getMunitionType() == munition) {
                return true;
            }
        }

        return false;
    }

    private boolean hasFireExtinguisher() {

        if (myEntity.getWeaponList().size() < 1) {
            return false;
        }

        for (Mounted weapon : myEntity.getWeaponList()) {
            if ((WeaponType) weapon.getType() instanceof ISFireExtinguisher || (WeaponType) weapon.getType() instanceof CLFireExtinguisher) {
                return true;
            }
        }

        return false;

    }

    private JMenuItem createTorsoTwistJMenuItem(int direction) {
        JMenuItem item = new JMenuItem();

        if (direction == 1) {
            item.setText("Right");
        } else {
            item.setText("Left");
        }

        item.setActionCommand(Integer.toString(direction));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int direction = Integer.parseInt(e.getActionCommand());
                    ((FiringDisplay) currentPanel).torsoTwist(direction);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createTorsoTwistJMenuItem(Coords coords) {
        JMenuItem item = new JMenuItem("Twist");

        item.setActionCommand(coords.x + "|" + coords.y);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {

                    StringTokenizer result = new StringTokenizer(e.getActionCommand(), "|");
                    Coords coord = new Coords(Integer.parseInt(result.nextToken()), Integer.parseInt(result.nextToken()));
                    ((FiringDisplay) currentPanel).torsoTwist(coord);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenu createTorsoTwistMenu() {
        JMenu menu = new JMenu();

        if (myEntity instanceof BipedMech) {
            menu.setText("Torso Twist");
            if (coords.equals(myEntity.getPosition())) {
                menu.add(createTorsoTwistJMenuItem(1));
                menu.add(createTorsoTwistJMenuItem(0));
            } else {
                menu.add(createTorsoTwistJMenuItem(coords));
            }
        } else if (myEntity instanceof Tank && ((Tank) myEntity).getInternal(Tank.LOC_TURRET) > -1) {
            menu.setText("Turret Twist");
            if (coords.equals(myEntity.getPosition())) {
                menu.add(createTorsoTwistJMenuItem(1));
                menu.add(createTorsoTwistJMenuItem(0));
            } else {
                menu.add(createTorsoTwistJMenuItem(coords));
            }
        }

        return menu;
    }

    private void selectTarget() {
        Vector<Entity> list = new Vector<Entity>();

        for (Entity en : game.getEntitiesVector(coords)) {
            if (en.isEnemyOf(myEntity) || (game.getOptions().booleanOption("friendly_fire") && !en.equals(myEntity))) {
                list.add(en);
            }
        }

        if (list.size() == 1) {
            myTarget = selectedEntity = list.firstElement();

            //gui.bv.centerOnHex(myTarget.getPosition());
           // gui.getBoardView().select(myTarget.getPosition());

            if (currentPanel instanceof FiringDisplay) {
                FiringDisplay panel = (FiringDisplay) currentPanel; 
                panel.target(myTarget);
            } else if (currentPanel instanceof PhysicalDisplay) {
                ((PhysicalDisplay) currentPanel).target(myTarget);
            } else if (currentPanel instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) currentPanel).target(myTarget);
            }

        }
    }

    private JMenu createModeMenu() {
        JMenu menu = new JMenu("Modes");

        int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = myEntity.getEquipment(weaponNum);

        if (mounted != null && mounted.getType().hasModes()) {
            for (int pos = 0; pos < mounted.getType().getModesCount(); pos++) {
                menu.add(createModeJMenuItem(mounted, pos));
            }
        }

        return menu;
    }

    private JMenuItem createModeJMenuItem(Mounted mounted, int position) {
        JMenuItem item = new JMenuItem();

        EquipmentMode mode = mounted.getType().getMode(position);

        if (mode.equals(mounted.curMode())) {
            item.setText("* " + mode.getDisplayableName());
        } else {
            item.setText(mode.getDisplayableName());
        }

        item.setActionCommand(Integer.toString(position));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int position = Integer.parseInt(e.getActionCommand());
                    int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
                    Mounted mounted = myEntity.getEquipment(weaponNum);
                    mounted.setMode(position);
                    client.sendModeChange(myEntity.getId(), weaponNum, position);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return item;

    }

    private JMenuItem createPunchJMenuItem() {
        JMenuItem item = new JMenuItem("Punch");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).punch();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createKickJMenuItem() {
        JMenuItem item = new JMenuItem("Kick");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).kick();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createPushJMenuItem() {
        JMenuItem item = new JMenuItem("Push");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).push();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }
    
    private JMenuItem createVibroClawMenuItem() {
        JMenuItem item = new JMenuItem("Vibro Claw Attack");
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).vibroclawatt();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item; 
    }

    private JMenuItem createJumpJetAttackJMenuItem() {
        JMenuItem item = new JMenuItem("Jump Jet Attack");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).jumpjetatt();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createThrashJMenuItem() {
        JMenuItem item = new JMenuItem("Thrash");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).thrash();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createGrappleJMenuItem() {
        JMenuItem item = new JMenuItem("Grapple");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).doGrapple();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createTripJMenuItem() {
        JMenuItem item = new JMenuItem("Trip");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).trip();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createDodgeJMenuItem() {
        JMenuItem item = new JMenuItem("Dodge");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ((PhysicalDisplay) currentPanel).dodge();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenu createClubMenu() {
        JMenu menu = new JMenu("Weapon");

        for (int pos = 0; pos < myEntity.getClubs().size(); pos++) {
            Mounted club = myEntity.getClubs().get(pos);
            if (!club.isDestroyed()) {
                menu.add(createClubJMenuItem(club.getName(), pos));
            }
        }
        return menu;
    }

    private JMenuItem createClubJMenuItem(String clubName, int clubNumber) {
        JMenuItem item = new JMenuItem(clubName);

        item.setActionCommand(Integer.toString(clubNumber));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Mounted club = myEntity.getClubs().get(Integer.parseInt(e.getActionCommand()));
                    ((PhysicalDisplay) currentPanel).club(club);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }
    
    public void show(Component comp, int x, int y){
        if (client.isMyTurn() && myEntity != null) {
            selectTarget();
        }
        super.show(comp, x, y);
    }

    public boolean getHasMenu() {
        return hasMenu;
    }

}