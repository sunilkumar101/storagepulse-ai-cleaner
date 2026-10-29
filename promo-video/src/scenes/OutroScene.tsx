import React from 'react';
import {AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig} from 'remotion';
import {Backdrop} from '../components/Backdrop';
import {Kinetic} from '../components/Kinetic';
import {Logo} from '../components/Logo';
import {Badge} from '../components/Ui';
import {BoostWidget, ClutterWidget, StripWidget} from '../components/Widgets';
import {C, SANS} from '../theme';

/** Vault + the 3 live home screen widgets, then logo, promise and CTA. */
export const OutroScene: React.FC = () => {
	const frame = useCurrentFrame();
	const {width, height, fps} = useVideoConfig();
	const portrait = height > width;
	const pop = (d: number) => spring({frame: frame - d, fps, config: {damping: 13, stiffness: 130}});
	const s = portrait ? 1.7 : 1.3;
	const w = (d: number): React.CSSProperties => ({opacity: pop(d), translate: `0 ${(1 - pop(d)) * 120}px`, rotate: `${(1 - pop(d)) * -6}deg`});
	const cta = pop(70);
	const glow = interpolate(Math.sin(frame / 8), [-1, 1], [0.5, 1]);

	const widgets = (
		<div style={{display: 'flex', flexDirection: 'column', gap: 26 * s, alignItems: portrait ? 'center' : 'flex-start'}}>
			<div style={w(8)}>
				<BoostWidget s={s} width={560 * s} />
			</div>
			<div style={{display: 'flex', gap: 26 * s, alignItems: 'flex-start'}}>
				<div style={w(18)}>
					<ClutterWidget s={s} width={267 * s} />
				</div>
				<div style={{...w(28), display: 'flex', flexDirection: 'column', gap: 26 * s}}>
					<div style={{...card(s), width: 267 * s, display: 'flex', alignItems: 'center', gap: 14 * s}}>
						<span style={{fontSize: 40 * s}}>🔒</span>
						<div>
							<div style={{fontFamily: SANS, fontWeight: 700, fontSize: 20 * s, color: C.text}}>Biometric Vault</div>
							<div style={{fontFamily: SANS, fontSize: 15 * s, color: C.indigoLight}}>AES-256-GCM</div>
						</div>
					</div>
				</div>
			</div>
			<div style={w(38)}>
				<StripWidget s={s} width={560 * s} />
			</div>
		</div>
	);

	const brand = (
		<div style={{display: 'flex', flexDirection: 'column', alignItems: portrait ? 'center' : 'flex-start', gap: 30}}>
			<div style={{display: 'flex', alignItems: 'center', gap: 24, opacity: pop(40), scale: String(0.8 + pop(40) * 0.2)}}>
				<Logo size={portrait ? 130 : 110} />
				<div style={{fontFamily: SANS, fontWeight: 800, fontSize: portrait ? 84 : 76, color: C.text, letterSpacing: '-0.02em'}}>
					StoragePulse <span style={{color: C.emeraldLight}}>AI</span>
				</div>
			</div>
			<Kinetic text="100% on-device · Zero cloud uploads" size={portrait ? 50 : 44} delay={52} stagger={2} align={portrait ? 'center' : 'left'} />
			<div
				style={{
					opacity: cta,
					scale: String(0.85 + cta * 0.15),
					padding: '26px 54px',
					borderRadius: 999,
					background: `linear-gradient(90deg, ${C.emeraldLight}, ${C.emerald})`,
					color: C.onEmerald,
					fontFamily: SANS,
					fontWeight: 800,
					fontSize: portrait ? 50 : 42,
					boxShadow: `0 0 ${70 * glow}px ${C.emerald}aa`,
				}}
			>
				Download now on Google Play
			</div>
			<Badge text="3 LIVE HOME SCREEN WIDGETS" color={C.amberLight} size={portrait ? 26 : 22} style={{opacity: pop(30)}} />
		</div>
	);

	return (
		<AbsoluteFill>
			<Backdrop seed="outro" particles={60} />
			<AbsoluteFill style={{display: 'flex', flexDirection: portrait ? 'column' : 'row', alignItems: 'center', justifyContent: 'center', gap: portrait ? 90 : 120, padding: portrait ? '120px 60px' : '0 100px'}}>
				{portrait ? brand : widgets}
				{portrait ? widgets : brand}
			</AbsoluteFill>
		</AbsoluteFill>
	);
};

const card = (s: number): React.CSSProperties => ({
	backgroundColor: C.card,
	border: `1px solid ${C.stroke}`,
	borderRadius: 26 * s,
	padding: 22 * s,
	boxSizing: 'border-box',
	boxShadow: '0 24px 60px rgba(0,0,0,0.5)',
});
