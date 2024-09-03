package megamek.server.totalwarfare;

import megamek.common.Game;
import megamek.common.Report;

import java.util.Vector;

abstract class AbstractTWRuleHandler  {

    final TWGameManager gameManager;

    AbstractTWRuleHandler(TWGameManager gameManager) {
        this.gameManager = gameManager;
    }

    void addReport(Report report) {
        gameManager.addReport(report);
    }

    void addReport(Vector<Report> reports) {
        gameManager.addReport(reports);
    }

    void addReport(Vector<Report> reports, int indent) {
        gameManager.addReport(reports, indent);
    }

    void addNewLines() {
        gameManager.addNewLines();
    }

    Game getGame() {
        return gameManager.getGame();
    }
}
