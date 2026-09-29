import React, { useState, useRef, useEffect } from 'react';
import {
  X,
  Play,
  Pause,
  RotateCcw,
  RotateCw,
  Volume2,
  VolumeX,
  Maximize2,
  Minimize2,
  Headphones,
  Lock,
  Unlock,
  Sun,
  Gauge,
  Sparkles,
  PictureInPicture,
  Sliders,
  Ratio,
  Maximize,
  Minimize,
  Tv,
  Eye,
  Settings2,
} from 'lucide-react';
import { Track } from '../types/media';

export type VideoPlayerSize = 'mini' | 'medium' | 'large' | 'fullscreen' | 'custom';
export type VideoAspectRatio = '16:9' | '4:3' | 'fit' | 'fill' | '1:1';

interface VideoPlayerModalProps {
  isOpen: boolean;
  onClose: () => void;
  videoTrack: Track | null;
  onPlayVideoAsAudio: (track: Track) => void;
  lang: 'bn' | 'en';
}

export const VideoPlayerModal: React.FC<VideoPlayerModalProps> = ({
  isOpen,
  onClose,
  videoTrack,
  onPlayVideoAsAudio,
  lang,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);

  // Player Window Size State
  const [playerSize, setPlayerSize] = useState<VideoPlayerSize>('medium');
  const [customWidth, setCustomWidth] = useState<number>(640);
  const [aspectRatio, setAspectRatio] = useState<VideoAspectRatio>('16:9');
  const [dimBackdrop, setDimBackdrop] = useState<boolean>(true);
  const [showSettingsDrawer, setShowSettingsDrawer] = useState<boolean>(false);

  // Playback States
  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.9);
  const [isMuted, setIsMuted] = useState(false);
  const [brightness, setBrightness] = useState(1);
  const [isLocked, setIsLocked] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [seekNotice, setSeekNotice] = useState<string | null>(null);

  // Auto-hide controls in medium/large/fullscreen when playing
  useEffect(() => {
    if (!showControls || !isPlaying || showSettingsDrawer || isLocked) return;
    const timer = setTimeout(() => {
      setShowControls(false);
    }, 4500);
    return () => clearTimeout(timer);
  }, [showControls, isPlaying, showSettingsDrawer, isLocked]);

  // Sync video play/pause
  const togglePlay = () => {
    if (!videoRef.current) return;
    if (videoRef.current.paused) {
      videoRef.current.play();
      setIsPlaying(true);
    } else {
      videoRef.current.pause();
      setIsPlaying(false);
    }
  };

  const handleSeek = (seconds: number) => {
    if (!videoRef.current) return;
    videoRef.current.currentTime = seconds;
    setCurrentTime(seconds);
  };

  const handleQuickSeek = (delta: number) => {
    if (!videoRef.current) return;
    const target = Math.max(0, Math.min(videoRef.current.currentTime + delta, duration));
    videoRef.current.currentTime = target;
    setSeekNotice(delta > 0 ? `+${delta}s` : `${delta}s`);
    setTimeout(() => setSeekNotice(null), 800);
  };

  const handleTogglePiP = async () => {
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (videoRef.current && document.pictureInPictureEnabled) {
        await videoRef.current.requestPictureInPicture();
      }
    } catch (e) {
      console.warn('PiP error:', e);
    }
  };

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  if (!isOpen || !videoTrack) return null;

  // Determine aspect ratio class
  const getAspectRatioStyle = () => {
    switch (aspectRatio) {
      case '16:9':
        return 'aspect-video object-contain';
      case '4:3':
        return 'aspect-[4/3] object-contain';
      case '1:1':
        return 'aspect-square object-contain';
      case 'fill':
        return 'w-full h-full object-cover';
      case 'fit':
      default:
        return 'w-full h-full object-contain';
    }
  };

  // If in 'mini' floating mode, render non-blocking floating window at bottom-right
  if (playerSize === 'mini') {
    return (
      <div className="fixed bottom-24 right-4 sm:right-6 z-50 w-72 sm:w-88 rounded-3xl glass-card border-2 border-cyan-400/40 bg-slate-950/95 shadow-2xl overflow-hidden backdrop-blur-2xl animate-fade-in group">
        {/* Mini Title Bar */}
        <div className="px-3 py-2 bg-slate-900/90 border-b border-white/10 flex items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 min-w-0">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
            <h4 className="text-xs font-bold text-white truncate">{videoTrack.title}</h4>
          </div>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPlayerSize('medium')}
              className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/10"
              title="Expand to Standard Window"
            >
              <Maximize2 className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={onClose}
              className="p-1 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10"
              title="Close"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Video Canvas Container */}
        <div
          onClick={togglePlay}
          className="relative aspect-video w-full bg-black cursor-pointer overflow-hidden"
          style={{ filter: `brightness(${brightness})` }}
        >
          <video
            ref={videoRef}
            src={videoTrack.url}
            className="w-full h-full object-cover"
            autoPlay
            playsInline
            onTimeUpdate={() => {
              if (videoRef.current) {
                setCurrentTime(videoRef.current.currentTime);
                setDuration(videoRef.current.duration || videoTrack.duration);
              }
            }}
            onEnded={() => setIsPlaying(false)}
          />

          {/* Mini Play/Pause overlay */}
          <div className="absolute inset-0 bg-black/25 opacity-0 group-hover:opacity-100 transition flex items-center justify-center">
            <div className="w-10 h-10 rounded-full bg-cyan-500 text-black flex items-center justify-center shadow-lg">
              {isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
            </div>
          </div>
        </div>

        {/* Mini progress bar */}
        <div className="px-3 py-2 bg-slate-900/90 flex items-center justify-between text-[11px] text-slate-300 font-mono">
          <span>{formatTime(currentTime)}</span>
          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                onClose();
                onPlayVideoAsAudio(videoTrack);
              }}
              className="text-purple-300 hover:underline flex items-center gap-1 text-[10px]"
            >
              <Headphones className="w-3 h-3" />
              <span>{lang === 'bn' ? 'অডিও' : 'Audio'}</span>
            </button>
            <button
              onClick={() => setPlayerSize('medium')}
              className="px-2 py-0.5 rounded-lg bg-cyan-500/20 text-cyan-300 border border-cyan-400/30 text-[10px] font-bold"
            >
              {lang === 'bn' ? 'বড় করুন' : 'Expand'}
            </button>
          </div>
          <span>{formatTime(duration)}</span>
        </div>
      </div>
    );
  }

  // Window Width calculation for medium / large / custom / fullscreen
  const getContainerWidthClass = () => {
    switch (playerSize) {
      case 'medium':
        return 'max-w-2xl';
      case 'large':
        return 'max-w-4xl';
      case 'fullscreen':
        return 'max-w-none w-screen h-screen rounded-none';
      case 'custom':
        return '';
      default:
        return 'max-w-2xl';
    }
  };

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 select-none ${
        playerSize === 'fullscreen' ? 'p-0' : ''
      } ${
        dimBackdrop ? 'bg-black/80 backdrop-blur-md' : 'bg-transparent pointer-events-none'
      } animate-fade-in`}
    >
      {/* Player Card Container */}
      <div
        style={
          playerSize === 'custom'
            ? { width: `${customWidth}px`, maxWidth: '95vw' }
            : {}
        }
        className={`relative w-full ${getContainerWidthClass()} rounded-3xl glass-panel border border-white/20 bg-slate-950/95 shadow-2xl flex flex-col overflow-hidden pointer-events-auto transition-all duration-300 ${
          playerSize === 'fullscreen' ? 'h-full border-0' : ''
        }`}
      >
        {/* Soft-Glass Header Bar: Title, Size Selector & Actions */}
        <div className="px-4 py-2.5 bg-slate-900/90 border-b border-white/10 flex items-center justify-between gap-3 z-30">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 animate-pulse flex-shrink-0" />
            <div className="min-w-0">
              <h3 className="text-xs sm:text-sm font-bold text-white truncate">
                {videoTrack.title}
              </h3>
              <p className="text-[11px] text-slate-400 truncate">
                {videoTrack.artist} • {videoTrack.folder || 'Offline Video'}
              </p>
            </div>
          </div>

          {/* Sizing & Window Controls */}
          <div className="flex items-center gap-1 sm:gap-2 flex-shrink-0">
            {/* Quick Size Mode Selector Buttons */}
            <div className="flex items-center gap-1 bg-white/5 p-1 rounded-xl border border-white/10">
              <button
                onClick={() => setPlayerSize('mini')}
                className="px-2 py-1 rounded-lg text-[10px] font-semibold text-slate-300 hover:text-white hover:bg-white/10 transition"
                title={lang === 'bn' ? 'ভাসমান মিনি প্লেয়ার' : 'Mini Floating Window'}
              >
                {lang === 'bn' ? 'মিনি' : 'Mini'}
              </button>
              <button
                onClick={() => setPlayerSize('medium')}
                className={`px-2 py-1 rounded-lg text-[10px] font-semibold transition ${
                  playerSize === 'medium'
                    ? 'bg-cyan-500 text-slate-950 font-bold'
                    : 'text-slate-300 hover:text-white'
                }`}
                title={lang === 'bn' ? 'মাঝারি উইন্ডো' : 'Medium Standard Window'}
              >
                {lang === 'bn' ? 'মাঝারি' : 'Mid'}
              </button>
              <button
                onClick={() => setPlayerSize('large')}
                className={`px-2 py-1 rounded-lg text-[10px] font-semibold transition ${
                  playerSize === 'large'
                    ? 'bg-cyan-500 text-slate-950 font-bold'
                    : 'text-slate-300 hover:text-white'
                }`}
                title={lang === 'bn' ? 'বড় থিয়েটার উইন্ডো' : 'Large Theater Window'}
              >
                {lang === 'bn' ? 'বড়' : 'Large'}
              </button>
              <button
                onClick={() =>
                  setPlayerSize(playerSize === 'fullscreen' ? 'medium' : 'fullscreen')
                }
                className={`p-1 rounded-lg text-slate-300 hover:text-white transition ${
                  playerSize === 'fullscreen' ? 'bg-cyan-500 text-slate-950' : ''
                }`}
                title="Fullscreen"
              >
                {playerSize === 'fullscreen' ? (
                  <Minimize className="w-3.5 h-3.5" />
                ) : (
                  <Maximize className="w-3.5 h-3.5" />
                )}
              </button>
            </div>

            {/* Custom Sizing Drawer Toggle */}
            <button
              onClick={() => setShowSettingsDrawer(!showSettingsDrawer)}
              className={`p-2 rounded-xl border transition ${
                showSettingsDrawer
                  ? 'bg-cyan-500/25 border-cyan-400 text-cyan-200'
                  : 'glass-card border-white/10 text-slate-300 hover:text-white'
              }`}
              title={lang === 'bn' ? 'কাস্টম সাইজ ও অনুপাত সেটিংস' : 'Custom Size & Aspect Ratio'}
            >
              <Settings2 className="w-4 h-4" />
            </button>

            {/* Close Button */}
            <button
              onClick={onClose}
              className="p-2 rounded-xl glass-card text-slate-400 hover:text-rose-400 hover:bg-rose-500/15 transition"
              title="Close Player"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Custom Sizing & Aspect Ratio Drawer Panel */}
        {showSettingsDrawer && (
          <div className="px-4 py-3 bg-slate-900/95 border-b border-cyan-500/20 space-y-3 z-30 animate-fade-in text-xs">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              {/* Width Slider */}
              <div className="flex-1 w-full space-y-1">
                <div className="flex items-center justify-between text-[11px] text-slate-300 font-semibold">
                  <span>{lang === 'bn' ? 'কাস্টম প্লেয়ারের প্রস্থ:' : 'Custom Player Width:'}</span>
                  <span className="font-mono text-cyan-300 font-bold">{customWidth}px</span>
                </div>
                <input
                  type="range"
                  min="340"
                  max="1020"
                  step="20"
                  value={customWidth}
                  onChange={(e) => {
                    setCustomWidth(Number(e.target.value));
                    setPlayerSize('custom');
                  }}
                  className="w-full accent-cyan-400 cursor-pointer"
                />
              </div>

              {/* Aspect Ratio Options */}
              <div className="space-y-1 w-full sm:w-auto">
                <span className="text-[11px] text-slate-300 font-semibold block">
                  {lang === 'bn' ? 'ভিডিও অনুপাত:' : 'Aspect Ratio:'}
                </span>
                <div className="flex items-center gap-1">
                  {(['16:9', '4:3', '1:1', 'fit', 'fill'] as const).map((ratio) => (
                    <button
                      key={ratio}
                      onClick={() => setAspectRatio(ratio)}
                      className={`px-2 py-1 rounded-lg text-[10px] font-mono font-bold uppercase transition ${
                        aspectRatio === ratio
                          ? 'bg-cyan-500 text-slate-950 shadow-sm'
                          : 'bg-white/5 hover:bg-white/10 text-slate-300 border border-white/5'
                      }`}
                    >
                      {ratio}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Additional switches: Backdrop dimmer & Background Audio Mode */}
            <div className="flex items-center justify-between pt-1 border-t border-white/10 text-[11px] text-slate-400">
              <label className="flex items-center gap-2 cursor-pointer hover:text-white">
                <input
                  type="checkbox"
                  checked={dimBackdrop}
                  onChange={(e) => setDimBackdrop(e.target.checked)}
                  className="accent-cyan-400 rounded"
                />
                <span>{lang === 'bn' ? 'পেছনের ব্যাকগ্রাউন্ড আবছা রাখুন' : 'Dim background overlay'}</span>
              </label>

              <button
                onClick={() => {
                  onClose();
                  onPlayVideoAsAudio(videoTrack);
                }}
                className="flex items-center gap-1.5 px-3 py-1 rounded-xl bg-purple-600/30 hover:bg-purple-600/40 border border-purple-400/40 text-purple-200 font-semibold text-xs transition"
              >
                <Headphones className="w-3.5 h-3.5" />
                <span>{lang === 'bn' ? 'ব্যাকগ্রাউন্ডে অডিও শুনুন' : 'Play as Background Audio'}</span>
              </button>
            </div>
          </div>
        )}

        {/* Video Canvas Stage */}
        <div
          className="relative w-full flex items-center justify-center bg-black overflow-hidden flex-1 min-h-[220px] sm:min-h-[320px] cursor-pointer"
          style={{ filter: `brightness(${brightness})` }}
          onClick={() => {
            if (!isLocked) setShowControls(!showControls);
          }}
        >
          <video
            ref={videoRef}
            src={videoTrack.url}
            className={`${getAspectRatioStyle()} transition-all duration-300 max-h-[70vh]`}
            autoPlay
            playsInline
            onTimeUpdate={() => {
              if (videoRef.current) {
                setCurrentTime(videoRef.current.currentTime);
                setDuration(videoRef.current.duration || videoTrack.duration);
              }
            }}
            onEnded={() => setIsPlaying(false)}
          />

          {/* Quick Double-Tap gesture zones */}
          <div className="absolute inset-0 flex pointer-events-auto">
            <div
              className="w-1/3 h-full"
              onDoubleClick={(e) => {
                e.stopPropagation();
                handleQuickSeek(-10);
              }}
            />
            <div
              className="w-1/3 h-full"
              onClick={() => {
                if (!isLocked) setShowControls(!showControls);
              }}
            />
            <div
              className="w-1/3 h-full"
              onDoubleClick={(e) => {
                e.stopPropagation();
                handleQuickSeek(10);
              }}
            />
          </div>

          {/* Double Tap seek banner */}
          {seekNotice && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <span className="px-5 py-2.5 rounded-2xl glass-panel text-white text-lg font-bold backdrop-blur-xl animate-ping border border-cyan-400/40">
                {seekNotice}
              </span>
            </div>
          )}

          {/* Center Play indicator when paused */}
          {!isPlaying && !isLocked && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-16 h-16 rounded-full bg-cyan-500/80 text-black flex items-center justify-center shadow-2xl backdrop-blur-md animate-pulse">
                <Play className="w-8 h-8 fill-current ml-1" />
              </div>
            </div>
          )}
        </div>

        {/* Video Bottom Floating Controls Overlay */}
        <div
          className={`p-3 sm:p-4 bg-slate-900/90 border-t border-white/10 space-y-2.5 transition-opacity duration-300 z-20 ${
            showControls || isLocked ? 'opacity-100' : 'opacity-0 pointer-events-none'
          }`}
        >
          {/* Progress Range Slider */}
          <div className="space-y-1">
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={(e) => handleSeek(Number(e.target.value))}
              aria-label="Seek video"
              className="w-full accent-cyan-400 cursor-pointer h-1.5"
            />
            <div className="flex items-center justify-between text-[11px] text-slate-300 font-mono">
              <span>{formatTime(currentTime)}</span>
              <span>{formatTime(duration)}</span>
            </div>
          </div>

          {/* Main Controls Row */}
          <div className="flex items-center justify-between gap-2">
            {/* Left: Volume & Brightness */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  const nextMuted = !isMuted;
                  setIsMuted(nextMuted);
                  if (videoRef.current) videoRef.current.muted = nextMuted;
                }}
                className="p-2 rounded-xl glass-card text-slate-300 hover:text-white"
              >
                {isMuted || volume === 0 ? (
                  <VolumeX className="w-4 h-4 text-rose-400" />
                ) : (
                  <Volume2 className="w-4 h-4 text-cyan-300" />
                )}
              </button>

              <input
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={isMuted ? 0 : volume}
                onChange={(e) => {
                  const val = Number(e.target.value);
                  setVolume(val);
                  setIsMuted(val === 0);
                  if (videoRef.current) {
                    videoRef.current.volume = val;
                    videoRef.current.muted = val === 0;
                  }
                }}
                className="w-16 sm:w-20 accent-cyan-400 cursor-pointer hidden xs:inline"
              />

              {/* Brightness */}
              <div className="hidden sm:flex items-center gap-1.5 glass-pill px-2.5 py-1 rounded-xl">
                <Sun className="w-3.5 h-3.5 text-amber-300" />
                <input
                  type="range"
                  min="0.4"
                  max="1.5"
                  step="0.05"
                  value={brightness}
                  onChange={(e) => setBrightness(Number(e.target.value))}
                  className="w-14 accent-amber-400"
                />
              </div>
            </div>

            {/* Center: Rewind, Play/Pause, Forward */}
            <div className="flex items-center gap-3">
              <button
                onClick={() => handleQuickSeek(-10)}
                className="p-2 text-slate-300 hover:text-white transition active:scale-90"
                title="-10s"
              >
                <RotateCcw className="w-4 h-4 sm:w-5 sm:h-5" />
              </button>

              <button
                onClick={togglePlay}
                className="w-11 h-11 rounded-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 flex items-center justify-center shadow-lg shadow-cyan-500/30 transition active:scale-95 font-bold"
              >
                {isPlaying ? (
                  <Pause className="w-5 h-5 fill-current" />
                ) : (
                  <Play className="w-5 h-5 fill-current ml-0.5" />
                )}
              </button>

              <button
                onClick={() => handleQuickSeek(10)}
                className="p-2 text-slate-300 hover:text-white transition active:scale-90"
                title="+10s"
              >
                <RotateCw className="w-4 h-4 sm:w-5 sm:h-5" />
              </button>
            </div>

            {/* Right: Speed, PiP, Lock */}
            <div className="flex items-center gap-1.5">
              {/* Playback speed */}
              <button
                onClick={() => {
                  const rates = [0.5, 1, 1.25, 1.5, 2];
                  const nextIdx = (rates.indexOf(playbackRate) + 1) % rates.length;
                  const nextRate = rates[nextIdx];
                  setPlaybackRate(nextRate);
                  if (videoRef.current) videoRef.current.playbackRate = nextRate;
                }}
                className="px-2 py-1 rounded-xl glass-pill text-[11px] font-mono font-bold text-cyan-300 hover:text-white"
              >
                {playbackRate}x
              </button>

              {/* PiP */}
              <button
                onClick={handleTogglePiP}
                className="p-2 rounded-xl glass-card text-slate-300 hover:text-white"
                title="Picture in Picture"
              >
                <PictureInPicture className="w-4 h-4" />
              </button>

              {/* Lock */}
              <button
                onClick={() => setIsLocked(!isLocked)}
                className={`p-2 rounded-xl transition ${
                  isLocked ? 'bg-amber-500 text-slate-950' : 'glass-card text-slate-300 hover:text-white'
                }`}
                title="Lock"
              >
                {isLocked ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
