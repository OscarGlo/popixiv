package dev.oscarglo.popixiv.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
inline fun <reified T : ViewModel> globalViewModel() =
    viewModel<T>(LocalContext.current as ViewModelStoreOwner)