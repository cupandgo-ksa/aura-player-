import React, { useEffect, useState } from 'react';
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
  User,
  Disc3,
  Tag,
  Radio,
  HardDrive,
  Image as ImageIcon,
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
  const [artStyle, setArtStyle] = useState<'vinyl' | 'cover' | 'glow'>(() => {
    const saved = localStorage.getItem('as-media-art-style');
    return saved === 'cover' || saved === 'glow' ? saved : 'vinyl';
  });
  const [showArtStyles, setShowArtStyles] = useState(false);
  const chooseArtStyle = (style: 'vinyl' | 'cover' | 'glow') => {
    setArtStyle(style); localStorage.setItem('as-media-art-style', style);
    setActiveTab('art'); setShowArtStyles(false);
  };

  useEffect(() => {
    if (!isOpen) { setShowArtStyles(false); setShowSpeedMenu(false); return; }
    const outside = (event: PointerEvent) => {
      if (!(event.target as Element).closest('[data-player-popover]')) {
        setShowArtStyles(false); setShowSpeedMenu(false);
      }
    };
    const scroll = () => { setShowArtStyles(false); setShowSpeedMenu(false); };
    const back = (event: KeyboardEvent) => { if (event.key === 'Escape') scroll(); };
    document.addEventListener('pointerdown', outside);
    document.addEventListener('scroll', scroll, true);
    window.addEventListener('keydown', back);
    return () => {
      document.removeEventListener('pointerdown', outside);
      document.removeEventListener('scroll', scroll, true);
      window.removeEventListener('keydown', back);
    };
  }, [isOpen]);
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
              {currentTrack.album || (lang === 'bn' ? 'আমার গান' : 'My music')}
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

        {/* View Switcher: Cover Art / Visualizer / Lyrics / Queue */}
        <div className="flex items-center justify-center gap-1.5 py-1 overflow-x-auto no-scrollbar">
          <button
            onClick={() => setActiveTab('art')}
            className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              activeTab === 'art'
                ? 'bg-purple-500/30 text-purple-200 border border-purple-400/40 shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {lang === 'bn' ? 'অ্যালবাম আর্ট' : 'Album Art'}
          </button>
          <button
            onClick={() => setActiveTab('lyrics')}
            className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              activeTab === 'lyrics'
                ? 'bg-purple-500/30 text-purple-200 border border-purple-400/40 shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {lang === 'bn' ? 'লিরিক্স' : 'Lyrics'}
          </button>
          <button
            onClick={() => setActiveTab('queue')}
            className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              activeTab === 'queue'
                ? 'bg-purple-500/30 text-purple-200 border border-purple-400/40 shadow-sm'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            {lang === 'bn' ? 'পরবর্তী গান' : 'Queue'} ({queue.length})
          </button>
        </div>

        <div className="relative flex justify-end" data-player-popover>
          <button onClick={() => setShowArtStyles(!showArtStyles)} className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-white/20 bg-white/10 text-xs text-slate-200" aria-label={lang === 'bn' ? 'কভার স্টাইল বদলান' : 'Change cover style'}>
            <ImageIcon className="w-3.5 h-3.5" /> {lang === 'bn' ? 'কভার স্টাইল' : 'Cover style'}
          </button>
          {showArtStyles && <div className="absolute right-0 top-9 z-10 w-40 p-1.5 rounded-2xl bg-[#252b3c]/95 border border-white/20 shadow-2xl backdrop-blur-xl">
            {(['vinyl', 'cover', 'glow'] as const).map((style) => <button key={style} onClick={() => chooseArtStyle(style)} className={`block w-full text-left rounded-xl px-3 py-2 text-sm ${artStyle === style ? 'bg-white/20 text-white' : 'text-slate-300 hover:bg-white/10'}`}>
              {style === 'vinyl' ? (lang === 'bn' ? 'ভিনাইল' : 'Vinyl') : style === 'cover' ? (lang === 'bn' ? 'স্কয়ার কভার' : 'Square cover') : (lang === 'bn' ? 'সফ্ট গ্লো' : 'Soft glow')}
            </button>)}
          </div>}
        </div>
        {/* Center Main Stage */}
        <div className="my-auto py-3 flex items-center justify-center min-h-[230px]">
          {activeTab === 'art' && artStyle === 'vinyl' && (
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

          {activeTab === 'art' && artStyle === 'cover' && <div className="w-64 h-64 sm:w-72 sm:h-72 rounded-[28px] overflow-hidden border border-white/25 shadow-2xl bg-white/10"><img src={currentTrack.coverUrl} alt={currentTrack.title} className="w-full h-full object-cover" /></div>}
          {activeTab === 'art' && artStyle === 'glow' && <div className="relative w-64 h-64 sm:w-72 sm:h-72 flex items-center justify-center"><img src={currentTrack.coverUrl} alt="" className="absolute inset-2 w-[95%] h-[95%] object-cover blur-3xl opacity-50 rounded-full" /><img src={currentTrack.coverUrl} alt={currentTrack.title} className="relative w-52 h-52 sm:w-60 sm:h-60 object-cover rounded-[36px] border border-white/35 shadow-2xl" /></div>}

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

        {/* Soft-Glass Track Metadata Section */}
        <div className="w-full glass-card rounded-3xl p-3.5 sm:p-4 border border-white/15 bg-gradient-to-br from-white/10 via-slate-900/40 to-purple-900/20 backdrop-blur-2xl shadow-xl space-y-2.5">
          {/* Top Row: Mini Album Art Thumbnail + Title + Artist + Favorite */}
          <div className="flex items-center gap-3">
            {/* Soft-Glass Album Art Thumbnail with glossy border */}
            <div className="relative w-13 h-13 sm:w-14 sm:h-14 rounded-2xl overflow-hidden p-[2px] bg-gradient-to-tr from-purple-500/50 via-cyan-400/40 to-pink-500/30 flex-shrink-0 shadow-lg shadow-purple-950/40">
              <img
                src={currentTrack.coverUrl}
                alt={currentTrack.album || currentTrack.title}
                className="w-full h-full object-cover rounded-[14px]"
              />
              {isPlaying && (
                <div className="absolute inset-0 bg-black/35 backdrop-blur-[1px] flex items-center justify-center">
                  <div className="flex items-end gap-0.5 h-3">
                    <span className="w-1 bg-cyan-300 rounded-full animate-pulse h-2" />
                    <span className="w-1 bg-purple-300 rounded-full animate-pulse h-3" />
                    <span className="w-1 bg-pink-300 rounded-full animate-pulse h-1.5" />
                  </div>
                </div>
              )}
            </div>

            {/* Title & Artist */}
            <div className="min-w-0 flex-1">
              <h3 className="text-base sm:text-lg font-bold text-white truncate tracking-tight">
                {currentTrack.title}
              </h3>
              <div className="flex items-center gap-1.5 text-xs text-purple-200/90 font-medium truncate mt-0.5">
                <User className="w-3.5 h-3.5 text-purple-400 flex-shrink-0" />
                <span className="truncate">{currentTrack.artist}</span>
              </div>
            </div>

            {/* Favorite Button */}
            <button
              onClick={() => onToggleFavorite(currentTrack.id)}
              className={`p-2.5 rounded-2xl border transition active:scale-90 flex-shrink-0 ${
                currentTrack.isFavorite
                  ? 'bg-rose-500/25 border-rose-500/40 text-rose-400 shadow-lg shadow-rose-500/20'
                  : 'glass-card border-white/10 text-slate-400 hover:text-rose-400'
              }`}
              title={currentTrack.isFavorite ? 'Remove Favorite' : 'Add to Favorites'}
            >
              <Heart
                className={`w-5 h-5 ${
                  currentTrack.isFavorite ? 'fill-rose-500 text-rose-500' : ''
                }`}
              />
            </button>
          </div>

          {/* Metadata Badges: Genre, Album, Format, Offline Indicator */}
          <div className="flex items-center gap-1.5 flex-wrap pt-0.5">
            {/* Genre Badge */}
            {currentTrack.genre && (
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-xl text-[11px] font-semibold bg-purple-500/20 border border-purple-400/30 text-purple-200">
                <Tag className="w-3 h-3 text-purple-400" />
                <span>{currentTrack.genre}</span>
              </span>
            )}

            {/* Album Badge */}
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-xl text-[11px] font-medium bg-white/5 border border-white/10 text-slate-300 max-w-[150px] sm:max-w-none truncate">
              <Disc3 className="w-3 h-3 text-cyan-400 flex-shrink-0" />
              <span className="truncate">{currentTrack.album || 'Single Album'}</span>
            </span>

            {/* Audio Quality / Format */}
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-xl text-[10px] font-mono font-semibold bg-cyan-500/15 border border-cyan-400/25 text-cyan-300">
              <Radio className="w-3 h-3 text-cyan-400" />
              <span>{currentTrack.format?.toUpperCase() || 'MP3'} • 320kbps</span>
            </span>

            {/* Offline Storage Status */}
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-xl text-[10px] font-medium bg-emerald-500/15 border border-emerald-500/30 text-emerald-300 ml-auto">
              <HardDrive className="w-3 h-3 text-emerald-400" />
              <span>{lang === 'bn' ? 'অফলাইন মিডিয়া' : 'Offline Media'}</span>
            </span>
          </div>
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
          <div className="relative" data-player-popover>
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
