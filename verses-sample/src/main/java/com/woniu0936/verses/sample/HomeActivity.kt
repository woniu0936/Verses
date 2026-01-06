package com.woniu0936.verses.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.woniu0936.verses.sample.databinding.ActivityHomeBinding
import com.woniu0936.verses.sample.tiktok.TikTokActivity
import com.woniu0936.verses.sample.playstore.PlayStoreActivity
import com.woniu0936.verses.core.Verses
import com.woniu0936.verses.sample.utils.ShareUtils
import android.widget.Toast

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnPlayStore.setOnClickListener {
            startActivity(Intent(this, PlayStoreActivity::class.java))
        }

        binding.btnTikTok.setOnClickListener {
            startActivity(Intent(this, TikTokActivity::class.java))
        }

        binding.btnShareLog.setOnClickListener {
            ShareUtils.shareLogFile(this)
        }

        binding.btnEmailSupport.setOnClickListener {
            ShareUtils.shareLogViaEmail(this)
        }
    }
}
