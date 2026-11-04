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
