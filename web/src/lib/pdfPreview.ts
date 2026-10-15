// La prima pagina di un PDF come immagine, per l'anteprima nella chat (Franz, 15/10 22:20). pdf.js si carica solo qui, al
// primo PDF: la pagina non lo porta con sé. Senza riuscita (PDF rotto o cifrato) null: resta la scheda col nome.
export async function firstPage(data: Blob, width = 720): Promise<Blob | null> {
  try {
    const pdfjs = await import('pdfjs-dist')
    const worker = (await import('pdfjs-dist/build/pdf.worker.min.mjs?url')).default
    pdfjs.GlobalWorkerOptions.workerSrc = worker
    const task = pdfjs.getDocument({ data: new Uint8Array(await data.arrayBuffer()), isEvalSupported: false })
    const doc = await task.promise
    const page = await doc.getPage(1)
    const base = page.getViewport({ scale: 1 })
    const viewport = page.getViewport({ scale: width / base.width })
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(viewport.width); canvas.height = Math.round(viewport.height)
    await page.render({ canvas, viewport }).promise
    const png = await new Promise<Blob | null>(r => canvas.toBlob(r, 'image/png'))
    // In pdf.js 6 il documento si chiude dal suo compito di caricamento.
    await task.destroy()
    return png
  } catch { return null }
}
