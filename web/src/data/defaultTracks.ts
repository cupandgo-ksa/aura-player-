import { Track, Playlist } from '../types/media';
import { createSynthesizedAudioBlob, createGlassAlbumArt } from '../services/mediaSynthesizer';

export function getInitialTracks(): Track[] {
  // Pre-generate blobs for the offline starter songs (instant generation)
  const lofiBlob = createSynthesizedAudioBlob('lofi', 15);
  const synthwaveBlob = createSynthesizedAudioBlob('synthwave', 15);
  const ambientBlob = createSynthesizedAudioBlob('ambient', 15);
  const pianoBlob = createSynthesizedAudioBlob('piano', 15);
  const fluteBlob = createSynthesizedAudioBlob('flute', 15);

  const tracks: Track[] = [
    {
      id: 'default-audio-1',
      title: 'Cosmic Lofi Dream (কসমিক লোফাই)',
      artist: 'Aura Studio',
      album: 'Midnight Chill Vol. 1',
      duration: 60,
      url: URL.createObjectURL(lofiBlob),
      coverUrl: createGlassAlbumArt('COSMIC LOFI', '#6b21a8', '#3b82f6', 'disc'),
      type: 'audio',
      size: lofiBlob.size,
      format: 'WAV',
      addedAt: Date.now() - 1000 * 60 * 60 * 24 * 3,
      isFavorite: true,
      folder: 'Internal / Music / Chill',
      genre: 'Lofi / Beats',
      lyrics: 'Floating through the neon starlight\nSoft glass reflections in the night\nJust peace, no noise, pure delight...',
      blob: lofiBlob,
    },
    {
      id: 'default-audio-2',
      title: 'Midnight Aurora Synthwave (মিডনাইট সিন্থ)',
      artist: 'Cyber Glaze',
      album: 'Neon Horizon',
      duration: 60,
      url: URL.createObjectURL(synthwaveBlob),
      coverUrl: createGlassAlbumArt('AURORA SYNTH', '#ec4899', '#8b5cf6', 'disc'),
      type: 'audio',
      size: synthwaveBlob.size,
      format: 'WAV',
      addedAt: Date.now() - 1000 * 60 * 60 * 24 * 2,
      isFavorite: false,
      folder: 'Internal / Music / Electronic',
      genre: 'Synthwave',
      lyrics: 'Speeding down the glass boulevard\nBass resonating deep and hard\nAnalog memories in the dark...',
      blob: synthwaveBlob,
    },
    {
      id: 'default-audio-3',
      title: 'Bengali Folk Flute & Melody (বাঙালি সুর ও বাঁশি)',
      artist: 'Aura Heritage',
      album: 'Rivers & Mist',
      duration: 60,
      url: URL.createObjectURL(fluteBlob),
      coverUrl: createGlassAlbumArt('RIVER FLUTE', '#0d9488', '#0284c7', 'disc'),
      type: 'audio',
      size: fluteBlob.size,
      format: 'WAV',
      addedAt: Date.now() - 1000 * 60 * 60 * 24,
      isFavorite: true,
      folder: 'Internal / Music / Folk',
      genre: 'Folk / Instrumental',
      lyrics: 'মাটির সুরে প্রাণের গান\nবাঁশির সুরে মন জুড়ান\nশান্ত নদীর মৃদুল তান...',
      blob: fluteBlob,
    },
    {
      id: 'default-audio-4',
      title: 'Deep Grand Piano Serenade (পিয়ানো সিম্ফনি)',
      artist: 'Elena Rostova',
      album: 'Acoustic Rain',
      duration: 60,
      url: URL.createObjectURL(pianoBlob),
      coverUrl: createGlassAlbumArt('PIANO RAIN', '#1e293b', '#64748b', 'disc'),
      type: 'audio',
      size: pianoBlob.size,
      format: 'WAV',
      addedAt: Date.now() - 1000 * 60 * 60 * 12,
      isFavorite: false,
      folder: 'Internal / Music / Classical',
      genre: 'Classical',
      lyrics: '[Instrumental Piano Performance]',
      blob: pianoBlob,
    },
    {
      id: 'default-audio-5',
      title: 'Deep Bass Meditation 432Hz (ডিপ মেডিটেশন)',
      artist: 'Soma Zen',
      album: 'Inner Harmony',
      duration: 60,
      url: URL.createObjectURL(ambientBlob),
      coverUrl: createGlassAlbumArt('ZEN HARMONY', '#4f46e5', '#06b6d4', 'disc'),
      type: 'audio',
      size: ambientBlob.size,
      format: 'WAV',
      addedAt: Date.now() - 1000 * 60 * 60 * 5,
      isFavorite: true,
      folder: 'Internal / Music / Ambient',
      genre: 'Ambient / Meditation',
      lyrics: '[Subtle 432Hz Harmonic Frequency]',
      blob: ambientBlob,
    },
  ];

  return tracks;
}

export function getInitialPlaylists(): Playlist[] {
  return [
    {
      id: 'playlist-favorites',
      name: 'প্রিয় গান (Favorites)',
      description: 'Your starred and favorite tracks',
      trackIds: ['default-audio-1', 'default-audio-3', 'default-audio-5'],
      createdAt: Date.now() - 1000 * 60 * 60 * 24 * 7,
      isSystem: true,
      coverUrl: createGlassAlbumArt('FAVORITES', '#e11d48', '#f43f5e', 'heart'),
    },
    {
      id: 'playlist-chill',
      name: 'শান্ত ও নিরিবিলি (Chill & Focus)',
      description: 'Calm instrumentals and lofi beats',
      trackIds: ['default-audio-1', 'default-audio-4'],
      createdAt: Date.now() - 1000 * 60 * 60 * 24 * 5,
      isSystem: false,
      coverUrl: createGlassAlbumArt('CHILL VIBES', '#8b5cf6', '#3b82f6', 'disc'),
    },
    {
      id: 'playlist-night',
      name: 'রাতের ড্রাইভ (Night Ride)',
      description: 'Synthwave and deep bass for late nights',
      trackIds: ['default-audio-2', 'default-audio-5'],
      createdAt: Date.now() - 1000 * 60 * 60 * 24 * 2,
      isSystem: false,
      coverUrl: createGlassAlbumArt('NIGHT RIDE', '#ec4899', '#7c3aed', 'disc'),
    },
  ];
}
