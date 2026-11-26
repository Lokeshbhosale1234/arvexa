import test, { after } from 'node:test'
import assert from 'node:assert/strict'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

const vite = await createServer({ server: { middlewareMode: true }, appType: 'custom', logLevel: 'error' })
after(() => vite.close())
const { default: Score } = await vite.ssrLoadModule('/src/components/dashboard/HealthScoreWidget.jsx')
const { default: Recommendations } = await vite.ssrLoadModule('/src/components/dashboard/AIRecommendations.jsx')

test('missing financial score renders unavailable instead of invented value', () => {
  const html = renderToStaticMarkup(React.createElement(Score, { error: 'Insights unavailable' }))
  assert.ok(html.includes('Insights unavailable'))
  assert.ok(!html.includes('/100'))
  assert.ok(!html.includes('72'))
})
test('empty insights never fabricate personalized spending', () => {
  const html = renderToStaticMarkup(React.createElement(Recommendations, { insights: [] }))
  assert.ok(html.includes('No insights to show yet'))
  assert.ok(!html.includes('8,400'))
  assert.ok(!html.includes('Live AI'))
})
test('failed insights render the error rather than recommendations', () => {
  const html = renderToStaticMarkup(React.createElement(Recommendations, { error: 'Service unavailable' }))
  assert.ok(html.includes('Service unavailable'))
})
test('actual insight titles are displayed and escaped as text', () => {
  const html = renderToStaticMarkup(React.createElement(Recommendations, { insights: [{ title: '<script>hello</script>', message: 'Test record' }] }))
  assert.ok(html.includes('&lt;script&gt;hello&lt;/script&gt;'))
  assert.ok(!html.includes('<script>'))
})


test('capture setup is honest about requiring a phone and net movement', async () => {
  const { default: Capture } = await vite.ssrLoadModule('/src/pages/AutomaticCapture.jsx')
  const html = renderToStaticMarkup(React.createElement(Capture))
  assert.ok(html.includes('This requires the Android app'))
  assert.ok(html.includes('Last four account digits'))
  assert.ok(html.includes('No supported alerts received yet') === false) // loading state, not invented history
  assert.ok(!html.includes('8550'))
})
