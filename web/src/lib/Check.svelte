<script lang="ts">
  // `inline`: nel testo, come la conferma di un'approvazione, invece che al centro del tasto.
  let { inline = false }: { inline?: boolean } = $props()
</script>

<!-- La spunta disegnata (D3 del mockup approvato da Franz, app-motion-loops-04-11): sul tasto di Ok, di un'approvazione e del
     Feedback mandato l'etichetta sparisce e la spunta si disegna; lo stile è in v3.css (`button.ck`, `.dck`). -->
<svg class="dck" class:inl={inline} viewBox="0 0 20 20" aria-hidden="true"><path pathLength="1" d="M4.5 10.5 8.5 14.5 15.5 6" /></svg>
