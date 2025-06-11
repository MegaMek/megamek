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
 * MechWarrior Copyright Microsoft Corporation. <Package Name> was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import megamek.client.ratgenerator.AvailabilityRating;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ui.util.SpringUtilities;
import megamek.client.ui.util.UIUtil;
import megamek.common.eras.Era;
import megamek.common.eras.Eras;
import org.apache.logging.log4j.LogManager;

public class AvailabilityPanel {

    private final static RATGenerator RAT_GENERATOR = RATGenerator.getInstance();
    private static Integer[] RG_ERAS;

    private final JFrame parent;
    private final JPanel panel = new UIUtil.FixedXPanel(new SpringLayout());
    private final Box mainPanel = Box.createVerticalBox();
    private final JScrollPane scrollPane = new JScrollPane(mainPanel);
    private int columns;
    private ModelRecord record;

    public AvailabilityPanel(JFrame parent) {
        this.parent = parent;
        panel.setAlignmentX(0);
        mainPanel.setBorder(new EmptyBorder(20, 25, 0, 0));
        mainPanel.add(new JLabel(
              "Clicking on the factions or eras opens a link to the MUL showing the respective entry."));
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        initializePanel();
    }

    public JComponent getPanel() {
        return scrollPane;
    }

    public void setUnit(String model, String chassis) {
        record = RAT_GENERATOR.getModelRecord(chassis + " " + model);
        initializePanel();
    }

    public void reset() {
        record = null;
    }

    private void initializePanel() {
        if (!RAT_GENERATOR.isInitialized()) {
            return;
        } else if (RG_ERAS == null) {
            RAT_GENERATOR.getEraSet().forEach(RAT_GENERATOR::loadYear);
            // RAT_GENERATOR.initRemainingUnits();
            RG_ERAS = RAT_GENERATOR.getEraSet().toArray(new Integer[0]);
        }
        panel.removeAll();
        columns = 0;
        addHeader("Faction");
        for (Era era : Eras.getEras()) {
            String link = "<HTML><BODY><DIV ALIGN=CENTER><A HREF = http://www.masterunitlist.info/Era/Details/"
                                + era.mulId() + ">" + era + "</A></BODY></HTML>";
            addHeader(link);
        }

        if (record != null) {
            int row = 1;
            List<AvailabilityRating> ratings = new ArrayList<>();
            for (String factionName : record.getIncludedFactions()) {
                addGridElementLeftAlign(factionName, row % 2 == 1);
                for (Era era : Eras.getEras()) {
                    String text = "--";
                    ratings.clear();

                    // Cycle the years and check if the year is in the current ERA and the faction is active
                    for (Integer year : RAT_GENERATOR.getEraSet()) {
                        FactionRecord faction = RAT_GENERATOR.getFaction(factionName);
                        if ((Eras.getEra(year) != era)
                                  || ((faction != null) && !faction.isActiveInYear(year))) {
                            continue;
                        }
                        ratings.add(RAT_GENERATOR.findModelAvailabilityRecord(year, record.getKey(), factionName));
                    }

                    ratings.removeIf(Objects::isNull);
                    // Merge all ratings from years that fell into the current era
                    AvailabilityRating eraAvailability = RAT_GENERATOR.mergeFactionAvailability(factionName, ratings);
                    if (eraAvailability != null) {
                        text = eraAvailability.getAvailabilityCode();
                    }

                    addGridElement(text, row % 2 == 1);
                }
                row++;
            }

            SpringUtilities.makeCompactGrid(panel, row, columns, 5, 5, 1, 1);
            panel.revalidate();
        }
    }

    private void addHeader(String text, float alignment) {
        columns++;
        var headerPanel = new UIUtil.FixedYPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.PAGE_AXIS));
        var textLabel = new JTextPane();
        textLabel.setContentType("text/html");
        textLabel.setEditable(false);
        textLabel.setText(text);
        textLabel.setAlignmentX(alignment);
        textLabel.setFont(panel.getFont().deriveFont(Font.BOLD));
        textLabel.setForeground(UIUtil.uiLightBlue());
        textLabel.setFont(UIUtil.getDefaultFont());
        textLabel.addHyperlinkListener(e -> {
            try {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });
        headerPanel.add(textLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(new JSeparator());
        panel.add(headerPanel);
    }

    private void addHeader(String text) {
        addHeader(text, JComponent.CENTER_ALIGNMENT);
    }

    private void addGridElement(String text, boolean coloredBG) {
        var elementPanel = new UIUtil.FixedYPanel();
        if (coloredBG) {
            elementPanel.setBackground(UIUtil.alternateTableBGColor());
        }
        var textLabel = new JLabel(text);
        textLabel.setFont(UIUtil.getDefaultFont());
        elementPanel.add(textLabel);
        panel.add(elementPanel);
    }

    private void addGridElementLeftAlign(String text, boolean coloredBG) {
        var elementPanel = new UIUtil.FixedYPanel(new FlowLayout(FlowLayout.LEFT));
        if (coloredBG) {
            elementPanel.setBackground(UIUtil.alternateTableBGColor());
        }
        var textLabel = new JLabel(text);
        textLabel.setFont(UIUtil.getDefaultFont());
        elementPanel.add(textLabel);
        panel.add(elementPanel);
    }
}
