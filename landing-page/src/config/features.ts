/**
 * Feature Flags Configuration
 *
 * @description
 * Toggle features on/off to customize which sections are enabled.
 * These flags control navigation visibility and section rendering.
 */

import type { FeatureFlags } from '../lib/types';

/** Feature flags to enable/disable site sections */
export const features: FeatureFlags = {
  /** Enable blog section and /blog routes */
  blog: false,

  /** Enable documentation section and /docs routes */
  docs: true,

  /** Enable changelog section and /changelog route */
  changelog: false,

  /** Enable testimonials section and /testimonials route */
  testimonials: false,

  /** Enable roadmap section and /roadmap route */
  roadmap: false,
};
