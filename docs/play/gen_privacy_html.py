#!/usr/bin/env python3
"""PRIVACY.md → web/public/privacy.html, servita su https://supervisor.francescosorrentino.com/privacy (Play Console).

Il Markdown di PRIVACY.md è semplice (titoli, paragrafi, elenchi, grassetto, corsivo, link): basta questo convertitore,
senza dipendenze. Rilanciarlo dopo ogni modifica di PRIVACY.md.
"""
import html
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def inline(t: str) -> str:
    t = html.escape(t, quote=False)
    t = re.sub(r"&lt;(https?://[^&]+)&gt;", r'<a href="\1">\1</a>', t)
    t = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', t)
    t = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", t)
    t = re.sub(r"(?<![\w/])_([^_]+)_(?![\w/])", r"<em>\1</em>", t)
    return t


def convert(md: str) -> str:
    out, para, items = [], [], []

    def flush():
        if para:
            out.append(f"<p>{inline(' '.join(para))}</p>")
            para.clear()
        if items:
            out.append("<ul>" + "".join(f"<li>{inline(i)}</li>" for i in items) + "</ul>")
            items.clear()

    for line in md.splitlines():
        if not line.strip():
            flush()
        elif m := re.match(r"(#+) (.*)", line):
            flush()
            n = len(m.group(1))
            out.append(f"<h{n}>{inline(m.group(2))}</h{n}>")
        elif line.startswith("- "):
            if para:
                flush()
            items.append(line[2:].strip())
        elif items and line.startswith("  "):
            items[-1] += " " + line.strip()
        else:
            para.append(line.strip())
    flush()
    return "\n".join(out)


def main():
    body = convert((ROOT / "PRIVACY.md").read_text(encoding="utf-8"))
    page = f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="dark">
<title>Privacy — Supervisor</title>
<style>
  body {{ background: #000; color: #F2F4F7; font: 16px/1.55 Roboto, system-ui, -apple-system, "Segoe UI", sans-serif;
    max-width: 720px; margin: 0 auto; padding: 32px 20px 64px; }}
  h1 {{ font-size: 28px; font-weight: 500; }}
  h2 {{ font-size: 20px; font-weight: 500; margin-top: 32px; }}
  p, li {{ color: #D0D6DE; }}
  a {{ color: #A8C7FA; }}
  strong {{ color: #F2F4F7; }}
</style>
</head>
<body>
{body}
</body>
</html>
"""
    (ROOT / "web/public/privacy.html").write_text(page, encoding="utf-8")


if __name__ == "__main__":
    main()
