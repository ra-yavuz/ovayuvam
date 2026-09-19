package tr.ovayuva.ovayuvam.map

import org.json.JSONArray
import org.json.JSONObject

internal object BasemapStyle {
    const val Endpoint = "https://tiles.openfreemap.org/planet"
    const val Attribution = "OpenFreeMap / OpenMapTiles / OpenStreetMap"

    private const val SourceId = "openfreemap"

    fun json(): String {
        val paper = "#F3F4ED"
        val ink = "#596359"
        val water = "#A4CDD0"
        val park = "#CBDEC8"
        val road = "#FFFFFA"
        val layers = JSONArray()

        fun expression(value: String) = JSONArray(value)
        fun paint(vararg pairs: Pair<String, Any>) = JSONObject().apply {
            pairs.forEach { put(it.first, it.second) }
        }
        fun layer(
            id: String,
            type: String,
            sourceLayer: String?,
            paint: JSONObject,
            layout: JSONObject? = null,
            filter: JSONArray? = null,
            minZoom: Int = 0,
        ) {
            layers.put(
                JSONObject()
                    .put("id", id)
                    .put("type", type)
                    .put("paint", paint)
                    .put("minzoom", minZoom)
                    .apply {
                        if (sourceLayer != null) {
                            put("source", SourceId)
                            put("source-layer", sourceLayer)
                        }
                        if (layout != null) put("layout", layout)
                        if (filter != null) put("filter", filter)
                    },
            )
        }

        val round = JSONObject()
            .put("line-cap", "round")
            .put("line-join", "round")

        layer("paper", "background", null, paint("background-color" to paper))
        layer(
            "green-wash",
            "fill",
            "landcover",
            paint("fill-color" to park, "fill-opacity" to 0.65),
            filter = expression("[\"in\",\"class\",\"wood\",\"grass\",\"wetland\"]"),
        )
        layer("parks", "fill", "park", paint("fill-color" to park, "fill-opacity" to 0.8))
        layer(
            "park-pencil",
            "line",
            "park",
            paint("line-color" to ink, "line-opacity" to 0.25, "line-width" to 0.7, "line-dasharray" to expression("[2,3]")),
            round,
            minZoom = 12,
        )
        layer("water-wash", "fill", "water", paint("fill-color" to water))
        layer("water-pencil", "line", "water", paint("line-color" to ink, "line-width" to 0.6, "line-opacity" to 0.35), round)
        layer(
            "streams",
            "line",
            "waterway",
            paint("line-color" to water, "line-width" to expression("[\"interpolate\",[\"linear\"],[\"zoom\"],8,0.5,16,3]")),
            round,
        )
        layer("buildings", "fill", "building", paint("fill-color" to ink, "fill-opacity" to 0.13), minZoom = 14)
        layer("building-pencil", "line", "building", paint("line-color" to ink, "line-opacity" to 0.35, "line-width" to 0.65), round, minZoom = 15)

        val streets = expression("[\"!in\",\"class\",\"rail\",\"transit\",\"path\"]")
        layer(
            "street-ink",
            "line",
            "transportation",
            paint(
                "line-color" to ink,
                "line-width" to expression("[\"interpolate\",[\"exponential\",1.4],[\"zoom\"],5,0.6,12,2.5,16,10,20,42]"),
                "line-opacity" to 0.65,
            ),
            round,
            streets,
        )
        layer(
            "street-paper",
            "line",
            "transportation",
            paint(
                "line-color" to road,
                "line-width" to expression("[\"interpolate\",[\"exponential\",1.4],[\"zoom\"],5,0.2,12,1.2,16,8,20,38]"),
            ),
            round,
            streets,
        )
        layer(
            "paths",
            "line",
            "transportation",
            paint("line-color" to ink, "line-width" to 1.1, "line-dasharray" to expression("[2,2]")),
            round,
            expression("[\"in\",\"class\",\"path\",\"rail\"]"),
            minZoom = 13,
        )
        layer(
            "borders",
            "line",
            "boundary",
            paint("line-color" to ink, "line-width" to 1, "line-opacity" to 0.45, "line-dasharray" to expression("[4,3]")),
            round,
        )

        val labelPaint = paint("text-color" to ink, "text-halo-color" to paper, "text-halo-width" to 1.5)
        val name = expression("[\"coalesce\",[\"get\",\"name\"],[\"get\",\"name:latin\"],[\"get\",\"name:en\"],\"\"]")
        val fonts = expression("[\"Noto Sans Regular\"]")
        layer(
            "street-names",
            "symbol",
            "transportation_name",
            labelPaint,
            paint("symbol-placement" to "line", "text-field" to name, "text-font" to fonts, "text-size" to 12, "text-letter-spacing" to 0),
            minZoom = 13,
        )
        layer(
            "place-names",
            "symbol",
            "place",
            labelPaint,
            paint("text-field" to name, "text-font" to fonts, "text-size" to 15, "text-letter-spacing" to 0, "text-max-width" to 8),
        )

        return JSONObject()
            .put("version", 8)
            .put("name", "ovayuvam ink")
            .put("glyphs", "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf")
            .put(
                "sources",
                JSONObject().put(
                    SourceId,
                    JSONObject()
                        .put("type", "vector")
                        .put("url", Endpoint)
                        .put("attribution", Attribution),
                ),
            )
            .put("layers", layers)
            .toString()
    }
}
