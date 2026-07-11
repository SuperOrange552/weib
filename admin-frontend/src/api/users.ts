import client from './client'
import type { ApiResponse, PageResponse, User, UserDetail } from '../types'

export const getUsers = (params: Record<string, any>) =>
  client.get<ApiResponse<PageResponse<User>>>('/users', { params })

export const getUserDetail = (id: number) =>
  client.get<ApiResponse<UserDetail>>(`/users/${id}`)

export const banUser = (id: number) =>
  client.put<ApiResponse<null>>(`/users/${id}/ban`)

export const unbanUser = (id: number) =>
  client.put<ApiResponse<null>>(`/users/${id}/unban`)

export interface UserIdentity {
  id: number
  userId: number
  roleType: 'SEEKER' | 'BOSS'
  status: 'ACTIVE' | 'DISABLED'
  enabledAt?: string
  enabledBy?: number
}

export const getUserIdentities = (userId: number) =>
  client.get<ApiResponse<UserIdentity[]>>(`/identities/users/${userId}`)

export const enableIdentity = (userId: number, role: string, reason: string, idempotencyKey: string) =>
  client.put<ApiResponse<UserIdentity>>(`/identities/users/${userId}/${role}/enable`, { reason },
    { headers: { 'Idempotency-Key': idempotencyKey } })

export const disableIdentity = (userId: number, role: string, reason: string, idempotencyKey: string) =>
  client.put<ApiResponse<UserIdentity>>(`/identities/users/${userId}/${role}/disable`, { reason },
    { headers: { 'Idempotency-Key': idempotencyKey } })
