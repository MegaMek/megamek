/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.TipList;
import megamek.client.ui.util.UIUtil.TipTextField;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.IBuilding;
import megamek.server.ServerBoardHelper;

public class BotConfigTargetHexDialog extends AbstractButtonDialog {

    private static final String OK_ACTION = "Ok_Action";

    private final TipTextField coordsField = new TipTextField("", 5, "x, y");
    private final JLabel coordsLabel = new JLabel(Messages.getString("BotConfigDialog.hexCoordsLabel"));
    private final JLabel listLabel = new JLabel(Messages.getString("BotConfigDialog.hexListLabel"));
    private final DefaultListModel<Coords> coordsListModel = new DefaultListModel<>();
    private final TipList<Coords> coordsList = new TipList<>(coordsListModel);
    private final ClientGUI clientGui;
    private final Board board;

    protected BotConfigTargetHexDialog(JFrame frame, @Nullable ClientGUI cg) {
        super(frame, "BotConfigTargetUnitDialog", "BotConfigDialog.bcthdTitle");
        clientGui = cg;
        if (clientGui != null) {
            if (clientGui.getClient().getGame().getPhase().isLounge()) {
                board = ServerBoardHelper.getPossibleGameBoard(clientGui.getClient().getMapSettings(), true);
            } else {
                board = clientGui.getClient().getBoard();
            }
        } else {
            board = null;
        }
        initialize();
        // Enter keypress
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, OK_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(enter, OK_ACTION);
        getRootPane().getActionMap().put(OK_ACTION, new AbstractAction() {
            @Serial
            private static final long serialVersionUID = -1060468627937876090L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                coordsField.requestFocus();
            }
        });
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        var coordsFieldPanel = new UIUtil.FixedYPanel();
        coordsFieldPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        coordsField.setToolTipText(Messages.getString("BotConfigDialog.hexCoordsTip"));
        coordsLabel.setLabelFor(coordsField);
        coordsLabel.setDisplayedMnemonic(KeyEvent.VK_X);
        coordsFieldPanel.add(coordsLabel);
        coordsFieldPanel.add(coordsField);

        var listLabelPanel = new UIUtil.FixedYPanel();
        listLabel.setLabelFor(coordsList);
        listLabel.setDisplayedMnemonic(KeyEvent.VK_S);
        listLabelPanel.add(listLabel);

        coordsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        coordsList.setLayoutOrientation(JList.VERTICAL);
        coordsList.setCellRenderer(new BuildingHexRenderer());
        coordsList.setFont(UIUtil.getDefaultFont());
        coordsList.setVisibleRowCount(6);
        coordsList.setPrototypeCellValue(new Coords(-21, 22));

        if (clientGui == null) {
            listLabel.setEnabled(false);
            coordsList.setEnabled(false);
        } else {
            board.getBuildingsVector().stream().map(IBuilding::getCoordsList).forEach(coordsListModel::addAll);
        }

        result.add(Box.createVerticalStrut(15));
        result.add(coordsFieldPanel);
        result.add(Box.createVerticalStrut(15));
        result.add(listLabelPanel);
        result.add(Box.createVerticalStrut(25));
        result.add(new JScrollPane(coordsList));

        return result;
    }

    public List<Coords> getSelectedCoords() {
        List<Coords> result = new ArrayList<>();
        // Parse the Coords text field
        try {
            String[] tokens = coordsField.getText().split(",");
            if (tokens.length == 2) {
                int x = Integer.parseInt(tokens[0]) - 1;
                int y = Integer.parseInt(tokens[1]) - 1;
                result.add(new Coords(x, y));
            }
        } catch (Exception ignored) {
            // No coords if it cannot be parsed
        }
        // Add the marked list entries
        result.addAll(coordsList.getSelectedValuesList());
        return result;
    }

    /** Shows building info for the hexes in the list model. */
    private class BuildingHexRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            Coords coords = (Coords) value;
            String content = Messages.getString("BotConfigDialog.hexListIntro", coords.getX() + 1, coords.getY() + 1);
            if (board != null && board.getHex(coords) != null) {
                content += BotConfigDialog.buildingInfoIfPresent(coords, board);
            }
            return super.getListCellRendererComponent(list, content, index, isSelected, cellHasFocus);
        }
    }
}
