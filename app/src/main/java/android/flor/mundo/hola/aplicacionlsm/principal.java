package android.flor.mundo.hola.aplicacionlsm;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class principal extends AppCompatActivity {

    private final int DURACION_SPLASH = 5000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.principal);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(principal.this, ListDevices.class);
                startActivity(intent);
                finish();
            }
        }, DURACION_SPLASH);
    }

    public void lanzarMensajeCorto(){
        Toast.makeText(this,"Conexión Bluetooth",Toast.LENGTH_SHORT).show();
    }
}
