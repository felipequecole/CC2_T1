for f in ./casosDeTesteT1/1.arquivos_com_erros_sintaticos/entrada/*.txt; do
echo "";
echo "--SAIDA-: " $(basename $f)  ;
echo "";
java -jar t1.jar $f saida/$(basename $f);
echo "";
echo "---------------esperada----------------";
echo "";
cat ./casosDeTesteT1/1.arquivos_com_erros_sintaticos/saida/$(basename $f);
read enter;
done;
