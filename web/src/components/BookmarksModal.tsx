import React, { useState, useEffect } from 'react';
import {
  Bookmark,
  BookmarkPlus,
  Play,
  Trash2,
  Clock,
  Sparkles,
  BookOpen,
  Check,
  X,
} from 'lucide-react';
import { Track } from '../types/media';

export interface AudioBookmark {
  id: string;
  trackId: string;
  trackTitle: string;
  timestamp: number;
  note: string;
  createdAt: number;
}

interface BookmarksModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentTrack: Track | null;
  currentTime: number;
  onSeek: (seconds: number) => void;
  onPlayTrack?: (track: Track) => void;
  lang: 'bn' | 'en';
}

export const BookmarksModal: React.FC<BookmarksModalProps> = ({
  isOpen,
  onClose,
  currentTrack,
  currentTime,
  onSeek,
  lang,
}) => {
  const [bookmarks, setBookmarks] = useState<AudioBookmark[]>(() => {
    try {
      const saved = localStorage.getItem('aura_audio_bookmarks');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [newNote, setNewNote] = useState('');
  const [justAdded, setJustAdded] = useState(false);

  useEffect(() => {
    try {
      localStorage.setItem('aura_audio_bookmarks', JSON.stringify(bookmarks));
    } catch {
      // storage quota
    }
  }, [bookmarks]);

  if (!isOpen) return null;

  const formatSeconds = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${mins}:${s < 10 ? '0' : ''}${s}`;
  };

  const handleAddBookmark = () => {
    if (!currentTrack) return;
    const noteText = newNote.trim() || (lang === 'bn' ? `বুকমার্ক (${formatSeconds(currentTime)})` : `Bookmark at ${formatSeconds(currentTime)}`);

    const newBookmark: AudioBookmark = {
      id: 'bm_' + Date.now(),
      trackId: currentTrack.id,
      trackTitle: currentTrack.title,
      timestamp: currentTime,
      note: noteText,
      createdAt: Date.now(),
    };

    setBookmarks([newBookmark, ...bookmarks]);
    setNewNote('');
    setJustAdded(true);
    setTimeout(() => setJustAdded(false), 2000);
  };

  const handleDeleteBookmark = (id: string) => {
    setBookmarks(bookmarks.filter((b) => b.id !== id));
  };

  const handleJumpTo = (bm: AudioBookmark) => {
    onSeek(bm.timestamp);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-2xl animate-fade-in">
      <div className="relative w-full max-w-lg glass-panel rounded-3xl p-5 sm:p-6 border border-white/20 bg-slate-900/90 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto no-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-amber-500/20 border border-amber-400/40 text-amber-300">
              <Bookmark className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white flex items-center gap-2">
                <span>{lang === 'bn' ? 'স্মার্ট বুকমার্ক ও পডকাস্ট রিজিউম' : 'Smart Bookmarks & Resume'}</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/25 text-amber-300 font-mono font-bold">
                  PRO
                </span>
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'ওয়াজ, অডিওবুক বা গানের যেকোনো স্থান সেভ রাখুন' : 'Save and jump to moments in long podcasts or audiobooks'}
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

        {/* Add Current Position Bookmark */}
        {currentTrack ? (
          <div className="p-3.5 rounded-2xl glass-card border border-white/10 bg-white/5 space-y-2.5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-300 font-medium">
                {lang === 'bn' ? 'চলতি অবস্থান:' : 'Current Position:'}
              </span>
              <span className="font-mono text-amber-300 font-bold px-2 py-0.5 rounded-lg bg-amber-500/15 border border-amber-500/30">
                {formatSeconds(currentTime)}
              </span>
            </div>

            <div className="flex gap-2">
              <input
                type="text"
                value={newNote}
                onChange={(e) => setNewNote(e.target.value)}
                placeholder={
                  lang === 'bn' ? 'বুকমার্ক নোট (যেমন: গুরুত্বপূর্ণ আলোচনা)...' : 'Bookmark note (e.g. key point)...'
                }
                className="flex-1 px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white placeholder-slate-500 text-xs focus:outline-none focus:border-amber-400/50"
              />
              <button
                onClick={handleAddBookmark}
                className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-amber-500/20 active:scale-95 whitespace-nowrap"
              >
                {justAdded ? (
                  <>
                    <Check className="w-3.5 h-3.5" />
                    <span>{lang === 'bn' ? 'যুক্ত হলো!' : 'Saved!'}</span>
                  </>
                ) : (
                  <>
                    <BookmarkPlus className="w-3.5 h-3.5" />
                    <span>{lang === 'bn' ? 'বুকমার্ক করুন' : 'Add Bookmark'}</span>
                  </>
                )}
              </button>
            </div>
          </div>
        ) : (
          <div className="p-3 rounded-2xl bg-white/5 text-xs text-slate-400 text-center">
            {lang === 'bn' ? 'বর্তমানে কোনো গান চলছে না।' : 'No audio is currently playing.'}
          </div>
        )}

        {/* Saved Bookmarks List */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold uppercase tracking-wider">
              {lang === 'bn' ? 'সংরক্ষিত বুকমার্কসমূহ' : 'Saved Bookmarks'} ({bookmarks.length})
            </span>
          </div>

          {bookmarks.length === 0 ? (
            <div className="p-6 rounded-2xl bg-white/5 border border-white/10 text-center space-y-2">
              <BookOpen className="w-8 h-8 mx-auto text-slate-500" />
              <p className="text-xs text-slate-400">
                {lang === 'bn'
                  ? 'এখনো কোনো বুকমার্ক নেই। কোনো গান চলাকালীন ওপরের বাটনটি দিয়ে পছন্দের স্থান সংরক্ষণ করুন।'
                  : 'No bookmarks saved yet. Save memorable moments to resume later.'}
              </p>
            </div>
          ) : (
            <div className="space-y-2 max-h-60 overflow-y-auto no-scrollbar">
              {bookmarks.map((bm) => (
                <div
                  key={bm.id}
                  className="p-3 rounded-2xl glass-card border border-white/10 hover:border-amber-400/30 flex items-center justify-between gap-3 group transition"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-amber-300 px-2 py-0.5 rounded-lg bg-amber-500/15 border border-amber-500/25">
                        {formatSeconds(bm.timestamp)}
                      </span>
                      <h4 className="text-xs font-bold text-white truncate">{bm.note}</h4>
                    </div>
                    <p className="text-[11px] text-slate-400 truncate mt-1">
                      {bm.trackTitle}
                    </p>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => handleJumpTo(bm)}
                      className="p-2 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 text-xs font-bold flex items-center gap-1 transition active:scale-95"
                      title="Jump to this position"
                    >
                      <Play className="w-3.5 h-3.5 fill-amber-300" />
                      <span className="text-[11px]">{lang === 'bn' ? 'শুনুন' : 'Play'}</span>
                    </button>
                    <button
                      onClick={() => handleDeleteBookmark(bm.id)}
                      className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition"
                      title="Delete"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Close Button */}
        <div className="pt-2">
          <button
            onClick={onClose}
            className="w-full py-2.5 rounded-2xl glass-card text-slate-300 hover:text-white text-xs font-semibold transition"
          >
            {lang === 'bn' ? 'বন্ধ করুন' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
