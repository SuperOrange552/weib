import React, { useEffect, useState, useCallback } from 'react'
import { getUsers, banUser, unbanUser } from '../api/users'
import { exportUsers } from '../api/auditLogs'
import ConfirmDialog from '../components/ConfirmDialog'
import UserDetailDrawer from './UserDetailDrawer'
import { getRoleLabel, getStatusLabel, getStatusColor, formatDate } from '../utils'
import type { User, PageResponse } from '../types'
import {
  Typography, Button, Chip, TextField, Select, MenuItem, FormControl, InputLabel,
  Table, TableBody, TableCell, TableHead, TableRow, TablePagination, Paper,
  Snackbar, Alert, Stack
} from '@mui/material'

const UserManagementPage: React.FC = () => {
  const [data, setData] = useState<PageResponse<User> | null>(null)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [role, setRole] = useState('')
  const [status, setStatus] = useState('')
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [action, setAction] = useState<'ban' | 'unban' | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [searchCounter, setSearchCounter] = useState(0)
  const [snackbar, setSnackbar] = useState<{
    open: boolean
    msg: string
    severity: 'success' | 'error'
  }>({ open: false, msg: '', severity: 'success' })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getUsers({
        page,
        size,
        role: role || undefined,
        status: status || undefined,
        keyword: keyword || undefined
      })
      setData(res.data.data)
    } catch {
      setSnackbar({ open: true, msg: '加载失败', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [page, size, role, status, keyword])

  useEffect(() => { load() }, [page, role, status, searchCounter])

  const handleSearch = () => {
    if (page === 0) { setSearchCounter(p => p + 1) }
    else { setPage(0) }
  }

  const handleAction = async () => {
    if (!selectedId || !action) return
    setActionLoading(true)
    try {
      if (action === 'ban') await banUser(selectedId)
      else await unbanUser(selectedId)
      setSnackbar({ open: true, msg: '操作成功', severity: 'success' })
      setAction(null)
      load()
    } catch {
      setSnackbar({ open: true, msg: '操作失败', severity: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleExport = async () => {
    try {
      const res = await exportUsers({ role, status, keyword })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = 'users.csv'
      a.click()
      window.URL.revokeObjectURL(url)
      setSnackbar({ open: true, msg: '导出成功', severity: 'success' })
    } catch {
      setSnackbar({ open: true, msg: '导出失败', severity: 'error' })
    }
  }

  const userForAction = data?.content.find(u => u.id === selectedId)

  return (
    <div className="space-y-4">
      <Typography variant="h5" className="font-semibold">用户管理</Typography>
      <Paper className="p-4">
        <Stack direction="row" spacing={2} className="mb-4" alignItems="center">
          <FormControl size="small" className="w-32">
            <InputLabel>角色</InputLabel>
            <Select
              value={role}
              label="角色"
              onChange={e => { setRole(e.target.value); setPage(0) }}
            >
              <MenuItem value="">全部</MenuItem>
              <MenuItem value="seeker">求职者</MenuItem>
              <MenuItem value="boss">Boss</MenuItem>
              <MenuItem value="admin">管理员</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" className="w-32">
            <InputLabel>状态</InputLabel>
            <Select
              value={status}
              label="状态"
              onChange={e => { setStatus(e.target.value); setPage(0) }}
            >
              <MenuItem value="">全部</MenuItem>
              <MenuItem value="active">正常</MenuItem>
              <MenuItem value="banned">已封禁</MenuItem>
            </Select>
          </FormControl>
          <TextField
            size="small"
            label="搜索用户名"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSearch() }}
          />
          <Button variant="outlined" onClick={handleSearch}>搜索</Button>
          <div className="flex-1" />
          <Button variant="outlined" onClick={handleExport}>导出 CSV</Button>
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>用户名</TableCell>
              <TableCell>角色</TableCell>
              <TableCell>状态</TableCell>
              <TableCell>投递数</TableCell>
              <TableCell>注册时间</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(data?.content || []).map(u => (
              <TableRow key={u.id}>
                <TableCell className="font-medium">
                  {u.nickname || u.username}
                </TableCell>
                <TableCell>
                  <Chip label={getRoleLabel(u.role)} size="small" variant="outlined" />
                </TableCell>
                <TableCell>
                  <Chip
                    label={getStatusLabel(u.status)}
                    size="small"
                    color={getStatusColor(u.status)}
                  />
                </TableCell>
                <TableCell>{u.applicationCount}</TableCell>
                <TableCell className="text-gray-500 text-sm">
                  {formatDate(u.createdAt)}
                </TableCell>
                <TableCell>
                  <div className="flex gap-2">
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => { setSelectedUser(u); setDetailOpen(true) }}
                    >
                      查看
                    </Button>
                    {u.status === 'banned' ? (
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        onClick={() => { setSelectedId(u.id); setAction('unban') }}
                      >
                        解封
                      </Button>
                    ) : (
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        onClick={() => { setSelectedId(u.id); setAction('ban') }}
                      >
                        封禁
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {(!data || data.content.length === 0) && !loading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-gray-400 py-8">
                  暂无数据
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        {data && (
          <TablePagination
            component="div"
            count={data.totalElements}
            page={page}
            rowsPerPage={size}
            onPageChange={(_, p) => setPage(p)}
            rowsPerPageOptions={[size]}
            labelRowsPerPage=""
          />
        )}
      </Paper>

      <ConfirmDialog
        open={action === 'ban'}
        title="封禁用户"
        message={`确认封禁用户 ${userForAction?.nickname || userForAction?.username}？封禁后该用户将无法登录。`}
        onConfirm={handleAction}
        onCancel={() => setAction(null)}
        confirmText="确认封禁"
        confirmColor="error"
        loading={actionLoading}
      />
      <ConfirmDialog
        open={action === 'unban'}
        title="解封用户"
        message={`确认解封用户 ${userForAction?.nickname || userForAction?.username}？`}
        onConfirm={handleAction}
        onCancel={() => setAction(null)}
        confirmText="确认解封"
        confirmColor="success"
        loading={actionLoading}
      />
      <UserDetailDrawer
        open={detailOpen}
        user={selectedUser}
        onClose={() => setDetailOpen(false)}
      />
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}
      >
        <Alert severity={snackbar.severity}>{snackbar.msg}</Alert>
      </Snackbar>
    </div>
  )
}

export default UserManagementPage
