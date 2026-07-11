import React, { useEffect, useState } from 'react'
import { getUserDetail, getUserIdentities, type UserIdentity } from '../api/users'
import { getUserSessions, type LoginSlot } from '../api/sessions'
import { getRoleLabel, getStatusLabel, formatDate } from '../utils'
import type { User, UserDetail } from '../types'
import { Drawer, Typography, Chip, Divider, Skeleton, Box } from '@mui/material'

interface Props {
  open: boolean
  user: User | null
  onClose: () => void
}

const UserDetailDrawer: React.FC<Props> = ({ open, user, onClose }) => {
  const [detail, setDetail] = useState<UserDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [identities, setIdentities] = useState<UserIdentity[]>([])
  const [sessions, setSessions] = useState<Record<string, LoginSlot>>({})

  useEffect(() => {
    if (open && user) {
      setDetail(null)
      setLoading(true)
      Promise.all([getUserDetail(user.id), getUserIdentities(user.id), getUserSessions(user.id)])
        .then(([detailRes, identityRes, sessionRes]) => {
          setDetail(detailRes.data.data)
          setIdentities(identityRes.data.data || [])
          setSessions(sessionRes.data.data || {})
        })
        .catch(() => {})
        .finally(() => setLoading(false))
    }
  }, [open, user?.id])

  return (
    <Drawer anchor="right" open={open} onClose={onClose}>
      <Box className="w-96 p-6">
        {loading ? (
          <div className="space-y-3">
            <Skeleton variant="rounded" height={32} />
            <Skeleton variant="rounded" height={200} />
          </div>
        ) : detail ? (
          <div className="space-y-4">
            <Typography variant="h6" className="font-semibold">
              {detail.nickname || detail.username}
            </Typography>
            <Divider />
            <Typography variant="subtitle1" className="font-semibold">身份与权限</Typography>
            <div className="flex gap-2 flex-wrap">
              {identities.length === 0 ? <span className="text-gray-500">暂无已开通身份</span> : identities.map(identity => (
                <Chip key={identity.id} label={`${identity.roleType} · ${identity.status}`}
                      color={identity.status === 'ACTIVE' ? 'success' : 'default'} size="small" />
              ))}
            </div>
            <Divider />
            <Typography variant="subtitle1" className="font-semibold">在线会话</Typography>
            <div className="space-y-2 text-sm">
              {Object.keys(sessions).length === 0 ? <span className="text-gray-500">当前无在线会话</span> :
                Object.entries(sessions).map(([clientType, slot]) => (
                  <div key={clientType} className="bg-gray-50 rounded p-2">
                    <b>{clientType}</b> · {slot.activeRole} · 登录时间 {formatDate(new Date(slot.issuedAt).toISOString())}
                  </div>
                ))}
            </div>
            <Divider />
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-gray-500">用户名：</span>
                {detail.username}
              </div>
              <div>
                <span className="text-gray-500">角色：</span>
                <Chip label={getRoleLabel(detail.role)} size="small" />
              </div>
              <div>
                <span className="text-gray-500">手机：</span>
                {detail.phone || '-'}
              </div>
              <div>
                <span className="text-gray-500">状态：</span>
                <Chip
                  label={getStatusLabel(detail.status)}
                  size="small"
                  color={detail.status === 'banned' ? 'error' : 'success'}
                />
              </div>
              <div>
                <span className="text-gray-500">简历数：</span>
                {detail.resumeCount}
              </div>
              <div>
                <span className="text-gray-500">投递数：</span>
                {detail.applicationCount}
              </div>
              <div className="col-span-2">
                <span className="text-gray-500">注册时间：</span>
                {formatDate(detail.createdAt)}
              </div>
            </div>
            {detail.resumeList && detail.resumeList.length > 0 && (
              <>
                <Divider />
                <Typography variant="subtitle1" className="font-semibold">
                  简历信息
                </Typography>
                {detail.resumeList.map(r => (
                  <div key={r.id} className="bg-gray-50 rounded p-3 text-sm space-y-1">
                    <div>
                      <span className="text-gray-500">姓名：</span>
                      {r.realName}
                    </div>
                    <div>
                      <span className="text-gray-500">学历：</span>
                      {r.education || '-'}
                    </div>
                    <div>
                      <span className="text-gray-500">学校：</span>
                      {r.school || '-'}
                    </div>
                    <div>
                      <span className="text-gray-500">技能：</span>
                      {r.skills || '-'}
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        ) : (
          <Typography className="text-gray-400">加载失败</Typography>
        )}
      </Box>
    </Drawer>
  )
}

export default UserDetailDrawer
