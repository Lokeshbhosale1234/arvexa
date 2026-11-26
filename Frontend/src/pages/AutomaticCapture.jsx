import React, { useCallback, useEffect, useState } from 'react'
import { captureApi } from '../api/captureApi'
import ErrorBanner from '../components/ui/ErrorBanner'
import { CATEGORIES } from '../utils/formatters'

const BANKS = ['HDFC', 'SBI', 'ICICI', 'AXIS', 'KOTAK', 'PNB', 'BOI', 'IDFC', 'INDUSIND', 'CANARA', 'UNION']
const inputStyle = { width: '100%', padding: 10, borderRadius: 8, background: 'var(--bg-input)', color: 'var(--text-primary)', border: '1px solid var(--border)' }
const money = value => Number(value).toLocaleString('en-IN', { style: 'currency', currency: 'INR' })

function ReviewAlert({ event, accounts, onConfirm, onIgnore, busy }) {
  const matches = accounts.filter(a => a.bank === event.bank && (!event.suffix || a.suffix === event.suffix))
  const [accountId, setAccountId] = useState('')
  const [category, setCategory] = useState('Uncategorized')
  return <div className="card" style={{ padding: 16 }}>
    <p><strong>{event.bank} · {event.suffix ? `••${event.suffix}` : 'Account not identified'}</strong></p>
    <p>{event.direction === 'CREDIT' ? '+' : '−'}{money(event.amount)} · {new Date(event.observedAt).toLocaleString('en-IN')}</p>
    <p style={{ color: 'var(--text-secondary)', fontSize: 12, margin: '8px 0' }}>Reference: {event.reference || 'Not present in message'}. Check this entry before confirming; it has not changed your totals.</p>
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'end' }}>
      <label style={{ flex: 1, minWidth: 180 }}>Account<select aria-label={`Account for alert ${event.id}`} value={accountId} onChange={e => setAccountId(e.target.value)} style={inputStyle}>
        <option value="">Select matching account</option>{matches.map(a => <option key={a.id} value={a.id}>{a.label} · ••{a.suffix}</option>)}
      </select></label>
      <label style={{ flex: 1, minWidth: 150 }}>Category<select value={category} onChange={e => setCategory(e.target.value)} style={inputStyle}>
        {['Uncategorized', ...CATEGORIES.filter(c => c !== 'Uncategorized')].map(c => <option key={c}>{c}</option>)}
      </select></label>
      <button className="btn btn-primary" disabled={busy || !accountId} onClick={() => onConfirm(event.id, accountId, category)}>Confirm entry</button>
      <button className="btn btn-ghost" disabled={busy} onClick={() => onIgnore(event.id)}>Ignore</button>
    </div>
    {matches.length === 0 && <p role="status" style={{ marginTop: 10 }}>Add the matching bank account below first.</p>}
  </div>
}

export default function AutomaticCapture() {
  const [accounts, setAccounts] = useState([]), [events, setEvents] = useState([]), [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(true), [error, setError] = useState(null), [busy, setBusy] = useState(false)
  const [form, setForm] = useState({ bank: 'HDFC', suffix: '', label: '' })
  const [review, setReview] = useState([])
  const [updatedAt, setUpdatedAt] = useState(null)
  const load = useCallback(async () => {
    try {
      const [a, e, d, pending] = await Promise.all([captureApi.accounts(), captureApi.events(), captureApi.devices(), captureApi.review()])
      setAccounts(a); setEvents(e); setDevices(d); setReview(pending); setUpdatedAt(new Date()); setError(null)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }, [])
  useEffect(() => {
    let active = true
    const refresh = () => { if (active && !document.hidden) load() }
    refresh()
    const timer = setInterval(refresh, 15000)
    document.addEventListener('visibilitychange', refresh)
    return () => { active = false; clearInterval(timer); document.removeEventListener('visibilitychange', refresh) }
  }, [load])
  const act = async fn => {
    setBusy(true); setError(null)
    try { await fn(); await load(); window.dispatchEvent(new Event('arvexa:transactions-changed')) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <div className="page-enter" style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
    <div><h2 style={{ fontSize: 24, fontWeight: 700 }}>Automatic Capture</h2><p style={{ color: 'var(--text-secondary)', marginTop: 6 }}>Connect your Android phone once. Supported bank SMS will appear in your transaction history automatically.</p></div>
    <ErrorBanner message={error} onRetry={load} />
    <section className="card" style={{ padding: 20 }} aria-labelledby="capture-setup">
      <h3 id="capture-setup">One-time setup</h3>
      <ol style={{ paddingLeft: 20, lineHeight: 1.9, marginTop: 8 }}><li>Add each supported bank and the last four account digits below.</li><li>Install Arvexa Capture from the repository’s Android build artifact.</li><li>Sign in on the phone, accept the disclosure, and enable SMS permission.</li></ol>
      <p style={{ marginTop: 10, color: 'var(--text-secondary)' }}>The website cannot read your phone’s messages. This requires the Android app. Capture covers new supported SMS only; delivery may be delayed. It is not a live bank connection.</p>
      <p style={{ fontSize: 12, marginTop: 8 }}>Raw SMS and OTPs are never uploaded. Unmapped accounts or missing references are held for review. Revoke a device below to stop its access.</p>
      <a href="https://github.com/Lokeshbhosale1234/arvexa/actions" target="_blank" rel="noreferrer" className="btn btn-ghost" style={{ marginTop: 12 }}>Open Android builds</a>
    </section>
    <section aria-labelledby="capture-accounts"><h3 id="capture-accounts" style={{ marginBottom: 10 }}>Your bank accounts</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
        {accounts.map(a => <div key={a.id} className="card" style={{ padding: 18 }}><strong>{a.label}</strong><p>{a.bank} · ••{a.suffix}</p><p className="font-mono" style={{ fontSize: 22, marginTop: 10 }}>{money(a.trackedNet)}</p><p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>Net captured movement · {a.transactions} entries. Excludes opening balance and missing/manual transactions.</p></div>)}
      </div>
      {!loading && accounts.length === 0 && <p>No bank accounts mapped yet.</p>}
      <form onSubmit={e => { e.preventDefault(); act(async () => { await captureApi.addAccount(form); setForm({ bank: 'HDFC', suffix: '', label: '' }) }) }} className="card" style={{ padding: 16, marginTop: 12 }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 12 }}>
          <label>Bank<select value={form.bank} onChange={e => setForm({ ...form, bank: e.target.value })} style={inputStyle}>{BANKS.map(b => <option key={b}>{b}</option>)}</select></label>
          <label>Last four account digits<input required inputMode="numeric" pattern="[0-9]{4}" minLength={4} maxLength={4} value={form.suffix} onChange={e => setForm({ ...form, suffix: e.target.value.replace(/\D/g, '') })} style={inputStyle} /></label>
          <label>Account label<input required maxLength={60} placeholder="e.g. Salary account" value={form.label} onChange={e => setForm({ ...form, label: e.target.value })} style={inputStyle} /></label>
          <button className="btn btn-primary" style={{ alignSelf: 'end' }} disabled={busy}>Add account</button>
        </div>
      </form>
    </section>
    <section aria-labelledby="capture-review"><h3 id="capture-review" style={{ marginBottom: 10 }}>Needs review · {review.length}{review.length === 100 ? ' (showing 100; handle these to load more)' : ''}</h3>
      {review.map(e => <ReviewAlert key={e.id} event={e} accounts={accounts} busy={busy} onConfirm={(id, a, c) => act(() => captureApi.confirm(id, a, c))} onIgnore={id => act(() => captureApi.ignore(id))} />)}
      {!loading && review.length === 0 && <p style={{ color: 'var(--text-secondary)' }}>No alerts waiting for review.</p>}
    </section>
    <section className="card" style={{ padding: 20 }} aria-labelledby="capture-devices"><h3 id="capture-devices">Connected devices</h3>
      {devices.map(d => <div key={d.id} style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 10, padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
        <div><strong>{d.name}</strong><p style={{ fontSize: 12 }}>{d.revoked ? 'Revoked' : new Date(d.expiresAt) < new Date() ? 'Expired — reconnect' : 'Connected'} · Last contact: {d.lastSeenAt ? new Date(d.lastSeenAt).toLocaleString() : 'Not yet synced'}</p></div>
        {!d.revoked && <button disabled={busy} className="btn btn-ghost" onClick={() => { if (window.confirm('Revoke this device? Its future sync requests will be rejected.')) act(() => captureApi.revoke(d.id)) }}>Revoke</button>}
      </div>)}
      {!loading && devices.length === 0 && <p style={{ marginTop: 8 }}>No phones connected yet.</p>}
    </section>
    <section className="card" style={{ padding: 20 }} aria-labelledby="capture-history"><h3 id="capture-history">Latest captured alerts</h3>
      <p style={{ fontSize: 12, color: 'var(--text-secondary)', margin: '6px 0 12px' }}>Up to 100 alerts · {updatedAt ? `Refreshed ${updatedAt.toLocaleTimeString()}` : 'Loading…'} · Times are SMS times, not verified bank posting times.</p>
      <div style={{ overflowX: 'auto' }}><table className="data-table"><thead><tr><th>Bank / account</th><th>SMS time</th><th>Amount</th><th>Reference</th><th>State</th></tr></thead><tbody>
        {events.map(e => <tr key={e.id}><td>{e.bank} · {e.suffix ? `••${e.suffix}` : 'Unmapped'}</td><td>{new Date(e.observedAt).toLocaleString()}</td><td>{e.direction === 'CREDIT' ? '+' : '−'}{money(e.amount)}</td><td>{e.reference || 'Missing'}</td><td>{e.state.replaceAll('_', ' ')}</td></tr>)}
      </tbody></table></div>
      {!loading && events.length === 0 && <p>No supported alerts received yet.</p>}
    </section>
  </div>
}
