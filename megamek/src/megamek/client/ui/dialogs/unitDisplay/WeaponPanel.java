/*
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.unitDisplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import megamek.client.Client;
import megamek.client.event.MekDisplayEvent;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.UnitToolTip;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay;
import megamek.client.ui.panels.phaseDisplay.TargetingPhaseDisplay;
import megamek.client.ui.widget.BackGroundDrawer;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.UnitDisplaySkinSpecification;
import megamek.client.ui.widget.picmap.PMUtil;
import megamek.client.ui.widget.picmap.PicMap;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.weapons.bayWeapons.BayWeapon;
import megamek.common.weapons.gaussRifles.HAGWeapon;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;

/**
 * This class contains the all the gizmos for firing the mek's weapons.
 */
public class WeaponPanel extends PicMap implements ListSelectionListener, ActionListener, IPreferenceChangeListener {
    private static final MMLogger logger = MMLogger.create(WeaponPanel.class);

    /**
     * Mouse adaptor for the weapon list. Supports rearranging the weapons to define a custom ordering.
     *
     * @author arlith
     */
    private class WeaponListMouseAdapter extends MouseInputAdapter {

        private boolean mouseDragging = false;
        private int dragSourceIndex;

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                Object src = e.getSource();
                if (src instanceof JList) {
                    dragSourceIndex = ((JList<?>) src).getSelectedIndex();
                    mouseDragging = true;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseDragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            removeListeners();

            try {
                Object src = e.getSource();
                // Check to see if we are in a state we care about
                if (!mouseDragging || !(src instanceof JList<?> srcList)) {
                    return;
                }
                WeaponListModel srcModel = (WeaponListModel) srcList.getModel();
                int currentIndex = srcList.locationToIndex(e.getPoint());
                if (currentIndex != dragSourceIndex) {
                    int dragTargetIndex = srcList.getSelectedIndex();
                    WeaponMounted weaponAt = srcModel.getWeaponAt(dragSourceIndex);

                    if (weaponAt == null) {
                        // Somehow we found no weapon there.
                        return;
                    }

                    srcModel.swapIdx(dragSourceIndex, dragTargetIndex);
                    dragSourceIndex = currentIndex;
                    Entity ent = weaponAt.getEntity();

                    // If this is a Custom Sort Order, update the weapon sort order drop down
                    if (!Objects.requireNonNull(comboWeaponSortOrder.getSelectedItem()).isCustom()) {
                        // Set the order to custom
                        ent.setWeaponSortOrder(WeaponSortOrder.CUSTOM);
                        comboWeaponSortOrder.setSelectedItem(WeaponSortOrder.CUSTOM);
                    }

                    // Update custom order
                    for (int i = 0; i < srcModel.getSize(); i++) {
                        WeaponMounted m = srcModel.getWeaponAt(i);
                        ent.setCustomWeaponOrder(m, i);
                    }
                }
            } catch (Exception ex) {
                logger.error("Unable to handle unexpected drag event: {}", e.toString());

            } finally {
                // Return listeners before returning!
                addListeners();
            }
        }
    }

    final UnitDisplayPanel unitDisplayPanel;
    private final Client client;

    private MMComboBox<WeaponSortOrder> comboWeaponSortOrder;
    public JList<String> weaponList;
    /**
     * Keep track of the previous target, used for certain weapons (like VGLs) that will force a target. With this, we
     * can restore the previous target after the forced target.
     */
    private Targetable prevTarget = null;
    private JScrollPane tWeaponScroll;
    private JComboBox<String> m_chAmmo;
    public JComboBox<String> m_chBayWeapon;

    private JLabel wBayWeapon;
    private JLabel wArcHeatL;
    private JLabel wMinL;
    private JLabel wShortL;
    private JLabel wMedL;
    private JLabel wLongL;
    private JLabel wExtL;
    private JLabel wAVL;
    private JLabel wNameR;
    private JLabel wHeatR;
    private JLabel wArcHeatR;
    private JLabel wDamR;
    private JLabel wMinR;
    private JLabel wShortR;
    private JLabel wMedR;
    private JLabel wLongR;
    private JLabel wExtR;
    private JLabel wShortAVR;
    private JLabel wMedAVR;
    private JLabel wLongAVR;
    private JLabel wExtAVR;
    private JLabel currentHeatBuildupR;
    public JLabel wTargetExtraInfo;
    public JLabel wRangeR;
    private JLabel wDamageTrooperL;
    private JLabel wDamageTrooperR;
    private JLabel wInfantryRange0L;
    private JLabel wInfantryRange0R;
    private JLabel wInfantryRange1L;
    private JLabel wInfantryRange1R;
    private JLabel wInfantryRange2L;
    private JLabel wInfantryRange2R;
    private JLabel wInfantryRange3L;
    private JLabel wInfantryRange3R;
    private JLabel wInfantryRange4L;
    private JLabel wInfantryRange4R;
    private JLabel wInfantryRange5L;
    private JLabel wInfantryRange5R;
    private JTextPane toHitText;
    private JTextPane wTargetInfo;
    private Targetable target;

    // I need to keep a pointer to the weapon list of the
    // currently selected mek.
    private ArrayList<AmmoMounted> vAmmo;
    private Entity entity;

    /**
     * Used to make sure that multiple removeListeners() calls (that have no cumulative effect) are not overbalanced by
     * multiple addListeners() calls. This would happen when one method that needs to use removeL [stuff ...] addL calls
     * another that needs to do the same.
     */
    private int listenerCounter = 0;

    Color[] bgColors = { Color.gray, Color.darkGray };
    int gridY;
    public static final int INTERNAL_PANE_WIDTH = 400;
    public static final int LINE_HEIGHT = 25;
    public static final Color COLOR_FG = Color.WHITE;
    public static final Color TEXT_BG = Color.DARK_GRAY;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    WeaponPanel(UnitDisplayPanel unitDisplayPanel, Client client) {
        this.unitDisplayPanel = unitDisplayPanel;
        this.client = client;

        JPanel panelTop = new JPanel();
        panelTop.setOpaque(false);
        panelTop.setLayout(new GridBagLayout());
        gridY = 0;
        panelTop.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelTop.setAlignmentY(Component.TOP_ALIGNMENT);
        panelTop.setPreferredSize(new Dimension(INTERNAL_PANE_WIDTH, 20));
        // having a max size set causes odd draw issues
        panelTop.setMaximumSize(null);
        createWeaponList(panelTop);
        createWeaponDisplay(panelTop);
        createRangeDisplay(panelTop);
        createToHitDisplay(panelTop);

        JPanel panelText = new JPanel();
        panelText.setOpaque(false);
        panelText.setLayout(new GridBagLayout());
        gridY = 0;
        panelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelText.setAlignmentY(Component.TOP_ALIGNMENT);
        panelText.setPreferredSize(new Dimension(INTERNAL_PANE_WIDTH, 20));
        panelText.setMaximumSize(null);
        createToHitText(panelText);

        JSplitPane splitPaneMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTop, panelText);
        splitPaneMain.setOpaque(false);

        JPanel panelMain = new JPanel();
        panelMain.setOpaque(false);
        panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.Y_AXIS));
        panelMain.add(splitPaneMain);

        JPanel panelLower = new JPanel();
        panelLower.setOpaque(false);
        panelLower.setLayout(new GridBagLayout());
        gridY = 0;
        panelLower.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelLower.setAlignmentY(Component.TOP_ALIGNMENT);
        panelLower.setPreferredSize(new Dimension(INTERNAL_PANE_WIDTH, 20));
        panelLower.setMaximumSize(null);

        createTargetDisplay(panelLower);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelMain, panelLower);
        splitPane.setOpaque(false);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(splitPane);

        addListeners();
        GUIP.addPreferenceChangeListener(this);

        setBackGround();
        onResize();
    }

    void setupLabel(JComponent label) {
        label.setOpaque(false);
        label.setForeground(COLOR_FG);
        label.setBackground(TEXT_BG);
    }

    void setupTextPane(JTextPane pane) {
        pane.setContentType("text/html");
        pane.setForeground(COLOR_FG);
        pane.setBackground(TEXT_BG);
        pane.setEditable(false);
        pane.setOpaque(true);
    }

    private void addSubDisplay(JPanel parent, JComponent child, int minHeight, int fill) {
        child.setMinimumSize(new Dimension(INTERNAL_PANE_WIDTH, minHeight));
        // null means allow UI to recompute
        child.setMaximumSize(new Dimension(INTERNAL_PANE_WIDTH, minHeight * 2));
        child.setPreferredSize(null);
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        child.setAlignmentY(Component.TOP_ALIGNMENT);
        child.setBackground(bgColors[(gridY++) % bgColors.length]);
        child.setOpaque(false);

        Dimension min = parent.getMinimumSize();
        min.height += minHeight;
        parent.setMinimumSize(min);

        Dimension pref = parent.getPreferredSize();
        pref.height += minHeight;
        parent.setPreferredSize(pref);

        parent.add(child, GBC.eol()
              .gridY(gridY++)
              .insets(10, 1, 10, 1)
              .weighty(1)
              .fill(fill));
    }

    private void createWeaponList(JPanel parent) {
        JLabel wSortOrder = new JLabel(
              Messages.getString("MekDisplay.WeaponSortOrder.label"),
              SwingConstants.LEFT);
        setupLabel(wSortOrder);

        JPanel pWeaponOrder = new JPanel(new GridBagLayout());
        pWeaponOrder.setOpaque(false);
        int parentGridY = 0;

        pWeaponOrder.add(wSortOrder,
              GBC.std().insets(15, 1, 1, 1).gridY(parentGridY).gridX(0));
        comboWeaponSortOrder = new MMComboBox<>("comboWeaponSortOrder", WeaponSortOrder.values());
        pWeaponOrder.add(comboWeaponSortOrder, GBC.eol()
              .fill(GridBagConstraints.HORIZONTAL)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 15, 1).gridY(parentGridY).gridX(1));
        addSubDisplay(parent, pWeaponOrder, LINE_HEIGHT, GridBagConstraints.BOTH);

        // weapon list
        weaponList = new JList<>(new DefaultListModel<>());
        WeaponListMouseAdapter mouseAdapter = new WeaponListMouseAdapter();
        weaponList.addMouseListener(mouseAdapter);
        weaponList.addMouseMotionListener(mouseAdapter);

        tWeaponScroll = new JScrollPane(weaponList);
        addSubDisplay(parent, tWeaponScroll, GUIP.getUnitDisplayWeaponListHeight(), GridBagConstraints.BOTH);

        weaponList.resetKeyboardActions();
        for (KeyListener key : weaponList.getKeyListeners()) {
            weaponList.removeKeyListener(key);
        }

        // adding Ammo choice + label
        JLabel wAmmo = new JLabel(Messages.getString("MekDisplay.Ammo"), SwingConstants.LEFT);
        setupLabel(wAmmo);
        m_chAmmo = new JComboBox<>();

        wBayWeapon = new JLabel(Messages.getString("MekDisplay.Weapon"), SwingConstants.LEFT);
        setupLabel(wBayWeapon);
        m_chBayWeapon = new JComboBox<>();

        JPanel pAmmo = new JPanel(new GridBagLayout());
        pAmmo.setOpaque(false);

        pAmmo.add(wBayWeapon, GBC.std().insets(15, 1, 1, 1).gridY(parentGridY).gridX(0));
        pAmmo.add(m_chBayWeapon, GBC.std().fill(GridBagConstraints.HORIZONTAL)
              .insets(15, 1, 15, 1).gridY(parentGridY).gridX(1));
        parentGridY++;

        pAmmo.add(wAmmo, GBC.std().insets(15, 9, 1, 1).gridY(parentGridY).gridX(0));

        pAmmo.add(m_chAmmo,
              GBC.eol().fill(GridBagConstraints.HORIZONTAL)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 9, 15, 1).gridY(parentGridY).gridX(1));

        addSubDisplay(parent, pAmmo, LINE_HEIGHT * 3 / 2, GridBagConstraints.BOTH);

    }

    private void createWeaponDisplay(JPanel parent) {
        // Adding weapon display labels
        JLabel wNameL = new JLabel(Messages.getString("MekDisplay.Name"), SwingConstants.CENTER);
        setupLabel(wNameL);
        JLabel wHeatL = new JLabel(Messages.getString("MekDisplay.Heat"), SwingConstants.CENTER);
        setupLabel(wHeatL);
        JLabel wDamL = new JLabel(Messages.getString("MekDisplay.Damage"), SwingConstants.CENTER);
        setupLabel(wDamL);
        wArcHeatL = new JLabel(Messages.getString("MekDisplay.ArcHeat"), SwingConstants.CENTER);
        setupLabel(wArcHeatL);

        wNameR = new JLabel("", SwingConstants.CENTER);
        setupLabel(wNameR);

        wHeatR = new JLabel("--", SwingConstants.CENTER);
        setupLabel(wHeatR);

        wDamR = new JLabel("--", SwingConstants.CENTER);
        setupLabel(wDamR);

        wArcHeatR = new JLabel("--", SwingConstants.CENTER);
        setupLabel(wArcHeatR);

        wDamageTrooperL = new JLabel(Messages.getString("MekDisplay.DamageTrooper"), SwingConstants.CENTER);
        setupLabel(wDamageTrooperL);

        wDamageTrooperR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wDamageTrooperR);

        JPanel pCurrentWeapon = new JPanel(new GridBagLayout());
        pCurrentWeapon.setOpaque(false);
        int parentGridY = 0;

        pCurrentWeapon.add(wNameL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(5, 9, 1, 1).gridY(parentGridY).gridX(0).weightX(1));

        pCurrentWeapon.add(wHeatL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 9, 1, 1).gridY(parentGridY).gridX(1).weightX(1));

        pCurrentWeapon.add(wDamL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 9, 1, 1).gridY(parentGridY).gridX(2).weightX(1));

        pCurrentWeapon.add(wArcHeatL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 9, 1, 1).gridY(parentGridY).gridX(3).weightX(1));

        pCurrentWeapon.add(wDamageTrooperL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 9, 1, 1).gridY(parentGridY).gridX(3).weightX(1));
        parentGridY++;
        pCurrentWeapon.add(wNameR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(5, 1, 1, 1).gridY(parentGridY).gridX(0).weightX(1));

        pCurrentWeapon.add(wHeatR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 1, 1, 1).gridY(parentGridY).gridX(1).weightX(1));

        pCurrentWeapon.add(wDamR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 1, 1, 1).gridY(parentGridY).gridX(2).weightX(1));

        pCurrentWeapon.add(wArcHeatR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 1, 1, 1).gridY(parentGridY).gridX(3).weightX(1));

        pCurrentWeapon.add(wDamageTrooperR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
              .insets(15, 1, 1, 1).gridY(parentGridY).gridX(3).weightX(1));

        addSubDisplay(parent, pCurrentWeapon, LINE_HEIGHT * 2, GridBagConstraints.NONE);
    }

    private void createRangeDisplay(JPanel parent) {
        // Adding range labels
        wMinL = new JLabel(Messages.getString("MekDisplay.Min"), SwingConstants.CENTER);
        setupLabel(wMinL);
        wShortL = new JLabel(Messages.getString("MekDisplay.Short"), SwingConstants.CENTER);
        setupLabel(wShortL);

        wMedL = new JLabel(Messages.getString("MekDisplay.Med"), SwingConstants.CENTER);
        setupLabel(wMedL);

        wLongL = new JLabel(Messages.getString("MekDisplay.Long"), SwingConstants.CENTER);
        setupLabel(wLongL);

        wExtL = new JLabel(Messages.getString("MekDisplay.Ext"), SwingConstants.CENTER);
        setupLabel(wExtL);

        wMinR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wMinR);

        wShortR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wShortR);

        wMedR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wMedR);

        wLongR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wLongR);

        wExtR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wExtR);

        wAVL = new JLabel(Messages.getString("MekDisplay.AV"), SwingConstants.CENTER);
        setupLabel(wAVL);

        wShortAVR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wShortAVR);

        wMedAVR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wMedAVR);

        wLongAVR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wLongAVR);

        wExtAVR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wExtAVR);

        wInfantryRange0L = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange0L);

        wInfantryRange0R = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange0R);

        wInfantryRange1L = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange1L);

        wInfantryRange1R = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange1R);

        wInfantryRange2L = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange2L);

        wInfantryRange2R = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange2R);

        wInfantryRange3L = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange3L);

        wInfantryRange3R = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange3R);

        wInfantryRange4L = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange4L);

        wInfantryRange4R = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange4R);

        wInfantryRange5L = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange5L);

        wInfantryRange5R = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wInfantryRange5R);

        // range panel
        JPanel pRange = new JPanel(new GridBagLayout());
        pRange.setAlignmentX(Component.LEFT_ALIGNMENT);
        pRange.setAlignmentY(Component.TOP_ALIGNMENT);
        pRange.setOpaque(false);
        int parentGridY = 0;

        pRange.add(wMinL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 9, 9, 1).gridY(parentGridY).gridX(0).weightX(1));

        pRange.add(wShortL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 9, 9, 1).gridY(parentGridY).gridX(1).weightX(1));

        pRange.add(wMedL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 9, 9, 1).gridY(parentGridY).gridX(2).weightX(1));

        pRange.add(wLongL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 9, 9, 1).gridY(parentGridY).gridX(3).weightX(1));

        pRange.add(wExtL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 9, 9, 1).gridY(parentGridY).gridX(4).weightX(1));

        parentGridY++;

        pRange.add(wInfantryRange0L, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 9, 1).gridY(parentGridY).gridX(0).weightX(1));

        pRange.add(wInfantryRange1L, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 9, 1).gridY(parentGridY).gridX(1).weightX(1));

        pRange.add(wInfantryRange2L, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 9, 1).gridY(parentGridY).gridX(2).weightX(1));

        pRange.add(wInfantryRange3L, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 9, 1).gridY(parentGridY).gridX(3).weightX(1));

        pRange.add(wInfantryRange4L, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 9, 1).gridY(parentGridY).gridX(4).weightX(1));

        pRange.add(wInfantryRange5L, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 9, 9, 1).gridY(parentGridY).gridX(4).weightX(1));

        parentGridY++;
        // ----------------

        pRange.add(wMinR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(0).weightX(1));

        pRange.add(wShortR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(1).weightX(1));

        pRange.add(wMedR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(2).weightX(1));

        pRange.add(wLongR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(3).weightX(1));

        pRange.add(wExtR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(4).weightX(1));

        pRange.add(wInfantryRange0R, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(0).weightX(1));

        pRange.add(wInfantryRange1R, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(1).weightX(1));

        pRange.add(wInfantryRange2R, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(2).weightX(1));

        pRange.add(wInfantryRange3R, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(3).weightX(1));

        pRange.add(wInfantryRange4R, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(4).weightX(1));

        pRange.add(wInfantryRange5R, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(5).weightX(1));

        parentGridY++;
        // ----------------
        pRange.add(wAVL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(0).weightX(1));

        pRange.add(wShortAVR, GBC.std().fill(GridBagConstraints.NONE)
              .anchor(GridBagConstraints.WEST)
              .insets(15, 1, 9, 1).gridY(parentGridY).gridX(1).weightX(1));

        pRange.add(wMedAVR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(2).weightX(1));

        pRange.add(wLongAVR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(3).weightX(1));

        pRange.add(wExtAVR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 9, 1).gridY(parentGridY).gridX(4).weightX(1));

        pRange.setMinimumSize(new Dimension(INTERNAL_PANE_WIDTH, LINE_HEIGHT));
        pRange.setMaximumSize(new Dimension(INTERNAL_PANE_WIDTH, LINE_HEIGHT));
        pRange.setPreferredSize(new Dimension(INTERNAL_PANE_WIDTH, LINE_HEIGHT));
        addSubDisplay(parent, pRange, LINE_HEIGHT * 2, GridBagConstraints.NONE);
    }

    private void createToHitDisplay(JPanel parent) {
        // to hit panel
        JPanel pTargetInfo = new JPanel(new GridBagLayout());
        pTargetInfo.setOpaque(true);

        JLabel wRangeL = new JLabel(Messages.getString("MekDisplay.Range"), SwingConstants.LEFT);
        setupLabel(wRangeL);

        wRangeR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wRangeR);

        JLabel currentHeatBuildupL = new JLabel(Messages.getString("MekDisplay.HeatBuildup"), SwingConstants.RIGHT);
        setupLabel(currentHeatBuildupL);

        currentHeatBuildupR = new JLabel("--", SwingConstants.LEFT);
        setupLabel(currentHeatBuildupR);

        wTargetExtraInfo = new JLabel();
        setupLabel(wTargetExtraInfo);

        int parentGridY = 0;
        wTargetExtraInfo.setMinimumSize(new Dimension(20, LINE_HEIGHT));
        pTargetInfo.add(wTargetExtraInfo,
              GBC.eol().fill(GridBagConstraints.BOTH)
                    .anchor(GridBagConstraints.WEST)
                    .insets(5, 1, 5, 1).gridY(parentGridY).gridX(0));
        parentGridY++;

        pTargetInfo.add(currentHeatBuildupL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(5, 1, 1, 1).gridY(parentGridY).gridX(0));

        pTargetInfo.add(currentHeatBuildupR,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(2, 1, 1, 1).gridY(parentGridY).gridX(1));

        pTargetInfo.add(wRangeL,
              GBC.std().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(15, 1, 1, 1).gridY(parentGridY).gridX(2));

        pTargetInfo.add(wRangeR,
              GBC.eol().fill(GridBagConstraints.NONE)
                    .anchor(GridBagConstraints.WEST)
                    .insets(5, 1, 5, 1).gridY(parentGridY).gridX(3));

        addSubDisplay(parent, pTargetInfo, LINE_HEIGHT * 2, GridBagConstraints.HORIZONTAL);
    }

    private void createToHitText(JPanel parent) {
        toHitText = new JTextPane();
        setupTextPane(toHitText);

        JScrollPane toHitScroll = new JScrollPane(toHitText);
        addSubDisplay(parent, toHitScroll, LINE_HEIGHT * 3, GridBagConstraints.BOTH);
    }

    private void createTargetDisplay(JPanel parent) {
        wTargetInfo = new JTextPane();
        setupTextPane(wTargetInfo);
        addSubDisplay(parent, wTargetInfo, LINE_HEIGHT * 2, GridBagConstraints.BOTH);
    }

    public void clearToHit() {
        toHitText.setText("---");
    }

    public void setToHit(ToHitData toHit) {
        setToHit(toHit, false);
    }

    public void setToHit(ToHitData toHit, boolean natAptGunnery) {
        String txt = switch (toHit.getValue()) {
            case TargetRoll.IMPOSSIBLE, TargetRoll.AUTOMATIC_FAIL -> String.format("To Hit: (0%%) %s", toHit.getDesc());
            case TargetRoll.AUTOMATIC_SUCCESS -> String.format("To Hit: (100%%) %s", toHit.getDesc());
            default -> String.format("<font color=\"%s\">To Hit: <b>%2d (%2.0f%%)</b></font> = %s",
                  GUIPreferences.hexColor(GUIP.getUnitToolTipHighlightColor()),
                  toHit.getValue(),
                  Compute.oddsAbove(toHit.getValue(), natAptGunnery),
                  toHit.getDesc());
        };

        toHitText.setText(UnitToolTip.wrapWithHTML(txt));
        toHitText.setCaretPosition(0);
    }

    public void setToHit(String message) {
        toHitText.setText(UnitToolTip.wrapWithHTML(message));
    }

    public void setTarget(@Nullable Targetable target, @Nullable String extraInfo) {
        this.target = target;
        updateTargetInfo();
        String txt = "";

        if (extraInfo == null || extraInfo.isEmpty()) {
            wTargetExtraInfo.setOpaque(false);
        } else {
            txt = extraInfo;
            wTargetExtraInfo.setOpaque(true);
        }

        wTargetExtraInfo.setText(UnitToolTip.wrapWithHTML(txt));
    }

    private void updateTargetInfo() {
        String txt;

        if (target == null) {
            txt = Messages.getString("MekDisplay.NoTarget");
        } else {
            txt = UnitToolTip.getTargetTipDetail(target, client);
        }

        wTargetInfo.setText(UnitToolTip.wrapWithHTML(txt));
    }

    @Override
    public void onResize() {
        int w = getSize().width;
        Rectangle r = getContentBounds();
        if (r == null) {
            return;
        }
        int dx = Math.round(((w - r.width) / 2.0f));
        int minLeftMargin = 8;
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = 8;
        setContentMargins(dx, dy, dx, dy);
        revalidate();
        repaint();
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_TOP;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.V_ALIGN_BOTTOM;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_LEFT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_TOP
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.V_ALIGN_BOTTOM
              | BackGroundDrawer.H_ALIGN_RIGHT;
        tile = getToolkit().getImage(
              new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));
    }

    /**
     * updates fields for the specified mek
     * <p>
     * fix the ammo when it's added
     */
    public void displayMek(Entity en) {
        removeListeners();

        // Grab a copy of the game.
        Game game = null;

        if (unitDisplayPanel.getClientGUI() != null) {
            game = unitDisplayPanel.getClientGUI().getClient().getGame();
        }

        // update pointer to weapons
        entity = en;

        // Check Game Options for max external heat
        int max_ext_heat = game != null ?
              game.getOptions().intOption(OptionsConstants.ADVANCED_COMBAT_MAX_EXTERNAL_HEAT)
              :
              15;
        if (max_ext_heat < 0) {
            max_ext_heat = 15; // Standard value specified in TW p.159
        }

        int currentHeatBuildup = (en.heat // heat from last round
              + en.getEngineCritHeat() // heat engine crits will add
              + Math.min(max_ext_heat, en.heatFromExternal) // heat from external sources
              + en.heatBuildup) // heat we're building up this round
              - Math.min(9, en.coolFromExternal); // cooling from external

        // sources
        if (en instanceof Mek) {
            if (en.infernos.isStillBurning()) { // hit with inferno ammo
                currentHeatBuildup += en.infernos.getHeat();
            }

            // extreme temperatures.
            if ((game != null) && (game.getPlanetaryConditions().getTemperature() > 0)) {
                int buildup = game.getPlanetaryConditions().getTemperatureDifference(50, -30);
                if (((Mek) en).hasIntactHeatDissipatingArmor()) {
                    buildup /= 2;
                }
                currentHeatBuildup += buildup;
            } else if (game != null) {
                currentHeatBuildup -= game.getPlanetaryConditions().getTemperatureDifference(50, -30);
            }
        }
        Coords position = entity.getPosition();
        if ((game != null) && !en.isOffBoard() && game.hasBoardLocation(position, entity.getBoardId())) {
            Hex hex = game.getBoard(entity.getBoardId()).getHex(position);
            if (hex != null) {
                if (hex.containsTerrain(Terrains.FIRE) && (hex.getFireTurn() > 0)) {
                    // standing in fire
                    if ((en instanceof Mek) && ((Mek) en).hasIntactHeatDissipatingArmor()) {
                        currentHeatBuildup += 2;
                    } else {
                        currentHeatBuildup += 5;
                    }
                }

                if (hex.terrainLevel(Terrains.MAGMA) == 1) {
                    if ((en instanceof Mek) && ((Mek) en).hasIntactHeatDissipatingArmor()) {
                        currentHeatBuildup += 2;
                    } else {
                        currentHeatBuildup += 5;
                    }
                } else if (hex.terrainLevel(Terrains.MAGMA) == 2) {
                    if ((en instanceof Mek) && ((Mek) en).hasIntactHeatDissipatingArmor()) {
                        currentHeatBuildup += 5;
                    } else {
                        currentHeatBuildup += 10;
                    }
                }
            } else {
                logger.warn("An entity is not offboard but has a position not on board.");
            }
        }

        if ((((en instanceof Mek) || (en instanceof Aero)) && en.isStealthActive())
              || en.isNullSigActive() || en.isVoidSigActive()) {
            currentHeatBuildup += 10; // active stealth/null sig/void sig heat
        }

        if ((en instanceof Mek) && en.isChameleonShieldOn()) {
            currentHeatBuildup += 6;
        }

        if (((en instanceof Mek) || (en instanceof Aero)) && en.hasActiveNovaCEWS()) {
            currentHeatBuildup += 2;
        }

        // update weapon list
        weaponList.setModel(new WeaponListModel(this, en));
        ((DefaultComboBoxModel<String>) m_chAmmo.getModel()).removeAllElements();

        m_chAmmo.setEnabled(false);
        m_chBayWeapon.removeAllItems();
        m_chBayWeapon.setEnabled(false);

        // on large craft we may need to take account of firing arcs
        boolean[] usedFrontArc = new boolean[entity.locations()];
        boolean[] usedRearArc = new boolean[entity.locations()];
        for (int i = 0; i < entity.locations(); i++) {
            usedFrontArc[i] = false;
            usedRearArc[i] = false;
        }

        boolean hasFiredWeapons = false;
        for (int i = 0; i < entity.getWeaponListWithHHW().size(); i++) {
            WeaponMounted mounted = entity.getWeaponListWithHHW().get(i);

            // Don't add bomb weapons for LAMs in mek mode except RL and TAG.
            if ((entity instanceof LandAirMek)
                  && (entity.getConversionMode() == LandAirMek.CONV_MODE_MEK)
                  && mounted.getType().hasFlag(WeaponType.F_BOMB_WEAPON)
                  && mounted.getType().getAmmoType() != AmmoType.AmmoTypeEnum.RL_BOMB
                  && !mounted.getType().hasFlag(WeaponType.F_TAG)) {
                continue;
            }

            ((WeaponListModel) weaponList.getModel()).addWeapon(mounted);
            if (mounted.isUsedThisRound() && game != null
                  && (game.getPhase() == mounted.usedInPhase())
                  && game.getPhase().isFiring()) {
                hasFiredWeapons = true;
                // add heat from weapons fire to heat tracker
                if (entity.isLargeCraft()) {
                    // if using bay heat option then don't add total arc
                    if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)) {
                        currentHeatBuildup += mounted.getHeatByBay();
                    } else {
                        // check whether arc has fired
                        int loc = mounted.getLocation();
                        boolean rearMount = mounted.isRearMounted();
                        if (!rearMount) {
                            if (!usedFrontArc[loc]) {
                                currentHeatBuildup += entity.getHeatInArc(loc, rearMount);
                                usedFrontArc[loc] = true;
                            }
                        } else {
                            if (!usedRearArc[loc]) {
                                currentHeatBuildup += entity.getHeatInArc(loc, rearMount);
                                usedRearArc[loc] = true;
                            }
                        }
                    }
                } else {
                    if (!mounted.isBombMounted() && entity.equals(mounted.getEntity())) {
                        currentHeatBuildup += mounted.getHeatByBay();
                    }
                }
            }
        }
        comboWeaponSortOrder.setSelectedItem(entity.getWeaponSortOrder());
        setWeaponComparator(comboWeaponSortOrder.getSelectedItem());

        if (en.hasDamagedRHS() && hasFiredWeapons) {
            currentHeatBuildup++;
        }

        String combatComputerIndicator = "";
        if (en.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
            currentHeatBuildup -= 4;
            combatComputerIndicator = " \uD83D\uDCBB";
        }

        // check for negative values due to extreme temp
        if (currentHeatBuildup < 0) {
            currentHeatBuildup = 0;
        }

        String heatText = Integer.toString(currentHeatBuildup);
        UnitToolTip.HeatDisplayHelper hdh = UnitToolTip.getHeatCapacityForDisplay(en);
        String heatCapacityStr = hdh.heatCapacityStr;
        int heatOverCapacity = currentHeatBuildup - hdh.heatCapWater;

        String sheatOverCapacity = "";
        if (heatOverCapacity > 0) {
            heatText += "*"; // overheat indication
            String msg_over = Messages.getString("MekDisplay.over");
            sheatOverCapacity = " " + heatOverCapacity + " " + msg_over;
        }

        String heatMessage = heatText + " (" + heatCapacityStr + ')' + sheatOverCapacity;
        String tempIndicator = "";

        if ((game != null) && (game.getPlanetaryConditions().isExtremeTemperature())) {
            tempIndicator = " " + game.getPlanetaryConditions().getTemperatureIndicator();
        }

        heatMessage += combatComputerIndicator + tempIndicator;

        currentHeatBuildupR.setForeground(GUIP.getColorForHeat(heatOverCapacity, Color.WHITE));
        currentHeatBuildupR.setText(heatMessage);

        // change what is visible based on type
        if (entity.usesWeaponBays()) {
            m_chBayWeapon.setVisible(true);
            wBayWeapon.setVisible(true);
        } else {
            m_chBayWeapon.setVisible(false);
            wBayWeapon.setVisible(false);
        }
        if ((!entity.isLargeCraft())
              || ((game != null) && (game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_HEAT_BY_BAY)))) {
            wArcHeatL.setVisible(false);
            wArcHeatR.setVisible(false);
        } else {
            wArcHeatL.setVisible(true);
            wArcHeatR.setVisible(true);
        }

        wDamageTrooperL.setVisible(false);
        wDamageTrooperR.setVisible(false);
        wInfantryRange0L.setVisible(false);
        wInfantryRange0R.setVisible(false);
        wInfantryRange1L.setVisible(false);
        wInfantryRange1R.setVisible(false);
        wInfantryRange2L.setVisible(false);
        wInfantryRange2R.setVisible(false);
        wInfantryRange3L.setVisible(false);
        wInfantryRange3R.setVisible(false);
        wInfantryRange4L.setVisible(false);
        wInfantryRange4R.setVisible(false);
        wInfantryRange5L.setVisible(false);
        wInfantryRange5R.setVisible(false);

        if (entity.isAero() && (entity.isAirborne() || entity.usesWeaponBays())) {
            wAVL.setVisible(true);
            wShortAVR.setVisible(true);
            wMedAVR.setVisible(true);
            wLongAVR.setVisible(true);
            wExtAVR.setVisible(true);
            wMinL.setVisible(false);
            wMinR.setVisible(false);
        } else {
            wAVL.setVisible(false);
            wShortAVR.setVisible(false);
            wMedAVR.setVisible(false);
            wLongAVR.setVisible(false);
            wExtAVR.setVisible(false);
            wMinL.setVisible(true);
            wMinR.setVisible(true);
        }

        // If MaxTech range rules are in play, display the extreme range.
        if (((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE))
              || (entity.isAero() && (entity.isAirborne() || entity.usesWeaponBays()))) {
            wExtL.setVisible(true);
            wExtR.setVisible(true);
        } else {
            wExtL.setVisible(false);
            wExtR.setVisible(false);
        }
        onResize();
        addListeners();
    }

    public int getSelectedEntityId() {
        return entity.getId();
    }

    /**
     * Selects the weapon with the specified weapon ID.
     */
    public void selectWeapon(int wn) {
        if (wn == -1) {
            weaponList.setSelectedIndex(-1);
            return;
        }
        int index = ((WeaponListModel) weaponList.getModel()).getIndex(wn);
        if (index == -1) {
            weaponList.setSelectedIndex(-1);
            return;
        }
        weaponList.setSelectedIndex(index);
        weaponList.ensureIndexIsVisible(index);
        displaySelected();
        weaponList.repaint();
    }

    public void selectWeapon(WeaponMounted weapon) {
        if (weapon == null) {
            weaponList.setSelectedIndex(-1);
            return;
        }
        int index = ((WeaponListModel) weaponList.getModel()).getIndex(weapon);
        if (index == -1) {
            weaponList.setSelectedIndex(-1);
            return;
        }
        weaponList.setSelectedIndex(index);
        weaponList.ensureIndexIsVisible(index);
        displaySelected();
        weaponList.repaint();
    }

    /**
     * @return the Mounted for the selected weapon in the weapon list.
     */
    public WeaponMounted getSelectedWeapon() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return null;
        }
        return ((WeaponListModel) weaponList.getModel()).getWeaponAt(selected);
    }

    /**
     * @return the AmmoMounted currently selected by the ammo selector combo box, if any. The returned AmmoMounted may
     *       or may not be the ammo that is linked to the weapon.
     */
    public Optional<AmmoMounted> getSelectedAmmo() {
        int selected = m_chAmmo.getSelectedIndex();
        if ((selected == -1) || (vAmmo == null) || (selected >= vAmmo.size())) {
            return Optional.empty();
        }

        return Optional.of(vAmmo.get(selected));
    }

    /**
     * Returns the equipment ID number for the weapon currently selected
     */
    public int getSelectedWeaponNum() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return -1;
        }
        Entity weaponEntity = getSelectedWeapon().getEntity();
        return weaponEntity.getEquipmentNum(((WeaponListModel) weaponList.getModel()).getWeaponAt(selected));
    }

    /**
     * Selects the first valid weapon in the weapon list.
     */
    public void selectFirstWeapon() {
        // Entity has no weapons, return -1;
        if (entity.getWeaponListWithHHW().isEmpty()
              || (entity.usesWeaponBays() && entity.getWeaponBayList().isEmpty())) {
            return;
        }
        WeaponListModel weaponList = (WeaponListModel) this.weaponList.getModel();
        for (int i = 0; i < this.weaponList.getModel().getSize(); i++) {
            WeaponMounted selectedWeapon = weaponList.getWeaponAt(i);
            if (entity.isWeaponValidForPhase(selectedWeapon)) {
                this.weaponList.setSelectedIndex(i);
                this.weaponList.ensureIndexIsVisible(i);
                entity.getEquipmentNum(selectedWeapon);
                return;
            }
        }
        // Found no valid weapon
        this.weaponList.setSelectedIndex(-1);
    }

    public int getNextWeaponListIdx() {
        if (weaponList.getModel().getSize() == 0) {
            return -1;
        }

        int selected = weaponList.getSelectedIndex();
        // In case nothing was selected
        if (selected == -1) {
            selected = weaponList.getModel().getSize() - 1;
        }
        WeaponMounted selectedWeapon;
        int initialSelection = selected;

        boolean hasLooped = false;
        do {
            selected++;
            if (selected >= weaponList.getModel().getSize()) {
                selected = 0;
            }
            if (selected == initialSelection) {
                hasLooped = true;
            }
            selectedWeapon = ((WeaponListModel) weaponList.getModel())
                  .getWeaponAt(selected);
        } while (!hasLooped && !entity.isWeaponValidForPhase(selectedWeapon));

        if ((selected >= 0) && (selected < entity.getWeaponListWithHHW().size())
              && !hasLooped) {
            return selected;
        } else {
            return -1;
        }
    }

    public int getPrevWeaponListIdx() {
        if (weaponList.getModel().getSize() == 0) {
            return -1;
        }

        int selected = weaponList.getSelectedIndex();
        // In case nothing was selected
        if (selected == -1) {
            selected = 0;
        }
        WeaponMounted selectedWeapon;
        int initialSelection = selected;
        boolean hasLooped = false;
        do {
            selected--;
            if (selected < 0) {
                selected = weaponList.getModel().getSize() - 1;
            }
            if (selected == initialSelection) {
                hasLooped = true;
            }
            selectedWeapon = ((WeaponListModel) weaponList.getModel())
                  .getWeaponAt(selected);
        } while (!hasLooped && !entity.isWeaponValidForPhase(selectedWeapon));

        if ((selected >= 0) && (selected < entity.getWeaponListWithHHW().size())
              && !hasLooped) {
            return selected;
        } else {
            return -1;
        }
    }

    public int getNextWeaponNum() {
        int selected = getNextWeaponListIdx();
        if ((selected >= 0) && (selected < entity.getWeaponListWithHHW().size())) {
            WeaponMounted weapon = ((WeaponListModel) weaponList
                  .getModel()).getWeaponAt(selected);
            Entity weaponEntity = weapon.getEntity();
            return weaponEntity.getEquipmentNum(weapon);
        } else {
            return -1;
        }
    }

    public WeaponMounted getNextWeapon() {
        int selected = getNextWeaponListIdx();
        if ((selected >= 0) && (selected < entity.getWeaponListWithHHW().size())) {
            return ((WeaponListModel) weaponList
                  .getModel()).getWeaponAt(selected);
        } else {
            return null;
        }
    }

    /**
     * Selects the next valid weapon in the weapon list.
     *
     * @return The weaponId for the selected weapon
     */
    public int selectNextWeapon() {
        int selected = getNextWeaponListIdx();
        weaponList.setSelectedIndex(selected);
        weaponList.ensureIndexIsVisible(selected);
        if ((selected >= 0) && (selected < entity.getWeaponListWithHHW().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                  .getModel()).getWeaponAt(selected));
        } else {
            return -1;
        }
    }

    /**
     * Selects the previous valid weapon in the weapon list.
     *
     * @return The weaponId for the selected weapon
     */
    public int selectPrevWeapon() {
        int selected = getPrevWeaponListIdx();
        weaponList.setSelectedIndex(selected);
        weaponList.ensureIndexIsVisible(selected);
        if ((selected >= 0) && (selected < entity.getWeaponListWithHHW().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                  .getModel()).getWeaponAt(selected));
        } else {
            return -1;
        }
    }

    /**
     * displays the selected item from the list in the weapon display panel.
     */
    private void displaySelected() {
        removeListeners();
        try {
            // short circuit if not selected
            if (weaponList.getSelectedIndex() == -1) {
            ((DefaultComboBoxModel<String>) m_chAmmo.getModel())
                  .removeAllElements();
            m_chAmmo.setEnabled(false);
            m_chBayWeapon.removeAllItems();
            m_chBayWeapon.setEnabled(false);
            wNameR.setText("");
            wHeatR.setText("--");
            wArcHeatR.setText("---");
            wDamR.setText("--");
            wMinR.setText("---");
            wShortR.setText("---");
            wMedR.setText("---");
            wLongR.setText("---");
            wExtR.setText("---");

            wDamageTrooperL.setVisible(false);
            wDamageTrooperR.setVisible(false);
            wInfantryRange0L.setVisible(false);
            wInfantryRange0R.setVisible(false);
            wInfantryRange1L.setVisible(false);
            wInfantryRange1R.setVisible(false);
            wInfantryRange2L.setVisible(false);
            wInfantryRange2R.setVisible(false);
            wInfantryRange3L.setVisible(false);
            wInfantryRange3R.setVisible(false);
            wInfantryRange4L.setVisible(false);
            wInfantryRange4R.setVisible(false);
            wInfantryRange5L.setVisible(false);
            wInfantryRange5R.setVisible(false);

            return;
        }

        WeaponMounted mounted = ((WeaponListModel) weaponList.getModel())
              .getWeaponAt(weaponList.getSelectedIndex());
        WeaponType weaponType = mounted.getType();
        // The rules are a bit sparse on airborne (dropping) ground units, but it seems they should still attack like
        // ground units.
        boolean aerospaceAttack = entity.isAero() && (entity.isAirborne() || entity.usesWeaponBays());
        // update weapon display
        wNameR.setText(mounted.getDesc());
        wHeatR.setText(Integer.toString(mounted.getCurrentHeat()));

        wArcHeatR.setText(Integer.toString(entity.getHeatInArc(
              mounted.getLocation(), mounted.isRearMounted())));

        if ((weaponType instanceof InfantryWeapon infantryType) && !weaponType.hasFlag(WeaponType.F_TAG)) {
            wDamageTrooperL.setVisible(true);
            wDamageTrooperR.setVisible(true);
            if (entity.isConventionalInfantry()) {
                wDamageTrooperR.setText(Double.toString((double) Math.round(
                      ((Infantry) entity).getDamagePerTrooper() * 1000) / 1000));
            } else {
                wDamageTrooperR.setText(Double.toString(infantryType.getInfantryDamage()));
            }
            // what a nightmare to set up all the range info for infantry weapons
            wMinL.setVisible(false);
            wShortL.setVisible(false);
            wMedL.setVisible(false);
            wLongL.setVisible(false);
            wExtL.setVisible(false);
            wMinR.setVisible(false);
            wShortR.setVisible(false);
            wMedR.setVisible(false);
            wLongR.setVisible(false);
            wExtR.setVisible(false);
            wInfantryRange0L.setVisible(false);
            wInfantryRange0R.setVisible(false);
            wInfantryRange1L.setVisible(false);
            wInfantryRange1R.setVisible(false);
            wInfantryRange2L.setVisible(false);
            wInfantryRange2R.setVisible(false);
            wInfantryRange3L.setVisible(false);
            wInfantryRange3R.setVisible(false);
            wInfantryRange4L.setVisible(false);
            wInfantryRange4R.setVisible(false);
            wInfantryRange5L.setVisible(false);
            wInfantryRange5R.setVisible(false);
            int zeroMods = 0;
            if (infantryType.hasFlag(WeaponType.F_INF_POINT_BLANK)) {
                zeroMods++;
            }

            if (infantryType.hasFlag(WeaponType.F_INF_ENCUMBER)
                  || (infantryType.getCrew() > 1)) {
                zeroMods++;
            }

            if (infantryType.hasFlag(WeaponType.F_INF_BURST)) {
                zeroMods--;
            }

            int range = infantryType.getInfantryRange();
            if (entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET) {
                range /= 2;
            }
            switch (range) {
                case 0:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R.setText("+" + zeroMods);
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    break;
                case 1:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("2");
                    wInfantryRange2R.setText("+2");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("3");
                    wInfantryRange3R.setText("+4");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    break;
                case 2:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-2");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("3-4");
                    wInfantryRange2R.setText("+2");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("5-6");
                    wInfantryRange3R.setText("+4");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    break;
                case 3:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-3");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("4-6");
                    wInfantryRange2R.setText("+2");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("7-9");
                    wInfantryRange3R.setText("+4");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    break;
                case 4:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 2));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-4");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("5-6");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("7-8");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("9-10");
                    wInfantryRange4R.setText("+3");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("11-12");
                    wInfantryRange5R.setText("+4");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
                case 5:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 1));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-5");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("6-7");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("8-10");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("11-12");
                    wInfantryRange4R.setText("+3");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("13-15");
                    wInfantryRange5R.setText("+4");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
                case 6:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 1));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-6");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("7-9");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("10-12");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("13-15");
                    wInfantryRange4R.setText("+4");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("16-18");
                    wInfantryRange5R.setText("+5");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
                case 7:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                          .setText(Integer.toString(zeroMods - 1));
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    wInfantryRange1L.setText("1-7");
                    wInfantryRange1R.setText("+0");
                    wInfantryRange1L.setVisible(true);
                    wInfantryRange1R.setVisible(true);
                    wInfantryRange2L.setText("8-10");
                    wInfantryRange2R.setText("+1");
                    wInfantryRange2L.setVisible(true);
                    wInfantryRange2R.setVisible(true);
                    wInfantryRange3L.setText("11-14");
                    wInfantryRange3R.setText("+2");
                    wInfantryRange3L.setVisible(true);
                    wInfantryRange3R.setVisible(true);
                    wInfantryRange4L.setText("15-17");
                    wInfantryRange4R.setText("+4");
                    wInfantryRange4L.setVisible(true);
                    wInfantryRange4R.setVisible(true);
                    wInfantryRange5L.setText("18-21");
                    wInfantryRange5R.setText("+6");
                    wInfantryRange5L.setVisible(true);
                    wInfantryRange5R.setVisible(true);
                    break;
            }
        } else {
            wDamageTrooperL.setVisible(false);
            wDamageTrooperR.setVisible(false);
            wInfantryRange0L.setVisible(false);
            wInfantryRange0R.setVisible(false);
            wInfantryRange1L.setVisible(false);
            wInfantryRange1R.setVisible(false);
            wInfantryRange2L.setVisible(false);
            wInfantryRange2R.setVisible(false);
            wInfantryRange3L.setVisible(false);
            wInfantryRange3R.setVisible(false);
            wInfantryRange4L.setVisible(false);
            wInfantryRange4R.setVisible(false);
            wInfantryRange5L.setVisible(false);
            wInfantryRange5R.setVisible(false);
            wShortL.setVisible(true);
            wMedL.setVisible(true);
            wLongL.setVisible(true);

            wMinR.setVisible(true);
            wShortR.setVisible(true);
            wMedR.setVisible(true);
            wLongR.setVisible(true);

            if (!aerospaceAttack) {
                wMinL.setVisible(true);
                wMinR.setVisible(true);
            }
            if (((entity.getGame() != null)
                  && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE))
                  || aerospaceAttack) {
                wExtL.setVisible(true);
                wExtR.setVisible(true);
            }
        }

        if (weaponType.getDamage() == WeaponType.DAMAGE_BY_CLUSTER_TABLE) {
            if (weaponType instanceof HAGWeapon) {
                wDamR.setText(Messages.getString("MekDisplay.Variable"));
            } else {
                wDamR.setText(Messages.getString("MekDisplay.Missile"));
            }
        } else if (weaponType.getDamage() == WeaponType.DAMAGE_VARIABLE) {
            wDamR.setText(Messages.getString("MekDisplay.Variable"));
        } else if (weaponType.getDamage() == WeaponType.DAMAGE_SPECIAL) {
            wDamR.setText(Messages.getString("MekDisplay.Special"));
        } else if (weaponType.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            StringBuilder damage = new StringBuilder();
            int artyDamage = weaponType.getRackSize();
            int falloff = 10;
            boolean fuelAirExplosive = false;
            boolean specialArrowIV = false;
            if ((mounted.getLinked() != null) && (mounted.getLinked().getType() instanceof AmmoType ammoType)) {
                fuelAirExplosive = ammoType.getMunitionType().contains(Munitions.M_FAE);
                specialArrowIV = (ammoType.is(AmmoTypeEnum.ARROW_IV)
                      && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)
                      || ammoType.getMunitionType().contains(AmmoType.Munitions.M_HOMING)));
                int attackingBA = (entity instanceof BattleArmor) ? ((BattleArmor) entity).getShootingStrength() : -1;
                DamageFalloff damageFalloff = AreaEffectHelper.calculateDamageFallOff(ammoType, attackingBA, false);
                artyDamage = damageFalloff.damage;
                falloff = damageFalloff.falloff;
            }
            damage.append(artyDamage);
            if (!specialArrowIV) {
                artyDamage -= falloff;
                while ((artyDamage > 0) && (falloff > 0)) {
                    damage.append('/').append(artyDamage);
                    artyDamage -= falloff;
                }
                if (fuelAirExplosive) {
                    damage.append("/5");
                }
            }
            wDamR.setText(damage.toString());
        } else if (weaponType.hasFlag(WeaponType.F_ENERGY)
              && mounted.hasModes()
              && (unitDisplayPanel.getClientGUI() != null)
              && unitDisplayPanel.getClientGUI().getClient().getGame().getOptions().booleanOption(
              OptionsConstants.ADVANCED_COMBAT_TAC_OPS_ENERGY_WEAPONS)) {
            if (mounted.hasChargedCapacitor() != 0) {
                if (mounted.hasChargedCapacitor() == 1) {
                    wDamR.setText(Integer.toString(Compute.dialDownDamage(
                          mounted, weaponType) + 5));
                }
                if (mounted.hasChargedCapacitor() == 2) {
                    wDamR.setText(Integer.toString(Compute.dialDownDamage(
                          mounted, weaponType) + 10));
                }
            } else {
                wDamR.setText(Integer.toString(Compute.dialDownDamage(
                      mounted, weaponType)));
            }
        } else if (mounted.curMode().getName().contains("Dazzle")) {
            // Gothic Dazzle Mode: half damage (rounded down, min 1)
            int baseDamage = weaponType.getDamage();
            int dazzleDamage = Math.max(1, baseDamage / 2);
            wDamR.setText(Integer.toString(dazzleDamage));
        } else {
            wDamR.setText(Integer.toString(weaponType.getDamage()));
        }

        // update range
        int shortR = weaponType.getShortRange();
        int mediumR = weaponType.getMediumRange();
        int longR = weaponType.getLongRange();
        int extremeR = weaponType.getExtremeRange();
        if (mounted.isInBearingsOnlyMode()) {
            extremeR = RangeType.RANGE_BEARINGS_ONLY_OUT;
        }
        // Show water ranges for submerged weapons and those that have only water ranges
        // (torpedoes)
        if ((entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET)
              || ((longR == 0) && weaponType.getWLongRange() > 0)) {
            shortR = weaponType.getWShortRange();
            mediumR = weaponType.getWMediumRange();
            longR = weaponType.getWLongRange();
            extremeR = weaponType.getWExtremeRange();
        } else if (weaponType.hasFlag(WeaponType.F_PD_BAY)) {
            // Point Defense bays have a variable range, depending on the mode they're in
            if (mounted.hasModes() && mounted.curMode().equals("Point Defense")) {
                shortR = 1;
                wShortR.setText("1");
            } else {
                shortR = 6;
                wShortR.setText("1-6");
            }
        }
        // We need to adjust the ranges for Centurion Weapon Systems: it's
        // default range is 6/12/18 but that's only for units that are
        // susceptible to CWS, for those that aren't the ranges are 1/2/3
        if (weaponType.hasFlag(WeaponType.F_CWS)) {
            Entity target = null;
            if ((unitDisplayPanel.getClientGUI() != null)
                  && (unitDisplayPanel.getClientGUI().getCurrentPanel() instanceof FiringDisplay)) {
                Targetable t = ((FiringDisplay) unitDisplayPanel.getClientGUI().getCurrentPanel()).getTarget();
                if (t instanceof Entity) {
                    target = (Entity) t;
                }
            }
            if ((target == null) || !target.hasQuirk("susceptible_cws")) {
                shortR = 1;
                mediumR = 2;
                longR = 3;
                extremeR = 4;
            }
        }
        if (weaponType.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(weaponType.getMinimumRange()));
        } else {
            wMinR.setText("---");
        }
        if (shortR > 1) {
            wShortR.setText("1 - " + shortR);
        } else {
            wShortR.setText("" + shortR);
        }
        if ((mediumR - shortR) > 1) {
            wMedR.setText(shortR + 1 + " - " + mediumR);
        } else {
            wMedR.setText("" + mediumR);
        }
        if ((longR - mediumR) > 1) {
            wLongR.setText(mediumR + 1 + " - " + longR);
        } else {
            wLongR.setText("" + longR);
        }
        if ((extremeR - longR) > 1) {
            wExtR.setText(longR + 1 + " - " + extremeR);
        } else {
            wExtR.setText("" + extremeR);
        }

        // Update the range display to account for the selected ammo, or the loaded ammo
        // if none is selected
        AmmoMounted mAmmo = getSelectedAmmo().orElse(mounted.getLinkedAmmo());
        if (mAmmo != null) {
            updateRangeDisplayForAmmo(mAmmo);
        }

        if (aerospaceAttack) {
            // change damage report to a statement of standard or capital
            if (weaponType.isCapital()) {
                wDamR.setText(Messages.getString("MekDisplay.CapitalD"));
            } else {
                wDamR.setText(Messages.getString("MekDisplay.StandardD"));
            }

            // if this is a weapons bay, then I need to compile it to get
            // accurate results
            if (weaponType instanceof BayWeapon) {
                compileWeaponBay(mounted, mAmmo, weaponType.isCapital());
            } else {
                // otherwise I need to replace range display with standard
                // ranges and attack values
                updateAttackValues(mounted, mAmmo);
            }

        }

        // update weapon bay selector
        int chosen = m_chBayWeapon.getSelectedIndex();
        m_chBayWeapon.removeAllItems();
        if (!(weaponType instanceof BayWeapon) || !entity.usesWeaponBays()) {
            m_chBayWeapon.setEnabled(false);
        } else {
            m_chBayWeapon.setEnabled(true);
            for (WeaponMounted curWeapon : mounted.getBayWeapons()) {
                m_chBayWeapon.addItem(formatBayWeapon(curWeapon));
            }

            if (chosen == -1 || chosen >= m_chBayWeapon.getItemCount()) {
                m_chBayWeapon.setSelectedIndex(0);
            } else {
                m_chBayWeapon.setSelectedIndex(chosen);
            }
        }

        // update ammo selector; reset to currently-displayed item if set.
        int currentAmmoSelectionIndex = m_chAmmo.getSelectedIndex();
        ((DefaultComboBoxModel<String>) m_chAmmo.getModel()).removeAllElements();
        WeaponMounted oldMount = mounted;
        if (weaponType instanceof BayWeapon) {
            int n = m_chBayWeapon.getSelectedIndex();
            if (n == -1) {
                n = 0;
            }
            mounted = mounted.getBayWeapon(n);
            weaponType = mounted.getType();
        }

        if (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.NA) {
            m_chAmmo.setEnabled(false);
        } else if (weaponType.hasFlag(WeaponType.F_DOUBLE_ONE_SHOT)
              || (entity.isSupportVehicle() && (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.INFANTRY))) {
            int count = 0;
            vAmmo = new ArrayList<>();
            for (AmmoMounted current = mounted.getLinkedAmmo(); current != null; current = (AmmoMounted) current
                  .getLinked()) {
                if (current.getUsableShotsLeft() > 0) {
                    vAmmo.add(current);
                    m_chAmmo.addItem(formatAmmo(current));
                    count++;
                }
            }
            // If there is no remaining ammo, show the last one linked and disable
            if (count == 0) {
                m_chAmmo.addItem(formatAmmo(mounted.getLinked()));
            }
            m_chAmmo.setSelectedIndex(0);
            m_chAmmo.setEnabled(count > 0);

        } else if (weaponType.hasFlag(WeaponType.F_ONE_SHOT)) {
            // this is the situation where there's some kind of ammo, but it's not changeable
            m_chAmmo.setEnabled(false);
            Mounted<?> mountedAmmo = mounted.getLinked();
            if (mountedAmmo != null) {
                m_chAmmo.addItem(formatAmmo(mountedAmmo));
            }

        } else {

            vAmmo = new ArrayList<>();
            // Ammo sharing between adjacent trailers
            List<AmmoMounted> fullAmmoList = new ArrayList<>(entity.getAmmo());
            if (entity.getTowedBy() != Entity.NONE) {
                Entity ahead = entity.getGame().getEntity(entity.getTowedBy());
                if (ahead != null) {
                    fullAmmoList.addAll(ahead.getAmmo());
                }
            }
            if (entity.getTowing() != Entity.NONE) {
                Entity behind = entity.getGame().getEntity(entity.getTowing());
                if (behind != null) {
                    fullAmmoList.addAll(behind.getAmmo());
                }
            }
            int newSelectedIndex = -1;
            int i = 0;
            for (AmmoMounted ammo : fullAmmoList) {
                AmmoType ammoType = ammo.getType();
                // for all aero units other than fighters, ammo must be located in the same place to be usable
                boolean sameLocationIfLargeAero = true;
                if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
                    sameLocationIfLargeAero = (mounted.getLocation() == ammo.getLocation());
                }
                boolean rightBay = true;
                if (entity.usesWeaponBays() && !(entity instanceof FighterSquadron)) {
                    rightBay = oldMount.ammoInBay(entity.getEquipmentNum(ammo));
                }

                // covers the situation where a weapon using non-caseless ammo should
                // not be able to switch to caseless on the fly and vice versa
                boolean canSwitchToAmmo = AmmoType.canSwitchToAmmo(mounted, ammoType);

                if (ammo.isAmmoUsable() && sameLocationIfLargeAero && rightBay && canSwitchToAmmo
                      && (ammoType.getAmmoType() == weaponType.getAmmoType())
                      && (ammoType.getRackSize() == weaponType.getRackSize())) {

                    vAmmo.add(ammo);


                    if ((mounted.getLinked() != null) && mounted.getLinked().equals(ammo)) {
                        newSelectedIndex = i;
                        // Prevent later overriding.
                        currentAmmoSelectionIndex = -1;
                    } else if (currentAmmoSelectionIndex != -1) {
                        // This should be the fallback
                        newSelectedIndex = currentAmmoSelectionIndex;
                    }
                    i++;
                }
            }
            m_chAmmo.setEnabled(true);
            for (var ammo : vAmmo) {
                m_chAmmo.addItem(formatAmmo(ammo));
            }

            if ((newSelectedIndex != -1) && (newSelectedIndex < m_chAmmo.getItemCount())) {
                m_chAmmo.setSelectedIndex(newSelectedIndex);
            }
        }

        // send event to other parts of the UI which care
        unitDisplayPanel.getClientGUI().showSensorRanges(entity);
        unitDisplayPanel.processMekDisplayEvent(new MekDisplayEvent(this, entity, mounted));
        onResize();
        } finally {
            addListeners();
        }
    }

    private String formatAmmo(Mounted<?> m) {
        StringBuilder sb = new StringBuilder(64);
        int ammoIndex = m.getDesc().indexOf(Messages.getString("MekDisplay.0"));
        int loc = m.getLocation();
        if (!m.getEntity().equals(entity) && !(m.getEntity() instanceof HandheldWeapon)) {
            sb.append("[TR] ");
        } else if (loc != Entity.LOC_NONE) {
            sb.append('[').append(entity.getLocationAbbr(loc)).append("] ");
        }
        if (ammoIndex == -1) {
            sb.append(m.getDesc());
        } else {
            sb.append(m.getDesc(), 0, ammoIndex);
            sb.append(m.getDesc().substring(ammoIndex + 4));
        }
        if (m.isHotLoaded()) {
            sb.append(Messages.getString("MekDisplay.isHotLoaded"));
        }
        return sb.toString();
    }

    private String formatBayWeapon(WeaponMounted m) {
        return m.getDesc();
    }

    /**
     * Update the range display for the selected ammo.
     *
     * @param mAmmo - the <code>AmmoType</code> of the weapon's loaded ammo.
     */
    private void updateRangeDisplayForAmmo(AmmoMounted mAmmo) {
        AmmoType ammoType = mAmmo.getType();
        // Only override the display for the various ATM and MML ammunition
        if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ATM) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                wMinR.setText("4");
                wShortR.setText("1 - 9");
                wMedR.setText("10 - 18");
                wLongR.setText("19 - 27");
                wExtR.setText("28 - 36");
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
                wMinR.setText("---");
                wShortR.setText("1 - 3");
                wMedR.setText("4 - 6");
                wLongR.setText("7 - 9");
                wExtR.setText("10 - 12");
            } else {
                wMinR.setText("4");
                wShortR.setText("1 - 5");
                wMedR.setText("6 - 10");
                wLongR.setText("11 - 15");
                wExtR.setText("16 - 20");
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML) {
            if (ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                    wMinR.setText("4");
                    wShortR.setText("1 - 5");
                    wMedR.setText("6 - 10");
                    wLongR.setText("11 - 15");
                    wExtR.setText("16 - 20");
                } else {
                    wMinR.setText("6");
                    wShortR.setText("1 - 7");
                    wMedR.setText("8 - 14");
                    wLongR.setText("15 - 21");
                    wExtR.setText("21 - 28");
                }
            } else {
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                    wMinR.setText("---");
                    wShortR.setText("1 - 2");
                    wMedR.setText("3 - 4");
                    wLongR.setText("5 - 6");
                    wExtR.setText("7 - 8");
                } else {
                    wMinR.setText("---");
                    wShortR.setText("1 - 3");
                    wMedR.setText("4 - 6");
                    wLongR.setText("7 - 9");
                    wExtR.setText("10 - 12");
                }
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.IATM) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                wMinR.setText("4");
                wShortR.setText("1 - 9");
                wMedR.setText("10 - 18");
                wLongR.setText("19 - 27");
                wExtR.setText("28 - 36");
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
                wMinR.setText("---");
                wShortR.setText("1 - 3");
                wMedR.setText("4 - 6");
                wLongR.setText("7 - 9");
                wExtR.setText("10 - 12");
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW)) {
                wMinR.setText("4");
                wShortR.setText("1 - 5");
                wMedR.setText("6 - 10");
                wLongR.setText("11 - 15");
                wExtR.setText("16 - 20");
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_IATM_IMP)) {
                wMinR.setText("---");
                wShortR.setText("1 - 3");
                wMedR.setText("4 - 6");
                wLongR.setText("7 - 9");
                wExtR.setText("10 - 12");
            } else /* standard */ {
                wMinR.setText("4");
                wShortR.setText("1 - 5");
                wMedR.setText("6 - 10");
                wLongR.setText("11 - 15");
                wExtR.setText("16 - 20");
            }
        } else if ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
              && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE))) {
            wMinR.setText("4");
            wShortR.setText("1 - 5");
            wMedR.setText("6 - 10");
            wLongR.setText("11 - 15");
            wExtR.setText("16 - 20");
        } else if ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM)
              && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE))) {
            wMinR.setText("---");
            wShortR.setText("1 - 2");
            wMedR.setText("3 - 4");
            wLongR.setText("5 - 6");
            wExtR.setText("7 - 8");
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.ARROW_IV) {
            // Special casing for ADA ranges
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                wMinR.setText("---");
                wShortR.setText("1 - 17 [0]");
                wMedR.setText("18 - 34 [1]");
                wLongR.setText("35 - 51 [2]");
                wExtR.setText("---");
            }

        }

        // Min range 0 for hot load
        if (mAmmo.isHotLoaded()) {
            wMinR.setText("---");
        }

        onResize();
    } // End private void updateRangeDisplayForAmmo( AmmoType )

    private void updateAttackValues(WeaponMounted weapon, AmmoMounted ammo) {
        WeaponType weaponType = weapon.getType();
        // update Attack Values and change range
        int avShort = weaponType.getRoundShortAV();
        int avMed = weaponType.getRoundMedAV();
        int avLong = weaponType.getRoundLongAV();
        int avExt = weaponType.getRoundExtAV();
        int maxRange = weaponType.getMaxRange(weapon);

        // change range and attack values based upon ammo
        if (null != ammo) {
            AmmoType ammoType = ammo.getType();
            double[] changes = changeAttackValues(ammoType, avShort, avMed,
                  avLong, avExt, maxRange);
            avShort = (int) changes[0];
            avMed = (int) changes[1];
            avLong = (int) changes[2];
            avExt = (int) changes[3];
            maxRange = (int) changes[4];
        }

        if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
              && weaponType.isCapital()) {
            avShort *= 10;
            avMed *= 10;
            avLong *= 10;
            avExt *= 10;
        }

        // set default values in case if statement stops
        wShortAVR.setText("---");
        wMedAVR.setText("---");
        wLongAVR.setText("---");
        wExtAVR.setText("---");
        wShortR.setText("---");
        wMedR.setText("---");
        wLongR.setText("---");
        wExtR.setText("---");
        // every weapon gets at least short range
        wShortAVR.setText(Integer.toString(avShort));
        if (weaponType.isCapital()) {
            wShortR.setText("1-12");
        } else if (weaponType.hasFlag(WeaponType.F_PD_BAY)) {
            // Point Defense bays have a variable range too, depending on the mode they're
            // in
            if (weapon.hasModes() && weapon.curMode().equals("Point Defense")) {
                wShortR.setText("1");
            } else {
                wShortR.setText("1-6");
            }
        } else {
            wShortR.setText("1-6");
        }
        if (maxRange > WeaponType.RANGE_SHORT) {
            wMedAVR.setText(Integer.toString(avMed));
            if (weaponType.isCapital()) {
                wMedR.setText("13-24");
            } else {
                wMedR.setText("7-12");
            }
        }
        if (maxRange > WeaponType.RANGE_MED) {
            wLongAVR.setText(Integer.toString(avLong));
            if (weaponType.isCapital()) {
                wLongR.setText("25-40");
            } else {
                wLongR.setText("13-20");
            }
        }
        if (maxRange > WeaponType.RANGE_LONG) {
            wExtAVR.setText(Integer.toString(avExt));
            if (weaponType.isCapital()) {
                wExtR.setText("41-50");
            } else {
                wExtR.setText("21-25");
            }
        }

        onResize();
    }

    private double[] changeAttackValues(AmmoType ammoType, double avShort,
          double avMed, double avLong, double avExt, int maxRange) {

        if (AmmoType.AmmoTypeEnum.ATM == ammoType.getAmmoType()) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                maxRange = WeaponType.RANGE_EXT;
                avShort = avShort / 2;
                avMed = avMed / 2;
                avLong = avMed;
                avExt = avMed;
            } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
                maxRange = WeaponType.RANGE_SHORT;
                avShort = avShort + (avShort / 2);
                avMed = 0;
                avLong = 0;
                avExt = 0;
            }
        } // End weapon-is-ATM
        else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML) {
            // first check for artemis
            int bonus = 0;
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
                int rack = ammoType.getRackSize();
                if (rack == 5) {
                    bonus += 1;
                } else if (rack >= 7) {
                    bonus += 2;
                }
                avShort = avShort + bonus;
                avMed = avMed + bonus;
                avLong = avLong + bonus;
            }
            if (!ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                maxRange = WeaponType.RANGE_SHORT;
                avShort = avShort * 2;
                avMed = 0;
                avLong = 0;
                avExt = 0;
            }
        } // end weapon is MML
        else if ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
              || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)
              || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM)
              || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP)) {

            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
                if ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) || (ammoType.getAmmoType()
                      == AmmoType.AmmoTypeEnum.LRM_IMP)) {
                    int bonus = (int) Math.ceil(ammoType.getRackSize() / 5.0);
                    avShort = avShort + bonus;
                    avMed = avMed + bonus;
                    avLong = avLong + bonus;
                }
                if ((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) || (ammoType.getAmmoType()
                      == AmmoType.AmmoTypeEnum.SRM_IMP)) {
                    avShort = avShort + 2;
                }
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_LBX) {
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
                int newAV = (int) Math.floor(0.6 * ammoType.getRackSize());
                avShort = newAV;
                if (avMed > 0) {
                    avMed = newAV;
                }
                if (avLong > 0) {
                    avLong = newAV;
                }
                if (avExt > 0) {
                    avExt = newAV;
                }
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AR10) {
            if (ammoType.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                avShort = 4;
                avMed = 4;
                avLong = 4;
                avExt = 4;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                avShort = 3;
                avMed = 3;
                avLong = 3;
                avExt = 3;
            } else if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                avShort = 100;
                avMed = 100;
                avLong = 100;
                avExt = 100;
            } else if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                avShort = 1000;
                avMed = 1000;
                avLong = 1000;
                avExt = 1000;
            } else {
                avShort = 2;
                avMed = 2;
                avLong = 2;
                avExt = 2;
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.KILLER_WHALE) {
            if (ammoType.hasFlag(AmmoType.F_PEACEMAKER)) {
                avShort = 1000;
                avMed = 1000;
                avLong = 1000;
                avExt = 1000;
            } else {
                avShort = 4;
                avMed = 4;
                avLong = 4;
                avExt = 4;
            }
        } else if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.WHITE_SHARK) {
            if (ammoType.hasFlag(AmmoType.F_SANTA_ANNA)) {
                avShort = 100;
                avMed = 100;
                avLong = 100;
                avExt = 100;
            } else {
                avShort = 3;
                avMed = 3;
                avLong = 3;
                avExt = 3;
            }
        }

        return new double[] { avShort, avMed, avLong, avExt, maxRange };

    }

    private void compileWeaponBay(WeaponMounted weapon, AmmoMounted mAmmo, boolean isCapital) {

        List<WeaponMounted> bayWeapons = weapon.getBayWeapons();
        WeaponType weaponType = weapon.getType();

        // set default values in case if statement stops
        wShortAVR.setText("---");
        wMedAVR.setText("---");
        wLongAVR.setText("---");
        wExtAVR.setText("---");
        wShortR.setText("---");
        wMedR.setText("---");
        wLongR.setText("---");
        wExtR.setText("---");

        int heat = 0;
        double avShort = 0;
        double avMed = 0;
        double avLong = 0;
        double avExt = 0;
        int maxRange = WeaponType.RANGE_SHORT;

        for (WeaponMounted m : bayWeapons) {
            if (!m.isBreached()
                  && !m.isMissing()
                  && !m.isDestroyed()
                  && !m.isJammed()
                  && ((mAmmo == null) || (mAmmo.getUsableShotsLeft() > 0))) {
                WeaponType bayWType = m.getType();
                heat = heat + m.getCurrentHeat();
                double mAVShort = bayWType.getShortAV();
                double mAVMed = bayWType.getMedAV();
                double mAVLong = bayWType.getLongAV();
                double mAVExt = bayWType.getExtAV();
                int mMaxR = bayWType.getMaxRange(m);

                // deal with any ammo adjustments
                if (null != mAmmo) {
                    double[] changes = changeAttackValues(mAmmo.getType(), mAVShort, mAVMed,
                          mAVLong, mAVExt, mMaxR);
                    mAVShort = changes[0];
                    mAVMed = changes[1];
                    mAVLong = changes[2];
                    mAVExt = changes[3];
                    mMaxR = (int) changes[4];
                }

                avShort = avShort + mAVShort;
                avMed = avMed + mAVMed;
                avLong = avLong + mAVLong;
                avExt = avExt + mAVExt;
                if (mMaxR > maxRange) {
                    maxRange = mMaxR;
                }
            }
        }
        // check for bracketing
        double multiplier = 1.0;
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            multiplier = 0.8;
        }
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            multiplier = 0.6;
        }
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            multiplier = 0.4;
        }
        avShort = multiplier * avShort;
        avMed = multiplier * avMed;
        avLong = multiplier * avLong;
        avExt = multiplier * avExt;

        if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_SANITY)
              && weaponType.isCapital()) {
            avShort *= 10;
            avMed *= 10;
            avLong *= 10;
            avExt *= 10;
        }

        wHeatR.setText(Integer.toString(heat));
        wShortAVR.setText(Integer.toString((int) Math.ceil(avShort)));
        if (isCapital) {
            wShortR.setText("1-12");
        } else {
            wShortR.setText("1-6");
        }
        if (maxRange > WeaponType.RANGE_SHORT) {
            wMedAVR.setText(Integer.toString((int) Math.ceil(avMed)));
            if (isCapital) {
                wMedR.setText("13-24");
            } else {
                wMedR.setText("7-12");
            }
        }
        if (maxRange > WeaponType.RANGE_MED) {
            wLongAVR.setText(Integer.toString((int) Math.ceil(avLong)));
            if (isCapital) {
                wLongR.setText("25-40");
            } else {
                wLongR.setText("13-20");
            }
        }
        if (maxRange > WeaponType.RANGE_LONG) {
            wExtAVR.setText(Integer.toString((int) Math.ceil(avExt)));
            if (isCapital) {
                wExtR.setText("41-50");
            } else {
                wExtR.setText("21-25");
            }
        }
        onResize();
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(weaponList)) {
            displaySelected();

            // Can't do anything if ClientGUI is null
            if (unitDisplayPanel.getClientGUI() == null) {
                return;
            }

            JComponent currPanel = unitDisplayPanel.getClientGUI().getCurrentPanel();
            // When in the Firing Phase, update the targeting information.
            if (currPanel instanceof FiringDisplay firingDisplay) {

                WeaponMounted mounted = null;
                WeaponListModel weaponModel = (WeaponListModel) weaponList.getModel();
                if (weaponList.getSelectedIndex() != -1) {
                    mounted = weaponModel.getWeaponAt(weaponList
                          .getSelectedIndex());
                }
                WeaponMounted prevMounted = null;
                if ((event.getLastIndex() != -1)
                      && (event.getLastIndex() < weaponModel.getSize())) {
                    prevMounted = weaponModel.getWeaponAt(event.getLastIndex());
                }
                // Some weapons have a specific target, which gets handled
                // in the target method
                if (mounted != null
                      && mounted.getType().hasFlag(WeaponType.F_VGL)) {
                    // Store previous target, if it's a weapon that doesn't
                    // have a forced target
                    if ((prevMounted != null)
                          && !prevMounted.getType().hasFlag(WeaponType.F_VGL)) {
                        prevTarget = firingDisplay.getTarget();
                    }
                    firingDisplay.target(null);
                } else {
                    if (prevTarget != null) {
                        firingDisplay.target(prevTarget);
                        unitDisplayPanel.getClientGUI().getBoardView()
                              .select(prevTarget.getPosition());
                        prevTarget = null;
                    } else {
                        firingDisplay.updateTarget();
                    }
                }
            } else if (currPanel instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) currPanel).updateTarget();
            }

            // Tell the <Phase>Display to update the
            // firing arc info when a weapon has been de-selected
            if (weaponList.getSelectedIndex() == -1) {
                unitDisplayPanel.getClientGUI().clearTemporarySprites();
            }
        }
        onResize();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        ClientGUI clientgui = unitDisplayPanel.getClientGUI();
        if (ev.getSource().equals(m_chAmmo)
              && (m_chAmmo.getSelectedIndex() != -1)
              && (clientgui != null)) {
            int n = weaponList.getSelectedIndex();
            if (n == -1) {
                return;
            }
            // This initializes the vAmmo if it is null
            if (vAmmo == null) {
                displaySelected();
            }

            // We can update display values without changing the selected unit's ammo;
            // this allows displaying selected unit's ammo-based ranges without owning it
            WeaponMounted mWeapon = ((WeaponListModel) weaponList.getModel()).getWeaponAt(n);

            AmmoMounted oldAmmo = mWeapon.getLinkedAmmo();
            AmmoMounted mAmmo = vAmmo.get(m_chAmmo.getSelectedIndex());

            weaponList.setSelectedIndex(n);
            weaponList.ensureIndexIsVisible(n);
            displaySelected();
            // Update once prior to changing any actual loaded ammo
            updateRangeDisplayForAmmo(mAmmo);

            // only change our own units
            if (!clientgui.getClient().getLocalPlayer()
                  .equals(entity.getOwner())) {
                return;
            }

            // if this is a weapon bay, then this is not what we want
            boolean isBay = false;
            if (mWeapon.getType() instanceof BayWeapon) {
                isBay = true;
                n = m_chBayWeapon.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                //
                WeaponMounted sWeapon = mWeapon.getBayWeapon(n);
                // cycle through all weapons of the same type and load with
                // this ammo
                if (sWeapon != null) {
                    for (WeaponMounted bWeapon : mWeapon.getBayWeapons()) {
                        // FIXME: Consider new AmmoType::equals / BombType::equals
                        if (bWeapon.getType().equals(sWeapon.getType())) {
                            entity.loadWeapon(bWeapon, mAmmo);
                            // Alert the server of the update.
                            clientgui.getClient().sendAmmoChange(
                                  entity.getId(),
                                  entity.getEquipmentNum(bWeapon),
                                  entity.getEquipmentNum(mAmmo),
                                  0);
                        }
                    }
                }
            } else {
                entity.loadWeapon(mWeapon, mAmmo);
                // Alert the server of the update.
                clientgui.getClient().sendAmmoChange(entity.getId(),
                      entity.getEquipmentNum(mWeapon),
                      entity.getEquipmentNum(mAmmo),
                      0);
            }

            // Refresh for hot load change
            if ((((oldAmmo == null) || !oldAmmo.isHotLoaded()) && mAmmo
                  .isHotLoaded())
                  || ((oldAmmo != null) && oldAmmo.isHotLoaded() && !mAmmo
                  .isHotLoaded())) {
                displayMek(entity);
                weaponList.setSelectedIndex(n);
                weaponList.ensureIndexIsVisible(n);
                displaySelected();
            }

            // Update the range display to account for the weapon's loaded
            // ammo.
            updateRangeDisplayForAmmo(mAmmo);
            if (entity.isAirborne() || entity.usesWeaponBays()) {
                WeaponType weaponType = mWeapon.getType();
                if (isBay) {
                    compileWeaponBay(mWeapon, mAmmo, weaponType.isCapital());
                } else {
                    // otherwise I need to replace range display with
                    // standard ranges and attack values
                    updateAttackValues(mWeapon, mAmmo);
                }
            }

            // When in the Firing Phase, update the targeting information.
            if (clientgui.getCurrentPanel() instanceof FiringDisplay) {
                ((FiringDisplay) clientgui.getCurrentPanel()).updateTarget();
            } else if (clientgui.getCurrentPanel() instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) clientgui.getCurrentPanel()).updateTarget();
            }
            displaySelected();
        } else if (ev.getSource().equals(m_chBayWeapon)
              && (m_chBayWeapon.getItemCount() > 0)) {
            int n = weaponList.getSelectedIndex();
            if (n == -1) {
                return;
            }
            displaySelected();
        } else if (ev.getSource().equals(comboWeaponSortOrder)) {
            setWeaponComparator(comboWeaponSortOrder.getSelectedItem());
            if (entity.getOwner().equals(unitDisplayPanel.getClientGUI().getClient().getLocalPlayer())) {
                unitDisplayPanel.getClientGUI().getClient().sendEntityWeaponOrderUpdate(entity);
            }
        }
        onResize();
    }

    void setWeaponComparator(final @Nullable WeaponSortOrder weaponSortOrder) {
        if (weaponSortOrder == null) {
            return;
        }
        removeListeners();

        entity.setWeaponSortOrder(weaponSortOrder);
        ((WeaponListModel) weaponList.getModel()).sort(weaponSortOrder.getWeaponSortComparator(entity));

        onResize();
        addListeners();
    }

    private void addListeners() {
        if (listenerCounter >= 0) {
            comboWeaponSortOrder.addActionListener(this);
            m_chAmmo.addActionListener(this);
            m_chBayWeapon.addActionListener(this);
            weaponList.addListSelectionListener(this);
        }
        listenerCounter++;
    }

    private void removeListeners() {
        comboWeaponSortOrder.removeActionListener(this);
        m_chAmmo.removeActionListener(this);
        m_chBayWeapon.removeActionListener(this);
        weaponList.removeListSelectionListener(this);
        listenerCounter--;
    }

    public Targetable getPrevTarget() {
        return prevTarget;
    }

    public void setPrevTarget(Targetable prevTarget) {
        this.prevTarget = prevTarget;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update the text size when the GUI scaling changes
        if (e.getName().equals(GUIPreferences.UNIT_DISPLAY_WEAPON_LIST_HEIGHT)) {
            tWeaponScroll.setMinimumSize(new Dimension(500, GUIP.getUnitDisplayWeaponListHeight()));
            tWeaponScroll.setPreferredSize(new Dimension(500, GUIP.getUnitDisplayWeaponListHeight()));
            tWeaponScroll.revalidate();
            tWeaponScroll.repaint();
        }
    }

    /**
     * Updates the Weapon Panel with the information for the given entity. If the given entity is `null`, this method
     * will do nothing.
     *
     * @param entity - The weapon panel will update info based on the {@link Entity} provided.
     */
    public void updateForEntity(Entity entity) {
        if (entity == null) {
            return;
        }

        // Takes note of the selected weapon to re-select after the call to
        // `displayMek()`
        int weaponNum = getSelectedWeaponNum();
        displayMek(entity);
        selectWeapon(weaponNum);
    }
}
