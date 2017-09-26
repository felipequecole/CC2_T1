package t1;

public class AnalisadorSemantico extends LABaseVisitor{
  @Override
  public Object visitPrograma(LAParser.ProgramaContext ctx) {
    return visitChildren(ctx);
  }
}
