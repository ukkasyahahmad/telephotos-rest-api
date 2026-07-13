export const config = {
  port: Number(process.env.PORT ?? 3000),
  botToken: process.env.BOT_TOKEN ?? "",
  chatId: process.env.CHAT_ID ?? "",
  databaseUrl: process.env.DATABASE_URL ?? "file:telephotos.db",
}

if (!config.botToken) {
  console.error("BOT_TOKEN is required in .env")
  process.exit(1)
}

if (!config.chatId) {
  console.error("CHAT_ID is required in .env")
  process.exit(1)
}
