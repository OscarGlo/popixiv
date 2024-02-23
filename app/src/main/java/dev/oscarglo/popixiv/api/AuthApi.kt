package dev.oscarglo.popixiv.api

import dev.oscarglo.popixiv.util.Prefs
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"

data class OAuthResponse(
    val access_token: String,
    val refresh_token: String,
    val user: User
)

interface AuthApi {
    companion object {
        var instance: AuthApi = Retrofit.Builder()
            .baseUrl("https://oauth.secure.pixiv.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)

        suspend fun refreshTokens() =
            instance.postRefreshToken(Prefs.PIXIV_REFRESH_TOKEN.get()).apply {
                Prefs.PIXIV_REFRESH_TOKEN.set(refresh_token)
                Prefs.PIXIV_ACCESS_TOKEN.set(access_token)
            }
    }

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun postAuthToken(
        @Field("code") code: String,
        @Field("code_verifier") code_verifier: String,
        @Field("client_id") client_id: String = CLIENT_ID,
        @Field("client_secret") client_secret: String = CLIENT_SECRET,
        @Field("grant_type") grant_type: String = "authorization_code",
        @Field("redirect_uri") redirect_uri: String = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback",
        @Field("include_policy") include_policy: Boolean = true
    ): OAuthResponse

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun postRefreshToken(
        @Field("refresh_token") refresh_token: String,
        @Field("client_id") id: String = CLIENT_ID,
        @Field("client_secret") secret: String = CLIENT_SECRET,
        @Field("grant_type") grant_type: String = "refresh_token",
        @Field("include_policy") get_secure_url: Boolean = true
    ): OAuthResponse
}