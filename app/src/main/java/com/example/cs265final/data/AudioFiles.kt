package com.example.cs265final.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

internal fun contentDisplayName(resolver: ContentResolver, uri: Uri): String? = runCatching {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()?.takeIf { it.isNotBlank() }
