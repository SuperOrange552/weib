import client from './client'
import type { ApiResponse, DashboardStats, AuditLog } from '../types'

export const getStats = () =>
  client.get<ApiResponse<DashboardStats>>('/dashboard/stats')

export const getCharts = () =>
  client.get<ApiResponse<{ userGrowth: any[]; jobDistribution: any[] }>>('/dashboard/charts')

export const getRecentLogs = (limit = 10) =>
  client.get<ApiResponse<AuditLog[]>>('/dashboard/recent-logs', { params: { limit } })
