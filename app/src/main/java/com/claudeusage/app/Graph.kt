package com.claudeusage.app

import android.content.Context
import com.claudeusage.app.data.UsageCache
import com.claudeusage.app.data.UsageRepository
import com.claudeusage.app.web.WebSessionStore

/**
 * Tiny manual dependency graph. A full DI framework would be overkill for an app
 * this size; this gives the ViewModel, widget and workers one shared repository.
 */
object Graph {
    @Volatile
    private var repo: UsageRepository? = null

    @Volatile
    private var cache: UsageCache? = null

    @Volatile
    private var session: WebSessionStore? = null

    fun repository(context: Context): UsageRepository =
        repo ?: synchronized(this) {
            repo ?: UsageRepository(context.applicationContext).also { repo = it }
        }

    fun cache(context: Context): UsageCache =
        cache ?: synchronized(this) {
            cache ?: UsageCache(context.applicationContext).also { cache = it }
        }

    fun webSession(context: Context): WebSessionStore =
        session ?: synchronized(this) {
            session ?: WebSessionStore(context.applicationContext).also { session = it }
        }
}
