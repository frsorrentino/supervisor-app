# Basi per i concept della master (06/10/2026)

## Il prodotto

claude-master: un'app (Android Compose, telefono Pixel 11 Pro XL, tablet, Chromebook; più una web app Svelte installabile come PWA) che mostra le sessioni Claude Code del PC di Franz. La home elenca le sessioni in card grigio-blu su nero, raggruppate per stato. La **master** è una sessione speciale: è quella che coordina le altre. Nella home sta in fondo come una **linguetta** (barra con badge, riga «MASTER · ora · modello · contesto», titolo dell'ultimo esito, tasti ▶ e ▲) con sotto il suo **campo** «Scrivi alla master». Un tocco la apre a tutta pagina: la linguetta sale in cima (stesso verso, angoli tondi in alto, tasto ▼), sotto c'è la riga modello/quota/contesto, la conversazione, il riquadro «Prossimi» e il campo.

## Cosa chiede Franz (06/10, 17:50-18:59)

- La master deve **percepirsi come una sessione diversa dalle altre**, a colpo d'occhio, sia chiusa (linguetta + campo in fondo alla home) sia aperta (pagina intera).
- Il fondo della linguetta deve proseguire intorno al campo: linguetta e campo sono un blocco unico.
- Da aperta va allineato **anche lo sfondo della chat**: oggi la linguetta e il campo hanno il colore della master, ma in mezzo la conversazione resta nera come le altre.
- La variante B «terracotta» (linguetta #36221A, fondo #24160F, campo #180E0A, riga e icone corallo) è ora nell'app: **il colore non lo convince**.

## Storia: cosa è già stato scartato e perché

- Lilla/viola (cornici, aurora, «violet», bordo lilla del campo, 04-05/10): scartato da Franz.
- A Vetro, B Editoriale (serif), C Isola (capsula in cima tipo Dynamic Island), D Luce (05/10): scelta D, poi **Luce** (alone corallo acceso solo mentre la master lavora) non capita: «mi aspettavo un effetto di luce sulla tab».
- Corallo pieno #D97757 sulla barra: «troppo acceso».
- Solo bordo corallo, poi «filo corallo» (filo di 3 px in alto + riga e icone corallo): accettato per un po', poi la richiesta del fondo che prosegue intorno al campo.
- Terracotta a blocco (B, ora installata): «il colore non mi convince».
- Immagini: `docs/mockup/2026-10-05-master-alternative/*.png`, `docs/mockup/2026-10-06-master-stile/master-stile.png`, `docs/mockup/2026-10-06-master-stile/master-blocco.png`, e dal telefono di Franz `docs/segnalazioni/2026-10-06-il-colore-non-mi-convince-e-serve-comunq.jpg` (home, B) e `docs/segnalazioni/2026-10-06-image-from-the-phone.jpg` (master aperta, B). Lo stato di oggi sul banco di prova: `docs/mockup/2026-10-06-master-concept/concept-base.png`.

## Vincoli fissi

- Tema scuro, nero di fondo (#000). Telefono prima di tutto (412 dp di larghezza).
- I colori di stato hanno già un significato e non vanno rubati dalla master: ambra #FFB020 = ti aspetta, verde #65C581/#34C759 = ha finito/ferma, blu #7FA1FF/#8BB4F7 = al lavoro, rosso #FF453A/#E5736B = errore/chiusura/produzione, viola #B9A6F5/#C58AF9 = da approvare, consigli.
- Regole di Franz: un solo bottone pieno per schermata; mai «…» nel corpo dei testi; una riga logica occupa la riga fisica; testi in italiano.
- Deve funzionare uguale su Android (Compose) e web (CSS), con gli stessi token. Niente effetti pesanti (blur animati, shader) che rallentano un telefono di fascia media.
- Contrasto WCAG AA per tutto il testo (4.5:1, 3:1 per testo grande e icone).

## Token di oggi (web/src/app.css)

```
--bg: #000; --low: #1B1F26; --surface: #23272E; --high: #292F3A; --line: #2A2E35;
--text: #F2F4F7; --text2: #B0B8C4; --icon: #A8C7FA; --primary: #D3E3FD; --on-primary: #0A2050;
--opus: #D97757 (corallo di Claude); --master-tab: #36221A; --master-bed: #24160F; --master-field: #180E0A;
--mono: ui-monospace, "Roboto Mono", monospace. Font del testo: Roboto.
```

## Mappa dei selettori della web app (per il CSS del concept)

Il banco di prova inietta `concept-<k>.css` nella pagina dell'app, dopo i suoi stili: usa `!important` dove serve vincere sugli stili con scope di Svelte.

Master chiusa (in fondo alla home):
- `.pane > .closed`: il blocco che contiene linguetta e campo (oggi fondo `--master-bed`, angoli 26 px in alto).
- `.dock`: la linguetta (oggi `--master-tab`). Dentro: `.dock .mono` la riga «MASTER · …», `.dock b` il titolo, `.dock .ib` i due tasti tondi, `.dock svg[role=img]` il badge di stato.
- `.mquick`: il contenitore del campo; `.field.master` il campo (form); dentro `textarea`, `.round` il tasto invia; sotto, una riga piccola «Invio manda · Maiusc+Invio a capo».

Master aperta (pagina intera):
- `.slide`: il contenitore della master aperta. `.slide .dock.expanded`: la linguetta in cima.
- `.slide .chat`: la conversazione (section). `.slide .chat > header`: riga modello/quota/contesto e menu. `.slide .pill`: la pillola «ledger-api ti aspetta».
- `.slide .lines`: l'area che scorre, **lo sfondo della chat**. `.slide .tohome`: il link «Casa».
- `.slide .claude`, `.slide .ctext`: una risposta di Claude; `.slide .me .bubble`: un messaggio di Franz (bolla a destra); `.slide .mrow`, `.slide .time`, `.slide .meta`: righe piccole; `.slide .steps`: un gruppo di passaggi.
- `.slide .box`: il riquadro «Prossimi». `.slide .field.master`: il campo in fondo.

Il resto della home (card delle altre sessioni `.card`, etichette di gruppo) **non** va cambiato: è il termine di paragone.

## Dove vive il codice (per le note Android)

- Android: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/MasterDock.kt` (linguetta), `SessionSheet.kt` (campo: `Composer`, `bar`; sfondo della master aperta: modificatore `masterChat` e `LocalMasterChatStyle`), `ui-tokens/src/main/kotlin/it/pixelbox/cmwatch/ui/tokens/CmColors.kt`.
- Web: `web/src/lib/MasterDock.svelte`, `HomePane.svelte`, `Chat.svelte`, `Composer.svelte`, `Feed.svelte`, `web/src/app.css`.

## Come si prova un concept

1. Scrivi il CSS in `docs/mockup/2026-10-06-master-concept/concept-<k>.css`.
2. `sh docs/mockup/2026-10-06-master-concept/render.sh <k>` (circa 1 minuto): produce `shot-<k>-home.png`, `shot-<k>-open.png` e `concept-<k>.png` affiancate, 412×915 a densità 2.
3. Guarda il PNG con lo strumento Read e correggi. Massimo 3 giri.
