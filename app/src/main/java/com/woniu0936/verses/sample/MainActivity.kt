package com.woniu0936.verses.sample

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.woniu0936.verses.ext.compose
import com.woniu0936.verses.sample.databinding.ItemHeaderBinding
import com.woniu0936.verses.sample.databinding.ItemUserBinding

data class User(val id: Int, val name: String)

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        val users = listOf(User(1, "Alice"), User(2, "Bob"), User(3, "Charlie"))
        val tags = listOf("Kotlin", "Android", "Verse", "Declarative")

        rv.compose {
            // 1. Single ViewBinding Item (Header)
            item(ItemHeaderBinding::inflate) {
                tvHeader.text = "User Directory"
            }

            // 2. ViewBinding List (Users)
            items(users, ItemUserBinding::inflate, key = { it.id }) { user ->
                tvName.text = user.name
                tvId.text = "ID: ${user.id}"
            }

            // 3. Custom View Item (Title for Tags)
            item(create = { context -> 
                TextView(context).apply { 
                    textSize = 18f
                    setTextColor(Color.BLUE)
                    setPadding(16, 32, 16, 8)
                }
            }) {
                text = "Trending Tags"
            }

            // 4. Custom View List (Tags)
            items(tags, create = { context -> 
                TextView(context).apply { 
                    textSize = 14f
                    setPadding(32, 8, 32, 8)
                }
            }) { tag ->
                text = "# $tag"
            }
        }
    }
}
