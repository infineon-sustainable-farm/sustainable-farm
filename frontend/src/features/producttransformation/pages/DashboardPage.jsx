import React, { useEffect, useState } from 'react'
import { 
  AreaChart, 
  Area, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  Legend,
  ResponsiveContainer
} from 'recharts'
import { 
  BarChart,
  Bar,
} from 'recharts'
import { PieChart, Pie, Cell } from 'recharts'
import { batchApis, dashboardApis } from '@/api/api-client'
import { useAuth } from '@/hooks/useAuth'

export function DashboardPage() {
  const { userRole } = useAuth()
  const [kpis, setKpis] = useState(null)
  const [qualityTrend, setQualityTrend] = useState(null)
  const [equipmentUtilization, setEquipmentUtilization] = useState(null)
  const [harvestTrend, setHarvestTrend] = useState(null)
  const [energyConsumption, setEnergyConsumption] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [kpiRes, trendRes, equipRes, harvestRes, energyRes] = await Promise.all([
          dashboardApis.getKPIs(),
          dashboardApis.getQualityTrend(),
          dashboardApis.getEquipmentUtilization(),
          dashboardApis.getHarvestTrend(),
          dashboardApis.getEnergyConsumption(),
        ])
        setKpis(kpiRes)
        setQualityTrend(trendRes)
        setEquipmentUtilization(equipRes)
        setHarvestTrend(harvestRes)
        setEnergyConsumption(energyRes)
      } catch (err) {
        console.error('Failed to fetch dashboard data:', err)
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  if (loading) {
    return (
      <div className="p-6">
        <h1 className="font-heading text-2xl font-bold mb-4">Dashboard</h1>
        <div className="animate-pulse bg-gray-200 h-8 w-3/4 rounded mb-4"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map((_, i) => (
            <div key={i} className="bg-white p-4 rounded shadow animate-pulse h-32">
              <div className="h-full bg-gray-200 rounded"></div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-heading text-2xl font-bold">Dashboard</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">
            Last updated: {new Date().toLocaleTimeString()}
          </span>
        </div>
      </div>

      {/* KPIs Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {/* Total Batches */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-2">Total Batches</h3>
          <p className="text-3xl font-bold text-primary">{kpis?.totalBatches ?? 0}</p>
        </div>
        
        {/* Active Batches */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-2">Active Batches</h3>
          <p className="text-3xl font-bold text-info">{kpis?.activeBatches ?? 0}</p>
        </div>
        
        {/* Completed Batches */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-2">Completed Batches</h3>
          <p className="text-3xl font-bold text-success">{kpis?.completedBatches ?? 0}</p>
        </div>
        
        {/* Rejected Batches */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-2">Rejected Batches</h3>
          <p className="text-3xl font-bold text-danger">{kpis?.rejectedBatches ?? 0}</p>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Quality Trend */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-4">Quality Control Trend</h3>
          {qualityTrend ? (
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={qualityTrend.dates.map((date, index) => ({
                name: date,
                pass: qualityTrend.passRate[index] || 0,
                reject: qualityTrend.rejectRate[index] || 0,
              }))}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Area type="monotone" dataKey="pass" stroke="#10B981" fillOpacity={{ 0: 0, 1: 0.1 }} />
                <Area type="monotone" dataKey="reject" stroke="#EF4444" fillOpacity={{ 0: 0, 1: 0.1 }} />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-32 flex items-center justify-center text-gray-400">
              Loading quality trend...
            </div>
          )}
        </div>

        {/* Equipment Utilization */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-4">Equipment Utilization</h3>
          {equipmentUtilization && equipmentUtilization.length > 0 ? (
            <BarChart data={equipmentUtilization.slice(0, 5)}> {/* Top 5 */}
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="equipmentId" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="utilizationPct" barSize="40%" fill="#3B82F6" />
            </BarChart>
          ) : (
            <div className="h-32 flex items-center justify-center text-gray-400">
              No equipment utilization data
            </div>
          )}
        </div>
      </div>

      {/* Harvest and Energy Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Harvest Trend */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-4">Harvest Trend</h3>
          {harvestTrend ? (
            <LineChart data={harvestTrend.labels.map((label, index) => ({
              name: label,
              value: harvestTrend.data[index] || 0,
            }))}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="value" stroke="#8B5CF6" strokeWidth={2} />
              <CartesianGrid strokeDasharray="3 3" />
            </LineChart>
          ) : (
            <div className="h-32 flex items-center justify-center text-gray-400">
              Loading harvest trend...
            </div>
          )}
        </div>

        {/* Energy Consumption */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="font-medium text-gray-500 mb-4">Energy Consumption</h3>
          {energyConsumption ? (
            <AreaChart data={energyConsumption.labels.map((label, index) => ({
              name: label,
              value: energyConsumption.data[index] || 0,
            }))}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Area type="monotone" dataKey="value" stroke="#F59E0B" fillOpacity={{ 0: 0, 1: 0.1 }} />
            </AreaChart>
          ) : (
            <div className="h-32 flex items-center justify-center text-gray-400">
              Loading energy consumption...
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// Chart component imports - ensure these are available
function LineChart(props) {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart {...props}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Area type="monotone" dataKey="value" stroke="#8B5CF6" strokeWidth={2} fillOpacity={{ 0: 0, 1: 0.1 }} />
      </AreaChart>
    </ResponsiveContainer>
  )
}