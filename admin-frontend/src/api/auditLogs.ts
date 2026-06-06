import client from './client'
import type { ApiResponse, PageResponse, AuditLog } from '../types'

export const getAuditLogs = (params: Record<string, any>) =>
  client.get<ApiResponse<PageResponse<AuditLog>>>('/audit-logs', { params })

export const exportAuditLogs = (params: Record<string, any>) =>
  client.get('/export/audit-logs', { params, responseType: 'blob' })

export const exportUsers = (params: Record<string, any>) =>
  client.get('/export/users', { params, responseType: 'blob' })
