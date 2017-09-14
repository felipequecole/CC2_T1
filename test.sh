for f in ./casosDeTesteT1/1.arquivos_com_erros_sintaticos/entrada/*.txt; do
echo "";
echo "--SAIDA-: " $(basename $f)  ;
echo "";
java -cp t1.jar:lib/antlr-4.7-complete.jar t1.TestaAnalisadorSintatico $f saida/$(basename $f);
echo "";
echo "---------------esperada----------------";
echo "";
cat ./casosDeTesteT1/1.arquivos_com_erros_sintaticos/saida/$(basename $f);
read enter;
done;
