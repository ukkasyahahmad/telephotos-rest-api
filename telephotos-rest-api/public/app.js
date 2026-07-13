const API = ""

let state = {
  media: [],
  tab: "upload",
}

async function api(path, init) {
  const res = await fetch(`${API}${path}`, init)
  const data = await res.json()
  if (!data.ok && !data.data) throw new Error(data.error || "request failed")
  return data
}

const els = {}

function render() {
  document.getElementById("app").innerHTML = `
    <div class="max-w-4xl mx-auto px-4 py-6">
      <div class="flex items-center gap-3 mb-8">
        <span class="text-3xl">📸</span>
        <h1 class="text-2xl font-bold">TelePhotos</h1>
        <span id="status-badge" class="ml-auto text-xs px-3 py-1 rounded-full font-medium"></span>
      </div>

      <div class="flex gap-1 mb-6 border-b border-gray-800">
        ${["upload", "gallery", "state"].map(t => `
          <button class="tab-btn px-4 py-2 text-sm font-medium rounded-t transition
            ${state.tab === t ? "bg-gray-800 text-white border-b-2 border-blue-500" : "text-gray-500 hover:text-gray-300"}"
            data-tab="${t}">${t === "upload" ? "📤 Upload" : t === "gallery" ? "🖼 Gallery" : "📋 State"}</button>
        `).join("")}
      </div>

      <div id="tab-content"></div>
    </div>
  `

  document.querySelectorAll(".tab-btn").forEach(btn =>
    btn.onclick = () => { state.tab = btn.dataset.tab; render() }
  )

  checkStatus()
  renderTab()
}

function renderTab() {
  const container = document.getElementById("tab-content")
  if (state.tab === "upload") renderUpload(container)
  else if (state.tab === "gallery") renderGallery(container)
  else if (state.tab === "state") renderState(container)
}

async function checkStatus() {
  const badge = document.getElementById("status-badge")
  try {
    const res = await api("/api/auth/check")
    badge.textContent = `✅ ${res.bot.first_name}`
    badge.className = "ml-auto text-xs px-3 py-1 rounded-full font-medium bg-green-900/50 text-green-400"
  } catch {
    badge.textContent = "❌ Disconnected"
    badge.className = "ml-auto text-xs px-3 py-1 rounded-full font-medium bg-red-900/50 text-red-400"
  }
}

function renderUpload(container) {
  container.innerHTML = `
    <div class="border-2 border-dashed border-gray-700 rounded-xl p-8 text-center hover:border-blue-500/50 transition cursor-pointer" id="dropzone">
      <div class="text-5xl mb-3">📁</div>
      <p class="text-lg font-medium mb-1">Drop files here or click to browse</p>
      <p class="text-sm text-gray-500">Photos, videos — max 50MB per file</p>
      <input type="file" id="file-input" multiple accept="image/*,video/*" class="hidden">
    </div>
    <div id="upload-queue" class="mt-4 space-y-2"></div>
    <div id="upload-progress" class="mt-4"></div>
  `

  const dropzone = document.getElementById("dropzone")
  const fileInput = document.getElementById("file-input")

  dropzone.onclick = () => fileInput.click()

  dropzone.ondragover = (e) => { e.preventDefault(); dropzone.className = dropzone.className.replace("border-gray-700", "border-blue-500") }
  dropzone.ondragleave = () => { dropzone.className = dropzone.className.replace("border-blue-500", "border-gray-700") }
  dropzone.ondrop = (e) => { e.preventDefault(); handleFiles([...e.dataTransfer.files]) }

  fileInput.onchange = () => handleFiles([...fileInput.files])
}

async function handleFiles(files) {
  const queue = document.getElementById("upload-queue")
  const progress = document.getElementById("upload-progress")

  queue.innerHTML = files.map(f => `
    <div class="flex items-center gap-3 bg-gray-900 rounded-lg px-4 py-2" data-file="${f.name}">
      <span class="text-sm">${f.name}</span>
      <span class="text-xs text-gray-500">${(f.size / 1024 / 1024).toFixed(1)} MB</span>
      <span class="ml-auto text-xs uploading-text text-yellow-400">⏳ uploading...</span>
    </div>
  `).join("")

  for (const file of files) {
    try {
      const form = new FormData()
      form.append("file", file)
      const res = await fetch("/api/media/upload", { method: "POST", body: form })
      const data = await res.json()
      const el = queue.querySelector(`[data-file="${file.name}"] .uploading-text`)
      if (data.ok) {
        el.textContent = "✅ done"
        el.className = "ml-auto text-xs text-green-400"
      } else {
        el.textContent = `❌ ${data.error}`
        el.className = "ml-auto text-xs text-red-400"
      }
    } catch (e) {
      const el = queue.querySelector(`[data-file="${file.name}"] .uploading-text`)
      if (el) { el.textContent = "❌ failed"; el.className = "ml-auto text-xs text-red-400" }
    }
  }

  progress.innerHTML = `<div class="bg-gray-900 rounded-lg px-4 py-3 text-center text-sm text-green-400">✅ All done! <button class="underline text-blue-400" onclick="loadGallery()">View in gallery →</button></div>`
}

async function loadGallery() {
  state.tab = "gallery"
  state.media = []
  render()
  const res = await api("/api/media")
  state.media = res.data
  render()
}

function renderGallery(container) {
  container.innerHTML = `
    <div class="flex gap-2 mb-4">
      <button id="refresh-gallery" class="px-3 py-1.5 text-sm bg-gray-800 hover:bg-gray-700 rounded-lg transition">↻ Refresh</button>
      <button id="free-space" class="px-3 py-1.5 text-sm bg-red-900/50 hover:bg-red-800/50 text-red-400 rounded-lg transition">🗑 Free Up Space</button>
    </div>
    <div id="media-grid" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3"></div>
  `

  document.getElementById("refresh-gallery").onclick = () => loadGallery()
  document.getElementById("free-space").onclick = async () => {
    if (!confirm("Mark all synced files as freed up?")) return
    const res = await api("/api/media/free-up-space", { method: "POST" })
    alert(`Freed ${res.deletedCount} files`)
    loadGallery()
  }

  const grid = document.getElementById("media-grid")
  if (state.media.length === 0) {
    grid.innerHTML = `<div class="col-span-full text-center py-12 text-gray-500">No uploads yet</div>`
    return
  }

  grid.innerHTML = state.media.map(m => {
    const isVideo = m.mimeType.startsWith("video/")
    const fileUrl = "/api/media/" + m.id + "/file"
    return `
    <div class="bg-gray-900 rounded-lg overflow-hidden border border-gray-800 hover:border-gray-600 transition group">
      <div class="aspect-square bg-gray-800 relative overflow-hidden">
        ${isVideo
          ? `<video src="${fileUrl}" class="w-full h-full object-cover" muted preload="metadata"></video><span class="absolute bottom-1 right-1 text-xs bg-black/70 px-1.5 py-0.5 rounded">▶</span>`
          : `<img src="${fileUrl}" class="w-full h-full object-cover" loading="lazy" onerror="this.parentElement.innerHTML='<span class=text-4xl>🖼</span>'">`
        }
      </div>
      <div class="p-2">
        <div class="text-xs text-gray-400 truncate">${m.mimeType}</div>
        <div class="flex items-center gap-1 mt-1">
          <span class="text-xs ${m.syncState === "SYNCED" ? "text-green-400" : "text-yellow-400"}">● ${m.syncState}</span>
        </div>
        <div class="flex gap-1 mt-2 opacity-0 group-hover:opacity-100 transition">
          <button class="dl-btn text-xs px-2 py-0.5 bg-blue-600/50 rounded" data-id="${m.id}">⬇</button>
          <button class="copy-btn text-xs px-2 py-0.5 bg-gray-600/50 rounded" data-url="${fileUrl}">🔗</button>
          <button class="del-btn text-xs px-2 py-0.5 bg-red-600/50 rounded" data-id="${m.id}">✕</button>
        </div>
      </div>
    </div>`
  }).join("")

  grid.querySelectorAll(".dl-btn").forEach(btn =>
    btn.onclick = () => window.open(`${API}/api/media/${btn.dataset.id}/download`, "_blank")
  )
  grid.querySelectorAll(".del-btn").forEach(btn =>
    btn.onclick = async () => {
      await fetch(`${API}/api/media/${btn.dataset.id}`, { method: "DELETE" })
      loadGallery()
    }
  )
  grid.querySelectorAll(".copy-btn").forEach(btn =>
    btn.onclick = async () => {
      const url = window.location.origin + btn.dataset.url
      try {
        await navigator.clipboard.writeText(url)
        const orig = btn.textContent
        btn.textContent = "✓"
        setTimeout(() => btn.textContent = orig, 1500)
      } catch {
        const orig = btn.textContent
        btn.textContent = "✗"
        setTimeout(() => btn.textContent = orig, 1500)
      }
    }
  )
}

function renderState(container) {
  container.innerHTML = `
    <div class="flex gap-2 mb-4">
      <button id="fetch-state" class="px-3 py-1.5 text-sm bg-gray-800 hover:bg-gray-700 rounded-lg transition">↻ Fetch State</button>
      <button id="save-state" class="px-3 py-1.5 text-sm bg-blue-600 hover:bg-blue-500 rounded-lg transition">💾 Save State</button>
    </div>
    <textarea id="state-editor" class="w-full h-96 bg-gray-900 border border-gray-700 rounded-lg p-4 text-sm font-mono text-gray-200 resize-y focus:outline-none focus:border-blue-500" placeholder="Pinned JSON state will appear here..."></textarea>
    <div id="state-msg" class="mt-2 text-sm"></div>
  `

  document.getElementById("fetch-state").onclick = async () => {
    const msg = document.getElementById("state-msg")
    try {
      const res = await api("/api/state")
      document.getElementById("state-editor").value = JSON.stringify(res.data, null, 2)
      msg.textContent = "✅ Loaded"
      msg.className = "mt-2 text-sm text-green-400"
    } catch (e) {
      msg.textContent = `❌ ${e.message}`
      msg.className = "mt-2 text-sm text-red-400"
    }
  }

  document.getElementById("save-state").onclick = async () => {
    const msg = document.getElementById("state-msg")
    try {
      const json = document.getElementById("state-editor").value
      JSON.parse(json)
      const res = await api("/api/state", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ json }),
      })
      msg.textContent = `✅ Saved (message #${res.messageId})`
      msg.className = "mt-2 text-sm text-green-400"
    } catch (e) {
      msg.textContent = `❌ ${e.message}${e.message === "Unexpected token" ? " — invalid JSON" : ""}`
      msg.className = "mt-2 text-sm text-red-400"
    }
  }
}

window.loadGallery = loadGallery
render()
