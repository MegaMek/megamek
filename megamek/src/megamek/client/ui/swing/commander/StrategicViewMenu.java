/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.client.ui.swing.commander;


import java.awt.Component;
import java.util.*;

import javax.swing.*;

import megamek.client.Client;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.commands.ClientCommandPanel;
import megamek.client.ui.swing.lobby.LobbyUtility;
import megamek.common.*;
import megamek.common.equipment.MiscMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.other.CLFireExtinguisher;
import megamek.common.weapons.other.ISFireExtinguisher;
import megamek.logging.MMLogger;
import megamek.server.commands.*;


/**
 * Context menu for the strategic view.
 */
public class StrategicViewMenu extends JPopupMenu {
    private final static MMLogger logger = MMLogger.create(StrategicViewMenu.class);

    private Coords coords;

    private Board board;
    Game game;
    Client client;
    Player localPlayer;
    CommanderGUI gui;
    Entity startingEntity;
    Entity selectedEntity;
    Targetable myTarget;

    private final boolean hasMenu;

    public StrategicViewMenu(Coords coords, Client client, CommanderGUI gui) {
        this.coords = coords;
        this.game = client.getGame();
        this.board = client.getBoard();
        this.client = client;
        this.gui = gui;
        this.localPlayer = client.getLocalPlayer();
        myTarget = null;
        selectedEntity = startingEntity = gui.getDisplayedUnit();

        hasMenu = createMenu();
        // make popups not consume mouse events outside them
        // so board dragging can start correctly when this menu is open
        UIManager.put("PopupMenu.consumeEventOnClose", false);
    }

    private boolean createMenu() {
        removeAll();
        add(createSelectMenu());
        add(createViewMenu());
        selectTarget();
        add(createTargetMenu());
        addSeparator();
        add(createMovementMenu());
        addSeparator();
        var menu = createPleaToRoyaltyMenu();
        if (menu.getItemCount() > 0) {
            add(menu);
        }
        addSeparator();
        menu = createGamemasterMenu();
        if (menu.getItemCount() > 0) {
            add(menu);
        }

        return true;
    }

    private JMenuItem TargetMenuItem(Targetable t) {
        JMenuItem item = new JMenuItem(megamek.client.ui.Messages.getString("ClientGUI.targetMenuItem")
            + t.getDisplayName());

        String targetCode = getTargetCode(t);

        item.setActionCommand(targetCode);
        item.addActionListener(evt -> {
            myTarget = decodeTargetInfo(evt.getActionCommand());
            // TODO
            // target this thing, if building, target ground, it mek, prioritize target

        });
        return item;
    }

    private static String getTargetCode(Targetable t) {
        String targetCode;

        if (t instanceof Entity) {
            targetCode = "E|" + t.getId();
        } else if (t instanceof BuildingTarget) {
            targetCode = "B|" + t.getPosition().getX() + "|" + t.getPosition().getY() + "|" + t.getTargetType();
        } else if (t instanceof MinefieldTarget) {
            targetCode = "M|" + t.getPosition().getX() + "|" + t.getPosition().getY();
        } else {
            targetCode = "H|" + t.getPosition().getX() + "|" + t.getPosition().getY() + "|" + t.getTargetType();
        }
        return targetCode;
    }

    private JMenuItem selectJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(megamek.client.ui.Messages.getString("ClientGUI.selectMenuItem")
            + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(evt -> {
            try {
                selectedEntity = game.getEntity(Integer.parseInt(evt.getActionCommand()));
                gui.setDisplayedUnit(selectedEntity);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem viewJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(megamek.client.ui.Messages.getString("ClientGUI.viewMenuItem")
            + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(evt -> {
            try {
                selectedEntity = game.getEntity(Integer.parseInt(evt.getActionCommand()));
                GUIPreferences.getInstance().setUnitDisplayEnabled(true);
                gui.setDisplayedUnit(selectedEntity);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem viewReadoutJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(megamek.client.ui.Messages.getString("ClientGUI.viewReadoutMenuItem")
            + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(evt -> {
            try {
                selectedEntity = game.getEntity(Integer.parseInt(evt.getActionCommand()));
                LobbyUtility.mekReadout(selectedEntity, 0, false, gui.getFrame());
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenu touchOffExplosivesMenu() {
        JMenu menu = new JMenu("Touch off explosives");

        Building bldg = client.getBoard().getBuildingAt(coords);
        if ((bldg != null)) {
            for (final Building.DemolitionCharge charge : bldg.getDemolitionCharges()) {
                if (charge.playerId == client.getLocalPlayer().getId() && coords.equals(charge.pos)) {
                    JMenuItem item = new JMenuItem(charge.damage + " Damage");
                    item.addActionListener(e -> client.sendExplodeBuilding(charge));
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

        final Collection<SpecialHexDisplay> shdList = game.getBoard().getSpecialHexDisplay(coords);

        return menu;
    }

    /**
     * Creates various menus related to giving commands to allied bots
     *
     * @return JMenu
     */
    private JMenu createPleaToRoyaltyMenu() {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.title"));
        var isGM = client.getLocalPlayer().isGameMaster();
        for (var player : client.getGame().getPlayersList()) {
            var isEnemy = player.isEnemyOf(client.getLocalPlayer());
            var playerIsBot = player.isBot();
            if (playerIsBot && (!isEnemy || isGM)) {
                menu.add(createBotCommands(player));
            }
        }
        return menu;
    }

    private JMenu createBotCommands(Player bot) {
        JMenu menu = new JMenu(bot.getName() + " (" + Player.TEAM_NAMES[bot.getTeam()] + ")");

        JMenu prioritizeTargetUnitMenu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.priority"));
        JMenu ignoreTargetMenu= new JMenu(megamek.client.ui.Messages.getString("Bot.commands.ignore"));
        JMenu fleeMenu = createFleeMenu(bot);
        JMenu behaviorMenu = createBehaviorMenu(bot);

        JMenu targetHexMenu = createTargetHexMenuItem(bot);
        menu.add(targetHexMenu);
        menu.add(createWaypointMenu(bot));
        for (Entity entity : client.getGame().getEntitiesVector(coords)) {
            prioritizeTargetUnitMenu.add(createPrioritizeTargetUnitMenu(bot, entity));
            ignoreTargetMenu.add(createIgnoreTargetUnitMenu(bot, entity));
        }

        if (prioritizeTargetUnitMenu.getItemCount() > 0) {
            menu.add(prioritizeTargetUnitMenu);
        }

        if (ignoreTargetMenu.getItemCount() > 0) {
            menu.add(ignoreTargetMenu);
        }

        menu.addSeparator();
        menu.add(behaviorMenu);
        menu.add(fleeMenu);
        return menu;
    }

    JMenu createBehaviorMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.behavior"));
        menu.add(createCautionMenu(bot));
        menu.add(createAvoidMenu(bot));
        menu.add(createAggressionMenu(bot));
        menu.add(createHerdingMenu(bot));
        menu.add(createBraveryMenu(bot));
        return menu;
    }

    JMenu createHerdingMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.herding"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: herd : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: herd : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createBraveryMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.bravery"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: brave : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: brave : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createAggressionMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.aggression"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: aggression : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: aggression : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createAvoidMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.avoid"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: avoid : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: avoid : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createCautionMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.caution"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: caution : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: caution : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }


    JMenu createFleeMenu(Player bot) {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.flee"));
        menu.add(setFleeAction(new JMenuItem(megamek.client.ui.Messages.getString("BotConfigDialog.northEdge")), bot, CardinalEdge.NORTH));
        menu.add(setFleeAction(new JMenuItem(megamek.client.ui.Messages.getString("BotConfigDialog.southEdge")), bot, CardinalEdge.SOUTH));
        menu.add(setFleeAction(new JMenuItem(megamek.client.ui.Messages.getString("BotConfigDialog.westEdge")), bot, CardinalEdge.WEST));
        menu.add(setFleeAction(new JMenuItem(megamek.client.ui.Messages.getString("BotConfigDialog.eastEdge")), bot, CardinalEdge.EAST));
        menu.add(setFleeAction(new JMenuItem(megamek.client.ui.Messages.getString("BotConfigDialog.nearestEdge")), bot, CardinalEdge.NEAREST));
        return menu;
    }

    private JMenuItem setFleeAction(JMenuItem fleeMenuItem, Player bot, CardinalEdge cardinalEdge) {
        fleeMenuItem.addActionListener(evt -> {
            int confirm = JOptionPane.showConfirmDialog(
                gui.getFrame(),
                megamek.client.ui.Messages.getString("Bot.commands.flee.confirmation", bot.getName()),
                megamek.client.ui.Messages.getString("Bot.commands.flee.confirm"),
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                client.sendChat(String.format("%s: fl : %d",
                    bot.getName(),
                    cardinalEdge.getIndex()
                ));
            }
        });

        return fleeMenuItem;
    }

    JMenuItem createIgnoreTargetUnitMenu(Player bot, Entity entity) {
        JMenuItem item = new JMenuItem(entity.getDisplayName());
        item.addActionListener(evt ->
            client.sendChat(String.format("%s: ig : %d",
                bot.getName(),
                entity.getId()
            ))
        );
        return item;
    }

    JMenuItem createPrioritizeTargetUnitMenu(Player bot, Entity entity) {
        JMenuItem item = new JMenuItem(entity.getDisplayName());
        item.addActionListener(evt ->
            client.sendChat(String.format("%s: pr : %d",
                bot.getName(),
                entity.getId()
            ))
        );
        return item;
    }

    JMenu createWaypointMenu(Player bot) {
        JMenu targetHexMenu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.waypoint"));
        JMenu setWaypointMenu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.setWaypoint")); // "Set hex " + coords.toFriendlyString() + " as the waypoint"
        for (Entity entity : client.getGame().getPlayerEntities(bot, false)) {
            JMenuItem waypoint = new JMenuItem(entity.getDisplayName());
            waypoint.addActionListener(evt ->
                client.sendChat(String.format("%s: aw : %s %s",
                    bot.getName(),
                    entity.getId() + "",
                    coords.hexCode(client.getGame().getBoard())
                ))
            );
            setWaypointMenu.add(waypoint);
        }

        JMenu addWaypointMenu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.addWaypoint")); // "Add hex " + coords.toFriendlyString() + " as a waypoint");
        for (Entity entity : client.getGame().getPlayerEntities(bot, false)) {
            JMenuItem waypoint = new JMenuItem(entity.getDisplayName());
            waypoint.addActionListener(evt ->
                client.sendChat(String.format("%s: sw : %s %s",
                    bot.getName(),
                    entity.getId() + "",
                    coords.hexCode(client.getGame().getBoard())
                ))
            );
            addWaypointMenu.add(waypoint);
        }

        JMenuItem clearWaypoints = new JMenuItem(megamek.client.ui.Messages.getString("Bot.commands.clearAllWaypoints"));
        clearWaypoints.addActionListener(evt -> client.sendChat(String.format("%s: nw", bot.getName())));

        targetHexMenu.add(setWaypointMenu);
        targetHexMenu.add(addWaypointMenu);
        targetHexMenu.add(clearWaypoints);

        return targetHexMenu;
    }

    JMenu createTargetHexMenuItem(Player bot) {
        JMenu targetHexMenu = new JMenu(megamek.client.ui.Messages.getString("Bot.commands.targetHex"));
        JMenuItem item = new JMenuItem("Add hex " + coords.toFriendlyString() + " as strategic target");
        item.addActionListener(evt ->
            client.sendChat(String.format("%s: ta : %02d%02d",
                bot.getName(),
                coords.getX()+1,
                coords.getY()+1
            ))
        );
        targetHexMenu.add(item);
        return targetHexMenu;
    }

    /**
     * Create various menus related to GameMaster (GM) mode
     *
     * @return
     */
    private JMenu createGamemasterMenu() {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.Gamemaster"));
        if (client.getLocalPlayer().getGameMaster()) {
            JMenu dmgMenu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.EditDamage"));
            JMenu cfgMenu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.Configure"));
            JMenu traitorMenu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.Traitor"));
            JMenu rescueMenu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.Rescue"));
            JMenu killMenu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.KillUnit"));
            JMenu specialCommandsMenu = createGMSpecialCommandsMenu();

            var entities = client.getGame().getEntitiesVector(coords);

            for (Entity entity : entities) {
                dmgMenu.add(createUnitEditorMenuItem(entity));
                cfgMenu.add(createCustomMekMenuItem(entity));
                traitorMenu.add(createTraitorMenuItem(entity));
                rescueMenu.add(createRescueMenuItem(entity));
                killMenu.add(createKillMenuItem(entity));
            }
            if (dmgMenu.getItemCount() != 0) {
                menu.add(dmgMenu);
            }
            if (cfgMenu.getItemCount() != 0) {
                menu.add(cfgMenu);
                menu.addSeparator();
            }
            if (traitorMenu.getItemCount() != 0) {
                menu.add(traitorMenu);
            }
            if (rescueMenu.getItemCount() != 0) {
                menu.add(rescueMenu);
            }
            if (killMenu.getItemCount() != 0) {
                menu.add(killMenu);
                menu.addSeparator();
            }
            menu.add(specialCommandsMenu);
        }
        return menu;
    }

    /**
     * Create a menu for special commands for the GM
     * @return the menu
     */
    private JMenu createGMSpecialCommandsMenu() {
        JMenu menu = new JMenu(megamek.client.ui.Messages.getString("Gamemaster.SpecialCommands"));
        java.util.List.of(
            new ChangeOwnershipCommand(null, null),
            new ChangeWeatherCommand(null, null),
            new DisasterCommand(null, null),
            new KillCommand(null, null),
            new FirefightCommand(null, null),
            new FirestarterCommand(null, null),
            new FirestormCommand(null, null),
            new NoFiresCommand(null, null),
            new OrbitalBombardmentCommand(null, null),
            new RemoveSmokeCommand(null, null),
            new RescueCommand(null, null)
        ).forEach(cmd -> {
            JMenuItem item = new JMenuItem(cmd.getLongName());
            item.addActionListener(evt -> new ClientCommandPanel(gui.getFrame(), client, cmd, coords).setVisible(true));
            menu.add(item);
        });

        return menu;
    }

    JMenuItem createCustomMekMenuItem(Entity entity) {
        JMenuItem item = new JMenuItem(entity.getDisplayName());
        item.addActionListener(evt -> {
            CustomMekDialog med = new CustomMekDialog(null, client, Collections.singletonList(entity), true, false);
            med.refreshOptions();
            med.setVisible(true);
            client.sendUpdateEntity(entity);
        });
        return item;
    }

    JMenuItem createUnitEditorMenuItem(Entity entity) {
        JMenuItem item = new JMenuItem(entity.getDisplayName());
        item.addActionListener(evt -> {
            UnitEditorDialog med = new UnitEditorDialog(gui.getFrame(), entity);
            med.setVisible(true);
            client.sendUpdateEntity(entity);
        });
        return item;
    }

    /**
     * Create traitor menu for game master options
     * @param entity    the entity to create the traitor menu for
     * @return JMenu    the traitor menu
     */
    private JMenuItem createTraitorMenuItem(Entity entity) {
        // Traitor Command
        JMenuItem item = new JMenuItem(megamek.client.ui.Messages.getString("Gamemaster.Traitor.text", entity.getDisplayName()));
        item.addActionListener(evt -> {
            var players = client.getGame().getPlayersList();
            Integer[] playerIds = new Integer[players.size() - 1];
            String[] playerNames = new String[players.size() - 1];
            String[] options = new String[players.size() - 1];

            Player currentOwner = entity.getOwner();
            // Loop through the players vector and fill in the arrays
            int idx = 0;
            for (var player : players) {
                if (player.getName().equals(currentOwner.getName())
                    || (player.getTeam() == Player.TEAM_UNASSIGNED)) {
                    continue;
                }
                playerIds[idx] = player.getId();
                playerNames[idx] = player.getName();
                options[idx] = player.getName() + " (ID: " + player.getId() + ")";
                idx++;
            }

            // No players available?
            if (idx == 0) {
                JOptionPane.showMessageDialog(gui.getFrame(),
                    megamek.client.ui.Messages.getString("Gamemaster.Traitor.text.noplayers"));
                return;
            }

            // Dialog for choosing which player to transfer to
            String option = (String) JOptionPane.showInputDialog(gui.getFrame(),
                megamek.client.ui.Messages.getString("Gamemaster.Traitor.text.selectplayer", entity.getDisplayName()),
                megamek.client.ui.Messages.getString("Gamemaster.Traitor.title"), JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);

            // Verify that we have a valid option...
            if (option != null) {
                // Now that we've selected a player, correctly associate the ID and name
                int id = playerIds[Arrays.asList(options).indexOf(option)];
                String name = playerNames[Arrays.asList(options).indexOf(option)];

                // And now we perform the actual transfer
                int confirm = JOptionPane.showConfirmDialog(
                    gui.getFrame(),
                    megamek.client.ui.Messages.getString("Gamemaster.Traitor.confirmation", entity.getDisplayName(), name),
                    megamek.client.ui.Messages.getString("Gamemaster.Traitor.confirm"),
                    JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("/changeOwner %d %d", entity.getId(), id));
                }
            }
        });

        return item;
    }

    /**
     * Create a menu for killing a specific entity
     *
     * @param entity    the entity to create the kill menu for
     * @return JMenuItem    the kill menu item
     */
    private JMenuItem createKillMenuItem(Entity entity) {
        return createEntityCommandMenuItem(entity, "Gamemaster.KillUnit.text",
            "Gamemaster.KillUnit.confirmation", String.format("/kill %d", entity.getId()));
    }

    /**
     * Create a menu for rescuing a specific entity
     * @param entity    the entity to create the rescue menu for
     * @return          the rescue menu item
     */
    private JMenuItem createRescueMenuItem(Entity entity) {
        return createEntityCommandMenuItem(entity, "Gamemaster.Rescue.text",
            "Gamemaster.Rescue.confirmation", String.format("/rescue %d", entity.getId()));
    }

    /**
     * Create a menu for a specific GM command
     * @param entity            the entity to create the menu for
     * @param messageKey        the menu item message key for the menu item
     * @param confirmationKey   the confirmation message key
     * @param command           the command that will be sent to the server
     * @return                  the menu item
     */
    private JMenuItem createEntityCommandMenuItem(Entity entity, String messageKey, String confirmationKey, String command) {
        JMenuItem item = new JMenuItem(megamek.client.ui.Messages.getString(messageKey, entity.getDisplayName()));
        item.addActionListener(evt -> {
            int confirm = JOptionPane.showConfirmDialog(
                gui.getFrame(), megamek.client.ui.Messages.getString(confirmationKey, entity.getDisplayName()),
                megamek.client.ui.Messages.getString("Gamemaster.dialog.confirm"), JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                client.sendChat(command);
            }
        });
        return item;
    }

    private JMenu createSelectMenu() {
        JMenu menu = new JMenu("Select");
        for (Entity entity : game.getEntitiesVector(coords, true)) {
            if (entity.isVisibleToPlayer(localPlayer)) {
                menu.add(selectJMenuItem(entity));
            }

        }
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        for (Entity entity : game.getEntitiesVector(coords, true)) {
            if (entity.isVisibleToPlayer(localPlayer)) {
                menu.add(viewReadoutJMenuItem(entity));
                menu.add(selectJMenuItem(entity));
            }
        }
        return menu;
    }

    private JMenu createMovementMenu() {
        JMenu menu = new JMenu("Movement");
        if (startingEntity == null) {
            return menu;
        }

        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butWaypoint"));
        item.addActionListener(evt -> {
            // TODO - implement waypoint movement
        });
        menu.add(item);
        item = new JMenuItem(Messages.getString("MovementDisplay.butWaypointAdd"));
        item.addActionListener(evt -> {
            // TODO - implement waypoint movement
        });
        menu.add(item);
        item = new JMenuItem(Messages.getString("MovementDisplay.butWaypointSet"));
        item.addActionListener(evt -> {
            // TODO - implement waypoint movement
        });
        menu.add(item);
        item = new JMenuItem(Messages.getString("MovementDisplay.butWaypointClear"));
        item.addActionListener(evt -> {
            // TODO - implement waypoint movement
        });
        menu.add(item);
        item = new JMenuItem(Messages.getString("MovementDisplay.butWaypointUndo"));
        item.addActionListener(evt -> {
            // TODO - implement waypoint movement
        });
        menu.add(item);

        return menu;
    }

    private JMenu createTargetMenu() {
        JMenu menu = new JMenu("Target");
        final boolean canStartFires = client.getGame().getOptions()
            .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE);

        Player localPlayer = client.getLocalPlayer();

        // Add menu item to target each entity in the coords
        for (Entity entity : client.getGame().getEntitiesVector(coords)) {
            // Only add the unit if it's actually visible
            // With double blind on, the game may have unseen units
            if (entity.isVisibleToPlayer(localPlayer)) {
                menu.add(TargetMenuItem(entity));
            }
        }

        Hex h = board.getHex(coords);
        // If the hex is null, we're done here
        if (h == null) {
            return menu;
        }

        // Clearing hexes and igniting hexes
        if (!board.inSpace() && !board.inAtmosphere()) {
            menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_CLEAR)));
            if (canStartFires
                && (h.containsTerrain(Terrains.WOODS)
                || h.containsTerrain(Terrains.JUNGLE)
                || h.containsTerrain(Terrains.FIELDS))) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_IGNITE)));
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
        if ((h.containsTerrain(Terrains.BUILDING) || h.containsTerrain(Terrains.BRIDGE))) {
            menu.add(TargetMenuItem(new BuildingTarget(coords, board, false)));
            if (canStartFires) {
                menu.add(TargetMenuItem(new BuildingTarget(coords, board, true)));
            }
        }

        if (board.inSpace() && hasAmmoType(AmmoType.T_SCREEN_LAUNCHER)) {
            menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_SCREEN)));
        } else {
            if ((hasAmmoType(AmmoType.T_LRM)
                || hasAmmoType(AmmoType.T_LRM_IMP)
                || hasAmmoType(AmmoType.T_MML))
                && (hasMunitionType(AmmoType.Munitions.M_FASCAM)
                || hasMunitionType(AmmoType.Munitions.M_THUNDER)
                || hasMunitionType(AmmoType.Munitions.M_THUNDER_ACTIVE)
                || hasMunitionType(AmmoType.Munitions.M_THUNDER_AUGMENTED)
                || hasMunitionType(AmmoType.Munitions.M_THUNDER_INFERNO)
                || hasMunitionType(AmmoType.Munitions.M_THUNDER_VIBRABOMB))) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_MINEFIELD_DELIVER)));
            }

            if (hasMunitionType(AmmoType.Munitions.M_FLARE)) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_FLARE_DELIVER)));
            }

            if (hasAmmoType(AmmoType.T_BA_MICRO_BOMB)) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_BOMB)));
            }

            if (hasWeaponFlag(WeaponType.F_DIVE_BOMB)
                || hasWeaponFlag(WeaponType.F_ALT_BOMB)) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_AERO_BOMB)));
            }

            if (hasAmmoType(AmmoType.T_ARROW_IV)
                || hasAmmoType(AmmoType.T_SNIPER)
                || hasAmmoType(AmmoType.T_CRUISE_MISSILE)
                || hasAmmoType(AmmoType.T_ALAMO)
                || hasAmmoType(AmmoType.T_KILLER_WHALE)
                || hasAmmoType(AmmoType.T_LONG_TOM)
                || hasAmmoType(AmmoType.T_THUMPER)
                || hasAmmoType(AmmoType.T_BA_TUBE)) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY)));
            }
            if (canStartFires && hasFireExtinguisher()
                && h.containsTerrain(Terrains.FIRE)) {
                menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_EXTINGUISH)));
            }
        }

        // Check for Mine Clearance
        if (client.getGame().containsMinefield(coords)) {
            menu.add(TargetMenuItem(new MinefieldTarget(coords)));
        }


        if (!board.inSpace()
            && !board.inAtmosphere()
            && (hasAmmoType(AmmoType.T_ARROW_IV)
            || hasAmmoType(AmmoType.T_SNIPER)
            || hasAmmoType(AmmoType.T_CRUISE_MISSILE)
            || hasAmmoType(AmmoType.T_ALAMO)
            || hasAmmoType(AmmoType.T_KILLER_WHALE)
            || hasAmmoType(AmmoType.T_LONG_TOM)
            || hasAmmoType(AmmoType.T_THUMPER)
            || hasAmmoType(AmmoType.T_BA_TUBE))) {
            menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY)));
        }
        // Check for adding TAG targeting buildings and hexes
        if (startingEntity.hasTAG() && !board.inSpace()) {
            menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_TAG)));
            if (h.containsTerrain(Terrains.FUEL_TANK)
                || h.containsTerrain(Terrains.BUILDING)
                || h.containsTerrain(Terrains.BRIDGE)) {
                menu.add(TargetMenuItem(new BuildingTarget(coords, board, Targetable.TYPE_BLDG_TAG)));
            }
        }
        return menu;
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
            return new BuildingTarget(targetCoords, board, Integer.parseInt(target.nextToken()));
        }

        if (type.equals("M")) {
            return new MinefieldTarget(targetCoords);
        }

        return new HexTarget(targetCoords, Integer.parseInt(target.nextToken()));
    }

    private boolean hasAmmoType(int ammoType) {
        if (startingEntity.getAmmo().isEmpty()) {
            return false;
        }

        for (Mounted<?> ammo : startingEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == ammoType) {
                return true;
            }
        }

        return false;
    }

    private boolean hasWeaponFlag(EquipmentFlag weaponFlag) {
        if (startingEntity.getWeaponList().isEmpty()) {
            return false;
        }

        for (Mounted<?> wpn : startingEntity.getWeaponList()) {
            if (wpn.getType().hasFlag(weaponFlag)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMunitionType(AmmoType.Munitions munition) {
        if (startingEntity.getAmmo().isEmpty()) {
            return false;
        }

        for (Mounted<?> ammo : startingEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getMunitionType().contains(munition)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasFireExtinguisher() {
        if (startingEntity.getWeaponList().isEmpty()) {
            return false;
        }

        for (Mounted<?> weapon : startingEntity.getWeaponList()) {
            if ((weapon.getType() instanceof ISFireExtinguisher)
                || (weapon.getType() instanceof CLFireExtinguisher)) {
                return true;
            }
        }

        return false;
    }

    private void selectTarget() {
        Vector<Entity> list = new Vector<>();
        boolean friendlyFire = (game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE));

        for (Entity en : game.getEntitiesVector(coords)) {
            // Only add the unit if it's actually visible
            // With double blind on, the game may have unseen units
            if ((en.isEnemyOf(startingEntity) || friendlyFire) && !en.equals(startingEntity)
                && !en.isSensorReturn(localPlayer)
                && en.hasSeenEntity(localPlayer)
                && !en.isHidden()) {
                list.add(en);
            }
        }

        if (list.size() == 1) {
            myTarget = selectedEntity = list.firstElement();
            // TODO target
            // panel.target(myTarget);
        }
    }

    @Override
    public void show(Component comp, int x, int y) {
        if (client.isMyTurn() && (startingEntity != null)) {
            selectTarget();
        }
        super.show(comp, x, y);
    }

    public boolean getHasMenu() {
        return hasMenu;
    }
}
