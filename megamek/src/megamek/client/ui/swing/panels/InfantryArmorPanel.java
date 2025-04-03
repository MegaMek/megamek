package megamek.client.ui.swing.panels;

import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.codeUtilities.MathUtility;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.MiscType;
import megamek.common.SimpleTechLevel;
import megamek.common.options.OptionsConstants;

public class InfantryArmorPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = -909995917737642853L;

    final private Infantry infantry;

    JLabel labArmor = new JLabel(Messages.getString("CustomMekDialog.labInfantryArmor"));
    JLabel labDivisor = new JLabel(Messages.getString("CustomMekDialog.labDamageDivisor"));
    JLabel labEncumber = new JLabel(Messages.getString("CustomMekDialog.labEncumber"));
    JLabel labSpaceSuit = new JLabel(Messages.getString("CustomMekDialog.labSpaceSuit"));
    JLabel labDEST = new JLabel(Messages.getString("CustomMekDialog.labDEST"));
    JLabel labSneakCamo = new JLabel(Messages.getString("CustomMekDialog.labSneakCamo"));
    JLabel labSneakIR = new JLabel(Messages.getString("CustomMekDialog.labSneakIR"));
    JLabel labSneakECM = new JLabel(Messages.getString("CustomMekDialog.labSneakECM"));
    JLabel labSpec = new JLabel(Messages.getString("CustomMekDialog.labInfSpec"));
    private final JComboBox<String> cbArmorKit = new JComboBox<>();
    private final JTextField fldDivisor = new JTextField(3);
    JCheckBox chEncumber = new JCheckBox();
    JCheckBox chSpaceSuit = new JCheckBox();
    JCheckBox chDEST = new JCheckBox();
    JCheckBox chSneakCamo = new JCheckBox();
    JCheckBox chSneakIR = new JCheckBox();
    JCheckBox chSneakECM = new JCheckBox();
    List<JCheckBox> chSpecs = new ArrayList<>(Infantry.NUM_SPECIALIZATIONS);

    List<EquipmentType> armorKits = new ArrayList<>();

    public InfantryArmorPanel(Entity entity, Client client) {
        this.infantry = (Infantry) entity;

        for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
            int spec = 1 << i;
            JCheckBox newSpec = new JCheckBox();
            newSpec.setText(Infantry.getSpecializationName(spec));
            newSpec.setToolTipText(Infantry.getSpecializationTooltip(spec));
            chSpecs.add(newSpec);
        }

        GridBagLayout g = new GridBagLayout();
        setLayout(g);
        add(labArmor, GBC.std());
        add(cbArmorKit, GBC.eol());
        add(labDivisor, GBC.std());
        add(fldDivisor, GBC.eol());
        add(labEncumber, GBC.std());
        add(chEncumber, GBC.eol());
        add(labSpaceSuit, GBC.std());
        add(chSpaceSuit, GBC.eol());
        add(labDEST, GBC.std());
        add(chDEST, GBC.eol());
        add(labSneakCamo, GBC.std());
        add(chSneakCamo, GBC.eol());
        add(labSneakIR, GBC.std());
        add(chSneakIR, GBC.eol());
        add(labSneakECM, GBC.std());
        add(chSneakECM, GBC.eol());
        add(Box.createVerticalStrut(10), GBC.eol());
        add(labSpec, GBC.eol());
        for (JCheckBox spec : chSpecs) {
            add(spec, GBC.eol());
        }

        SimpleTechLevel gameTechLevel = SimpleTechLevel.getGameTechLevel(client.getGame());
        int year = client.getGame().getOptions().intOption("year");
        for (Enumeration<EquipmentType> e = MiscType.getAllTypes(); e.hasMoreElements(); ) {
            final EquipmentType et = e.nextElement();
            if (et.hasFlag(MiscType.F_ARMOR_KIT) &&
                      et.isLegal(year,
                            gameTechLevel,
                            entity.isClan(),
                            entity.isMixedTech(),
                            entity.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
                armorKits.add(et);
            }
        }
        armorKits.sort(Comparator.comparing(EquipmentType::getName));

        cbArmorKit.addItem(Messages.getString("CustomMekDialog.Custom"));
        armorKits.forEach(k -> cbArmorKit.addItem(k.getName()));
        EquipmentType kit = infantry.getArmorKit();
        if (kit == null) {
            cbArmorKit.setSelectedIndex(0);
        } else {
            cbArmorKit.setSelectedIndex(armorKits.indexOf(kit) + 1);
        }
        fldDivisor.setText(Double.toString(infantry.calcDamageDivisor()));
        chEncumber.setSelected(infantry.isArmorEncumbering());
        chSpaceSuit.setSelected(infantry.hasSpaceSuit());
        chDEST.setSelected(infantry.hasDEST());
        chSneakCamo.setSelected(infantry.hasSneakCamo());
        chSneakIR.setSelected(infantry.hasSneakIR());
        chSneakECM.setSelected(infantry.hasSneakECM());
        armorStateChanged();
        cbArmorKit.addActionListener(e -> {
            armorStateChanged();
            updateArmorValues();
        });
        chDEST.addItemListener(e -> armorStateChanged());

        for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
            int spec = 1 << i;
            chSpecs.get(i).setSelected(infantry.hasSpecialization(spec));
        }
    }

    public void armorStateChanged() {
        fldDivisor.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chEncumber.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chSpaceSuit.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chDEST.setEnabled(cbArmorKit.getSelectedIndex() == 0);
        chSneakCamo.setEnabled(cbArmorKit.getSelectedIndex() == 0 && !chDEST.isSelected());
        chSneakIR.setEnabled(cbArmorKit.getSelectedIndex() == 0 && !chDEST.isSelected());
        chSneakECM.setEnabled(cbArmorKit.getSelectedIndex() == 0 && !chDEST.isSelected());
    }

    public void updateArmorValues() {
        if (cbArmorKit.getSelectedIndex() > 0) {
            EquipmentType kit = armorKits.get(cbArmorKit.getSelectedIndex() - 1);
            fldDivisor.setText(Double.toString(((MiscType) kit).getDamageDivisor()));
            chEncumber.setSelected((kit.getSubType() & MiscType.S_ENCUMBERING) != 0);
            chSpaceSuit.setSelected((kit.getSubType() & MiscType.S_SPACE_SUIT) != 0);
            chDEST.setSelected((kit.getSubType() & MiscType.S_DEST) != 0);
            chSneakCamo.setSelected((kit.getSubType() & MiscType.S_SNEAK_CAMO) != 0);
            chSneakIR.setSelected((kit.getSubType() & MiscType.S_SNEAK_IR) != 0);
            chSneakECM.setSelected((kit.getSubType() & MiscType.S_SNEAK_ECM) != 0);
        }
    }

    public void applyChoice() {
        if (cbArmorKit.getSelectedIndex() > 0) {
            infantry.setArmorKit(armorKits.get(cbArmorKit.getSelectedIndex() - 1));
        } else {
            infantry.setArmorKit(null);
            infantry.setArmorDamageDivisor(MathUtility.parseDouble(fldDivisor.getText(), 0.0));
            infantry.setArmorEncumbering(chEncumber.isSelected());
            infantry.setSpaceSuit(chSpaceSuit.isSelected());
            infantry.setDEST(chDEST.isSelected());
            if (!chDEST.isSelected()) {
                infantry.setSneakCamo(chSneakCamo.isSelected());
                infantry.setSneakIR(chSneakIR.isSelected());
                infantry.setSneakECM(chSneakECM.isSelected());
            }
        }
        int spec = 0;
        for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
            if (chSpecs.get(i).isSelected()) {
                spec |= 1 << i;
            }
        }
        infantry.setSpecializations(spec);
    }

    @Override
    public void setEnabled(boolean enabled) {
        cbArmorKit.setEnabled(enabled);
        if (enabled) {
            armorStateChanged();
        } else {
            fldDivisor.setEnabled(false);
            chEncumber.setEnabled(false);
            chSpaceSuit.setEnabled(false);
            chDEST.setEnabled(false);
            chSneakCamo.setEnabled(false);
            chSneakIR.setEnabled(false);
            chSneakECM.setEnabled(false);
        }
        for (JCheckBox spec : chSpecs) {
            spec.setEnabled(enabled);
        }
    }
}
