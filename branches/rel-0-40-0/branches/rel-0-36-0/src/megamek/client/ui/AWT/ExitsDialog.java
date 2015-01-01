package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionListener;

import megamek.client.ui.Messages;

/**
 * A dialog of which exits are connected for terrain.
 */
public class ExitsDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 3877195794958147600L;
    private Checkbox cheExit0 = new Checkbox("0"); //$NON-NLS-1$
    private Checkbox cheExit1 = new Checkbox("1"); //$NON-NLS-1$
    private Checkbox cheExit2 = new Checkbox("2"); //$NON-NLS-1$
    private Checkbox cheExit3 = new Checkbox("3"); //$NON-NLS-1$
    private Checkbox cheExit4 = new Checkbox("4"); //$NON-NLS-1$
    private Checkbox cheExit5 = new Checkbox("5"); //$NON-NLS-1$

    private Label labBlank = new Label(""); //$NON-NLS-1$

    private Panel panNorth = new Panel(new GridBagLayout());
    private Panel panSouth = new Panel(new GridBagLayout());
    private Panel panWest = new Panel(new BorderLayout());
    private Panel panEast = new Panel(new BorderLayout());

    private Panel panExits = new Panel(new BorderLayout());

    private Button butDone = new Button(Messages.getString("BoardEditor.Done")); //$NON-NLS-1$

    public ExitsDialog(Frame frame) {
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

        setLayout(new BorderLayout());

        add(panExits, BorderLayout.CENTER);
        add(butDone, BorderLayout.SOUTH);

        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void setExits(int exits) {
        cheExit0.setState((exits & 1) != 0);
        cheExit1.setState((exits & 2) != 0);
        cheExit2.setState((exits & 4) != 0);
        cheExit3.setState((exits & 8) != 0);
        cheExit4.setState((exits & 16) != 0);
        cheExit5.setState((exits & 32) != 0);
    }

    public int getExits() {
        int exits = 0;
        exits |= cheExit0.getState() ? 1 : 0;
        exits |= cheExit1.getState() ? 2 : 0;
        exits |= cheExit2.getState() ? 4 : 0;
        exits |= cheExit3.getState() ? 8 : 0;
        exits |= cheExit4.getState() ? 16 : 0;
        exits |= cheExit5.getState() ? 32 : 0;
        return exits;
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        setVisible(false);
    }
}