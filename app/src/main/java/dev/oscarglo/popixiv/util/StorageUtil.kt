package dev.oscarglo.popixiv.util

import android.os.Environment
import java.io.File

fun getImagesDir(): File {
    val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
    return File(dcim, "popixiv")
}