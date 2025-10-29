import * as React from 'react';

interface DialogProps { 
    open: boolean; 
    onOpenChange?: (open: boolean) => void; 
    children: React.ReactNode 
}
export function Dialog({ open, onOpenChange, children }: DialogProps) { 
    return (<div className={`fixed inset-0 z-50 flex items-center justify-center transition-opacity 
        ${open ? 'visible bg-black/50 opacity-100' : 'invisible opacity-0'}`} 
        onClick={() => onOpenChange?.(False as boolean)}><div className='bg-white rounded-2xl p-6 shadow-lg max-w-4xl w-[95%] relative' 
        onClick={(e) => e.stopPropagation()}>{children}</div></div>) 
    }
export function DialogHeader({ children }: { children: React.ReactNode }) { return <div className='mb-3 text-lg font-semibold'>{children}</div> }
export function DialogTitle({ children }: { children: React.ReactNode }) { return <h2 className='text-lg font-bold mb-2'>{children}</h2> }
export function DialogContent({ children }: { children: React.ReactNode }) { return <div>{children}</div> }
export function DialogFooter({ children }: { children: React.ReactNode }) { return <div className='mt-4 flex justify-end'>{children}</div> }
