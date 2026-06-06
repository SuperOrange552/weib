import client from './client'
import type { ApiResponse, PageResponse, Job, RejectRequest, BatchOfflineRequest } from '../types'

export const getJobs = (params: Record<string, any>) =>
  client.get<ApiResponse<PageResponse<Job>>>('/jobs', { params })

export const getJobDetail = (id: number) =>
  client.get<ApiResponse<Job>>(`/jobs/${id}`)

export const approveJob = (id: number) =>
  client.put<ApiResponse<null>>(`/jobs/${id}/approve`)

export const rejectJob = (id: number, data: RejectRequest) =>
  client.put<ApiResponse<null>>(`/jobs/${id}/reject`, data)

export const batchOffline = (data: BatchOfflineRequest) =>
  client.post<ApiResponse<{ successCount: number }>>('/jobs/batch-offline', data)
