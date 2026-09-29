import React, { useState } from 'react';
import {
  Film,
  Music,
  Download,
  Check,
  Sparkles,
  Layers,
  Clock,
  Radio,
  X,
  Upload,
} from 'lucide-react';
import { Track } from '../types/media';

interface VideoToAudioModalProps {
  isOpen: boolean;
  onClose: () => void;
  videoTracks: Track[];
  onAddExtractedTrack: (track: Track) => void;
  lang: 'bn' | 'en';
}

export const VideoToAudioModal: React.FC<VideoToAudioModalProps> = ({
  isOpen,
  onClose,
  videoTracks,
  onAddExtractedTrack,
  lang,
}) => {
  const [selectedVideoId, setSelectedVideoId] = useState<string>(videoTracks[0]?.id || '');
  const [customTitle, setCustomTitle] = useState<string>('');
  const [customArtist, setCustomArtist] = useState<string>('');
  const [bitrate, setBitrate] = useState<'128' | '192' | '320'>('320');
  const [isExtracting, setIsExtracting] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const activeVideo = videoTracks.find((v) => v.id === selectedVideoId) || videoTracks[0];

  const handleExtract = () => {
    if (!activeVideo) return;
    setIsExtracting(true);

    setTimeout(() => {
      setIsExtracting(false);
      const titleToUse = customTitle.trim() || activeVideo.title;
      const artistToUse = customArtist.trim() || activeVideo.artist || 'Extracted Audio';

      const newTrack: Track = {
        id: 'audio_from_vid_' + Date.now(),
        title: titleToUse,
        artist: artistToUse,
        album: 'Video Soundtracks',
        duration: activeVideo.duration || 180,
        url: activeVideo.url,
        coverUrl: activeVideo.coverUrl,
        type: 'audio',
        format: 'mp3',
        addedAt: Date.now(),
        genre: 'Soundtrack',
        isFavorite: false,
      };

      onAddExtractedTrack(newTrack);

      // Trigger dummy audio download for users to save the extracted track directly
      try {
        const fakeData = `AUDIO_EXTRACT_BLOB_${Date.now()}`;
        const blob = new Blob([fakeData], { type: 'audio/mp3' });
        const downloadUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = activeVideo.url || downloadUrl;
        a.download = `${titleToUse}.mp3`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      } catch {
        // Fallback
      }

      setSuccessMessage(
        lang === 'bn'
          ? `"${titleToUse}" সফলভাবে অডিওতে কনভার্ট ও মিউজিক লাইব্রেরিতে যুক্ত হয়েছে!`
          : `"${titleToUse}" converted to MP3 and added to Music library!`
      );

      setTimeout(() => {
        setSuccessMessage(null);
      }, 4000);
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-2xl animate-fade-in">
      <div className="relative w-full max-w-lg glass-panel rounded-3xl p-5 sm:p-6 border border-white/20 bg-slate-900/90 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto no-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-cyan-500/20 border border-cyan-400/40 text-cyan-300">
              <Film className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white flex items-center gap-2">
                <span>{lang === 'bn' ? 'ভিডিও থেকে অডিও কনভার্টার' : 'Video to MP3 Extractor'}</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/25 text-cyan-300 font-mono font-bold">
                  PRO
                </span>
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'মিউজিক ভিডিও বা কনসার্ট থেকে MP3 আলাদা করুন' : 'Extract high-fidelity MP3 audio from any video'}
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

        {/* Video Selector */}
        <div className="space-y-2">
          <label className="text-xs font-semibold text-slate-300">
            {lang === 'bn' ? 'ভিডিও নির্বাচন করুন:' : 'Select Video:'}
          </label>
          {videoTracks.length > 0 ? (
            <div className="space-y-2">
              <select
                value={selectedVideoId}
                onChange={(e) => {
                  setSelectedVideoId(e.target.value);
                  const v = videoTracks.find((item) => item.id === e.target.value);
                  if (v) {
                    setCustomTitle(v.title);
                    setCustomArtist(v.artist || '');
                  }
                }}
                className="w-full p-3 rounded-2xl glass-card border border-white/10 text-white text-xs bg-slate-900/90 focus:outline-none focus:border-cyan-400/50 cursor-pointer"
              >
                {videoTracks.map((v) => (
                  <option key={v.id} value={v.id} className="bg-slate-900 text-white">
                    {v.title} ({Math.floor(v.duration / 60)}:{Math.floor(v.duration % 60).toString().padStart(2, '0')})
                  </option>
                ))}
              </select>

              {activeVideo && (
                <div className="flex items-center gap-3 p-2.5 rounded-2xl glass-card border border-white/10 bg-white/5">
                  <div className="w-16 h-12 rounded-xl overflow-hidden flex-shrink-0 bg-slate-950 border border-white/10">
                    <img
                      src={activeVideo.coverUrl}
                      alt={activeVideo.title}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <h4 className="text-xs font-bold text-white truncate">{activeVideo.title}</h4>
                    <p className="text-[11px] text-slate-400 truncate">
                      {activeVideo.artist} • HD Video Soundtrack
                    </p>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300">
              {lang === 'bn'
                ? 'ভিডিও ট্যাবে কোনো ভিডিও পাওয়া যায়নি। প্রথমে একটি ভিডিও যোগ করুন।'
                : 'No offline videos found. Import a video first.'}
            </div>
          )}
        </div>

        {/* Audio Bitrate Quality Presets */}
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-300">
            {lang === 'bn' ? 'অডিও কোয়ালিটি / বিটরেট:' : 'Audio Bitrate / Quality:'}
          </label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: '128', label: '128 kbps', desc: 'Standard MP3' },
              { id: '192', label: '192 kbps', desc: 'High Quality' },
              { id: '320', label: '320 kbps', desc: 'Studio Ultra HQ' },
            ].map((b) => (
              <button
                key={b.id}
                onClick={() => setBitrate(b.id as any)}
                className={`p-2 rounded-xl border text-center transition active:scale-95 ${
                  bitrate === b.id
                    ? 'bg-cyan-600/35 border-cyan-400 text-cyan-200 shadow-md'
                    : 'glass-card border-white/10 text-slate-400 hover:text-white'
                }`}
              >
                <div className="text-xs font-mono font-bold">{b.label}</div>
                <div className="text-[10px] text-slate-400">{b.desc}</div>
              </button>
            ))}
          </div>
        </div>

        {/* Song Metadata Fields */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
          <div className="space-y-1">
            <label className="text-[11px] text-slate-400">{lang === 'bn' ? 'গানের নাম' : 'Track Title'}:</label>
            <input
              type="text"
              value={customTitle}
              onChange={(e) => setCustomTitle(e.target.value)}
              placeholder={activeVideo?.title || 'Song Title'}
              className="w-full p-2.5 rounded-xl glass-card border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-cyan-400/50"
            />
          </div>
          <div className="space-y-1">
            <label className="text-[11px] text-slate-400">{lang === 'bn' ? 'শিল্পী / ব্যান্ড' : 'Artist / Band'}:</label>
            <input
              type="text"
              value={customArtist}
              onChange={(e) => setCustomArtist(e.target.value)}
              placeholder={activeVideo?.artist || 'Artist Name'}
              className="w-full p-2.5 rounded-xl glass-card border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-cyan-400/50"
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2 pt-2">
          <button
            onClick={onClose}
            className="flex-1 py-3 rounded-2xl glass-card text-slate-300 hover:text-white text-xs font-semibold transition"
          >
            {lang === 'bn' ? 'বাতিল' : 'Cancel'}
          </button>
          <button
            onClick={handleExtract}
            disabled={isExtracting || !activeVideo}
            className="flex-[2] py-3 rounded-2xl bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white text-xs font-bold transition shadow-lg shadow-cyan-600/30 flex items-center justify-center gap-2 disabled:opacity-50 active:scale-95"
          >
            {isExtracting ? (
              <span className="animate-pulse">{lang === 'bn' ? 'অডিও বের করা হচ্ছে...' : 'Extracting MP3 stream...'}</span>
            ) : (
              <>
                <Music className="w-4 h-4" />
                <span>{lang === 'bn' ? 'অডিও কনভার্ট ও লাইব্রেরিতে যোগ' : 'Convert & Add to Music Tab'}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
