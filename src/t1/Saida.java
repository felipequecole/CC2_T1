/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package t1;

/**
 *
 * @author daniel
 */
public class Saida {
    private static StringBuffer texto = new StringBuffer();

    private static boolean modificado = false;
    
    public static void println(String txt) {
        texto.append(txt).append("\n");
        if(!modificado) modificado = true;
    }
    
    public static void clear() {
        texto = new StringBuffer();
    }
    
    public static String getTexto() {
        return texto.toString();
    }

    public boolean isModificado() {
        return modificado;
    }

    @Override
    public String toString() {
        return texto.toString();
    }
}
