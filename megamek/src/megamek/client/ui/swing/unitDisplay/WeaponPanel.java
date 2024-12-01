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
package megamek.client.ui.swing.unitDisplay;

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
import java.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

import megamek.client.Client;
import megamek.client.event.MekDisplayEvent;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.FiringDisplay;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.TargetingPhaseDisplay;
import megamek.client.ui.swing.tooltip.UnitToolTip;
import megamek.client.ui.swing.widget.BackGroundDrawer;
import megamek.client.ui.swing.widget.PMUtil;
import megamek.client.ui.swing.widget.PicMap;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.UnitDisplaySkinSpecification;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.WeaponSortOrder;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.modifiers.DamageModifier;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.weapons.AreaEffectHelper;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.gaussrifles.HAGWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class contains the all the gizmos for firing the mek's weapons.
 */
public class WeaponPanel extends PicMap implements ListSelectionListener, ActionListener, IPreferenceChangeListener {
    /**
     * Mouse adaptor for the weapon list. Supports rearranging the weapons
     * to define a custom ordering.
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

            Object src = e.getSource();
            // Check to see if we are in a state we care about
            if (!mouseDragging || !(src instanceof JList)) {
                return;
            }
            JList<?> srcList = (JList<?>) src;
            WeaponListModel srcModel = (WeaponListModel) srcList.getModel();
            int currentIndex = srcList.locationToIndex(e.getPoint());
            if (currentIndex != dragSourceIndex) {
                int dragTargetIndex = srcList.getSelectedIndex();
                WeaponMounted weap1 = srcModel.getWeaponAt(dragSourceIndex);
                srcModel.swapIdx(dragSourceIndex, dragTargetIndex);
                dragSourceIndex = currentIndex;
                Entity ent = weap1.getEntity();

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
            addListeners();
        }
    }

    /**
     * ListModel implementation that supports keeping track of a list of Mounted
     * instantiations, how to display them in the JList, and an ability to sort
     * the Mounteds given WeaponComparators.
     *
     * @author arlith
     */
    class WeaponListModel extends AbstractListModel<String> {
        private static final long serialVersionUID = 6312003196674512339L;

        /**
         * A collection of Mounted instantiations.
         */
        private List<WeaponMounted> weapons;

        /**
         * The Entity that owns the collection of Mounteds.
         */
        private Entity en;

        WeaponListModel(Entity e) {
            en = e;
            weapons = new ArrayList<>();
        }

        /**
         * Add a new weapon to the list.
         *
         * @param w
         */
        public void addWeapon(WeaponMounted w) {
            weapons.add(w);
            fireIntervalAdded(this, weapons.size() - 1, weapons.size() - 1);
        }

        /**
         * Given the equipment (weapon) id, return the index in the (possibly
         * sorted) list of Mounteds.
         *
         * @param weaponId
         * @return
         */
        public int getIndex(int weaponId) {
            Mounted<?> mount = en.getEquipment(weaponId);
            for (int i = 0; i < weapons.size(); i++) {
                if (weapons.get(i).equals(mount)) {
                    return i;
                }
            }
            return -1;
        }

        public void removeAllElements() {
            int numWeapons = weapons.size() - 1;
            weapons.clear();
            fireIntervalRemoved(this, 0, numWeapons);
        }

        /**
         * Swap the Mounteds at the two specified index values.
         *
         * @param idx1
         * @param idx2
         */
        public void swapIdx(int idx1, int idx2) {
            // Bounds checking
            if ((idx1 >= weapons.size()) || (idx2 >= weapons.size())
                    || (idx1 < 0) || (idx2 < 0)) {
                return;
            }
            WeaponMounted m1 = weapons.get(idx1);
            weapons.set(idx1, weapons.get(idx2));
            weapons.set(idx2, m1);
            fireContentsChanged(this, idx1, idx1);
            fireContentsChanged(this, idx2, idx2);
        }

        public WeaponMounted getWeaponAt(int index) {
            return weapons.get(index);
        }

        /**
         * Given an index into the (possibly sorted) list of Mounted, return a
         * text description. This consists of the Mounted's description, as
         * well as additional information like location, whether the Mounted is
         * shot/jammed/destroyed, etc. This is what the JList will display.
         */
        @Override
        public String getElementAt(int index) {
            final WeaponMounted mounted = weapons.get(index);
            final WeaponType wtype = mounted.getType();
            Game game = null;
            if (unitDisplay.getClientGUI() != null) {
                game = unitDisplay.getClientGUI().getClient().getGame();
            }

            StringBuilder wn = new StringBuilder(mounted.getDesc());
            if ((mounted.getLinkedBy() != null)
                    && (mounted.getLinkedBy().getType() instanceof MiscType)
                    && (mounted.getLinkedBy().getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE))) {
                wn.append("+").append(mounted.getLinkedBy().getShortName());
            }
            wn.append(" [");
            wn.append(en.getLocationAbbr(mounted.getLocation()));
            //Check if mixedTech and add Clan or IS tag
            if (en.isMixedTech()) {
                wn.insert(0, wtype.isClan() ? "(C) " : "(IS) ");
            }
            if (mounted.isSplit()) {
                wn.append('/');
                wn.append(en.getLocationAbbr(mounted.getSecondLocation()));
            }
            wn.append(']');
            // determine shots left & total shots left
            if ((wtype.getAmmoType() != AmmoType.T_NA)
                    && (!wtype.hasFlag(WeaponType.F_ONESHOT)
                            || wtype.hasFlag(WeaponType.F_BA_INDIVIDUAL))
                    && (wtype.getAmmoType() != AmmoType.T_INFANTRY)) {
                int shotsLeft = 0;
                if ((mounted.getLinked() != null)
                        && !mounted.getLinked().isDumping()) {
                    shotsLeft = mounted.getLinked().getUsableShotsLeft();
                }

                int totalShotsLeft = en.getTotalMunitionsOfType(mounted);

                wn.append(" (");
                wn.append(shotsLeft);
                wn.append('/');
                wn.append(totalShotsLeft);
                wn.append(')');
            } else if (wtype.hasFlag(WeaponType.F_DOUBLE_ONESHOT)
                    || (en.isSupportVehicle() && (wtype.getAmmoType() == AmmoType.T_INFANTRY))) {
                int shotsLeft = 0;
                int totalShots = 0;
                EnumSet<AmmoType.Munitions> munition = ((AmmoType) mounted.getLinked().getType()).getMunitionType();
                for (Mounted<?> current = mounted.getLinked(); current != null; current = current.getLinked()) {
                    if (((AmmoType) current.getType()).getMunitionType().equals(munition)) {
                        shotsLeft += current.getUsableShotsLeft();
                        totalShots += current.getOriginalShots();
                    }
                }
                wn.append(" (").append(shotsLeft)
                        .append("/").append(totalShots).append(")");
            }

            // MG rapidfire
            if (mounted.isRapidfire()) {
                wn.append(Messages.getString("MekDisplay.rapidFire"));
            }

            // Hotloaded Missile Launchers
            if (mounted.isHotLoaded()) {
                wn.append(Messages.getString("MekDisplay.isHotLoaded"));
            }

            // Fire Mode - lots of things have variable modes
            if (mounted.hasModes()) {
                wn.append(' ');

                wn.append(mounted.curMode().getDisplayableName());
                if (!mounted.pendingMode().equals("None")) {
                    wn.append(" (next turn, ");
                    wn.append(mounted.pendingMode().getDisplayableName());
                    wn.append(')');
                }
            }
            if ((game != null)
                    && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS)) {
                wn.append(' ');
                wn.append(mounted.getCalledShot().getDisplayableName());
            }
            return wn.toString();
        }

        /**
         * Returns the number of Mounteds in the list.
         */
        @Override
        public int getSize() {
            return weapons.size();
        }

        /**
         * Sort the Mounteds, generally using a WeaponComparator.
         *
         * @param comparator
         */
        public void sort(Comparator<WeaponMounted> comparator) {
            weapons.sort(comparator);
            fireContentsChanged(this, 0, weapons.size() - 1);
        }
    }

    private final UnitDisplay unitDisplay;
    private final Client client;

    private MMComboBox<WeaponSortOrder> comboWeaponSortOrder;
    public JList<String> weaponList;
    /**
     * Keep track of the previous target, used for certain weapons (like VGLs)
     * that will force a target. With this, we can restore the previous target
     * after the forced target.
     */
    private Targetable prevTarget = null;
    private JPanel panelMain;
    private JPanel panelLower;
    private JScrollPane tWeaponScroll;
    private JComboBox<String> m_chAmmo;
    public JComboBox<String> m_chBayWeapon;

    private JLabel wSortOrder;
    private JLabel wAmmo;
    private JLabel wBayWeapon;
    private JLabel wNameL;
    private JLabel wHeatL;
    private JLabel wArcHeatL;
    private JLabel wDamL;
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
    private JLabel currentHeatBuildupL;
    private JLabel currentHeatBuildupR;
    private JLabel wRangeL;
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

    private int minTopMargin = 8;
    private int minLeftMargin = 8;

    Color[] bgcolors = { Color.gray, Color.darkGray };
    int gridy = 0;
    public static final int INTERNAL_PANE_WIDTH = 400;
    public static final int LINE_HEIGHT = 25;
    public static final Color COLOR_FG = Color.WHITE;
    public static final Color TEXT_BG = Color.DARK_GRAY;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    WeaponPanel(UnitDisplay unitDisplay, Client client) {
        this.unitDisplay = unitDisplay;
        this.client = client;

        JPanel panelTop = new JPanel();
        panelTop.setOpaque(false);
        panelTop.setLayout(new GridBagLayout());
        gridy = 0;
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
        gridy = 0;
        panelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelText.setAlignmentY(Component.TOP_ALIGNMENT);
        panelText.setPreferredSize(new Dimension(INTERNAL_PANE_WIDTH, 20));
        panelText.setMaximumSize(null);
        createToHitText(panelText);

        JSplitPane splitPaneMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTop, panelText);
        splitPaneMain.setOpaque(false);

        panelMain = new JPanel();
        panelMain.setOpaque(false);
        panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.Y_AXIS));
        panelMain.add(splitPaneMain);

        panelLower = new JPanel();
        panelLower.setOpaque(false);
        panelLower.setLayout(new GridBagLayout());
        gridy = 0;
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

    private void addSubdisplay(JPanel parent, JComponent child, int minHeight, int fill) {
        child.setMinimumSize(new Dimension(INTERNAL_PANE_WIDTH, minHeight));
        // null means allow UI to recompute
        child.setMaximumSize(new Dimension(INTERNAL_PANE_WIDTH, minHeight * 2));
        child.setPreferredSize(null);
        child.setAlignmentX(Component.LEFT_ALIGNMENT);
        child.setAlignmentY(Component.TOP_ALIGNMENT);
        child.setBackground(bgcolors[(gridy++) % bgcolors.length]);
        child.setOpaque(false);

        Dimension min = parent.getMinimumSize();
        min.height += minHeight;
        parent.setMinimumSize(min);

        Dimension pref = parent.getPreferredSize();
        pref.height += minHeight;
        parent.setPreferredSize(pref);

        parent.add(child, GBC.eol()
                .gridy(gridy++)
                .insets(10, 1, 10, 1)
                .weighty(1)
                .fill(fill));
    }

    private void createWeaponList(JPanel parent) {
        wSortOrder = new JLabel(
                Messages.getString("MekDisplay.WeaponSortOrder.label"),
                SwingConstants.LEFT);
        setupLabel(wSortOrder);

        JPanel pWeaponOrder = new JPanel(new GridBagLayout());
        pWeaponOrder.setOpaque(false);
        int pgridy = 0;

        pWeaponOrder.add(wSortOrder,
                GBC.std().insets(15, 1, 1, 1).gridy(pgridy).gridx(0));
        comboWeaponSortOrder = new MMComboBox<>("comboWeaponSortOrder", WeaponSortOrder.values());
        pWeaponOrder.add(comboWeaponSortOrder, GBC.eol()
                .fill(GridBagConstraints.HORIZONTAL)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 15, 1).gridy(pgridy).gridx(1));
        addSubdisplay(parent, pWeaponOrder, LINE_HEIGHT, GridBagConstraints.BOTH);

        // weapon list
        weaponList = new JList<>(new DefaultListModel<>());
        WeaponListMouseAdapter mouseAdapter = new WeaponListMouseAdapter();
        weaponList.addMouseListener(mouseAdapter);
        weaponList.addMouseMotionListener(mouseAdapter);

        tWeaponScroll = new JScrollPane(weaponList);
        addSubdisplay(parent, tWeaponScroll, GUIP.getUnitDisplayWeaponListHeight(), GridBagConstraints.BOTH);

        weaponList.resetKeyboardActions();
        for (KeyListener key : weaponList.getKeyListeners()) {
            weaponList.removeKeyListener(key);
        }

        // adding Ammo choice + label
        wAmmo = new JLabel(Messages.getString("MekDisplay.Ammo"), SwingConstants.LEFT);
        setupLabel(wAmmo);
        m_chAmmo = new JComboBox<>();

        wBayWeapon = new JLabel(Messages.getString("MekDisplay.Weapon"), SwingConstants.LEFT);
        setupLabel(wBayWeapon);
        m_chBayWeapon = new JComboBox<>();

        JPanel pAmmo = new JPanel(new GridBagLayout());
        pAmmo.setOpaque(false);
        pgridy = 0;

        pAmmo.add(wBayWeapon, GBC.std().insets(15, 1, 1, 1).gridy(pgridy).gridx(0));
        pAmmo.add(m_chBayWeapon, GBC.std().fill(GridBagConstraints.HORIZONTAL)
                .insets(15, 1, 15, 1).gridy(pgridy).gridx(1));
        pgridy++;

        pAmmo.add(wAmmo, GBC.std().insets(15, 9, 1, 1).gridy(pgridy).gridx(0));

        pAmmo.add(m_chAmmo,
                GBC.eol().fill(GridBagConstraints.HORIZONTAL)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 9, 15, 1).gridy(pgridy).gridx(1));

        addSubdisplay(parent, pAmmo, LINE_HEIGHT * 3 / 2, GridBagConstraints.BOTH);

    }

    private void createWeaponDisplay(JPanel parent) {
        // Adding weapon display labels
        wNameL = new JLabel(Messages.getString("MekDisplay.Name"), SwingConstants.CENTER);
        setupLabel(wNameL);
        wHeatL = new JLabel(Messages.getString("MekDisplay.Heat"), SwingConstants.CENTER);
        setupLabel(wHeatL);
        wDamL = new JLabel(Messages.getString("MekDisplay.Damage"), SwingConstants.CENTER);
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
        int pgridy = 0;

        pCurrentWeapon.add(wNameL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(5, 9, 1, 1).gridy(pgridy).gridx(0).weightx(1));

        pCurrentWeapon.add(wHeatL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 9, 1, 1).gridy(pgridy).gridx(1).weightx(1));

        pCurrentWeapon.add(wDamL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 9, 1, 1).gridy(pgridy).gridx(2).weightx(1));

        pCurrentWeapon.add(wArcHeatL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 9, 1, 1).gridy(pgridy).gridx(3).weightx(1));

        pCurrentWeapon.add(wDamageTrooperL, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 9, 1, 1).gridy(pgridy).gridx(3).weightx(1));
        pgridy++;
        pCurrentWeapon.add(wNameR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(5, 1, 1, 1).gridy(pgridy).gridx(0).weightx(1));

        pCurrentWeapon.add(wHeatR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 1, 1, 1).gridy(pgridy).gridx(1).weightx(1));

        pCurrentWeapon.add(wDamR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 1, 1, 1).gridy(pgridy).gridx(2).weightx(1));

        pCurrentWeapon.add(wArcHeatR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 1, 1, 1).gridy(pgridy).gridx(3).weightx(1));

        pCurrentWeapon.add(wDamageTrooperR, GBC.std().fill(GridBagConstraints.NONE).anchor(GridBagConstraints.WEST)
                .insets(15, 1, 1, 1).gridy(pgridy).gridx(3).weightx(1));

        addSubdisplay(parent, pCurrentWeapon, LINE_HEIGHT * 2, GridBagConstraints.NONE);
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
        int pgridy = 0;

        pRange.add(wMinL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 9, 9, 1).gridy(pgridy).gridx(0).weightx(1));

        pRange.add(wShortL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 9, 9, 1).gridy(pgridy).gridx(1).weightx(1));

        pRange.add(wMedL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 9, 9, 1).gridy(pgridy).gridx(2).weightx(1));

        pRange.add(wLongL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 9, 9, 1).gridy(pgridy).gridx(3).weightx(1));

        pRange.add(wExtL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 9, 9, 1).gridy(pgridy).gridx(4).weightx(1));

        pgridy++;

        pRange.add(wInfantryRange0L, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 9, 1).gridy(pgridy).gridx(0).weightx(1));

        pRange.add(wInfantryRange1L, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 9, 1).gridy(pgridy).gridx(1).weightx(1));

        pRange.add(wInfantryRange2L, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 9, 1).gridy(pgridy).gridx(2).weightx(1));

        pRange.add(wInfantryRange3L, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 9, 1).gridy(pgridy).gridx(3).weightx(1));

        pRange.add(wInfantryRange4L, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 9, 1).gridy(pgridy).gridx(4).weightx(1));

        pRange.add(wInfantryRange5L, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 9, 9, 1).gridy(pgridy).gridx(4).weightx(1));

        pgridy++;
        // ----------------

        pRange.add(wMinR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(0).weightx(1));

        pRange.add(wShortR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(1).weightx(1));

        pRange.add(wMedR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(2).weightx(1));

        pRange.add(wLongR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(3).weightx(1));

        pRange.add(wExtR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(4).weightx(1));

        pRange.add(wInfantryRange0R, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(0).weightx(1));

        pRange.add(wInfantryRange1R, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(1).weightx(1));

        pRange.add(wInfantryRange2R, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(2).weightx(1));

        pRange.add(wInfantryRange3R, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(3).weightx(1));

        pRange.add(wInfantryRange4R, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(4).weightx(1));

        pRange.add(wInfantryRange5R, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(5).weightx(1));

        pgridy++;
        // ----------------
        pRange.add(wAVL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(0).weightx(1));

        pRange.add(wShortAVR, GBC.std().fill(GridBagConstraints.NONE)
                .anchor(GridBagConstraints.WEST)
                .insets(15, 1, 9, 1).gridy(pgridy).gridx(1).weightx(1));

        pRange.add(wMedAVR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(2).weightx(1));

        pRange.add(wLongAVR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(3).weightx(1));

        pRange.add(wExtAVR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 9, 1).gridy(pgridy).gridx(4).weightx(1));

        pRange.setMinimumSize(new Dimension(INTERNAL_PANE_WIDTH, LINE_HEIGHT));
        pRange.setMaximumSize(new Dimension(INTERNAL_PANE_WIDTH, LINE_HEIGHT));
        pRange.setPreferredSize(new Dimension(INTERNAL_PANE_WIDTH, LINE_HEIGHT));
        addSubdisplay(parent, pRange, LINE_HEIGHT * 2, GridBagConstraints.NONE);
    }

    private void createToHitDisplay(JPanel parent) {
        // to hit panel
        JPanel pTargetInfo = new JPanel(new GridBagLayout());
        pTargetInfo.setOpaque(true);

        wRangeL = new JLabel(Messages.getString("MekDisplay.Range"), SwingConstants.LEFT);
        setupLabel(wRangeL);

        wRangeR = new JLabel("---", SwingConstants.CENTER);
        setupLabel(wRangeR);

        currentHeatBuildupL = new JLabel(Messages.getString("MekDisplay.HeatBuildup"), SwingConstants.RIGHT);
        setupLabel(currentHeatBuildupL);

        currentHeatBuildupR = new JLabel("--", SwingConstants.LEFT);
        setupLabel(currentHeatBuildupR);

        wTargetExtraInfo = new JLabel();
        setupLabel(wTargetExtraInfo);

        int pgridy = 0;
        wTargetExtraInfo.setMinimumSize(new Dimension(20, LINE_HEIGHT));
        pTargetInfo.add(wTargetExtraInfo,
                GBC.eol().fill(GridBagConstraints.BOTH)
                        .anchor(GridBagConstraints.WEST)
                        .insets(5, 1, 5, 1).gridy(pgridy).gridx(0));
        pgridy++;

        pTargetInfo.add(currentHeatBuildupL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(5, 1, 1, 1).gridy(pgridy).gridx(0));

        pTargetInfo.add(currentHeatBuildupR,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(2, 1, 1, 1).gridy(pgridy).gridx(1));

        pTargetInfo.add(wRangeL,
                GBC.std().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(15, 1, 1, 1).gridy(pgridy).gridx(2));

        pTargetInfo.add(wRangeR,
                GBC.eol().fill(GridBagConstraints.NONE)
                        .anchor(GridBagConstraints.WEST)
                        .insets(5, 1, 5, 1).gridy(pgridy).gridx(3));

        addSubdisplay(parent, pTargetInfo, LINE_HEIGHT * 2, GridBagConstraints.HORIZONTAL);
    }

    private void createToHitText(JPanel parent) {
        toHitText = new JTextPane();
        setupTextPane(toHitText);

        JScrollPane toHitScroll = new JScrollPane(toHitText);
        addSubdisplay(parent, toHitScroll, LINE_HEIGHT * 3, GridBagConstraints.BOTH);
    }

    private void createTargetDisplay(JPanel parent) {
        wTargetInfo = new JTextPane();
        setupTextPane(wTargetInfo);
        addSubdisplay(parent, wTargetInfo, LINE_HEIGHT * 2, GridBagConstraints.BOTH);
    }

    public void clearToHit() {
        toHitText.setText("---");
    }

    public void setToHit(ToHitData toHit) {
        setToHit(toHit, false);
    }

    public void setToHit(ToHitData toHit, boolean natAptGunnery) {
        String txt = "";

        switch (toHit.getValue()) {
            case TargetRoll.IMPOSSIBLE:
            case TargetRoll.AUTOMATIC_FAIL:
                txt = String.format("To Hit: (0%%) %s", toHit.getDesc());
                break;
            case TargetRoll.AUTOMATIC_SUCCESS:
                txt = String.format("To Hit: (100%%) %s", toHit.getDesc());
                break;
            default:
                txt = String.format("<font color=\"%s\">To Hit: <b>%2d (%2.0f%%)</b></font> = %s",
                        GUIP.hexColor(GUIP.getUnitToolTipHighlightColor()),
                        toHit.getValue(),
                        Compute.oddsAbove(toHit.getValue(), natAptGunnery),
                        toHit.getDesc());
                break;
        }

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
        String txt = "";

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
        int dx = Math.round(((w - r.width) / 2));
        if (dx < minLeftMargin) {
            dx = minLeftMargin;
        }
        int dy = minTopMargin;
        setContentMargins(dx, dy, dx, dy);
    }

    private void setBackGround() {
        UnitDisplaySkinSpecification udSpec = SkinXMLHandler.getUnitDisplaySkin();

        Image tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBackgroundTile()).toString());
        PMUtil.setImage(tile, this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_HORIZONTAL | BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getLeftLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.TILING_VERTICAL | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getRightLine()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getBottomLeftCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_TOP
                | BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage(
                new MegaMekFile(Configuration.widgetsDir(), udSpec.getTopRightCorner()).toString());
        PMUtil.setImage(tile, this);
        addBgDrawer(new BackGroundDrawer(tile, b));

        b = BackGroundDrawer.NO_TILING | BackGroundDrawer.VALIGN_BOTTOM
                | BackGroundDrawer.HALIGN_RIGHT;
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

        if (unitDisplay.getClientGUI() != null) {
            game = unitDisplay.getClientGUI().getClient().getGame();
        }

        // update pointer to weapons
        entity = en;

        // Check Game Options for max external heat
        int max_ext_heat = game != null ? game.getOptions().intOption(OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT)
                : 15;
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

            if (!((Mek) en).hasLaserHeatSinks()) {
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
        }
        Coords position = entity.getPosition();
        if (!en.isOffBoard() && (position != null)) {
            Hex hex = game.getBoard().getHex(position);
            if (hex.containsTerrain(Terrains.FIRE)
                    && (hex.getFireTurn() > 0)) {
                // standing in fire
                if ((en instanceof Mek)
                        && ((Mek) en).hasIntactHeatDissipatingArmor()) {
                    currentHeatBuildup += 2;
                } else {
                    currentHeatBuildup += 5;
                }
            }

            if (hex.terrainLevel(Terrains.MAGMA) == 1) {
                if ((en instanceof Mek)
                        && ((Mek) en).hasIntactHeatDissipatingArmor()) {
                    currentHeatBuildup += 2;
                } else {
                    currentHeatBuildup += 5;
                }
            } else if (hex.terrainLevel(Terrains.MAGMA) == 2) {
                if ((en instanceof Mek)
                        && ((Mek) en).hasIntactHeatDissipatingArmor()) {
                    currentHeatBuildup += 5;
                } else {
                    currentHeatBuildup += 10;
                }
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
        weaponList.setModel(new WeaponListModel(en));
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
        for (int i = 0; i < entity.getWeaponList().size(); i++) {
            WeaponMounted mounted = entity.getWeaponList().get(i);

            // Don't add bomb weapons for LAMs in mek mode except RL and TAG.
            if ((entity instanceof LandAirMek)
                    && (entity.getConversionMode() == LandAirMek.CONV_MODE_MEK)
                    && mounted.getType().hasFlag(WeaponType.F_BOMB_WEAPON)
                    && mounted.getType().getAmmoType() != AmmoType.T_RL_BOMB
                    && !mounted.getType().hasFlag(WeaponType.F_TAG)) {
                continue;
            }

            ((WeaponListModel) weaponList.getModel()).addWeapon(mounted);
            if (mounted.isUsedThisRound()
                    && (game.getPhase() == mounted.usedInPhase())
                    && game.getPhase().isFiring()) {
                hasFiredWeapons = true;
                // add heat from weapons fire to heat tracker
                if (entity.usesWeaponBays()) {
                    // if using bay heat option then don't add total arc
                    if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY)) {
                        for (WeaponMounted weapon : mounted.getBayWeapons()) {
                            currentHeatBuildup += weapon.getCurrentHeat();
                        }
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
                    if (!mounted.isBombMounted()) {
                        currentHeatBuildup += mounted.getCurrentHeat();
                    }
                }
            }
        }
        comboWeaponSortOrder.setSelectedItem(entity.getWeaponSortOrder());
        setWeaponComparator(comboWeaponSortOrder.getSelectedItem());

        if (en.hasDamagedRHS() && hasFiredWeapons) {
            currentHeatBuildup++;
        }

        if (en.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
            currentHeatBuildup -= 4;
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

        currentHeatBuildupR.setForeground(GUIP.getColorForHeat(heatOverCapacity, Color.WHITE));
        currentHeatBuildupR.setText(heatText + " (" + heatCapacityStr + ')' + sheatOverCapacity);

        // change what is visible based on type
        if (entity.usesWeaponBays()) {
            wArcHeatL.setVisible(true);
            wArcHeatR.setVisible(true);
            m_chBayWeapon.setVisible(true);
            wBayWeapon.setVisible(true);
        } else {
            wArcHeatL.setVisible(false);
            wArcHeatR.setVisible(false);
            m_chBayWeapon.setVisible(false);
            wBayWeapon.setVisible(false);
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
        if (((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE))
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
     * @return the AmmoMounted currently selected by the ammo selector combo box, if any.
     * The returned AmmoMounted may or may not be the ammo that is linked to the weapon.
     */
    public Optional<AmmoMounted> getSelectedAmmo() {
        int selected = m_chAmmo.getSelectedIndex();
        if ((selected == -1) || (vAmmo == null) || (selected >= vAmmo.size())) {
            return Optional.empty();
        } else {
            return Optional.of(vAmmo.get(selected));
        }
    }

    /**
     * Returns the equipment ID number for the weapon currently selected
     */
    public int getSelectedWeaponNum() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return -1;
        }
        return entity.getEquipmentNum(((WeaponListModel) weaponList.getModel()).getWeaponAt(selected));
    }

    /**
     * Selects the first valid weapon in the weapon list.
     *
     * @return The weapon id of the weapon selected
     */
    public int selectFirstWeapon() {
        // Entity has no weapons, return -1;
        if (entity.getWeaponList().isEmpty()
                || (entity.usesWeaponBays() && entity.getWeaponBayList().isEmpty())) {
            return -1;
        }
        WeaponListModel weapList = (WeaponListModel) weaponList.getModel();
        for (int i = 0; i < weaponList.getModel().getSize(); i++) {
            WeaponMounted selectedWeap = weapList.getWeaponAt(i);
            if (entity.isWeaponValidForPhase(selectedWeap)) {
                weaponList.setSelectedIndex(i);
                weaponList.ensureIndexIsVisible(i);
                return entity.getEquipmentNum(selectedWeap);
            }
        }
        // Found no valid weapon
        weaponList.setSelectedIndex(-1);
        return -1;
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
        WeaponMounted selectedWeap;
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
            selectedWeap = ((WeaponListModel) weaponList.getModel())
                    .getWeaponAt(selected);
        } while (!hasLooped && !entity.isWeaponValidForPhase(selectedWeap));

        if ((selected >= 0) && (selected < entity.getWeaponList().size())
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
        WeaponMounted selectedWeap;
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
            selectedWeap = ((WeaponListModel) weaponList.getModel())
                    .getWeaponAt(selected);
        } while (!hasLooped && !entity.isWeaponValidForPhase(selectedWeap));

        if ((selected >= 0) && (selected < entity.getWeaponList().size())
                && !hasLooped) {
            return selected;
        } else {
            return -1;
        }
    }

    public int getNextWeaponNum() {
        int selected = getNextWeaponListIdx();
        if ((selected >= 0) && (selected < entity.getWeaponList().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                    .getModel()).getWeaponAt(selected));
        } else {
            return -1;
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
        if ((selected >= 0) && (selected < entity.getWeaponList().size())) {
            return entity.getEquipmentNum(((WeaponListModel) weaponList
                    .getModel()).getWeaponAt(selected));
        } else {
            return -1;
        }
    }

    /**
     * Selects the prevous valid weapon in the weapon list.
     *
     * @return The weaponId for the selected weapon
     */
    public int selectPrevWeapon() {
        int selected = getPrevWeaponListIdx();
        weaponList.setSelectedIndex(selected);
        weaponList.ensureIndexIsVisible(selected);
        if ((selected >= 0) && (selected < entity.getWeaponList().size())) {
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
        WeaponType wtype = mounted.getType();
        // The rules are a bit sparse on airborne (dropping) ground units, but it seems
        // they should
        // still attack like ground units.
        boolean aerospaceAttack = entity.isAero() && (entity.isAirborne() || entity.usesWeaponBays());
        // update weapon display
        wNameR.setText(mounted.getDesc());
        wHeatR.setText(Integer.toString(mounted.getCurrentHeat()));

        wArcHeatR.setText(Integer.toString(entity.getHeatInArc(
                mounted.getLocation(), mounted.isRearMounted())));

        if ((wtype instanceof InfantryWeapon) && !wtype.hasFlag(WeaponType.F_TAG)) {
            wDamageTrooperL.setVisible(true);
            wDamageTrooperR.setVisible(true);
            InfantryWeapon inftype = (InfantryWeapon) wtype;
            if (entity.isConventionalInfantry()) {
                wDamageTrooperR.setText(Double.toString((double) Math.round(
                        ((Infantry) entity).getDamagePerTrooper() * 1000) / 1000));
            } else {
                wDamageTrooperR.setText(Double.toString(inftype.getInfantryDamage()));
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
            int zeromods = 0;
            if (inftype.hasFlag(WeaponType.F_INF_POINT_BLANK)) {
                zeromods++;
            }

            if (inftype.hasFlag(WeaponType.F_INF_ENCUMBER)
                    || (inftype.getCrew() > 1)) {
                zeromods++;
            }

            if (inftype.hasFlag(WeaponType.F_INF_BURST)) {
                zeromods--;
            }

            int range = inftype.getInfantryRange();
            if (entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET) {
                range /= 2;
            }
            switch (range) {
                case 0:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R.setText("+" + zeromods);
                    wInfantryRange0L.setVisible(true);
                    wInfantryRange0R.setVisible(true);
                    break;
                case 1:
                    wInfantryRange0L.setText("0");
                    wInfantryRange0R
                            .setText(Integer.toString(zeromods - 2));
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
                            .setText(Integer.toString(zeromods - 2));
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
                            .setText(Integer.toString(zeromods - 2));
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
                            .setText(Integer.toString(zeromods - 2));
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
                            .setText(Integer.toString(zeromods - 1));
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
                            .setText(Integer.toString(zeromods - 1));
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
                            .setText(Integer.toString(zeromods - 1));
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
                    && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE))
                    || aerospaceAttack) {
                wExtL.setVisible(true);
                wExtR.setVisible(true);
            }
        }

        if (wtype.getDamage() == WeaponType.DAMAGE_BY_CLUSTERTABLE) {
            if (wtype instanceof HAGWeapon) {
                wDamR.setText(Messages.getString("MekDisplay.Variable"));
            } else {
                wDamR.setText(Messages.getString("MekDisplay.Missile"));
            }
        } else if (wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
            wDamR.setText(Messages.getString("MekDisplay.Variable"));
        } else if (wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
            wDamR.setText(Messages.getString("MekDisplay.Special"));
        } else if (wtype.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
            StringBuilder damage = new StringBuilder();
            int artyDamage = wtype.getRackSize();
            damage.append(artyDamage);
            int falloff = 10;
            boolean specialArrowIV = false;
            if ((mounted.getLinked() != null) && (mounted.getLinked().getType() instanceof AmmoType)) {
                AmmoType ammoType = (AmmoType) mounted.getLinked().getType();
                specialArrowIV = (ammoType.is(AmmoType.T_ARROW_IV)
                        && (ammoType.getMunitionType().contains(AmmoType.Munitions.M_ADA)
                                || ammoType.getMunitionType().contains(AmmoType.Munitions.M_HOMING)));
                int attackingBA = (entity instanceof BattleArmor) ? ((BattleArmor) entity).getShootingStrength() : -1;
                falloff = AreaEffectHelper.calculateDamageFallOff(ammoType, attackingBA, false).falloff;
            }
            if (!specialArrowIV) {
                artyDamage -= falloff;
                while ((artyDamage > 0) && (falloff > 0)) {
                    damage.append('/').append(artyDamage);
                    artyDamage -= falloff;
                }
            }
            wDamR.setText(damage.toString());
        } else if (wtype.hasFlag(WeaponType.F_ENERGY)
                && mounted.hasModes()
                && (unitDisplay.getClientGUI() != null)
                && unitDisplay.getClientGUI().getClient().getGame().getOptions().booleanOption(
                        OptionsConstants.ADVCOMBAT_TACOPS_ENERGY_WEAPONS)) {
            int damage = Compute.dialDownDamage(mounted, wtype);
            if (mounted.hasChargedCapacitor() != 0) {
                damage += mounted.hasChargedCapacitor() * 5;
            }
            damage = Math.max(0, damage + damageModification(mounted));
            wDamR.setText(Integer.toString(damage));
        } else {
            int damage = Math.max(0, wtype.getDamage() + damageModification(mounted));
            wDamR.setText(Integer.toString(damage));
        }

        // update range
        int shortR = wtype.getShortRange();
        int mediumR = wtype.getMediumRange();
        int longR = wtype.getLongRange();
        int extremeR = wtype.getExtremeRange();
        if (mounted.isInBearingsOnlyMode()) {
            extremeR = RangeType.RANGE_BEARINGS_ONLY_OUT;
        }
        // Show water ranges for submerged weapons and those that have only water ranges
        // (torpedoes)
        if ((entity.getLocationStatus(mounted.getLocation()) == ILocationExposureStatus.WET)
                || ((longR == 0) && wtype.getWLongRange() > 0)) {
            shortR = wtype.getWShortRange();
            mediumR = wtype.getWMediumRange();
            longR = wtype.getWLongRange();
            extremeR = wtype.getWExtremeRange();
        } else if (wtype.hasFlag(WeaponType.F_PDBAY)) {
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
        if (wtype.hasFlag(WeaponType.F_CWS)) {
            Entity target = null;
            if ((unitDisplay.getClientGUI() != null)
                    && (unitDisplay.getClientGUI().getCurrentPanel() instanceof FiringDisplay)) {
                Targetable t = ((FiringDisplay) unitDisplay.getClientGUI().getCurrentPanel()).getTarget();
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
        if (wtype.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(wtype.getMinimumRange()));
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
            if (wtype.isCapital()) {
                wDamR.setText(Messages.getString("MekDisplay.CapitalD"));
            } else {
                wDamR.setText(Messages.getString("MekDisplay.StandardD"));
            }

            // if this is a weapons bay, then I need to compile it to get
            // accurate results
            if (wtype instanceof BayWeapon) {
                compileWeaponBay(mounted, mAmmo, wtype.isCapital());
            } else {
                // otherwise I need to replace range display with standard
                // ranges and attack values
                updateAttackValues(mounted, mAmmo);
            }

        }

        // update weapon bay selector
        int chosen = m_chBayWeapon.getSelectedIndex();
        m_chBayWeapon.removeAllItems();
        if (!(wtype instanceof BayWeapon) || !entity.usesWeaponBays()) {
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
        WeaponMounted oldmount = mounted;
        if (wtype instanceof BayWeapon) {
            int n = m_chBayWeapon.getSelectedIndex();
            if (n == -1) {
                n = 0;
            }
            mounted = mounted.getBayWeapon(n);
            wtype = mounted.getType();
        }

        if (wtype.getAmmoType() == AmmoType.T_NA) {
            m_chAmmo.setEnabled(false);
        } else if (wtype.hasFlag(WeaponType.F_DOUBLE_ONESHOT)
                || (entity.isSupportVehicle() && (wtype.getAmmoType() == AmmoType.T_INFANTRY))) {
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

        } else if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
            // this is the situation where there's some kind of ammo but it's not changeable
            m_chAmmo.setEnabled(false);
            Mounted<?> mountedAmmo = mounted.getLinked();
            if (mountedAmmo != null) {
                m_chAmmo.addItem(formatAmmo(mountedAmmo));
            }

        } else {
            m_chAmmo.setEnabled(true);
            vAmmo = new ArrayList<>();
            // Ammo sharing between adjacent trailers
            List<AmmoMounted> fullAmmoList = new ArrayList<>(entity.getAmmo());
            if (entity.getTowedBy() != Entity.NONE) {
                Entity ahead = entity.getGame().getEntity(entity.getTowedBy());
                fullAmmoList.addAll(ahead.getAmmo());
            }
            if (entity.getTowing() != Entity.NONE) {
                Entity behind = entity.getGame().getEntity(entity.getTowing());
                fullAmmoList.addAll(behind.getAmmo());
            }
            int newSelectedIndex = -1;
            int i = 0;
            for (AmmoMounted ammo : fullAmmoList) {
                AmmoType atype = ammo.getType();
                // for all aero units other than fighters, ammo must be located in the same place to be usable
                boolean sameLocationIfLargeAero = true;
                if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
                    sameLocationIfLargeAero = (mounted.getLocation() == ammo.getLocation());
                }
                boolean rightBay = true;
                if (entity.usesWeaponBays() && !(entity instanceof FighterSquadron)) {
                    rightBay = oldmount.ammoInBay(entity.getEquipmentNum(ammo));
                }

                // covers the situation where a weapon using non-caseless ammo should
                // not be able to switch to caseless on the fly and vice versa
                boolean canSwitchToAmmo = AmmoType.canSwitchToAmmo(mounted, atype);

                if (ammo.isAmmoUsable() && sameLocationIfLargeAero && rightBay && canSwitchToAmmo
                        && (atype.getAmmoType() == wtype.getAmmoType())
                        && (atype.getRackSize() == wtype.getRackSize())) {

                    vAmmo.add(ammo);
                    m_chAmmo.addItem(formatAmmo(ammo));
                    if (currentAmmoSelectionIndex != -1) {
                        newSelectedIndex = currentAmmoSelectionIndex;
                    } else if ((mounted.getLinked() != null) && mounted.getLinked().equals(ammo)) {
                        newSelectedIndex = i;
                    }
                    i++;
                }
            }
            if ((newSelectedIndex != -1) && (newSelectedIndex < m_chAmmo.getItemCount())) {
                m_chAmmo.setSelectedIndex(newSelectedIndex);
            }
        }

        // send event to other parts of the UI which care
        unitDisplay.getClientGUI().showSensorRanges(entity);
        unitDisplay.processMekDisplayEvent(new MekDisplayEvent(this, entity, mounted));
        onResize();
        addListeners();
    }

    private String formatAmmo(Mounted<?> m) {
        StringBuffer sb = new StringBuffer(64);
        int ammoIndex = m.getDesc().indexOf(Messages.getString("MekDisplay.0"));
        int loc = m.getLocation();
        if (!m.getEntity().equals(entity)) {
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

    private int damageModification(WeaponMounted weapon) {
        return weapon.getModifiers().stream()
            .filter(m -> m instanceof DamageModifier)
            .map(m -> (DamageModifier) m)
            .mapToInt(DamageModifier::getDeltaDamage)
            .sum();
    }

    private String formatBayWeapon(WeaponMounted m) {
        StringBuffer sb = new StringBuffer(64);
        sb.append(m.getDesc());
        return sb.toString();
    }

    /**
     * Update the range display for the selected ammo.
     *
     * @param mAmmo - the <code>AmmoType</code> of the weapon's loaded ammo.
     */
    private void updateRangeDisplayForAmmo(AmmoMounted mAmmo) {
        AmmoType atype = mAmmo.getType();
        // Only override the display for the various ATM and MML ammos
        if (atype.getAmmoType() == AmmoType.T_ATM) {
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                wMinR.setText("4");
                wShortR.setText("1 - 9");
                wMedR.setText("10 - 18");
                wLongR.setText("19 - 27");
                wExtR.setText("28 - 36");
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
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
        } else if (atype.getAmmoType() == AmmoType.T_MML) {
            if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
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
                if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
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
        } else if (atype.getAmmoType() == AmmoType.T_IATM) {
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                wMinR.setText("4");
                wShortR.setText("1 - 9");
                wMedR.setText("10 - 18");
                wLongR.setText("19 - 27");
                wExtR.setText("28 - 36");
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
                wMinR.setText("---");
                wShortR.setText("1 - 3");
                wMedR.setText("4 - 6");
                wLongR.setText("7 - 9");
                wExtR.setText("10 - 12");
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_IATM_IIW)) {
                wMinR.setText("4");
                wShortR.setText("1 - 5");
                wMedR.setText("6 - 10");
                wLongR.setText("11 - 15");
                wExtR.setText("16 - 20");
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_IATM_IMP)) {
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
        } else if ((atype.getAmmoType() == AmmoType.T_LRM)
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE))) {
            wMinR.setText("4");
            wShortR.setText("1 - 5");
            wMedR.setText("6 - 10");
            wLongR.setText("11 - 15");
            wExtR.setText("16 - 20");
        } else if ((atype.getAmmoType() == AmmoType.T_SRM)
                && (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE))) {
            wMinR.setText("---");
            wShortR.setText("1 - 2");
            wMedR.setText("3 - 4");
            wLongR.setText("5 - 6");
            wExtR.setText("7 - 8");
        } else if (atype.getAmmoType() == AmmoType.T_ARROW_IV) {
            // Special casing for ADA ranges
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ADA)) {
                wMinR.setText("---");
                wShortR.setText("1 - 17 [0]");
                wMedR.setText("18 - 34 [1]");
                wLongR.setText("35 - 51 [2]");
                wExtR.setText("---");
            }

        }

        // Min range 0 for hotload
        if (mAmmo.isHotLoaded()) {
            wMinR.setText("---");
        }

        onResize();
    } // End private void updateRangeDisplayForAmmo( AmmoType )

    private void updateAttackValues(WeaponMounted weapon, AmmoMounted ammo) {
        WeaponType wtype = weapon.getType();
        // update Attack Values and change range
        int avShort = wtype.getRoundShortAV();
        int avMed = wtype.getRoundMedAV();
        int avLong = wtype.getRoundLongAV();
        int avExt = wtype.getRoundExtAV();
        int maxr = wtype.getMaxRange(weapon);

        // change range and attack values based upon ammo
        if (null != ammo) {
            AmmoType atype = ammo.getType();
            double[] changes = changeAttackValues(atype, avShort, avMed,
                    avLong, avExt, maxr);
            avShort = (int) changes[0];
            avMed = (int) changes[1];
            avLong = (int) changes[2];
            avExt = (int) changes[3];
            maxr = (int) changes[4];
        }

        if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && wtype.isCapital()) {
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
        if (wtype.isCapital()) {
            wShortR.setText("1-12");
        } else if (wtype.hasFlag(WeaponType.F_PDBAY)) {
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
        if (maxr > WeaponType.RANGE_SHORT) {
            wMedAVR.setText(Integer.toString(avMed));
            if (wtype.isCapital()) {
                wMedR.setText("13-24");
            } else {
                wMedR.setText("7-12");
            }
        }
        if (maxr > WeaponType.RANGE_MED) {
            wLongAVR.setText(Integer.toString(avLong));
            if (wtype.isCapital()) {
                wLongR.setText("25-40");
            } else {
                wLongR.setText("13-20");
            }
        }
        if (maxr > WeaponType.RANGE_LONG) {
            wExtAVR.setText(Integer.toString(avExt));
            if (wtype.isCapital()) {
                wExtR.setText("41-50");
            } else {
                wExtR.setText("21-25");
            }
        }

        onResize();
    }

    private double[] changeAttackValues(AmmoType atype, double avShort,
            double avMed, double avLong, double avExt, int maxr) {

        if (AmmoType.T_ATM == atype.getAmmoType()) {
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_EXTENDED_RANGE)) {
                maxr = WeaponType.RANGE_EXT;
                avShort = avShort / 2;
                avMed = avMed / 2;
                avLong = avMed;
                avExt = avMed;
            } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_HIGH_EXPLOSIVE)) {
                maxr = WeaponType.RANGE_SHORT;
                avShort = avShort + (avShort / 2);
                avMed = 0;
                avLong = 0;
                avExt = 0;
            }
        } // End weapon-is-ATM
        else if (atype.getAmmoType() == AmmoType.T_MML) {
            // first check for artemis
            int bonus = 0;
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
                int rack = atype.getRackSize();
                if (rack == 5) {
                    bonus += 1;
                } else if (rack >= 7) {
                    bonus += 2;
                }
                avShort = avShort + bonus;
                avMed = avMed + bonus;
                avLong = avLong + bonus;
            }
            if (!atype.hasFlag(AmmoType.F_MML_LRM)) {
                maxr = WeaponType.RANGE_SHORT;
                avShort = avShort * 2;
                avMed = 0;
                avLong = 0;
                avExt = 0;
            }
        } // end weapon is MML
        else if ((atype.getAmmoType() == AmmoType.T_LRM)
                || (atype.getAmmoType() == AmmoType.T_LRM_IMP)
                || (atype.getAmmoType() == AmmoType.T_SRM)
                || (atype.getAmmoType() == AmmoType.T_SRM_IMP)) {

            if (atype.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) {
                if ((atype.getAmmoType() == AmmoType.T_LRM) || (atype.getAmmoType() == AmmoType.T_LRM_IMP)) {
                    int bonus = (int) Math.ceil(atype.getRackSize() / 5.0);
                    avShort = avShort + bonus;
                    avMed = avMed + bonus;
                    avLong = avLong + bonus;
                }
                if ((atype.getAmmoType() == AmmoType.T_SRM) || (atype.getAmmoType() == AmmoType.T_SRM_IMP)) {
                    avShort = avShort + 2;
                }
            }
        } else if (atype.getAmmoType() == AmmoType.T_AC_LBX) {
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
                int newAV = (int) Math.floor(0.6 * atype.getRackSize());
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
        } else if (atype.getAmmoType() == AmmoType.T_AR10) {
            if (atype.hasFlag(AmmoType.F_AR10_KILLER_WHALE)) {
                avShort = 4;
                avMed = 4;
                avLong = 4;
                avExt = 4;
            } else if (atype.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                avShort = 3;
                avMed = 3;
                avLong = 3;
                avExt = 3;
            } else if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
                avShort = 100;
                avMed = 100;
                avLong = 100;
                avExt = 100;
            } else if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
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
        } else if (atype.getAmmoType() == AmmoType.T_KILLER_WHALE) {
            if (atype.hasFlag(AmmoType.F_PEACEMAKER)) {
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
        } else if (atype.getAmmoType() == AmmoType.T_WHITE_SHARK) {
            if (atype.hasFlag(AmmoType.F_SANTA_ANNA)) {
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

        double[] result = { avShort, avMed, avLong, avExt, maxr };
        return result;

    }

    private void compileWeaponBay(WeaponMounted weapon, AmmoMounted mAmmo, boolean isCapital) {

        List<WeaponMounted> bayWeapons = weapon.getBayWeapons();
        WeaponType wtype = weapon.getType();

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
        int maxr = WeaponType.RANGE_SHORT;

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
                    double[] changes = changeAttackValues((AmmoType) mAmmo.getType(), mAVShort, mAVMed,
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
                if (mMaxR > maxr) {
                    maxr = mMaxR;
                }
            }
        }
        // check for bracketing
        double mult = 1.0;
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 80%")) {
            mult = 0.8;
        }
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 60%")) {
            mult = 0.6;
        }
        if (weapon.hasModes() && weapon.curMode().equals("Bracket 40%")) {
            mult = 0.4;
        }
        avShort = mult * avShort;
        avMed = mult * avMed;
        avLong = mult * avLong;
        avExt = mult * avExt;

        if (entity.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AERO_SANITY)
                && wtype.isCapital()) {
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
        if (maxr > WeaponType.RANGE_SHORT) {
            wMedAVR.setText(Integer.toString((int) Math.ceil(avMed)));
            if (isCapital) {
                wMedR.setText("13-24");
            } else {
                wMedR.setText("7-12");
            }
        }
        if (maxr > WeaponType.RANGE_MED) {
            wLongAVR.setText(Integer.toString((int) Math.ceil(avLong)));
            if (isCapital) {
                wLongR.setText("25-40");
            } else {
                wLongR.setText("13-20");
            }
        }
        if (maxr > WeaponType.RANGE_LONG) {
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
            if (unitDisplay.getClientGUI() == null) {
                return;
            }

            JComponent currPanel = unitDisplay.getClientGUI().getCurrentPanel();
            // When in the Firing Phase, update the targeting information.
            if (currPanel instanceof FiringDisplay) {
                FiringDisplay firingDisplay = (FiringDisplay) currPanel;

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
                        unitDisplay.getClientGUI().getBoardView()
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
                unitDisplay.getClientGUI().clearTemporarySprites();
            }
        }
        onResize();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        ClientGUI clientgui = unitDisplay.getClientGUI();
        if (ev.getSource().equals(m_chAmmo)
                && (m_chAmmo.getSelectedIndex() != -1)
                && (clientgui != null)) {
            int n = weaponList.getSelectedIndex();
            if (n == -1) {
                return;
            }

            // We can update display values without changing the selected unit's ammo;
            // this allows displaying selected unit's ammo-based ranges without owning it
            WeaponMounted mWeap = ((WeaponListModel) weaponList.getModel()).getWeaponAt(n);
            WeaponMounted oldWeap = mWeap;
            AmmoMounted oldAmmo = mWeap.getLinkedAmmo();
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
            if (mWeap.getType() instanceof BayWeapon) {
                isBay = true;
                n = m_chBayWeapon.getSelectedIndex();
                if (n == -1) {
                    return;
                }
                //
                WeaponMounted sWeap = mWeap.getBayWeapon(n);
                // cycle through all weapons of the same type and load with
                // this ammo
                if (sWeap != null) {
                    for (WeaponMounted bWeap : mWeap.getBayWeapons()) {
                        // FIXME: Consider new AmmoType::equals / BombType::equals
                        if (bWeap.getType().equals(sWeap.getType())) {
                            entity.loadWeapon(bWeap, mAmmo);
                            // Alert the server of the update.
                            clientgui.getClient().sendAmmoChange(
                                    entity.getId(),
                                    entity.getEquipmentNum(bWeap),
                                    entity.getEquipmentNum(mAmmo),
                                    0);
                        }
                    }
                }
            } else {
                entity.loadWeapon(mWeap, mAmmo);
                // Alert the server of the update.
                clientgui.getClient().sendAmmoChange(entity.getId(),
                        entity.getEquipmentNum(mWeap),
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
                WeaponType wtype = (WeaponType) mWeap.getType();
                if (isBay) {
                    compileWeaponBay(oldWeap, mAmmo, wtype.isCapital());
                } else {
                    // otherwise I need to replace range display with
                    // standard ranges and attack values
                    updateAttackValues(mWeap, mAmmo);
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
            if (entity.getOwner().equals(unitDisplay.getClientGUI().getClient().getLocalPlayer())) {
                unitDisplay.getClientGUI().getClient().sendEntityWeaponOrderUpdate(entity);
            }
        }
        onResize();
    }

    void setWeaponComparator(final @Nullable WeaponSortOrder weaponSortOrder) {
        if (weaponSortOrder == null) {
            return;
        }

        entity.setWeaponSortOrder(weaponSortOrder);
        ((WeaponListModel) weaponList.getModel()).sort(weaponSortOrder.getWeaponSortComparator(entity));
    }

    private void addListeners() {
        comboWeaponSortOrder.addActionListener(this);
        m_chAmmo.addActionListener(this);
        m_chBayWeapon.addActionListener(this);
        weaponList.addListSelectionListener(this);
    }

    private void removeListeners() {
        comboWeaponSortOrder.removeActionListener(this);
        m_chAmmo.removeActionListener(this);
        m_chBayWeapon.removeActionListener(this);
        weaponList.removeListSelectionListener(this);
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
     * Updates the Weapon Panel with the information for the given entity. If the
     * given entity
     * is `null`, this method will do nothing.
     *
     * @param entity - The weapon panel will update info based on the {@link Entity}
     *               provided.
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
