// Minimal declarations for the vendored pdf-lib 1.17.1 public APIs used here.
export type Color = { type: string; red: number; green: number; blue: number };
export function rgb(red: number, green: number, blue: number): Color;
export const StandardFonts: { Helvetica: string; HelveticaBold: string };
export interface PDFFont {
  widthOfTextAtSize(text: string, size: number): number;
  encodeText(text: string): unknown;
}
export interface PDFImage { width: number; height: number }
export interface PDFPage {
  drawCircle(options: { x: number; y: number; size: number; color: Color }): void;
  drawSvgPath(path: string, options: { x: number; y: number; color: Color }): void;
  drawText(text: string, options: { x: number; y: number; size: number; font: PDFFont; color: Color }): void;
  drawRectangle(options: { x: number; y: number; width: number; height: number; color: Color }): void;
  drawImage(image: PDFImage, options: { x: number; y: number; width: number; height: number }): void;
}
export class PDFDocument {
  static create(): Promise<PDFDocument>;
  embedFont(name: string): Promise<PDFFont>;
  embedPng(data: string): Promise<PDFImage>;
  addPage(size: [number, number]): PDFPage;
  getPages(): PDFPage[];
  setTitle(title: string): void;
  setSubject(subject: string): void;
  setCreator(creator: string): void;
  save(): Promise<Uint8Array>;
}
