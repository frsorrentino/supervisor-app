import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import { execSync } from 'node:child_process'

// La versione in alto: solo numeri, come sul telefono (Franz 31/10 21:38). Il commit resta in Impostazioni, Informazioni.
// Dove la copia non ha il git giusto si passano APP_VERSION e APP_COMMIT.
const git = (args: string) => { try { return execSync('git ' + args).toString().trim() } catch { return '' } }
const version = process.env.APP_VERSION || (git('rev-list --count HEAD') ? '0.2.' + git('rev-list --count HEAD') : 'sviluppo')
const commit = process.env.APP_COMMIT || git('rev-parse --short HEAD')

// Le fixture del contratto stanno fuori dalla cartella della web app (../contract): i dati di prova le leggono da lì.
export default defineConfig({
  // /version.json accanto alla pagina (app-web-versione-nuova-11-11): la web aperta lo legge per sapere se è uscita una versione nuova.
  plugins: [svelte(), { name: 'version-json', apply: 'build', generateBundle() { this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ version, commit }) }) } }],
  define: { __APP_VERSION__: JSON.stringify(version), __APP_COMMIT__: JSON.stringify(commit) },
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
