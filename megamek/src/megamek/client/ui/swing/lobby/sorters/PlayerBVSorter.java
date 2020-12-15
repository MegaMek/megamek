package megamek.client.ui.swing.lobby.sorters;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.IPlayer;

/** A Lobby Mek Table sorter that sorts for 1) Player 2) BV. */
public class PlayerBVSorter implements MekTableSorter {
    
    private ClientGUI clientGui;
    private Sorting direction;

    /** A Lobby Mek Table sorter that sorts for 1) Player 2) BV. */
    public PlayerBVSorter(ClientGUI cg, Sorting dir) {
        clientGui = cg;
        direction = dir;
    }
    
    @Override
    public int compare(final Entity a, final Entity b) {
        final IPlayer p_a = clientGui.getClient().getGame().getPlayer(a.getOwnerId());
        final IPlayer p_b = clientGui.getClient().getGame().getPlayer(b.getOwnerId());
        final IPlayer localPlayer = clientGui.getClient().getLocalPlayer();
        final int t_a = p_a.getTeam();
        final int t_b = p_b.getTeam();
        if (p_a.equals(localPlayer) && !p_b.equals(localPlayer)) {
            return -1;
        } else if (!p_a.equals(localPlayer) && p_b.equals(localPlayer)) {
            return 1;
        } else if ((t_a == localPlayer.getTeam()) && (t_b != localPlayer.getTeam())) {
            return -1;
        } else if ((t_b == localPlayer.getTeam()) && (t_a != localPlayer.getTeam())) {
            return 1;
        } else if (t_a != t_b) {
            return t_a - t_b;
        } else if (!p_a.equals(p_b)) {
            return p_a.getName().compareTo(p_b.getName());
        } else {
            int aBV = a.calculateBattleValue();
            int bBV = b.calculateBattleValue();
            if (bBV > aBV) {
                return smaller(direction);
            } else if (bBV < aBV) {
                return bigger(direction);
            } else {
                return 0;
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Player, BV";
    }
    
    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_BV;
    }
    
    @Override
    public Sorting getSortingDirection() {
        return direction;
    }

}
