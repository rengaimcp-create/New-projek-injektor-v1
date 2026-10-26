package com.emeng.proxxy.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Native floating utility panel. Feature switches are UI/local visual toggles only. */
class CrosshairOverlayService : Service() {
    private var wm: WindowManager? = null
    private var panel: View? = null
    private var minimized = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (panel == null) createPanel()
        if (intent?.action == ACTION_LAUNCH_GAME) launchGame()
        return START_NOT_STICKY
    }

    private fun createPanel() {
        val d = resources.displayMetrics.density
        val cyan = Color.rgb(0, 220, 245)
        val purple = Color.rgb(170, 30, 255)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12*d).toInt(), (10*d).toInt(), (12*d).toInt(), (10*d).toInt())
            background = GradientDrawable().apply { setColor(Color.rgb(7, 9, 16)); cornerRadius = 18*d; setStroke((2*d).toInt(), purple) }
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply { text = "⚙  KAKORO INJECTOR"; setTextColor(cyan); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(2, 8, 8, 8) }
        val minimize = Button(this).apply { text = "—"; isAllCaps = false; setOnClickListener { toggleMinimized() } }
        val close = Button(this).apply { text = "×"; isAllCaps = false; setOnClickListener { stopSelf() } }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f)); header.addView(minimize); header.addView(close)
        root.addView(header)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "panel_content" }
        content.addView(label("FLOATING PANEL  •  LOCAL CONTROLS", cyan))
        content.addView(actionButton("ENTER FREE FIRE", purple) { launchGame() })
        content.addView(label("VISUAL MODULES", cyan))
        val features = listOf(
            "SHOW GHOST", "SHOW FREEZE", "SHOW TUNEL", "ESP LINE", "ESP BOX", "ESP SKELETON",
            "ESP ENEMY", "RADAR", "CROSSHAIR", "ESP COLOR", "TEMA COLOR", "ICON (SS00)", "NEW BYPASS"
        )
        val scroll = ScrollView(this).apply { isFillViewport = false; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS }
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        features.forEachIndexed { i, name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding((4*d).toInt(), (2*d).toInt(), (2*d).toInt(), (2*d).toInt())
                background = GradientDrawable().apply { setColor(if (i % 2 == 0) Color.rgb(17, 20, 30) else Color.rgb(10, 13, 22)); cornerRadius = 9*d }
            }
            val text = TextView(this).apply { this.text = name; setTextColor(Color.WHITE); textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setPadding((8*d).toInt(), (8*d).toInt(), 0, (8*d).toInt()) }
            val toggle = Switch(this).apply { text = ""; buttonTintList = android.content.res.ColorStateList.valueOf(cyan); isChecked = false; scaleX = .85f; scaleY = .85f }
            row.addView(text, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(toggle)
            toggle.setOnCheckedChangeListener { _, checked ->
                if (name == "NEW BYPASS" && checked) {
                    Toast.makeText(this, "Ini hanya sakelar UI; tidak melakukan bypass anti-cheat.", Toast.LENGTH_LONG).show()
                } else if (name.startsWith("ESP") && checked) {
                    Toast.makeText(this, "$name adalah kontrol visual demo; tidak membaca data musuh dari game.", Toast.LENGTH_SHORT).show()
                }
            }
            val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = (4*d).toInt(); rows.addView(row, lp)
        }
        scroll.addView(rows)
        content.addView(scroll, LinearLayout.LayoutParams(-1, (310*d).toInt()))
        content.addView(actionButton("MINIMIZE PANEL", Color.rgb(32, 35, 48)) { toggleMinimized() })
        content.addView(label("Drag the title to move • switches are local UI controls", Color.LTGRAY))
        root.addView(content)

        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams((320*d).toInt().coerceAtMost((resources.displayMetrics.widthPixels * .92f).toInt()), WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = (12*d).toInt(); y = (90*d).toInt()
        }
        title.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { startX=lp.x; startY=lp.y; touchX=e.rawX; touchY=e.rawY; return true }
                    MotionEvent.ACTION_MOVE -> { lp.x=startX+(e.rawX-touchX).toInt(); lp.y=startY+(e.rawY-touchY).toInt(); try { wm?.updateViewLayout(root,lp) } catch (_: Exception) {}; return true }
                }; return false
            }
        })
        panel = root
        try { wm?.addView(root, lp) } catch (_: Exception) { panel=null; stopSelf() }
    }

    private fun label(s: String, color: Int) = TextView(this).apply { text=s; setTextColor(color); textSize=10f; setPadding(4,8,4,8) }
    private fun actionButton(textValue: String, color: Int, action: () -> Unit) = Button(this).apply {
        text=textValue; textSize=12f; isAllCaps=false; setTextColor(Color.WHITE)
        background=GradientDrawable().apply { setColor(color); cornerRadius=12f }
        setOnClickListener { action() }
    }
    private fun toggleMinimized() {
        val root = panel as? LinearLayout ?: return
        val content = root.findViewWithTag<View>("panel_content") ?: return
        minimized = !minimized
        content.visibility = if (minimized) View.GONE else View.VISIBLE
    }
    private fun launchGame() {
        val launch = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
        if (launch == null) { Toast.makeText(this, "Free Fire biasa tidak ditemukan (com.dts.freefireth).", Toast.LENGTH_LONG).show(); return }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(launch) } catch (_: Exception) { Toast.makeText(this, "Tidak bisa membuka Free Fire.", Toast.LENGTH_SHORT).show() }
    }
    override fun onDestroy() { try { panel?.let { wm?.removeView(it) } } catch (_: Exception) {}; panel=null; wm=null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_LAUNCH_GAME = "com.emeng.proxxy.action.LAUNCH_GAME" }
}
