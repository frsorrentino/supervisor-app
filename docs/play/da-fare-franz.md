# Supervisor da Franz a Pixelfarm: cosa resta a Franz (21/10/2026)

App «Supervisor», `com.francescosorrentino.cmaster`, oggi nell'account Play personale (6472649933491251229), id app
4974977762481831649, test chiuso in corso dal 13/10. Franz ha scelto il trasferimento a Pixelfarm (21/10 01:14).

## Le due strade (per memoria)

| | Trasferimento (scelto) | Pacchetto nuovo |
|---|---|---|
| Cosa passa | scheda, grafica, contenuti app, release, firma di Play | niente: app da rifare da zero |
| Codice | invariato | nuovo `applicationId`, nuova chiave di firma Play, nuova scheda |
| Tester del test chiuso | non passano: il gruppo `supervisor-testers@googlegroups.com` va riaggiunto | da reinstallare tutti |
| Tempi | dipende da Google (giorni) e serve l'ID transazione di Pixelfarm | subito |
| App nell'account personale | sparisce da lì | resta, da ritirare a mano |

Con un account organizzazione Pixelfarm cade anche il requisito dei 12 tester per 14 giorni che Google chiede agli
account personali prima della produzione: da verificare sul tipo di account di Pixelfarm.

## Passi, in ordine

1. **Contatti della scheda** — Play Console personale › Supervisor › Crescita › Impostazioni dello Store › «Dati di
   contatto scheda dello Store» › Modifica: email `info@pixelfarm.it`, sito `https://www.pixelfarm.it/`, telefono vuoto.
   La finestra ha solo «Salva e pubblica», quindi l'ho aperta, compilata e **annullata** senza salvare (21/10 02:00):
   tocca a te premerlo. Dopo il trasferimento restano giusti.
2. **Video per i servizi in primo piano** — gira 30-60 secondi (demo sul telefono: live in cuffia a schermo spento con
   la notifica e Stop; sull'orologio ▶ in una notifica), caricalo su YouTube non in elenco e mandami il link. Poi
   Contenuti app › Autorizzazioni per i servizi in primo piano › «Riproduzione di contenuti multimediali», testo pronto
   in `dichiarazioni-bozza.md` §7. È in ritardo: senza, Play blocca i prossimi aggiornamenti.
3. **Informativa privacy** — scegli se nominare Pixelfarm in `PRIVACY.md` (proposta in `dichiarazioni-bozza.md` §1) e se
   l'indirizzo resta su supervisor.francescosorrentino.com. Dimmi «ok privacy» e la aggiorno e la pubblico.
4. **Screenshot tablet** — guarda `screenshot/tablet-store/en/` (vedi `grafica.md`, nota sul token finto). Con il tuo ok
   li carico nella scheda principale (7" e 10"); la scheda si salva in bozza, senza inviare.
5. **Release di test interno** — l'AAB 0.3 (versionCode 31 telefono, 32 orologio) viene dalla CI del ramo
   `notte/play-pixelfarm`. Stato in `docs/notte/2026-10-21-play-pixelfarm.md`. Il test interno non ha tester: aggiungili
   tu se lo vuoi usare, io non tocco gli elenchi.
6. **Trasferimento** — quando hai l'ID transazione di Pixelfarm (la ricevuta della quota di registrazione dell'account
   sviluppatore Pixelfarm, in Google Payments): Play Console personale › Impostazioni › Trasferimento app, scegli
   Supervisor, account di destinazione e ID transazione; poi accetti dall'account Pixelfarm.
7. **Dopo il trasferimento** — nell'account Pixelfarm: riaggiungi il gruppo `supervisor-testers@googlegroups.com` ai due
   canali di test chiuso (telefono «Alpha» e «Wear closed testing»), controlla che Contenuti app sia completo e i paesi
   dei canali (il 13/10 la selezione di tutti non si salvava da automazione).

## Da decidere

- Testo «the author» / «l'autore» nella descrizione completa: lasciarlo o scrivere Pixelfarm (`grafica.md`).
- La vecchia app «Claude Master App» (`it.pixelbox.cmwatch`, test interno fermo al 26/09) nello stesso account: lasciarla,
  trasferirla o ritirarla.
- Il fattore di forma Android XR risulta attivo dal 29/09 (riepilogo del 13/10): tenerlo o toglierlo.
