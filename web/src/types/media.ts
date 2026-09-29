export type MediaType = 'audio' | 'video';

export interface Track {
  id: string;
  title: string;
  artist: string;
  album?: string;
  duration: number; // in seconds
  url: string; // Blob URL, object URL, or synthetic generator
  coverUrl: string;
  type: MediaType;
  size?: number; // bytes
  format?: string; // mp3, mp4, wav, flac, etc.
  addedAt: number;
  isFavorite: boolean;
  folder?: string;
  genre?: string;
  lyrics?: string;
  blob?: Blob; // Stored in IndexedDB for 100% offline persistence
}

export interface Playlist {
  id: string;
  name: string;
  description?: string;
  coverUrl?: string;
  trackIds: string[];
  createdAt: number;
  isSystem?: boolean;
}

export interface TVChannel {
  id: string;
  name: string;
  bnName: string;
  category: 'music' | 'nature' | 'entertainment' | 'news';
  streamUrl: string;
  logoUrl: string;
  description: string;
  bnDescription: string;
  resolution: string;
  isLive: boolean;
}

export interface EqualizerPreset {
  id: string;
  name: string;
  bnName: string;
  gains: [number, number, number, number, number]; // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
}

export type RepeatMode = 'off' | 'all' | 'one';

export type MainTab = 'music' | 'video' | 'tv' | 'playlists' | 'folders' | 'equalizer';

export interface SleepTimerState {
  isActive: boolean;
  targetTimestamp: number | null;
  remainingSeconds: number;
  action: 'pause' | 'stop';
}
