package cz.mtrakal.vmptesty.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Výběr sady. Klepnutí rovnou startuje procvičování — na hodinkách se nevyplácí
 * skládat výběr z checkboxů a voleb délky, chceš začít na dva klepy.
 */
@Composable
fun PickSetScreen(
    counts: Map<WearPick, Int>,
    onPick: (WearPick) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text("VMP Testy")
                }
            }

            items(WearPick.entries.size) { index ->
                val pick = WearPick.entries[index]
                val count = counts[pick] ?: 0
                Button(
                    onClick = { onPick(pick) },
                    enabled = count > 0,
                    label = { Text(pick.label) },
                    secondaryLabel = { Text("$count ${questionWord(count)}") },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding,
                        )
                        .transformedHeight(this, transformationSpec),
                )
            }
        }
    }
}

/** Skloňování "otázka" podle počtu. */
internal fun questionWord(count: Int): String = when {
    count == 1 -> "otázka"
    count in 2..4 -> "otázky"
    else -> "otázek"
}
