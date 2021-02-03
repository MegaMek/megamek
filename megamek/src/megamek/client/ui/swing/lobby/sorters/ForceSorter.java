package megamek.client.ui.swing.lobby.sorters;

import java.util.ArrayList;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.lobby.MekTableModel;
import megamek.common.Entity;
import megamek.common.Force;
import megamek.common.IPlayer;

public class ForceSorter implements MekTableSorter {

private ClientGUI clientGui;
    
    /** A Lobby Mek Table sorter that sorts mainly by association to forces. */
    public ForceSorter(ClientGUI cg) {
        clientGui = cg;
    }
    
    @Override
    public String getDisplayName() {
        return "Forces";
    }
    
    @Override
    public int getColumnIndex() {
        return MekTableModel.COL_UNIT;
    }

    @Override
    public int compare(final Entity a, final Entity b) {
        final IPlayer p_a = clientGui.getClient().getGame().getPlayer(a.getOwnerId());
        final IPlayer p_b = clientGui.getClient().getGame().getPlayer(b.getOwnerId());
        final IPlayer localPlayer = clientGui.getClient().getLocalPlayer();
        final int localteam = localPlayer.getTeam();
        final int team_a = p_a.getTeam();
        final int team_b = p_b.getTeam();
        String force_a = a.getForce();
        String force_b = b.getForce();
        force_a = (force_a == null ? "" : force_a);
        force_b = (force_b == null ? "" : force_b);
//        boolean a_alone = !Force.hasForce(a);
//        boolean b_alone = !Force.hasForce(b);
//        int a_id = a.getId();
//        int b_id = b.getId();
//        int a_lowlevel = Force.lowestForceLevel(a);
//        int b_lowlevel = Force.lowestForceLevel(b);
//        int a_level = Force.forceLevel(a);
//        int b_level = Force.forceLevel(b);
//        
//        ArrayList<String> a_full = Force.getFullForceList(a);
//        ArrayList<String> b_full = Force.getFullForceList(b);
        
        if ((team_a == localteam) && (team_b != localteam)) {
            return -1;
        } else if ((team_b == localteam) && (team_a != localteam)) {
            return 1;
        } else if (team_a != team_b) {
            return team_a - team_b;
//        } else if (!a_alone && b_alone) {
//            return -1;
//        } else if (a_alone && !b_alone) {
//            return 1;
//        } else if (a_alone && b_alone) {
//            return a_id - b_id;
//        } else if (a_level > b_level) { // From here on both entities are in forces on the same team
//            return -1;
//        } else if (b_level > a_level) {
//            return 1;
////        } else if (!force_a.equals(force_b)) {
////            return force_a.compareTo(force_b);
//        } else {
//            // Same top level height, find the level where they differ, if any
//            int firstDiff = -1;
//            for (int i = 0; i < a_full.size(); i++) {
//                if (!a_full.get(i).equals(b_full.get(i))) {
//                    firstDiff = i;
//                    break;
//                }
//            }
//            if (firstDiff != -1) {
//                if (a_full.get(firstDiff).equals("_") && !b_full.get(firstDiff).equals("_")) {
//                    return -1;
//                } else if (!a_full.get(firstDiff).equals("_") && b_full.get(firstDiff).equals("_")) {
//                    return 1;
//                } else if (!a_full.get(firstDiff).equals(b_full.get(firstDiff))) {
//                    return force_a.compareTo(force_b);
//                }
//            }
//            return a_id - b_id;
//                
////            }
////            if (!tr_a.equals(tr_b)) {
//            // The units are on the same network; sort by hierarchy (for standard C3) and ID
//                // The Company Commander on top
////                if (a.isC3CompanyCommander()) {
////                    return -1;
////                } else if (b.isC3CompanyCommander()) {
////                    return 1;
////                }
////                // All units below their masters
////                if (b.C3MasterIs(a)) {
////                    return -1;
////                } else if (a.C3MasterIs(b)) {
////                    return 1;
////                }
////                // Two slaves of the same master sort by ID
////                if (a.hasC3S() && b.hasC3S() && a.getC3MasterId() == b.getC3MasterId()) {
////                    return a_id - b_id;    
////                }
////                // Slaves of different masters sort by their master's IDs
////                if (a.hasC3S()) {
////                    a_id = a.getC3MasterId();
////                }
////                if (b.hasC3S()) {
////                    b_id = b.getC3MasterId();
////                }
        }
        return -1;
    }


}
