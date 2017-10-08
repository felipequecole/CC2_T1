package t1;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frankson on 07/10/17.
 */
public class ParametrosFuncProc {
    private ArrayList<String> Lista;
    private String identificador;

    public ParametrosFuncProc(String identificador){
        this.identificador = identificador;
        Lista = new ArrayList<String>();
    }

    public String getIdentificador(){
        return identificador;
    }

    public void setLista(ArrayList<String> NLista){
        this.Lista = NLista;
    }

    public ArrayList<String> getLista(){
        return this.Lista;
    }

}
