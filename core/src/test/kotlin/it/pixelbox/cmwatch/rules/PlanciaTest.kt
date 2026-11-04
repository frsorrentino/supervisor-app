package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** La plancia del tablet senza sessioni aperte, come `Plancia.svelte` (app-tablet-come-pwa-03-11). */
class PlanciaTest {
    private val order = listOf("you", "work", "out", "tools")

    // Quante colonne da 320 ci stanno, tolto il pannello della master: da 1 a 4.
    @Test fun columnsThatFit() {
        assertEquals(4, Plancia.count(1280f, null))
        assertEquals(2, Plancia.count(1280f, 420f))
        assertEquals(1, Plancia.count(1280f, 720f))
        assertEquals(1, Plancia.count(200f, null))
        assertEquals(4, Plancia.count(2560f, null))
    }

    // Quelle che non stanno scendono in fondo all'ultima che si vede, nell'ordine della home.
    @Test fun overflowGoesUnderTheLast() {
        assertEquals(listOf(listOf("you"), listOf("work"), listOf("out"), listOf("tools")), Plancia.layout(order, 4))
        assertEquals(listOf(listOf("you"), listOf("work", "out", "tools")), Plancia.layout(order, 2))
        assertEquals(listOf(listOf("tools", "you", "work", "out")), Plancia.layout(listOf("tools", "you", "work", "out"), 1))
        assertEquals(listOf(listOf("you"), listOf("work")), Plancia.layout(listOf("you", "work"), 4))
    }
}
