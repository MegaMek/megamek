/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import megamek.client.IClient;
import megamek.client.commands.ClientCommand;
import megamek.client.ui.IClientCommandHandler;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.spriteHandler.BoardViewSpriteHandler;
import megamek.client.ui.dialogs.MMDialogs.MMNarrativeStoryDialog;
import megamek.client.ui.dialogs.minimap.MinimapDialog;
import megamek.client.ui.util.UIUtil;
import megamek.common.board.BoardLocation;
import megamek.common.Configuration;
import megamek.common.units.Targetable;
import megamek.common.event.GameScriptedMessageEvent;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

public abstract class AbstractClientGUI implements IClientGUI, IClientCommandHandler {

    /** The smallest GUI scaling value; smaller will make text unreadable */
    public static final float MIN_GUI_SCALE = 0.7f;

    /** The highest GUI scaling value; increase this for 16K monitors */
    public static final float MAX_GUI_SCALE = 2.4f;

    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    protected static final ClientPreferences CP = PreferenceManager.getClientPreferences();

    protected static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    protected static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    protected static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    protected static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    protected final JFrame frame = new JFrame(Messages.getString("ClientGUI.title"));

    /** Temporarily stores story dialogs so they can be shown one after the other */
    private final List<JDialog> queuedStoryDialogs = new ArrayList<>();

    protected Map<String, ClientCommand> clientCommands = new HashMap<>();

    /**
     * The {@link megamek.client.ui.clientGUI.boardview.BoardView}'s of the game with the board ID as the map key
     */
    public final Map<Integer, IBoardView> boardViews = new HashMap<>();

    /**
     * The minimaps of the game with the board ID as the map key
     */
    protected final Map<Integer, MinimapDialog> miniMaps = new HashMap<>();
    protected final BoardViewsContainer boardViewsContainer = new BoardViewsContainer(this);
    protected final List<BoardViewSpriteHandler> spriteHandlers = new ArrayList<>();

    public AbstractClientGUI(IClient iClient) {
        initializeFrame();
    }

    @Override
    public JFrame getFrame() {
        return frame;
    }

    @Override
    public boolean shouldIgnoreHotKeys() {
        return UIUtil.isModalDialogDisplayed();
    }

    @Override
    public void initialize() {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!GUIP.getNoSaveNag()) {
                    int savePrompt = JOptionPane.showConfirmDialog(null,
                          Messages.getString("ClientGUI.gameSaveDialogMessage"),
                          Messages.getString("ClientGUI.gameSaveFirst"),
                          JOptionPane.YES_NO_CANCEL_OPTION,
                          JOptionPane.WARNING_MESSAGE);
                    if ((savePrompt == JOptionPane.CANCEL_OPTION)
                          || ((savePrompt == JOptionPane.YES_OPTION) && !saveGame())) {
                        // When the user clicked YES but did not actually save the game, don't close the game
                        return;
                    }
                } // We should wait here until the save game packet arrives.
                frame.setVisible(false);
                saveSettings();
                die();
            }
        });
        initializeFrame();
    }

    @Override
    public void die() {
        spriteHandlers.forEach(BoardViewSpriteHandler::dispose);
        frame.removeAll();
        frame.setVisible(false);
        frame.dispose();
    }

    protected void initializeFrame() {
        if (GUIP.getWindowSizeHeight() != 0) {
            frame.setLocation(GUIP.getWindowPosX(), GUIP.getWindowPosY());
            frame.setSize(GUIP.getWindowSizeWidth(), GUIP.getWindowSizeHeight());
        } else {
            frame.setSize(800, 600);
        }
        frame.setMinimumSize(new Dimension(640, 480));
        UIUtil.updateWindowBounds(frame);

        List<Image> iconList = new ArrayList<>();
        iconList.add(frame.getToolkit()
              .getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()));
        iconList.add(frame.getToolkit()
              .getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()));
        iconList.add(frame.getToolkit()
              .getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()));
        iconList.add(frame.getToolkit()
              .getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()));
        frame.setIconImages(iconList);
    }

    /**
     * Saves window and other settings to the GUIPreferences. Typically called when this ClientGUI shuts down. By
     * default, the size and position of the window are saved. When overriding this to save additional settings, super
     * should be called.
     */
    void saveSettings() {
        GUIP.setWindowPosX(frame.getLocation().x);
        GUIP.setWindowPosY(frame.getLocation().y);
        GUIP.setWindowSizeWidth(frame.getSize().width);
        GUIP.setWindowSizeHeight(frame.getSize().height);
    }

    protected abstract boolean saveGame();

    public List<IBoardView> boardViews() {
        return new ArrayList<>(boardViews.values());
    }

    /** Registers a new command in the client command table. */
    @Override
    public void registerCommand(ClientCommand command) {
        // Warning, the special direction commands are registered separately
        clientCommands.put(command.getName(), command);
    }

    /** Returns the command associated with the specified name. */
    @Override
    public ClientCommand getCommand(String commandName) {
        return clientCommands.get(commandName);
    }

    @Override
    public Set<String> getAllCommandNames() {
        return clientCommands.keySet();
    }


    /**
     * @param cmd a client command with CLIENT_COMMAND prepended.
     */
    public String runCommand(String cmd) {
        cmd = cmd.substring(ClientCommand.CLIENT_COMMAND.length());
        return runCommand(cmd.split("\\s+"));
    }

    /**
     * Runs the command
     *
     * @param args the command and it's arguments with the CLIENT_COMMAND already removed, and the string tokenized.
     */
    public String runCommand(String[] args) {
        if ((args != null) && (args.length > 0) && clientCommands.containsKey(args[0])) {
            return clientCommands.get(args[0]).run(args);
        }
        return "Unknown Client Command.";
    }

    protected void showScriptedMessage(GameScriptedMessageEvent event) {
        queuedStoryDialogs.add(new MMNarrativeStoryDialog(frame, event));
        showDialogs();
    }

    /**
     * Shows a queued story dialog if no other is shown at this time. Normally, not more than one modal dialog (and its
     * child dialogs) can be shown at any one time as Swing blocks access to buttons outside a first modal dialog. In
     * MM, this is different as the server may send multiple story dialog packets which will all be processed and shown
     * without user input. This method prevents that behavior so that only one story dialog is shown at one time. Note
     * that when story dialogs trigger at the same time, their order is likely to be the order they were added to the
     * game but packet transport can make them arrive at the client in a different order.
     */
    private void showDialogs() {
        if (!UIUtil.isModalDialogDisplayed()) {
            while (!queuedStoryDialogs.isEmpty()) {
                JDialog dialog = queuedStoryDialogs.remove(0);
                dialog.setVisible(true);
            }
        }
    }

    public void showBoardView(int boardId) {
        boardViewsContainer.showBoardView(boardId);
    }

    public IBoardView getBoardView(Targetable entity) {
        return getBoardView(entity.getBoardId());
    }

    public IBoardView getBoardView(BoardLocation boardLocation) {
        return getBoardView(boardLocation.boardId());
    }

    public IBoardView getBoardView(int boardId) {
        return boardViews.get(boardId);
    }

    /**
     * @return The currently shown {@link megamek.client.ui.clientGUI.boardview.BoardView}. If there is only a single
     *       {@link megamek.client.ui.clientGUI.boardview.BoardView} (no tabbed pane), this will be returned. With
     *       multiple {@link megamek.client.ui.clientGUI.boardview.BoardView}'s, the one in the currently selected tab
     *       is returned.
     *       <p>
     *       Unfortunately it is possible to have no selected tab in a JTabbedPane; also, theoretically, there could be
     *       no {@link megamek.client.ui.clientGUI.boardview.BoardView}. Therefore, the result is returned as an
     *       Optional.
     */
    public Optional<IBoardView> getCurrentBoardView() {
        return boardViewsContainer.getCurrentBoardView();
    }
}
