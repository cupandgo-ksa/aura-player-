import React, { useState } from 'react';
import {
  ChevronDown,
  Heart,
  Shuffle,
  Repeat,
  Repeat1,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Volume2,
  VolumeX,
  Sliders,
  List,
  FileText,
  Clock,
  Sparkles,
  Gauge,
} from 'lucide-react';
import { Track, RepeatMode } from '../types/media';

interface NowPlayingModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTrack: Track | null;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  isMuted: boolean;
  playbackRate: number;
  isShuffle: boolean;
  repeatMode: RepeatMode;
  queue: Track[];
  onTogglePlay: () => void;
  onSeek: (seconds: number) => void;
  onPreviousTrack: () => void;
  onNextTrack: () => void;
  onToggleFavorite: (trackId: string) => void;
  onSetVolume: (vol: number) => void;
  onToggleMute: () => void;
  onSetPlaybackRate: (rate: number) => void;
  onToggleShuffle: () => void;
  onCycleRepeatMode: () => void;
  onOpenEqualizer: () => void;
  onOpenSleepTimer: () => void;
  lang: 'bn' | 'en';
}

export const NowPlayingModal: React.FC<NowPlayingModalProps> = ({
  isOpen,
  onClose,
  currentTrack,
  isPlaying,
  currentTime,
  duration,
  volume,
  isMuted,
  playbackRate,
  isShuffle,
  repeatMode,
  queue,
  onTogglePlay,
  onSeek,
  onPreviousTrack,
  onNextTrack,
  onToggleFavorite,
  onSetVolume,
  onToggleMute,
  onSetPlaybackRate,
  onToggleShuffle,
  onCycleRepeatMode,
  onOpenEqualizer,
  onOpenSleepTimer,
  lang,
}) => {
  const [activeTab, setActiveTab] = useState<'art' | 'lyrics' | 'queue'>('art');
  const [showSpeedMenu, setShowSpeedMenu] = useState(false);

  if (!isOpen || !currentTrack) return null;

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const speedOptions = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0];

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end bg-black/70 backdrop-blur-3xl animate-fade-in overflow-hidden">
      {/* Dynamic Background Glass Glow */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[500px] h-[500px] rounded-full bg-purple-600/25 blur-[120px] animate-pulse-glow" />
        <div className="absolute bottom-10 left-1/3 w-[400px] h-[400px] rounded-full bg-blue-600/20 blur-[130px]" />
      </div>

      <div className="relative w-full max-w-lg mx-auto h-[95vh] rounded-t-[36px] glass-panel border border-white/20 p-5 sm:p-6 flex flex-col justify-between shadow-2xl bg-slate-900/85 overflow-y-auto no-scrollbar">
        {/* Top App Bar */}
        <div className="flex items-center justify-between pb-2">
          <button
            onClick={onClose}
            className="p-2 rounded-2xl glass-card text-slate-300 hover:text-white transition active:scale-95"
            title="Minimize"
          >
            <ChevronDown className="w-5 h-5" />
          </button>

          <div className="text-center">
            <span className="text-[10px] uppercase font-bold tracking-widest text-purple-300">
              {lang === 'bn' ? 'এখন চলছে' : 'NOW PLAYING'}
            </span>
            <p className="text-xs text-slate-400 font-medium">
              {currentTrack.album || 'Aura Glass Collection'}
            </p>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={onOpenSleepTimer}
              className="p-2 rounded-2xl glass-card text-slate-300 hover:text-white transition"
              title="Sleep Timer"
            >
              <Clock className="w-4 h-4" />
            </button>
            <button
              onClick={() => {
                onClose();
                onOpenEqualizer();
              }}
              className="p-2 rounded-2xl glass-card text-purple-300 hover:text-white transition"
              title="Equalizer"
            >
              <Sliders className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* View Switcher: Cover Art / Lyrics / Queue */}
        <div className="flex items-center justify-center gap-2 py-1">
          <button
            onClick={() => setActiveTab('art')}
            className={`px-3 py-1 rounded-full text-xs font-semibold transition ${
              activeTab === 'art'
                ? 'bg-purple-500/30 text-purple-200 border border-purple-400/40'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {lang === 'bn' ? 'অ্যালবাম আর্ট' : 'Album Art'}
          </button>
          <button
            onClick={() => setActiveTab('lyrics')}
            className={`px-3 py-1 rounded-full text-xs font-semibold transition ${
              activeTab === 'lyrics'
                ? 'bg-purple-500/30 text-purple-200 border border-purple-400/40'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {lang === 'bn' ? 'লিরিক্স' : 'Lyrics'}
          </button>
          <button
            onClick={() => setActiveTab('queue')}
            className={`px-3 py-1 rounded-full text-xs font-semibold transition ${
              activeTab === 'queue'
                ? 'bg-purple-500/30 text-purple-200 border border-purple-400/40'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {lang === 'bn' ? 'পরবর্তী গান' : 'Queue'} ({queue.length})
          </button>
        </div>

        {/* Center Main Stage */}
        <div className="my-auto py-4 flex items-center justify-center min-h-[260px]">
          {activeTab === 'art' && (
            <div className="relative group flex items-center justify-center">
              {/* Outer soft glass glow halo */}
              <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-purple-500 to-cyan-500 opacity-20 blur-2xl group-hover:opacity-30 transition" />

              {/* Vinyl / Cover Art Container */}
              <div
                className={`relative w-64 h-64 sm:w-72 sm:h-72 rounded-full p-2.5 glass-panel border-2 border-white/25 shadow-2xl flex items-center justify-center overflow-hidden ${
                  isPlaying ? 'animate-spin-slow' : ''
                }`}
              >
                {/* Vinyl Grooves effect */}
                <div className="absolute inset-4 rounded-full border border-white/10" />
                <div className="absolute inset-8 rounded-full border border-white/10" />
                <div className="absolute inset-12 rounded-full border border-white/10" />

                {/* Inner center image */}
                <div className="w-48 h-48 sm:w-52 sm:h-52 rounded-full overflow-hidden shadow-inner border border-white/30">
                  <img
                    src={currentTrack.coverUrl}
                    alt={currentTrack.title}
                    className="w-full h-full object-cover"
                  />
                </div>

                {/* Center spindle hole */}
                <div className="absolute w-8 h-8 rounded-full bg-slate-900 border-2 border-white/40 shadow-inner flex items-center justify-center">
                  <div className="w-2.5 h-2.5 rounded-full bg-purple-400" />
                </div>
              </div>
            </div>
          )}

          {activeTab === 'lyrics' && (
            <div className="w-full h-64 glass-card rounded-2xl p-5 border border-white/15 overflow-y-auto text-center space-y-3">
              <FileText className="w-6 h-6 mx-auto text-purple-400" />
              <h4 className="text-sm font-bold text-white">
                {lang === 'bn' ? 'লিরিক্স' : 'Song Lyrics'}
              </h4>
              <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-line font-medium">
                {currentTrack.lyrics ||
                  (lang === 'bn'
                    ? 'এই গানের জন্য কোন লিরিক্স পাওয়া যায়নি।'
                    : 'No lyrics available for this offline track.')}
              </p>
            </div>
          )}

          {activeTab === 'queue' && (
            <div className="w-full h-64 glass-card rounded-2xl p-3 border border-white/15 overflow-y-auto space-y-1.5">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 px-2 py-1">
                {lang === 'bn' ? 'প্লেলিস্ট কিউ' : 'Up Next in Queue'}
              </h4>
              {queue.map((track, i) => (
                <div
                  key={track.id + i}
                  className={`p-2 rounded-xl flex items-center gap-2.5 text-xs ${
                    track.id === currentTrack.id
                      ? 'bg-purple-500/25 border border-purple-400/40 text-purple-200 font-bold'
                      : 'hover:bg-white/5 text-slate-300'
                  }`}
                >
                  <span className="w-4 text-center font-mono text-slate-400">
                    {i + 1}
                  </span>
                  <div className="w-7 h-7 rounded-lg overflow-hidden flex-shrink-0">
                    <img
                      src={track.coverUrl}
                      alt={track.title}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="min-w-0 flex-1 truncate">
                    <p className="truncate">{track.title}</p>
                    <p className="text-[10px] text-slate-400 truncate">
                      {track.artist}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Title, Artist & Favorite */}
        <div className="flex items-center justify-between gap-3 pt-2">
          <div className="min-w-0 flex-1">
            <h3 className="text-lg sm:text-xl font-bold text-white truncate">
              {currentTrack.title}
            </h3>
            <p className="text-xs sm:text-sm text-slate-300 truncate mt-0.5">
              {currentTrack.artist} • {currentTrack.genre || 'Offline Audio'}
            </p>
          </div>

          <button
            onClick={() => onToggleFavorite(currentTrack.id)}
            className="p-2.5 rounded-2xl glass-card text-slate-300 hover:text-red-400 transition active:scale-90"
            title="Favorite"
          >
            <Heart
              className={`w-5 h-5 ${
                currentTrack.isFavorite ? 'fill-red-500 text-red-500' : ''
              }`}
            />
          </button>
        </div>

        {/* Progress Bar & Timers */}
        <div className="space-y-1.5 pt-3">
          <input
            type="range"
            min="0"
            max={duration || 100}
            step="0.5"
            value={currentTime}
            onChange={(e) => onSeek(Number(e.target.value))}
            aria-label="Seek track"
            className="w-full accent-purple-400"
          />
          <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Primary Controls */}
        <div className="flex items-center justify-between px-2 pt-2">
          {/* Shuffle */}
          <button
            onClick={onToggleShuffle}
            className={`p-2.5 rounded-xl transition ${
              isShuffle ? 'text-purple-400 bg-purple-500/20' : 'text-slate-400 hover:text-white'
            }`}
            title="Shuffle"
          >
            <Shuffle className="w-4 h-4" />
          </button>

          {/* Previous */}
          <button
            onClick={onPreviousTrack}
            className="p-3 text-slate-200 hover:text-white active:scale-90 transition"
            title="Previous"
          >
            <SkipBack className="w-6 h-6 fill-current" />
          </button>

          {/* Big Play / Pause */}
          <button
            onClick={onTogglePlay}
            className="w-16 h-16 rounded-full bg-gradient-to-tr from-purple-600 via-indigo-600 to-cyan-500 p-[2px] shadow-xl shadow-purple-600/40 hover:scale-105 active:scale-95 transition"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            <div className="w-full h-full rounded-full bg-slate-900/40 backdrop-blur-md flex items-center justify-center text-white">
              {isPlaying ? (
                <Pause className="w-7 h-7 fill-white" />
              ) : (
                <Play className="w-7 h-7 fill-white ml-1" />
              )}
            </div>
          </button>

          {/* Next */}
          <button
            onClick={onNextTrack}
            className="p-3 text-slate-200 hover:text-white active:scale-90 transition"
            title="Next"
          >
            <SkipForward className="w-6 h-6 fill-current" />
          </button>

          {/* Repeat Mode */}
          <button
            onClick={onCycleRepeatMode}
            className={`p-2.5 rounded-xl transition ${
              repeatMode !== 'off'
                ? 'text-purple-400 bg-purple-500/20'
                : 'text-slate-400 hover:text-white'
            }`}
            title={`Repeat: ${repeatMode}`}
          >
            {repeatMode === 'one' ? (
              <Repeat1 className="w-4 h-4" />
            ) : (
              <Repeat className="w-4 h-4" />
            )}
          </button>
        </div>

        {/* Bottom Secondary Controls: Volume & Speed */}
        <div className="flex items-center justify-between gap-4 pt-4 border-t border-white/10">
          {/* Volume slider */}
          <div className="flex items-center gap-2 flex-1 max-w-[200px]">
            <button
              onClick={onToggleMute}
              className="text-slate-400 hover:text-white p-1"
            >
              {isMuted || volume === 0 ? (
                <VolumeX className="w-4 h-4 text-red-400" />
              ) : (
                <Volume2 className="w-4 h-4" />
              )}
            </button>
            <input
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={isMuted ? 0 : volume}
              onChange={(e) => onSetVolume(Number(e.target.value))}
              aria-label="Volume"
              className="w-full accent-purple-400"
            />
          </div>

          {/* Playback speed selector */}
          <div className="relative">
            <button
              onClick={() => setShowSpeedMenu(!showSpeedMenu)}
              className="flex items-center gap-1 px-3 py-1.5 rounded-xl glass-card text-xs font-semibold text-purple-300 hover:text-white transition"
            >
              <Gauge className="w-3.5 h-3.5" />
              <span>{playbackRate}x</span>
            </button>

            {showSpeedMenu && (
              <div className="absolute right-0 bottom-10 w-24 rounded-2xl glass-panel p-1.5 shadow-2xl border border-white/20 z-50">
                {speedOptions.map((speed) => (
                  <button
                    key={speed}
                    onClick={() => {
                      onSetPlaybackRate(speed);
                      setShowSpeedMenu(false);
                    }}
                    className={`w-full py-1 text-center rounded-lg text-xs ${
                      playbackRate === speed
                        ? 'bg-purple-600 text-white font-bold'
                        : 'text-slate-300 hover:bg-white/10'
                    }`}
                  >
                    {speed}x
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
