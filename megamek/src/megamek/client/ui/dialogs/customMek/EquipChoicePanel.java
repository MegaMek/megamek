/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2012-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.Client;
import megamek.client.ui.GBC2;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.overlay.ToastLevel;
import megamek.client.ui.util.StringDrawer;
import megamek.client.ui.util.UIUtil;
import megamek.common.SimpleTechLevel;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.AmmoTypeFlag;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.IBomber;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import org.apache.logging.log4j.Logger;

// Possible Improvements:
// FIXME: allow selecting no connection in C3 choice
// FIXME: improve advanced building equipment location description
// FIXME: Remove the lobby dump/lobby dump first round game options or clarify their use case

/**
 * This class builds the Equipment Panel for use in MegaMek and MekHQ
 */
public class EquipChoicePanel extends JPanel {

    private static final Logger LOGGER = MMLogger.create(EquipChoicePanel.class);

    private final Entity entity;
    private final List<MunitionChoice> m_vMunitions = new ArrayList<>();
    private final List<WeaponAmmoChoice> m_vWeaponAmmoChoice = new ArrayList<>();
    /**
     * An <code>ArrayList</code> to keep track of all of the
     * <code>APWeaponChoicePanels</code> that were added, so we can apply
     * their choices when the dialog is closed.
     */
    private final ArrayList<APWeaponChoice> m_vAPMounts = new ArrayList<>();
    /**
     * Panel for adding components related to selecting which anti-personnel weapons are mounted in an AP Mount (armored
     * gloves are also considered AP mounts)
     **/
    private BaManipulatorChoice panBaManipulators;
    private final ArrayList<RapidFireMGChoice> m_vMGs = new ArrayList<>();
    private VRTChoice panVRT;
    private final ArrayList<MineChoice> m_vMines = new ArrayList<>();
    private final JCheckBox chAutoEject = new JCheckBox(Messages.getString("CustomMekDialog.labAutoEject"));
    private final JCheckBox chCondEjectAmmo = new JCheckBox(Messages.getString(
          "CustomMekDialog.labConditional_Ejection_Ammo"));
    private final JCheckBox chCondEjectEngine = new JCheckBox(Messages.getString(
          "CustomMekDialog.labConditional_Ejection_Engine"));
    private final JCheckBox chCondEjectCTDest = new JCheckBox(Messages.getString(
          "CustomMekDialog.labConditional_Ejection_CT_Destroyed"));
    private final JCheckBox chCondEjectHeadshot = new JCheckBox(Messages.getString(
          "CustomMekDialog.labConditional_Ejection_Headshot"));
    private final JCheckBox chCondEjectFuel = new JCheckBox(Messages.getString(
          "CustomMekDialog.labConditional_Ejection_Fuel"));
    private final JCheckBox chCondEjectSIDest = new JCheckBox(Messages.getString(
          "CustomMekDialog.labConditional_Ejection_SI_Destroyed"));
    private final JCheckBox chSearchlight = new JCheckBox(Messages.getString("CustomMekDialog.labSearchlight"));
    private final JCheckBox chSearchlightStatus = new JCheckBox(Messages.getString(
          "CustomMekDialog.labSearchlightStatus"));
    private final JCheckBox chDNICockpitMod = new JCheckBox(Messages.getString("CustomMekDialog.labDNICockpitMod"));
    private final JCheckBox chEICockpit = new JCheckBox(Messages.getString("CustomMekDialog.labEICockpit"));
    private final JCheckBox chDamageInterruptCircuit
          = new JCheckBox(Messages.getString("CustomMekDialog.labDamageInterruptCircuit"));
    /** Ghost target equipment mode selectors, keyed by equipment number on the entity. */
    private final Map<Integer, JComboBox<String>> ecmModeSelectors = new LinkedHashMap<>();
    private final JComboBox<String> choC3 = new JComboBox<>();
    ClientGUI clientgui;
    Client client;
    private SmallSVMunitionsChoice smallSvMunitionsChoice;
    private BayMunitionsChoicePanel bayMunitionsChoicePanel;
    private int[] entityCorrespondence;
    private InfantryArmorPanel panInfArmor;
    private BombChoicePanel m_bombs;

    private final StringDrawer nothingToConfigureText =
          new StringDrawer("No configurable equipment.").center().color(UIManager.getColor("Label.disabledForeground"));

    public EquipChoicePanel(Entity entity, ClientGUI clientgui, Client client) {
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;
        Game game = (clientgui == null) ? client.getGame() : clientgui.getClient().getGame();

        setLayout(new GridBagLayout());
        GBC2 gbc = new GBC2(new Insets(0, 40, 0, 10), new Insets(0, 2, 1, 2));

        if (entity instanceof HandheldWeapon) {
            // HHW have nothing to configure
            addNoConfigureLabel(gbc);
            return;
        }

        if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3()) {
            add(new SectionTitleLabel("C3 Configuration"), gbc.fullLine());
            JLabel labC3 = new JLabel(Messages.getString("CustomMekDialog.labC3"), SwingConstants.RIGHT);
            add(labC3, gbc.forLabel());
            add(choC3, gbc.eol());
            refreshC3();
        }

        if ((entity instanceof BattleArmor battleArmor)) {
            List<WeaponType> apmWeaponTypes = new Vector<>(100);
            List<WeaponType> agloveWeaponTypes = new Vector<>(100);
            int gameYear;
            SimpleTechLevel legalLevel;
            if (clientgui == null) {
                gameYear = client.getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
                legalLevel = SimpleTechLevel.getGameTechLevel(client.getGame());
            } else {
                gameYear = clientgui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
                legalLevel = SimpleTechLevel.getGameTechLevel(clientgui.getClient().getGame());
            }
            for (EquipmentType eq : EquipmentType.allTypes()) {
                // Only non-melee infantry weapons are allowed, TM p.170
                if (!(eq instanceof InfantryWeapon infantryWeapon)
                      || !infantryWeapon.hasFlag(WeaponType.F_INFANTRY)
                      || infantryWeapon.hasFlag(WeaponType.F_INF_POINT_BLANK)
                      || infantryWeapon.hasFlag(WeaponType.F_INF_ARCHAIC)) {
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

                if (!infantryWeapon.hasFlag(WeaponType.F_INF_SUPPORT)) {
                    apmWeaponTypes.add(infantryWeapon);
                }
                if (infantryWeapon.getCrew() < 2) {
                    agloveWeaponTypes.add(infantryWeapon);
                }
            }
            apmWeaponTypes.sort(Comparator.comparing(EquipmentType::getName));
            agloveWeaponTypes.sort(Comparator.comparing(EquipmentType::getName));

            // AP mounts (not armored gloves)
            if (entity.hasMisc(EquipmentTypeLookup.BA_APM)) {
                String apTitle = Messages.getString("CustomMekDialog.APMountPanelTitle");
                add(new SectionTitleLabel(apTitle), gbc.fullLine());
                for (Mounted<?> misc : entity.getMisc()) {
                    if (misc.is(EquipmentTypeLookup.BA_APM)) {
                        var apWeaponChoice = new APWeaponChoice(entity, misc, apmWeaponTypes, this, gbc);
                        m_vAPMounts.add(apWeaponChoice);
                    }
                }
            }

            // Manipulators and Armored Glove AP mounting
            if (entity.hasWorkingMisc(MiscType.F_BA_MEA) || battleArmor.hasMisc(MiscTypeFlag.F_ARMORED_GLOVE)) {
                String meaTitle = Messages.getString("CustomMekDialog.MEAPanelTitle");
                add(new SectionTitleLabel(meaTitle), gbc.fullLine());
                panBaManipulators = new BaManipulatorChoice(battleArmor, agloveWeaponTypes, this, gbc);
            }
        }

        if (entity.isBattleArmor() || !(entity instanceof Infantry infantry) || infantry.hasFieldWeapon()) {
            setupMunitions(gbc);
            if (clientgui != null) {
                setupWeaponAmmoChoice(gbc);
            }
        }

        if (entity.isBomber()) {
            setupBombs(gbc);
        }

        // Set up rapid fire mg; per errata infantry of any kind cannot use them
        if (game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BURST) &&
              !(entity instanceof Infantry)) {
            setupRapidFireMGs(gbc);
        }

        // Set up Variable Range Targeting mode selection
        if (entity.hasVariableRangeTargeting()) {
            setupVRT(gbc);
        }

        // set up infantry armor
        if (entity.isConventionalInfantry()) {
            panInfArmor = new InfantryArmorPanel(entity, this, gbc);
        }

        // Set up mines
        setupMines(gbc);

        // Set up ECM equipment mode selectors (ECM/ECCM, and Ghost Targets per TO:AR p.100)
        setupEcmModes(game, gbc);

        // Misc section
        JComponent miscTitle = new SectionTitleLabel(Messages.getString("CustomMekDialog.miscSection"));
        add(miscTitle, gbc.fullLine());
        boolean hasMiscSection = false;

        // Set up searchlight
        if (client.getGame().getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            if (!entity.getsAutoExternalSearchlight()) {
                add(new JLabel(), gbc.forLabel());
                add(chSearchlight, gbc.eol());
                chSearchlight.setSelected(entity.hasSearchlight() ||
                      entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
                chSearchlight.setEnabled(!entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
                chSearchlightStatus.setEnabled(true);
            }

            // Searchlights are on at the start
            boolean startSLOn = game.getOptions().booleanOption(OptionsConstants.SEARCHLIGHTS_ON);
            add(new JLabel(), gbc.oneColumn());
            add(chSearchlightStatus, gbc.eol());
            if (entity.getsAutoExternalSearchlight() || chSearchlight.isSelected()) {
                chSearchlightStatus.setEnabled(true);
                if (entity.getSearchlightOverride()) {
                    startSLOn = !startSLOn;
                }
                chSearchlightStatus.setSelected(startSLOn);
            }
            hasMiscSection = true;
        }

        // Set up DNI Cockpit Modification (IO p.83)
        // Only show in Full Tracking mode (hardware + pilot required)
        // DNI is Inner Sphere tech (E/X-X-E-F) - not available for pure Clan units
        boolean isFullTracking = OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING.equals(
              game.getOptions().stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
        if (isFullTracking) {
            // DNI cockpit mod is available for Meks, Tanks, Fighters, BA, and Support Vehicles
            // Must be IS, Mixed IS, or Mixed Clan (not pure Clan)
            boolean validUnitType = entity.isMek() || entity.isCombatVehicle() || entity.isFighter()
                  || entity.isSupportVehicle() || (entity instanceof BattleArmor);
            boolean validTechBase = !entity.isClan() || entity.isMixedTech();
            if (validUnitType && validTechBase) {
                // Check game year against equipment introduction date
                EquipmentType dniEquipment = EquipmentType.get("DNICockpitModification");
                int gameYear = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
                int dniIntroYear = (dniEquipment != null) ? dniEquipment.getIntroductionDate(false) : 3052;
                if (gameYear >= dniIntroYear) {
                    add(new JLabel(), gbc.forLabel());
                    add(chDNICockpitMod, gbc.eol());
                    // Auto-select if pilot has DNI implant (smart detection)
                    boolean hasHardware = entity.hasDNICockpitMod();
                    boolean hasImplant = entity.hasDNIImplant();
                    chDNICockpitMod.setSelected(hasHardware || hasImplant);
                    hasMiscSection = true;
                }
            }
        }

        // Set up EI Interface (IO p.69)
        // Only show in Full Tracking mode (hardware + pilot required)
        // EI is Clan tech (F/X-X-D-D) - not available for pure IS units
        if (isFullTracking) {
            // EI Interface is available for Meks, BA, and ProtoMeks
            // Must be Clan, Mixed Clan, or Mixed IS (not pure IS)
            boolean validUnitType = entity.isMek() || (entity instanceof BattleArmor) || entity.isProtoMek();
            boolean validTechBase = entity.isClan() || entity.isMixedTech();
            if (validUnitType && validTechBase) {
                // Check game year against equipment introduction date
                EquipmentType eiEquipment = EquipmentType.get("EIInterface");
                int gameYear = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
                int eiIntroYear = (eiEquipment != null) ? eiEquipment.getIntroductionDate(true) : 3040;
                if (gameYear >= eiIntroYear) {
                    add(new JLabel(), gbc.forLabel());
                    add(chEICockpit, gbc.eol());
                    // Auto-select if pilot has EI implant (smart detection)
                    boolean hasHardware = entity.hasEiCockpit();
                    boolean hasImplant = entity.hasAbility(OptionsConstants.MD_EI_IMPLANT);
                    chEICockpit.setSelected(hasHardware || hasImplant);
                    hasMiscSection = true;
                }
            }
        }

        // Auto-eject checkbox and conditional ejections.
        if (entity instanceof Mek mek) {
            if (mek.hasEjectSeat() && clientgui != null) {
                add(new JLabel(), gbc.forLabel());
                add(chAutoEject, gbc.eol());
                chAutoEject.setSelected(!mek.isAutoEject());
                hasMiscSection = true;
            }

            // Conditional Ejections
            if (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) && mek.hasEjectSeat()) {
                add(new JLabel(), gbc.forLabel());
                add(chCondEjectAmmo, gbc.eol());
                chCondEjectAmmo.setSelected(mek.isCondEjectAmmo());

                add(new JLabel(), gbc.forLabel());
                add(chCondEjectEngine, gbc.eol());
                chCondEjectEngine.setSelected(mek.isCondEjectEngine());

                add(new JLabel(), gbc.forLabel());
                add(chCondEjectCTDest, gbc.eol());
                chCondEjectCTDest.setSelected(mek.isCondEjectCTDest());

                add(new JLabel(), gbc.forLabel());
                add(chCondEjectHeadshot, gbc.eol());
                chCondEjectHeadshot.setSelected(mek.isCondEjectHeadshot());
                hasMiscSection = true;
            }

        } else if (entity instanceof AeroSpaceFighter aero) {
            // Ejection Seat
            if (aero.hasEjectSeat()) {
                add(new JLabel(), gbc.forLabel());
                add(chAutoEject, gbc.eol());
                chAutoEject.setSelected(!aero.isAutoEject());
                hasMiscSection = true;
            }

            // Conditional Ejections
            if (game.getOptions().booleanOption(OptionsConstants.RPG_CONDITIONAL_EJECTION) && aero.hasEjectSeat()) {
                add(new JLabel(), gbc.oneColumn());
                add(chCondEjectAmmo, gbc.eol());
                chCondEjectAmmo.setSelected(aero.isCondEjectAmmo());

                add(new JLabel(), gbc.oneColumn());
                add(chCondEjectFuel, gbc.eol());
                chCondEjectFuel.setSelected(aero.isCondEjectFuel());

                add(new JLabel(), gbc.oneColumn());
                add(chCondEjectSIDest, gbc.eol());
                chCondEjectSIDest.setSelected(aero.isCondEjectSIDest());
                hasMiscSection = true;
            }
        }

        // Set up Damage Interrupt Circuit (IO p.39) - BattleMeks and IndustrialMeks only, IS or Mixed tech
        if ((entity instanceof Mek mek) && ((!entity.isClan()) || (entity.isMixedTech()))) {
            EquipmentType dicEquipment = EquipmentType.get("DamageInterruptCircuit");
            if (dicEquipment != null) {
                int gameYear = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
                int dicIntroYear = dicEquipment.getIntroductionDate(false); // IS tech
                if (gameYear >= dicIntroYear) {
                    add(new JLabel(), gbc.forLabel());
                    add(chDamageInterruptCircuit, gbc.eol());
                    chDamageInterruptCircuit.setSelected(mek.hasDamageInterruptCircuit());
                    hasMiscSection = true;
                }
            }
        }

        if (!hasMiscSection) {
            remove(miscTitle);
        }

        if (getComponentCount() == 0) {
            addNoConfigureLabel(gbc);
        }
        revalidate();
        repaint();
    }

    private void addNoConfigureLabel(GBC2 gbc) {
        JLabel nothingLabel = new JLabel("No equipment to configure.");
        nothingLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "large");
        nothingLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(nothingLabel, gbc.forLabel());
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

    private void setupMunitions(GBC2 gbc) {
        Game game = clientgui == null ? client.getGame() : clientgui.getClient().getGame();
        IGameOptions gameOpts = game.getOptions();
        int gameYear = gameOpts.intOption(OptionsConstants.ALLOWED_YEAR);

        String ammoSelectionTitle = Messages.getString("CustomMekDialog.MunitionsPanelTitle");
        JComponent title = new SectionTitleLabel(ammoSelectionTitle);
        add(title, gbc.fullLine());

        if (entity.usesWeaponBays() || entity instanceof Dropship) {
            // Grounded dropships don't *use* weapon bays as such, but should load ammo as if they did
            bayMunitionsChoicePanel = new BayMunitionsChoicePanel(entity, game, this, gbc);
            if (bayMunitionsChoicePanel.isEmpty()) {
                remove(title);
            }
            return;
        }
        // Small support vehicle ammo is part of the weapon, and the only munitions choice is standard or inferno,
        // and only for some weapons.
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            smallSvMunitionsChoice = new SmallSVMunitionsChoice(entity, this, gbc);
            if (smallSvMunitionsChoice.isEmpty()) {
                remove(title);
            }
            return;
        }

        for (AmmoMounted ammoMounted : entity.getAmmo()) {
            AmmoType at = ammoMounted.getType();
            Vector<AmmoType> vTypes = new Vector<>();
            Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());

            if (vAllTypes == null) {
                continue;
            }

            // don't allow ammo switching of most things for Aerospace allow only MML, ATM, and NARC. LRM/SRM can
            // switch between Artemis and standard, but not other munitions. Same with MRM.
            if ((entity instanceof Aero) &&
                  !((at.getAmmoType() == AmmoType.AmmoTypeEnum.MML) ||
                        (at.getAmmoType() == AmmoType.AmmoTypeEnum.SRM) ||
                        (at.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) ||
                        (at.getAmmoType() == AmmoType.AmmoTypeEnum.MRM) ||
                        (at.getAmmoType() == AmmoType.AmmoTypeEnum.ATM) ||
                        (at.getAmmoType() == AmmoType.AmmoTypeEnum.IATM))) {
                continue;
            }

            boolean considerAllAmmoMixedTech = gameOpts.booleanOption(OptionsConstants.ALLOWED_ALL_AMMO_MIXED_TECH);
            boolean isClan = entity.isClan();
            boolean isIs = !isClan;
            boolean canUseISAmmo = entity.isMixedTech() || isIs || considerAllAmmoMixedTech;
            boolean canUseClanAmmo = entity.isMixedTech() || isClan || considerAllAmmoMixedTech;

            for (AmmoType atCheck : vAllTypes) {
                if (entity.hasETypeFlag(Entity.ETYPE_AERO) &&
                      !atCheck.canAeroUse(game.getOptions()
                            .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_ARTILLERY_MUNITIONS))) {
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
                    // This is the way MegaMek is intended to use tech levels.
                    boolean isClanAccessibleTech = atCheck.isClan() || atCheck.isMixedTech();
                    boolean isIsAccessibleTech = !atCheck.isClan() || atCheck.isMixedTech();
                    boolean canUseThisAmmo = (canUseISAmmo && isIsAccessibleTech) || (canUseClanAmmo
                          && isClanAccessibleTech);
                    bTechMatch = atCheck.getStaticTechLevel().ordinal() <= legalLevel.ordinal() && canUseThisAmmo;
                }

                // If clan_ignore_eq_limits is unchecked, do NOT allow Clans to use IS-only ammo.
                EnumSet<AmmoType.Munitions> munitionsTypes = atCheck.getMunitionType();
                if (!gameOpts.booleanOption(OptionsConstants.ALLOWED_ALL_AMMO_MIXED_TECH) &&
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

                if (Set.of(AmmoType.AmmoTypeEnum.LRM, AmmoType.AmmoTypeEnum.SRM).contains(atCheck.getAmmoType()) &&
                      entity.isBattleArmor() &&
                      !atCheck.hasFlag(AmmoTypeFlag.F_BATTLEARMOR)) {
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
            if (vTypes.isEmpty() &&
                  !client.getGame().getOptions().booleanOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP) &&
                  !client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD)) {
                continue;
            }
            MunitionChoice munitionChoice = new MunitionChoice(ammoMounted,
                  vTypes,
                  m_vWeaponAmmoChoice,
                  entity,
                  game, this, gbc);

            m_vMunitions.add(munitionChoice);
        }
        if (m_vMunitions.isEmpty()) {
            remove(title);
        }
    }

    /**
     * Worker function that creates a series of weapon ammo choice panels that allow the user to pick a particular ammo
     * bin for an ammo-using weapon with matching ammo.
     */
    private void setupWeaponAmmoChoice(GBC2 gbc) {
        String ammoTitle = Messages.getString("CustomMekDialog.WeaponSelectionTitle");
        JComponent title = new SectionTitleLabel(ammoTitle);
        add(title, gbc.fullLine());
        for (WeaponMounted weapon : entity.getWeaponList()) {
            // don't deal with bay or grouped weapons for now
            if (weapon.getType().getAmmoType() != AmmoType.AmmoTypeEnum.NA) {
                var ammoChoice = new WeaponAmmoChoice(weapon, entity, this, gbc);
                if (!ammoChoice.isEmpty()) {
                    m_vWeaponAmmoChoice.add(ammoChoice);
                }
            }
        }
        if (m_vWeaponAmmoChoice.isEmpty()) {
            remove(title);
        }
    }

    private void setupBombs(GBC2 gbc) {
        JComponent title = new SectionTitleLabel(Messages.getString("CustomMekDialog.bombSection"));
        add(title, gbc.fullLine());

        int techLevel = Arrays.binarySearch(TechConstants.T_SIMPLE_NAMES,
              client.getGame().getOptions().stringOption(OptionsConstants.ALLOWED_TECH_LEVEL));
        boolean allowNukes = client.getGame()
              .getOptions()
              .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES);
        m_bombs = new BombChoicePanel((IBomber) entity, allowNukes,
              techLevel >= TechConstants.T_SIMPLE_ADVANCED, this, gbc);
    }

    private void setupRapidFireMGs(GBC2 gbc) {
        String mgTitle = Messages.getString("CustomMekDialog.rapidFireSection");
        JComponent title = new SectionTitleLabel(mgTitle);
        add(title, gbc.fullLine());
        for (Mounted<?> mounted : entity.getWeaponList()) {
            WeaponType weaponType = (WeaponType) mounted.getType();
            if (weaponType.hasFlag(WeaponType.F_MG)) {
                RapidFireMGChoice rapidFireMGChoice = new RapidFireMGChoice(mounted, entity, this, gbc);
                m_vMGs.add(rapidFireMGChoice);
            }
        }
        if (m_vMGs.isEmpty()) {
            remove(title);
        }
    }

    /**
     * Sets up the Variable Range Targeting mode selection panel. Only called if entity has the VRT quirk.
     */
    private void setupVRT(GBC2 gbc) {
        String vrtTitle = Messages.getString("CustomMekDialog.VRTPanelTitle");
        JComponent title = new SectionTitleLabel(vrtTitle);
        add(title, gbc.fullLine());
        panVRT = new VRTChoice(entity, this, gbc);
    }

    private void setupMines(GBC2 gbc) {
        String minesTitle = Messages.getString("CustomMekDialog.mineSection");
        JComponent title = new SectionTitleLabel(minesTitle);
        add(title, gbc.fullLine());
        for (MiscMounted miscMounted : entity.getMisc()) {
            if (miscMounted.getType().hasFlag((MiscType.F_MINE)) ||
                  miscMounted.getType().hasFlag((MiscType.F_VEHICLE_MINE_DISPENSER))) {
                m_vMines.add(new MineChoice(miscMounted, entity, this, gbc));
            }
        }
        if (m_vMines.isEmpty()) {
            remove(title);
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
        chSearchlightStatus.setEnabled(false);

        chDamageInterruptCircuit.setEnabled(false);
        if (m_bombs != null) {
            m_bombs.setEnabled(false);
        }
        disableMunitionEditing();
        disableAPMEditing();
        disableMEAEditing();
        disableMGSetting();
        disableVRTSetting();
        disableMineSetting();
        panInfArmor.setEnabled(false);
    }

    private void disableMunitionEditing() {
        for (MunitionChoice mVMunition : m_vMunitions) {
            mVMunition.setEnabled(false);
        }
    }

    private void disableAPMEditing() {
        for (APWeaponChoice mVAPMount : m_vAPMounts) {
            mVAPMount.setEnabled(false);
        }
    }

    private void disableMEAEditing() {
        if (panBaManipulators != null) {
            panBaManipulators.setEnabled(false);
        }
    }

    private void disableMGSetting() {
        for (RapidFireMGChoice mVMG : m_vMGs) {
            mVMG.setEnabled(false);
        }
    }

    private void disableVRTSetting() {
        if (panVRT != null) {
            panVRT.setEnabled(false);
        }
    }

    private void disableMineSetting() {
        for (MineChoice mVMine : m_vMines) {
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
        for (APWeaponChoice apChoicePanel : m_vAPMounts) {
            apChoicePanel.applyChoice();
        }

        // update modular equipment adaptor selections
        if (panBaManipulators != null) {
            panBaManipulators.applyChoice();
        }

        // update munitions selections
        for (final MunitionChoice munitions : m_vMunitions) {
            munitions.applyChoice();
        }
        if (smallSvMunitionsChoice != null) {
            smallSvMunitionsChoice.apply();
        }
        if (bayMunitionsChoicePanel != null) {
            bayMunitionsChoicePanel.apply();
        } else {
            // update ammo names for weapon ammo choice selectors
            for (WeaponAmmoChoice wacPanel : m_vWeaponAmmoChoice) {
                wacPanel.applyChoice();
            }
        }

        // update MG rapid fire settings
        for (final RapidFireMGChoice rapidfireMGChoice : m_vMGs) {
            rapidfireMGChoice.applyChoice();
        }
        // update Variable Range Targeting mode
        if (panVRT != null) {
            panVRT.applyChoice();
        }
        // update mines setting
        for (final MineChoice mineChoice : m_vMines) {
            mineChoice.applyChoice();
        }
        // update bomb setting
        if (null != m_bombs) {
            m_bombs.applyChoice();
        }
        if (entity.isConventionalInfantry()) {
            panInfArmor.applyChoice();
        }

        // Get the game object. Used for both searchlights and DNI
        Game game = (clientgui == null) ? client.getGame() : clientgui.getClient().getGame();

        boolean searchlightsDefault = game.getOptions().booleanOption(OptionsConstants.SEARCHLIGHTS_ON);
        // Only apply changes to searchlights if the planetary conditions call for it
        if (client.getGame().getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) {
            // update searchlight setting for non-mek/tank entities
            if (!entity.getsAutoExternalSearchlight()) {
                // Add the searchlight to the Entity
                entity.setExternalSearchlight(chSearchlight.isSelected());
                // If the searchlight is off, turn off the status
                if (!chSearchlight.isSelected()) {
                    chSearchlightStatus.setEnabled(false);
                }
                // Only set the override if we are choosing something that is not the default behavior
                entity.setSearchlightOverride((searchlightsDefault && !chSearchlightStatus.isSelected())
                      || (!searchlightsDefault
                      && chSearchlightStatus.isSelected()));
            }
            // Update searchlights for meks and tanks
            if (entity.getsAutoExternalSearchlight()) {
                // Only set the override if we are choosing something that is not the default behavior
                entity.setSearchlightOverride((searchlightsDefault && !chSearchlightStatus.isSelected())
                      || (!searchlightsDefault
                      && chSearchlightStatus.isSelected()));
            }
        }

        // update DNI Cockpit Modification setting (IO p.83) - only in Full Tracking mode
        boolean isFullTrackingMode = OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING.equals(
              game.getOptions().stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE));
        if (isFullTrackingMode) {
            boolean wantsDNI = chDNICockpitMod.isSelected();
            boolean hasDNI = entity.hasDNICockpitMod();
            if (wantsDNI && !hasDNI) {
                // Add DNI Cockpit Mod
                MiscType dniMod = (MiscType) EquipmentType.get("DNICockpitModification");
                if (dniMod != null) {
                    try {
                        entity.addEquipment(dniMod, Entity.LOC_NONE);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to add DNI cockpit modification to {}: {}",
                              entity.getDisplayName(), e.getMessage());
                    }
                }
            } else if (!wantsDNI && hasDNI) {
                // Remove DNI Cockpit Mod
                for (MiscMounted mounted : entity.getMisc()) {
                    if (mounted.getType().hasFlag(MiscType.F_DNI_COCKPIT_MOD)) {
                        entity.removeMisc(mounted.getName());
                        break;
                    }
                }
            }
        }

        // update EI Interface setting (IO p.69) - only in Full Tracking mode
        if (isFullTrackingMode) {
            boolean wantsEI = chEICockpit.isSelected();
            boolean hasEI = entity.hasEiCockpit();
            if (wantsEI && !hasEI) {
                // Add EI Interface
                MiscType eiInterface = (MiscType) EquipmentType.get("EIInterface");
                if (eiInterface != null) {
                    try {
                        entity.addEquipment(eiInterface, Entity.LOC_NONE);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to add EI Interface to {}: {}",
                              entity.getDisplayName(), e.getMessage());
                    }
                }
            } else if (!wantsEI && hasEI) {
                // Remove EI Interface
                for (MiscMounted mounted : entity.getMisc()) {
                    if (mounted.getType().hasFlag(MiscType.F_EI_INTERFACE)) {
                        entity.removeMisc(mounted.getName());
                        break;
                    }
                }
            }
        }

        // update Damage Interrupt Circuit setting (IO p.39)
        if ((entity instanceof Mek mek) && ((!entity.isClan()) || (entity.isMixedTech()))) {
            boolean hasDamageInterruptCircuit = mek.hasDamageInterruptCircuit();
            boolean wantsDamageInterruptCircuit = chDamageInterruptCircuit.isSelected();
            if ((wantsDamageInterruptCircuit) && (!hasDamageInterruptCircuit)) {
                // Add Damage Interrupt Circuit equipment
                try {
                    EquipmentType damageInterruptCircuitType = EquipmentType.get("DamageInterruptCircuit");
                    if (damageInterruptCircuitType != null) {
                        entity.addEquipment(damageInterruptCircuitType, Entity.LOC_NONE);
                    }
                } catch (Exception e) {
                    // 0-crit equipment shouldn't fail to add
                }
            } else if ((!wantsDamageInterruptCircuit) && (hasDamageInterruptCircuit)) {
                // Remove Damage Interrupt Circuit equipment
                for (MiscMounted mounted : entity.getMisc()) {
                    if (mounted.getType().hasFlag(MiscType.F_DAMAGE_INTERRUPT_CIRCUIT)) {
                        entity.removeMisc(mounted.getType().getInternalName());
                        break;
                    }
                }
            }
        }

        // Apply ghost target equipment mode selections
        applyEcmModes();

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
                if (clientgui == null) {
                    JOptionPane.showMessageDialog(this,
                          Messages.getString("CustomMekDialog.NetworkTooBig.title"),
                          message,
                          JOptionPane.WARNING_MESSAGE);
                } else {
                    clientgui.addToast(ToastLevel.WARNING, message);
                }
                refreshC3();
            }
        } else if (entity.hasC3i() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondence[choC3.getSelectedIndex()]));
        } else if (entity.hasNavalC3() && (choC3.getSelectedIndex() > -1)) {
            entity.setC3NetId(client.getEntity(entityCorrespondence[choC3.getSelectedIndex()]));
        }
    }

    /**
     * Refreshes the neural interface checkboxes based on current pilot implant status. Called when switching to the
     * Equipment tab to pick up changes made in the Pilot tab.
     *
     * <p>This method only auto-CHECKS the checkbox when an implant is detected but hardware is missing.
     * It respects manual unchecking - if the user has unchecked the box (hardware not present and box unchecked), it
     * won't force it back to checked. This allows testing scenarios where pilot has implant but unit lacks
     * hardware.</p>
     */
    public void refreshNeuralInterfaceCheckboxes() {
        Game game = (clientgui == null) ? client.getGame() : clientgui.getClient().getGame();
        if (!OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING.equals(
              game.getOptions().stringOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE))) {
            return;
        }

        // Refresh DNI checkbox - only force checked when hardware is present
        // Respects manual unchecking when pilot has implant but user wants to test without hardware
        if (entity.hasDNICockpitMod()) {
            chDNICockpitMod.setSelected(true);
        }

        // Refresh EI checkbox - only force checked when hardware is present
        // Respects manual unchecking when pilot has implant but user wants to test without hardware
        if (entity.hasEiCockpit()) {
            chEICockpit.setSelected(true);
        }
    }

    /**
     * Sets the DNI Cockpit Modification checkbox state directly. Called from CustomMekDialog when a DNI implant option
     * is toggled.
     *
     * @param selected true to check the checkbox, false to uncheck
     */
    public void setDNICockpitModSelected(boolean selected) {
        chDNICockpitMod.setSelected(selected);
    }

    /**
     * Sets the EI Interface checkbox state directly. Called from CustomMekDialog when the EI implant option is
     * toggled.
     *
     * @param selected true to check the checkbox, false to uncheck
     */
    public void setEICockpitSelected(boolean selected) {
        chEICockpit.setSelected(selected);
    }

    /**
     * Sets up mode dropdowns for ECM equipment. Covers plain ECM/ECCM mode selection as well as Ghost Targets modes
     * (TO:AR p.100) when that game option is enabled. Also includes Communications Equipment (7+ tons) and Cockpit
     * Command Console when they can be set to Ghost Targets mode.
     */
    private void setupEcmModes(Game game, GBC2 gbc) {
        boolean hasEccmOption = game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_ECCM);
        boolean hasGhostTargetOption = game.getOptions()
              .booleanOption(OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET);

        JComponent title = new SectionTitleLabel(Messages.getString("CustomMekDialog.ecmSection"));
        add(title, gbc.fullLine());

        for (MiscMounted equipment : entity.getMisc()) {
            if (equipment.isInoperable()) {
                continue;
            }
            MiscType type = equipment.getType();
            List<String> modes = new ArrayList<>();

            if (type.hasFlag(MiscType.F_ECM) && !type.hasFlag(MiscType.F_NOVA)) {
                // ECM / Angel ECM
                modes.add("ECM");
                if (hasEccmOption) {
                    modes.add("ECCM");
                    if (type.hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & ECCM");
                    }
                }
                if (hasGhostTargetOption) {
                    if (type.hasFlag(MiscType.F_ANGEL_ECM)) {
                        modes.add("ECM & Ghost Targets");
                        if (hasEccmOption) {
                            modes.add("ECCM & Ghost Targets");
                        }
                    } else {
                        modes.add("Ghost Targets");
                    }
                }
            } else if (hasGhostTargetOption
                  && type.hasFlag(MiscType.F_COMMUNICATIONS)
                  && (entity.getTotalCommGearTons() >= 7)) {
                modes.add("Default");
                if (hasEccmOption) {
                    modes.add("ECCM");
                }
                modes.add("Ghost Targets");
            } else if (hasGhostTargetOption && type.hasFlag(MiscType.F_COMMAND_CONSOLE)) {
                modes.add("Default");
                modes.add("Ghost Targets");
            }

            if (modes.size() > 1) {
                int equipmentNumber = entity.getEquipmentNum(equipment);
                JComboBox<String> combo = new JComboBox<>(modes.toArray(new String[0]));
                combo.setSelectedItem(equipment.curMode().getName());

                JLabel label = new JLabel(equipment.getName() + ":", SwingConstants.RIGHT);
                add(label, gbc.forLabel());
                add(combo, gbc.eol());
                ecmModeSelectors.put(equipmentNumber, combo);
            }
        }

        if (ecmModeSelectors.isEmpty()) {
            remove(title);
        }
    }

    /**
     * Applies the ECM mode selections from the lobby dropdowns to the entity's equipment.
     */
    private void applyEcmModes() {
        for (Map.Entry<Integer, JComboBox<String>> entry : ecmModeSelectors.entrySet()) {
            int equipmentNumber = entry.getKey();
            JComboBox<String> combo = entry.getValue();
            String selectedMode = (String) combo.getSelectedItem();
            if (selectedMode == null) {
                continue;
            }
            Mounted<?> equipment = entity.getEquipment(equipmentNumber);
            if (equipment == null) {
                continue;
            }
            // Find the mode index matching the selected string
            for (int i = 0; i < equipment.getType().getModesCount(); i++) {
                if (equipment.getType().getMode(i).getName().equals(selectedMode)) {
                    equipment.setMode(i);
                    break;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getComponentCount() == 0) {
            UIUtil.setHighQualityRendering(g);
            nothingToConfigureText.at(getWidth() / 2, getHeight() / 2).draw(g);
        }
    }

    static class SectionTitleLabel extends JPanel {

        public SectionTitleLabel(String text) {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(35, 0, 5, 0));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.weightx = 1;
            gbc.gridx = 1;
            add(new JSeparator(), gbc); // right line

            // Label
            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.ipadx = 0;
            gbc.insets = new Insets(0, 0, 0, 25);
            var titleLabel = new JLabel(text);
            titleLabel.setForeground(UIUtil.uiLightGreen());
            titleLabel.putClientProperty(FlatClientProperties.STYLE_CLASS, "large");
            add(titleLabel, gbc);
        }
    }
}
