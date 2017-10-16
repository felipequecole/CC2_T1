package t1;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CommonTokenStream;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AnalisadorSemantico extends LABaseVisitor{
  PilhaDeTabelas escopos = new PilhaDeTabelas();
  PilhaDeTabelas escoposTipo = new PilhaDeTabelas();
  ArrayList<ParametrosFuncProc> listaPFC= new ArrayList<ParametrosFuncProc>();
  TabelaDeSimbolos tabelaRegistro = new TabelaDeSimbolos("registro");


  CommonTokenStream cts;
  public void setTokenStream(CommonTokenStream c){
    cts=c;
  }

  public Object visitExpressao(LAParser.ExpressaoContext ctx){
  //  System.out.println((String) tipo_expressao(ctx));
    TabelaDeSimbolos atual = escopos.topo();
    TabelaDeSimbolos atualTipo = escoposTipo.topo();

    for(int i=ctx.getSourceInterval().a;i<=ctx.getSourceInterval().b;i++) {
      Token token=cts.get(i);
      if(token.getType()==LAParser.IDENT){
        String simbolo = token.getText();
        String stop = ctx.getStop().getText();

        if (ctx.escreva){
          if (!simbolo.equals(stop) && ctx.getSourceInterval().b - ctx.getSourceInterval().a == 2 /* && alguma coisa */) {
            //System.out.println("simbolo: " + simbolo + " getText: " + stop);
            simbolo += "." + stop;
          }
        }

        if((!escopos.existeSimbolo(token.getText()))&&(!escoposTipo.existeSimbolo(token.getText()))){
          Saida.println("Linha "+ token.getLine()+": identificador " +simbolo+ " nao declarado");
        }
      }
    }
    //visitChildren(ctx);
    return null;
  }


  public Object tipo_expressao(LAParser.ExpressaoContext ctx){
    if(ctx==null)
      return "";
    if(ctx.outros_termos_logicos().getChildCount()!=0)
      return "logico";
    if(ctx.termo_logico().outros_fatores_logicos().getChildCount()!=0)
      return "logico";
    if(ctx.termo_logico()!=null && ctx.termo_logico().fator_logico()!=null)
      return visitTipoParcela_logica(ctx.termo_logico().fator_logico().parcela_logica());
    return null;
  }

  @Override
  public Object visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
    if(ctx != null){
      if((ctx.chamada_partes() != null)&&(ctx.chamada_partes().expressao()!= null)){
        //Criando lista de parametros da função
        ParametrosFuncProc aux = new ParametrosFuncProc(ctx.IdentChamada.getText());
        System.out.println("------"+ctx.IdentChamada.getText()+"------");
        aux.setLista((ArrayList<String>) visitChamada_partes(ctx.chamada_partes()));

        //Verificando se a lista é correspondente a lista global
        for(int i = 0; i<=listaPFC.size()-1;i++){

          String LG = (String)listaPFC.get(i).getIdentificador();
          String LL = (String) aux.getIdentificador();

          if(LG.equals(LL)){
            if(!listaPFC.get(i).getLista().equals(aux.getLista())){
              Saida.println("Linha "+ctx.getStart().getLine() + ": incompatibilidade de " +
                      "parametros na chamada de " + ctx.IdentChamada.getText());
            }
          }
        }

      }if(ctx.outros_ident() != null){
        visitOutros_ident(ctx.outros_ident());
      }if(ctx.dimensao() != null){
        visitDimensao(ctx.dimensao());
      }if(ctx.expressao() != null){
        visitExpressao(ctx.expressao());
      }
    }
    return null;//super.visitParcela_unario(ctx);
  }

  @Override
  public Object visitChamada_partes(LAParser.Chamada_partesContext ctx) {
    ArrayList<String> aux = new ArrayList<String>();

    if(ctx != null){
      if(ctx.expressao() != null){
        aux.add((String)tipo_expressao(ctx.expressao()));
      }if(ctx.mais_expressao() != null){
        for(LAParser.ExpressaoContext ct: ctx.mais_expressao().lista_expressao){
          aux.add((String)tipo_expressao(ct));
        }
      }
    }

    return aux;//super.visitChamada_partes(ctx);
  }

  public Object visitTipoParcela_logica(LAParser.Parcela_logicaContext ctx){
    if(ctx!=null){
      if(ctx.exp_relacional()!=null){
        if(ctx.exp_relacional().getChildCount()==0)
          return "logico";
        return visitTipoExp_relacional(ctx.exp_relacional());
      }
      return "logico";
    }
    return null;
  }

  public Object visitTipoExp_relacional(LAParser.Exp_relacionalContext ctx){
    if(ctx.op_opcional().getChildCount()!=0){
      return "logico";
    }
    /*** falta fazer expressão aritmerica; diferenciar inteiro de real***/
    String tipoExp="";
    for(int i=ctx.getSourceInterval().a;i<=ctx.getSourceInterval().b;i++) {
      Token token=cts.get(i);
      int tipoToken=token.getType();

      if(tipoToken==LAParser.IDENT){
        String aux=escopos.getTipoSimbolo(token.getText());
        if(aux==null)
          aux=escoposTipo.getTipoSimbolo(token.getText());
        if(aux==null)
          return "";
        if(aux.equals("void"))
          return "void";
        if(aux.equals("literal"))
          tipoToken=LAParser.CADEIA;
        if(aux.equals("inteiro"))
          tipoToken=LAParser.NUM_INT;
        if(aux.equals("real"))
          return "real";
      }
      if(tipoToken==LAParser.CADEIA){
        if(tipoExp.equals(""))
          tipoExp="literal";
      }else if(tipoToken==LAParser.NUM_INT){
        if(tipoExp.equals("") || tipoExp.equals("literal"))
          tipoExp="inteiro";
      }else if(tipoToken==LAParser.NUM_REAL){
        return "real";
      }
    }
    return tipoExp;
  }

  @Override
  public Object visitPrograma(LAParser.ProgramaContext ctx) {
    escopos.empilhar(new TabelaDeSimbolos("global"));
    escoposTipo.empilhar(new TabelaDeSimbolos("Tipo_global"));
    if(ctx.declaracoes() != null){
      visitDeclaracoes(ctx.declaracoes());
    }
    if(ctx.corpo() !=null){
      visitCorpo(ctx.corpo());
    }
    return null;
  }
  @Override
  public Object visitCorpo(LAParser.CorpoContext ctx) {
    escopos.empilhar(new TabelaDeSimbolos("local"));
    //Visitando comando

          visitDeclaracoes_locais(ctx.declaracoes_locais());
          visitComandos(ctx.comandos());
    return null;
  }

    @Override
    public Object visitDeclaracoes_locais(LAParser.Declaracoes_locaisContext ctx) {
        for (LAParser.Declaracao_localContext ct : ctx.declocais){
          visitDeclaracao_local(ct);
        }
        return null;
    }

    @Override
  public Object visitDeclaracoes(LAParser.DeclaracoesContext ctx) {

    for(LAParser.Decl_local_globalContext ct : ctx.lista_DeclLocalGlobal){
      visitDecl_local_global(ct);
    }
    return null;
  }

  @Override
  public Object visitDecl_local_global(LAParser.Decl_local_globalContext ctx) {

    if(ctx!=null){
      if(ctx.declaracao_global() != null){
        visitDeclaracao_global(ctx.declaracao_global());
      }if(ctx.declaracao_local()!=null){
        return visitDeclaracao_local(ctx.declaracao_local());
      }
    }
    return null;
  }

  @Override
  public Object visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
    TabelaDeSimbolos atual = escopos.topo();
    TabelaDeSimbolos atualTipo = escoposTipo.topo();
    if(ctx != null){

      String simbolo = ctx.IDENT().getText();
      //verifica que é uma função
      if(ctx.tipo_estendido() != null){
        String tipo = ctx.tipo_estendido().getText();
        if((!escopos.existeSimbolo(simbolo))&&(!escoposTipo.existeSimbolo(simbolo))){
          atualTipo.adicionarSimbolo(simbolo,tipo);
        }else{
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
        }

        //escopo de um funcao
        escopos.empilhar(new TabelaDeSimbolos("Funcao"));

        //adicionando os parametros
        ParametrosFuncProc ListParametros = new ParametrosFuncProc(ctx.IDENT().getText());
        listaPFC.add(ListParametros);
        ArrayList<String> aux = new ArrayList<String>();

        if(ctx.parametros_opcional()!=null){
          aux = (ArrayList<String>) visitParametros_opcional(ctx.parametros_opcional());
        }

        int i = listaPFC.indexOf(ListParametros);
        System.out.println(i);
        listaPFC.get(i).setLista(aux);

        for(int j = 0;j<listaPFC.get(i).getLista().size();j++){
          System.out.println(listaPFC.get(i).getLista().get(j));
        }
        //visitando os comandos
        if(ctx.comandos() != null){
          visitComandos(ctx.comandos());
        }
        //saindo do escopo funcao
        escopos.desempilhar();

        // é um procedimento
      }else {
        if ((!escopos.existeSimbolo(simbolo)) && (!escoposTipo.existeSimbolo(simbolo))) {
          atualTipo.adicionarSimbolo(simbolo, "void");
        } else {
          Saida.println("Linha " + ctx.getStart().getLine() + ": identificador " + simbolo + " ja declarado anteriormente");
        }

        //escopo de um procedimento
        escopos.empilhar(new TabelaDeSimbolos("Procedimento"));
        //adicionando os parametros


        if(ctx.parametros_opcional()!=null){
         visitParametros_opcional(ctx.parametros_opcional());
        }

        //visitando os comandos
        if(ctx.comandos() != null){
          visitComandos(ctx.comandos());
        }
        //saindo do escopo funcao
        escopos.desempilhar();
      }
    }
    return null;
  }

  @Override
  public Object visitParametros_opcional(LAParser.Parametros_opcionalContext ctx) {
    TabelaDeSimbolos atual = escopos.topo();
    ArrayList<String> listaDeParametros = new ArrayList<String>();
    if(ctx != null){
      for(LAParser.ParametroContext ct: ctx.lista_parametro){
        listaDeParametros.addAll((ArrayList<String>) visitParametro(ct));
      }
    }
    return listaDeParametros;
  }

  @Override
  public Object visitParametro(LAParser.ParametroContext ctx) {
    TabelaDeSimbolos atual = escopos.topo();
    ArrayList<String> listaDeParametros = new ArrayList<String>();

    if(ctx != null){
      if(ctx.identificador()!=null){
        //adicionando o primeiro parametro na tabela
        String simbolo = ctx.identificador().IDENT().getText();
        String tipo =(String)visitTipo_estendido(ctx.tipo_estendido());
        //Adiciona na lista
        listaDeParametros.add(tipo);
        if(!atual.existeSimbolo(simbolo)){
          atual.adicionarSimbolo(simbolo,tipo);

        }else{
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
        }
        //adicionando as demais
        if(ctx.mais_ident() != null){
          for(LAParser.IdentificadorContext ct: ctx.mais_ident().lista_ident){
            simbolo = ct.IDENT().getText();
            //Adiciona na lista
            listaDeParametros.add(tipo);
            if(!atual.existeSimbolo(simbolo)){
              atual.adicionarSimbolo(simbolo,tipo);

            }else{
              Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
            }
          }
        }
      }
      //Visitando mais_parametros
      for(LAParser.ParametroContext ct: ctx.mais_parametros().lista_MaisParametros){
        //Concatenando todas as listas de parametros
        listaDeParametros.addAll(((ArrayList<String>)visitParametro(ct)));
      }
    }

    return listaDeParametros;
  }

  /*  @Override
  public Object visitComandos(LAParser.ComandosContext ctx) {
      if(ctx.cmd() !=null){
        visitCmd(ctx.cmd());
      }

    return null;
  }*/



  @Override
  public Object visitCmd(LAParser.CmdContext ctx) {

    if(ctx==null)
      return null;
      //Verificando se a variavel existe na atribuição, para e ^
      if(ctx.IDENT() != null){
        String simbolo = ctx.IDENT().getText(); //(String) visitIdentificador(ctx.identificador())
        if((!escopos.existeSimbolo(simbolo))&&(!escoposTipo.existeSimbolo(simbolo))){
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" nao declarado");
        }
      }
        //Verificando a primeira variavel de leia
      if(ctx.identificador()!=null){
        String simbolo = (String) visitIdentificador(ctx.identificador()); //ctx.identificador().IDENT().getText();
        if((!escopos.existeSimbolo(simbolo))&&(!escoposTipo.existeSimbolo(simbolo))){
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" nao declarado");
        }
      }
    if(ctx.chamada_atribuicao()!=null){
      String simbolo = ctx.IDENT().getText();
      if (ctx.chamada_atribuicao().outros_ident() != null) {
        simbolo += visitOutros_ident(ctx.chamada_atribuicao().outros_ident());
      }
      String tipo=(String)tipo_expressao(ctx.chamada_atribuicao().expressao());
      //System.out.println(escopos.getTipoSimbolo(ctx.IDENT().getText())+" a "+ tipo);
      boolean atribInvalida=!tipo.equals(escopos.getTipoSimbolo(ctx.IDENT().getText()));
      if(escopos.getTipoSimbolo(ctx.IDENT().getText())==null)
        atribInvalida=false;
      if(tipo.equals(""))
        atribInvalida=true;
      //System.out.println(ctx.IDENT().getText());
      if(atribInvalida){
        Saida.println("Linha " +ctx.IDENT().getSymbol().getLine()+
              ": atribuicao nao compativel para "+ simbolo);
      }
    }else  if((ctx.expReturn != null)&&(escopos.topo().getEscopo() != "Funcao")){
        Saida.println("Linha "+ctx.getStart().getLine() + ": comando retorne nao permitido nesse escopo");
      }
    return visitChildren(ctx);
    //Verificando as demais variaveis de leia
  //  visitMais_ident(ctx.mais_ident());
    //falta verificar na expressao
  }


  @Override
  public Object visitMais_ident(LAParser.Mais_identContext ctx) {
    for(LAParser.IdentificadorContext ct: ctx.lista_ident){
      String simbolo = (String) visitIdentificador(ct); //ct.IDENT().getText();

      if((!escopos.existeSimbolo(simbolo))&&(!escoposTipo.existeSimbolo(simbolo))){
        Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" nao declarado");
      }


    }

    return null;
  }



  @Override
  public Object visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {

    TabelaDeSimbolos atual = escopos.topo();
    TabelaDeSimbolos atualTipo = escoposTipo.topo();

    if (ctx.variavel() != null){
      visitVariavel(ctx.variavel());

    } else if (ctx.tipo_basico() != null){
      String tipo = (String) visitTipo_basico(ctx.tipo_basico());
      String simbolo = ctx.IDENT().getText();
      //adicionando constante na tabela de simbolo tipos
      if(!atual.existeSimbolo(simbolo)&&(!atualTipo.existeSimbolo(simbolo))){
        atualTipo.adicionarSimbolo(simbolo, tipo);
      } else {
        Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
      }
      visitValor_constante(ctx.valor_constante());
    } else {
      /*
      String tipo = (String) visitTipo(ctx.tipo());
      String simbolo = ctx.IDENT().getText();
*/     String tipo = "";
      String simbolo = ctx.IDENT().getText();
      if (ctx.tipo().tipo_estendido() != null) {
        tipo = (String) visitTipo(ctx.tipo());
      } else if (ctx.tipo().registro() != null) {
        tipo = (String) visitRegistro(ctx.tipo().registro());
      }

      //adicionando tipo na tabela de simbolo tipos
      if(!atual.existeSimbolo(simbolo)&&(!atualTipo.existeSimbolo(simbolo))){
        atualTipo.adicionarSimbolo(simbolo, tipo);
      } else {
        Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
      }
    }
    return null;
  }

  /*@Override
  public Object visitVariavel(LAParser.VariavelContext ctx) {
    String tipo = (String) visitTipo(ctx.tipo());
    TabelaDeSimbolos atualTipo = escoposTipo.topo();
    TabelaDeSimbolos atual = escopos.topo();
//   EntradaTabelaDeSimbolos etds = new EntradaTabelaDeSimbolos();
    String simbolo = ctx.IDENT().getText();
    if(!atual.existeSimbolo(simbolo)&&(!atualTipo.existeSimbolo(simbolo))){
      atual.adicionarSimbolo(simbolo, tipo);
    } else {
      Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
    }

    for(LAParser.Mais_varContext ct: ctx.lista_mais_var){
      simbolo = ct.IDENT().getText();

      if(!atual.existeSimbolo(simbolo)&&(!atualTipo.existeSimbolo(simbolo))){
        //System.out.println("entrou aqui");
        atual.adicionarSimbolo(simbolo, tipo);

        if (ctx.reg) {
          //System.out.println("entrou aqui2");
          tabelaRegistro.adicionarSimbolo(simbolo, tipo);
          System.out.println("simbolo: "+simbolo+" tipo: "+tipo);
        } else if (atual.getTipo(simbolo) == "registro") { //pra não inserir as variaveis padrão do registro na hora que ele for definido
          for (EntradaTabelaDeSimbolos e : tabelaRegistro.getSimbolos()) {
            System.out.println("simbolo struct: " + simbolo + e.getNome() + "tipo: " + e.getTipo());
            atual.adicionarSimbolo(simbolo + e.getNome(), e.getTipo());

          }
        }
      } else {
        Saida.println("Linha "+ct.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");

      }

    }

    return null;
  }
*/

  @Override
  public Object visitVariavel(LAParser.VariavelContext ctx) {
    String tipo = (String) visitTipo(ctx.tipo());
    TabelaDeSimbolos atualTipo = escoposTipo.topo();
    TabelaDeSimbolos atual = escopos.topo();
//   EntradaTabelaDeSimbolos etds = new EntradaTabelaDeSimbolos();
    //System.out.println("struct = "+ctx.reg);
    String simbolo = ctx.IDENT().getText();    //original
    //System.out.println("simbolo = "+simbolo);
    /*
    if (ctx.reg != ""){
      System.out.println("entrou aqui");
      simbolo = ctx.reg+"."+ simbolo;
      System.out.println("simbolo com struct = "+simbolo);
    }
*/
    if (!atual.existeSimbolo(simbolo) && (!atualTipo.existeSimbolo(simbolo))) {
      atual.adicionarSimbolo(simbolo, tipo);

      if (ctx.reg) {
        //System.out.println("entrou aqui");
        tabelaRegistro.adicionarSimbolo(simbolo, tipo);
        //System.out.println("simbolo: "+simbolo+" tipo: "+tipo);
      } else if (atual.getTipo(simbolo).equals("registro")) { //pra não inserir as variaveis padrão do registro na hora que ele for definido
        for (EntradaTabelaDeSimbolos e : tabelaRegistro.getSimbolos()) {
          //System.out.println("simbolo struct: " + simbolo +"."+ e.getNome() + " tipo: " + e.getTipo());
          atual.adicionarSimbolo(simbolo +"."+ e.getNome(), e.getTipo());
        }
      }
    } else {
      Saida.println("Linha " + ctx.getStart().getLine() + ": identificador " + simbolo + " ja declarado anteriormente");
    }
    if (ctx.mais_var() != null) {
      for (LAParser.Mais_varContext ct : ctx.lista_mais_var) {
        simbolo = ct.IDENT().getText();
      /*
      if (ctx.reg != ""){
        System.out.println("entrou aqui virgula");
        simbolo = ctx.reg+"."+ simbolo;
        System.out.println("simbolo com struct virgula = "+simbolo);
      }
      */
        //System.out.println("simbolo: "+ simbolo+ " tipo: "+tipo);
        if (!atual.existeSimbolo(simbolo) && (!atualTipo.existeSimbolo(simbolo))) {
          atual.adicionarSimbolo(simbolo, tipo);
          //System.out.println("mais var");
          //System.out.println("mais var  simbolo: "+simbolo+" tipo: "+tipo);
          //System.out.println("qual eh: "+atualTipo.getTipo(tipo));
          //System.out.println(ctx.reg || atual.getTipo(simbolo) == "registro");
          if (ctx.reg || atual.getTipo(simbolo) == "registro") {
            tabelaRegistro.adicionarSimbolo(simbolo, tipo);
            for (EntradaTabelaDeSimbolos e : tabelaRegistro.getSimbolos()) {
              //System.out.println("simbolo struct: "+simbolo+"."+e.getNome()+" tipo: "+e.getTipo());
              atual.adicionarSimbolo(simbolo + "." + e.getNome(), e.getTipo());
            }

          } else if (atualTipo.getTipo(tipo).equals("registro")) { //pra não inserir as variaveis padrão do registro na hora que ele for definido
            for (EntradaTabelaDeSimbolos e : tabelaRegistro.getSimbolos()) {
              //System.out.println("simbolo struct: "+simbolo+"."+e.getNome()+" tipo: "+e.getTipo());
              atual.adicionarSimbolo(simbolo + "." + e.getNome(), e.getTipo());
            }
          }
        } else {
          Saida.println("Linha " + ct.getStart().getLine() + ": identificador " + simbolo + " ja declarado anteriormente");

        }
      }
    }

    return null;
  }

  @Override
  public Object visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
    if(ctx == null)
      return null;
    if(ctx.tipo_basico_ident()!=null)
      return visitTipo_basico_ident(ctx.tipo_basico_ident());
    return null;
  }

  @Override
  public Object visitTipo_basico_ident(LAParser.Tipo_basico_identContext ctx) {

    if(ctx != null){
      if(ctx.tipo_basico() != null){
        return visitTipo_basico(ctx.tipo_basico());
      }else if(ctx.IDENT() != null){
        if(escoposTipo.existeSimbolo(ctx.IDENT().getText())){
          return ctx.IDENT().getText();
        }else{
          Saida.println("Linha "+ctx.IDENT().getSymbol().getLine() + ": tipo "
                  +ctx.IDENT().getText() +" nao declarado");
        }

      }
    }



    return null;
  }

  @Override
  public Object visitTipo_basico(LAParser.Tipo_basicoContext ctx) {
    if(ctx!=null)
      return ctx.getText();
    return null;
  }

  @Override
  public Object visitTipo(LAParser.TipoContext ctx) {

    if (ctx != null) {
      if (ctx.tipo_estendido() != null) {
        //  ctx.reg = "";
        return visitTipo_estendido(ctx.tipo_estendido());
      } else if (ctx.registro() != null) {
        //System.out.println("entra registro");
        return visitRegistro(ctx.registro());
      }
    }
    return null;
  }

  @Override
  public Object visitRegistro(LAParser.RegistroContext ctx) {
    visitVariavel(ctx.variavel());
    visitMais_variaveis(ctx.mais_variaveis());
    return "registro";
  }


  @Override
  public Object visitIdentificador(LAParser.IdentificadorContext ctx) {
    //checagem se o simbolo ja foi declarado
    String simbolo = ctx.IDENT().getText();

    if(ctx.outros_ident() != null){
      simbolo += visitOutros_ident(ctx.outros_ident());
    }

    return simbolo;

  }

  @Override
  public Object visitOutros_ident(LAParser.Outros_identContext ctx) {
    String outrosIdent = "";
    for (LAParser.IdentificadorContext ct : ctx.lista_outrosIdent){
      outrosIdent += "."+ct.IDENT().getText();
    }

    return outrosIdent;
  }
}
