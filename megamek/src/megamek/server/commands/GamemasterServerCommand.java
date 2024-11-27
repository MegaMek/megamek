/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.server.commands;

import megamek.client.ui.Messages;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.*;

/**
 * A ServerCommand that can only be used by Game Masters,
 * This abstract class implements many features that are common to all Game Master commands,
 * like the isGM check for users, it also uses the Argument class for building the command arguments
 * and to abstract the parsing of the arguments, limit assertion and error handling, and for building
 * a more dynamic "help" feature.
 * It also has a more advanced parser and argument handling than the ServerCommand class, which allows for
 * named arguments, positional arguments, optional arguments and default values.
 * named arguments can be passed in any order, and positional arguments are parsed in order and MUST appear before named
 * arguments.
 *
 * @author Luana Coppio
 */
public abstract class GamemasterServerCommand extends ServerCommand {
    private static final String NEWLINE = "\n";
    private static final String WHITESPACE = " ";
    private static final String LONG_WHITESPACE = "   ";
    private static final String EMPTY_ARGUMENT = null;
    protected final TWGameManager gameManager;
    protected final static MMLogger logger = MMLogger.create(GamemasterServerCommand.class);
    private final String errorMsg;
    private final String longName;

    /**
     * Creates new ServerCommand that can only be used by Game Masters
     *
     * @param server        instance of the server
     * @param gameManager   instance of the game manager
     * @param name          the name of the command
     * @param helpText      the help text for the command
     */
    public GamemasterServerCommand(Server server, TWGameManager gameManager, String name, String helpText, String longName) {
        super(server, name, helpText);
        this.gameManager = gameManager;
        this.errorMsg = "Error executing command: " + name;
        this.longName = longName;
    }

    private boolean isGM(int connId) {
        return server.getGameManager().getGame().getPlayer(connId).getGameMaster();
    }

    protected TWGameManager getGameManager() {
        return gameManager;
    }

    @Override
    public void run(int connId, String[] args) {
        if (!isGM(connId)) {
            server.sendServerChat(connId, "This command can only be used by a game master.");
            return;
        }

        try {
            Map<String, Argument<?>> parsedArguments = parseArguments(args);
            runAsGM(connId, parsedArguments);
        } catch (IllegalArgumentException e) {
            server.sendServerChat(connId, "Invalid arguments: " + e.getMessage() + "\nUsage: " + this.getHelp());
        } catch (Exception e) {
            server.sendServerChat(connId, "An error occurred while executing the command. Check the log for more information");
            logger.error(errorMsg, e);
        }
    }

    // Method to parse arguments, to be implemented by the specific command class
    public abstract List<Argument<?>> defineArguments();


    // Parses the arguments using the definition
    private Map<String, Argument<?>> parseArguments(String[] args) {

        List<Argument<?>> argumentDefinitions = defineArguments();
        Map<String, Argument<?>> parsedArguments = new HashMap<>();
        List<String> positionalArguments = new ArrayList<>();

        // Map argument names to definitions for easy lookup
        Map<String, Argument<?>> argumentMap = new HashMap<>();
        for (Argument<?> argument : argumentDefinitions) {
            argumentMap.put(argument.getName(), argument);
        }

        // Separate positional arguments and named arguments
        boolean namedArgumentStarted = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            String[] keyValue = arg.split("=");

            if (keyValue.length == 2) {
                // Handle named arguments
                namedArgumentStarted = true;
                String key = keyValue[0];
                String value = keyValue[1];

                if (!argumentMap.containsKey(key)) {
                    throw new IllegalArgumentException("Unknown argument: " + key);
                }

                Argument<?> argument = argumentMap.get(key);
                argument.parse(value);
                parsedArguments.put(key, argument);
            } else {
                // Handle positional arguments
                if (namedArgumentStarted) {
                    throw new IllegalArgumentException("Positional arguments cannot come after named arguments.");
                }
                positionalArguments.add(arg);
            }
        }

        // Parse positional arguments
        int index = 0;
        for (Argument<?> argument : argumentDefinitions) {
            if (parsedArguments.containsKey(argument.getName())) {
                continue;
            }
            if (index < positionalArguments.size()) {
                String value = positionalArguments.get(index);
                argument.parse(value);
                parsedArguments.put(argument.getName(), argument);
                index++;
            } else {
                // designed to throw an error if the arg doesn't have a default value
                argument.parse(EMPTY_ARGUMENT);
                parsedArguments.put(argument.getName(), argument);
            }
        }

        return parsedArguments;
    }

    public String getHelpHtml() {
        return "<html>" +
            this.getHelp()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll(LONG_WHITESPACE, "| ")
                .replaceAll(NEWLINE, "<br>")+
            "</html>";
    }

    @Override
    public String getHelp() {
        StringBuilder help = new StringBuilder();
        help.append(super.getHelp())
            .append(NEWLINE)
            .append(Messages.getString("Gamemaster.cmd.help"))
            .append(NEWLINE)
            .append(NEWLINE)
            .append("/")
            .append(getName());

        for (Argument<?> arg : defineArguments()) {
            help.append(WHITESPACE)
                .append(arg.getRepr());
        }

        help.append(NEWLINE)
            .append(NEWLINE);

        for (var arg : defineArguments()) {
            help.append(LONG_WHITESPACE)
                .append(arg.getName())
                .append(":")
                .append(WHITESPACE)
                .append(arg.getHelp())
                .append(NEWLINE);
        }
        return help.toString();
    }

    public String getLongName() {
        return longName;
    }

    // The new method for game master commands that uses parsed arguments
    protected abstract void runAsGM(int connId, Map<String, Argument<?>> args);
}
