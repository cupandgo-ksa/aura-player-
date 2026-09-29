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
} from 'lucide-react';
import { Track } from '../types/media';

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
  const canvasAnimRef = useRef<HTMLCanvasElement | null>(null);

  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.9);
  const [brightness, setBrightness] = useState(1);
  const [isLocked, setIsLocked] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [aspectRatio, setAspectRatio] = useState<'contain' | 'cover'>('contain');
  const [playbackRate, setPlaybackRate] = useState(1);
  const [seekNotice, setSeekNotice] = useState<string | null>(null);

  // Hide controls after 4 seconds of inactivity
  useEffect(() => {
    if (!showControls || !isPlaying) return;
    const timer = setTimeout(() => {
      setShowControls(false);
    }, 4000);
    return () => clearTimeout(timer);
  }, [showControls, isPlaying]);

  // Handle play/pause sync
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

  // Picture in picture support
  const handleTogglePiP = async () => {
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (videoRef.current && document.pictureInPictureEnabled) {
        await videoRef.current.requestPictureInPicture();
      }
    } catch (e) {
      console.warn('PiP error or unsupported:', e);
    }
  };

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  if (!isOpen || !videoTrack) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col justify-between overflow-hidden select-none">
      {/* Video Content with Brightness filter */}
      <div
        className="relative w-full h-full flex items-center justify-center cursor-pointer overflow-hidden"
        style={{ filter: `brightness(${brightness})` }}
        onClick={() => {
          if (!isLocked) setShowControls(!showControls);
        }}
      >
        <video
          ref={videoRef}
          src={videoTrack.url}
          className={`w-full h-full ${
            aspectRatio === 'cover' ? 'object-cover' : 'object-contain'
          }`}
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

        {/* Double-tap seek notice banner */}
        {seekNotice && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <span className="px-5 py-2.5 rounded-2xl glass-panel text-white text-lg font-bold backdrop-blur-xl animate-ping border border-cyan-400/40">
              {seekNotice}
            </span>
          </div>
        )}
      </div>

      {/* Screen Gestures: Double Tap Areas (Left/Right) */}
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

      {/* Top Glass Bar Controls */}
      <div
        className={`absolute top-0 left-0 right-0 z-20 p-4 glass-panel border-x-0 border-t-0 border-b border-white/10 transition-opacity duration-300 backdrop-blur-2xl ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        <div className="max-w-4xl mx-auto flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <button
              onClick={onClose}
              className="p-2 rounded-2xl glass-card text-white hover:bg-white/20 transition active:scale-95"
            >
              <X className="w-5 h-5" />
            </button>
            <div className="min-w-0">
              <h3 className="text-sm sm:text-base font-bold text-white truncate">
                {videoTrack.title}
              </h3>
              <p className="text-xs text-slate-300 truncate">
                {videoTrack.artist} • {videoTrack.format || 'HD Video'}
              </p>
            </div>
          </div>

          {/* Top Actions: Background Audio mode, PiP, Aspect ratio, Lock */}
          <div className="flex items-center gap-2">
            {/* Background Audio Mode */}
            <button
              onClick={() => {
                onClose();
                onPlayVideoAsAudio(videoTrack);
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/30 hover:bg-purple-600/50 border border-purple-400/40 text-purple-200 text-xs font-semibold transition active:scale-95"
              title={
                lang === 'bn'
                  ? 'ব্যাকগ্রাউন্ডে অডিও শুনুন'
                  : 'Play as Background Audio'
              }
            >
              <Headphones className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">
                {lang === 'bn' ? 'ব্যাকগ্রাউন্ডে চালান' : 'Background Audio'}
              </span>
            </button>

            {/* PiP */}
            <button
              onClick={handleTogglePiP}
              className="p-2 rounded-xl glass-card text-white hover:bg-white/20 transition"
              title="Picture-in-Picture"
            >
              <PictureInPicture className="w-4 h-4" />
            </button>

            {/* Aspect Ratio */}
            <button
              onClick={() =>
                setAspectRatio(aspectRatio === 'contain' ? 'cover' : 'contain')
              }
              className="p-2 rounded-xl glass-card text-white text-xs font-bold transition"
              title="Aspect Ratio"
            >
              {aspectRatio === 'contain' ? 'FIT' : 'FILL'}
            </button>

            {/* Lock Controls */}
            <button
              onClick={() => setIsLocked(!isLocked)}
              className={`p-2 rounded-xl transition ${
                isLocked ? 'bg-amber-500 text-black' : 'glass-card text-white'
              }`}
              title="Lock Screen"
            >
              {isLocked ? <Lock className="w-4 h-4" /> : <Unlock className="w-4 h-4" />}
            </button>
          </div>
        </div>
      </div>

      {/* Floating unlock button if locked */}
      {isLocked && (
        <button
          onClick={() => setIsLocked(false)}
          className="absolute top-6 left-6 z-30 p-3 rounded-2xl bg-amber-500/90 text-black shadow-2xl flex items-center gap-2 text-xs font-bold backdrop-blur-md"
        >
          <Lock className="w-4 h-4" />
          <span>{lang === 'bn' ? 'আনলক করুন' : 'Unlock Screen'}</span>
        </button>
      )}

      {/* Bottom Glass Bar Controls */}
      <div
        className={`absolute bottom-0 left-0 right-0 z-20 p-4 sm:p-6 glass-panel border-x-0 border-b-0 border-t border-white/10 transition-opacity duration-300 backdrop-blur-2xl space-y-3 ${
          showControls && !isLocked ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        <div className="max-w-4xl mx-auto space-y-3">
          {/* Progress Slider */}
          <div className="space-y-1">
            <input
              type="range"
              min="0"
              max={duration || 100}
              step="0.5"
              value={currentTime}
              onChange={(e) => handleSeek(Number(e.target.value))}
              aria-label="Seek video"
              className="w-full accent-cyan-400"
            />
            <div className="flex items-center justify-between text-xs text-slate-300 font-mono">
              <span>{formatTime(currentTime)}</span>
              <span>{formatTime(duration)}</span>
            </div>
          </div>

          {/* Main Controls Row */}
          <div className="flex items-center justify-between">
            {/* Left: Volume & Brightness sliders */}
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1.5 glass-pill px-2.5 py-1 rounded-xl">
                <Volume2 className="w-3.5 h-3.5 text-cyan-300" />
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={volume}
                  onChange={(e) => {
                    const val = Number(e.target.value);
                    setVolume(val);
                    if (videoRef.current) videoRef.current.volume = val;
                  }}
                  aria-label="Volume"
                  className="w-16 accent-cyan-400"
                />
              </div>

              <div className="hidden sm:flex items-center gap-1.5 glass-pill px-2.5 py-1 rounded-xl">
                <Sun className="w-3.5 h-3.5 text-amber-300" />
                <input
                  type="range"
                  min="0.3"
                  max="1.5"
                  step="0.05"
                  value={brightness}
                  onChange={(e) => setBrightness(Number(e.target.value))}
                  aria-label="Brightness"
                  className="w-16 accent-amber-400"
                />
              </div>
            </div>

            {/* Center: Rewind 10s, Play/Pause, Forward 10s */}
            <div className="flex items-center gap-4">
              <button
                onClick={() => handleQuickSeek(-10)}
                className="p-2 text-white hover:text-cyan-300 transition active:scale-90"
                title="Rewind 10s"
              >
                <RotateCcw className="w-5 h-5" />
              </button>

              <button
                onClick={togglePlay}
                className="w-12 h-12 rounded-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 flex items-center justify-center shadow-lg shadow-cyan-500/40 active:scale-95 transition font-bold"
              >
                {isPlaying ? (
                  <Pause className="w-6 h-6 fill-current" />
                ) : (
                  <Play className="w-6 h-6 fill-current ml-0.5" />
                )}
              </button>

              <button
                onClick={() => handleQuickSeek(10)}
                className="p-2 text-white hover:text-cyan-300 transition active:scale-90"
                title="Forward 10s"
              >
                <RotateCw className="w-5 h-5" />
              </button>
            </div>

            {/* Right: Playback Speed */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  const rates = [0.5, 1, 1.25, 1.5, 2];
                  const nextIdx = (rates.indexOf(playbackRate) + 1) % rates.length;
                  const nextRate = rates[nextIdx];
                  setPlaybackRate(nextRate);
                  if (videoRef.current) videoRef.current.playbackRate = nextRate;
                }}
                className="px-2.5 py-1 rounded-xl glass-pill text-xs font-mono font-bold text-cyan-300 hover:text-white transition"
              >
                {playbackRate}x
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
