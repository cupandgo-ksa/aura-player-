import React, { useState } from 'react';
import {
  ListMusic,
  Plus,
  Play,
  Heart,
  Trash2,
  Music,
  Clock,
  Sparkles,
  ChevronRight,
  ArrowLeft,
} from 'lucide-react';
import { Track, Playlist } from '../types/media';

interface PlaylistsTabProps {
  playlists: Playlist[];
  tracks: Track[];
  onPlayPlaylist: (playlist: Playlist) => void;
  onPlayTrack: (track: Track) => void;
  onCreatePlaylistOpen: () => void;
  onDeletePlaylist: (playlistId: string) => void;
  onRemoveTrackFromPlaylist: (playlistId: string, trackId: string) => void;
  onToggleFavorite: (trackId: string) => void;
  lang: 'bn' | 'en';
}

export const PlaylistsTab: React.FC<PlaylistsTabProps> = ({
  playlists,
  tracks,
  onPlayPlaylist,
  onPlayTrack,
  onCreatePlaylistOpen,
  onDeletePlaylist,
  onRemoveTrackFromPlaylist,
  onToggleFavorite,
  lang,
}) => {
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string | null>(null);

  const selectedPlaylist = playlists.find((p) => p.id === selectedPlaylistId);

  const playlistTracks = selectedPlaylist
    ? selectedPlaylist.id === 'playlist-favorites'
      ? tracks.filter((t) => t.isFavorite)
      : selectedPlaylist.trackIds
          .map((id) => tracks.find((t) => t.id === id))
          .filter(Boolean) as Track[]
    : [];

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  // If a playlist is selected, show its detail view
  if (selectedPlaylist) {
    return (
      <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
        {/* Back button & Playlist Header */}
        <button
          onClick={() => setSelectedPlaylistId(null)}
          className="flex items-center gap-1.5 text-xs text-slate-300 hover:text-white glass-pill px-3 py-1.5 rounded-xl transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>{lang === 'bn' ? 'সব প্লেলিস্টে ফিরুন' : 'Back to Playlists'}</span>
        </button>

        {/* Playlist Banner */}
        <div className="glass-card rounded-3xl p-5 border border-white/10 flex flex-col sm:flex-row items-center sm:items-start gap-4">
          <div className="w-28 h-28 rounded-2xl overflow-hidden shadow-2xl flex-shrink-0 bg-slate-800">
            {selectedPlaylist.coverUrl ? (
              <img
                src={selectedPlaylist.coverUrl}
                alt={selectedPlaylist.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-purple-900/40 text-purple-300">
                <ListMusic className="w-10 h-10" />
              </div>
            )}
          </div>

          <div className="flex-1 text-center sm:text-left space-y-2">
            <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-400/30">
              {lang === 'bn' ? 'প্লেলিস্ট' : 'Playlist'}
            </span>
            <h2 className="text-xl sm:text-2xl font-bold text-white">
              {selectedPlaylist.name}
            </h2>
            <p className="text-xs text-slate-300">
              {selectedPlaylist.description || (lang === 'bn' ? 'ব্যক্তিগত সংগ্রহ' : 'Personal Collection')}
            </p>
            <p className="text-xs text-slate-400 font-mono">
              {playlistTracks.length} {lang === 'bn' ? 'টি গান' : 'tracks'}
            </p>

            {/* Play all button */}
            {playlistTracks.length > 0 && (
              <div className="pt-2 flex items-center justify-center sm:justify-start gap-2">
                <button
                  onClick={() => onPlayPlaylist(selectedPlaylist)}
                  className="flex items-center gap-2 px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95"
                >
                  <Play className="w-4 h-4 fill-white" />
                  <span>{lang === 'bn' ? 'সব গান চালান' : 'Play All'}</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Tracks List inside playlist */}
        <div className="space-y-2">
          {playlistTracks.length === 0 ? (
            <div className="glass-card rounded-2xl p-8 text-center space-y-2 border border-white/10">
              <Music className="w-10 h-10 mx-auto text-slate-500" />
              <p className="text-sm text-slate-300">
                {lang === 'bn' ? 'এই প্লেলিস্টে কোন গান নেই' : 'This playlist is empty'}
              </p>
              <p className="text-xs text-slate-500">
                {lang === 'bn'
                  ? 'গান ট্যাব থেকে গানে থ্রি-ডটে চেপে এই প্লেলিস্টে যোগ করুন।'
                  : 'Add songs from the Music tab via the three-dot menu.'}
              </p>
            </div>
          ) : (
            playlistTracks.map((track) => (
              <div
                key={track.id}
                onClick={() => onPlayTrack(track)}
                className="glass-card glass-card-hover rounded-2xl p-2.5 flex items-center gap-3 cursor-pointer border border-white/10 group"
              >
                <div className="w-10 h-10 rounded-xl overflow-hidden bg-slate-800 flex-shrink-0">
                  <img
                    src={track.coverUrl}
                    alt={track.title}
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-semibold text-white truncate">
                    {track.title}
                  </h4>
                  <p className="text-xs text-slate-400 truncate">{track.artist}</p>
                </div>
                <div
                  className="flex items-center gap-2 flex-shrink-0"
                  onClick={(e) => e.stopPropagation()}
                >
                  <span className="text-xs text-slate-400 font-mono">
                    {formatDuration(track.duration)}
                  </span>
                  {!selectedPlaylist.isSystem && (
                    <button
                      onClick={() =>
                        onRemoveTrackFromPlaylist(selectedPlaylist.id, track.id)
                      }
                      className="p-1.5 text-slate-400 hover:text-red-400 transition"
                      title="Remove from playlist"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="pb-36 pt-2 px-4 max-w-4xl mx-auto space-y-4">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <ListMusic className="w-5 h-5 text-purple-400" />
            <span>{lang === 'bn' ? 'প্লেলিস্ট ও প্রিয় গান' : 'Playlists & Favorites'}</span>
          </h2>
          <p className="text-xs text-slate-400">
            {lang === 'bn' ? 'আপনার নিজস্ব মিউজিক ফোল্ডার ও তালিকা' : 'Organize your music collection'}
          </p>
        </div>

        <button
          onClick={onCreatePlaylistOpen}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-2xl bg-purple-600/30 hover:bg-purple-600/40 border border-purple-400/30 text-xs font-semibold text-purple-200 transition active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>{lang === 'bn' ? 'নতুন প্লেলিস্ট' : 'New Playlist'}</span>
        </button>
      </div>

      {/* Playlists Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
        {playlists.map((pl) => {
          const count =
            pl.id === 'playlist-favorites'
              ? tracks.filter((t) => t.isFavorite).length
              : pl.trackIds.length;

          return (
            <div
              key={pl.id}
              onClick={() => setSelectedPlaylistId(pl.id)}
              className="glass-card glass-card-hover rounded-2xl p-4 flex items-center gap-3.5 cursor-pointer border border-white/10 group relative"
            >
              <div className="relative w-16 h-16 rounded-2xl overflow-hidden bg-slate-800 shadow-md flex-shrink-0">
                {pl.coverUrl ? (
                  <img
                    src={pl.coverUrl}
                    alt={pl.name}
                    className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-purple-900/30 text-purple-300">
                    <ListMusic className="w-7 h-7" />
                  </div>
                )}
                {pl.id === 'playlist-favorites' && (
                  <div className="absolute top-1 right-1 p-1 rounded-full bg-red-500/80 text-white">
                    <Heart className="w-3 h-3 fill-white" />
                  </div>
                )}
              </div>

              <div className="flex-1 min-w-0">
                <h4 className="text-sm font-bold text-white truncate group-hover:text-purple-300 transition-colors">
                  {pl.name}
                </h4>
                <p className="text-xs text-slate-400 truncate mt-0.5">
                  {pl.description || (lang === 'bn' ? 'ব্যক্তিগত তালিকা' : 'Personal Playlist')}
                </p>
                <span className="inline-block mt-1 text-[11px] font-mono text-purple-300 font-medium">
                  {count} {lang === 'bn' ? 'টি গান' : 'tracks'}
                </span>
              </div>

              <div
                className="flex items-center gap-1"
                onClick={(e) => e.stopPropagation()}
              >
                {!pl.isSystem && (
                  <button
                    onClick={() => onDeletePlaylist(pl.id)}
                    className="p-2 text-slate-400 hover:text-red-400 transition"
                    title="Delete playlist"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
                <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-white group-hover:translate-x-0.5 transition" />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
