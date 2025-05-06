package com.example.ros_stringsub

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var wsClient: WebSocketClient
    private lateinit var textViewMensaje: TextView
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)

        textViewMensaje = findViewById(R.id.textViewMensaje)

        // Inicializar TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val installIntent = android.content.Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                }
            } else {
                println("Error al inicializar TTS")
            }
        }

        val serverUri = URI("ws://192.168.247.47:9090")

        wsClient = object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("Conectado a rosbridge")

                val subscribeMsg = """
                    {
                        "op": "subscribe",
                        "topic": "/msg_ROS",
                        "type": "std_msgs/String"
                    }
                """.trimIndent()

                wsClient.send(subscribeMsg)
            }

            override fun onMessage(message: String?) {
                println("Mensaje recibido: $message")

                val regex = """"data"\s*:\s*"([^"]*)"""".toRegex()
                val match = regex.find(message ?: "")
                val texto = match?.groups?.get(1)?.value

                texto?.let {
                    runOnUiThread {
                        textViewMensaje.text = it
                        textToSpeech.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("Conexión cerrada: $reason")
            }

            override fun onError(ex: Exception?) {
                println("Error: ${ex?.message}")
            }
        }
        wsClient.connect()
    }

    override fun onDestroy() {
        wsClient.close()
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
