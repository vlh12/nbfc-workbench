import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { clsx } from "clsx";
import { twMerge } from "tailwind-merge";
type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & { asChild?: boolean; variant?: "primary" | "outline" };
export function Button({ className, asChild, variant = "primary", ...props }: Props) {
  const Component = asChild ? Slot : "button";
  return <Component className={twMerge(clsx("inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 disabled:opacity-50 disabled:pointer-events-none", variant === "primary" ? "bg-teal-800 text-white hover:bg-teal-900" : "border border-slate-300 bg-white text-slate-800 hover:bg-slate-50", className))} {...props} />;
}

