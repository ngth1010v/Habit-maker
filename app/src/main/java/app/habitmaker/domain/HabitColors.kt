package app.habitmaker.domain

/**
 * The fixed 15 × 5 habit color table: 15 hues (columns) × 5 shades (rows, light to dark), taken
 * from the Material palette (300, 500, 600, 700, 900).
 */
object HabitColors {
    const val HUES = 15
    const val SHADES = 5

    private val table: Array<LongArray> = arrayOf(
        // red, pink, purple, deep purple, indigo, blue, light blue, cyan, teal, green, light green, amber, orange, brown, blue grey
        longArrayOf(0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD, 0xFF7986CB, 0xFF64B5F6, 0xFF4FC3F7, 0xFF4DD0E1, 0xFF4DB6AC, 0xFF81C784, 0xFFAED581, 0xFFFFD54F, 0xFFFFB74D, 0xFFA1887F, 0xFF90A4AE),
        longArrayOf(0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFFFC107, 0xFFFF9800, 0xFF795548, 0xFF607D8B),
        longArrayOf(0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF039BE5, 0xFF00ACC1, 0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFFFB300, 0xFFFB8C00, 0xFF6D4C41, 0xFF546E7A),
        longArrayOf(0xFFD32F2F, 0xFFC2185B, 0xFF7B1FA2, 0xFF512DA8, 0xFF303F9F, 0xFF1976D2, 0xFF0288D1, 0xFF0097A7, 0xFF00796B, 0xFF388E3C, 0xFF689F38, 0xFFFFA000, 0xFFF57C00, 0xFF5D4037, 0xFF455A64),
        longArrayOf(0xFFB71C1C, 0xFF880E4F, 0xFF4A148C, 0xFF311B92, 0xFF1A237E, 0xFF0D47A1, 0xFF01579B, 0xFF006064, 0xFF004D40, 0xFF1B5E20, 0xFF33691E, 0xFFFF6F00, 0xFFE65100, 0xFF3E2723, 0xFF263238),
    )

    /** Row-major: shade by shade, each row all 15 hues. */
    val ALL: List<Int> = table.flatMap { row -> row.map { it.toInt() } }

    val DEFAULT: Int = table[2][8].toInt() // teal 600
}
