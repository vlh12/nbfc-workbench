export type AmountSlice = { name: string; amount: bigint };

export function moneyCents(value: string): bigint {
  if (!/^\d+(\.\d{1,2})?$/.test(value)) throw new Error("Invalid monetary amount");
  const [whole, fraction = ""] = value.split(".");
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"));
}
export function moneyLabel(value: bigint): string {
  return "₹" + (value / 100n).toLocaleString("en-IN") + "." + (value % 100n).toString().padStart(2, "0");
}
export function shareLabel(value: bigint, total: bigint): string {
  if (total <= 0n) return "Not available";
  const hundredths = (value * 10000n + total / 2n) / total;
  return (hundredths / 100n).toString() + "." + (hundredths % 100n).toString().padStart(2, "0") + "%";
}
export function exposureMetrics(totalEad: string | null, rows: { nbfcId: string; eadAmount: string | null }[]) {
  if (totalEad == null || rows.some(row => row.eadAmount == null)) return null;
  const total = moneyCents(totalEad);
  const ranked = rows.map(row => ({ name: row.nbfcId, amount: moneyCents(row.eadAmount!) }))
    .sort((a, b) => a.amount === b.amount ? a.name.localeCompare(b.name) : a.amount > b.amount ? -1 : 1);
  if (ranked.reduce((sum, row) => sum + row.amount, 0n) !== total) return null;
  const topThree = ranked.slice(0, 3).reduce((sum, row) => sum + row.amount, 0n);
  const slices = ranked.slice(0, 5);
  if (ranked.length > 5) slices.push({ name: "Others", amount: ranked.slice(5).reduce((sum, row) => sum + row.amount, 0n) });
  return { total, largest: ranked[0], topThree, slices };
}
export function localDateKey(date = new Date()): string {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-");
}
export function overdueCount(actions: { dueDate: string; status: string }[], today = localDateKey()): number {
  return actions.filter(action => action.dueDate < today && !["COMPLETED", "CLOSED"].includes(action.status)).length;
}
