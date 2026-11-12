package com.lyldz.ab.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherApiTest {
    @Test fun parsesMaxTemp() {
        assertEquals(31.4, WeatherApi.parseMaxTemp("""{"daily":{"time":["2026-07-01"],"temperature_2m_max":[31.4]}}""")!!, 0.0)
    }

    @Test fun badJsonOrMissingTempIsNull() {
        assertNull(WeatherApi.parseMaxTemp("not json"))
        assertNull(WeatherApi.parseMaxTemp("""{"daily":{"temperature_2m_max":[]}}"""))
        assertNull(WeatherApi.parseMaxTemp("""{"daily":{"temperature_2m_max":[null]}}"""))
        assertNull(WeatherApi.parseMaxTemp("""{}"""))
    }

    @Test fun parsesCitiesAndRoundsCoordinates() {
        val json = """{"results":[{"name":"Ankara","admin1":"Ankara","country":"Türkiye","latitude":39.9199,"longitude":32.8543},
            {"name":"Bad"},{"name":"İzmir","admin1":"İzmir","country":"Türkiye","latitude":38.4189,"longitude":27.1287}]}"""
        val c = WeatherApi.parseCities(json)
        assertEquals(2, c.size)
        assertEquals(39.9, c[0].lat, 0.0)
        assertEquals(32.9, c[0].lon, 0.0)
        assertEquals("Ankara, Türkiye", c[0].label)
    }

    @Test fun noResultsIsEmpty() {
        assertEquals(0, WeatherApi.parseCities("""{"generationtime_ms":0.5}""").size)
    }
}
