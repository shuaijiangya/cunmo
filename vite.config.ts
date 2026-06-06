import { fileURLToPath, URL } from 'node:url';

import uniPlugin from '@dcloudio/vite-plugin-uni';
import { defineConfig } from 'vite';
import { UnifiedViteWeappTailwindcssPlugin } from 'weapp-tailwindcss/vite';

const uni = 'default' in uniPlugin ? uniPlugin.default : uniPlugin;
const stylesPath = fileURLToPath(new URL('./src/styles.css', import.meta.url));
const tailwindPath = fileURLToPath(new URL('./src/tailwind.generated.css', import.meta.url));

export default defineConfig({
  plugins: [
    uni(),
    UnifiedViteWeappTailwindcssPlugin({
      cssEntries: [tailwindPath, stylesPath],
      rem2rpx: {
        rootValue: 32,
        propList: ['*'],
        transformUnit: 'rpx'
      }
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
});
