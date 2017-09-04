package t1;

import java.io.IOException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import java.io.FileInputStream;

public class TestaAnalisadorSintatico {

    public static void main(String args[]) throws IOException, RecognitionException {
        SaidaParser out = new SaidaParser();
// Descomente as linhas abaixo para testar o analisador gerado.
// Obs: a linha abaixo está configurada para usar como entrada o arquivo LA1.txt
// Modifique-a para testar os demais exemplos
//System.out.println("Working Directory = " +
              //System.getProperty("user.dir
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(args[0]));
        LALexer lexer = new LALexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LAParser parser = new LAParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new T1ErrorListener(out));
        parser.programa();
        if (!out.isModificado()) {
            out.println("Fim da analise. Sem erros sintaticos.");
            out.println("Tabela de simbolos:");

          //  TabelaDeSimbolos.imprimirTabela(out);
            System.out.print(out);
        } else {
            out.println("Fim da compilacao");
            System.out.print(out);
        }
    }
}
