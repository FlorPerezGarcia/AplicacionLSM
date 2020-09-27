package android.flor.mundo.hola.aplicacionlsm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVFile {
  InputStream inputStream;

  public CSVFile(InputStream inputStream){
    this.inputStream = inputStream;
  }

  public float[][] read(int filas, int columnas){
    //List resultList = new ArrayList();
    float[][] matrixPesos = new float[filas][columnas];
    int fila = 0;

    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    try {
      String csvLine;
      while ((csvLine = reader.readLine()) != null) {
        String[] row = csvLine.split(",");
        for(int i=0; i < row.length; i++){
          matrixPesos[fila][i] = Float.parseFloat(row[i]);
        }
        fila++;
          //resultList.add(row);
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Error leyendo archivo CSV: "+ex);
    }
    finally {
      try {
        inputStream.close();
      }
      catch (IOException e) {
        throw new RuntimeException("Error cerrando input stream: "+e);
      }
    }
    return matrixPesos;
  }
}
