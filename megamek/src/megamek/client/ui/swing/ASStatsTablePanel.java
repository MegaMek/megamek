/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.dialogs.ASConversionInfoDialog;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.util.SpringUtilities;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ASStatsTablePanel implements ActionListener {

    private final int COLUMNS = 15;
    private final static Color GROUP_NAME_COLOR = UIUtil.uiLightGreen();
    private final static Color HEADER_COLOR = UIUtil.uiLightBlue();

    private final JPanel panel = new JPanel(new SpringLayout());
    private int rows;
    private final List<EntityGroup> groups = new ArrayList<>();
    private final JFrame frame;
    private JButton pvButton;
    private final AlphaStrikeElementComparator aseComparator = new AlphaStrikeElementComparator();
    /**
     * Constructs a panel with a table of AlphaStrike stats for any units that are added to it.
     * To add units to it, call {@link #add(Collection)} or {@link #add(Collection, String)}. These
     * calls can be stringed. The panel can be obtained with {@link #getPanel()}.
     *
     * The table will show buttons for calling up conversion reports. The given frame is needed as a
     * parent to the conversion report windows. Pilot skills will be included when converting TW units.
     *
     * @param frame The parent frame (important for giving a parent to conversion report dialogs)
     */
    public ASStatsTablePanel(JFrame frame) {
        this.frame = frame;
    }

    /**
     * Adds a block of units to the panel. AlphaStrikeElements are shown as they are, Entities are
     * converted first. All other types of unit are ignored. This block of units is shown with the
     * given name.
     *
     * @param name The name of this block of units
     * @param units a collection of units to (convert and) show
     * @return This panel to allow stringing.
     */
    public ASStatsTablePanel add(Collection<? extends ForceAssignable> units, @Nullable String name) {
        groups.add(new EntityGroup(units, name));
        rebuildPanel();
        return this;
    }

    /**
     * Adds a block of units to the panel. AlphaStrikeElements are shown as they are, Entities are
     * converted first. All other types of unit are ignored. This block of units is shown without a name.
     *
     * @param units a collection of units to (convert and) show
     * @return This panel to allow stringing.
     */
    public ASStatsTablePanel add(Collection<? extends ForceAssignable> units) {
        groups.add(new EntityGroup(units, ""));
        rebuildPanel();
        return this;
    }

    /**
     * Returns the fully constructed JPanel with all units added through the add() methods to add to
     * any other Container.
     *
     * @return The swing JPanel
     */
    public JPanel getPanel() {
        constructPanel();
        return panel;
    }

    /** returns a new sorted list of elements */
    public List<AlphaStrikeElement> getElements() {
        // TODO sort groups?
        return groups.stream()
                .flatMap(group -> group.elements.stream()
                .sorted(aseComparator)).collect(Collectors.toList());
    }

    /** Assembles the JPanel. It is empty before calling this method. */
    private void constructPanel() {
        addVerticalSpace();
        for (EntityGroup group : groups) {
            addGroupToPanel(group);
        }
        finalizePanel();
    }

    private void rebuildPanel() {
        rows = 0;
        panel.removeAll();
        constructPanel();
    }


    /** Adds one group of units to the JPanel. */
    private void addGroupToPanel(EntityGroup group) {
        // Print the elements
        rows++;
        addGroupName(group.name);
//        addHeader("Name");
        addHeader("Type");
        addHeader("SZ");
        addHeader("TMM");
        addHeader("MV (THR)");
        addHeader("Role");
        addHeader("Dmg S/M/L");
        addHeader("OV");
        addHeader("Arm");
        addHeader("Str");
        addHeader("Th");
        addHeader("Skill");
        pvButton = addSortableHeader("PV");
        addHeader("Specials");
        addHeader("Conversion");
        addLine();

        for (AlphaStrikeElement element : group.elements.stream().sorted(aseComparator).collect(Collectors.toList())) {
            boolean oddRow = (rows++ % 2) == 1;
            addGridElementLeftAlign(element.getName(), oddRow);
            addGridElement(element.getASUnitType() + "", oddRow);
            addGridElement(element.getSize() + "", oddRow);
            addGridElement(element.isAerospace() ? "" : element.getTMM() + "", oddRow);
            addGridElement(element.getMovementAsString(), oddRow);
            addGridElement(element.getRole() + "", oddRow);
            addGridElement(element.usesArcs() ? "" : element.getStandardDamage() + "", oddRow);
            addGridElement(element.usesOV() ? element.getOV() + "" : "", oddRow);
            addGridElement(element.getFullArmor() + "", oddRow);
            addGridElement(element.getFullStructure() + "", oddRow);
            addGridElement(element.usesThreshold() ? element.getThreshold() + "" : " ", oddRow);
            addGridElement(element.getSkill() + "", oddRow);
            addGridElement(element.getPointValue() + "", oddRow);
            if (element.usesArcs()) {
                addGridElementLeftAlign(getArcedSpecials(element), oddRow);
            } else {
                addGridElementLeftAlign(element.getSpecialsDisplayString(element), oddRow);
            }
            addConversionInfo(oddRow, group.reports.get(element), element, frame);
        }

        addVerticalSpace();
    }

    /** Finalizes the JPanel by constructing the Swing layout table. */
    private void finalizePanel() {
        SpringUtilities.makeCompactGrid(panel, rows, COLUMNS, 5, 5, 1, 5);
        panel.revalidate();
    }

    private void addConversionInfo(boolean coloredBG, FlexibleCalculationReport conversionReport,
                                   AlphaStrikeElement element, JFrame frame) {
        var conversionPanel = new UIUtil.FixedYPanel();
        if (coloredBG) {
            conversionPanel.setBackground(UIUtil.alternateTableBGColor());
        }
        JButton button = new JButton("?");
        button.setEnabled(conversionReport != null);
        button.setFont(UIUtil.getScaledFont());
        button.addActionListener(e -> new ASConversionInfoDialog(frame, conversionReport, element, true).setVisible(true));
        conversionPanel.add(button);
        panel.add(conversionPanel);
    }

    private void addGridElement(String text, boolean coloredBG) {
        writeGridElement(text, coloredBG, null, FlowLayout.CENTER);
    }

    private void addGridElementLeftAlign(String text, boolean coloredBG) {
        writeGridElement(text, coloredBG, null, FlowLayout.LEFT);
    }

    private void writeGridElement(String text, boolean coloredBG, Color color, int alignment) {
        var elementPanel = new UIUtil.FixedYPanel(new FlowLayout(alignment));
        if (coloredBG) {
            elementPanel.setBackground(UIUtil.alternateTableBGColor());
        }
        var textLabel = new JLabel(text);
        textLabel.setForeground(color);
        textLabel.setFont(UIUtil.getScaledFont());
        elementPanel.add(textLabel);
        panel.add(elementPanel);
    }

    private void addGroupName(String text) {
        writeGridElement(text, false, GROUP_NAME_COLOR, FlowLayout.LEFT);
    }

    private void addHeader(String text) {
        var textLabel = new JLabel(text);
        textLabel.setForeground(HEADER_COLOR);
        textLabel.setFont(UIUtil.getScaledFont());
        panel.add(textLabel);
    }

    private JButton addSortableHeader(String text) {
        var textLabel = new JButton(text);
        textLabel.addActionListener(this);
        textLabel.setForeground(HEADER_COLOR);
        textLabel.setFont(UIUtil.getScaledFont());
        panel.add(textLabel);
        return textLabel;
    }

    private String getArcedSpecials(AlphaStrikeElement element) {
        return "<HTML>" + element.getSpecialsDisplayString(element) +
                "<BR>FRONT(" + element.getFrontArc().getSpecialsExportString(", ", element) + ")" +
                "<BR>LEFT(" + element.getLeftArc().getSpecialsExportString(", ", element) + ")" +
                "<BR>RIGHT(" + element.getRightArc().getSpecialsExportString(", ", element) + ")" +
                "<BR>REAR(" + element.getRearArc().getSpecialsExportString(", ", element) + ")";
    }

    private void addVerticalSpace() {
        rows++;
        for (int col = 0; col < COLUMNS; col++) {
            panel.add(Box.createVerticalStrut(20));
        }
    }

    /** Adds a line of JSeperators to the panel. The additional strut is required for the line to show. */
    private void addLine() {
        rows++;
        for (int col = 0; col < COLUMNS; col++) {
            var spacerPanel = new UIUtil.FixedYPanel();
            spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.PAGE_AXIS));
            spacerPanel.add(new JSeparator());
            spacerPanel.add(Box.createVerticalStrut(2));
            panel.add(spacerPanel);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == pvButton) {
            aseComparator.togglePV();
        }
        rebuildPanel();
    }

    /** A record to store added groups of units before constructing the panel. */
    private static class EntityGroup {
        private final List<AlphaStrikeElement> elements = new ArrayList<>();
        private final Map<AlphaStrikeElement, FlexibleCalculationReport> reports = new HashMap<>();
        private final String name;

        private EntityGroup(Collection<? extends ForceAssignable> entities, String name) {
            this.name = Objects.requireNonNullElse(name, "");

            for (ForceAssignable unit : entities) {
                AlphaStrikeElement element = null;
                if (unit instanceof AlphaStrikeElement) {
                    element = (AlphaStrikeElement) unit;
                } else if ((unit instanceof Entity) && (ASConverter.canConvert((Entity) unit))) {
                    element = ASConverter.convert((Entity) unit, new FlexibleCalculationReport());
                }
                if (element != null) {
                    elements.add(element);
                    if (element.hasConversionReport()) {
                        reports.put(element, (FlexibleCalculationReport) element.getConversionReport());
                    }
                }
            }
        }
    }

//    private class ModifiableComparator implements Comparator<AlphaStrikeElement> {
//
//    }

    /** Orderable, optional sorting criteria for AlphaStrikeElements*/
    private class AlphaStrikeElementComparator implements Comparator<AlphaStrikeElement> {
        private static final int REVERSE_SORT = -1;
        private static final int NO_SORT = 0;
        private static final int SORT = 1;
        //sort criteria: 1 is sort, 0 is do not sort, -1 is reverse sort
        private int sortByPV = 0,  sortByName = 0;
        @Override
        public int compare(AlphaStrikeElement o1,AlphaStrikeElement o2) {
            if (sortByPV != 0) {
                int result = Integer.compare(o1.getPointValue(), o2.getPointValue());
                if (result != 0) {
                    return result * sortByPV;
                }
            }

            if (sortByName != 0) {
                int result = o1.getName().compareTo(o2.getName());
                if (result != 0) {
                    return result * sortByName;
                }
            }
            return 0;
        }

        public void togglePV() {
            sortByPV = toggle(sortByPV);
        }

        private int toggle(int current) {
            return current < 0 ? 0 : current > 0 ? -1 : 1;
        }
    }
}