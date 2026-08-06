import { useMemo, useState } from 'react'
import { Icon } from '../components/Icon'
import { Card } from '../components/UI'
import { currency, number } from '../utils/format'
import { targetPeriodLabel, targetPeriodRange, type TargetPeriod, type TargetPeriodRange } from '../utils/targetPeriods'

const moneyTargets = new Set(['OTC $/Customer', 'GP ($)'])
const metricRates: Record<string, { actual: number; target1: number; target2: number }> = {
  'Number of Scripts': { actual: 125, target1: 775, target2: 775 },
  'OTC $/Customer': { actual: 7.8, target1: 9.8, target2: 10.69 },
  'GP ($)': { actual: 1080, target1: 1450, target2: 1540 },
  'Scripts on File': { actual: 1120, target1: 1750, target2: 1900 }
}

interface SeriesPoint { date: Date; actual: number | null; target1: number; target2: number }

function dayWeight(date: Date) { return date.getDay() === 0 ? 0 : date.getDay() === 6 ? .65 : 1 }

function buildSeries(name: string, range: TargetPeriodRange): SeriesPoint[] {
  const rates = metricRates[name]
  let actual = 0
  let target1 = 0
  let target2 = 0
  return range.dates.map((date, index) => {
    const weight = dayWeight(date)
    target1 += rates.target1 * weight
    target2 += rates.target2 * weight
    const hasActual = date <= range.today
    if (hasActual) {
      const variation = .91 + ((date.getDate() * 7 + index * 3) % 17) / 100
      actual += rates.actual * weight * variation
    }
    return { date, actual: hasActual ? actual : null, target1, target2 }
  })
}

function axisTicks(period: TargetPeriod, points: SeriesPoint[]) {
  if (period === 'WTD') return points.map((point, index) => ({ index, label: point.date.toLocaleDateString('en-AU', { weekday: 'short' }).toUpperCase() }))
  if (period === 'MTD') {
    const indexes = [0, 4, 9, 14, 19, 24, points.length - 1]
    return [...new Set(indexes)].map((index) => ({ index, label: String(points[index].date.getDate()) }))
  }
  return points.flatMap((point, index) => point.date.getDate() === 1 ? [{ index, label: point.date.toLocaleDateString('en-AU', { month: 'short' }).toUpperCase() }] : [])
}

function chartPath(values: Array<number | null>, max: number) {
  const available = values.length - 1 || 1
  return values.flatMap((value, index) => value === null ? [] : [`${index === 0 ? 'M' : 'L'}${52 + index / available * 676} ${326 - value / max * 288}`]).join(' ')
}

function LineChart({ name, period, points }: { name: string; period: TargetPeriod; points: SeriesPoint[] }) {
  const max = Math.max(...points.flatMap((point) => [point.actual ?? 0, point.target1, point.target2]), 1)
  const ticks = axisTicks(period, points)
  const xAxisLabel = period === 'YTD' ? 'Month' : 'Day'
  return <svg className="target-line-chart" viewBox="0 0 780 360" preserveAspectRatio="none" aria-label={`${name} ${period} target chart`}>
    <path className="target-axis" d="M52 18V326H728" />
    {ticks.map((tick) => <text key={tick.index} x={52 + tick.index / Math.max(points.length - 1, 1) * 676} y="346" textAnchor="middle">{tick.label}</text>)}
    <text className="target-axis-label" x="390" y="359" textAnchor="middle">{xAxisLabel}</text>
    <path className="target-line target-line--one" d={chartPath(points.map((point) => point.target1), max)} /><path className="target-line target-line--two" d={chartPath(points.map((point) => point.target2), max)} /><path className="target-line target-line--actual" d={chartPath(points.map((point) => point.actual), max)} />
  </svg>
}

function TargetTable({ name, points }: { name: string; points: SeriesPoint[] }) {
  const format = moneyTargets.has(name) ? currency.format : number.format
  const dateFormat = new Intl.DateTimeFormat('en-AU', { day: '2-digit', month: '2-digit', year: 'numeric' })
  return <div className="target-table-scroll"><table className="target-mini-table"><thead><tr><th>Date</th><th>Actual</th><th>Target 1</th><th>Target 2</th></tr></thead><tbody>{points.map((point) => <tr key={point.date.toISOString()}><td>{dateFormat.format(point.date)}</td><td>{point.actual === null ? '' : format(point.actual)}</td><td>{format(point.target1)}</td><td>{format(point.target2)}</td></tr>)}</tbody></table></div>
}

function varianceMessage(value: number, money: boolean, targetNumber: number) {
  const amount = money ? currency.format(Math.abs(value)) : number.format(Math.abs(value))
  return `${amount} ${value >= 0 ? 'left until you hit' : 'over'} Target ${targetNumber}!`
}

function LineTargetCard({ name, period, range, table, onFlip }: { name: string; period: TargetPeriod; range: TargetPeriodRange; table: boolean; onFlip: () => void }) {
  const points = useMemo(() => buildSeries(name, range), [name, range])
  const latest = [...points].reverse().find((point) => point.actual !== null) ?? points[0]
  const actual = latest.actual ?? 0
  const variance1 = latest.target1 - actual
  const variance2 = latest.target2 - actual
  const showVariance = name !== 'GP ($)'
  return <Card className="desktop-target-card"><header><div><h2>{name}</h2>{showVariance && <><p>{varianceMessage(variance1, moneyTargets.has(name), 1)}</p><p>{varianceMessage(variance2, moneyTargets.has(name), 2)}</p></>}</div><div className="target-legend"><span><i className="legend-dot actual" />Actual</span><span><i className="legend-dot target" />Target 1</span><span><i className="legend-dot target-two" />Target 2</span></div><button className="target-flip" aria-label={`${table ? 'Show graph for' : 'Show table for'} ${name}`} onClick={onFlip}><Icon name="switch" /></button></header>{table ? <TargetTable name={name} points={points} /> : <LineChart name={name} period={period} points={points} />}</Card>
}

function GaugeCard({ period, range }: { period: TargetPeriod; range: TargetPeriodRange }) {
  const elapsedDays = range.dates.filter((date) => date <= range.today).length
  const value = period === 'WTD' ? 4.38 : period === 'MTD' ? 18.75 : 142.5
  const progress = Math.min(96, 50 + elapsedDays / range.dates.length * 45)
  return <Card className="desktop-target-card gauge-card"><header><div><h2>Medschecks</h2><p>As of {range.today.toLocaleDateString('en-AU')}</p></div></header><svg className="full-gauge" viewBox="0 0 400 400" role="img" aria-label={`Medschecks ${period} ${value}`}><circle className="full-gauge__track" cx="200" cy="200" r="167" /><circle className="full-gauge__value" cx="200" cy="200" r="167" pathLength="100" style={{ strokeDasharray: `${progress} ${100 - progress}` }} /><text x="200" y="226" textAnchor="middle">{number.format(value)}</text></svg></Card>
}

export function TargetsPage() {
  const [period, setPeriod] = useState<TargetPeriod>('WTD')
  const [tableCard, setTableCard] = useState<string | null>(null)
  const range = useMemo(() => targetPeriodRange(period), [period])
  const label = targetPeriodLabel(range)
  const lineCard = (name: string) => <LineTargetCard name={name} period={period} range={range} table={tableCard === name} onFlip={() => setTableCard(tableCard === name ? null : name)} />
  return <div className="page targets-desktop-page"><div className="period-tabs desktop-period-tabs" role="tablist" aria-label="Target date range">{(['WTD', 'MTD', 'YTD'] as const).map((value) => <button role="tab" aria-selected={period === value} className={period === value ? 'active' : ''} title={value === period ? label : undefined} key={value} onClick={() => setPeriod(value)}>{value}{period === value && <small>{label}</small>}</button>)}</div><div className="desktop-target-grid">{lineCard('Number of Scripts')}{lineCard('OTC $/Customer')}<GaugeCard period={period} range={range} />{lineCard('GP ($)')}{lineCard('Scripts on File')}<GaugeCard period={period} range={range} /></div></div>
}
