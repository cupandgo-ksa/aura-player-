import React from 'react';
import { Play, Pause, SkipForward, Heart, Music } from 'lucide-react';
import { Track } from '../types/media';

interface MiniPlayerBarProps {
  currentTrack: Track | null;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  onTogglePlay: () => void;
  onNextTrack: () => void;
  onToggleFavorite: (trackId: string) => void;
  onOpenNowPlaying: () => void;
}

export const MiniPlayerBar: React.FC<MiniPlayerBarProps> = ({
  currentTrack,
  isPlaying,
  currentTime,
  duration,
  onTogglePlay,
  onNextTrack,
  onToggleFavorite,
  onOpenNowPlaying,
}) => {
  if (!currentTrack) return null;

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="fixed bottom-[68px] left-0 right-0 z-30 px-3 max-w-lg mx-auto">
      <div
        onClick={onOpenNowPlaying}
        className="glass-card glass-card-hover rounded-2xl p-2.5 flex items-center gap-3 cursor-pointer shadow-2xl relative overflow-hidden backdrop-blur-2xl border border-white/20 bg-slate-900/80"
      >
        {/* Dynamic Glowing Accent line based on progress */}
        <div
          className="absolute bottom-0 left-0 h-[2.5px] bg-gradient-to-r from-purple-500 via-indigo-400 to-cyan-400 transition-all duration-200"
          style={{ width: `${progressPercent}%` }}
        />

        {/* Thumbnail with spin animation when playing */}
        <div className="relative w-12 h-12 rounded-xl overflow-hidden flex-shrink-0 bg-slate-800 shadow-md">
          {currentTrack.coverUrl ? (
            <img
              src={currentTrack.coverUrl}
              alt={currentTrack.title}
              className={`w-full h-full object-cover ${
                isPlaying ? 'scale-105' : ''
              } transition-transform duration-500`}
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center bg-purple-900/40 text-purple-300">
              <Music className="w-5 h-5" />
            </div>
          )}
          {isPlaying && (
            <div className="absolute inset-0 bg-purple-500/10 pointer-events-none animate-pulse" />
          )}
        </div>

        {/* Title & Artist */}
        <div className="flex-1 min-w-0">
          <h4 className="text-sm font-semibold text-white truncate leading-tight">
            {currentTrack.title}
          </h4>
          <p className="text-xs text-slate-300 truncate mt-0.5">
            {currentTrack.artist} • {currentTrack.format || 'Offline'}
          </p>
        </div>

        {/* Controls */}
        <div
          className="flex items-center gap-1.5"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Favorite */}
          <button
            onClick={() => onToggleFavorite(currentTrack.id)}
            className="p-2 text-slate-300 hover:text-red-400 active:scale-90 transition"
            title="Favorite"
          >
            <Heart
              className={`w-4 h-4 ${
                currentTrack.isFavorite ? 'fill-red-500 text-red-500' : ''
              }`}
            />
          </button>

          {/* Play/Pause button */}
          <button
            onClick={onTogglePlay}
            className="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-600 to-indigo-500 flex items-center justify-center text-white shadow-lg shadow-purple-500/40 hover:scale-105 active:scale-95 transition"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? (
              <Pause className="w-4 h-4 fill-white" />
            ) : (
              <Play className="w-4 h-4 fill-white ml-0.5" />
            )}
          </button>

          {/* Next */}
          <button
            onClick={onNextTrack}
            className="p-2 text-slate-300 hover:text-white active:scale-90 transition"
            title="Next Track"
          >
            <SkipForward className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
