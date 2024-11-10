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
import megamek.client.ui.swing.ButtonEsc;
import megamek.client.ui.swing.CloseAction;
import megamek.client.ui.swing.dialog.DialogButton;
import megamek.common.Board;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utilities.BoardClassifier;
import megamek.utilities.BoardsTagger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * This is the dialog for advanced map filtering
 */
public class AdvancedSearchMapDialog extends AbstractButtonDialog {

    BoardClassifier bc;
    JTable boardTable;
    JList<String> listBoardTags;
    JList<String> listBoardPaths;
    JLabel boardImage;
    JLabel boardInfo;
    TableRowSorter<BoardTableModel> boardSorter;

    private BoardTableModel boardModel;
    public AdvancedSearchMapDialog(JFrame parent) {
        super(parent, true, "AdvancedSearchMapDialog", "AdvancedSearchMapDialog.title");

        initialize();
    }


    @Override
    protected JPanel createButtonPanel() {
        JButton cancelButton = new ButtonEsc(new CloseAction(this));
        JButton okButton = new DialogButton(Messages.getString("Ok.text"));
        okButton.addActionListener(this::okButtonActionPerformed);
        getRootPane().setDefaultButton(okButton);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel outerPanel = new JPanel(new GridLayout(1,1));
        outerPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
            new EmptyBorder(10, 0, 10, 0)));
        outerPanel.add(buttonPanel);

        return outerPanel;
    }

    @Override
    protected Container createCenterPane() {
        bc = BoardClassifier.getInstance();

        JPanel advancedSearchPane = new JPanel();

        advancedSearchPane.setLayout(new BoxLayout(advancedSearchPane, BoxLayout.PAGE_AXIS));

        advancedSearchPane.add(createInfo());

        JPanel mainPanel = new JPanel(new FlowLayout());
        mainPanel.add(createFilter());
        mainPanel.add(createList());

        advancedSearchPane.add(mainPanel);

        return advancedSearchPane;

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

        List<String> tags = Arrays.stream(BoardsTagger.Tags.values()).map(BoardsTagger.Tags::getName).distinct().toList();
        DefaultListModel<String> tagsModel = new DefaultListModel<>();
        tagsModel.addAll(tags);
        listBoardTags = new JList<>(tagsModel);
        listBoardTags.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listBoardTags.addSelectionInterval(0, tags.size());
        listBoardTags.addListSelectionListener (new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                filterTables();
            }
        });
        JPanel boardTagsPanel = new JPanel(new BorderLayout());
        boardTagsPanel.add(new JLabel("Board Tags"), BorderLayout.NORTH);
        boardTagsPanel.add(new JScrollPane(listBoardTags), BorderLayout.CENTER);
        filterPanel.add(boardTagsPanel);

        List<String> paths = bc.getBoardPaths().values().stream().toList();
        paths = paths.stream().map(p -> p.substring(0, p.lastIndexOf("\\") + 1 )).distinct().sorted().toList();
        DefaultListModel<String> pathsModel = new DefaultListModel<>();
        pathsModel.addAll(paths);
        listBoardPaths = new JList<>(pathsModel);
        listBoardPaths.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listBoardPaths.setSelectedIndex(0);
        listBoardPaths.addListSelectionListener (new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                filterTables();
            }
        });
        JPanel boardPathsPanel = new JPanel(new BorderLayout());
        boardPathsPanel.add(new JLabel("Board Paths"), BorderLayout.NORTH);
        boardPathsPanel.add(new JScrollPane(listBoardPaths), BorderLayout.CENTER);
        filterPanel.add(boardPathsPanel);

        return filterPanel;
    }

    private JPanel createList() {
        JPanel listPanel = new JPanel(new FlowLayout());
        boardModel = new BoardTableModel();
        boardModel.setData(bc.getBoardPaths().values().stream().toList());
        boardTable = new JTable();
        boardTable.setName("RAT");
        ListSelectionModel boardSelModel = boardTable.getSelectionModel();
        boardSelModel.addListSelectionListener (new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = boardTable.getSelectedRow() ;
                if (index >= 0) {
                    index = boardTable.convertRowIndexToModel(index);
                    boardImage.setIcon(boardModel.getIconAt(index, 200));
                    boardInfo.setText(boardModel.getInfoAt(index));
                }
            }
        });
        boardTable.setModel(boardModel);
        boardSorter = new TableRowSorter<>(boardModel);
        boardTable.setRowSorter(boardSorter);
        boardTable.setIntercellSpacing(new Dimension(5, 0));
        boardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (int i = 0; i < boardModel.getColumnCount(); i++) {
            boardTable.getColumnModel().getColumn(i).setPreferredWidth(boardModel.getPreferredWidth(i));
        }
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

        listPanel.add(new JScrollPane(boardTable));

        boardTable.setRowSelectionInterval(0,0);

        return listPanel;
    }

    void filterTables() {
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

                    return pathMatch && tagMatch;
                }
            };
        } catch (PatternSyntaxException ignored) {
            return;
        }

        boardSorter.setRowFilter(boardFilter);
    }

    boolean matchPath(String path) {
        List<String> include = listBoardPaths.getSelectedValuesList();

        String value = path.substring(0, path.lastIndexOf("\\") + 1 );

        return !include.isEmpty() && include.stream().anyMatch(value::contains);
    }

    boolean matchTag(List<String> tags) {
        List<String> include = listBoardTags.getSelectedValuesList();

        return !include.isEmpty() && include.stream().anyMatch(tags::contains);
    }

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
