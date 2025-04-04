/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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
 */
package megamek.client.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.panels.APWeaponChoicePanel;
import megamek.client.ui.swing.panels.InfantryArmorPanel;
import megamek.client.ui.swing.panels.MEAChoicePanel;
import megamek.client.ui.swing.panels.MineChoicePanel;
import megamek.client.ui.swing.panels.MunitionChoicePanel;
import megamek.client.ui.swing.panels.RapidFireMGPanel;
import megamek.client.ui.swing.panels.WeaponAmmoChoicePanel;
import megamek.common.*;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This class builds the Equipment Panel for use in MegaMek and MekHQ
 *
 * @author Dylan Myers (ralgith-erian@users.sourceforge.net)
 * @author arlith
 * @since 2012-05-20
 */
public class EquipChoicePanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 672299770230285567L;

    private final Entity entity;
    private final List<MunitionChoicePanel> m_vMunitions = new ArrayList<>();
    private final List<WeaponAmmoChoicePanel> m_vWeaponAmmoChoice = new ArrayList<>();
    /**
     * An <code>ArrayList</code> to keep track of all of the
     * <code>APWeaponChoicePanels</code> that were added, so we can apply
     * their choices when the dialog is closed.
     */
    private final ArrayList<APWeaponChoicePanel> m_vAPMounts = new ArrayList<>();
    /**
     * An <code>ArrayList</code> to keep track of all of the
     * <code>MEAChoicePanels</code> that were added, so we can apply
     * their choices when the dialog is closed.
     */
    private final ArrayList<MEAChoicePanel> m_vMEAdaptors = new ArrayList<>();
    /**
     * Panel for adding components related to selecting which anti-personnel weapons are mounted in an AP Mount (armored
     * gloves are also considered AP mounts)
     **/
    private final JPanel panAPMounts = new JPanel();
    private final JPanel panMEAdaptors = new JPanel();
    private final JPanel panWeaponAmmoSelector = new JPanel();
    private final ArrayList<RapidFireMGPanel> m_vMGs = new ArrayList<>();
    private final JPanel panRapidFireMGs = new JPanel();
    private final ArrayList<MineChoicePanel> m_vMines = new ArrayList<>();
    private final JPanel panMines = new JPanel();
    private final JPanel panBombs = new JPanel();
    private final JCheckBox chAutoEject = new JCheckBox();
    private final JCheckBox chCondEjectAmmo = new JCheckBox();
    private final JCheckBox chCondEjectEngine = new JCheckBox();
    private final JCheckBox chCondEjectCTDest = new JCheckBox();
    private final JCheckBox chCondEjectHeadshot = new JCheckBox();
    private final JCheckBox chCondEjectFuel = new JCheckBox();
    private final JCheckBox chCondEjectSIDest = new JCheckBox();
    private final JCheckBox chSearchlight = new JCheckBox();
    private final JComboBox<String> choC3 = new JComboBox<>();
    ClientGUI clientgui;
    Client client;
    private int[] entityCorrespondence;
    private JPanel panMunitions = new JPanel();
    private InfantryArmorPanel panInfArmor;
    private BombChoicePanel m_bombs;

    public EquipChoicePanel(Entity entity, ClientGUI clientgui, Client client) {
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;

        GridBagLayout g = new GridBagLayout();
        setLayout(g);

        // **EQUIPMENT TAB**//
        // Auto-eject checkbox and conditional ejections.
        JLabel labAutoEject = new JLabel(Messages.getString("CustomMekDialog.labAutoEject"), SwingConstants.RIGHT);
        JLabel labCondEjectAmmo = new JLabel(Messages.getString("CustomMekDialog.labConditional_Ejection_Ammo"),
              SwingConstants.RIGHT);
        if (entity instanceof Mek mek) {
            if (mek.hasEjectSeat()) {
                add(labAutoEject, GBC.std());
                add(chAutoEject, GBC.eol());
                chAutoEject.setSelected(!mek.isAutoEject());
            }

            // Conditional Ejections
            if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                      mek.hasEjectSeat()) {
                add(labCondEjectAmmo, GBC.std());
                add(chCondEjectAmmo, GBC.eol());
                chCondEjectAmmo.setSelected(mek.isCondEjectAmmo());
                JLabel labCondEjectEngine = new JLabel(Messages.getString(
                      "CustomMekDialog.labConditional_Ejection_Engine"), SwingConstants.RIGHT);
                add(labCondEjectEngine, GBC.std());
                add(chCondEjectEngine, GBC.eol());
                chCondEjectEngine.setSelected(mek.isCondEjectEngine());
                JLabel labCondEjectCTDest = new JLabel(Messages.getString(
                      "CustomMekDialog.labConditional_Ejection_CT_Destroyed"), SwingConstants.RIGHT);
                add(labCondEjectCTDest, GBC.std());
                add(chCondEjectCTDest, GBC.eol());
                chCondEjectCTDest.setSelected(mek.isCondEjectCTDest());
                JLabel labCondEjectHeadshot = new JLabel(Messages.getString(
                      "CustomMekDialog.labConditional_Ejection_Headshot"), SwingConstants.RIGHT);
                add(labCondEjectHeadshot, GBC.std());
                add(chCondEjectHeadshot, GBC.eol());
                chCondEjectHeadshot.setSelected(mek.isCondEjectHeadshot());
            }
        } else if (entity.isFighter()) {
            Aero aero = (Aero) entity;

            // Ejection Seat
            if (aero.hasEjectSeat()) {
                add(labAutoEject, GBC.std());
                add(chAutoEject, GBC.eol());
                chAutoEject.setSelected(!aero.isAutoEject());
            }

            // Conditional Ejections
            if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) &&
                      aero.hasEjectSeat()) {
                add(labCondEjectAmmo, GBC.std());
                add(chCondEjectAmmo, GBC.eol());
                chCondEjectAmmo.setSelected(aero.isCondEjectAmmo());
                JLabel labCondEjectFuel = new JLabel(Messages.getString("CustomMekDialog.labConditional_Ejection_Fuel"),
                      SwingConstants.RIGHT);
                add(labCondEjectFuel, GBC.std());
                add(chCondEjectFuel, GBC.eol());
                chCondEjectFuel.setSelected(aero.isCondEjectFuel());
                JLabel labCondEjectSIDest = new JLabel(Messages.getString(
                      "CustomMekDialog.labConditional_Ejection_SI_Destroyed"), SwingConstants.RIGHT);
                add(labCondEjectSIDest, GBC.std());
                add(chCondEjectSIDest, GBC.eol());
                chCondEjectSIDest.setSelected(aero.isCondEjectSIDest());
            }
        }

        if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3()) {
            JLabel labC3 = new JLabel(Messages.getString("CustomMekDialog.labC3"), SwingConstants.RIGHT);
            add(labC3, GBC.std());
            add(choC3, GBC.eol());
            refreshC3();
        }

        // Setup AP mounts
        if ((entity instanceof BattleArmor) && entity.hasWorkingMisc(MiscType.F_AP_MOUNT)) {
            setupAPMounts();
            panAPMounts.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                  Messages.getString("CustomMekDialog.APMountPanelTitle"),
                  TitledBorder.TOP,
                  TitledBorder.DEFAULT_POSITION));

            add(panAPMounts, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        if ((entity instanceof BattleArmor battleArmor) && entity.hasWorkingMisc(MiscType.F_BA_MEA)) {
            panMEAdaptors.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                  Messages.getString("CustomMekDialog.MEAPanelTitle"),
                  TitledBorder.TOP,
                  TitledBorder.DEFAULT_POSITION));
            // We need to determine how much weight is free, so the user can pick legal combinations of manipulators
            EntityVerifier verifier = EntityVerifier.getInstance(new MegaMekFile(Configuration.unitsDir(),
                  EntityVerifier.CONFIG_FILENAME).getFile());
            TestBattleArmor testBA = new TestBattleArmor(battleArmor, verifier.baOption, null);
            double maxTrooperWeight = 0;
            for (int i = 1; i < battleArmor.getTroopers(); i++) {
                double trooperWeight = testBA.calculateWeight(i);
                if (trooperWeight > maxTrooperWeight) {
                    maxTrooperWeight = trooperWeight;
                }
            }
            String freeWeight = Messages.getString("CustomMekDialog.freeWeight") +
                                      String.format(": %1$.3f/%2$.3f",
                                            maxTrooperWeight,
                                            battleArmor.getTrooperWeight());

            setupMEAdaptors(freeWeight);
            add(panMEAdaptors, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Can't set up munitions on infantry.
        if (!((entity instanceof Infantry) && !((Infantry) entity).hasFieldWeapon()) ||
                  (entity instanceof BattleArmor)) {
            setupMunitions();
            panMunitions.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                  Messages.getString("CustomMekDialog.MunitionsPanelTitle"),
                  TitledBorder.TOP,
                  TitledBorder.DEFAULT_POSITION));
            add(panMunitions, GBC.eop().anchor(GridBagConstraints.CENTER));

            setupWeaponAmmoChoice();
            panWeaponAmmoSelector.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                  Messages.getString("CustomMekDialog.WeaponSelectionTitle"),
                  TitledBorder.TOP,
                  TitledBorder.DEFAULT_POSITION));
            add(panWeaponAmmoSelector, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        if (entity.isBomber()) {
            setupBombs();
            add(panBombs, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Set up rapid fire mg; per errata infantry of any kind cannot use them
        if (clientgui.getClient().getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_BURST) &&
                  !(entity instanceof Infantry)) {
            setupRapidFireMGs();
            add(panRapidFireMGs, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // set up infantry armor
        if (entity.isConventionalInfantry()) {
            panInfArmor = new InfantryArmorPanel(entity);
            add(panInfArmor, GBC.eop().anchor(GridBagConstraints.CENTER));
        }

        // Set up searchlight
        if (!entity.getsAutoExternalSearchlight() &&
                  client.getGame().getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            JLabel labSearchlight = new JLabel(Messages.getString("CustomMekDialog.labSearchlight"),
                  SwingConstants.RIGHT);
            add(labSearchlight, GBC.std());
            add(chSearchlight, GBC.eol());
            chSearchlight.setSelected(entity.hasSearchlight() ||
                                            entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
            chSearchlight.setEnabled(!entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
        }

        // Set up mines
        setupMines();
        add(panMines, GBC.eop().anchor(GridBagConstraints.CENTER));
    }

    private void refreshC3() {
        choC3.removeAllItems();
        int listIndex = 0;
        entityCorrespondence = new int[client.getGame().getNoOfEntities() + 2];

        if (entity.hasC3i() || entity.hasNavalC3()) {
            choC3.addItem(Messages.getString("CustomMekDialog.CreateNewNetwork"));
            if (entity.getC3Master() == null) {
                choC3.setSelectedIndex(listIndex);
            }
            entityCorrespondence[listIndex++] = entity.getId();
        } else if (entity.hasC3MM()) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3.addItem(Messages.getString("CustomMekDialog.setCompanyMaster", mNodes, sNodes));

            listIndex = getListIndex(listIndex, sNodes);

        } else if (entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3.addItem(Messages.getString("CustomMekDialog.setCompanyMaster1", nodes));
            listIndex = getListIndex(listIndex, nodes);

        }
        for (Entity e : client.getEntitiesVector()) {
            // ignore enemies or self
            if (entity.isEnemyOf(e) || entity.equals(e)) {
                continue;
            }
            // c3i only links with c3i
            if (entity.hasC3i() != e.hasC3i()) {
                continue;
            }
            // NC3 only links with NC3
            if (entity.hasNavalC3() != e.hasNavalC3()) {
                continue;
            }
            // likewise can't connect c3 to nova
            if (entity.hasNovaCEWS() != e.hasNovaCEWS()) {
                continue;
            }
            // maximum depth of a c3 network is 2 levels.
            Entity eCompanyMaster = e.getC3Master();
            if ((eCompanyMaster != null) && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
                continue;
            }
            int nodes = e.calculateFreeC3Nodes();
            if (e.hasC3MM() && entity.hasC3M() && e.C3MasterIs(e)) {
                nodes = e.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(e) && !entity.equals(e)) {
                nodes++;
            }
            if ((entity.hasC3i() || entity.hasNavalC3()) && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            if (e.hasC3i() || e.hasNavalC3()) {
                if (entity.onSameC3NetworkAs(e)) {
                    choC3.addItem(Messages.getString("CustomMekDialog.join1",
                          e.getDisplayName(),
                          e.getC3NetId(),
                          nodes - 1));
                    choC3.setSelectedIndex(listIndex);
                } else {
                    choC3.addItem(Messages.getString("CustomMekDialog.join2",
                          e.getDisplayName(),
                          e.getC3NetId(),
                          nodes));
                }
                entityCorrespondence[listIndex++] = e.getId();
            } else if (e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have *both* sub-masters AND slave units.
                choC3.addItem(Messages.getString("CustomMekDialog.connect2",
                      e.getDisplayName(),
                      e.getC3NetId(),
                      nodes));
                entityCorrespondence[listIndex] = e.getId();
                if (entity.C3MasterIs(e)) {
                    choC3.setSelectedIndex(listIndex);
                }
                listIndex++;
            } else if (e.C3MasterIs(e) != entity.hasC3M()) {
                // If we're a slave-unit, we can only connect to sub-masters, not main masters likewise, if we're a
                // master unit, we can only connect to main master units, not sub-masters.
            } else if (entity.C3MasterIs(e)) {
                choC3.addItem(Messages.getString("CustomMekDialog.connect1",
                      e.getDisplayName(),
                      e.getC3NetId(),
                      nodes - 1));
                choC3.setSelectedIndex(listIndex);
                entityCorrespondence[listIndex++] = e.getId();
            } else {
                choC3.addItem(Messages.getString("CustomMekDialog.connect2",
                      e.getDisplayName(),
                      e.getC3NetId(),
                      nodes));
                entityCorrespondence[listIndex++] = e.getId();
            }
        }
    }

    /**
     * Set up the layout of <code>panAPMounts</code>, which contains components for selecting which anti-personnel
     * weapons are mounted in an AP mount.
     */
    private void setupAPMounts() {
        GridBagLayout gbl = new GridBagLayout();
        panAPMounts.setLayout(gbl);

        // Weapons that can be used in an AP Mount
        ArrayList<WeaponType> apWeaponTypes = new ArrayList<>(100);
        // Weapons that can be used in an Armored Glove
        ArrayList<WeaponType> agWeaponTypes = new ArrayList<>(100);
        Enumeration<EquipmentType> allTypes = EquipmentType.getAllTypes();
        int gameYear = clientgui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        SimpleTechLevel legalLevel = SimpleTechLevel.getGameTechLevel(clientgui.getClient().getGame());
        while (allTypes.hasMoreElements()) {
            EquipmentType eq = allTypes.nextElement();

            // If it's not an infantry weapon, we don't care
            if (!(eq instanceof InfantryWeapon infantryWeapon)) {
                continue;
            }

            // Check to see if the tech level of the equipment is legal
            if (!eq.isLegal(gameYear,
                  legalLevel,
                  entity.isClan(),
                  entity.isMixedTech(),
                  entity.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
                continue;
            }

            // Check to see if we've got a valid infantry weapon
            if (infantryWeapon.hasFlag(WeaponType.F_INFANTRY) &&
                      !infantryWeapon.hasFlag(WeaponType.F_INF_POINT_BLANK) &&
                      !infantryWeapon.hasFlag(WeaponType.F_INF_ARCHAIC) &&
                      !infantryWeapon.hasFlag(WeaponType.F_INF_SUPPORT)) {
                apWeaponTypes.add(infantryWeapon);
            }
            if (infantryWeapon.hasFlag(WeaponType.F_INFANTRY) &&
                      !infantryWeapon.hasFlag(WeaponType.F_INF_POINT_BLANK) &&
                      !infantryWeapon.hasFlag(WeaponType.F_INF_ARCHAIC) &&
                      (infantryWeapon.getCrew() < 2)) {
                agWeaponTypes.add(infantryWeapon);
            }
        }
        apWeaponTypes.sort(Comparator.comparing(EquipmentType::getName));
        agWeaponTypes.sort(Comparator.comparing(EquipmentType::getName));

        ArrayList<Mounted<?>> armoredGloves = new ArrayList<>(2);
        for (Mounted<?> m : entity.getMisc()) {
            if (!m.getType().hasFlag(MiscType.F_AP_MOUNT)) {
                continue;
            }
            APWeaponChoicePanel apWeaponChoicePanel = null;
            // Armored gloves need to be treated slightly differently, since
            // 1 or 2 armored gloves allow 1 additional AP weapon
            if (m.getType().hasFlag(MiscType.F_ARMORED_GLOVE)) {
                armoredGloves.add(m);
            } else {
                apWeaponChoicePanel = new APWeaponChoicePanel(entity, m, apWeaponTypes);
            }
            if (apWeaponChoicePanel != null) {
                panAPMounts.add(apWeaponChoicePanel, GBC.eol());
                m_vAPMounts.add(apWeaponChoicePanel);
            }
        }

        // If there is an armored glove with a weapon already mounted, we need to ensure that that glove is
        // displayed, and not the empty glove
        Mounted<?> aGlove = null;
        for (Mounted<?> ag : armoredGloves) {
            if (aGlove == null) {
                aGlove = ag;
            } else if ((aGlove.getLinked() == null) && (ag.getLinked() != null)) {
                aGlove = ag;
            }
            // If both are linked, TestBattleArmor will mark unit as invalid
        }
        if (aGlove != null) {
            APWeaponChoicePanel apWeaponChoicePanel = new APWeaponChoicePanel(entity, aGlove, agWeaponTypes);
            panAPMounts.add(apWeaponChoicePanel, GBC.eol());
            m_vAPMounts.add(apWeaponChoicePanel);
        }
    }

    /**
     * Set up the layout of <code>panMEAdaptors</code>, which contains components for selecting which manipulators are
     * mounted in a modular equipment adaptor
     */
    private void setupMEAdaptors(String freeWeight) {
        GridBagLayout gbl = new GridBagLayout();
        panMEAdaptors.setLayout(gbl);

        JLabel lblFreeWeight = new JLabel(freeWeight);
        panMEAdaptors.add(lblFreeWeight, GBC.eol().anchor(GridBagConstraints.CENTER));

        ArrayList<MiscType> manipulatorTypes = new ArrayList<>();

        for (String manipulatorTypeName : BattleArmor.MANIPULATOR_TYPE_STRINGS) {
            // Ignore the "None" option
            if (manipulatorTypeName.equals(BattleArmor.MANIPULATOR_TYPE_STRINGS[0])) {
                continue;
            }
            MiscType miscType = (MiscType) EquipmentType.get(manipulatorTypeName);
            manipulatorTypes.add(miscType);
        }

        for (Mounted<?> m : entity.getMisc()) {
            if (!m.getType().hasFlag(MiscType.F_BA_MEA)) {
                continue;
            }
            Mounted<?> currentManipulator;
            if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
                currentManipulator = ((BattleArmor) entity).getLeftManipulator();
            } else if (m.getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
                currentManipulator = ((BattleArmor) entity).getRightManipulator();
            } else {
                // We can only have MEA's in an arm
                continue;
            }
            MEAChoicePanel meaChoicePanel;
            meaChoicePanel = new MEAChoicePanel(entity, m.getBaMountLoc(), currentManipulator, manipulatorTypes);

            panMEAdaptors.add(meaChoicePanel, GBC.eol());
            m_vMEAdaptors.add(meaChoicePanel);
        }
    }

    private void setupMunitions() {
        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        Game game = clientgui.getClient().getGame();
        IGameOptions gameOpts = game.getOptions();
        int gameYear = gameOpts.intOption(OptionsConstants.ALLOWED_YEAR);

        if (entity.usesWeaponBays() || entity instanceof Dropship) {
            // Grounded dropships don't *use* weapon bays as such, but should load ammo as if they did
            panMunitions = new BayMunitionsChoicePanel(entity, game);
            return;
        }
        // Small support vehicle ammo is part of the weapon, and the only munitions choice is standard or inferno,
        // and only for some weapons.
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            panMunitions = new SmallSVMunitionsChoicePanel(entity);
            return;
        }
        panMunitions.setLayout(gbl);

        for (AmmoMounted m : entity.getAmmo()) {
            AmmoType at = m.getType();
            ArrayList<AmmoType> vTypes = new ArrayList<>();
            Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
            if (vAllTypes == null) {
                continue;
            }

            // don't allow ammo switching of most things for Aerospace allow only MML, ATM, and NARC. LRM/SRM can
            // switch between Artemis and standard, but not other munitions. Same with MRM.
            if ((entity instanceof Aero) &&
                      !((at.getAmmoType() == AmmoType.T_MML) ||
                              (at.getAmmoType() == AmmoType.T_SRM) ||
                              (at.getAmmoType() == AmmoType.T_LRM) ||
                              (at.getAmmoType() == AmmoType.T_MRM) ||
                              (at.getAmmoType() == AmmoType.T_ATM) ||
                              (at.getAmmoType() == AmmoType.T_IATM))) {
                continue;
            }

            for (AmmoType atCheck : vAllTypes) {
                if (entity.hasETypeFlag(Entity.ETYPE_AERO) &&
                          !atCheck.canAeroUse(game.getOptions()
                                                    .booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS))) {
                    continue;
                }
                SimpleTechLevel legalLevel = SimpleTechLevel.getGameTechLevel(game);
                boolean bTechMatch;
                if (game.getOptions().booleanOption(OptionsConstants.ALLOWED_ERA_BASED)) {
                    bTechMatch = atCheck.isLegal(gameYear,
                          legalLevel,
                          entity.isClan(),
                          entity.isMixedTech(),
                          game.getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT));
                } else {
                    bTechMatch = atCheck.getStaticTechLevel().ordinal() <= legalLevel.ordinal();
                }

                // If clan_ignore_eq_limits is unchecked, do NOT allow Clans to use IS-only ammo. "Incendiary"
                // munition type gets removed here for reasons unknown.
                EnumSet<AmmoType.Munitions> munitionsTypes = atCheck.getMunitionType();
                munitionsTypes.remove(AmmoType.Munitions.M_INCENDIARY_LRM);
                if (!gameOpts.booleanOption(OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS) &&
                          entity.isClan() &&
                          atCheck.notAllowedByClanRules()) {
                    bTechMatch = false;
                }

                if ((munitionsTypes.contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)) &&
                          !entity.hasWorkingMisc(MiscType.F_ARTEMIS) &&
                          !entity.hasWorkingMisc(MiscType.F_ARTEMIS_PROTO)) {
                    continue;
                }
                if ((munitionsTypes.contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)) &&
                          !entity.hasWorkingMisc(MiscType.F_ARTEMIS_V) &&
                          !entity.hasWorkingMisc(MiscType.F_ARTEMIS_PROTO)) {
                    continue;
                }

                if (!gameOpts.booleanOption(OptionsConstants.ADVANCED_MINEFIELDS) &&
                          AmmoType.canDeliverMinefield(atCheck)) {
                    continue;
                }

                // Only ProtoMeks can use Proto-specific ammo
                if (atCheck.hasFlag(AmmoType.F_PROTOMEK) && !(entity instanceof ProtoMek)) {
                    continue;
                }

                // When dealing with machine guns, ProtoMeks can only use proto-specific machine gun ammo
                if ((entity instanceof ProtoMek) &&
                          atCheck.hasFlag(AmmoType.F_MG) &&
                          !atCheck.hasFlag(AmmoType.F_PROTOMEK)) {
                    continue;
                }

                // Battle Armor ammo can't be selected at all. All other ammo types need to match on rack size and tech.
                if (bTechMatch &&
                          (atCheck.getRackSize() == at.getRackSize()) &&
                          (atCheck.hasFlag(AmmoType.F_BATTLEARMOR) == at.hasFlag(AmmoType.F_BATTLEARMOR)) &&
                          (atCheck.hasFlag(AmmoType.F_ENCUMBERING) == at.hasFlag(AmmoType.F_ENCUMBERING)) &&
                          (atCheck.getTonnage(entity) == at.getTonnage(entity))) {
                    vTypes.add(atCheck);
                }
            }
            if ((vTypes.isEmpty()) &&
                      !client.getGame().getOptions().booleanOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP) &&
                      !client.getGame().getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HOTLOAD)) {
                continue;
            }
            MunitionChoicePanel mcp;
            mcp = new MunitionChoicePanel(m, vTypes, m_vWeaponAmmoChoice, entity, clientgui);
            panMunitions.add(mcp, GBC.eol());
            m_vMunitions.add(mcp);
        }
    }

    /**
     * Worker function that creates a series of weapon ammo choice panels that allow the user to pick a particular ammo
     * bin for an ammo-using weapon with matching ammo.
     */
    private void setupWeaponAmmoChoice() {
        GridBagLayout gbl = new GridBagLayout();
        panWeaponAmmoSelector.setLayout(gbl);

        for (WeaponMounted weapon : entity.getWeaponList()) {
            // don't deal with bay or grouped weapons for now
            if (weapon.getType().getAmmoType() == AmmoType.T_NA) {
                continue;
            }

            WeaponAmmoChoicePanel ammoChoicePanel = new WeaponAmmoChoicePanel(weapon, entity);
            panWeaponAmmoSelector.add(ammoChoicePanel, GBC.eol());
            m_vWeaponAmmoChoice.add(ammoChoicePanel);
        }
    }

    private void setupBombs() {
        GridBagLayout gbl = new GridBagLayout();
        panBombs.setLayout(gbl);

        int techLevel = Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES,
              client.getGame().getOptions().stringOption(OptionsConstants.ALLOWED_TECHLEVEL));
        boolean allowNukes = client.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES);
        m_bombs = new BombChoicePanel((IBomber) entity, allowNukes, techLevel >= TechConstants.T_SIMPLE_ADVANCED);
        panBombs.add(m_bombs, GBC.std());
    }

    private void setupRapidFireMGs() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        panRapidFireMGs.setLayout(gridBagLayout);
        for (Mounted<?> mounted : entity.getWeaponList()) {
            WeaponType weaponType = (WeaponType) mounted.getType();

            if (!weaponType.hasFlag(WeaponType.F_MG)) {
                continue;
            }

            RapidFireMGPanel rapidFireMGPanel = new RapidFireMGPanel(mounted, entity);
            panRapidFireMGs.add(rapidFireMGPanel, GBC.eol());
            m_vMGs.add(rapidFireMGPanel);
        }
    }

    private void setupMines() {
        GridBagLayout gbl = new GridBagLayout();
        panMines.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (MiscMounted miscMounted : entity.getMisc()) {
            if (!miscMounted.getType().hasFlag((MiscType.F_MINE)) &&
                      !miscMounted.getType().hasFlag((MiscType.F_VEHICLE_MINE_DISPENSER))) {
                continue;
            }

            gbc.gridy = row++;
            MineChoicePanel mcp = new MineChoicePanel(miscMounted, entity);
            gbl.setConstraints(mcp, gbc);
            panMines.add(mcp);
            m_vMines.add(mcp);
        }
    }

    private int getListIndex(int listIndex, int sNodes) {
        if (entity.C3MasterIs(entity)) {
            choC3.setSelectedIndex(listIndex);
        }
        entityCorrespondence[listIndex++] = entity.getId();

        choC3.addItem(Messages.getString("CustomMekDialog.setIndependentMaster", sNodes));
        if (entity.getC3Master() == null) {
            choC3.setSelectedIndex(listIndex);
        }
        entityCorrespondence[listIndex++] = -1;
        return listIndex;
    }

    public void initialize() {
        choC3.setEnabled(false);
        chAutoEject.setEnabled(false);
        chSearchlight.setEnabled(false);
        if (m_bombs != null) {
            m_bombs.setEnabled(false);
        }
        disableMunitionEditing();
        disableAPMEditing();
        disableMEAEditing();
        disableMGSetting();
        disableMineSetting();
        panInfArmor.setEnabled(false);
    }

    private void disableMunitionEditing() {
        for (MunitionChoicePanel mVMunition : m_vMunitions) {
            mVMunition.setEnabled(false);
        }
    }

    private void disableAPMEditing() {
        for (APWeaponChoicePanel mVAPMount : m_vAPMounts) {
            mVAPMount.setEnabled(false);
        }
    }

    private void disableMEAEditing() {
        for (MEAChoicePanel mVMEAdaptor : m_vMEAdaptors) {
            mVMEAdaptor.setEnabled(false);
        }
    }

    private void disableMGSetting() {
        for (RapidFireMGPanel mVMG : m_vMGs) {
            mVMG.setEnabled(false);
        }
    }

    private void disableMineSetting() {
        for (MineChoicePanel mVMine : m_vMines) {
            mVMine.setEnabled(false);
        }
    }

    public void applyChoices() {
        // Auto ejection Options
        boolean autoEject = chAutoEject.isSelected();
        boolean condEjectAmmo = chCondEjectAmmo.isSelected();
        // Meks and LAMs Only
        boolean condEjectEngine = chCondEjectEngine.isSelected();
        boolean condEjectCTDest = chCondEjectCTDest.isSelected();
        boolean condEjectHeadshot = chCondEjectHeadshot.isSelected();
        // Aerospace Only
        boolean condEjectFuel = chCondEjectFuel.isSelected();
        boolean condEjectSIDest = chCondEjectSIDest.isSelected();

        if (entity instanceof Mek mek) {
            mek.setAutoEject(!autoEject);
            mek.setCondEjectAmmo(condEjectAmmo);
            mek.setCondEjectEngine(condEjectEngine);
            mek.setCondEjectCTDest(condEjectCTDest);
            mek.setCondEjectHeadshot(condEjectHeadshot);
        } else if (entity.isFighter()) {
            Aero aero = (Aero) entity;
            aero.setAutoEject(!autoEject);
            aero.setCondEjectAmmo(condEjectAmmo);
            aero.setCondEjectFuel(condEjectFuel);
            aero.setCondEjectSIDest(condEjectSIDest);
        }

        // update AP weapon selections
        for (APWeaponChoicePanel apChoicePanel : m_vAPMounts) {
            apChoicePanel.applyChoice();
        }

        // update modular equipment adaptor selections
        for (MEAChoicePanel meaChoicePanel : m_vMEAdaptors) {
            meaChoicePanel.applyChoice();
        }

        // update munitions selections
        for (final MunitionChoicePanel munitions : m_vMunitions) {
            munitions.applyChoice();
        }
        if (panMunitions instanceof BayMunitionsChoicePanel) {
            ((BayMunitionsChoicePanel) panMunitions).apply();
        } else {
            if (panMunitions instanceof SmallSVMunitionsChoicePanel) {
                ((SmallSVMunitionsChoicePanel) panMunitions).apply();
            }
            // update ammo names for weapon ammo choice selectors
            for (WeaponAmmoChoicePanel wacPanel : m_vWeaponAmmoChoice) {
                wacPanel.applyChoice();
            }
        }

        // update MG rapid fire settings
        for (final RapidFireMGPanel rapidfireMGPanel : m_vMGs) {
            rapidfireMGPanel.applyChoice();
        }
        // update mines setting
        for (final MineChoicePanel mineChoicePanel : m_vMines) {
            mineChoicePanel.applyChoice();
        }
        // update bomb setting
        if (null != m_bombs) {
            m_bombs.applyChoice();
        }
        if (entity.isConventionalInfantry()) {
            panInfArmor.applyChoice();
        }

        // update searchlight setting
        if (!entity.getsAutoExternalSearchlight()) {
            entity.setExternalSearchlight(chSearchlight.isSelected());
            entity.setSearchlightState(chSearchlight.isSelected());
        }

        if (entity.hasC3() && (choC3.getSelectedIndex() > -1)) {
            Entity chosen = client.getEntity(entityCorrespondence[choC3.getSelectedIndex()]);
            int entC3nodeCount = client.getGame().getC3SubNetworkMembers(entity).size();
            int choC3nodeCount = client.getGame().getC3NetworkMembers(chosen).size();

            if ((entC3nodeCount + choC3nodeCount) <= Entity.MAX_C3_NODES &&
                      ((chosen == null) || entity.getC3MasterId() != chosen.getId())) {
                entity.setC3Master(chosen, true);
            } else if ((chosen != null) && entity.getC3MasterId() != chosen.getId()) {
                String message = Messages.getString("CustomMekDialog.NetworkTooBig.message",
                      entity.getShortName(),
                      chosen.getShortName(),
                      entC3nodeCount,
                      choC3nodeCount,
                      Entity.MAX_C3_NODES);
                clientgui.doAlertDialog(Messages.getString("CustomMekDialog.NetworkTooBig.title"), message);
                refreshC3();
            }
        } else if (entity.hasC3i() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondence[choC3.getSelectedIndex()]));
        } else if (entity.hasNavalC3() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondence[choC3.getSelectedIndex()]));
        }
    }
}
