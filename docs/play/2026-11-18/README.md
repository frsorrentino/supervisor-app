# Scheda Play di Supervisor, 18/11/2026 (app-play-aggiornamento-18-11)

Da approvare da Franz prima del caricamento. Versione: pre-release 0.2.2229 (`auto-2026-11-18-1132-14736b2d`), versionCode 41 telefono e 42 orologio; gli AAB vengono dalla run di CI dell'ultimo commit (artifact `cmwatch-apk`, tenuto 1 giorno).

Schermate prese dall'app vera (APK di release 0.2.2229) in modalità Demo su emulatore, tema scuro. Testi d'esempio della demo in inglese anche nelle schermate italiane, come nell'app.

## Immagini

| File | Misura | Cosa mostra |
|---|---|---|
| `grafica/grafica-in-evidenza-it.png` | 1024×500 | logo, nome, «Le sessioni di Claude Code, sul telefono e al polso.», home del telefono e orologio |
| `grafica/feature-graphic-en.png` | 1024×500 | la stessa in inglese |
| `telefono/it/1-home.png` … `5-notte.png` | 1080×2160 | home col tabellone e la master, sessione con «Per te», Rendiconti con la bozza chiesta, Utilizzo e limiti, Notte |
| `telefono/en/1-home.png` … `5-night.png` | 1080×2160 | le stesse in inglese |
| `tablet-10/it/1-scrivania.png`, `2-utilizzo.png` | 2560×1600 | la scrivania a colonne; Utilizzo e limiti |
| `tablet-10/en/1-desk.png`, `2-usage.png` | 2560×1600 | le stesse in inglese |
| `tablet-7/it/1-home.png`, `2-sessione.png` | 1200×1920 | home e sessione in verticale |
| `tablet-7/en/1-home.png`, `2-session.png` | 1200×1920 | le stesse in inglese |
| `orologio/it/1-sessioni.png` … `4-panoramica.png` | 454×454 | sessioni, una domanda, le opzioni, la quota |
| `orologio/en/1-sessions.png` … `4-overview.png` | 454×454 | sessioni, domanda, contesto della sessione con Rispondi, la quota |

Icona: invariata. `docs/play/icon-512.png` è già il logo attuale dell'app (anello ambra, `app-logo-ambra-01-11`).

`grafica/make_feature.py` rigenera la grafica in evidenza da una schermata del telefono e una dell'orologio.

## Testi, Italiano

**Titolo** (10/30): Supervisor

**Descrizione breve** (76/80): Rispondi alle sessioni di Claude Code da telefono e orologio. Non ufficiale.

**Descrizione lunga** (2354/4000):

```
App non ufficiale per Claude Code, non affiliata ad Anthropic.

Supervisor porta sul telefono, sul tablet e sull'orologio Wear OS tutte le sessioni di Claude Code che girano sul tuo computer, di tutti i tuoi account. Vedi chi ti aspetta, rispondi e dai lavoro nuovo senza tornare alla scrivania.

QUELLO CHE ASPETTA TE
• In cima alla home tre cifre: chi ti aspetta, chi lavora, gli esiti di oggi.
• Ti aspettano: una pagina per voce, dalla più vecchia, per rispondere di fila. Ogni voce dice da dove viene.
• Domande con le opzioni come tasti; le scelte rischiose chiedono una pressione lunga.
• Ok da dare, prove delle versioni nuove e decisioni, raccolte e in ordine.
• Notifiche su domande, esiti e fermi, con le azioni direttamente nella notifica.

SEGUIRE IL LAVORO
• Le sessioni aperte: chi lavora, su cosa, con quale strumento e da quanto.
• La conversazione di ogni sessione con i passi, il costo del turno e l'esito; i file toccati e le modifiche.
• Gli esiti di oggi e il resoconto della notte, progetto per progetto.
• I file che le sessioni ti consegnano: video, immagini, documenti, con anteprima e condivisione.

DARE LAVORO
• Scrivi a una sessione, allega foto e file, anche dalla condivisione di Android.
• Lancia una sessione nuova scegliendo account, progetto e primo messaggio.
• La master, la sessione che coordina le altre, sempre a portata in fondo alla home, con i prossimi passi come tasti.
• Messaggi a un'ora fissata e lavori da lasciare per la notte.

RISORSE
• Quota delle cinque ore e della settimana per ogni account, con il ritmo e l'ora in cui si azzera.
• Quanto è pieno il contesto di una sessione, modello e sforzo, carico del computer.

SU OGNI SCHERMO
• Tablet e schermi larghi: la home e le sessioni in colonne affiancate.
• Orologio: app, tile e complicazione con chi ti aspetta e la risposta rapida; modalità dal vivo con le cuffie.
• Widget per la schermata Home del telefono.
• Italiano e inglese; lettura ad alta voce di risposte ed esiti.

COSA SERVE
Il plugin gratuito supervisor per Claude Code sul computer e un progetto Firebase tuo, che il plugin crea. Tutto viaggia cifrato da un capo all'altro: né Google né l'autore possono leggerlo. Nessun account, nessuna pubblicità, nessuna statistica.

Vuoi solo dare un'occhiata? La modalità Demo mostra tutte le schermate con sessioni di esempio, senza computer.
```

## Testi, English

**Titolo** (10/30): Supervisor

**Descrizione breve** (70/80): Answer your Claude Code sessions from phone and watch. Unofficial app.

**Descrizione lunga** (2275/4000):

```
Unofficial app for Claude Code, not affiliated with Anthropic.

Supervisor brings every Claude Code session running on your computer, across all your accounts, to your phone, tablet and Wear OS watch. See who is waiting for you, answer and hand out new work without going back to your desk.

WHAT WAITS FOR YOU
• Three figures on top of the home: who waits for you, who is working, today's outcomes.
• Waiting for you: one page per item, oldest first, to answer them in a row. Each item says where it comes from.
• Questions with their options as buttons; risky choices ask for a long press.
• Approvals, tests of new versions and decisions, collected and in order.
• Notifications for questions, outcomes and stops, with actions right in the notification.

FOLLOW THE WORK
• Open sessions: who is working, on what, with which tool and for how long.
• Each session's conversation with its steps, the cost of the turn and the outcome; the files it touched and the changes.
• Today's outcomes and the night report, project by project.
• The files sessions deliver to you: videos, images, documents, with preview and sharing.

GIVE WORK
• Write to a session, attach photos and files, also from Android's share menu.
• Launch a new session by picking account, project and first message.
• The master, the session that coordinates the others, always at hand at the bottom of the home, with its next steps as buttons.
• Messages sent at a set time and jobs left for the night.

RESOURCES
• Five-hour and weekly quota of each account, with the pace and the time it resets.
• How full a session's context is, model and effort, the computer's load.

ON EVERY SCREEN
• Tablets and wide screens: the home and sessions in side-by-side columns.
• Watch: app, tile and complication with who waits for you and a quick answer; live mode with headphones.
• A widget for the phone's home screen.
• Italian and English; answers and outcomes read aloud.

WHAT YOU NEED
The free supervisor plugin for Claude Code on your computer and a Firebase project of your own, created by the plugin. Everything travels end-to-end encrypted: neither Google nor the author can read it. No account, no ads, no analytics.

Just want a look? Demo mode shows every screen with sample sessions, no computer needed.
```
