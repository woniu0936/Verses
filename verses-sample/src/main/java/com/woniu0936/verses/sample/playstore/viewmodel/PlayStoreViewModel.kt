package com.woniu0936.verses.sample.playstore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woniu0936.verses.sample.playstore.model.AppModel
import com.woniu0936.verses.sample.playstore.model.BannerModel
import com.woniu0936.verses.sample.playstore.model.CategoryModel
import com.woniu0936.verses.sample.playstore.model.HomeState
import com.woniu0936.verses.sample.playstore.model.SectionModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayStoreViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState(isLoading = true))
    val uiState: StateFlow<HomeState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Simulate network delay
            delay(1000)

            val banners = listOf(
                BannerModel("1", "https://picsum.photos/id/10/600/320", "Amazing Landscapes", "Explore the world"),
                BannerModel("2", "https://picsum.photos/id/20/600/320", "Tech Trends", "Stay updated"),
                BannerModel("3", "https://picsum.photos/id/30/600/320", "Cooking Secrets", "Master the kitchen")
            )

            val categories = listOf(
                CategoryModel("1", "Games"),
                CategoryModel("2", "Apps"),
                CategoryModel("3", "Movies"),
                CategoryModel("4", "Books"),
                CategoryModel("5", "Kids"),
                CategoryModel("6", "Top Charts")
            )

            val sections = listOf(
                SectionModel("s1", "Recommended for you", createMockApps("rec")),
                SectionModel("s2", "New & updated", createMockApps("new")),
                SectionModel("s3", "Suggested for you", createMockApps("sug"))
            )

            _uiState.value = HomeState(
                isLoading = false,
                banners = banners,
                categories = categories,
                sections = sections,
                gridApps = createMockApps("grid")
            )
        }
    }

    fun shuffleData() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        _uiState.value = currentState.copy(
            sections = currentState.sections.shuffled().map { it.copy(apps = it.apps.shuffled()) },
            gridApps = currentState.gridApps.shuffled()
        )
    }

    private fun createMockApps(prefix: String): List<AppModel> {
        return (1..10).map { i ->
            val id = "$prefix-$i"
            AppModel(
                id = id,
                name = "App $prefix $i",
                iconUrl = "https://picsum.photos/seed/$id/200/200",
                rating = (30..50).random() / 10f,
                size = "${(10..100).random()} MB",
                developer = "Developer $i"
            )
        }
    }
}