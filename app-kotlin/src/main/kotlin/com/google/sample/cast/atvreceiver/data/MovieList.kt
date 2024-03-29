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
package com.google.sample.cast.atvreceiver.data

import android.util.Log
import org.json.JSONObject
import org.json.JSONException
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.URL
import java.util.ArrayList
import java.util.HashMap

class MovieList {
    protected fun parseUrl(urlString: String?): JSONObject? {
        var inputStream: InputStream? = null
        return try {
            val url = URL(urlString)
            val urlConnection = url.openConnection()
            inputStream = BufferedInputStream(urlConnection.getInputStream())
            val reader = BufferedReader(InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"), 1024)
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            val json = sb.toString()
            JSONObject(json)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse the json for media list", e)
            null
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    // ignore
                }
            }
        }
    }

    companion object {
        private const val TAG = "MovieList"
        private const val TAG_VIDEOS = "videos"
        private const val TAG_HLS = "hls"
        private const val TAG_DASH = "dash"
        private const val TAG_MP4 = "mp4"
        private const val TAG_IMAGES = "images"
        private const val TAG_VIDEO_TYPE = "type"
        private const val TAG_VIDEO_URL = "url"
        private const val TAG_VIDEO_MIME = "mime"
        private const val TAG_CATEGORIES = "categories"
        private const val TAG_NAME = "name"
        private const val TAG_STUDIO = "studio"
        private const val TAG_SOURCES = "sources"
        private const val TAG_SUBTITLE = "subtitle"
        private const val TAG_DURATION = "duration"
        private const val TAG_THUMB = "image-480x270" // "thumb";
        private const val TAG_IMG_780_1200 = "image-780x1200"
        private const val TAG_TITLE = "title"
        private const val TARGET_FORMAT = TAG_HLS
        val MOVIE_CATEGORY = arrayOf(
                "VOD",
                "Playlist",
                "Ads",
                "Live"
        )
        private var list: MutableList<Movie>? = null
        private var count = 0
        fun getList(): List<Movie>? {
            return list
        }

        @Throws(JSONException::class)
        fun setupMovies(url: String?): List<Movie>? {
            if (null != list) {
                return list
            }
            list = ArrayList()
            val urlPrefixMap: MutableMap<String, String> = HashMap()
            val jsonObj = MovieList().parseUrl(url)
            val categories = jsonObj!!.getJSONArray(TAG_CATEGORIES)
            if (null != categories) {
                for (i in 0 until categories.length()) {
                    val category = categories.getJSONObject(i)
                    urlPrefixMap[TAG_HLS] = category.getString(TAG_HLS)
                    urlPrefixMap[TAG_DASH] = category.getString(TAG_DASH)
                    urlPrefixMap[TAG_MP4] = category.getString(TAG_MP4)
                    urlPrefixMap[TAG_IMAGES] = category.getString(TAG_IMAGES)
                    category.getString(TAG_NAME)
                    val videos = category.getJSONArray(TAG_VIDEOS)
                    if (null != videos) {
                        for (j in 0 until videos.length()) {
                            var videoUrl: String? = null
                            var mimeType: String? = null
                            val video = videos.getJSONObject(j)
                            val subTitle = video.getString(TAG_SUBTITLE)
                            val videoSpecs = video.getJSONArray(TAG_SOURCES)
                            if (null == videoSpecs || videoSpecs.length() == 0) {
                                continue
                            }
                            for (k in 0 until videoSpecs.length()) {
                                val videoSpec = videoSpecs.getJSONObject(k)
                                if (TARGET_FORMAT == videoSpec.getString(TAG_VIDEO_TYPE)) {
                                    videoUrl = urlPrefixMap[TARGET_FORMAT].toString() + videoSpec
                                            .getString(TAG_VIDEO_URL)
                                    mimeType = videoSpec.getString(TAG_VIDEO_MIME)
                                }
                            }
                            if (videoUrl == null) {
                                continue
                            }
                            val imageUrl = urlPrefixMap[TAG_IMAGES].toString() + video.getString(TAG_THUMB)
                            val bigImageUrl = urlPrefixMap[TAG_IMAGES].toString() + video
                                    .getString(TAG_IMG_780_1200)
                            val title = video.getString(TAG_TITLE)
                            val studio = video.getString(TAG_STUDIO)
                            val duration = (video.getInt(TAG_DURATION) * 1000).toLong()
                            list?.add(buildMovieInfo(
                                    title, subTitle, studio, videoUrl, imageUrl, bigImageUrl))
                        }
                    }
                }
            }
            return list
        }

        private fun buildMovieInfo(
                title: String,
                description: String,
                studio: String,
                videoUrl: String,
                cardImageUrl: String,
                backgroundImageUrl: String): Movie {
            val movie = Movie()
            movie.id = count++
            movie.title = title
            movie.description = description
            movie.studio = studio
            movie.cardImageUrl = cardImageUrl
            movie.backgroundImageUrl = backgroundImageUrl
            movie.videoUrl = videoUrl
            return movie
        }
    }
}