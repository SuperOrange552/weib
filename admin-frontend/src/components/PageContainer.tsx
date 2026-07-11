import React from 'react'
import { Box } from '@mui/material'

const PageContainer: React.FC<React.PropsWithChildren> = ({ children }) => (
  <Box sx={{ width: '100%', maxWidth: 1200, mx: 'auto' }}>{children}</Box>
)
export default PageContainer
