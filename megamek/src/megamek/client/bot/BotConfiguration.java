package megamek.client.bot;

import java.io.FileInputStream;
import java.util.Properties;

public class BotConfiguration {
    
    static Properties BotProperties = new Properties();

    static {
        try {
            BotProperties.load(new FileInputStream("bot.properties")); //$NON-NLS-1$
        } catch (Exception e) {
            System.out.println("Bot properties could not be loaded, will use defaults"); //$NON-NLS-1$
        }
    }
    
    public int getIgnoreLevel() {
        int difficulty = 3;
        try {
            difficulty = Integer.parseInt(BotProperties.getProperty("difficulty", "3")); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (Exception e) {
            //do nothing
        }

        switch (difficulty) {
            case 1 :
                return 8;
            case 2 :
                return 9;
            default:
                return 10;
        }
    }
}
