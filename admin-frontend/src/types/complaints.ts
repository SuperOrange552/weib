export type ComplaintStatus = 'PENDING' | 'RESOLVED' | 'REJECTED'

export interface Complaint {
  id: number
  reporterId: number
  targetType: string
  targetId: number
  category: string
  description: string
  evidenceUrls: string[]
  status: ComplaintStatus
  reviewReason?: string
  createdAt: string
  reviewedAt?: string
}

export interface SanctionCreateRequest {
  /** Optional when resolving a JOB/COMPANY/RESUME complaint; backend infers owner. */
  userId?: number
  sanctionType: 'MUTE' | 'PUBLISH_BAN' | 'ACCOUNT_BAN'
  targetType?: string
  targetId?: number
  sourceComplaintId?: number
  reason: string
  startsAt?: string
  endsAt?: string
}

export interface ComplaintReviewRequest {
  reason: string
  contentAction?: 'OFFLINE'
  sanction?: SanctionCreateRequest
}

export interface Sanction {
  id: number
  userId: number
  sanctionType: string
  targetType?: string
  targetId?: number
  reason: string
  startsAt: string
  endsAt?: string
  status: string
  adminId: number
}
