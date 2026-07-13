export type SyncState = "PENDING" | "UPLOADING" | "SYNCED"

export interface MediaRecord {
  id: number
  localUri: string | null
  mimeType: string
  dateAdded: number
  width: number
  height: number
  telegramMessageId: number | null
  telegramFileId: string | null
  syncState: SyncState
  createdAt: number
}
