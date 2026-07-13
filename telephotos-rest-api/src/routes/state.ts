import { Hono } from "hono"
import {
  getChat,
  getFile,
  sendDocument,
  pinChatMessage,
  unpinChatMessage,
  downloadFile,
} from "../lib/telegram"

const state = new Hono()

state.get("/", async (c) => {
  const chatRes = await getChat()
  if (!chatRes.ok || !chatRes.result) {
    return c.json({ ok: false, error: chatRes.description ?? "cannot fetch chat" }, 502)
  }

  const pinned = chatRes.result.pinned_message
  if (!pinned?.document || pinned.document.file_name !== "telephotos_meta.json") {
    return c.json({ ok: false, error: "no pinned state found" }, 404)
  }

  const fileRes = await getFile(pinned.document.file_id)
  if (!fileRes.ok || !fileRes.result?.file_path) {
    return c.json({ ok: false, error: "cannot get file info" }, 502)
  }

  const dl = await downloadFile(fileRes.result.file_path)
  const text = await dl.text()

  return c.json({ ok: true, data: JSON.parse(text) })
})

state.put("/", async (c) => {
  const { json } = await c.req.json<{ json: string }>()
  if (!json) return c.json({ ok: false, error: "json field required" }, 400)

  const chatRes = await getChat()
  const oldPinnedMsgId = chatRes.ok ? chatRes.result?.pinned_message?.message_id : undefined

  const file = new File([json], "telephotos_meta.json", { type: "application/json" })
  const uploadRes = await sendDocument(file)

  if (!uploadRes.ok || !uploadRes.result) {
    return c.json({ ok: false, error: uploadRes.description ?? "upload failed" }, 502)
  }

  const newMsgId = uploadRes.result.message_id
  await pinChatMessage(newMsgId)

  if (oldPinnedMsgId) {
    await unpinChatMessage(oldPinnedMsgId)
  }

  return c.json({ ok: true, messageId: newMsgId })
})

export default state
