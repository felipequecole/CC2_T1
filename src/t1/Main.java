package t1;

import java.io.IOException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileInputStream;
import java.io.PrintWriter;

public class Main {

    public static void main(String args[]) throws IOException, RecognitionException {
        Saida out = new Saida();
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(args[0]));
        LALexer lexer = new LALexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LAParser parser = new LAParser(tokens);
        //remove todos os listeners de erro
        parser.removeErrorListeners();
        // adiciona o ErroListener Customizado
        parser.addErrorListener(new T1ErrorListener(out));
        //executa análise sintática
        LAParser.ProgramaContext arvore=parser.programa();

        if (!out.isModificado()) {//se foi bem sucedida
            //cria objeta analisador semantico
            AnalisadorSemantico as=new AnalisadorSemantico();
            //torna o tokenstream acessivel ao Analisador Semantico
            as.setTokenStream(tokens);
            //executa a análise sintatica
            as.visitPrograma(arvore);

            if (!out.isModificado()) {//se a análise foi bem sucedida
                //Executa gerador de código
                GeradorDeCodigo gc = new GeradorDeCodigo();
                ParseTreeWalker.DEFAULT.walk(gc, arvore);
                try{
                    PrintWriter writer = new PrintWriter(args[1], "UTF-8");
                    writer.print(gc.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                out.println("Fim da compilacao");
                try{
                    PrintWriter writer = new PrintWriter(args[1], "UTF-8");
                    writer.print(out);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.print(out);
            }
        } else {
            out.println("Fim da compilacao");
            System.out.print(out);
            try{
              PrintWriter writer = new PrintWriter(args[1], "UTF-8");
              writer.print(out);
              writer.close();
            } catch (IOException e) {
              e.printStackTrace();
        }
           // do somehing
        }
    }
}
