export interface AutoPayPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  performGesture(options: { x: number, y: number }): Promise<any>;
  navigateGCash(options: any): Promise<any>;
  stopNavigation(options: any): Promise<any>;
}
