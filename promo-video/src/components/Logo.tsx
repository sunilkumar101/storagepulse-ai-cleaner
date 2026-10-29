import React from 'react';
import {C} from '../theme';

/**
 * StoragePulse mark: the app's emerald pulse line over a storage ring, on an obsidian tile.
 * [bleed] = full-square tile (Play hi-res icon); otherwise rounded.
 */
export const Logo: React.FC<{readonly size: number; readonly bleed?: boolean}> = ({size, bleed}) => (
	<svg width={size} height={size} viewBox="0 0 108 108" style={{display: 'block', filter: bleed ? undefined : `drop-shadow(0 0 ${size * 0.12}px ${C.emerald}88)`}}>
		<defs>
			<linearGradient id="spBg" x1="0" y1="0" x2="1" y2="1">
				<stop offset="0" stopColor="#141c2e" />
				<stop offset="1" stopColor="#070a12" />
			</linearGradient>
			<linearGradient id="spRing" x1="0" y1="0" x2="1" y2="1">
				<stop offset="0" stopColor={C.emeraldLight} />
				<stop offset="1" stopColor={C.indigo} />
			</linearGradient>
		</defs>
		<rect x="0" y="0" width="108" height="108" rx={bleed ? 0 : 26} fill="url(#spBg)" />
		{/* storage ring: 88% used arc */}
		<circle cx="54" cy="54" r="34" fill="none" stroke="#1F2937" strokeWidth="7" />
		<circle cx="54" cy="54" r="34" fill="none" stroke="url(#spRing)" strokeWidth="7" strokeLinecap="round" strokeDasharray={`${2 * Math.PI * 34 * 0.88} 999`} transform="rotate(-90 54 54)" />
		{/* pulse line (same path as the launcher icon) */}
		<path d="M30,56 L44,56 L49,42 L57,70 L63,50 L67,56 L78,56" fill="none" stroke={C.emeraldLight} strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
	</svg>
);
