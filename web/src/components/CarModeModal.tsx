import React, { useState, useEffect } from 'react';
import {
  Car,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Volume2,
  VolumeX,
  X,
  Clock,
  Heart,
  Shuffle,
  Repeat,
  Radio,
} from 'lucide-react';
import { Track } from '../types/media';

interface CarModeModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTrack: Track | null;
  isPlaying: boolean;
  onTogglePlay: () => void;
  onPreviousTrack: () => void;
  onNextTrack: () => void;
  onToggleFavorite: (id: string) => void;
  volume: number;
  onSetVolume: (vol: number) => void;
  isShuffle: boolean;
  onToggleShuffle: () => void;
  lang: 'bn' | 'en';
}

export const CarModeModal: React.FC<CarModeModalProps> = ({
  isOpen,
  onClose,
  currentTrack,
  isPlaying,
  onTogglePlay,
  onPreviousTrack,
  onNextTrack,
  onToggleFavorite,
  volume,
  onSetVolume,
  isShuffle,
  onToggleShuffle,
  lang,
}) => {
  const [currentTimeStr, setCurrentTimeStr] = useState<string>('');
  const [touchStartX, setTouchStartX] = useState<number | null>(null);

  // Digital clock for dashboard
  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setCurrentTimeStr(
        now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      );
    };
    updateTime();
    const interval = setInterval(updateTime, 1000);
    return () => clearInterval(interval);
  }, []);

  if (!isOpen || !currentTrack) return null;

  // Swipe gestures for driving safety
  const handleTouchStart = (e: React.TouchEvent) => {
    setTouchStartX(e.touches[0].clientX);
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX === null) return;
    const diff = e.changedTouches[0].clientX - touchStartX;
    if (diff > 80) {
      // Swiped right -> Previous
      onPreviousTrack();
    } else if (diff < -80) {
      // Swiped left -> Next
      onNextTrack();
    }
    setTouchStartX(null);
  };

  return (
    <div
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
      className="fixed inset-0 z-50 bg-black flex flex-col justify-between p-6 sm:p-8 animate-fade-in select-none overflow-hidden"
    >
      {/* Top Status Bar: Clock, Mode & Exit */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
            <Car className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <div className="text-xl sm:text-2xl font-black text-white font-mono tracking-wider">
              {currentTimeStr}
            </div>
            <div className="text-xs text-amber-300 font-bold uppercase tracking-widest">
              {lang === 'bn' ? 'কার ড্রাইভিং মোড' : 'CAR MODE'}
            </div>
          </div>
        </div>

        <button
          onClick={onClose}
          className="p-3.5 rounded-2xl bg-white/10 text-white hover:bg-white/20 transition active:scale-95 flex items-center gap-2 border border-white/10"
        >
          <span className="text-xs font-bold">{lang === 'bn' ? 'প্রস্থান' : 'Exit'}</span>
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Center Stage: Huge Track Artwork, Title, and Artist */}
      <div className="flex flex-col items-center justify-center text-center space-y-4 my-auto">
        {/* Big Artwork with High-Visibility Glow */}
        <div className="relative w-48 h-48 sm:w-60 sm:h-60 rounded-3xl overflow-hidden border-4 border-white/20 shadow-2xl shadow-purple-950/80">
          <img
            src={currentTrack.coverUrl}
            alt={currentTrack.title}
            className="w-full h-full object-cover"
          />
          {isPlaying && (
            <div className="absolute top-3 right-3 px-3 py-1 rounded-full bg-emerald-500/90 text-black text-xs font-black uppercase tracking-wider shadow-lg">
              PLAYING
            </div>
          )}
        </div>

        <div className="max-w-md px-4">
          <h2 className="text-2xl sm:text-3xl font-black text-white truncate drop-shadow-md">
            {currentTrack.title}
          </h2>
          <p className="text-lg sm:text-xl font-bold text-amber-300 truncate mt-1">
            {currentTrack.artist}
          </p>
          <p className="text-xs text-slate-400 truncate mt-0.5">
            {currentTrack.album || 'My music'}
          </p>
        </div>

        {/* Gesture Hint for safe glance */}
        <p className="text-[11px] font-mono text-slate-500">
          {lang === 'bn'
            ? '💡 গান বদলাতে বাঁয়ে/ডানে সোয়াইপ করুন'
            : '💡 Swipe left/right anywhere to skip tracks'}
        </p>
      </div>

      {/* Bottom Large Controls */}
      <div className="w-full max-w-lg mx-auto space-y-5 pb-2">
        {/* Giant Playback Buttons */}
        <div className="flex items-center justify-center gap-6 sm:gap-8">
          <button
            onClick={onPreviousTrack}
            className="w-18 h-18 sm:w-20 sm:h-20 rounded-3xl bg-white/10 hover:bg-white/15 border-2 border-white/15 text-white flex items-center justify-center transition active:scale-90 shadow-xl"
            title="Previous"
          >
            <SkipBack className="w-8 h-8 sm:w-9 sm:h-9" />
          </button>

          <button
            onClick={onTogglePlay}
            className="w-24 h-24 sm:w-28 sm:h-28 rounded-full bg-gradient-to-tr from-purple-600 to-amber-500 hover:from-purple-500 hover:to-amber-400 text-white flex items-center justify-center transition active:scale-95 shadow-2xl shadow-purple-600/50 border-4 border-white/30"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <Pause className="w-12 h-12 sm:w-14 sm:h-14 fill-white" />
            ) : (
              <Play className="w-12 h-12 sm:w-14 sm:h-14 fill-white translate-x-1" />
            )}
          </button>

          <button
            onClick={onNextTrack}
            className="w-18 h-18 sm:w-20 sm:h-20 rounded-3xl bg-white/10 hover:bg-white/15 border-2 border-white/15 text-white flex items-center justify-center transition active:scale-90 shadow-xl"
            title="Next"
          >
            <SkipForward className="w-8 h-8 sm:w-9 sm:h-9" />
          </button>
        </div>

        {/* Volume & Aux Quick Actions */}
        <div className="flex items-center justify-between gap-4 px-2">
          <button
            onClick={onToggleShuffle}
            className={`p-3.5 rounded-2xl border transition active:scale-95 ${
              isShuffle
                ? 'bg-purple-600 text-white border-purple-400'
                : 'bg-white/10 text-slate-400 border-white/10'
            }`}
          >
            <Shuffle className="w-5 h-5" />
          </button>

          <div className="flex-1 flex items-center gap-3 bg-white/10 p-2.5 rounded-2xl border border-white/10">
            <Volume2 className="w-5 h-5 text-amber-300" />
            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={volume}
              onChange={(e) => onSetVolume(Number(e.target.value))}
              className="w-full accent-amber-400 h-2 cursor-pointer"
            />
          </div>

          <button
            onClick={() => onToggleFavorite(currentTrack.id)}
            className={`p-3.5 rounded-2xl border transition active:scale-95 ${
              currentTrack.isFavorite
                ? 'bg-rose-500/25 text-rose-400 border-rose-500/40'
                : 'bg-white/10 text-slate-400 border-white/10'
            }`}
          >
            <Heart
              className={`w-5 h-5 ${
                currentTrack.isFavorite ? 'fill-rose-500 text-rose-500' : ''
              }`}
            />
          </button>
        </div>
      </div>
    </div>
  );
};
