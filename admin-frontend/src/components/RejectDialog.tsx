import React, { useState } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, TextField } from '@mui/material'

interface Props {
  open: boolean
  onConfirm: (reason: string) => void
  onCancel: () => void
  loading?: boolean
}

const RejectDialog: React.FC<Props> = ({ open, onConfirm, onCancel, loading = false }) => {
  const [reason, setReason] = useState('')

  const handleConfirm = () => {
    if (reason.trim()) {
      onConfirm(reason.trim())
      setReason('')
    }
  }

  return (
    <Dialog open={open} onClose={onCancel} maxWidth="sm" fullWidth>
      <DialogTitle>驳回理由</DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          fullWidth
          multiline
          rows={3}
          variant="outlined"
          placeholder="请输入驳回理由..."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          sx={{ mt: 1 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancel} disabled={loading}>取消</Button>
        <Button
          onClick={handleConfirm}
          color="error"
          variant="contained"
          disabled={!reason.trim() || loading}
        >
          {loading ? '处理中...' : '确认驳回'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default RejectDialog
