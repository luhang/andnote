package com.localnotes.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localnotes.app.BuildConfig

sealed interface Screen {
    data object Notes : Screen
    data class Edit(val noteId: Long?) : Screen
}

@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Notes) }

    // 登出后回到列表页
    LaunchedEffect(state.account) {
        if (state.account == null) screen = Screen.Notes
    }

    val account = state.account
    when {
        !state.ready -> SplashScreen()

        account == null -> SignInScreen(
            signingIn = state.signingIn,
            error = state.error,
            showDebugFallback = BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank(),
            onSignIn = viewModel::signIn,
            onContinueLocally = viewModel::continueWithoutGoogle,
            onDismissError = viewModel::dismissError
        )

        else -> when (val current = screen) {
            is Screen.Notes -> NotesScreen(
                state = state,
                account = account,
                viewModel = viewModel,
                onOpenNote = { screen = Screen.Edit(it) },
                onCreateNote = { screen = Screen.Edit(null) },
                onSignOut = viewModel::signOut
            )

            is Screen.Edit -> EditorScreen(
                noteId = current.noteId,
                viewModel = viewModel,
                onBack = { screen = Screen.Notes }
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                Text(
                    text = "正在打开笔记…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
