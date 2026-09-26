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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.Princess;
import megamek.common.units.UnitLocation;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utilities.botorders.ScriptedOrder.OrderAction;

/**
 * Plays a {@link ScenarioOrderScript} into a headless game and drives the {@link BotOrderRecorder}.
 *
 * <p>Listens to the server's game. Each round's orders are applied once, at the Initiative Report phase (or, if
 * that phase is skipped, at the start of the Movement phase), so the bots hold them before they plan their moves.
 * Test-only damage ({@code cripple}, {@code damage internal N%}) is applied to the server's copy of the unit and sent
 * to every client, as real damage would be.</p>
 *
 * <p>Every order and its result is logged at INFO with the prefix {@code [BotOrdersHarness]}.</p>
 */
public class ScriptedOrderDirector {
    private static final MMLogger logger = MMLogger.create(ScriptedOrderDirector.class);

    private final ScenarioOrderScript script;
    private final OrderApplier orderApplier;
    private final Game serverGame;
    private final TWGameManager gameManager;
    private final Map<Integer, Princess> botsByPlayer;
    private final BotOrderRecorder recorder;
    private int lastAppliedRound = -1;

    private final GameListenerAdapter serverListener = new GameListenerAdapter() {
        @Override
        public void gamePhaseChange(GamePhaseChangeEvent event) {
            onServerPhaseChange(event.getOldPhase(), event.getNewPhase());
        }
    };

    private final GameListenerAdapter watcherListener = new GameListenerAdapter() {
        @Override
        public void gameEntityChange(GameEntityChangeEvent event) {
            onWatcherEntityChange(event);
        }
    };

    private Game watcherGame;

    /**
     * @param script       the orders to apply
     * @param orderApplier how to give the bot its orders
     * @param serverGame   the server's game
     * @param gameManager  the server's game manager, used to send damaged units to the clients
     * @param botsByPlayer every bot, keyed by its player id
     * @param recorder     the TSV recorder, or {@code null} to apply orders without recording
     */
    public ScriptedOrderDirector(ScenarioOrderScript script, OrderApplier orderApplier, Game serverGame,
          TWGameManager gameManager, Map<Integer, Princess> botsByPlayer, @Nullable BotOrderRecorder recorder) {
        this.script = script;
        this.orderApplier = orderApplier;
        this.serverGame = serverGame;
        this.gameManager = gameManager;
        this.botsByPlayer = botsByPlayer;
        this.recorder = recorder;
    }

    /**
     * Starts listening to the server's game, and to the watcher's game for the hexes each move passes through.
     *
     * @param watcherClientGame the watcher client's game, or {@code null} to skip move paths
     */
    public void attach(@Nullable Game watcherClientGame) {
        serverGame.addGameListener(serverListener);
        watcherGame = watcherClientGame;
        if (watcherGame != null) {
            watcherGame.addGameListener(watcherListener);
        }
        logger.info("[BotOrdersHarness] {} scripted orders from {}", script.getOrders().size(),
              script.getSourceFile().getName());
    }

    /**
     * Stops listening. Call when the game ends.
     */
    public void detach() {
        serverGame.removeGameListener(serverListener);
        if (watcherGame != null) {
            watcherGame.removeGameListener(watcherListener);
        }
    }

    private void onServerPhaseChange(GamePhase oldPhase, GamePhase newPhase) {
        try {
            int round = serverGame.getCurrentRound();
            if ((newPhase == GamePhase.INITIATIVE_REPORT)
                  || ((newPhase == GamePhase.MOVEMENT) && (lastAppliedRound != round))) {
                if (lastAppliedRound != round) {
                    lastAppliedRound = round;
                    applyOrdersForRound(round);
                }
            }
            if ((recorder != null) && (newPhase == GamePhase.MOVEMENT)) {
                recorder.recordSnapshot(BotOrderRecorder.STAGE_START, serverGame, botsByPlayer);
            }
            if ((recorder != null) && (oldPhase == GamePhase.MOVEMENT) && (newPhase != GamePhase.MOVEMENT)) {
                recorder.recordSnapshot(BotOrderRecorder.STAGE_END, serverGame, botsByPlayer);
            }
        } catch (RuntimeException failure) {
            // a harness fault must never stall the game it is watching
            logger.error(failure, "[BotOrdersHarness] failed handling phase change to " + newPhase);
        }
    }

    private void onWatcherEntityChange(GameEntityChangeEvent event) {
        try {
            Vector<UnitLocation> movePath = event.getMovePath();
            Entity unit = event.getEntity();
            if ((recorder == null) || (unit == null) || (movePath == null) || movePath.isEmpty()
                  || !botsByPlayer.containsKey(unit.getOwnerId())) {
                return;
            }
            recorder.recordPath(serverGame.getCurrentRound(), unit, movePath);
        } catch (RuntimeException failure) {
            logger.error(failure, "[BotOrdersHarness] failed recording a move path");
        }
    }

    private void applyOrdersForRound(int round) {
        for (ScriptedOrder order : script.getOrdersForRound(round)) {
            List<Entity> targets = selectTargets(order);
            if (targets.isEmpty()) {
                logger.warn("[BotOrdersHarness] round {}: no unit matches line {}: {}", round, order.lineNumber(),
                      order.sourceText());
                continue;
            }
            if (order.action() == OrderAction.FLEE) {
                applyFlee(round, order, targets);
                continue;
            }
            for (Entity serverUnit : targets) {
                applyToUnit(round, order, serverUnit);
            }
        }
    }

    private void applyFlee(int round, ScriptedOrder order, List<Entity> targets) {
        CardinalEdge edge = CardinalEdge.valueOf(order.arguments().getFirst().toUpperCase(Locale.ROOT));
        List<Princess> ordered = new ArrayList<>();
        for (Entity serverUnit : targets) {
            Princess bot = botsByPlayer.get(serverUnit.getOwnerId());
            if ((bot != null) && !ordered.contains(bot)) {
                ordered.add(bot);
                orderApplier.orderFlee(bot, edge);
                logger.info("[BotOrdersHarness] round {}: bot {} ordered to flee {}", round, bot.getName(), edge);
                if (recorder != null) {
                    recorder.recordOrder(round, null, bot.getName(), order, "flee " + edge);
                }
            }
        }
    }

    private void applyToUnit(int round, ScriptedOrder order, Entity serverUnit) {
        Princess bot = botsByPlayer.get(serverUnit.getOwnerId());
        if (bot == null) {
            logger.warn("[BotOrdersHarness] round {}: {} is not a bot unit; line {} skipped", round,
                  serverUnit.getDisplayName(), order.lineNumber());
            return;
        }
        Entity botUnit = bot.getGame().getEntity(serverUnit.getId());
        if (botUnit == null) {
            botUnit = serverUnit;
        }
        String result = switch (order.action()) {
            case WAYPOINTS -> {
                List<Coords> waypoints = hexes(order);
                int accepted = orderApplier.setWaypoints(bot, botUnit, waypoints);
                yield "waypoints set, " + accepted + " of " + waypoints.size() + " reachable";
            }
            case ADD_WAYPOINTS -> {
                List<Coords> waypoints = hexes(order);
                int accepted = orderApplier.addWaypoints(bot, botUnit, waypoints);
                yield "waypoints added, " + accepted + " of " + waypoints.size() + " reachable";
            }
            case CLEAR -> {
                orderApplier.clearOrders(bot, botUnit);
                yield "orders cleared";
            }
            case CRIPPLE -> damageInternal(serverUnit, 50, true);
            case DAMAGE_INTERNAL -> damageInternal(serverUnit, ScenarioOrderScript.damagePercent(order), false);
            case FLEE -> "flee handled per bot";
        };
        logger.info("[BotOrdersHarness] round {}: {} (ID {}) {}: {}", round, serverUnit.getDisplayName(),
              serverUnit.getId(), order.action(), result);
        if (recorder != null) {
            recorder.recordOrder(round, serverUnit, bot.getName(), order, result);
        }
    }

    /**
     * Removes internal structure from the server's copy of the unit and sends it to every client. {@code sideTorsos}
     * limits the damage to a Mek's side torsos, which is the smallest damage that counts as crippled (two torsos
     * with internal damage).
     */
    private String damageInternal(Entity serverUnit, int percent, boolean sideTorsosOnly) {
        for (int location = 0; location < serverUnit.locations(); location++) {
            if (sideTorsosOnly && serverUnit.isMek()
                  && (location != Mek.LOC_LEFT_TORSO) && (location != Mek.LOC_RIGHT_TORSO)) {
                continue;
            }
            if (serverUnit.isMek() && (location == Mek.LOC_HEAD)) {
                continue;
            }
            int originalInternal = serverUnit.getOInternal(location);
            int currentInternal = serverUnit.getInternal(location);
            if ((originalInternal <= 0) || (currentInternal <= 1)) {
                continue;
            }
            int loss = Math.max(1, (int) Math.ceil(originalInternal * percent / 100.0));
            serverUnit.setArmor(0, location);
            serverUnit.setInternal(Math.max(1, currentInternal - loss), location);
        }
        gameManager.entityUpdate(serverUnit.getId());
        return "internal damaged " + percent + "%, now crippled=" + serverUnit.isCrippled(true);
    }

    private static List<Coords> hexes(ScriptedOrder order) {
        List<Coords> waypoints = new ArrayList<>();
        for (String hexNumber : order.arguments()) {
            waypoints.add(ScenarioOrderScript.parseHexNumber(hexNumber));
        }
        return waypoints;
    }

    private List<Entity> selectTargets(ScriptedOrder order) {
        List<Entity> targets = new ArrayList<>();
        for (Entity serverUnit : serverGame.getEntitiesVector()) {
            Princess bot = botsByPlayer.get(serverUnit.getOwnerId());
            if (bot == null) {
                continue;
            }
            boolean selected = switch (order.targetKind()) {
                case UNIT_ID -> Integer.toString(serverUnit.getId()).equals(order.targetValue());
                case UNIT_NAME -> matchesName(serverUnit, order.targetValue());
                case BOT -> bot.getName().equalsIgnoreCase(order.targetValue());
                case ALL -> true;
            };
            if (selected) {
                targets.add(serverUnit);
            }
        }
        return targets;
    }

    private static boolean matchesName(Entity unit, String name) {
        String chassisAndModel = (unit.getChassis() + " " + unit.getModel()).trim();
        return unit.getDisplayName().equalsIgnoreCase(name) || unit.getShortName().equalsIgnoreCase(name)
              || chassisAndModel.equalsIgnoreCase(name);
    }
}
