package dev.oscarglo.popixiv.activities.viewModels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

typealias FetcherMap = Map<String, IllustFetcher<*>>

class FetcherViewModel : ViewModel() {
    val fetcherMaps = MutableStateFlow<List<FetcherMap>>(
        listOf(
            mapOf(
                "follow" to IllustFetcher.follow(),
                "bookmark" to IllustFetcher.bookmark(BookmarkMeta("public")),
                "user" to IllustFetcher.user(UserMeta()),
                "search" to IllustFetcher.search(SearchMeta("", offset = 0))
            )
        )
    )

    @Composable
    fun get(key: String): IllustFetcher<*> {
        val mapIndex = rememberSaveable {
            if (fetcherMaps.value.isEmpty())
                throw IllegalStateException("No fetchers on stack")

            fetcherMaps.value.size - 1
        }

        val fetchers by fetcherMaps.collectAsState()

        var fetcher by remember {
            if (key !in fetchers[mapIndex])
                throw IllegalStateException("No fetcher with key '$key'")

            mutableStateOf(fetchers[mapIndex][key] as IllustFetcher<*>)
        }

        LaunchedEffect(fetchers) {
            if (fetchers.size > mapIndex && key in fetchers[mapIndex]) {
                fetcher = fetchers[mapIndex][key] as IllustFetcher<*>
            }
        }

        return fetcher
    }

    fun push(fetcherMap: FetcherMap) {
        fetcherMaps.value = fetcherMaps.value + fetcherMap
    }

    fun pop() {
        fetcherMaps.value = fetcherMaps.value.dropLast(1)
    }

    fun updateLast(key: String, operation: IllustFetcher<*>.() -> IllustFetcher<*>) {
        val map = fetcherMaps.value.last()
        val updated = map.map { (k, v) -> if (k == key) k to v.operation() else k to v }.toMap()
        fetcherMaps.value = fetcherMaps.value.dropLast(1) + updated
    }

    suspend fun updateLastSuspend(
        key: String,
        operation: suspend IllustFetcher<*>.() -> IllustFetcher<*>
    ) {
        val map = fetcherMaps.value.last()
        val updated = map.map { (k, v) -> if (k == key) k to v.operation() else k to v }.toMap()
        fetcherMaps.value = fetcherMaps.value.dropLast(1) + updated
    }

    fun reset(key: String) = updateLast(key, IllustFetcher<*>::reset)
    suspend fun fetch(key: String) = updateLastSuspend(key, IllustFetcher<*>::fetch)
}