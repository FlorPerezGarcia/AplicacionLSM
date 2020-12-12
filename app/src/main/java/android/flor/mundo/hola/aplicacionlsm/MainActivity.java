package android.flor.mundo.hola.aplicacionlsm;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

  ImageButton IdDesconectar, IdBorrar, IdReproducir;
  EditText IdBufferIn;

  Handler bluetoothIn;
  final int handlerState = 0;
  private float sumaTotal;
  private int countTrama= 0;
  final int neuronas = 16;
  final int rowPesos = 16, colPesos = 330;
  final int rowPolarizacion = 1, colPolarizacion = 16;
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder DataStringIN = new StringBuilder();
  private StringBuilder dataMessageFull = new StringBuilder();
  private ConnectedThread MyConexionBT;

  List<float[]> listPesos             = new ArrayList<float[]>();
  private float[] arrayPolarizacion = new float[neuronas];
  private float[] sumaNeurona = new float[neuronas];
  private float[] letraReconocida = new float[neuronas];
  private float[][] matrixPesos= new float[rowPesos][colPesos];
  private float[][] matrixPolarizacion= new float[rowPolarizacion][colPolarizacion];

  private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private static String address = null;
  private TextToSpeech tts;
  private ItemArrayAdapter itemArrayAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    IdDesconectar     = (ImageButton) findViewById(R.id.btnSalir);
    IdBorrar          = (ImageButton) findViewById(R.id.btnBorrar);
    IdBufferIn        = (EditText) findViewById(R.id.tvMensaje);
    IdReproducir      = (ImageButton) findViewById(R.id.btnReproducir);
    itemArrayAdapter  = new ItemArrayAdapter(getApplicationContext(), R.layout.item_layout);

    InputStream inputStreamPesos        = getResources().openRawResource(R.raw.pesos);
    InputStream inputStreamPolarizacion = getResources().openRawResource(R.raw.polarizacion);
    CSVFile csvFilePesos                = new CSVFile(inputStreamPesos);
    CSVFile csvFilePolarizacion         = new CSVFile(inputStreamPolarizacion);
    matrixPesos                         = csvFilePesos.read(rowPesos, colPesos);
    matrixPolarizacion                  = csvFilePolarizacion.read(rowPolarizacion, colPolarizacion);
    for(int i= 0; i < rowPesos; i++){
      listPesos.add(matrixPesos[i]);
    }

    for (int j = 0; j <neuronas; j++) {
      arrayPolarizacion[j] = matrixPolarizacion[0][j];
    }
    tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        Locale locSpanish = new Locale("spa", "MEX");
        if(status != TextToSpeech.ERROR) {
          tts.setLanguage(locSpanish);
        }
       }
    });

    bluetoothIn = new Handler() {
      public void handleMessage(android.os.Message msg) {

        if (msg.what == handlerState) {
          String readMessage = (String) msg.obj;
          DataStringIN.append(readMessage);
          int startOfLineIndex = DataStringIN.indexOf("a")+2;
          int endOfLineIndex = DataStringIN.indexOf("#")-1;


          if (endOfLineIndex > 0 && endOfLineIndex > startOfLineIndex) {
            float[] x = new float[330];
            //Log.i("DataStringIN.length(): ", String.valueOf(DataStringIN.length()));
            Log.i("endOfLineIndex: ", String.valueOf(endOfLineIndex));
            Log.i("startOfLineIndex: ", String.valueOf(startOfLineIndex));
            String dataInPrint = DataStringIN.substring(startOfLineIndex, endOfLineIndex);
            int indexFlagSubTrama = 0;
            String[] entrada = dataInPrint.split(",");
            String[] entrada_aux = new String[330];
            int aux = 0;
            for(int j=0; j < entrada.length; j++) {
                if (String.valueOf(entrada[j]).contains("i")) {
                  for (int i = 0; i < 11; i++) {
                    entrada_aux[aux + i] = entrada[j+1+i];
                    Log.i("entrada_aux", String.valueOf(entrada_aux[aux+i]));
                    Log.i("entrada", String.valueOf(entrada[j+1+i]));
                  }
                  aux = aux + 11;
                }
                j=j+11;
              }
            Log.i("entrada_aux longitud", String.valueOf(entrada_aux.length));
              if(dataInPrint.contains("&,")){
                dataMessageFull.append(" ");
              }

              if (entrada_aux.length > 329) {
                x = convert_float_array(entrada_aux);
                for (int i = 0; i < neuronas; i++) {
                  //Log.i("neuronas ", String.valueOf(i));
                  sumaNeurona[i] = processData(x, listPesos.get(i), arrayPolarizacion[i], i);
                }
                recognitionData(sumaNeurona);
                //IdBufferIn.setText(dataInPrint);
                entrada_aux = null;
                DataStringIN.delete(0, DataStringIN.length());
              }

          }
        }
      }
    };

    IdReproducir.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String toSpeak = IdBufferIn.getText().toString();
        Toast.makeText(getApplicationContext(), "Reproduciendo",Toast.LENGTH_SHORT).show();
        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
      }
    });

    IdBorrar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(dataMessageFull.length() > 0 ){
          Log.i("dataMessageFull", String.valueOf(dataMessageFull));
          dataMessageFull.deleteCharAt(dataMessageFull.length() - 1 );
          IdBufferIn.setText(dataMessageFull);
        }
      }
    });
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    VerificarEstadoBT();

    IdDesconectar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onBackPressed();
      }
    });
  }

  public float[] convert_float_array(String[] entrada_aux){
    float[] xi = new float[330];
    for(int i = 0; i < entrada_aux.length; i++){
//      Log.i("entrada", String.valueOf(entrada[i]));
      if(!entrada_aux[i].isEmpty() || entrada_aux[i] != null){
        xi[i] = Float.valueOf(entrada_aux[i]);
      }
      //Log.i("xi", String.valueOf(xi[i]));
    }
    return xi;
  }

  public static float processData(float[] xi, float[] wi, float b, int numNeurona){
    float productoPunto = 0;
    //Log.i("xi length", String.valueOf(xi.length));
    //Log.i("wi length", String.valueOf(wi.length));
    //Log.i("b ", String.valueOf(b));

    int n = xi.length;
    for(int i=0; i<n; i++){
      productoPunto += xi[i]*wi[i];
    }
    productoPunto = productoPunto + b;
    return productoPunto;
  }

  public void recognitionData(float[] resultadoNeurona){
//    Log.i("resultadoNeurona", String.valueOf(resultadoNeurona));
    // Función de activación heavside
    for(int n=0; n<neuronas;n++){
      if(resultadoNeurona[n] > 0){
        letraReconocida[n] = 1;
      }
      else{
        letraReconocida[n] = 0;
      }
    }
    for(int i=0 ; i<neuronas;i++){
      if(i==0 && letraReconocida[i] == 1){
        Log.i("a ",String.valueOf(letraReconocida[i]));
          dataMessageFull.append("a");
      }
      else if(i==1 && letraReconocida[i] == 1){
        Log.i("b ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("b");
      }
      else if(i==2 && letraReconocida[i] == 1){
        Log.i("c ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("c");
      }
      else if(i==3 && letraReconocida[i] == 1){
        Log.i("d ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("d");
      }
      else if(i==4 && letraReconocida[i] == 1){
        Log.i("e ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("e");
      }
      else if(i==5 && letraReconocida[i] == 1){
        Log.i("f ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("f");
      }
      else if(i==6 && letraReconocida[i] == 1){
        Log.i("g ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("g");
      }
      else if(i==7 && letraReconocida[i] == 1){
        Log.i("h ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("h");
      }
      else if(i==8 && letraReconocida[i] == 1){
        Log.i("i ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("i");
      }
      else if(i==9 && letraReconocida[i] == 1){
        Log.i("j ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("j");
      }
      else if(i==10 && letraReconocida[i] == 1){
        Log.i("k ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("k");
      }
      else if(i==11 && letraReconocida[i] == 1){
        Log.i("l ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("l");
      }
      else if(i==12 && letraReconocida[i] == 1){
        Log.i("m ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("m");
      }
      else if(i==13 && letraReconocida[i] == 1){
        Log.i("n ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("n");
      }
      else if(i==14 && letraReconocida[i] == 1){
        Log.i("ñ ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("ñ");
      }
      else if(i==15 && letraReconocida[i] == 1){
        Log.i("o ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("o");
      }
      /*else if(i==16 && letraReconocida[i] == 1){
        Log.i("p ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("p");
      }
      else if(i==17 && letraReconocida[i] == 1){
        Log.i("q ",String.valueOf(letraReconocida[i]));
        dataMessageFull.append("q");
      }*/
      IdBufferIn.setText(dataMessageFull);
    }
  }

  @Override
  public void onBackPressed() {
    new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle("Salir")
      .setMessage("¿Estás seguro de salir de Manos con voz?")
      .setPositiveButton("Sí", new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          Intent homeIntent = new Intent(Intent.ACTION_MAIN);
          homeIntent.addCategory( Intent.CATEGORY_HOME );
          homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(homeIntent);
        }

      })
      .setNegativeButton("No", null)
      .show();
  }

  @Override
  protected void onDestroy() {
    if(tts != null){
      tts.stop();
      tts.shutdown();
    }
    super.onDestroy();
  }

  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
    return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
  }

  @Override
  public void onResume() {
    super.onResume();
    Intent intent = getIntent();
    address = intent.getStringExtra(ListDevices.EXTRA_DEVICE_ADDRESS);
    BluetoothDevice device = btAdapter.getRemoteDevice(address);

    try {
      btSocket = createBluetoothSocket(device);
    } catch (IOException e) {
      Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
    }

    try {
      btSocket.connect();
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {

      }
    }
    MyConexionBT = new ConnectedThread(btSocket);
    MyConexionBT.start();
  }

  @Override
  public void onPause() {
    super.onPause();
    try
    {
      btSocket.close();
    } catch (IOException e2) {}
  }

  private void VerificarEstadoBT() {
    String TAG = "MainActivity";
    Log.i(TAG, "VerificarEstadoBT");
    if(btAdapter == null) {
      Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
    } else {
      if (btAdapter.isEnabled()) {
      } else {
        Log.i("Conexion establecida",String.valueOf(btAdapter.isEnabled()));
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }

  private class ConnectedThread extends Thread {
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket) {
      InputStream tmpIn = null;
      OutputStream tmpOut = null;
      try
      {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      } catch (IOException e) { }
      mmInStream = tmpIn;
      mmOutStream = tmpOut;
    }

    public void run() {
      byte[] buffer = new byte[256];
      int bytes;

      while (true) {
        try {
          bytes = mmInStream.read(buffer);
          String readMessage = new String(buffer, 0, bytes);
          bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
        } catch (IOException e) {
          break;
        }
      }
    }

    public void write(String input) {
      try {
        mmOutStream.write(input.getBytes());
      }
      catch (IOException e)
      {
        Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }
}

