import { Track, Playlist } from '../types/media';

const DB_NAME = 'AuraGlassMediaPlayerDB';
const DB_VERSION = 1;
const TRACKS_STORE = 'tracks';
const PLAYLISTS_STORE = 'playlists';
const SETTINGS_STORE = 'settings';

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(TRACKS_STORE)) {
        const trackStore = db.createObjectStore(TRACKS_STORE, { keyPath: 'id' });
        trackStore.createIndex('type', 'type', { unique: false });
        trackStore.createIndex('isFavorite', 'isFavorite', { unique: false });
        trackStore.createIndex('folder', 'folder', { unique: false });
      }
      if (!db.objectStoreNames.contains(PLAYLISTS_STORE)) {
        db.createObjectStore(PLAYLISTS_STORE, { keyPath: 'id' });
      }
      if (!db.objectStoreNames.contains(SETTINGS_STORE)) {
        db.createObjectStore(SETTINGS_STORE, { keyPath: 'key' });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveTrack(track: Track): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(TRACKS_STORE, 'readwrite');
    const store = tx.objectStore(TRACKS_STORE);
    store.put(track);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function saveMultipleTracks(tracks: Track[]): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(TRACKS_STORE, 'readwrite');
    const store = tx.objectStore(TRACKS_STORE);
    tracks.forEach((track) => store.put(track));
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function getAllTracks(): Promise<Track[]> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(TRACKS_STORE, 'readonly');
    const store = tx.objectStore(TRACKS_STORE);
    const req = store.getAll();
    req.onsuccess = () => {
      const tracks: Track[] = req.result || [];
      // Re-create Blob URLs for persisted tracks if blob exists
      const hydrated = tracks.map((t) => {
        if (t.blob && (!t.url || t.url.startsWith('blob:'))) {
          try {
            t.url = URL.createObjectURL(t.blob);
          } catch {
            // Keep existing url if cannot revoke/create
          }
        }
        return t;
      });
      resolve(hydrated);
    };
    req.onerror = () => reject(req.error);
  });
}

export async function deleteTrack(trackId: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(TRACKS_STORE, 'readwrite');
    const store = tx.objectStore(TRACKS_STORE);
    store.delete(trackId);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function getAllPlaylists(): Promise<Playlist[]> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(PLAYLISTS_STORE, 'readonly');
    const store = tx.objectStore(PLAYLISTS_STORE);
    const req = store.getAll();
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

export async function savePlaylist(playlist: Playlist): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(PLAYLISTS_STORE, 'readwrite');
    const store = tx.objectStore(PLAYLISTS_STORE);
    store.put(playlist);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function deletePlaylist(playlistId: string): Promise<void> {
  const db = await openDB();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(PLAYLISTS_STORE, 'readwrite');
    const store = tx.objectStore(PLAYLISTS_STORE);
    store.delete(playlistId);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function getSetting<T>(key: string, defaultValue: T): Promise<T> {
  try {
    const db = await openDB();
    return new Promise((resolve) => {
      const tx = db.transaction(SETTINGS_STORE, 'readonly');
      const store = tx.objectStore(SETTINGS_STORE);
      const req = store.get(key);
      req.onsuccess = () => {
        if (req.result && req.result.value !== undefined) {
          resolve(req.result.value);
        } else {
          resolve(defaultValue);
        }
      };
      req.onerror = () => resolve(defaultValue);
    });
  } catch {
    return defaultValue;
  }
}

export async function saveSetting<T>(key: string, value: T): Promise<void> {
  try {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(SETTINGS_STORE, 'readwrite');
      const store = tx.objectStore(SETTINGS_STORE);
      store.put({ key, value });
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch {
    // ignore
  }
}
