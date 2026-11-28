<script lang="ts">
  import { onMount } from 'svelte'
  import type { ChangedFile, ChangesView, Session } from './contract'
  import { alone, askFile, folder, hidden, hiddenFiles, label, name, showCounts, source, type Load } from './diff'
  import DiffBox from './DiffBox.svelte'
  import DiffLine from './DiffLine.svelte'
  import Page from './Page.svelte'
  import { t } from './t'
  // La vista «Modifiche» (contratto 1.72, ChangesPanel in DiffViews.kt), sopra la sessione: sul telefono ne prende la pagina,
  // sulla plancia la colonna. I file rispetto all'ultimo commit coi loro numeri e l'etichetta; un tocco apre il diff del file
  // (op `changes` con `file=`), Aggiorna rilegge tutto. Mai vuota senza spiegazione.
  // app-posizioni-sistema-27-11 gruppo 2: `bare`, dentro la tendina della scheda Modifiche: senza la pagina e la sua testata (il nome è
  // sulla scheda aperta), «Aggiorna» sulla riga della base; le righe nell'elemento `data-rows`, che la tendina fa comparire una alla volta.
  let { s, load, onClose, bare = false }: { s: Session; load: (file: string | null) => Promise<Load<ChangesView>>; onClose: () => void; bare?: boolean } = $props()
  let got = $state<Load<ChangesView> | null>(null)
  let loading = $state(false)
  let open = $state<Record<string, boolean>>({})
  let one = $state<Record<string, Load<ChangesView> | null>>({})
  async function readFile(path: string) { one[path] = null; one[path] = await load(path) }
  async function refresh() {
    loading = true
    got = await load(null)
    loading = false
    one = {}
    // Si rilegge con `file=` solo un file aperto che l'elenco nuovo ha ancora tagliato.
    const files = got.ok ? got.value.files : []
    for (const [p, on] of Object.entries(open)) { const f = files.find(x => x.path === p); if (on && f && askFile(f)) void readFile(p) }
  }
  function toggle(f: ChangedFile) {
    open[f.path] = !open[f.path]
    if (open[f.path] && askFile(f)) void readFile(f.path)
  }
  onMount(() => { void refresh() })
  const fail = (x: { why: string; text: string }) => (x.why === 'git' ? t.diffGit : x.why === 'down' ? t.diffDown : x.text)
</script>

{#snippet actions()}
  <button class="ib" aria-label={t.changesRefresh} title={t.changesRefresh} disabled={loading} onclick={refresh}>
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7L21 8" /><path d="M21 3v5h-5" /></svg>
  </button>
{/snippet}

{#snippet body()}
  <div class="chg" class:bare data-rows={bare ? '' : undefined} data-testid={bare ? 'modifiche' : undefined}>
    {#if bare}
      {@const v0 = got?.ok ? got.value : null}
      {@const src0 = v0 ? source(v0) : null}
      <div class="head"><p class="src" class:outside={src0 === 'transcript'}>{got == null ? t.changesLoading : v0 == null ? '' : src0 === 'transcript' ? t.changesOutsideGit : src0 === 'no_commits' ? t.changesNoCommits : t.changesBase(v0.base ?? '')}</p>{@render actions()}</div>
    {:else}<p class="who">{s.name}</p>{/if}
    {#if loading}<div class="bar"><b></b></div>{/if}
    {#if got == null}{#if !bare}<p class="note">{t.changesLoading}</p>{/if}
    {:else if !got.ok}
      <div class="fail"><p>{fail(got)}</p><button class="retry" onclick={refresh}>{t.diffRetry}</button></div>
    {:else}
      {@const v = got.value}
      {@const src = source(v)}
      {#if !bare}<p class="src" class:outside={src === 'transcript'}>{src === 'transcript' ? t.changesOutsideGit : src === 'no_commits' ? t.changesNoCommits : t.changesBase(v.base ?? '')}</p>{/if}
      {#if !v.files.length}<p class="note">{src === 'transcript' ? t.changesNoneTranscript : t.changesNone}</p>{/if}
      <!-- Chiave con la posizione: due file dallo stesso percorso non rompono l'elenco (la chiave doppia del 15/10). -->
      {#each v.files as f, i (`${i}:${f.path}`)}
        {@const why = hidden(f)}
        {@const l = label(f.status)}
        <div class="file">
          <DiffLine name={name(f.path)} folder={folder(f.path)} added={f.added} removed={f.removed} counts={showCounts(f)}
            label={l ? t.changesLabels[l] : null} open={!!open[f.path]} onToggle={() => toggle(f)} />
          {#if open[f.path]}
            {#if why}<p class="note">{t.diffHidden[why]}</p>
            {:else if !askFile(f)}<DiffBox hunks={f.hunks} />
            {:else}
              {@const a = one[f.path]}
              {#if a == null}
                {#if f.hunks.length}<DiffBox hunks={f.hunks} />{/if}<p class="note">{t.changesFileLoading}</p>
              {:else if !a.ok}
                {#if f.hunks.length}<DiffBox hunks={f.hunks} />{/if}
                <div class="fail"><p>{fail(a)}</p><button class="retry" onclick={() => readFile(f.path)}>{t.diffRetry}</button></div>
              {:else}
                {@const x = alone(a.value, f.path)}
                {#if !x}<p class="note">{t.changesFileGone}</p>
                {:else if x.hunks.length}<DiffBox hunks={x.hunks} />{#if x.cut}<p class="note">{t.diffBeyond}</p>{/if}
                {:else}<p class="note">{t.diffHidden[hidden(x) ?? 'empty']}</p>{/if}
              {/if}
            {/if}
          {/if}
        </div>
      {/each}
      {#if hiddenFiles(v) > 0}<p class="note">{t.changesMoreFiles(hiddenFiles(v))}</p>{/if}
    {/if}
  </div>
{/snippet}

{#if bare}{@render body()}{:else}<Page title={t.changesTitle} onBack={onClose} {actions}>{@render body()}</Page>{/if}

<style>
  .chg { display: flex; flex-direction: column; gap: 8px; padding: 0 16px 24px; }
  .chg.bare { padding: 0 2px 8px; }
  .head { display: flex; align-items: center; gap: 8px; min-height: 44px; }
  .head .src { flex: 1; min-width: 0; }
  .who { font-size: 12.5px; color: var(--text2); }
  .src { font-size: 14px; color: var(--text2); }
  .src.outside { color: var(--wait); }
  .note { font-size: 14px; color: var(--text2); padding: 2px 4px; }
  .file { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
  .fail { display: flex; align-items: center; gap: 8px; }
  .fail p { flex: 1; color: var(--gone-dim); font-size: 14px; }
  .retry { color: var(--icon); padding: 8px 14px; border-radius: 999px; font-weight: 500; }
  .retry:hover, .ib:hover { background: var(--surface); }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; }
  .ib:disabled { opacity: .5; }
  .bar { height: 3px; border-radius: 2px; background: var(--high); overflow: hidden; }
  .bar b { display: block; width: 40%; height: 100%; background: var(--icon); animation: run 1.2s ease-in-out infinite; }
  @keyframes run { from { translate: -100% 0; } to { translate: 250% 0; } }
  @media (prefers-reduced-motion: reduce) { .bar b { animation: none; } }
</style>
