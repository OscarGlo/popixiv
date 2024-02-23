package dev.oscarglo.popixiv.util

import android.os.Environment
import java.io.File

fun getImagesDir(): File {
    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    return File(pictures, "popixiv")
}