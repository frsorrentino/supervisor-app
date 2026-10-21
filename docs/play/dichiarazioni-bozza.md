# Supervisor: bozze delle dichiarazioni Play da firmare (21/10/2026)

Stato in Console al 21/10 02:10 (account personale, app «Supervisor»): tutte le dichiarazioni di Contenuti app sono
complete dal 29/09 e dal 13/10, tranne **Autorizzazioni per i servizi in primo piano**, in ritardo. Il trasferimento
porta con sé le dichiarazioni; dopo il trasferimento vanno riviste solo le voci che nominano lo sviluppatore.

## 1. Informativa privacy

Oggi: `PRIVACY.md` → https://supervisor.francescosorrentino.com/privacy, sviluppatore Francesco Sorrentino, contatto
con le issue di GitHub. Dopo il trasferimento lo sviluppatore su Play è Pixelfarm: l'informativa deve nominarlo.
Proposta di modifica a `PRIVACY.md` (da applicare solo quando Franz la approva, poi `gen_privacy_html.py` e il deploy
della web app):

- Sotto il titolo, una riga nuova:
  > Supervisor is published on Google Play by **Pixelfarm** (<https://www.pixelfarm.it/>, info@pixelfarm.it).
- «Changes and contact», ultima frase: «Questions: info@pixelfarm.it, or an issue at
  <https://github.com/frsorrentino/supervisor-app/issues>.»
- Il resto non cambia: Pixelfarm non riceve dati, perché l'app parla solo con il Firebase dell'utente.

Decisione di Franz: l'indirizzo dell'informativa. Può restare su supervisor.francescosorrentino.com (funziona e non
serve altro) o andare su una pagina di pixelfarm.it; in Console il campo è «Norme sulla privacy» in Contenuti app.

## 2. Sicurezza dei dati (invariata dal 13/10)

- Raccolta: solo «ID dispositivo o altri ID» (id anonimo di Firebase Authentication, id di installazione di Firebase,
  token di Firebase Cloud Messaging), scopo «Funzionalità dell'app», obbligatoria, non condivisa.
- Il contenuto delle sessioni non è dichiarato come raccolto: viaggia cifrato da un capo all'altro (AES-256-GCM) verso
  il progetto Firebase dell'utente; né Pixelfarm né Google possono leggerlo.
- Dati cifrati in transito: sì. Account da creare: nessuno. Richiesta di eliminazione: indirizzo dell'informativa
  (sezione «Deleting your data»).
- Con la voce bilingue (ramo `notte/voce-bilingue`) non cambia niente: la lingua si riconosce sul dispositivo, senza
  rete.

## 3. Classificazione dei contenuti

Questionario IARC del 29/09, invariato (le risposte sono in Console, non le ho riaperte). L'app non ha contenuti
generati da altri utenti: si scrive solo alle proprie sessioni. Si rifà solo se cambia la natura dell'app.

## 4. Pubblico di destinazione

18 anni o più. Motivo: strumento per sviluppatori che usano Claude Code con il loro account; non è pensata per minori.

## 5. Annunci

Nessuna pubblicità (dichiarazione del 29/09). Nessun SDK pubblicitario nel codice.

## 6. Accesso per i revisori

«Sì, alcune funzionalità sono limitate» (l'app si accoppia con il QR del computer). Istruzioni già salvate il 13/10,
da tenere:

> No account needed. Tap «Try the demo» on the first screen of the phone or the watch, or open Settings › Demo mode.
> The demo shows every screen with invented sessions and stays on the device.

## 7. Autorizzazioni per i servizi in primo piano (da completare)

Tipo: **Riproduzione di contenuti multimediali** (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`). Testo proposto:

> Supervisor reads text aloud with the device's text-to-speech engine. On the phone, Live mode reads the updates of the
> user's Claude Code sessions in the headphones while the screen is off; on the watch, a tap on ▶ in a notification
> reads the reply aloud. Speech starts only on a user action, shows an ongoing notification with a Stop button and ends
> by itself when the text is read. Without a foreground service the system would stop the audio as soon as the screen
> turns off.

Serve un **link a un video** (YouTube non in elenco va bene) che mostri: telefono in modalità Demo, tocco su «Live» con
le cuffie, schermo spento, la voce che legge, la notifica con Stop; poi sull'orologio una notifica con ▶ e la lettura.
30-60 secondi bastano. Il 21/10 ho aperto il modulo: scelto il tipo, chiede il «Link video»; non ho salvato niente.

## 8. App governative, finanziarie, sanitarie, notizie

«No» a tutte (29/09), invariate.
