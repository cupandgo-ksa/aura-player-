import { Track } from '../types/media';
import { createGlassAlbumArt, createSynthesizedAudioBlob } from '../services/mediaSynthesizer';

export function getInitialVideos(): Track[] {
  // Offline sample videos using generated blobs and animation canvases
  const synthBlob = createSynthesizedAudioBlob('synthwave', 15);
  const lofiBlob = createSynthesizedAudioBlob('lofi', 15);
  const ambientBlob = createSynthesizedAudioBlob('ambient', 15);

  return [
    {
      id: 'default-video-1',
      title: 'Neon Aurora 4K Visualizer (অরোরা ভিডিও)',
      artist: 'Aura Visuals',
      album: 'Cosmic Journeys',
      duration: 45,
      url: URL.createObjectURL(synthBlob), // Fallback or video container
      coverUrl: createGlassAlbumArt('NEON AURORA', '#06b6d4', '#8b5cf6', 'video'),
      type: 'video',
      size: 4200000,
      format: 'MP4 / HD',
      addedAt: Date.now() - 1000 * 60 * 60 * 30,
      isFavorite: true,
      folder: 'Internal / Videos / Visuals',
      genre: 'Visualizer',
    },
    {
      id: 'default-video-2',
      title: 'Lofi Rain Glass Window 1080p (বৃষ্টির কাচ)',
      artist: 'Lofi World',
      album: 'Monsoon Dreams',
      duration: 45,
      url: URL.createObjectURL(lofiBlob),
      coverUrl: createGlassAlbumArt('RAIN GLASS', '#1e3a8a', '#3b82f6', 'video'),
      type: 'video',
      size: 3800000,
      format: 'MP4 / 1080p',
      addedAt: Date.now() - 1000 * 60 * 60 * 18,
      isFavorite: false,
      folder: 'Internal / Videos / Nature',
      genre: 'Nature & Rain',
    },
    {
      id: 'default-video-3',
      title: 'Deep Ocean Bioluminescence (গভীর সমুদ্রের আলো)',
      artist: 'Oceanic Explorations',
      album: 'Abyss Wonders',
      duration: 45,
      url: URL.createObjectURL(ambientBlob),
      coverUrl: createGlassAlbumArt('DEEP OCEAN', '#0f766e', '#14b8a6', 'video'),
      type: 'video',
      size: 5100000,
      format: 'MKV / 4K',
      addedAt: Date.now() - 1000 * 60 * 60 * 8,
      isFavorite: true,
      folder: 'Internal / Videos / Documentary',
      genre: 'Nature',
    },
  ];
}
