package com.codepalace.accelerometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.MediaPlayer.create
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.Calendar
import android.os.AsyncTask
import android.os.Build
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.ListView
import pl.droidsonroids.gif.GifImageView
import android.widget.SimpleAdapter
import androidx.annotation.RequiresApi
import java.text.DecimalFormat


class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var botonPopup: ImageButton
    private lateinit var valores: Valores
    private val ventanaTiempo = 300000000L
    private val handler =Handler()
    private var tiempoInicioCondicion: Long = 100
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var countDownTimer2: CountDownTimer

    var contador_estado: Int =0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) //DEJAR LA PANTALLA ENCENDIDA SIEMPRE QUE LA APLICACIÓN ESTÉ EN PRIMER PLANO
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        botonPopup = findViewById(R.id.warning)
        botonPopup.visibility = View.GONE
        valores = Valores()

        setUpSensorStuff()
        ///CREACIÓN DE CONTADOR///
        countDownTimer2 = object:
            CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                llamar()
                registro_caida()
            }}
    }
    ///SET UP DEL SENSOR Y SUS CARACTERÍSTICAS
    private fun setUpSensorStuff() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST
            )}}

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {

        val andargif: GifImageView = findViewById(R.id.andargif) //GIF DE ANCIANA
        display_caidas() //DISPLAY DE CAIDAS

        //BOTÓN DE LLAMADA//
        val callButton:ImageButton = findViewById(R.id.callButton)
        callButton.visibility=View.GONE
        callButton.setOnClickListener {
            callButton.visibility = View.GONE
            parar_alarma()
            registro_caida()
            llamar()
            stopCountdown()
            limpiarVentanaTiempo()
        }

        //BOTÓN POPUP DE ESTOY BIEN//
        botonPopup.setOnClickListener {
            botonPopup.visibility = View.GONE
            stopCountdown()
            limpiarVentanaTiempo()
            parar_alarma()
        }

        //REGISTRO DE ACELERACIONES//
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val X = event.values[0]
            val Y = event.values[1]
            val Z = event.values[2]

            //MODIFICACIÓN DE UMBRAL Y LÓGICA DE CAÍDAS//
            if (valores.getMaximoX().toInt() > 30 || valores.getMaximoY().toInt() > 20 || valores.getMaximoZ().toInt() > 20) {
                botonPopup.visibility = View.VISIBLE
                andargif.visibility = View.INVISIBLE
                callButton.visibility = View.VISIBLE

                if  (contador_estado==1){
                    ////No hace nada
                }else{
                    suena_alarma() //SUENA LA ALARMA SOLO SI TE HAS CAÍDO POR PRIMERA VEZ
                    startCountdown() //se ha caído por primera vez, comienza el estado caída
                }
                contador_estado=1

            } else { //no se ha caído
                contador_estado= 0 //ESTADO NO CAÍDA
                valores.agregarValores(X, Y, Z) //AÑADIR ACELERACIONES A LA LISTA
                andargif.visibility = View.VISIBLE
                botonPopup.visibility= View.INVISIBLE
            }}}

    override fun onAccuracyChanged(p0: Sensor?, accuracy: Int) {
        // Implementar si la precisión del sensor cambia, no es el caso de nuestra aplicación
    }
    override fun onDestroy() {
        //liberación de la memoria de alarma
        sensorManager.unregisterListener(this)
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    private fun limpiarVentanaTiempo() {
        // Limpiar valores en la ventana de tiempo
        valores.limpiarVentanaTiempo(System.currentTimeMillis() - ventanaTiempo)
        val postDelayed = handler.postDelayed({ limpiarVentanaTiempo() }, ventanaTiempo)
    }
    //FUNCIONES DE APLICACIÓN//
    fun suena_alarma(){
        val resourceId = R.raw.alarma
        mediaPlayer = create(this, resourceId)
        mediaPlayer?.start()
    }
    fun parar_alarma(){
        mediaPlayer?.apply {
            if (isPlaying) {  // Comprueba si el MediaPlayer está reproduciendo
                stop()  // Detiene la reproducción
            }
            release()  // Libera los recursos del MediaPlayer
        }
        mediaPlayer = null  // Establece el objeto MediaPlayer como nulo
    }
    //FUNCIONAMIENTO CONTADOR
    fun startCountdown() {
        countDownTimer2.start()
    }
    fun stopCountdown(){
        countDownTimer2.cancel()
    }

    fun llamar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val it=intent
            val username=it.getStringExtra("username")
            val password=it.getStringExtra("password")
            val db = DataBase(applicationContext,"SOSFall",null,5)
            val telf = db.getContact(username = username, password = password) // Tu número de teléfono
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$telf")
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
            Toast.makeText(this, "No se encontró una aplicación para realizar la llamada", Toast.LENGTH_SHORT).show()
        }}

    //REGISTROS DE CAÍDA Y SU DISPLAY//
    @RequiresApi(Build.VERSION_CODES.O)
    fun registro_caida(){
        val it=intent
        val username=it.getStringExtra("username")
        val db = DataBase(applicationContext,"SOSFall",null,5)
        db.registraCaida(username,valores.getMaximoX(),valores.getMaximoY(),valores.getMaximoZ())
    }
    fun display_caidas() {
        val it=intent
        val username=it.getStringExtra("username")
        val db = DataBase(applicationContext,"SOSFall",null,5)

        val fall=db.getFalls("*",username!!)
        val lst: ListView= findViewById(R.id.tablaMainCaidas)
        val list = ArrayList<HashMap<String, String>>()
        var item: HashMap<String, String>
        val sa: SimpleAdapter
        if(fall.size>=5) {
            for (i in 0 until fall.size-1 step 6) {
                item = HashMap<String, String>()

                item["col1"] = fall!![i+1]
                item["col2"] = fall!![i+2]
                item["col3"] = fall!![i+3]
                item["col4"] = fall!![i+4]
                item["col5"] = fall!![i+5]
                list.add(item)
            }
            sa= SimpleAdapter(
                this,
                list,R.layout.multi_line,
                arrayOf("col1", "col2", "col3", "col4", "col5"),
                intArrayOf(R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e)
            )
        }else{
            item = HashMap<String, String>()
            item["col1"] = fall!![0]
            list.add(item)
            sa = SimpleAdapter(
                this,
                list,R.layout.single_line,
                arrayOf("col1"),
                intArrayOf(R.id.line_a)
            )}
        lst.adapter= sa
        val headLst: ListView= findViewById(R.id.titleCaidas)
        val headList = ArrayList<HashMap<String, String>>()

        var header: HashMap<String, String> = HashMap<String, String>()
        header["col1"] = "AccX"
        header["col2"] = "AccY"
        header["col3"] = "AccZ"
        header["col4"] = "Hora"
        header["col5"] = "Fecha"
        headList.add(header)

        val Hsa: SimpleAdapter = SimpleAdapter(
            this,
            headList,R.layout.title_line,
            arrayOf("col1", "col2", "col3", "col4", "col5"),
            intArrayOf(R.id.line_a, R.id.line_b, R.id.line_c, R.id.line_d, R.id.line_e)
        )
        headLst.adapter=Hsa
    }
}
private fun Float.format(digits: Int) = "%.${digits}f".format(this)
