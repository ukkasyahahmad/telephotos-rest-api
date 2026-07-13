import { Hono } from "hono"
import { getMe } from "../lib/telegram"

const auth = new Hono()

auth.get("/check", async (c) => {
  const res = await getMe()
  if (!res.ok) {
    return c.json({ ok: false, error: res.description ?? "bot token invalid" }, 401)
  }
  return c.json({ ok: true, bot: res.result })
})

export default auth
