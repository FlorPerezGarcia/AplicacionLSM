package android.flor.mundo.hola.aplicacionlsm;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class ItemArrayAdapter extends ArrayAdapter<Float[]> {
  private List<String[]> listPesos = new ArrayList<String[]>();
  private float[] arrayPeso= new float[330];

  static class ItemViewHolder {
    TextView name;
    TextView score;
  }

  public ItemArrayAdapter(Context context, int textViewResourceId) {
    super(context, textViewResourceId);
  }


  public float[] add(String[] object) {
    for(String data: object){
      for(int i = 0; i < object.length; i++){
        arrayPeso[i] = Float.valueOf(data);
      }
    }
    //Log.i("arrayPeso", arrayPeso.toString());
    //listPeso  s.add(arrayPeso);
//
    //super.add(object);
  return arrayPeso;
  }

  @Override
  public int getCount() {
    Log.i("this.listPesos.size()", String.valueOf(this.listPesos.size()));
    return this.listPesos.size();
  }
}
