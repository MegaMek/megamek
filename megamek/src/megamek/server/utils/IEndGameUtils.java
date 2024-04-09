package megamek.server.utils;

import megamek.common.Game;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.server.GameManager;

public class EndGameUtils {
    public static void changeToPhaseEnd(Game game, GameManager gm) {
        // remove any entities that died in the heat/end phase before
        // checking for victory
        gm.resetEntityPhase(GamePhase.END);
        boolean victory = gm.victory(); // note this may add reports
        // check phase report
        // HACK: hardcoded message ID check
        if ((gm.vPhaseReport.size() > 3) || ((gm.vPhaseReport.size() > 1)
                && (gm.vPhaseReport.elementAt(1).messageId != 1205))) {
            game.addReports(gm.vPhaseReport);
            gm.changePhase(GamePhase.END_REPORT);
        } else {
            // just the heat and end headers, so we'll add
            // the <nothing> label
            gm.addReport(new Report(1205, Report.PUBLIC));
            game.addReports(gm.vPhaseReport);
            gm.sendReport();
            if (victory) {
                gm.changePhase(GamePhase.VICTORY);
            } else {
                gm.changePhase(GamePhase.INITIATIVE);
            }
        }
        // Decrement the ASEWAffected counter
        gm.decrementASEWTurns();
    }
    void changeToPhaseVictory(Game game, GameManager gm);
}
