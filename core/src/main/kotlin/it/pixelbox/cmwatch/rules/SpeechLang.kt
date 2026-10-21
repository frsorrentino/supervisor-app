package it.pixelbox.cmwatch.rules

/**
 * Voce bilingue (Franz, 21/10 00:57: «si può fare in modo che venga riconosciuta la parte inglese e la voce legga con
 * l'accento corretto?»). Un testo da leggere si divide in pezzi con la loro lingua: si decide per frase, e dentro la
 * frase per le parti tra virgolette o parentesi. La voce cambia solo per spezzoni di almeno `MIN_WORDS` parole, così un
 * termine tecnico in una frase italiana («il push del branch») resta italiano e la voce non salta a metà frase.
 *
 * Il classificatore è leggero e lavora senza rete: conta le parole funzionali più frequenti delle due lingue, le
 * elisioni italiane («l'app», «c'è») e le contrazioni inglesi («don't»); una parola accentata vale doppio. Serve almeno
 * `MIN_SCORE` di parole funzionali a favore: una frase di soli nomi tecnici («26 test TypeScript verdi») non ne ha e resta
 * nella lingua di base (prova sugli handoff del repo, 21/10: con le desinenze passavano all'inglese).
 */
object SpeechLang {
    enum class Lang {
        IT, EN;

        companion object {
            /** La lingua della voce di base (`Locale.language`); un'altra lingua legge tutto con la sua voce, come prima. */
            fun of(language: String): Lang? = when (language) {
                "it" -> IT
                "en" -> EN
                else -> null
            }
        }
    }

    data class Piece(val text: String, val lang: Lang)

    /** Sotto questo numero di parole uno spezzone resta nella lingua intorno. */
    const val MIN_WORDS = 3

    /** Quante parole funzionali servono a una lingua per prendersi uno spezzone. */
    const val MIN_SCORE = 2

    /** Parole funzionali che esistono in una lingua sola: «a», «in», «no», «come», «so», «me» stanno in tutte e due. */
    private val IT_WORDS = setOf(
        "il", "lo", "la", "gli", "le", "un", "una", "uno", "di", "del", "dello", "della", "dei", "degli", "delle", "da",
        "dal", "dalla", "dai", "dalle", "con", "su", "sul", "sulla", "sui", "nel", "nella", "nei", "nelle", "al", "alla",
        "ai", "alle", "tra", "fra", "che", "non", "è", "e", "ed", "ma", "anche", "più", "già", "poi", "sono", "sei",
        "siamo", "ho", "hai", "ha", "abbiamo", "hanno", "era", "questo", "questa", "questi", "queste", "quello",
        "quella", "quando", "dove", "perché", "cosa", "ci", "si", "mi", "ti", "ne", "sia", "o", "oppure", "ogni",
        "tutto", "tutti", "tutte", "stato", "stata", "fatto", "fatta", "molto", "ancora", "solo", "qui", "adesso", "ora",
        "dopo", "prima", "senza", "se", "quindi", "però", "invece", "ieri", "oggi", "domani", "va", "fa", "può",
    )
    private val EN_WORDS = setOf(
        "the", "of", "and", "to", "is", "are", "was", "were", "be", "been", "being", "it", "its", "this", "that",
        "these", "those", "with", "for", "on", "at", "by", "from", "or", "not", "but", "you", "your", "we", "our",
        "they", "their", "he", "she", "his", "her", "will", "would", "should", "could", "can", "cannot", "have", "has",
        "had", "do", "does", "did", "what", "which", "who", "when", "where", "why", "how", "if", "then", "than",
        "there", "here", "all", "any", "some", "just", "only", "also", "more", "most", "about", "into", "over",
        "after", "before", "now", "new", "get", "got", "make", "made", "need", "needs", "please", "let", "an", "as",
        "my", "us", "up", "out", "without", "yet", "each", "every", "same", "other", "one", "two", "still", "again",
    )
    private val WORD = Regex("\\p{L}+")
    private val ELISION = Regex("(?i)\\b(l|dell|nell|all|dall|sull|un|c|d|quest|quell|po)['’]\\p{L}")
    private val CONTRACTION = Regex("(?i)\\p{L}['’](t|s|re|ve|ll|m|d)\\b")
    private val ACCENT = Regex("[àèéìíòóù]")
    /** Le frasi si tagliano solo a `.!?…` e a capo: «First closed test of Supervisor: see which…» è una frase sola. */
    private val SENTENCE = Regex("(?<=[.!?…])\\s+|\\n+")
    /** Una parentesi con virgolette dentro non è uno spezzone: lo sono le virgolette. */
    private val SPAN = Regex("«[^»]*»|“[^”]*”|\"[^\"]*\"|\\([^)«“\"]*\\)")
    /** Nomi di file, rami, domini, identificativi: «my-pixelbox-it», «feature/tablet», «needs_you» non sono parole. */
    private val IDENT = Regex("\\S*[\\p{L}\\d][-_/.@][\\p{L}\\d]\\S*")

    /** La lingua di uno spezzone; null se ha meno di `MIN_WORDS` parole o non si capisce. */
    fun classify(text: String): Lang? {
        val words = WORD.findAll(IDENT.replace(text, " x ").lowercase()).map { it.value }.toList()
        if (words.size < MIN_WORDS) return null
        val itScore = words.count { w -> w in IT_WORDS } + words.count { w -> ACCENT.containsMatchIn(w) } + ELISION.findAll(text).count()
        val enScore = words.count { w -> w in EN_WORDS } + CONTRACTION.findAll(text).count()
        return when {
            itScore >= MIN_SCORE && itScore > enScore -> Lang.IT
            enScore >= MIN_SCORE && enScore > itScore -> Lang.EN
            else -> null
        }
    }

    /** I pezzi del testo, ognuno con la sua lingua; quelli vicini della stessa lingua si uniscono. */
    fun split(text: String, base: Lang): List<Piece> {
        val out = mutableListOf<Piece>()
        fun add(t: String, lang: Lang) {
            val s = t.trim()
            if (s.isEmpty()) return
            val last = out.lastOrNull()
            if (last?.lang == lang) out[out.lastIndex] = Piece(last.text + " " + s, lang) else out += Piece(s, lang)
        }
        for (frase in text.trim().split(SENTENCE)) {
            val spans = SPAN.findAll(frase).toList()
            val fuori = classify(SPAN.replace(frase, " ")) ?: base
            var from = 0
            for (m in spans) {
                add(frase.substring(from, m.range.first), fuori)
                add(m.value, classify(m.value) ?: fuori)
                from = m.range.last + 1
            }
            add(frase.substring(from), fuori)
        }
        return out
    }

    /**
     * I pezzi da dare al motore vocale, ognuno entro `max` caratteri e tagliato a fine frase come `SpeechText.chunks`;
     * senza la voce bilingue (impostazione spenta) tutto nella lingua di base.
     */
    fun chunks(text: String, base: Lang, bilingual: Boolean = true, max: Int = SpeechText.MAX_CHUNK): List<Piece> {
        val pezzi = if (bilingual) split(text, base) else listOf(Piece(text, base))
        return pezzi.flatMap { p -> SpeechText.chunks(p.text, max).map { Piece(it, p.lang) } }
    }
}
