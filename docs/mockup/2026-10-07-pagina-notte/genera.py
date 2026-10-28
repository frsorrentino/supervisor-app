#!/usr/bin/env python3
"""Bozza della pagina «Notte» (07/10/2026): in alto quello che serve a Franz, poi una card per ogni lavoro o sessione
della notte con la sua barra sull'asse della finestra, poi i progetti. Al tocco la card si apre sul posto; dalla card
aperta si va alla conversazione. Dati veri del rapporto `~/.claude-master/night-report/2026-10-07.json` (generato alle
02:36, quindi a notte non finita). 412 px come il telefono, PNG a 2x con chromium headless; colori di web/src/app.css."""
import pathlib
import subprocess

HERE = pathlib.Path(__file__).parent

CSS = """
:root{--bg:#000;--low:#1B1F26;--surf:#23272E;--high:#292F3A;--line:#2A2E35;--text:#F2F4F7;--text2:#B0B8C4;--icon:#A8C7FA;
--primary:#D3E3FD;--onp:#0A2050;--good:#65C581;--warn:#FFC46B;--bad:#F2B8B5;--run:#8BB4F7;--track:#455165;--ask:#C58AF9}
*{box-sizing:border-box;margin:0}
body{background:#120D1C;color:var(--text);font:15px/1.4 Roboto,system-ui,sans-serif;width:412px}
.screen{position:relative;width:412px;min-height:915px;background:var(--bg);padding-bottom:24px}
.tag{background:#2B1F45;color:#E9DDFF;font:600 11px/1.3 "Roboto Mono",monospace;letter-spacing:.06em;padding:6px 12px}
svg.i{width:24px;height:24px;flex:none}
.r1{display:flex;align-items:center;gap:10px;height:56px;padding:0 12px 0 8px;margin-top:8px}
.back{width:40px;display:grid;place-items:center}
.name{font-size:22px;font-weight:500}
.sp{flex:1}
.sub{color:var(--text2);font-size:14px;padding:0 20px 6px 58px}
.grp{font:500 12px/1 "Roboto Mono",monospace;letter-spacing:.14em;color:var(--text2);margin:22px 20px 10px}
.need{margin:0 12px;background:var(--surf);border-radius:24px;padding:14px 16px;display:flex;gap:14px;align-items:center}
.circ{width:40px;height:40px;border-radius:50%;display:grid;place-items:center;background:var(--high);flex:none}
.tx{flex:1;min-width:0}
.t{font-size:16px;font-weight:500}
.s{font-size:13.5px;color:var(--text2);margin-top:2px;line-height:1.35}
.fill{background:var(--primary);color:var(--onp);border-radius:999px;padding:10px 18px;font-weight:500;font-size:14.5px;white-space:nowrap}
.tonal{display:inline-flex;align-items:center;gap:8px;background:rgba(168,199,250,.16);color:var(--primary);border-radius:999px;padding:9px 16px;font-weight:500;font-size:14.5px;white-space:nowrap}
.axis{position:relative;height:18px;margin:0 40px 6px 78px;font:500 11px/1 "Roboto Mono",monospace;color:var(--text2)}
.axis span{position:absolute;transform:translateX(-50%)}
.card{margin:0 12px 8px;background:var(--low);border-radius:20px;padding:12px 14px 12px 12px}
.card.open{background:var(--surf);border-radius:28px;padding:14px 14px 16px 12px}
.head{display:flex;gap:14px;align-items:flex-start}
.row1{display:flex;align-items:baseline;gap:8px}
.row1 .t{flex:1;min-width:0}
.when{font-size:13px;color:var(--text2);white-space:nowrap}
.one{font-size:13.5px;color:var(--text2);margin-top:2px}
.when2{font-size:13px;color:var(--text2);margin-top:1px}
.track{position:relative;height:6px;border-radius:3px;background:var(--high);margin:10px 14px 0 54px}
.bar{position:absolute;top:0;bottom:0;border-radius:3px}
.chev{margin-top:8px}
.kind{font:600 10.5px/1.4 "Roboto Mono",monospace;letter-spacing:.08em;border-radius:999px;padding:2px 8px;margin-left:2px}
.body{margin:12px 2px 0 54px;font-size:14.5px;line-height:1.45}
.chips{display:flex;gap:6px;flex-wrap:wrap;margin:12px 0 0 54px}
.chip{background:var(--high);border-radius:8px;padding:5px 10px;font-size:13px;color:var(--text2)}
.ev{margin:12px 0 0 54px;border-left:2px solid var(--line);padding-left:12px}
.ev div{font-size:13.5px;line-height:1.35;margin:0 0 8px}
.ev b{font:500 12px "Roboto Mono",monospace;color:var(--text2);margin-right:6px}
.acts{display:flex;gap:8px;margin:14px 0 0 54px;flex-wrap:wrap}
.proj{margin:0 12px 8px;background:var(--low);border-radius:20px;padding:14px 16px}
.pbar{display:flex;gap:3px;margin:10px 0 6px}
.pbar i{flex:1;height:8px;border-radius:2px;background:var(--high)}
.legend{background:#120D1C;color:#E9DDFF;font-size:12.5px;line-height:1.4;padding:10px 14px 14px;border-top:1px solid #3A2C5C}
"""

I = {
    'back': '<path d="M15 6l-6 6 6 6" fill="none" stroke="#F2F4F7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
    'ok': '<path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="#65C581" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>',
    'stop': '<path d="M6 6l12 12M18 6 6 18" stroke="#F2B8B5" stroke-width="2.4" stroke-linecap="round"/>',
    'run': '<path d="M8 5.5v13l10.5-6.5z" fill="#8BB4F7"/>',
    'ask': '<path d="M9.2 9a3 3 0 1 1 4.3 2.7c-.9.5-1.5 1.2-1.5 2.3M12 18h.01" fill="none" stroke="#C58AF9" stroke-width="2.4" stroke-linecap="round"/>',
    'down': '<path d="M7 10l5 5 5-5" fill="none" stroke="#B0B8C4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
    'up': '<path d="M7 14l5-5 5 5" fill="none" stroke="#B0B8C4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
    'chat': '<path d="M4 5h16v11H8l-4 4z" fill="none" stroke="#D3E3FD" stroke-width="2" stroke-linejoin="round"/>',
    'doc': '<path d="M7 3h7l4 4v14H7zM14 3v4h4M9.5 12h6M9.5 16h6" fill="none" stroke="#D3E3FD" stroke-width="2" stroke-linejoin="round"/>',
    'refresh': '<path d="M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5" fill="none" stroke="#A8C7FA" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>',
}
COL = {'ok': '#65C581', 'stop': '#F2B8B5', 'run': '#8BB4F7', 'ask': '#C58AF9'}


def ic(name, size=24):
    return f'<svg class="i" viewBox="0 0 24 24" style="width:{size}px;height:{size}px">{I[name]}</svg>'


# finestra 00:33-02:36; l'asse va da 00:30 a 02:40 (130 minuti)
A0, SPAN = 30, 130


def m(hhmm):
    h, mm = hhmm.split(':')
    return int(h) * 60 + int(mm)


def bar(start, end, state):
    left = (m(start) - A0) / SPAN * 100
    right = ((m(end) if end else A0 + SPAN) - A0) / SPAN * 100
    width = max(right - left, 1.5)
    style = f'left:{left:.1f}%;width:{width:.1f}%;background:{COL[state]}'
    if not end:
        style += ';background:linear-gradient(90deg,#8BB4F7 70%,rgba(139,180,250,.15))'
    return f'<div class="track"><div class="bar" style="{style}"></div></div>'


def kind(job):
    if job:
        return '<span class="kind" style="color:#E9DDFF;background:rgba(197,138,249,.18)">CODA</span>'
    return ''


def card(state, title, start, end, dur, line, job=False):
    when = f'{start}–{end}' if end else f'dalle {start}'
    return f'''<div class="card"><div class="head"><span class="circ">{ic(state, 22)}</span><span class="tx">
 <div class="t">{title}{kind(job)}</div><div class="when2">{when} · {dur}</div>
 <div class="one">{line}</div></span><span class="chev">{ic('down', 22)}</span></div>{bar(start, end, state)}</div>'''


AXIS = '<div class="axis">' + ''.join(
    f'<span style="left:{(t - A0) / SPAN * 100:.1f}%">{t // 60:02d}:{t % 60:02d}</span>' for t in (30, 60, 90, 120, 150)) + '</div>'

TOP = f'''<div class="r1"><span class="back">{ic('back')}</span><span class="name">Notte del 6-7 ottobre</span><span class="sp"></span>
{ic('refresh')}</div><div class="sub">Dalle 00:33, ultimo tuo messaggio, alle 02:36 · 2 ore e 3 minuti</div>'''

NEED = f'''<div class="grp">SERVE A TE</div>
<div class="need"><span class="circ">{ic('ask', 22)}</span><span class="tx"><div class="t">fiscale</div>
<div class="s">Ferma: chiede «/clear» per ripartire</div></span><span class="fill">Manda /clear</span></div>'''

CARDS_CLOSED = [
    card('run', 'master', '00:33', None, 'in corso', 'Chiudo ora, prosegui in autonomia e segui tutte le sessioni al mio posto'),
    card('ok', 'claude-master', '00:36', '00:56', '20 min', 'Lavoro di stanotte committato in locale, nessuna release'),
    card('ok', 'claude-master-phone', '00:36', '01:40', '1 h 4 min', 'Handoff scritto e annunciato alla master'),
    card('ok', 'claude-master-notte', '00:51', '02:11', '1 h 20 min', 'chore(night-report): keep personal names out of the public code'),
    card('ok', 'claude-observe', '00:55', '01:04', '9 min', 'Deploy di report.php con team-supervisor in OBSERVE_REPOS'),
    card('ok', 'claude-master-phone-notte', '01:06', '02:01', '55 min', 'Blocco segnalato alla master ed esito scritto'),
    card('stop', 'Nucleo live, prima esecuzione', '02:00', '02:01', '1 min', 'Fermo prima del piano: non legge la specifica', job=True),
    card('run', 'Dati della pagina Notte', '02:01', None, 'in corso', 'claude-master-notte', job=True),
]

PROJ = '''<div class="grp">PROGETTI</div>
<div class="proj"><div class="row1"><span class="t">claude-master</span><span class="when">11 di 13 parti</span></div>
<div class="pbar">''' + '<i style="background:#65C581"></i>' * 11 + '<i style="background:#8BB4F7"></i>' * 2 + '''</div>
<div class="s">In corso: close sulle sessioni attaccate; rinomino in team-supervisor</div>
<div class="s">Prossimo: release 0.6.10 pubblicata, permessi win aggiornati</div></div>
<div class="proj"><div class="row1"><span class="t">fiscale</span><span class="when">nessuna parte</span></div>
<div class="s" style="margin-top:6px">Aspetta: /clear · Prossimo: riscontro AdER sul fermo targa</div></div>'''

OPEN_JOB = f'''<div class="card open"><div class="head"><span class="circ">{ic('stop', 22)}</span><span class="tx">
 <div class="t">Nucleo live, prima esecuzione{kind(True)}</div>
 <div class="when2">02:00–02:01 · 1 min</div><div class="one">claude-master-phone-notte</div></span><span class="chev">{ic('up', 22)}</span></div>{bar('02:00', '02:01', 'stop')}
 <div class="body">Sono fermo prima del piano e non ho scritto codice: non riesco a leggere né la specifica né la bozza della
 modalità live. Le fonti stanno fuori dal worktree e i permessi della notte ne negano la lettura.</div>
 <div class="chips"><span class="chip">0 commit</span><span class="chip">0 test</span><span class="chip">1 prompt</span></div>
 <div class="ev"><div><b>02:00</b>Partito dalla coda della notte</div><div><b>02:01</b>Blocco segnalato alla master</div>
 <div><b>02:03</b>Rimesso in coda come seconda esecuzione</div></div>
 <div class="acts"><span class="tonal">{ic('chat', 20)}Conversazione</span><span class="tonal">{ic('doc', 20)}Rapporto</span></div></div>'''

screens = {
    '1-pagina': (
        'BOZZA · PAGINA NOTTE, CARD CHIUSE',
        TOP + NEED + '<div class="grp">LAVORI E SESSIONI</div>' + AXIS + ''.join(CARDS_CLOSED) + PROJ,
        'In alto quello che serve a te, con i tasti che esistono già (un solo tasto pieno). Poi una card per lavoro o '
        'sessione, in ordine di inizio: icona dell\'esito, nome, orari e durata, una riga di esito; la barra sotto sta '
        'sull\'asse della finestra, così si vede cosa girava insieme. «CODA» segna i lavori della coda notturna. Dati veri '
        'del rapporto delle 02:36; «master» e «claude-master-notte» uniscono doppioni del rapporto.'),
    '2-card-aperta': (
        'BOZZA · UNA CARD APERTA AL TOCCO',
        TOP + '<div class="grp">LAVORI E SESSIONI</div>' + AXIS + ''.join(CARDS_CLOSED[4:6]) + OPEN_JOB
        + CARDS_CLOSED[7],
        'Il tocco apre la card sul posto (e chiude quella aperta prima): esito intero, conteggi, i passi della notte con '
        'l\'ora. «Conversazione» va alla trascrizione della sessione, in sola lettura se è chiusa, alla chat se è '
        'viva. «Rapporto» c\'è solo per i lavori della coda. L\'esito «fermo» qui è corretto a mano: il rapporto di oggi '
        'lo segna ok.'),
}

for name, (tag, content, legend) in screens.items():
    html = (f'<!doctype html><html lang="it"><head><meta charset="utf-8"><meta name="viewport" content="width=412">'
            f'<style>{CSS}</style></head><body><div class="screen"><div class="tag">{tag}</div>{content}</div>'
            f'<div class="legend">{legend}</div></body></html>')
    (HERE / f'{name}.html').write_text(html, encoding='utf-8')
    subprocess.run(['chromium', '--headless=new', '--no-sandbox', '--hide-scrollbars', '--force-device-scale-factor=2',
                    '--window-size=412,1900', '--virtual-time-budget=3000', f'--screenshot={HERE / (name + ".png")}',
                    (HERE / f'{name}.html').as_uri()], check=True, capture_output=True, timeout=90)
    print(name)
