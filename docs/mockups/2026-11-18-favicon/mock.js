(() => {
  const V = window.__V || 'A', day = !!window.__DAY;
  if (day) document.documentElement.classList.add('theme-day');
  const svg = s => 'data:image/svg+xml;utf8,' + encodeURIComponent(s);
  const FAV = {
    storefront: svg('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="7" fill="#1F7A4D"/><path d="M9 12h14l-1.2 12.5a1.5 1.5 0 0 1-1.5 1.5h-8.6a1.5 1.5 0 0 1-1.5-1.5Z" fill="#fff"/><path d="M12.5 12V10a3.5 3.5 0 0 1 7 0v2" fill="none" stroke="#fff" stroke-width="2"/></svg>'),
    'admin-panel': svg('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="7" fill="#4C5BD4"/><rect x="7" y="8" width="8" height="7" rx="1.5" fill="#fff"/><rect x="17" y="8" width="8" height="16" rx="1.5" fill="#fff" opacity=".75"/><rect x="7" y="17" width="8" height="7" rx="1.5" fill="#fff" opacity=".9"/></svg>'),
    blog: svg('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="16" fill="#E8590C"/><path d="M16 6l6 9-6 11-6-11Z" fill="#fff"/><circle cx="16" cy="16" r="2" fill="#E8590C"/><path d="M16 18v8" stroke="#E8590C" stroke-width="1.6"/></svg>'),
  };
  const css = document.createElement('style');
  css.textContent = `
  .fav{position:relative;display:inline-grid;place-items:center;width:30px;height:30px;flex:none}
  .fav .tile{width:26px;height:26px;border-radius:8px;background:#F5F5F2;display:grid;place-items:center}
  .fav .tile img{width:18px;height:18px;border-radius:4px}
  .fav.A .dot{position:absolute;right:-2px;bottom:-2px;width:11px;height:11px;border-radius:50%;background:var(--st);box-shadow:0 0 0 2.5px var(--card-bg,#1a1a1a)}
  .fav.B .tile{box-shadow:0 0 0 2px var(--card-bg,#1a1a1a),0 0 0 4px var(--st)}
  .fav.B.busy .tile{box-shadow:0 0 0 2px var(--card-bg,#1a1a1a),0 0 0 4px transparent}
  .fav.B.busy::after{content:"";position:absolute;inset:-1px;border-radius:11px;border:2px solid transparent;border-top-color:var(--st);border-right-color:var(--st)}
  .theme-day .fav .tile{background:#fff;outline:1px solid rgba(0,0,0,.08)}
  .fav-h{display:inline-grid;margin-right:8px;vertical-align:middle}`;
  document.head.appendChild(css);
  const make = (name, st, state, small) => {
    const f = document.createElement('span'); f.className = `fav ${V} ${state}` + (small ? ' fav-h' : '');
    f.style.setProperty('--st', st);
    f.innerHTML = `<span class="tile"><img src="${FAV[name]}"></span>` + (V === 'A' ? '<i class="dot"></i>' : '');
    return f;
  };
  const apply = () => {
    document.querySelectorAll('.card[data-sess]').forEach(c => {
      const n = c.dataset.sess; if (!FAV[n] || c.querySelector('.fav')) return;
      const icon = c.querySelector('.rail svg'); if (!icon) return;
      const ring = icon.querySelector('[stroke^="var("]'); const st = ring ? ring.getAttribute('stroke') : 'var(--stale)';
      const state = c.classList.contains('waiting') ? 'waiting' : c.classList.contains('busy') ? 'busy' : 'idle';
      c.style.setProperty('--card-bg', getComputedStyle(c).backgroundColor);
      icon.replaceWith(make(n, st, state));
      c.querySelector('.idc')?.remove();
    });
    document.querySelectorAll('.head .top').forEach(h => {
      const b = h.querySelector(':scope > b'); const icon = h.querySelector(':scope > svg');
      if (!b || !icon || !FAV[b.textContent.trim()]) return;
      const lab = icon.getAttribute('aria-label') || '';
      const card = document.querySelector(`.card[data-sess="${b.textContent.trim()}"]`);
      const state = card ? (card.classList.contains('waiting') ? 'waiting' : card.classList.contains('busy') ? 'busy' : 'idle') : /wait/.test(lab) ? 'waiting' : /busy/.test(lab) ? 'busy' : 'idle';
      const st = state === 'waiting' ? 'var(--wait)' : state === 'busy' ? 'var(--busy)' : 'var(--idle)';
      h.style.setProperty('--card-bg', getComputedStyle(h.closest('[class*=col],section,div') || h).backgroundColor);
      icon.replaceWith(make(b.textContent.trim(), st, state));
    });
  };
  apply(); new MutationObserver(apply).observe(document.body, { childList: true, subtree: true });
  return 'ok ' + V;
})()
