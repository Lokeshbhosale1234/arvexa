import api from './axios'
export const captureApi = {
  accounts: () => api.get('/capture/accounts').then(r => r.data.data),
  addAccount: data => api.post('/capture/accounts', data).then(r => r.data.data),
  events: () => api.get('/capture/events').then(r => r.data.data),
  review: () => api.get('/capture/events', { params: { state: 'NEEDS_REVIEW' } }).then(r => r.data.data),
  devices: () => api.get('/capture/devices').then(r => r.data.data),
  revoke: id => api.delete(`/capture/devices/${id}`),
  confirm: (id, accountId, category) => api.post(`/capture/events/${id}/confirm`, { accountId: Number(accountId), category }),
  ignore: id => api.post(`/capture/events/${id}/ignore`),
}
