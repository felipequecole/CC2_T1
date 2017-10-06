// @franksonTeotonho

grammar LA;

IDENT	:	('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')*;

CADEIA	:	'"' ~('\n' | '\r' | '\'')* '\'' | '"' ~('\n' | '\r' | '"')* '"';

NUM_INT: ('-')? '0'..'9' ('0'..'9')* ;
NUM_REAL: NUM_INT  ('.' '0'..'9' ('0'..'9')*)?;

COMMENT : '{'.*?'}' -> skip;
COMMENTNFECHADO: '{' .*? ;
WS	:	(' ' | '\t' | '\r' | '\n') -> skip;
ERROCHAR:.;

programa : declaracoes 'algoritmo' corpo 'fim_algoritmo';
declaracoes : decl_local_global declaracoes |;
decl_local_global : declaracao_local | declaracao_global;
declaracao_local : 'declare' variavel
 | 'constante' IDENT ':' tipo_basico '=' valor_constante
 | 'tipo' IDENT ':' tipo;
variavel : IDENT dimensao lista_mais_var+= mais_var* ':' tipo;
mais_var : ',' IDENT dimensao ;
identificador : ponteiros_opcionais IDENT dimensao outros_ident;
ponteiros_opcionais : '^' ponteiros_opcionais |;
outros_ident : '.' identificador |;
dimensao : '[' exp_aritmetica ']' dimensao|;
tipo : registro | tipo_estendido;
//mais_ident : ',' identificador mais_ident |;
mais_ident : (',' lista_ident += identificador)*;
mais_variaveis : variavel mais_variaveis |;
tipo_basico : 'literal' | 'inteiro' | 'real' | 'logico';
tipo_basico_ident : tipo_basico | IDENT;


//Felipe
tipo_estendido : ponteiros_opcionais tipo_basico_ident ;
valor_constante : CADEIA | NUM_INT | NUM_REAL | 'verdadeiro' | 'falso' ;
registro : 'registro' variavel mais_variaveis 'fim_registro' ;
declaracao_global : 'procedimento' IDENT '(' parametros_opcional ')' declaracoes_locais comandos 'fim_procedimento'
 | 'funcao' IDENT '(' parametros_opcional ')' ':' tipo_estendido declaracoes_locais comandos 'fim_funcao' ;
parametros_opcional : parametro | ;
parametro : var_opcional identificador mais_ident ':' tipo_estendido mais_parametros ;
var_opcional : 'var' | ;
mais_parametros : ',' parametro | ;
declaracoes_locais : declocais+=declaracao_local*  ;
corpo : declaracoes_locais comandos ;
comandos : cmd comandos | ;
cmd : 'leia' '(' identificador mais_ident ')'
 | 'escreva' '(' expressao mais_expressao ')'
 | 'se' expressao 'entao' comandos senao_opcional 'fim_se'
 | 'caso' exp_aritmetica 'seja' selecao senao_opcional 'fim_caso'
 | 'para' IDENT '<-' exp_aritmetica 'ate' exp_aritmetica 'faca' comandos 'fim_para'
 | 'enquanto' expressao 'faca' comandos 'fim_enquanto'
 | 'faca' comandos 'ate' expressao
 | '^' IDENT outros_ident dimensao '<-' expressao
 | IDENT chamada_atribuicao
 | 'retorne' expressao ;
//mais_expressao : ',' expressao mais_expressao | ;
mais_expressao : (',' lista_expressao += expressao)*;
senao_opcional : 'senao' comandos | ;
// Fim


// &Italo
chamada_atribuicao : '(' argumentos_opcional ')' | outros_ident dimensao '<-' expressao;
argumentos_opcional : expressao mais_expressao |;
selecao : constantes ':' comandos mais_selecao;
mais_selecao : selecao |;
constantes : numero_intervalo mais_constantes;
mais_constantes : ',' constantes |;
numero_intervalo : op_unario NUM_INT intervalo_opcional;
intervalo_opcional : '..' op_unario NUM_INT |;
op_unario : '-' |;
exp_aritmetica : termo outros_termos;
op_multiplicacao : '*' | '/';
op_adicao : '+' | '-';
termo : fator outros_fatores;
outros_termos : op_adicao termo outros_termos |;
fator : parcela outras_parcelas;
outros_fatores: op_multiplicacao fator outros_fatores |;


//@duduyamauchi
parcela : op_unario parcela_unario | parcela_nao_unario;
parcela_unario : '^' IDENT outros_ident dimensao | IDENT chamada_partes | NUM_INT | NUM_REAL | '(' expressao ')';
parcela_nao_unario : '&' IDENT outros_ident dimensao | CADEIA;
outras_parcelas : '%' parcela outras_parcelas | ;
chamada_partes : '(' expressao mais_expressao ')' | outros_ident dimensao ;
exp_relacional : exp_aritmetica op_opcional;
op_opcional : op_relacional exp_aritmetica | ;
op_relacional : '=' | '<>' | '>=' | '<=' | '>' | '<';
expressao : termo_logico outros_termos_logicos;
op_nao : 'nao' | ;
termo_logico : fator_logico outros_fatores_logicos;
outros_termos_logicos : 'ou' termo_logico outros_termos_logicos | ;
//outros_termos_logicos : 'ou' lista_termosLogicos += termo_logico * ;
outros_fatores_logicos : 'e' fator_logico outros_fatores_logicos | ;
//outros_fatores_logicos : 'e' lista_fatoresLogicos += fator_logico*;
fator_logico : op_nao parcela_logica;
parcela_logica : 'verdadeiro' | 'falso' | exp_relacional;
