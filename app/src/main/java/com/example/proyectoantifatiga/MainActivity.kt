package com.example.proyectoantifatiga

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.START_STICKY
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LifecycleOwner
import com.example.proyectoantifatiga.databinding.ActivityMainBinding
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.security.Provider.Service
import java.util.concurrent.Executors
import kotlin.jvm.java

/*clase de camara

class ServicioCamara : Service() {
    private lateinit var faceLandmarker: FaceLandmarker
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        iniciarNotificacion()
        iniciarCamara()
        return START_STICKY
    }

    private fun iniciarNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "CANAL_CAMARA",
                "Detección de Fatiga",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
        val notificacion = NotificationCompat.Builder(this, "CANAL_CAMARA")
            .setContentTitle("Monitoreo de Fatiga")
            .setContentText("La detección sigue funcionando en segundo plano")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notificacion)
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                try {
                    val bitmap = BitmapUtils.imageProxyToBitmap(imageProxy)
                    val mpImg = BitmapImageBuilder(bitmap).build()
                    faceLandmarker.detectAsync(mpImg, ImageProcessingOptions.builder().build())
                } catch (e: Exception) {
                    Log.e("ServicioCamara", "Error: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onBind(intent: Intent?): IBinder? = null
}


*/


class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var faceLandmarker: FaceLandmarker
    private val executor = Executors.newSingleThreadExecutor()

    private var showFatigueMessageRef: MutableState<Boolean>? = null
    private var mediaPlayer: MediaPlayer? = null
    private var eyeClosedStartTime: Long? = null
    private val fatigueDurationMillis = 3000L
    //private val EYE_CLOSED_THRESHOLD = 0.01f
    private val eyeClosedThreshold = 0.01f

    override fun onCreate(savedInstanceState: Bundle?) {
        //DANIEL
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //permite la app en segundo plano y no se cierre
        val intentServicio = Intent(this, ServicioFatiga::class.java)
        startService(intentServicio)


        //AQUI
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                setupFaceLandmarker()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            setupFaceLandmarker()
        }

        setContent { AppUI() }
    }

    private fun setupFaceLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
           // .setAlgorithm(BaseOptions.Algorithm.PRECISE)//daniel
            .build()

        val options = FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ -> checkFatigue(result) }
            .setErrorListener { e -> Log.e("MediaPipe", "Error: ${e.message}") }
            .build()

        faceLandmarker = FaceLandmarker.createFromOptions(this, options)
    }

    @Composable
    fun AppUI() {
        val showDrivingMode = remember { mutableStateOf(false) }

        //val lifecycleOwner = LocalLifecycleOwner.current
        //val context = LocalContext.current
        val showFatigueMessageState = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            this@MainActivity.showFatigueMessageRef = showFatigueMessageState
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        startCamera(previewView)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showFatigueMessageState.value) {
                Text(
                    text = "\u00a1Fatiga detectada!",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }

    private fun startCamera(previewView: PreviewView) {
        // ... faceProcessor ...

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                try {
                    val bitmap = BitmapUtils.imageProxyToBitmap(imageProxy)
                    val mpImg = BitmapImageBuilder(bitmap).build()
                    faceLandmarker.detectAsync(
                        mpImg,
                        ImageProcessingOptions.builder().build(),
                        System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e("Analyzer", "Error: ${e.message}")
                } finally {
                    imageProxy.close()
                }

            }

            val preview = Preview.Builder().build()
           // preview.setSurfaceProvider(previewView.surfaceProvider)


            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            //DANIEL MODIFICACION
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                // preview, // Opcional: puedes incluirlo o no. Si no se muestra, quizás no sea necesario.
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkFatigue(result: FaceLandmarkerResult) {
        val faces = result.faceLandmarks()
        if (faces.size != 1) {
            eyeClosedStartTime = null
            showFatigueMessageRef?.value = false
            stopAlarm()
            return
        }

        val landmarks = faces.first()
        if (landmarks.size <= 386) {
            eyeClosedStartTime = null
            showFatigueMessageRef?.value = false
            stopAlarm()
            return
        }

        val leftEyeOpen = kotlin.math.abs(landmarks[159].y() - landmarks[145].y())
        val rightEyeOpen = kotlin.math.abs(landmarks[386].y() - landmarks[374].y())
        val eyeAvg = (leftEyeOpen + rightEyeOpen) / 2f

        runOnUiThread {
            if (eyeAvg < eyeClosedThreshold) {
                if (eyeClosedStartTime == null) {
                    eyeClosedStartTime = System.currentTimeMillis()
                }
                val elapsed = System.currentTimeMillis() - eyeClosedStartTime!!
                if (elapsed >= fatigueDurationMillis) {
                    if (showFatigueMessageRef?.value != true) {
                        showFatigueMessageRef?.value = true
                        playAlarm()
                    }
                }
            } else {
                eyeClosedStartTime = null
                showFatigueMessageRef?.value = false
                stopAlarm()
            }
        }
    }
   /* //DANIEL REPOSO
   private fun checkFatigue(result: FaceLandmarkerResult) {
       val faces = result.faceLandmarks()
       if (faces.size != 1) {
           eyeClosedStartTime = null
           showFatigueMessageRef?.value = false
           stopAlarm()
           return
       }

       val landmarks = faces.first()
       if (landmarks.size <= 386) {
           eyeClosedStartTime = null
           showFatigueMessageRef?.value = false
           stopAlarm()
           return
       }

       val leftEyeOpen = kotlin.math.abs(landmarks[159].y() - landmarks[145].y())
       val rightEyeOpen = kotlin.math.abs(landmarks[386].y() - landmarks[374].y())
       val eyeAvg = (leftEyeOpen + rightEyeOpen) / 2f

       runOnUiThread {
           if (eyeAvg < eyeClosedThreshold) {
               if (eyeClosedStartTime == null) {
                   eyeClosedStartTime = System.currentTimeMillis()
               }
               val elapsed = System.currentTimeMillis() - eyeClosedStartTime!!

               if (elapsed >= fatigueDurationMillis) {
                   if (showFatigueMessageRef?.value != true) {
                       showFatigueMessageRef?.value = true
                       playAlarm()

                       // **Inicia la pantalla de reposo**
                       val intentReposo = Intent(this, PantallaReposo::class.java)
                       startActivity(intentReposo)

                       // **Opcional: Apagar la pantalla**
                       window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                       window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
                       //window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                   }
               }
           } else {
               eyeClosedStartTime = null
               showFatigueMessageRef?.value = false
               stopAlarm()
           }
       }
   }

    //ASTA AQUI   */

    private fun playAlarm() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarma)
            mediaPlayer?.isLooping = true
        }
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    private fun stopAlarm() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            mediaPlayer?.seekTo(0)
        }
    }
}