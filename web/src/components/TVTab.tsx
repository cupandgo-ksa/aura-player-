import React, { useState } from 'react';
import {
  Tv,
  Play,
  Cast,
  Plus,
  Radio,
  Sparkles,
  Maximize2,
  Volume2,
  VolumeX,
  X,
  PictureInPicture,
  Layers,
  Check,
} from 'lucide-react';
import { TVChannel } from '../types/media';

interface TVTabProps {
  channels: TVChannel[];
  onAddChannel: (channel: TVChannel) => void;
  lang: 'bn' | 'en';
}

export const TVTab: React.FC<TVTabProps> = ({ channels, onAddChannel, lang }) => {
  const [selectedChannel, setSelectedChannel] = useState<TVChannel | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [isAddChannelOpen, setIsAddChannelOpen] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');
  const [newChannelUrl, setNewChannelUrl] = useState('');
  const [newChannelCat, setNewChannelCat] = useState<'music' | 'nature' | 'entertainment' | 'news'>('music');
  const [castMessage, setCastMessage] = useState<string | null>(null);

  const categories = [
    { id: 'all', labelBn: 'সকল চ্যানেল', labelEn: 'All Channels' },
    { id: 'music', labelBn: 'মিউজিক টিভি', labelEn: 'Music TV' },
    { id: 'nature', labelBn: 'প্রকৃতি ও রিল্যাক্স', labelEn: 'Nature & Relax' },
    { id: 'entertainment', labelBn: 'বিনোদন ও ফিল্ম', labelEn: 'Entertainment' },
  ];

  const filteredChannels = channels.filter((ch) => {
    return selectedCategory === 'all' || ch.category === selectedCategory;
  });

  const handleCastToTV = () => {
    // Web Remote Playback / Cast trigger
    const video = document.getElementById('tv-video-player') as HTMLVideoElement;
    if (video && 'remote' in video) {
      try {
        (video as any).remote.prompt();
        setCastMessage(lang === 'bn' ? 'স্মার্ট টিভি সংযোগ খোলা হচ্ছে...' : 'Opening Smart TV Cast Dialog...');
      } catch (e) {
        setCastMessage(lang === 'bn' ? 'স্মার্ট টিভি একই ওয়াইফাই নেটওয়ার্কে রাখুন' : 'Ensure Smart TV is on same Wi-Fi');
      }
    } else {
      setCastMessage(
        lang === 'bn'
          ? 'টিভি কাস্ট: আপনার ক্রোম বা ব্রাউজারের "Cast" অপশন ব্যবহার করুন'
          : 'TV Cast: Use browser Cast menu to stream to TV'
      );
    }
    setTimeout(() => setCastMessage(null), 3500);
  };

  const handleCreateChannel = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newChannelName.trim() || !newChannelUrl.trim()) return;

    const newCh: TVChannel = {
      id: `custom-tv-${Date.now()}`,
      name: newChannelName.trim(),
      bnName: newChannelName.trim(),
      category: newChannelCat,
      streamUrl: newChannelUrl.trim(),
      logoUrl: selectedChannel?.logoUrl || channels[0]?.logoUrl || '',
      description: 'Custom User Added TV Stream',
      bnDescription: 'ব্যবহারকারীর নিজস্ব যুক্ত করা লাইভ টিভি চ্যানেল',
      resolution: 'HD Live',
      isLive: true,
    };

    onAddChannel(newCh);
    setNewChannelName('');
    setNewChannelUrl('');
    setIsAddChannelOpen(false);
  };

  return (
    <div className="pb-36 pt-2 px-4 max-w-5xl mx-auto space-y-4">
      {/* Top TV Banner */}
      <div className="glass-card rounded-3xl p-5 border border-purple-500/20 bg-gradient-to-r from-purple-950/40 via-indigo-950/30 to-blue-950/30 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-xl">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-500 p-[1.5px] shadow-lg shadow-purple-500/30">
            <div className="w-full h-full rounded-[14px] bg-[#0c101c] flex items-center justify-center">
              <Tv className="w-6 h-6 text-purple-300 animate-pulse" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base sm:text-lg font-bold text-white">
                {lang === 'bn' ? 'লাইভ টিভি ও এইচডি স্ট্রিম' : 'Live TV & HD Channels'}
              </h3>
              <span className="flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-500/20 border border-red-500/40 text-red-300">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-ping" />
                LIVE TV
              </span>
            </div>
            <p className="text-xs text-slate-300 mt-0.5">
              {lang === 'bn'
                ? 'স্মার্ট টিভি কাস্ট, ২৪/৭ মিউজিক টিভি ও রিল্যাক্সিং ব্যাকগ্রাউন্ড চ্যানেল'
                : 'Full screen Smart TV playback, 24/7 music streams & Chromecast support'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-stretch sm:self-auto justify-end">
          <button
            onClick={() => setIsAddChannelOpen(true)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-purple-600/30 hover:bg-purple-600/40 border border-purple-400/40 text-purple-200 text-xs font-semibold transition active:scale-95 whitespace-nowrap"
          >
            <Plus className="w-4 h-4" />
            <span>{lang === 'bn' ? 'নতুন চ্যানেল যোগ' : 'Add Stream URL'}</span>
          </button>
        </div>
      </div>

      {/* Category Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
        {categories.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setSelectedCategory(cat.id)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
              selectedCategory === cat.id
                ? 'bg-purple-500 text-white shadow-md shadow-purple-500/30 border border-purple-400'
                : 'glass-pill text-slate-300 hover:text-white hover:bg-white/10'
            }`}
          >
            {lang === 'bn' ? cat.labelBn : cat.labelEn}
          </button>
        ))}
      </div>

      {/* Channel Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredChannels.map((channel) => (
          <div
            key={channel.id}
            onClick={() => setSelectedChannel(channel)}
            className="glass-card glass-card-hover rounded-2xl overflow-hidden border border-white/10 group cursor-pointer flex flex-col justify-between"
          >
            <div className="relative aspect-video w-full bg-slate-900 overflow-hidden">
              <img
                src={channel.logoUrl}
                alt={channel.name}
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

              {/* LIVE Badge */}
              <span className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded-md bg-red-600/80 backdrop-blur-md text-white text-[10px] font-bold flex items-center gap-1 border border-red-500/40 shadow-sm">
                <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                LIVE
              </span>

              {/* Resolution Badge */}
              <span className="absolute top-2.5 right-2.5 px-2 py-0.5 rounded-md bg-black/60 backdrop-blur-md text-cyan-300 text-[10px] font-mono font-bold border border-white/15">
                {channel.resolution}
              </span>

              {/* Play Overlay */}
              <div className="absolute inset-0 flex items-center justify-center opacity-85 group-hover:opacity-100 transition-opacity">
                <div className="w-12 h-12 rounded-full bg-purple-600/80 backdrop-blur-md flex items-center justify-center text-white shadow-xl shadow-purple-600/40 group-hover:scale-110 transition-transform">
                  <Play className="w-5 h-5 fill-white ml-0.5" />
                </div>
              </div>
            </div>

            <div className="p-3.5 space-y-1.5">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-white truncate group-hover:text-purple-300 transition-colors">
                  {lang === 'bn' ? channel.bnName : channel.name}
                </h4>
                <Tv className="w-4 h-4 text-purple-400 flex-shrink-0" />
              </div>
              <p className="text-xs text-slate-400 line-clamp-2 leading-relaxed">
                {lang === 'bn' ? channel.bnDescription : channel.description}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* TV Player Modal */}
      {selectedChannel && (
        <div className="fixed inset-0 z-50 bg-black/90 flex flex-col justify-between overflow-hidden animate-fade-in backdrop-blur-xl">
          {/* Top TV Controls Bar */}
          <div className="p-4 glass-panel border-x-0 border-t-0 border-b border-white/10 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <button
                onClick={() => setSelectedChannel(null)}
                className="p-2 rounded-2xl glass-card text-white hover:bg-white/20 transition active:scale-95"
              >
                <X className="w-5 h-5" />
              </button>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-sm sm:text-base font-bold text-white">
                    {lang === 'bn' ? selectedChannel.bnName : selectedChannel.name}
                  </h3>
                  <span className="px-1.5 py-0.5 rounded bg-red-600 text-white text-[9px] font-bold">
                    LIVE
                  </span>
                </div>
                <p className="text-xs text-slate-400">
                  {selectedChannel.resolution} • {lang === 'bn' ? 'স্মার্ট টিভি মোড' : 'Smart TV Mode'}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {/* Cast to Smart TV */}
              <button
                onClick={handleCastToTV}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/30 hover:bg-purple-600/50 border border-purple-400/40 text-purple-200 text-xs font-semibold transition active:scale-95"
                title={lang === 'bn' ? 'টিভিতে কাস্ট করুন' : 'Cast to Smart TV'}
              >
                <Cast className="w-4 h-4" />
                <span className="hidden sm:inline">
                  {lang === 'bn' ? 'টিভিতে কাস্ট' : 'Cast to TV'}
                </span>
              </button>

              {/* PiP */}
              <button
                onClick={async () => {
                  const v = document.getElementById('tv-video-player') as HTMLVideoElement;
                  if (v && document.pictureInPictureEnabled) {
                    if (document.pictureInPictureElement) {
                      await document.exitPictureInPicture();
                    } else {
                      await v.requestPictureInPicture();
                    }
                  }
                }}
                className="p-2 rounded-xl glass-card text-white hover:bg-white/20 transition"
                title="Picture in Picture"
              >
                <PictureInPicture className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Cast status toast */}
          {castMessage && (
            <div className="absolute top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-2xl glass-panel bg-purple-900/90 text-white text-xs font-bold border border-purple-400 shadow-2xl animate-bounce">
              {castMessage}
            </div>
          )}

          {/* Video Player Viewport */}
          <div className="relative flex-1 flex items-center justify-center p-2 sm:p-6 bg-black">
            <video
              id="tv-video-player"
              src={selectedChannel.streamUrl}
              autoPlay
              controls
              playsInline
              loop
              className="w-full max-h-[75vh] object-contain rounded-2xl shadow-2xl border border-white/10"
            />
          </div>

          {/* Bottom Channel Switcher Bar */}
          <div className="p-3 glass-panel border-x-0 border-b-0 border-t border-white/10">
            <div className="max-w-4xl mx-auto flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider whitespace-nowrap pl-1 pr-2">
                {lang === 'bn' ? 'অন্য চ্যানেল:' : 'Channels:'}
              </span>
              {channels.map((ch) => (
                <button
                  key={ch.id}
                  onClick={() => setSelectedChannel(ch)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition flex items-center gap-1.5 ${
                    ch.id === selectedChannel.id
                      ? 'bg-purple-600 text-white border border-purple-400 shadow-md'
                      : 'glass-card text-slate-300 hover:text-white'
                  }`}
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-red-400" />
                  <span>{lang === 'bn' ? ch.bnName : ch.name}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Add Custom Stream URL Modal */}
      {isAddChannelOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xl animate-fade-in">
          <div className="w-full max-w-sm rounded-3xl glass-panel p-6 border border-white/20 shadow-2xl space-y-4 bg-slate-900/90">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-2xl bg-purple-500/20 border border-purple-500/40 flex items-center justify-center text-purple-300">
                  <Tv className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-white">
                    {lang === 'bn' ? 'কাস্টম টিভি চ্যানেল' : 'Add Custom Stream'}
                  </h3>
                  <p className="text-xs text-slate-400">
                    {lang === 'bn' ? 'IPTV বা লাইভ স্ট্রিম লিংক যুক্ত করুন' : 'Add M3U8, MP4 or Live TV stream'}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setIsAddChannelOpen(false)}
                className="p-1.5 rounded-xl glass-card text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateChannel} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  {lang === 'bn' ? 'চ্যানেলের নাম *' : 'Channel Name *'}
                </label>
                <input
                  type="text"
                  required
                  value={newChannelName}
                  onChange={(e) => setNewChannelName(e.target.value)}
                  placeholder={lang === 'bn' ? 'যেমন: প্রিয় মিউজিক টিভি' : 'e.g. My Favorite News TV'}
                  className="w-full px-3.5 py-2.5 rounded-xl glass-card border border-white/10 text-white text-sm focus:outline-none focus:border-purple-400/50"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  {lang === 'bn' ? 'স্ট্রিম URL (ভিডিও লিংক) *' : 'Stream URL (HTTPS) *'}
                </label>
                <input
                  type="url"
                  required
                  value={newChannelUrl}
                  onChange={(e) => setNewChannelUrl(e.target.value)}
                  placeholder="https://.../stream.mp4 or .m3u8"
                  className="w-full px-3.5 py-2.5 rounded-xl glass-card border border-white/10 text-white text-sm focus:outline-none focus:border-purple-400/50 font-mono text-xs"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  {lang === 'bn' ? 'ক্যাটাগরি' : 'Category'}
                </label>
                <select
                  value={newChannelCat}
                  onChange={(e) => setNewChannelCat(e.target.value as any)}
                  aria-label={lang === 'bn' ? 'ক্যাটাগরি' : 'Category'}
                  className="w-full px-3.5 py-2.5 rounded-xl glass-card border border-white/10 bg-slate-900 text-white text-xs focus:outline-none cursor-pointer"
                >
                  <option value="music">{lang === 'bn' ? 'মিউজিক টিভি' : 'Music TV'}</option>
                  <option value="nature">{lang === 'bn' ? 'প্রকৃতি ও রিল্যাক্স' : 'Nature'}</option>
                  <option value="entertainment">{lang === 'bn' ? 'বিনোদন ও ফিল্ম' : 'Entertainment'}</option>
                  <option value="news">{lang === 'bn' ? 'খবর ও ইনফো' : 'News'}</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsAddChannelOpen(false)}
                  className="px-4 py-2 rounded-xl glass-pill text-xs font-semibold text-slate-300 hover:text-white"
                >
                  {lang === 'bn' ? 'বাতিল' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={!newChannelName.trim() || !newChannelUrl.trim()}
                  className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95"
                >
                  {lang === 'bn' ? 'যোগ করুন' : 'Add Channel'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
