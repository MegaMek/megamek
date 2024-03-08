/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.jacksonadapters.MMUWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ASStatsTablePanel implements ActionListener {

    private final int COLUMNS = 15;
    private final static Color GROUP_NAME_COLOR = UIUtil.uiLightGreen();
    private final static Color HEADER_COLOR = UIUtil.uiLightBlue();
    private final static Color SORTED_HEADER_COLOR = UIUtil.uiYellow();

    private final JPanel panel = new JPanel(new SpringLayout());
    private int rows;
    private final List<EntityGroup> groups = new ArrayList<>();
    private final JFrame frame;
    private final Map<JButton, AlphaStrikeElementComparator> buttonMap = new HashMap<>();
    private final ASETableComparator aseTableComparator = new ASETableComparator();
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

        // the table sorting rules
        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Name") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Type") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return o1.getASUnitType().toString().compareTo(o2.getASUnitType().toString());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("SZ") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getSize(), o2.getSize());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("TMM") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getTMM(), o2.getTMM());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("MV (THR)") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return o1.getMovementAsString().compareTo(o2.getMovementAsString());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Role") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return o1.getRole().compareTo(o2.getRole());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Dmg S/M/L") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return o1.getStandardDamage().toString().compareTo(o2.getStandardDamage().toString());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("OV") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getOV(), o2.getOV());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Arm") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getFullArmor(), o2.getFullArmor());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Str") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getFullStructure(), o2.getFullStructure());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Th") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getThreshold(), o2.getThreshold());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("Skill") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getSkill(), o2.getSkill());
            }
        });

        aseTableComparator.comparatorList.add(new AlphaStrikeElementComparator("PV") {
            @Override
            public int compare(AlphaStrikeElement o1, AlphaStrikeElement o2) {
                return Integer.compare(o1.getPointValue(), o2.getPointValue());
            }
        });
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
        rebuildPanel();
        return panel;
    }

    /** returns the list of elements in the panel, sorted in the same way they are displayed */
    public List<AlphaStrikeElement> getSortedElements() {
        return groups.stream()
                .flatMap(group -> group.elements.stream()
                .sorted(aseTableComparator)).collect(Collectors.toList());
    }

    /** remove and rebuild the table grid */
    private void rebuildPanel() {
        panel.removeAll();
        addVerticalSpace();
        buttonMap.clear();
        rows = 0;
        addElementHeaders();
        for (EntityGroup group : groups) {
            addGroupToPanel(group);
        }
        finalizePanel();
    }

    /** Adds one group of units to the JPanel. */
    private void addGroupToPanel(EntityGroup group) {
        addGroupHeaders(group);

        for (AlphaStrikeElement element : group.elements.stream().sorted(aseTableComparator).collect(Collectors.toList())) {
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
        var textLabel = new JLabel(text, SwingConstants.CENTER);
        textLabel.setForeground(HEADER_COLOR);
        textLabel.setFont(UIUtil.getScaledFont());
        panel.add(textLabel);
    }

    /** add a header button that connects to the <code>sort</code> of an AlphaStrikeElementComparator */
    private JButton addSortableHeader( AlphaStrikeElementComparator comparator ) {
        var button = new JButton(comparator.getLabel());
        button.addActionListener(this);
        button.setForeground( comparator.sort == 0 ? HEADER_COLOR : SORTED_HEADER_COLOR);
        button.setFont(UIUtil.getScaledFont());
        button.setToolTipText("Click to sort by "+comparator.name);
        panel.add(button);
        buttonMap.put(button, comparator);
        return button;
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

    private void addElementHeaders() {
        rows++;
        aseTableComparator.comparatorList.stream().forEach(mc -> addSortableHeader(mc));
        addHeader("Specials");
        addHeader("Conversion");
        addLine();
    }

    /** Adds an entry for the Group name, followed by separators. */
    private void addGroupHeaders(EntityGroup group) {
        rows++;
        addGroupName(group.name);
        for (int col = 1; col < COLUMNS; col++) {
            var spacerPanel = new UIUtil.FixedYPanel();
            spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.PAGE_AXIS));
            spacerPanel.add(new JSeparator());
            spacerPanel.add(Box.createVerticalStrut(2));
            panel.add(spacerPanel);
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
        if (buttonMap.containsKey(e.getSource())) {
            JButton button = (JButton) e.getSource();
            AlphaStrikeElementComparator comparator = buttonMap.get(button);
            comparator.toggleSort();
            button.setText(comparator.getLabel());
            rebuildPanel();
        }
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

            try {
                MMUWriter.writeMMUFile(new File("aselements.mmu"), elements);
            } catch (IOException ignored) {
                // ignored, this is just for testing
            }
        }
    }

    /** Extend this class to create a comparator triggered by a button*/
    private abstract class AlphaStrikeElementComparator implements Comparator<AlphaStrikeElement> {
        // 0 is do not sort, 1 is sort, -q is reverse sort
        private int sort = 0;
        private String name;

        AlphaStrikeElementComparator(String name) {
            this.name = name;
        }

        private String getLabel() {
            return name + (sort < 0 ? '-' : sort > 0 ? '+' : "");
        }
        private void toggleSort() {
            sort = sort < 0 ? 0 : sort > 0 ? -1 : 1;
        }
    }

    /** Orderable, optional sorting criteria for AlphaStrikeElements. Execute sorts in order of
     * <code>comparatorList</code>*/
    private class ASETableComparator implements Comparator<AlphaStrikeElement> {
        //sort criteria: 1 is sort, 0 is do not sort, -1 is reverse sort
        private final List<AlphaStrikeElementComparator> comparatorList = new ArrayList<>();

        @Override
        public int compare(AlphaStrikeElement o1,AlphaStrikeElement o2) {
            for (var comparator : comparatorList) {
                if (comparator.sort != 0) {
                    int result = comparator.compare(o1, o2);
                    if (result != 0) {
                        // reverse sort if sort is -1
                        return result * comparator.sort;
                    }
                }
            }
            return 0;
        }
    }
}