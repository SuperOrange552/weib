import React, { useEffect, useState, useCallback } from 'react'
import { getCompanies, approveCompany, rejectCompany } from '../api/companies'
import ConfirmDialog from '../components/ConfirmDialog'
import RejectDialog from '../components/RejectDialog'
import { getStatusLabel, getStatusColor, formatDate } from '../utils'
import type { Company, PageResponse } from '../types'
import {
  Typography, Button, Chip, TextField, Select, MenuItem, FormControl, InputLabel,
  Table, TableBody, TableCell, TableHead, TableRow, TablePagination, Paper,
  Snackbar, Alert, Stack
} from '@mui/material'

const CompanyAuditPage: React.FC = () => {
  const [data, setData] = useState<PageResponse<Company> | null>(null)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [status, setStatus] = useState('pending')
  const [keyword, setKeyword] = useState('')
  const [loading, setLoading] = useState(true)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [showConfirm, setShowConfirm] = useState(false)
  const [showReject, setShowReject] = useState(false)
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
      const res = await getCompanies({
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
      await approveCompany(selectedId)
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
      await rejectCompany(selectedId, { reason })
      setSnackbar({ open: true, msg: '已驳回', severity: 'success' })
      setShowReject(false)
      load()
    } catch {
      setSnackbar({ open: true, msg: '操作失败', severity: 'error' })
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="space-y-4">
      <Typography variant="h5" className="font-semibold">公司审核</Typography>
      <Paper className="p-4">
        <Stack direction="row" spacing={2} className="mb-4">
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
            label="搜索公司名"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSearch() }}
          />
          <Button variant="outlined" onClick={handleSearch}>搜索</Button>
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>公司名</TableCell>
              <TableCell>行业</TableCell>
              <TableCell>Boss</TableCell>
              <TableCell>状态</TableCell>
              <TableCell>注册时间</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(data?.content || []).map(c => (
              <TableRow key={c.id}>
                <TableCell className="font-medium">{c.name}</TableCell>
                <TableCell>{c.industry}</TableCell>
                <TableCell>{c.bossName}</TableCell>
                <TableCell>
                  <Chip
                    label={getStatusLabel(c.auditStatus)}
                    size="small"
                    color={getStatusColor(c.auditStatus)}
                  />
                </TableCell>
                <TableCell className="text-gray-500 text-sm">
                  {formatDate(c.createdAt)}
                </TableCell>
                <TableCell>
                  {c.auditStatus === 'pending' && (
                    <div className="flex gap-2">
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        onClick={() => { setSelectedId(c.id); setShowConfirm(true) }}
                      >
                        通过
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        onClick={() => { setSelectedId(c.id); setShowReject(true) }}
                      >
                        驳回
                      </Button>
                    </div>
                  )}
                  {c.auditStatus !== 'pending' && c.auditReason && (
                    <span className="text-xs text-gray-400">{c.auditReason}</span>
                  )}
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
        open={showConfirm}
        title="审核通过"
        message="确认通过该公司的注册审核？"
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

export default CompanyAuditPage
