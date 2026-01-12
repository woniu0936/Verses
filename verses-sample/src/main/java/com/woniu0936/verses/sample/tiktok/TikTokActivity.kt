package com.woniu0936.verses.sample.tiktok

import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.woniu0936.verses.ext.composeColumn
import com.woniu0936.verses.sample.databinding.ActivityTiktokBinding
import com.woniu0936.verses.sample.databinding.ItemTiktokVideoBinding
import com.woniu0936.verses.sample.tiktok.model.VideoItem
import com.woniu0936.verses.sample.tiktok.viewmodel.TikTokViewModel
import kotlinx.coroutines.launch

import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

import java.lang.ref.WeakReference

class TikTokActivity : AppCompatActivity() {

    private val viewModel: TikTokViewModel by viewModels()
    private lateinit var binding: ActivityTiktokBinding
    
    // 1. One Global Player Instance (Optimization)
    private var exoPlayer: ExoPlayer? = null
    
    // 2. The Bridge: Maps Data ID -> Active View (Held weakly to prevent leaks)
    private val viewCache = mutableMapOf<String, WeakReference<ItemTiktokVideoBinding>>()
    
    // 3. Track Active Video for UI updates (Loading)
    private var currentVideoId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTiktokBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializePlayer()
        setupRecyclerView()
        observeState()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        // Configure Cache Data Source with User-Agent
        val simpleCache = VideoCacheManager.getCache(this)
        val userAgent = Util.getUserAgent(this, "VersesSample")
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(cacheDataSourceFactory)

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val binding = viewCache[currentVideoId]?.get() ?: return
                        
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                binding.pbBuffering.isVisible = true
                            }
                            Player.STATE_READY -> {
                                binding.pbBuffering.isVisible = false
                                binding.ivCover.isVisible = false
                            }
                            Player.STATE_ENDED, Player.STATE_IDLE -> {
                                 binding.pbBuffering.isVisible = false
                            }
                        }
                    }
                })
            }
    }

    private fun setupRecyclerView() {
        PagerSnapHelper().attachToRecyclerView(binding.recyclerView)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loading.isVisible = state.isLoading
                    if (!state.isLoading) {
                        render(state.videos)
                    }
                }
            }
        }
    }

    private fun render(videos: List<VideoItem>) {
        // 🚀 Verses DSL: Full Screen Video List
        binding.recyclerView.composeColumn {
            items(
                items = videos,
                key = { it.id },
                inflate = ItemTiktokVideoBinding::inflate,
                onAttach = { video -> playVideo(video) },
                onDetach = { video -> 
                    stopVideo(video)
                    // We DO NOT remove from viewCache here.
                    // When scrolling back, RecyclerView might re-attach a cached view 
                    // without triggering onBind, so we need the old entry to find the PlayerView.
                }
            ) { video ->
                // Register view in cache for player switching
                viewCache[video.id] = WeakReference(this)
                
                // Normal UI Binding
                tvTitle.text = video.title
                tvDescription.text = video.description
                tvLikes.text = formatNumber(video.likes)
                tvComments.text = formatNumber(video.comments)
                ivCover.load(video.coverUrl)
                ivCover.isVisible = true
                pbBuffering.isVisible = false
            }
        }
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> "${"%.1f".format(number / 1000000.0)}m"
            number >= 1000 -> "${"%.1f".format(number / 1000.0)}k"
            else -> number.toString()
        }
    }

    private fun playVideo(video: VideoItem) {
        val player = exoPlayer ?: return
        val currentViewBinding = viewCache[video.id]?.get() ?: return
        
        currentVideoId = video.id
        currentViewBinding.ivCover.isVisible = true
        currentViewBinding.playerView.player = player
        
        val mediaItem = MediaItem.fromUri(Uri.parse(video.videoUrl))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        
        currentViewBinding.pbBuffering.isVisible = true
    }

    private fun stopVideo(video: VideoItem) {
        val currentViewBinding = viewCache[video.id]?.get() ?: return
        currentViewBinding.playerView.player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}