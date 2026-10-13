package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.makay.launcher.domain.AppInfo
import com.makay.launcher.domain.AppShortcut
import com.makay.launcher.domain.SearchResult

class SearchEngine(private val context: Context) {
    fun query(
        raw: String,
        apps: List<AppInfo>,
        shortcuts: List<AppShortcut>,
        includeContacts: Boolean,
        allowedActions: Set<String> = emptySet(),
        onAction: (String) -> Unit,
        onShortcut: (AppShortcut) -> Unit,
    ): List<SearchResult> {
        val q = raw.trim()
        if (q.isEmpty()) return emptyList()
        val lower = q.lowercase()

        val appHits = apps
            .filter { it.label.lowercase().contains(lower) || it.packageName.lowercase().contains(lower) }
            .take(12)
            .map {
                SearchResult(
                    title = it.label,
                    subtitle = it.packageName,
                    kind = SearchResult.Kind.App,
                    app = it,
                )
            }

        val shortcutHits = shortcuts
            .filter { it.label.lowercase().contains(lower) || it.packageName.lowercase().contains(lower) }
            .take(10)
            .map { shortcut ->
                val appLabel = apps.firstOrNull { it.packageName == shortcut.packageName }?.label ?: shortcut.packageName
                SearchResult(
                    title = shortcut.label,
                    subtitle = "Kısayol · $appLabel",
                    kind = SearchResult.Kind.Shortcut,
                    action = { onShortcut(shortcut) },
                )
            }
        val actions = builtInActions(lower, q, onAction, allowedActions)
        val contacts = if (includeContacts) contacts(lower) else emptyList()
        val web = listOf(
            SearchResult(
                title = "Web’de ara: $q",
                subtitle = "Tarayıcıda aç",
                kind = SearchResult.Kind.Web,
                action = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
            ),
        )
        return appHits + shortcutHits + actions + contacts + web
    }

    private fun builtInActions(
        lower: String,
        original: String,
        onAction: (String) -> Unit,
        allowedActions: Set<String>,
    ): List<SearchResult> {
        if (allowedActions.isEmpty()) return emptyList()
        val catalog = listOf(
            Triple("ayarlar", "Launcher ayarları", "settings"),
            Triple("settings", "Launcher settings", "settings"),
            Triple("çekmece", "Uygulama çekmecesini aç", "drawer"),
            Triple("drawer", "Open the app drawer", "drawer"),
            Triple("arama", "Aramayı odakla", "search"),
            Triple("search", "Focus search", "search"),
            Triple("kontrol", "Kontrol merkezini aç", "control"),
            Triple("control", "Open the control center", "control"),
            Triple("duvar", "Bing günlük duvar kağıdı", "wallpaper"),
            Triple("wallpaper", "Bing daily wallpaper", "wallpaper"),
            Triple("widget", "Widget ekle", "widget"),
            Triple("yedek", "Yedek al", "backup"),
            Triple("backup", "Export a backup", "backup"),
            Triple("kasa", "Gizli kasa", "vault"),
            Triple("vault", "Hidden vault", "vault"),
        )
        return catalog
            .filter { it.third in allowedActions }
            .filter { it.first.contains(lower) || lower.contains(it.first) || original.isNotBlank() && it.second.lowercase().contains(lower) }
            .take(3)
            .map { (_, title, id) ->
                SearchResult(
                    title = title,
                    subtitle = "Hızlı eylem",
                    kind = SearchResult.Kind.Action,
                    action = { onAction(id) },
                )
            }
    }

    private fun contacts(lower: String): List<SearchResult> {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.LOOKUP_KEY,
                ),
                "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
                arrayOf("%$lower%"),
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC",
            )
            cursor.use { c ->
                if (c == null) return emptyList()
                buildList {
                    var i = 0
                    while (c.moveToNext() && i < 6) {
                        val name = c.getString(0) ?: continue
                        val lookup = c.getString(1) ?: continue
                        add(
                            SearchResult(
                                title = name,
                                subtitle = "Kişi",
                                kind = SearchResult.Kind.Contact,
                                action = {
                                    val uri = Uri.withAppendedPath(
                                        ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                                        lookup,
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }
                                },
                            ),
                        )
                        i++
                    }
                }
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
