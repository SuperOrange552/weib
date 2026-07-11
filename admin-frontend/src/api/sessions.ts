import client from './client'
import type { ApiResponse } from '../types'

export interface LoginSlot {
  sid: string
  activeRole: 'SEEKER' | 'BOSS'
  clientType: 'WEB' | 'MOBILE'
  issuedAt: number
  deviceIdHash?: string
}

export const getUserSessions = (userId: number) =>
  client.get<ApiResponse<Record<string, LoginSlot>>>(`/sessions/users/${userId}`)

export const forceLogout = (userId: number, reason: string, idempotencyKey: string) =>
  client.post<ApiResponse<null>>(`/sessions/users/${userId}/force-logout`, { reason },
    { headers: { 'Idempotency-Key': idempotencyKey } })
