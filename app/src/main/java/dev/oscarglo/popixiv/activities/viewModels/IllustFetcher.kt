package dev.oscarglo.popixiv.activities.viewModels

import android.net.Uri
import dev.oscarglo.popixiv.activities.components.dialog.SearchFilters
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.api.PixivApi

sealed class RestrictMeta(val restrict: String, val allAllowed: Boolean)

class FollowMeta(restrict: String, val offset: Int = 0) : RestrictMeta(restrict, true)

class BookmarkMeta(restrict: String, val userId: Long? = null, val maxId: Long? = null) :
    RestrictMeta(restrict, false)

class UserMeta(val userId: Long? = null, val offset: Int = 0)

class SearchMeta(
    val filters: SearchFilters = SearchFilters(),
    val offset: Int = 0
)

class IllustMeta(val id: Long)

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

        fun follow(meta: FollowMeta = FollowMeta("all")) = IllustFetcher(
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
            }
        )

        fun bookmark(meta: BookmarkMeta) = IllustFetcher(
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
            }
        )

        fun user(meta: UserMeta) = IllustFetcher(
            meta,
            {
                if (this.meta.userId == null)
                    return@IllustFetcher this.copy(done = true)

                val data =
                    PixivApi.instance.getUserIllusts(this.meta.userId, "illust", this.meta.offset)

                return@IllustFetcher if (data.next_url != null) {
                    val nextOffset = Uri.parse(data.next_url).getQueryParameter("offset")?.toInt()

                    this.copy(
                        meta = UserMeta(
                            this.meta.userId,
                            nextOffset ?: (this.meta.offset + data.illusts.size)
                        ),
                        illusts = mergeIllusts(this.illusts, data.illusts),
                    )
                } else this.copy(illusts = mergeIllusts(this.illusts, data.illusts), done = true)
            },
            {
                this.copy(
                    meta = UserMeta(this.meta.userId),
                    illusts = emptyList(),
                    done = false,
                )
            }
        )

        fun search(meta: SearchMeta) = IllustFetcher(
            meta,
            {
                val tags = this.meta.filters.tags.filter { !it.negative }
                if (tags.isEmpty())
                    return@IllustFetcher this.copy(done = true)

                val query = tags.filter { !it.negative }.joinToString(" ") { it.name }

                val data =
                    if (this.meta.filters.sort == "popular_desc" && tags.size == 1)
                        PixivApi.instance.getSearchIllustPreview(
                            query,
                            this.meta.filters.sort,
                            duration = this.meta.filters.duration
                        )
                    else
                        PixivApi.instance.getSearchIllusts(
                            query,
                            this.meta.filters.sort,
                            offset = this.meta.offset
                        )

                return@IllustFetcher if (data.next_url != null) {
                    val nextOffset = Uri.parse(data.next_url).getQueryParameter("offset")?.toInt()

                    this.copy(
                        meta = SearchMeta(
                            this.meta.filters,
                            nextOffset ?: (this.meta.offset + data.illusts.size)
                        ),
                        illusts = mergeIllusts(this.illusts, data.illusts),
                    )
                } else this.copy(illusts = mergeIllusts(this.illusts, data.illusts), done = true)
            },
            {
                this.copy(
                    meta = SearchMeta(this.meta.filters),
                    illusts = emptyList(),
                    done = false,
                )
            }
        )

        fun illust(meta: IllustMeta) = IllustFetcher(
            meta,
            {
                this.copy(
                    illusts = listOf(PixivApi.instance.getIllustDetail(this.meta.id).illust),
                    done = true
                )
            },
            { this.copy(illusts = emptyList(), done = false) }
        )
    }
}