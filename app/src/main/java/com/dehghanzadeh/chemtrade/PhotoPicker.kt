package com.dehghanzadeh.chemtrade

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory

@Composable
fun PhotoPickerField(currentValue: String, onSelected: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onSelected(it.toString())
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("عکس محصول مشتری")
        Button(
            onClick = { launcher.launch(arrayOf("image/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (currentValue.startsWith("content://")) "تغییر عکس" else "انتخاب عکس از گوشی")
        }
        if (currentValue.startsWith("content://")) {
            val bitmap = remember(currentValue) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(currentValue)).use { input ->
                        input?.let { BitmapFactory.decodeStream(it) }
                    }
                }.getOrNull()
            }
            bitmap?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "عکس محصول",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        } else if (currentValue.isNotBlank()) {
            Text(currentValue)
        }
    }
}

@Composable
fun OfferPhotoPreview(value: String) {
    if (value.startsWith("content://")) {
        val context = LocalContext.current
        val bitmap = remember(value) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(value)).use { input ->
                    input?.let { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
        bitmap?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "عکس محصول",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).padding(6.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    } else if (value.isNotBlank()) {
        Text(value)
    }
}
