package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry

/**
 * La chat a scheda unica (app-chat-scheda-unica-13-11, Franz 13/11 09:32: E1 a quattro zone, separate come in F3), gemella di
 * web/src/lib/chatTurns.ts. Un turno è la richiesta e tutto quello che la sessione scrive fino alla richiesta dopo: il lavoro (le
 * frasi intermedie e i passi, nell'ordine della trascrizione) e la risposta, l'ultimo messaggio se dopo non ci sono altri passi.
 */
object ChatTurns {
    /** Una voce del lavoro: una frase della sessione (`say`) o un passo. */
    data class Step(val say: Boolean, val entry: TranscriptEntry)

    sealed interface Row {
        /** `request` è un [ChatFeed.Item.Mine] o un [ChatFeed.Item.User]; null se la pagina comincia a metà turno. */
        data class Turn(val key: String, val request: ChatFeed.Item?, val work: List<Step>, val answer: TranscriptEntry?) : Row {
            /** Tutte le voci della trascrizione del turno, per trovare quella cercata. */
            val entryIds: List<String> get() = listOfNotNull(request?.let(ChatFeed::entryOf)) + work.map { it.entry.id } + listOfNotNull(answer?.id)
            val tools: Int get() = work.count { !it.say }
            val failed: Int get() = work.count { !it.say && it.entry.error == true }
        }
        data class Sep(val item: ChatFeed.Item.Separator) : Row
    }

    fun key(r: Row): String = when (r) { is Row.Turn -> r.key; is Row.Sep -> "x-" + r.item.entry.id }

    /** Le voci della chat (anche già raccolte in gruppi di passi) divise in turni; i separatori restano fra un turno e l'altro. */
    fun turns(items: List<ChatFeed.Item>): List<Row> {
        class Open(val key: String, val request: ChatFeed.Item?) { val work = mutableListOf<Step>() }
        val out = mutableListOf<Any>()
        var cur: Open? = null
        fun step(say: Boolean, e: TranscriptEntry) {
            val c = cur ?: Open("h-" + e.id, null).also { out += it; cur = it }
            c.work += Step(say, e)
        }
        items.forEach { it ->
            when (it) {
                is ChatFeed.Item.Mine -> cur = Open("m-" + it.sent.id, it).also { o -> out += o }
                is ChatFeed.Item.User -> cur = Open("u-" + it.entry.id, it).also { o -> out += o }
                is ChatFeed.Item.Separator -> { out += it; cur = null }
                is ChatFeed.Item.Steps -> it.entries.forEach { e -> step(false, e) }
                is ChatFeed.Item.Tool -> step(false, it.entry)
                is ChatFeed.Item.Claude -> step(true, it.entry)
            }
        }
        return out.map { o ->
            if (o is Open) {
                val last = o.work.lastOrNull()
                if (last?.say == true) Row.Turn(o.key, o.request, o.work.dropLast(1), last.entry) else Row.Turn(o.key, o.request, o.work.toList(), null)
            } else Row.Sep(o as ChatFeed.Item.Separator)
        }
    }

    /** La riga della voce della trascrizione `id`, -1 se non c'è. */
    fun rowOfEntry(rows: List<Row>, id: String): Int = rows.indexOfFirst { r ->
        when (r) { is Row.Turn -> id in r.entryIds; is Row.Sep -> r.item.entry.id == id }
    }

    /** La riga di una voce della chat, per «Nuovi» e per tornare a un messaggio mandato; -1 se non c'è. */
    fun rowOf(rows: List<Row>, item: ChatFeed.Item): Int = when (item) {
        is ChatFeed.Item.Mine -> rows.indexOfFirst { r -> r is Row.Turn && (r.request as? ChatFeed.Item.Mine)?.sent?.id == item.sent.id }
        else -> ChatFeed.entryOf(item)?.let { rowOfEntry(rows, it) } ?: -1
    }

    /** Oltre [FOLD_OVER] voci il lavoro chiuso mostra solo le ultime [FOLD_TAIL]; il tocco sulla sua testata lo apre tutto. */
    const val FOLD_OVER = 6
    const val FOLD_TAIL = 3
    fun foldable(work: List<Step>): Boolean = work.size > FOLD_OVER
    fun shownWork(work: List<Step>, open: Boolean): List<Step> = if (open || !foldable(work)) work else work.takeLast(FOLD_TAIL)

    /** `prose` = l'argomento è una frase (la descrizione di un comando), non codice. */
    data class Line(val name: String, val arg: String, val prose: Boolean = false)

    /**
     * La riga di un passo: il nome dello strumento e il suo argomento su una riga. Per i file le ultime due parti del percorso; per
     * un comando la sua descrizione (app-chat-bash-descrizione-13-11, Franz 13/11 10:47: «Bash: descrizione», il comando all'apertura), e
     * senza descrizione il comando; per gli altri la prima riga del testo, e senza testo la nota.
     */
    fun stepLine(e: TranscriptEntry): Line {
        val raw = e.tool?.trim().orEmpty()
        val name = ToolText.short(raw.split(' ', '(').first()).ifEmpty { "?" }
        val text = e.text?.trim().orEmpty()
        val k = ToolText.kind(e.tool)
        if ((k == ToolText.Kind.READ || k == ToolText.Kind.EDIT || k == ToolText.Kind.WRITE) && '/' in text && '\n' !in text) {
            return Line(name, text.split('/').filter { it.isNotEmpty() }.takeLast(2).joinToString("/"))
        }
        val said = e.note?.trim()?.substringBefore('\n')?.takeIf { it.isNotEmpty() }
        if (k == ToolText.Kind.RUN && said != null) return Line(name, said, prose = true)
        return Line(name, text.substringBefore('\n').ifEmpty { e.note?.trim().orEmpty() })
    }

    data class Detail(val note: String?, val text: String?)

    /** Il dettaglio che il tocco apre: la nota e il testo intero, solo se dicono più della riga. */
    fun stepDetail(e: TranscriptEntry): Detail {
        val line = stepLine(e)
        val text = e.text?.trim().orEmpty()
        val note = e.note?.trim()?.takeIf { it.isNotEmpty() }
        return Detail(note?.takeIf { line.prose || it != line.arg }, text.takeIf { it.isNotEmpty() && it != line.arg })
    }
}
