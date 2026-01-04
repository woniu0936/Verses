package com.woniu0936.verses.sample.tiktok.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woniu0936.verses.sample.tiktok.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TikTokState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = true
)

class TikTokViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TikTokState())
    val uiState: StateFlow<TikTokState> = _uiState.asStateFlow()

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            // Mock Data Source (Consistent Video & Cover Pairs)
            val rawVideos = listOf(
                VideoItem(
                    id = "1",
                    title = "Google Chromecast: Blaze",
                    description = "A fiery display of high-definition streaming capabilities.",
                    videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    coverUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
                    likes = 12400,
                    comments = 532
                ),
                VideoItem(
                    id = "2",
                    title = "Google Chromecast: Escape",
                    description = "Escape into a world of endless entertainment and discovery.",
                    videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    coverUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg",
                    likes = 8900,
                    comments = 210
                ),
                VideoItem(
                    id = "3",
                    title = "Google Chromecast: Fun",
                    description = "Bringing the joy of shared experiences to your living room.",
                    videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    coverUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerFun.jpg",
                    likes = 4500,
                    comments = 120
                ),
                VideoItem(
                    id = "4",
                    title = "Google Chromecast: Joyrides",
                    description = "Take a ride through the most vibrant visual stories.",
                    videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    coverUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerJoyrides.jpg",
                    likes = 3200,
                    comments = 88
                )
            )

            // Generate 20 items (Repeat 5 times)
            val generatedList = (1..5).flatMap { iteration ->
                rawVideos.map { original ->
                    original.copy(
                        id = "${original.id}_$iteration",
                        title = "${original.title} #$iteration"
                    )
                }
            }

            _uiState.value = TikTokState(videos = generatedList, isLoading = false)
        }
    }
}
