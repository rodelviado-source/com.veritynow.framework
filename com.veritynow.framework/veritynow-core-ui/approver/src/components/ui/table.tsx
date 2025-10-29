import * as React from 'react';
export function Table(p: React.TableHTMLAttributes<HTMLTableElement>){ return <table {...p} className={`w-full text-sm ${p.className||''}`} /> }
export function TableHeader(p: React.HTMLAttributes<HTMLTableSectionElement>){ return <thead {...p} className={`bg-gray-50 ${p.className||''}`} /> }
export function TableBody(p: React.HTMLAttributes<HTMLTableSectionElement>){ return <tbody {...p} className={`${p.className||''}`} /> }
export function TableRow(p: React.HTMLAttributes<HTMLTableRowElement>){ return <tr {...p} className={`border-b last:border-0 ${p.className||''}`} /> }
export function TableHead(p: React.ThHTMLAttributes<HTMLTableCellElement>){ return <th {...p} className={`text-left font-medium px-3 py-2 ${p.className||''}`} /> }
export function TableCell(p: React.TdHTMLAttributes<HTMLTableCellElement>){ return <td {...p} className={`px-3 py-2 align-middle ${p.className||''}`} /> }
