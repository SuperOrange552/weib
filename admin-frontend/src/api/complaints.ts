import client from './client'
import type { ApiResponse, PageResponse } from '../types'
import type { Complaint, ComplaintReviewRequest, Sanction, SanctionCreateRequest } from '../types/complaints'

export const getComplaints = (params: Record<string, any>) =>
  client.get<ApiResponse<PageResponse<Complaint>>>('/complaints', { params })

export const getComplaintDetail = (id: number) =>
  client.get<ApiResponse<Complaint>>(`/complaints/${id}`)

export const rejectComplaint = (id: number, reason: string) =>
  client.post<ApiResponse<null>>(`/complaints/${id}/reject`, { reason })

export const resolveComplaint = (id: number, data: ComplaintReviewRequest) =>
  client.post<ApiResponse<null>>(`/complaints/${id}/resolve`, data)

export const createSanction = (data: SanctionCreateRequest) =>
  client.post<ApiResponse<Sanction>>('/sanctions', data)

export const getSanctions = (params: Record<string, any>) =>
  client.get<ApiResponse<PageResponse<Sanction>>>('/sanctions', { params })

export const revokeSanction = (id: number, reason: string) =>
  client.post<ApiResponse<null>>(`/sanctions/${id}/revoke`, { reason })
