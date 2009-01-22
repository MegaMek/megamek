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

package megamek.client.ui.AWT;

import java.awt.Component;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

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
import megamek.common.options.GameOptions;
import megamek.common.weapons.CLFireExtinguisher;
import megamek.common.weapons.ISFireExtinguisher;

/**
 * Context menu for the board.
 */
public class MapMenu extends PopupMenu implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 2879345079968414986L;

    Coords coords;
    IGame game;
    Component currentPanel;
    IBoard board;
    GameOptions options;
    Client client;
    ClientGUI gui;
    Entity selectedEntity;
    Entity myEntity;
    Targetable myTarget = null;

    public MapMenu(Coords coords, Client client, Component panel, ClientGUI gui) {
        this.coords = coords;
        game = client.game;
        currentPanel = panel;
        board = client.getBoard();
        options = client.game.getOptions();
        this.client = client;
        this.gui = gui;
        selectedEntity = myEntity = game.getEntity(gui.getSelectedEntityNum());

        createMenu();
    }

    private boolean canSelectEntities() {
        return client.isMyTurn() && (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof MovementDisplay || currentPanel instanceof TargetingPhaseDisplay);
    }

    private boolean canTargetEntities() {
        return client.isMyTurn() && (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof TargetingPhaseDisplay);
    }

    private void createMenu() {
        removeAll();

        Menu menu = createSelectMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
        }

        menu = createViewMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
        }

        if (client.isMyTurn() && myEntity != null) {
            selectTarget();

            menu = createTargetMenu();
            if (menu.getItemCount() > 0) {
                this.add(menu);
            }

            if (currentPanel instanceof MovementDisplay) {
                menu = createMovementMenu(myEntity.getPosition().equals(coords));

                if (getItemCount() > 0) {
                    addSeparator();
                }

                if (menu.getItemCount() > 0) {
                    this.add(menu);
                }

                menu = createTurnMenu();

                if (menu.getItemCount() > 0) {
                    this.add(menu);
                }

                menu = createStandMenu();

                if (menu.getItemCount() > 0) {
                    this.add(menu);
                }

                menu = createPhysicalMenu(true);

                if (menu.getItemCount() > 0) {
                    addSeparator();
                    this.add(menu);
                }

            } else if (currentPanel instanceof FiringDisplay && client.isMyTurn()) {

                if (getItemCount() > 0) {
                    addSeparator();
                }

                menu = createWeaponsFireMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                }

                menu = createModeMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                }

                menu = createTorsoTwistMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                }
            } else if (currentPanel instanceof PhysicalDisplay && client.isMyTurn()) {
                menu = createPhysicalMenu(false);

                if (menu.getItemCount() > 0) {
                    addSeparator();
                    this.add(menu);
                }

            }
        }
    }

    private MenuItem TargetMenuItem(Targetable t) {
        MenuItem item = new MenuItem(Messages.getString("ClientGUI.targetMenuItem") + t.getDisplayName());

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

    private MenuItem createChargeMenuItem() {
        MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butCharge"));

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

    private MenuItem createDFAMenuItem() {
        MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butDfa"));

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

    private MenuItem SelectMenuItem(Entity en) {

        MenuItem item = new MenuItem(Messages.getString("ClientGUI.selectMenuItem") + en.getDisplayName());

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

    private MenuItem ViewMenuItem(Entity en) {
        MenuItem item = new MenuItem(Messages.getString("ClientGUI.viewMenuItem") + en.getDisplayName());

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

    private Menu createSelectMenu() {
        Menu menu = new Menu("Select");
        // add select options
        if (canSelectEntities()) {
            for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = i.nextElement();
                if (client.getMyTurn().isValidEntity(entity, client.game)) {
                    menu.add(SelectMenuItem(entity));
                }
            }
        }

        return menu;
    }

    private Menu createViewMenu() {
        Menu menu = new Menu("View");
        for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
            final Entity entity = i.nextElement();
            menu.add(ViewMenuItem(entity));
        }

        return menu;

    }

    private Menu createMovementMenu(boolean entityInHex) {
        Menu menu = new Menu("Movement");

        if (myEntity == null) {
            return menu;
        }

        if (entityInHex) {
            MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butWalk"));

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

            item = new MenuItem(Messages.getString("MovementDisplay.butBackup"));

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
                item = new MenuItem(Messages.getString("CommonMenuBar.moveJump"));

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
                item = new MenuItem(Messages.getString("MovementDisplay.butEvade"));

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
                item = new MenuItem(Messages.getString("MovementDisplay.butReckless"));

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

            MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butWalk"));

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

            item = new MenuItem(Messages.getString("MovementDisplay.butBackup"));

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
                item = new MenuItem(Messages.getString("CommonMenuBar.moveJump"));

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
                item = new MenuItem(Messages.getString("MovementDisplay.butEvade"));

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
                item = new MenuItem(Messages.getString("MovementDisplay.butReckless"));

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

    private Menu createTurnMenu() {
        Menu menu = new Menu(Messages.getString("MovementDisplay.butTurn"));

        MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butTurnRight"));

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

        item = new MenuItem(Messages.getString("MovementDisplay.butTurnLeft"));

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

        item = new MenuItem("About Face");

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

    private Menu createWeaponsFireMenu() {
        Menu menu = new Menu("Weapons");

        /*
         * if ( myTarget == null || (myTarget instanceof Entity &&
         * !myEntity.isEnemyOf((Entity)myTarget)) ){ return menu; }
         */
        menu.add(createFireMenuItem());
        menu.add(createSkipMenuItem());
        menu.add(createAlphaStrikeMenuItem());

        if ( myEntity.canFlipArms() ) {
            menu.add(createFlipArmsMenuItem());
        }

        return menu;
    }

    private MenuItem createSkipMenuItem() {
        MenuItem item = new MenuItem("Skip");
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

    private MenuItem createAlphaStrikeMenuItem() {
        MenuItem item = new MenuItem("Alpha Strike");
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

    private MenuItem createFlipArmsMenuItem() {
        MenuItem item = new MenuItem("Flip Arms");

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

    private MenuItem createFireMenuItem() {
        MenuItem item = new MenuItem("Fire");

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

    private Menu createPhysicalMenu(boolean isMovementPhase) {
        Menu menu = new Menu("Physicals");

        if (isMovementPhase) {
            if (myEntity.canCharge()) {

                MenuItem item = createChargeMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            if (myEntity.canDFA()) {
                MenuItem item = createDFAMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }
        } else {

            MenuItem item = null;

            if (!myEntity.isHullDown() && !myEntity.isProne() && !myEntity.hasHipCrit()) {
                item = createKickMenuItem();

                if (item != null) {
                    menu.add(item);
                }

                item = createTripMenuItem();

                if (item != null) {
                    menu.add(item);
                }

            }

            if (myEntity instanceof BipedMech
                    && (!myEntity.isLocationBad(Mech.LOC_LARM)
                    || !myEntity.isLocationBad(Mech.LOC_RARM)) ) {
                item = createPunchMenuItem();

                if (item != null) {
                    menu.add(item);
                }

            }

            if (myEntity instanceof BipedMech
                    && !myEntity.isLocationBad(Mech.LOC_LARM) && !myEntity.isLocationBad(Mech.LOC_RARM) ) {
                item = createPushMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            if (myEntity.getJumpMP() > 0) {
                item = createJumpJetAttackMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            if (myEntity.isProne()) {
                item = createThrashMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }

            item = createDodgeMenuItem();

            if (item != null) {
                menu.add(item);
            }

            if (myEntity.getClubs().size() > 0) {

                Menu clubMenu = createClubMenu();

                if (clubMenu.getItemCount() > 0) {
                    menu.add(clubMenu);
                }
            }

            ToHitData grap = GrappleAttackAction.toHit(client.game, myEntity.getId(), myTarget);
            ToHitData bgrap = BreakGrappleAttackAction.toHit(client.game, myEntity.getId(), myTarget);
            if (grap.getValue() != TargetRoll.IMPOSSIBLE || bgrap.getValue() != TargetRoll.IMPOSSIBLE) {

                item = createGrappleMenuItem();

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

    private Menu createStandMenu() {
        Menu menu = new Menu();

        if (selectedEntity.isProne()) {
            menu.setLabel("Stand");
            menu.add(createStandMenuItem(false));

            if (game.getOptions().booleanOption("tacops_careful_stand") && myEntity.getWalkMP() > 2 && myEntity.moved < 1) {
                menu.add(createStandMenuItem(true));
            }

            if (game.getOptions().booleanOption("tacops_hull_down")) {
                menu.add(createHullDownMenuItem());
            }

        } else if (selectedEntity.isHullDown()) {
            menu.setLabel("Stand");
            menu.add(createStandMenuItem(false));

            if (game.getOptions().booleanOption("tacops_careful_stand")) {
                menu.add(createStandMenuItem(true));
            }

            menu.add(createProneMenuItem());
        } else {
            menu.setLabel("Prone");
            menu.add(createProneMenuItem());

            if (game.getOptions().booleanOption("tacops_hull_down")) {
                menu.add(createHullDownMenuItem());
            }
        }

        return menu;
    }

    private MenuItem createStandMenuItem(boolean carefulStand) {
        MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butUp"));

        if (carefulStand) {
            item.setLabel("Careful Stand");
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

    private MenuItem createHullDownMenuItem() {
        MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butHullDown"));

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

    private MenuItem createProneMenuItem() {
        MenuItem item = new MenuItem(Messages.getString("MovementDisplay.butDown"));

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

    private Menu createTargetMenu() {
        Menu menu = new Menu("Target");

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

        if (myEntity.getAmmo().size() < 1) {
            return false;
        }

        for (Mounted ammo : myEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == ammoType) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMunitionType(long munition) {

        if (myEntity.getAmmo().size() < 1) {
            return false;
        }

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

    private MenuItem createTorsoTwistMenuItem(int direction) {
        MenuItem item = new MenuItem();

        if (direction == 1) {
            item.setLabel("Right");
        } else {
            item.setLabel("Left");
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

    private MenuItem createTorsoTwistMenuItem(Coords coords) {
        MenuItem item = new MenuItem("Twist");

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

    private Menu createTorsoTwistMenu() {
        Menu menu = new Menu();

        if (myEntity instanceof BipedMech) {
            menu.setLabel("Torso Twist");
            if (coords.equals(myEntity.getPosition())) {
                menu.add(createTorsoTwistMenuItem(1));
                menu.add(createTorsoTwistMenuItem(0));
            } else {
                menu.add(createTorsoTwistMenuItem(coords));
            }
        } else if (myEntity instanceof Tank && ((Tank) myEntity).getInternal(Tank.LOC_TURRET) > -1) {
            menu.setLabel("Turret Twist");
            if (coords.equals(myEntity.getPosition())) {
                menu.add(createTorsoTwistMenuItem(1));
                menu.add(createTorsoTwistMenuItem(0));
            } else {
                menu.add(createTorsoTwistMenuItem(coords));
            }
        }

        return menu;
    }

    private void selectTarget() {
        Vector<Entity> list = new Vector<Entity>();

        for (Entity en : game.getEntitiesVector(coords)) {
            if (en.isEnemyOf(myEntity)) {
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

    private Menu createModeMenu() {
        Menu menu = new Menu("Modes");

        int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = myEntity.getEquipment(weaponNum);

        if (mounted != null && mounted.getType().hasModes()) {
            for (int pos = 0; pos < mounted.getType().getModesCount(); pos++) {
                menu.add(createModeMenuItem(mounted, pos));
            }
        }

        return menu;
    }

    private MenuItem createModeMenuItem(Mounted mounted, int position) {
        MenuItem item = new MenuItem();

        EquipmentMode mode = mounted.getType().getMode(position);

        if (mode.equals(mounted.curMode())) {
            item.setLabel("* " + mode.getDisplayableName());
        } else {
            item.setLabel(mode.getDisplayableName());
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

    private MenuItem createPunchMenuItem() {
        MenuItem item = new MenuItem("Punch");

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

    private MenuItem createKickMenuItem() {
        MenuItem item = new MenuItem("Kick");

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

    private MenuItem createPushMenuItem() {
        MenuItem item = new MenuItem("Push");

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

    private MenuItem createJumpJetAttackMenuItem() {
        MenuItem item = new MenuItem("Jump Jet Attack");

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

    private MenuItem createVibroClawMenuItem() {
        MenuItem item = new MenuItem("Vibro Claw Attack");

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

    private MenuItem createThrashMenuItem() {
        MenuItem item = new MenuItem("Thrash");

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

    private MenuItem createGrappleMenuItem() {
        MenuItem item = new MenuItem("Grapple");

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

    private MenuItem createTripMenuItem() {
        MenuItem item = new MenuItem("Trip");

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

    private MenuItem createDodgeMenuItem() {
        MenuItem item = new MenuItem("Dodge");

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

    private Menu createClubMenu() {
        Menu menu = new Menu("Weapon");

        for (int pos = 0; pos < myEntity.getClubs().size(); pos++) {
            Mounted club = myEntity.getClubs().get(pos);
            if (!club.isDestroyed()) {
                menu.add(createClubMenuItem(club.getName(), pos));
            }
        }
        return menu;
    }

    private MenuItem createClubMenuItem(String clubName, int clubNumber) {
        MenuItem item = new MenuItem(clubName);

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

    @Override
    public void show(Component comp, int x, int y){
        if (client.isMyTurn() && myEntity != null) {
            selectTarget();
        }
        super.show(comp, x, y);
    }

}