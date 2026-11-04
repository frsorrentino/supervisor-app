package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MasterLanding.Height

/**
 * Dove sta la master sul tablet, come nella PWA larga (App.svelte: masterOpen, masterLeft, masterHalf, masterW, masterUnder,
 * stackH; app-pwa-master-tasti-03-11, app-master-sopra-dashboard-03-11, app-master-compatta-03-11, app-master-trascina-03-11;
 * sul tablet app-tablet-come-pwa-03-11). Chiusa è la barra in fondo; aperta un pannello di lato, stretto (420) o a metà schermo
 * (720) o al posto fisso lasciato col bordo (colonna o metà schermo); con le sessioni aperte può stare sotto la home, compatta o a metà colonna.
 */
data class MasterPlace(
    val open: Boolean = false,
    val left: Boolean = false,
    /** Stretta (420): il tasto Allarga; false = 720. */
    val narrow: Boolean = true,
    /** La misura lasciata col bordo; null = quella del tasto. */
    val width: Float? = null,
    val under: Boolean = false,
    val stack: Height = Height.COMPACT,
) {
    fun panelWidth(): Float = width ?: if (narrow) MasterLanding.COLUMN else WIDE

    /** Home sopra e master sotto: solo con le sessioni aperte e la master aperta. */
    fun stacked(sessionsOpen: Boolean): Boolean = under && open && sessionsOpen

    fun height(sessionsOpen: Boolean): Height = when {
        !open -> Height.BAR
        !stacked(sessionsOpen) -> Height.COLUMN
        else -> stack
    }

    fun withHeight(h: Height): MasterPlace = when (h) {
        Height.BAR -> copy(open = false)
        Height.COLUMN -> copy(open = true, under = false)
        else -> copy(open = true, under = true, stack = h)
    }

    /** Il tasto tondo e il tocco sulla maniglia sotto la home: compatta e metà si alternano. */
    fun stepStack(): MasterPlace = copy(stack = if (stack == Height.COMPACT) Height.HALF else Height.COMPACT)

    /** Il tasto Allarga/Stringi: torna alla misura del tasto, dimenticando quella del bordo. */
    fun toggleSize(): MasterPlace = copy(narrow = !narrow, width = null)

    /** Il bordo lasciato: il posto fisso più vicino, e conta come stretta sotto il punto di mezzo. */
    fun widthDropped(px: Float, screen: Float): MasterPlace {
        val w = MasterLanding.widthAt(px, screen)
        return copy(width = w, narrow = MasterLanding.narrow(w, screen))
    }

    enum class Item { RIGHT, LEFT, UNDER, SIZE, CLOSE }
    data class Entry(val item: Item, val on: Boolean = false)

    /**
     * Il menu Posizione accanto al ▶ (Franz 03/11 17:23): destra, sinistra, sotto la home (solo con le sessioni aperte), la
     * misura e Chiudi (solo aperta).
     */
    fun menu(sessionsOpen: Boolean): List<Entry> = buildList {
        add(Entry(Item.RIGHT, !left)); add(Entry(Item.LEFT, left))
        if (sessionsOpen) add(Entry(Item.UNDER, under))
        if (open) { add(Entry(Item.SIZE)); add(Entry(Item.CLOSE)) }
    }

    fun pick(item: Item): MasterPlace = when (item) {
        Item.RIGHT -> copy(left = false)
        Item.LEFT -> copy(left = true)
        Item.UNDER -> copy(under = !under)
        Item.SIZE -> toggleSize()
        Item.CLOSE -> copy(open = false)
    }

    /** Ricordata su questo dispositivo, come le chiavi `cm.master_*` della PWA; aperta o chiusa non si ricorda. */
    fun pref(): String = listOf(if (left) "L" else "R", if (narrow) "N" else "W", width?.toInt()?.toString() ?: "-", if (under) "U" else "-", stack.name).joinToString(",")

    companion object {
        const val WIDE = 720f
        fun fromPref(raw: String?): MasterPlace {
            val p = raw?.split(',') ?: return MasterPlace()
            if (p.size != 5) return MasterPlace()
            return MasterPlace(
                left = p[0] == "L", narrow = p[1] != "W", width = p[2].toFloatOrNull(), under = p[3] == "U",
                stack = runCatching { Height.valueOf(p[4]) }.getOrNull()?.takeIf { it == Height.COMPACT || it == Height.HALF } ?: Height.COMPACT,
            )
        }
    }
}
