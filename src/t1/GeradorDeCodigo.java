package t1;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by felipequecole on 06/10/17.
 */
public class GeradorDeCodigo extends LABaseListener {
    // String que contém a saída (codigo em C)
    private String saida;
    // Buffer que contém a dimensão (em casos que existe)
    private String dimensao = "";
    // Buffer de uso geral para comunicação entre listeners
    private String buffer = "";

    // flags
    // True quando está ocorrendo declaração de um "Caso"
    private boolean switchCase = false;
    // True quando o "Caso" possui um switch
    //      (utilizado para diferenciar "default" de "else"
    private boolean switchDefault = false;
    // Demarca quando está ocorrendo atribuição
    // (útil para registro)
    private boolean atribuicao = false;
    // demarca quando o que está sendo declarado é um registro
    // (útil para tratar variáveis internas do registro)
    private boolean declarouRegistro = false;
    // demarca quando está ocorrendo um print
    // (útil para tratar caso de registro, e acesso a atributos dele)
    private boolean imprimindo = false;
    // fim das flags

    // contador de identação (apenas para deixar o codigo mais limpo)
    private int ci = 0;
    // utilizada para verificao de tipo em operações de read ou print
    private PilhaDeTabelas pilhaDeTabelas = new PilhaDeTabelas();
    // utilizada para verificacao de tipo de retorno de funcoes
    private TabelaDeSimbolos funcoes = new TabelaDeSimbolos("funcoes");

    // obs: a geração de código não verifica novamente coerção de tipo e etc
    // esses casos são cobertos no analisador semântico.


    // apenas inicializa a string de saída
    public GeradorDeCodigo(){
        saida = "";
    }


    // insere o texto na mesma linha
    private void print(String texto){
        this.saida += texto;
    }


    // insere o texto na mesma linha e quebra a linha
    private void println(String texto){
        this.saida += texto + "\n";
    }


    // insere a quantidade corretas de tabulações (apenas fins estéticos)
    private void identar() {
        for(int i = 0; i < ci; i++){
            this.saida += "\t";
        }
    }


    // retorna a string do tipo (ainda na sintaxe de LA) dado o nome do identificador
    private String getTipo(String id) {
        return pilhaDeTabelas.topo().getTipo(id);
    }


    // retorna o especificador adequado para operações de leitura e escrita em C
    // dado o tipo em LA
    private String getTagC(String tipo) {
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


    // apenas para fins de teste
    public void testaGerador(){
        String entrada = "/home/felipequecole/IdeaProjects/T1_CC2/casosDeTesteT1/";
        entrada+= "3.arquivos_sem_erros/ENTRADA/17.alg";
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
        GeradorDeCodigo gc = new GeradorDeCodigo();
        ParseTreeWalker.DEFAULT.walk(gc, tree);
    }


    // dado o tipo em LA, retorna o tipo correspondente em C
    private String getTipoEmC(String tipo_la){
        return tipo_la.replace("inteiro", "int").
                replace("real", "float").
                replace("literal", "char");
    }


    // faz alteração para operadores compatíveis com C
    private String converteExpressaoParaC(String expr){
        if (!expr.contains("<=") && !expr.contains(">=")) {
            return expr.replace("=", "==").
                    replace("<>", "!=");
        } else {
            return expr.replace("<>", "!=");
        }
    }


    // converte os operadores lógicos de LA para os compatíveis com C
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

    /*
     Como a regra dimensão é chamada sempre (podendo gerar vazio)
        foi necessário fazer essa função para verificar quando realmente
        a dimensão é declarada
    */
    private boolean temDimensao(){
        return !(this.dimensao.equals("[") || this.dimensao.equals("[]"));
    }


    @Override
    public void enterPrograma(LAParser.ProgramaContext ctx) {
        // declara as bibliotecas padrões
        println("#include<stdio.h>\n#include<stdlib.h>");
        // inicializa o novo escopo
        pilhaDeTabelas.empilhar(new TabelaDeSimbolos("main"));
    }


    // regra que inicia a função principal do programa
    @Override
    public void enterCorpo(LAParser.CorpoContext ctx) {
        println("");
        println("int main() {");
        this.ci++;
    }


    // regra que finaliza a função principal do programa
    @Override
    public void exitCorpo(LAParser.CorpoContext ctx) {
        identar();
        println("return 0;");
        print("}");
    }


    // regra que começa a declaração de funções e procedimentos
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
                break;
        }
    }


    // finaliza a declaração de funções e procedimentos
    @Override
    public void exitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        println("}");
    }


    // regra que inicia a declaração de parâmetros de funções e procedimentos
    @Override
    public void enterParametros_opcional(LAParser.Parametros_opcionalContext ctx) {
        print("(");
    }


    // finaliza a declaração de parâmetros de uma função ou procedimento
    @Override
    public void exitParametros_opcional(LAParser.Parametros_opcionalContext ctx) {
        println(") {");
        this.ci++;
    }


    // trata a lista de parametros e converte para o tipo correspondente em C
    @Override
    public void enterParametro(LAParser.ParametroContext ctx) {
        String id = ctx.identificador().IDENT().getText();
        String tipo = ctx.tipo_estendido().getText();
        pilhaDeTabelas.topo().adicionarSimbolo(id, tipo);
        if(!tipo.equals("literal")) {
            print(getTipoEmC(tipo) + " " + id);
        } else {
            // como em C não existe String, apenas vetor de caracteres
            // declara-se um vetor de 100 posições (arbitrário)
            print(getTipoEmC(tipo) + " " + id + "[100]");
        }
    }


    // trata casos especiais de declarações locais (constante e tipo)
    // as demais são tratadas na regra de variáveis.
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
            case "tipo":
                identar();
                print("typedef");
        }
    }


    // regra que finaliza as declarações locais especiais
    // criada especialmente para o caso de um tipo novo (é ali que seu nome é colocado)
    @Override
    public void exitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        String token = ctx.getStart().getText();
        switch (token) {
            case "tipo":
                println(ctx.IDENT().getText() + ";");
        }
    }


    // trata os casos de variável (incluindo registro) em geral
    @Override
    public void enterVariavel(LAParser.VariavelContext ctx) {
        String id = ctx.IDENT().getText();
        String tipo = ctx.tipo().getText();
        if (ctx.tipo().registro() == null) {
            // caso não seja registro, insiro na tabela de simbolos
            // e gero o codigo em C correspondente
            pilhaDeTabelas.topo().adicionarSimbolo(id, tipo);
            identar();
            // tiro o operador de ponteiro pois isso será tratado em outra regra
            print(getTipoEmC(ctx.tipo().getText().replace("^", "")) + " ");
        } else {
            // no caso de registro, eu só insiro na tabela
            // o código em C para isso é gerado em outra regra
            pilhaDeTabelas.topo().adicionarSimbolo(id, "registro");
        }
        for (LAParser.Mais_varContext mais_var : ctx.lista_mais_var) {
            // se tiver mais de um identificador de mesmo tipo
            // varro todos, inserido-os na TS.
            pilhaDeTabelas.topo().adicionarSimbolo(mais_var.IDENT().getText(), ctx.tipo().getText());
        }

    }


    // ao sair da regra variavel, essa função é chamada
    // ela gera o nome das variaveis, já que a ordem em C é tipo identificador
    // e em LA é identificador : tipo
    @Override
    public void exitVariavel(LAParser.VariavelContext ctx) {
        if (!ctx.IDENT().getText().contains(".")) {
            // caso seja um atributo de uma struct é tratado em outra regra
            print(ctx.IDENT().getText());
        }
        if (ctx.tipo().getText().equals("literal")){
            // Não existe "String" em C, apenas vetor de caracteres
            // Optamos por adotar tamanho padrão 100
            print("[100]");
        }
        if(ctx.dimensao() != null && temDimensao()) {
            // caso seja um vetor, é aqui que é gerado o "[tamanho]"
            print(dimensao);
        }
        // esse for trata o caso de uma lista de variaveis
        // exemplo: tipo ident1, ident2...
        for (LAParser.Mais_varContext mais_var : ctx.lista_mais_var) {
            print("," + mais_var.IDENT().getText());
            if(ctx.tipo().getText().equals("literal")){
                print("[100]");
            }
            if (mais_var.dimensao() != null && temDimensao()){
                print(dimensao);
            }
        }
        println(";");
    }


    // regra que começa a inserir informações sobre a dimensao no buffer adequado
    @Override
    public void enterDimensao(LAParser.DimensaoContext ctx) {
        dimensao = "";
        dimensao = "[";
    }


    // termina a criação do buffer de dimensão
    @Override
    public void exitDimensao(LAParser.DimensaoContext ctx) {
        if(ctx.exp_aritmetica() != null) {
            dimensao += ctx.exp_aritmetica().getText();
            dimensao += "]";
        }
    }


    /*
    Regra mais complexa: nela são tratados diversos comandos existentes
    em LA e seu mapeamento para C
     */
    @Override
    public void enterCmd(LAParser.CmdContext ctx) {
        // foi utilizado o token inicial para identificar qual
        // comando específico estava sendo chamado
        String token = ctx.getStart().getText();

        // caso o comando seja de leitura do teclado
        if (token.equals("leia")) {
            // identificamos a variavel a ser lida
            String id = ctx.identificador().IDENT().getText();
            // e o tipo da mesma
            String tipo = pilhaDeTabelas.topo().getTipo(id);
            // caso seja string, utilizamos gets()
            if (tipo.equals("literal")) {
                identar();
                println("gets(" + id + ");");
            }
            // caso contrário, é utilizado o scanf
            else {
                identar();
                print("scanf(");
                // utilizando o especificador correto
                print(tipo.equals("inteiro") ? "\"%d\"" : "\"%f\"");
                // e inserindo o operador &
                print(", &" + ctx.identificador().IDENT().getText());
                println(");");
            }
        } else if (token.equals("escreva")) {
            // algumas flags locais que auxiliaram no tratamento de casos
            boolean mais =  !ctx.mais_expressao().lista_expressao.isEmpty();
            boolean tratado = false;
            // flag global é ativa
            this.imprimindo = true;
            identar();
            print("printf(");
            String id = ctx.expressao().getText();
            // tipo_id é utilizada para pegar o tipo adequado na tabela de simbolos
            String tipo_id = id;
            // ele é importante em casos de vetor, registro ou função
            if(id.contains("[")){ // caso de vetor
                String[] split = id.split("\\[");
                String[] split_2 = split[1].split("]");
                try {
                    tipo_id = split[0] + split_2[1];
                } catch (IndexOutOfBoundsException e) {
                    tipo_id = split[0];
                }
            } else if (id.contains("(")) { // caso de funcao
                String[] split = id.split("\\(");
                tipo_id = split[0];
            } else if (id.contains(".")) { // caso de registro
                String[] split = id.split("\\.");
                tipo_id = split[split.length-1];
            }
            String tipo = pilhaDeTabelas.topo().getTipo(tipo_id);
            if (tipo.equals("null")) { // caso o tipo não seja encontrado na TS
                // ele verifica se é uma função e qual o tipo de retorno
                tipo = funcoes.getTipo(tipo_id);
            }
            // converte para o especificador correto em C
            switch (tipo) {
                case "literal":
                    print("\"" + getTagC(tipo));
                    break;
                case "inteiro":
                    print("\"" + getTagC(tipo));
                    break;
                case "real":
                    print("\"" + getTagC(tipo));
                    break;
                default:
                    if (id.contains("\"")) { //literal constante + variavel ou só literal
                        mais = true;
                        tratado = true;
                        if (ctx.mais_expressao() != null) {
                            buffer = "";
                        }
                        id = id.replace("\"", ""); //tira as aspas
                        // chama função responsável por construir o buffer
                        enterMais_expressao(ctx.mais_expressao());
                        for (String mais_id : buffer.split("$$")) {
                            mais_id = mais_id.replace("$$", "");
                            if (!getTagC(getTipo(mais_id)).equals("null")) {
                                id += getTagC(getTipo(mais_id));
                            }
                        }
                        // reinsere as aspas
                        id = "\"" + id + "\"";
                        print(id);
                        // se tiver mais que uma expressão, separo por virgulas
                        // se tiver apenas uma, removo o separador
                        buffer = buffer.split("$$").length > 1 ? buffer.replace("$$", ",") :
                                buffer.replace("$$", "");
                        if (!buffer.equals("")) {
                            println("," + buffer + ");");
                        } else {
                            println(");");
                        }
                    // se for uma expressao aritmetica
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
                this.buffer = "";
                enterMais_expressao(ctx.mais_expressao());
                if (!tratado) {
                    if(buffer.equals("")) {
                        print("\\n\", " + id);
                        println(");");
                    } else {
                        // outros casos, divido em mais de um printf
                        String[] split = buffer.replace("$$", "-").split("-");
                        print("\", " + id);
                        println(");");
                        for (String part : split){
                            identar();
                            if (part.contains("\"")) {
                                println("printf("+part+");");
                            } else {
                                if (part.contains(".")){
                                    String[] split_2 = part.split("\\.");
                                    id = split_2[split_2.length-1];
                                } else {
                                    id = part;
                                }
                                println("printf(\"" + getTagC(getTipo(id)) + "\"," + part + ");");
                            }
                        }
                    }
                }

            } else { // caso não tenha nenhuma outra expressão
                print("\", " + id);
                println(");");
            }
        // comando if
        } else if (token.equals("se")) {
            identar();
            // necessario converter os operadores
            String expressao = converteOperadoresLogicos(ctx.expressao());
            println("if (" + converteExpressaoParaC(expressao) + "){");
            this.ci++;
        // comando "caso"
        } else if (token.equals("caso")) {
            identar();
            println("switch(" + ctx.exp_aritmetica(0).getText() + ") {");
            this.ci++;
            this.ci++;
            if (ctx.senao_opcional().comandos() != null) {
                // necessario para criar um "default" ao inves de "else"
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
        } else if (ctx.getText().contains("<-")) { // comando de atribuicao
            identar();
            // set flag de atribuicao
            this.atribuicao = true;
            if (ctx.ponteiros_opcionais() != null && ctx.ponteiros_opcionais().ponteiros_opcionais() != null) {
                // caso seja ponteiro
                print("*");
            }
            if (ctx.chamada_atribuicao().expressao().getText().contains("\"")) {
                // caso seja um literal, é necessário utilizar strcpy()
                print("strcpy(" + ctx.IDENT().getText());
            } else {
                print(ctx.IDENT().getText());
            }

        } else if (token.equals("retorne")) {
            identar();
            println("return " + ctx.expressao().getText() + ";");
        } else { // chamada de funcao ou procedimento
            identar();
            println(ctx.getText() + ";");
        }
    }


    // faz a geração necessária após o tratamento interno dos comandos da regra cmd
    @Override
    public void exitCmd(LAParser.CmdContext ctx) {
        String token = ctx.getStart().getText();
        switch (token) {
            case "se":
                this.ci--;
                identar();
                println("}");
                break;
            case "caso":
                if (ctx.senao_opcional() == null) {
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
                break;
            case "escreva":
                this.imprimindo = false;
                break;
        }
        if (switchCase) {
            identar();
            println("break;");
            switchCase = false;
        }

    }


    @Override
    public void enterChamada_atribuicao(LAParser.Chamada_atribuicaoContext ctx) {
        // set flag de atribuicao
        this.atribuicao = true;
    }


    @Override
    public void exitChamada_atribuicao(LAParser.Chamada_atribuicaoContext ctx) {
        if(ctx.dimensao() != null) {
            print(ctx.dimensao().getText());
        }
        if(ctx.expressao() != null) {
            if (ctx.expressao().getText().contains("\"")){ // caso seja um literal
                println("," + ctx.expressao().getText() + ");");
            } else {
                println(" = " + ctx.expressao().getText() + ";");
            }

        }
        this.atribuicao = false;
    }


    // cobre o caso de chamada de atributos de um registro
    @Override
    public void enterOutros_ident(LAParser.Outros_identContext ctx) {
        // caso seja impressão, não é necessário fazer isso
        // pois é tratado de outra maneira
        if (ctx.getText().contains(".") && !this.imprimindo){
            print(ctx.getText());
        }
    }


    @Override
    public void enterRegistro(LAParser.RegistroContext ctx) {
        // set flag de declaração de registro
        this.declarouRegistro = true;
        identar();
        println("struct {");
        this.ci++;
    }


    @Override
    public void exitRegistro(LAParser.RegistroContext ctx) {
        this.ci--;
        identar();
        print("}");
        this.declarouRegistro = false;
    }


    @Override
    public void enterPonteiros_opcionais(LAParser.Ponteiros_opcionaisContext ctx) {
        if (ctx.ponteiros_opcionais() != null && !atribuicao) {
            print("*");
        }
    }


    // gera else em caso de if, e default em caso de switch case
    @Override
    public void enterSenao_opcional(LAParser.Senao_opcionalContext ctx) {
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


    // em caso de switch, necessario corrigir o recuo da identação
    @Override
    public void exitSenao_opcional(LAParser.Senao_opcionalContext ctx) {
        if (switchDefault) {
            ci--;
            ci--;
            identar();
            println("}");
        }
    }


    // regra para os casos do switch case
    @Override
    public void enterSelecao(LAParser.SelecaoContext ctx) {
        ci--;
        String selecao = ctx.constantes().getText();
        if (selecao.contains("..")){
            // trata o caso de intervalos (não existe em C)
            // gerando um case para cada numero no intervalo
            selecao = selecao.replace("..", " ");
            String[] split = selecao.split(" ");
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


    // varre preenchendo o buffer para mais expressoes (utilizado em "escreva", por exemplo)
    @Override
    public void enterMais_expressao(LAParser.Mais_expressaoContext ctx) {
        buffer="";
        for (LAParser.ExpressaoContext expr : ctx.lista_expressao){
            enterExpressao(expr);
        }
    }


    // varre cada expressa do "mais expressao" preenchendo o buffer
    // o separador "$$" foi escolhido arbitrariamente
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
                buffer+= part.replace("+", "").replace("-", "") + "$$";
            }
        } else {
            buffer += ctx.getText() + "$$";
        }
    }


    @Override
    public String toString() {
        return this.saida;
    }
}