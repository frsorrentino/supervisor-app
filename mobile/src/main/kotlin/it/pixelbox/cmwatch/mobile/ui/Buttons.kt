package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp

/*
 * I tasti di Material con la mano del mouse (Franz, 05/10 12:38: «molti tasti non cambiano il cursore in mano»): stessi nomi
 * e stessi parametri, nello stesso package delle schermate, così ogni tasto la prende senza toccare le chiamate.
 */

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, colors: IconButtonColors = IconButtonDefaults.iconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun FilledIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = IconButtonDefaults.filledShape, colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    androidx.compose.material3.FilledIconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun FilledTonalIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = IconButtonDefaults.filledShape, colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    androidx.compose.material3.FilledTonalIconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.textShape, colors: ButtonColors = ButtonDefaults.textButtonColors(), contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.buttonColors(), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.Button(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.filledTonalShape, colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.outlinedShape, colors: ButtonColors = ButtonDefaults.outlinedButtonColors(), border: androidx.compose.foundation.BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, border = border, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true, colors: MenuItemColors = MenuDefaults.itemColors(), contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding, interactionSource: MutableInteractionSource? = null) =
    androidx.compose.material3.DropdownMenuItem(text = text, onClick = onClick, modifier = modifier.handCursor(), leadingIcon = leadingIcon, trailingIcon = trailingIcon, enabled = enabled, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource)

/** La superficie toccabile di Material con la mano (il ▶, la pillola di modello ed effort, i file della chat). */
@Composable
fun Surface(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = androidx.compose.ui.graphics.RectangleShape,
    color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.contentColorFor(color),
    tonalElevation: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(0f), shadowElevation: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(0f),
    border: androidx.compose.foundation.BorderStroke? = null, interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit,
) = androidx.compose.material3.Surface(
    onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, color = color, contentColor = contentColor,
    tonalElevation = tonalElevation, shadowElevation = shadowElevation, border = border, interactionSource = interactionSource, content = content,
)

/** La superficie di Material senza tocco, invariata: serve accanto all'altra perché i file del package non importano quella di Material. */
@Composable
fun Surface(
    modifier: Modifier = Modifier, shape: Shape = androidx.compose.ui.graphics.RectangleShape,
    color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.contentColorFor(color),
    tonalElevation: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(0f), shadowElevation: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(0f),
    border: androidx.compose.foundation.BorderStroke? = null, content: @Composable () -> Unit,
) = androidx.compose.material3.Surface(modifier = modifier, shape = shape, color = color, contentColor = contentColor, tonalElevation = tonalElevation, shadowElevation = shadowElevation, border = border, content = content)

/**
 * Il foglio dal basso sul telefono; su tablet e desktop una scheda al centro, larga al massimo 560 dp (Franz, 05/10 13:10:
 * «i menu che escono dal basso devono essere proporzionati, moderni e in una posizione naturale»).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit, modifier: Modifier = Modifier,
    sheetState: androidx.compose.material3.SheetState = androidx.compose.material3.rememberModalBottomSheetState(),
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.BottomSheetDefaults.ContainerColor,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    // Stile «Studio e Diretta» (app-menu-v3-30-10): un fondo solo per tutti i fogli, qualunque colore chieda chi li apre.
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    if (cfg.screenWidthDp < 600) {
        V3Theme { androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState, containerColor = V3.S1, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp), content = content) }
        return
    }
    androidx.compose.ui.window.Dialog(onDismissRequest, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(
            shape = V3.Shape, color = V3.S1, shadowElevation = 12.dp,
            modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(0.92f).heightIn(max = (cfg.screenHeightDp * 0.86f).dp),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.padding(top = 16.dp, bottom = 12.dp), content = content)
        }
    }
}

/** Il dialogo dell'app nello stile «Studio e Diretta» (app-menu-v3-30-10): stessa firma di quello del Material, un fondo solo. */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, modifier: Modifier = Modifier, dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null, text: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") containerColor: androidx.compose.ui.graphics.Color = V3.S1,
) = V3Theme {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest, confirmButton = confirmButton, modifier = modifier, dismissButton = dismissButton, icon = icon, title = title, text = text,
        shape = V3.Shape, containerColor = V3.S1, tonalElevation = 0.dp, titleContentColor = V3.Tx, textContentColor = V3.Tx2,
    )
}
