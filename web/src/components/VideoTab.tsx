import React, { useState, useMemo } from 'react';
import {
  Video,
  Play,
  Headphones,
  Search,
  Plus,
  Clock,
  Sparkles,
  Layers,
  Heart,
  Maximize2,
} from 'lucide-react';
import { Track } from '../types/media';

interface VideoTabProps {
  tracks: Track[];
  onPlayVideo: (track: Track) => void;
  onPlayVideoAsAudio: (track: Track) => void;
  onToggleFavorite: (trackId: string) => void;
  onOpenImport: () => void;
  lang: 'bn' | 'en';
}

export type VideoSearchScope = 'all' | 'title' | 'artist' | 'album';

export const VideoTab: React.FC<VideoTabProps> = ({
  tracks,
  onPlayVideo,
  onPlayVideoAsAudio,
  onToggleFavorite,
  onOpenImport,
  lang,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchScope, setSearchScope] = useState<VideoSearchScope>('all');

  const videoTracks = useMemo(() => {
    return tracks.filter((t) => t.type === 'video');
  }, [tracks]);

  const filteredVideos = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return videoTracks;

    return videoTracks.filter((t) => {
      const matchTitle = t.title.toLowerCase().includes(q);
      const matchArtist = t.artist.toLowerCase().includes(q);
      const matchAlbum = Boolean(t.album && t.album.toLowerCase().includes(q));
      const matchGenre = Boolean(t.genre && t.genre.toLowerCase().includes(q));

      if (searchScope === 'title') return matchTitle;
      if (searchScope === 'artist') return matchArtist;
      if (searchScope === 'album') return matchAlbum;
      return matchTitle || matchArtist || matchAlbum || matchGenre;
    });
  }, [videoTracks, searchQuery, searchScope]);

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const formatSize = (bytes?: number) => {
    if (!bytes) return '4.2 MB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
      {/* Top Search & Filter Bar */}
      <div className="glass-card rounded-3xl p-3 sm:p-4 border border-cyan-500/20 bg-slate-900/60 shadow-xl space-y-2.5 backdrop-blur-2xl">
        <div className="flex flex-col sm:flex-row gap-2.5">
          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-cyan-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={
                searchScope === 'title'
                  ? lang === 'bn' ? 'ভিডিও নাম দিয়ে খুঁজুন...' : 'Search by video title...'
                  : searchScope === 'artist'
                  ? lang === 'bn' ? 'নির্মাতা/শিল্পীর নাম দিয়ে খুঁজুন...' : 'Search by artist or creator...'
                  : searchScope === 'album'
                  ? lang === 'bn' ? 'অ্যালবাম/সংকলন দিয়ে খুঁজুন...' : 'Search by collection/album...'
                  : lang === 'bn' ? 'ভিডিও নাম, শিল্পী বা অ্যালবাম খুঁজুন...' : 'Search title, artist, or album...'
              }
              className="w-full pl-10 pr-9 py-2.5 rounded-2xl bg-white/5 border border-white/10 text-white placeholder-slate-400 text-sm focus:outline-none focus:border-cyan-400/50 transition backdrop-blur-xl"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full text-xs text-slate-400 hover:text-white bg-white/10 hover:bg-white/20 transition"
                title="Clear"
              >
                ✕
              </button>
            )}
          </div>

          <button
            onClick={onOpenImport}
            className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-cyan-600/30 hover:bg-cyan-600/40 border border-cyan-400/30 text-xs font-semibold text-cyan-200 transition active:scale-95 whitespace-nowrap justify-center"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'ভিডিও যোগ করুন' : 'Import Video'}</span>
          </button>
        </div>

        {/* Search Scope Filter Chips (All, Title, Artist, Album) */}
        <div className="flex items-center justify-between gap-2 pt-1 border-t border-white/5">
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
            <span className="text-[11px] text-slate-400 font-medium mr-1 hidden sm:inline">
              {lang === 'bn' ? 'ফিল্টার:' : 'Filter:'}
            </span>
            {(
              [
                { id: 'all', labelBn: 'সবগুলো', labelEn: 'All' },
                { id: 'title', labelBn: 'ভিডিও নাম', labelEn: 'Title' },
                { id: 'artist', labelBn: 'শিল্পী / চ্যানেল', labelEn: 'Artist' },
                { id: 'album', labelBn: 'অ্যালবাম', labelEn: 'Album' },
              ] as const
            ).map((scope) => {
              const isActive = searchScope === scope.id;
              return (
                <button
                  key={scope.id}
                  onClick={() => setSearchScope(scope.id)}
                  className={`px-2.5 py-1 rounded-xl text-xs font-medium transition active:scale-95 whitespace-nowrap ${
                    isActive
                      ? 'bg-cyan-600 text-white shadow-md border border-cyan-400/40'
                      : 'bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white border border-white/5'
                  }`}
                >
                  {lang === 'bn' ? scope.labelBn : scope.labelEn}
                </button>
              );
            })}
          </div>

          {searchQuery && (
            <div className="text-[11px] font-mono text-cyan-300 whitespace-nowrap">
              {filteredVideos.length} {lang === 'bn' ? 'টি ভিডিও' : 'found'}
            </div>
          )}
        </div>
      </div>

      {/* Video Background Playback highlight */}
      <div className="glass-card rounded-3xl p-4 sm:p-5 border border-cyan-500/20 bg-gradient-to-r from-cyan-950/40 via-purple-950/30 to-indigo-950/30 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xl">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-300 flex-shrink-0">
            <Headphones className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-2">
              {lang === 'bn'
                ? 'ভিডিও ব্যাকগ্রাউন্ড প্লেয়ার'
                : 'Background Video Playback'}
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/20 border border-cyan-400/30 text-cyan-200">
                PRO
              </span>
            </h3>
            <p className="text-xs text-slate-300 mt-0.5">
              {lang === 'bn'
                ? 'স্ক্রিন অফ রেখে বা অন্য অ্যাপ চালালেও ভিডিও অডিও চলবে।'
                : 'Listen to video soundtracks in the background or with screen turned off.'}
            </p>
          </div>
        </div>
      </div>

      {/* Videos Count */}
      <div className="flex items-center justify-between text-xs text-slate-400 px-1">
        <span>
          {filteredVideos.length} {lang === 'bn' ? 'টি অফলাইন ভিডিও' : 'Offline Videos'}
        </span>
        <span className="flex items-center gap-1 text-[11px] text-cyan-300">
          <Sparkles className="w-3 h-3" />
          {lang === 'bn' ? 'এইচডি ও ৪কে সাপোর্ট' : 'HD & 4K Ready'}
        </span>
      </div>

      {/* Video Grid */}
      {filteredVideos.length === 0 ? (
        <div className="glass-card rounded-3xl p-8 text-center space-y-3 border border-white/10">
          <Video className="w-12 h-12 mx-auto text-cyan-400/60" />
          <h3 className="text-base font-semibold text-white">
            {lang === 'bn' ? 'কোন ভিডিও পাওয়া যায়নি' : 'No videos found'}
          </h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            {lang === 'bn'
              ? 'আপনার মেমোরি কার্ড বা ফোন থেকে ভিডিও ইমপোর্ট করতে "ভিডিও যোগ করুন" বাটনে চাপুন।'
              : 'Click "Import Video" to add offline MP4, MKV, or WebM clips from your device.'}
          </p>
          <button
            onClick={onOpenImport}
            className="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-cyan-600 text-white text-xs font-semibold shadow-lg shadow-cyan-600/30"
          >
            <Plus className="w-4 h-4" />
            {lang === 'bn' ? 'ডিভাইস থেকে ভিডিও আনুন' : 'Import Video'}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {filteredVideos.map((video) => (
            <div
              key={video.id}
              className="glass-card glass-card-hover rounded-2xl overflow-hidden border border-white/10 group flex flex-col justify-between"
            >
              {/* Thumbnail Container */}
              <div
                onClick={() => onPlayVideo(video)}
                className="relative aspect-video w-full bg-slate-900 cursor-pointer overflow-hidden"
              >
                <img
                  src={video.coverUrl}
                  alt={video.title}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                />

                {/* Dark Vignette Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

                {/* Duration Badge */}
                <span className="absolute bottom-2.5 right-2.5 px-2 py-0.5 rounded-lg bg-black/75 backdrop-blur-md text-white text-[11px] font-mono font-medium border border-white/15 flex items-center gap-1">
                  <Clock className="w-3 h-3 text-cyan-400" />
                  {formatDuration(video.duration)}
                </span>

                {/* Format / Resolution Badge */}
                <span className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded-lg bg-cyan-500/30 backdrop-blur-md text-cyan-200 text-[10px] font-bold border border-cyan-400/30">
                  {video.format || 'HD MP4'}
                </span>

                {/* Center Play Button Overlay */}
                <div className="absolute inset-0 flex items-center justify-center opacity-90 group-hover:opacity-100 transition-opacity">
                  <div className="w-12 h-12 rounded-full bg-cyan-500/80 backdrop-blur-md flex items-center justify-center text-white shadow-xl shadow-cyan-500/30 group-hover:scale-110 transition-transform">
                    <Play className="w-5 h-5 fill-white ml-0.5" />
                  </div>
                </div>
              </div>

              {/* Video Info and Controls */}
              <div className="p-3.5 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <h4 className="text-sm font-semibold text-white truncate leading-snug">
                      {video.title}
                    </h4>
                    <p className="text-xs text-slate-400 truncate mt-0.5">
                      {video.artist} • {formatSize(video.size)}
                    </p>
                  </div>

                  <button
                    onClick={() => onToggleFavorite(video.id)}
                    className="p-1.5 text-slate-400 hover:text-red-400 transition"
                    title="Favorite"
                  >
                    <Heart
                      className={`w-4 h-4 ${
                        video.isFavorite ? 'fill-red-500 text-red-500' : ''
                      }`}
                    />
                  </button>
                </div>

                {/* Action Buttons: Fullscreen Video vs Background Audio */}
                <div className="flex items-center gap-2 pt-1">
                  <button
                    onClick={() => onPlayVideo(video)}
                    className="flex-1 py-1.5 px-3 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30 border border-cyan-400/30 text-cyan-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition active:scale-95"
                  >
                    <Maximize2 className="w-3.5 h-3.5" />
                    <span>{lang === 'bn' ? 'ভিডিও চালান' : 'Play Video'}</span>
                  </button>

                  <button
                    onClick={() => onPlayVideoAsAudio(video)}
                    className="flex-1 py-1.5 px-3 rounded-xl bg-purple-500/20 hover:bg-purple-500/30 border border-purple-400/30 text-purple-200 text-xs font-semibold flex items-center justify-center gap-1.5 transition active:scale-95"
                    title={
                      lang === 'bn'
                        ? 'ব্যাকগ্রাউন্ডে শুধু অডিও হিসেবে চালান'
                        : 'Play audio in background'
                    }
                  >
                    <Headphones className="w-3.5 h-3.5" />
                    <span>{lang === 'bn' ? 'ব্যাকগ্রাউন্ড অডিও' : 'Background Audio'}</span>
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
