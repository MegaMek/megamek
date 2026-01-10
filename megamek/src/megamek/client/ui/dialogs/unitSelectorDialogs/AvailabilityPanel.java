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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.VolatileImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.View;

import megamek.client.ratgenerator.AvailabilityRating;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ui.util.UIUtil;
import megamek.common.eras.Era;
import megamek.common.eras.Eras;
import megamek.common.util.ManagedVolatileImage;
import megamek.logging.MMLogger;

public class AvailabilityPanel {
    private static final MMLogger logger = MMLogger.create(AvailabilityPanel.class);


    private static class FixedColumnGrid extends JPanel {
        private final JTable fixedTable;
        private final JTable scrollableTable;
        private final JScrollPane fixedScrollPane;
        private final JScrollPane scrollableScrollPane;
        private final DefaultTableModel model;
        private static final int FIXED_COLUMN_WIDTH = 200;

        public static class FactionCellData {
            ImageIcon icon;
            String factionCode;
            String factionName;

            public FactionCellData(ImageIcon icon, String factionCode) {
                this.icon = icon;
                this.factionCode = factionCode;
                FactionRecord faction = RAT_GENERATOR.getFaction(factionCode);
                if (faction != null && faction.getName() != null) {
                    this.factionName = faction.getName();
                } else {
                    this.factionName = factionCode;
                }
            }
        }

        private static class FactionCellRenderer extends JPanel implements TableCellRenderer {
            private ManagedVolatileImage factionImage;
            private final JLabel textLabel = new JLabel();
            private boolean showIcon = false;
            private int textContentWidth = 0;
            private static final int ICON_SIZE = 32; // Fixed height for icon
            private static final int ICON_MARGIN = 5;

            public FactionCellRenderer() {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                setOpaque(true);
                setBorder(new EmptyBorder(2, 5, 2, 5)); // Padding for the whole cell
                textLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
                textLabel.setVerticalAlignment(SwingConstants.CENTER);
                add(textLabel, BorderLayout.CENTER);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (showIcon && factionImage != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    UIUtil.setHighQualityRendering(g2d);

                    // Draw the icon on the left side, vertically centered
                    VolatileImage img = factionImage.getImage();
                    int iconY = (getHeight() - ICON_SIZE) / 2;
                    g2d.drawImage(img, ICON_MARGIN, iconY, null);

                    g2d.dispose();
                }
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
                if (value instanceof FactionCellData data) {
                    this.textContentWidth = FIXED_COLUMN_WIDTH -
                          getInsets().left - getInsets().right - // Panel border
                          ICON_SIZE -
                          ICON_MARGIN -
                          30;
                    // Create ManagedVolatileImage for the icon
                    if (data.icon != null) {
                        factionImage = new ManagedVolatileImage(data.icon.getImage(),
                              Transparency.TRANSLUCENT, ICON_SIZE, ICON_SIZE);
                        showIcon = true;
                    } else {
                        factionImage = null;
                        showIcon = false;
                    }
                    textLabel.setBorder(new EmptyBorder(0, ICON_SIZE + ICON_MARGIN, 0, 0));
                    textLabel.setText("<html><body style='width: " + textContentWidth + "px'>" +
                          (data.factionName != null ? data.factionName : "Unknown") +
                          "</body></html>");
                }

                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                    textLabel.setForeground(table.getSelectionForeground()); // Ensure text color changes
                } else {
                    setBackground(row % 2 == 0 ? table.getBackground() : slightlyDarker(table.getBackground()));
                    setForeground(table.getForeground());
                    textLabel.setForeground(table.getForeground());
                }
                return this;
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                int minHeight = showIcon ? ICON_SIZE : 0;
                String text = textLabel.getText();
                int textHeight = 0;
                if (text != null && text.contains("<html>")) {
                    if (this.textContentWidth > 0) {
                        try {
                            JLabel tempLabel = new JLabel(text);
                            tempLabel.setFont(textLabel.getFont());
                            tempLabel.setSize(this.textContentWidth, Integer.MAX_VALUE);
                            View view = (View) tempLabel.getClientProperty("html");
                            if (view == null) {
                                tempLabel.getPreferredSize();
                                view = (View) tempLabel.getClientProperty("html");
                            }
                            if (view != null) {
                                view.setSize(this.textContentWidth, 0);
                                textHeight = (int) view.getPreferredSpan(View.Y_AXIS);
                            }
                        } catch (NumberFormatException ignored) {
                            // Fallback
                            textHeight = textLabel.getPreferredSize().height;
                        }
                    }
                } else {
                    textHeight = textLabel.getPreferredSize().height;
                }
                int contentHeight = Math.max(minHeight, textHeight);
                Insets insets = getInsets();
                int totalHeight = contentHeight + insets.top + insets.bottom;
                size.height = Math.max(size.height, totalHeight);
                return size;
            }
        }

        private static class HyperlinkHeaderRenderer extends JLabel implements TableCellRenderer {
            public HyperlinkHeaderRenderer() {
                setOpaque(true);
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                setHorizontalAlignment(CENTER);
                setFont(UIManager.getFont("TableHeader.font").deriveFont(Font.BOLD));
                setForeground(UIUtil.uiLightBlue());
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
                setText(value == null ? "" : value.toString());
                return this;
            }
        }

        public FixedColumnGrid() {
            setLayout(new BorderLayout());

            // Create a shared model for both tables
            model = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // Fixed column table (left side)
            fixedTable = new JTable(model);
            fixedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            fixedTable.getTableHeader().setReorderingAllowed(false);
            fixedTable.setIntercellSpacing(new Dimension(0, 1));

            // Create the scrollable columns table (right side)
            scrollableTable = new JTable(model);
            scrollableTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            scrollableTable.getTableHeader().setReorderingAllowed(false);
            scrollableTable.setIntercellSpacing(new Dimension(1, 1));

            // Set up scroll panes
            fixedScrollPane = new JScrollPane(fixedTable,
                  JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            fixedScrollPane.setWheelScrollingEnabled(true);

            scrollableScrollPane = new JScrollPane(scrollableTable,
                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                  JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollableScrollPane.setWheelScrollingEnabled(true);

            scrollableScrollPane.getVerticalScrollBar().setModel(fixedScrollPane.getVerticalScrollBar().getModel());
            scrollableTable.setSelectionModel(fixedTable.getSelectionModel());

            // Set up the main layout with both tables
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                  fixedScrollPane, scrollableScrollPane);
            splitPane.setDividerSize(0);
            splitPane.setContinuousLayout(true);
            splitPane.setDividerLocation(FIXED_COLUMN_WIDTH);
            splitPane.setResizeWeight(0.0);

            add(splitPane, BorderLayout.CENTER);

            model.addTableModelListener(new RowHeightSynchronizer());

            // Setup header renderers and mouse listener for hyperlinks
            HyperlinkHeaderRenderer headerRenderer = new HyperlinkHeaderRenderer();
            MouseAdapter headerMouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    JTableHeader header = (JTableHeader) e.getSource();
                    JTable table = header.getTable();
                    int columnIndexInView = table.columnAtPoint(e.getPoint());
                    if (columnIndexInView != -1) {
                        int modelColumnIndex = table.convertColumnIndexToModel(columnIndexInView);
                        String headerValue = table.getModel()
                              .getColumnName(modelColumnIndex); // Get from model for original HTML
                        if (headerValue != null) {
                            Pattern pattern = Pattern.compile("href\\s*=\\s*['\"]([^'\"]*)['\"]",
                                  Pattern.CASE_INSENSITIVE);
                            Matcher matcher = pattern.matcher(headerValue);
                            if (matcher.find()) {
                                String url = matcher.group(1);
                                try {
                                    if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                          .isSupported(Desktop.Action.BROWSE)) {
                                        Desktop.getDesktop().browse(new URI(url));
                                    }
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(table,
                                          "Could not open link: " + ex.getMessage(),
                                          "Link Error",
                                          JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                }
            };

            fixedTable.getTableHeader().setDefaultRenderer(headerRenderer);
            fixedTable.getTableHeader().addMouseListener(headerMouseAdapter);
            scrollableTable.getTableHeader().setDefaultRenderer(headerRenderer);
            scrollableTable.getTableHeader().addMouseListener(headerMouseAdapter);


            // Listen for column model changes to re-apply setup
            fixedTable.addPropertyChangeListener("model", evt -> {
                setupFixedColumns();
                synchronizeRowHeights();
            });
            scrollableTable.addPropertyChangeListener("model", evt -> {
                setupScrollableColumns();
                synchronizeRowHeights();
            });


            ComponentAdapter componentAdapter = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    synchronizeRowHeights(); // This will also call adjustColumnWidths
                }
            };
            fixedTable.addComponentListener(componentAdapter);
            scrollableTable.addComponentListener(componentAdapter);
            addComponentListener(componentAdapter);
        }

        /**
         * Configure the fixed column table to show only the first column
         */
        private void setupFixedColumns() {
            TableColumnModel fcm = fixedTable.getColumnModel();
            while (fcm.getColumnCount() > 1) {
                fcm.removeColumn(fcm.getColumn(1));
            }

            if (model.getColumnCount() > 0) {
                TableColumn fixedColCandidate = null;
                if (fcm.getColumnCount() == 1) {
                    fixedColCandidate = fcm.getColumn(0);
                }
                if (fixedColCandidate != null && fcm.getColumnCount() == 1 && fixedColCandidate.getModelIndex() == 0) {
                    fixedColCandidate.setCellRenderer(new FactionCellRenderer());
                    fixedColCandidate.setPreferredWidth(FIXED_COLUMN_WIDTH);
                    fixedColCandidate.setHeaderValue(model.getColumnName(0));
                }
            } else {
                while (fcm.getColumnCount() > 0) {
                    fcm.removeColumn(fcm.getColumn(0));
                }
            }

            fixedTable.setPreferredScrollableViewportSize(new Dimension(FIXED_COLUMN_WIDTH,
                  fixedTable.getPreferredScrollableViewportSize().height));
            fixedTable.getTableHeader().revalidate();
            fixedTable.getTableHeader().repaint();
        }

        /**
         * Configure the scrollable table to hide the first column
         */
        private void setupScrollableColumns() {
            TableColumnModel scm = scrollableTable.getColumnModel();
            if (scm.getColumnCount() != 1) {
                for (int i = scm.getColumnCount() - 1; i >= 0; i--) {
                    if (scm.getColumn(i).getModelIndex() == 0) {
                        scm.removeColumn(scm.getColumn(i));
                    }
                }
            }
            for (int i = 0; i < scm.getColumnCount(); i++) {
                TableColumn col = scm.getColumn(i);
                col.setHeaderValue(model.getColumnName(col.getModelIndex()));
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(SwingConstants.CENTER);
                col.setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus,
                          int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value,
                              isSelected, hasFocus, row, column);
                        if (!isSelected) {
                            c.setBackground(row % 2 == 0 ?
                                  table.getBackground() :
                                  slightlyDarker(table.getBackground()));
                        }
                        setHorizontalAlignment(SwingConstants.CENTER);
                        return c;
                    }
                });
            }
            scrollableTable.getTableHeader().revalidate();
            scrollableTable.getTableHeader().repaint();
        }

        private static Color slightlyDarker(Color color) {
            if (color == null) {return Color.LIGHT_GRAY;}
            return color.darker();
        }

        /**
         * Synchronize row heights between the fixed and scrollable tables
         */
        public void synchronizeRowHeights() {
            for (int row = 0; row < model.getRowCount(); row++) {
                int fixedHeight = 0;
                if (fixedTable.getColumnCount() > 0 && row < fixedTable.getRowCount()) {
                    fixedHeight = getPreferredRowHeight(fixedTable, row);
                }

                int scrollHeight = 0;
                if (scrollableTable.getColumnCount() > 0 && row < scrollableTable.getRowCount()) {
                    scrollHeight = getPreferredRowHeight(scrollableTable, row);
                }

                int maxHeight = Math.max(fixedHeight, scrollHeight);
                if (maxHeight <= 0) {
                    maxHeight = fixedTable.getRowHeight();
                }
                if (maxHeight <= 0) {
                    maxHeight = UIManager.getFont("Table.font").getSize() + 4; // Default based on font
                }
                if (fixedTable.getRowCount() > row) {fixedTable.setRowHeight(row, maxHeight);}
                if (scrollableTable.getRowCount() > row) {scrollableTable.setRowHeight(row, maxHeight);}
            }
            adjustColumnWidths();
            fixedScrollPane.revalidate();
            fixedScrollPane.repaint();
            scrollableScrollPane.revalidate();
            scrollableScrollPane.repaint();
        }

        /**
         * Calculate the preferred height for a row based on its content
         */
        private int getPreferredRowHeight(JTable table, int row) {
            int height = table.getRowHeight(); // Start with current or default
            if (row < 0 || row >= table.getRowCount()) {return height;}

            for (int column = 0; column < table.getColumnCount(); column++) {
                if (column >= table.getColumnCount()) {continue;}
                Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
                try {
                    int compHeight = comp.getPreferredSize().height + table.getRowMargin();
                    height = Math.max(height, compHeight);
                } catch (Exception e) {
                    // Could happen if renderer is misbehaving or data is unusual
                    logger.warn("Could not get preferred height for cell ({}, {}): {}", row, column, e.getMessage());
                }
            }
            return height;
        }

        public void adjustColumnWidths() {
            // Fixed table column width is managed by FIXED_COLUMN_WIDTH and dividerLocation
            if (fixedTable.getColumnModel().getColumnCount() > 0) {
                fixedTable.getColumnModel().getColumn(0).setPreferredWidth(FIXED_COLUMN_WIDTH);
            }

            // Scrollable table columns
            TableColumnModel scm = scrollableTable.getColumnModel();
            for (int i = 0; i < scm.getColumnCount(); i++) {
                TableColumn column = scm.getColumn(i);
                int modelColIdx = column.getModelIndex();
                int maxWidth = 0;

                // Header width
                TableCellRenderer headerRenderer = column.getHeaderRenderer();
                if (headerRenderer == null) {
                    headerRenderer = scrollableTable.getTableHeader().getDefaultRenderer();
                }
                Object headerValue = model.getColumnName(modelColIdx); // Use model name (HTML)
                Component headerComp = headerRenderer.getTableCellRendererComponent(
                      scrollableTable, headerValue, false, false, -1, i); // view column index i
                maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width);

                // Cell content width
                for (int row = 0; row < scrollableTable.getRowCount(); row++) {
                    TableCellRenderer cellRenderer = scrollableTable.getCellRenderer(row, i); // view column index i
                    Component cellComp = scrollableTable.prepareRenderer(cellRenderer, row, i); // view column index i
                    maxWidth = Math.max(maxWidth, cellComp.getPreferredSize().width);
                }
                column.setPreferredWidth(maxWidth + scrollableTable.getIntercellSpacing().width + 15); // Add padding
            }
        }

        /**
         * Set the data model for the grid
         */
        public void setModel(DefaultTableModel newModel) {
            Vector<Object> columnIdentifiers = new Vector<>();
            for (int i = 0; i < newModel.getColumnCount(); i++) {
                columnIdentifiers.add(newModel.getColumnName(i));
            }
            this.model.setDataVector(newModel.getDataVector(), columnIdentifiers);
            fixedTable.setModel(model); // Re-set to ensure listeners fire if not already
            scrollableTable.setModel(model);

            setupFixedColumns();
            setupScrollableColumns();
            synchronizeRowHeights(); // This calls adjustColumnWidths
            revalidate();
            repaint();
        }

        /**
         * Table model listener that synchronizes row heights when data changes
         */
        private class RowHeightSynchronizer implements TableModelListener {
            @Override
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                        setupFixedColumns();
                        setupScrollableColumns();
                    }
                    // Always synchronize heights and adjust widths for any change
                    synchronizeRowHeights();
                });
            }
        }

    }

    private final static RATGenerator RAT_GENERATOR = RATGenerator.getInstance();
    private static volatile Integer[] RG_ERAS;

    private final Box mainPanel = Box.createVerticalBox();
    private final JScrollPane scrollPane = new JScrollPane(mainPanel);
    private ModelRecord record;
    private final FixedColumnGrid grid;

    public AvailabilityPanel() {
        grid = new FixedColumnGrid();
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        mainPanel.add(grid);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        initializePanel();
    }

    public JComponent getPanel() {
        return scrollPane;
    }

    public void setUnit(String model, String chassis) {
        record = RAT_GENERATOR.getModelRecord((chassis + " " + model).trim());
        initializePanel();
    }

    public void reset() {
        record = null;
    }

    private void initializePanel() {
        if (!RAT_GENERATOR.isInitialized()) {
            return;
        }
        if (RG_ERAS == null) {
            synchronized (AvailabilityPanel.class) {
                RAT_GENERATOR.getEraSet().forEach(RAT_GENERATOR::loadYear);
                RG_ERAS = RAT_GENERATOR.getEraSet().toArray(new Integer[0]);
            }
        }
        DefaultTableModel newGridModel = new DefaultTableModel();
        List<String> finalColumnTitles = new ArrayList<>();
        finalColumnTitles.add("Faction");
        List<Era> allEras = Eras.getEras();
        List<Era> erasToDisplay = new ArrayList<>();
        Map<Era, Map<String, String>> eraFactionAvailabilityCache = new HashMap<>();
        if (record != null && !allEras.isEmpty()) {
            boolean[] eraHasDataFlags = new boolean[allEras.size()];
            for (int i = 0; i < allEras.size(); i++) {
                Era currentEra = allEras.get(i);
                boolean currentEraHasAnyData = false;
                for (String factionName : record.getIncludedFactions()) {
                    String availabilityTextCheck = "";
                    List<AvailabilityRating> ratingsForEra = new ArrayList<>();
                    FactionRecord factionRecord = RAT_GENERATOR.getFaction(factionName);
                    for (Integer year : RG_ERAS) {
                        if ((Eras.getEra(year) != currentEra)
                              || (factionRecord != null && !factionRecord.isActiveInYear(year))) {
                            continue;
                        }
                        AvailabilityRating modelAvailRecord = RAT_GENERATOR.findModelAvailabilityRecord(
                              year, record.getKey(), factionName);
                        if (modelAvailRecord != null) {
                            ratingsForEra.add(modelAvailRecord);
                        }
                    }
                    if (!ratingsForEra.isEmpty()) {
                        AvailabilityRating mergedAvailability = RAT_GENERATOR.mergeFactionAvailability(
                              factionName, ratingsForEra);
                        if (mergedAvailability != null) {
                            availabilityTextCheck = mergedAvailability.formatAvailability(factionRecord);
                        }
                    }
                    eraFactionAvailabilityCache.computeIfAbsent(currentEra, k -> new HashMap<>())
                          .put(factionName, availabilityTextCheck);
                    if (!availabilityTextCheck.isEmpty()) {
                        currentEraHasAnyData = true;
                    }
                }
                eraHasDataFlags[i] = currentEraHasAnyData;
            }

            // Find the first era with data, if any
            int firstEraWithDataIndex = -1;
            for (int i = 0; i < eraHasDataFlags.length; i++) {
                if (eraHasDataFlags[i]) {
                    firstEraWithDataIndex = i;
                    break;
                }
            }
            // Find the last era with data, if any
            int lastEraWithDataIndex = -1;
            if (firstEraWithDataIndex != -1) {
                for (int i = eraHasDataFlags.length - 1; i >= firstEraWithDataIndex; i--) {
                    if (eraHasDataFlags[i]) {
                        lastEraWithDataIndex = i;
                        break;
                    }
                }
            }

            if (firstEraWithDataIndex != -1 && lastEraWithDataIndex != -1) {
                for (int i = firstEraWithDataIndex; i <= lastEraWithDataIndex; i++) {
                    erasToDisplay.add(allEras.get(i));
                }
            }
        }

        for (Era era : erasToDisplay) {
            String link = String.format("<HTML><A HREF=\"http://www.masterunitlist.info/Era/Details/%s\">%s</A></HTML>",
                  era.mulId(), era.name().replace("\n", "<br>"));
            finalColumnTitles.add(link);
        }
        newGridModel.setColumnIdentifiers(finalColumnTitles.toArray());

        if (record != null) {
            for (String factionCode : record.getIncludedFactions()) {
                List<Object> rowData = new ArrayList<>();
                String baseAbbr = factionCode.split("\\.")[0];
                ImageIcon factionIcon = RAT_GENERATOR.getFactionLogo(0, baseAbbr, Color.WHITE);
                rowData.add(new FixedColumnGrid.FactionCellData(factionIcon, factionCode));

                for (Era era : erasToDisplay) {
                    String availabilityText = eraFactionAvailabilityCache
                          .getOrDefault(era, Collections.emptyMap())
                          .getOrDefault(factionCode, "-");
                    if (availabilityText.isEmpty()) {
                        availabilityText = "-";
                    }
                    rowData.add("<html><center>" + availabilityText.replace("\n", "<br>") + "</center></html>");
                }
                newGridModel.addRow(rowData.toArray());
            }
        }
        grid.setModel(newGridModel);
    }
}
