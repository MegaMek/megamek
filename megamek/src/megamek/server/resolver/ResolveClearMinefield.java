package megamek.server.resolver;

import megamek.common.*;
import megamek.server.Server;

public class ResolveClearMinefield {
    public static void resolveClearMinefield(Server server, Entity ent, Minefield mf) {

        if ((null == mf) || (null == ent) || ent.isDoomed()
            || ent.isDestroyed()) {
            return;
        }

        Coords pos = mf.getCoords();
        int clear = Minefield.CLEAR_NUMBER_INFANTRY;
        int boom = Minefield.CLEAR_NUMBER_INFANTRY_ACCIDENT;

        Report r = new Report(2245);
        // Does the entity has a minesweeper?
        if ((ent instanceof BattleArmor)) {
            BattleArmor ba = (BattleArmor) ent;
            String mcmName = BattleArmor.MANIPULATOR_TYPE_STRINGS
                    [BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE];
            if (ba.getLeftManipulatorName().equals(mcmName)) {
                clear = Minefield.CLEAR_NUMBER_BA_SWEEPER;
                boom = Minefield.CLEAR_NUMBER_BA_SWEEPER_ACCIDENT;
                r = new Report(2246);
            }
        } else if (ent instanceof Infantry) { // Check Minesweeping Engineers
            Infantry inf = (Infantry) ent;
            if (inf.hasSpecialization(Infantry.MINE_ENGINEERS)) {
                clear = Minefield.CLEAR_NUMBER_INF_ENG;
                boom = Minefield.CLEAR_NUMBER_INF_ENG_ACCIDENT;
                r = new Report(2247);
            }
        }
        // mine clearing roll
        r.subject = ent.getId();
        r.add(ent.getShortName(), true);
        r.add(Minefield.getDisplayableName(mf.getType()));
        r.add(pos.getBoardNum(), true);
        server.addReport(r);

        if (server.clearMinefield(mf, ent, clear, boom, server.getvPhaseReport())) {
            server.removeMinefield(mf);
        }
        // some mines might have blown up
        server.resetMines();

        server.addNewLines();
    }
}
