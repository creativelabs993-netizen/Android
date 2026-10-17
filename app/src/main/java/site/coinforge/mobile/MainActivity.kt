package site.coinforge.mobile

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 40, 32, 40)
            setBackgroundColor(Color.rgb(12, 18, 32))
        }
        fun label(text: String, size: Float, color: Int, bold: Boolean = false) =
            TextView(this).apply {
                this.text = text; textSize = size; setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 12, 0, 12)
            }
        root.addView(label("COINFORGE", 30f, Color.rgb(90, 220, 180), true))
        root.addView(label("Android mining client • Experimental", 15f, Color.WHITE))
        root.addView(label(
            "A live CoinForge network node and peer network are not deployed yet. Starting a local hash loop would not create spendable coins, so this build will not pretend to mine or award tokens.",
            16f, Color.LTGRAY
        ))
        root.addView(label("STATUS: NETWORK NOT CONFIGURED", 14f, Color.rgb(255, 190, 90), true))
        val button = Button(this).apply {
            text = "CHECK NODE STATUS"
            setOnClickListener {
                status.text = "No verified CoinForge node endpoint is configured. No mining started."
            }
        }
        root.addView(button)
        status = label("No node connected.", 14f, Color.LTGRAY)
        root.addView(status)
        setContentView(root)
    }
    private lateinit var status: TextView
}
