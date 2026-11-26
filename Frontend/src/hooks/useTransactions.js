import { useState, useEffect, useCallback } from 'react'
import { transactionApi } from '../api/transactionApi'

export function useTransactions() {
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState(null)

  const fetchTransactions = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await transactionApi.getAll()
      setTransactions(data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const refresh = () => { if (!document.hidden) fetchTransactions() }
    refresh()
    const timer = setInterval(refresh, 30000)
    window.addEventListener('arvexa:transactions-changed', refresh)
    document.addEventListener('visibilitychange', refresh)
    return () => {
      clearInterval(timer)
      window.removeEventListener('arvexa:transactions-changed', refresh)
      document.removeEventListener('visibilitychange', refresh)
    }
  }, [fetchTransactions])

  const addTransaction = useCallback(async (payload) => {
    const created = await transactionApi.create(payload)
    setTransactions((prev) => [created, ...prev])
    window.dispatchEvent(new Event('arvexa:transactions-changed'))
    return created
  }, [])

  const deleteTransaction = useCallback(async (id) => {
    await transactionApi.delete(id)
    setTransactions((prev) => prev.filter((t) => t.id !== id))
    window.dispatchEvent(new Event('arvexa:transactions-changed'))
  }, [])

  return { transactions, loading, error, addTransaction, deleteTransaction, refetch: fetchTransactions }
}
