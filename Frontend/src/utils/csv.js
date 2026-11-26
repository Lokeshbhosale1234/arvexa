// Quote every field and neutralize spreadsheet formulas in user-controlled text.
export function csvCell(value) {
  let text = String(value ?? '')
  if (/^[\s\uFEFF]*[=+@-]/u.test(text) || /^[\t\r\n]/u.test(text)) text = "'" + text
  return '"' + text.replaceAll('"', '""') + '"'
}

export function transactionsToCsv(transactions) {
  const rows = [['Date', 'Title', 'Description', 'Type', 'Category', 'Status', 'Amount (INR)']]
  for (const t of transactions) {
    rows.push([t.transactionDate, t.title, t.description, t.type, t.category, t.status, t.amount])
  }
  return '\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n')
}

export function downloadTransactions(transactions) {
  const blob = new Blob([transactionsToCsv(transactions)], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `arvexa-transactions-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
