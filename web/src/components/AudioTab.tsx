import React, { useState, useMemo, useEffect } from 'react';
import {
  Search,
  Play,
  Pause,
  Heart,
  MoreVertical,
  Plus,
  SlidersHorizontal,
  Clock,
  Sparkles,
  Volume2,
} from 'lucide-react';
import { Track, Playlist } from '../types/media';

interface AudioTabProps {
  tracks: Track[];
  currentTrack: Track | null;
  isPlaying: boolean;
  onPlayTrack: (track: Track) => void;
  onTogglePlay: () => void;
  onToggleFavorite: (trackId: string) => void;
  onAddToQueue: (track: Track) => void;
  onOpenImport: () => void;
  playlists: Playlist[];
  onAddTrackToPlaylist: (playlistId: string, trackId: string) => void;
  lang: 'bn' | 'en';
}

export type AudioSearchScope = 'all' | 'title' | 'artist' | 'album';

export const AudioTab: React.FC<AudioTabProps> = ({
  tracks,
  currentTrack,
  isPlaying,
  onPlayTrack,
  onTogglePlay,
  onToggleFavorite,
  onAddToQueue,
  onOpenImport,
  playlists,
  onAddTrackToPlaylist,
  lang,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchScope, setSearchScope] = useState<AudioSearchScope>('all');
  const [selectedGenre, setSelectedGenre] = useState<string>('all');
  const [sortBy, setSortBy] = useState<'recent' | 'title' | 'duration'>('recent');
  const [activeMenuTrackId, setActiveMenuTrackId] = useState<string | null>(null);

  useEffect(() => {
    if (!activeMenuTrackId) return;
    const outside = (event: PointerEvent) => {
      if (!(event.target as Element).closest('[data-audio-menu]')) setActiveMenuTrackId(null);
    };
    const scroll = () => setActiveMenuTrackId(null);
    const back = (event: KeyboardEvent) => { if (event.key === 'Escape') setActiveMenuTrackId(null); };
    document.addEventListener('pointerdown', outside);
    window.addEventListener('scroll', scroll, true);
    window.addEventListener('keydown', back);
    return () => {
      document.removeEventListener('pointerdown', outside);
      window.removeEventListener('scroll', scroll, true);
      window.removeEventListener('keydown', back);
    };
  }, [activeMenuTrackId]);

  const audioTracks = useMemo(() => {
    return tracks.filter((t) => t.type === 'audio');
  }, [tracks]);

  const genres = useMemo(() => {
    const list = Array.from(new Set(audioTracks.map((t) => t.genre).filter(Boolean))) as string[];
    return ['all', ...list];
  }, [audioTracks]);

  const filteredTracks = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();
    let result = audioTracks.filter((t) => {
      let matchSearch = true;
      if (q) {
        if (searchScope === 'title') {
          matchSearch = t.title.toLowerCase().includes(q);
        } else if (searchScope === 'artist') {
          matchSearch = t.artist.toLowerCase().includes(q);
        } else if (searchScope === 'album') {
          matchSearch = Boolean(t.album && t.album.toLowerCase().includes(q));
        } else {
          matchSearch =
            t.title.toLowerCase().includes(q) ||
            t.artist.toLowerCase().includes(q) ||
            Boolean(t.album && t.album.toLowerCase().includes(q));
        }
      }

      const matchGenre = selectedGenre === 'all' || t.genre === selectedGenre;
      return matchSearch && matchGenre;
    });

    if (sortBy === 'title') {
      result.sort((a, b) => a.title.localeCompare(b.title));
    } else if (sortBy === 'duration') {
      result.sort((a, b) => b.duration - a.duration);
    } else {
      result.sort((a, b) => b.addedAt - a.addedAt);
    }

    return result;
  }, [audioTracks, searchQuery, searchScope, selectedGenre, sortBy]);

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
      {/* Top Search & Filter Bar */}
      <div className="glass-card rounded-3xl p-3 sm:p-4 border border-purple-500/20 bg-slate-900/60 shadow-xl space-y-2.5 backdrop-blur-2xl">
        <div className="flex flex-col sm:flex-row gap-2.5">
          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-purple-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={
                searchScope === 'title'
                  ? lang === 'bn' ? 'গানের নাম দিয়ে খুঁজুন...' : 'Search by track title...'
                  : searchScope === 'artist'
                  ? lang === 'bn' ? 'শিল্পীর নাম দিয়ে খুঁজুন...' : 'Search by artist name...'
                  : searchScope === 'album'
                  ? lang === 'bn' ? 'অ্যালবাম নাম দিয়ে খুঁজুন...' : 'Search by album name...'
                  : lang === 'bn' ? 'গান, শিল্পী বা অ্যালবাম খুঁজুন...' : 'Search title, artist, or album...'
              }
              className="w-full pl-10 pr-9 py-2.5 rounded-2xl bg-white/5 border border-white/10 text-white placeholder-slate-400 text-sm focus:outline-none focus:border-purple-400/50 transition backdrop-blur-xl"
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

          {/* Sort & Quick Actions */}
          <div className="flex items-center gap-2">
            <div className="relative flex items-center glass-pill rounded-2xl px-3 py-2 text-xs text-slate-300">
              <SlidersHorizontal className="w-3.5 h-3.5 mr-1.5 text-purple-300" />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                aria-label={lang === 'bn' ? 'বাছাই করুন' : 'Sort by'}
                className="bg-transparent text-xs text-white focus:outline-none cursor-pointer"
              >
                <option value="recent" className="bg-slate-900 text-white">
                  {lang === 'bn' ? 'সর্বশেষ যোগ' : 'Recently Added'}
                </option>
                <option value="title" className="bg-slate-900 text-white">
                  {lang === 'bn' ? 'নাম অনুসারে' : 'Title (A-Z)'}
                </option>
                <option value="duration" className="bg-slate-900 text-white">
                  {lang === 'bn' ? 'দৈর্ঘ্য' : 'Duration'}
                </option>
              </select>
            </div>

            <button
              onClick={onOpenImport}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-2xl bg-purple-600/30 hover:bg-purple-600/40 border border-purple-400/30 text-xs font-semibold text-purple-200 transition active:scale-95"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>{lang === 'bn' ? 'অডিও যোগ' : 'Add Audio'}</span>
            </button>
          </div>
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
                { id: 'title', labelBn: 'গানের নাম', labelEn: 'Title' },
                { id: 'artist', labelBn: 'শিল্পী', labelEn: 'Artist' },
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
                      ? 'bg-purple-600 text-white shadow-md border border-purple-400/40'
                      : 'bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white border border-white/5'
                  }`}
                >
                  {lang === 'bn' ? scope.labelBn : scope.labelEn}
                </button>
              );
            })}
          </div>

          {searchQuery && (
            <div className="text-[11px] font-mono text-purple-300 whitespace-nowrap">
              {filteredTracks.length} {lang === 'bn' ? 'টি গান' : 'found'}
            </div>
          )}
        </div>
      </div>

      {/* Genre Pills */}
      {genres.length > 1 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
          {genres.map((g) => (
            <button
              key={g}
              onClick={() => setSelectedGenre(g)}
              className={`px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
                selectedGenre === g
                  ? 'bg-purple-500 text-white shadow-md shadow-purple-500/30 border border-purple-400'
                  : 'glass-pill text-slate-300 hover:text-white hover:bg-white/10'
              }`}
            >
              {g === 'all'
                ? lang === 'bn'
                  ? 'সবগুলো গান'
                  : 'All Songs'
                : g}
            </button>
          ))}
        </div>
      )}

      {/* Track Stats Bar */}
      <div className="flex items-center justify-between text-xs text-slate-400 px-1">
        <span>
          {filteredTracks.length} {lang === 'bn' ? 'টি অফলাইন গান' : 'Offline Tracks'}
        </span>

      </div>

      {/* Track List */}
      <div className="space-y-2">
        {filteredTracks.length === 0 ? (
          <div className="glass-card rounded-3xl p-8 text-center space-y-3 border border-white/10">
            <Volume2 className="w-12 h-12 mx-auto text-purple-400/60" />
            <h3 className="text-base font-semibold text-white">
              {lang === 'bn' ? 'কোন গান পাওয়া যায়নি' : 'No tracks found'}
            </h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              {lang === 'bn'
                ? 'আপনার ডিভাইসের গান যোগ করতে ওপরের "অডিও যোগ" বাটনে চাপুন অথবা সার্চ ফিল্টার পরিবর্তন করুন।'
                : 'Import songs from your device storage by clicking "Add Audio" or change your search filter.'}
            </p>
            <button
              onClick={onOpenImport}
              className="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 text-white text-xs font-semibold shadow-lg shadow-purple-600/30"
            >
              <Plus className="w-4 h-4" />
              {lang === 'bn' ? 'ডিভাইস থেকে গান আনুন' : 'Import from Device'}
            </button>
          </div>
        ) : (
          filteredTracks.map((track, idx) => {
            const isThisTrackActive = currentTrack?.id === track.id;
            return (
              <div
                key={track.id}
                onClick={() => {
                  if (isThisTrackActive) {
                    onTogglePlay();
                  } else {
                    onPlayTrack(track);
                  }
                }}
                className={`group relative glass-card glass-card-hover rounded-2xl p-3 flex items-center gap-3.5 cursor-pointer border transition-all ${activeMenuTrackId === track.id ? 'z-[90]' : 'z-0'} ${
                  isThisTrackActive
                    ? 'border-purple-400/50 bg-purple-950/30 shadow-lg shadow-purple-900/30'
                    : 'border-white/10 hover:border-white/20'
                }`}
              >
                {/* Track Index or Play State */}
                <div className="relative w-12 h-12 rounded-xl overflow-hidden flex-shrink-0 bg-slate-800 shadow-md">
                  <img
                    src={track.coverUrl}
                    alt={track.title}
                    className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                  {/* Play icon overlay */}
                  <div
                    className={`absolute inset-0 bg-black/40 flex items-center justify-center transition-opacity ${
                      isThisTrackActive ? 'opacity-100 bg-purple-900/40' : 'opacity-0 group-hover:opacity-100'
                    }`}
                  >
                    {isThisTrackActive && isPlaying ? (
                      <div className="flex items-end justify-center gap-0.5 h-4">
                        <span className="w-1 bg-purple-300 rounded-full animate-wave-bar" style={{ animationDelay: '0ms' }} />
                        <span className="w-1 bg-purple-300 rounded-full animate-wave-bar" style={{ animationDelay: '200ms' }} />
                        <span className="w-1 bg-purple-300 rounded-full animate-wave-bar" style={{ animationDelay: '400ms' }} />
                      </div>
                    ) : (
                      <Play className="w-5 h-5 fill-white text-white ml-0.5" />
                    )}
                  </div>
                </div>

                {/* Track Info */}
                <div className="flex-1 min-w-0">
                  <h4
                    className={`text-sm font-semibold truncate transition-colors ${
                      isThisTrackActive ? 'text-purple-300 font-bold' : 'text-white'
                    }`}
                  >
                    {track.title}
                  </h4>
                  <div className="flex items-center gap-2 mt-0.5 text-xs text-slate-400">
                    <span className="truncate">{track.artist}</span>
                    <span>•</span>
                    <span className="text-[10px] px-1.5 py-0.2 rounded bg-white/5 border border-white/10 text-slate-300">
                      {track.format || 'AUDIO'}
                    </span>
                  </div>
                </div>

                {/* Duration & Actions */}
                <div
                  className="flex items-center gap-2 flex-shrink-0"
                  onClick={(e) => e.stopPropagation()}
                >
                  <span className="text-xs text-slate-400 flex items-center gap-1 font-mono">
                    <Clock className="w-3 h-3 text-slate-500" />
                    {formatDuration(track.duration)}
                  </span>

                  {/* Favorite */}
                  <button
                    onClick={() => onToggleFavorite(track.id)}
                    className="p-2 text-slate-400 hover:text-red-400 transition active:scale-90"
                    title={track.isFavorite ? 'Remove Favorite' : 'Add Favorite'}
                  >
                    <Heart
                      className={`w-4 h-4 ${
                        track.isFavorite ? 'fill-red-500 text-red-500' : ''
                      }`}
                    />
                  </button>

                  {/* Context Menu Toggle */}
                  <div className="relative" data-audio-menu>
                    <button
                      onClick={() =>
                        setActiveMenuTrackId(
                          activeMenuTrackId === track.id ? null : track.id
                        )
                      }
                      className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-white/10 transition"
                      title="More Options"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>

                    {/* Dropdown Menu */}
                    {activeMenuTrackId === track.id && (
                      <div className="absolute right-0 top-8 w-48 max-h-72 overflow-y-auto rounded-2xl glass-panel p-2 shadow-2xl z-[70] border border-white/20 space-y-1">
                        <button
                          onClick={() => {
                            onAddToQueue(track);
                            setActiveMenuTrackId(null);
                          }}
                          className="w-full text-left px-3 py-1.5 rounded-xl text-xs text-slate-200 hover:bg-white/10 flex items-center gap-2 transition"
                        >
                          <Plus className="w-3.5 h-3.5 text-purple-400" />
                          {lang === 'bn' ? 'পরের গানে যোগ করুন' : 'Add to Queue'}
                        </button>

                        {playlists.length > 0 && (
                          <div className="pt-1 border-t border-white/10">
                            <div className="px-3 py-1 text-[10px] text-slate-400 font-semibold uppercase">
                              {lang === 'bn' ? 'প্লেলিস্টে যোগ করুন' : 'Add to Playlist'}
                            </div>
                            {playlists.map((pl) => (
                              <button
                                key={pl.id}
                                onClick={() => {
                                  onAddTrackToPlaylist(pl.id, track.id);
                                  setActiveMenuTrackId(null);
                                }}
                                className="w-full text-left px-3 py-1.5 rounded-lg text-xs text-slate-300 hover:bg-white/10 truncate transition"
                              >
                                + {pl.name}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
