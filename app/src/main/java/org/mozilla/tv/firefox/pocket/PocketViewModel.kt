/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import org.json.JSONObject
import org.mozilla.tv.firefox.R

const val POCKET_VIDEO_COUNT = 20

/**
 * Provides data that maps 1:1 to Pocket view state.
 *
 * This view state is provided by transforming backing state (provided by the
 * [PocketVideoRepo]), stripping information not important to the view, and adding
 * information required by the view. This should be enough to render (i.e., the
 * view should not have to perform any transformations on this data).
 */
class PocketViewModel(
    private val pocketVideoRepo: PocketVideoRepo,
    pocketRepoCache: PocketRepoCache
) : ViewModel() {

    sealed class State {
        object Error : State()
        data class Feed(val feed: List<FeedItem>) : State()
        object NotDisplayed : State()
    }

    sealed class FeedItem {
        data class Loading(val thumbnailResource: Int) : FeedItem()
        data class Video(
            val id: Int,
            val title: String,
            val url: String,
            val thumbnailURL: String,
            val popularitySortId: Int,
            val authors: String
        ) : FeedItem() {
            companion object {
                fun fromJSONObject(jsonObject: JSONObject) = PocketVideoParser.parse(jsonObject)
            }
        }
    }

    val state: Observable<State> = pocketRepoCache.feedState
        .map { repoState ->
            when (repoState) {
                is PocketVideoRepo.FeedState.Loading -> State.Feed(loadingPlaceholders)
                is PocketVideoRepo.FeedState.LoadComplete -> State.Feed(repoState.videos)
                is PocketVideoRepo.FeedState.NoAPIKey -> State.Feed(noKeyPlaceholders)
                is PocketVideoRepo.FeedState.FetchFailed -> State.Error
                is PocketVideoRepo.FeedState.Inactive -> State.NotDisplayed
            }
        }
        .replay(1)
        .autoConnect(0)

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val loadingPlaceholders: List<FeedItem.Loading> =
            List(POCKET_VIDEO_COUNT) { FeedItem.Loading(R.color.photonGrey50) }

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        val noKeyPlaceholders: List<FeedItem.Video> by lazy {
            List(POCKET_VIDEO_COUNT) {
                FeedItem.Video(
                    id = it,
                    title = "Mozilla",
                    url = "https://www.mozilla.org/en-US/",
                    thumbnailURL = "https://blog.mozilla.org/firefox/files/2017/12/Screen-Shot-2017-12-18-at-2.39.25-PM.png",
                    popularitySortId = it,
                    authors = "0:{'name':'Mozilla'}"
                )
            }
        }
    }

    fun update() = pocketVideoRepo.update()
}
