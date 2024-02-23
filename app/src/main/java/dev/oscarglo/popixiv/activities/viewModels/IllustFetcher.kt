package dev.oscarglo.popixiv.activities.viewModels

import android.net.Uri
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.api.PixivApi

sealed class RestrictMeta(val restrict: String, val allAllowed: Boolean)

class FollowMeta(restrict: String, val offset: Int = 0) : RestrictMeta(restrict, true)

class BookmarkMeta(restrict: String, val userId: Long? = null, val maxId: Long? = null) :
    RestrictMeta(restrict, false)

open class IllustFetcher<T>(
    val meta: T,
    val fetchFn: suspend IllustFetcher<T>.() -> IllustFetcher<T>,
    val resetFn: IllustFetcher<T>.() -> IllustFetcher<T>,
    val illusts: List<Illust> = emptyList(),
    val current: Int = 0,
    val done: Boolean = false,
) {
    fun copy(
        meta: T = this.meta,
        fetchFn: suspend IllustFetcher<T>.() -> IllustFetcher<T> = this.fetchFn,
        resetFn: IllustFetcher<T>.() -> IllustFetcher<T> = this.resetFn,
        illusts: List<Illust> = this.illusts,
        current: Int = this.current,
        done: Boolean = this.done
    ) = IllustFetcher(meta, fetchFn, resetFn, illusts, current, done)

    suspend fun fetch() = fetchFn()
    fun reset() = resetFn()

    companion object {
        private fun mergeIllusts(illusts: List<Illust>, newIllusts: List<Illust>): List<Illust> {
            val ids = illusts.map { it.id }
            return illusts + newIllusts.filter { !ids.contains(it.id) }
        }

        fun follow(
            meta: FollowMeta = FollowMeta("all"),
            illusts: List<Illust> = emptyList(),
            current: Int = 0,
        ) = IllustFetcher(
            meta,
            {
                val data = PixivApi.instance.getFollowIllusts(this.meta.restrict, this.meta.offset)

                return@IllustFetcher if (data.next_url != null) {
                    val nextOffset = Uri.parse(data.next_url).getQueryParameter("offset")?.toInt()

                    this.copy(
                        meta = FollowMeta(
                            this.meta.restrict,
                            nextOffset ?: (this.meta.offset + data.illusts.size)
                        ),
                        illusts = mergeIllusts(this.illusts, data.illusts),
                    )
                } else this.copy(illusts = mergeIllusts(this.illusts, data.illusts), done = true)
            },
            {
                this.copy(
                    meta = FollowMeta(this.meta.restrict),
                    illusts = emptyList(),
                    done = false,
                )
            },
            illusts,
            current,
        )

        fun bookmark(
            meta: BookmarkMeta,
            illusts: List<Illust> = emptyList(),
            current: Int = 0,
        ) = IllustFetcher(
            meta,
            {
                if (this.meta.userId == null)
                    return@IllustFetcher this.copy(done = true)

                val data = PixivApi.instance.getBookmarkIllusts(
                    this.meta.restrict,
                    this.meta.userId,
                    this.meta.maxId
                )

                return@IllustFetcher if (data.next_url != null) {
                    val maxId =
                        Uri.parse(data.next_url).getQueryParameter("max_bookmark_id")?.toLong()

                    this.copy(
                        meta = BookmarkMeta(
                            this.meta.restrict,
                            this.meta.userId,
                            maxId ?: (data.illusts.last().id - 1)
                        ),
                        illusts = mergeIllusts(this.illusts, data.illusts),
                    )
                } else this.copy(illusts = mergeIllusts(this.illusts, data.illusts), done = true)
            },
            {
                this.copy(
                    meta = BookmarkMeta(this.meta.restrict, this.meta.userId),
                    illusts = emptyList(),
                    done = false,
                )
            },
            illusts,
            current,
        )
    }
}