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

package megamek.client.ui.swing;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.HeadlessClient;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.BotCommandsPanel;
import megamek.client.ui.swing.audio.AudioService;
import megamek.client.ui.swing.audio.SoundManager;
import megamek.client.ui.swing.audio.SoundType;
import megamek.client.ui.swing.minimap.BoardviewlessMinimap;
import megamek.client.ui.swing.overlay.ChatOverlay;
import megamek.client.ui.swing.phaseDisplay.MiniReportDisplay;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.logging.MMLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Luana Coppio
 */
public class CommanderGUI extends Thread implements IClientGUI, ILocalBots {
    private static final MMLogger logger = MMLogger.create(CommanderGUI.class);
    private final Client client;
    private final MegaMekController controller;
    private final Map<String, AbstractClient> localBots;
    private final JFrame frame;
    private BoardviewlessMinimap minimap;
    private boolean isLoading;
    private JProgressBar progressBar;
    private boolean alive = true;
    private final AudioService audioService;
    private BotCommandsPanel buttonPanel;
    private JPanel centerPanel;

    private final TreeMap<Integer, String> splashImages = new TreeMap<>();
    {
        splashImages.put(0, Configuration.miscImagesDir() + "/acar_splash_hd.png");
    }

    public CommanderGUI(Client client, MegaMekController controller) {
        this.client = client;
        if (client instanceof HeadlessClient headlessClient) {
            headlessClient.setSendDoneOnVictoryAutomatically(false);
        }
        this.controller = controller;
        this.localBots = new HashMap<>();
        this.isLoading = true;
        this.audioService = new SoundManager();
        this.audioService.loadSoundFiles();
        frame = new JFrame(Messages.getString("ClientGUI.mini.title"));
        frame.setMinimumSize(new Dimension(800, 800));
    }

    @Override
    public void run() {
        initialize();
        loop();
    }

    private static final long targetFrameTimeNanos = 1_000_000_000 / 60;

    private void loop() {
        long previousNanos = System.nanoTime() - 16_000_000; // artificially say it has passed 1 FPS in the first loop
        long currentNanos;
        long awaitMillis;
        long elapsedNanos;
        while (alive) {
            currentNanos = System.nanoTime();
            elapsedNanos = currentNanos - previousNanos;
            // keeps around 60fps, not that it is important now, but anyway
            tick(elapsedNanos / 1_000_000);
            previousNanos = currentNanos;
            awaitMillis = (targetFrameTimeNanos - elapsedNanos) / 1_000_000;
            try {
                Thread.sleep(Math.max(1, awaitMillis));
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for next frame", e);
                alive = false;
            }
        }
    }

    @SuppressWarnings("unused")
    private void tick(long deltaTime) {
        // nothing to do here for now
    }

    @Override
    public void initialize() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Center: Splash image with progress bar
        centerPanel = new JPanel(new BorderLayout());
        JLabel splashImage = UIUtil.createSplashComponent(splashImages, getFrame());
        MiniReportDisplay miniReportDisplay = new MiniReportDisplay(this);
        miniReportDisplay.setMinimumSize(new Dimension(600, 600));
        miniReportDisplay.setPreferredSize(new Dimension(600, 600));

        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setVisible(true);
        minimap = new BoardviewlessMinimap(client);
        var chatOverlay = new ChatOverlay(8);
        minimap.addOverlay(chatOverlay);
        centerPanel.add(splashImage, BorderLayout.CENTER);
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        // Right: List of current entities with their status
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setMinimumSize(new Dimension(600, frame.getHeight()));

        JPanel entityListEntries = new JPanel();
        JLabel entitiesHeader = new JLabel("Entities in game");
        entityListEntries.add(entitiesHeader);
        entityListEntries.setLayout(new BoxLayout(entityListEntries, BoxLayout.Y_AXIS));
        buttonPanel = new BotCommandsPanel(this.client, audioService, controller);
        buttonPanel.useSpaceForPauseUnpause();

        var jScroll = new JScrollPane(entityListEntries);
        jScroll.setMinimumSize(new Dimension(-1, 20));
        jScroll.setPreferredSize(new Dimension(-1, 300));

        rightPanel.add(jScroll, BorderLayout.NORTH);
        rightPanel.add(miniReportDisplay, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        frame.getContentPane().add(mainPanel);
        frame.pack();

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (getClient().getGame().getPhase() == GamePhase.VICTORY) {
                    die();
                } else {
                    int closePrompt = JOptionPane.showConfirmDialog(null,
                        "Would you like to exit the game?",
                        Messages.getString("ClientGUI.gameSaveFirst"),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    if (closePrompt == JOptionPane.YES_OPTION) {
                        getClient().die();
                        die();
                    }
                }
            }
        });

        // Update entity list on phase change
        client.getGame().addGameListener(new GameListenerAdapter() {
            @Override
            public void gamePlayerChat(GamePlayerChatEvent e) {
                chatOverlay.addChatMessage(e.getPlayer(), e.getMessage());
            }

            @Override
            public void gamePhaseChange(GamePhaseChangeEvent e) {
                super.gamePhaseChange(e);
                var game = getClient().getGame();
                var round = game.getCurrentRound();
                if (e.getOldPhase() == GamePhase.LOUNGE) {
                    buttonPanel.setMiscButtonAsRequestVictory();
                    progressBar.setIndeterminate(true);
                }
                if (e.getNewPhase() == GamePhase.VICTORY) {
                    audioService.playSound(SoundType.BING_MY_TURN);
                    buttonPanel.setMiscButton("Scenario Completed", "Click here to finish it", evt -> {
                        client.sendDone(true);
                        die();
                    });
                    progressBar.setString("Game Over");
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                } else {
                    if (round < 1) {
                        progressBar.setString("Preparing...");
                    } else {
                        setupMinimap();
                        progressBar.setString("Round #" + game.getCurrentRound() + ": " + e.getNewPhase().localizedName());
                    }
                }
                entityListEntries.removeAll();
                entityListEntries.add(entitiesHeader);
                game.getInGameObjects().stream().filter(entity -> entity instanceof Entity).forEach(ent -> {
                    var entity = (Entity) ent;
                    var isCrippled = entity.isCrippled(true);
                    var entityLabelText = entity.getId() + " - " + entity.getDisplayName() + (isCrippled ? " (Crippled)" : "");
                    JLabel entityLabel = new JLabel(entityLabelText);
                    entityLabel.setForeground(entity.getOwner().getColour().getColour());
                    entityListEntries.add(entityLabel);
                });
                entityListEntries.revalidate();
                entityListEntries.repaint();
            }
        });
        frame.setVisible(true);
    }

    private void setupMinimap() {
        if (isLoading) {
            isLoading = false;
            centerPanel.remove(0);
            centerPanel.add(minimap, BorderLayout.CENTER, 0);
            audioService.playSound(SoundType.BING_MY_TURN);
            SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(false));
        }
    }

    @Override
    public JFrame getFrame() {
        return frame;
    }

    @Override
    public boolean shouldIgnoreHotKeys() {
        return false;
    }

    @Override
    public void die() {
        frame.dispose();
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public JComponent turnTimerComponent() {
        return null;
    }

    @Override
    public void setChatBoxActive(boolean active) {

    }

    @Override
    public void clearChatBox() {

    }

    @Override
    public Map<String, AbstractClient> getLocalBots() {
        return localBots;
    }

    public void enableReady() {
        if (buttonPanel != null) {
            buttonPanel.setMiscButton(
                Messages.getString("BotCommandPanel.Ready.title"),
                Messages.getString("BotCommandPanel.Ready.tooltip"),
                e -> {
                    getLocalBots().values().forEach(bot -> bot.sendDone(true));
                    client.sendDone(true);
                });
            setupMinimap();
        }
    }
}
