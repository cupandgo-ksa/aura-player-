import React, { useState } from 'react';
import {
  Tag,
  Image,
  Upload,
  Check,
  Disc3,
  User,
  Music2,
  Sparkles,
  X,
} from 'lucide-react';
import { Track } from '../types/media';

interface TagEditorModalProps {
  isOpen: boolean;
  onClose: () => void;
  track: Track | null;
  onSaveTrack: (updatedTrack: Track) => void;
  lang: 'bn' | 'en';
}

const PRESET_COVERS = [
  'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500&auto=format&fit=crop&q=80',
  'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80',
];

export const TagEditorModal: React.FC<TagEditorModalProps> = ({
  isOpen,
  onClose,
  track,
  onSaveTrack,
  lang,
}) => {
  if (!isOpen || !track) return null;

  const [title, setTitle] = useState(track.title);
  const [artist, setArtist] = useState(track.artist);
  const [album, setAlbum] = useState(track.album || '');
  const [genre, setGenre] = useState(track.genre || '');
  const [coverUrl, setCoverUrl] = useState(track.coverUrl);
  const [isSaved, setIsSaved] = useState(false);

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const url = URL.createObjectURL(file);
      setCoverUrl(url);
    }
  };

  const handleSave = () => {
    const updated: Track = {
      ...track,
      title: title.trim() || track.title,
      artist: artist.trim() || track.artist,
      album: album.trim() || track.album,
      genre: genre.trim() || track.genre,
      coverUrl,
    };
    onSaveTrack(updated);
    setIsSaved(true);
    setTimeout(() => {
      setIsSaved(false);
      onClose();
    }, 800);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-2xl animate-fade-in">
      <div className="relative w-full max-w-lg glass-panel rounded-3xl p-5 sm:p-6 border border-white/20 bg-slate-900/90 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto no-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-purple-500/20 border border-purple-400/40 text-purple-300">
              <Tag className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white">
                {lang === 'bn' ? 'ট্যাগ ও কভার ফটো এডিটর' : 'ID3 Tag & Cover Art Editor'}
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'গানের নাম, শিল্পী ও কভার আর্ট পরিবর্তন করুন' : 'Edit song metadata and album artwork'}
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

        {/* Cover Art Preview & Upload */}
        <div className="flex flex-col sm:flex-row items-center gap-4 p-3.5 rounded-2xl bg-white/5 border border-white/10">
          <div className="relative w-24 h-24 rounded-2xl overflow-hidden border-2 border-white/25 shadow-xl flex-shrink-0 group">
            <img src={coverUrl} alt={title} className="w-full h-full object-cover" />
            <label className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex flex-col items-center justify-center cursor-pointer transition text-white text-[10px] font-semibold">
              <Upload className="w-5 h-5 mb-1 text-purple-300" />
              <span>{lang === 'bn' ? 'ফটো আপলোড' : 'Change'}</span>
              <input
                type="file"
                accept="image/*"
                onChange={handleImageUpload}
                className="hidden"
              />
            </label>
          </div>

          <div className="space-y-2 flex-1 w-full text-center sm:text-left">
            <div>
              <span className="text-xs font-semibold text-white block">
                {lang === 'bn' ? 'অ্যালবাম কভার আর্ট' : 'Album Cover Art'}
              </span>
              <span className="text-[11px] text-slate-400">
                {lang === 'bn' ? 'ডিভাইস থেকে ছবি আপলোড করুন অথবা প্রিসেট বেছে নিন' : 'Choose custom photo or pick from glass presets'}
              </span>
            </div>

            <div className="flex items-center justify-center sm:justify-start gap-1.5 pt-1">
              {PRESET_COVERS.slice(0, 4).map((preset, idx) => (
                <button
                  key={idx}
                  onClick={() => setCoverUrl(preset)}
                  className={`w-8 h-8 rounded-lg overflow-hidden border transition active:scale-95 ${
                    coverUrl === preset ? 'border-purple-400 scale-105' : 'border-white/20 opacity-70'
                  }`}
                >
                  <img src={preset} alt="preset" className="w-full h-full object-cover" />
                </button>
              ))}
              <label className="px-2.5 py-1.5 rounded-lg glass-card border border-white/20 text-[10px] font-semibold text-purple-200 cursor-pointer hover:bg-white/10 flex items-center gap-1">
                <Upload className="w-3 h-3" />
                <span>{lang === 'bn' ? 'ফাইল' : 'Custom'}</span>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  className="hidden"
                />
              </label>
            </div>
          </div>
        </div>

        {/* Input Fields: Title, Artist, Album, Genre */}
        <div className="space-y-3 text-xs">
          <div className="space-y-1">
            <label className="font-semibold text-slate-300 flex items-center gap-1.5">
              <Music2 className="w-3.5 h-3.5 text-purple-400" />
              <span>{lang === 'bn' ? 'গানের শিরোনাম' : 'Track Title'}</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full p-2.5 rounded-xl glass-card border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400/50"
            />
          </div>

          <div className="space-y-1">
            <label className="font-semibold text-slate-300 flex items-center gap-1.5">
              <User className="w-3.5 h-3.5 text-purple-400" />
              <span>{lang === 'bn' ? 'শিল্পী / ব্যান্ড' : 'Artist / Band'}</span>
            </label>
            <input
              type="text"
              value={artist}
              onChange={(e) => setArtist(e.target.value)}
              className="w-full p-2.5 rounded-xl glass-card border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400/50"
            />
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-1">
              <label className="font-semibold text-slate-300 flex items-center gap-1.5">
                <Disc3 className="w-3.5 h-3.5 text-cyan-400" />
                <span>{lang === 'bn' ? 'অ্যালবাম' : 'Album'}</span>
              </label>
              <input
                type="text"
                value={album}
                onChange={(e) => setAlbum(e.target.value)}
                className="w-full p-2.5 rounded-xl glass-card border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400/50"
              />
            </div>
            <div className="space-y-1">
              <label className="font-semibold text-slate-300 flex items-center gap-1.5">
                <Tag className="w-3.5 h-3.5 text-pink-400" />
                <span>{lang === 'bn' ? 'জনরা (Genre)' : 'Genre'}</span>
              </label>
              <input
                type="text"
                value={genre}
                onChange={(e) => setGenre(e.target.value)}
                className="w-full p-2.5 rounded-xl glass-card border border-white/10 text-white text-xs focus:outline-none focus:border-purple-400/50"
              />
            </div>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex items-center gap-2 pt-2">
          <button
            onClick={onClose}
            className="flex-1 py-3 rounded-2xl glass-card text-slate-300 hover:text-white text-xs font-semibold transition"
          >
            {lang === 'bn' ? 'বাতিল' : 'Cancel'}
          </button>
          <button
            onClick={handleSave}
            disabled={isSaved}
            className="flex-[2] py-3 rounded-2xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold transition shadow-lg shadow-purple-600/30 flex items-center justify-center gap-2 active:scale-95"
          >
            {isSaved ? (
              <>
                <Check className="w-4 h-4 text-emerald-300" />
                <span>{lang === 'bn' ? 'সংরক্ষিত হয়েছে!' : 'Saved Successfully!'}</span>
              </>
            ) : (
              <span>{lang === 'bn' ? 'পরিবর্তন সংরক্ষণ করুন' : 'Save Changes'}</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
