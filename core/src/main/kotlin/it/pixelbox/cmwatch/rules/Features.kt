package it.pixelbox.cmwatch.rules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * La panoramica delle funzioni (Franz 01/11 20:39): un elenco solo, `mobile/src/main/assets/features.json`, letto da telefono e web.
 * Ogni voce «c'è» ha la prova nel codice: file e testo, che i test `panoramica` ricontrollano. `version` è il primo commit con quella prova,
 * non la nascita della funzione: serve solo qui, sulla pagina non si mostra.
 */
object Features {
    const val READY = "ready"

    /** Le voci col riquadro «Prova», il componente vero coi dati della demo (Franz 01/11 22:13). */
    val DEMO_KEYS = setOf("tabellone", "ti_aspettano", "slot", "esiti", "lavori", "suggestions", "master", "system", "notte_report", "web_app")
    const val COMING = "coming"

    @Serializable data class Text(val it: String, val en: String) { fun of(italian: Boolean) = if (italian) it else en }
    @Serializable data class Proof(val file: String, val needle: String)
    @Serializable data class Group(val key: String, val it: String, val en: String)
    @Serializable data class Feature(
        val key: String, val group: String, val name: Text, val what: Text, val where: Text, val state: String, val `when`: Text, val how: Text,
        val version: String? = null, val route: String? = null, val section: String? = null, val phone: Proof? = null, val web: Proof? = null,
    )
    @Serializable data class Catalog(val groups: List<Group>, val features: List<Feature>)

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(text: String): Catalog = json.decodeFromString(text)

    /** I gruppi con le loro voci, in ordine; i vuoti non ci sono. */
    fun grouped(c: Catalog): List<Pair<Group, List<Feature>>> = c.groups.mapNotNull { g -> c.features.filter { it.group == g.key }.takeIf { it.isNotEmpty() }?.let { g to it } }

    /** Le sezioni della home senza una voce: devono essere zero. */
    fun missingSections(c: Catalog, sections: List<String>): List<String> = sections.filter { s -> c.features.none { it.section == s } }

    /** «Portami lì» solo se la funzione c'è e ha un posto suo: la home non conta, dice meno di quello che si vedeva già (Franz 01/11 22:05). */
    fun canGo(f: Feature): Boolean = f.state == READY && f.route != null && f.route != "home"
}
