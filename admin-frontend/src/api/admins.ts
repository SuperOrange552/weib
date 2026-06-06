import client from './client'
import type { ApiResponse, SubAdmin, CreateAdminRequest } from '../types'

export const getAdmins = () =>
  client.get<ApiResponse<SubAdmin[]>>('/admins')

export const createAdmin = (data: CreateAdminRequest) =>
  client.post<ApiResponse<{ id: number; username: string; roleType: string }>>('/admins', data)

export const updateAdminRole = (userId: number, data: { roleType: string }) =>
  client.put<ApiResponse<null>>(`/admins/${userId}`, data)

export const disableAdmin = (userId: number) =>
  client.put<ApiResponse<null>>(`/admins/${userId}/disable`)
