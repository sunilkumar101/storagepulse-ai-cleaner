import React from 'react';
import {Composition, Folder, Still} from 'remotion';
import {StoreBannerAutoplay} from './StoreBannerAutoplay';
import {StoreScreenshot} from './StoreScreenshot';
import {FeatureGraphic, HiResIcon} from './FeatureGraphic';

export const RemotionRoot: React.FC = () => {
	return (
		<>
			<Folder name="StoreBannerAutoplay">
				{/* Google Play promo (YouTube) 16:9 */}
				<Composition id="StoreBanner16x9" component={StoreBannerAutoplay} durationInFrames={1080} fps={60} width={1920} height={1080} />
				{/* Vertical 9:16 (App Store preview / Shorts) */}
				<Composition id="StoreBanner9x16" component={StoreBannerAutoplay} durationInFrames={1080} fps={60} width={1080} height={1920} />
			</Folder>
			<Folder name="PlayStoreFramedScreenshots">
				<Still
					id="Shot01Treemap"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{screen: '03_treemap.png', headline: 'See where every *GB* went', sub: 'Interactive DiskUsage treemap & sunburst', badge: 'FULL-DISK MAP', accent: '#4EDEA3', overlays: [], widgets: false}}
				/>
				<Still
					id="Shot02Dashboard"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{screen: '02_dashboard.png', headline: 'Storage full? *Fix* it in one tap', sub: 'Live telemetry · Safe-to-clean picks', badge: 'STORAGE COMMAND CENTER', accent: '#4EDEA3', overlays: [], widgets: false}}
				/>
				<Still
					id="Shot03AiDedup"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{screen: '04_ai_dedup.png', headline: 'AI finds *duplicates* & blurry shots', sub: 'Always keeps your sharpest photo', badge: 'SMART KEEPER', accent: '#C0C1FF', overlays: [], widgets: false}}
				/>
				<Still
					id="Shot04SwipeDeck"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{
						screen: '05_swipe_deck.png',
						headline: 'Swipe away *heavy* videos',
						sub: 'Up = Safe Trash · Down = keep · Undo anytime',
						badge: '30-DAY SAFE TRASH',
						accent: '#F43F5E',
						overlays: [
							{x: 32, y: 448, w: 656, h: 478, art: 'beach', radius: 26, tag: '#1 LARGEST · 612 MB'},
							{x: 32, y: 936, w: 656, h: 108, title: 'VID_4K_Beach_Trip.mp4', sub: '/DCIM/Camera/ · Sep 14 · 48 Mbps', radius: 10},
						],
						widgets: false,
					}}
				/>
				<Still
					id="Shot05Compressor"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{screen: '06_compressor.png', headline: 'Shrink videos, *keep* the quality', sub: 'H.265 hardware encoding · −76% in this test', badge: 'HARDWARE HEVC', accent: '#FFB95F', overlays: [], widgets: false}}
				/>
				<Still
					id="Shot06Social"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{
						screen: '07_social_cleaner.png',
						headline: 'Clear *WhatsApp* junk safely',
						sub: 'Sent copies & old media · gallery untouched',
						badge: 'CHAT CLEANER',
						accent: '#4EDEA3',
						overlays: [
							{x: 64, y: 1186, w: 208, h: 150, art: 'city', tag: '117 MB'},
							{x: 288, y: 1186, w: 208, h: 150, art: 'beach', tag: '84 MB'},
							{x: 512, y: 1186, w: 176, h: 150, art: 'mountain', tag: '84 MB'},
						],
						widgets: false,
					}}
				/>
				<Still
					id="Shot07Vault"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{
						screen: '08_gallery_vault.png',
						headline: 'Fast gallery + *private* vault',
						sub: 'AES-256 encrypted · fingerprint unlock',
						badge: 'BIOMETRIC VAULT',
						accent: '#C0C1FF',
						overlays: [
							{x: 30, y: 930, w: 318, h: 318, art: 'mountain', title: 'Camera (DCIM)', sub: '1,273 items', tag: '9.2 GB', radius: 22},
							{x: 372, y: 930, w: 318, h: 318, art: 'beach', title: 'Travel', sub: '56 items', tag: '3.2 GB', radius: 22},
							{x: 30, y: 1262, w: 318, h: 140, art: 'city', tag: '3.1 GB', radius: 22},
							{x: 372, y: 1262, w: 318, h: 140, art: 'portrait', tag: '2.1 GB', radius: 22},
						],
						widgets: false,
					}}
				/>
				<Still
					id="Shot08Widgets"
					component={StoreScreenshot}
					width={1080}
					height={1920}
					defaultProps={{screen: '10_widgets_pro_settings.png', headline: 'Live home screen *widgets*', sub: 'Storage, clutter & Safe Trash at a glance', badge: '3 GLANCE WIDGETS', accent: '#4EDEA3', overlays: [], widgets: true}}
				/>
				<Still id="FeatureGraphic" component={FeatureGraphic} width={1024} height={500} />
				<Still id="HiResIcon" component={HiResIcon} width={512} height={512} />
			</Folder>
		</>
	);
};
