/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.utilities.botorders;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.utilities.botorders.ScriptedOrder.OrderAction;
import megamek.utilities.botorders.ScriptedOrder.TargetKind;

/**
 * A bot orders script: a plain text file of {@link ScriptedOrder}s, one per line, read next to a scenario file.
 *
 * <p>Format, one order per line, fields separated by {@code |}; blank lines and lines starting with {@code #} are
 * ignored:</p>
 * <pre>
 * round 2 | unit "Champion CHP-2N" | waypoints 1501 1305
 * round 2 | unit id 101 | cripple
 * round 2 | unit id 102 | damage internal 60%
 * round 3 | bot "Lyran" | flee NORTH
 * round 4 | unit id 5 | clear
 * round 4 | all | add waypoints 1610
 * </pre>
 *
 * <p>Hexes are MegaMek board numbers (column then row, 1-based): {@code 1501} is column 15, row 1. Boards wider or
 * taller than 99 hexes use six digits.</p>
 */
public final class ScenarioOrderScript {

    /** The file extension of an orders script, which sits next to the scenario with the same base name. */
    public static final String ORDERS_EXTENSION = ".orders";

    private static final Pattern ROUND_PATTERN = Pattern.compile("(?i)round\\s+(\\d+)");
    private static final Pattern UNIT_ID_PATTERN = Pattern.compile("(?i)unit\\s+id\\s+(\\d+)");
    private static final Pattern UNIT_NAME_PATTERN = Pattern.compile("(?i)unit\\s+\"([^\"]+)\"");
    private static final Pattern BOT_PATTERN = Pattern.compile("(?i)bot\\s+\"([^\"]+)\"");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+)%?");

    private final List<ScriptedOrder> orders;
    private final File sourceFile;

    private ScenarioOrderScript(List<ScriptedOrder> orders, File sourceFile) {
        this.orders = Collections.unmodifiableList(orders);
        this.sourceFile = sourceFile;
    }

    /**
     * Returns the orders script next to the given scenario ({@code name.mms} to {@code name.orders}), or {@code null}
     * when there is none.
     *
     * @param scenarioFile the scenario file
     *
     * @return the orders file, or {@code null} if it does not exist
     */
    public static @Nullable File findFor(File scenarioFile) {
        String scenarioName = scenarioFile.getName();
        int extensionStart = scenarioName.lastIndexOf('.');
        String baseName = (extensionStart > 0) ? scenarioName.substring(0, extensionStart) : scenarioName;
        File ordersFile = new File(scenarioFile.getAbsoluteFile().getParentFile(), baseName + ORDERS_EXTENSION);
        return ordersFile.isFile() ? ordersFile : null;
    }

    /**
     * Reads and parses an orders script.
     *
     * @param ordersFile the file to read
     *
     * @return the parsed script
     *
     * @throws IOException              when the file cannot be read
     * @throws IllegalArgumentException when a line cannot be parsed; the message names the line
     */
    public static ScenarioOrderScript load(File ordersFile) throws IOException {
        List<String> lines = Files.readAllLines(ordersFile.toPath(), StandardCharsets.UTF_8);
        List<ScriptedOrder> orders = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            orders.add(parseLine(line, index + 1));
        }
        return new ScenarioOrderScript(orders, ordersFile);
    }

    /**
     * Parses a single script line.
     *
     * @param line       the line, trimmed
     * @param lineNumber the 1-based line number, for messages
     *
     * @return the order
     *
     * @throws IllegalArgumentException when the line cannot be parsed
     */
    static ScriptedOrder parseLine(String line, int lineNumber) {
        String[] fields = line.split("\\|");
        if (fields.length != 3) {
            throw new IllegalArgumentException(problem(lineNumber, line, "expected 3 fields separated by |"));
        }

        Matcher roundMatcher = ROUND_PATTERN.matcher(fields[0].trim());
        if (!roundMatcher.matches()) {
            throw new IllegalArgumentException(problem(lineNumber, line, "first field must be 'round N'"));
        }
        int round = Integer.parseInt(roundMatcher.group(1));

        String selector = fields[1].trim();
        TargetKind targetKind;
        String targetValue;
        Matcher unitIdMatcher = UNIT_ID_PATTERN.matcher(selector);
        Matcher unitNameMatcher = UNIT_NAME_PATTERN.matcher(selector);
        Matcher botMatcher = BOT_PATTERN.matcher(selector);
        if (unitIdMatcher.matches()) {
            targetKind = TargetKind.UNIT_ID;
            targetValue = unitIdMatcher.group(1);
        } else if (unitNameMatcher.matches()) {
            targetKind = TargetKind.UNIT_NAME;
            targetValue = unitNameMatcher.group(1);
        } else if (botMatcher.matches()) {
            targetKind = TargetKind.BOT;
            targetValue = botMatcher.group(1);
        } else if (selector.equalsIgnoreCase("all")) {
            targetKind = TargetKind.ALL;
            targetValue = "";
        } else {
            throw new IllegalArgumentException(problem(lineNumber, line,
                  "second field must be unit \"name\", unit id N, bot \"name\" or all"));
        }

        String[] words = fields[2].trim().split("\\s+");
        String verb = words[0].toLowerCase(Locale.ROOT);
        OrderAction action;
        int firstArgument = 1;
        switch (verb) {
            case "waypoints", "waypoint", "route" -> action = OrderAction.WAYPOINTS;
            case "add" -> {
                action = OrderAction.ADD_WAYPOINTS;
                boolean hasNoun = (words.length > 1) && words[1].toLowerCase(Locale.ROOT).startsWith("waypoint");
                firstArgument = hasNoun ? 2 : 1;
            }
            case "clear" -> action = OrderAction.CLEAR;
            case "flee" -> action = OrderAction.FLEE;
            case "cripple" -> action = OrderAction.CRIPPLE;
            case "damage" -> {
                action = OrderAction.DAMAGE_INTERNAL;
                boolean hasNoun = (words.length > 1) && words[1].equalsIgnoreCase("internal");
                firstArgument = hasNoun ? 2 : 1;
            }
            default -> throw new IllegalArgumentException(problem(lineNumber, line, "unknown action " + verb
                  + " (waypoints, add waypoints, clear, flee, cripple, damage internal N%)"));
        }

        List<String> arguments = new ArrayList<>();
        for (int index = firstArgument; index < words.length; index++) {
            arguments.add(words[index]);
        }
        validateArguments(action, arguments, lineNumber, line);
        return new ScriptedOrder(round, targetKind, targetValue, action, List.copyOf(arguments), lineNumber, line);
    }

    private static void validateArguments(OrderAction action, List<String> arguments, int lineNumber, String line) {
        switch (action) {
            case WAYPOINTS, ADD_WAYPOINTS -> {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException(problem(lineNumber, line, "waypoints need at least one hex"));
                }
                for (String hexNumber : arguments) {
                    parseHexNumber(hexNumber);
                }
            }
            case FLEE -> {
                if (arguments.size() != 1) {
                    throw new IllegalArgumentException(problem(lineNumber, line, "flee needs one edge"));
                }
            }
            case DAMAGE_INTERNAL -> {
                if ((arguments.size() != 1) || !PERCENT_PATTERN.matcher(arguments.getFirst()).matches()) {
                    throw new IllegalArgumentException(problem(lineNumber, line, "damage internal needs N%"));
                }
            }
            case CLEAR, CRIPPLE -> {
                // these take no arguments
            }
        }
    }

    /**
     * Parses a MegaMek board hex number into board coordinates: {@code 1501} is column 15, row 1, which is
     * {@code Coords(14, 0)}. Six digits split three and three.
     *
     * @param hexNumber the hex number
     *
     * @return the coordinates
     *
     * @throws IllegalArgumentException when the text is not four or six digits
     */
    public static Coords parseHexNumber(String hexNumber) {
        if (!hexNumber.matches("\\d{4}|\\d{6}")) {
            throw new IllegalArgumentException("Not a hex number: " + hexNumber);
        }
        int half = hexNumber.length() / 2;
        int column = Integer.parseInt(hexNumber.substring(0, half));
        int row = Integer.parseInt(hexNumber.substring(half));
        return new Coords(column - 1, row - 1);
    }

    /**
     * Formats board coordinates as a MegaMek hex number, the reverse of {@link #parseHexNumber(String)}.
     *
     * @param coords the coordinates, or {@code null}
     *
     * @return the hex number, or an empty string for {@code null}
     */
    public static String toHexNumber(@Nullable Coords coords) {
        if (coords == null) {
            return "";
        }
        if ((coords.getX() >= 99) || (coords.getY() >= 99)) {
            return String.format("%03d%03d", coords.getX() + 1, coords.getY() + 1);
        }
        return String.format("%02d%02d", coords.getX() + 1, coords.getY() + 1);
    }

    /**
     * Returns the percentage of a {@code DAMAGE_INTERNAL} order.
     *
     * @param order the order
     *
     * @return the percentage, 0 to 100
     */
    public static int damagePercent(ScriptedOrder order) {
        Matcher matcher = PERCENT_PATTERN.matcher(order.arguments().getFirst());
        if (!matcher.matches()) {
            return 0;
        }
        return Math.max(0, Math.min(100, Integer.parseInt(matcher.group(1))));
    }

    private static String problem(int lineNumber, String line, String reason) {
        return "Orders script line " + lineNumber + ": " + reason + ": " + line;
    }

    /**
     * @return every order in file order
     */
    public List<ScriptedOrder> getOrders() {
        return orders;
    }

    /**
     * @param round the game round
     *
     * @return the orders for that round, in file order
     */
    public List<ScriptedOrder> getOrdersForRound(int round) {
        List<ScriptedOrder> roundOrders = new ArrayList<>();
        for (ScriptedOrder order : orders) {
            if (order.round() == round) {
                roundOrders.add(order);
            }
        }
        return roundOrders;
    }

    /**
     * @return the file this script was read from
     */
    public File getSourceFile() {
        return sourceFile;
    }
}
