import client from './client'
import type { ApiResponse, PageResponse, Company, RejectRequest } from '../types'

export const getCompanies = (params: Record<string, any>) =>
  client.get<ApiResponse<PageResponse<Company>>>('/companies', { params })

export const getCompanyDetail = (id: number) =>
  client.get<ApiResponse<Company>>(`/companies/${id}`)

export const approveCompany = (id: number) =>
  client.put<ApiResponse<null>>(`/companies/${id}/approve`)

export const rejectCompany = (id: number, data: RejectRequest) =>
  client.put<ApiResponse<null>>(`/companies/${id}/reject`, data)
