package t1;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by felipequecole on 06/10/17.
 */
public class GeradorDeCodigo extends LABaseListener {
    private String saida;
    private String dimensao = "";
    private String buffer = "";
    private boolean switchCase = false;
    private boolean switchDefault = false;
    private int ci = 0; //contador de identacao
    private PilhaDeTabelas pilhaDeTabelas = new PilhaDeTabelas();
    private TabelaDeSimbolos funcoes = new TabelaDeSimbolos("funcoes");
    public GeradorDeCodigo(){
        saida = "";
    }

    private void print(String texto){
        this.saida += texto;
    }

    private void println(String texto){
        this.saida += texto + "\n";
    }

    private void identar() {
        for(int i = 0; i < ci; i++){
            this.saida += "\t";
        }
    }

    private String getTipo(String id) {
//        System.out.println(id + " tem tipo: "  + pilhaDeTabelas.topo().getTipo(id));
        return pilhaDeTabelas.topo().getTipo(id);
    }

    private String getTagC(String tipo) {
//        System.out.println(tipo);
        switch(tipo) {
            case "literal":
                return "%s";
            case "inteiro":
                return "%d";
            case "real":
                return "%f";
            default:
                return "null";
        }

    }

    public void testaGerador(){
        String entrada = "/home/felipequecole/IdeaProjects/T1_CC2/casosDeTesteT1/";
        entrada+= "3.arquivos_sem_erros/ENTRADA/18.alg";
        ANTLRInputStream input = null;
        try {
            input = new ANTLRInputStream(new FileInputStream(entrada));
        } catch (IOException e) {
            e.printStackTrace();
        }
        LALexer lexer = new LALexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LAParser parser = new LAParser(tokens);
        LAParser.ProgramaContext tree = parser.programa();
        AnalisadorSemantico as = new AnalisadorSemantico();
        GeradorDeCodigo gc = new GeradorDeCodigo();
        ParseTreeWalker.DEFAULT.walk(gc, tree);
    }

    private String getTipoEmC(String tipo_la){
        return tipo_la.replace("inteiro", "int").
                replace("real", "float").
                replace("literal", "char");
    }

    private String converteExpressaoParaC(String expr){
        if (!expr.contains("<=") && !expr.contains(">=")) {
            return expr.replace("=", "==").
                    replace("<>", "!=");
        } else {
            return expr.replace("<>", "!=");
        }
    }

    private String converteOperadoresLogicos(LAParser.ExpressaoContext expr) {
        try {
            String operacao = expr.getText();
            if (expr.termo_logico().outros_fatores_logicos() != null){
                operacao = operacao.replace("e", " && ").
                        replace("ou", " || ");
            }
            if (expr.termo_logico().fator_logico().op_nao()!=null){
                operacao = operacao.replace("nao", "!");
            }
            return operacao;
        } catch (NullPointerException e) {
            return "null";
        }
    }

    @Override
    public void enterPrograma(LAParser.ProgramaContext ctx) {
        println("#include<stdio.h>\n#include<stdlib.h>");
        pilhaDeTabelas.empilhar(new TabelaDeSimbolos("main"));
        System.out.println("Entrou no programa");
    }


    @Override
    public void enterCorpo(LAParser.CorpoContext ctx) {
//        System.out.println("Entrou no corpo");
        println("");
        println("int main() {");
        ci++;
    }


    @Override
    public void exitCorpo(LAParser.CorpoContext ctx) {
//        System.out.println("saiu do corpo");
        identar();
        println("return 0;");
        print("}");
    }

    @Override
    public void enterDecl_local_global(LAParser.Decl_local_globalContext ctx) {
//        super.enterDecl_local_global(ctx);
    }

    @Override
    public void enterDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        pilhaDeTabelas.empilhar(new TabelaDeSimbolos("global"));
        String token = ctx.getStart().getText();
        String nome_procfunc = ctx.IDENT().getText();
        println("");
        switch (token){
            case "procedimento":
                identar();
                print("void " + nome_procfunc + " ");
                break;
            case "funcao":
                identar();
                String tipo_retorno = ctx.tipo_estendido().getText();
                print(getTipoEmC(tipo_retorno) + " " + nome_procfunc + " ");
                funcoes.adicionarSimbolo(nome_procfunc, tipo_retorno);
                System.out.println(ctx.getText());

                break;
        }
    }

    @Override
    public void exitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        println("}");
    }

    @Override
    public void enterParametros_opcional(LAParser.Parametros_opcionalContext ctx) {
        print("(");
    }

    @Override
    public void exitParametros_opcional(LAParser.Parametros_opcionalContext ctx) {
        println(") {");
        this.ci++;
    }

    @Override
    public void enterParametro(LAParser.ParametroContext ctx) {
        String id = ctx.identificador().IDENT().getText();
        String tipo = ctx.tipo_estendido().getText();
        pilhaDeTabelas.topo().adicionarSimbolo(id, tipo);
        if(!tipo.equals("literal")) {
            print(getTipoEmC(tipo) + " " + id);
        } else {
            print(getTipoEmC(tipo) + " " + id + "[100]");
        }
    }

    @Override
    public void enterDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        String token = ctx.getStart().getText();
        switch (token){
            case "constante":
                identar();
                print("const " + getTipoEmC(ctx.tipo_basico().getText()));
                print(" " + ctx.IDENT().getText() + " = ");
                println(ctx.valor_constante().getText() + ";");
                break;
            case "declare":
                identar();
                if (ctx.variavel().tipo().getText().contains("^")){
                    print(getTipoEmC(ctx.variavel().tipo().getText()).replace("^", "") + "* ");
                } else {
                    print(getTipoEmC(ctx.variavel().tipo().getText()) + " ");

                }
                // o resto é feito no listener da variavel
        }
    }


    @Override
    public void enterVariavel(LAParser.VariavelContext ctx) {
        pilhaDeTabelas.topo().adicionarSimbolo(ctx.IDENT().getText(), ctx.tipo().getText());
        for (LAParser.Mais_varContext mais_var : ctx.lista_mais_var) {
            pilhaDeTabelas.topo().adicionarSimbolo(mais_var.IDENT().getText(), ctx.tipo().getText());
        }
    }

    @Override
    public void exitVariavel(LAParser.VariavelContext ctx) {
//        System.out.println("Saiu variavel");
        print(ctx.IDENT().getText());
        if (ctx.tipo().getText().equals("literal")){
            print("[100]");
        }
        if(ctx.dimensao() != null && !dimensao.equals("[") && !dimensao.equals("[]")) {
            print(dimensao);
        }
        for (LAParser.Mais_varContext mais_var : ctx.lista_mais_var) {
            print("," + mais_var.IDENT().getText());
            if(ctx.tipo().getText().equals("literal")){
                print("[100]");
            }
            if (mais_var.dimensao() != null && !dimensao.equals("[")){
                //todo pode causar problemas.
                print(dimensao);
            }
        }
        println(";");
    }

    @Override
    public void enterDimensao(LAParser.DimensaoContext ctx) {
//        System.out.println("entrou dimensao");
        dimensao = "";
        dimensao = "[";
    }

    @Override
    public void exitDimensao(LAParser.DimensaoContext ctx) {
//        System.out.println("saiu dimensao");
        System.out.println(ctx.dimensao() != null);
        if(ctx.exp_aritmetica() != null) {
            dimensao += ctx.exp_aritmetica().getText();
            dimensao += "]";
        }
    }

    @Override
    public void enterExp_aritmetica(LAParser.Exp_aritmeticaContext ctx) {
//        System.out.println("entrou exp aritmetica");
//        print(ctx.getText());
    }

    @Override
    public void enterCmd(LAParser.CmdContext ctx) {
        String token = ctx.getStart().getText();
        System.out.println("token: " + token);
        if (token.equals("leia")) {
            String id = ctx.identificador().IDENT().getText();
            String tipo = pilhaDeTabelas.topo().getTipo(id);
            if (tipo.equals("literal")) {
                identar();
                println("gets(" + id + ");");
            } else {
                identar();
                print("scanf(");
                print(tipo.equals("inteiro") ? "\"%d\"" : "\"%f\"");
                print(", &" + ctx.identificador().IDENT().getText());
                println(");");
            }
        } else if (token.equals("escreva")) {
            boolean mais = false;
            boolean tratado = false;
            identar();
            print("printf(");
            String id = ctx.expressao().getText();
            String tipo_id = id;
            System.out.println(id);
            if(id.contains("[")){
                String[] split = id.split("\\[");
                String[] split_2 = split[1].split("]");
                try {
                    tipo_id = split[0] + split_2[1];
                } catch (IndexOutOfBoundsException e) {
                    tipo_id = split[0];
                }
            } else if (id.contains("(")) {
                String[] split = id.split("\\(");
                tipo_id = split[0];
            }
            System.out.println(tipo_id);
            String tipo = pilhaDeTabelas.topo().getTipo(tipo_id);
            if (tipo.equals("null")) {
                tipo = funcoes.getTipo(tipo_id);
            }
            switch (tipo) {
                case "literal":
                    print("\"" + getTagC(tipo));
                    if (!ctx.mais_expressao().lista_expressao.isEmpty()) {
                        mais = true;
                    }
                    break;
                case "inteiro":
                    print("\"" + getTagC(tipo));
                    if (!ctx.mais_expressao().lista_expressao.isEmpty()) {
                        mais = true;
                    }
                    break;
                case "real":
                    print("\"" + getTagC(tipo));
                    if (!ctx.mais_expressao().lista_expressao.isEmpty()) {
                        mais = true;
                    }
                    break;
                default:
                    if (id.contains("\"")) { //literal + variavel ou só literal
                        mais = true;
                        tratado = true;
                        if (ctx.mais_expressao() != null) {
                            buffer = "";
                        }
                        id = id.replace("\"", ""); //tira as aspas
                        enterMais_expressao(ctx.mais_expressao());
                        for (String mais_id : buffer.split("$$")) {
                            mais_id = mais_id.replace("$$", "");
//                            System.out.println("mais id: " + mais_id);
                            if (!getTagC(getTipo(mais_id)).equals("null")) {
                                id += getTagC(getTipo(mais_id));
                            }
                        }
                        id = "\"" + id + "\"";
                        print(id);
                        buffer = buffer.split("$$").length > 1 ? buffer.replace("$$", ",") :
                                buffer.replace("$$", "");
                        if (!buffer.equals("")) {
                            println("," + buffer + ");");
                        } else {
                            println(");");
                        }
                    } else if (id.contains("+") || id.contains("-")) {
                        mais = true;
                        tratado = true;
                        if (ctx.expressao() != null) {
                            buffer = "";
                            boolean real = false;
                            boolean inteiro = false;
                            enterExpressao(ctx.expressao());
                            String[] split;
                            buffer = buffer.replace("$$", " ");
                            split = buffer.split(" ");
                            for (String part : split) {
                                part = part.replace("$$", "");
                                if (!part.equals("")) {
                                    real = real || getTipo(part).equals("real");
                                    inteiro = inteiro || getTipo(part).equals("inteiro");
                                }
                            }
                            if (real) {
                                println("\"" + getTagC("real") + "\", " + id + ");");
                            } else if (inteiro) {
                                println("\"" + getTagC("inteiro") + "\", " + id + ");");
                            } else {
                                println("\"" + getTagC("literal") + "\", " + id + ");");
                            }
                        }

                    }

            }
            if (mais) {
                // todo esvaziar o buffer, pegar tudo que veio depois
                // todo fazer com que eu só feche o printf aqui
                this.buffer = "";
                enterMais_expressao(ctx.mais_expressao());
                if (!tratado) {
                    System.out.println(buffer);
                    if(buffer.equals("")) {
                        print("\\n\", " + id);
                        println(");");
                    } else {
                        print(buffer.replace("\"", "").replace("$$", "") + "\", " + id);
                        println(");");
                    }
                }

            } else {
                print("\", " + id);
                println(");");
            }
        } else if (token.equals("se")) {
            identar();
            String expressao = converteOperadoresLogicos(ctx.expressao());
            println("if (" + converteExpressaoParaC(expressao) + "){");
            this.ci++;
        } else if (token.equals("caso")) {
            identar();
            println("switch(" + ctx.exp_aritmetica(0).getText() + ") {");
            this.ci++;
            this.ci++;
            if (ctx.senao_opcional().comandos() != null) {
                this.switchDefault = true;
            }
        } else if (token.equals("para")) {
            identar();
            print("for(" + ctx.IDENT().getText() + " = ");
            print(ctx.exp_aritmetica(0).getText() + ";");
            print(ctx.IDENT().getText() + " <= " + ctx.exp_aritmetica(1).getText() + ";");
            println(ctx.IDENT().getText() + "++) {");
            ci++;
        } else if (token.equals("enquanto")) {
            identar();
            println("while (" + ctx.expressao().getText() + ") {");
            ci++;
        } else if (token.equals("faca")){
            identar();
            println("do {");
            ci++;
        } else if (ctx.getText().contains("^")) {
            identar();
            print("*" + ctx.IDENT().getText());
            if(ctx.dimensao() != null && !dimensao.equals("[]") && !dimensao.equals("[")) {
                print(dimensao);
            }
            println(" = " + ctx.expressao().getText() + ";");
        } else if (ctx.getText().contains("<-")) { //atribuicao
            identar();
            print(ctx.IDENT().getText());

//            if (ctx.chamada_atribuicao().expressao() != null) {
//                println(ctx.IDENT().getText() + " = " + ctx.chamada_atribuicao().expressao().getText() + ";");
//            } else {
//                println("calma fera");
//            }
        } else if (token.equals("retorne")) {
            identar();
            println("return " + ctx.expressao().getText() + ";");
        } else { //chamada de funcao ou procedimento
            identar();
            println(ctx.getText() + ";");
        }
    }


    @Override
    public void enterChamada_atribuicao(LAParser.Chamada_atribuicaoContext ctx) {
        if(ctx.dimensao()!= null) {
            print(ctx.dimensao().getText());
        }
        if(ctx.expressao() != null) {
            println(" = " + ctx.expressao().getText() + ";");
        }
    }

    @Override
    public void exitCmd(LAParser.CmdContext ctx) {
        String token = ctx.getStart().getText();
        System.out.println("saida: " + token);
        switch (token) {
            case "se":
                this.ci--;
                identar();
                println("}");
                break;
            case "caso":
                if (ctx.senao_opcional() != null) { // se tem valor default
                    System.out.println("Tem default");
                } else {
                    System.out.println("Nao tem default");
                    this.ci--;
                    identar();
                    println("}");
                }
                break;
            case "para":
                ci--;
                identar();
                println("}");
                break;
            case "enquanto":
                ci--;
                identar();
                println("}");
                break;
            case "faca":
                ci--;
                identar();
                String exp = converteOperadoresLogicos(ctx.expressao());
                exp = converteExpressaoParaC(exp);
                println("}while (" + exp + ");");
        }
        if (switchCase) {
            identar();
            println("break;");
            switchCase = false;
        }

    }





    @Override
    public void enterSenao_opcional(LAParser.Senao_opcionalContext ctx) {
        //todo o switch case usa isso para default tem que tratar
        System.out.println("switch: " + switchCase);
        if (switchDefault){
            ci--;
            identar();
            println("default:");
            ci++;
            switchCase = false;
        } else if (ctx.comandos() != null) {
            ci--;
            identar();
            println("} else {");
            ci++;
        }
    }

    @Override
    public void exitSenao_opcional(LAParser.Senao_opcionalContext ctx) {
        if (switchDefault) {
            ci--;
            ci--;
            identar();
            println("}");
        }
    }

    @Override
    public void enterSelecao(LAParser.SelecaoContext ctx) {
        ci--;
        String selecao = ctx.constantes().getText();
        System.out.println("selecao: " + selecao);
        if (selecao.contains("..")){
            selecao = selecao.replace("..", " ");
            String[] split = selecao.split(" ");
            System.out.println(split[0]);
            int inicio = Integer.parseInt(split[0].trim());
            int fim = Integer.parseInt(split[1].trim());
            for(int i = inicio; i <= fim; i++) {
                identar();
                println("case " + String.valueOf(i) + ":");
            }
            ci++;
        } else {
            identar();
            println("case " + selecao + " :");
            ci++;
        }
        switchCase = true;

    }


    @Override
    public void enterMais_expressao(LAParser.Mais_expressaoContext ctx) {
        buffer="";
        for (LAParser.ExpressaoContext expr : ctx.lista_expressao){
//            System.out.println(expr.getText());
            enterExpressao(expr);
        }
    }


    @Override
    public void enterExpressao(LAParser.ExpressaoContext ctx) {
        String exp = ctx.getText();
        String[] split = null;
        if (ctx.getText().contains("+")) {
            split = exp.split("\\+");
        } else if (ctx.getText().contains("-")){
            split = exp.split("-");
        }
        if (split != null) {
            for (String part : split) {
//                System.out.println("part_exp: " + part);
                buffer+= part.replace("+", "").replace("-", "") + "$$";
            }
        } else {
            buffer += ctx.getText() + "$$";
        }
    }

    @Override
    public void exitExpressao(LAParser.ExpressaoContext ctx) {
//        print(ctx.getText());
//        System.out.println(buffer);
    }

    @Override
    public void exitPrograma(LAParser.ProgramaContext ctx) {
        System.out.println("Saiu do programa");
        System.out.println(saida);
    }

    @Override
    public String toString() {
        return this.saida;
    }
}
