import client from './client'
import type { ApiResponse, LoginRequest, LoginResponse, AdminInfo } from '../types'

export const login = (data: LoginRequest) =>
  client.post<ApiResponse<LoginResponse>>('/auth/login', data)

export const getMe = () =>
  client.get<ApiResponse<AdminInfo>>('/auth/me')

export const logout = () =>
  client.post<ApiResponse<null>>('/auth/logout')
