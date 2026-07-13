import { Database } from "bun:sqlite"
import { drizzle } from "drizzle-orm/bun-sqlite"
import { migrate } from "drizzle-orm/bun-sqlite/migrator"
import * as schema from "./schema"

const sqlite = new Database(process.env.DATABASE_URL ?? "file:telephotos.db")
sqlite.run("PRAGMA journal_mode = WAL")

export const db = drizzle(sqlite, { schema })
export { schema }
