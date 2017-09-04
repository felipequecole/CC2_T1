package t1;

import java.util.BitSet;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.CommonToken;
import t1.LALexer;
public class T1ErrorListener implements ANTLRErrorListener {

    SaidaParser sp;

    public T1ErrorListener(SaidaParser sp) {
        this.sp = sp;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> rcgnzr, Object o, int i, int i1, String string, RecognitionException re) {
    CommonToken ct=(CommonToken) o;
        if (!sp.isModificado()) {
            if(ct.getType()==LALexer.ERROCHAR){
              sp.println("Linha " + i + ": " + ct.getText()+" - simbolo nao identificado");
            }else if (ct.getType()==LALexer.COMMENTNFECHADO){
              sp.println("Linha " + (i+1) + ": comentario nao fechado");
            }else{
              sp.println("Linha " + i + ": erro sintatico proximo a " + ct.getText());
            }
        }
    }

    @Override
    public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean bln, BitSet bitset, ATNConfigSet atncs) {
        //CommonToken ct=parser.getToken();
        //if (!sp.isModificado()) {
      //      sp.println("Linha " + i + ": " +" - simbolo não indetificado");
    //    }
    }

    @Override
    public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitset, ATNConfigSet atncs) {
    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atncs) {
    }
}
