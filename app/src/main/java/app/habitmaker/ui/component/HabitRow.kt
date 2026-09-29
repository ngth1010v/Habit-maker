package app.habitmaker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.habitmaker.R

val CardShape = RoundedCornerShape(14.dp)

/**
 * The habit row shared by Home and the Habit list: icon on the left, the name beside it, the note
 * under the name (when there is one), and [trailing] on the right.
 */
@Composable
fun HabitRow(
    icon: String,
    color: Int,
    name: String,
    note: String,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val tint = Color(color)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .heightIn(min = 48.dp)
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleIcon(icon, if (dimmed) tint.copy(alpha = 0.55f) else tint, size = 32.dp)
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dimmed) 0.6f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.isNotBlank()) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

/**
 * The done / undone toggle: a tick for a habit still to do, an x to undo a done one.
 * Disabled on future days.
 */
@Composable
fun DoneToggle(done: Boolean, enabled: Boolean, color: Int, contentDescription: String, onClick: () -> Unit) {
    val tint = Color(color)
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    when {
                        !enabled -> MaterialTheme.colorScheme.surfaceVariant
                        done -> MaterialTheme.colorScheme.surfaceVariant
                        else -> tint
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (done) R.drawable.ph_x_bold else R.drawable.ph_check_bold),
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.outline
                    done -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color.White
                },
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (count != null) {
            Text(
                "  $count",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** A tab's title, centered at the top. */
@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    )
}

@Composable
fun EmptyLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
    )
}
