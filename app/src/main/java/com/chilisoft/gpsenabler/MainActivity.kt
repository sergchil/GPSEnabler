package com.chilisoft.gpsenabler

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var gpsUtils: GpsUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gpsUtils = GpsUtils(this)

        gpsUtils.onProgressUpdate = { show ->
            println("need to show progress $show")
        }

        button.setOnClickListener {
            gpsUtils.getLatLong { lat, long ->
                println("location is $lat + $long")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        gpsUtils.onActivityResult(requestCode, resultCode, data)
    }

}
