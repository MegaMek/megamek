package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Player;

/**
 * Allow a user to set types and colors for scenario players
 */
public class ScenarioDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -5682593522064612790L;
    private static final int T_ME = 0;
    public static final int T_BOT = 2;
    private Player[] m_players;
    private JLabel[] m_labels;
    private JComboBox[] m_typeChoices;
    private JButton[] m_camoButtons;
    private JFrame m_frame;
    /**
     * The camo selection dialog.
     */
    private CamoChoiceDialog camoDialog;
    private ItemListener prevListener;
    public boolean bSet;
    public int[] playerTypes;
    public String localName = ""; //$NON-NLS-1$

    public ScenarioDialog(JFrame frame, Player[] pa) {
        super(frame, Messages.getString("MegaMek.ScenarioDialog.title"), true); //$NON-NLS-1$
        m_frame = frame;
        camoDialog = new CamoChoiceDialog(frame);
        m_players = pa;
        m_labels = new JLabel[pa.length];
        m_typeChoices = new JComboBox[pa.length];
        m_camoButtons = new JButton[pa.length];
        playerTypes = new int[pa.length];
        for (int x = 0; x < pa.length; x++) {
            final Player curPlayer = m_players[x];
            curPlayer.setColorIndex(x);
            m_labels[x] = new JLabel(pa[x].getName(), SwingConstants.LEFT);
            m_typeChoices[x] = new JComboBox();
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.me")); //$NON-NLS-1$
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.otherh")); //$NON-NLS-1$
            m_typeChoices[x].addItem(Messages
                    .getString("MegaMek.ScenarioDialog.bot")); //$NON-NLS-1$
            final Color defaultBackground = m_typeChoices[x].getBackground();
            m_camoButtons[x] = new JButton();
            final JButton curButton = m_camoButtons[x];
            curButton.setText(Messages.getString("MegaMek.NoCamoBtn")); //$NON-NLS-1$
            curButton.setPreferredSize(new Dimension(84, 72));
            curButton.setBackground(PlayerColors.getColor(x));
            curButton.setActionCommand("camo"); //$NON-NLS-1$

            // When a camo button is pressed, remove any previous
            // listener from the dialog, update the dialog for the
            // button's player, and add a new listener.
            curButton.addActionListener(new ActionListener() {
                private final CamoChoiceDialog dialog = camoDialog;
                private final JButton button = curButton;
                private final Color background = defaultBackground;
                private final Player player = curPlayer;

                public void actionPerformed(ActionEvent e) {
                    if (prevListener != null) {
                        dialog.removeItemListener(prevListener);
                    }
                    if (player.getCamoFileName() == null) {
                        dialog.setCategory(Player.NO_CAMO);
                        dialog.setItemName(Player.colorNames[player
                                .getColorIndex()]);
                    } else {
                        dialog.setCategory(player.getCamoCategory());
                        dialog.setItemName(player.getCamoFileName());
                    }
                    prevListener = new CamoChoiceListener(dialog, button,
                            background, player);
                    dialog.addItemListener(prevListener);
                    dialog.setVisible(true);
                }
            });
        }
        getContentPane().setLayout(new BorderLayout());
        JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 0));
        choicePanel.add(new JLabel(Messages
                .getString("MegaMek.ScenarioDialog.pNameType"))); //$NON-NLS-1$
        choicePanel.add(new JLabel(Messages
                .getString("MegaMek.ScenarioDialog.Camo"))); //$NON-NLS-1$
        for (int x = 0; x < pa.length; x++) {
            JPanel typePanel = new JPanel();
            typePanel.setLayout(new GridLayout(0, 1));
            typePanel.add(m_labels[x]);
            typePanel.add(m_typeChoices[x]);
            choicePanel.add(typePanel);
            choicePanel.add(m_camoButtons[x]);
        }
        getContentPane().add(choicePanel, BorderLayout.CENTER);
        JPanel butPanel = new JPanel();
        butPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton bOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        bOkay.setActionCommand("okay"); //$NON-NLS-1$
        bOkay.addActionListener(this);
        JButton bCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        bCancel.setActionCommand("cancel"); //$NON-NLS-1$
        bCancel.addActionListener(this);
        butPanel.add(bOkay);
        butPanel.add(bCancel);
        getContentPane().add(butPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if ("okay".equals(e.getActionCommand())) { //$NON-NLS-1$
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        JOptionPane
                                .showMessageDialog(
                                        m_frame,
                                        Messages
                                                .getString("MegaMek.ScenarioErrorAllert.message"),
                                        Messages
                                                .getString("MegaMek.ScenarioErrorAllert.title"),
                                        JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    bMeSet = true;
                    localName = m_players[x].getName();
                }
            }
            bSet = true;
            setVisible(false);
        } else if ("cancel".equals(e.getActionCommand())) { //$NON-NLS-1$
            setVisible(false);
        }
    }
}