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
  CommonTokenStream cts;
  public void setTokenStream(CommonTokenStream c){
    cts=c;
  }
  public Object visitExpressao(LAParser.ExpressaoContext ctx){
    //System.out.println((String) tipo_expressao(ctx));
    for(int i=ctx.getSourceInterval().a;i<=ctx.getSourceInterval().b;i++) {
      Token token=cts.get(i);
      if(token.getType()==LAParser.IDENT){
        if(!escopos.existeSimbolo(token.getText())){
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
    return visitCorpo(ctx.corpo());
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
    return visitDecl_local_global(ctx.decl_local_global());
  }

  @Override
  public Object visitDecl_local_global(LAParser.Decl_local_globalContext ctx) {
    return visitDeclaracao_local(ctx.declaracao_local());
  }


  @Override
  public Object visitComandos(LAParser.ComandosContext ctx) {
      if(ctx.cmd() !=null){
        visitCmd(ctx.cmd());
      }

    return null;
  }

  @Override
  public Object visitCmd(LAParser.CmdContext ctx) {
    //Verificando a primeira variavel de leia
    String simbolo = ctx.identificador().IDENT().getText();
    if(!escopos.existeSimbolo(simbolo)){
      Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" nao declarado");
    }
    //Verificando as demais variaveis de leia
    visitMais_ident(ctx.mais_ident());
    //falta verificar na expressao

    return null;
  }


  @Override
  public Object visitMais_ident(LAParser.Mais_identContext ctx) {

    for(LAParser.IdentificadorContext ct: ctx.lista_ident){
      String simbolo = ct.IDENT().getText();
      if(!escopos.existeSimbolo(simbolo)){
        Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" nao declarado");
      }
    }

    return null;
  }

  @Override
  public Object visitMais_expressao(LAParser.Mais_expressaoContext ctx) {

    return null;
  }

  @Override
  public Object visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
    if (ctx.variavel() != null){
      visitVariavel(ctx.variavel());

    } else if (ctx.tipo_basico() != null){
      visitTipo_basico(ctx.tipo_basico());
      visitValor_constante(ctx.valor_constante());
    } else {
      visitTipo(ctx.tipo());
    }
    return null;
  }

  @Override
  public Object visitVariavel(LAParser.VariavelContext ctx) {
    String tipo = (String) visitTipo(ctx.tipo());
    TabelaDeSimbolos atual = escopos.topo();
//   EntradaTabelaDeSimbolos etds = new EntradaTabelaDeSimbolos();
    String simbolo = ctx.IDENT().getText();
    if(!atual.existeSimbolo(simbolo)){
      atual.adicionarSimbolo(simbolo, tipo);
    } else {
      Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
    }

    for(LAParser.Mais_varContext ct: ctx.lista_mais_var){
      simbolo = ct.IDENT().getText();

      if(!atual.existeSimbolo(simbolo)){
        atual.adicionarSimbolo(simbolo, tipo);
      } else {
        Saida.println("Linha "+ct.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");

      }
    }

    return null;
  }

  @Override
  public Object visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
    return visitTipo_basico_ident(ctx.tipo_basico_ident());
  }

  @Override
  public Object visitTipo_basico_ident(LAParser.Tipo_basico_identContext ctx) {
    if(ctx!=null)
        return visitTipo_basico(ctx.tipo_basico());
    return null;
  }

  @Override
  public Object visitTipo_basico(LAParser.Tipo_basicoContext ctx) {
    if(ctx!=null)
      return ctx.getText();
    return null;
  }
}
