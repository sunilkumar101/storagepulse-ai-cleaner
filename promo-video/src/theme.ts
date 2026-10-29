import {loadFont as loadJakarta} from '@remotion/google-fonts/PlusJakartaSans';
import {loadFont as loadMono} from '@remotion/google-fonts/JetBrainsMono';

// Obsidian Telemetry tokens (design_screenshots/.../obsidian_telemetry/DESIGN.md).
export const C = {
	canvas: '#090D16',
	card: '#111827',
	stroke: '#1F2937',
	raised: '#1C1F29',
	emerald: '#10B981',
	emeraldLight: '#4EDEA3',
	onEmerald: '#003824',
	indigo: '#6366F1',
	indigoLight: '#C0C1FF',
	amber: '#F59E0B',
	amberLight: '#FFB95F',
	rose: '#F43F5E',
	violet: '#8B5CF6',
	cyan: '#06B6D4',
	red: '#EF4444',
	text: '#DFE2EF',
	textDim: '#BBCABF',
	muted: '#86948A',
} as const;

export const {fontFamily: SANS} = loadJakarta('normal', {weights: ['500', '600', '700', '800'], subsets: ['latin']});
export const {fontFamily: MONO} = loadMono('normal', {weights: ['500', '700'], subsets: ['latin']});

/** Spring presets (snappy UI, soft camera). */
export const SPRING_UI = {damping: 14, stiffness: 140, mass: 0.6};
export const SPRING_SOFT = {damping: 200};
