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
package megamek.client.ui.swing.lobby;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.options.OptionsConstants;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;

import static megamek.client.ui.swing.util.UIUtil.alternateTableBGColor;
import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiGreen;

public class MekTableModel extends AbstractTableModel {
    //region Variable Declarations
    private enum COLS { UNIT, PILOT, PLAYER, BV }

    public static final int COL_UNIT = COLS.UNIT.ordinal();
    public static final int COL_PILOT = COLS.PILOT.ordinal();
    public static final int COL_PLAYER = COLS.PLAYER.ordinal();
    public static final int COL_BV = COLS.BV.ordinal();
    public static final int N_COL = COLS.values().length;
    
    // Some unicode symbols. These work on Windows when setting the font 
    // to Dialog (which I believe uses Arial). I hope they work on other systems.
    public static final String DOT_SPACER = " \u2B1D ";

    /** Control value for the size of camo and portraits in the table at GUI scale == 1. */
    static final int MEKTABLE_IMGHEIGHT = 60;

    private static final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
            "unknown_unit.gif").toString();
    private static final String DEF_PORTRAIT = new MegaMekFile(Configuration.portraitImagesDir(),
            Portrait.DEFAULT_PORTRAIT_FILENAME).toString();

    // Parent access
    private final ClientGUI clientGui;
    private final ChatLounge chatLounge;

    /** The displayed entities. This list is the actual table data. */
    private final ArrayList<InGameObject> entities = new ArrayList<>();
    /** The contents of the battle value column. Gets formatted for display (font scaling). */
    private final ArrayList<Integer> bv = new ArrayList<>();
    /** The displayed contents of the Unit column. */
    private final ArrayList<String> unitCells = new ArrayList<>();
    /** The displayed contents of the Pilot column. */
    private final ArrayList<String> pilotCells = new ArrayList<>();
    /** The list of cached tooltips for the displayed units. */
    private final ArrayList<String> unitTooltips = new ArrayList<>();
    /** The list of cached tooltips for the displayed pilots. */
    private final ArrayList<String> pilotTooltips = new ArrayList<>();
    /** The displayed contents of the Player column. */
    private final ArrayList<String> playerCells = new ArrayList<>();
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    //endregion Variable Declarations

    //region Constructors
    public MekTableModel(ClientGUI cg, ChatLounge cl) {
        clientGui = cg;
        chatLounge = cl;
    }
    //endregion Constructors

    @Override
    public Object getValueAt(int row, int col) {
        final InGameObject entity = entities.get(row);
        if (entity == null) {
            return "Error: Unit not found";
        }

        if (col == COLS.BV.ordinal()) {
            boolean isEnemy = clientGui.getClient().getLocalPlayer().isEnemyOf(ownerOf(entity));
            boolean isBlindDrop = clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
            boolean localGM = clientGui.getClient().getLocalPlayer().isGameMaster();
            boolean hideEntity = !localGM && isEnemy && isBlindDrop;
            float size = chatLounge.isCompact() ? 0 : 0.2f;
            return hideEntity ? "" : guiScaledFontHTML(size) + NumberFormat.getIntegerInstance().format(bv.get(row));
            
        } else if (col == COLS.PLAYER.ordinal()) {
             return playerCells.get(row);
             
        } else if (col == COLS.PILOT.ordinal()) {
            return pilotCells.get(row);
            
        } else if (col == COLS.UNIT.ordinal()) {
            return unitCells.get(row);
            
        } else { 
            return "";
        }
    }

    @Override
    public int getRowCount() {
        return entities.size();
    }

    /** Clears all saved data of the model including the entities. */
    public void clearData() {
        entities.clear();
        bv.clear();
        unitTooltips.clear(); 
        pilotTooltips.clear();
        unitCells.clear();
        pilotCells.clear();
        playerCells.clear();
        fireTableDataChanged();
    }
    
    /** 
     * Rebuilds the display content of the table cells from the present entity list.
     * Used when the GUI scale changes. 
     */
    public void refreshCells() {
        bv.clear();
        unitTooltips.clear(); 
        pilotTooltips.clear();
        playerCells.clear();
        unitCells.clear();
        pilotCells.clear();
        for (InGameObject entity : entities) {
            addCellData(entity);
        }
        fireTableDataChanged();
    }

    /** Adds the given entity to the table and builds the display content. */
    public void addUnit(InGameObject entity) {
        entities.add(entity);
        addCellData(entity);
        fireTableDataChanged();
    }

    /** 
     * Adds display content for the given entity.
     * The entity is assumed to be the last entity added to the table and 
     * the display content will be added as a new last table row. 
     */  
    private void addCellData(InGameObject entity) {
        bv.add(entity.getStrength());
        playerCells.add(playerCellContent(entity));

        Player owner = ownerOf(entity);
        // Note that units of a player's bots are obscured because they could be added from
        // a MekHQ AtB campaign. Thus, the player can still configure them and so can identify
        // the obscured units but has to actively decide to do it.
        boolean localGM = clientGui.getClient().getLocalPlayer().isGameMaster();
        boolean hideEntity = !localGM && clientGui.getClient().getLocalPlayer().isEnemyOf(owner)
                && clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);

        if (hideEntity) {
            unitTooltips.add(null);
            pilotTooltips.add(null);
        } else {
            MapSettings mset = chatLounge.mapSettings;
            Player lPlayer = clientGui.getClient().getLocalPlayer();
            String s = UnitToolTip.lobbyTip(entity, lPlayer, mset).toString();
            unitTooltips.add(UnitToolTip.wrapWithHTML(s));
            s = PilotToolTip.lobbyTip(entity).toString();
            if (entity instanceof Entity) {
                s += PilotToolTip.getCrewAdvs((Entity) entity, true).toString();
            }
            pilotTooltips.add(UnitToolTip.wrapWithHTML(s));
        }
        final boolean rpgSkills = clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        unitCells.add(LobbyMekCellFormatter.unitTableEntry(entity, chatLounge, false, chatLounge.isCompact()));
        pilotCells.add(LobbyMekCellFormatter.pilotTableEntry(entity, chatLounge.isCompact(), hideEntity, rpgSkills));
    }
    
    /** Returns the tooltip for the given row and column from the tooltip cache. */
    public String getTooltip(int row, int col) {
        if (col == COLS.PILOT.ordinal()) {
            return pilotTooltips.get(row);
        } else if (col == COLS.UNIT.ordinal()) {
            return unitTooltips.get(row);
        } else {
            return null;
        }
    }
    
    /** 
     * Returns the column header for the given column. The header text is HTML and 
     * scaled according to the GUI scale. 
     */
    @Override
    public String getColumnName(int column) {
        String result = "<HTML>" + UIUtil.guiScaledFontHTML(0.2f);
        if (column == COLS.PILOT.ordinal()) {
            return result + Messages.getString("ChatLounge.colPilot");
        } else if (column == COLS.UNIT.ordinal()) {
            return result + Messages.getString("ChatLounge.colUnit");
        } else if (column == COLS.PLAYER.ordinal()) {
            return result + Messages.getString("ChatLounge.colPlayer");
        } else if (column == COLS.BV.ordinal()) {
            return result + Messages.getString("ChatLounge.colBV");
        } else {
            return "??";
        }
    }

    /** Returns the owner of the given entity. Prefer this over entity.getOwner(). */
    private Player ownerOf(InGameObject entity) {
        return clientGui.getClient().getGame().getPlayer(entity.getOwnerId());
    }
    
    /** Creates and returns the display content of the "Player" column for the given entity. */
    private String playerCellContent(final InGameObject entity) {
        if (entity == null) {
            return "";
        }

        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        Player owner = ownerOf(entity);
        boolean isEnemy = clientGui.getClient().getLocalPlayer().isEnemyOf(owner);
        float size = chatLounge.isCompact() ? 0 : 0.2f;
        String sep = chatLounge.isCompact() ? DOT_SPACER : "<BR>";
        result.append(guiScaledFontHTML(owner.getColour().getColour(), size)).append(owner.getName())
                .append("</FONT>").append(guiScaledFontHTML(size)).append(sep).append("</FONT>")
                .append(guiScaledFontHTML(isEnemy ? Color.RED : uiGreen(), size))
                .append(Player.TEAM_NAMES[owner.getTeam()]);
        return result.toString();
    }
    
    /** Returns the entity of the given table row. */
    public InGameObject getEntityAt(int row) {
        return entities.get(row);
    }
    
    /** Returns the subclassed cell renderer for all columns except the force column. */
    public MekTableModel.Renderer getRenderer() {
        return new MekTableModel.Renderer();
    }
    
    /** A specialized renderer for the mek table. */
    public class Renderer extends DefaultTableCellRenderer implements TableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final @Nullable Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row, final int column) {
            final InGameObject entity = getEntityAt(row);
            if ((entity == null) || (value == null)) {
                return null;
            }

            setIconTextGap(UIUtil.scaleForGUI(10));
            setText("<HTML>" + value);
            boolean compact = chatLounge.isCompact();
            if (compact) {
                setIcon(null);
            }

            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                Color background = table.getBackground();
                if (row % 2 != 0) {
                    background = alternateTableBGColor();
                }
                setBackground(background);
            }

            Player owner = ownerOf(entity);
            boolean localGM = clientGui.getClient().getLocalPlayer().isGameMaster();
            boolean showAsUnknown = !localGM && clientGui.getClient().getLocalPlayer().isEnemyOf(owner)
                    && clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
            int size = UIUtil.scaleForGUI(MEKTABLE_IMGHEIGHT);

            if (showAsUnknown) {
                setToolTipText(null);
                if (column == COLS.UNIT.ordinal()) {
                    if (!compact) {
                        setIcon(getToolkit().getImage(UNKNOWN_UNIT), size);
                    }
                } else if (column == COLS.PILOT.ordinal()) {
                    if (!compact) {
                        setIcon(getToolkit().getImage(DEF_PORTRAIT), size);
                    }
                } 
            } else {
                if (column == COLS.UNIT.ordinal()) {
                    setToolTipText(unitTooltips.get(row));
                    if (entity instanceof Entity) {
                        final Camouflage camouflage = ((Entity) entity).getCamouflageOrElseOwners();
                        final Image icon = clientGui.getBoardView().getTilesetManager().loadPreviewImage((Entity) entity, camouflage, this);
                        if (!compact) {
                            setIcon(icon, size);
                            setIconTextGap(UIUtil.scaleForGUI(10));
                        } else {
                            setIcon(icon, size / 3);
                            setIconTextGap(UIUtil.scaleForGUI(5));
                        }
                    }
                } else if (column == COLS.PILOT.ordinal()) {
                    setToolTipText(pilotTooltips.get(row));
                    if (!compact && (entity instanceof Entity)) {
                        setIcon(new ImageIcon(((Entity) entity).getCrew().getPortrait(0).getImage(size)));
                    }
                } else {
                    setToolTipText(null);
                }
            }
            
            if (column == COLS.BV.ordinal()) {
                setHorizontalAlignment(JLabel.CENTER);
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }
            
            return this;
        }
        
        private void setIcon(Image image, int height) {
            if ((image.getHeight(null) > 0) && (image.getWidth(null) > 0)) {
                int width = height * image.getWidth(null) / image.getHeight(null);
                setIcon(new ImageIcon(ImageUtil.getScaledImage(image, width, height)));
            } else {
                LogManager.getLogger().error("Trying to resize a unit icon of height or width 0!");
                setIcon(null);
            }
        }
    }
    
    
    @Override
    public int getColumnCount() {
        return N_COL;
    }
    
    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

} 