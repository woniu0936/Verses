package com.woniu0936.verses.sample.playstore

import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.woniu0936.verses.ext.composeRow
import com.woniu0936.verses.ext.composeVerticalGrid
import com.woniu0936.verses.sample.databinding.ActivityPlayStoreBinding
import com.woniu0936.verses.sample.databinding.ItemAppBinding
import com.woniu0936.verses.sample.databinding.ItemAppGridBinding
import com.woniu0936.verses.sample.databinding.ItemBannerBinding
import com.woniu0936.verses.sample.databinding.ItemCategoryBinding
import com.woniu0936.verses.sample.databinding.ItemHorizontalListBinding
import com.woniu0936.verses.sample.databinding.ItemSearchBarBinding
import com.woniu0936.verses.sample.databinding.ItemSectionHeaderBinding
import com.woniu0936.verses.sample.playstore.model.HomeState
import com.woniu0936.verses.sample.playstore.viewmodel.PlayStoreViewModel
import kotlinx.coroutines.launch

class PlayStoreActivity : AppCompatActivity() {
    private val viewModel: PlayStoreViewModel by viewModels()
    private lateinit var binding: ActivityPlayStoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPlayStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.fabShuffle.setOnClickListener {
            viewModel.shuffleData()
            Toast.makeText(this, "Shuffling content...", Toast.LENGTH_SHORT).show()
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
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        if (state.isLoading) return
        binding.recyclerView.composeVerticalGrid(
            spanCount = 3,
            spacing = 16,
            contentPadding = 8
        ) {
            // 1. Search Bar
            item("search_bar", ItemSearchBarBinding::inflate, fullSpan = true) {
                tvSearchHint.text = state.searchHint
            }

            // 2. Featured Banners
            item("banners_container", ItemHorizontalListBinding::inflate, data = state.banners) {
                rvHorizontal.composeRow(spacing = 12) {
                    items(
                        items = state.banners,
                        key = { it.id },
                        inflate = ItemBannerBinding::inflate,
                        onClick = { banner ->
                            Toast.makeText(this@PlayStoreActivity, "Clicked Banner: ${banner.title}", Toast.LENGTH_SHORT).show()
                        }
                    ) { banner ->
                        tvTitle.text = banner.title
                        tvSubtitle.text = banner.subtitle
                        ivBanner.load(banner.imageUrl) {
                            placeholder(android.R.color.darker_gray)
                            error(android.R.color.darker_gray)
                            crossfade(true)
                        }
                    }
                }
            }

            // 3. Categories
            item("categories_container", ItemHorizontalListBinding::inflate, data = state.categories) {
                rvHorizontal.composeRow(spacing = 8) {
                    items(
                        items = state.categories,
                        key = { it.id },
                        inflate = ItemCategoryBinding::inflate
                    ) { category ->
                        chipCategory.text = category.name
                    }
                }
            }

            // 4. Grid Apps (3 Columns)
            if (state.gridApps.isNotEmpty()) {
                item("grid_header_title", ItemSectionHeaderBinding::inflate, fullSpan = true) {
                    tvSectionTitle.text = "Popular Apps"
                }
                items(
                    items = state.gridApps,
                    key = { "grid_${it.id}" },
                    inflate = ItemAppGridBinding::inflate,
                    span = 1,
                    onClick = { app ->
                        Toast.makeText(this@PlayStoreActivity, "Clicked Grid App: ${app.name}", Toast.LENGTH_SHORT).show()
                    }
                ) { app ->
                    tvAppName.text = app.name
                    tvRating.text = app.rating.toString()
                    ratingBar.rating = app.rating
                    ivIcon.load(app.iconUrl) {
                        crossfade(true)
                        placeholder(android.R.color.darker_gray)
                        error(android.R.color.darker_gray)
                    }
                }
            }

            // 5. Dynamic Sections
            state.sections.forEach { section ->
                item("header_${section.id}", ItemSectionHeaderBinding::inflate, data = section.title, fullSpan = true) {
                    tvSectionTitle.text = section.title
                }
                item("list_${section.id}", ItemHorizontalListBinding::inflate, data = section.apps, fullSpan = true) {
                    rvHorizontal.composeRow(spacing = 8) {
                        items(
                            items = section.apps,
                            key = { it.id },
                            inflate = ItemAppBinding::inflate,
                            onClick = { app ->
                                Toast.makeText(this@PlayStoreActivity, "Clicked App: ${app.name}", Toast.LENGTH_SHORT).show()
                            }
                        ) { app ->
                            tvAppName.text = app.name
                            tvRating.text = app.rating.toString()
                            ratingBar.rating = app.rating
                            ivIcon.load(app.iconUrl) {
                                crossfade(true)
                                placeholder(android.R.color.darker_gray)
                                error(android.R.color.darker_gray)
                            }
                        }
                    }
                }
            }
        }
    }
}