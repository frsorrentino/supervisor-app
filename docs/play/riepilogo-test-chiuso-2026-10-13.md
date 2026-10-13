# Test chiuso di com.francescosorrentino.cmaster: riepilogo da approvare (13/10/2026)

Tutto salvato in bozza in Play Console, account personale 6472649933491251229. Niente è stato inviato in revisione.

## Build

- Telefono: AAB versionCode 21, versionName 0.2, dalla CI 38398505919 (commit 36b8ba20), firmato con la chiave di upload
  dei secret (quella del watchface). Bozza nel canale «Test chiusi - Alpha», nome release «0.2 (21)».
- Orologio: AAB versionCode 22, stessa CI. Bozza nel canale nuovo «Test chiusi - Wear closed testing» (Solo Wear OS),
  nome release «0.2 (22)».
- Note di versione (en-US): «First closed test of Supervisor: see which Claude Code session waits on you, answer with one
  tap, read conversations and files, the master session, the evening recap and the night report. Demo mode needs no
  computer.»

## Tester e paesi

- Tester: Google Gruppo supervisor-testers@googlegroups.com, su tutti e due i canali. Feedback:
  https://github.com/frsorrentino/supervisor-app/issues.
- Paesi: tutti, ancora da salvare (vedi «Aperto»).

## Scheda dello Store (en-US predefinita, it-IT)

- Nome: Supervisor.
- Breve en: «Steer Claude Code sessions from phone and watch. Not affiliated with Anthropic».
- Breve it: «Le sessioni di Claude Code su telefono e orologio. Non affiliata ad Anthropic».
- Descrizione completa: `docs/play/listing.json` (en-US e it-IT).
- Icona: `docs/play/icon-512.png` (la nuova, uguale all'icona Android).
- Grafica in primo piano: `docs/play/feature-graphic-1024x500.png` (scelta da Franz il 29/09; dice «Answer Claude Code
  from your wrist»).
- Screenshot telefono, 1080x2160, dalla demo: `docs/play/screenshot/phone-store/en/s1Home.png`, `s2Question.png`,
  `s3Conversation.png`, `s4Master.png` (anche in `it/`; sulla scheda ci sono quelli `en`).
- Screenshot orologio, 456x456, dalla demo: `docs/play/screenshot/watch-store/sessions.png`, `question.png`, `card.png`,
  `quota.png`, `terminal.png`, `liveQuestion.png`.
- Categoria: Produttività. Contatti: info@francescosorrentino.com, https://supervisor.francescosorrentino.com.

## Dichiarazioni

- Privacy policy: https://supervisor.francescosorrentino.com/privacy.
- Accesso per i revisori: «Sì, parti limitate» (l'app si accoppia con un QR del computer). Istruzioni: nessun account;
  modalità Demo dal primo schermo («Try the demo») sul telefono e sull'orologio, o Impostazioni › Demo mode; i dati della
  demo sono inventati e restano sul dispositivo. Casella «le credenziali danno accesso completo» spuntata.
- Annunci: nessuna pubblicità (dichiarazione del 29/09, invariata).
- Classificazione dei contenuti: questionario IARC del 29/09, invariato.
- Pubblico di destinazione: 18 anni o più.
- Sicurezza dei dati: raccoglie solo «ID dispositivo o altri ID» (id anonimo di Firebase, id di installazione, token di
  messaggistica), per «Funzionalità dell'app»; nessun dato condiviso con terze parti; cifrati in transito; nessun account
  da creare; richiesta di eliminazione dei dati: https://supervisor.francescosorrentino.com/privacy. Il contenuto delle
  sessioni viaggia cifrato da un capo all'altro verso il Firebase dell'utente, quindi non è dichiarato come raccolto.
- App governative, finanza, salute: «no» (29/09, invariate).

## Aperto

- Paesi dei due canali: nella tabella «Aggiungi paesi/regioni» la selezione di tutti non si salva da automazione.
- Fattori di forma: c'è anche Android XR «Attivo» dal 29/09; non toccato.
- Account personale: per la produzione Google chiede almeno 12 tester per 14 giorni di test chiuso.
