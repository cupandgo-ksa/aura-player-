import React, { useState } from 'react';
import { RotateCcw, Sliders } from 'lucide-react';
import { AudioEngine, EQ_PRESETS, EQ_FREQUENCIES } from '../services/audioEngine';
import { EqualizerPreset } from '../types/media';

export const EqualizerTab: React.FC<{ isPlaying: boolean; lang: 'bn' | 'en' }> = ({ lang }) => {
  const engine = AudioEngine.getInstance();
  const [gains, setGains] = useState([0, 0, 0, 0, 0]);
  const [preset, setPreset] = useState('flat');
  const [bass, setBass] = useState(0);
  const [treble, setTreble] = useState(0);
  const apply = (choice: EqualizerPreset) => {
    setPreset(choice.id); setGains([...choice.gains]); engine.applyPreset(choice);
  };
  const reset = () => {
    apply(EQ_PRESETS[0]); setBass(0); setTreble(0);
    engine.setBassBoost(0); engine.setTrebleBoost(0);
  };
  return <div className="max-w-lg mx-auto px-5 pt-8 pb-36 space-y-6 text-white">
    <div className="flex items-center justify-between"><h2 className="flex items-center gap-2 text-xl font-semibold"><Sliders size={22}/>{lang === 'bn' ? 'সাউন্ড সেটিংস' : 'Sound settings'}</h2><button onClick={reset} aria-label="Reset equalizer" className="p-2 rounded-xl bg-white/10"><RotateCcw size={19}/></button></div>
    <div className="rounded-3xl bg-white/10 border border-white/15 p-5">
      <label className="block text-sm text-slate-300 mb-3">{lang === 'bn' ? 'প্রিসেট' : 'Preset'}</label>
      <div className="flex flex-wrap gap-2">{EQ_PRESETS.map(choice => <button key={choice.id} onClick={() => apply(choice)} className={`px-3 py-2 rounded-xl text-xs border ${preset === choice.id ? 'bg-purple-400/30 border-purple-300/50' : 'bg-white/5 border-white/10'}`}>{lang === 'bn' ? choice.bnName : choice.name}</button>)}</div>
    </div>
    <div className="rounded-3xl bg-white/10 border border-white/15 p-5">
      <h3 className="font-semibold mb-5">{lang === 'bn' ? 'পাঁচ ব্যান্ড ইকুয়ালাইজার' : 'Five band equalizer'}</h3>
      <div className="grid grid-cols-5 gap-3">{gains.map((value, index) => <label key={index} className="flex flex-col items-center gap-3 text-xs text-slate-300">
        <span>{value > 0 ? '+' : ''}{value} dB</span>
        <input aria-label={EQ_FREQUENCIES[index] + ' Hz'} type="range" min="-12" max="12" value={value} onChange={e => { const next = [...gains]; next[index] = Number(e.target.value); setGains(next); setPreset('custom'); engine.setEqualizerBandGain(index, next[index]); }} className="w-full accent-purple-300"/>
        <span>{EQ_FREQUENCIES[index] >= 1000 ? (EQ_FREQUENCIES[index]/1000).toFixed(1)+'k' : EQ_FREQUENCIES[index]}</span>
      </label>)}</div>
    </div>
    {[{ label: lang === 'bn' ? 'বাস' : 'Bass', value: bass, set: setBass, apply: (v: number) => engine.setBassBoost(v) }, { label: lang === 'bn' ? 'ট্রেবল' : 'Treble', value: treble, set: setTreble, apply: (v: number) => engine.setTrebleBoost(v) }].map(item =>
      <label key={item.label} className="block rounded-2xl bg-white/10 border border-white/15 p-4 text-sm">{item.label} · {item.value}%<input className="w-full mt-3 accent-purple-300" type="range" min="0" max="100" value={item.value} onChange={e => { const n=Number(e.target.value); item.set(n); item.apply(n); }}/></label>)}
  </div>;
};
