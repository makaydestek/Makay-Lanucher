package com.makay.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.makay.launcher.domain.IconPackInfo
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

class IconPackEngine(private val context: Context) {
    private val filterCache = ConcurrentHashMap<String, Map<String, String>>()

    fun installedPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.dlto.atom.launcher.THEME",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
        )
        val packs = linkedMapOf<String, IconPackInfo>()
        actions.forEach { action ->
            pm.queryIntentActivities(Intent(action), 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                packs[pkg] = IconPackInfo(pkg, info.loadLabel(pm).toString())
            }
        }
        return packs.values.toList()
    }

    fun loadIcon(packPackage: String, packageName: String, activityName: String): Drawable? {
        if (packPackage.isBlank()) return null
        return runCatching {
            val filter = filterCache.computeIfAbsent(packPackage) { parseFilter(it) }
            val keys = listOf(
                "ComponentInfo{$packageName/$activityName}",
                "$packageName/$activityName",
                packageName,
            )
            val drawableName = keys.firstNotNullOfOrNull { filter[it] } ?: return null
            val resources = context.packageManager.getResourcesForApplication(packPackage)
            val id = resources.getIdentifier(drawableName, "drawable", packPackage)
            if (id == 0) null else resources.getDrawable(id, null)
        }.getOrNull()
    }

    fun evict(packPackage: String? = null) {
        if (packPackage == null) filterCache.clear() else filterCache.remove(packPackage)
    }

    private fun parseFilter(packPackage: String): Map<String, String> {
        val resources = context.packageManager.getResourcesForApplication(packPackage)
        val xmlId = resources.getIdentifier("appfilter", "xml", packPackage)
        val parser = if (xmlId != 0) {
            resources.getXml(xmlId)
        } else {
            runCatching { resources.assets.open("appfilter.xml") }.getOrNull()
                ?.let { android.util.Xml.newPullParser().apply { setInput(it, "UTF-8") } }
        } ?: return emptyMap()

        val map = mutableMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    map[component] = drawable
                    val inner = component.removePrefix("ComponentInfo{").removeSuffix("}")
                    map[inner] = drawable
                    map[inner.substringBefore("/")] = drawable
                }
            }
            event = parser.next()
        }
        return map
    }
}
