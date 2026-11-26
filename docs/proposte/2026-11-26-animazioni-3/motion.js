// Ogni mockup ripete la sua scena: `M.loop(scena, fotogramma)` la fa partire, `?still` la ferma sul fotogramma del PNG,
// `?reduce` (o il riquadro) mostra la versione con le animazioni ridotte. Script classico: si apre anche da file://.
window.M = (() => {
  const q = new URLSearchParams(location.search)
  const still = q.has('still')
  const root = document.documentElement
  if (still) root.classList.add('still')
  if (q.has('reduce') || matchMedia('(prefers-reduced-motion: reduce)').matches) root.classList.add('reduce')
  const reduced = () => root.classList.contains('reduce')
  const wait = ms => new Promise(r => setTimeout(r, ms))
  async function loop(scene, stillFrame, every = 900) {
    // Le misure delle parole valgono solo con i caratteri veri caricati.
    await document.fonts.ready
    if (still) { stillFrame(); return }
    const box = document.querySelector('.ctl')
    if (box) box.innerHTML = '<button class="tonal" id="again">Riproduci</button><label><input type="checkbox" id="red"' + (reduced() ? ' checked' : '') + '> Animazioni ridotte</label>'
    document.getElementById('red')?.addEventListener('change', e => root.classList.toggle('reduce', e.target.checked))
    let run = 0
    const go = async () => { const me = ++run; while (me === run) { await scene(); await wait(every) } }
    document.getElementById('again')?.addEventListener('click', go)
    go()
  }
  return { still, reduced, wait, loop }
})()
