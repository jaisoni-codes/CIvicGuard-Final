package com.js11.p_cubspolice

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreen : AppCompatActivity() {

    private var handler : SplashScreenHandler? = null

    private var runnable : Runnable? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.navigationBarColor = resources.getColor(R.color.splashScreenBg)

        vv_logo.setVideoPath("android.resource://" + packageName + "/" + R.raw.logo)
        vv_logo.setZOrderOnTop(true)
        vv_logo.start()

        handler = SplashScreenHandler()

        runnable = Runnable {
            kotlin.run {
                val mainActivityIntent = Intent(this, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
                overridePendingTransition(0, 0)
            }
        }

        handler!!.postDelayed(runnable!!,3000)
    }


    override fun onPause() {
        super.onPause()
        vv_logo.pause()
        handler!!.pause()
    }

    override fun onResume() {
        super.onResume()
        vv_logo.resume()
        handler!!.resume()
    }
}