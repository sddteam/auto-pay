import { WebPlugin } from '@capacitor/core';
import type { AutoPayPlugin, PayMateOptions } from './definitions';
export declare class AutoPayWeb extends WebPlugin implements AutoPayPlugin {
    checkOverlayPermission(): Promise<any>;
    enableOverlayPermission(): Promise<any>;
    checkAccessibility(): Promise<any>;
    enableAccessibility(): Promise<any>;
    startAutoPay(options: PayMateOptions): Promise<any>;
}
