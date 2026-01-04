package com.woniu0936.verses.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.woniu0936.verses.ext.composeVerticalGrid
import com.woniu0936.verses.ext.composeRow
import com.woniu0936.verses.sample.databinding.*
import com.woniu0936.verses.sample.model.HomeState
import com.woniu0936.verses.sample.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.fabShuffle.setOnClickListener {
            viewModel.shuffleData()
        }
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: HomeState) {
        if (state.isLoading) return
        // 🚀 Using Verses with built-in spacing and automatic lifecycle disposal
        binding.recyclerView.composeVerticalGrid(
            spanCount = 3,
            spacing = 16,        // Global item gap
            contentPadding = 8   // Edge padding
        ) {
            // 1. Search Bar
            item(ItemSearchBarBinding::inflate, fullSpan = true) {
                tvSearchHint.text = state.searchHint
            }

            // 2. Featured Banners
            item(ItemHorizontalListBinding::inflate, data = state.banners, key = "banners_container") {
                rvHorizontal.composeRow(spacing = 12) {
                    items(
                        items = state.banners,
                        inflate = ItemBannerBinding::inflate,
                        key = { it.id }
                    ) { banner ->
                        root.setOnClickListener {
                            Toast.makeText(this@MainActivity, "Clicked Banner: ${banner.title}", Toast.LENGTH_SHORT).show()
                        }
                        tvTitle.text = banner.title
                        tvSubtitle.text = banner.subtitle
                        ivBanner.load(banner.imageUrl)
                    }
                }
            }

            // 3. Categories
            item(ItemHorizontalListBinding::inflate, data = state.categories, key = "categories_container") {
                rvHorizontal.composeRow(spacing = 8) {
                    items(
                        items = state.categories,
                        inflate = ItemCategoryBinding::inflate,
                        key = { it.id }
                    ) { category ->
                        chipCategory.text = category.name
                    }
                }
            }

            // 4. Grid Apps (3 Columns)
            if (state.gridApps.isNotEmpty()) {
                item(ItemSectionHeaderBinding::inflate, fullSpan = true, key = "grid_header_title") {
                    tvSectionTitle.text = "Popular Apps"
                }
                items(
                    items = state.gridApps,
                    inflate = ItemAppGridBinding::inflate,
                    key = { "grid_${it.id}" },
                    span = 1
                ) { app ->
                    root.setOnClickListener {
                        Toast.makeText(this@MainActivity, "Clicked Grid App: ${app.name}", Toast.LENGTH_SHORT).show()
                    }
                    tvAppName.text = app.name
                    tvRating.text = app.rating.toString()
                    ratingBar.rating = app.rating
                    ivIcon.load(app.iconUrl) {
                        crossfade(true)
                        placeholder(android.R.drawable.ic_menu_report_image)
                    }
                }
            }

            // 5. Dynamic Sections
            state.sections.forEach { section ->
                item(ItemSectionHeaderBinding::inflate, data = section.title, fullSpan = true, key = "header_${section.id}") {
                    tvSectionTitle.text = section.title
                }
                item(ItemHorizontalListBinding::inflate, data = section.apps, fullSpan = true, key = "list_${section.id}") {
                    rvHorizontal.composeRow(spacing = 8) {
                        items(
                            items = section.apps,
                            inflate = ItemAppBinding::inflate
                        ) { app ->
                            root.setOnClickListener {
                                Toast.makeText(this@MainActivity, "Clicked App: ${app.name}", Toast.LENGTH_SHORT).show()
                            }
                            tvAppName.text = app.name
                            tvRating.text = app.rating.toString()
                            ratingBar.rating = app.rating
                            ivIcon.load(app.iconUrl) {
                                crossfade(true)
                                placeholder(android.R.drawable.ic_menu_report_image)
                            }
                        }
                    }
                }
            }
        }
    }
}