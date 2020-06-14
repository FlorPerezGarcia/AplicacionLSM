package android.flor.mundo.hola.aplicacionlsm;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity
        implements OnClickListener {

    private static final String TAG = "android.flor.mundo.hola.aplicacionlsm.MainActivity";

    // Declaramos una constante para lanzar los Intent de activacion de Bluetooth
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String ALERTA = "alerta";

    // Declaramos una variable privada para cada control de la actividad
    private Button btnEnviar;
    private Button btnBluetooth;
    private Button btnBuscarDispositivo;
    private Button btnConectarDispositivo;
    private Button btnSalir;
    private EditText txtMensaje;
    private TextView tvMensaje;
    private TextView tvConexion;
    private ListView lvDispositivos;

    private BluetoothAdapter bAdapter;                    // Adapter para uso del Bluetooth
    private ArrayList<BluetoothDevice> arrayDevices;    // Listado de dispositivos
    private ArrayAdapter arrayAdapter;                    // Adaptador para el listado de dispositivos
    private BluetoothService servicio;
    private BluetoothDevice ultimoDispositivo;

    // Instanciamos un BroadcastReceiver que se encargara de detectar si el estado
    // del Bluetooth del dispositivo ha cambiado mediante su handler onReceive
    private final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @SuppressLint("LongLogTag")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            // BluetoothAdapter.ACTION_STATE_CHANGED
            // Codigo que se ejecutara cuando el Bluetooth cambie su estado.
            // Manejaremos los siguientes estados:
            //		- STATE_OFF: El Bluetooth se desactiva
            //		- STATE ON: El Bluetooth se activa
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (estado) {
                    case BluetoothAdapter.STATE_OFF: {
                        Log.v(TAG, "onReceive: Apagando");
                        ((Button) findViewById(R.id.btnBluetooth)).setText(R.string.ActivarBluetooth);
                        ((Button) findViewById(R.id.btnBuscarDispositivo)).setEnabled(false);
                        ((Button) findViewById(R.id.btnConectarDispositivo)).setEnabled(false);
                        break;
                    }

                    case BluetoothAdapter.STATE_ON: {
                        Log.v(TAG, "onReceive: Encendiendo");
                        ((Button) findViewById(R.id.btnBluetooth)).setText(R.string.DesactivarBluetooth);
                        ((Button) findViewById(R.id.btnBuscarDispositivo)).setEnabled(true);
                        ((Button) findViewById(R.id.btnConectarDispositivo)).setEnabled(true);

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                        startActivity(discoverableIntent);

                        break;
                    }
                    default:
                        break;
                }

            } // Fin if

            // BluetoothDevice.ACTION_FOUND
            // Cada vez que se descubra un nuevo dispositivo por Bluetooth, se ejecutara
            // este fragmento de codigo
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (arrayDevices == null)
                    arrayDevices = new ArrayList<BluetoothDevice>();

                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                arrayDevices.add(dispositivo);
                String descripcionDispositivo = dispositivo.getName() + " [" + dispositivo.getAddress() + "]";
                Toast.makeText(getBaseContext(), getString(R.string.DetectadoDispositivo) + ": " + descripcionDispositivo, Toast.LENGTH_SHORT).show();
                Log.v(TAG, "ACTION_FOUND: Dispositivo encontrado: " + descripcionDispositivo);
            }

            // BluetoothAdapter.ACTION_DISCOVERY_FINISHED
            // Codigo que se ejecutara cuando el Bluetooth finalice la busqueda de dispositivos.
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Instanciamos un nuevo adapter para el ListView
                arrayAdapter = new BluetoothDeviceArrayAdapter(getBaseContext(), android.R.layout.simple_list_item_2, arrayDevices);
                lvDispositivos.setAdapter(arrayAdapter);
                Toast.makeText(getBaseContext(), R.string.FinBusqueda, Toast.LENGTH_SHORT).show();
            }

        } // Fin onReceive
    };

    // Handler que obtendrá informacion de BluetoothService
    private final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            byte[] buffer = null;
            String mensaje = null;

            // Atendemos al tipo de mensaje
            switch (msg.what) {
                // Mensaje de lectura: se mostrara en el TextView
                case BluetoothService.MSG_LEER: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer, 0, msg.arg1);
                    tvMensaje.setText(mensaje);
                    break;
                }

                // Mensaje de escritura: se mostrara en el Toast
                case BluetoothService.MSG_ESCRIBIR: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer);
                    mensaje = getString(R.string.EnviandoMensaje) + ": " + mensaje;
                    Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                    break;
                }


                case BluetoothService.MSG_CAMBIO_ESTADO: {
                    switch (msg.arg1) {
                        case BluetoothService.ESTADO_ATENDIENDO_PETICIONES:
                            break;

                        // CONECTADO: Se muestra el dispositivo al que se ha conectado y se activa el boton de enviar
                        case BluetoothService.ESTADO_CONECTADO: {
                            mensaje = getString(R.string.ConexionActual) + " " + servicio.getNombreDispositivo();
                            Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            btnEnviar.setEnabled(true);
                            break;
                        }

                        // REALIZANDO CONEXION: Se muestra el dispositivo al que se esta conectando
                        case BluetoothService.ESTADO_REALIZANDO_CONEXION: {
                            mensaje = getString(R.string.ConectandoA) + " " + ultimoDispositivo.getName() + " [" + ultimoDispositivo.getAddress() + "]";
                            Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                            btnEnviar.setEnabled(false);
                            break;
                        }

                        // NINGUNO: Mensaje por defecto. Desactivacion del boton de enviar
                        case BluetoothService.ESTADO_NINGUNO: {
                            mensaje = getString(R.string.SinConexion);
                            Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                            tvConexion.setText(mensaje);
                            btnEnviar.setEnabled(false);
                            break;
                        }
                        default:
                            break;
                    }
                    break;
                }

                // Mensaje de alerta: se mostrara en el Toast
                case BluetoothService.MSG_ALERTA: {
                    mensaje = msg.getData().getString(ALERTA);
                    Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
                    break;
                }

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Invocamos el metodo de configuracion de nuestros controles
        configurarControles();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void configurarControles() {

        // Instanciamos el array de dispositivos
        arrayDevices = new ArrayList<BluetoothDevice>();

        // Referenciamos los controles y registramos los listeners
        referenciarControles();
        registrarEventosControles();

        // Por defecto, desactivamos los botones que no puedan utilizarse
        btnEnviar.setEnabled(false);
        btnBuscarDispositivo.setEnabled(false);
        btnConectarDispositivo.setEnabled(false);

        // Configuramos el adaptador bluetooth y nos suscribimos a sus eventos
        configurarAdaptadorBluetooth();
        registrarEventosBluetooth();
    }


    private void referenciarControles() {
        // Referenciamos los elementos de interfaz
        btnEnviar = (Button) findViewById(R.id.btnEnviar);
        btnBluetooth = (Button) findViewById(R.id.btnBluetooth);
        btnBuscarDispositivo = (Button) findViewById(R.id.btnBuscarDispositivo);
        btnConectarDispositivo = (Button) findViewById(R.id.btnConectarDispositivo);
        btnSalir = (Button) findViewById(R.id.btnSalir);
        txtMensaje = (EditText) findViewById(R.id.txtMensaje);
        tvMensaje = (TextView) findViewById(R.id.tvMensaje);
        tvConexion = (TextView) findViewById(R.id.tvConexion);
        lvDispositivos = (ListView) findViewById(R.id.lvDispositivos);
    }

    /**
     * Registra los eventos de interfaz (eventos onClick, onItemClick, etc.)
     */
    private void registrarEventosControles() {
        // Asignamos los handlers de los botones
        btnEnviar.setOnClickListener(this);
        btnBluetooth.setOnClickListener(this);
        btnBuscarDispositivo.setOnClickListener(this);
        btnConectarDispositivo.setOnClickListener(this);
        btnSalir.setOnClickListener(this);

        // Configuramos la lista de dispositivos para que cuando seleccionemos
        // uno de sus elementos realice la conexion al dispositivo
        configurarListaDispositivos();
    }

    /**
     * Configura el ListView para que responda a los eventos de pulsacion
     */
    private void configurarListaDispositivos() {
        lvDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapter, View view, int position, long arg) {
                // El ListView tiene un adaptador de tipo BluetoothDeviceArrayAdapter.
                // Invocamos el metodo getItem() del adaptador para recibir el dispositivo
                // bluetooth y realizar la conexion.
                BluetoothDevice dispositivo = (BluetoothDevice) lvDispositivos.getAdapter().getItem(position);

                AlertDialog dialog = crearDialogoConexion(getString(R.string.Conectar),
                        getString(R.string.MsgConfirmarConexion) + " " + dispositivo.getName() + "?",
                        dispositivo.getAddress());

                dialog.show();
            }
        });
    }

    private AlertDialog crearDialogoConexion(String titulo, String mensaje, final String direccion) {
        // Instanciamos un nuevo AlertDialog Builder y le asociamos titulo y mensaje
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(titulo);
        alertDialogBuilder.setMessage(mensaje);

        // Creamos un nuevo OnClickListener para el boton OK que realice la conexion
        DialogInterface.OnClickListener listenerOk = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                conectarDispositivo(direccion);
            }
        };

        // Creamos un nuevo OnClickListener para el boton Cancelar
        DialogInterface.OnClickListener listenerCancelar = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        };

        // Asignamos los botones positivo y negativo a sus respectivos listeners
        alertDialogBuilder.setPositiveButton(R.string.Conectar, listenerOk);
        alertDialogBuilder.setNegativeButton(R.string.Cancelar, listenerCancelar);

        return alertDialogBuilder.create();
    }

    /**
     * Configura el BluetoothAdapter y los botones asociados
     */
    private void configurarAdaptadorBluetooth() {
        // Obtenemos el adaptador Bluetooth. Si es NULL, significara que el
        // dispositivo no posee Bluetooth, por lo que deshabilitamos el boton
        // encargado de activar/desactivar esta caracteristica.
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bAdapter == null) {
            btnBluetooth.setEnabled(false);
            return;
        }

        // Comprobamos si el Bluetooth esta activo y cambiamos el texto de los botones
        // dependiendo del estado. Tambien activamos o desactivamos los botones
        // asociados a la conexion
        if (bAdapter.isEnabled()) {
            btnBluetooth.setText(R.string.DesactivarBluetooth);
            btnBuscarDispositivo.setEnabled(true);
            btnConectarDispositivo.setEnabled(true);
        } else {
            btnBluetooth.setText(R.string.ActivarBluetooth);
        }
    }

    /**
     * Suscribe el BroadcastReceiver a los eventos relacionados con Bluetooth que queremos
     * controlar.
     */
    private void registrarEventosBluetooth() {
        // Registramos el BroadcastReceiver que instanciamos previamente para
        // detectar los distintos eventos que queremos recibir
        IntentFilter filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filtro.addAction(BluetoothDevice.ACTION_FOUND);
        filtro.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        this.registerReceiver(bReceiver, filtro);
    }

    public void conectarDispositivo(String direccion) {
        Toast.makeText(this, "Conectando a " + direccion, Toast.LENGTH_LONG).show();
        if (servicio != null) {
            BluetoothDevice dispositivoRemoto = bAdapter.getRemoteDevice(direccion);
            servicio.solicitarConexion(dispositivoRemoto);
            this.ultimoDispositivo = dispositivoRemoto;
        }
    }

    public void enviarMensaje(String mensaje) {
        if (servicio.getEstado() != BluetoothService.ESTADO_CONECTADO) {
            Toast.makeText(this, R.string.MsgErrorConexion, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mensaje.length() > 0) {
            byte[] buffer = mensaje.getBytes();
            servicio.enviar(buffer);
        }
    }

    /**
     * Handler para manejar los eventos onClick de los botones.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btnEnviar: {
                if (servicio != null) {
                    servicio.enviar(txtMensaje.getText().toString().getBytes());
                    txtMensaje.setText("");
                }
                break;
            }

            // Codigo ejecutado al pulsar el Button que se va a encargar de activar y
            // desactivar el Bluetooth.
            case R.id.btnBluetooth: {
                if (bAdapter.isEnabled()) {
                    if (servicio != null)
                        servicio.finalizarServicio();

                    bAdapter.disable();
                } else {

                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                break;
            }

            case R.id.btnBuscarDispositivo: {
                arrayDevices.clear();

                // Comprobamos si existe un descubrimiento en curso. En caso afirmativo, se
                // cancela.
                if (bAdapter.isDiscovering())
                    bAdapter.cancelDiscovery();

                // Iniciamos la busqueda de dispositivos
                if (bAdapter.startDiscovery())
                    Toast.makeText(this, R.string.IniciandoDescubrimiento, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, R.string.ErrorIniciandoDescubrimiento, Toast.LENGTH_SHORT).show();
                break;
            }

            // Codigo ejecutado al pulsar el Button que se encarga de mostrar todos los dispositivos
            // previamente enlazados al dispositivo actual.
            case R.id.btnConectarDispositivo: {
                Set<BluetoothDevice> dispositivosEnlazados = bAdapter.getBondedDevices();
                // Instanciamos un nuevo adapter para el ListView
                arrayDevices = new ArrayList<BluetoothDevice>(dispositivosEnlazados);
                arrayAdapter = new BluetoothDeviceArrayAdapter(getBaseContext(), android.R.layout.simple_list_item_1, arrayDevices);
                lvDispositivos.setAdapter(arrayAdapter);
                Toast.makeText(getBaseContext(), R.string.FinBusqueda, Toast.LENGTH_SHORT).show();
            }
            case R.id.btnSalir: {
                if (servicio != null)
                    servicio.finalizarServicio();
                finish();
                System.exit(0);
                break;
            }
            default:
                break;
        }
    }

    private UUID generarUUID() {
        ContentResolver appResolver = getApplicationContext().getContentResolver();
        //String id = Secure.getString(appResolver, Secure.ANDROID_ID);


        // Here, thisActivity is the current activity
       /* if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            }
            else
                {
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }*/


        final TelephonyManager tManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

        }
        final String deviceId = String.valueOf(tManager.getDeviceId());
        final String simSerialNumber = String.valueOf(tManager.getSimSerialNumber());
        final String androidId	= android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID uuid = new UUID(androidId.hashCode(), ((long)deviceId.hashCode() << 32) | simSerialNumber.hashCode());
        return uuid;
    }

    /**
     * Handler del evento desencadenado al retornar de una actividad. En este caso, se utiliza
     * para comprobar el valor de retorno al lanzar la actividad que activara el Bluetooth.
     * En caso de que el usuario acepte, resultCode sera RESULT_OK
     * En caso de que el usuario no acepte, resultCode valdra RESULT_CANCELED
     */
    @SuppressLint("LongLogTag")
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch(requestCode)
        {
            case REQUEST_ENABLE_BT:
            {
                Log.v(TAG, "onActivityResult: REQUEST_ENABLE_BT");
                if(resultCode == RESULT_OK)
                {
                    btnBluetooth.setText(R.string.DesactivarBluetooth);
                    if(servicio != null)
                    {
                        servicio.finalizarServicio();
                        servicio.iniciarServicio();
                    }
                    else
                        servicio = new BluetoothService(this, handler, bAdapter);
                }
                break;
            }
            default:
                break;
        }
    }

    // Ademas de realizar la destruccion de la actividad, eliminamos el registro del
    // BroadcastReceiver.
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bReceiver);
        if(servicio != null)
            servicio.finalizarServicio();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(servicio != null)
        {
            if(servicio.getEstado() == BluetoothService.ESTADO_NINGUNO)
            {
                servicio.iniciarServicio();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

}

