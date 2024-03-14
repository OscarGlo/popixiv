package dev.oscarglo.popixiv.util

import android.os.Environment
import java.io.File

const val APP_DIR = "popixiv"

fun getImagesPath() = "${Environment.DIRECTORY_PICTURES}/$APP_DIR"

fun getImagesDir(): File {
    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    return File(pictures, APP_DIR)
}