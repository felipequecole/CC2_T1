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


  CommonTokenStream cts;
  public void setTokenStream(CommonTokenStream c){
    cts=c;
  }
  public Object visitExpressao(LAParser.ExpressaoContext ctx){
    //System.out.println((String) tipo_expressao(ctx));
    for(int i=ctx.getSourceInterval().a;i<=ctx.getSourceInterval().b;i++) {
      Token token=cts.get(i);
      if(token.getType()==LAParser.IDENT){
        if((!escopos.existeSimbolo(token.getText()))&&(!escoposTipo.existeSimbolo(token.getText()))){
          Saida.println("Linha "+ token.getLine()+": identificador " +token.getText()+ " nao declarado");
        }
      }
    }
    return null;
  }


  @Override
  public Object visitOutros_termos_logicos(LAParser.Outros_termos_logicosContext ctx) {

    return null;
  }

  public Object tipo_expressao(LAParser.ExpressaoContext ctx){
    if(ctx.outros_termos_logicos().getChildCount()!=0)
      return "logico";
    if(ctx.termo_logico().outros_fatores_logicos().getChildCount()!=0)
      return "logico";
    return visitParcela_logica(ctx.termo_logico().fator_logico().parcela_logica());
  }

  @Override
  public Object visitFator_logico(LAParser.Fator_logicoContext ctx){
    return null;
  }

  @Override
  public Object visitOutros_fatores_logicos(LAParser.Outros_fatores_logicosContext ctx) {

    return null;
  }

  @Override
  public Object visitParcela_logica(LAParser.Parcela_logicaContext ctx){
    if(ctx.exp_relacional().getChildCount()==0)
      return "logico";
    return visitExp_relacional(ctx.exp_relacional());
  }
  @Override
  public Object visitExp_relacional(LAParser.Exp_relacionalContext ctx){
    if(ctx.op_opcional().getChildCount()!=0){
      return "logico";
    }
    /*** falta fazer expressão aritmerica; diferenciar inteiro de real***/

    return "inteiro";
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
      System.out.println("passei aqui");
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
          System.out.println("Adicionei: "+ simbolo);
          atualTipo.adicionarSimbolo(simbolo,tipo);
        }else{
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
        }
        // é um procedimento
      }else{
        if((!escopos.existeSimbolo(simbolo))&&(!escoposTipo.existeSimbolo(simbolo))){
          atualTipo.adicionarSimbolo(simbolo,"void");
          System.out.println("Adicionei: "+ simbolo);
        }else{
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
        }
      }
        //escopo de um funcao ou procedimento
        escopos.empilhar(new TabelaDeSimbolos("Funcao|Procedimento"));

        if(ctx.parametros_opcional()!=null){
            visitParametros_opcional(ctx.parametros_opcional());
        }
        //visitando os comandos
        if(ctx.comandos() != null){
          visitComandos(ctx.comandos());
        }
        //saindo do escopo funcao ou procedimento
        escopos.desempilhar();
    }
    return null;
  }

  @Override
  public Object visitParametros_opcional(LAParser.Parametros_opcionalContext ctx) {
    TabelaDeSimbolos atual = escopos.topo();
    if(ctx != null){
      for(LAParser.ParametroContext ct: ctx.lista_parametro){
        visitParametro(ct);
      }
    }
    return null;
  }

  @Override
  public Object visitParametro(LAParser.ParametroContext ctx) {
    TabelaDeSimbolos atual = escopos.topo();
    if(ctx != null){
      if(ctx.identificador()!=null){
        //adicionando o primeiro parametro na tabela
        String simbolo = ctx.identificador().IDENT().getText();
        String tipo =(String)visitTipo_estendido(ctx.tipo_estendido());

        if(!atual.existeSimbolo(simbolo)){
          atual.adicionarSimbolo(simbolo,tipo);
        }else{
          Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
        }
        //adicionando as demais
        if(ctx.mais_ident() != null){
          for(LAParser.IdentificadorContext ct: ctx.mais_ident().lista_ident){
            simbolo = ct.IDENT().getText();
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
        visitParametro(ct);
      }
    }

    return null;
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
    //Verificando a primeira variavel de leia
    if(ctx!=null){
        if(ctx.identificador()!=null){
          String simbolo = ctx.identificador().IDENT().getText();
          if((!escopos.existeSimbolo(simbolo))&&(!escoposTipo.existeSimbolo(simbolo))){
            Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" nao declarado");
          }
        }
      return visitChildren(ctx);
    }
    return null;
    //Verificando as demais variaveis de leia
  //  visitMais_ident(ctx.mais_ident());
    //falta verificar na expressao
  }


  @Override
  public Object visitMais_ident(LAParser.Mais_identContext ctx) {
    for(LAParser.IdentificadorContext ct: ctx.lista_ident){
      String simbolo = ct.IDENT().getText();

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
      String tipo = (String) visitTipo(ctx.tipo());
      String simbolo = ctx.IDENT().getText();

      //adicionando tipo na tabela de simbolo tipos
      if(!atual.existeSimbolo(simbolo)&&(!atualTipo.existeSimbolo(simbolo))){
        atualTipo.adicionarSimbolo(simbolo, tipo);
      } else {
        Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
      }
    }
    return null;
  }

  @Override
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
        atual.adicionarSimbolo(simbolo, tipo);
      } else {
        Saida.println("Linha "+ct.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");

      }
    }

    return null;
  }

  @Override
  public Object visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
    if(ctx != null){
      if(ctx.tipo_basico_ident()!=null){
        return visitTipo_basico_ident(ctx.tipo_basico_ident());
      }
    }

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

    return visitTipo_estendido(ctx.tipo_estendido());
  }
}
