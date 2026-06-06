import { createPinia } from 'pinia';
import { createSSRApp } from 'vue';

import App from './App.vue';
import './tailwind.generated.css';
import './styles.css';

export function createApp() {
  const app = createSSRApp(App);
  app.use(createPinia());
  return {
    app
  };
}
