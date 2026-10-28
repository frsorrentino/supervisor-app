package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** Franz, 22/10 13:36: niente emoji nell'app e nelle notifiche; i testi del relay passano dal filtro. */
class EmojiTest {
    @Test fun statusSymbolsAtTheHeadOfARelayTitleGo() {
        assertEquals("atlas-release-2-4: check green", Emoji.strip("✓ atlas-release-2-4: check green"))
        assertEquals("ledger-migrations-011: check red", Emoji.strip("✗ ledger-migrations-011: check red"))
        assertEquals("social-example-it: limite cinque ore", Emoji.strip("⛔ social-example-it: limite cinque ore"))
        assertEquals("atlas-shop: turn failed — 529", Emoji.strip("⚠ atlas-shop: turn failed — 529"))
        assertEquals("ledger-api", Emoji.strip("❓ ledger-api"))
        assertEquals("orbit-docs in pausa", Emoji.strip("⏸ orbit-docs in pausa"))
    }

    @Test fun pictogramsSkinTonesFlagsAndKeycapsGoAndTheSpacesCloseUp() {
        assertEquals("deploy fatto, controlla", Emoji.strip("🚀 deploy fatto 🎉, controlla 👍🏽"))
        assertEquals("nome", Emoji.strip("🟢 nome"))
        assertEquals("ok 3 file", Emoji.strip("ok ✅ 3️⃣ file 🇮🇹"))
        assertEquals("famiglia", Emoji.strip("famiglia 👨‍👩‍👧"))
        // ▶ ha la presentazione emoji e va via; ◐ ○ ● sono forme geometriche di testo e restano.
        assertEquals("in corso · ◐ 2", Emoji.strip("▶ in corso · ◐ 2"))
    }

    @Test fun lettersArrowsAndPunctuationStay() {
        assertEquals("→ prossimo: apri la CI · 95 % · ↳ ok · © 2026 · ≥ 6 GB", Emoji.strip("→ prossimo: apri la CI · 95 % · ↳ ok · © 2026 · ≥ 6 GB"))
        assertEquals("perché «sì» — à é ì", Emoji.strip("perché «sì» — à é ì"))
        assertEquals("", Emoji.strip("✓"))
        assertEquals("3 m", Emoji.strip("✓ · 3 m"))
    }
}
