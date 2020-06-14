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

        // Tenemos una plantilla llamada splash.xml donde mostraremos la información que queramos (logotipo, etc.)
        setContentView(R.layout.principal);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                // Cuando pasen los 3 segundos, pasamos a la actividad principal de la aplicación
                Intent intent = new Intent(principal.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            ;
        }, DURACION_SPLASH);
    }

    public void lanzarMensajeCorto(){
        Toast.makeText(this,"Conexión Bluetooth",Toast.LENGTH_SHORT).show();
    }
}
