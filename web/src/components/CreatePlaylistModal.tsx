import React, { useState } from 'react';
import { X, ListMusic, Plus } from 'lucide-react';

interface CreatePlaylistModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreatePlaylist: (name: string, description: string) => void;
  lang: 'bn' | 'en';
}

export const CreatePlaylistModal: React.FC<CreatePlaylistModalProps> = ({
  isOpen,
  onClose,
  onCreatePlaylist,
  lang,
}) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onCreatePlaylist(name.trim(), description.trim());
    setName('');
    setDescription('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xl animate-fade-in">
      <div className="w-full max-w-sm rounded-3xl glass-panel p-6 border border-white/20 shadow-2xl space-y-4 bg-slate-900/90">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-purple-500/20 border border-purple-500/40 flex items-center justify-center text-purple-300">
              <ListMusic className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {lang === 'bn' ? 'নতুন প্লেলিস্ট' : 'New Playlist'}
              </h3>
              <p className="text-xs text-slate-400">
                {lang === 'bn' ? 'পছন্দের গান সাজিয়ে রাখুন' : 'Create a custom collection'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-xl glass-card text-slate-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              {lang === 'bn' ? 'প্লেলিস্টের নাম *' : 'Playlist Name *'}
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={lang === 'bn' ? 'যেমন: প্রিয় বাংলা গান' : 'e.g., My Favorite Beats'}
              className="w-full px-3.5 py-2.5 rounded-xl glass-card border border-white/10 text-white text-sm focus:outline-none focus:border-purple-400/50"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1">
              {lang === 'bn' ? 'বিবরণ (ঐচ্ছিক)' : 'Description (Optional)'}
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={lang === 'bn' ? 'যেমন: রাতের বেলা শোনার গান' : 'e.g., Best acoustic songs'}
              className="w-full px-3.5 py-2.5 rounded-xl glass-card border border-white/10 text-white text-sm focus:outline-none focus:border-purple-400/50"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl glass-pill text-xs font-semibold text-slate-300 hover:text-white"
            >
              {lang === 'bn' ? 'বাতিল' : 'Cancel'}
            </button>
            <button
              type="submit"
              disabled={!name.trim()}
              className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95"
            >
              {lang === 'bn' ? 'তৈরি করুন' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
