/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package t1;

/**
 *
 * @author daniel
 */
public class EntradaTabelaDeSimbolos {
    String nome, tipo;
    public EntradaTabelaDeSimbolos(){};
    public EntradaTabelaDeSimbolos(String nome, String tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }
    
    public String getNome() {
        return nome;
    }
    
    public String getTipo() {
        return tipo;
    }
    
    @Override
    public String toString() {
        return nome+"("+tipo+")";
    }
}
