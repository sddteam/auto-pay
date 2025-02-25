import { registerPlugin } from '@capacitor/core';
const AutoPay = registerPlugin('AutoPay', {
    web: () => import('./web').then((m) => new m.AutoPayWeb()),
});
export * from './definitions';
export { AutoPay };
//# sourceMappingURL=index.js.map