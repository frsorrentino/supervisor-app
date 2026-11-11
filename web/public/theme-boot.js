// Il tema prima di disegnare la pagina (app-tema-giorno-notte-11-11): senza, chi ha scelto Giorno con il sistema scuro (o al
// contrario) vedrebbe un lampo dell'altro tema. Lo stesso di applyTheme in src/lib/theme.ts, che poi lo tiene aggiornato.
(function () {
  var c = 'auto'
  try { var v = localStorage.getItem('cm.theme'); if (v === 'day' || v === 'night') c = v } catch (e) { /* Automatico */ }
  var light = window.matchMedia && matchMedia('(prefers-color-scheme: light)').matches
  var day = c === 'day' || (c === 'auto' && light)
  var html = document.documentElement
  if (day) html.classList.add('theme-day')
  if (c === 'auto') return
  html.style.colorScheme = day ? 'light' : 'dark'
  var bar = day ? '#F4F4F4' : '#0B0B0B'
  document.querySelectorAll('meta[name="theme-color"]').forEach(function (m) { m.content = bar })
  var cs = document.querySelector('meta[name="color-scheme"]'); if (cs) cs.content = day ? 'light' : 'dark'
})()
