package t1;

public class AnalisadorSemantico extends LABaseVisitor{
  PilhaDeTabelas escopos = new PilhaDeTabelas();

  @Override
  public Object visitPrograma(LAParser.ProgramaContext ctx) {
    escopos.empilhar(new TabelaDeSimbolos("global"));
    return visitCorpo(ctx.corpo());
  }
  @Override
  public Object visitCorpo(LAParser.CorpoContext ctx) {
    escopos.empilhar(new TabelaDeSimbolos("local"));
    return visitDeclaracoes_locais(ctx.declaracoes_locais());

  }

    @Override
    public Object visitDeclaracoes_locais(LAParser.Declaracoes_locaisContext ctx) {
        visitDeclaracao_local(ctx.declaracao_local());

        for (LAParser.Declaracoes_locaisContext ct : ctx.declocais){
          visitDeclaracao_local(ct.declaracao_local());
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
//    EntradaTabelaDeSimbolos etds = new EntradaTabelaDeSimbolos();
    String simbolo = ctx.IDENT().getText();
    if(!atual.existeSimbolo(simbolo)){
      atual.adicionarSimbolo(simbolo, tipo);
    } else {
      Saida.println("Linha "+ctx.getStart().getLine() + ": identificador " +simbolo+" ja declarado anteriormente");
    }
    return null;
  }

  @Override
  public Object visitTipo_estendido(LAParser.Tipo_estendidoContext ctx) {
    return visitTipo_basico_ident(ctx.tipo_basico_ident());
  }

  @Override
  public Object visitTipo_basico_ident(LAParser.Tipo_basico_identContext ctx) {
    return visitTipo_basico(ctx.tipo_basico());

  }

  @Override
  public Object visitTipo_basico(LAParser.Tipo_basicoContext ctx) {
    return ctx.getText();
  }
}
