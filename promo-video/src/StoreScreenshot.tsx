import React from 'react';
import {AbsoluteFill} from 'remotion';
import {Backdrop} from './components/Backdrop';
import {Phone} from './components/Phone';
import {Badge, SceneArt} from './components/Ui';
import {BoostWidget, ClutterWidget, StripWidget} from './components/Widgets';
import {C, MONO, SANS} from './theme';

type Art = 'beach' | 'mountain' | 'portrait' | 'city';
/** Demo content drawn over the privacy-blurred parts of a real screenshot (720×1600 px coords). */
export type Overlay = {x: number; y: number; w: number; h: number; art?: Art; hue?: number; title?: string; sub?: string; tag?: string; radius?: number};

export type StoreScreenshotProps = {
	readonly screen: string;
	readonly headline: string;
	readonly sub: string;
	readonly badge: string;
	readonly accent: string;
	readonly overlays: Overlay[];
	readonly widgets: boolean;
};

const pct = (v: number, of: number) => `${(v / of) * 100}%`;

const OverlayBox: React.FC<{readonly o: Overlay}> = ({o}) => (
	<div
		style={{
			position: 'absolute',
			left: pct(o.x, 720),
			top: pct(o.y, 1600),
			width: pct(o.w, 720),
			height: pct(o.h, 1600),
			borderRadius: o.radius ?? 14,
			overflow: 'hidden',
			backgroundColor: C.card,
		}}
	>
		{o.art ? <SceneArt kind={o.art} hue={o.hue} /> : null}
		{o.tag ? (
			<div style={{position: 'absolute', right: 8, top: 8, fontFamily: MONO, fontWeight: 700, fontSize: 13, color: C.emeraldLight, background: 'rgba(9,13,22,0.8)', padding: '3px 9px', borderRadius: 99}}>{o.tag}</div>
		) : null}
		{o.title ? (
			<div style={{position: 'absolute', left: 10, right: 10, bottom: 8, color: '#fff', textShadow: '0 2px 8px #000'}}>
				<div style={{fontFamily: SANS, fontWeight: 700, fontSize: 19}}>{o.title}</div>
				{o.sub ? <div style={{fontFamily: MONO, fontSize: 12, color: C.textDim, marginTop: 3}}>{o.sub}</div> : null}
			</div>
		) : null}
	</div>
);

/** 1080×1920 Play Store screenshot: ASO headline, glowing badge, real app screen in a 3D phone. */
export const StoreScreenshot: React.FC<StoreScreenshotProps> = ({screen, headline, sub, badge, accent, overlays, widgets}) => {
	const words = headline.split(' ');
	return (
		<AbsoluteFill>
			<Backdrop seed={screen} particles={30} />
			<AbsoluteFill style={{alignItems: 'center', padding: '110px 80px 0', boxSizing: 'border-box'}}>
				<Badge text={badge} color={accent} dot size={30} />
				<div
					style={{
						marginTop: 34,
						fontFamily: SANS,
						fontWeight: 800,
						fontSize: 92,
						lineHeight: 1.04,
						letterSpacing: '-0.02em',
						color: C.text,
						textAlign: 'center',
					}}
				>
					{words.map((w, i) =>
						w.startsWith('*') ? (
							<span key={i} style={{backgroundImage: `linear-gradient(90deg, ${accent}, ${C.indigoLight})`, WebkitBackgroundClip: 'text', color: 'transparent'}}>
								{w.replace(/\*/g, '')}{' '}
							</span>
						) : (
							<span key={i}>{w} </span>
						),
					)}
				</div>
				<div style={{marginTop: 22, fontFamily: SANS, fontWeight: 500, fontSize: 44, color: C.textDim, textAlign: 'center', lineHeight: 1.25}}>{sub}</div>
			</AbsoluteFill>
			{/* phone: bleeds off the bottom edge, slight 3D tilt */}
			<div style={{position: 'absolute', left: 0, right: 0, top: 640, display: 'flex', justifyContent: 'center'}}>
				{widgets ? (
					// Widgets live on the home screen: show them on a wallpaper, not over the app.
					<Phone width={720} rotateY={-6} rotateX={8} glow={accent}>
						<div style={{position: 'absolute', inset: 0, background: 'linear-gradient(160deg,#1b2a4a 0%,#2c1f4a 45%,#0f3b3a 100%)'}} />
						<div style={{position: 'absolute', left: 0, right: 0, top: 90, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 26}}>
							<BoostWidget s={1.08} width={604} />
							<div style={{display: 'flex', gap: 24, width: 604}}>
								<ClutterWidget s={1.0} width={290} />
								<div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 22, flex: 1, alignContent: 'start', paddingTop: 10}}>
									{['#4EDEA3', '#C0C1FF', '#FFB95F', '#F43F5E', '#06B6D4', '#8B5CF6'].map((c) => (
										<div key={c} style={{aspectRatio: '1', borderRadius: 28, background: `${c}55`, border: `1px solid ${c}88`}} />
									))}
								</div>
							</div>
							<StripWidget s={1.0} width={604} />
						</div>
					</Phone>
				) : (
					<Phone width={720} screen={`screens/${screen}`} rotateY={-6} rotateX={8} glow={accent}>
						{overlays.length ? overlays.map((o, i) => <OverlayBox key={i} o={o} />) : null}
					</Phone>
				)}
			</div>
		</AbsoluteFill>
	);
};
