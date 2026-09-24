package tr.ovayuva.ovayuvam.map

/** Shared by the cached history mask and the single live location brush. */
object RevealBrush {
    private val offsets = floatArrayOf(-0.32f,-0.08f, 0.28f,-0.18f, -0.18f,0.28f, 0.34f,0.18f, 0.04f,-0.36f)
    fun circles(x: Int, y: Int, samples: Int, road: Boolean, radius: Float,
                draw: (dx: Float, dy: Float, radius: Float, alpha: Float) -> Unit) {
        draw(0f,0f,radius * if (road) 1.05f else 1.18f,if (road) 0.18f else 0.3f)
        for (i in 0..4) {
            val mixed = ((x + i * 11) * 73856093) xor ((y - i * 7) * 19349663)
            val noise = (mixed and 0xFFFF) / 65535f
            draw(offsets[i*2]*radius,offsets[i*2+1]*radius,
                radius * if (road) 0.36f else 0.58f + noise * 0.28f,
                if (road) 0.14f else 0.34f)
        }
        val strength = if (road) (0.48f + samples.coerceAtMost(6)*0.035f).coerceAtMost(0.72f)
            else (0.7f + samples.coerceAtMost(8)*0.035f).coerceAtMost(0.95f)
        draw(0f,0f,radius * if (road) 0.82f else 0.86f,strength)
        draw(0f,0f,radius * if (road) 0.28f else 0.56f,1f)
    }
}
