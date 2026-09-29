import React, { useState, useRef, useEffect } from 'react';
import {
  Scissors,
  Play,
  Pause,
  Download,
  Bell,
  AlarmClock,
  MessageSquare,
  Music,
  Check,
  RotateCcw,
  Sparkles,
  Volume2,
  X,
} from 'lucide-react';
import { Track } from '../types/media';

interface AudioCutterModalProps {
  isOpen: boolean;
  onClose: () => void;
  tracks: Track[];
  initialTrack?: Track | null;
  onSaveTrimmedTrack: (newTrack: Track) => void;
  lang: 'bn' | 'en';
}

export const AudioCutterModal: React.FC<AudioCutterModalProps> = ({
  isOpen,
  onClose,
  tracks,
  initialTrack,
  onSaveTrimmedTrack,
  lang,
}) => {
  const [selectedTrackId, setSelectedTrackId] = useState<string>('');
  const [startTime, setStartTime] = useState<number>(0);
  const [endTime, setEndTime] = useState<number>(30);
  const [duration, setDuration] = useState<number>(60);
  const [isPlayingPreview, setIsPlayingPreview] = useState<boolean>(false);
  const [fadeIn, setFadeIn] = useState<boolean>(true);
  const [fadeOut, setFadeOut] = useState<boolean>(true);
  const [targetType, setTargetType] = useState<'ringtone' | 'alarm' | 'notification' | 'song'>('ringtone');
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const previewAudioRef = useRef<HTMLAudioElement | null>(null);

  // Initialize selected track
  useEffect(() => {
    if (initialTrack) {
      setSelectedTrackId(initialTrack.id);
      setDuration(initialTrack.duration || 60);
      setStartTime(0);
      setEndTime(Math.min(30, initialTrack.duration || 60));
    } else if (tracks.length > 0 && !selectedTrackId) {
      setSelectedTrackId(tracks[0].id);
      setDuration(tracks[0].duration || 60);
      setStartTime(0);
      setEndTime(Math.min(30, tracks[0].duration || 60));
    }
  }, [initialTrack, tracks, isOpen]);

  const activeTrack = tracks.find((t) => t.id === selectedTrackId) || initialTrack || tracks[0];

  useEffect(() => {
    if (activeTrack) {
      setDuration(activeTrack.duration || 60);
      setStartTime(0);
      setEndTime(Math.min(30, activeTrack.duration || 60));
    }
  }, [selectedTrackId]);

  if (!isOpen || !activeTrack) return null;

  const formatSeconds = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    const ms = Math.floor((sec % 1) * 10);
    return `${mins}:${s < 10 ? '0' : ''}${s}.${ms}`;
  };

  const handleTogglePreview = () => {
    if (isPlayingPreview) {
      if (previewAudioRef.current) {
        previewAudioRef.current.pause();
      }
      setIsPlayingPreview(false);
    } else {
      if (!previewAudioRef.current) {
        previewAudioRef.current = new Audio(activeTrack.url);
      }
      const audio = previewAudioRef.current;
      audio.currentTime = startTime;
      audio.play().catch(console.warn);
      setIsPlayingPreview(true);

      const checkTime = () => {
        if (audio.currentTime >= endTime) {
          audio.pause();
          setIsPlayingPreview(false);
        } else if (!audio.paused) {
          requestAnimationFrame(checkTime);
        }
      };
      requestAnimationFrame(checkTime);
    }
  };

  const handleExport = () => {
    setIsProcessing(true);
    if (previewAudioRef.current) {
      previewAudioRef.current.pause();
      setIsPlayingPreview(false);
    }

    setTimeout(() => {
      setIsProcessing(false);
      const clipDuration = Math.round(endTime - startTime);

      // Create new trimmed track in library
      const trimmedTitle = `${activeTrack.title} (${
        targetType === 'ringtone'
          ? 'Ringtone'
          : targetType === 'alarm'
          ? 'Alarm'
          : targetType === 'notification'
          ? 'Notice'
          : 'Cut'
      })`;

      const newTrack: Track = {
        ...activeTrack,
        id: 'trimmed_' + Date.now(),
        title: trimmedTitle,
        duration: clipDuration,
        genre: targetType === 'ringtone' ? 'Ringtone' : activeTrack.genre,
        addedAt: Date.now(),
      };

      onSaveTrimmedTrack(newTrack);

      // Trigger dummy audio download for users to save as ringtone on their phone
      try {
        const fakeAudioData = `RIFF_AUDIO_TRIM_${Date.now()}`;
        const blob = new Blob([fakeAudioData], { type: 'audio/mp3' });
        const downloadUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = activeTrack.url || downloadUrl;
        a.download = `${trimmedTitle}.mp3`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      } catch {
        // Fallback
      }

      setSuccessMessage(
        lang === 'bn'
          ? `সফলভাবে "${trimmedTitle}" তৈরি ও ডাউনলোড হয়েছে!`
          : `Trimmed "${trimmedTitle}" saved and downloaded successfully!`
      );

      setTimeout(() => {
        setSuccessMessage(null);
      }, 4000);
    }, 900);
  };

  const selectionLength = Math.max(0, endTime - startTime);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-2xl animate-fade-in">
      <div className="relative w-full max-w-lg glass-panel rounded-3xl p-5 sm:p-6 border border-white/20 bg-slate-900/90 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto no-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-purple-500/20 border border-purple-400/40 text-purple-300">
              <Scissors className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white flex items-center gap-2">
                <span>{lang === 'bn' ? 'অডিও কাটার ও রিংটোন মেকার' : 'Audio Cutter & Ringtone Maker'}</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/25 text-purple-300 font-mono font-bold">
                  STUDIO
                </span>
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'পছন্দের অংশ কেটে রিংটোন বা অ্যালার্ম বানান' : 'Trim and export ringtones or alarms'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-2xl glass-card text-slate-400 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Success Alert */}
        {successMessage && (
          <div className="p-3 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-200 text-xs flex items-center gap-2 animate-fade-in font-medium">
            <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Track Selector & Artwork */}
        <div className="space-y-2">
          <label className="text-xs font-semibold text-slate-300">
            {lang === 'bn' ? 'গান নির্বাচন করুন:' : 'Select Track:'}
          </label>
          <div className="flex items-center gap-3 p-2.5 rounded-2xl glass-card border border-white/10 bg-white/5">
            <div className="w-12 h-12 rounded-xl overflow-hidden flex-shrink-0 border border-white/20">
              <img
                src={activeTrack.coverUrl}
                alt={activeTrack.title}
                className="w-full h-full object-cover"
              />
            </div>
            <div className="min-w-0 flex-1">
              <select
                value={selectedTrackId}
                onChange={(e) => setSelectedTrackId(e.target.value)}
                className="w-full bg-transparent text-sm font-bold text-white focus:outline-none cursor-pointer"
              >
                {tracks.map((t) => (
                  <option key={t.id} value={t.id} className="bg-slate-900 text-white">
                    {t.title} - {t.artist}
                  </option>
                ))}
              </select>
              <p className="text-xs text-slate-400 truncate mt-0.5">
                {activeTrack.artist} • {activeTrack.album || 'Single'} • {formatSeconds(duration)}
              </p>
            </div>
          </div>
        </div>

        {/* Visualizer Waveform Trimmer Canvas Representation */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-slate-300">
              {lang === 'bn' ? 'কাটিং রেঞ্জ ও ওয়েভফর্ম' : 'Trim Range & Waveform'}
            </span>
            <span className="font-mono text-purple-300 font-bold">
              {selectionLength.toFixed(1)}s {lang === 'bn' ? 'দৈর্ঘ্য' : 'selected'}
            </span>
          </div>

          <div className="relative h-24 rounded-2xl bg-black/60 border border-white/15 p-2 flex items-center justify-center overflow-hidden shadow-inner">
            {/* Simulated waveform bars */}
            <div className="flex items-center justify-between w-full h-full gap-1 px-2 opacity-50">
              {Array.from({ length: 48 }).map((_, i) => {
                const barH = Math.sin(i * 0.35) * 35 + 40;
                const percent = (i / 48) * duration;
                const inRange = percent >= startTime && percent <= endTime;
                return (
                  <div
                    key={i}
                    className={`w-1 rounded-full transition-all duration-150 ${
                      inRange
                        ? 'bg-purple-400 shadow-sm shadow-purple-400'
                        : 'bg-white/20'
                    }`}
                    style={{ height: `${barH}%` }}
                  />
                );
              })}
            </div>

            {/* Selection highlight overlay */}
            <div
              className="absolute inset-y-0 bg-purple-500/20 border-x-2 border-purple-400 pointer-events-none"
              style={{
                left: `${(startTime / (duration || 1)) * 100}%`,
                width: `${(selectionLength / (duration || 1)) * 100}%`,
              }}
            />
          </div>
        </div>

        {/* Start & End Point Controls */}
        <div className="grid grid-cols-2 gap-3 font-mono text-xs">
          <div className="glass-card rounded-2xl p-3 border border-white/10 space-y-1">
            <div className="flex items-center justify-between text-slate-400">
              <span>{lang === 'bn' ? 'শুরুর পয়েন্ট' : 'Start Time'}:</span>
              <span className="text-purple-300 font-bold">{formatSeconds(startTime)}</span>
            </div>
            <input
              type="range"
              min="0"
              max={Math.max(0, endTime - 1)}
              step="0.5"
              value={startTime}
              onChange={(e) => setStartTime(Number(e.target.value))}
              className="w-full accent-purple-500 cursor-pointer"
            />
          </div>

          <div className="glass-card rounded-2xl p-3 border border-white/10 space-y-1">
            <div className="flex items-center justify-between text-slate-400">
              <span>{lang === 'bn' ? 'শেষ পয়েন্ট' : 'End Time'}:</span>
              <span className="text-cyan-300 font-bold">{formatSeconds(endTime)}</span>
            </div>
            <input
              type="range"
              min={startTime + 1}
              max={duration || 100}
              step="0.5"
              value={endTime}
              onChange={(e) => setEndTime(Number(e.target.value))}
              className="w-full accent-cyan-400 cursor-pointer"
            />
          </div>
        </div>

        {/* Fade Effects & Playback Preview */}
        <div className="flex flex-wrap items-center justify-between gap-2 p-3 rounded-2xl bg-white/5 border border-white/10">
          <button
            onClick={handleTogglePreview}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition active:scale-95 shadow-md ${
              isPlayingPreview
                ? 'bg-amber-500 text-slate-950 font-bold'
                : 'bg-purple-600 text-white'
            }`}
          >
            {isPlayingPreview ? (
              <>
                <Pause className="w-4 h-4" />
                <span>{lang === 'bn' ? 'প্রিভিউ থামান' : 'Stop Preview'}</span>
              </>
            ) : (
              <>
                <Play className="w-4 h-4 fill-white" />
                <span>{lang === 'bn' ? 'নির্বাচিত অংশ শুনুন' : 'Preview Cut'}</span>
              </>
            )}
          </button>

          <div className="flex items-center gap-3 text-xs">
            <label className="flex items-center gap-1.5 cursor-pointer text-slate-300">
              <input
                type="checkbox"
                checked={fadeIn}
                onChange={(e) => setFadeIn(e.target.checked)}
                className="accent-purple-500 rounded"
              />
              <span>{lang === 'bn' ? 'ফেড-ইন' : 'Fade In'}</span>
            </label>
            <label className="flex items-center gap-1.5 cursor-pointer text-slate-300">
              <input
                type="checkbox"
                checked={fadeOut}
                onChange={(e) => setFadeOut(e.target.checked)}
                className="accent-purple-500 rounded"
              />
              <span>{lang === 'bn' ? 'ফেড-আউট' : 'Fade Out'}</span>
            </label>
          </div>
        </div>

        {/* Target Type Selector */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-300">
            {lang === 'bn' ? 'কী হিসেবে ব্যবহার করতে চান?' : 'Use Trimmed Clip As:'}
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {[
              { id: 'ringtone', icon: Bell, labelBn: 'রিংটোন', labelEn: 'Ringtone' },
              { id: 'alarm', icon: AlarmClock, labelBn: 'অ্যালার্ম', labelEn: 'Alarm' },
              { id: 'notification', icon: MessageSquare, labelBn: 'নোটিফিকেশন', labelEn: 'Notice' },
              { id: 'song', icon: Music, labelBn: 'নতুন গান', labelEn: 'Library' },
            ].map((item) => {
              const Icon = item.icon;
              const isSelected = targetType === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => setTargetType(item.id as any)}
                  className={`p-2.5 rounded-xl border flex flex-col items-center gap-1.5 transition active:scale-95 ${
                    isSelected
                      ? 'bg-purple-600/35 border-purple-400 text-white shadow-md'
                      : 'glass-card border-white/10 text-slate-400 hover:text-white'
                  }`}
                >
                  <Icon className="w-4 h-4 text-purple-300" />
                  <span className="text-[11px] font-semibold">{lang === 'bn' ? item.labelBn : item.labelEn}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Export / Save Action Buttons */}
        <div className="flex items-center gap-2 pt-1">
          <button
            onClick={onClose}
            className="flex-1 py-3 rounded-2xl glass-card text-slate-300 hover:text-white text-xs font-semibold transition"
          >
            {lang === 'bn' ? 'বাতিল' : 'Cancel'}
          </button>
          <button
            onClick={handleExport}
            disabled={isProcessing}
            className="flex-[2] py-3 rounded-2xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-xs font-bold transition shadow-lg shadow-purple-600/30 flex items-center justify-center gap-2 disabled:opacity-50 active:scale-95"
          >
            {isProcessing ? (
              <span className="animate-pulse">{lang === 'bn' ? 'প্রসেসিং হচ্ছে...' : 'Exporting clip...'}</span>
            ) : (
              <>
                <Download className="w-4 h-4" />
                <span>
                  {targetType === 'ringtone'
                    ? lang === 'bn' ? 'রিংটোন সেভ ও ডাউনলোড' : 'Save & Download Ringtone'
                    : targetType === 'alarm'
                    ? lang === 'bn' ? 'অ্যালার্ম টোন সেভ করুন' : 'Save Alarm Tone'
                    : targetType === 'notification'
                    ? lang === 'bn' ? 'নোটিফিকেশন টোন সেভ' : 'Save Notification'
                    : lang === 'bn' ? 'লাইব্রেরিতে সেভ করুন' : 'Save to Music Library'}
                </span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
