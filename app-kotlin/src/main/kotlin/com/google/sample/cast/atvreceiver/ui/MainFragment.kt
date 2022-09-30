/**
 * Copyright 2022 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.sample.cast.atvreceiver.ui

import android.app.LoaderManager
import androidx.leanback.app.BrowseFragment
import android.util.DisplayMetrics
import androidx.leanback.app.BackgroundManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.sample.cast.atvreceiver.R
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.google.sample.cast.atvreceiver.data.MovieListLoader
import com.google.sample.cast.atvreceiver.presenter.CardPresenter
import com.google.sample.cast.atvreceiver.data.MovieList
import android.content.Intent
import android.content.Loader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.Log
import androidx.leanback.widget.*
import com.bumptech.glide.request.transition.Transition
import com.google.sample.cast.atvreceiver.data.Movie
import java.util.*

class MainFragment : BrowseFragment(), LoaderManager.LoaderCallbacks<List<Movie?>> {
    private val mHandler = Handler()
    private var mDefaultBackground: Drawable? = null
    private var mMetrics: DisplayMetrics? = null
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null
    private var mBackgroundManager: BackgroundManager? = null
    private var mCategoryRowAdapter: ArrayObjectAdapter? = null
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)
        prepareBackgroundManager()
        setupUIElements()
        mCategoryRowAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = mCategoryRowAdapter
        setupEventListeners()
        var loaderBundle : Bundle =  Bundle()
        loaderManager.initLoader(0, loaderBundle
                , this as LoaderManager.LoaderCallbacks<List<Movie>>)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString())
            mBackgroundTimer!!.cancel()
        }
    }

    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager?.attach(activity.window)
        mDefaultBackground = ContextCompat.getDrawable(activity, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(activity, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(activity, R.color.search_opaque)
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            Toast.makeText(activity, "Implement your own in-app search", Toast.LENGTH_LONG)
                    .show()
        }
        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics!!.widthPixels
        val height = mMetrics!!.heightPixels
        Glide.with(activity)
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(object : SimpleTarget<Drawable?>(width, height) {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
                        mBackgroundManager!!.drawable = resource
                    }
                })
        mBackgroundTimer!!.cancel()
    }

    private fun startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer!!.cancel()
        }
        mBackgroundTimer = Timer()
        mBackgroundTimer!!.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<List<Movie?>> {
        return MovieListLoader(activity!!, getString(R.string.catalog_url)) as Loader<List<Movie?>>
    }

    override fun onLoadFinished(loader: Loader<List<Movie?>>, data: List<Movie?>) {
        val cardPresenter = CardPresenter()
        var i: Int
        i = 0
        while (i < MovieList.MOVIE_CATEGORY.size) {
            if (i != 0) {
                Collections.shuffle(data)
            }
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            for (j in data.indices) {
                listRowAdapter.add(data[j])
            }
            val header = HeaderItem(i.toLong(), MovieList.MOVIE_CATEGORY[i])
            mCategoryRowAdapter!!.add(ListRow(header, listRowAdapter))
            i++
        }
    }

    override fun onLoaderReset(loader: Loader<List<Movie?>>) {
        mCategoryRowAdapter!!.clear()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(itemViewHolder: Presenter.ViewHolder, item: Any,
                                   rowViewHolder: RowPresenter.ViewHolder, row: Row) {
            if (item is Movie) {
                Log.d(TAG, "Item: $item")
                val intent = Intent(activity, PlaybackActivity::class.java)
                intent.putExtra(MainActivity.MOVIE, item)
                startActivity(intent)
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
                itemViewHolder: Presenter.ViewHolder?,
                item: Any?,
                rowViewHolder: RowPresenter.ViewHolder?,
                row: Row?) {
            if (item is Movie) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }
        }
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val BACKGROUND_UPDATE_DELAY = 300
    }
}