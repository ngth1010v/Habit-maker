package app.habitmaker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.habitmaker.R
import app.habitmaker.ui.LocalAppContainer

private val CellShape = RoundedCornerShape(10.dp)

/** Every Phosphor icon, searchable by name and tags. [tint] previews the habit's color. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(selected: String, tint: Color, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val icons = LocalAppContainer.current.icons
    var query by remember { mutableStateOf("") }
    val all = icons.all
    val shown = remember(all, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) all else all.filter { q in it.searchText }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(0.85f).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.icon_picker_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.icon_picker_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ph_magnifying_glass), contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            if (shown.isEmpty() && all.isNotEmpty()) {
                EmptyLine(stringResource(R.string.icon_picker_none))
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(52.dp), modifier = Modifier.fillMaxWidth()) {
                items(shown, key = { it.name }) { icon ->
                    val isSelected = icon.name == selected
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .aspectRatio(1f)
                            .background(if (isSelected) tint.light() else Color.Transparent, CellShape)
                            .let { if (isSelected) it.border(1.5.dp, tint, CellShape) else it }
                            .clickable { onSelect(icon.name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        PhIcon(icon.name, tint = tint, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}
