import test from 'node:test'
import assert from 'node:assert/strict'
import { csvCell, transactionsToCsv } from './csv.js'

test('quotes commas, quotes, line breaks, and preserves Unicode', () => {
  assert.equal(csvCell('Cafe, "Pune"\n₹100'), '"Cafe, ""Pune""\n₹100"')
})
test('neutralizes spreadsheet formulas including leading whitespace', () => {
  for (const value of ['=SUM(A1)', '+CMD', '-CMD', '@SUM(A1)', '  =1+1', '\t=1+1']) {
    assert.ok(csvCell(value).startsWith('"\''))
  }
})
test('exports only supplied rows and retains decimal amounts', () => {
  const csv = transactionsToCsv([{ title: 'Lunch', amount: '125.50', type: 'DEBIT' }])
  assert.ok(csv.startsWith('\uFEFF"Date"'))
  assert.equal(csv.split('\r\n').length, 2)
  assert.ok(csv.includes('"125.50"'))
  assert.ok(!csv.includes('undefined'))
})
test('empty export contains headers without fictional records', () => {
  assert.equal(transactionsToCsv([]).split('\r\n').length, 1)
})
