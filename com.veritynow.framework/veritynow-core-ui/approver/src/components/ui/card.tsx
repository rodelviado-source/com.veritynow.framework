import * as React from 'react';
export function Card(p: React.HTMLAttributes<HTMLDivElement>){ return <div {...p} className={`rounded-2xl border bg-white ${p.className||''}`} /> }
export function CardHeader(p: React.HTMLAttributes<HTMLDivElement>){ return <div {...p} className={`border-b p-4 ${p.className||''}`} /> }
export function CardTitle(p: React.HTMLAttributes<HTMLHeadingElement>){ return <h3 {...p} className={`text-lg font-semibold ${p.className||''}`} /> }
export function CardContent(p: React.HTMLAttributes<HTMLDivElement>){ return <div {...p} className={`p-4 space-y-3 ${p.className||''}`} /> }
