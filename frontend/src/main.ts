import { createApp } from 'vue';
import { Button, Cell, CellGroup, Tag, Tabs, Tab, Field, Dialog, Empty } from 'vant';
import 'vant/lib/index.css';
import App from './App.vue';
import './styles.css';

createApp(App)
  .use(Button)
  .use(Cell)
  .use(CellGroup)
  .use(Tag)
  .use(Tabs)
  .use(Tab)
  .use(Field)
  .use(Dialog)
  .use(Empty)
  .mount('#app');
