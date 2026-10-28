#!/usr/bin/env python3
"""Mockup 22/10: la pagina unica «Lavori» che fonde Registro, Recap e Impegni (ok di Franz dal telefono alle 07:47).
Dati veri del 22/10 alle 08:00: registro (supervisor task impegni --json), agenda.tsv, resoconto della notte 2026-10-22.
Telefono 412 px, PNG a 2x; cruscotto tablet 1280 px."""
import pathlib, subprocess

HERE = pathlib.Path(__file__).parent
CSS = """
:root{--bg:#000;--low:#1B1F26;--surf:#23272E;--high:#292F3A;--line:#2A2E35;--text:#F2F4F7;--text2:#B0B8C4;--icon:#A8C7FA;
--primary:#D3E3FD;--onp:#0A2050;--good:#65C581;--warn:#FFC46B;--bad:#F2B8B5;--run:#8BB4F7;--track:#2E3642;--night:#C9B6FF;--nightbg:#221B33;
--mono:'Roboto Mono',ui-monospace,monospace}
*{box-sizing:border-box;margin:0}
body{background:#120D1C;color:var(--text);font:15px/1.4 Roboto,system-ui,sans-serif;width:412px}
.screen{width:412px;min-height:915px;background:var(--bg);padding-bottom:28px;position:relative}
.tag{background:#2B1F45;color:#E9DDFF;font:600 11px/1.3 var(--mono);letter-spacing:.06em;padding:6px 12px}
.r1{display:flex;align-items:center;gap:12px;height:56px;padding:0 16px 0 18px;margin-top:8px}
.name{font-size:22px;font-weight:500}.cnt{color:var(--text2);font-size:15px}.sp{flex:1}.ic{color:var(--icon);font-size:22px}
.menu{width:24px;height:16px;border-top:2px solid var(--icon);border-bottom:2px solid var(--icon);position:relative}
.menu:after{content:"";position:absolute;left:0;right:0;top:5px;border-top:2px solid var(--icon)}
.sec{display:flex;align-items:center;gap:10px;margin:18px 20px 4px;font:500 12px/1 var(--mono);letter-spacing:.14em;color:var(--text2)}
.sec .ln{flex:1;border-top:1px solid var(--line)}.sec .sum{letter-spacing:0;font:13px/1 Roboto,sans-serif;color:var(--text2)}
.chev{font:16px/1 sans-serif;color:var(--icon);letter-spacing:0}
.expl{font-size:13.5px;color:var(--text2);margin:0 20px 10px;line-height:1.4}
.grp{font:600 11px/1 var(--mono);letter-spacing:.12em;margin:12px 20px 8px;display:flex;gap:8px;align-items:center}.grp i{flex:1;height:1px;background:currentColor;opacity:.3}
.card{margin:0 12px 8px;background:var(--low);border-radius:20px;padding:12px 14px 12px 16px}
.row{display:flex;gap:12px;align-items:center}.row .t{flex:1;min-width:0}
.t b{font-weight:500;font-size:15px;display:block;line-height:1.35}.t small{display:block;color:var(--text2);font-size:13px;margin-top:3px;line-height:1.35}
.t small em{font-style:normal;color:var(--text)}
.fill{background:var(--primary);color:var(--onp);border-radius:999px;padding:9px 16px;font-weight:500;font-size:14px;white-space:nowrap}
.tonal{background:rgba(168,199,250,.16);color:var(--primary);border-radius:999px;padding:8px 14px;font-weight:500;font-size:14px;white-space:nowrap}
.mark{width:18px;height:18px;flex:none;border:2px solid var(--text2)}.mark.p{border-radius:50%}.mark.a{border-radius:5px}.mark.w{border-radius:3px;transform:rotate(45deg) scale(.8)}
.mark.ok{border-color:var(--primary);background:rgba(211,227,253,.25)}
.fold{color:var(--text2);font-size:14px;padding:8px 20px 2px;display:flex;justify-content:space-between}.fold .ic{font-size:16px}
.ok{color:var(--good)}.no{color:var(--bad)}.run{color:var(--run)}.wa{color:var(--warn)}.pa{color:var(--text2)}
.state{font-size:12px;font-weight:600;border-radius:999px;padding:3px 9px;white-space:nowrap}
.st-run{background:#1E2D47;color:var(--run)}.st-pa{background:#2A2E35;color:var(--text2)}.st-wa{background:#3A3220;color:var(--warn)}
.axis{display:flex;margin:4px 16px 2px;font:11px/1 var(--mono);color:#8B929C}.axis span{flex:1;text-align:left}.axis span:last-child{text-align:right;color:var(--text)}
.track{position:relative;height:8px;border-radius:4px;background:var(--track);margin:10px 0 2px}
.track i{position:absolute;top:0;height:8px;border-radius:4px}
.track .sol{background:var(--run)}.track .dash{background:repeating-linear-gradient(90deg,#6B7483 0 6px,transparent 6px 10px)}
.track .nite{background:#3A2E58;top:-3px;height:14px;border-radius:3px;opacity:.9}
.track .now{width:2px;background:var(--text);top:-4px;height:16px;border-radius:0}
.key{font:11px/1.4 var(--mono);color:#8B929C;margin:0 20px 2px}
.night{background:var(--nightbg);border:1px solid #3A2E58}
.nt{display:flex;gap:12px;align-items:center}.moon{width:36px;height:36px;border-radius:50%;background:#3A2E58;display:grid;place-items:center;color:var(--night);font-size:18px;flex:none}
.day{display:flex;align-items:center;gap:8px;margin:12px 20px 6px;font:600 12px/1 var(--mono);letter-spacing:.12em;color:var(--text2)}.day i{flex:1;height:1px;background:var(--line)}
.done{margin:0 12px 6px;background:var(--low);border-radius:18px;padding:10px 14px 10px 14px;display:flex;gap:10px;align-items:flex-start}
.done .tm{font:12px/1.5 var(--mono);color:var(--text2);white-space:nowrap;margin-top:1px}.done .ck{color:var(--good);font-size:15px;margin-top:0}
.done .t b{font-size:14.5px}.done .t small{font-size:12.5px}
.proof{color:var(--icon)}
.foldline{position:absolute;left:0;right:0;top:915px;border-top:1px dashed #6A5A9A}
.foldline span{position:absolute;right:10px;top:-9px;background:#2B1F45;color:#E9DDFF;font:600 10px/1 var(--mono);padding:4px 8px;border-radius:999px;letter-spacing:.06em}
.ghost{opacity:.45;text-decoration:line-through}
.mi{display:flex;gap:14px;align-items:center;padding:12px 18px}.mi .ico{width:40px;height:40px;border-radius:12px;background:var(--surf);display:grid;place-items:center;color:var(--icon);font-size:18px;flex:none}
.mi .t b{font-size:16px}.mi.acc .ico{background:#1E2D47}
.arrow{color:var(--warn);font-size:13px;margin-left:auto;white-space:nowrap;font-weight:500}
.note{margin:12px 20px 0;color:#BCE4C7;font-size:13px;line-height:1.45;border-top:1px solid var(--line);padding-top:10px}
"""

def page(title, body, extra=""):
    return f"<!doctype html><meta charset=utf-8><style>{CSS}{extra}</style><div class=screen><div class=tag>{title}</div>{body}</div>"

# ---- I quattro blocchi, dati veri del 22/10 alle 08:00 ----
PER_TE = """
<div class=sec><span class=chev>▾</span>PER TE · 29<span class=ln></span></div>
<div class=expl>Solo quello che aspetta te: un ok da dare o una cosa da fare fuori dalla chat. Ogni riga ha il suo tasto.</div>
<div class=grp style="color:var(--primary)">OK DA DARE · 1<i></i></div>
<div class=card><div class=row><span class="mark a ok"></span><div class=t><b>fable-director: push su origin/main del lavoro della notte</b><small>chiede la <em>master</em> alle 05:53 · correzione del doppio conteggio: i totali di token del plugin si dimezzano</small></div><span class=fill>Approva</span></div></div>
<div class=grp style="color:var(--warn)">DA FARE FUORI DALLA CHAT · 18<i></i></div>
<div class=card><div class=row><span class="mark p"></span><div class=t><b>Revocare la chiave API Anthropic trovata in chiaro</b><small>console.anthropic.com → API Keys · personale</small></div><span class=tonal>Fatto</span></div></div>
<div class=card><div class=row><span class="mark a"></span><div class=t><b>Albo Chiaro: la casella Aruba albo@, con utente e password</b><small>sblocca <em>albo-crotone</em>: 5 compiti in pausa dal 18/10</small></div><span class=tonal>Fatto</span></div></div>
<div class=fold><span>Altre 16 dall'agenda</span><span class=ic>›</span></div>
<div class=grp style="color:var(--run)">PUÒ FARLO CLAUDE · 10<i></i></div>
<div class=card><div class=row><span class="mark a"></span><div class=t><b>Migrare a pixel i 4 siti di produzione con idemedia attivo</b><small>alfa.example beta.example gamma.example delta.example</small></div><span class=tonal>Avvia</span></div></div>
<div class=fold><span>Altre 9 dall'agenda</span><span class=ic>›</span></div>
"""

GANTT = """
<div class=axis><span>16</span><span>17</span><span>18</span><span>19</span><span>20</span><span>21</span><span>22</span><span>oggi</span></div>
<div class=key>barra piena = al lavoro · tratteggio = in pausa · banda viola = la notte · riga bianca = adesso (08:00)</div>
"""

def bar(sol=None, dash=None):
    s = '<div class=track><i class=nite style="left:86%;width:3.6%"></i>'
    if sol: s += f'<i class=sol style="left:{sol[0]}%;width:{sol[1]}%"></i>'
    if dash: s += f'<i class=dash style="left:{dash[0]}%;width:{dash[1]}%"></i>'
    return s + '<i class=now style="left:90.5%"></i></div>'

IN_CORSO = """
<div class=sec><span class=chev>▾</span>IN CORSO · 10<span class=ln></span></div>
<div class=expl>Un compito per riga: cosa, chi, da quando, il prossimo passo. La barra è il tempo dall'ok a adesso, come nel Gantt della Notte.</div>
""" + GANTT + f"""
<div class=card><div class=row><div class=t><b>Mockup della pagina unica «Lavori»</b><small><em>supervisor-app</em> · ok alle 07:48 · prossimo: supervisor-app lavora, chiude con task done</small></div><span class="state st-run">▶ al lavoro</span></div>{bar(sol=(90.3, 0.5))}</div>
<div class=card><div class=row><div class=t><b>Albo Chiaro fase 1 (task 1-8): repository e CI, verifiche, database, lettore orario, area riservata e deploy</b><small><em>albo-crotone</em> · ok il 17/10 alle 22:05 · in pausa dal 18/10: aspetta da te la casella Aruba</small></div><span class="state st-pa">⏸ in pausa</span></div>{bar(sol=(27.4, 1.2), dash=(28.6, 61.9))}</div>
<div class=card><div class=row><div class=t><b>Albo Chiaro fase 2 (task 9-15): testo, OCR, dati personali, IA Gemini, lotti, pagine pubbliche</b><small><em>albo-crotone</em> · ok il 17/10 · parte dopo la fase 1</small></div><span class="state st-pa">⏸ in pausa</span></div>{bar(dash=(27.4, 63.1))}</div>
<div class=fold><span>Altri 4 in pausa: Albo fase 3, Provincia, Regione · App telefono del 03/10</span><span class=ic>›</span></div>
<div class=fold><span>3 fermi su altri: Chrome Web Store, dev.alfa.example, chrome-bridge #2</span><span class=ic>›</span></div>
"""

STANOTTE = """
<div class=sec><span class=chev>▾</span>STANOTTE<span class=ln></span></div>
<div class=expl>La coda per stanotte. Sta solo qui: non più anche nel Registro.</div>
<div class="card night"><div class=nt><div class=moon>☾</div><div class=t style="flex:1"><b>Nessun lavoro in coda</b><small>Dal menu Azioni di una scheda, o da qui</small></div><span class=fill>Aggiungi</span></div></div>
<div class=fold><span>La notte scorsa: 5 progetti, 1 fatto, 3 in parte, 1 fermo · 00:25–06:30</span><span class=ic>›</span></div>
"""

FATTO = """
<div class=sec><span class=chev>▾</span>FATTO<span class=ln></span></div>
<div class=expl>Per giorno, con la prova accanto: un link, un commit, una release. Oggi aperto, i giorni prima chiusi.</div>
<div class=day>OGGI 22/10 · 7<i></i></div>
<div class=done><span class=tm>07:45</span><div class=t><b>App del telefono: Impegni, Approva e aggiornamento dentro l'app</b><small>supervisor-app · <span class=proof>release impegni-2026-10-22-b</span>, installata sul telefono</small></div></div>
<div class=done><span class=tm>06:40</span><div class=t><b>Bluey: pubblicazioni automatiche fino alle 04:00</b><small>win:bluey · <span class=proof>origin/main 1c04bb2</span></small></div></div>
<div class=done><span class=tm>02:29</span><div class=t><b>claude-observe: proposta per l'evento safety_event</b><small>lavoro della notte · <span class=proof>docs/notte/2026-10-22-36846617-….md</span></small></div></div>
<div class=done><span class=tm>02:17</span><div class=t><b>fable-director: misure sui subagenti dopo Claude Code 2.1.307</b><small>lavoro della notte · <span class=proof>docs/notte/2026-10-22-cc-2.1.307.md</span></small></div></div>
<div class=done><span class=tm>01:29</span><div class=t><b>Relay 0.8.41: filtro api-request e motivo dei turni falliti</b><small>supervisor · <span class=proof>release v0.8.41</span></small></div></div>
<div class=done><span class=tm>00:52</span><div class=t><b>Piano Agent SDK per l'app</b><small>supervisor-app · <span class=proof>commit b0129a75</span></small></div></div>
<div class=done><span class=tm>00:19</span><div class=t><b>Relay 0.8.40: impegni sempre visibili, contratto 1.76</b><small>supervisor · <span class=proof>release v0.8.40</span></small></div></div>
<div class=fold><span>IERI 21/10</span><span class=ic>›</span></div>
<div class=fold><span>MARTEDÌ 20/10</span><span class=ic>›</span></div>
<div class=note>La quota non è qui: resta in «Utilizzo e limiti».</div>
"""

PAGE = page("PROPOSTA · PAGINA «LAVORI» (= REGISTRO + RECAP + IMPEGNI) · la riga tratteggiata è la fine del primo schermo", """
<div class=r1><span class=ic>←</span><span class=name>Lavori</span><span class=cnt>29 per te</span><span class=sp></span><span class=menu></span></div>
<div class=foldline></div>
""" + PER_TE + IN_CORSO + STANOTTE + FATTO, ".screen{min-height:2560px}")

HOME = page("PROPOSTA · HOME: UNA SOLA SEZIONE «LAVORI»", """
<div class=r1><span class=name>Master</span><span class=cnt>6 aperte</span><span class=sp></span><span class=menu></span></div>
<div class=sec><span class=chev>▸</span>NOTTE<span class=ln></span><span class=sum>5 progetti · 1 fatto · 3 in parte · 1 fermo</span></div>
<div class=sec><span class=chev>▾</span>LAVORI<span class=ln></span><span class=sum>29 per te · 10 in corso</span></div>
<div class=expl>Al posto di «Da approvare», «Impegni» e «Recap»: qui solo le righe con un tasto, il resto nella pagina.</div>
<div class=card><div class=row><span class="mark a ok"></span><div class=t><b>fable-director: push su origin/main del lavoro della notte</b><small>chiede la <em>master</em> alle 05:53</small></div><span class=fill>Approva</span></div></div>
<div class=card><div class=row><span class="mark p"></span><div class=t><b>Revocare la chiave API Anthropic trovata in chiaro</b><small>console.anthropic.com → API Keys</small></div><span class=tonal>Fatto</span></div></div>
<div class=card><div class=row><span class="mark a"></span><div class=t><b>Albo Chiaro: la casella Aruba albo@, con utente e password</b><small>sblocca <em>albo-crotone</em>: 5 compiti in pausa dal 18/10</small></div><span class=tonal>Fatto</span></div></div>
<div class=fold><span>Tutti i lavori · in corso 10 · stanotte vuota · fatto oggi 7</span><span class=ic>›</span></div>
<div class=sec><span class=chev>▾</span>SESSIONI · 6<span class=ln></span></div>
<div class=card><div class=row><span style="width:26px;height:26px;border-radius:50%;background:#3B82F6;flex:none"></span><div class=t><b>supervisor-app</b><small>mockup della pagina «Lavori» · al lavoro</small></div></div></div>
<div class=card><div class=row><span style="width:26px;height:26px;border-radius:50%;background:#8B929C;flex:none"></span><div class=t><b>finanza</b><small>ferma da 14 ore</small></div></div></div>
<div class=card><div class=row><span style="width:26px;height:26px;border-radius:50%;background:#8B929C;flex:none"></span><div class=t><b>shop-example-it</b><small>ferma da 11 ore</small></div></div></div>
<div class=sec><span class=chev>▸</span>UTILIZZO<span class=ln></span><span class=sum>personale 5 h 2 % · settimana 0 %</span></div>
""")

MENU = page("PROPOSTA · MENU ≡: «LAVORI» AL POSTO DI REGISTRO E RECAP", """
<div class=r1><span class=name>Vai a</span><span class=sp></span><span class=ic>✕</span></div>
<div class="mi acc"><span class=ico>🚀</span><div class=t><b>Lancia una sessione</b><small>Un progetto, con il primo messaggio</small></div></div>
<div class="mi"><span class=ico>☰</span><div class=t><b>Lavori</b><small>Per te, in corso, stanotte, fatto</small></div><span class=arrow>nuova</span></div>
<div class="mi ghost"><span class=ico>📖</span><div class=t><b>Registro</b><small>Stanotte, i giorni, la coda della notte</small></div><span class=arrow>→ Lavori</span></div>
<div class="mi ghost"><span class=ico>☑</span><div class=t><b>Recap</b><small>Azioni, chi deve muoversi, fatto e sospeso</small></div><span class=arrow>→ Lavori</span></div>
<div class="mi"><span class=ico>☾</span><div class=t><b>Notte</b><small>Il rapporto di stanotte e le notti prima (la coda sta in Lavori)</small></div></div>
<div class="mi"><span class=ico>◔</span><div class=t><b>Utilizzo e limiti</b><small>Finestre di 5 ore e settimana, ritmo: la quota sta solo qui</small></div></div>
<div class="mi"><span class=ico>⌕</span><div class=t><b>Cerca nelle conversazioni</b><small>Messaggi, esiti e lavori</small></div></div>
<div class=note>Nella home la sezione «Impegni» (e «Da approvare», «Recap») diventa «Lavori»: solo le righe con un tasto, il resto nella pagina. La pagina Notte resta: rapporto e Gantt; la coda di stanotte si gestisce da Lavori.</div>
""")

TABLET = f"""<!doctype html><meta charset=utf-8><style>{CSS}
body{{width:1280px;background:var(--bg)}}.screen{{width:1280px;min-height:1360px;padding:0 0 20px}}
.cols{{display:grid;grid-template-columns:1fr 1fr;gap:0 24px;padding:0 12px}}
.col{{min-width:0}}.r1{{padding:0 24px}}
</style><div class=screen><div class=tag>PROPOSTA · CRUSCOTTO TABLET: «LAVORI» IN DUE COLONNE (stesso contenuto del telefono)</div>
<div class=r1><span class=name>Lavori</span><span class=cnt>29 per te · 10 in corso · stanotte vuota · fatto oggi 7</span><span class=sp></span><span class=menu></span></div>
<div class=cols><div class=col>{PER_TE}{STANOTTE}</div><div class=col>{IN_CORSO}{FATTO}</div></div></div>"""

def render(name, html, size):
    f = HERE / (name + ".html")
    f.write_text(html, encoding="utf-8")
    subprocess.run(["timeout", "90", "nice", "-n", "19", "chromium", "--headless=new", "--no-sandbox", "--virtual-time-budget=4000", "--hide-scrollbars",
                    "--force-device-scale-factor=2", f"--window-size={size}", f"--screenshot={HERE / (name + '.png')}", f.as_uri()], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

if __name__ == "__main__":
    render("lavori-telefono", PAGE, "412,2560")
    render("home", HOME, "412,915")
    render("menu", MENU, "412,915")
    render("lavori-tablet", TABLET, "1280,1360")
    print("ok")
