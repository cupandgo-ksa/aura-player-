import { Track } from '../types/media';

export interface NativeBridge {
  getMedia(): string;
  pickMedia(): void;
  setQueue(json: string): void;
  mode(shuffle: boolean, repeat: string): void;
  play(uri: string, title: string, artist: string): void;
  pause(): void;
  resume(): void;
  seek(ms: number): void;
  speed(value: number): void;
  volume(value: number): void;
  band(index: number, value: number): void;
  bass(value: number): void;
  treble(value: number): void;
  state(): string;
}

declare global { interface Window { AuraNative?: NativeBridge } }

export function phoneMedia(): Track[] {
  if (!window.AuraNative) return [];
  try { return JSON.parse(window.AuraNative.getMedia()) as Track[]; }
  catch { return []; }
}

