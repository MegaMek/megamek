package megamek.test.entities;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Vector;

import megamek.common.Entity;
import megamek.common.MULParser;

public class TestMulParser {
    public static void main(String[] args) {
        try {
        FileInputStream slayer = new FileInputStream("d:\\slayer.xml");
        MULParser prs = new MULParser(slayer);
        Vector<Entity> ents = prs.getEntities();
        }
        catch(Exception e) {
            int wellfuckme = 0;
        }
    }
    
}
