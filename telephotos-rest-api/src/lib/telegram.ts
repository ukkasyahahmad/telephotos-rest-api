import { config } from "../config"

const BOT_API = "https://api.telegram.org"

const botUrl = (method: string) => `${BOT_API}/bot${config.botToken}/${method}`

async function callApi<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  return res.json() as T
}

export interface TelegramUser {
  id: number
  first_name: string
  username?: string
}

export interface TelegramMessage {
  message_id: number
  date: number
  document?: { file_id: string; file_name?: string }
  photo?: { file_id: string }[]
  video?: { file_id: string }
}

export interface TelegramFile {
  file_id: string
  file_size?: number
  file_path?: string
}

export interface ApiResponse<T> {
  ok: boolean
  result?: T
  description?: string
}

export async function getMe(): Promise<ApiResponse<TelegramUser>> {
  return callApi(botUrl("getMe"))
}

export async function sendPhoto(file: File): Promise<ApiResponse<TelegramMessage>> {
  const form = new FormData()
  form.append("chat_id", config.chatId)
  form.append("photo", file)
  return callApi(botUrl("sendPhoto"), { method: "POST", body: form })
}

export async function sendVideo(file: File): Promise<ApiResponse<TelegramMessage>> {
  const form = new FormData()
  form.append("chat_id", config.chatId)
  form.append("video", file)
  return callApi(botUrl("sendVideo"), { method: "POST", body: form })
}

export async function sendDocument(file: File): Promise<ApiResponse<TelegramMessage>> {
  const form = new FormData()
  form.append("chat_id", config.chatId)
  form.append("document", file)
  return callApi(botUrl("sendDocument"), { method: "POST", body: form })
}

export async function getChat(): Promise<ApiResponse<{ id: number; pinned_message?: TelegramMessage }>> {
  return callApi(botUrl("getChat") + `?chat_id=${config.chatId}`)
}

export async function getFile(fileId: string): Promise<ApiResponse<TelegramFile>> {
  return callApi(botUrl("getFile") + `?file_id=${fileId}`)
}

export async function pinChatMessage(messageId: number) {
  return callApi(botUrl("pinChatMessage"), {
    method: "POST",
    body: JSON.stringify({ chat_id: config.chatId, message_id: messageId, disable_notification: true }),
    headers: { "Content-Type": "application/json" },
  })
}

export async function unpinChatMessage(messageId: number) {
  return callApi(botUrl("unpinChatMessage"), {
    method: "POST",
    body: JSON.stringify({ chat_id: config.chatId, message_id: messageId }),
    headers: { "Content-Type": "application/json" },
  })
}

export function fileUrl(filePath: string): string {
  return `${BOT_API}/file/bot${config.botToken}/${filePath}`
}

export async function downloadFile(filePath: string, range?: string): Promise<Response> {
  const headers: Record<string, string> = {}
  if (range) headers["Range"] = range
  return fetch(fileUrl(filePath), { headers })
}
