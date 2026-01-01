package com.woniu0936.verses.sample.model

data class AppModel(
    val id: String,
    val name: String,
    val iconUrl: String,
    val rating: Float,
    val size: String,
    val developer: String
)

data class BannerModel(
    val id: String,
    val imageUrl: String,
    val title: String,
    val subtitle: String
)

data class CategoryModel(
    val id: String,
    val name: String
)

data class SectionModel(
    val id: String,
    val title: String,
    val apps: List<AppModel>
)

data class HomeState(
    val isLoading: Boolean = false,
    val banners: List<BannerModel> = emptyList(),
    val categories: List<CategoryModel> = emptyList(),
    val sections: List<SectionModel> = emptyList(),
    val gridApps: List<AppModel> = emptyList(),
    val searchHint: String = "Search for apps & games"
)
