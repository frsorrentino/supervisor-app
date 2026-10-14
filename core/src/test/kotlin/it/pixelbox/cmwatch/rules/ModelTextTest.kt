package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Model
import org.junit.Assert.*
import org.junit.Test

/**
 * Nome breve del modello (Franz, 16/09 08:09: nel riquadro il modello non compariva). Dal vivo il relay manda
 * `label` null e l'id senza suffisso: il nome si ricava dall'id invece di non mostrare niente.
 */
class ModelTextTest {
    @Test fun lEtichettaDelPcVinceQuandoCE() {
        assertEquals("Opus 5", ModelText.short(Model("claude-opus-5[1m]", "Opus 5")))
    }

    @Test fun senzaEtichettaIlNomeVieneDallId() {
        assertEquals("Opus 5", ModelText.short(Model("claude-opus-5", null)))
        assertEquals("Sonnet 5", ModelText.short(Model("claude-sonnet-5", null)))
        assertEquals("Haiku 4.5", ModelText.short(Model("claude-haiku-4-5-20251001", null)))
    }

    @Test fun ilSuffissoDellaFinestraNonEntraNelNome() {
        assertEquals("Opus 5", ModelText.short(Model("claude-opus-5[1m]", null)))
    }

    @Test fun unIdSconosciutoSiMostraComEDopoAverTolto_claude() {
        assertEquals("qualcosa-nuovo", ModelText.short(Model("claude-qualcosa-nuovo", null)))
    }

    @Test fun senzaModelloNienteNome() {
        assertNull(ModelText.short(null))
    }

    /** La finestra la dice il suffisso: serve al contesto quando un giorno lo calcolerà l'orologio, non ora. */
    @Test fun laFinestraSiLeggeDalSuffisso() {
        assertTrue(ModelText.millionWindow(Model("claude-opus-5[1m]", null)))
        assertFalse(ModelText.millionWindow(Model("claude-opus-5", null)))
    }

    // Contratto 1.54: la modalità dei permessi accanto al modello; niente per default o se non si sa.
    @Test fun permissionModeNextToTheModel() {
        val w = ModelText.Modes("piano", "modifiche auto", "senza permessi")
        assertNull(ModelText.permission(null, w)); assertNull(ModelText.permission("default", w))
        assertEquals("piano", ModelText.permission("plan", w))
        assertEquals("modifiche auto", ModelText.permission("acceptEdits", w))
        assertEquals("senza permessi", ModelText.permission("bypassPermissions", w))
        assertEquals("dontAsk", ModelText.permission("dontAsk", w))
        val json = """{"id":"x","name":"x","account":"a","project":"p","state":"idle","since":1,"permission_mode":"plan"}"""
        assertEquals("plan", it.pixelbox.cmwatch.contract.ContractJson.json.decodeFromString(it.pixelbox.cmwatch.contract.Session.serializer(), json).permissionMode)
    }
}
