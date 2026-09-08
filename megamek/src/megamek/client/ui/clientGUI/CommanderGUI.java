/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.clientGUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;

import megamek.client.AbstractClient;
import megamek.client.Client;
import megamek.client.HeadlessClient;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.audio.AudioService;
import megamek.client.ui.clientGUI.audio.SoundManager;
import megamek.client.ui.clientGUI.audio.SoundType;
import megamek.client.ui.clientGUI.overlay.ChatOverlay;
import megamek.client.ui.dialogs.BotCommands.BotCommandsPanel;
import megamek.client.ui.dialogs.miniReport.MiniReportDisplayPanel;
import megamek.client.ui.dialogs.minimap.BoardViewLessMinimapPanel;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.RawImagePanel;
import megamek.common.Configuration;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameVictoryEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * @author Luana Coppio
 */
public class CommanderGUI implements IClientGUI, ILocalBots {
    private static final MMLogger logger = MMLogger.create(CommanderGUI.class);
    private final Client client;
    private final MegaMekController controller;
    private final Map<String, AbstractClient> localBots;
    private final JFrame frame;
    private BoardViewLessMinimapPanel minimap;
    private boolean isLoading;
    private JProgressBar progressBar;
    private final AudioService audioService;
    private BotCommandsPanel buttonPanel;
    private JPanel centerPanel;
    private String serverPassword = "";
    private volatile boolean readyRequested = false;

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
    }

    /**
     * Sets the server password so the "Request Victory" button can authenticate its {@code /victory} command. MekHQ's
     * Host dialog pre-fills the last password, and {@code VictoryCommand} rejects a passwordless request on a
     * passworded server. See issue #8891.
     *
     * @param serverPassword the server password, or empty/{@code null} when the server has none
     */
    public void setServerPassword(String serverPassword) {
        this.serverPassword = (serverPassword == null) ? "" : serverPassword;
        if (buttonPanel != null) {
            buttonPanel.setServerPassword(this.serverPassword);
        }
    }

    @Override
    public void initialize() {
        // The whole window is built and shown on the EDT. This is invoked from the MekHQ game thread, so hop over with
        // invokeAndWait; building Swing off the EDT is what made the window unreliable. See issue #8891.
        if (SwingUtilities.isEventDispatchThread()) {
            buildAndShow();
        } else {
            try {
                SwingUtilities.invokeAndWait(this::buildAndShow);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while building the Commander window", e);
            } catch (InvocationTargetException e) {
                logger.error("Failed to build the Commander window", e);
            }
        }
    }

    private void buildAndShow() {
        frame.setMinimumSize(UIUtil.scaleForGUI(800, 800));
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Center: Splash image with progress bar
        centerPanel = new JPanel(new BorderLayout());
        RawImagePanel splashImage = UIUtil.createSplashComponent(splashImages, getFrame());
        MiniReportDisplayPanel miniReportDisplayPanel = new MiniReportDisplayPanel(this);
        miniReportDisplayPanel.setMinimumSize(UIUtil.scaleForGUI(600, 600));
        miniReportDisplayPanel.setPreferredSize(UIUtil.scaleForGUI(600, 600));

        progressBar = new JProgressBar(0, 100);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setVisible(true);
        minimap = new BoardViewLessMinimapPanel(client);
        var chatOverlay = new ChatOverlay(8);
        minimap.addOverlay(chatOverlay);
        centerPanel.add(splashImage, BorderLayout.CENTER);
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        // Right: List of current entities with their status
        JPanel rightPanel = new JPanel(new BorderLayout());
        // Height is left to the layout: frame.getHeight() is still 0 before pack(), so a fixed value here was junk.
        rightPanel.setMinimumSize(new Dimension(UIUtil.scaleForGUI(600), 0));

        JPanel entityListEntries = new JPanel();
        JLabel entitiesHeader = new JLabel("Entities in game");
        entityListEntries.add(entitiesHeader);
        entityListEntries.setLayout(new BoxLayout(entityListEntries, BoxLayout.Y_AXIS));
        buttonPanel = new BotCommandsPanel(this.client, audioService, controller);
        buttonPanel.useSpaceForPauseUnpause();
        buttonPanel.setServerPassword(serverPassword);

        var jScroll = new JScrollPane(entityListEntries);
        jScroll.setMinimumSize(new Dimension(-1, UIUtil.scaleForGUI(20)));
        jScroll.setPreferredSize(new Dimension(-1, UIUtil.scaleForGUI(300)));

        rightPanel.add(jScroll, BorderLayout.NORTH);
        rightPanel.add(miniReportDisplayPanel, BorderLayout.CENTER);
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
                    // The game is already over, so no confirmation is needed, but we must still close the client
                    // connection.
                    getClient().die();
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
                        deliverVictoryToListeners();
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
                        progressBar.setString("Round #" + game.getCurrentRound() + ": " + e.getNewPhase()
                              .localizedName());
                    }
                }
                entityListEntries.removeAll();
                entityListEntries.add(entitiesHeader);
                game.getInGameObjects().stream().filter(entity -> entity instanceof Entity).forEach(ent -> {
                    var entity = (Entity) ent;
                    var isCrippled = entity.isCrippled(true);
                    var entityLabelText = entity.getId() + " - " + entity.getDisplayName() + (isCrippled ?
                          " (Crippled)" :
                          "");
                    JLabel entityLabel = new JLabel(entityLabelText);
                    // Owners are removed at resetGame(), so a late repaint can see a null owner. See issue #8891.
                    var owner = entity.getOwner();
                    if (owner != null) {
                        entityLabel.setForeground(owner.getColour().getColour());
                    }
                    entityListEntries.add(entityLabel);
                });
                entityListEntries.revalidate();
                entityListEntries.repaint();
            }
        });
        frame.setVisible(true);

        // The launcher may have called enableReady() before the panel existed; apply it now that it does.
        applyReadyIfRequested();
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
        // The key dispatcher is registered application-wide, but this window is embedded in MekHQ alongside other
        // windows. Only honor hotkeys (notably the space-bar pause) while this Commander window is the active window
        // and no modal dialog is showing, so pressing space in a MekHQ window does not pause the running scenario.
        // See issue #8888.
        return !frame.isActive() || UIUtil.isModalDialogDisplayed();
    }

    /**
     * Delivers the game result to the game's listeners (notably MekHQ) directly from the snapshot captured when the
     * VICTORY phase began, rather than relying on the {@code GAME_VICTORY_EVENT} packet. That packet is only sent once
     * the VICTORY phase ends, which can stall indefinitely on bot disconnects, leaving MekHQ uninformed. MekHQ's
     * {@code gameVictory} guards against double-processing, so the packet is harmlessly ignored if it later arrives.
     * See issue #8889.
     */
    private void deliverVictoryToListeners() {
        if (client instanceof HeadlessClient headlessClient) {
            GameVictoryEvent snapshot = headlessClient.getVictorySnapshot();
            if (snapshot != null) {
                client.getGame().processGameEvent(snapshot);
            }
        }
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

    /**
     * Requests that the Ready button be wired up. This may be called by the MekHQ launcher before {@link #initialize()}
     * has finished building the panel; if so the request is remembered and applied at the end of {@code initialize()}
     * rather than silently dropped (which left the Ready button dead and preparation stalled). See issue #8891.
     */
    public void enableReady() {
        readyRequested = true;
        applyReadyIfRequested();
    }

    private void applyReadyIfRequested() {
        if (!readyRequested || (buttonPanel == null)) {
            // Not yet buildable; buildAndShow() calls this again once the panel exists.
            return;
        }
        Runnable apply = () -> {
            logger.info("Commander GUI: wiring the Ready button");
            buttonPanel.setMiscButton(
                  Messages.getString("BotCommandPanel.Ready.title"),
                  Messages.getString("BotCommandPanel.Ready.tooltip"),
                  e -> {
                      getLocalBots().values().forEach(bot -> bot.sendDone(true));
                      client.sendDone(true);
                  });
            setupMinimap();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            apply.run();
        } else {
            SwingUtilities.invokeLater(apply);
        }
    }
}
