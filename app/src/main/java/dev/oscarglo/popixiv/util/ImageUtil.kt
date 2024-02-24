package dev.oscarglo.popixiv.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import dev.oscarglo.popixiv.api.PixivApi

@Composable
fun pixivImage(url: String) =
    ImageRequest.Builder(LocalContext.current)
        .data(url)
        .headers(PixivApi.getHeaders())
        .build()