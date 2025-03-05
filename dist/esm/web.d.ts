import { WebPlugin } from '@capacitor/core';
import type { AutoPayPlugin } from './definitions';
export declare class AutoPayWeb extends WebPlugin implements AutoPayPlugin {
    checkOverlayPermission(): Promise<any>;
    enableOverlayPermission(): Promise<any>;
    checkAccessibility(): Promise<any>;
    enableAccessibility(): Promise<any>;
    stopNavigation(options: any): Promise<any>;
    performGesture(options: {
        x: number;
        y: number;
    }): Promise<any>;
    startAutoPay(options: any): Promise<any>;
    navigateGCash(options: any): Promise<any>;
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
