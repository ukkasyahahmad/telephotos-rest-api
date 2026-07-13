import { Hono } from "hono"
import { eq } from "drizzle-orm"
import { db, schema } from "../lib/db"
import {
  sendPhoto,
  sendVideo,
  sendDocument,
  getFile,
  downloadFile,
} from "../lib/telegram"
import type { MediaRecord } from "../types"

const media = new Hono()

media.get("/", (c) => {
  const rows = db.select().from(schema.media).orderBy(schema.media.dateAdded).all()
  return c.json({ ok: true, data: rows })
})

media.get("/:id", (c) => {
  const id = Number(c.req.param("id"))
  const row = db.select().from(schema.media).where(eq(schema.media.id, id)).get()
  if (!row) return c.json({ ok: false, error: "not found" }, 404)
  return c.json({ ok: true, data: row })
})

media.get("/:id/download", async (c) => {
  const id = Number(c.req.param("id"))
  const row = db.select().from(schema.media).where(eq(schema.media.id, id)).get()
  if (!row) return c.json({ ok: false, error: "not found" }, 404)
  if (!row.telegramFileId) return c.json({ ok: false, error: "no telegram file id" }, 404)

  const fileRes = await getFile(row.telegramFileId)
  if (!fileRes.ok || !fileRes.result?.file_path) {
    return c.json({ ok: false, error: "cannot get file from telegram" }, 502)
  }

  const dl = await downloadFile(fileRes.result.file_path)
  if (!dl.ok) return c.json({ ok: false, error: "download failed" }, 502)

  const blob = await dl.blob()
  return c.newResponse(blob, 200, {
    "Content-Type": row.mimeType,
    "Content-Disposition": `attachment; filename="${row.telegramFileId}"`,
  })
})

media.get("/:id/file", async (c) => {
  const id = Number(c.req.param("id"))
  const row = db.select().from(schema.media).where(eq(schema.media.id, id)).get()
  if (!row) return c.json({ ok: false, error: "not found" }, 404)
  if (!row.telegramFileId) return c.json({ ok: false, error: "no telegram file id" }, 404)

  const fileRes = await getFile(row.telegramFileId)
  if (!fileRes.ok || !fileRes.result?.file_path) {
    return c.json({ ok: false, error: "cannot get file from telegram" }, 502)
  }

  const dl = await downloadFile(fileRes.result.file_path)
  if (!dl.ok) return c.json({ ok: false, error: "download failed" }, 502)

  return c.newResponse(dl.body, 200, {
    "Content-Type": row.mimeType,
    "Cache-Control": "public, max-age=86400",
  })
})

media.delete("/:id", (c) => {
  const id = Number(c.req.param("id"))
  db.delete(schema.media).where(eq(schema.media.id, id)).run()
  return c.json({ ok: true })
})

media.post("/free-up-space", (c) => {
  const synced = db
    .select()
    .from(schema.media)
    .where(eq(schema.media.syncState, "SYNCED"))
    .all()

  let deleted = 0
  for (const m of synced) {
    db.update(schema.media).set({ localUri: null }).where(eq(schema.media.id, m.id)).run()
    deleted++
  }

  return c.json({ ok: true, deletedCount: deleted })
})

media.post("/upload", async (c) => {
  const body = await c.req.parseBody()
  const file = body.file as File | undefined
  if (!file) return c.json({ ok: false, error: "file field required" }, 400)

  if (file.size > 50 * 1024 * 1024) {
    return c.json({ ok: false, error: "file exceeds 50MB limit" }, 400)
  }

  const isVideo = file.type.startsWith("video/")
  const isPhoto = file.type.startsWith("image/")

  let tgRes
  if (isVideo) {
    tgRes = await sendVideo(file)
  } else if (isPhoto) {
    tgRes = await sendPhoto(file)
  } else {
    tgRes = await sendDocument(file)
  }

  if (!tgRes.ok) {
    return c.json({ ok: false, error: tgRes.description ?? "upload failed" }, 502)
  }

  let fileId: string | null = null
  const result = tgRes.result!
  if (isVideo && result.video) {
    fileId = result.video.file_id
  } else if (result.photo && result.photo.length > 0) {
    fileId = result.photo[result.photo.length - 1].file_id
  } else if (result.document) {
    fileId = result.document.file_id
  }

  const record: Omit<MediaRecord, "id"> = {
    localUri: null,
    mimeType: file.type,
    dateAdded: Math.floor(Date.now() / 1000),
    width: 0,
    height: 0,
    telegramMessageId: result.message_id,
    telegramFileId: fileId,
    syncState: "SYNCED",
    createdAt: Date.now(),
  }

  const inserted = db.insert(schema.media).values(record).returning().get()
  return c.json({ ok: true, data: inserted }, 201)
})

export default media
