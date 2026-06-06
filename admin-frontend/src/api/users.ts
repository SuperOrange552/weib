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
