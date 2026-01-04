package com.woniu0936.verses.sample.tiktok.model

data class VideoItem(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String,
    val coverUrl: String,
    val likes: Int,
    val comments: Int
)
