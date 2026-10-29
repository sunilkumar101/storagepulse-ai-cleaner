import React from 'react';
import {AbsoluteFill} from 'remotion';
import {Backdrop} from './components/Backdrop';
import {Logo} from './components/Logo';
import {Phone} from './components/Phone';
import {Treemap} from './components/Treemap';
import {Badge} from './components/Ui';
import {C, SANS} from './theme';

/** 1024×500 Play feature graphic. Key text kept away from the edges (Play may crop/overlay). */
export const FeatureGraphic: React.FC = () => (
	<AbsoluteFill>
		<Backdrop seed="fg" particles={26} />
		<AbsoluteFill style={{flexDirection: 'row', alignItems: 'center', padding: '0 60px', gap: 30}}>
			<div style={{flex: 1, display: 'flex', flexDirection: 'column', gap: 18}}>
				<div style={{display: 'flex', alignItems: 'center', gap: 16}}>
					<Logo size={72} />
					<div style={{fontFamily: SANS, fontWeight: 800, fontSize: 40, color: C.text}}>
						StoragePulse <span style={{color: C.emeraldLight}}>AI</span>
					</div>
				</div>
				<div style={{fontFamily: SANS, fontWeight: 800, fontSize: 46, lineHeight: 1.05, color: C.text, letterSpacing: '-0.02em'}}>
					See where every{' '}
					<span style={{backgroundImage: `linear-gradient(90deg, ${C.emeraldLight}, ${C.indigoLight})`, WebkitBackgroundClip: 'text', color: 'transparent'}}>GB</span> went.
				</div>
				<div style={{display: 'flex', gap: 10, flexWrap: 'wrap'}}>
					<Badge text="DISK TREEMAP" color={C.emeraldLight} size={15} />
					<Badge text="AI DUPLICATES" color={C.indigoLight} size={15} />
					<Badge text="H.265 COMPRESSOR" color={C.amberLight} size={15} />
				</div>
			</div>
			<div style={{position: 'relative', width: 420, height: 500}}>
				<div style={{position: 'absolute', left: 0, top: 70, opacity: 0.95, rotate: '-4deg'}}>
					<Treemap width={360} height={280} start={-200} />
				</div>
				<div style={{position: 'absolute', right: 0, top: 40}}>
					<Phone width={200} screen="screens/02_dashboard.png" rotateY={-10} rotateX={6} />
				</div>
			</div>
		</AbsoluteFill>
	</AbsoluteFill>
);

/** 512×512 hi-res icon: full-bleed square (Play applies its own mask). */
export const HiResIcon: React.FC = () => (
	<AbsoluteFill style={{backgroundColor: C.canvas}}>
		<Logo size={512} bleed />
	</AbsoluteFill>
);
