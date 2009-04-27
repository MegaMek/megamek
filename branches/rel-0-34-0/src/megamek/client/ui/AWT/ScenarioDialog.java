package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.client.ui.AWT.widget.AdvancedLabel;
import megamek.client.ui.AWT.widget.ImageButton;
import megamek.common.Player;

/**
 * Allow a user to set types and colors for scenario players
 */
public class ScenarioDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -991978467413706967L;
    public static final int T_ME = 0;
    public static final int T_HUMAN = 1;
    public static final int T_BOT = 2;

    private Player[] m_players;
    private Label[] m_labels;
    private Choice[] m_typeChoices;
    private ImageButton[] m_camoButtons;
    private Frame m_frame;

    /**
     * The camo selection dialog.
     */
    private CamoChoiceDialog camoDialog;
    private ItemListener prevListener = null;

    public boolean bSet = false;
    public int[] playerTypes;
    public String localName = ""; //$NON-NLS-1$

    public ScenarioDialog(Frame frame, Player[] pa) {
        super(frame, Messages.getString("MegaMek.ScenarioDialog.title"), true); //$NON-NLS-1$
        m_frame = frame;
        camoDialog = new CamoChoiceDialog(frame);
        m_players = pa;
        m_labels = new Label[pa.length];
        m_typeChoices = new Choice[pa.length];
        m_camoButtons = new ImageButton[pa.length];

        playerTypes = new int[pa.length];

        for (int x = 0; x < pa.length; x++) {
            final Player curPlayer = m_players[x];
            curPlayer.setColorIndex(x);

            m_labels[x] = new Label(pa[x].getName(), Label.LEFT);

            m_typeChoices[x] = new Choice();
            m_typeChoices[x].add(Messages
                    .getString("MegaMek.ScenarioDialog.me")); //$NON-NLS-1$
            m_typeChoices[x].add(Messages
                    .getString("MegaMek.ScenarioDialog.otherh")); //$NON-NLS-1$
            m_typeChoices[x].add(Messages
                    .getString("MegaMek.ScenarioDialog.bot")); //$NON-NLS-1$
            final Color defaultBackground = m_typeChoices[x].getBackground();

            m_camoButtons[x] = new ImageButton();
            final ImageButton curButton = m_camoButtons[x];
            curButton.setLabel(Messages.getString("MegaMek.NoCamoBtn")); //$NON-NLS-1$
            curButton.setPreferredSize(84, 72);
            curButton.setBackground(PlayerColors.getColor(x));
            curButton.setActionCommand("camo"); //$NON-NLS-1$

            // When a camo button is pressed, remove any previous
            // listener from the dialog, update the dialog for the
            // button's player, and add a new listener.
            curButton.addActionListener(new ActionListener() {
                private final CamoChoiceDialog dialog = camoDialog;
                private final ImageButton button = curButton;
                private final Color background = defaultBackground;
                private final Player player = curPlayer;

                public void actionPerformed(ActionEvent e) {
                    if (null != prevListener) {
                        dialog.removeItemListener(prevListener);
                    }
                    if (null == player.getCamoFileName()) {
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

        setLayout(new BorderLayout());
        Panel choicePanel = new Panel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 0));
        choicePanel.add(new AdvancedLabel(Messages
                .getString("MegaMek.ScenarioDialog.pNameType"))); //$NON-NLS-1$
        choicePanel.add(new Label(Messages
                .getString("MegaMek.ScenarioDialog.Camo"))); //$NON-NLS-1$
        for (int x = 0; x < pa.length; x++) {
            Panel typePanel = new Panel();
            typePanel.setLayout(new GridLayout(0, 1));
            typePanel.add(m_labels[x]);
            typePanel.add(m_typeChoices[x]);
            choicePanel.add(typePanel);
            choicePanel.add(m_camoButtons[x]);
        }
        add(choicePanel, BorderLayout.CENTER);

        Panel butPanel = new Panel();
        butPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        Button bOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        bOkay.setActionCommand("okay"); //$NON-NLS-1$
        bOkay.addActionListener(this);
        Button bCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        bCancel.setActionCommand("cancel"); //$NON-NLS-1$
        bCancel.addActionListener(this);
        butPanel.add(bOkay);
        butPanel.add(bCancel);
        add(butPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("okay")) { //$NON-NLS-1$
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        new AlertDialog(
                                m_frame,
                                Messages
                                        .getString("MegaMek.ScenarioErrorAllert.title"), Messages.getString("MegaMek.ScenarioErrorAllert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
                        return;
                    }
                    bMeSet = true;
                    localName = m_players[x].getName();
                }
            }
            bSet = true;
            setVisible(false);
        } else if (e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            setVisible(false);
        }
    }
}
