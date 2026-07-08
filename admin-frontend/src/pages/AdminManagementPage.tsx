import React, { useEffect, useState } from 'react'
import { getAdmins, createAdmin, disableAdmin } from '../api/admins'
import { getAdminRoleLabel } from '../utils'
import type { SubAdmin } from '../types'
import {
  Typography, Button, Table, TableBody, TableCell, TableHead, TableRow,
  Paper, Snackbar, Alert, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Select, MenuItem, FormControl, InputLabel, Chip
} from '@mui/material'

const AdminManagementPage: React.FC = () => {
  const [admins, setAdmins] = useState<SubAdmin[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [newUsername, setNewUsername] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newRole, setNewRole] = useState('auditor')
  const [createLoading, setCreateLoading] = useState(false)
  const [snackbar, setSnackbar] = useState<{
    open: boolean
    msg: string
    severity: 'success' | 'error'
  }>({ open: false, msg: '', severity: 'success' })

  const load = async () => {
    setLoading(true)
    try {
      const res = await getAdmins()
      setAdmins(res.data.data || [])
    } catch {
      setSnackbar({ open: true, msg: '加载失败', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async () => {
    if (!newUsername.trim() || !newPassword.trim()) {
      setSnackbar({ open: true, msg: '请填写用户名和密码', severity: 'error' })
      return
    }
    if (newUsername.trim().length < 3 || newUsername.trim().length > 32 || !/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(newUsername.trim())) {
      setSnackbar({ open: true, msg: '??????3-32??????????????????', severity: 'error' })
      return
    }
    if (newPassword.length < 8 || newPassword.length > 64 || !/[a-z]/.test(newPassword) || !/[A-Z]/.test(newPassword) || !/\d/.test(newPassword)) {
      setSnackbar({ open: true, msg: '?????8-64????????????', severity: 'error' })
      return
    }
    setCreateLoading(true)
    try {
      await createAdmin({
        username: newUsername,
        password: newPassword,
        roleType: newRole
      })
      setSnackbar({ open: true, msg: '创建成功', severity: 'success' })
      setShowCreate(false)
      setNewUsername('')
      setNewPassword('')
      setNewRole('auditor')
      load()
    } catch (err: any) {
      setSnackbar({
        open: true,
        msg: err.response?.data?.msg || '创建失败',
        severity: 'error'
      })
    } finally {
      setCreateLoading(false)
    }
  }

  const handleDisable = async (userId: number) => {
    try {
      await disableAdmin(userId)
      setSnackbar({ open: true, msg: '已禁用', severity: 'success' })
      load()
    } catch {
      setSnackbar({ open: true, msg: '操作失败', severity: 'error' })
    }
  }

  return (
    <div className="space-y-4">
      <Typography variant="h5" className="font-semibold">管理员管理</Typography>
      <Paper className="p-4">
        <Button
          variant="contained"
          onClick={() => setShowCreate(true)}
          className="mb-4"
        >
          新建管理员
        </Button>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>用户名</TableCell>
              <TableCell>角色</TableCell>
              <TableCell>创建时间</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {admins.map(a => (
              <TableRow key={a.userId}>
                <TableCell className="font-medium">
                  {a.nickname || a.username}
                </TableCell>
                <TableCell>
                  <Chip label={getAdminRoleLabel(a.roleType)} size="small" />
                </TableCell>
                <TableCell className="text-gray-500">{a.createdAt}</TableCell>
                <TableCell>
                  <Button
                    size="small"
                    variant="outlined"
                    color="error"
                    onClick={() => handleDisable(a.userId)}
                  >
                    禁用
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {admins.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-gray-400 py-8">
                  暂无数据
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>

      <Dialog
        open={showCreate}
        onClose={() => setShowCreate(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>新建管理员</DialogTitle>
        <DialogContent className="space-y-3 mt-2">
          <TextField
            fullWidth
            label="用户名"
            value={newUsername}
            onChange={e => setNewUsername(e.target.value)}
            inputProps={{ minLength: 3, maxLength: 32 }}
            helperText="3-32?????????????"
          />
          <TextField
            fullWidth
            label="密码"
            type="password"
            value={newPassword}
            onChange={e => setNewPassword(e.target.value)}
            inputProps={{ minLength: 8, maxLength: 64 }}
            helperText="8-64??????????????"
          />
          <FormControl fullWidth>
            <InputLabel>角色</InputLabel>
            <Select
              value={newRole}
              label="角色"
              onChange={e => setNewRole(e.target.value)}
            >
              <MenuItem value="super_admin">超级管理员</MenuItem>
              <MenuItem value="auditor">审核员</MenuItem>
              <MenuItem value="viewer">观察员</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowCreate(false)}>取消</Button>
          <Button
            variant="contained"
            onClick={handleCreate}
            disabled={createLoading}
          >
            {createLoading ? '创建中...' : '创建'}
          </Button>
        </DialogActions>
      </Dialog>

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

export default AdminManagementPage
