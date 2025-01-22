export interface AutoPayPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
