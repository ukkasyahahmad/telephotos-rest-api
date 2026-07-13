import { sqliteTable, text, integer } from "drizzle-orm/sqlite-core"

export const media = sqliteTable("media", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  localUri: text("local_uri"),
  mimeType: text("mime_type").notNull(),
  dateAdded: integer("date_added").notNull(),
  width: integer("width"),
  height: integer("height"),
  telegramMessageId: integer("telegram_message_id"),
  telegramFileId: text("telegram_file_id"),
  syncState: text("sync_state").notNull().default("PENDING"),
  createdAt: integer("created_at").notNull().$default(() => Date.now()),
})
