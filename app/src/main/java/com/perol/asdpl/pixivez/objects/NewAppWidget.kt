/*
 * MIT License
 *
 * Copyright (c) 2019 Perol_Notsfsssf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 */

package com.perol.asdpl.pixivez.objects

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.networks.RestClient
import com.perol.asdpl.pixivez.services.PixivApiService
import com.perol.asdpl.pixivez.ui.pic.PictureActivity
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Random

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [NewAppWidgetConfigureActivity]
 */
class NewAppWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val pixivApiService =
                RestClient.retrofitAppApi.create(PixivApiService::class.java)
            MainScope().launch {
                pixivApiService.walkthroughIllusts().let { resp ->
                    // Construct the RemoteViews object
                    val rand = Random()
                    val randomnum = rand.nextInt(resp.illusts.size - 1)
                    val views = RemoteViews(context.packageName, R.layout.new_app_widget)
                    val bundle = Bundle()
                    bundle.putInt("illustid", resp.illusts[randomnum].id)
                    val illustIdList = IntArray(1) { resp.illusts[randomnum].id }
                    bundle.putIntArray("illustidlist", illustIdList)
                    val intent = Intent(context, PictureActivity::class.java)
                    intent.putExtras(bundle)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)
                    Glide.with(context.applicationContext).asBitmap()
                        .load(resp.illusts[randomnum].meta[0].medium)
                        .transform(
                            RoundedCornersTransformation(
                                24,
                                0,
                                RoundedCornersTransformation.CornerType.ALL
                            )
                        ).into(object : AppWidgetTarget(
                            context.applicationContext,
                            R.id.widget_image,
                            views,
                            appWidgetId
                        ) {})
//                            appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
