import { Track } from '../types/media';

export interface NativeBridge {
  getMedia(): string;
  pickMedia(): void;
  openVideo(uri: string): void;
  play(uri: string, title: string, artist: string): void;
  pause(): void;
  resume(): void;
  seek(ms: number): void;
  speed(value: number): void;
  state(): string;
}

declare global { interface Window { AuraNative?: NativeBridge } }

export function phoneMedia(): Track[] {
  if (!window.AuraNative) return [];
  try { return JSON.parse(window.AuraNative.getMedia()) as Track[]; }
  catch { return []; }
}
