/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.IClient;
import megamek.client.commands.ClientCommand;
import megamek.client.ui.IClientCommandHandler;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.boardview.*;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Configuration;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.*;
import java.util.List;

public abstract class AbstractClientGUI implements IClientGUI, IClientCommandHandler {

    /** The smallest GUI scaling value; smaller will make text unreadable */
    public static final float MIN_GUISCALE = 0.7f;

    /** The highest GUI scaling value; increase this for 16K monitors */
    public static final float MAX_GUISCALE = 2.4f;

    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();
    protected static final ClientPreferences CP = PreferenceManager.getClientPreferences();

    protected static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    protected static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    protected static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    protected static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    protected final JFrame frame = new JFrame(Messages.getString("ClientGUI.title"));

    protected Map<String, ClientCommand> clientCommands = new HashMap<>();

    // BoardViews
    protected final Map<Integer, IBoardView> boardViews = new HashMap<>();
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
                }
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
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()));
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()));
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()));
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()));
        frame.setIconImages(iconList);
    }

    /**
     * Saves window and other settings to the GUIPreferences. Typically called when this ClientGUI shuts down.
     * By default, the size and position of the window are saved. When overriding this to save additional
     * settings, super should be called.
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
     * @param cmd
     *            a client command with CLIENT_COMMAND prepended.
     */
    public String runCommand(String cmd) {
        cmd = cmd.substring(ClientCommand.CLIENT_COMMAND.length());
        return runCommand(cmd.split("\\s+"));
    }

    /**
     * Runs the command
     *
     * @param args
     *            the command and it's arguments with the CLIENT_COMMAND already
     *            removed, and the string tokenized.
     */
    public String runCommand(String[] args) {
        if ((args != null) && (args.length > 0) && clientCommands.containsKey(args[0])) {
            return clientCommands.get(args[0]).run(args);
        }
        return "Unknown Client Command.";
    }

}
