# Effetti moderni da scegliere

Compito `app-audit-grafica-animazioni-04-11`, parte 3. Franz, 04/11 00:22: «proposte di effetti moderni analoghi». Analoghi al
morphing: una forma che diventa un'altra, senza decorazione. Prova dal vivo di ciascuno: `docs/mockup/2026-11-04-effetti/effetti.html`
(immagine: `effetti.png`). Niente entra nell'app senza la scelta di Franz.

| | Effetto | Dove | Telefono | Web | Costo |
|---|---|---|---|---|---|
| A | **Glifi di stato che cambiano forma**: il ▶ si piega nella spunta o nella ✗ quando la sessione finisce o fallisce | Glifi di stato di home, testate, ricerca | `Morph` di `androidx.graphics:graphics-shapes`, o i tre segmenti di `StateGlyph` interpolati | Transizione del tracciato SVG (`d`) in `StateGlyph.svelte` | S |
| B | **Indietro predittivo**: col gesto Indietro la pagina rimpicciolisce sotto il dito e, lasciata, torna nella voce che l'aveva aperta | Tutte le pagine e la sessione | `PredictiveBackHandler` in `PageIn`, che chiude il morphing al contrario | Il gesto è del browser: niente | M |
| C | **Tasti che cambiano forma alla pressione**: la pillola diventa un rettangolo morbido e torna con la molla | Tutti i tasti pillola | Forma premuta di Material 3 Expressive su `V3Pill` e `labelLarge` | `border-radius` con la molla accanto alla scala 0,97 in `v3.css` | S |
| D | **Testata che si compatta scorrendo**: il titolo grande scende nella barra e il tabellone si ritira, legati al dito | Home del telefono | `LargeTopAppBar` con `exitUntilCollapsedScrollBehavior` | `animation-timeline: scroll()` | M |

Regole che valgono per tutti: con le animazioni ridotte niente movimento, solo lo stato finale; niente rimbalzo su colori e
opacità; nessun effetto sui dati che arrivano a ogni push.

## Cosa consiglio

**B** per primo: chiude il morphing appena fatto sul telefono, che oggi ha solo l'andata, ed è il gesto di sistema di Android 14
e successivi. Poi **A**, piccolo e legato al significato delle icone di Telegram. **C** è gusto: si vede solo premendo. **D** cambia
la testata della home, che è stata riordinata da poco: lo terrei per ultimo.

## Scelta e stato

Franz, 04/11 06:49: «Tutti e quattro». Commit 84ec91d6, web 0.2.1871; telefono con la pre-release che segue.

| | Telefono | Web | Limiti |
|---|---|---|---|
| A | `StateGlyph` in `HomeV3.kt`: i sei punti di ▶, ✓ e ✗ (`MARKS`) si interpolano con una molla; a riposo il disegno è quello di prima | `StateGlyph.svelte`: lo stesso tracciato in `d`, transizione di 420 ms con `--spring-bounce` | Solo fra ▶, ✓ e ✗; ❓ e il tratteggio di chi è fermo cambiano di colpo come prima |
| B | `PageBackHandler` in `PageMotion.kt` sulle otto pagine con `PageIn`: la pagina rimpicciolisce del 10%, si sposta di 24 dp e si arrotonda fino a 28; lasciata, si chiude nel riquadro del tocco che l'aveva aperta e la home compare sul posto | — (il gesto è del browser) | Le pagine prendono il posto della home: durante il gesto dietro c'è lo sfondo, non la home. Il tasto Indietro a tre pulsanti chiude come prima |
| C | `pressShape` in `PhoneTheme.kt` su `V3Pill`, `MiniButton`, `ElicitButton`, `ConfirmButton`, `NeedButton`: da pillola a 9 dp premuto, ritorno con la molla | Regola `:active` in `app.css` su `.tonal`, `.filled`, `.approve`, `.vbtn` | Gli altri tasti Material del telefono restano pillole ferme |
| D | `HomeScroll` scritto da `SummaryList`: titolo della home da 22 a 19 nei primi 90 dp, filo sotto la testata fra 40 e 90, tabellone fino al 25% e al 96% nei primi 110 | `scroll-timeline: --home-scroll` su `.home`, `timeline-scope` su html; stessi valori in `AppBar.svelte` e `Home.svelte` | La testata del telefono è già bassa (56 dp): si compatta il titolo, non l'altezza. Sul web, con più colonne `.home` aperte la timeline è ambigua e la testata resta ferma |

Con le animazioni ridotte, e negli snapshot (`LocalStill`), niente movimento: solo lo stato finale. Prova sul web (demo 0.2.1871,
Chrome): scorrendo di 120 px il titolo è a 0,864, il tabellone a 0,25 e il filo compare; il tracciato di A interpola `d` in un
SVG di prova con gli stessi punti (nella demo l'unico segno a vista sta in un dialogo chiuso). La pressione di C e il gesto di B
si provano solo a mano.
