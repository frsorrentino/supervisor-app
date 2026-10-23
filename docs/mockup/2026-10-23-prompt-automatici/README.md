# Prompt automatici nella chat della master: prima e dopo (23/10/2026)

Segnalazione di Franz delle 20:38 (via master): il prompt del cron «Regia di Bluey ad oltranza» (ogni 20 minuti) compariva nella chat della master come un fumetto utente di venti righe, con sotto quasi sempre la riga «Esito: regia di Bluey, nessuna azione». Franz: «possiamo dare una gerarchia migliore a questo tipo di messaggio? mi mostri come potrebbe venire?».

Tela con le quattro tavole e la nota delle regole: https://claude.ai/artifact/X6gprTvjkZg5xB7xAmqbAR (artefatto privato dell'account di Franz). Qui i PNG a grandezza reale, resi con chromium headless a scala 2: `telefono-prima.png` e `telefono-dopo.png` (390×844), `web-prima.png` e `web-dopo.png` (960×720).

Il «dopo»:

1. Un prompt automatico (cron, regia, promemoria) è una riga di sistema: orologio, nome breve, ora, esito in un chip. Niente fumetto utente.
2. Il testo completo del prompt si apre al tocco della riga, sotto, in grigio, con l'esito.
3. Giri consecutivi senza azione si raggruppano in una riga sola: «Regia di Bluey · 4 controlli · 19:16–20:16 · nessuna azione».
4. Un giro con un'azione resta visibile da solo: chip ambra con l'azione («rilanciata») e la risposta della master sotto, come un turno normale.
5. I messaggi di Franz e le risposte della master non cambiano.

Per l'implementazione, dopo l'ok di Franz: oggi `origin` delle voci della trascrizione vale pc/watch/phone/web; i prompt del cron devono arrivare dal relay con un'origine propria (per esempio `origin: "cron"` e il nome breve della regia), altrimenti l'app non sa quali fumetti compattare. Richiesta di contratto da fare quando si parte. Solo il mockup per ora.
