import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";

export function SuccessToast({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  const dismiss = useRef(onDismiss);
  useEffect(() => { dismiss.current = onDismiss; }, [onDismiss]);
  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => dismiss.current(), 8000);
    return () => window.clearTimeout(timer);
  }, [message]);

  if (!message) return null;
  return createPortal(
    <div className="fixed bottom-5 right-5 z-[100] flex w-[calc(100%-2.5rem)] max-w-sm items-start gap-3 rounded-xl border border-teal-200 bg-white p-4 text-teal-950 shadow-xl" aria-label="Save confirmation">
      <span aria-hidden="true" className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-teal-100 font-bold">✓</span>
      <p role="status" aria-live="polite" aria-atomic="true" className="min-w-0 flex-1 break-words text-sm font-medium">{message}</p>
      <button type="button" aria-label="Dismiss notification" onClick={onDismiss} className="shrink-0 rounded px-2 py-1 text-lg leading-none hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-teal-700">×</button>
    </div>,
    document.body
  );
}