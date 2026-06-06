// ============ 认证相关 ============

export interface LoginRequest {
  username: string
  password: string
}

export interface AdminInfo {
  id: number
  username: string
  nickname: string
  roleType: 'super_admin' | 'auditor' | 'viewer'
}

export interface LoginResponse {
  token: string
  admin: AdminInfo
}

// ============ 仪表盘 ============

export interface UserGrowthPoint {
  date: string
  count: number
}

export interface JobDistribution {
  industry: string
  count: number
}

export interface DashboardStats {
  totalUsers: number
  totalJobs: number
  todayNewUsers: number
  pendingCount: number
  userGrowth: UserGrowthPoint[]
  jobDistribution: JobDistribution[]
}

// ============ 审核日志 ============

export interface AuditLog {
  id: number
  adminId: number
  adminName?: string
  action: string
  targetType: string
  targetId: number
  reason: string
  createdAt: string
}

// ============ 公司审核 ============

export interface Company {
  id: number
  name: string
  industry: string
  scale: string
  address: string
  description: string
  bossName: string
  auditStatus: 'pending' | 'approved' | 'rejected'
  auditReason: string
  createdAt: string
}

// ============ 职位审核 ============

export interface Job {
  id: number
  title: string
  companyName: string
  salaryMin: number
  salaryMax: number
  city: string
  education: string
  experience: string
  auditStatus: 'pending' | 'approved' | 'rejected'
  auditReason: string
  createdAt: string
}

// ============ 用户管理 ============

export interface User {
  id: number
  username: string
  nickname: string
  phone: string
  role: 'seeker' | 'boss' | 'admin'
  status: 'active' | 'banned'
  resumeCount: number
  applicationCount: number
  createdAt: string
}

export interface UserDetail extends User {
  email?: string
  resumeList?: ResumeInfo[]
}

export interface ResumeInfo {
  id: number
  realName: string
  education: string
  school: string
  skills: string
}

// ============ 子管理员 ============

export interface SubAdmin {
  userId: number
  username: string
  nickname: string
  roleType: 'super_admin' | 'auditor' | 'viewer'
  createdAt: string
}

export interface CreateAdminRequest {
  username: string
  password: string
  roleType: string
}

// ============ 通用 ============

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

export interface BatchOfflineRequest {
  ids: number[]
}

export interface RejectRequest {
  reason: string
}
