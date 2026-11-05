package it.pixelbox.cmwatch.rules

/**
 * Feedback dal menù (app-feedback-menu-04-11, scelta A di Franz): «Suggerimento» o «Segnala un bug», un testo, i dati tecnici
 * aggiunti da soli e, se la si accende, la foto della pagina. L'invio passa dal relay alle osservazioni di supervisor
 * (supervisor-feedback-app-04-11): prima la bozza anonima, poi «Invia dal mio GitHub», «Invia anonimo» o «Non ora». Finché il
 * relay non ha l'op `feedback` la bozza si prepara qui e l'invio resta spento. Le stesse regole di web/src/lib/feedback.ts.
 */
object Feedback {
    enum class Kind { IDEA, BUG }
    data class Tech(val version: String, val commit: String, val device: String, val screen: String, val lang: String)
    /** `text`: quello che la persona ha scritto, reso anonimo (al relay va questo, i dati tecnici a parte). */
    data class Draft(val kind: Kind, val title: String, val body: String, val photo: Boolean, val text: String = "")
    data class Labels(val idea: String, val bug: String, val tech: String, val photo: String)

    private val EMAIL = Regex("""[\w.+-]+@[\w-]+(\.[\w-]+)+""")
    private val HOME = Regex("""([A-Za-z]:\\Users\\|/home/|/Users/)[^\\/\s]+""")
    private val KEY = Regex("""\b(gh[pousr]_[A-Za-z0-9]{20,}|sk-[A-Za-z0-9_-]{20,}|AIza[A-Za-z0-9_-]{30,})\b""")
    private val HEX = Regex("""\b[A-Fa-f0-9]{32,}\b""")
    private val IP = Regex("""\b(\d{1,3}\.){3}\d{1,3}\b""")

    /** Il testo reso anonimo: indirizzi email, cartelle personali, chiavi e token lunghi, indirizzi IP. */
    fun anonymize(s: String): String = s
        .replace(EMAIL, "<email>")
        .replace(HOME) { it.groupValues[1] + "<utente>" }
        .replace(KEY, "<chiave>")
        .replace(HEX, "<chiave>")
        .replace(IP, "<ip>")

    /** Il titolo: la prima riga del testo, al massimo 72 caratteri tagliati a una parola intera. */
    fun titleOf(text: String): String {
        val first = text.trim().lineSequence().first().trim()
        if (first.length <= 72) return first
        val cut = first.take(72)
        val sp = cut.lastIndexOf(' ')
        return if (sp > 40) cut.take(sp) else cut
    }

    /** La bozza anonima come andrà alle osservazioni: titolo, testo, dati tecnici e, se c'è, la foto allegata. */
    fun draft(kind: Kind, text: String, tech: Tech, photo: Boolean, l: Labels): Draft {
        val clean = anonymize(text.trim())
        val lines = mutableListOf(clean, "", "### ${l.tech}", "- app: ${tech.version} (${tech.commit})", "- ${tech.device}", "- ${tech.screen}", "- ${tech.lang}")
        if (photo) lines += listOf("", "_${l.photo}_")
        return Draft(kind, "[${if (kind == Kind.IDEA) l.idea else l.bug}] ${titleOf(clean)}", lines.joinToString("\n"), photo, clean)
    }

    /** Contratto 1.121, `feedback_app`: il tipo come lo vuole il relay. */
    fun relayKind(k: Kind): String = if (k == Kind.BUG) "bug" else "suggerimento"

    /** I dati tecnici per il relay: chiavi [a-z_], al massimo 10, valori tagliati a 80. */
    fun relayTech(t: Tech): Map<String, String> = listOf("app_version" to t.version, "commit" to t.commit, "device" to t.device, "screen" to t.screen, "lang" to t.lang)
        .filter { it.second.isNotBlank() }.take(10).associate { (k, v) -> k to v.take(80) }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** L'hash della bozza del primo passo, o null se il testo non è una bozza. */
    fun draftHash(text: String?): String? = runCatching {
        (json.parseToJsonElement(text ?: "") as kotlinx.serialization.json.JsonObject)["hash"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content }
    }.getOrNull()

    /**
     * La bozza del PC dopo il primo passo (contratto 1.123): con la schermata `notice` è la riga da mostrare prima dei tasti;
     * `githubOk` true solo se il gh del PC legge il repo privato dei feedback (campo assente = no).
     */
    data class Prepared(val hash: String, val notice: String?, val githubOk: Boolean)

    fun preparedOf(text: String?): Prepared? = runCatching {
        val o = json.parseToJsonElement(text ?: "") as kotlinx.serialization.json.JsonObject
        fun prim(k: String) = o[k] as? kotlinx.serialization.json.JsonPrimitive
        val hash = prim("hash")?.takeIf { it.isString }?.content ?: return@runCatching null
        val shot = prim("screenshot")?.content == "true"
        Prepared(hash, prim("screenshot_notice")?.takeIf { shot && it.isString }?.content, prim("github_ok")?.content == "true")
    }.getOrNull()

    /** L'esito del secondo passo; `screenshotUrl` il link dell'immagine per github (contratto 1.123). */
    data class Choice(val choice: String, val url: String?, val text: String, val screenshotUrl: String? = null)

    /** Il primo passo dal foglio: la riga sulla schermata e se c'è GitHub, o l'errore da dire. */
    data class Ready(val notice: String?, val github: Boolean, val error: String?)

    /** L'invio dal foglio: il link di GitHub e il testo del PC (che dice anche se la schermata non è partita), o l'errore. */
    data class Sent(val url: String?, val error: String?, val text: String? = null)

    /** I due passi del Feedback, dati da chi ha il relay. */
    class Link(val prepare: suspend (Draft, Tech, ByteArray?) -> Ready, val choose: suspend (String) -> Sent)

    /** L'esito del secondo passo, o null. */
    fun choiceOf(text: String?): Choice? = runCatching {
        val o = json.parseToJsonElement(text ?: "") as kotlinx.serialization.json.JsonObject
        fun str(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
        Choice(str("choice") ?: return@runCatching null, str("url"), str("text").orEmpty(), str("screenshot_url"))
    }.getOrNull()
}
