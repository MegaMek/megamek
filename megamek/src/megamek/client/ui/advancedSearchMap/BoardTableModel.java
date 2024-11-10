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

import megamek.client.ui.swing.minimap.Minimap;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utilities.BoardClassifier;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A table model for the advanced map search
 */
class BoardTableModel extends AbstractTableModel {
    private static final int COL_NAME = 0;
    private static final int COL_SIZE = 1;
    private static final int N_COL = 2;

    private List<String> data;
    private List<String> tags;
    private List<String> size;
    private List<Integer> width;
    private List<Integer> height;


    public BoardTableModel() {
        data = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    public void clearData() {
        data = new ArrayList<>();
        tags = new ArrayList<>();
        size = new ArrayList<>();
        width = new ArrayList<>();
        height = new ArrayList<>();
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    public int getPreferredWidth(int col) {
        return switch (col) {
            case COL_NAME -> 200;
            case COL_SIZE -> 20;
            default -> 10;
        };
    }

    public void setData(BoardClassifier bc) {
        data = bc.getBoardPaths().values().stream().toList();;
        tags = new ArrayList<>();
        size = new ArrayList<>();
        width = new ArrayList<>();
        height = new ArrayList<>();

        for (String path : data) {
            String key = Configuration.boardsDir() + path;
            tags.add(bc.getBoardTags().get(key));
            size.add(bc.getBoardWidth().get(key) + "x" + bc.getBoardHeigth().get(key));
            width.add(bc.getBoardWidth().get(key));
            height.add(bc.getBoardHeigth().get(key));
        }

        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case COL_NAME:
                return "Name";
            case COL_SIZE:
                return "Size";
            default:
                return "??";
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int col) {
        String path = getPathAt(row);
        String size = getSizeAt(row);

        if (path== null) {
            return "?";
        }

        String value = "";

        if (col == COL_NAME) {
            value = path.substring(path.lastIndexOf("\\") + 1, path.length());
            value = value.substring(0, value.lastIndexOf(".board"));
            value = value.replace(size, "").trim();
        } else if (col == COL_SIZE) {
            value = size;
        }

        return value;
    }

    public String getPathAt(int row) {
        if (data.size() <= row) {
            return null;
        }

        return data.get(row);
    }

    public String getSizeAt(int row) {
        if (size.size() <= row) {
            return null;
        }

        return size.get(row);
    }

    public Integer getWidthAt(int row) {
        if (width.size() <= row) {
            return null;
        }

        return width.get(row);
    }

    public Integer getHeightAt(int row) {
        if (height.size() <= row) {
            return null;
        }

        return height.get(row);
    }

    public List<String> getAllPaths() {
        return data;
    }

    public ImageIcon getIconAt(int row, int height) {
        String path = getPathAt(row);
        Board board = new Board(16, 17);
        board.load(new MegaMekFile(Configuration.boardsDir(), path).getFile());

        BufferedImage image = Minimap.getMinimapImageMaxZoom(board);

        int scaledHeight = Math.min(image.getHeight(), height);
        int scaledWidth = Math.max(1, image.getWidth() * scaledHeight / image.getHeight());

        image = ImageUtil.getScaledImage(image, scaledWidth, scaledHeight);
        ImageIcon icon = new ImageIcon(image);
        return icon;
    }

    public String getInfoAt(int row) {
        String path = getPathAt(row);
        Board board = new Board(16, 17);
        board.load(new MegaMekFile(Configuration.boardsDir(), path).getFile());

        String info;
        String col = UIUtil.tag("td", "", path);
        info = UIUtil.tag("tr", "", col);
        col = UIUtil.tag("td", "", board.getWidth() + "x" + board.getHeight());
        info += UIUtil.tag("tr", "", col);
        col = UIUtil.tag("td", "", board.getTags().toString());
        info += UIUtil.tag("tr", "", col);
        info = UIUtil.tag("table", "", info);
        String attr = String.format("WIDTH=%s", UIUtil.scaleForGUI(500));
        info = UIUtil.tag("div", attr,  info);
        info = UIUtil.tag("body", "", info);
        info = UIUtil.tag("html", "", info);

        return info;
    }

    public List<String> getTagAt(int row) {
        String tag = tags.get(row);
        tag = tag.substring(1, tag.length() -1);
        return Arrays.stream(tag.split(", ")).toList();
    }
}
