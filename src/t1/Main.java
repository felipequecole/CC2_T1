package t1;

import org.antlr.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Created by felipequecole on 06/10/17.
 */
public class Main {
    public static void main(String[] args) {
        GeradorDeCodigo gc = new GeradorDeCodigo();
        gc.testaGerador();
    }
}
