import { useState, useEffect, useCallback } from 'react'
import { transactionApi } from '../api/transactionApi'

export function useAnalytics() {
  const [analytics, setAnalytics] = useState(null)
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState(null)

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await transactionApi.getAnalytics()
      setAnalytics(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const refresh = () => { if (!document.hidden) fetchAnalytics() }
    refresh()
    const timer = setInterval(refresh, 30000)
    window.addEventListener('arvexa:transactions-changed', refresh)
    document.addEventListener('visibilitychange', refresh)
    return () => {
      clearInterval(timer)
      window.removeEventListener('arvexa:transactions-changed', refresh)
      document.removeEventListener('visibilitychange', refresh)
    }
  }, [fetchAnalytics])

  return { analytics, loading, error, refetch: fetchAnalytics }
}
