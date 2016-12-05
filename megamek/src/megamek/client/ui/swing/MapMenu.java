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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.common.AmmoType;
import megamek.common.BipedMech;
import megamek.common.Building;
import megamek.common.Building.DemolitionCharge;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.EquipmentMode;
import megamek.common.HexTarget;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IPlayer;
import megamek.common.Mech;
import megamek.common.MinefieldTarget;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.SpecialHexDisplay;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponComparatorDamage;
import megamek.common.WeaponType;
import megamek.common.actions.BAVibroClawAttackAction;
import megamek.common.actions.BreakGrappleAttackAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.CLFireExtinguisher;
import megamek.common.weapons.ISFireExtinguisher;

/**
 * Context menu for the board.
 */
public class MapMenu extends JPopupMenu {

    /**
     *
     */
    private static final long serialVersionUID = 2879345079968414986L;

    private Coords coords;
    IGame game;
    Component currentPanel;
    private IBoard board;
    Client client;
    ClientGUI gui;
    Entity selectedEntity;
    Entity myEntity;
    Targetable myTarget = null;
    private boolean hasMenu = false;

    public MapMenu(Coords coords, Client client, Component panel, ClientGUI gui) {
        this.coords = coords;
        game = client.getGame();
        currentPanel = panel;
        board = client.getBoard();
        this.client = client;
        this.gui = gui;
        selectedEntity = myEntity = game.getEntity(gui.getSelectedEntityNum());

        hasMenu = createMenu();
        // make popups not consume mouse events outside them
        // so board dragging can start correctly when this menu is open
        UIManager.put("PopupMenu.consumeEventOnClose", false);
    }

    private boolean canSelectEntities() {
        return client.isMyTurn()
               && ((currentPanel instanceof FiringDisplay)
                   || (currentPanel instanceof PhysicalDisplay)
                   || (currentPanel instanceof MovementDisplay)
                   || (currentPanel instanceof TargetingPhaseDisplay));
    }

    private boolean canTargetEntities() {
        return client.isMyTurn()
               && ((currentPanel instanceof FiringDisplay)
                   || (currentPanel instanceof PhysicalDisplay)
                   || (currentPanel instanceof TargetingPhaseDisplay));
    }

    private boolean createMenu() {
        removeAll();
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

        if (client.isMyTurn() && (myEntity != null)) {
            selectTarget();

            menu = createTargetMenu();
            if (menu.getItemCount() > 0) {
                this.add(menu);
                itemCount++;
            }

            if (currentPanel instanceof MovementDisplay) {
                menu = createMovementMenu(myEntity.getPosition().equals(coords));

                if (itemCount > 0) {
                    addSeparator();
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
                    addSeparator();
                    this.add(menu);
                    itemCount++;
                }

            } else if (currentPanel instanceof TargetingPhaseDisplay) {
                
                if (itemCount > 0) {
                    addSeparator();
                    itemCount++;
                }
                    
                menu = createTorsoTwistMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }

                menu = createRotateTurretMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }
                
            } else if ((currentPanel instanceof FiringDisplay)) {

                if (itemCount > 0) {
                    addSeparator();
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
                
                menu = createRotateTurretMenu();
                if (menu.getItemCount() > 0) {
                    this.add(menu);
                    itemCount++;
                }
                
            } else if ((currentPanel instanceof PhysicalDisplay)) {
                menu = createPhysicalMenu(false);

                if (menu.getItemCount() > 0) {
                    addSeparator();
                    this.add(menu);
                    itemCount++;
                }

            }

            // Traitor Command
            JMenuItem item = new JMenuItem(
                    Messages.getString("MovementDisplay.Traitor")); //$NON-NLS-1$
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TRAITOR
                                          .getCmd());
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (currentPanel instanceof MovementDisplay) {
                            ((MovementDisplay) currentPanel).actionPerformed(e);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            this.add(item);
        }
        
        menu = touchOffExplosivesMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
            itemCount++;
        }

        menu = createSpecialHexDisplayMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
            itemCount++;
        }

        return itemCount > 0;
    }

    private JMenuItem TargetMenuItem(Targetable t) {
        JMenuItem item = new JMenuItem(
                Messages.getString("ClientGUI.targetMenuItem")
                + t.getDisplayName());

        String targetCode = "";

        if (t instanceof Entity) {
            targetCode = "E|" + ((Entity) t).getId();
        } else if (t instanceof BuildingTarget) {
            targetCode = "B|" + t.getPosition().getX() + "|" + t.getPosition().getY() + "|" + t.getTargetType();
        } else if (t instanceof MinefieldTarget) {
            targetCode = "M|" + t.getPosition().getX() + "|" + t.getPosition().getY();
        } else {
            targetCode = "H|" + t.getPosition().getX() + "|" + t.getPosition().getY() + "|" + t.getTargetType();
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
        JMenuItem item = new JMenuItem(
                Messages.getString("MovementDisplay.butCharge"));

        if (!client.getGame().getEntities(coords).hasNext()) {
            return null;
        }
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_CHARGE.getCmd());
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotCourse(e);
            }
        });

        return item;
    }

    private JMenuItem createDFAJMenuItem() {
        JMenuItem item = new JMenuItem(
                Messages.getString("MovementDisplay.butDfa"));

        if (!client.getGame().getEntities(coords).hasNext()) {
            return null;
        }
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_DFA.getCmd());
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotCourse(e);
            }
        });

        return item;
    }

    private JMenuItem selectJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(
                Messages.getString("ClientGUI.selectMenuItem")
                + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Entity entity = game.getEntity(Integer.parseInt(e
                            .getActionCommand()));
                    selectedEntity = entity;
                    if (currentPanel instanceof MovementDisplay) {
                        ((MovementDisplay) currentPanel)
                                .selectEntity(selectedEntity.getId());
                    } else if (currentPanel instanceof FiringDisplay) {
                        ((FiringDisplay) currentPanel)
                                .selectEntity(selectedEntity.getId());
                    } else if (currentPanel instanceof PhysicalDisplay) {
                        ((PhysicalDisplay) currentPanel)
                                .selectEntity(selectedEntity.getId());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem viewJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(
                Messages.getString("ClientGUI.viewMenuItem")
                + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    selectedEntity = game.getEntity(Integer.parseInt(e
                            .getActionCommand()));
                    gui.setDisplayVisible(true);
                    gui.mechD.displayEntity(selectedEntity);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenu touchOffExplosivesMenu() {
        JMenu menu = new JMenu("Touch off explosives");

        Building bldg = client.getBoard().getBuildingAt(coords);
        if ((bldg != null)) {
            for (final DemolitionCharge charge : bldg.getDemolitionCharges()) {
                if (charge.playerId == client.getLocalPlayer().getId()
                        && coords.equals(charge.pos)) {
                    JMenuItem item = new JMenuItem(charge.damage + " Damage");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            client.sendExplodeBuilding(charge);
                        }
                    });
                    menu.add(item);
                }
            }
        }
        return menu;
    }

    /**
     * Create various menus related to <code>SpecialHexDisplay</code>.
     *
     * @return
     */
    private JMenu createSpecialHexDisplayMenu() {
        JMenu menu = new JMenu("Special Hex Display");

        final Collection<SpecialHexDisplay> shdList = game.getBoard()
                .getSpecialHexDisplay(coords);

        SpecialHexDisplay note = null;
        if (shdList != null) {
            for (SpecialHexDisplay shd : shdList) {
                if (shd.getType() == SpecialHexDisplay.Type.PLAYER_NOTE
                    && shd.getOwner().equals(client.getLocalPlayer())) {
                    note = shd;
                    break;
                }
            }
        }


        final SpecialHexDisplay finalNote;
        if (note == null) {
            finalNote = new SpecialHexDisplay(
                    SpecialHexDisplay.Type.PLAYER_NOTE,
                    SpecialHexDisplay.NO_ROUND, client.getLocalPlayer(), "");
        } else {
            finalNote = note;
        }
        JMenuItem item = new JMenuItem(Messages.getString("NoteDialog.action")); //$NON-NLS-1$
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NoteDialog nd = new NoteDialog(gui.frame, finalNote);
                gui.bv.setShouldIgnoreKeys(true);
                nd.setVisible(true);
                gui.bv.setShouldIgnoreKeys(false);
                if (nd.isAccepted()) {
                    client.sendSpecialHexDisplayAppend(coords, finalNote);
                }
            }
        });
        menu.add(item);

        if (note != null) {
            item = new JMenuItem(Messages.getString("NoteDialog.delete")); //$NON-NLS-1$
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    client.sendSpecialHexDisplayDelete(coords, finalNote);
                }
            });
        }
        menu.add(item);

        return menu;
    }

    private JMenu createSelectMenu() {
        JMenu menu = new JMenu("Select");
        // add select options
        if (canSelectEntities()) {
            for (Entity entity : client.getGame().getEntitiesVector(coords,
                    canTargetEntities())) {
                if (client.getMyTurn().isValidEntity(entity, client.getGame())) {
                    menu.add(selectJMenuItem(entity));
                }
            }
        }
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        IGame game = client.getGame();
                
        IPlayer localPlayer = client.getLocalPlayer();
        
        for (Entity entity : game.getEntitiesVector(coords, true)) {
            // Only add the unit if it's actually visible
            //  With double blind on, the game may unseen units
            if (!entity.isSensorReturn(localPlayer)
                    && entity.hasSeenEntity(localPlayer)) {
                menu.add(viewJMenuItem(entity));
            }
        }
        return menu;
    }

    private JMenu createMovementMenu(boolean entityInHex) {
        JMenu menu = new JMenu("Movement");

        if (myEntity == null) {
            return menu;
        }

        if (entityInHex) {
            JMenuItem item = new JMenuItem(
                    Messages.getString("MovementDisplay.MoveEnvelope")); //$NON-NLS-1$
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_ENVELOPE
                                          .getCmd());
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


            item = new JMenuItem(Messages.getString("MovementDisplay.butWalk"));

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_WALK.getCmd());
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

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_BACK_UP.getCmd());
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
                item = new JMenuItem(
                        Messages.getString("CommonMenuBar.moveJump"));

                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_JUMP
                                              .getCmd());
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

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_EVADE)) {
                item = new JMenuItem(
                        Messages.getString("MovementDisplay.butEvade"));

                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_EVADE
                                              .getCmd());
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

            if (game.getPlanetaryConditions().isRecklessConditions()
                && !game.getBoard().inSpace()
                && !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN)) {
                item = new JMenuItem(
                        Messages.getString("MovementDisplay.butReckless"));

                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_RECKLESS
                                              .getCmd());
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

            JMenuItem item = new JMenuItem(
                    Messages.getString("MovementDisplay.butWalk"));

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_WALK
                                          .getCmd());
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

            item = new JMenuItem(
                    Messages.getString("MovementDisplay.butBackup"));

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_BACK_UP
                                          .getCmd());
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
                item = new JMenuItem(
                        Messages.getString("CommonMenuBar.moveJump"));

                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_JUMP
                                              .getCmd());
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

            item = new JMenuItem(
                    Messages.getString("MovementDisplay.moveLongestRun")); //$NON-NLS-1$

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_LONGEST_RUN
                                          .getCmd());
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

            item = new JMenuItem(
                    Messages.getString("MovementDisplay.moveLongestWalk")); //$NON-NLS-1$

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_LONGEST_WALK
                                          .getCmd());
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

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_EVADE)) {
                item = new JMenuItem(
                        Messages.getString("MovementDisplay.butEvade"));

                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_EVADE
                                              .getCmd());
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

            if (game.getPlanetaryConditions().isRecklessConditions()
                && !game.getBoard().inSpace()
                && !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN)) {
                item = new JMenuItem(
                        Messages.getString("MovementDisplay.butReckless"));

                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_RECKLESS
                                              .getCmd());
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

        JMenuItem item = new JMenuItem(
                Messages.getString("MovementDisplay.butTurnRight"));

        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TURN_RIGHT
                                      .getCmd());
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

        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TURN_LEFT
                                      .getCmd());
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

        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TURN_RIGHT
                                      .getCmd());
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

        if (myEntity.canFlipArms()) {
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
                    // Get all weapons
                    ArrayList<Mounted> weapons = myEntity.getWeaponList();
                    // We will need to map a Mounted to it's weapon number
                    HashMap<Mounted, Integer> weapToId = 
                            new HashMap<Mounted, Integer>();
                    for (Mounted weapon : weapons) {
                        weapToId.put(weapon, myEntity.getEquipmentNum(weapon));
                    }
                    // Sort weapons from high damage to low
                    Collections.sort(weapons, new WeaponComparatorDamage(false));
                    
                    Targetable target = panel.getTarget();
                    for (Mounted weapon : weapons) {
                        // If the weapon has been used at all this turn, ignore
                        if (weapon.usedInPhase() != IGame.Phase.PHASE_UNKNOWN) {
                            continue;
                        }
                        int weaponNum = weapToId.get(weapon);
                        // Used to determine if attack is valid
                        WeaponAttackAction waa = new WeaponAttackAction(
                                myEntity.getId(), target.getTargetType(),
                                target.getTargetId(), weaponNum);
                        // Only fire weapons that have a chance to hit
                        int toHitVal = waa.toHit(game).getValue(); 
                        if ((toHitVal != TargetRoll.IMPOSSIBLE)
                                && (toHitVal <= 12)) {
                            gui.mechD.wPan.selectWeapon(weaponNum);
                            panel.fire();
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
                    FiringDisplay display = (FiringDisplay) currentPanel;

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

            if (!myEntity.isHullDown() && !myEntity.isProne()
                && !myEntity.hasHipCrit()) {
                item = createKickJMenuItem();

                if (item != null) {
                    menu.add(item);
                }

                item = createTripJMenuItem();

                if (item != null) {
                    menu.add(item);
                }

            }

            if ((myEntity instanceof BipedMech)
                && (!myEntity.isLocationBad(Mech.LOC_LARM) || !myEntity
                    .isLocationBad(Mech.LOC_RARM))) {
                item = createPunchJMenuItem();

                if (item != null) {
                    menu.add(item);
                }

            }

            if ((myEntity instanceof BipedMech)
                && !myEntity.isLocationBad(Mech.LOC_LARM)
                && !myEntity.isLocationBad(Mech.LOC_RARM)) {
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

            ToHitData grap = GrappleAttackAction.toHit(client.getGame(),
                                                       myEntity.getId(), myTarget);
            ToHitData bgrap = BreakGrappleAttackAction.toHit(client.getGame(),
                                                             myEntity.getId(), myTarget);
            if ((grap.getValue() != TargetRoll.IMPOSSIBLE)
                || (bgrap.getValue() != TargetRoll.IMPOSSIBLE)) {

                item = createGrappleJMenuItem();

                if (item != null) {
                    menu.add(item);
                }
            }
            if (myTarget != null) {
                ToHitData vibro = BAVibroClawAttackAction.toHit(
                        client.getGame(), myEntity.getId(), myTarget);
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

    private JMenu createStandMenu() {
        JMenu menu = new JMenu();

        if (selectedEntity.isProne()) {
            menu.setText("Stand");
            menu.add(createStandJMenuItem(false));

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_CAREFUL_STAND)
                && (myEntity.getWalkMP() > 2)
                && (myEntity.moved == EntityMovementType.MOVE_NONE)) {
                menu.add(createStandJMenuItem(true));
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN)) {
                menu.add(createHullDownJMenuItem());
            }

        } else if (selectedEntity.isHullDown()) {
            menu.setText("Stand");
            menu.add(createStandJMenuItem(false));

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_CAREFUL_STAND)) {
                menu.add(createStandJMenuItem(true));
            }

            menu.add(createProneJMenuItem());
        } else {
            menu.setText("Prone");
            menu.add(createProneJMenuItem());

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN)) {
                menu.add(createHullDownJMenuItem());
            }
        }

        return menu;
    }

    private JMenuItem createStandJMenuItem(boolean carefulStand) {
        JMenuItem item = new JMenuItem(
                Messages.getString("MovementDisplay.butUp"));

        if (carefulStand) {
            item.setText("Careful Stand");
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_CAREFUL_STAND
                                          .getCmd());
        } else {
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_GET_UP
                                          .getCmd());
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
        JMenuItem item = new JMenuItem(
                Messages.getString("MovementDisplay.butHullDown"));

        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_HULL_DOWN
                                      .getCmd());
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
        JMenuItem item = new JMenuItem(
                Messages.getString("MovementDisplay.butDown"));

        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_GO_PRONE
                                      .getCmd());
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

        // If we can't target entities, nothing to do
        if (!canTargetEntities()) {
            return menu;
        }

        final boolean isFiringDisplay = (currentPanel instanceof FiringDisplay);
        final boolean isTargetingDisplay = (currentPanel instanceof TargetingPhaseDisplay);
        final boolean canStartFires = client.getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE); //$NON-NLS-1$
        
        IPlayer localPlayer = client.getLocalPlayer();
        
        // Add menu item to target each entity in the coords
        for (Entity entity : client.getGame().getEntitiesVector(coords)) {
            // Only add the unit if it's actually visible
            //  With double blind on, the game may have unseen units
            if (!entity.isSensorReturn(localPlayer)
                    && entity.hasSeenEntity(localPlayer)) {
                menu.add(TargetMenuItem(entity));
            }
        }

        IHex h = board.getHex(coords);
        // If the hex is null, we're done here
        if (h == null) {
            return menu;
        }

        // Clearing hexes and igniting hexes
        if (isFiringDisplay && !board.inSpace() && !board.inAtmosphere()) {
            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_CLEAR)));
            if (canStartFires
                && (h.containsTerrain(Terrains.WOODS)
                    || h.containsTerrain(Terrains.JUNGLE)
                    || h.containsTerrain(Terrains.FIELDS)
                    || hasMunitionType(AmmoType.M_INFERNO)
                    || hasMunitionType(AmmoType.M_INFERNO_IV)
                    || hasMunitionType(AmmoType.M_THUNDER_INFERNO))) {
                menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_IGNITE)));
            }
            // Targeting fuel tanks
        }
        if (h.containsTerrain(Terrains.FUEL_TANK)) {
            menu.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
            if (canStartFires) {
                menu.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
            }
            // Targeting buildings or bridges
        }
        if ((h.containsTerrain(Terrains.BUILDING)
             || h.containsTerrain(Terrains.BRIDGE))) {
            menu.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
            if (canStartFires) {
                menu.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
            }
        }
        if (isFiringDisplay) {
            if (board.inSpace() && hasAmmoType(AmmoType.T_SCREEN_LAUNCHER)) {
                menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_SCREEN)));
            } else {
                if ((hasAmmoType(AmmoType.T_LRM) || hasAmmoType(AmmoType.T_MML))
                    && (hasMunitionType(AmmoType.M_FASCAM)
                        || hasMunitionType(AmmoType.M_THUNDER)
                        || hasMunitionType(AmmoType.M_THUNDER_ACTIVE)
                        || hasMunitionType(AmmoType.M_THUNDER_AUGMENTED)
                        || hasMunitionType(AmmoType.M_THUNDER_INFERNO)
                        || hasMunitionType(AmmoType.M_THUNDER_VIBRABOMB))) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_MINEFIELD_DELIVER)));
                }

                if (hasMunitionType(AmmoType.M_FLARE)) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_FLARE_DELIVER)));
                }

                if (hasAmmoType(AmmoType.T_BA_MICRO_BOMB)) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_BOMB)));
                }

                if (hasWeaponFlag(WeaponType.F_DIVE_BOMB)
                    || hasWeaponFlag(WeaponType.F_ALT_BOMB)) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_AERO_BOMB)));
                }

                if (hasAmmoType(AmmoType.T_ARROW_IV)
                    || hasAmmoType(AmmoType.T_SNIPER)
                    || hasAmmoType(AmmoType.T_CRUISE_MISSILE)
                    || hasAmmoType(AmmoType.T_ALAMO)
                    || hasAmmoType(AmmoType.T_KILLER_WHALE)
                    || hasAmmoType(AmmoType.T_LONG_TOM)
                    || hasAmmoType(AmmoType.T_THUMPER)) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_ARTILLERY)));
                }
                if (canStartFires && hasFireExtinguisher()
                    && h.containsTerrain(Terrains.FIRE)) {
                    menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_EXTINGUISH)));
                }
            }
        }
        // Check for Mine Clearance
        if (isFiringDisplay || isTargetingDisplay) {
            if (client.getGame().containsMinefield(coords)) {
                menu.add(TargetMenuItem(new MinefieldTarget(coords, board)));
            }
        }

        if (isTargetingDisplay
            && !board.inSpace()
            && !board.inAtmosphere()
            && (hasAmmoType(AmmoType.T_ARROW_IV)
                || hasAmmoType(AmmoType.T_SNIPER)
                || hasAmmoType(AmmoType.T_CRUISE_MISSILE)
                || hasAmmoType(AmmoType.T_ALAMO)
                || hasAmmoType(AmmoType.T_KILLER_WHALE)
                || hasAmmoType(AmmoType.T_LONG_TOM)
                || hasAmmoType(AmmoType.T_THUMPER))) {
            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_ARTILLERY)));
        }
        // Check for adding TAG targeting buildings and hexes
        if (isTargetingDisplay && myEntity.hasTAG() && !board.inSpace()) {
            menu.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_TAG)));
            if (h.containsTerrain(Terrains.FUEL_TANK)
                || h.containsTerrain(Terrains.BUILDING)
                || h.containsTerrain(Terrains.BRIDGE)) {
                menu.add(TargetMenuItem(new BuildingTarget(coords, board, Targetable.TYPE_BLDG_TAG)));
            }
        }
        return menu;
    }

    void plotCourse(ActionEvent e) {
        ((MovementDisplay) currentPanel).actionPerformed(e);

        // Drag
        ((BoardView1) gui.bv).mouseAction(coords, 3, 16);
        // Click
        ((BoardView1) gui.bv).mouseAction(coords, 1, 16);
    }

    Targetable decodeTargetInfo(String info) {

        StringTokenizer target = new StringTokenizer(info, "|");
        String type = target.nextToken();

        if (type.equalsIgnoreCase("E")) {
            return game.getEntity(Integer.parseInt(target.nextToken()));
        }

        Coords targetCoords = new Coords(Integer.parseInt(target.nextToken()),
                                         Integer.parseInt(target.nextToken()));

        if (type.equals("B")) {
            return new BuildingTarget(targetCoords, board,
                                      Integer.parseInt(target.nextToken()));
        }

        if (type.equals("M")) {
            return new MinefieldTarget(targetCoords, board);
        }

        return new HexTarget(targetCoords, board, Integer.parseInt(target
                                                                           .nextToken()));
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

    private boolean hasWeaponFlag(BigInteger weaponFlag) {

        if (myEntity.getWeaponList().size() < 1) {
            return false;
        }

        for (Mounted wpn : myEntity.getWeaponList()) {
            if (((WeaponType) wpn.getType()).hasFlag(weaponFlag)) {
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
            if (((WeaponType) weapon.getType() instanceof ISFireExtinguisher)
                || ((WeaponType) weapon.getType() instanceof CLFireExtinguisher)) {
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
                    int twistDir = Integer.parseInt(e.getActionCommand());
                    if (currentPanel instanceof FiringDisplay) {
                        ((FiringDisplay) currentPanel).torsoTwist(twistDir);
                    } else if (currentPanel instanceof TargetingPhaseDisplay) {
                        ((TargetingPhaseDisplay) currentPanel)
                                .torsoTwist(twistDir);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createTorsoTwistJMenuItem(Coords twistCoords) {
        JMenuItem item = new JMenuItem("Twist");

        item.setActionCommand(twistCoords.getX() + "|" + twistCoords.getY());
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    StringTokenizer result = new StringTokenizer(e
                                                                         .getActionCommand(), "|");
                    Coords coord = new Coords(Integer.parseInt(result
                                                                       .nextToken()), Integer.parseInt(result.nextToken()));
                    if (currentPanel instanceof FiringDisplay) {
                        ((FiringDisplay) currentPanel).torsoTwist(coord);
                    } else if (currentPanel instanceof TargetingPhaseDisplay) {
                        ((TargetingPhaseDisplay) currentPanel)
                                .torsoTwist(coord);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    private JMenuItem createRotateTurretJMenuItem(final Mech mech,
                                                  final Mounted turret) {
        String turretString;
        if (turret.getType().hasFlag(MiscType.F_SHOULDER_TURRET)) {
            turretString = "Rotate Shoulder Turret ("
                           + mech.getLocationAbbr(turret.getLocation()) + ")";
        } else if (turret.getType().hasFlag(MiscType.F_HEAD_TURRET)) {
            turretString = "Rotate Head Turret";
        } else {
            turretString = "Rotate Quad Turret";
        }
        JMenuItem item = new JMenuItem(turretString);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                TurretFacingDialog tfe = new TurretFacingDialog(gui.frame,
                                                                mech, turret, gui);
                tfe.setVisible(true);
            }
        });
        return item;
    }

    private JMenuItem createRotateDualTurretJMenuItem(final Tank tank) {
        String turretString;
        turretString = "Rotate Front Turret";
        JMenuItem item = new JMenuItem(turretString);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                TurretFacingDialog tfe = new TurretFacingDialog(gui.frame,
                                                                tank, gui);
                tfe.setVisible(true);
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
        } else if ((myEntity instanceof Tank)
                   && (((Tank) myEntity).getInternal(((Tank) myEntity)
                                                             .getLocTurret()) > -1)) {
            menu.setText("Turret Twist");
            if (coords.equals(myEntity.getPosition())) {
                menu.add(createTorsoTwistJMenuItem(1));
                menu.add(createTorsoTwistJMenuItem(0));
            } else {
                menu.add(createTorsoTwistJMenuItem(coords));
            }
        }
        if ((myEntity instanceof Tank) && !((Tank) myEntity).hasNoDualTurret()) {
            menu.add(createRotateDualTurretJMenuItem((Tank) myEntity));
        }

        return menu;
    }

    private JMenu createRotateTurretMenu() {
        JMenu menu = new JMenu();
        menu.setText("Turret Rotation");
        if (myEntity instanceof Mech) {
            for (Mounted mount : myEntity.getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_SHOULDER_TURRET)
                    || mount.getType().hasFlag(MiscType.F_HEAD_TURRET)
                    || mount.getType().hasFlag(MiscType.F_QUAD_TURRET)) {
                    menu.add(createRotateTurretJMenuItem((Mech) myEntity, mount));
                }
            }
        }
        return menu;
    }

    private void selectTarget() {
        Vector<Entity> list = new Vector<Entity>();

        IPlayer localPlayer = client.getLocalPlayer();
        boolean friendlyFire = (game.getOptions()
                .booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE));

        for (Entity en : game.getEntitiesVector(coords)) {
            // Only add the unit if it's actually visible
            //  With double blind on, the game may have unseen units
            if ((en.isEnemyOf(myEntity) || friendlyFire) && !en.equals(myEntity)
                    && !en.isSensorReturn(localPlayer)
                    && en.hasSeenEntity(localPlayer)) {
                list.add(en);
            }
        }

        if (list.size() == 1) {
            myTarget = selectedEntity = list.firstElement();

            // gui.bv.centerOnHex(myTarget.getPosition());
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

        if ((mounted != null) && mounted.getType().hasModes()) {
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
                    int modePosition = Integer.parseInt(e.getActionCommand());
                    int weaponNum = gui.mechD.wPan.getSelectedWeaponNum();
                    Mounted equip = myEntity.getEquipment(weaponNum);
                    equip.setMode(modePosition);
                    client.sendModeChange(myEntity.getId(), weaponNum,
                                          modePosition);
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
                    Mounted club = myEntity.getClubs().get(
                            Integer.parseInt(e.getActionCommand()));
                    ((PhysicalDisplay) currentPanel).club(club);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }

    @Override
    public void show(Component comp, int x, int y) {
        if (client.isMyTurn() && (myEntity != null)) {
            selectTarget();
        }
        super.show(comp, x, y);
    }

    public boolean getHasMenu() {
        return hasMenu;
    }

}
