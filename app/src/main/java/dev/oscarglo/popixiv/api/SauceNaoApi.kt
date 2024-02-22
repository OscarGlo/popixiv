package dev.oscarglo.popixiv.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query

data class SearchResponse(
    val results: List<SearchResult>,
)

data class SearchResult(
    val header: SearchResultHeader,
    val data: SearchResultData
)

data class SearchResultHeader(
    val similarity: Double,
    val thumbnail: String,
    val index_id: Int,
    val index_name: String,
    val dupes: Int,
    val hidden: Int,
)

data class SearchResultData(
    val ext_urls: List<String>?,
    val title: String?,
    val pixiv_id: String?,
    val member_name: String?,
    val member_id: Int?,
    val author_name: String?,
    val author_url: String?,
    val source: String?
)

interface SauceNaoApi {
    companion object {
        val instance = Retrofit.Builder()
            .baseUrl("https://saucenao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SauceNaoApi::class.java)
    }

    @POST("search.php")
    suspend fun search(
        @Query("url") url: String,
        @Query("api_key") api_key: String,
        @Query("output_type") output_type: Int = 2,
    ): SearchResponse
}