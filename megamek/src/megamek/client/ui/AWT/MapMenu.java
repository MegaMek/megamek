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
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import megamek.client.Client;
import megamek.common.BuildingTarget;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HexTarget;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.MinefieldTarget;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.options.GameOptions;

/*
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
    
    public MapMenu(Coords coords, Client client, Component panel, ClientGUI gui) {
        this.coords = coords;
        this.game = client.game;
        this.currentPanel = panel;
        this.board = client.getBoard();
        this.options = client.game.getOptions();
        this.client = client;
        this.gui = gui;
    }

    private boolean canSelectEntities() {
        return client.isMyTurn() && (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof MovementDisplay || currentPanel instanceof TargetingPhaseDisplay);
    }

    private boolean canTargetEntities() {
        return client.isMyTurn() && (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof TargetingPhaseDisplay);
    }

    private void createMenu() {
        this.removeAll();

        Menu menu = new Menu();
        
        menu = createSelectMenu();

        if (this.getItemCount() > 0) {
            this.addSeparator();
        }

        // add view options
        for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
            final Entity entity = i.nextElement();
            this.add(ViewMenuItem(entity));
        }

        // add target options
        if (canTargetEntities()) {
            if (this.getItemCount() > 0) {
                this.addSeparator();
            }
            for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = i.nextElement();
                this.add(TargetMenuItem(entity));
            }
            // Can target weapons at the hex if it contains woods or building.
            // Can target physical attacks at the hex if it contains building.
            if (currentPanel instanceof FiringDisplay || currentPanel instanceof PhysicalDisplay || currentPanel instanceof TargetingPhaseDisplay) {
                IHex h = board.getHex(coords);
                if (h != null && currentPanel instanceof FiringDisplay && !board.inSpace() && !board.inAtmosphere()) {
                    this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_CLEAR)));
                    if (client.game.getOptions().booleanOption("tacops_start_fire")) { //$NON-NLS-1$
                        this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_IGNITE)));
                    }
                } else if (h != null && h.containsTerrain(Terrains.FUEL_TANK)) {
                    this.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
                    if (client.game.getOptions().booleanOption("tacops_start_fire")) { //$NON-NLS-1$
                        this.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
                    }
                } else if (h != null && h.containsTerrain(Terrains.BUILDING)) {
                    this.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
                    if (client.game.getOptions().booleanOption("tacops_start_fire")) { //$NON-NLS-1$
                        this.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
                    }
                }
                if (h != null && client.game.containsMinefield(coords) && (currentPanel instanceof FiringDisplay || currentPanel instanceof TargetingPhaseDisplay)) {
                    this.add(TargetMenuItem(new MinefieldTarget(coords, board)));
                }
                if (h != null && currentPanel instanceof FiringDisplay) {
                    if (board.inSpace()) {
                        this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_SCREEN)));
                    } else {
                        this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_MINEFIELD_DELIVER)));
                        this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_FLARE_DELIVER)));
                        this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_BOMB)));
                        this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_ARTILLERY)));
                        if (client.game.getOptions().booleanOption("tacops_start_fire") && h.containsTerrain(Terrains.FIRE)) {
                            this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_EXTINGUISH)));
                        }
                    }
                }
                if (h != null && currentPanel instanceof TargetingPhaseDisplay && !board.inSpace() && !board.inAtmosphere()) {
                    this.add(TargetMenuItem(new HexTarget(coords, board, Targetable.TYPE_HEX_ARTILLERY)));
                }
            }
        }

    }

    
    private MenuItem TargetMenuItem(HexTarget hex) {
        return null;
    }
    
    private MenuItem TargetMenuItem(Entity en) {
        return null;
    }
    
    private MenuItem TargetMenuItem(BuildingTarget building) {
        return null;
    }
    
    private MenuItem TargetMenuItem(MinefieldTarget mines) {
        return null;
    }
    
    private MenuItem SelectMenuItem(Entity en) {
        
        MenuItem item = new MenuItem(Messages.getString("ClientGUI.selectMenuItem") + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if (currentPanel instanceof MovementDisplay) {
                        ((MovementDisplay) currentPanel).selectEntity(Integer.parseInt(e.getActionCommand()));
                    } else if (currentPanel instanceof FiringDisplay) {
                        ((FiringDisplay) currentPanel).selectEntity(Integer.parseInt(e.getActionCommand()));
                    } else if (currentPanel instanceof PhysicalDisplay) {
                        ((PhysicalDisplay) currentPanel).selectEntity(Integer.parseInt(e.getActionCommand()));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }
    
    private MenuItem ViewMenuItem(Entity en) {
        MenuItem item = new MenuItem(Messages.getString("ClientGUI.selectMenuItem") + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    
                    gui.setDisplayVisible(true);
                    gui.mechD.displayEntity(game.getEntity(Integer.parseInt(e.getActionCommand())));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return item;
    }
    
    
    private Menu createSelectMenu() {
        Menu menu = new Menu();
        // add select options
        if (canSelectEntities()) {
            for (Enumeration<Entity> i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = i.nextElement();
                if (client.game.getTurn().isValidEntity(entity, client.game)) {
                    menu.add(SelectMenuItem(entity));
                }
            }
        }
        
        return menu;
    }
    
    private Menu createViewMenu() {
        Menu menu = new Menu();
        
        return menu;
        
    }
    
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }
}