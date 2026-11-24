# Indicatore di attività di una sessione al lavoro: proposte A-D (app-indicatore-attivita-24-11)

Franz, 24/11 alle 09:38: «mandami proposte per inserire uno spinner migliore», «o comunque un indicatore di attività».

Oggi (6185c51c, proposta B di app-favicon-siti-18-11) la favicon sta su un riquadro chiaro con l'anello del colore dello
stato. Per una sessione al lavoro l'anello è un bordo con gli angoli arrotondati che ruota tutto intero: gli angoli girano
col riquadro e non si vede mai un anello pulito (`oggi.png`).

`index.html` ha le animazioni vere: si apre nel browser, con tutte le proposte una sotto l'altra.
- `?p=A` mostra una proposta sola.
- `?rm` forza «riduci movimento»; il pannello «riduci movimento» lo mostra comunque.

In ogni PNG ci sono tre pannelli: la notte, il giorno e la notte con «riduci movimento». In ogni pannello:
- una sessione al lavoro e una che ti aspetta a 40 (telefono);
- una sessione al lavoro a 28 (web).

Colori e caratteri vengono da `web/src/v3.css`. Le favicon sono inventate. Il fotogramma della PNG è fermo a metà
animazione.

| Proposta | Cosa si muove | Con «riduci movimento» | PNG |
|---|---|---|---|
| Oggi | il bordo ruota con gli angoli del riquadro | fermo, a un quarto | `oggi.png` |
| **A · Traccia lungo il bordo** | il riquadro sta fermo; un tratto blu corre lungo il contorno, sopra l'anello tenue (1,6 s a giro) | anello blu pieno | `A.png` |
| **B · Pallino che respira** | anello blu pieno e fermo; all'angolo un pallino blu manda un'onda leggera ogni 1,8 s | resta il pallino fermo | `B.png` |
| **C · Tre punti accanto ai minuti** | anello blu pieno e fermo; tre punti accanto ai minuti si accendono uno dopo l'altro, come «sta scrivendo» | tre punti accesi | `C.png` |
| **D · Barra sottile** | anello blu pieno e fermo; sotto la favicon una barra di 2 px con un segmento che scorre, come la barra indeterminata di Android | barra piena | `D.png` |

Cosa cambia fra le strade:
- **A** è lo spinner di oggi corretto: dice «al lavoro» nello stesso posto e con lo stesso gesto, senza far ruotare il riquadro.
- **B, C e D** lasciano l'anello pieno e fermo per tutti gli stati (ambra, blu, verde). Il movimento sta in un segno a
  parte, più piccolo e meno insistente in un elenco con molte sessioni al lavoro.
- **C** sposta un po' i minuti a sinistra.
- **D** allunga la riga di 6 px.

Le sessioni senza sito non cambiano: tengono il glifo ▶ con l'anello del contesto. L'orologio non cambia. Il codice
dell'app non cambia finché Franz non sceglie; poi la stessa scelta va in `SiteAvatar.svelte` e in `SiteAvatar.kt`.
