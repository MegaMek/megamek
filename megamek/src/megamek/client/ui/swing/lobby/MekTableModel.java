/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/ 
package megamek.client.ui.swing.lobby;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.text.MessageFormat;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ChatLounge;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.icons.Camouflage;
import megamek.common.icons.Portrait;
import megamek.common.options.*;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.CrewSkillSummaryUtil;
import megamek.common.util.fileUtils.MegaMekFile;

import static megamek.client.ui.swing.util.UIUtil.*;

public class MekTableModel extends AbstractTableModel {
    
    private static final long serialVersionUID = 4819661751806908535L;

    public static final int COL_UNIT = 0;
    public static final int COL_PILOT = 1;
    public static final int COL_PLAYER = 2;
    public static final int COL_BV = 3;
    public static final int N_COL = 4;
    
    // Some unicode symbols. These work on Windows when setting the font 
    // to Dialog (which I believe uses Arial). I hope they work on other systems.
    public static final String DOT_SPACER = " \u2B1D ";
    private static final String LOADED_SIGN = " \u26DF ";
    private static final String UNCONNECTED_SIGN = " \u26AC";
    private static final String CONNECTED_SIGN = " \u26AF ";
    private static final String WARNING_SIGN = " \u26A0 ";
    
    /** Control value for the size of camo and portraits in the table at GUI scale == 1. */
    private static final int MEKTABLE_IMGHEIGHT = 60;
    
    private static final String UNKNOWN_UNIT = new MegaMekFile(Configuration.miscImagesDir(),
            "unknown_unit.gif").toString();
    private static final String DEF_PORTRAIT = new MegaMekFile(Configuration.portraitImagesDir(),
            Portrait.DEFAULT_PORTRAIT_FILENAME).toString();

    // Parent access
    private ClientGUI clientGui;
    private ChatLounge chatLounge;

    /** The displayed entities. This list is the actual table data. */
    private ArrayList<Entity> entities = new ArrayList<>();
    /** The contents of the battle value column. Gets formatted for display (font scaling). */
    private ArrayList<Integer> bv = new ArrayList<>();
    /** The displayed contents of the Unit column. */
    private ArrayList<String> unitCells = new ArrayList<>();
    /** The displayed contents of the Pilot column. */
    private ArrayList<String> pilotCells = new ArrayList<>();
    /** The list of cached tooltips for the displayed units. */
    private ArrayList<String> unitTooltips = new ArrayList<>();
    /** The list of cached tooltips for the displayed pilots. */
    private ArrayList<String> pilotTooltips = new ArrayList<>();
    /** The displayed contents of the Player column. */
    private ArrayList<String> playerCells = new ArrayList<>();

    public MekTableModel(ClientGUI cg, ChatLounge cl) {
        clientGui = cg;
        chatLounge = cl;
    }
    
    @Override
    public Object getValueAt(int row, int col) {
        final Entity entity = entities.get(row);
        if (entity == null) {
            return "Error: Unit not found";
        }

        if (col == COL_BV) {
            boolean isEnemy = clientGui.getClient().getLocalPlayer().isEnemyOf(ownerOf(entity));
            boolean hideEntity = isEnemy && isBlindDrop();
            float size = chatLounge.isCompact() ? 0 : 0.2f;
            return hideEntity ? "" : guiScaledFontHTML(size) + bv.get(row);
            
        } else if (col == COL_PLAYER) {
             return playerCells.get(row);
             
        } else if (col == COL_PILOT) {
            return pilotCells.get(row);
            
        } else { // UNIT
            return unitCells.get(row);
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
        for (Entity entity: entities) {
            addCellData(entity);
        }
        fireTableDataChanged();
    }

    /** Adds the given entity to the table and builds the display content. */
    public void addUnit(Entity entity) {
        entities.add(entity);
        addCellData(entity);
        fireTableDataChanged();
    }

    /** 
     * Adds display content for the given entity.
     * The entity is assumed to be the last entity added to the table and 
     * the display content will be added as a new last table row. 
     */  
    private void addCellData(Entity entity) {
        bv.add(entity.calculateBattleValue());
        playerCells.add(playerCellContent(entity));

        IPlayer owner = ownerOf(entity);
        boolean hideEntity = !owner.equals(clientGui.getClient().getLocalPlayer())
                && clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
        if (hideEntity) {
            unitTooltips.add(null);
            pilotTooltips.add(null);
        } else {
            MapSettings mset = chatLounge.getMapSettings();
            IPlayer lPlayer = clientGui.getClient().getLocalPlayer();
            unitTooltips.add("<HTML>" + UnitToolTip.getEntityTipLobby(entity, lPlayer, mset));
            pilotTooltips.add("<HTML>" + PilotToolTip.getPilotTipDetailed(entity));
        }
        final boolean rpgSkills = clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        if (chatLounge.isCompact()) {
            unitCells.add(formatUnitCompact(entity, hideEntity, chatLounge.getMapSettings().getMedium()));
            pilotCells.add(formatPilotCompact(entity.getCrew(), hideEntity, rpgSkills));
        } else {
            unitCells.add(formatUnitFull(entity, hideEntity, chatLounge.getMapSettings().getMedium()));
            pilotCells.add(formatPilotFull(entity, hideEntity));
        }

    }
    
    /** Returns the tooltip for the given row and column from the tooltip cache. */
    public String getTooltip(int row, int col) {
        if (col == COL_PILOT) {
            return pilotTooltips.get(row);
        } else if (col == COL_UNIT) {
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
        switch (column) {
            case (COL_PILOT):
                return result + Messages.getString("ChatLounge.colPilot");
            case (COL_UNIT):
                return result + Messages.getString("ChatLounge.colUnit");
            case (COL_PLAYER):
                return result + Messages.getString("ChatLounge.colPlayer");
            case (COL_BV):
                return result + Messages.getString("ChatLounge.colBV");
        }
        return "??";
    }
    
    /** Returns the owner of the given entity. Prefer this over entity.getOwner(). */
    private IPlayer ownerOf(Entity entity) {
        return clientGui.getClient().getGame().getPlayer(entity.getOwnerId());
    }
    
    /** Creates and returns the display content of the "Player" column for the given entity. */
    private String playerCellContent(final Entity entity) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        IPlayer owner = ownerOf(entity);
        boolean isEnemy = clientGui.getClient().getLocalPlayer().isEnemyOf(owner);
        float size = chatLounge.isCompact() ? 0 : 0.2f;
        String sep = chatLounge.isCompact() ? DOT_SPACER : "<BR>";
        result.append(guiScaledFontHTML(PlayerColors.getColor(owner.getColorIndex()), size));
        result.append(owner.getName());
        result.append("</FONT>" + guiScaledFontHTML(size) + sep + "</FONT>");
        result.append(guiScaledFontHTML(isEnemy ? Color.RED : uiGreen(), size));
        result.append(IPlayer.teamNames[owner.getTeam()]);
        return result.toString();
    }
    
    /** Returns the entity of the given table row. */
    public Entity getEntityAt(int row) {
        return entities.get(row);
    }
    

    /** Returns the subclassed cell renderer for this table. */
    public MekTableModel.Renderer getRenderer() {
        return new MekTableModel.Renderer();
    }

    /** A specialized renderer for the mek table. */
    public class Renderer extends DefaultTableCellRenderer implements TableCellRenderer {
        
        private static final long serialVersionUID = -9154596036677641620L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Entity entity = getEntityAt(row);
            if (null == entity) {
                return null;
            }
            setIconTextGap(UIUtil.scaleForGUI(10));
            setText("<HTML>" + value.toString());
            boolean compact = chatLounge.isCompact();
            if (compact) {
                setIcon(null);
            }
            
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                Color background = table.getBackground();
                if (row % 2 != 0) {
                    Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                    if (alternateColor == null) {
                        // If we don't have an alternate row color, use 'controlHighlight'
                        // as it is pretty reasonable across the various themes.
                        alternateColor = UIManager.getColor("controlHighlight");
                    }
                    if (alternateColor != null) {
                        background = alternateColor;
                    }
                }
                setForeground(table.getForeground());
                setBackground(background);
            }
            
            IPlayer owner = ownerOf(entity);
            boolean showAsUnknown = !owner.equals(clientGui.getClient().getLocalPlayer())
                    && clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
            int size = UIUtil.scaleForGUI(MEKTABLE_IMGHEIGHT);
            
            if (showAsUnknown) {
                setToolTipText(null);
                if (column == COL_UNIT) {
                    if (!compact) {
                        Image image = getToolkit().getImage(UNKNOWN_UNIT);
                        setIcon(new ImageIcon(image.getScaledInstance(-1, size, Image.SCALE_SMOOTH)));
                    }
                } else if (column == COL_PILOT) {
                    if (!compact) {
                        Image image = getToolkit().getImage(DEF_PORTRAIT);
                        setIcon(new ImageIcon(image.getScaledInstance(-1, size, Image.SCALE_SMOOTH)));
                    }
                }
            } else {
                if (column == COL_UNIT) {
                    setToolTipText(unitTooltips.get(row));
                    if (!compact) {
                        Image camo = owner.getCamouflage().getImage();
                        if ((entity.getCamoCategory() != null) && !entity.getCamoCategory().equals(Camouflage.NO_CAMOUFLAGE)) {
                            camo = entity.getCamouflage().getImage();
                        } 
                        Image image = clientGui.bv.getTilesetManager().loadPreviewImage(entity, camo, 0, this);
                        setIcon(new ImageIcon(image.getScaledInstance(-1, size, Image.SCALE_SMOOTH)));
                    }
                } else if (column == COL_PILOT) {
                    setToolTipText(pilotTooltips.get(row));
                    if (!compact) {
                        setIcon(new ImageIcon(entity.getCrew().getPortrait(0).getImage(size)));
                    }
                }
            }

            return this;
        }
    }
    
    /** Returns true when the game option Blind Drop is active. */ 
    private boolean isBlindDrop() {
        return clientGui.getClient().getGame().getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
    }
    
    @Override
    public int getColumnCount() {
        return N_COL;
    }
    

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    /** 
     * Creates and returns the display content of the Unit column for the given entity and 
     * for the compact display mode. 
     * When blindDrop is true, the unit details are not given.
     */
    private static String formatUnitCompact(Entity entity, boolean blindDrop, int mapType) {
        
        if (blindDrop) {
            String value = "<HTML><NOBR>";
            if (PreferenceManager.getClientPreferences().getShowUnitId()) {
                value += UIUtil.guiScaledFontHTML(UIUtil.uiGray());
                value += MessageFormat.format("[{0}] </FONT>", entity.getId());
            }
            String uType;
            if (entity instanceof Infantry) {
                uType = Messages.getString("ChatLounge.0");
            } else if (entity instanceof Protomech) {
                uType = Messages.getString("ChatLounge.1");
            } else if (entity instanceof GunEmplacement) {
                uType = Messages.getString("ChatLounge.2");
            } else {
                uType = entity.getWeightClassName();
                if (entity instanceof Tank) {
                    uType += Messages.getString("ChatLounge.6");
                }
            }
            return value + UIUtil.guiScaledFontHTML() + DOT_SPACER + uType + DOT_SPACER;
        }
        
        StringBuilder result = new StringBuilder("<HTML><NOBR>" + UIUtil.guiScaledFontHTML());
        
        // Signs before the unit name
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            result.append(UIUtil.guiScaledFontHTML(UIUtil.uiGray()));
            result.append(MessageFormat.format("[{0}] </FONT>", entity.getId()));
        }

        // Critical (Red) Warnings
        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                || (!entity.isDesignValid())
                ) {
            result.append(guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor())); 
            result.append(WARNING_SIGN + "</FONT>");
        }
        
        // General (Yellow) Warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                ) {
            result.append(guiScaledFontHTML(uiYellow())); 
            result.append(WARNING_SIGN + "</FONT>");
        }
        
        // Loaded unit
        if (entity.getTransportId() != Entity.NONE) {
            result.append(guiScaledFontHTML(uiGreen()) + LOADED_SIGN + "</FONT>");
        }
        
        // Unit name
        result.append(entity.getShortNameRaw());

        // Invalid unit design
        if (!entity.isDesignValid()) {
            result.append(DOT_SPACER + guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()));
            result.append(Messages.getString("ChatLounge.invalidDesign"));
            result.append("</FONT>");
        }
        
        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
            result.append(Messages.getString("BT.Quirks") + ": ");
            result.append(quirkCount + "</FONT>");
        }
        
        // C3 ...
        if (entity.hasC3i() || entity.hasNavalC3()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()));
            String c3Name = entity.hasC3i() ? "C3i" : "NC3";
            if (entity.calculateFreeC3Nodes() >= 5) {
                result.append(c3Name + UNCONNECTED_SIGN);
            } else {
                result.append(c3Name + CONNECTED_SIGN + entity.getC3NetId());
            }
            result.append("</FONT>");
        } 

        if (entity.hasC3()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()));
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    result.append("C3S" + UNCONNECTED_SIGN); 
                }  
                if (entity.hasC3M()) {
                    result.append("C3M"); 
                }
            } else if (entity.C3MasterIs(entity)) {
                result.append("C3M (CC)");
            } else {
                if (entity.hasC3S()) {
                    result.append("C3S" + CONNECTED_SIGN); 
                } else {
                    result.append("C3M" + CONNECTED_SIGN); 
                }
                result.append(entity.getC3Master().getChassis());
            }
            result.append("</FONT>");
        }
        
        // Loaded onto another unit
        if (entity.getTransportId() != Entity.NONE) {
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) +  "<I>(");
            result.append(loader.getChassis() + ")</I></FONT>");
        }

        // Hidden deployment
        if (entity.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.hidden") + "</I></FONT>");
        }
        
        if (entity.isHullDown()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.hulldown") + "</I></FONT>");
        }
        
        if (entity.isProne()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.prone") + "</I></FONT>");
        }
        
        if (entity.countPartialRepairs() > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiLightRed()));
            result.append("Partial Repairs</FONT>");
        }

        // Offboard deployment
        if (entity.isOffBoard()) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>"); 
            result.append(Messages.getString("ChatLounge.compact.deploysOffBoard") + "</I></FONT>");
        } else if (entity.getDeployRound() > 0) {
            result.append(DOT_SPACER + UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.deployRound", entity.getDeployRound()));
            if (entity.getStartingPos(false) != Board.START_NONE) {
                result.append(Messages.getString("ChatLounge.compact.deployZone", 
                        IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]));
            }
            result.append("</I></FONT>");
        }

        // Starting values for Altitude / Velocity / Elevation
        if (entity.isAero()) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>"); 
            Aero aero = (Aero) entity;
            result.append(Messages.getString("ChatLounge.compact.velocity") + ": ");
            result.append(aero.getCurrentVelocity());
            if (mapType != MapSettings.MEDIUM_SPACE) {
                result.append(", " + Messages.getString("ChatLounge.compact.altitude") + ": ");
                result.append(aero.getAltitude());
            } 
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                result.append(", " + Messages.getString("ChatLounge.compact.fuel") + ": ");
                result.append(aero.getCurrentFuel());
            }
            result.append("</I></FONT>");
        } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiGreen()) + "<I>");
            result.append(Messages.getString("ChatLounge.compact.elevation") + ": ");
            result.append(entity.getElevation() + "</I></FONT>");
        }
        return result.toString(); 
    }

    /** 
     * Creates and returns the display content of the Unit column for the given entity and
     * for the non-compact display mode.
     * When blindDrop is true, the unit details are not given.
     */
    private static String formatUnitFull(Entity entity, boolean blindDrop, int mapType) {

        String value = "<HTML><NOBR>" + UIUtil.guiScaledFontHTML();

        if (blindDrop) {
            value += DOT_SPACER;
            if (entity instanceof Infantry) {
                value += Messages.getString("ChatLounge.0"); 
            } else if (entity instanceof Protomech) {
                value += Messages.getString("ChatLounge.1"); 
            } else if (entity instanceof GunEmplacement) {
                value += Messages.getString("ChatLounge.2"); 
            } else {
                value += entity.getWeightClassName();
                if (entity instanceof Tank) {
                    value += Messages.getString("ChatLounge.6"); 
                }
            }
            value += DOT_SPACER;
            return value;
        } 
        
        // ID
        if (PreferenceManager.getClientPreferences().getShowUnitId()) {
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGray());
            value += "[ID: " + entity.getId() + "] </FONT>";
        }

        boolean hasWarning = false;
        boolean hasCritical = false;
        // General (Yellow) Warnings
        if (((entity.hasC3i() || entity.hasNavalC3()) && (entity.calculateFreeC3Nodes() == 5))
                || ((entity.getC3Master() == null) && entity.hasC3S())
                ) {
            value += UIUtil.guiScaledFontHTML(UIUtil.uiYellow()); 
            value += WARNING_SIGN + "</FONT>";
            hasWarning = true;
        }

        // Critical (Red) Warnings
        if ((entity.getGame().getPlanetaryConditions().whyDoomed(entity, entity.getGame()) != null)
                || (entity.doomedInAtmosphere() && mapType == MapSettings.MEDIUM_ATMOSPHERE)
                || (entity.doomedOnGround() && mapType == MapSettings.MEDIUM_GROUND)
                || (entity.doomedInSpace() && mapType == MapSettings.MEDIUM_SPACE)
                || (!entity.isDesignValid())
                ) {
            value += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor()); 
            value += WARNING_SIGN + "</FONT>";
            hasCritical = true;
        }
        
        // Unit Name
        if (hasCritical) {
            value += UIUtil.guiScaledFontHTML(GUIPreferences.getInstance().getWarningColor());
        } else if (hasWarning) {
            value += UIUtil.guiScaledFontHTML(UIUtil.uiYellow());
        } else {
            value += UIUtil.guiScaledFontHTML();
        }
        value += "<B>" + entity.getShortNameRaw() + "</B></FONT><BR>";

        // SECOND LINE----
        // Tonnage
        value += UIUtil.guiScaledFontHTML();
        value += Math.round(entity.getWeight()) + Messages.getString("ChatLounge.Tons");
        value += "</FONT>";

        // Invalid Design
        if (!entity.isDesignValid()) {
            value += DOT_SPACER;
            value += Messages.getString("ChatLounge.invalidDesign");
        }
        
        // Quirk Count
        int quirkCount = entity.countQuirks() + entity.countWeaponQuirks();
        if (quirkCount > 0) {
            value += DOT_SPACER;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiQuirksColor());
            value += Messages.getString("ChatLounge.Quirks") + ": ";
            
            int posQuirks = entity.countQuirks(Quirks.POS_QUIRKS);
            String pos = posQuirks > 0 ? "+" + posQuirks : ""; 
            int negQuirks = entity.countQuirks(Quirks.NEG_QUIRKS);
            String neg = negQuirks > 0 ? "-" + negQuirks : "";
            int wpQuirks = entity.countWeaponQuirks();
            String wpq = wpQuirks > 0 ? "W" + wpQuirks : "";
            value += UIUtil.arrangeInLine(" / ", pos, neg, wpq);
            
            value += "</FONT>";
        }

        // Partial Repairs
        int partRepCount = entity.countPartialRepairs();
        if ((partRepCount > 0)) {
            value += DOT_SPACER;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiLightRed());
            value += Messages.getString("ChatLounge.PartialRepairs");
            value += "</FONT>";
        }
        value += "<BR>";
        
        // THIRD LINE----
        
        // Controls the separator dot character
        boolean subsequentElement = false;

        // C3 ...
        if (entity.hasC3i()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color());
            if (entity.calculateFreeC3Nodes() >= 5) {
                value += "C3i" + UNCONNECTED_SIGN;
            } else {
                value += "C3i" + CONNECTED_SIGN + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                    value += Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes());
                }
            }
            value += "</FONT>";
        } 
        
        if (entity.hasNavalC3()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color());
            if (entity.calculateFreeC3Nodes() >= 5) {
                value += "NC3" + UNCONNECTED_SIGN;
            } else {
                value += "NC3" + CONNECTED_SIGN + entity.getC3NetId();
                if (entity.calculateFreeC3Nodes() > 0) {
                    value += Messages.getString("ChatLounge.C3iNodes", entity.calculateFreeC3Nodes());
                }
            }
            value += "</FONT>";
        } 
        
        if (entity.hasC3()) {
            
            if (entity.getC3Master() == null) {
                if (entity.hasC3S()) {
                    if (subsequentElement) {
                        value += DOT_SPACER;
                    }
                    subsequentElement = true;
                    value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()) + "C3 Slave" + UNCONNECTED_SIGN;
                    value += "</FONT>";
                } 
                
                if (entity.hasC3M()) {
                    if (subsequentElement) {
                        value += DOT_SPACER;
                    }
                    subsequentElement = true;

                    value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()) + "C3 Master";
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        value += " (full)";
                    } else {
                        value += Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes());
                    }
                    value += "</FONT>";
                }
            } else if (entity.C3MasterIs(entity)) {
                if (subsequentElement) {
                    value += DOT_SPACER;
                }
                subsequentElement = true;
                value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color()) + "C3 Company Commander";
                if (entity.hasC3MM()) {
                    value += MessageFormat.format(" ({0}M, {1}S free)", entity.calculateFreeC3MNodes(), entity.calculateFreeC3Nodes());
                } else {
                    value += Messages.getString("ChatLounge.C3MNodes", entity.calculateFreeC3MNodes());
                }
                value += "</FONT>";
            } else {
                if (subsequentElement) {
                    value += DOT_SPACER;
                }
                subsequentElement = true;
                value += UIUtil.guiScaledFontHTML(UIUtil.uiC3Color());
                if (entity.hasC3S()) {
                    value += "C3 Slave" + CONNECTED_SIGN; 
                } else {
                    value += "C3 Master";
                    int freeS = entity.calculateFreeC3Nodes();
                    if (freeS == 0) {
                        value += " (full)";
                    } else {
                        value += Messages.getString("ChatLounge.C3SNodes", entity.calculateFreeC3Nodes());
                    }
                    value += CONNECTED_SIGN + "(CC) "; 
                }
                value += entity.getC3Master().getChassis();
                value += "</FONT>";
            }
        }

        // Loaded onto transport
        if (entity.getTransportId() != Entity.NONE) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            Entity loader = entity.getGame().getEntity(entity.getTransportId());
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + LOADED_SIGN;
            value += "<I> aboard " + loader.getChassis() + "</I></FONT>";
        }
        
        if (entity.isHidden() && mapType == MapSettings.MEDIUM_GROUND) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.compact.hidden") + "</I></FONT>";
        }
        
        if (entity.isHullDown()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.hulldown") + "</I></FONT>";
        }
        
        if (entity.isProne()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.prone") + "</I></FONT>";
        }

        if (entity.isOffBoard()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.deploysOffBoard") + "</I></FONT>"; 
        } else if (entity.getDeployRound() > 0) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.deploysAfterRound", entity.getDeployRound()); 
            if (entity.getStartingPos(false) != Board.START_NONE) {
                value += Messages.getString("ChatLounge.deploysAfterZone", 
                        IStartingPositions.START_LOCATION_NAMES[entity.getStartingPos(false)]);
            }
            value += "</I></FONT>";
        }
        
        // Starting values for Altitude / Velocity / Elevation
        if (entity.isAero()) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += UIUtil.guiScaledFontHTML(UIUtil.uiGreen()) + "<I>"; 
            Aero aero = (Aero) entity;
            value += Messages.getString("ChatLounge.compact.velocity") + ": ";
            value += aero.getCurrentVelocity();
            if (mapType != MapSettings.MEDIUM_SPACE) {
                value += ", " + Messages.getString("ChatLounge.compact.altitude") + ": ";
                value += aero.getAltitude();
            } 
            if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_FUEL_CONSUMPTION)) {
                value += ", " + Messages.getString("ChatLounge.compact.fuel") + ": ";
                value += aero.getCurrentFuel();
            }
            value += "</I></FONT>";
        } else if ((entity.getElevation() != 0) || (entity instanceof VTOL)) {
            if (subsequentElement) {
                value += DOT_SPACER;
            }
            subsequentElement = true;
            value += guiScaledFontHTML(uiGreen()) + "<I>";
            value += Messages.getString("ChatLounge.compact.elevation") + ": ";
            value += entity.getElevation() + "</I></FONT>";
        }
        
        return value;
    }

    /** 
     * Creates and returns the display content of the Pilot column for the given entity and
     * for the compact display mode.
     * When blindDrop is true, the pilot details are not given.
     */
    private static String formatPilotCompact(Crew pilot, boolean blindDrop, boolean rpgSkills) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        result.append(guiScaledFontHTML());
        
        if (blindDrop) {
            result.append(Messages.getString("ChatLounge.Unknown"));
            return result.toString();
        } 

        if (pilot.getSlotCount() > 1) {
            result.append("<I>Multiple Crewmembers</I>");
        } else if ((pilot.getNickname(0) != null) && !pilot.getNickname(0).isEmpty()) {
            result.append(guiScaledFontHTML(uiNickColor()) + "<B>'"); 
            result.append(pilot.getNickname(0).toUpperCase() + "'</B></FONT>");
            if (!pilot.getStatusDesc(0).isEmpty()) {
                result.append(" (" + pilot.getStatusDesc(0) + ")");
            }
        } else {
            result.append(pilot.getDesc(0));
        }

        result.append(" (" + pilot.getSkillsAsString(rpgSkills) + ")");
        int advs = pilot.countOptions();
        if (advs > 0) {
            result.append(DOT_SPACER + guiScaledFontHTML(uiQuirksColor()));
            String msg = "ChatLounge.compact." + (advs == 1 ? "advantage" : "advantages");
            result.append(pilot.countOptions() + Messages.getString(msg));
        }

        result.append("</FONT>");
        return result.toString();
    }
    
    /** 
     * Creates and returns the display content of the Pilot column for the given entity and
     * for the non-compact display mode.
     * When blindDrop is true, the pilot details are not given.
     */
    private static String formatPilotFull(Entity entity, boolean blindDrop) {
        StringBuilder result = new StringBuilder("<HTML><NOBR>");
        
        final Crew crew = entity.getCrew();
        final GameOptions options = entity.getGame().getOptions();
        final boolean rpgSkills = options.booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        final float overallScale = 0f;
        
        result.append(UIUtil.guiScaledFontHTML(overallScale));
        
        if (blindDrop) {
            result.append("<B>" + Messages.getString("ChatLounge.Unknown") + "</B>");
            return result.toString();
        } 
        
        if (crew.getSlotCount() == 1) { // Single-person crew
            if (crew.isMissing(0)) {
                result.append("<B>No " + crew.getCrewType().getRoleName(0) + "</B>");
            } else {
                if ((crew.getNickname(0) != null) && !crew.getNickname(0).isEmpty()) {
                    result.append(UIUtil.guiScaledFontHTML(UIUtil.uiNickColor(), overallScale)); 
                    result.append("<B>'" + crew.getNickname(0).toUpperCase() + "'</B></FONT>");
                } else {
                    result.append("<B>" + crew.getDesc(0) + "</B>");
                }
            }
            result.append("<BR>");
        } else { // Multi-person crew
            result.append("<I><B>Multiple Crewmembers</B></I>");
            result.append("<BR>");
        }
        result.append(CrewSkillSummaryUtil.getSkillNames(entity) + ": ");
        result.append("<B>" + crew.getSkillsAsString(rpgSkills) + "</B><BR>");
        
        // Advantages, MD, Edge
        if (crew.countOptions() > 0) {
            result.append(UIUtil.guiScaledFontHTML(UIUtil.uiQuirksColor(), overallScale));
            result.append(Messages.getString("ChatLounge.abilities"));
            result.append(" " + crew.countOptions());
            result.append("</FONT>");
        }
        result.append("</FONT>");
        return result.toString();

    }

} 

