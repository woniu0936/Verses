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
import com.woniu0936.verses.ext.composeGrid
import com.woniu0936.verses.ext.composeLinearRow
import com.woniu0936.verses.sample.databinding.ActivityMainBinding
import com.woniu0936.verses.sample.databinding.ItemAppBinding
import com.woniu0936.verses.sample.databinding.ItemAppGridBinding
import com.woniu0936.verses.sample.databinding.ItemBannerBinding
import com.woniu0936.verses.sample.databinding.ItemCategoryBinding
import com.woniu0936.verses.sample.databinding.ItemHorizontalListBinding
import com.woniu0936.verses.sample.databinding.ItemSearchBarBinding
import com.woniu0936.verses.sample.databinding.ItemSectionHeaderBinding
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

        // Setup ItemAnimator: We enable it to prove the library's 'supportsChangeAnimations = false' 
        // effectively prevents the flash while keeping the list fluid.
        binding.recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

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

        binding.recyclerView.composeGrid(spanCount = 3) {
            // 1. Search Bar (Full Span)
            item(ItemSearchBarBinding::inflate, fullSpan = true) {
                tvSearchHint.text = state.searchHint
            }

            // 2. Featured Banners (Full Span, Nested Horizontal RV)
            item(ItemHorizontalListBinding::inflate, data = state.banners, key = "banners_container") {
                rvHorizontal.composeLinearRow {
                    items(
                        items = state.banners,
                        inflate = ItemBannerBinding::inflate,
                        key = { it.id },
                        onClick = { banner ->
                            Toast.makeText(this@MainActivity, "Clicked Banner: ${banner.title}", Toast.LENGTH_SHORT).show()
                        }
                    ) { banner ->
                        tvTitle.text = banner.title
                        tvSubtitle.text = banner.subtitle
                        ivBanner.load(banner.imageUrl)
                    }
                }
            }

            // 3. Categories (Full Span, Nested Horizontal RV)
            item(ItemHorizontalListBinding::inflate, data = state.categories, key = "categories_container") {
                rvHorizontal.composeLinearRow {
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
                    tvSectionTitle.text = "Popular Apps (Grid 3-Columns)"
                }

                items(
                    items = state.gridApps,
                    inflate = ItemAppGridBinding::inflate,
                    key = { "grid_${it.id}" },
                    span = 1,
                    onClick = { app ->
                        Toast.makeText(this@MainActivity, "Clicked Grid App: ${app.name}", Toast.LENGTH_SHORT).show()
                    }
                ) { app ->
                    tvAppName.text = app.name
                    tvRating.text = app.rating.toString()
                    ratingBar.rating = app.rating
                    ivIcon.load(app.iconUrl) {
                        crossfade(true)
                        placeholder(android.R.drawable.ic_menu_report_image)
                    }
                }
            }

            // 5. Dynamic Sections (Full Span, Nested Horizontal RV)
            state.sections.forEach { section ->
                item(ItemSectionHeaderBinding::inflate, data = section.title, fullSpan = true, key = "header_${section.id}") {
                    tvSectionTitle.text = section.title
                }
                item(ItemHorizontalListBinding::inflate, data = section.apps, fullSpan = true, key = "list_${section.id}") {
                    rvHorizontal.composeLinearRow {
                        items(
                            items = section.apps,
                            inflate = ItemAppBinding::inflate,
                            onClick = { app ->
                                Toast.makeText(this@MainActivity, "Clicked App in Section: ${app.name}", Toast.LENGTH_SHORT).show()
                            }
                        ) { app ->
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