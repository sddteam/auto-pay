import { PluginListenerHandle } from "@capacitor/core";

export interface AutoPayPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  performGesture(options: { x: number, y: number }): Promise<any>;
  navigateGCash(options: any): Promise<any>;
  stopNavigation(options: any): Promise<any>;
  checkAccessibility(): Promise<any>;
  enableAccessibility(): Promise<any>;
  checkOverlayPermission(): Promise<any>;
  enableOverlayPermission(): Promise<any>;
  addListener(
    eventName: 'warning',
    listenerFunc: (data: { message: string }) => void
  ): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'error',
    listenerFunc: (data: { message: string }) => void
  ): Promise<PluginListenerHandle>;
  removeAllListeners(): Promise<void>;
  startAutoPay(options: any): Promise<any>;
}
