<script lang="ts">
  import { onDestroy } from 'svelte'
  import { parseInvite, PairError, type Invite } from './pairing'
  import { browserDeps, deviceName, needsHomeScreen, pairBrowser } from './remoteAccess'
  import { t } from './t'

  // La prima pagina della web app pubblica finché il browser non è collegato (fase 3 del piano web): l'invito arriva dal
  // QR inquadrato con la fotocamera, dal codice incollato o dal link con #pair=; si collega solo dopo la conferma.
  type Store = { get(k: string): string | null; set(k: string, v: string): void; remove(k: string): void }
  let { store, onPaired }: { store: Store; onPaired: () => void } = $props()

  const standalone = matchMedia('(display-mode: standalone)').matches || (navigator as { standalone?: boolean }).standalone === true
  let homeFirst = $state(needsHomeScreen(navigator.userAgent, standalone, navigator.maxTouchPoints ?? 0))
  const fromLink = parseInvite(location.href)
  if (fromLink) history.replaceState(null, '', location.pathname)
  let invite = $state<Invite | null>(fromLink)
  let pasted = $state('')
  let error = $state<string | null>(null)
  let busy = $state<string | null>(null)
  let done = $state<string | null>(null)

  let video = $state<HTMLVideoElement>()
  let stream: MediaStream | null = null
  let scanning = $state(false)
  let raf = 0

  function found(text: string): boolean {
    const q = parseInvite(text)
    if (!q) return false
    invite = q
    error = null
    stopScan()
    return true
  }

  async function startScan() {
    error = null
    try {
      stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false })
    } catch { error = t.pairCamera; return }
    scanning = true
    await Promise.resolve()
    if (!video) return
    video.srcObject = stream
    await video.play().catch(() => {})
    // BarcodeDetector dove c'è (Chrome su Android); altrove, Safari compreso, jsQR caricato solo ora.
    const Detector = (globalThis as { BarcodeDetector?: new (o: object) => { detect(s: CanvasImageSource): Promise<{ rawValue: string }[]> } }).BarcodeDetector
    const detector = Detector ? new Detector({ formats: ['qr_code'] }) : null
    const jsQR = detector ? null : (await import('jsqr')).default
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d', { willReadFrequently: true })!
    let last = 0
    const tick = async (ts: number) => {
      if (!scanning || !video) return
      if (ts - last > 200 && video.videoWidth) {
        last = ts
        if (detector) {
          const codes = await detector.detect(video).catch(() => [])
          if (codes.some(c => found(c.rawValue))) return
        } else if (jsQR) {
          canvas.width = video.videoWidth
          canvas.height = video.videoHeight
          ctx.drawImage(video, 0, 0)
          const img = ctx.getImageData(0, 0, canvas.width, canvas.height)
          const code = jsQR(img.data, img.width, img.height, { inversionAttempts: 'attemptBoth' })
          if (code && found(code.data)) return
        }
      }
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
  }

  function stopScan() {
    scanning = false
    cancelAnimationFrame(raf)
    stream?.getTracks().forEach(tr => tr.stop())
    stream = null
  }
  onDestroy(stopScan)

  function usePasted() {
    if (!found(pasted)) error = t.pairBad
  }

  async function pair() {
    if (!invite) return
    const q = invite
    error = null
    busy = t.pairWorking(q.h)
    try {
      const r = await pairBrowser(q, store, browserDeps(deviceName(navigator.userAgent)))
      done = t.pairDone(r.host)
      setTimeout(onPaired, 900)
    } catch (e) {
      error = e instanceof PairError ? t.pairFail(e.why) : t.pairFail(String((e as Error)?.message ?? e))
      invite = null
    } finally { busy = null }
  }
</script>

<main class="pair">
  <img src="/icon.svg" alt="" width="64" height="64" />
  <h1>{t.pairTitle}</h1>

  {#if done}
    <p class="ok" role="status">{done}</p>
  {:else if homeFirst}
    <p>{t.pairIos}</p>
    <button class="text" onclick={() => (homeFirst = false)}>{t.pairIosAnyway}</button>
  {:else if invite}
    <p class="ask">{busy ?? t.pairConfirm(invite.h)}</p>
    {#if !busy}<button class="filled" onclick={pair}>{t.pairGo}</button>{/if}
  {:else}
    <p>{t.pairHow}</p>
    {#if scanning}
      <!-- svelte-ignore a11y_media_has_caption -->
      <video bind:this={video} playsinline muted></video>
      <button class="tonal" onclick={stopScan}>{t.pairScanStop}</button>
    {:else}
      <button class="filled" onclick={startScan}>{t.pairScan}</button>
    {/if}
    <label class="paste">
      <span>{t.pairPaste}</span>
      <textarea rows="3" bind:value={pasted} placeholder={t.pairPasteHint} spellcheck="false" autocapitalize="off"></textarea>
    </label>
    {#if pasted.trim()}<button class="tonal" onclick={usePasted}>{t.pairGo}</button>{/if}
  {/if}

  {#if error}<p class="err" role="alert">{error}</p>{/if}
  {#if !done}<a class="demo" href="?demo">{t.pairDemo}</a>{/if}
</main>

<style>
  .pair { min-height: 100dvh; max-width: 480px; margin: 0 auto; padding: max(32px, env(safe-area-inset-top)) 24px 32px;
    display: flex; flex-direction: column; align-items: stretch; gap: 16px; }
  img { align-self: center; border-radius: 16px; }
  h1 { font-size: 24px; font-weight: 400; text-align: center; }
  p { color: var(--text2); }
  .ask, .ok { color: var(--text); font-size: 18px; text-align: center; }
  .err { color: var(--b-alert); }
  video { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 16px; background: var(--low); }
  button { min-height: 48px; border-radius: 24px; font-size: 16px; }
  .filled { background: var(--primary); color: var(--on-primary); }
  .tonal { background: var(--high); color: var(--text); }
  .text { color: var(--icon); }
  .paste { display: flex; flex-direction: column; gap: 6px; }
  .paste span { color: var(--text2); font-size: 14px; }
  textarea { background: var(--low); color: var(--text); border: 1px solid var(--line); border-radius: 12px; padding: 10px;
    font-family: var(--mono); font-size: 16px; resize: vertical; }
  .demo { align-self: center; color: var(--text2); font-size: 14px; margin-top: auto; }
</style>
