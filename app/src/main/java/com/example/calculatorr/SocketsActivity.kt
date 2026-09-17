package com.example.calculatorr

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class SocketsActivity : AppCompatActivity() {

    private val logTag = "ZMQ_LOG"

    private lateinit var tvSockets: TextView
    private lateinit var tvServerIp: TextView

    private lateinit var handler: Handler
    private var logBuilder = StringBuilder()

    private val serverIp = "192.168.1.105"   // ← IP твоего компьютера
    private val serverPort = 5555

    private var isRunning = false
    private var clientThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        tvSockets = findViewById(R.id.tvSockets)
        tvServerIp = findViewById(R.id.tvServerIp)

        handler = Handler(Looper.getMainLooper())

        tvServerIp.text = "Сервер: $serverIp:$serverPort"

        findViewById<Button>(R.id.btnStartTransfer).setOnClickListener {
            startTransfer()
        }

        findViewById<Button>(R.id.btnStopTransfer).setOnClickListener {
            stopTransfer()
        }
    }

    private fun startTransfer() {
        if (isRunning) return
        isRunning = true
        logBuilder = StringBuilder()
        appendLog("=== Запуск передачи данных ===")
        appendLog("Сервер: $serverIp:$serverPort")

        clientThread = Thread {
            runClient()
        }
        clientThread?.start()
    }

    private fun stopTransfer() {
        isRunning = false
        appendLog("=== Остановка ===")
    }

    private fun runClient() {
        var context: ZContext? = null
        try {
            context = ZContext()
            val socket = context.createSocket(SocketType.REQ)
            socket.connect("tcp://$serverIp:$serverPort")
            appendLog("[CLIENT] Подключился к $serverIp:$serverPort")

            val totalMessages = 10

            for (i in 1..totalMessages) {
                if (!isRunning) {
                    appendLog("[CLIENT] Остановлено пользователем")
                    break
                }

                val request = "Hello from Android!"
                socket.send(request.toByteArray(ZMQ.CHARSET), 0)
                appendLog("[CLIENT #$i] Отправлено: $request")

                val replyBytes = socket.recv(0)
                if (replyBytes != null) {
                    val reply = String(replyBytes, ZMQ.CHARSET)
                    appendLog("[CLIENT #$i] Получено: $reply")
                }

                Thread.sleep(1000)   // пауза 1 секунда между сообщениями
            }

            socket.close()
            appendLog("[CLIENT] Соединение закрыто")

        } catch (e: Exception) {
            appendLog("[CLIENT] ОШИБКА: ${e.message}")
            Log.e(logTag, "Client error", e)
        } finally {
            context?.close()
        }
    }

    private fun appendLog(message: String) {
        Log.d(logTag, message)
        handler.post {
            logBuilder.append(message).append("\n")
            tvSockets.text = logBuilder.toString()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}