import React, { useEffect, useState, useCallback, useRef } from 'react';
import { Track, Playlist, MainTab, RepeatMode } from './types/media';
import { AudioEngine } from './services/audioEngine';
import { phoneMedia } from './services/native';
import {
  getAllTracks,
  saveTrack,
  saveMultipleTracks,
  getAllPlaylists,
  savePlaylist,
  deletePlaylist as dbDeletePlaylist,
  getSetting,
  saveSetting,
} from './services/db';
import { getInitialTracks, getInitialPlaylists } from './data/defaultTracks';

import { GlassBackground } from './components/GlassBackground';
import { GlassHeader } from './components/GlassHeader';
import { GlassNavbar } from './components/GlassNavbar';
import { MiniPlayerBar } from './components/MiniPlayerBar';
import { AudioTab } from './components/AudioTab';
import { PlaylistsTab } from './components/PlaylistsTab';
import { FoldersTab } from './components/FoldersTab';
import { EqualizerTab } from './components/EqualizerTab';
import { NowPlayingModal } from './components/NowPlayingModal';
import { SleepTimerModal } from './components/SleepTimerModal';
import { CreatePlaylistModal } from './components/CreatePlaylistModal';
import { ImportMediaModal } from './components/ImportMediaModal';

export const App: React.FC = () => {
  const audioEngine = AudioEngine.getInstance();

  // Core State
  const [tracks, setTracks] = useState<Track[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [currentTab, setCurrentTab] = useState<MainTab>('music');
  const [lang, setLang] = useState<'bn' | 'en'>('bn');

  // Player State
  const [currentTrack, setCurrentTrack] = useState<Track | null>(null);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [duration, setDuration] = useState<number>(0);
  const [volume, setVolume] = useState<number>(0.85);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [playbackRate, setPlaybackRate] = useState<number>(1.0);
  const [isShuffle, setIsShuffle] = useState<boolean>(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>('all');
  const [queue, setQueue] = useState<Track[]>([]);

  // Modals
  const [isNowPlayingOpen, setIsNowPlayingOpen] = useState(false);
  const [isSleepTimerOpen, setIsSleepTimerOpen] = useState(false);
  const [isCreatePlaylistOpen, setIsCreatePlaylistOpen] = useState(false);
  const [isImportMediaOpen, setIsImportMediaOpen] = useState(false);

  // Sleep Timer
  const [sleepTimerRemaining, setSleepTimerRemaining] = useState<number>(0);
  const sleepTimerRef = useRef<any>(null);

  // Refs for current values inside callbacks
  const currentTrackRef = useRef(currentTrack);
  currentTrackRef.current = currentTrack;
  const queueRef = useRef(queue);
  queueRef.current = queue;
  const repeatModeRef = useRef(repeatMode);
  repeatModeRef.current = repeatMode;
  const isShuffleRef = useRef(isShuffle);
  isShuffleRef.current = isShuffle;

  // Initialize data on mount
  useEffect(() => {
    async function initData() {
      // 1. Load language preference
      const savedLang = await getSetting<'bn' | 'en'>('lang', 'bn');
      setLang(savedLang);

      // 2. Load stored tracks or seed initial tracks
      const storedTracks = await getAllTracks();
      if (window.AuraNative) {
        const found = phoneMedia().filter((item) => item.type === 'audio').map((item) => ({ ...item,
          isFavorite: storedTracks.find((old) => old.id === item.id)?.isFavorite || false }));
        setTracks(found);
        setQueue(found.filter((t) => t.type === 'audio'));
        const first = found.find((t) => t.type === 'audio');
        if (first) { setCurrentTrack(first); audioEngine.setTrack(first, false); }
      } else if (storedTracks.length === 0) {
        const combined = getInitialTracks();
        await saveMultipleTracks(combined);
        setTracks(combined);
        setQueue(combined.filter((t) => t.type === 'audio'));
        if (combined.length > 0) {
          setCurrentTrack(combined[0]);
          audioEngine.setTrack(combined[0], false);
        }
      } else {
        const audioList = storedTracks.filter((t) => t.type === 'audio');
        setTracks(audioList);
        setQueue(audioList);
        if (audioList.length > 0) {
          setCurrentTrack(audioList[0]);
          audioEngine.setTrack(audioList[0], false);
        }
      }

      // 3. Load playlists or seed initial playlists
      const storedPlaylists = await getAllPlaylists();
      if (storedPlaylists.length === 0) {
        const initPlaylists = window.AuraNative
          ? [{ id: 'playlist-favorites', name: 'প্রিয় গান (Favorites)', trackIds: [], createdAt: Date.now(), isSystem: true }]
          : getInitialPlaylists();
        for (const pl of initPlaylists) {
          await savePlaylist(pl);
        }
        setPlaylists(initPlaylists);
      } else {
        setPlaylists(storedPlaylists);
      }
    }

    initData();
  }, []);

  useEffect(() => {
    if (!window.AuraNative) return;
    const refresh = () => {
      const found = phoneMedia().filter((item) => item.type === 'audio');
      setTracks((previous) => {
        const updated = found.map((item) => ({ ...item,
          isFavorite: previous.find((old) => old.id === item.id)?.isFavorite || false }));
        setQueue(updated.filter((t) => t.type === 'audio'));
        return updated;
      });
    };
    window.addEventListener('auraLibraryChanged', refresh);
    return () => window.removeEventListener('auraLibraryChanged', refresh);
  }, []);

  useEffect(() => {
    if (!window.AuraNative) return;
    const changed = (event: Event) => {
      const track = tracks.find((item) => item.url === (event as CustomEvent<string>).detail);
      if (track) { setCurrentTrack(track); audioEngine.setTrack(track, false); }
    };
    window.addEventListener('auraNativeTrack', changed);
    return () => window.removeEventListener('auraNativeTrack', changed);
  }, [tracks]);

  useEffect(() => {
    if (!window.AuraNative) return;
    window.AuraNative.setQueue(JSON.stringify(queue.map((t) => ({ uri: t.url, title: t.title, artist: t.artist }))));
  }, [queue]);

  useEffect(() => {
    window.AuraNative?.mode(isShuffle, repeatMode);
  }, [isShuffle, repeatMode]);

  useEffect(() => {
    const closeOnBack = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      setIsNowPlayingOpen(false);
      setIsSleepTimerOpen(false);
      setIsCreatePlaylistOpen(false);
      setIsImportMediaOpen(false);
    };
    window.addEventListener('keydown', closeOnBack);
    return () => window.removeEventListener('keydown', closeOnBack);
  }, []);

  // Set up Audio Engine callbacks & MediaSession
  useEffect(() => {
    audioEngine.setCallbacks(
      () => {
        // Track ended handler
        handleTrackEnded();
      },
      (time, dur) => {
        setCurrentTime(time);
        setDuration(dur);
      },
      (playing) => {
        setIsPlaying(playing);
      }
    );

    audioEngine.setupMediaSessionHandlers({
      onPlay: () => handleTogglePlay(),
      onPause: () => handleTogglePlay(),
      onPrevious: () => handlePreviousTrack(),
      onNext: () => handleNextTrack(),
      onSeek: (seconds) => audioEngine.seek(seconds),
    });
  }, []);

  // Sleep Timer Interval countdown
  useEffect(() => {
    if (sleepTimerRemaining > 0) {
      sleepTimerRef.current = setInterval(() => {
        setSleepTimerRemaining((prev) => {
          if (prev <= 1) {
            audioEngine.pause();
            setIsPlaying(false);
            if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
    }

    return () => {
      if (sleepTimerRef.current) clearInterval(sleepTimerRef.current);
    };
  }, [sleepTimerRemaining]);

  // Audio Playback Actions
  const handlePlayTrack = useCallback((track: Track) => {
    setCurrentTrack(track);
    audioEngine.setTrack(track, true);
    setIsPlaying(true);
  }, []);

  const handleTogglePlay = useCallback(() => {
    if (!currentTrack) {
      if (queue.length > 0) {
        handlePlayTrack(queue[0]);
      }
      return;
    }
    audioEngine.togglePlay();
  }, [currentTrack, queue, handlePlayTrack]);

  const handleNextTrack = useCallback(() => {
    const cur = currentTrackRef.current;
    const q = queueRef.current;
    if (q.length === 0) return;

    if (isShuffleRef.current) {
      const randomIndex = Math.floor(Math.random() * q.length);
      handlePlayTrack(q[randomIndex]);
      return;
    }

    const currentIndex = q.findIndex((t) => t.id === cur?.id);
    const nextIndex = (currentIndex + 1) % q.length;
    handlePlayTrack(q[nextIndex]);
  }, [handlePlayTrack]);

  const handlePreviousTrack = useCallback(() => {
    const cur = currentTrackRef.current;
    const q = queueRef.current;
    if (q.length === 0) return;

    // If more than 3 seconds in, restart track
    if (currentTime > 3) {
      audioEngine.seek(0);
      return;
    }

    const currentIndex = q.findIndex((t) => t.id === cur?.id);
    const prevIndex = (currentIndex - 1 + q.length) % q.length;
    handlePlayTrack(q[prevIndex]);
  }, [currentTime, handlePlayTrack]);

  const handleTrackEnded = useCallback(() => {
    const mode = repeatModeRef.current;
    if (mode === 'one') {
      audioEngine.seek(0);
      audioEngine.play();
    } else if (mode === 'all') {
      handleNextTrack();
    } else {
      // Off
      const cur = currentTrackRef.current;
      const q = queueRef.current;
      const currentIndex = q.findIndex((t) => t.id === cur?.id);
      if (currentIndex < q.length - 1) {
        handleNextTrack();
      } else {
        setIsPlaying(false);
      }
    }
  }, [handleNextTrack]);

  const handleSeek = (seconds: number) => {
    audioEngine.seek(seconds);
    setCurrentTime(seconds);
  };

  const handleSetVolume = (vol: number) => {
    setVolume(vol);
    setIsMuted(vol === 0);
    audioEngine.setVolume(vol);
  };

  const handleToggleMute = () => {
    if (isMuted) {
      setIsMuted(false);
      audioEngine.setVolume(volume || 0.8);
    } else {
      setIsMuted(true);
      audioEngine.setVolume(0);
    }
  };

  const handleSetPlaybackRate = (rate: number) => {
    setPlaybackRate(rate);
    audioEngine.setPlaybackRate(rate);
  };

  const handleToggleShuffle = () => {
    setIsShuffle((prev) => !prev);
  };

  const handleCycleRepeatMode = () => {
    const modes: RepeatMode[] = ['all', 'one', 'off'];
    const next = modes[(modes.indexOf(repeatMode) + 1) % modes.length];
    setRepeatMode(next);
  };

  // Favorite toggle
  const handleToggleFavorite = async (trackId: string) => {
    const updated = tracks.map((t) => {
      if (t.id === trackId) {
        const nextFav = !t.isFavorite;
        const updatedTrack = { ...t, isFavorite: nextFav };
        saveTrack(updatedTrack);
        return updatedTrack;
      }
      return t;
    });
    setTracks(updated);

    if (currentTrack && currentTrack.id === trackId) {
      setCurrentTrack((prev) => prev ? { ...prev, isFavorite: !prev.isFavorite } : null);
    }
  };

  // Playlist Actions
  const handlePlayPlaylist = (playlist: Playlist) => {
    const plTracks =
      playlist.id === 'playlist-favorites'
        ? tracks.filter((t) => t.isFavorite)
        : playlist.trackIds
            .map((id) => tracks.find((t) => t.id === id))
            .filter(Boolean) as Track[];

    if (plTracks.length > 0) {
      setQueue(plTracks);
      window.AuraNative?.setQueue(JSON.stringify(plTracks.map((t) => ({ uri: t.url, title: t.title, artist: t.artist }))));
      handlePlayTrack(plTracks[0]);
    }
  };

  const handleCreatePlaylist = async (name: string, description: string) => {
    const newPlaylist: Playlist = {
      id: `playlist-${Date.now()}`,
      name,
      description,
      trackIds: [],
      createdAt: Date.now(),
      isSystem: false,
    };
    await savePlaylist(newPlaylist);
    setPlaylists((prev) => [...prev, newPlaylist]);
  };

  const handleDeletePlaylist = async (playlistId: string) => {
    await dbDeletePlaylist(playlistId);
    setPlaylists((prev) => prev.filter((p) => p.id !== playlistId));
  };

  const handleAddTrackToPlaylist = async (playlistId: string, trackId: string) => {
    const updated = playlists.map((p) => {
      if (p.id === playlistId && !p.trackIds.includes(trackId)) {
        const updatedPl = { ...p, trackIds: [...p.trackIds, trackId] };
        savePlaylist(updatedPl);
        return updatedPl;
      }
      return p;
    });
    setPlaylists(updated);
  };

  const handleRemoveTrackFromPlaylist = async (playlistId: string, trackId: string) => {
    const updated = playlists.map((p) => {
      if (p.id === playlistId) {
        const updatedPl = {
          ...p,
          trackIds: p.trackIds.filter((id) => id !== trackId),
        };
        savePlaylist(updatedPl);
        return updatedPl;
      }
      return p;
    });
    setPlaylists(updated);
  };

  const handleAddToQueue = (track: Track) => {
    setQueue((prev) => [...prev, track]);
  };

  // Import local media
  const handleImportTracks = async (newTracks: Track[]) => {
    newTracks = newTracks.filter((t) => t.type === 'audio');
    await saveMultipleTracks(newTracks);
    setTracks((prev) => [...prev, ...newTracks]);
    const newAudio = newTracks.filter((t) => t.type === 'audio');
    if (newAudio.length > 0) {
      setQueue((prev) => [...prev, ...newAudio]);
    }
  };

  const openImport = () => {
    if (window.AuraNative) window.AuraNative.pickMedia();
    else setIsImportMediaOpen(true);
  };

  // Toggle Language
  const handleToggleLang = () => {
    const nextLang = lang === 'bn' ? 'en' : 'bn';
    setLang(nextLang);
    saveSetting('lang', nextLang);
  };

  return (
    <div className="relative min-h-screen text-slate-100 flex flex-col justify-between selection:bg-purple-500/30 overflow-x-hidden">
      {/* Background Soft Glass Glow and Aurora Orbs */}
      <GlassBackground />

      {/* Top Glass Header */}
      <GlassHeader
        lang={lang}
        onToggleLang={handleToggleLang}
        onOpenSleepTimer={() => setIsSleepTimerOpen(true)}
        sleepTimerRemaining={sleepTimerRemaining}
        onOpenImport={openImport}
      />

      {/* Main Tab Content based on cleanly separated selection */}
      <main className="flex-1 w-full relative z-10">
        {currentTab === 'music' && (
          <AudioTab
            tracks={tracks}
            currentTrack={currentTrack}
            isPlaying={isPlaying}
            onPlayTrack={handlePlayTrack}
            onTogglePlay={handleTogglePlay}
            onToggleFavorite={handleToggleFavorite}
            onAddToQueue={handleAddToQueue}
            onOpenImport={openImport}
            playlists={playlists}
            onAddTrackToPlaylist={handleAddTrackToPlaylist}
            lang={lang}
          />
        )}

        {currentTab === 'playlists' && (
          <PlaylistsTab
            playlists={playlists}
            tracks={tracks}
            onPlayPlaylist={handlePlayPlaylist}
            onPlayTrack={handlePlayTrack}
            onCreatePlaylistOpen={() => setIsCreatePlaylistOpen(true)}
            onDeletePlaylist={handleDeletePlaylist}
            onRemoveTrackFromPlaylist={handleRemoveTrackFromPlaylist}
            onToggleFavorite={handleToggleFavorite}
            lang={lang}
          />
        )}

        {currentTab === 'folders' && (
          <FoldersTab
            tracks={tracks}
            onPlayTrack={handlePlayTrack}
            onOpenImport={openImport}
            lang={lang}
          />
        )}

        {currentTab === 'equalizer' && (
          <EqualizerTab isPlaying={isPlaying} lang={lang} />
        )}
      </main>

      {/* Floating Mini Player Bar above Navigation */}
      <MiniPlayerBar
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        currentTime={currentTime}
        duration={duration}
        onTogglePlay={handleTogglePlay}
        onNextTrack={handleNextTrack}
        onToggleFavorite={handleToggleFavorite}
        onOpenNowPlaying={() => setIsNowPlayingOpen(true)}
      />

      {/* Bottom Separated Glass Navbar */}
      <GlassNavbar
        currentTab={currentTab}
        onSelectTab={(tab) => setCurrentTab(tab)}
        lang={lang}
      />

      {/* Full Screen Soft Glass Now Playing Modal */}
      <NowPlayingModal
        isOpen={isNowPlayingOpen}
        onClose={() => setIsNowPlayingOpen(false)}
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        currentTime={currentTime}
        duration={duration}
        volume={volume}
        isMuted={isMuted}
        playbackRate={playbackRate}
        isShuffle={isShuffle}
        repeatMode={repeatMode}
        queue={queue}
        onTogglePlay={handleTogglePlay}
        onSeek={handleSeek}
        onPreviousTrack={handlePreviousTrack}
        onNextTrack={handleNextTrack}
        onToggleFavorite={handleToggleFavorite}
        onSetVolume={handleSetVolume}
        onToggleMute={handleToggleMute}
        onSetPlaybackRate={handleSetPlaybackRate}
        onToggleShuffle={handleToggleShuffle}
        onCycleRepeatMode={handleCycleRepeatMode}
        onOpenEqualizer={() => {
          setIsNowPlayingOpen(false);
          setCurrentTab('equalizer');
        }}
        onOpenSleepTimer={() => setIsSleepTimerOpen(true)}
        lang={lang}
      />

      {/* Sleep Timer Modal */}
      <SleepTimerModal
        isOpen={isSleepTimerOpen}
        onClose={() => setIsSleepTimerOpen(false)}
        remainingSeconds={sleepTimerRemaining}
        onSetTimer={(mins) => setSleepTimerRemaining(mins * 60)}
        onCancelTimer={() => setSleepTimerRemaining(0)}
        lang={lang}
      />

      {/* Create Playlist Modal */}
      <CreatePlaylistModal
        isOpen={isCreatePlaylistOpen}
        onClose={() => setIsCreatePlaylistOpen(false)}
        onCreatePlaylist={handleCreatePlaylist}
        lang={lang}
      />

      {/* Import Local Media Modal */}
      <ImportMediaModal
        isOpen={isImportMediaOpen}
        onClose={() => setIsImportMediaOpen(false)}
        onImportTracks={handleImportTracks}
        lang={lang}
      />

    </div>
  );
};
