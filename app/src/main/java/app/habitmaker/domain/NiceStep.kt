package app.habitmaker.domain

private val NiceBases = intArrayOf(1, 2, 5)

/**
 * The smallest step on the 1/2/5 x 10^k ladder (1, 2, 5, 10, 20, 50, ...) that is at least [min].
 * Chart axes label every such step, so `niceStep(span / maxLabels)` keeps the labels to maxLabels.
 */
fun niceStep(min: Float): Int {
    var scale = 1
    var index = 0
    var step = 1
    // Int overflows past ~2.1e9; no day count reaches it, but stop rather than wrap.
    while (step < min && scale < Int.MAX_VALUE / 100) {
        index++
        if (index == NiceBases.size) {
            index = 0
            scale *= 10
        }
        step = NiceBases[index] * scale
    }
    return step
}
