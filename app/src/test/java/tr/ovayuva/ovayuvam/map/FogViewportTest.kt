package tr.ovayuva.ovayuvam.map

import org.junit.Assert.*
import org.junit.Test
import tr.ovayuva.ovayuvam.domain.GeoPosition

class FogViewportTest {
    @Test fun reuseOnlyWhileCachedExtentAndResolutionCoverTheView() {
        val view = FogViewport(MercatorPoint(0.0,0.0),2.0,720,1280)
        val cache = view.rasterViewport()
        assertTrue(cache.covers(view))
        assertTrue(cache.covers(view.copy(center=MercatorPoint(100.0,0.0))))
        assertFalse(cache.covers(view.copy(center=MercatorPoint(2000.0,0.0))))
        assertFalse(cache.covers(view.copy(metersPerPixel=0.2)))
    }
    @Test fun metersRemainAnchoredWhileZoomingAndPanning() {
        val center = MercatorPoint.from(GeoPosition(48.86, 2.33))
        val first = FogViewport(center, 2.0, 720, 1280)
        val next = FogViewport(MercatorPoint(center.x + 100, center.y - 40), 1.0, 720, 1280)
        val point = MercatorPoint(center.x + 20, center.y + 30)
        assertEquals(370.0, first.screenX(point.x), 0.001)
        assertEquals(625.0, first.screenY(point.y), 0.001)
        assertEquals(280.0, next.screenX(point.x), 0.001)
        assertEquals(570.0, next.screenY(point.y), 0.001)
        assertEquals(first.radiusPixels(20.0, point.y) * 2, next.radiusPixels(20.0, point.y), 0.001)
    }

    @Test fun subPixelBrushDoesNotGrowToOnePixelAtWorldZoom() {
        val view = FogViewport(MercatorPoint(0.0,0.0), 40000.0, 720,1280)
        assertEquals(0.0005, view.radiusPixels(20.0,0.0), 0.000001)
    }

    @Test fun dateLineUsesNearestWorldCopy() {
        val east = MercatorPoint.from(GeoPosition(0.0,179.999))
        val west = MercatorPoint.from(GeoPosition(0.0,-179.999))
        val view = FogViewport(east,1.0,720,1280)
        assertEquals(360.0 + 222.639, view.screenX(west.x),0.01)
    }

    @Test fun northSouthGroundRadiiMatchMercatorScale() {
        val equator = MercatorPoint.from(GeoPosition(0.0,0.0))
        val north = MercatorPoint.from(GeoPosition(80.0,0.0))
        val south = MercatorPoint.from(GeoPosition(-80.0,0.0))
        val view = FogViewport(equator,1.0,720,1280)
        assertEquals(20.0,view.radiusPixels(20.0,equator.y),0.001)
        assertEquals(115.175,view.radiusPixels(20.0,north.y),0.01)
        assertEquals(view.radiusPixels(20.0,north.y),view.radiusPixels(20.0,south.y),0.001)
    }

    @Test fun rasterAllocationIsBoundedOnLargeDisplays() {
        val view = FogViewport(MercatorPoint(0.0,0.0),1.0,4000,8000)
        val raster = view.rasterViewport()
        assertTrue(raster.width <= 1536 && raster.height <= 1536)
        assertTrue(raster.width*raster.metersPerPixel >= view.width*view.metersPerPixel)
        assertTrue(raster.height*raster.metersPerPixel >= view.height*view.metersPerPixel)
        assertEquals(view.center,raster.center)
    }
}
