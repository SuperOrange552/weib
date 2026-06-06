import React from 'react'

interface Props {
  title: string
  value: number | string
  icon?: React.ReactNode
  color?: 'blue' | 'green' | 'orange' | 'red'
}

const colorMap: Record<string, string> = {
  blue: 'bg-blue-50 text-blue-600 border-blue-200',
  green: 'bg-green-50 text-green-600 border-green-200',
  orange: 'bg-orange-50 text-orange-600 border-orange-200',
  red: 'bg-red-50 text-red-600 border-red-200'
}

const StatCard: React.FC<Props> = ({ title, value, icon, color = 'blue' }) => {
  return (
    <div className={`rounded-lg border p-5 ${colorMap[color]}`}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm opacity-75">{title}</p>
          <p className="text-2xl font-bold mt-1">{value}</p>
        </div>
        {icon && <div className="text-3xl opacity-50">{icon}</div>}
      </div>
    </div>
  )
}

export default StatCard
