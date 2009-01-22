package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.Messages;

/**
 * A dialog of which exits are connected for terrain.
 */
public class ExitsDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -3126840102187553386L;
    private JCheckBox cheExit0 = new JCheckBox("0"); //$NON-NLS-1$
    private JCheckBox cheExit1 = new JCheckBox("1"); //$NON-NLS-1$
    private JCheckBox cheExit2 = new JCheckBox("2"); //$NON-NLS-1$
    private JCheckBox cheExit3 = new JCheckBox("3"); //$NON-NLS-1$
    private JCheckBox cheExit4 = new JCheckBox("4"); //$NON-NLS-1$
    private JCheckBox cheExit5 = new JCheckBox("5"); //$NON-NLS-1$
    private JLabel labBlank = new JLabel(""); //$NON-NLS-1$
    private JPanel panNorth = new JPanel(new GridBagLayout());
    private JPanel panSouth = new JPanel(new GridBagLayout());
    private JPanel panWest = new JPanel(new BorderLayout());
    private JPanel panEast = new JPanel(new BorderLayout());
    private JPanel panExits = new JPanel(new BorderLayout());
    private JButton butDone = new JButton(Messages
            .getString("BoardEditor.Done")); //$NON-NLS-1$

    ExitsDialog(JFrame frame) {
        super(frame, Messages.getString("BoardEditor.SetExits"), true); //$NON-NLS-1$
        setResizable(false);
        butDone.addActionListener(this);
        panNorth.add(cheExit0);
        panSouth.add(cheExit3);
        panWest.add(cheExit5, BorderLayout.NORTH);
        panWest.add(cheExit4, BorderLayout.SOUTH);
        panEast.add(cheExit1, BorderLayout.NORTH);
        panEast.add(cheExit2, BorderLayout.SOUTH);
        panExits.add(panNorth, BorderLayout.NORTH);
        panExits.add(panWest, BorderLayout.WEST);
        panExits.add(labBlank, BorderLayout.CENTER);
        panExits.add(panEast, BorderLayout.EAST);
        panExits.add(panSouth, BorderLayout.SOUTH);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panExits, BorderLayout.CENTER);
        getContentPane().add(butDone, BorderLayout.SOUTH);
        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void setExits(int exits) {
        cheExit0.setSelected((exits & 1) != 0);
        cheExit1.setSelected((exits & 2) != 0);
        cheExit2.setSelected((exits & 4) != 0);
        cheExit3.setSelected((exits & 8) != 0);
        cheExit4.setSelected((exits & 16) != 0);
        cheExit5.setSelected((exits & 32) != 0);
    }

    public int getExits() {
        int exits = 0;
        exits |= cheExit0.isSelected() ? 1 : 0;
        exits |= cheExit1.isSelected() ? 2 : 0;
        exits |= cheExit2.isSelected() ? 4 : 0;
        exits |= cheExit3.isSelected() ? 8 : 0;
        exits |= cheExit4.isSelected() ? 16 : 0;
        exits |= cheExit5.isSelected() ? 32 : 0;
        return exits;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        setVisible(false);
    }
}
