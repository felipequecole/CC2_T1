#sed -i '1 i asdkfjçalsdf;' LA*.java
for f in LA*.java; do 
sed '1 i package pacagio;' $f > $f; 
done
