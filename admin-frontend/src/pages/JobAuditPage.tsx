import React, { useEffect, useState, useCallback } from 'react'
import { getJobs, approveJob, rejectJob, batchOffline } from '../api/jobs'
import ConfirmDialog from '../components/ConfirmDialog'
import RejectDialog from '../components/RejectDialog'
import { getStatusLabel, getStatusColor, formatDate } from '../utils'
import type { Job, PageResponse } from '../types'
import {
  Typography, Button, Chip, TextField, Select, MenuItem, FormControl, InputLabel,
  Table, TableBody, TableCell, TableHead, TableRow, TablePagination, Paper,
  Snackbar, Alert, Stack, Checkbox
} from '@mui/material'

const JobAuditPage: React.FC = () => {
  const [data, setData] = useState<PageResponse<Job> | null>(null)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [status, setStatus] = useState('pending')
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [checkedIds, setCheckedIds] = useState<Set<number>>(new Set())
  const [showConfirm, setShowConfirm] = useState(false)
  const [showReject, setShowReject] = useState(false)
  const [showBatchConfirm, setShowBatchConfirm] = useState(false)
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
      const res = await getJobs({
        page,
        size,
        status: status || undefined,
        keyword: keyword || undefined
      })
      setData(res.data.data)
    } catch {
      setSnackbar({ open: true, msg: '加载失败', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [page, size, status, keyword])

  useEffect(() => { load() }, [page, status, searchCounter])

  const handleSearch = () => {
    if (page === 0) { setSearchCounter(p => p + 1) }
    else { setPage(0) }
  }

  const handleApprove = async () => {
    if (!selectedId) return
    setActionLoading(true)
    try {
      await approveJob(selectedId)
      setSnackbar({ open: true, msg: '审核通过', severity: 'success' })
      setShowConfirm(false)
      load()
    } catch {
      setSnackbar({ open: true, msg: '操作失败', severity: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleReject = async (reason: string) => {
    if (!selectedId) return
    setActionLoading(true)
    try {
      await rejectJob(selectedId, { reason })
      setSnackbar({ open: true, msg: '已驳回', severity: 'success' })
      setShowReject(false)
      load()
    } catch {
      setSnackbar({ open: true, msg: '操作失败', severity: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleBatchOffline = async () => {
    setActionLoading(true)
    try {
      const res = await batchOffline({ ids: Array.from(checkedIds) })
      setSnackbar({
        open: true,
        msg: `下架成功: ${res.data.data?.successCount || 0} 个`,
        severity: 'success'
      })
      setCheckedIds(new Set())
      setShowBatchConfirm(false)
      load()
    } catch {
      setSnackbar({ open: true, msg: '操作失败', severity: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  const toggleCheck = (id: number) => {
    const next = new Set(checkedIds)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    setCheckedIds(next)
  }

  const toggleAll = () => {
    if (!data) return
    if (checkedIds.size === data.content.length) {
      setCheckedIds(new Set())
    } else {
      setCheckedIds(new Set(data.content.map(j => j.id)))
    }
  }

  return (
    <div className="space-y-4">
      <Typography variant="h5" className="font-semibold">职位审核</Typography>
      <Paper className="p-4">
        <Stack direction="row" spacing={2} className="mb-4" alignItems="center">
          <FormControl size="small" className="w-40">
            <InputLabel>状态</InputLabel>
            <Select
              value={status}
              label="状态"
              onChange={e => { setStatus(e.target.value); setPage(0) }}
            >
              <MenuItem value="pending">待审核</MenuItem>
              <MenuItem value="approved">已通过</MenuItem>
              <MenuItem value="rejected">已驳回</MenuItem>
            </Select>
          </FormControl>
          <TextField
            size="small"
            label="搜索职位"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSearch() }}
          />
          <Button variant="outlined" onClick={handleSearch}>搜索</Button>
          {checkedIds.size > 0 && (
            <Button
              variant="contained"
              color="error"
              onClick={() => setShowBatchConfirm(true)}
            >
              批量下架({checkedIds.size})
            </Button>
          )}
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox">
                <Checkbox
                  size="small"
                  checked={data ? checkedIds.size === data.content.length && data.content.length > 0 : false}
                  indeterminate={checkedIds.size > 0 && data ? checkedIds.size < data.content.length : false}
                  onChange={toggleAll}
                />
              </TableCell>
              <TableCell>职位</TableCell>
              <TableCell>公司</TableCell>
              <TableCell>薪资</TableCell>
              <TableCell>城市</TableCell>
              <TableCell>状态</TableCell>
              <TableCell>时间</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(data?.content || []).map(j => (
              <TableRow key={j.id}>
                <TableCell padding="checkbox">
                  <Checkbox
                    size="small"
                    checked={checkedIds.has(j.id)}
                    onChange={() => toggleCheck(j.id)}
                  />
                </TableCell>
                <TableCell className="font-medium">{j.title}</TableCell>
                <TableCell>{j.companyName}</TableCell>
                <TableCell>
                  {j.salaryMin ? `${j.salaryMin}K-${j.salaryMax}K` : '面议'}
                </TableCell>
                <TableCell>{j.city}</TableCell>
                <TableCell>
                  <Chip
                    label={getStatusLabel(j.auditStatus)}
                    size="small"
                    color={getStatusColor(j.auditStatus)}
                  />
                </TableCell>
                <TableCell className="text-gray-500 text-sm">
                  {formatDate(j.createdAt)}
                </TableCell>
                <TableCell>
                  {j.auditStatus === 'pending' && (
                    <div className="flex gap-2">
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        onClick={() => { setSelectedId(j.id); setShowConfirm(true) }}
                      >
                        通过
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        onClick={() => { setSelectedId(j.id); setShowReject(true) }}
                      >
                        驳回
                      </Button>
                    </div>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {(!data || data.content.length === 0) && !loading && (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-gray-400 py-8">
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
        open={showConfirm}
        title="审核通过"
        message="确认通过该职位的审核？"
        onConfirm={handleApprove}
        onCancel={() => setShowConfirm(false)}
        confirmText="通过"
        confirmColor="success"
        loading={actionLoading}
      />
      <RejectDialog
        open={showReject}
        onConfirm={handleReject}
        onCancel={() => setShowReject(false)}
        loading={actionLoading}
      />
      <ConfirmDialog
        open={showBatchConfirm}
        title="批量下架"
        message={`确认下架选中的 ${checkedIds.size} 个职位？此操作不可撤销。`}
        onConfirm={handleBatchOffline}
        onCancel={() => setShowBatchConfirm(false)}
        confirmText="确认下架"
        confirmColor="error"
        loading={actionLoading}
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

export default JobAuditPage
