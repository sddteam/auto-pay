import { registerPlugin } from '@capacitor/core';

import type { AutoPayPlugin } from './definitions';

const AutoPay = registerPlugin<AutoPayPlugin>('AutoPay', {
  web: () => import('./web').then((m) => new m.AutoPayWeb()),
});

export * from './definitions';
export { AutoPay };
