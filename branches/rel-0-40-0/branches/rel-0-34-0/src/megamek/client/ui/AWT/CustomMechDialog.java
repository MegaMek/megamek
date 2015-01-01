/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * CustomMechDialog.java
 *
 * Created on March 18, 2002, 2:56 PM
 */

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BombType;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.EquipmentType;
import megamek.common.FighterSquadron;
import megamek.common.GunEmplacement;
import megamek.common.IGame;
import megamek.common.IOffBoardDirections;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.PlanetaryConditions;
import megamek.common.Player;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.preference.PreferenceManager;

/**
 * A dialog that a player can use to customize his mech before battle.
 * Currently, changing pilots, setting up C3 networks, changing ammunition,
 * deploying artillery offboard, setting MGs to rapidfire, setting auto-eject is
 * supported.
 *
 * @author Ben
 * @version
 */
public class CustomMechDialog extends ClientDialog implements ActionListener,
        DialogOptionListener {

    /**
     *
     */
    private static final long serialVersionUID = -4426594323169113467L;
    private Label labName = new Label(Messages
            .getString("CustomMechDialog.labName"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldName = new TextField(20);
    private Label labGunnery = new Label(Messages
            .getString("CustomMechDialog.labGunnery"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldGunnery = new TextField(3);
    private Label labGunneryL = new Label(Messages
            .getString("CustomMechDialog.labGunneryL"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldGunneryL = new TextField(3);
    private Label labGunneryM = new Label(Messages
            .getString("CustomMechDialog.labGunneryM"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldGunneryM = new TextField(3);
    private Label labGunneryB = new Label(Messages
            .getString("CustomMechDialog.labGunneryB"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldGunneryB = new TextField(3);
    private Label labPiloting = new Label(Messages
            .getString("CustomMechDialog.labPiloting"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldPiloting = new TextField(3);
    private Label labInit = new Label(Messages
            .getString("CustomMechDialog.labInit"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldInit = new TextField(3);
    private Label labCommandInit = new Label(Messages
            .getString("CustomMechDialog.labCommandInit"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldCommandInit = new TextField(3);
    private Label labC3 = new Label(Messages
            .getString("CustomMechDialog.labC3"), Label.RIGHT); //$NON-NLS-1$
    private Choice choC3 = new Choice();
    private int[] entityCorrespondance;
    private Label labCallsign = new Label(Messages
            .getString("CustomMechDialog.labCallsign"), Label.CENTER); //$NON-NLS-1$
    private Label labUnitNum = new Label(Messages
            .getString("CustomMechDialog.labUnitNum"), Label.CENTER); //$NON-NLS-1$
    private Choice choUnitNum = new Choice();
    private Vector<Entity> entityUnitNum = new Vector<Entity>();
    private Label labDeployment = new Label(Messages
            .getString("CustomMechDialog.labDeployment"), Label.RIGHT); //$NON-NLS-1$
    private Choice choDeployment = new Choice();
    private Label labAutoEject = new Label(Messages
            .getString("CustomMechDialog.labAutoEject"), Label.RIGHT); //$NON-NLS-1$
    private Checkbox chAutoEject = new Checkbox();
    private Label labSearchlight = new Label(Messages
            .getString("CustomMechDialog.labSearchlight"), Label.RIGHT); //$NON-NLS-1$
    private Checkbox chSearchlight = new Checkbox();
    private Label labCommander = new Label(Messages
            .getString("CustomMechDialog.labCommander"), Label.RIGHT); //$NON-NLS-1$
    private Checkbox chCommander = new Checkbox();

    private Label labOffBoard = new Label(Messages
            .getString("CustomMechDialog.labOffBoard"), Label.RIGHT); //$NON-NLS-1$
    private Checkbox chOffBoard = new Checkbox();
    private Label labOffBoardDirection = new Label(Messages
            .getString("CustomMechDialog.labOffBoardDirection"), Label.RIGHT); //$NON-NLS-1$
    private Choice choOffBoardDirection = new Choice();
    private Label labOffBoardDistance = new Label(Messages
            .getString("CustomMechDialog.labOffBoardDistance"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldOffBoardDistance = new TextField(4);
    private Button butOffBoardDistance = new Button("0");

    private Label labTargSys = new Label(Messages
            .getString("CustomMechDialog.labTargSys"), Label.RIGHT);
    private Choice choTargSys = new Choice();

    private Label labStartVelocity = new Label(Messages
            .getString("CustomMechDialog.labStartVelocity"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldStartVelocity = new TextField(3);

    private Label labStartElevation = new Label(Messages
            .getString("CustomMechDialog.labStartElevation"), Label.RIGHT); //$NON-NLS-1$
    private TextField fldStartElevation = new TextField(3);

    private Panel panButtons = new Panel();
    private Button butOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Button butCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
    private Button butNext = new Button(Messages.getString("Next"));
    private Button butPrev = new Button(Messages.getString("Previous"));

    private Vector<MunitionChoicePanel> m_vMunitions = new Vector<MunitionChoicePanel>();
    private Panel panMunitions = new Panel();
    private Vector<RapidfireMGPanel> m_vMGs = new Vector<RapidfireMGPanel>();
    private Panel panRapidfireMGs = new Panel();
    private Vector<MineChoicePanel> m_vMines = new Vector<MineChoicePanel>();
    private Panel panMines = new Panel();
    private Vector<SantaAnnaChoicePanel> m_vSantaAnna = new Vector<SantaAnnaChoicePanel>();
    private Panel panSantaAnna = new Panel();
    private BombChoicePanel m_bombs;
    private Panel panBombs = new Panel();

    private Entity entity;
    private boolean okay = false;
    private ClientGUI clientgui;
    private Client client;

    private PilotOptions options;

    private Vector<DialogOptionComponent> optionComps = new Vector<DialogOptionComponent>();

    private Panel panOptions = new Panel();
    private ScrollPane scrOptions = new ScrollPane();

    private ScrollPane scrAll = new ScrollPane();

    private TextArea texDesc = new TextArea(
            Messages.getString("CustomMechDialog.texDesc"), 3, 35, TextArea.SCROLLBARS_VERTICAL_ONLY); //$NON-NLS-1$

    private boolean editable;

    private int direction = -1;
    private int distance = 17;

    /** Creates new CustomMechDialog */
    public CustomMechDialog(ClientGUI clientgui, Client client, Entity entity,
            boolean editable) {
        super(clientgui.frame,
                Messages.getString("CustomMechDialog.title"), true); //$NON-NLS-1$

        Panel tempPanel = new Panel();
        this.entity = entity;
        this.clientgui = clientgui;
        this.client = client;
        options = entity.getCrew().getOptions();
        this.editable = editable;

        texDesc.setEditable(false);

        if (entity instanceof Tank) {
            labPiloting.setText(Messages
                    .getString("CustomMechDialog.labDriving"));
        } else if (entity instanceof Infantry) {
            labPiloting.setText(Messages
                    .getString("CustomMechDialog.labAntiMech"));
        } else {
            labPiloting.setText(Messages
                    .getString("CustomMechDialog.labPiloting"));
        }

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        tempPanel.setLayout(gridbag);

        tempPanel.add(labName, GBC.std());
        tempPanel.add(fldName, GBC.eol());

        if (client.game.getOptions().booleanOption("rpg_gunnery")) {
            tempPanel.add(labGunneryL, GBC.std());
            tempPanel.add(fldGunneryL, GBC.eol());

            tempPanel.add(labGunneryM, GBC.std());
            tempPanel.add(fldGunneryM, GBC.eol());

            tempPanel.add(labGunneryB, GBC.std());
            tempPanel.add(fldGunneryB, GBC.eol());

        } else {
            tempPanel.add(labGunnery, GBC.std());
            tempPanel.add(fldGunnery, GBC.eol());
        }
        tempPanel.add(labPiloting, GBC.std());
        tempPanel.add(fldPiloting, GBC.eol());

        if (client.game.getOptions().booleanOption("individual_initiative")) {
            tempPanel.add(labInit, GBC.std());
            tempPanel.add(fldInit, GBC.eol());
        }

        if (client.game.getOptions().booleanOption("command_init")) {
            tempPanel.add(labCommandInit, GBC.std());
            tempPanel.add(fldCommandInit, GBC.eol());
        }

        if (entity instanceof Aero) {
            tempPanel.add(labStartVelocity, GBC.std());
            tempPanel.add(fldStartVelocity, GBC.eol());

            tempPanel.add(labStartElevation, GBC.std());
            tempPanel.add(fldStartElevation, GBC.eol());
        }

        // Auto-eject checkbox.
        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            boolean hasEjectSeat = true;
            // torso mounted cockpits don't have an ejection seat
            if (mech.getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
                hasEjectSeat = false;
            }
            if (mech.isIndustrial()) {
                // industrials can only eject when they have an ejection seat
                for (Mounted misc : mech.getMisc()) {
                    if (misc.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                        hasEjectSeat = true;
                    }
                }
            }
            if (hasEjectSeat) {
                tempPanel.add(labAutoEject, GBC.std());
                tempPanel.add(chAutoEject, GBC.eol());
                chAutoEject.setState(!mech.isAutoEject());
            }
        }

        tempPanel.add(labDeployment, GBC.std());
        tempPanel.add(choDeployment, GBC.eol());
        refreshDeployment();

        if (clientgui.getClient().game.getOptions().booleanOption(
                "pilot_advantages") //$NON-NLS-1$
                || clientgui.getClient().game.getOptions().booleanOption(
                        "manei_domini")) { //$NON-NLS-1$
            scrOptions.add(panOptions);
            tempPanel.add(scrOptions, GBC.std());
            tempPanel.add(texDesc, GBC.eol());
        }

        if (entity.hasC3() || entity.hasC3i()) {
            tempPanel.add(labC3, GBC.std());

            tempPanel.add(choC3, GBC.eol());
            refreshC3();
        }
        boolean eligibleForOffBoard = false;
        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
                eligibleForOffBoard = true;
            }
        }
        if (eligibleForOffBoard) {
            tempPanel.add(labOffBoard, GBC.std());
            tempPanel.add(chOffBoard, GBC.eol());
            chOffBoard.setState(entity.isOffBoard());

            tempPanel.add(labOffBoardDirection, GBC.std());
            choOffBoardDirection.add(Messages
                    .getString("CustomMechDialog.North")); //$NON-NLS-1$
            choOffBoardDirection.add(Messages
                    .getString("CustomMechDialog.South")); //$NON-NLS-1$
            choOffBoardDirection.add(Messages
                    .getString("CustomMechDialog.East")); //$NON-NLS-1$
            choOffBoardDirection.add(Messages
                    .getString("CustomMechDialog.West")); //$NON-NLS-1$
            direction = entity.getOffBoardDirection();
            if (IOffBoardDirections.NONE == direction) {
                direction = IOffBoardDirections.NORTH;
            }
            choOffBoardDirection.select(direction);
            tempPanel.add(choOffBoardDirection, GBC.eol());

            tempPanel.add(labOffBoardDistance, GBC.std());
            butOffBoardDistance.setLabel(Integer.toString(distance));
            butOffBoardDistance.addActionListener(this);
            tempPanel.add(butOffBoardDistance, GBC.eol());
        }

        if (!(entity.hasTargComp())
                && (clientgui.getClient().game.getOptions()
                        .booleanOption("allow_level_3_targsys"))
                && ((entity instanceof Mech) || (clientgui.getClient().game
                        .getOptions().booleanOption("tank_level_3_targsys") && (entity instanceof Tank)))
                && !entity.hasC3() && !entity.hasC3i()) {
            tempPanel.add(labTargSys, GBC.std());
            choTargSys.add(MiscType
                    .getTargetSysName(MiscType.T_TARGSYS_STANDARD));
            choTargSys.add(MiscType
                    .getTargetSysName(MiscType.T_TARGSYS_LONGRANGE));
            choTargSys.add(MiscType
                    .getTargetSysName(MiscType.T_TARGSYS_SHORTRANGE));
            choTargSys.add(MiscType
                    .getTargetSysName(MiscType.T_TARGSYS_ANTI_AIR));
            // choTargSys.add(MiscType.getTargetSysName(MiscType.
            // T_TARGSYS_MULTI_TRAC))
            tempPanel.add(choTargSys, GBC.eol());

            choTargSys.select(MiscType
                    .getTargetSysName(entity.getTargSysType()));
        }

        if (entity instanceof Protomech) {
            // All Protomechs have a callsign.
            StringBuffer callsign = new StringBuffer(Messages
                    .getString("CustomMechDialog.Callsign")); //$NON-NLS-1$
            callsign.append(": "); //$NON-NLS-1$
            callsign.append(
                    (char) (this.entity.getUnitNumber() + PreferenceManager
                            .getClientPreferences().getUnitStartChar()))
                    .append('-').append(this.entity.getId());
            labCallsign.setText(callsign.toString());

            // Get the Protomechs of this entity's player
            // that *aren't* in the entity's unit.
            Enumeration<Entity> otherUnitEntities = client.game
                    .getSelectedEntities(new EntitySelector() {
                        private final int ownerId = CustomMechDialog.this.entity
                                .getOwnerId();
                        private final char unitNumber = CustomMechDialog.this.entity
                                .getUnitNumber();

                        public boolean accept(Entity entity) {
                            if ((entity instanceof Protomech)
                                    && (ownerId == entity.getOwnerId())
                                    && (unitNumber != entity.getUnitNumber())) {
                                return true;
                            }
                            return false;
                        }
                    });

            // If we got any other entites, show the unit number controls.
            if (otherUnitEntities.hasMoreElements()) {
                tempPanel.add(labCallsign, GBC.std().anchor(GridBagConstraints.CENTER));
                tempPanel.add(labUnitNum, GBC.std());
                tempPanel.add(choUnitNum, GBC.eol());
                refreshUnitNum(otherUnitEntities);
            } else {
                tempPanel.add(labCallsign, GBC.eol().anchor(GridBagConstraints.CENTER));
            }
        }

        // Can't set up munitions on infantry.
        if (!(entity instanceof Infantry) || (entity instanceof BattleArmor)) {
            setupMunitions();
            tempPanel.add(panMunitions, GBC.eol().anchor(GridBagConstraints.CENTER));
        }

        // set up Santa Annas if using nukes
        if (((entity instanceof Dropship) || (entity instanceof Jumpship))
                && clientgui.getClient().game.getOptions().booleanOption(
                        "at2_nukes")) {
            setupSantaAnna();
            tempPanel.add(panSantaAnna, GBC.eol().anchor(GridBagConstraints.CENTER));
        }

        // set up bombs
        if ((entity instanceof Aero)
                && !((entity instanceof FighterSquadron)
                        || (entity instanceof SmallCraft) || (entity instanceof Jumpship))) {
            setupBombs();
            tempPanel.add(panBombs, GBC.eol().anchor(GridBagConstraints.CENTER));
        }

        // Set up rapidfire mg
        if (clientgui.getClient().game.getOptions().booleanOption(
                "tacops_burst")) { //$NON-NLS-1$
            setupRapidfireMGs();
            tempPanel.add(panRapidfireMGs, GBC.eol().anchor(GridBagConstraints.CENTER));
        }

        // Set up searchlight
        if (clientgui.getClient().game.getPlanetaryConditions().getLight() > PlanetaryConditions.L_DUSK) {
            tempPanel.add(labSearchlight, GBC.std());

            tempPanel.add(chSearchlight, GBC.eol());
            chSearchlight.setState(entity.hasSpotlight());
        }

        // Set up commanders for commander killed victory condition
        if (clientgui.getClient().game.getOptions().booleanOption(
                "commander_killed")) { //$NON-NLS-1$
            tempPanel.add(labCommander, GBC.std());

            tempPanel.add(chCommander, GBC.eol());
            chCommander.setState(entity.isCommander());
        }

        // Set up mines
        setupMines();
        tempPanel.add(panMines, GBC.eol().anchor(GridBagConstraints.CENTER));

        setupButtons();
        tempPanel.add(panButtons, GBC.eol().anchor(GridBagConstraints.CENTER).insets(5, 0, 5,
                5));

        fldName.setText(entity.getCrew().getName());
        fldName.addActionListener(this);
        fldGunnery.setText(new Integer(entity.getCrew().getGunnery())
                .toString());
        fldGunnery.addActionListener(this);
        fldGunneryL.setText(new Integer(entity.getCrew().getGunneryL())
                .toString());
        fldGunneryL.addActionListener(this);
        fldGunneryM.setText(new Integer(entity.getCrew().getGunneryM())
                .toString());
        fldGunneryM.addActionListener(this);
        fldGunneryB.setText(new Integer(entity.getCrew().getGunneryB())
                .toString());
        fldGunneryB.addActionListener(this);
        fldPiloting.setText(new Integer(entity.getCrew().getPiloting())
                .toString());
        fldPiloting.addActionListener(this);
        fldInit.setText(new Integer(entity.getCrew().getInitBonus())
                 .toString());
        fldInit.addActionListener(this);
        fldCommandInit.setText(new Integer(entity.getCrew().getCommandBonus())
                .toString());
        fldCommandInit.addActionListener(this);
        if (entity instanceof Aero) {
            Aero a = (Aero) entity;
            fldStartVelocity.setText(new Integer(a.getCurrentVelocity())
                    .toString());
            fldStartVelocity.addActionListener(this);

            fldStartElevation.setText(new Integer(a.getElevation()).toString());
            fldStartElevation.addActionListener(this);
        }

        if (!editable) {
            fldName.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldGunneryL.setEnabled(false);
            fldGunneryM.setEnabled(false);
            fldGunneryB.setEnabled(false);
            fldPiloting.setEnabled(false);
            fldInit.setEnabled(false);
            fldCommandInit.setEnabled(false);
            choC3.setEnabled(false);
            choDeployment.setEnabled(false);
            chAutoEject.setEnabled(false);
            chSearchlight.setEnabled(false);
            chCommander.setEnabled(false);
            choTargSys.setEnabled(false);
            disableMunitionEditing();
            disableMGSetting();
            disableMineSetting();
            m_bombs.setEnabled(false);
            chOffBoard.setEnabled(false);
            choOffBoardDirection.setEnabled(false);
            fldOffBoardDistance.setEnabled(false);
            fldStartVelocity.setEnabled(false);
            fldStartElevation.setEnabled(false);

        }
        scrAll.add(tempPanel);

        // add the scrollable panel
        this.add(scrAll);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();

        // Why do we have to add all this stuff together to get the
        // right size? I hate GUI programming...especially AWT.
        int w = tempPanel.getPreferredSize().width + scrAll.getInsets().right;
        int h = tempPanel.getPreferredSize().height
                + panButtons.getPreferredSize().height
                + scrAll.getInsets().bottom;
        setLocationAndSize(w, h);
    }

    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        butNext.addActionListener(this);
        butPrev.addActionListener(this);

        // layout
        panButtons.setLayout(new GridLayout(1, 4, 10, 0));
        panButtons.add(butPrev);
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butNext);

        butNext.setEnabled(getNextEntity(true) != null);
        butPrev.setEnabled(getNextEntity(false) != null);
    }

    private void setupRapidfireMGs() {
        GridBagLayout gbl = new GridBagLayout();
        panRapidfireMGs.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) m.getType();
            if (!wtype.hasFlag(WeaponType.F_MG)) {
                continue;
            }
            gbc.gridy = row++;
            RapidfireMGPanel rmp = new RapidfireMGPanel(m);
            gbl.setConstraints(rmp, gbc);
            panRapidfireMGs.add(rmp);
            m_vMGs.addElement(rmp);
        }
    }

    private void setupMines() {
        GridBagLayout gbl = new GridBagLayout();
        panMines.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getMisc()) {
            if (!m.getType().hasFlag((MiscType.F_MINE))) {
                continue;
            }

            gbc.gridy = row++;
            MineChoicePanel mcp = new MineChoicePanel(m);
            gbl.setConstraints(mcp, gbc);
            panMines.add(mcp);
            m_vMines.addElement(mcp);
        }
    }

    private void setupBombs() {
        GridBagLayout gbl = new GridBagLayout();
        panBombs.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        Aero a = (Aero) entity;
        m_bombs = new BombChoicePanel(a.getBombChoices(), a.getMaxBombPoints());
        gbl.setConstraints(m_bombs, gbc);
        panBombs.add(m_bombs);
    }

    private void setupSantaAnna() {
        GridBagLayout gbl = new GridBagLayout();
        panSantaAnna.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getAmmo()) {
            AmmoType at = (AmmoType) m.getType();
            // Santa Annas?
            if (clientgui.getClient().game.getOptions().booleanOption(
                    "at2_nukes")
                    && ((at.getAmmoType() == AmmoType.T_KILLER_WHALE) || ((at
                            .getAmmoType() == AmmoType.T_AR10) && at
                            .hasFlag(AmmoType.F_AR10_KILLER_WHALE)))) {
                gbc.gridy = row++;
                SantaAnnaChoicePanel sacp = new SantaAnnaChoicePanel(m);
                gbl.setConstraints(sacp, gbc);
                panSantaAnna.add(sacp);
                m_vSantaAnna.addElement(sacp);
            }
        }
    }

    private void setupMunitions() {

        GridBagLayout gbl = new GridBagLayout();
        panMunitions.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();

        int row = 0;
        for (Mounted m : entity.getAmmo()) {
            AmmoType at = (AmmoType) m.getType();
            Vector<AmmoType> vTypes = new Vector<AmmoType>();
            Vector<AmmoType> vAllTypes = new Vector<AmmoType>();
            vAllTypes = AmmoType.getMunitionsFor(at.getAmmoType());
            if (vAllTypes == null) {
                continue;
            }

            // don't allow ammo switching of most things for Aeros
            // allow only MML, ATM, NARC, and LBX switching
            // TODO: need a better way to customize munitions on Aeros
            // currently this doesn't allow AR10 and tele-missile launchers
            // to switch back and forth between tele and regular missiles
            // also would be better to not have to add Santa Anna's in such
            // an idiosyncratic fashion
            if ((entity instanceof Aero)
                    && !((at.getAmmoType() == AmmoType.T_MML)
                            || (at.getAmmoType() == AmmoType.T_ATM)
                            || (at.getAmmoType() == AmmoType.T_NARC) || (at
                            .getAmmoType() == AmmoType.T_AC_LBX))) {
                continue;
            }

            for (int x = 0, n = vAllTypes.size(); x < n; x++) {
                AmmoType atCheck = vAllTypes.elementAt(x);
                boolean bTechMatch = TechConstants.isLegal(entity
                        .getTechLevel(), atCheck.getTechLevel());

                // allow all lvl2 IS units to use level 1 ammo
                // lvl1 IS units don't need to be allowed to use lvl1 ammo,
                // because there is no special lvl1 ammo, therefore it doesn't
                // need to show up in this display.
                if (!bTechMatch
                        && (entity.getTechLevel() == TechConstants.T_IS_TW_NON_BOX)
                        && (atCheck.getTechLevel() == TechConstants.T_INTRO_BOXSET)) {
                    bTechMatch = true;
                }

                // if is_eq_limits is unchecked allow l1 guys to use l2 stuff
                if (!clientgui.getClient().game.getOptions().booleanOption(
                        "is_eq_limits") //$NON-NLS-1$
                        && (entity.getTechLevel() == TechConstants.T_INTRO_BOXSET)
                        && (atCheck.getTechLevel() == TechConstants.T_IS_TW_NON_BOX)) {
                    bTechMatch = true;
                }

                // Possibly allow advanced/experimental ammos, possibly not.
                if (clientgui.getClient().game.getOptions().booleanOption(
                        "allow_advanced_ammo")) {
                    if (!clientgui.getClient().game.getOptions().booleanOption(
                            "is_eq_limits")) {
                        if ((entity.getTechLevel() == TechConstants.T_CLAN_TW)
                                && ((atCheck.getTechLevel() == TechConstants.T_CLAN_ADVANCED)
                                        || (atCheck.getTechLevel() == TechConstants.T_CLAN_EXPERIMENTAL) || (atCheck
                                        .getTechLevel() == TechConstants.T_CLAN_UNOFFICIAL))) {
                            bTechMatch = true;
                        }
                        if (((entity.getTechLevel() == TechConstants.T_INTRO_BOXSET) || (entity
                                .getTechLevel() == TechConstants.T_IS_TW_NON_BOX))
                                && ((atCheck.getTechLevel() == TechConstants.T_IS_ADVANCED)
                                        || (atCheck.getTechLevel() == TechConstants.T_IS_EXPERIMENTAL) || (atCheck
                                        .getTechLevel() == TechConstants.T_IS_UNOFFICIAL))) {
                            bTechMatch = true;
                        }
                    }
                } else if ((atCheck.getTechLevel() == TechConstants.T_IS_ADVANCED)
                        || (atCheck.getTechLevel() == TechConstants.T_CLAN_ADVANCED)) {
                    bTechMatch = false;
                }

                // allow mixed Tech Mechs to use both IS and Clan ammo of any
                // level (since mixed tech is always level 3)
                if (entity.isMixedTech()) {
                    bTechMatch = true;
                }

                // If clan_ignore_eq_limits is unchecked,
                // do NOT allow Clans to use IS-only ammo.
                // N.B. play bit-shifting games to allow "incendiary"
                // to be combined to other munition types.
                long muniType = atCheck.getMunitionType();
                muniType &= ~AmmoType.M_INCENDIARY_LRM;
                if (!clientgui.getClient().game.getOptions().booleanOption(
                        "clan_ignore_eq_limits") //$NON-NLS-1$
                        && entity.isClan()
                        && ((muniType == AmmoType.M_SEMIGUIDED)
                                || (muniType == AmmoType.M_SWARM_I)
                                || (muniType == AmmoType.M_FLARE)
                                || (muniType == AmmoType.M_FRAGMENTATION)
                                || (muniType == AmmoType.M_THUNDER_AUGMENTED)
                                || (muniType == AmmoType.M_THUNDER_INFERNO)
                                || (muniType == AmmoType.M_THUNDER_VIBRABOMB)
                                || (muniType == AmmoType.M_THUNDER_ACTIVE)
                                || (muniType == AmmoType.M_INFERNO_IV)
                                || (muniType == AmmoType.M_VIBRABOMB_IV)
                                || (muniType == AmmoType.M_LISTEN_KILL) || (muniType == AmmoType.M_ANTI_TSM))) {
                    bTechMatch = false;
                }

                if (!clientgui.getClient().game.getOptions().booleanOption(
                        "minefields") && //$NON-NLS-1$
                        AmmoType.canDeliverMinefield(atCheck)) {
                    continue;
                }

                // Only Protos can use Proto-specific ammo
                if (atCheck.hasFlag(AmmoType.F_PROTOMECH)
                        && !(entity instanceof Protomech)) {
                    continue;
                }

                // When dealing with machine guns, Protos can only
                // use proto-specific machine gun ammo
                if ((entity instanceof Protomech)
                        && atCheck.hasFlag(AmmoType.F_MG)
                        && !atCheck.hasFlag(AmmoType.F_PROTOMECH)) {
                    continue;
                }

                // Battle Armor ammo can't be selected at all.
                // All other ammo types need to match on rack size and tech.
                if (bTechMatch
                        && (atCheck.getRackSize() == at.getRackSize())
                        && (atCheck.hasFlag(AmmoType.F_BATTLEARMOR) == at
                                .hasFlag(AmmoType.F_BATTLEARMOR))
                        && (atCheck.hasFlag(AmmoType.F_ENCUMBERING) == at
                                .hasFlag(AmmoType.F_ENCUMBERING))
                        && (atCheck.getTonnage(entity) == at.getTonnage(entity))) {
                    vTypes.addElement(atCheck);
                }
            }
            if ((vTypes.size() < 2)
                    && !client.game.getOptions().booleanOption(
                            "lobby_ammo_dump")
                    && !client.game.getOptions()
                            .booleanOption("tacops_hotload")) { //$NON-NLS-1$
                continue;
            }

            gbc.gridy = row++;
            // Protomechs need special choice panels.
            MunitionChoicePanel mcp;
            if (entity instanceof Protomech) {
                mcp = new ProtomechMunitionChoicePanel(m, vTypes);
            } else {
                mcp = new MunitionChoicePanel(m, vTypes);
            }
            gbl.setConstraints(mcp, gbc);
            panMunitions.add(mcp);
            m_vMunitions.addElement(mcp);
        }
    }

    class MineChoicePanel extends Panel {
        /**
         *
         */
        private static final long serialVersionUID = 7164680650764583622L;
        private Choice m_choice;
        private Mounted m_mounted;

        public MineChoicePanel(Mounted m) {
            m_mounted = m;
            m_choice = new Choice();
            m_choice.add(Messages.getString("CustomMechDialog.Conventional")); //$NON-NLS-1$
            m_choice.add(Messages.getString("CustomMechDialog.Vibrabomb")); //$NON-NLS-1$
            m_choice.add(Messages.getString("CustomMechDialog.Active")); //$NON-NLS-1$
            m_choice.add(Messages.getString("CustomMechDialog.Inferno")); //$NON-NLS-1$
            // m_choice.add("Messages.getString("CustomMechDialog.Command-
            // detonated"));
            // //$NON-NLS-1$
            int loc;
            loc = m.getLocation();
            String sDesc = "(" + entity.getLocationAbbr(loc) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            Label lLoc = new Label(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(lLoc, c);
            add(lLoc);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(m_choice, c);
            m_choice.select(m.getMineType());
            add(m_choice);
        }

        public void applyChoice() {
            m_mounted.setMineType(m_choice.getSelectedIndex());
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_choice.setEnabled(enabled);
        }
    }

    class MunitionChoicePanel extends Panel {
        /**
         *
         */
        private static final long serialVersionUID = 5264839073432470450L;
        private Vector<AmmoType> m_vTypes;
        private Choice m_choice;
        private Mounted m_mounted;

        protected Label labDump = new Label(Messages
                .getString("CustomMechDialog.labDump")); //$NON-NLS-1$
        protected Checkbox chDump = new Checkbox();
        protected Label labHotLoad = new Label(Messages
                .getString("CustomMechDialog.switchToHotLoading")); //$NON-NLS-1$
        protected Checkbox chHotLoad = new Checkbox();

        public MunitionChoicePanel(Mounted m, Vector<AmmoType> vTypes) {
            m_vTypes = vTypes;
            m_mounted = m;
            AmmoType curType = (AmmoType) m.getType();
            m_choice = new Choice();
            Enumeration<AmmoType> e = m_vTypes.elements();
            for (int x = 0; e.hasMoreElements(); x++) {
                AmmoType at = e.nextElement();
                m_choice.add(at.getName());
                if (at.getInternalName() == curType.getInternalName()) {
                    m_choice.select(x);
                }
            }
            int loc;
            if (m.getLocation() == Entity.LOC_NONE) {
                // oneshot weapons don't have a location of their own
                Mounted linkedBy = m.getLinkedBy();
                loc = linkedBy.getLocation();
            } else {
                loc = m.getLocation();
            }
            String sDesc = "(" + entity.getLocationAbbr(loc) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            Label lLoc = new Label(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(lLoc, c);
            add(lLoc);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(m_choice, c);
            add(m_choice);
            if (clientgui.getClient().game.getOptions().booleanOption(
                    "lobby_ammo_dump")) { //$NON-NLS-1$
                c.gridx = 0;
                c.gridy = 1;
                c.anchor = GridBagConstraints.EAST;
                g.setConstraints(labDump, c);
                add(labDump);
                c.gridx = 1;
                c.gridy = 1;
                c.anchor = GridBagConstraints.WEST;
                g.setConstraints(chDump, c);
                add(chDump);
                if (clientgui.getClient().game.getOptions().booleanOption(
                        "tacops_hotload")
                        && curType.hasFlag(AmmoType.F_HOTLOAD)) {
                    c.gridx = 0;
                    c.gridy = 2;
                    c.anchor = GridBagConstraints.EAST;
                    g.setConstraints(labHotLoad, c);
                    add(labHotLoad);
                    c.gridx = 1;
                    c.gridy = 2;
                    c.anchor = GridBagConstraints.WEST;
                    g.setConstraints(chHotLoad, c);
                    add(chHotLoad);
                }
            } else if (clientgui.getClient().game.getOptions().booleanOption(
                    "tacops_hotload")
                    && curType.hasFlag(AmmoType.F_HOTLOAD)) {
                c.gridx = 0;
                c.gridy = 1;
                c.anchor = GridBagConstraints.EAST;
                g.setConstraints(labHotLoad, c);
                add(labHotLoad);
                c.gridx = 1;
                c.gridy = 1;
                c.anchor = GridBagConstraints.WEST;
                g.setConstraints(chHotLoad, c);
                add(chHotLoad);
            }
        }

        public void applyChoice() {
            int n = m_choice.getSelectedIndex();
            AmmoType at = m_vTypes.elementAt(n);
            m_mounted.changeAmmoType(at);
            if (chDump.getState()) {
                m_mounted.setShotsLeft(0);
            }
            if (clientgui.getClient().game.getOptions().booleanOption(
                    "tacops_hotload")) {
                if (chHotLoad.getState() != m_mounted.isHotLoaded()) {
                    m_mounted.setHotLoad(chHotLoad.getState());
                }
            }

        }

        @Override
        public void setEnabled(boolean enabled) {
            m_choice.setEnabled(enabled);
        }

        /**
         * Get the number of shots in the mount.
         *
         * @return the <code>int</code> number of shots in the mount.
         */
        /* package */int getShotsLeft() {
            return m_mounted.getShotsLeft();
        }

        /**
         * Set the number of shots in the mount.
         *
         * @param shots
         *            the <code>int</code> number of shots for the mount.
         */
        /* package */void setShotsLeft(int shots) {
            m_mounted.setShotsLeft(shots);
        }
    }

    /**
     * a choice panel for determining number of santa anna warheads
     */
    class SantaAnnaChoicePanel extends Panel {
        /**
         *
         */
        private static final long serialVersionUID = 4976381641641260454L;
        private Choice m_choice;
        private Mounted m_mounted;

        public SantaAnnaChoicePanel(Mounted m) {
            m_mounted = m;
            m_choice = new Choice();
            for (int i = 0; i <= m_mounted.getShotsLeft(); i++) {
                m_choice.add(Integer.toString(i));
            }
            int loc;
            loc = m.getLocation();
            String sDesc = "Nuclear warheads for " + m_mounted.getName() + " (" + entity.getLocationAbbr(loc) + "):"; //$NON-NLS-1$ //$NON-NLS-2$
            Label lLoc = new Label(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(lLoc, c);
            add(lLoc);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(m_choice, c);
            m_choice.select(m.getNSantaAnna());
            add(m_choice);
        }

        public void applyChoice() {
            //this is a hack. I can't immediately apply the choice, because
            //that would split this ammo bin in two and then the player could never
            //get back to it. So I keep track of the Santa Anna allocation
            //on the mounted and then apply it before deployment
            m_mounted.setNSantaAnna(m_choice.getSelectedIndex());

        }

        @Override
        public void setEnabled(boolean enabled) {
            m_choice.setEnabled(enabled);
        }
    }

    /**
     * bombs
     */
    class BombChoicePanel extends Panel implements ItemListener {
        /**
         *
         */
        private static final long serialVersionUID = 8294210679667925079L;
        private Choice[] b_choices = new Choice[BombType.B_NUM];
        private Label[] b_labels = new Label[BombType.B_NUM];
        private int maxPoints = 0;
        private int maxRows = (int)Math.ceil(BombType.B_NUM / 2.0);

        public BombChoicePanel(int[] bombChoices, int maxBombPoints) {
            // b_vTypes = vTypes;
            maxPoints = maxBombPoints;

            // how many bomb points am I currently using?
            int curBombPoints = 0;
            for (int i = 0; i < bombChoices.length; i++) {
                curBombPoints += bombChoices[i] * BombType.getBombCost(i);
            }
            int availBombPoints = maxBombPoints - curBombPoints;

            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();

            int column = 0;
            int row = 0;
            for(int type = 0; type < BombType.B_NUM; type++) {

                b_labels[type] = new Label();
                b_choices[type] = new Choice();

                for (int x = 0; x <= Math.max(Math.round(availBombPoints / BombType.getBombCost(type)),
                        bombChoices[type]); x++) {
                    b_choices[type].add(Integer.toString(x));
                }

                b_choices[type].select(bombChoices[type]);
                b_labels[type].setText(BombType.getBombName(type));
                b_choices[type].addItemListener(this);

                if((type == BombType.B_ALAMO) && !client.game.getOptions().booleanOption("at2_nukes")) {
                    b_choices[type].setEnabled(false);
                }
                if((type > BombType.B_TAG) && !client.game.getOptions().booleanOption("allow_advanced_ammo")) {
                    b_choices[type].setEnabled(false);
                }
                if((type == BombType.B_ASEW) || (type == BombType.B_ALAMO) || (type == BombType.B_TAG)) {
                    b_choices[type].setEnabled(false);
                }

                if(row >= maxRows) {
                    row = 0;
                    column += 2;
                }

                c.gridx = column;
                c.gridy = row;
                c.anchor = GridBagConstraints.EAST;
                g.setConstraints(b_labels[type], c);
                add(b_labels[type]);

                c.gridx = column + 1;
                c.gridy = row;
                c.anchor = GridBagConstraints.WEST;
                g.setConstraints(b_choices[type], c);
                add(b_choices[type]);
                row++;
            }
        }

        public void itemStateChanged(ItemEvent ie) {

            int[] current = new int[BombType.B_NUM];
            int curPoints= 0;
            for(int type = 0; type < BombType.B_NUM; type++) {
                current[type] = b_choices[type].getSelectedIndex();
                curPoints += current[type] * BombType.getBombCost(type);
            }

            int availBombPoints = maxPoints - curPoints;

            for(int type = 0; type < BombType.B_NUM; type++) {
                b_choices[type].removeItemListener(this);
                b_choices[type].removeAll();
                for (int x = 0; x <= Math.max(Math.round(availBombPoints / BombType.getBombCost(type)),
                        current[type]); x++) {
                    b_choices[type].add(Integer.toString(x));
                }
                b_choices[type].select(current[type]);
                b_choices[type].addItemListener(this);
            }
        }

        public void applyChoice() {
            int[] choices = new int[BombType.B_NUM];
            for(int type = 0; type < BombType.B_NUM; type++) {
                choices[type] = b_choices[type].getSelectedIndex();
            }
            ((Aero) entity).setBombChoices(choices);

        }

        @Override
        public void setEnabled(boolean enabled) {
            for(int type = 0; type < BombType.B_NUM; type++) {
                if((type == BombType.B_ALAMO) && !client.game.getOptions().booleanOption("at2_nukes")) {
                    b_choices[type].setEnabled(false);
                }
                else if((type > BombType.B_TAG) && !client.game.getOptions().booleanOption("allow_advanced_ammo")) {
                    b_choices[type].setEnabled(false);
                }
                else if((type == BombType.B_ASEW) || (type == BombType.B_ALAMO) || (type == BombType.B_TAG)) {
                    b_choices[type].setEnabled(false);
                }
                else {
                    b_choices[type].setEnabled(false);
                }
            }
        }

    }

    /**
     * When a Protomech selects ammo, you need to adjust the shots on the unit
     * for the weight of the selected munition.
     */
    class ProtomechMunitionChoicePanel extends MunitionChoicePanel {
        /**
         *
         */
        private static final long serialVersionUID = 4915594909134005147L;
        private final float m_origShotsLeft;
        private final AmmoType m_origAmmo;

        public ProtomechMunitionChoicePanel(Mounted m, Vector<AmmoType> vTypes) {
            super(m, vTypes);
            m_origAmmo = (AmmoType) m.getType();
            m_origShotsLeft = m.getShotsLeft();
        }

        /**
         * All ammo must be applied in ratios to the starting load.
         */
        @Override
        public void applyChoice() {
            super.applyChoice();

            // Calculate the number of shots for the new ammo.
            // N.B. Some special ammos are twice as heavy as normal
            // so they have half the number of shots (rounded down).
            setShotsLeft(Math.round(getShotsLeft() * m_origShotsLeft
                    / m_origAmmo.getShots()));
            if (chDump.getState()) {
                setShotsLeft(0);
            }
        }
    }

    class RapidfireMGPanel extends Panel {
        /**
         *
         */
        private static final long serialVersionUID = -5884253129817172942L;

        private Mounted m_mounted;

        protected Checkbox chRapid = new Checkbox();

        public RapidfireMGPanel(Mounted m) {
            m_mounted = m;
            int loc = m.getLocation();
            String sDesc = Messages
                    .getString(
                            "CustomMechDialog.switchToRapidFire", new Object[] { entity.getLocationAbbr(loc) }); //$NON-NLS-1$
            Label labRapid = new Label(sDesc);
            GridBagLayout g = new GridBagLayout();
            setLayout(g);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.EAST;
            g.setConstraints(labRapid, c);
            add(labRapid);
            c.gridx = 1;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            g.setConstraints(chRapid, c);
            chRapid.setState(m.isRapidfire());
            add(chRapid);
        }

        public void applyChoice() {
            boolean b = chRapid.getState();
            m_mounted.setRapidfire(b);
        }

        @Override
        public void setEnabled(boolean enabled) {
            chRapid.setEnabled(enabled);
        }
    }

    public void disableMunitionEditing() {
        for (int i = 0; i < m_vMunitions.size(); i++) {
            m_vMunitions.elementAt(i).setEnabled(false);
        }
    }

    public void disableMGSetting() {
        for (int i = 0; i < m_vMGs.size(); i++) {
            m_vMGs.elementAt(i).setEnabled(false);
        }
    }

    public void disableMineSetting() {
        for (int i = 0; i < m_vMines.size(); i++) {
            m_vMines.elementAt(i).setEnabled(false);
        }
    }

    public void setOptions() {
        IOption option;
        for (DialogOptionComponent comp : optionComps) {
         option = comp.getOption();
         if ((comp.getValue() == Messages.getString("CustomMechDialog.None"))) { // NON
                                                                            // -
                                                                            // NLS
                                                                            // -
                                                                            // $1
        entity.getCrew().getOptions().getOption(option.getName())
                .setValue("None"); // NON-NLS-$1
         } else {
        entity.getCrew().getOptions().getOption(option.getName())
                .setValue(comp.getValue());
         }
      }
    }

    public void resetOptions() {
        IOption option;
        for (DialogOptionComponent comp : optionComps) {
         option = comp.getOption();
         option.setValue(false);
         entity.getCrew().getOptions().getOption(option.getName()).setValue(
            comp.getValue());
      }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new Vector<DialogOptionComponent>();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridbag);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES)
                    && !clientgui.getClient().game.getOptions().booleanOption(
                            "pilot_advantages")) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES)
                    && !clientgui.getClient().game.getOptions().booleanOption(
                            "manei_domini")) {
                continue;
            }

            addGroup(group, gridbag, c);

            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption option = j.nextElement();
                
                if(entity instanceof GunEmplacement) {
                    continue;
                }
                
                // a bunch of stuf should get disabled for conv infantry
                if (((entity instanceof Infantry && !(entity instanceof BattleArmor)))
                        && (option.getName().equals("vdni") 
                                || option.getName().equals("bvdni"))) {
                    continue;
                }
                
                //a bunch of stuff should get disabled for all but conventional infantry
                if(!(entity instanceof Infantry && !(entity instanceof BattleArmor)) 
                        && (option.getName().equals("grappler") 
                                || option.getName().equals("pl_masc")
                                || option.getName().equals("cyber_eye_im")
                                || option.getName().equals("cyber_eye_tele"))) {
                    continue;
                }

                addOption(option, gridbag, c, editable);
            }
        }

        validate();
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridbag,
            GridBagConstraints c) {
        Label groupLabel = new Label(group.getDisplayableName());

        gridbag.setConstraints(groupLabel, c);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridbag,
            GridBagConstraints c, boolean editable) {
        DialogOptionComponent optionComp = new DialogOptionComponent(this,
                option, editable);

        if (option.getName().equals("weapon_specialist")) { //$NON-NLS-1$
            optionComp.addValue(Messages.getString("CustomMechDialog.None")); //$NON-NLS-1$
            TreeSet<String> uniqueWeapons = new TreeSet<String>();
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                Mounted m = entity.getWeaponList().get(i);
                uniqueWeapons.add(m.getName());
            }
            for (String name : uniqueWeapons) {
                optionComp.addValue(name);

            }
            optionComp.setSelected(option.stringValue());
        }

        gridbag.setConstraints(optionComp, c);
        panOptions.add(optionComp);

        optionComps.addElement(optionComp);
    }

    public void showDescFor(IOption option) {
        texDesc.setText(option.getDescription());
    }

    // TODO : implement me!!!
    public void optionClicked(DialogOptionComponent comp, IOption option,
            boolean state) {
    }

    public boolean isOkay() {
        return okay;
    }

    private void refreshDeployment() {
        choDeployment.removeAll();
        choDeployment.add(Messages.getString("CustomMechDialog.StartOfGame")); //$NON-NLS-1$

        if (entity.getDeployRound() < 1) {
            choDeployment.select(0);
        }

        for (int i = 1; i <= 15; i++) {
            choDeployment
                    .add(Messages.getString("CustomMechDialog.AfterRound") + i); //$NON-NLS-1$

            if (entity.getDeployRound() == i) {
                choDeployment.select(i);
            }
        }
    }

    private void refreshC3() {
        choC3.removeAll();
        int listIndex = 0;
        entityCorrespondance = new int[client.game.getNoOfEntities() + 2];

        if (entity.hasC3i()) {
            choC3.add(Messages.getString("CustomMechDialog.CreateNewNetwork")); //$NON-NLS-1$
            if (entity.getC3Master() == null) {
                choC3.select(listIndex);
            }
            entityCorrespondance[listIndex++] = entity.getId();
        } else if (entity.hasC3MM()) {
            int mNodes = entity.calculateFreeC3MNodes();
            int sNodes = entity.calculateFreeC3Nodes();

            choC3
                    .add(Messages
                            .getString(
                                    "CustomMechDialog.setCompanyMaster", new Object[] { new Integer(mNodes), new Integer(sNodes) })); //$NON-NLS-1$

            if (entity.C3MasterIs(entity)) {
                choC3.select(listIndex);
            }
            entityCorrespondance[listIndex++] = entity.getId();

            choC3
                    .add(Messages
                            .getString(
                                    "CustomMechDialog.setIndependentMaster", new Object[] { new Integer(sNodes) })); //$NON-NLS-1$
            if (entity.getC3Master() == null) {
                choC3.select(listIndex);
            }
            entityCorrespondance[listIndex++] = -1;

        }

        else if (entity.hasC3M()) {
            int nodes = entity.calculateFreeC3Nodes();

            choC3
                    .add(Messages
                            .getString(
                                    "CustomMechDialog.setCompanyMaster1", new Object[] { new Integer(nodes) })); //$NON-NLS-1$
            if (entity.C3MasterIs(entity)) {
                choC3.select(listIndex);
            }
            entityCorrespondance[listIndex++] = entity.getId();

            choC3
                    .add(Messages
                            .getString(
                                    "CustomMechDialog.setIndependentMaster", new Object[] { new Integer(nodes) })); //$NON-NLS-1$
            if (entity.getC3Master() == null) {
                choC3.select(listIndex);
            }
            entityCorrespondance[listIndex++] = -1;

        }
        for (Enumeration<Entity> i = client.getEntities(); i.hasMoreElements();) {
            final Entity e = i.nextElement();
            // ignore enemies or self
            if (entity.isEnemyOf(e) || entity.equals(e)) {
                continue;
            }
            // c3i only links with c3i
            if (entity.hasC3i() != e.hasC3i()) {
                continue;
            }
            // maximum depth of a c3 network is 2 levels.
            Entity eCompanyMaster = e.getC3Master();
            if ((eCompanyMaster != null)
                    && (eCompanyMaster.getC3Master() != eCompanyMaster)) {
                continue;
            }
            int nodes = e.calculateFreeC3Nodes();
            if (e.hasC3MM() && entity.hasC3M() && e.C3MasterIs(e)) {
                nodes = e.calculateFreeC3MNodes();
            }
            if (entity.C3MasterIs(e) && !entity.equals(e)) {
                nodes++;
            }
            if (entity.hasC3i()
                    && (entity.onSameC3NetworkAs(e) || entity.equals(e))) {
                nodes++;
            }
            if (nodes == 0) {
                continue;
            }
            if (e.hasC3i()) {
                if (entity.onSameC3NetworkAs(e)) {
                    choC3
                            .add(Messages
                                    .getString(
                                            "CustomMechDialog.join1", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1) })); //$NON-NLS-1$
                    choC3.select(listIndex);
                } else {
                    choC3
                            .add(Messages
                                    .getString(
                                            "CustomMechDialog.join2", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes) })); //$NON-NLS-1$
                }
                entityCorrespondance[listIndex++] = e.getId();
            } else if (e.C3MasterIs(e) && e.hasC3MM()) {
                // Company masters with 2 computers can have
                // *both* sub-masters AND slave units.
                choC3
                        .add(Messages
                                .getString(
                                        "CustomMechDialog.connect2", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes) })); //$NON-NLS-1$
                entityCorrespondance[listIndex] = e.getId();
                if (entity.C3MasterIs(e)) {
                    choC3.select(listIndex);
                }
                listIndex++;
            } else if (e.C3MasterIs(e) && !entity.hasC3M()) {
                // If we're a slave-unit, we can only connect to sub-masters,
                // not main masters likewise, if we're a master unit, we can
                // only connect to main master units, not sub-masters.
            } else if (entity.C3MasterIs(e)) {
                choC3
                        .add(Messages
                                .getString(
                                        "CustomMechDialog.connect1", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes - 1) })); //$NON-NLS-1$
                choC3.select(listIndex);
                entityCorrespondance[listIndex++] = e.getId();
            } else {
                choC3
                        .add(Messages
                                .getString(
                                        "CustomMechDialog.connect2", new Object[] { e.getDisplayName(), e.getC3NetId(), new Integer(nodes) })); //$NON-NLS-1$
                entityCorrespondance[listIndex++] = e.getId();
            }
        }
    }

    /**
     * Populate the list of entities in other units from the given enumeration.
     *
     * @param others
     *            the <code>Enumeration</code> containing entities in other
     *            units.
     */
    private void refreshUnitNum(Enumeration<Entity> others) {
        // Clear the list of old values
        choUnitNum.removeAll();
        entityUnitNum.removeAllElements();

        // Make an entry for "no change".
        choUnitNum.add(Messages.getString("CustomMechDialog.doNotSwapUnits")); //$NON-NLS-1$
        entityUnitNum.addElement(entity);

        // Walk through the other entities.
        while (others.hasMoreElements()) {
            // Track the position of the next other entity.
            final Entity other = others.nextElement();
            entityUnitNum.addElement(other);

            // Show the other entity's name and callsign.
            StringBuffer callsign = new StringBuffer(other.getDisplayName());
            callsign
                    .append(" (") //$NON-NLS-1$
                    .append(
                            (char) (other.getUnitNumber() + PreferenceManager
                                    .getClientPreferences().getUnitStartChar()))
                    .append('-').append(other.getId()).append(')');
            choUnitNum.add(callsign.toString());
        }
        choUnitNum.select(0);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOffBoardDistance) {
            int maxDistance = 19 * 17; // Long Tom
            for (Mounted wep : entity.getWeaponList()) {
                EquipmentType e = wep.getType();
                WeaponType w = (WeaponType) e;
                if (w.hasFlag(WeaponType.F_ARTILLERY)) {
                    int nDistance = (w.getLongRange() - 1) * 17;
                    if (nDistance < maxDistance) {
                        maxDistance = nDistance;
                    }
                }
            }
            Slider sl = new Slider(
                    clientgui.frame,
                    Messages
                            .getString("CustomMechDialog.offboardDistanceTitle"),
                    Messages
                            .getString("CustomMechDialog.offboardDistanceQuestion"),
                            Math.min(Math.max(entity.getOffBoardDistance(), 17), maxDistance), 17, maxDistance);
            if (!sl.showDialog()) {
                return;
            }
            distance = sl.getValue();
            butOffBoardDistance.setLabel(Integer.toString(distance));
            return;
        }
        if (actionEvent.getSource() != butCancel) {
            // get values
            String name = fldName.getText();
            int gunnery;
            int gunneryL;
            int gunneryM;
            int gunneryB;
            int piloting;
            int init = 0;
            int command = 0;
            int velocity = 0;
            int elev = 0;
            ;
            int offBoardDistance;
            boolean autoEject = chAutoEject.getState();
            try {
                gunnery = Integer.parseInt(fldGunnery.getText());
                gunneryL = Integer.parseInt(fldGunneryL.getText());
                gunneryM = Integer.parseInt(fldGunneryM.getText());
                gunneryB = Integer.parseInt(fldGunneryB.getText());
                piloting = Integer.parseInt(fldPiloting.getText());
                init = Integer.parseInt(fldInit.getText());
                command = Integer.parseInt(fldCommandInit.getText());
                if (entity instanceof Aero) {
                    velocity = Integer.parseInt(fldStartVelocity.getText());
                    elev = Integer.parseInt(fldStartElevation.getText());
                }
            } catch (NumberFormatException e) {
                new AlertDialog(
                        clientgui.frame,
                        Messages
                                .getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterValidSkills")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            // keep these reasonable, please
            if ((gunnery < 0) || (gunnery > 8) || (piloting < 0) || (piloting > 8)
                    || (gunneryL < 0) || (gunneryL > 8) || (gunneryM < 0)
                    || (gunneryM > 8) || (gunneryB < 0) || (gunneryB > 8)) {
                new AlertDialog(
                        clientgui.frame,
                        Messages
                                .getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterSkillsBetween0_8")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            if ((velocity > (2 * entity.getWalkMP())) || (velocity < 0)) {
                new AlertDialog(
                        clientgui.frame,
                        Messages
                                .getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterCorrectVelocity")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            if ((elev < 0) || (elev > 10)) {
                new AlertDialog(
                        clientgui.frame,
                        Messages
                                .getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterCorrectElev")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            if (chOffBoard.getState()) {
                try {
                    offBoardDistance = distance;
                } catch (NumberFormatException e) {
                    new AlertDialog(
                            clientgui.frame,
                            Messages
                                    .getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.EnterValidSkills")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
                if (offBoardDistance < 17) {
                    new AlertDialog(
                            clientgui.frame,
                            Messages
                                    .getString("CustomMechDialog.NumberFormatError"), Messages.getString("CustomMechDialog.OffboardDistance")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                    return;
                }
                entity.setOffBoard(offBoardDistance, choOffBoardDirection
                        .getSelectedIndex());
            } else {
                entity.setOffBoard(0, Entity.NONE);
            }

            // change entity
            if (client.game.getOptions().booleanOption("rpg_gunnery")) {
                entity.setCrew(new Pilot(name, gunneryL, gunneryM, gunneryB,
                        piloting));
            } else {
                entity.setCrew(new Pilot(name, gunnery, piloting));
            }
            entity.getCrew().setInitBonus(init);
            entity.getCrew().setCommandBonus(command);
            if (entity instanceof Mech) {
                Mech mech = (Mech) entity;
                mech.setAutoEject(!autoEject);
            }
            if (entity instanceof Aero) {
                Aero a = (Aero) entity;
                a.setCurrentVelocity(velocity);
                a.setNextVelocity(velocity);
                a.setElevation(elev);
            }

            // Update the entity's targeting system type.
            if (!(entity.hasTargComp())
                    && (clientgui.getClient().game.getOptions()
                            .booleanOption("allow_level_3_targsys"))) {
                int targSysIndex = MiscType.T_TARGSYS_STANDARD;
                if (choTargSys.getSelectedItem() != null) {
                    targSysIndex = MiscType.getTargetSysType(choTargSys
                            .getSelectedItem());
                }
                if (targSysIndex >= 0) {
                    entity.setTargSysType(targSysIndex);
                } else {
                    System.err.println("Illegal targeting system index: "
                            + targSysIndex);
                    entity.setTargSysType(MiscType.T_TARGSYS_STANDARD);
                }
            }

            // If the player wants to swap unit numbers, update both
            // entities and send an update packet for the other entity.
            if (!entityUnitNum.isEmpty() && (choUnitNum.getSelectedIndex() > 0)) {
                Entity other = entityUnitNum.elementAt(choUnitNum
                        .getSelectedIndex());
                char temp = entity.getUnitNumber();
                entity.setUnitNumber(other.getUnitNumber());
                other.setUnitNumber(temp);
                client.sendUpdateEntity(other);
            }

            // Set the entity's deployment round.
            // entity.setDeployRound((choDeployment.getSelectedIndex() ==
            // 0?0:choDeployment.getSelectedIndex()+1));
            entity.setDeployRound(choDeployment.getSelectedIndex());

            // update munitions selections
            for (MunitionChoicePanel munitionChoicePanel : m_vMunitions) {
            munitionChoicePanel.applyChoice();
         }
            // update MG rapid fire settings
            for (RapidfireMGPanel rapidfireMGPanel : m_vMGs) {
            rapidfireMGPanel.applyChoice();
         }
            // update mines setting
            for (MineChoicePanel mineChoicePanel : m_vMines) {
            mineChoicePanel.applyChoice();
         }
            // update Santa Anna setting
            for (SantaAnnaChoicePanel santaAnnaChoicePanel : m_vSantaAnna) {
            santaAnnaChoicePanel.applyChoice();
         }
            // update bomb setting
            if (null != m_bombs) {
                m_bombs.applyChoice();
            }
            // update searchlight setting
            entity.setSpotlight(chSearchlight.getState());
            entity.setSpotlightState(chSearchlight.getState());

            // update commander status
            entity.setCommander(chCommander.getState());

            setOptions();
            
            if (entity.hasC3() && (choC3.getSelectedIndex() > -1)) {
                Entity chosen = client.getEntity(entityCorrespondance[choC3
                        .getSelectedIndex()]);
                int entC3nodeCount = client.game.getC3SubNetworkMembers(entity)
                        .size();
                int choC3nodeCount = client.game.getC3NetworkMembers(chosen)
                        .size();
                if (entC3nodeCount + choC3nodeCount <= Entity.MAX_C3_NODES) {
                    entity.setC3Master(chosen);
                } else {
                    String message = Messages
                            .getString(
                                    "CustomMechDialog.NetworkTooBig.message", new Object[] { //$NON-NLS-1$
                                    entity.getShortName(),
                                            chosen.getShortName(),
                                            new Integer(entC3nodeCount),
                                            new Integer(choC3nodeCount),
                                            new Integer(Entity.MAX_C3_NODES) });
                    clientgui.doAlertDialog(Messages
                            .getString("CustomMechDialog.NetworkTooBig.title"), //$NON-NLS-1$
                            message);
                    refreshC3();
                }
            } else if (entity.hasC3i() && (choC3.getSelectedIndex() > -1)) {
                entity.setC3NetId(client.getEntity(entityCorrespondance[choC3
                        .getSelectedIndex()]));
            }
            
            if(entity instanceof BattleArmor) {
                //have to reset internals because of dermal armor option
                if(entity.crew.getOptions().booleanOption("dermal_armor")) {
                    ((BattleArmor)entity).setInternal(2);
                } else {
                    ((BattleArmor)entity).setInternal(1);
                }
            } else if(entity instanceof Infantry) {
                //need to reset armor on conventional infantry
                if(entity.crew.getOptions().booleanOption("dermal_armor")) {
                    entity.initializeArmor(entity.getOInternal(Infantry.LOC_INFANTRY), Infantry.LOC_INFANTRY);
                } else {
                    entity.initializeArmor(0, Infantry.LOC_INFANTRY);
                }
            }

            okay = true;
            clientgui.chatlounge.refreshEntities();
        }
        setVisible(false);
        Entity nextOne = null;
        if (actionEvent.getSource() == butPrev) {
            nextOne = getNextEntity(false);
        } else if (actionEvent.getSource() == butNext) {
            nextOne = getNextEntity(true);
        }
        if (nextOne != null) {
            clientgui.chatlounge.customizeMech(nextOne);
        }
    }

    private Entity getNextEntity(boolean forward) {
        IGame game = client.game;
        boolean bd = game.getOptions().booleanOption("blind_drop"); //$NON-NLS-1$
        boolean rbd = game.getOptions().booleanOption("real_blind_drop"); //$NON-NLS-1$
        Player p = client.getLocalPlayer();

        Entity nextOne = null;
        if (forward) {
            nextOne = game.getNextEntityFromList(entity);
        } else {
            nextOne = game.getPreviousEntityFromList(entity);
        }
        while ((nextOne != entity) && (nextOne != null)) {
            if (nextOne.getOwner().equals(p) || !(bd || rbd)) {
                return nextOne;
            }
            if (forward) {
                nextOne = game.getNextEntityFromList(nextOne);
            } else {
                nextOne = game.getPreviousEntityFromList(nextOne);
            }
        }
        return null;
    }
}
