import React from 'react';

interface GlassBackgroundProps {
  accentColor?: string;
}

export const GlassBackground: React.FC<GlassBackgroundProps> = ({
  accentColor = 'rgba(168, 85, 247, 0.22)',
}) => {
  return (
    <div className="fixed inset-0 pointer-events-none overflow-hidden z-0 bg-[#090d16]">
      {/* Dynamic Aurora Glow Orbs */}
      <div
        className="absolute -top-[25%] -left-[15%] w-[600px] h-[600px] rounded-full blur-[140px] opacity-40 animate-pulse-glow"
        style={{ background: accentColor }}
      />
      <div
        className="absolute top-[40%] -right-[20%] w-[550px] h-[550px] rounded-full blur-[150px] opacity-35 bg-blue-600/30"
      />
      <div
        className="absolute -bottom-[20%] left-[20%] w-[500px] h-[500px] rounded-full blur-[130px] opacity-25 bg-emerald-600/25"
      />

      {/* Subtle Noise / Mesh Texture */}
      <div className="absolute inset-0 opacity-[0.03] bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:24px_24px]" />

      {/* Subtle specular vignette */}
      <div className="absolute inset-0 bg-radial-gradient from-transparent via-[#090d16]/30 to-[#090d16]/80" />
    </div>
  );
};
