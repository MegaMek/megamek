/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;

import megamek.client.Client;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.commands.ClientCommandPanel;
import megamek.client.ui.swing.lobby.LobbyUtility;
import megamek.common.*;
import megamek.common.Building.DemolitionCharge;
import megamek.common.actions.BAVibroClawAttackAction;
import megamek.common.actions.BreakGrappleAttackAction;
import megamek.common.actions.GrappleAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.other.CLFireExtinguisher;
import megamek.common.weapons.other.ISFireExtinguisher;
import megamek.logging.MMLogger;
import megamek.server.commands.*;

/**
 * Context menu for the board.
 */
public class MapMenu extends JPopupMenu {
    private final static MMLogger logger = MMLogger.create(MapMenu.class);

    private Coords coords;
    Game game;
    Component currentPanel;
    private Board board;
    Client client;
    ClientGUI gui;
    Entity selectedEntity;
    Entity myEntity;
    Targetable myTarget = null;
    private boolean hasMenu;

    public MapMenu(Coords coords, Client client, Component panel, ClientGUI gui) {
        this.coords = coords;
        game = client.getGame();
        currentPanel = panel;
        board = client.getBoard();
        this.client = client;
        this.gui = gui;
        selectedEntity = myEntity = gui.getDisplayedUnit();

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

                menu = createConvertMenu();

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
        menu = createPleaToRoyaltyMenu();
        if (menu.getItemCount() > 0) {
            this.addSeparator();
            this.add(menu);
            itemCount++;
        }

        this.addSeparator();
        menu = createGamemasterMenu();
        if (menu.getItemCount() > 0) {
            this.add(menu);
            itemCount++;
        }

        return itemCount > 0;
    }

    private JMenuItem TargetMenuItem(Targetable t) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.targetMenuItem")
                + t.getDisplayName());

        String targetCode = getTargetCode(t);

        item.setActionCommand(targetCode);
        item.addActionListener(evt -> {
            myTarget = decodeTargetInfo(evt.getActionCommand());
            if (currentPanel instanceof FiringDisplay) {
                ((FiringDisplay) currentPanel).target(myTarget);
            } else if (currentPanel instanceof PhysicalDisplay) {
                ((PhysicalDisplay) currentPanel).target(myTarget);
            } else if (currentPanel instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) currentPanel).target(myTarget);
            }
        });
        return item;
    }

    private static String getTargetCode(Targetable t) {
        String targetCode;

        if (t instanceof Entity) {
            targetCode = "E|" + ((Entity) t).getId();
        } else if (t instanceof BuildingTarget) {
            targetCode = "B|" + t.getPosition().getX() + "|" + t.getPosition().getY() + "|" + t.getTargetType();
        } else if (t instanceof MinefieldTarget) {
            targetCode = "M|" + t.getPosition().getX() + "|" + t.getPosition().getY();
        } else {
            targetCode = "H|" + t.getPosition().getX() + "|" + t.getPosition().getY() + "|" + t.getTargetType();
        }
        return targetCode;
    }

    private @Nullable JMenuItem createChargeMenuItem() {
        if (!client.getGame().getEntities(coords).hasNext()) {
            return null;
        }

        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butCharge"));
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_CHARGE.getCmd());
        item.addActionListener(this::plotCourse);
        return item;
    }

    private @Nullable JMenuItem createDFAJMenuItem() {
        if (!client.getGame().getEntities(coords).hasNext()) {
            return null;
        }

        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butDfa"));
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_DFA.getCmd());
        item.addActionListener(this::plotCourse);
        return item;
    }

    private JMenuItem selectJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.selectMenuItem")
                + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(evt -> {
            try {
                selectedEntity = game.getEntity(Integer.parseInt(evt.getActionCommand()));
                if (currentPanel instanceof MovementDisplay) {
                    ((MovementDisplay) currentPanel).selectEntity(selectedEntity.getId());
                } else if (currentPanel instanceof FiringDisplay) {
                    ((FiringDisplay) currentPanel).selectEntity(selectedEntity.getId());
                } else if (currentPanel instanceof PhysicalDisplay) {
                    ((PhysicalDisplay) currentPanel).selectEntity(selectedEntity.getId());
                }
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem viewJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.viewMenuItem")
                + en.getDisplayName());

        item.setActionCommand(Integer.toString(en.getId()));
        item.addActionListener(evt -> {
            try {
                selectedEntity = game.getEntity(Integer.parseInt(evt.getActionCommand()));
                GUIPreferences.getInstance().setUnitDisplayEnabled(true);
                gui.getUnitDisplay().displayEntity(selectedEntity);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem viewReadoutJMenuItem(Entity en) {
        JMenuItem item = new JMenuItem(Messages.getString("ClientGUI.viewReadoutMenuItem")
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
            for (final DemolitionCharge charge : bldg.getDemolitionCharges()) {
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

        final SpecialHexDisplay finalNote = Objects.requireNonNullElseGet(note,
                () -> new SpecialHexDisplay(SpecialHexDisplay.Type.PLAYER_NOTE,
                        SpecialHexDisplay.NO_ROUND, client.getLocalPlayer(), ""));
        JMenuItem item = new JMenuItem(Messages.getString("NoteDialog.action"));
        item.addActionListener(evt -> {
            NoteDialog nd = new NoteDialog(gui.frame, finalNote);
            gui.getBoardView().setShouldIgnoreKeys(true);
            nd.setVisible(true);
            gui.getBoardView().setShouldIgnoreKeys(false);
            if (nd.isAccepted()) {
                client.sendSpecialHexDisplayAppend(coords, finalNote);
            }
        });
        menu.add(item);

        if (note != null) {
            item = new JMenuItem(Messages.getString("NoteDialog.delete"));
            item.addActionListener(e -> client.sendSpecialHexDisplayDelete(coords, finalNote));
        }
        menu.add(item);

        return menu;
    }

    /**
     * Creates various menus related to giving commands to allied bots
     *
     * @return JMenu
     */
    private JMenu createPleaToRoyaltyMenu() {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.title"));
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

        JMenu targetHexMenu = new JMenu(Messages.getString("Bot.commands.targetHex"));
        JMenu prioritizeTargetUnitMenu = new JMenu(Messages.getString("Bot.commands.priority"));
        JMenu ignoreTargetMenu= new JMenu(Messages.getString("Bot.commands.ignore"));
        JMenu fleeMenu = createFleeMenu(bot);
        JMenu behaviorMenu = createBehaviorMenu(bot);

        targetHexMenu.add(createTargetHexMenuItem(bot));
        targetHexMenu.add(createWaypointHexMenuItem(bot));
        menu.add(targetHexMenu);

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
        JMenu menu = new JMenu(Messages.getString("Bot.commands.behavior"));
        menu.add(createCautionMenu(bot));
        menu.add(createAvoidMenu(bot));
        menu.add(createAggressionMenu(bot));
        menu.add(createHerdingMenu(bot));
        menu.add(createBraveryMenu(bot));
        return menu;
    }

    JMenu createHerdingMenu(Player bot) {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.herding"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: he : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: he : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createBraveryMenu(Player bot) {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.bravery"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: br : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: br : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createAggressionMenu(Player bot) {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.aggression"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: ag : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: ag : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createAvoidMenu(Player bot) {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.avoid"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: av : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: av : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }

    JMenu createCautionMenu(Player bot) {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.caution"));
        JMenuItem item = new JMenuItem("+");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: ca : +",
                bot.getName()
            ));
        });
        menu.add(item);
        item = new JMenuItem("-");
        item.addActionListener(evt -> {
            client.sendChat(String.format("%s: ca : -",
                bot.getName()
            ));
        });
        menu.add(item);
        return menu;
    }


    JMenu createFleeMenu(Player bot) {
        JMenu menu = new JMenu(Messages.getString("Bot.commands.flee"));
        menu.add(setFleeAction(new JMenuItem(Messages.getString("BotConfigDialog.northEdge")), bot, CardinalEdge.NORTH));
        menu.add(setFleeAction(new JMenuItem(Messages.getString("BotConfigDialog.southEdge")), bot, CardinalEdge.SOUTH));
        menu.add(setFleeAction(new JMenuItem(Messages.getString("BotConfigDialog.westEdge")), bot, CardinalEdge.WEST));
        menu.add(setFleeAction(new JMenuItem(Messages.getString("BotConfigDialog.eastEdge")), bot, CardinalEdge.EAST));
        menu.add(setFleeAction(new JMenuItem(Messages.getString("BotConfigDialog.nearestEdge")), bot, CardinalEdge.NEAREST));
        return menu;
    }

    private JMenuItem setFleeAction(JMenuItem fleeMenuItem, Player bot, CardinalEdge cardinalEdge) {
        fleeMenuItem.addActionListener(evt -> {
            int confirm = JOptionPane.showConfirmDialog(
                gui.getFrame(),
                Messages.getString("Bot.commands.flee.confirmation", bot.getName()),
                Messages.getString("Bot.commands.flee.confirm"),
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

    JMenuItem createTargetHexMenuItem(Player bot) {
        JMenuItem item = new JMenuItem(coords.toFriendlyString());

        var format = (Math.max(coords.getX()+1, coords.getY() + 1) > 99) ? "%s: ta: %03d%03d" : "%s: ta: %02d%02d";

        item.addActionListener(evt ->
            client.sendChat(String.format(format,
                bot.getName(),
                coords.getX()+1,
                coords.getY()+1
            ))
        );
        return item;
    }

    JMenuItem createWaypointHexMenuItem(Player bot) {
        JMenu item = new JMenu("Add waypoint to " + coords.toFriendlyString());
        var format = (Math.max(coords.getX()+1, coords.getY() + 1) > 99) ? "%s: aw: %d %03d%03d" : "%s: aw: %d %02d%02d";
        for (var unit : client.getGame().getPlayerEntities(bot, false)) {
            JMenuItem entry = new JMenuItem(unit.getDisplayName());
            item.addActionListener(evt ->
                client.sendChat(String.format(format,
                    bot.getName(),
                    unit.getId(),
                    coords.getX()+1,
                    coords.getY()+1
                ))
            );
            item.add(entry);
        }

        return item;
    }

    /**
     * Create various menus related to GameMaster (GM) mode
     *
     * @return
     */
    private JMenu createGamemasterMenu() {
        JMenu menu = new JMenu(Messages.getString("Gamemaster.Gamemaster"));
        if (client.getLocalPlayer().getGameMaster()) {
            JMenu dmgMenu = new JMenu(Messages.getString("Gamemaster.EditDamage"));
            JMenu cfgMenu = new JMenu(Messages.getString("Gamemaster.Configure"));
            JMenu traitorMenu = new JMenu(Messages.getString("Gamemaster.Traitor"));
            JMenu rescueMenu = new JMenu(Messages.getString("Gamemaster.Rescue"));
            JMenu killMenu = new JMenu(Messages.getString("Gamemaster.KillUnit"));
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
        JMenu menu = new JMenu(Messages.getString("Gamemaster.SpecialCommands"));
        List.of(
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
            item.addActionListener(evt -> new ClientCommandPanel(gui.getFrame(), gui, cmd, coords).setVisible(true));
            menu.add(item);
        });

        return menu;
    }

    JMenuItem createCustomMekMenuItem(Entity entity) {
        JMenuItem item = new JMenuItem(entity.getDisplayName());
        item.addActionListener(evt -> {
            CustomMekDialog med = new CustomMekDialog(gui, client, Collections.singletonList(entity), true, false);
            med.refreshOptions();
            gui.getBoardView().setShouldIgnoreKeys(true);
            med.setVisible(true);
            client.sendUpdateEntity(entity);
            gui.getBoardView().setShouldIgnoreKeys(false);
        });
        return item;
    }

    JMenuItem createUnitEditorMenuItem(Entity entity) {
        JMenuItem item = new JMenuItem(entity.getDisplayName());
        item.addActionListener(evt -> {
            UnitEditorDialog med = new UnitEditorDialog(gui.getFrame(), entity);
            gui.getBoardView().setShouldIgnoreKeys(true);
            med.setVisible(true);
            client.sendUpdateEntity(entity);
            gui.getBoardView().setShouldIgnoreKeys(false);
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
        JMenuItem item = new JMenuItem(Messages.getString("Gamemaster.Traitor.text", entity.getDisplayName()));
        item.addActionListener(evt -> {
            gui.getBoardView().setShouldIgnoreKeys(false);
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
                    Messages.getString("Gamemaster.Traitor.text.noplayers"));
                return;
            }

            // Dialog for choosing which player to transfer to
            String option = (String) JOptionPane.showInputDialog(gui.getFrame(),
                Messages.getString("Gamemaster.Traitor.text.selectplayer", entity.getDisplayName()),
                Messages.getString("Gamemaster.Traitor.title"), JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);

            // Verify that we have a valid option...
            if (option != null) {
                // Now that we've selected a player, correctly associate the ID and name
                int id = playerIds[Arrays.asList(options).indexOf(option)];
                String name = playerNames[Arrays.asList(options).indexOf(option)];

                // And now we perform the actual transfer
                int confirm = JOptionPane.showConfirmDialog(
                    gui.getFrame(),
                    Messages.getString("Gamemaster.Traitor.confirmation", entity.getDisplayName(), name),
                    Messages.getString("Gamemaster.Traitor.confirm"),
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
        JMenuItem item = new JMenuItem(Messages.getString(messageKey, entity.getDisplayName()));
        item.addActionListener(evt -> {
            int confirm = JOptionPane.showConfirmDialog(
                gui.getFrame(), Messages.getString(confirmationKey, entity.getDisplayName()),
                Messages.getString("Gamemaster.dialog.confirm"), JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                client.sendChat(command);
            }
        });
        return item;
    }

    private JMenu createSelectMenu() {
        JMenu menu = new JMenu("Select");
        // add select options
        if (canSelectEntities()) {
            for (Entity entity : client.getGame().getEntitiesVector(coords, canTargetEntities())) {
                if (client.getMyTurn().isValidEntity(entity, client.getGame())) {
                    menu.add(selectJMenuItem(entity));
                }
            }
        }
        return menu;
    }

    private JMenu createViewMenu() {
        JMenu menu = new JMenu("View");
        Game game = client.getGame();

        Player localPlayer = client.getLocalPlayer();

        for (Entity entity : game.getEntitiesVector(coords, true)) {
            // Only add the unit if it's actually visible
            // With double blind on, the game may unseen units
            if (!entity.isSensorReturn(localPlayer) && entity.hasSeenEntity(localPlayer)) {
                menu.add(viewJMenuItem(entity));
                menu.add(viewReadoutJMenuItem(entity));
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
            JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.MoveEnvelope"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_ENVELOPE.getCmd());
            item.addActionListener(evt -> {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });
            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.moveChaff"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_CHAFF.getCmd());
            item.addActionListener(evt -> {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });
            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.butWalk"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_WALK.getCmd());
            item.addActionListener(evt -> {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });
            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.butBackup"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_BACK_UP.getCmd());
            item.addActionListener(evt -> {
                try {
                    ((MovementDisplay) currentPanel).actionPerformed(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });

            menu.add(item);

            if (myEntity.getJumpMP() > 0) {
                item = new JMenuItem(Messages.getString("CommonMenuBar.moveJump"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_JUMP.getCmd());
                item.addActionListener(evt -> {
                    try {
                        ((MovementDisplay) currentPanel).actionPerformed(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
                    }
                });
                menu.add(item);
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_EVADE)) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butEvade"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_EVADE.getCmd());
                item.addActionListener(evt -> {
                    try {
                        ((MovementDisplay) currentPanel).actionPerformed(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
                    }
                });
                menu.add(item);
            }

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_VEHICLE_ADVANCED_MANEUVERS)) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butEvade"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_BOOTLEGGER.getCmd());
                item.addActionListener(evt -> {
                    try {
                        ((MovementDisplay) currentPanel).actionPerformed(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
                    }
                });
                menu.add(item);
            }

            if (game.getPlanetaryConditions().isRecklessConditions()
                    && !game.getBoard().inSpace()
                    && !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN)) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butReckless"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_RECKLESS.getCmd());
                item.addActionListener(evt -> {
                    try {
                        ((MovementDisplay) currentPanel).actionPerformed(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
                    }
                });
                menu.add(item);
            }
        } else {
            JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butWalk"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_WALK.getCmd());
            item.addActionListener(evt -> {
                try {
                    plotCourse(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });

            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.butBackup"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_BACK_UP.getCmd());
            item.addActionListener(evt -> {
                try {
                    plotCourse(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });

            menu.add(item);

            if (myEntity.getJumpMP() > 0) {
                item = new JMenuItem(Messages.getString("CommonMenuBar.moveJump"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_JUMP.getCmd());
                item.addActionListener(evt -> {
                    try {
                        plotCourse(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
                    }
                });
                menu.add(item);
            }

            item = new JMenuItem(Messages.getString("MovementDisplay.moveLongestRun"));

            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_LONGEST_RUN.getCmd());
            item.addActionListener(evt -> {
                try {
                    plotCourse(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });

            menu.add(item);

            item = new JMenuItem(Messages.getString("MovementDisplay.moveLongestWalk"));
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_LONGEST_WALK.getCmd());
            item.addActionListener(evt -> {
                try {
                    plotCourse(evt);
                } catch (Exception ex) {
                    logger.error(ex, "");
                }
            });

            menu.add(item);

            if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_EVADE)) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butEvade"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_EVADE.getCmd());
                item.addActionListener(evt -> {
                    try {
                        plotCourse(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
                    }
                });
                menu.add(item);
            }

            if (game.getPlanetaryConditions().isRecklessConditions()
                    && !game.getBoard().inSpace()
                    && !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_NIGHT_MOVE_PEN)) {
                item = new JMenuItem(Messages.getString("MovementDisplay.butReckless"));
                item.setActionCommand(MovementDisplay.MoveCommand.MOVE_RECKLESS.getCmd());
                item.addActionListener(evt -> {
                    try {
                        plotCourse(evt);
                    } catch (Exception ex) {
                        logger.error(ex, "");
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
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TURN_RIGHT.getCmd());
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        menu.add(item);

        item = new JMenuItem(Messages.getString("MovementDisplay.butTurnLeft"));
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TURN_LEFT.getCmd());
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        menu.add(item);

        item = new JMenuItem("About Face");
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_TURN_RIGHT.getCmd());
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
                ((MovementDisplay) currentPanel).actionPerformed(evt);
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        menu.add(item);

        return menu;
    }

    private JMenu createWeaponsFireMenu() {
        JMenu menu = new JMenu("Weapons");

        // Hidden entities are not allowed to shoot without being revealed
        // so let's not give them the option
        if (myEntity.isHidden()) {
            return menu;
        }

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
        item.addActionListener(evt -> {
            try {
                ((FiringDisplay) currentPanel).nextWeapon();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem createAlphaStrikeJMenuItem() {
        JMenuItem item = new JMenuItem("Alpha Strike");
        item.addActionListener(evt -> {
            try {
                FiringDisplay panel = (FiringDisplay) currentPanel;
                // Get all weapons
                List<WeaponMounted> weapons = myEntity.getWeaponList();
                // We will need to map a Mounted to its weapon number
                HashMap<WeaponMounted, Integer> weapToId = new HashMap<>();
                for (WeaponMounted weapon : weapons) {
                    weapToId.put(weapon, myEntity.getEquipmentNum(weapon));
                }
                // Sort weapons from high damage to low
                weapons.sort(new WeaponComparatorDamage(false));

                Targetable target = panel.getTarget();
                for (WeaponMounted weapon : weapons) {
                    // If the weapon has been used at all this turn, ignore
                    if (!weapon.usedInPhase().isUnknown()) {
                        continue;
                    }
                    int weaponNum = weapToId.get(weapon);
                    // Used to determine if attack is valid
                    WeaponAttackAction waa = new WeaponAttackAction(myEntity.getId(),
                            target.getTargetType(), target.getId(), weaponNum);
                    // Only fire weapons that have a chance to hit
                    int toHitVal = waa.toHit(game).getValue();
                    if (toHitVal <= 12) {
                        gui.getUnitDisplay().wPan.selectWeapon(weaponNum);
                        panel.fire();
                    }
                }
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem createFlipArmsJMenuItem() {
        JMenuItem item = new JMenuItem("Flip Arms");

        item.setActionCommand(Integer.toString(myEntity.getId()));
        item.addActionListener(evt -> {
            try {
                FiringDisplay display = (FiringDisplay) currentPanel;

                int id = Integer.parseInt(evt.getActionCommand());
                display.updateFlipArms(!game.getEntity(id).getArmsFlipped());
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem createFireJMenuItem() {
        JMenuItem item = new JMenuItem("Fire");
        item.addActionListener(evt -> {
            try {
                ((FiringDisplay) currentPanel).fire();
            } catch (Exception ex) {
                logger.error(ex, "");
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

            if (!myEntity.isHullDown() && !myEntity.isProne() && !myEntity.hasHipCrit()) {
                menu.add(createKickJMenuItem());
                menu.add(createTripJMenuItem());
            }

            if ((myEntity instanceof BipedMek)
                    && (!myEntity.isLocationBad(Mek.LOC_LARM) || !myEntity.isLocationBad(Mek.LOC_RARM))) {
                menu.add(createPunchJMenuItem());
            }

            if ((myEntity instanceof BipedMek)
                    && !myEntity.isLocationBad(Mek.LOC_LARM)
                    && !myEntity.isLocationBad(Mek.LOC_RARM)) {
                menu.add(createPushJMenuItem());
            }

            if (myEntity.getJumpMP() > 0) {
                menu.add(createJumpJetAttackJMenuItem());
            }

            if (myEntity.isProne()) {
                menu.add(createThrashJMenuItem());
            }

            menu.add(createDodgeJMenuItem());

            if (!myEntity.getClubs().isEmpty()) {
                JMenu clubMenu = createClubMenu();

                if (clubMenu.getItemCount() > 0) {
                    menu.add(clubMenu);
                }
            }

            ToHitData grap = GrappleAttackAction.toHit(client.getGame(), myEntity.getId(), myTarget);
            ToHitData bgrap = BreakGrappleAttackAction.toHit(client.getGame(), myEntity.getId(), myTarget);
            if ((grap.getValue() != TargetRoll.IMPOSSIBLE) || (bgrap.getValue() != TargetRoll.IMPOSSIBLE)) {
                menu.add(createGrappleJMenuItem());
            }
            if (myTarget != null) {
                ToHitData vibro = BAVibroClawAttackAction.toHit(client.getGame(), myEntity.getId(), myTarget);
                if (vibro.getValue() != TargetRoll.IMPOSSIBLE) {
                    menu.add(createVibroClawMenuItem());
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
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butUp"));

        if (carefulStand) {
            item.setText("Careful Stand");
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_CAREFUL_STAND.getCmd());
        } else {
            item.setActionCommand(MovementDisplay.MoveCommand.MOVE_GET_UP.getCmd());
        }
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });

        return item;
    }

    private JMenuItem createHullDownJMenuItem() {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butHullDown"));
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_HULL_DOWN.getCmd());
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createProneJMenuItem() {
        JMenuItem item = new JMenuItem(Messages.getString("MovementDisplay.butDown"));
        item.setActionCommand(MovementDisplay.MoveCommand.MOVE_GO_PRONE.getCmd());
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenu createConvertMenu() {
        JMenu menu = new JMenu(Messages.getString("MovementDisplay.moveModeConvert"));

        if (myEntity instanceof Mek && ((Mek) myEntity).hasTracks()) {
            menu.add(createConvertMenuItem("MovementDisplay.moveModeLeg",
                    MovementDisplay.MoveCommand.MOVE_MODE_LEG, false));
            menu.add(createConvertMenuItem("MovementDisplay.moveModeTrack",
                    MovementDisplay.MoveCommand.MOVE_MODE_VEE, false));
        } else if (myEntity instanceof QuadVee) {
            menu.add(createConvertMenuItem("MovementDisplay.moveModeMek",
                    MovementDisplay.MoveCommand.MOVE_MODE_LEG,
                    myEntity.getConversionMode() == QuadVee.CONV_MODE_MEK));
            menu.add(createConvertMenuItem("MovementDisplay.moveModeVee",
                    MovementDisplay.MoveCommand.MOVE_MODE_VEE,
                    myEntity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE));
        } else if (myEntity instanceof LandAirMek) {
            int currentMode = myEntity.getConversionMode();
            JMenuItem item = createConvertMenuItem("MovementDisplay.moveModeMek",
                    MovementDisplay.MoveCommand.MOVE_MODE_LEG,
                    currentMode == LandAirMek.CONV_MODE_MEK);
            item.setEnabled(currentMode == LandAirMek.CONV_MODE_MEK
                    || ((LandAirMek) myEntity).canConvertTo(currentMode, LandAirMek.CONV_MODE_MEK));
            menu.add(item);
            if (((LandAirMek) myEntity).getLAMType() == LandAirMek.LAM_STANDARD) {
                item = createConvertMenuItem("MovementDisplay.moveModeAirMek",
                        MovementDisplay.MoveCommand.MOVE_MODE_VEE,
                        currentMode == LandAirMek.CONV_MODE_AIRMEK);
                item.setEnabled(currentMode == LandAirMek.CONV_MODE_AIRMEK
                        || ((LandAirMek) myEntity).canConvertTo(currentMode, LandAirMek.CONV_MODE_AIRMEK));
                menu.add(item);
            }
            item = createConvertMenuItem("MovementDisplay.moveModeFighter",
                    MovementDisplay.MoveCommand.MOVE_MODE_AIR,
                    currentMode == LandAirMek.CONV_MODE_FIGHTER);
            item.setEnabled(currentMode == LandAirMek.CONV_MODE_FIGHTER
                    || ((LandAirMek) myEntity).canConvertTo(currentMode, LandAirMek.CONV_MODE_FIGHTER));
            menu.add(item);
        }
        return menu;
    }

    private JMenuItem createConvertMenuItem(String resourceKey, MovementDisplay.MoveCommand cmd,
            boolean isCurrent) {
        String text = Messages.getString(resourceKey);
        if (isCurrent) {
            text = "No Conversion";
        }
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(cmd.getCmd());
        item.addActionListener(evt -> {
            try {
                ((MovementDisplay) currentPanel).actionPerformed(evt);
            } catch (Exception ex) {
                logger.error(ex, "");
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

        // VTOLs/AirMeks making strafing or bombing attacks already declared the target
        // hex(es)
        // in the movement phase and cannot change them.
        if (myEntity.isMakingVTOLGroundAttack()) {
            menu.setEnabled(false);
            return menu;
        }

        final boolean isFiringDisplay = (currentPanel instanceof FiringDisplay);
        final boolean isTargetingDisplay = (currentPanel instanceof TargetingPhaseDisplay);
        final boolean canStartFires = client.getGame().getOptions()
                .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_START_FIRE);

        Player localPlayer = client.getLocalPlayer();

        // Add menu item to target each entity in the coords
        for (Entity entity : client.getGame().getEntitiesVector(coords)) {
            // Only add the unit if it's actually visible
            // With double blind on, the game may have unseen units
            if (!entity.isSensorReturn(localPlayer)
                    && entity.hasSeenEntity(localPlayer)
                    && !entity.isHidden()) {
                menu.add(TargetMenuItem(entity));
            }
        }

        Hex h = board.getHex(coords);
        // If the hex is null, we're done here
        if (h == null) {
            return menu;
        }

        // Clearing hexes and igniting hexes
        if (isFiringDisplay && !board.inSpace() && !board.inAtmosphere()) {
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

        if (isFiringDisplay) {
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
        }
        // Check for Mine Clearance
        if (isFiringDisplay || isTargetingDisplay) {
            if (client.getGame().containsMinefield(coords)) {
                menu.add(TargetMenuItem(new MinefieldTarget(coords)));
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
                        || hasAmmoType(AmmoType.T_THUMPER)
                        || hasAmmoType(AmmoType.T_BA_TUBE))) {
            menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY)));
        }
        // Check for adding TAG targeting buildings and hexes
        if (isTargetingDisplay && myEntity.hasTAG() && !board.inSpace()) {
            menu.add(TargetMenuItem(new HexTarget(coords, Targetable.TYPE_HEX_TAG)));
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

        // Cursor over the hex.
        gui.getBoardView().mouseAction(coords, BoardViewEvent.BOARD_HEX_CURSOR, InputEvent.BUTTON1_DOWN_MASK,
                MouseEvent.BUTTON1);
        // Click
        gui.getBoardView().mouseAction(coords, BoardViewEvent.BOARD_HEX_CLICKED, InputEvent.BUTTON1_DOWN_MASK,
                MouseEvent.BUTTON1);
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
        if (myEntity.getAmmo().isEmpty()) {
            return false;
        }

        for (Mounted<?> ammo : myEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getAmmoType() == ammoType) {
                return true;
            }
        }

        return false;
    }

    private boolean hasWeaponFlag(EquipmentFlag weaponFlag) {
        if (myEntity.getWeaponList().isEmpty()) {
            return false;
        }

        for (Mounted<?> wpn : myEntity.getWeaponList()) {
            if (wpn.getType().hasFlag(weaponFlag)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMunitionType(AmmoType.Munitions munition) {
        if (myEntity.getAmmo().isEmpty()) {
            return false;
        }

        for (Mounted<?> ammo : myEntity.getAmmo()) {
            if (((AmmoType) ammo.getType()).getMunitionType().contains(munition)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasFireExtinguisher() {
        if (myEntity.getWeaponList().isEmpty()) {
            return false;
        }

        for (Mounted<?> weapon : myEntity.getWeaponList()) {
            if ((weapon.getType() instanceof ISFireExtinguisher)
                    || (weapon.getType() instanceof CLFireExtinguisher)) {
                return true;
            }
        }

        return false;
    }

    private JMenuItem createTorsoTwistJMenuItem(int direction) {
        JMenuItem item = new JMenuItem((direction == 1) ? "Right" : "Left");
        item.setActionCommand(Integer.toString(direction));
        item.addActionListener(evt -> {
            try {
                int twistDir = Integer.parseInt(evt.getActionCommand());
                if (currentPanel instanceof FiringDisplay) {
                    ((FiringDisplay) currentPanel).torsoTwist(twistDir);
                }
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createTorsoTwistJMenuItem(Coords twistCoords) {
        JMenuItem item = new JMenuItem("Twist");
        item.setActionCommand(twistCoords.getX() + "|" + twistCoords.getY());
        item.addActionListener(evt -> {
            try {
                StringTokenizer result = new StringTokenizer(evt.getActionCommand(), "|");
                Coords coord = new Coords(Integer.parseInt(result.nextToken()),
                        Integer.parseInt(result.nextToken()));
                if (currentPanel instanceof FiringDisplay) {
                    ((FiringDisplay) currentPanel).torsoTwist(coord);
                }
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createRotateTurretJMenuItem(final Mek mek, final Mounted<?> turret) {
        String turretString;
        if (turret.getType().hasFlag(MiscType.F_SHOULDER_TURRET)) {
            turretString = "Rotate Shoulder Turret (" + mek.getLocationAbbr(turret.getLocation()) + ")";
        } else if (turret.getType().hasFlag(MiscType.F_HEAD_TURRET)) {
            turretString = "Rotate Head Turret";
        } else {
            turretString = "Rotate Quad Turret";
        }
        JMenuItem item = new JMenuItem(turretString);
        item.addActionListener(evt -> {
            TurretFacingDialog tfe = new TurretFacingDialog(gui.frame, mek, turret, gui);
            tfe.setVisible(true);
        });
        return item;
    }

    private JMenuItem createRotateDualTurretJMenuItem(final Tank tank) {
        String turretString = "Rotate Front Turret";
        JMenuItem item = new JMenuItem(turretString);
        item.addActionListener(evt -> {
            TurretFacingDialog tfe = new TurretFacingDialog(gui.frame, tank, gui);
            tfe.setVisible(true);
        });
        return item;
    }

    private JMenu createTorsoTwistMenu() {
        JMenu menu = new JMenu();

        if (myEntity instanceof BipedMek) {
            menu.setText("Torso Twist");
            if (coords.equals(myEntity.getPosition())) {
                menu.add(createTorsoTwistJMenuItem(1));
                menu.add(createTorsoTwistJMenuItem(0));
            } else {
                menu.add(createTorsoTwistJMenuItem(coords));
            }
        } else if ((myEntity instanceof Tank)
                && (myEntity.getInternal(((Tank) myEntity).getLocTurret()) > -1)) {
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
        if (myEntity instanceof Mek) {
            for (Mounted<?> mount : myEntity.getMisc()) {
                if (mount.getType().hasFlag(MiscType.F_SHOULDER_TURRET)
                        || mount.getType().hasFlag(MiscType.F_HEAD_TURRET)
                        || mount.getType().hasFlag(MiscType.F_QUAD_TURRET)) {
                    menu.add(createRotateTurretJMenuItem((Mek) myEntity, mount));
                }
            }
        }
        return menu;
    }

    private void selectTarget() {
        Vector<Entity> list = new Vector<>();

        Player localPlayer = client.getLocalPlayer();
        boolean friendlyFire = (game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE));

        for (Entity en : game.getEntitiesVector(coords)) {
            // Only add the unit if it's actually visible
            // With double blind on, the game may have unseen units
            if ((en.isEnemyOf(myEntity) || friendlyFire) && !en.equals(myEntity)
                    && !en.isSensorReturn(localPlayer)
                    && en.hasSeenEntity(localPlayer)
                    && !en.isHidden()) {
                list.add(en);
            }
        }

        if (list.size() == 1) {
            myTarget = selectedEntity = list.firstElement();
            if (currentPanel instanceof FiringDisplay panel) {
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

        int weaponNum = gui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted<?> mounted = myEntity.getEquipment(weaponNum);

        if ((mounted != null) && mounted.hasModes()) {
            for (int pos = 0; pos < mounted.getModesCount(); pos++) {
                menu.add(createModeJMenuItem(mounted, pos));
            }
        }

        return menu;
    }

    private JMenuItem createModeJMenuItem(Mounted<?> mounted, int position) {
        JMenuItem item = new JMenuItem();

        EquipmentMode mode = mounted.getType().getMode(position);

        if (mode.equals(mounted.curMode())) {
            item.setText("* " + mode.getDisplayableName());
        } else {
            item.setText(mode.getDisplayableName());
        }
        item.setActionCommand(Integer.toString(position));
        item.addActionListener(evt -> {
            try {
                int modePosition = Integer.parseInt(evt.getActionCommand());
                int weaponNum = gui.getUnitDisplay().wPan.getSelectedWeaponNum();
                Mounted<?> equip = myEntity.getEquipment(weaponNum);
                equip.setMode(modePosition);
                client.sendModeChange(myEntity.getId(), weaponNum, modePosition);
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createPunchJMenuItem() {
        JMenuItem item = new JMenuItem("Punch");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).punch();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createKickJMenuItem() {
        JMenuItem item = new JMenuItem("Kick");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).kick();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createPushJMenuItem() {
        JMenuItem item = new JMenuItem("Push");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).push();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createVibroClawMenuItem() {
        JMenuItem item = new JMenuItem("Vibro Claw Attack");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).vibroclawatt();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createJumpJetAttackJMenuItem() {
        JMenuItem item = new JMenuItem("Jump Jet Attack");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).jumpjetatt();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createThrashJMenuItem() {
        JMenuItem item = new JMenuItem("Thrash");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).thrash();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createGrappleJMenuItem() {
        JMenuItem item = new JMenuItem("Grapple");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).doGrapple();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createTripJMenuItem() {
        JMenuItem item = new JMenuItem("Trip");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).trip();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenuItem createDodgeJMenuItem() {
        JMenuItem item = new JMenuItem("Dodge");
        item.addActionListener(evt -> {
            try {
                ((PhysicalDisplay) currentPanel).dodge();
            } catch (Exception ex) {
                logger.error(ex, "");
            }
        });
        return item;
    }

    private JMenu createClubMenu() {
        JMenu menu = new JMenu("Weapon");

        for (int pos = 0; pos < myEntity.getClubs().size(); pos++) {
            Mounted<?> club = myEntity.getClubs().get(pos);
            if (!club.isDestroyed()) {
                menu.add(createClubJMenuItem(club.getName(), pos));
            }
        }
        return menu;
    }

    private JMenuItem createClubJMenuItem(String clubName, int clubNumber) {
        JMenuItem item = new JMenuItem(clubName);
        item.setActionCommand(Integer.toString(clubNumber));
        item.addActionListener(evt -> {
            try {
                MiscMounted club = myEntity.getClubs().get(
                        Integer.parseInt(evt.getActionCommand()));
                ((PhysicalDisplay) currentPanel).club(club);
            } catch (Exception ex) {
                logger.error(ex, "");
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
