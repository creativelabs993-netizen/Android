package site.coinforge.mobile

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var engine: CpuPowEngine? = null

    private val refresh = object : Runnable {
        override fun run() {
            render(engine?.snapshot())
            if (engine?.snapshot()?.running == true) mainHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 30, 28, 28)
            setBackgroundColor(Color.rgb(12, 18, 32))
        }
        fun label(text: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 10, 0, 10)
            }

        root.addView(label("COINFORGE", 30f, Color.rgb(90, 220, 180), true))
        root.addView(label("ON-DEVICE CPU WORKBENCH", 14f, Color.WHITE, true))
        root.addView(label(
            "This build tests real SHA-256 work on your phone. It is NOT connected to a CoinForge blockchain and cannot earn or create coins.",
            15f, Color.LTGRAY
        ))
        root.addView(label("MODE: LOCAL BENCHMARK", 14f, Color.rgb(255, 190, 90), true))
        startButton = Button(this).apply {
            text = "START CPU TEST"
            setOnClickListener {
                if (engine?.snapshot()?.running != true) {
                    engine = CpuPowEngine(
                        onUpdate = { snap -> mainHandler.post { render(snap) } },
                        onFinished = { snap -> mainHandler.post {
                            render(snap)
                            startButton.isEnabled = true
                            stopButton.isEnabled = false
                        } }
                    )
                    startButton.isEnabled = false
                    stopButton.isEnabled = true
                    engine?.start()
                    mainHandler.removeCallbacks(refresh)
                    mainHandler.post(refresh)
                }
            }
        }
        root.addView(startButton, matchWidth())
        stopButton = Button(this).apply {
            text = "STOP TEST"
            isEnabled = false
            setOnClickListener {
                engine?.stop()
                text = "STOPPING…"
            }
        }
        root.addView(stopButton, matchWidth())
        status = label("Ready. No network connected.", 14f, Color.LTGRAY)
        root.addView(status)
        stats = label("Hashes: 0\nHashrate: 0 H/s\nLocal solutions: 0\nElapsed: 0 s\nLast local hash: —", 14f, Color.WHITE)
        root.addView(stats)
        root.addView(label(
            "Safety: runs only while this screen is open, yields the CPU regularly, and stops after 5 minutes. Stop immediately if the phone becomes hot. Do not use while charging or leave unattended.",
            13f, Color.LTGRAY
        ))
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = 8 }

    private fun render(s: CpuPowEngine.Snapshot?) {
        if (s == null) return
        status.text = s.message
        stats.text = "Hashes: ${s.hashes}\nHashrate: ${s.hashesPerSecond} H/s\nLocal solutions: ${s.solutions}\nElapsed: ${s.elapsedSeconds} s\nLast local hash: ${s.lastHash}"
        if (!s.running && ::startButton.isInitialized && s.elapsedSeconds > 0L) {
            startButton.isEnabled = true
            stopButton.isEnabled = false
            mainHandler.removeCallbacks(refresh)
        }
    }

    override fun onDestroy() {
        engine?.stop()
        mainHandler.removeCallbacks(refresh)
        super.onDestroy()
    }
}
