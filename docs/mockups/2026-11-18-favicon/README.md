# Favicon del sito come avatar della sessione: mockup A e B (app-favicon-siti-18-11)

Da scegliere da Franz. Fatti sull'interfaccia vera: la web in demo (`?demo`) con `mock.js` iniettato, larga come un
telefono (412) e come un desktop (1280), notte e giorno. Telefono e web hanno lo stesso disegno delle righe.

La favicon prende il posto dell'icona di stato a sinistra della riga (Home, «Ti aspettano» e le altre) e dell'icona
accanto al nome nelle colonne della scrivania; il pallino colorato del progetto accanto al nome sparisce. Sta su un
fondo chiaro arrotondato, così si legge anche una favicon scura sul tema notte. Sotto restano i minuti.

- **A, pallino all'angolo**: la favicon intera e un pallino del colore dello stato in basso a destra (ambra: ti aspetta;
  blu: al lavoro; verde: ferma).
- **B, anello intorno**: un anello del colore dello stato intorno al riquadro; al lavoro l'anello è un arco che gira,
  come lo spinner di oggi.

Fallback: un progetto senza sito (qui `payments-api`) tiene l'icona di oggi, col punto di domanda o lo spinner.
Le favicon sono inventate (storefront, blog, admin-panel); nell'app verranno dal PC (`supervisor-favicon-siti-18-11`).
Niente sull'orologio.

| File | Cosa |
|---|---|
| `confronto-righe.png` | le righe di «Ti aspettano» ingrandite: A notte, A giorno (sopra), B notte, B giorno (sotto) |
| `A-telefono-night.png`, `A-telefono-day.png`, `B-telefono-…` | la home a 412 px, notte e giorno |
| `A-web-night.png`, `A-web-day.png`, `B-web-…` | la scrivania a 1280 px: colonne con l'avatar nel titolo, home a destra |
