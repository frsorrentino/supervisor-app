import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import { execSync } from 'node:child_process'

// La versione mostrata nelle Impostazioni: il commit da cui è costruita la web app.
const version = process.env.APP_VERSION || (() => { try { return execSync('git rev-parse --short HEAD').toString().trim() } catch { return 'sviluppo' } })()

// Le fixture del contratto stanno fuori dalla cartella della web app (../contract): i dati di prova le leggono da lì.
export default defineConfig({
  plugins: [svelte()],
  define: { __APP_VERSION__: JSON.stringify(version) },
  server: { fs: { allow: ['..'] } },
  // Il service worker si costruisce accanto alla pagina, come /sw.js alla radice (lo scope è la cartella del file).
  build: {
    rollupOptions: {
      // `embed.html`: la sola chat della master, incorporabile da Studio (Franz 24/10 09:41, strada A).
      input: { index: 'index.html', embed: 'embed.html', sw: 'src/sw.ts' },
      output: { entryFileNames: c => (c.name === 'sw' ? 'sw.js' : 'assets/[name]-[hash].js') },
    },
  },
})
