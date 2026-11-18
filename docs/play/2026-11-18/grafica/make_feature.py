# python make.py <lang> <phone.png> <watch.png> <out.html>
import sys
lang, phone, watch, out = sys.argv[1:5]
T = {"it": ("Le sessioni di Claude Code,", "sul telefono e al polso."),
     "en": ("Your Claude Code sessions,", "on your phone and wrist.")}[lang]
icon = '''<svg viewBox="27 27 54 54" width="112" height="112">
<path stroke="#3A404C" stroke-width="5.7" stroke-linecap="round" fill="none" d="M36.548,74.798 A27.150,27.150 0 1 1 71.452,74.798"/>
<path stroke="#FFB23F" stroke-width="5.7" stroke-linecap="round" fill="none" d="M36.548,74.798 A27.150,27.150 0 1 1 76.508,38.818"/>
<path stroke="#FFB23F" stroke-width="3.617" stroke-linecap="round" stroke-linejoin="round" fill="none" d="M42.949,44.065 L52.493,52.605 L42.949,61.144"/>
<path stroke="#FFB23F" stroke-width="3.617" stroke-linecap="round" fill="none" d="M56.813,61.144 L63.745,61.144"/></svg>'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{{font-family:B;src:url(v3_barlow_sc_semibold.ttf)}} @font-face{{font-family:R;src:url(v3_barlow_regular.ttf)}}
html,body{{margin:0;background:#0B0B0B}} .c{{position:relative;width:1024px;height:500px;overflow:hidden;background:#0B0B0B}}
.l{{position:absolute;left:64px;top:84px}} h1{{font:600 76px/1 B;color:#EEEEEE;margin:26px 0 18px;letter-spacing:.5px}}
p{{font:400 29px/1.25 R;color:#9B9B9B;margin:0}}
.ph{{position:absolute;left:612px;top:48px;width:316px;border-radius:30px;overflow:hidden;border:2px solid #2A2A2A}}
.ph img{{display:block;width:100%}}
.w{{position:absolute;left:520px;top:262px;width:214px;height:214px;border-radius:50%;overflow:hidden;border:10px solid #181818}}
.w img{{width:100%;height:100%;display:block}}
</style></head><body><div class="c"><div class="l">{icon}<h1>Supervisor</h1><p>{T[0]}<br>{T[1]}</p></div>
<div class="ph"><img src="{phone}"></div><div class="w"><img src="{watch}"></div></div></body></html>'''
open(out, "w", encoding="utf8").write(html)
