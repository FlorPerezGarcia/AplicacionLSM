package android.flor.mundo.hola.aplicacionlsm;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

  ImageButton IdDesconectar, IdBorrar, IdReproducir;
  EditText IdBufferIn;

  Handler bluetoothIn;
  final int handlerState = 0;
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder DataStringIN = new StringBuilder();
  private ConnectedThread MyConexionBT;

  private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private static String address = null;
  private TextToSpeech tts;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    IdDesconectar = (ImageButton) findViewById(R.id.btnSalir);
    IdBorrar = (ImageButton) findViewById(R.id.btnBorrar);
    IdBufferIn = (EditText) findViewById(R.id.tvMensaje);
    IdReproducir = (ImageButton) findViewById(R.id.btnReproducir);

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
          //StringBuilder stringBuilder = new StringBuilder(readMessage.getText().toString());
          DataStringIN.append(readMessage);
          int endOfLineIndex = DataStringIN.indexOf("#");

          if (endOfLineIndex > 0) {
            String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
            IdBufferIn.setText("Dato: " + dataInPrint);
            DataStringIN.delete(0, DataStringIN.length());
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
        IdBufferIn.setText("");
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

    try
    {
      btSocket = createBluetoothSocket(device);
    } catch (IOException e) {
      Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
    }

    try
    {
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

    if(btAdapter==null) {
      Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
    } else {
      if (btAdapter.isEnabled()) {
      } else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }

  private class ConnectedThread extends Thread {
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket)
    {
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

    public void run()
    {
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
    public void write(String input)
    {
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

