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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.princess.UnitBehavior;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.common.units.UnitLocation;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

/**
 * Writes one TSV per scripted game: every order applied, every bot unit's position and decision at the start and
 * end of each movement phase, and the hexes each unit moved through. The Python renderer
 * ({@code docs/bot-orders-tests/render_bot_orders.py}) turns it into a map of "what we ordered vs what it did".
 *
 * <p>The rule each unit followed is taken from the bot's {@code [BotOrders]} log line when the build writes one
 * (the #9038 fix does), captured by a temporary log appender. Builds without it still get the bot's cached
 * behaviour, from which the renderer derives the rule.</p>
 *
 * <p>Columns (hexes are 1-based board columns and rows, as MegaMek shows them):</p>
 * <pre>
 * game stage round unitId name owner col row facing crippled withdrawing behaviour rule detail headWaypoint
 * fleeEdge retreatEdge orderAction orderArgs note
 * </pre>
 * <p>{@code stage} is {@code order}, {@code start}, {@code end}, {@code path} or {@code gone}. A {@code path} row
 * lists the hexes of one move in {@code detail} as {@code col,row,facing;...}.</p>
 */
public class BotOrderRecorder implements AutoCloseable {
    private static final MMLogger logger = MMLogger.create(BotOrderRecorder.class);

    public static final String STAGE_ORDER = "order";
    public static final String STAGE_START = "start";
    public static final String STAGE_END = "end";
    public static final String STAGE_PATH = "path";
    public static final String STAGE_GONE = "gone";

    private static final String HEADER = String.join("\t", "game", "stage", "round", "unitId", "name", "owner",
          "col", "row", "facing", "crippled", "withdrawing", "behaviour", "rule", "detail", "headWaypoint",
          "fleeEdge", "retreatEdge", "orderAction", "orderArgs", "note");

    /** Matches the #9038 decision line: "[BotOrders] name (ID 12) round 3: RULE - detail". */
    private static final Pattern DECISION_PATTERN =
          Pattern.compile("^\\[BotOrders] .*\\(ID (\\d+)\\) round (\\d+): (\\S+) - (.*)$");

    private static final String CAPTURED_LOGGER = UnitBehavior.class.getName();

    private final PrintWriter writer;
    private final int gameNumber;
    private final OrderApplier orderApplier;
    private final Map<String, String[]> decisionsByUnitAndRound = new HashMap<>();
    private final TreeSet<Integer> goneUnitIds = new TreeSet<>();
    private final DecisionCapture decisionCapture;
    private boolean addedLoggerConfig;

    /**
     * Opens the TSV and starts capturing {@code [BotOrders]} log lines.
     *
     * @param traceFile    the TSV to write
     * @param gameNumber   the game's number in its batch
     * @param orderApplier where to read the units' waypoints from
     * @param headerLines  comment lines written at the top, each as {@code # key<TAB>value}
     *
     * @throws IOException when the file cannot be opened
     */
    public BotOrderRecorder(File traceFile, int gameNumber, OrderApplier orderApplier,
          Map<String, String> headerLines) throws IOException {
        this.writer = new PrintWriter(traceFile, StandardCharsets.UTF_8);
        this.gameNumber = gameNumber;
        this.orderApplier = orderApplier;
        for (Map.Entry<String, String> headerLine : headerLines.entrySet()) {
            writer.println("# " + headerLine.getKey() + "\t" + clean(headerLine.getValue()));
        }
        writer.println(HEADER);
        writer.flush();
        decisionCapture = new DecisionCapture("BotOrdersCapture-" + System.identityHashCode(this));
        startCapture();
        logger.info("[BotOrdersHarness] recording game {} to {}", gameNumber, traceFile.getAbsolutePath());
    }

    /**
     * Records one order applied to one unit (or to a bot, for flee; then {@code unit} is {@code null}).
     */
    public synchronized void recordOrder(int round, @Nullable Entity unit, String ownerName, ScriptedOrder order,
          String note) {
        StringJoiner arguments = new StringJoiner(" ");
        for (String argument : order.arguments()) {
            arguments.add(argument);
        }
        String unitId = (unit == null) ? "" : Integer.toString(unit.getId());
        String unitName = (unit == null) ? "" : unit.getDisplayName();
        String col = "";
        String row = "";
        String facing = "";
        if ((unit != null) && (unit.getPosition() != null)) {
            col = Integer.toString(unit.getPosition().getX() + 1);
            row = Integer.toString(unit.getPosition().getY() + 1);
            facing = Integer.toString(unit.getFacing());
        }
        writeRow(STAGE_ORDER, round, unitId, unitName, ownerName, col, row, facing, "", "", "", "", "", "", "", "",
              order.action().name(), arguments.toString(), note + " [line " + order.lineNumber() + "]");
    }

    /**
     * Records every unit of every bot: position from the server's game, decision and waypoint from the bot.
     *
     * @param stage        {@link #STAGE_START} or {@link #STAGE_END}
     * @param serverGame   the server's game, which holds the true positions
     * @param botsByPlayer each bot keyed by its player id
     */
    public synchronized void recordSnapshot(String stage, Game serverGame, Map<Integer, Princess> botsByPlayer) {
        int round = serverGame.getCurrentRound();
        for (Map.Entry<Integer, Princess> botEntry : botsByPlayer.entrySet()) {
            Princess bot = botEntry.getValue();
            for (Entity serverUnit : serverGame.getEntitiesVector()) {
                if (serverUnit.getOwnerId() != botEntry.getKey()) {
                    continue;
                }
                recordUnit(stage, round, serverUnit, bot);
            }
            for (Entity removedUnit : serverGame.getOutOfGameEntitiesVector()) {
                if ((removedUnit.getOwnerId() == botEntry.getKey()) && goneUnitIds.add(removedUnit.getId())) {
                    writeRow(STAGE_GONE, round, Integer.toString(removedUnit.getId()), removedUnit.getDisplayName(),
                          bot.getName(), "", "", "", "", "", "", "", "", "", "", "", "", "",
                          removalName(removedUnit.getRemovalCondition()));
                }
            }
        }
    }

    private void recordUnit(String stage, int round, Entity serverUnit, Princess bot) {
        Entity botUnit = bot.getGame().getEntity(serverUnit.getId());
        if (botUnit == null) {
            botUnit = serverUnit;
        }
        Coords position = serverUnit.getPosition();
        String col = (position == null) ? "" : Integer.toString(position.getX() + 1);
        String row = (position == null) ? "" : Integer.toString(position.getY() + 1);

        BehaviorType behaviour = bot.getUnitBehaviorTracker().getCachedBehaviorType(botUnit);
        boolean withdrawing = bot.getForcedWithdrawalTracker().isWithdrawing(botUnit);
        BehaviorSettings settings = bot.getBehaviorSettings();
        boolean fleeOrdered = settings.shouldAutoFlee() && (settings.getDestinationEdge() != CardinalEdge.NONE);
        String fleeEdge = fleeOrdered ? settings.getDestinationEdge().name() : "";

        String rule = "";
        String detail = "";
        if (STAGE_END.equals(stage)) {
            String[] decision = decisionsByUnitAndRound.get(serverUnit.getId() + ":" + round);
            if (decision != null) {
                rule = decision[0];
                detail = decision[1];
            }
        }
        writeRow(stage, round, Integer.toString(serverUnit.getId()), serverUnit.getDisplayName(), bot.getName(),
              col, row, Integer.toString(serverUnit.getFacing()), Boolean.toString(serverUnit.isCrippled(true)),
              Boolean.toString(withdrawing), (behaviour == null) ? "" : behaviour.name(), rule, detail,
              ScenarioOrderScript.toHexNumber(orderApplier.headWaypoint(bot, botUnit)), fleeEdge,
              settings.getRetreatEdge().name(), "", "", "");
    }

    /**
     * Records the hexes of one unit's move, as the watcher client saw them animated.
     */
    public synchronized void recordPath(int round, Entity unit, List<UnitLocation> movePath) {
        StringJoiner steps = new StringJoiner(";");
        for (UnitLocation step : movePath) {
            if (step.coords() != null) {
                steps.add((step.coords().getX() + 1) + "," + (step.coords().getY() + 1) + "," + step.facing());
            }
        }
        writeRow(STAGE_PATH, round, Integer.toString(unit.getId()), unit.getDisplayName(), "", "", "", "", "", "",
              "", "", steps.toString(), "", "", "", "", "", "");
    }

    private void writeRow(String stage, int round, String unitId, String name, String owner, String col, String row,
          String facing, String crippled, String withdrawing, String behaviour, String rule, String detail,
          String headWaypoint, String fleeEdge, String retreatEdge, String orderAction, String orderArgs,
          String note) {
        writer.println(String.join("\t", Integer.toString(gameNumber), stage, Integer.toString(round), unitId,
              clean(name), clean(owner), col, row, facing, crippled, withdrawing, behaviour, rule, clean(detail),
              headWaypoint, fleeEdge, retreatEdge, orderAction, clean(orderArgs), clean(note)));
        writer.flush();
    }

    private static String clean(String text) {
        return (text == null) ? "" : text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static String removalName(int removalCondition) {
        return switch (removalCondition) {
            case IEntityRemovalConditions.REMOVE_IN_RETREAT -> "RETREATED";
            case IEntityRemovalConditions.REMOVE_PUSHED -> "PUSHED";
            case IEntityRemovalConditions.REMOVE_SALVAGEABLE -> "DESTROYED_SALVAGEABLE";
            case IEntityRemovalConditions.REMOVE_DEVASTATED -> "DEVASTATED";
            case IEntityRemovalConditions.REMOVE_EJECTED -> "EJECTED";
            case IEntityRemovalConditions.REMOVE_CAPTURED -> "CAPTURED";
            case IEntityRemovalConditions.REMOVE_NEVER_JOINED -> "NEVER_JOINED";
            default -> "REMOVED_" + removalCondition;
        };
    }

    private void acceptLogMessage(String message) {
        if ((message == null) || !message.startsWith("[BotOrders]")) {
            return;
        }
        Matcher matcher = DECISION_PATTERN.matcher(message);
        if (matcher.matches()) {
            synchronized (this) {
                decisionsByUnitAndRound.put(matcher.group(1) + ":" + matcher.group(2),
                      new String[] { matcher.group(3), matcher.group(4) });
            }
        }
    }

    private void startCapture() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration configuration = context.getConfiguration();
            decisionCapture.start();
            configuration.addAppender(decisionCapture);
            LoggerConfig loggerConfig = configuration.getLoggerConfig(CAPTURED_LOGGER);
            if (!loggerConfig.getName().equals(CAPTURED_LOGGER)) {
                // give UnitBehavior its own config so INFO reaches the capture even under an ERROR-level parent,
                // without hiding DEBUG lines the parent would have written
                Level parentLevel = loggerConfig.getLevel();
                Level level = parentLevel.isLessSpecificThan(Level.INFO) ? parentLevel : Level.INFO;
                LoggerConfig ownConfig = new LoggerConfig(CAPTURED_LOGGER, level, true);
                configuration.addLogger(CAPTURED_LOGGER, ownConfig);
                loggerConfig = ownConfig;
                addedLoggerConfig = true;
            }
            loggerConfig.addAppender(decisionCapture, Level.INFO, null);
            context.updateLoggers();
        } catch (RuntimeException captureFailure) {
            // the TSV is still useful without the log lines; the renderer derives the rule from the behaviour
            logger.warn(captureFailure, "[BotOrdersHarness] could not capture [BotOrders] log lines");
        }
    }

    private void stopCapture() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration configuration = context.getConfiguration();
            if (addedLoggerConfig) {
                configuration.removeLogger(CAPTURED_LOGGER);
            } else {
                configuration.getLoggerConfig(CAPTURED_LOGGER).removeAppender(decisionCapture.getName());
            }
            context.updateLoggers();
            decisionCapture.stop();
        } catch (RuntimeException captureFailure) {
            logger.warn(captureFailure, "[BotOrdersHarness] could not remove the [BotOrders] log capture");
        }
    }

    @Override
    public synchronized void close() {
        stopCapture();
        writer.flush();
        writer.close();
    }

    /** Log appender that hands every message to {@link #acceptLogMessage}. */
    private final class DecisionCapture extends AbstractAppender {
        DecisionCapture(String name) {
            super(name, null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            acceptLogMessage(event.getMessage().getFormattedMessage());
        }
    }
}
