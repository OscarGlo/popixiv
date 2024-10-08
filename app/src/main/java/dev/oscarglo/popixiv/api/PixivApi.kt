package dev.oscarglo.popixiv.api

import androidx.compose.ui.text.intl.Locale
import dev.oscarglo.popixiv.util.Prefs
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


data class IllustResponse(
    val illusts: List<Illust>,
    val next_url: String?
)

data class Illust(
    val id: Long,
    val type: String,
    val title: String,
    val caption: String,
    val create_date: String,
    val tags: List<Tag>,
    val width: Int,
    val height: Int,
    val page_count: Int,
    val total_view: Int,
    val total_bookmarks: Int,
    val is_bookmarked: Boolean,
    val visible: Boolean,
    val is_muted: Boolean,
    val url: String?,
    val image_urls: ImageUrls,
    val meta_single_page: MetaSinglePage,
    val meta_pages: List<IllustPage>,
    val user: User
) {
    val pages: List<IllustPage>
        get() = if (meta_single_page.original_image_url != null)
            listOf(
                IllustPage(
                    ImageUrls(
                        image_urls.square_medium,
                        image_urls.medium,
                        image_urls.large,
                        meta_single_page.original_image_url
                    ),
                    "${id}_p0.png"
                )
            )
        else
            meta_pages.onEachIndexed { i, page -> page.filename = "${id}_p$i.png" }

    val r18 get() = tags.any { it.name == "R-18" }
}

data class Tag(
    val name: String,
    val translated_name: String? = null,
    val negative: Boolean = false
) {
    companion object {
        fun parse(s: String) =
            s.trimStart('-').split("–").let { Tag(it[0], it.getOrNull(1), s.startsWith("-")) }
    }

    override fun toString() =
        (if (negative) "-" else "") + (if (translated_name != null) "$name–$translated_name" else name)

    fun match(s: String) = s.split("–")[0] == name
    fun match(t: Tag) = t.name == name
}

data class User(
    val id: Long,
    val name: String,
    val account: String,
    val profile_image_urls: Map<String, String>,
    val is_followed: Boolean?
)

data class ImageUrls(
    val square_medium: String,
    val medium: String,
    val large: String,
    val original: String?
)

data class MetaSinglePage(
    val original_image_url: String?
)

data class IllustPage(
    val image_urls: ImageUrls,
    var filename: String = ""
)

data class UserResponse(
    val user: User
)

data class TagResponse(
    val tags: List<Tag>
)

data class IllustDetailResponse(
    val illust: Illust
)

val userAgent =
    "PixivAndroidApp/7.13.3 (Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"

interface PixivApi {
    companion object {
        fun getHeaders(accessToken: String = Prefs.PIXIV_ACCESS_TOKEN.get()) = Headers.Builder()
            .add("User-Agent", userAgent)
            .add("Accept-Language", "${Locale.current.language}_${Locale.current.region}")
            .add("Referer", "https://app-api.pixiv.net/")
            .add("Authorization", "Bearer $accessToken")
            .build()

        private val client = OkHttpClient.Builder()
            .addInterceptor {
                runBlocking {
                    val res = it.proceed(it.request().newBuilder().headers(getHeaders()).build())

                    if (res.code == 400 && Prefs.PIXIV_REFRESH_TOKEN.exists()) {
                        res.close()

                        val tokens = AuthApi.refreshTokens()

                        // Retry request
                        it.proceed(
                            it.request().newBuilder()
                                .headers(getHeaders(tokens.access_token))
                                .build()
                        )
                    } else {
                        res
                    }
                }
            }
            .build()

        val instance: PixivApi = Retrofit.Builder()
            .baseUrl("https://app-api.pixiv.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(PixivApi::class.java)
    }

    @GET("/v2/illust/follow")
    suspend fun getFollowIllusts(
        @Query("restrict") restrict: String,
        @Query("offset") offset: Int? = 0
    ): IllustResponse

    @GET("/v1/user/bookmarks/illust")
    suspend fun getBookmarkIllusts(
        @Query("restrict") restrict: String,
        @Query("user_id") user_id: Long,
        @Query("max_bookmark_id") max_bookmark_id: Long? = 0
    ): IllustResponse

    @GET("/v1/user/illusts")
    suspend fun getUserIllusts(
        @Query("user_id") user_id: Long,
        @Query("type") type: String,
        @Query("offset") offset: Int? = 0,
    ): IllustResponse

    @GET("/v2/search/autocomplete?merge_plain_keyword_results=true")
    suspend fun getSearchAutocomplete(
        @Query("word") word: String?
    ): TagResponse

    @GET("/v1/search/popular-preview/illust?merge_plain_keyword_results=true")
    suspend fun getSearchIllustPreview(
        @Query("word") word: String,
        @Query("sort") sort: String = "popular_desc", // date_desc / date_asc / popular_desc
        @Query("search_target") search_target: String? = null, // partial_match_for_tags / exact_match_for_tags / title_and_caption
        @Query("bookmark_num") bookmark_num: Int? = null, // 50000, 30000, 20000, 10000, 5000, 1000, 500, 250, 100, 0
        @Query("duration") duration: String? = null // null / within_last_day / within_last_week / within_last_month / within_half_year / within_year
    ): IllustResponse

    @GET("/v1/search/illust?merge_plain_keyword_results=true")
    suspend fun getSearchIllusts(
        @Query("word") word: String,
        @Query("sort") sort: String = "date_desc", // date_desc / date_asc / popular_desc
        @Query("search_target") search_target: String? = null, // partial_match_for_tags / exact_match_for_tags / title_and_caption
        @Query("start_date") start_date: String? = null,
        @Query("end_date") end_date: String? = null,
        @Query("bookmark_num") bookmark_num: Int? = null, // 50000, 30000, 20000, 10000, 5000, 1000, 500, 250, 100, 0
        @Query("offset") offset: Int? = 0,
    ): IllustResponse

    @GET("/v1/user/detail")
    suspend fun getUserDetail(
        @Query("user_id") user_id: Long
    ): UserResponse

    @GET("/v1/illust/detail")
    suspend fun getIllustDetail(
        @Query("illust_id") illust_id: Long
    ): IllustDetailResponse

    @FormUrlEncoded
    @POST("/v2/illust/bookmark/add")
    suspend fun addBookmark(
        @Field("illust_id") illust_id: Long,
        @Field("restrict") restrict: String = "public",
        @Field("tags[]") tagList: List<String>? = null
    ): ResponseBody

    @FormUrlEncoded
    @POST("/v1/illust/bookmark/delete")
    suspend fun deleteBookmark(
        @Field("illust_id") illust_id: Long
    ): ResponseBody

    @FormUrlEncoded
    @POST("/v1/user/follow/add")
    suspend fun addFollow(
        @Field("user_id") user_id: Long,
        @Field("restrict") restrict: String = "public"
    ): ResponseBody

    @FormUrlEncoded
    @POST("/v1/user/follow/delete")
    suspend fun deleteFollow(
        @Field("user_id") user_id: Long
    ): ResponseBody
}

