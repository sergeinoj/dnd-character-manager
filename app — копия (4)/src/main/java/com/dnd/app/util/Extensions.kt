// Имя файла: app/src/main/java/com/dnd/app/util/Extensions.kt
// --- НАЧАЛО ФАЙЛА ---
package com.dnd.app.util

import java.util.Locale

fun String.stripHtml(): String {
    return this.replace(Regex("<.*?>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
}

fun String.capitalizeFirst(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: app/src/main/java/com/dnd/app/util/Extensions.kt