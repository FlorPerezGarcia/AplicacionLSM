package android.flor.mundo.hola.aplicacionlsm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class ListDevices extends AppCompatActivity {

  private static final String TAG = "ListDevices";
  ListView IdLista;
  public static String EXTRA_DEVICE_ADDRESS = "device_address";
  private BluetoothAdapter mBtAdapter;
  private ArrayAdapter mPairedDevicesArrayAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_list_devices);
  }

  @Override
  public void onResume() {
    super.onResume();
    VerificarEstadoBT();
    mPairedDevicesArrayAdapter = new ArrayAdapter(this, R.layout.nombre_dispositivo);

    IdLista = (ListView) findViewById(R.id.idListaDispositivos);
    IdLista.setAdapter(mPairedDevicesArrayAdapter);
    IdLista.setOnItemClickListener(mDeviceClickListener);

    //mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

    if (pairedDevices.size() > 0)
    {
      for (BluetoothDevice device : pairedDevices) {
        mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
      }
    }
  }

  private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
    public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
      String info = ((TextView) v).getText().toString();
      String address = info.substring(info.length() - 17);

      Intent i = new Intent(ListDevices.this, MainActivity.class);
      i.putExtra(EXTRA_DEVICE_ADDRESS, address);
      startActivity(i);
    }
  };

  private void VerificarEstadoBT() {

    mBtAdapter= BluetoothAdapter.getDefaultAdapter();
    if(mBtAdapter==null) {
      Toast.makeText(getBaseContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
    } else {
      if (mBtAdapter.isEnabled()) {
        //Log.d(TAG, "Bluetooth Activado");
      } else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);

      }
    }
  }
}