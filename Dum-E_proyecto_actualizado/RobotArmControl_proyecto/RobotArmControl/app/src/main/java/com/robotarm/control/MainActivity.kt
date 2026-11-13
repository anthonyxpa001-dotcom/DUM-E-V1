package com.robotarm.control

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.math.min

class MainActivity : Activity() {
    private val spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var status: TextView? = null
    private var distanceLabel: TextView? = null
    private var radar: RadarView? = null
    private val adapter: BluetoothAdapter? get() = BluetoothAdapter.getDefaultAdapter()
    private val bg = Color.rgb(5, 12, 17)
    private val panel = Color.rgb(13, 27, 34)
    private val green = Color.rgb(57, 255, 145)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        requestBtPermission()
    }

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 42)
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(24))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        val logo = ImageView(this).apply { setImageResource(R.drawable.dume_logo); scaleType = ImageView.ScaleType.FIT_CENTER }
        header.addView(logo, LinearLayout.LayoutParams(dp(76), dp(76)))
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, 0, 0) }
        brand.addView(TextView(this).apply { text = "DUM-E"; textSize = 31f; typeface = Typeface.DEFAULT_BOLD; setTextColor(green) })
        brand.addView(TextView(this).apply { text = "ROBOT ARM CONTROL"; textSize = 12f; letterSpacing = .12f; setTextColor(Color.LTGRAY) })
        header.addView(brand)
        page.addView(header, LinearParams(-1, dp(88)))

        status = TextView(this).apply {
            text = "● DESCONECTADO · HC-05"; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(255, 190, 80)); setBackgroundColor(panel)
        }
        page.addView(status, LinearParams(-1, dp(38)))
        val connect = actionButton("CONECTAR BLUETOOTH", green) { connectToPairedDevice() }
        page.addView(connect, LinearParams(-1, dp(50)))

        val radarCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(8)); setBackgroundColor(panel) }
        radarCard.addView(TextView(this).apply { text = "RADAR ULTRASÓNICO  ·  HC-SR04"; setTextColor(green); textSize = 14f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }, LinearParams(-1, dp(28)))
        radar = RadarView(this)
        radarCard.addView(radar, LinearParams(-1, dp(190)))
        distanceLabel = TextView(this).apply { text = "DISTANCIA: -- cm"; setTextColor(green); textSize = 23f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD }
        radarCard.addView(distanceLabel, LinearParams(-1, dp(38)))
        page.addView(radarCard, LinearParams(-1, dp(276)))

        addControlSection(page, "BASE · GIRO", "◀  IZQUIERDA", "DERECHA  ▶", "BASE_LEFT", "BASE_RIGHT")
        addControlSection(page, "BRAZO · ELEVACIÓN", "▲  SUBIR", "▼  BAJAR", "ARM_UP", "ARM_DOWN")
        addControlSection(page, "PINZA", "ABRIR", "CERRAR", "GRIP_OPEN", "GRIP_CLOSE")
        page.addView(TextView(this).apply { text = "DUM-E  •  CONTROL POR BLUETOOTH"; setTextColor(Color.GRAY); textSize = 10f; gravity = Gravity.CENTER; setPadding(0, dp(14), 0, dp(8)) }, LinearParams(-1, dp(34)))

        setContentView(ScrollView(this).apply { isFillViewport = true; addView(page) })
    }

    private fun addControlSection(page: LinearLayout, title: String, left: String, right: String, cmdLeft: String, cmdRight: String) {
        page.addView(TextView(this).apply { text = title; setTextColor(Color.rgb(105, 235, 170)); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(2), dp(7), 0, dp(3)) }, LinearParams(-1, dp(34)))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(actionButton(left, green) { send(cmdLeft) }, LinearParams(0, dp(58), 1f))
        row.addView(View(this), LinearParams(dp(10), 1))
        row.addView(actionButton(right, green) { send(cmdRight) }, LinearParams(0, dp(58), 1f))
        page.addView(row, LinearParams(-1, dp(64)))
    }

    private fun actionButton(label: String, accent: Int, action: () -> Unit) = TextView(this).apply {
        text = label; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        setTextColor(accent); setBackgroundColor(Color.rgb(25, 48, 53)); isClickable = true; isFocusable = true
        setPadding(dp(6), dp(8), dp(6), dp(8))
        setOnClickListener { action() }
    }

    private fun connectToPairedDevice() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBtPermission(); status?.text = "PERMISO BLUETOOTH PENDIENTE"; return
        }
        val bt = adapter
        if (bt == null) { status?.text = "ESTE CELULAR NO TIENE BLUETOOTH"; return }
        if (!bt.isEnabled) { status?.text = "ACTIVA BLUETOOTH DEL CELULAR"; return }
        val device: BluetoothDevice? = try { bt.bondedDevices.firstOrNull { it.name?.contains("HC-05", true) == true } } catch (_: SecurityException) { null }
        if (device == null) { status?.text = "EMPAREJA HC-05 EN AJUSTES DEL CELULAR"; return }
        status?.text = "CONECTANDO A ${device.name}..."
        thread {
            try {
                bt.cancelDiscovery()
                val s = device.createRfcommSocketToServiceRecord(spp); s.connect(); socket = s
                runOnUiThread { status?.text = "● CONECTADO · ${device.name}" }
                listen(s)
            } catch (_: Exception) {
                runOnUiThread { status?.text = "ERROR DE CONEXIÓN · REVISA HC-05" }
                try { socket?.close() } catch (_: Exception) {}; socket = null
            }
        }
    }

    private fun listen(s: BluetoothSocket) {
        val buffer = ByteArray(128); val pending = StringBuilder()
        try {
            while (true) {
                val n = s.inputStream.read(buffer); if (n <= 0) break
                pending.append(String(buffer, 0, n, Charsets.UTF_8))
                while (pending.contains("\n")) {
                    val idx = pending.indexOf("\n"); val line = pending.substring(0, idx).trim(); pending.delete(0, idx + 1)
                    val cm = line.toFloatOrNull()
                    if (cm != null && cm in 0f..400f) runOnUiThread { distanceLabel?.text = "DISTANCIA: ${cm.toInt()} cm"; radar?.setDistance(cm) }
                }
            }
        } catch (_: IOException) { runOnUiThread { status?.text = "● DESCONECTADO" } }
    }

    private fun send(command: String) {
        val s = socket
        if (s == null || !s.isConnected) { status?.text = "PRIMERO CONECTA EL HC-05"; return }
        thread { try { s.outputStream.write((command + "\n").toByteArray(Charsets.UTF_8)) } catch (_: Exception) { runOnUiThread { status?.text = "ERROR AL ENVIAR" } } }
    }

    override fun onDestroy() { try { socket?.close() } catch (_: Exception) {}; super.onDestroy() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun LinearParams(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

    inner class RadarView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var distance = -1f
        fun setDistance(cm: Float) { distance = cm; invalidate() }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f; val cy = height * .96f; val r = min(width * .43f, height * .92f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1).toFloat(); paint.color = Color.rgb(25, 112, 75)
            for (f in floatArrayOf(.25f, .5f, .75f, 1f)) canvas.drawArc(cx-r*f, cy-r*f, cx+r*f, cy+r*f, 180f, 180f, false, paint)
            canvas.drawLine(cx-r, cy, cx+r, cy, paint); canvas.drawLine(cx, cy, cx, cy-r, paint)
            canvas.drawLine(cx-r*.707f, cy-r*.707f, cx+r*.707f, cy-r*.707f, paint)
            paint.color = green; paint.strokeWidth = dp(2).toFloat(); canvas.drawLine(cx, cy, cx, cy-r, paint)
            if (distance >= 0f) {
                val range = (distance / 200f).coerceIn(.05f, 1f); val px = cx + r * range * .48f; val py = cy - r * range * .87f
                paint.style = Paint.Style.FILL; paint.color = Color.RED; canvas.drawCircle(px, py, dp(6).toFloat(), paint)
                paint.color = Color.argb(65, 255, 0, 0); canvas.drawCircle(px, py, dp(13).toFloat(), paint)
            }
            paint.style = Paint.Style.FILL; paint.color = green; canvas.drawCircle(cx, cy, dp(4).toFloat(), paint)
        }
    }
}
