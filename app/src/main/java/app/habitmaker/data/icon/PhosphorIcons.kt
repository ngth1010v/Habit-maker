package app.habitmaker.data.icon

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class PhosphorIcon(val name: String, val category: String, val tags: String, val path: String) {
    /** Lowercase text the picker's search matches against. */
    val searchText: String = "$name $category $tags".lowercase()
}

/**
 * The whole Phosphor "fill" icon set (MIT, phosphoricons.com), bundled as one text asset generated
 * by `tools/gen_phosphor.mjs`. Icons are stored by name in the database and turned into
 * [ImageVector]s on demand, cached, so a row never re-parses a path. The asset is read once on a
 * background thread ([load]); until then [vector] returns null and callers draw a placeholder.
 */
class PhosphorIcons(private val context: Context) {
    /** Snapshot state, so every icon composable redraws once the asset has loaded. */
    var all: List<PhosphorIcon> by mutableStateOf(emptyList())
        private set
    private var byName: Map<String, PhosphorIcon> = emptyMap()
    private val vectors = ConcurrentHashMap<String, ImageVector>()

    suspend fun load() {
        if (all.isNotEmpty()) return
        val icons = withContext(Dispatchers.IO) {
            context.assets.open(ASSET).bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val parts = line.split('|', limit = 4)
                    if (parts.size == 4) PhosphorIcon(parts[0], parts[1], parts[2], parts[3]) else null
                }.toList()
            }
        }
        byName = icons.associateBy { it.name }
        all = icons
    }

    /** Builds (and caches) the vectors for [names] off the main thread, so the first frame showing them is cheap. */
    suspend fun warm(names: Collection<String>) = withContext(Dispatchers.Default) {
        load()
        names.forEach { vector(it) }
    }

    fun vector(name: String): ImageVector? {
        vectors[name]?.let { return it }
        val icon = byName[name] ?: return null
        val nodes = PathParser().parsePathString(icon.path).toNodes()
        val vector = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 256f, viewportHeight = 256f)
            .addPath(pathData = nodes, fill = SolidColor(Color.Black))
            .build()
        vectors[name] = vector
        return vector
    }

    companion object {
        private const val ASSET = "phosphor_fill.txt"
        const val DEFAULT_HABIT = "check-circle"
        const val DEFAULT_REWARD = "gift"
    }
}
