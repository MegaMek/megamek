/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import static megamek.common.Terrains.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.util.UIUtil.TipList;
import megamek.client.ui.swing.util.UIUtil.TipTextField;
import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;

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
            if (clientGui.getClient().getGame().getPhase() == GamePhase.LOUNGE) {
                board = clientGui.chatlounge.getPossibleGameBoard(true);
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
        adaptToGUIScale();
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
        coordsList.setFont(UIUtil.getScaledFont());
        coordsList.setVisibleRowCount(6);
        coordsList.setPrototypeCellValue(new Coords(-21, 22));
        
        if (clientGui == null) {
            listLabel.setEnabled(false);
            coordsList.setEnabled(false);   
        } else {
            board.getBuildingsVector().stream().map(Building::getCoordsList).forEach(coordsListModel::addAll);
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
                final Hex hex = board.getHex(coords); 
                final Building bldg = board.getBuildingAt(coords);
                if (hex.containsTerrain(BUILDING)) {
                    content += Messages.getString("BotConfigDialog.hexListBldg", Building.typeName(bldg.getType()),
                            Building.className(bldg.getBldgClass()), hex.terrainLevel(BLDG_ELEV), hex.terrainLevel(BLDG_CF));
                } else if (hex.containsTerrain(FUEL_TANK)) {
                    content += Messages.getString("BotConfigDialog.hexListFuel", 
                            hex.terrainLevel(FUEL_TANK_CF), hex.terrainLevel(FUEL_TANK_MAGN));
                } else {
                    content += Messages.getString("BotConfigDialog.hexListBrdg", Building.typeName(bldg.getType()),
                            hex.terrainLevel(BRIDGE_ELEV), hex.terrainLevel(BRIDGE_CF));
                }
            }
            return super.getListCellRendererComponent(list, content, index, isSelected, cellHasFocus);
        }
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}
