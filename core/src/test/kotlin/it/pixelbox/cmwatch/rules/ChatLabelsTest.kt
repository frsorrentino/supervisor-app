package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.ChatLabels.ForYou
import it.pixelbox.cmwatch.rules.ChatLabels.NotTested
import it.pixelbox.cmwatch.rules.ChatLabels.Plain
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatLabelsTest {
    private fun s(t: String) = ChatLabels.split(t)

    @Test fun chat_etichette_daTeInTutteLeForme() {
        val body = "1. Installa\n2. Prova"
        for (h in listOf("Da te", "Da te:", "**Da te**", "**Da te:**", "**Da te**:", "__Da te__")) assertEquals(h, listOf(Plain("Prima"), ForYou("Da te", body), Plain("Dopo")), s("Prima\n\n$h\n$body\n\nDopo"))
        assertEquals(listOf(ForYou("For you", "- a")), s("**For you:**\n\n- a"))
    }

    @Test fun chat_etichette_daTeSenzaElencoOAMetaFraseResta() {
        for (t in listOf("Da te\nNiente elenco", "Aspetta da te\n- a", "Da te ci vuole poco\n- a", "da te\n- a", "Da te")) assertEquals(t, listOf(Plain(t)), s(t))
    }

    @Test fun chat_etichette_nonProvato() {
        for (l in listOf("**Non provato:** il gesto", "**Non provato**: il gesto", "**Non provato** il gesto", "Non provato: il gesto", "**Not tested:** x", "Not tested: x", "Non provato")) assertEquals(l, listOf(NotTested(l)), s(l))
        assertEquals(listOf(Plain("A"), NotTested("**Non provato:**\n- uno\n- due"), Plain("B")), s("A\n\n**Non provato:**\n- uno\n- due\n\nB"))
    }

    @Test fun chat_etichette_nonProvatoAMetaFraseResta() {
        for (t in listOf("Non provato il gesto", "Il gesto è Non provato: dopo", "non provato: x", "**Non provato il gesto**")) assertEquals(t, listOf(Plain(t)), s(t))
    }

    @Test fun chat_etichette_senzaEtichetteIdenticoENelCodiceNo() {
        val t = "\n  Testo **vero**\n\n\n- a\n"
        assertEquals(listOf(Plain(t)), s(t))
        val c = "```\nDa te\n- a\nNon provato: x\n```"
        assertEquals(listOf(Plain(c)), s(c))
    }

    private val names = mapOf("app-a-01" to "Logo", "app-a-01-11" to "Slot")

    @Test fun chat_nomi_codiceEsattoDiventaIlNome() {
        val u = ChatNames.TASK_URL
        assertEquals("fatto [Logo](${u}app-a-01) e [Slot](${u}app-a-01-11).", ChatNames.replace("fatto app-a-01 e app-a-01-11.", names))
        assertEquals("[Logo](${u}app-a-01)", ChatNames.replace("`app-a-01`", names))
    }

    @Test fun chat_nomi_codiceSconosciutoOParzialeResta() {
        for (t in listOf("app-a-01-12", "xapp-a-01", "app-a", "app-a-01_b")) assertEquals(t, ChatNames.replace(t, names))
        assertEquals("app-a-01", ChatNames.replace("app-a-01", emptyMap()))
    }

    @Test fun chat_nomi_codiceLinkEBlocchiNonSiToccano() {
        for (t in listOf("```\napp-a-01\n```", "`git log app-a-01`", "[app-a-01](https://x.example/a)", "https://x.example/app-a-01")) assertEquals(t, ChatNames.replace(t, names))
    }

    @Test fun chat_nomi_dalloStatoSoloConNomeBreve() {
        val c = { task: String, short: String? -> it.pixelbox.cmwatch.contract.Commitment(task = task, title = "t", short = short) }
        val st = it.pixelbox.cmwatch.contract.State(v = 1, ts = 0, host = "pc", sessions = emptyList(), quota = emptyMap(), commitments = listOf(c("a", "Logo"), c("b", null), c("d", "d"), c("e", " ")))
        assertEquals(mapOf("a" to "Logo"), ChatNames.names(st))
    }
}
