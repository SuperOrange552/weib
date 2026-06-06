import dayjs from 'dayjs'

/** 格式化日期时间：YYYY-MM-DD HH:mm */
export const formatDate = (dateStr: string): string => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

/** 短格式日期时间：MM-DD HH:mm */
export const formatDateShort = (dateStr: string): string => {
  return dayjs(dateStr).format('MM-DD HH:mm')
}

/** 获取审核/用户状态的中文标签 */
export const getStatusLabel = (status: string): string => {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
    active: '正常',
    banned: '已封禁'
  }
  return map[status] || status
}

/** 获取状态对应的 MUI Chip color */
export const getStatusColor = (status: string): 'default' | 'primary' | 'success' | 'error' | 'warning' => {
  const map: Record<string, 'default' | 'primary' | 'success' | 'error' | 'warning'> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'error',
    active: 'success',
    banned: 'error'
  }
  return map[status] || 'default'
}

/** 获取用户角色的中文标签 */
export const getRoleLabel = (role: string): string => {
  const map: Record<string, string> = {
    seeker: '求职者',
    boss: 'Boss',
    admin: '管理员'
  }
  return map[role] || role
}

/** 获取操作类型的中文标签 */
export const getActionLabel = (action: string): string => {
  const map: Record<string, string> = {
    approve_company: '通过公司',
    reject_company: '驳回公司',
    approve_job: '通过职位',
    reject_job: '驳回职位',
    ban_user: '封禁用户',
    unban_user: '解封用户',
    batch_offline: '批量下架'
  }
  return map[action] || action
}

/** 获取管理员角色的中文标签 */
export const getAdminRoleLabel = (roleType: string): string => {
  const map: Record<string, string> = {
    super_admin: '超级管理员',
    auditor: '审核员',
    viewer: '观察员'
  }
  return map[roleType] || roleType
}
