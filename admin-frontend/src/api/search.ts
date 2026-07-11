import client from './client'
import type { ApiResponse, PageResponse } from '../types'

export interface AdminSearchResult {
  type: 'USER' | 'COMPANY' | 'JOB' | 'RESUME'
  id: number
  ownerId?: number
  title: string
  subtitle?: string
  avatar?: string
  status?: string
  createdAt?: string
}
export interface AdminSearchDetail { type: string; id: number; data: Record<string, unknown> }
export const searchAdmin = (params: { type: string; q: string; page: number; size: number }) => client.get<ApiResponse<PageResponse<AdminSearchResult>>>('/search', { params })
export const getAdminSearchDetail = (type: string, id: number) => client.get<ApiResponse<AdminSearchDetail>>(`/search/${type}/${id}`)
