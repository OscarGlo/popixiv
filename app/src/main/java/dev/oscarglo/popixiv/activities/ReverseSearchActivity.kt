package dev.oscarglo.popixiv.activities

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import dev.oscarglo.popixiv.api.SauceNaoApi
import dev.oscarglo.popixiv.api.SearchResponse
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.Prefs

class ReverseSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Prefs.init(this)

        val url = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM).toString()

        setContent {
            AppTheme {
                ReverseSearchView(url)
            }
        }
    }
}

@Composable
fun ReverseSearchView(url: String) {
    val context = LocalContext.current

    var res: SearchResponse? by rememberSaveable { mutableStateOf(null) }

    LaunchedEffect("loadResult") {
        if (res != null)
            return@LaunchedEffect

        if (!Prefs.SAUCENAO_TOKEN.exists()) {
            Toast.makeText(context, "No SauceNAO api key set", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        res = SauceNaoApi.instance.search(url, api_key = Prefs.SAUCENAO_TOKEN.get())
    }

    Surface {
        Column {
            AsyncImage(
                url,
                contentDescription = "User provided image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )

            res?.results?.map {
                Row {
                    AsyncImage(
                        it.header.thumbnail,
                        contentDescription = "result thumbnail",
                    )
                    
                    //ClickableText(text = , onClick = )
                }
            }
        }
    }
}
