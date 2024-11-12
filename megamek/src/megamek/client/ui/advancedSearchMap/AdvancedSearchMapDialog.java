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
package megamek.client.ui.advancedSearchMap;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.util.StringUtil;
import megamek.utilities.BoardClassifier;
import megamek.utilities.BoardsTagger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * This is the dialog for advanced map filtering
 */
public class AdvancedSearchMapDialog extends AbstractButtonDialog {
    private BoardClassifier bc;
    private JTable boardTable = new JTable() {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Dimension standardSize = super.getPreferredScrollableViewportSize();
            return new Dimension(standardSize.width, getRowHeight() * 20);
        }
    };
    private JList<String> listBoardTags = new JList<>();
    private final JCheckBox boardTagsAllCheckBox = new JCheckBox(Messages.getString("AdvancedSearchMapDialog.boardTagsAllCheckBox"));
    private JList<String> listBoardPaths = new JList<>();
    private JLabel boardImage;
    private JLabel boardInfo;
    private TableRowSorter<BoardTableModel> boardSorter = new TableRowSorter<>();
    private BoardTableModel boardModel = new BoardTableModel();
    private JLabel boardCountLabel = new JLabel("");
    private JTextField widthStartTextField = new JTextField(4);
    private JTextField widthEndTextField = new JTextField(4);
    private JTextField heightStartTextField = new JTextField(4);
    private JTextField heightEndTextField = new JTextField(4);
    private JTextField nameTextField = new JTextField(4);

    public AdvancedSearchMapDialog(JFrame parent) {
        super(parent, true, "AdvancedSearchMapDialog", "AdvancedSearchMapDialog.title");

        setPreferredSize(new Dimension(1200, 1600));

        initialize();
    }

    @Override
    protected Container createCenterPane() {
        bc = BoardClassifier.getInstance();

        JPanel advancedSearchPane = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;

        advancedSearchPane.add(createInfo(), c);

        JPanel mainPanel = new JPanel(new FlowLayout());
        mainPanel.add(createFilter());
        mainPanel.add(createListTable());

        c.gridy++;

        advancedSearchPane.add(mainPanel, c);

        return new JScrollPane(advancedSearchPane);
    }

    private JPanel createInfo() {
        JPanel infoPanel = new JPanel(new FlowLayout());
        boardImage = new JLabel();
        infoPanel.add(boardImage);
        boardInfo = new JLabel();
        infoPanel.add(boardInfo);
        return infoPanel;
    }

    private JPanel createFilter() {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.PAGE_AXIS));

        filterPanel.add(createFilterRange(widthStartTextField, widthEndTextField, Messages.getString("AdvancedSearchMapDialog.filterRangeWidth")));
        filterPanel.add(createFilterRange(heightStartTextField, heightEndTextField, Messages.getString("AdvancedSearchMapDialog.filterRangeHeight")));
        filterPanel.add(createFilterText(nameTextField, Messages.getString("AdvancedSearchMapDialog.filterName")));

        JPanel tagsTitlePanel = createTitleWithCheckBox(boardTagsAllCheckBox, Messages.getString("AdvancedSearchMapDialog.filterBoardTags"));
        List<String> tags = Arrays.stream(BoardsTagger.Tags.values()).map(BoardsTagger.Tags::getName).distinct().sorted().toList();
        filterPanel.add(createFilterList(listBoardTags, tags, tagsTitlePanel, true));

        JPanel pathsTitlePanel = createTitle(Messages.getString("AdvancedSearchMapDialog.filterBoardPaths"));
        List<String> paths = bc.getBoardPaths().values().stream().toList();
        paths = paths.stream().map(p -> p.substring(0, p.lastIndexOf("\\") + 1 )).distinct().sorted().toList();
        filterPanel.add(createFilterList(listBoardPaths, paths, pathsTitlePanel, false));

        return filterPanel;
    }

    private JPanel createFilterText(JTextField textField, String caption) {
        JPanel textPanel = new JPanel(new BorderLayout());
        Box textBox = new Box(BoxLayout.LINE_AXIS);
        textBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBox.add(new JLabel(caption));
        textBox.add(Box.createRigidArea( new Dimension(5, 0)));
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTables();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTables();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTables();
            }
        });
        textBox.add(textField);

        textPanel.add(textBox);

        return textPanel;
    }

    private JPanel createFilterRange(JTextField startTextField, JTextField endTextField, String caption) {
        JPanel textPanel = new JPanel(new BorderLayout());

        Box textBox = new Box(BoxLayout.LINE_AXIS);
        textBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBox.add(new JLabel(caption));
        textBox.add(Box.createRigidArea( new Dimension(5, 0)));
        startTextField.setToolTipText(Messages.getString("AdvancedSearchMapDialog.filterRangeStart.tooltip"));
        startTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTables();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTables();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTables();
            }
        });
        textBox.add(startTextField);

        textBox.add(new JLabel(" - "));

        endTextField.setToolTipText(Messages.getString("AdvancedSearchMapDialog.filterRangeEnd.tooltip"));
        endTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTables();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTables();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTables();
            }
        });
        textBox.add(endTextField);

        textPanel.add(textBox, BorderLayout.NORTH);
        return textPanel;
    }

    private JPanel createTitleWithCheckBox(JCheckBox checkBox, String caption) {
        JPanel titlePanel = new JPanel(new BorderLayout());

        Box titleBox = new Box(BoxLayout.LINE_AXIS);
        titleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(new JLabel(caption));
        titleBox.add(Box.createRigidArea( new Dimension(5, 0)));
        checkBox.setSelected(false);
        checkBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterTables();
            }
        });
        titleBox.add(checkBox);
        titlePanel.add(titleBox, BorderLayout.NORTH);

        return titlePanel;
    }

    private JPanel createTitle(String caption) {
        JPanel titlePanel = new JPanel(new BorderLayout());

        Box titleBox = new Box(BoxLayout.LINE_AXIS);
        titleBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(new JLabel(caption));
        titlePanel.add(titleBox, BorderLayout.NORTH);

        return titlePanel;
    }

    private JPanel createFilterList(JList list, List<String> data, JPanel title, boolean selectAll) {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addAll(data);
        list.setModel(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setSelectedIndex(0);
        list.addListSelectionListener (new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                filterTables();
            }
        });

        if (selectAll) {
            list.addSelectionInterval(0, data.size());
        }

        JPanel boardPathsPanel = new JPanel(new BorderLayout());
        boardPathsPanel.add(title, BorderLayout.NORTH);
        boardPathsPanel.add(new JScrollPane(list), BorderLayout.CENTER);

        return boardPathsPanel;
    }

    private JPanel createListTable() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));

        boardModel.setData(bc);
        boardTable.setName("Board");
        ListSelectionModel boardSelModel = boardTable.getSelectionModel();
        boardSelModel.addListSelectionListener (new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = boardTable.getSelectedRow() ;
                if (index >= 0) {
                    index = boardTable.convertRowIndexToModel(index);
                    boardImage.setIcon(boardModel.getIconAt(index, UIUtil.scaleForGUI(200)));
                    boardInfo.setText(boardModel.getInfoAt(index));
                }
            }
        });
        boardTable.setModel(boardModel);
        boardSorter.setModel(boardModel);
        boardSorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        boardTable.setRowSorter(boardSorter);
        boardTable.setIntercellSpacing(new Dimension(5, 0));
        boardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int i = 0; i < boardModel.getColumnCount(); i++) {
            boardTable.getColumnModel().getColumn(i).setPreferredWidth(boardModel.getPreferredWidth(i));
        }
        boardTable.setRowSelectionInterval(0,0);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        boardTable.setFillsViewportHeight(true);
        listPanel.add(new JScrollPane(boardTable));

        JPanel countPanel = new JPanel(new FlowLayout());
        JLabel countLabel = new JLabel(Messages.getString("AdvancedSearchMapDialog.boardTableCount"));
        countPanel.add(countLabel);

        boardCountLabel.setText(boardModel.getRowCount() + "");
        countPanel.add(boardCountLabel);
        listPanel.add(countPanel);

        return listPanel;
    }

    private void filterTables() {
        RowFilter<BoardTableModel, Integer> boardFilter;
        try {
            boardFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends BoardTableModel, ? extends Integer> entry) {
                    BoardTableModel eqModel = entry.getModel();

                    String path = eqModel.getPathAt(entry.getIdentifier());
                    boolean pathMatch = matchPath(path);

                    List<String> tags = eqModel.getTagAt(entry.getIdentifier());
                    boolean tagMatch = matchTag(tags);

                    boolean widthMatch = StringUtil.isBetween(eqModel.getWidthAt(entry.getIdentifier()), widthStartTextField.getText(), widthEndTextField.getText());

                    boolean heightMatch = StringUtil.isBetween(eqModel.getHeightAt(entry.getIdentifier()), heightStartTextField.getText(), heightEndTextField.getText());

                    boolean nameMatch = eqModel.getPathAt(entry.getIdentifier()).toUpperCase().contains(nameTextField.getText().toUpperCase());

                    return pathMatch && tagMatch && widthMatch && heightMatch && nameMatch;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }

        boardSorter.setRowFilter(boardFilter);
        boardCountLabel.setText(boardTable.getRowCount() + "");

        if (boardTable.getRowCount() > 0) {
            boardTable.setRowSelectionInterval(0, 0);
            boardTable.scrollRectToVisible(boardTable.getCellRect(0, 0, true));
        }
    }

    private boolean matchPath(String path) {
        List<String> include = listBoardPaths.getSelectedValuesList();

        String value = path.substring(0, path.lastIndexOf("\\") + 1 );

        return !include.isEmpty() && include.stream().anyMatch(value::contains);
    }

    private boolean matchTag(List<String> tags) {
        List<String> include = listBoardTags.getSelectedValuesList();

        if (boardTagsAllCheckBox.isSelected()) {
            return !include.isEmpty() && include.stream().allMatch(tags::contains);
        } else {
            return !include.isEmpty() && include.stream().anyMatch(tags::contains);
        }
    }

    /**
     * Returns if path for the selected map when the dialog is confirmed
     */
    public String getPath() {
        int index = boardTable.getSelectedRow() ;
        if (index >= 0) {
            index = boardTable.convertRowIndexToModel(index);
            String path =  boardModel.getPathAt(index);
            return path;
        }

        return null;
    }
}
