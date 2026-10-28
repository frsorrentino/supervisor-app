package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.SystemAdvice
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

/** La sezione «Sistema» della home (contratto 1.90, Franz 28/10 13:19): quando si apre, quale tasto, le cifre. */
object SystemRules {
    /** Il tocco sull'intestazione vale per il livello in cui è stato dato. */
    data class Manual(val level: String, val open: Boolean)

    /** Compatta quando è tutto verde, aperta da sola in giallo e in rosso. */
    fun isOpen(level: String, manual: Manual?): Boolean = if (manual != null && manual.level == level) manual.open else level != "green"

    /** L'op del tasto, solo se il consiglio ne porta uno, l'app la conosce e il relay la esegue (heavy_job e low_memory sono solo parole). */
    fun opOf(a: SystemAdvice, ops: List<String>?): CmdOp? {
        val op = a.action?.op ?: return null
        if (ops?.contains(op) != true) return null
        return when (op) { "sys_kill" -> CmdOp.SYS_KILL; "sys_unit_stop" -> CmdOp.SYS_UNIT_STOP; "close" -> CmdOp.CLOSE; else -> null }
    }

    /** Al massimo un decimale, senza «,0»; la virgola in italiano. */
    fun num(x: Double, comma: Boolean): String {
        val r = (x * 10).roundToInt() / 10.0
        val s = if (r == floor(r)) r.toLong().toString() else String.format(Locale.ROOT, "%.1f", r)
        return if (comma) s.replace('.', ',') else s
    }

    fun size(mb: Int, comma: Boolean): String = if (mb < 1024) "$mb MB" else "${num(mb / 1024.0, comma)} GB"

    fun cpu(x: Double): Int = x.roundToInt()
}
