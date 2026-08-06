import type { SVGProps } from 'react'

const paths: Record<string, string> = {
  targets: 'M3 3v18h18M7 16l4-5 4 3 5-7', eod: 'M4 20h16M6 16l9-9 3 3-9 9H6v-3z', payments: 'M3 7h18v12H3zM3 10h18M7 15h3',
  roster: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zm0-13v5l3 2', employees: 'M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm8 1a3 3 0 0 0 0-6m5 15v-2a4 4 0 0 0-3-3.87',
  invoices: 'M5 3h14v18l-3-2-4 2-4-2-3 2V3zm4 5h6m-6 4h6', bas: 'M4 19V5h16v14H4zm4-3v-5m4 5V8m4 8v-3',
  budget: 'M3 7h18v12H3zM8 3v8m8-8v8M7 15h4', summary: 'M4 4h16v16H4zM4 9h16M9 4v16', settings: 'M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zm7-3.5 2-1-2-3-2 .5-1-2.5-3-1-1.5 1-2-1-1 2.5L5 8.5 3 8l-2 3 2 1-2 3 2 1 2-.5L7 18l1 2 3-1 1 2 3-1 1-2 2.5-1.5 2 .5 1-3-2-1z',
  logout: 'M10 17l5-5-5-5m5 5H3m9-9h7v18h-7', plus: 'M12 5v14m-7-7h14', close: 'M6 6l12 12M18 6 6 18', edit: 'M4 20h4L19 9l-4-4L4 16v4zm9-13 4 4', trash: 'M4 7h16m-10 4v6m4-6v6M9 7V4h6v3m-9 0 1 14h10l1-14', calendar: 'M5 4h14v16H5zM8 2v4m8-4v4M5 9h14', switch: 'M7 7h11l-3-3m3 3-3 3M17 17H6l3 3m-3-3 3-3',
  chevron: 'M9 6l6 6-6 6', user: 'M20 21a8 8 0 0 0-16 0m8-11a4 4 0 1 0 0-8 4 4 0 0 0 0 8', upload: 'M12 16V4m-5 5 5-5 5 5M4 20h16', download: 'M12 4v12m-5-5 5 5 5-5M4 20h16', menu: 'M4 7h16M4 12h16M4 17h16'
}

export function Icon({ name, ...props }: SVGProps<SVGSVGElement> & { name: string }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}><path d={paths[name] || paths.summary} /></svg>
}
